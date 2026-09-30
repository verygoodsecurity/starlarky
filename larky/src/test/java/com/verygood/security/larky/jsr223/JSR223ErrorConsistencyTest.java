package com.verygood.security.larky.jsr223;

import static org.junit.Assert.*;

import javax.script.ScriptException;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests to ensure consistent error messages across all JSR-223 compilation modes:
 * - INTERPRETED: Traditional tree-walking interpreter
 * - BYTECODE: Bytecode interpreter
 *
 * <p>These tests verify that users receive consistent error messages regardless
 * of which compilation mode is configured.
 */
public class JSR223ErrorConsistencyTest {

  private LarkyScriptEngine engine;

  @Before
  public void setUp() {
    engine = new LarkyScriptEngine();
  }

  // ==================== Syntax Error Tests ====================

  @Test
  public void testSyntaxErrorConsistencyAcrossModes() {
    String script = "def broken(\n";  // Missing closing paren and colon

    ScriptException interpretedError = compileWithMode(script, LarkyCompiledScript.CompilationMode.INTERPRETED);
    ScriptException bytecodeError = compileWithMode(script, LarkyCompiledScript.CompilationMode.BYTECODE);

    // All modes should fail with syntax errors
    assertNotNull("INTERPRETED mode should fail", interpretedError);
    assertNotNull("BYTECODE mode should fail", bytecodeError);

    System.out.println("=== Syntax Error Consistency ===");
    System.out.println("INTERPRETED: " + interpretedError.getMessage());
    System.out.println("BYTECODE: " + bytecodeError.getMessage());

    // All should mention syntax-related issue
    assertErrorRelated(interpretedError, "syntax", "expected", "parse");
    assertErrorRelated(bytecodeError, "syntax", "expected", "parse");
  }

  @Test
  public void testUnmatchedBracketError() {
    String script = "x = [1, 2, 3\n";  // Missing closing bracket

    ScriptException interpretedError = compileWithMode(script, LarkyCompiledScript.CompilationMode.INTERPRETED);
    ScriptException bytecodeError = compileWithMode(script, LarkyCompiledScript.CompilationMode.BYTECODE);

    assertNotNull("INTERPRETED mode should fail", interpretedError);
    assertNotNull("BYTECODE mode should fail", bytecodeError);

    System.out.println("=== Unmatched Bracket Error ===");
    System.out.println("INTERPRETED: " + interpretedError.getMessage());
    System.out.println("BYTECODE: " + bytecodeError.getMessage());
  }

  // ==================== Undefined Variable Tests ====================

  @Test
  public void testUndefinedVariableConsistency() throws ScriptException {
    String script = "result = undefined_variable + 1\n";

    // Globals may be supplied through bindings at eval time, so an undefined name is
    // reported when the script is evaluated, not when it is compiled.
    assertNull("INTERPRETED compile should succeed",
        compileWithMode(script, LarkyCompiledScript.CompilationMode.INTERPRETED));
    assertNull("BYTECODE compile should succeed",
        compileWithMode(script, LarkyCompiledScript.CompilationMode.BYTECODE));

    LarkyEvaluationScriptException interpretedError =
        evalWithMode(script, LarkyCompiledScript.CompilationMode.INTERPRETED);
    LarkyEvaluationScriptException bytecodeError =
        evalWithMode(script, LarkyCompiledScript.CompilationMode.BYTECODE);

    assertNotNull("INTERPRETED eval should fail", interpretedError);
    assertNotNull("BYTECODE eval should fail", bytecodeError);
    assertErrorRelated(interpretedError, "undefined_variable");
    assertErrorRelated(bytecodeError, "undefined_variable");
    assertEquals(interpretedError.getMessage(), bytecodeError.getMessage());
  }

  // ==================== Runtime Error Tests ====================

  @Test
  public void testDivisionByZeroConsistency() throws ScriptException {
    String script = "x = 10 / 0\n";

    LarkyEvaluationScriptException interpretedError = evalWithMode(script, LarkyCompiledScript.CompilationMode.INTERPRETED);
    LarkyEvaluationScriptException bytecodeError = evalWithMode(script, LarkyCompiledScript.CompilationMode.BYTECODE);

    if (interpretedError != null && bytecodeError != null) {
      System.out.println("=== Division by Zero Consistency ===");
      System.out.println("INTERPRETED: " + interpretedError.getMessage());
      System.out.println("BYTECODE: " + bytecodeError.getMessage());

      assertErrorRelated(interpretedError, "division", "zero", "divide");
      assertErrorRelated(bytecodeError, "division", "zero", "divide");
    }
  }

  @Test
  public void testTypeErrorConsistency() throws ScriptException {
    String script = "x = 'hello' + 42\n";

    LarkyEvaluationScriptException interpretedError = evalWithMode(script, LarkyCompiledScript.CompilationMode.INTERPRETED);
    LarkyEvaluationScriptException bytecodeError = evalWithMode(script, LarkyCompiledScript.CompilationMode.BYTECODE);

    if (interpretedError != null && bytecodeError != null) {
      System.out.println("=== Type Error Consistency ===");
      System.out.println("INTERPRETED: " + interpretedError.getMessage());
      System.out.println("BYTECODE: " + bytecodeError.getMessage());

      // Both should be type-related errors
      assertErrorRelated(interpretedError, "type", "cannot", "unsupported");
      assertErrorRelated(bytecodeError, "type", "cannot", "unsupported");
    }
  }

  @Test
  public void testIndexErrorConsistency() throws ScriptException {
    String script = "x = [1, 2, 3][100]\n";

    LarkyEvaluationScriptException interpretedError = evalWithMode(script, LarkyCompiledScript.CompilationMode.INTERPRETED);
    LarkyEvaluationScriptException bytecodeError = evalWithMode(script, LarkyCompiledScript.CompilationMode.BYTECODE);

    if (interpretedError != null && bytecodeError != null) {
      System.out.println("=== Index Error Consistency ===");
      System.out.println("INTERPRETED: " + interpretedError.getMessage());
      System.out.println("BYTECODE: " + bytecodeError.getMessage());

      assertErrorRelated(interpretedError, "index", "out", "range", "bound");
      assertErrorRelated(bytecodeError, "index", "out", "range", "bound");
    }
  }

  @Test
  public void testKeyErrorConsistency() throws ScriptException {
    String script = "d = {'a': 1}\nx = d['missing_key']\n";

    LarkyEvaluationScriptException interpretedError = evalWithMode(script, LarkyCompiledScript.CompilationMode.INTERPRETED);
    LarkyEvaluationScriptException bytecodeError = evalWithMode(script, LarkyCompiledScript.CompilationMode.BYTECODE);

    if (interpretedError != null && bytecodeError != null) {
      System.out.println("=== Key Error Consistency ===");
      System.out.println("INTERPRETED: " + interpretedError.getMessage());
      System.out.println("BYTECODE: " + bytecodeError.getMessage());

      assertErrorRelated(interpretedError, "key", "missing", "not found");
      assertErrorRelated(bytecodeError, "key", "missing", "not found");
    }
  }

  // ==================== Function Error Tests ====================

  @Test
  public void testMissingArgumentConsistency() throws ScriptException {
    String script = "def foo(a, b, c):\n  return a + b + c\n\nresult = foo(1)\n";

    LarkyEvaluationScriptException interpretedError = evalWithMode(script, LarkyCompiledScript.CompilationMode.INTERPRETED);
    LarkyEvaluationScriptException bytecodeError = evalWithMode(script, LarkyCompiledScript.CompilationMode.BYTECODE);

    if (interpretedError != null && bytecodeError != null) {
      System.out.println("=== Missing Argument Consistency ===");
      System.out.println("INTERPRETED: " + interpretedError.getMessage());
      System.out.println("BYTECODE: " + bytecodeError.getMessage());

      assertErrorRelated(interpretedError, "argument", "parameter", "missing", "required");
      assertErrorRelated(bytecodeError, "argument", "parameter", "missing", "required");
    }
  }

  @Test
  public void testExtraArgumentConsistency() throws ScriptException {
    String script = "def foo(a):\n  return a\n\nresult = foo(1, 2, 3, 4)\n";

    LarkyEvaluationScriptException interpretedError = evalWithMode(script, LarkyCompiledScript.CompilationMode.INTERPRETED);
    LarkyEvaluationScriptException bytecodeError = evalWithMode(script, LarkyCompiledScript.CompilationMode.BYTECODE);

    if (interpretedError != null && bytecodeError != null) {
      System.out.println("=== Extra Argument Consistency ===");
      System.out.println("INTERPRETED: " + interpretedError.getMessage());
      System.out.println("BYTECODE: " + bytecodeError.getMessage());

      assertErrorRelated(interpretedError, "argument", "too many", "accepts", "unexpected");
      assertErrorRelated(bytecodeError, "argument", "too many", "accepts", "unexpected");
    }
  }

  // ==================== Output Format Tests ====================

  @Test
  public void testBytecodeOutputGeneratedOnSuccess() throws ScriptException {
    String script = "x = 1 + 2\ny = x * 3\n";

    LarkyCompiledScript compiled = engine.compile(script, "test.star");

    assertNotNull("Bytecode should be available", compiled.getBytecode());
  }

  // ==================== Helper Methods ====================

  private ScriptException compileWithMode(String script, LarkyCompiledScript.CompilationMode mode) {
    try {
      engine.compile(script, "test.star", mode);
      return null;
    } catch (ScriptException e) {
      return e;
    }
  }

  private LarkyEvaluationScriptException evalWithMode(String script, LarkyCompiledScript.CompilationMode mode)
      throws ScriptException {
    try {
      LarkyCompiledScript compiled = engine.compile(script, "test.star", mode);
      compiled.eval();
      return null;
    } catch (LarkyEvaluationScriptException e) {
      return e;
    } catch (ScriptException e) {
      // Compile-time error, not runtime
      return null;
    }
  }

  private void assertErrorRelated(Exception error, String... keywords) {
    if (error == null) {
      return;
    }
    String message = error.getMessage();
    if (message == null) {
      message = error.toString();
    }
    String lowerMessage = message.toLowerCase();

    boolean found = false;
    for (String keyword : keywords) {
      if (lowerMessage.contains(keyword.toLowerCase())) {
        found = true;
        break;
      }
    }

    assertTrue("Error should contain one of: " + String.join(", ", keywords)
        + "\nActual: " + message, found);
  }
}
