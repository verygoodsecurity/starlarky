// Copyright 2016 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.java.turbine.javac;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LambdaExpressionTree.BodyKind;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import java.util.HashSet;
import java.util.Set;

/**
 * Prunes AST nodes that are not required for header compilation.
 *
 * <p>Used by Turbine after parsing and before all subsequent phases to avoid
 * doing unnecessary work.
 */
public class TreePruner {

  /**
   * Prunes AST nodes that are not required for header compilation.
   *
   * <p>Specifically:
   *
   * <ul>
   *   <li>method bodies
   *   <li>class and instance initializer blocks
   *   <li>initializers of definitely non-constant fields
   * </ul>
   */
  static void prune(Context context, JCCompilationUnit unit) {
    unit.accept(new PruningVisitor(context));
    removeUnusedImports(unit);
  }

  /** A {@link TreeScanner} that deletes method bodies and blocks from the AST. */
  private static class PruningVisitor extends TreeScanner {

    private final TreeMaker make;
    private final Symtab symtab;

    PruningVisitor(Context context) {
      this.make = TreeMaker.instance(context);
      this.symtab = Symtab.instance(context);
    }

    JCClassDecl enclClass = null;

    @Override
    public void visitClassDef(JCClassDecl tree) {
      JCClassDecl prev = enclClass;
      enclClass = tree;
      try {
        super.visitClassDef(tree);
      } finally {
        enclClass = prev;
      }
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
      if (tree.body == null) {
        return;
      }
      if (tree.getReturnType() == null && delegatingConstructor(tree.body.stats)) {
        // if the first statement of a constructor declaration delegates to another
        // constructor, it needs to be preserved to satisfy checks in Resolve
        tree.body.stats = com.sun.tools.javac.util.List.of(tree.body.stats.get(0));
        return;
      }
      tree.body.stats = com.sun.tools.javac.util.List.nil();
    }

    @Override
    public void visitLambda(JCLambda tree) {
      if (tree.getBodyKind() == BodyKind.STATEMENT) {
        JCExpression ident = make.at(tree).QualIdent(symtab.assertionErrorType.tsym);
        JCThrow throwTree = make.Throw(make.NewClass(null, List.nil(), ident, List.nil(), null));
        tree.body = make.Block(0, List.of(throwTree));
      }
    }

    @Override
    public void visitBlock(JCBlock tree) {
      tree.stats = List.nil();
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
      if ((tree.mods.flags & Flags.ENUM) == Flags.ENUM) {
        // javac desugars enum constants into fields during parsing
        super.visitVarDef(tree);
        return;
      }
      // drop field initializers unless the field looks like a JLS §4.12.4 constant variable
      if (isConstantVariable(enclClass, tree)) {
        return;
      }
      tree.init = null;
    }
  }

  private static boolean delegatingConstructor(List<JCStatement> stats) {
    if (stats.isEmpty()) {
      return false;
    }
    JCStatement stat = stats.get(0);
    if (stat.getKind() != Kind.EXPRESSION_STATEMENT) {
      return false;
    }
    JCExpression expr = ((JCExpressionStatement) stat).getExpression();
    if (expr.getKind() != Kind.METHOD_INVOCATION) {
      return false;
    }
    JCExpression method = ((JCMethodInvocation) expr).getMethodSelect();
    Name name;
    switch (method.getKind()) {
      case IDENTIFIER:
        name = ((JCIdent) method).getName();
        break;
      case MEMBER_SELECT:
        name = ((JCFieldAccess) method).getIdentifier();
        break;
      default:
        return false;
    }
    return name.contentEquals("this") || name.contentEquals("super");
  }

  private static boolean isFinal(JCClassDecl enclClass, JCVariableDecl tree) {
    if ((tree.mods.flags & Flags.FINAL) == Flags.FINAL) {
      return true;
    }
    if (enclClass != null && (enclClass.mods.flags & (Flags.ANNOTATION | Flags.INTERFACE)) != 0) {
      // Fields in annotation declarations and interfaces are implicitly final
      return true;
    }
    return false;
  }

  private static boolean isConstantVariable(JCClassDecl enclClass, JCVariableDecl tree) {
    if (!isFinal(enclClass, tree)) {
      return false;
    }
    if (!constantType(tree.getType())) {
      return false;
    }
    if (tree.getInitializer() != null) {
      Boolean result = tree.getInitializer().accept(CONSTANT_VISITOR, null);
      if (result == null || !result) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true iff the given tree could be the type name of a constant type.
   *
   * <p>This is a conservative over-approximation: an identifier named {@code String}
   * isn't necessarily a type name, but this is used at parse-time before types have
   * been attributed.
   */
  private static boolean constantType(JCTree tree) {
    switch (tree.getKind()) {
      case PRIMITIVE_TYPE:
        return true;
      case IDENTIFIER:
        return tree.toString().contentEquals("String");
      case MEMBER_SELECT:
        return tree.toString().contentEquals("java.lang.String");
      default:
        return false;
    }
  }

  /** A visitor that identifies JLS §15.28 constant expressions. */
  private static final SimpleTreeVisitor<Boolean, Void> CONSTANT_VISITOR =
      new SimpleTreeVisitor<Boolean, Void>(false) {

        @Override
        public Boolean visitConditionalExpression(ConditionalExpressionTree node, Void p) {
          return reduce(
              node.getCondition().accept(this, null),
              node.getTrueExpression().accept(this, null),
              node.getFalseExpression().accept(this, null));
        }

        @Override
        public Boolean visitParenthesized(ParenthesizedTree node, Void p) {
          return node.getExpression().accept(this, null);
        }

        @Override
        public Boolean visitUnary(UnaryTree node, Void p) {
          switch (node.getKind()) {
            case UNARY_PLUS:
            case UNARY_MINUS:
            case BITWISE_COMPLEMENT:
            case LOGICAL_COMPLEMENT:
              break;
            default:
              // non-constant unary expression
              return false;
          }
          return node.getExpression().accept(this, null);
        }

        @Override
        public Boolean visitBinary(BinaryTree node, Void p) {
          switch (node.getKind()) {
            case MULTIPLY:
            case DIVIDE:
            case REMAINDER:
            case PLUS:
            case MINUS:
            case LEFT_SHIFT:
            case RIGHT_SHIFT:
            case UNSIGNED_RIGHT_SHIFT:
            case LESS_THAN:
            case LESS_THAN_EQUAL:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
            case AND:
            case XOR:
            case OR:
            case CONDITIONAL_AND:
            case CONDITIONAL_OR:
            case EQUAL_TO:
            case NOT_EQUAL_TO:
              break;
            default:
              // non-constant binary expression
              return false;
          }
          return reduce(
              node.getLeftOperand().accept(this, null), node.getRightOperand().accept(this, null));
        }

        @Override
        public Boolean visitTypeCast(TypeCastTree node, Void p) {
          return reduce(
              constantType((JCTree) node.getType()), node.getExpression().accept(this, null));
        }

        @Override
        public Boolean visitMemberSelect(MemberSelectTree node, Void p) {
          return node.getExpression().accept(this, null);
        }

        @Override
        public Boolean visitIdentifier(IdentifierTree node, Void p) {
          // Assume all variables are constant variables. This is a conservative assumption, but
          // it's the best we can do with only syntactic information.
          return true;
        }

        @Override
        public Boolean visitLiteral(LiteralTree node, Void unused) {
          switch (node.getKind()) {
            case STRING_LITERAL:
            case INT_LITERAL:
            case LONG_LITERAL:
            case FLOAT_LITERAL:
            case DOUBLE_LITERAL:
            case BOOLEAN_LITERAL:
            case CHAR_LITERAL:
              return true;
            default:
              return false;
          }
        }

        public boolean reduce(Boolean... bx) {
          boolean r = true;
          for (Boolean b : bx) {
            r &= firstNonNull(b, false);
          }
          return r;
        }
      };

  private static void removeUnusedImports(JCCompilationUnit unit) {
    Set<String> usedNames = new HashSet<>();
    // TODO(cushon): consider folding this into PruningVisitor to avoid a second pass
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitImport(ImportTree importTree, Void usedSymbols) {
        return null;
      }

      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (tree == null) {
          return null;
        }
        usedNames.add(tree.getName().toString());
        return null;
      }
    }.scan(unit, null);
    com.sun.tools.javac.util.List<JCTree> replacements = com.sun.tools.javac.util.List.nil();
    for (JCTree def : unit.defs) {
      if (!def.hasTag(JCTree.Tag.IMPORT) || !isUnused(unit, usedNames, (JCImport) def)) {
        replacements = replacements.append(def);
      }
    }
    unit.defs = replacements;
  }

  private static boolean isUnused(
      JCCompilationUnit unit, Set<String> usedNames, JCImport importTree) {
    String simpleName =
        importTree.getQualifiedIdentifier() instanceof JCIdent
            ? ((JCIdent) importTree.getQualifiedIdentifier()).getName().toString()
            : ((JCFieldAccess) importTree.getQualifiedIdentifier()).getIdentifier().toString();
    String qualifier =
        ((JCFieldAccess) importTree.getQualifiedIdentifier()).getExpression().toString();
    if (qualifier.equals("java.lang")) {
      return true;
    }
    if (unit.getPackageName() != null && unit.getPackageName().toString().equals(qualifier)) {
      // remove imports of classes from the current package
      return true;
    }
    if (importTree.getQualifiedIdentifier() instanceof JCFieldAccess
        && ((JCFieldAccess) importTree.getQualifiedIdentifier())
            .getIdentifier()
            .contentEquals("*")) {
      return false;
    }
    if (usedNames.contains(simpleName)) {
      return false;
    }
    return true;
  }
}
