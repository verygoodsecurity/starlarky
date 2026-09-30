package com.verygood.security.larky.parser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.nio.charset.StandardCharsets;
import net.starlark.java.eval.StarlarkSemantics;
import org.junit.Test;

/** Type annotations in scripts, enabled by Starlark's experimental type-checking flags. */
public class TypeAnnotationTest {

  private static final StarlarkSemantics STATIC =
      LarkySemantics.LARKY_SEMANTICS.toBuilder()
          .setBool(StarlarkSemantics.EXPERIMENTAL_STARLARK_STATIC_TYPE_CHECKING, true)
          .setBool(StarlarkSemantics.EXPERIMENTAL_STARLARK_DYNAMIC_TYPE_CHECKING, true)
          .build();

  private static final StarlarkSemantics DYNAMIC =
      LarkySemantics.LARKY_SEMANTICS.toBuilder()
          .setBool(StarlarkSemantics.EXPERIMENTAL_STARLARK_DYNAMIC_TYPE_CHECKING, true)
          .build();

  // A module that main.star can load classes from.
  private static final String SHAPES =
      String.join(
          "\n",
          "load('@stdlib//types', 'types')",
          "Shape = type('Shape', (), {})",
          "Square = types.new_class('Square', (Shape,), {}, lambda ns: None)",
          "Other = type('Other', (), {})",
          "");

  private static void exec(StarlarkSemantics semantics, String... lines) throws Exception {
    LarkyScript script =
        new LarkyScript(
            ModuleSupplier.CORE_MODULES, LarkyScript.StarlarkMode.STRICT, ImmutableMap.of(),
            new ModuleSupplier().create(), semantics);
    new LarkyEvaluator(script, new TestingConsole())
        .eval(
            new InMemMapBackedStarFile(
                ImmutableMap.of(
                    "main.star",
                    (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8),
                    "shapes.star",
                    SHAPES.getBytes(StandardCharsets.UTF_8)),
                "main.star"));
  }

  /** Returns the message of the innermost exception that has one. */
  private static String error(StarlarkSemantics semantics, String... lines) {
    Throwable t = assertThrows(Throwable.class, () -> exec(semantics, lines));
    while (t.getCause() != null && t.getMessage() == null) {
      t = t.getCause();
    }
    return t.getMessage();
  }

  @Test
  public void annotationsAreDisallowedByDefault() {
    assertThat(error(LarkySemantics.LARKY_SEMANTICS, "def f(x: int): pass"))
        .contains("type annotations are disallowed");
  }

  @Test
  public void annotatedScriptsRun() throws Exception {
    for (StarlarkSemantics semantics : new StarlarkSemantics[] {STATIC, DYNAMIC}) {
      exec(
          semantics,
          "type Num = int | float",
          "n: Num = 1",
          "def f(x: int, a: list[int], d: dict[str, int], o: None | int = None) -> str:",
          "  return 'a' * (x + len(a) + len(d))",
          "def g(b: bytes) -> bytes:",
          "  return b",
          "f(2, [1], {'a': 1})",
          "g(b'x')",
          "g(bytes('x', 'utf-8'))",
          "def h(b: bytearray) -> bytearray:",
          "  return b",
          "h(bytearray(b'x'))");
    }
  }

  @Test
  public void typedCodeUsesLarkyModules() throws Exception {
    exec(
        STATIC,
        "load('@stdlib//base64', 'base64')",
        "load('@stdlib//larky', 'larky')",
        "def enc(b: bytes) -> bytes:",
        "  return base64.b64encode(b)",
        "def v(s: Any) -> int:",
        "  return s.v",
        "enc(b'hi')",
        "v(larky.struct(v=1))");
  }

  @Test
  public void untypedScriptsRunWithTypeCheckingOn() throws Exception {
    exec(
        STATIC,
        "load('@stdlib//json', 'json')",
        "load('@stdlib//hashlib', 'hashlib')",
        "json.dumps({'a': hashlib.sha256(bytes('x', 'utf-8')).hexdigest()})");
  }

  @Test
  public void staticCheckingRejectsBeforeRunning() {
    assertThat(error(STATIC, "def f(x: int) -> int:", "  return x + 'a'"))
        .contains("operator '+' cannot be applied to types 'int' and 'str'");
    assertThat(error(STATIC, "x: int = 'a'")).contains("cannot assign type 'str' to 'x' of type 'int'");
    assertThat(error(STATIC, "def f(s: str): pass", "f(b'x')"))
        .contains("parameter 's' got value of type 'bytes', want 'str'");
  }

  @Test
  public void bytesAndBytearrayAreDistinct() {
    assertThat(error(DYNAMIC, "def f(b: bytearray): pass", "f(b'x')"))
        .contains("parameter 'b' got value of type 'bytes', want 'bytearray'");
  }

  @Test
  public void dynamicCheckingRejectsCalls() {
    assertThat(error(DYNAMIC, "def f(x: int): pass", "f('a')"))
        .contains("in call to f(), parameter 'x' got value of type 'str', want 'int'");
    assertThat(error(DYNAMIC, "def f() -> str:", "  return 1", "f()"))
        .contains("f(): returns value of type 'int', declares 'str'");
  }

  @Test
  public void loadedClassesAreTypes() throws Exception {
    for (StarlarkSemantics semantics : new StarlarkSemantics[] {STATIC, DYNAMIC}) {
      exec(
          semantics,
          "load('shapes', 'Shape', 'Square')",
          "def area(s: Shape) -> Shape:",
          "  return s",
          "area(Shape())",
          "area(Square())");
      assertThat(error(semantics, "load('shapes', 'Shape', 'Other')", "def f(s: Shape): pass", "f(Other())"))
          .contains("in call to f(), parameter 's' got value of type 'Other', want 'Shape'");
      assertThat(error(semantics, "load('shapes', 'Shape', 'Square')", "def f(s: Square): pass", "f(Shape())"))
          .contains("in call to f(), parameter 's' got value of type 'Shape', want 'Square'");
    }
    assertThat(error(STATIC, "load('shapes', 'Shape')", "def f(s: Shape): pass", "f(1)"))
        .contains("Error type checking");
  }

  @Test
  public void classesDefinedInTheScriptAreTypes() throws Exception {
    for (StarlarkSemantics semantics : new StarlarkSemantics[] {STATIC, DYNAMIC}) {
      exec(
          semantics,
          "load('shapes', 'Shape')",
          "load('@stdlib//types', 'types')",
          "A = type('A', (), {})",
          "C = types.new_class('C', (Shape,), {}, lambda ns: None)",
          "def f(a: A):",
          "  return a.x",
          "def g(c: C) -> Shape:",
          "  return c",
          "a = A()",
          "a.x = 1",
          "f(a)",
          "g(C())");
      assertThat(error(semantics, "A = type('A', (), {})", "B = type('B', (), {})", "def f(a: A): pass", "f(B())"))
          .contains("in call to f(), parameter 'a' got value of type 'B', want 'A'");
    }
  }

  @Test
  public void methodsOfClassesAreCallable() throws Exception {
    for (StarlarkSemantics semantics : new StarlarkSemantics[] {STATIC, DYNAMIC}) {
      exec(
          semantics,
          "load('@stdlib//types', 'types')",
          "def _init(ns):",
          "  def double(self, x):",
          "    return 2 * x",
          "  ns['double'] = double",
          "A = types.new_class('A', (), {}, _init)",
          "def apply(f: Callable, x: int) -> int:",
          "  return f(x)",
          "apply(A().double, 2)");
    }
  }
}
