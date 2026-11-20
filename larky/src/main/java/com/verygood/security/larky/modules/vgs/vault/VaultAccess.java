package com.verygood.security.larky.modules.vgs.vault;

import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import com.verygood.security.larky.modules.vgs.vault.spi.RevealOutcome;
import java.util.List;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;

/** Dispatches vault reveals and notifies the observer for the current execution. */
public final class VaultAccess {
    private VaultAccess() {}

    @FunctionalInterface
    public interface RevealObserver {
        /** Consumes raw revealed values synchronously before the script receives the result. */
        void onRevealed(List<Object> values);
    }

    public static final class Scope implements AutoCloseable {
        @Nullable private Runnable restore;

        private Scope(@Nullable Runnable restore) {
            this.restore = restore;
        }

        @Override
        public void close() {
            Runnable action = restore;
            if (action != null) {
                restore = null;
                action.run();
            }
        }
    }

    /** Installs an observer for the scope and restores the previous observer on close. */
    public static Scope observe(StarlarkThread thread, @Nullable RevealObserver observer) {
        RevealObserver previous = thread.getThreadLocal(RevealObserver.class);
        if (previous == observer) {
            return new Scope(null);
        }
        thread.setThreadLocal(RevealObserver.class, observer);
        return new Scope(() -> thread.setThreadLocal(RevealObserver.class, previous));
    }

    public static Object reveal(LarkyVault provider, Object alias, Object storage, StarlarkThread thread)
        throws EvalException {
        RevealOutcome outcome = provider.revealOutcome(alias, storage);
        RevealObserver observer = thread.getThreadLocal(RevealObserver.class);
        if (observer != null && !outcome.revealedValues().isEmpty()) {
            observer.onRevealed(outcome.revealedValues());
        }
        return outcome.value();
    }
}
