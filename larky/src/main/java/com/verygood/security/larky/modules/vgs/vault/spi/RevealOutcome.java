package com.verygood.security.larky.modules.vgs.vault.spi;

import java.util.List;

/**
 * The provider's original return value and the individual values actually found in the vault.
 * A miss carries the provider's usual fallback value and an empty list of hits.
 */
public record RevealOutcome(Object value, List<Object> revealedValues) {
    public RevealOutcome {
        revealedValues = List.copyOf(revealedValues);
    }

    public static RevealOutcome hit(Object value) {
        return new RevealOutcome(value, List.of(value));
    }

    public static RevealOutcome miss(Object fallback) {
        return new RevealOutcome(fallback, List.of());
    }

    @Override
    public String toString() {
        return "RevealOutcome[revealedCount=" + revealedValues.size() + "]";
    }
}
