package net.starlark.java.eval;

import java.util.Locale;

/** Behaviour of secret taint for a script run; see {@link SecretTaint}. */
public enum TaintMode {
  /** No tracking: the run behaves as if taint did not exist. */
  OFF,
  /** Tracks secrets and reports output that would be blocked, but leaves output unchanged. */
  OBSERVE,
  /** Tracks secrets and blocks output that carries them. */
  ENFORCE;

  /** The setting name, e.g. {@code -Dlarky.taint.mode=observe}. */
  public static final String PROPERTY = "larky.taint.mode";

  /** Parses a setting value; anything other than {@code observe} or {@code enforce} is off. */
  public static TaintMode parse(String value) {
    if (value == null) {
      return OFF;
    }
    switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "observe":
        return OBSERVE;
      case "enforce":
        return ENFORCE;
      default:
        return OFF;
    }
  }
}
