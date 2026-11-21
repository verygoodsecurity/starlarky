package net.starlark.java.eval;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.TokenKind;
import org.junit.Test;

public class EvaluationExtensionsTest {
  @Test
  public void returnsEquivalentResultWithObserverIdentity() throws Exception {
    AtomicReference<String> observed = new AtomicReference<>();
    try (Mutability mu = Mutability.create("binary result")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Module module = Module.create();
      try (var scope = EvaluationExtensions.observeBinary(
          thread, (t, op, left, right, result, augmented) -> {
            assertThat(t).isSameInstanceAs(thread);
            assertThat(op).isEqualTo(TokenKind.PLUS);
            assertThat(left).isEqualTo("a");
            assertThat(right).isEqualTo("b");
            assertThat(result).isEqualTo("ab");
            assertThat(augmented).isFalse();
            String tagged = new String((String) result);
            observed.set(tagged);
            return tagged;
          })) {
        exec(thread, module, "left = \"a\"\ntext = left + \"b\"");
      }
      assertThat(module.getGlobal("text")).isSameInstanceAs(observed.get());
      assertThat(module.getGlobal("text")).isEqualTo("ab");
    }
  }

  @Test
  public void observesSuccessfulOperatorsOnceAndPreservesInPlaceListMutation() throws Exception {
    List<TokenKind> operators = new ArrayList<>();
    List<Boolean> augmented = new ArrayList<>();
    try (Mutability mu = Mutability.create("binary operators")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Module module = Module.create();
      try (var scope = EvaluationExtensions.observeBinary(
          thread, (t, op, left, right, result, inPlace) -> {
            operators.add(op);
            augmented.add(inPlace);
            return result;
          })) {
        exec(thread, module, """
            number = 1
            number += 2
            items = [1]
            alias = items
            items += [2]
            missing = number not in items
            folded = "a" + "b"
            short_and = False and (number + 2)
            short_or = True or (number + 2)
            """);
        assertThrows(EvalException.class, () -> exec(thread, module, "bad = items - 1"));
      }
      assertThat(operators).containsExactly(TokenKind.PLUS, TokenKind.PLUS, TokenKind.NOT_IN)
          .inOrder();
      assertThat(augmented).containsExactly(true, true, false).inOrder();
      assertThat(module.getGlobal("number")).isEqualTo(StarlarkInt.of(3));
      assertThat(module.getGlobal("items")).isSameInstanceAs(module.getGlobal("alias"));
      assertThat((List<?>) module.getGlobal("items"))
          .containsExactly(StarlarkInt.of(1), StarlarkInt.of(2)).inOrder();
      assertThat(module.getGlobal("missing")).isEqualTo(true);
    }
  }

  @Test
  public void restoresOuterObserverOnFailureAndScopesObservationToOneThread() throws Exception {
    List<String> events = new ArrayList<>();
    try (Mutability mu = Mutability.create("nested observers")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      StarlarkThread other = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Module module = Module.create();
      EvaluationExtensions.BinaryObserver outer = (t, op, left, right, result, augmented) -> {
        events.add("outer");
        return result;
      };
      EvaluationExtensions.BinaryObserver inner = (t, op, left, right, result, augmented) -> {
        events.add("inner");
        return result;
      };

      try (var outerScope = EvaluationExtensions.observeBinary(thread, outer)) {
        assertThrows(EvalException.class, () -> {
          try (var innerScope = EvaluationExtensions.observeBinary(thread, inner)) {
            exec(thread, module, "one = 1\nnested = one + 2\nfail(\"fixture failure\")");
          }
        });
        exec(other, Module.create(), "one = 1\nisolated = one + 2");
        exec(thread, module, "restored = one + 2");
      }
      exec(thread, module, "unconfigured = one + 2");

      assertThat(events).containsExactly("inner", "outer").inOrder();
      assertThat(module.getGlobal("nested")).isEqualTo(StarlarkInt.of(3));
      assertThat(module.getGlobal("restored")).isEqualTo(StarlarkInt.of(3));
      assertThat(module.getGlobal("unconfigured")).isEqualTo(StarlarkInt.of(3));
    }
  }

  private static void exec(StarlarkThread thread, Module module, String source)
      throws Exception {
    Starlark.execFile(ParserInput.fromString(source, "fixture.star"),
        FileOptions.DEFAULT.toBuilder().allowToplevelRebinding(true).build(), module, thread);
  }
}
