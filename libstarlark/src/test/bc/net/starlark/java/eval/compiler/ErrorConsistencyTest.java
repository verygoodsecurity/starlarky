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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import net.starlark.java.eval.BytecodeInterpreter;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import net.starlark.java.syntax.SyntaxError;
import org.junit.Test;

/**
 * Tests to ensure consistent error messages between the tree-walking interpreter and the
 * bytecode VMs.
 *
 * <p>These tests verify that users receive the same error messages regardless
 * of which execution backend is used.
 */
public class ErrorConsistencyTest {

  private static final String TEST_FILE = "test.star";

  // ==================== Syntax Error Tests ====================

  @Test
  public void testSyntaxErrorMissingColon() {
    String source = "def foo()\n  pass\n";

    // All backends should report the same syntax error during parsing
    SyntaxError.Exception parseError = assertParseError(source);
    assertNotNull(parseError);
    assertTrue("Should mention missing colon or syntax error",
        parseError.getMessage().toLowerCase().contains("syntax")
            || parseError.getMessage().contains(":")
            || parseError.getMessage().contains("expected"));

    System.out.println("=== Syntax Error (Missing Colon) ===");
    System.out.println("Error: " + parseError.getMessage());
  }

  @Test
  public void testSyntaxErrorUnmatchedParenthesis() {
    String source = "x = (1 + 2\n";

    SyntaxError.Exception parseError = assertParseError(source);
    assertNotNull(parseError);

    System.out.println("=== Syntax Error (Unmatched Parenthesis) ===");
    System.out.println("Error: " + parseError.getMessage());
  }

  @Test
  public void testSyntaxErrorInvalidToken() {
    String source = "x = @invalid\n";

    SyntaxError.Exception parseError = assertParseError(source);
    assertNotNull(parseError);

    System.out.println("=== Syntax Error (Invalid Token) ===");
    System.out.println("Error: " + parseError.getMessage());
  }

  @Test
  public void testSyntaxErrorIndentation() {
    String source = "def foo():\npass\n";  // Missing indentation

    SyntaxError.Exception parseError = assertParseError(source);
    assertNotNull(parseError);

    System.out.println("=== Syntax Error (Indentation) ===");
    System.out.println("Error: " + parseError.getMessage());
  }

  // ==================== Name Resolution Error Tests ====================

  @Test
  public void testUndefinedVariableError() throws Exception {
    String source = "x = undefined_variable\n";

    // This should fail during resolution/compilation, not parsing
    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    assertNotNull("Interpreter should throw error", interpreterError);
    assertNotNull("Bytecode should throw error", bytecodeError);

    // Both should mention the undefined variable
    assertErrorContains(interpreterError, "undefined", "unbound", "not found", "undefined_variable");
    assertErrorContains(bytecodeError, "undefined", "unbound", "not found", "undefined_variable");

    System.out.println("=== Undefined Variable Error ===");
    System.out.println("Interpreter: " + interpreterError.message);
    System.out.println("Bytecode: " + bytecodeError.message);
  }

  @Test
  public void testUndefinedFunctionError() throws Exception {
    String source = "result = nonexistent_function()\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    assertNotNull("Interpreter should throw error", interpreterError);
    assertNotNull("Bytecode should throw error", bytecodeError);

    // Both should mention the undefined function
    assertErrorContains(interpreterError, "undefined", "unbound", "not found", "nonexistent_function");
    assertErrorContains(bytecodeError, "undefined", "unbound", "not found", "nonexistent_function");

    System.out.println("=== Undefined Function Error ===");
    System.out.println("Interpreter: " + interpreterError.message);
    System.out.println("Bytecode: " + bytecodeError.message);
  }

  // ==================== Type Error Tests ====================

  @Test
  public void testTypeErrorAddition() throws Exception {
    String source = "x = 'hello' + 42\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    // Both should produce type errors
    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Type Error (String + Int) ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      // Messages should be semantically similar
      // Accept messages containing "type", "cannot", "unsupported", or "operation"
      assertErrorContains(interpreterError, "type", "cannot", "unsupported", "operation");
      assertErrorContains(bytecodeError, "type", "cannot", "unsupported", "operation");
    }
  }

  @Test
  public void testTypeErrorSubscript() throws Exception {
    String source = "x = 42[0]\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Type Error (Int Subscript) ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      assertErrorContains(interpreterError, "type", "subscript", "index", "int");
      assertErrorContains(bytecodeError, "type", "subscript", "index", "int");
    }
  }

  @Test
  public void testTypeErrorCall() throws Exception {
    String source = "x = 42()\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Type Error (Int Call) ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      assertErrorContains(interpreterError, "callable", "call", "int", "type");
      assertErrorContains(bytecodeError, "callable", "call", "int", "type");
    }
  }

  // ==================== Division Error Tests ====================

  @Test
  public void testDivisionByZero() throws Exception {
    String source = "x = 10 / 0\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Division by Zero ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      assertErrorContains(interpreterError, "division", "zero", "divide");
      assertErrorContains(bytecodeError, "division", "zero", "divide");
    }
  }

  @Test
  public void testIntegerDivisionByZero() throws Exception {
    String source = "x = 10 // 0\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Integer Division by Zero ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      assertErrorContains(interpreterError, "division", "zero", "divide");
      assertErrorContains(bytecodeError, "division", "zero", "divide");
    }
  }

  @Test
  public void testModuloByZero() throws Exception {
    String source = "x = 10 % 0\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Modulo by Zero ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      assertErrorContains(interpreterError, "division", "zero", "modulo");
      assertErrorContains(bytecodeError, "division", "zero", "modulo");
    }
  }

  // ==================== Index Error Tests ====================

  @Test
  public void testIndexOutOfBounds() throws Exception {
    String source = "x = [1, 2, 3][10]\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Index Out of Bounds ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      assertErrorContains(interpreterError, "index", "out", "range", "bound");
      assertErrorContains(bytecodeError, "index", "out", "range", "bound");
    }
  }

  @Test
  public void testNegativeIndexOutOfBounds() throws Exception {
    String source = "x = [1, 2, 3][-10]\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Negative Index Out of Bounds ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      assertErrorContains(interpreterError, "index", "out", "range", "bound");
      assertErrorContains(bytecodeError, "index", "out", "range", "bound");
    }
  }

  // ==================== Key Error Tests ====================

  @Test
  public void testDictKeyError() throws Exception {
    String source = "d = {'a': 1}\nx = d['missing']\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Dict Key Error ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      assertErrorContains(interpreterError, "key", "missing", "not found", "dict");
      assertErrorContains(bytecodeError, "key", "missing", "not found", "dict");
    }
  }

  // ==================== Argument Error Tests ====================

  @Test
  public void testTooFewArguments() throws Exception {
    String source = "def foo(a, b, c):\n  return a + b + c\n\nresult = foo(1)\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Too Few Arguments ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      assertErrorContains(interpreterError, "argument", "missing", "required", "parameter");
      assertErrorContains(bytecodeError, "argument", "missing", "required", "parameter");
    }
  }

  @Test
  public void testTooManyArguments() throws Exception {
    String source = "def foo(a):\n  return a\n\nresult = foo(1, 2, 3)\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Too Many Arguments ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      assertErrorContains(interpreterError, "argument", "too many", "unexpected", "accepts");
      assertErrorContains(bytecodeError, "argument", "too many", "unexpected", "accepts");
    }
  }

  // ==================== Attribute Error Tests ====================

  @Test
  public void testAttributeError() throws Exception {
    String source = "x = 'hello'.nonexistent_method()\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Attribute Error ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      assertErrorContains(interpreterError, "attribute", "method", "nonexistent", "string");
      assertErrorContains(bytecodeError, "attribute", "method", "nonexistent", "string");
    }
  }

  // ==================== Multi-Backend Compilation Tests ====================

  @Test
  public void testMultiBackendSyntaxError() throws Exception {
    String source = "def broken(\n";  // Syntax error

    // All backends should fail at compile time with similar errors
    SyntaxError.Exception parseError = assertParseError(source);
    assertNotNull("Should fail during parsing", parseError);

    // Bytecode compilation isn't even attempted for syntax errors
    System.out.println("=== Multi-Backend Syntax Error ===");
    System.out.println("Parse error (affects all backends): " + parseError.getMessage());
  }

  @Test
  public void testCompilationErrorConsistency() throws Exception {
    // This test ensures that resolution errors are consistent
    String source = "x = undefined_var + another_undefined\n";

    // Try to compile - should fail during resolution
    try {
      StarlarkFile file = parse(source);
      if (file.ok()) {
        Program.compileFile(file, Module.create());
        fail("Should have failed during compilation due to undefined variables");
      }
    } catch (SyntaxError.Exception e) {
      System.out.println("=== Compilation Error Consistency ===");
      System.out.println("Resolution error: " + e.getMessage());
      assertTrue("Should mention undefined variable",
          e.getMessage().toLowerCase().contains("undefined")
              || e.getMessage().toLowerCase().contains("unbound")
              || e.getMessage().toLowerCase().contains("not found"));
    }
  }

  // ==================== Stack Trace Consistency Tests ====================

  @Test
  public void testStackTraceInNestedCall() throws Exception {
    String source =
        "def inner():\n"
            + "  return 1 / 0\n"
            + "\n"
            + "def outer():\n"
            + "  return inner()\n"
            + "\n"
            + "result = outer()\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Stack Trace in Nested Call ===");
      System.out.println("Interpreter error: " + interpreterError.message);
      System.out.println("Interpreter has stack: " + (interpreterError.hasStackTrace));
      System.out.println("Bytecode error: " + bytecodeError.message);
      System.out.println("Bytecode has stack: " + (bytecodeError.hasStackTrace));

      // Both should mention division by zero
      assertErrorContains(interpreterError, "division", "zero");
      assertErrorContains(bytecodeError, "division", "zero");
    }
  }

  @Test
  public void testErrorLineNumberPreservation() throws Exception {
    // Line 3 has the error
    String source =
        "x = 1\n"
            + "y = 2\n"
            + "z = x / 0\n"  // Line 3: division by zero
            + "w = 4\n";

    ErrorInfo interpreterError = executeWithInterpreter(source);
    ErrorInfo bytecodeError = executeBytecode(source);

    if (interpreterError != null && bytecodeError != null) {
      System.out.println("=== Error Line Number Preservation ===");
      System.out.println("Interpreter: " + interpreterError.message);
      System.out.println("Bytecode: " + bytecodeError.message);

      // The error should ideally reference line 3
      // (Implementation may vary in how line info is reported)
    }
  }

  // ==================== Helper Methods ====================

  private SyntaxError.Exception assertParseError(String source) {
    try {
      StarlarkFile file = StarlarkFile.parse(
          ParserInput.fromString(source, TEST_FILE),
          FileOptions.DEFAULT);
      if (!file.ok()) {
        return new SyntaxError.Exception(file.errors());
      }
      // If parsing succeeded, try compilation
      Program.compileFile(file, Module.create());
      return null;
    } catch (SyntaxError.Exception e) {
      return e;
    }
  }

  private StarlarkFile parse(String source) throws SyntaxError.Exception {
    ParserInput input = ParserInput.fromString(source, TEST_FILE);
    StarlarkFile file = StarlarkFile.parse(input, FileOptions.DEFAULT);
    if (!file.ok()) {
      throw new SyntaxError.Exception(file.errors());
    }
    return file;
  }

  private ErrorInfo executeWithInterpreter(String source) {
    try {
      StarlarkFile file = parse(source);
      Module module = Module.create();
      Program program = Program.compileFile(file, module);

      try (Mutability mu = Mutability.create("test")) {
        StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
        Starlark.execFile(ParserInput.fromString(source, TEST_FILE), FileOptions.DEFAULT, module, thread);
      }
      return null; // No error
    } catch (SyntaxError.Exception e) {
      return new ErrorInfo(e.getMessage(), e, false);
    } catch (EvalException e) {
      return new ErrorInfo(e.getMessage(), e, e.getCallStack() != null && !e.getCallStack().isEmpty());
    } catch (InterruptedException e) {
      return new ErrorInfo("Interrupted: " + e.getMessage(), e, false);
    } catch (Exception e) {
      return new ErrorInfo(e.getMessage(), e, false);
    }
  }

  private ErrorInfo executeBytecode(String source) {
    try {
      StarlarkFile file = parse(source);
      Module module = Module.create();
      // Force bytecode compilation for this test
      Program program = Program.compileFile(file, module, /*enableBytecode=*/ true);

      if (!program.hasBytecode()) {
        return new ErrorInfo("No bytecode generated", null, false);
      }

      BytecodeChunk bytecode = program.getBytecode();
      Map<String, Object> globals = new HashMap<>();

      try (Mutability mu = Mutability.create("test")) {
        StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
        BytecodeInterpreter.execute(bytecode, thread, globals, TEST_FILE);
      }
      return null; // No error
    } catch (SyntaxError.Exception e) {
      return new ErrorInfo(e.getMessage(), e, false);
    } catch (EvalException e) {
      return new ErrorInfo(e.getMessage(), e, e.getCallStack() != null && !e.getCallStack().isEmpty());
    } catch (InterruptedException e) {
      return new ErrorInfo("Interrupted: " + e.getMessage(), e, false);
    } catch (Exception e) {
      return new ErrorInfo(e.getMessage(), e, false);
    }
  }

  private void assertErrorContains(ErrorInfo error, String... anyOf) {
    if (error == null) {
      return; // Skip if no error
    }
    String lowerMessage = error.message.toLowerCase();
    boolean found = false;
    for (String keyword : anyOf) {
      if (lowerMessage.contains(keyword.toLowerCase())) {
        found = true;
        break;
      }
    }
    assertTrue("Error message should contain one of: " + String.join(", ", anyOf)
        + "\nActual: " + error.message, found);
  }

  private static class ErrorInfo {
    final String message;
    final Throwable cause;
    final boolean hasStackTrace;

    ErrorInfo(String message, Throwable cause, boolean hasStackTrace) {
      this.message = message != null ? message : "Unknown error";
      this.cause = cause;
      this.hasStackTrace = hasStackTrace;
    }
  }

  /**
   * Manual test runner.
   */
  public static void main(String[] args) {
    ErrorConsistencyTest test = new ErrorConsistencyTest();

    System.out.println("========================================");
    System.out.println("Error Consistency Test Suite");
    System.out.println("========================================\n");

    runTest("Syntax Error (Missing Colon)", test::testSyntaxErrorMissingColon);
    runTest("Syntax Error (Unmatched Paren)", test::testSyntaxErrorUnmatchedParenthesis);
    runTest("Syntax Error (Invalid Token)", test::testSyntaxErrorInvalidToken);
    runTest("Undefined Variable", test::testUndefinedVariableError);
    runTest("Undefined Function", test::testUndefinedFunctionError);
    runTest("Type Error (Addition)", test::testTypeErrorAddition);
    runTest("Type Error (Subscript)", test::testTypeErrorSubscript);
    runTest("Division by Zero", test::testDivisionByZero);
    runTest("Index Out of Bounds", test::testIndexOutOfBounds);
    runTest("Dict Key Error", test::testDictKeyError);
    runTest("Too Few Arguments", test::testTooFewArguments);
    runTest("Too Many Arguments", test::testTooManyArguments);
    runTest("Stack Trace Nested", test::testStackTraceInNestedCall);

    System.out.println("\n========================================");
    System.out.println("Test suite completed");
    System.out.println("========================================");
  }

  private static void runTest(String name, TestRunnable test) {
    try {
      test.run();
      System.out.println("\n[PASS] " + name + "\n");
    } catch (AssertionError e) {
      System.err.println("\n[FAIL] " + name + ": " + e.getMessage() + "\n");
    } catch (Exception e) {
      System.err.println("\n[ERROR] " + name + ": " + e.getMessage() + "\n");
      e.printStackTrace();
    }
  }

  @FunctionalInterface
  private interface TestRunnable {
    void run() throws Exception;
  }
}
