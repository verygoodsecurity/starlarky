package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import com.verygood.security.larky.modules.vgs.vault.spi.RevealOutcome;
import java.util.ArrayList;
import java.util.List;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import org.junit.jupiter.api.Test;

class VaultAccessTest {
  @Test
  void observesHitsSynchronouslyAndRestoresTheOuterObserverOnFailure() throws Exception {
    LarkyVault vault = mock(LarkyVault.class);
    List<Object> values = List.of("missing alias", "fixture value");
    when(vault.revealOutcome(eq("batch"), any()))
        .thenReturn(new RevealOutcome(values, List.of("fixture value")));
    when(vault.revealOutcome(eq("miss"), any())).thenReturn(RevealOutcome.miss("miss"));
    when(vault.revealOutcome(eq("error"), any())).thenThrow(new EvalException("provider failure"));
    List<List<Object>> outer = new ArrayList<>();
    List<List<Object>> inner = new ArrayList<>();

    try (Mutability mutability = Mutability.create("vault observer test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mutability, StarlarkSemantics.DEFAULT);
      VaultAccess.Scope innerScope;
      try (var outerScope = VaultAccess.observe(thread, outer::add)) {
        assertSame(values, VaultAccess.reveal(vault, "batch", Starlark.NONE, thread));
        assertEquals(List.of(List.of("fixture value")), outer);
        assertEquals("miss", VaultAccess.reveal(vault, "miss", Starlark.NONE, thread));

        try (var suspended = VaultAccess.suspendObservation(thread)) {
          assertSame(values, VaultAccess.reveal(vault, "batch", Starlark.NONE, thread));
        }
        assertEquals(List.of(List.of("fixture value")), outer);

        innerScope = VaultAccess.observe(thread, inner::add);
        assertThrows(EvalException.class, () -> {
          try (innerScope) {
            VaultAccess.reveal(vault, "error", Starlark.NONE, thread);
          }
        });
        assertEquals(List.of(), inner);
        assertSame(values, VaultAccess.reveal(vault, "batch", Starlark.NONE, thread));
      }
      innerScope.close();
      assertSame(values, VaultAccess.reveal(vault, "batch", Starlark.NONE, thread));
    }

    assertEquals(List.of(List.of("fixture value"), List.of("fixture value")), outer);
  }
}
