package com.verygood.security.larky.modules.vgs.vault;

import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import com.verygood.security.larky.modules.vgs.vault.spi.RevealOutcome;
import java.util.List;
import java.util.Objects;
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

    @FunctionalInterface
    public interface OutcomeObserver {
        /** Observes a confirmed outcome and returns its script-visible value. */
        Object onOutcome(StarlarkThread thread, RevealOutcome outcome);
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
    public static Scope observe(StarlarkThread thread, RevealObserver observer) {
        Objects.requireNonNull(observer);
        return observeOutcomes(thread, (execution, outcome) -> {
            observer.onRevealed(outcome.revealedValues());
            return outcome.value();
        });
    }

    /** Installs a run observer that can attach metadata to the script-visible result. */
    public static Scope observeOutcomes(StarlarkThread thread, OutcomeObserver observer) {
        return bindObserver(thread, Objects.requireNonNull(observer));
    }

    /** Suspends observation for the scope and restores the previous observer on close. */
    public static Scope suspendObservation(StarlarkThread thread) {
        return bindObserver(thread, null);
    }

    private static Scope bindObserver(StarlarkThread thread, @Nullable OutcomeObserver observer) {
        OutcomeObserver previous = thread.getThreadLocal(OutcomeObserver.class);
        if (previous == observer) {
            return new Scope(null);
        }
        thread.setThreadLocal(OutcomeObserver.class, observer);
        return new Scope(() -> thread.setThreadLocal(OutcomeObserver.class, previous));
    }

    public static Object reveal(LarkyVault provider, Object alias, Object storage, StarlarkThread thread)
        throws EvalException {
        RevealOutcome outcome = provider.revealOutcome(alias, storage);
        OutcomeObserver observer = thread.getThreadLocal(OutcomeObserver.class);
        return observer == null || outcome.revealedValues().isEmpty()
            ? outcome.value()
            : observer.onOutcome(thread, outcome);
    }
}
