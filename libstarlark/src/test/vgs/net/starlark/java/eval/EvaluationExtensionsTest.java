package net.starlark.java.eval;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.TokenKind;
import org.junit.Test;

public class EvaluationExtensionsTest {
  private static final FileOptions OPTIONS =
      FileOptions.DEFAULT.toBuilder().allowToplevelRebinding(true).build();

  @Test
  public void overridesSelectedOperationsAndObservesEachAugmentedAssignmentOnce() throws Exception {
    List<Boolean> augmented = new ArrayList<>();
    EvaluationHooks hooks = new EvaluationHooks() {
      @Override
      public Object afterBinary(
          StarlarkThread thread, TokenKind op, Object left, Object right, Object result,
          boolean inPlace) {
        augmented.add(inPlace);
        return result;
      }
    };
    EvaluationExtensions extensions = new EvaluationExtensions(
        hooks,
        (upstream, op, left, right, thread, inPlace) -> {
          if (op == TokenKind.PLUS && left instanceof String && right instanceof String) {
            return left + ":" + right;
          }
          return upstream.apply(op, left, right, thread);
        });

    try (Mutability mu = Mutability.create("binary hooks")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Module module = Module.create();
      try (var scope = extensions.installOn(thread)) {
        exec(thread, module, """
            left = "a"
            text = left + "b"
            number = 1
            number += 2
            items = [1]
            alias = items
            items += [2]
            """);
      }
      assertThat(module.getGlobal("text")).isEqualTo("a:b");
      assertThat(module.getGlobal("number")).isEqualTo(StarlarkInt.of(3));
      assertThat(module.getGlobal("items")).isSameInstanceAs(module.getGlobal("alias"));
      assertThat((List<?>) module.getGlobal("items")).hasSize(2);
      assertThat(augmented).containsExactly(false, true, true).inOrder();

      exec(thread, module, "default_left = \"a\"\ndefault_text = default_left + \"b\"");
      assertThat(module.getGlobal("default_text")).isEqualTo("ab");
      assertThat(augmented).hasSize(3);
    }
  }

  @Test
  public void restoresTheOuterScopeAfterFailure() throws Exception {
    EvaluationExtensions outer =
        new EvaluationExtensions(null, (next, op, x, y, thread, inPlace) -> "outer");
    EvaluationExtensions inner =
        new EvaluationExtensions(null, (next, op, x, y, thread, inPlace) -> "inner");

    try (Mutability mu = Mutability.create("nested scopes")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Module module = Module.create();
      try (var outerScope = outer.installOn(thread)) {
        assertThrows(EvalException.class, () -> {
          try (var innerScope = inner.installOn(thread)) {
            exec(thread, module, "nested = 1 + 2\nfail(\"fixture failure\")");
          }
        });
        assertThat(module.getGlobal("nested")).isEqualTo("inner");
        exec(thread, module, "restored = 1 + 2");
        assertThat(module.getGlobal("restored")).isEqualTo("outer");
      }
      exec(thread, module, "unconfigured = 1 + 2");
      assertThat(module.getGlobal("unconfigured")).isEqualTo(StarlarkInt.of(3));
    }
  }

  @Test
  public void suppliesBoundArgumentsAndStateOnBuiltinReturnAndFailure() throws Exception {
    List<String> calls = new ArrayList<>();
    EvaluationHooks hooks = new EvaluationHooks() {
      @Override
      public Object beforeBuiltin(
          StarlarkThread thread, Method method, Object receiver, Object[] arguments) {
        calls.add("before");
        return arguments[0];
      }

      @Override
      public Object afterBuiltin(
          StarlarkThread thread, Method method, Object receiver, Object[] arguments,
          Object state, Object result) {
        assertThat(state).isSameInstanceAs(arguments[0]);
        calls.add("return");
        return result;
      }

      @Override
      public void onBuiltinError(
          StarlarkThread thread, Method method, Object receiver, Object[] arguments,
          Object state, Throwable error) {
        assertThat(state).isSameInstanceAs(arguments[0]);
        calls.add("failure");
      }
    };
    try (Mutability mu = Mutability.create("native hooks")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Module module = Module.create();
      try (var scope = new EvaluationExtensions(hooks).installOn(thread)) {
        exec(thread, module, "length = len(\"abc\")\nsorted_values = sorted([1, 2], reverse=True)");
        assertThrows(EvalException.class, () -> exec(thread, module, "int(\"invalid\")"));
      }
      assertThat(module.getGlobal("length")).isEqualTo(StarlarkInt.of(3));
    }
    assertThat(calls).containsExactly("before", "return", "before", "return", "before", "failure")
        .inOrder();
  }

  @Test
  public void observesBranchIterationReadAndOriginalIndexFailure() throws Exception {
    List<String> calls = new ArrayList<>();
    List<EvalException> indexErrors = new ArrayList<>();
    EvaluationHooks hooks = new EvaluationHooks() {
      @Override
      public void onTruthTest(StarlarkThread thread, Object value) {
        calls.add("truth");
      }

      @Override
      public Object afterIterationElement(StarlarkThread thread, Object iterable, Object element) {
        calls.add("element");
        return element;
      }

      @Override
      public Object afterIndex(StarlarkThread thread, Object target, Object key, Object result) {
        calls.add("index");
        return result;
      }

      @Override
      public void onIndexError(StarlarkThread thread, Object target, Object key, EvalException error) {
        indexErrors.add(error);
      }
    };
    try (Mutability mu = Mutability.create("read hooks")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Module module = Module.create();
      try (var scope = new EvaluationExtensions(hooks).installOn(thread)) {
        exec(thread, module, """
            def read(values):
                if values:
                    return [value for value in values][0]
                return None
            result = read(["first", "second"])
            """);
        assertThat(module.getGlobal("result")).isEqualTo("first");
        EvalException thrown =
            assertThrows(EvalException.class, () -> exec(thread, module, "result = [1][9]"));
        assertThat(indexErrors).containsExactly(thrown);
      }
    }
    assertThat(calls).containsExactly("truth", "element", "element", "index").inOrder();
  }

  @Test
  public void notInDelegatesToVanillaMembershipWithoutASecondEvent() throws Exception {
    List<TokenKind> completed = new ArrayList<>();
    List<TokenKind> failed = new ArrayList<>();
    EvaluationHooks hooks = new EvaluationHooks() {
      @Override
      public Object afterBinary(
          StarlarkThread thread, TokenKind op, Object left, Object right, Object result,
          boolean augmented) {
        completed.add(op);
        return result;
      }

      @Override
      public void onBinaryError(
          StarlarkThread thread, TokenKind op, Object left, Object right, boolean augmented,
          EvalException error) {
        failed.add(op);
      }
    };
    EvaluationExtensions extensions = new EvaluationExtensions(
        hooks,
        (upstream, op, left, right, thread, augmented) ->
            op == TokenKind.IN ? false : upstream.apply(op, left, right, thread));
    try (Mutability mu = Mutability.create("membership hooks")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Module module = Module.create();
      try (var scope = extensions.installOn(thread)) {
        exec(thread, module, "present = 'a' in 'a'\nabsent = 'a' not in 'a'");
        assertThrows(EvalException.class, () -> exec(thread, module, "1 not in 'a'"));
      }
      assertThat(module.getGlobal("present")).isEqualTo(false);
      assertThat(module.getGlobal("absent")).isEqualTo(false);
    }
    assertThat(completed).containsExactly(TokenKind.IN, TokenKind.NOT_IN).inOrder();
    assertThat(failed).containsExactly(TokenKind.NOT_IN);
  }

  public static final class InterruptingBuiltin {
    @StarlarkMethod(name = "cancel_evaluation", documented = false)
    public void cancelEvaluation() throws InterruptedException {
      throw new InterruptedException("fixture interruption");
    }
  }

  @Test
  public void reportsInterruptedNativeMethodWithItsOriginalException() throws Exception {
    List<Throwable> failures = new ArrayList<>();
    EvaluationHooks hooks = new EvaluationHooks() {
      @Override
      public void onBuiltinError(
          StarlarkThread thread, Method method, Object receiver, Object[] arguments,
          Object state, Throwable error) {
        failures.add(error);
      }
    };
    try (Mutability mu = Mutability.create("interrupted native method")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      ImmutableMap.Builder<String, Object> bindings = ImmutableMap.builder();
      Starlark.addMethods(bindings, new InterruptingBuiltin(), thread.getSemantics());
      Module module = Module.withPredeclared(thread.getSemantics(), bindings.buildOrThrow());
      try (var scope = new EvaluationExtensions(hooks).installOn(thread)) {
        InterruptedException thrown = assertThrows(
            InterruptedException.class, () -> exec(thread, module, "cancel_evaluation()"));
        assertThat(failures).containsExactly(thrown);
      }
    }
  }

  @Test
  public void reportsResolvedAttributesAndLeavesTheMissingDefaultAlone() throws Exception {
    List<String> attributes = new ArrayList<>();
    List<String> calls = new ArrayList<>();
    EvaluationHooks hooks = new EvaluationHooks() {
      @Override
      public Object afterFieldRead(StarlarkThread thread, Object target, String name, Object result) {
        attributes.add(name);
        return result;
      }

      @Override
      public Object afterCall(StarlarkThread thread, StarlarkCallable callable, Object result) {
        if (callable instanceof BuiltinFunction) {
          calls.add(callable.getName());
        }
        return result;
      }
    };
    try (Mutability mu = Mutability.create("field read hooks")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Module module = Module.create();
      try (var scope = new EvaluationExtensions(hooks).installOn(thread)) {
        exec(thread, module, """
            text = 'a'
            upper = text.upper()
            lower = getattr(text, 'lower')()
            missing = getattr(text, 'missing', 'fallback')
            """);
      }
      assertThat(module.getGlobal("upper")).isEqualTo("A");
      assertThat(module.getGlobal("lower")).isEqualTo("a");
      assertThat(module.getGlobal("missing")).isEqualTo("fallback");
    }
    assertThat(attributes).containsExactly("upper", "lower").inOrder();
    assertThat(calls).containsExactly("upper", "getattr", "lower", "getattr").inOrder();
  }

  private static void exec(StarlarkThread thread, Module module, String source) throws Exception {
    Starlark.execFile(ParserInput.fromString(source, "extensions.star"), OPTIONS, module, thread);
  }
}
