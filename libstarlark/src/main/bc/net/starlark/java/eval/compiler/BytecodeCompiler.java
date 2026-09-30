// Copyright 2025 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.starlark.java.eval.compiler;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.eval.BytecodeFunction;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.syntax.*;

/**
 * Compiles Starlark AST nodes to bytecode.
 *
 * <p>This compiler performs a single pass over the AST and generates stack-based bytecode
 * that can be executed by BytecodeInterpreter or translated to WebAssembly.
 *
 * <p>The compiler assumes the AST has been resolved (variables bound, scopes determined).
 */
public final class BytecodeCompiler {

  /**
   * Version of the code this compiler emits. Bump it whenever the meaning of compiled output
   * changes (instruction semantics, operand encoding, constants), so that serialized modules
   * (CompiledModule) from an older compiler are recompiled instead of run.
   */
  public static final int VERSION = 1;

  private final BytecodeChunk.Builder builder;
  private final Map<String, Integer> labelOffsets;
  private final List<PatchLocation> jumpsToPatch;
  private int loopDepth;
  private List<BreakContinue> breakContinueStack;

  private static class PatchLocation {
    final int instructionIndex;
    final String targetLabel;

    PatchLocation(int instructionIndex, String targetLabel) {
      this.instructionIndex = instructionIndex;
      this.targetLabel = targetLabel;
    }
  }

  private static class BreakContinue {
    final List<Integer> breakInstructions;
    final String continueLabel;

    BreakContinue(String continueLabel) {
      this.breakInstructions = new ArrayList<>();
      this.continueLabel = continueLabel;
    }
  }

  private int labelCounter = 0;

  private BytecodeCompiler(String name) {
    this.builder = new BytecodeChunk.Builder(name);
    this.labelOffsets = new HashMap<>();
    this.jumpsToPatch = new ArrayList<>();
    this.loopDepth = 0;
    this.breakContinueStack = new ArrayList<>();
  }

  /** Reports whether programs are compiled to bytecode by default ({@code -Dstarlark.bytecode}). */
  public static boolean enabledByDefault() {
    return Boolean.getBoolean("starlark.bytecode");
  }

  /**
   * Compiles a program body for {@link Program}, or returns null if {@code enable} is false or
   * compilation fails (the program then runs on the tree-walker). With {@code
   * -Dstarlark.bytecode.strict=true} a failure throws instead, so test runs exercise the bytecode
   * path rather than silently falling back.
   */
  @Nullable
  public static BytecodeChunk compileProgram(Resolver.Function body, boolean enable) {
    if (!enable) {
      return null;
    }
    try {
      return compileFunction(body);
    } catch (Exception e) {
      if (Boolean.getBoolean("starlark.bytecode.strict")) {
        throw new IllegalStateException("Bytecode compilation failed: " + e.getMessage(), e);
      }
      System.err.println("Warning: Bytecode compilation failed: " + e.getMessage());
      return null;
    }
  }

  /**
   * Compiles a Starlark function to bytecode.
   *
   * @param func the resolved function to compile
   * @return the compiled bytecode chunk
   */
  public static BytecodeChunk compileFunction(Resolver.Function func) {
    BytecodeCompiler compiler = new BytecodeCompiler(func.getName());

    // Set up function metadata
    compiler.builder.setParameterCount(func.getParameterNames().size());
    for (String param : func.getParameterNames()) {
      compiler.builder.addParameter(param);
    }
    compiler.builder.setLocalCount(func.getLocals().size());

    // Add local variable names for better error messages
    for (Resolver.Binding binding : func.getLocals()) {
      compiler.builder.addLocal(binding);
    }

    // Compile function body
    for (Statement stmt : func.getBody()) {
      compiler.compileStatement(stmt);
      if (func.isToplevel()) {
        compiler.emitPostAssign(stmt);
      }
    }

    // Ensure function returns (implicit None return)
    compiler.builder.emit(Opcode.LOAD_NONE, -1);
    compiler.builder.emit(Opcode.RETURN, -1);

    // Patch all forward jumps
    compiler.patchJumps();

    return compiler.builder.build();
  }

  /**
   * After an unindented top-level assignment or def, reports each bound global to the thread's
   * post-assign hook, as {@code Eval.execStatements} does (Bazel's "export" semantics).
   */
  private void emitPostAssign(Statement stmt) {
    Iterable<Identifier> ids;
    if (stmt instanceof AssignmentStatement assign) {
      ids = Identifier.boundIdentifiers(assign.getLHS());
    } else if (stmt instanceof DefStatement def) {
      ids = ImmutableList.of(def.getIdentifier());
    } else {
      return;
    }
    for (Identifier id : ids) {
      visit(id);
      emitAt(id.getStartLocation(), Opcode.POST_ASSIGN, builder.addConstant(id.getName()));
    }
  }

  /**
   * Compiles a Starlark module (top-level statements) to bytecode.
   *
   * @param statements the list of statements in the module
   * @param moduleName the name of the module
   * @return the compiled bytecode chunk
   */
  public static BytecodeChunk compileModule(List<Statement> statements, String moduleName) {
    BytecodeCompiler compiler = new BytecodeCompiler(moduleName);

    for (Statement stmt : statements) {
      compiler.compileStatement(stmt);
    }

    // Module execution returns None
    compiler.builder.emit(Opcode.LOAD_NONE, -1);
    compiler.builder.emit(Opcode.RETURN, -1);

    compiler.patchJumps();

    return compiler.builder.build();
  }

  private void compileStatement(Statement stmt) {
    switch (stmt.kind()) {
      case ASSIGNMENT:
        visit((AssignmentStatement) stmt);
        break;
      case EXPRESSION:
        visit((ExpressionStatement) stmt);
        break;
      case IF:
        visit((IfStatement) stmt);
        break;
      case FOR:
        visit((ForStatement) stmt);
        break;
      case DEF:
        visit((DefStatement) stmt);
        break;
      case RETURN:
        visit((ReturnStatement) stmt);
        break;
      case FLOW:
        visit((FlowStatement) stmt);
        break;
      case LOAD:
        visit((LoadStatement) stmt);
        break;
      case TYPE_ALIAS:
        // Binds the alias to its type constructor if the program has a type table (Eval's
        // execTypeAlias).
        Identifier alias = ((TypeAliasStatement) stmt).getIdentifier();
        emitAt(stmt.getStartLocation(), Opcode.TYPE_ALIAS, builder.addConstant(alias));
        break;
      case VAR:
        // No runtime effect, as in Eval.
        break;
      default:
        throw new UnsupportedOperationException("Unsupported statement: " + stmt.kind());
    }
  }

  private void compileExpression(Expression expr) {
    switch (expr.kind()) {
      case IDENTIFIER:
        visit((Identifier) expr);
        break;
      case INT_LITERAL:
        visit((IntLiteral) expr);
        break;
      case FLOAT_LITERAL:
        visit((FloatLiteral) expr);
        break;
      case STRING_LITERAL:
        visit((StringLiteral) expr);
        break;
      case BINARY_OPERATOR:
        visit((BinaryOperatorExpression) expr);
        break;
      case UNARY_OPERATOR:
        visit((UnaryOperatorExpression) expr);
        break;
      case LIST_EXPR:
        visit((ListExpression) expr);
        break;
      case DICT_EXPR:
        visit((DictExpression) expr);
        break;
      case CALL:
        visit((CallExpression) expr);
        break;
      case DOT:
        visit((DotExpression) expr);
        break;
      case INDEX:
        visit((IndexExpression) expr);
        break;
      case SLICE:
        visit((SliceExpression) expr);
        break;
      case CONDITIONAL:
        visit((ConditionalExpression) expr);
        break;
      case COMPREHENSION:
        visit((Comprehension) expr);
        break;
      case LAMBDA:
        visit((LambdaExpression) expr);
        break;
      case CAST:
        // cast(T, x) evaluates to x.
        compileExpression(((CastExpression) expr).getValue());
        break;
      case BYTE_LITERAL:
        builder.emit(
            Opcode.LOAD_BYTES, builder.addConstant(((ByteLiteral) expr).getValue()), getLine(expr));
        break;
      default:
        throw new UnsupportedOperationException("Unsupported expression: " + expr.kind());
    }
  }

  // Statement visitors

  public void visit(AssignmentStatement node) {
    int lineNum = getLine(node);

    if (node.isAugmented()) {
      // x op= y: the LHS operands are evaluated once, and INPLACE_OP applies the
      // in-place semantics (list += list extends, dict |= dict merges).
      Expression lhs = node.getLHS();
      int opIndex = node.getOperator().ordinal();
      Location opLoc = node.getOperatorLocation();
      if (lhs instanceof Identifier) {
        compileExpression(lhs);                               // [x]
        compileExpression(node.getRHS());                     // [x, y]
        emitAt(opLoc, Opcode.INPLACE_OP, opIndex);            // [z]
        storeVariable((Identifier) lhs);
      } else if (lhs instanceof IndexExpression) {
        IndexExpression idx = (IndexExpression) lhs;
        compileExpression(idx.getObject());                   // [obj]
        compileExpression(idx.getKey());                      // [obj, key]
        builder.emit(Opcode.DUP_TOP_TWO, lineNum);            // [obj, key, obj, key]
        emitAt(opLoc, Opcode.INDEX);                          // [obj, key, x]
        compileExpression(node.getRHS());                     // [obj, key, x, y]
        emitAt(opLoc, Opcode.INPLACE_OP, opIndex);            // [obj, key, z]
        builder.emit(Opcode.ROT_THREE, lineNum);              // [z, obj, key]
        emitAt(opLoc, Opcode.STORE_INDEX);
      } else if (lhs instanceof DotExpression) {
        DotExpression dot = (DotExpression) lhs;
        int nameIndex = builder.addConstant(dot.getField().getName());
        compileExpression(dot.getObject());                   // [obj]
        builder.emit(Opcode.DUP, lineNum);                    // [obj, obj]
        emitAt(dot.getDotLocation(), Opcode.LOAD_ATTR, nameIndex);  // [obj, x]
        compileExpression(node.getRHS());                     // [obj, x, y]
        emitAt(opLoc, Opcode.INPLACE_OP, opIndex);            // [obj, z]
        builder.emit(Opcode.SWAP, lineNum);                   // [z, obj]
        emitAt(dot.getDotLocation(), Opcode.STORE_ATTR, nameIndex);
      } else {
        throw new UnsupportedOperationException("cannot perform augmented assignment on " + lhs);
      }
    } else {
      // Regular assignment: compile RHS, store to LHS
      compileExpression(node.getRHS());
      compileLValue(node.getLHS(), node.getOperatorLocation());
    }
  }

  public void visit(ExpressionStatement node) {
    compileExpression(node.getExpression());
    // Expression result is not used, pop it
    builder.emit(Opcode.POP, getLine(node));
  }

  public void visit(IfStatement node) {
    int lineNum = getLine(node);

    // Compile condition
    compileExpression(node.getCondition());

    ImmutableList<Statement> elseBlock = node.getElseBlock();
    boolean hasElse = elseBlock != null && !elseBlock.isEmpty();

    String elseLabel = newLabel("else");
    String endLabel = newLabel("endif");

    // Jump to else/end if condition is false
    emitJump(Opcode.POP_JUMP_IF_FALSE, hasElse ? elseLabel : endLabel, lineNum);

    // Compile then branch
    for (Statement stmt : node.getThenBlock()) {
      compileStatement(stmt);
    }

    if (hasElse) {
      // Jump to end (skip else)
      emitJump(Opcode.JUMP, endLabel, lineNum);

      // Else branch
      markLabel(elseLabel);
      for (Statement stmt : elseBlock) {
        compileStatement(stmt);
      }
    }

    markLabel(endLabel);
  }

  public void visit(ForStatement node) {
    int lineNum = getLine(node);
    loopDepth++;

    String continueLabel = newLabel("for_continue");
    String breakLabel = newLabel("for_break");

    BreakContinue bc = new BreakContinue(continueLabel);
    breakContinueStack.add(bc);

    // Compile iterable expression
    Expression collection = node.getCollection();
    compileExpression(collection);

    // Get iterator - use collection's location for better error messages
    int iterLineNum = getLine(collection);
    int iterColNum = getColumn(collection);
    builder.emitWithColumn(Opcode.GET_ITER, iterLineNum, iterColNum);

    // Loop start
    markLabel(continueLabel);

    // Try to get next item
    emitJump(Opcode.FOR_ITER, breakLabel, lineNum);

    // Assign to loop variable
    compileLValue(node.getVars(), node.getStartLocation());

    // Execute loop body
    for (Statement stmt : node.getBody()) {
      compileStatement(stmt);
    }

    // Jump back to loop start
    emitJump(Opcode.JUMP, continueLabel, lineNum);

    // Loop end
    markLabel(breakLabel);
    builder.emit(Opcode.END_FOR, lineNum); // Pop iterator and remove iteration lock

    // Patch break statements
    for (int breakInstr : bc.breakInstructions) {
      patchJump(breakInstr, breakLabel);
    }

    breakContinueStack.remove(breakContinueStack.size() - 1);
    loopDepth--;
  }

  public void visit(DefStatement node) {
    Identifier id = node.getIdentifier();
    emitMakeFunction(
        id.getName(),
        id.getStartLocation(),
        node.getParameters(),
        node.getResolvedFunction(),
        node.getBody(),
        getLine(node));
    storeVariable(id);
  }

  public void visit(LambdaExpression node) {
    Resolver.Function resolvedFunc = node.getResolvedFunction();
    emitMakeFunction(
        "lambda",
        resolvedFunc.getLocation(),
        node.getParameters(),
        resolvedFunc,
        resolvedFunc.getBody(),  // a single `return <body>` statement
        getLine(node));
  }

  /** Compiles a function body and emits MAKE_FUNCTION, leaving the function on the stack. */
  private void emitMakeFunction(
      String funcName,
      Location funcLocation,
      List<Parameter> params,
      Resolver.Function resolvedFunc,
      List<Statement> body,
      int lineNum) {

    // Extract parameter names from resolved function (includes *args/**kwargs names)
    ImmutableList<String> paramNames = resolvedFunc != null
        ? resolvedFunc.getParameterNames()
        : extractParamNames(params);

    // Get function signature info
    boolean hasVarargs = resolvedFunc != null && resolvedFunc.hasVarargs();
    boolean hasKwargs = resolvedFunc != null && resolvedFunc.hasKwargs();
    int numKeywordOnlyParams = resolvedFunc != null ? resolvedFunc.numKeywordOnlyParams() : 0;
    int localCount = resolvedFunc != null ? resolvedFunc.getLocals().size() : paramNames.size();

    // Compile default value expressions before MAKE_FUNCTION
    // Default values are pushed onto the stack and consumed by MAKE_FUNCTION
    // The defaults tuple covers parameters from first-with-default to last-ordinary-param.
    // Required parameters in that range get MANDATORY sentinel.
    int numDefaults = 0;
    boolean seenFirstDefault = false;
    boolean seenStar = false;  // Track if we're past *args

    // Count parameters excluding *args and **kwargs
    int nparams = params.size() - (hasVarargs ? 1 : 0) - (hasKwargs ? 1 : 0);

    // Find where defaults start and compile them
    // Parameters after * (keyword-only) that don't have defaults need MANDATORY
    for (int i = 0; i < params.size(); i++) {
      Parameter param = params.get(i);

      // Track if we're past *args (keyword-only zone)
      if (param instanceof Parameter.Star) {
        seenStar = true;
        continue;
      }
      if (param instanceof Parameter.StarStar) {
        continue;
      }

      Expression defaultExpr = param.getDefaultValue();
      if (defaultExpr != null) {
        compileExpression(defaultExpr);
        numDefaults++;
        seenFirstDefault = true;
      } else if (seenFirstDefault || seenStar) {
        // After seeing a default OR in keyword-only zone without defaults,
        // we need MANDATORY sentinel for required params
        // Load the MANDATORY constant
        int mandatoryIndex = builder.addConstant(BytecodeFunction.MANDATORY);
        builder.emit(Opcode.LOAD_CONST, mandatoryIndex, lineNum);
        numDefaults++;
      }
    }

    // Create a new compiler for the function body
    BytecodeCompiler funcCompiler = new BytecodeCompiler(funcName);
    funcCompiler.builder.setParameterCount(paramNames.size());

    // Set local count and names from resolved function information
    if (resolvedFunc != null) {
      funcCompiler.builder.setLocalCount(localCount);

      // Add local variable names for better error messages
      for (Resolver.Binding binding : resolvedFunc.getLocals()) {
        funcCompiler.builder.addLocal(binding);
      }
    }

    // Compile function body statements
    for (Statement stmt : body) {
      funcCompiler.compileStatement(stmt);
    }

    // Ensure function returns None if no explicit return
    funcCompiler.builder.emit(Opcode.LOAD_NONE, lineNum);
    funcCompiler.builder.emit(Opcode.RETURN, lineNum);

    // Patch jumps before building the chunk
    funcCompiler.patchJumps();

    BytecodeChunk funcChunk = funcCompiler.builder.build();

    // Build freevar info for closures
    ImmutableList.Builder<FunctionDescriptor.FreevarInfo> freevarInfosBuilder =
        ImmutableList.builder();
    if (resolvedFunc != null) {
      for (Resolver.Binding binding : resolvedFunc.getFreeVars()) {
        // isFromEnclosingFreevars is true if scope is FREE (from outer function's freevars),
        // false if scope is CELL (from current function's locals)
        boolean isFromEnclosingFreevars = binding.getScope() == Resolver.Scope.FREE;
        freevarInfosBuilder.add(
            new FunctionDescriptor.FreevarInfo(isFromEnclosingFreevars, binding.getIndex()));
      }
    }
    ImmutableList<FunctionDescriptor.FreevarInfo> freevarInfos = freevarInfosBuilder.build();

    // Build cell indices (locals that need to be wrapped in Cells for nested functions)
    ImmutableList.Builder<Integer> cellIndicesBuilder = ImmutableList.builder();
    if (resolvedFunc != null) {
      for (int index : resolvedFunc.getCellIndices()) {
        cellIndicesBuilder.add(index);
      }
    }
    ImmutableList<Integer> cellIndices = cellIndicesBuilder.build();

    // Create function descriptor with all signature metadata
    // Note: defaultValues will be collected at runtime from the stack
    FunctionDescriptor descriptor = new FunctionDescriptor(
        funcName,
        funcLocation,
        funcChunk,
        paramNames,
        hasVarargs,
        hasKwargs,
        numKeywordOnlyParams,
        ImmutableList.of(),  // defaults are on stack, collected at runtime
        localCount,
        freevarInfos,
        cellIndices);
    descriptor.setResolvedFunction(resolvedFunc);

    // Store descriptor as constant and emit MAKE_FUNCTION with default count
    int descriptorIndex = builder.addConstant(descriptor);
    // MAKE_FUNCTION takes descriptorIndex as operand1, numDefaults as operand2
    builder.emit(Opcode.MAKE_FUNCTION, descriptorIndex, numDefaults, lineNum);
  }

  private ImmutableList<String> extractParamNames(List<Parameter> params) {
    List<String> names = new ArrayList<>();
    for (Parameter param : params) {
      names.add(param.getIdentifier().getName());
    }
    return ImmutableList.copyOf(names);
  }

  public void visit(ReturnStatement node) {
    int lineNum = getLine(node);

    Expression result = node.getResult();
    if (result != null) {
      compileExpression(result);
    } else {
      builder.emit(Opcode.LOAD_NONE, lineNum);
    }

    builder.emit(Opcode.RETURN, lineNum);
  }

  public void visit(FlowStatement node) {
    int lineNum = getLine(node);

    switch (node.getFlowKind()) {
      case BREAK:
        if (breakContinueStack.isEmpty()) {
          throw new IllegalStateException("break outside loop");
        }
        BreakContinue bc = breakContinueStack.get(breakContinueStack.size() - 1);
        int breakInstr = builder.getInstructionCount();
        builder.emit(Opcode.JUMP, 0, lineNum); // Will be patched later
        bc.breakInstructions.add(breakInstr);
        break;

      case CONTINUE:
        if (breakContinueStack.isEmpty()) {
          throw new IllegalStateException("continue outside loop");
        }
        BreakContinue bcCont = breakContinueStack.get(breakContinueStack.size() - 1);
        emitJump(Opcode.JUMP, bcCont.continueLabel, lineNum);
        break;

      case PASS:
        builder.emit(Opcode.NOP, lineNum);
        break;
    }
  }

  public void visit(LoadStatement node) {
    int lineNum = getLine(node);

    // Load the module - this pushes the Module object onto the stack
    int moduleIndex = builder.addConstant(node.getImport().getValue());
    builder.emit(Opcode.LOAD_MODULE, moduleIndex, lineNum);

    // For each binding, extract the symbol from the module and store it
    for (LoadStatement.Binding binding : node.getBindings()) {
      // Duplicate the module on stack (since LOAD_ATTR will consume it)
      builder.emit(Opcode.DUP, lineNum);

      // Get the symbol from the module
      int symbolIndex = builder.addConstant(binding.getOriginalName().getName());
      builder.emit(Opcode.LOAD_ATTR, symbolIndex, lineNum);

      // Store it to the appropriate variable (local or global based on FileOptions)
      storeVariable(binding.getLocalName());
    }

    // Pop the module from stack
    builder.emit(Opcode.POP, lineNum);
  }

  // Expression visitors

  public void visit(Identifier node) {
    loadVariable(node);
  }

  public void visit(IntLiteral node) {
    // Convert Number to StarlarkInt for proper runtime behavior
    Number value = node.getValue();
    Object starlarkValue;
    if (value instanceof Integer) {
      starlarkValue = StarlarkInt.of(value.intValue());
    } else if (value instanceof Long) {
      starlarkValue = StarlarkInt.of(value.longValue());
    } else if (value instanceof java.math.BigInteger) {
      starlarkValue = StarlarkInt.of((java.math.BigInteger) value);
    } else {
      starlarkValue = StarlarkInt.of(value.longValue());
    }
    int index = builder.addConstant(starlarkValue);
    builder.emit(Opcode.LOAD_CONST, index, getLine(node));
  }

  public void visit(FloatLiteral node) {
    // Convert to StarlarkFloat
    int index = builder.addConstant(StarlarkFloat.of(node.getValue()));
    builder.emit(Opcode.LOAD_CONST, index, getLine(node));
  }

  public void visit(StringLiteral node) {
    int index = builder.addConstant(node.getValue());
    builder.emit(Opcode.LOAD_CONST, index, getLine(node));
  }

  public void visit(BinaryOperatorExpression node) {
    int lineNum = getLine(node);

    // Special handling for short-circuit operators
    if (node.getOperator() == TokenKind.AND) {
      compileExpression(node.getX());
      String endLabel = newLabel("and_end");
      builder.emit(Opcode.DUP, lineNum);
      emitJump(Opcode.POP_JUMP_IF_FALSE, endLabel, lineNum);
      builder.emit(Opcode.POP, lineNum);
      compileExpression(node.getY());
      markLabel(endLabel);
      return;
    }

    if (node.getOperator() == TokenKind.OR) {
      compileExpression(node.getX());
      String endLabel = newLabel("or_end");
      builder.emit(Opcode.DUP, lineNum);
      emitJump(Opcode.POP_JUMP_IF_TRUE, endLabel, lineNum);
      builder.emit(Opcode.POP, lineNum);
      compileExpression(node.getY());
      markLabel(endLabel);
      return;
    }

    // Regular binary operators
    compileExpression(node.getX());
    compileExpression(node.getY());

    Opcode opcode = getOpcodeForBinaryOp(node.getOperator());
    emitAt(node.getOperatorLocation(), opcode);
  }

  public void visit(UnaryOperatorExpression node) {
    compileExpression(node.getX());

    Opcode opcode = getOpcodeForUnaryOp(node.getOperator());
    emitAt(node.getStartLocation(), opcode);
  }

  public void visit(ListExpression node) {
    int lineNum = getLine(node);

    // Compile all elements
    for (Expression elem : node.getElements()) {
      compileExpression(elem);
    }

    // Build list or tuple from elements on stack
    if (node.isTuple()) {
      builder.emit(Opcode.BUILD_TUPLE, node.getElements().size(), lineNum);
    } else {
      builder.emit(Opcode.BUILD_LIST, node.getElements().size(), lineNum);
    }
  }

  public void visit(DictExpression node) {
    int lineNum = getLine(node);

    // Compile all key-value pairs
    for (DictExpression.Entry entry : node.getEntries()) {
      compileExpression(entry.getKey());
      compileExpression(entry.getValue());
    }

    // Build dict from key-value pairs on stack
    builder.emit(Opcode.BUILD_DICT, node.getEntries().size(), lineNum);
  }

  public void visit(CallExpression node) {
    int lineNum = getLine(node);
    // Get the lparen location for precise call site tracking
    Location lparenLoc = node.getLparenLocation();
    int callLineNum = lparenLoc.line();
    int callColNum = lparenLoc.column();

    // Check if we have *args or **kwargs expansion
    boolean hasStar = false;
    boolean hasStarStar = false;
    for (Argument arg : node.getArguments()) {
      if (arg instanceof Argument.Star) {
        hasStar = true;
      } else if (arg instanceof Argument.StarStar) {
        hasStarStar = true;
      }
    }

    // Compile function expression
    compileExpression(node.getFunction());

    if (hasStar || hasStarStar) {
      // Use CALL_EX for complex calls with *args or **kwargs
      // Stack layout: [func, pos_list, kw_dict, star_arg?, starstar_arg?], evaluated in
      // source order (positional < keyword < *args < **kwargs); the flags operand says
      // which of the optional values are present.

      // Collect arguments by type
      List<Expression> posArgs = new ArrayList<>();
      Expression starArg = null;
      List<Argument.Keyword> kwArgs = new ArrayList<>();
      Expression starStarArg = null;

      for (Argument arg : node.getArguments()) {
        if (arg instanceof Argument.Positional) {
          posArgs.add(((Argument.Positional) arg).getValue());
        } else if (arg instanceof Argument.Star) {
          if (starArg != null) {
            throw new IllegalStateException("Multiple *args not supported");
          }
          starArg = ((Argument.Star) arg).getValue();
        } else if (arg instanceof Argument.Keyword) {
          kwArgs.add((Argument.Keyword) arg);
        } else if (arg instanceof Argument.StarStar) {
          if (starStarArg != null) {
            throw new IllegalStateException("Multiple **kwargs not supported");
          }
          starStarArg = ((Argument.StarStar) arg).getValue();
        }
      }

      // Build positional args as a list
      for (Expression posArg : posArgs) {
        compileExpression(posArg);
      }
      builder.emit(Opcode.BUILD_LIST, posArgs.size(), lineNum);

      // Build keyword args as a dict
      for (Argument.Keyword kwArg : kwArgs) {
        int nameIndex = builder.addConstant(kwArg.getName());
        builder.emit(Opcode.LOAD_CONST, nameIndex, lineNum);
        compileExpression(kwArg.getValue());
      }
      builder.emit(Opcode.BUILD_DICT, kwArgs.size(), lineNum);

      if (starArg != null) {
        compileExpression(starArg);
      }
      if (starStarArg != null) {
        compileExpression(starStarArg);
      }

      // Flags: 1 = *args present, 2 = **kwargs present
      int flags = (starArg != null ? 1 : 0) | (starStarArg != null ? 2 : 0);
      builder.emitWithColumn(Opcode.CALL_EX, flags, callLineNum, callColNum);

    } else {
      // Simple call without *args or **kwargs expansion
      int posArgCount = 0;
      int kwArgCount = 0;

      for (Argument arg : node.getArguments()) {
        if (arg instanceof Argument.Positional) {
          compileExpression(((Argument.Positional) arg).getValue());
          posArgCount++;
        } else if (arg instanceof Argument.Keyword) {
          Argument.Keyword kwArg = (Argument.Keyword) arg;
          int nameIndex = builder.addConstant(kwArg.getName());
          builder.emit(Opcode.LOAD_CONST, nameIndex, lineNum);
          compileExpression(kwArg.getValue());
          kwArgCount++;
        }
      }

      builder.emitWithColumn(Opcode.CALL, posArgCount, kwArgCount, callLineNum, callColNum);
    }
  }

  public void visit(DotExpression node) {
    int lineNum = getLine(node);

    // Compile object expression
    compileExpression(node.getObject());

    // Load attribute
    int nameIndex = builder.addConstant(node.getField().getName());
    emitAt(node.getDotLocation(), Opcode.LOAD_ATTR, nameIndex);
  }

  public void visit(IndexExpression node) {
    int lineNum = getLine(node);

    // Compile object and key
    compileExpression(node.getObject());
    compileExpression(node.getKey());

    emitAt(node.getLbracketLocation(), Opcode.INDEX);
  }

  public void visit(SliceExpression node) {
    int lineNum = getLine(node);

    // Compile object
    compileExpression(node.getObject());

    // Compile start, stop, step (push None if not present)
    if (node.getStart() != null) {
      compileExpression(node.getStart());
    } else {
      builder.emit(Opcode.LOAD_NONE, lineNum);
    }

    if (node.getStop() != null) {
      compileExpression(node.getStop());
    } else {
      builder.emit(Opcode.LOAD_NONE, lineNum);
    }

    if (node.getStep() != null) {
      compileExpression(node.getStep());
    } else {
      builder.emit(Opcode.LOAD_NONE, lineNum);
    }

    emitAt(node.getLbracketLocation(), Opcode.SLICE);
  }

  public void visit(ConditionalExpression node) {
    int lineNum = getLine(node);

    String elseLabel = newLabel("cond_else");
    String endLabel = newLabel("cond_end");

    // Compile condition
    compileExpression(node.getCondition());

    // Jump to else if false
    emitJump(Opcode.POP_JUMP_IF_FALSE, elseLabel, lineNum);

    // True branch
    compileExpression(node.getThenCase());
    emitJump(Opcode.JUMP, endLabel, lineNum);

    // False branch
    markLabel(elseLabel);
    compileExpression(node.getElseCase());

    markLabel(endLabel);
  }

  public void visit(Comprehension node) {
    int lineNum = getLine(node);

    // Record where this comprehension's variables are in scope, for the debugger.
    Comprehension.For first = (Comprehension.For) node.getClauses().get(0);
    ComprehensionScope scope =
        new ComprehensionScope(
            first.getIterable().getStartLocation(),
            first.getIterable().getEndLocation(),
            node.getStartLocation(),
            node.getEndLocation());
    for (Comprehension.Clause clause : node.getClauses()) {
      if (clause instanceof Comprehension.For forClause) {
        for (Identifier id : Identifier.boundIdentifiers(forClause.getVars())) {
          if (id.getBinding() instanceof Resolver.ComprehensionBinding) {
            builder.setLocalScope(id.getBinding().getIndex(), scope);
          }
        }
      }
    }

    // Create empty collection
    if (node.isDict()) {
      builder.emit(Opcode.BUILD_DICT, 0, lineNum);
    } else {
      builder.emit(Opcode.BUILD_LIST, 0, lineNum);
    }

    // The result collection is now on top of stack
    // Compile nested loops and conditionals
    // loopDepth tracks how many iterators are on stack above the result
    compileClauses(node, 0, 0);
  }

  // stackDepth is the number of items between TOS and the result collection
  private void compileClauses(Comprehension comp, int clauseIndex, int stackDepth) {
    if (clauseIndex >= comp.getClauses().size()) {
      // Base case: evaluate body and add to result
      if (comp.isDict()) {
        // Dict comprehension: {k: v for ...}
        DictExpression.Entry body = (DictExpression.Entry) comp.getBody();

        // Compile key and value
        compileExpression(body.getKey());
        compileExpression(body.getValue());

        // Stack: [result_dict, ...iterators..., key, value]
        // DICT_ADD pops key and value, adds to dict at stack[-(stackDepth+2)]
        // stackDepth accounts for iterators, +2 for key and value
        emitAt(body.getColonLocation(), Opcode.DICT_ADD, stackDepth + 3);
      } else {
        // List comprehension: [expr for ...]
        Expression body = (Expression) comp.getBody();

        // Compile body expression
        compileExpression(body);

        // Stack: [result_list, ...iterators..., value]
        // LIST_APPEND pops value, appends to list at stack[-(stackDepth+1)]
        // stackDepth accounts for iterators, +1 for the value itself
        builder.emit(Opcode.LIST_APPEND, stackDepth + 2, getLine(comp));
      }
      return;
    }

    // Recursive case: process current clause
    Comprehension.Clause clause = comp.getClauses().get(clauseIndex);

    if (clause instanceof Comprehension.For) {
      Comprehension.For forClause = (Comprehension.For) clause;
      int lineNum = getLine(forClause);

      // Compile iterable expression
      Expression iterable = forClause.getIterable();
      compileExpression(iterable);

      // Get iterator - this adds 1 to stack depth
      // Use the iterable's location for better error messages
      int iterLineNum = getLine(iterable);
      int iterColNum = getColumn(iterable);
      builder.emitWithColumn(Opcode.GET_ITER, iterLineNum, iterColNum);

      // Start of loop
      String continueLabel = newLabel("comp_for_continue");
      String breakLabel = newLabel("comp_for_break");

      markLabel(continueLabel);

      // Try to get next value - FOR_ITER jumps to breakLabel when done
      emitJump(Opcode.FOR_ITER, breakLabel, lineNum);

      // Assign loop variable (pops value from stack)
      compileLValue(forClause.getVars(), forClause.getStartLocation());

      // Process remaining clauses with increased stack depth (iterator added)
      compileClauses(comp, clauseIndex + 1, stackDepth + 1);

      // Continue loop
      emitJump(Opcode.JUMP, continueLabel, lineNum);

      // Loop done - pop iterator and remove iteration lock
      markLabel(breakLabel);
      builder.emit(Opcode.END_FOR, lineNum);

    } else if (clause instanceof Comprehension.If) {
      Comprehension.If ifClause = (Comprehension.If) clause;
      int lineNum = getLine(ifClause);

      // Compile condition
      compileExpression(ifClause.getCondition());

      // If false, skip the body
      String skipLabel = newLabel("comp_if_skip");
      emitJump(Opcode.POP_JUMP_IF_FALSE, skipLabel, lineNum);

      // Process remaining clauses (no stack depth change)
      compileClauses(comp, clauseIndex + 1, stackDepth);

      markLabel(skipLabel);
    }
  }

  // Helper methods

  /**
   * Stores the value on top of the stack to {@code lhs}. {@code errorLoc} is where the
   * tree-walker reports assignment errors in this context (the '=' of an assignment, or the
   * start of a for statement or comprehension clause).
   */
  private void compileLValue(Expression lhs, Location errorLoc) {
    if (lhs instanceof Identifier) {
      storeVariable((Identifier) lhs);
    } else if (lhs instanceof IndexExpression) {
      IndexExpression idx = (IndexExpression) lhs;
      compileExpression(idx.getObject());
      compileExpression(idx.getKey());
      emitAt(errorLoc, Opcode.STORE_INDEX);
    } else if (lhs instanceof DotExpression) {
      DotExpression dot = (DotExpression) lhs;
      compileExpression(dot.getObject());
      int nameIndex = builder.addConstant(dot.getField().getName());
      emitAt(dot.getDotLocation(), Opcode.STORE_ATTR, nameIndex);
    } else if (lhs instanceof ListExpression) {
      // Tuple/list unpacking: a, b = [1, 2]
      // The RHS value is already on the stack
      ListExpression list = (ListExpression) lhs;
      int count = list.getElements().size();

      // Emit UNPACK_SEQUENCE to unpack the sequence into N values on stack
      emitAt(errorLoc, Opcode.UNPACK_SEQUENCE, count);

      // Now assign each unpacked value to the corresponding lvalue
      // UNPACK_SEQUENCE pushes elements in forward order, so rightmost is on top
      // We assign from right to left (popping from stack)
      for (int i = count - 1; i >= 0; i--) {
        compileLValue(list.getElements().get(i), errorLoc);
      }
    } else {
      throw new IllegalArgumentException("Invalid lvalue: " + lhs);
    }
  }

  private void loadVariable(Identifier id) {
    Resolver.Binding binding = id.getBinding();
    int lineNum = getLine(id);

    if (binding == null) {
      // Unresolved - treat as global
      int nameIndex = builder.addConstant(id.getName());
      emitAt(id.getStartLocation(), Opcode.LOAD_GLOBAL, nameIndex);
      return;
    }

    switch (binding.getScope()) {
      case LOCAL:
        emitAt(id.getStartLocation(), Opcode.LOAD_LOCAL, binding.getIndex());
        break;
      case GLOBAL:
        emitAt(id.getStartLocation(), Opcode.LOAD_GLOBAL, builder.addConstant(id.getName()));
        break;
      case PREDECLARED:
      case UNIVERSAL:
        emitAt(id.getStartLocation(), Opcode.LOAD_BUILTIN, builder.addConstant(id.getName()));
        break;
      case FREE:
        emitAt(id.getStartLocation(), Opcode.LOAD_FREE, binding.getIndex());
        break;
      case CELL:
        // Cell variables are locals shared with nested functions
        emitAt(id.getStartLocation(), Opcode.LOAD_CELL, binding.getIndex());
        break;
    }
  }

  private void storeVariable(Identifier id) {
    Resolver.Binding binding = id.getBinding();
    int lineNum = getLine(id);

    if (binding == null) {
      // Unresolved - treat as global
      int nameIndex = builder.addConstant(id.getName());
      builder.emit(Opcode.STORE_GLOBAL, nameIndex, lineNum);
      return;
    }

    switch (binding.getScope()) {
      case LOCAL:
        builder.emit(Opcode.STORE_LOCAL, binding.getIndex(), lineNum);
        break;
      case GLOBAL:
        int nameIndex = builder.addConstant(id.getName());
        builder.emit(Opcode.STORE_GLOBAL, nameIndex, lineNum);
        break;
      case FREE:
        builder.emit(Opcode.STORE_FREE, binding.getIndex(), lineNum);
        break;
      case CELL:
        // Cell variables are locals shared with nested functions
        builder.emit(Opcode.STORE_CELL, binding.getIndex(), lineNum);
        break;
      default:
        throw new IllegalStateException("Cannot store to " + binding.getScope());
    }
  }

  private Opcode getOpcodeForBinaryOp(TokenKind op) {
    switch (op) {
      case PLUS:
        return Opcode.ADD;
      case MINUS:
        return Opcode.SUBTRACT;
      case STAR:
        return Opcode.MULTIPLY;
      case SLASH:
        return Opcode.DIVIDE;
      case SLASH_SLASH:
        return Opcode.FLOOR_DIV;
      case PERCENT:
        return Opcode.MODULO;
      case EQUALS_EQUALS:
        return Opcode.EQUAL;
      case NOT_EQUALS:
        return Opcode.NOT_EQUAL;
      case LESS:
        return Opcode.LESS;
      case LESS_EQUALS:
        return Opcode.LESS_EQUAL;
      case GREATER:
        return Opcode.GREATER;
      case GREATER_EQUALS:
        return Opcode.GREATER_EQUAL;
      case IN:
        return Opcode.IN;
      case NOT_IN:
        return Opcode.NOT_IN;
      case PIPE:
        return Opcode.BIT_OR;
      case AMPERSAND:
        return Opcode.BIT_AND;
      case CARET:
        return Opcode.BIT_XOR;
      case LESS_LESS:
        return Opcode.LEFT_SHIFT;
      case GREATER_GREATER:
        return Opcode.RIGHT_SHIFT;
      default:
        throw new UnsupportedOperationException("Unsupported binary operator: " + op);
    }
  }

  private Opcode getOpcodeForUnaryOp(TokenKind op) {
    switch (op) {
      case MINUS:
        return Opcode.NEGATE;
      case PLUS:
        return Opcode.POSITIVE;
      case NOT:
        return Opcode.NOT;
      case TILDE:
        return Opcode.BIT_NOT;
      default:
        throw new UnsupportedOperationException("Unsupported unary operator: " + op);
    }
  }

  private String newLabel(String prefix) {
    return prefix + "_" + (labelCounter++);
  }

  private void markLabel(String label) {
    // Store instruction index, not byte offset (interpreter uses instruction indices for jumps)
    int instrIndex = builder.getInstructionCount();
    if (Boolean.getBoolean("debug.bytecode")) {
      System.out.printf("LABEL: '%s' = instr %d%n", label, instrIndex);
    }
    labelOffsets.put(label, instrIndex);
  }

  private void emitJump(Opcode jumpOpcode, String targetLabel, int lineNum) {
    int instrIndex = builder.getInstructionCount();
    builder.emit(jumpOpcode, 0, lineNum); // Placeholder offset
    jumpsToPatch.add(new PatchLocation(instrIndex, targetLabel));
  }

  private void patchJump(int instructionIndex, String targetLabel) {
    Integer targetOffset = labelOffsets.get(targetLabel);
    if (targetOffset == null) {
      throw new IllegalStateException("Undefined label: " + targetLabel);
    }
    if (Boolean.getBoolean("debug.bytecode")) {
      System.out.printf("PATCH: instr[%d] -> label '%s' = offset %d%n",
          instructionIndex, targetLabel, targetOffset);
    }
    // Update the jump instruction's operand to point to the target instruction
    builder.updateInstructionOperand(instructionIndex, targetOffset);
  }

  private void patchJumps() {
    // Patch all forward jumps to their target offsets
    // This is a simplified implementation
    for (PatchLocation patch : jumpsToPatch) {
      patchJump(patch.instructionIndex, patch.targetLabel);
    }
  }

  private int getLine(Node node) {
    Location loc = node.getStartLocation();
    return loc != null ? loc.line() : -1;
  }

  /** Emits an instruction whose runtime errors are reported at {@code loc}, like the tree-walker. */
  private void emitAt(Location loc, Opcode opcode, int... operands) {
    switch (operands.length) {
      case 0:
        builder.emitWithColumn(opcode, loc.line(), loc.column());
        break;
      case 1:
        builder.emitWithColumn(opcode, operands[0], loc.line(), loc.column());
        break;
      case 2:
        builder.emitWithColumn(opcode, operands[0], operands[1], loc.line(), loc.column());
        break;
      default:
        throw new IllegalArgumentException("too many operands for " + opcode);
    }
  }

  private int getColumn(Node node) {
    Location loc = node.getStartLocation();
    return loc != null ? loc.column() : 0;
  }
}
