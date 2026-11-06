package com.verygood.security.larky.modules.vgs.synthetic;

import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkValue;

/**
 * An immutable description of a SYNTHETIC alias: the format and the decorator config that {@code
 * vault.redact(value, alias=spec)} sends to the vault.
 *
 * <p>Only {@link SyntheticModule} creates instances, and it validates every field, so a script
 * cannot forge one with an unchecked shape: {@code vault.redact} accepts this type and nothing
 * else for {@code alias}.
 */
@StarlarkBuiltin(
    name = "alias_spec",
    category = "BUILTIN",
    doc = "An immutable specification of a SYNTHETIC alias, made by @vgs//synthetic.")
public final class AliasSpec implements StarlarkValue {

  /** The alias format every spec carries. */
  public static final String FORMAT = "SYNTHETIC";

  private final ImmutableMap<String, Object> decoratorConfig;

  /**
   * @param decoratorConfig entries in the order a script should see them; values are Strings or
   *     Booleans
   */
  AliasSpec(ImmutableMap<String, Object> decoratorConfig) {
    this.decoratorConfig = decoratorConfig;
  }

  /** The alias format, always {@link #FORMAT}. */
  public String getFormat() {
    return FORMAT;
  }

  /** The decorator config for the vault, as an immutable Java map. */
  public ImmutableMap<String, Object> getDecoratorConfigMap() {
    return decoratorConfig;
  }

  @StarlarkMethod(name = "format", doc = "The alias format: always 'SYNTHETIC'.", structField = true)
  public String format() {
    return FORMAT;
  }

  @StarlarkMethod(
      name = "decorator_config",
      doc = "The decorator config sent to the vault, as an immutable dict.",
      structField = true)
  public Dict<String, Object> decoratorConfig() {
    return Dict.immutableCopyOf(decoratorConfig);
  }

  @StarlarkMethod(name = "kind", doc = "The kind of value to generate.", structField = true)
  public String kind() {
    return (String) decoratorConfig.get("kind");
  }

  @StarlarkMethod(name = "record", doc = "The record the alias belongs to.", structField = true)
  public String record() {
    return (String) decoratorConfig.get("record");
  }

  @StarlarkMethod(name = "country", doc = "The two-letter country code.", structField = true)
  public String country() {
    return (String) decoratorConfig.get("country");
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    printer.append("alias_spec(").append(describe()).append(")");
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof AliasSpec other && decoratorConfig.equals(other.decoratorConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(decoratorConfig);
  }

  @Override
  public String toString() {
    return "AliasSpec" + describe();
  }

  /**
   * The config as text for logs and error messages. A date of birth is personal data about a real
   * person, so it is shown only as {@code <set>}.
   */
  private String describe() {
    Map<String, Object> shown = new LinkedHashMap<>(decoratorConfig);
    shown.computeIfPresent("dob", (k, v) -> new Object() {
      @Override
      public String toString() {
        return "<set>";
      }
    });
    return shown.toString();
  }
}
