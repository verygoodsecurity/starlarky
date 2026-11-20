package net.starlark.java.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link StarlarkMethod} whose return value is a secret: the interpreter tags it, and
 * everything derived from it, for the duration of the run (see {@code SecretTaint}).
 *
 * <p>Read once, when the method's descriptor is built. The annotation may sit on the implementing
 * method or anywhere it is declared in the class hierarchy.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SecretSource {
  /**
   * Zero-based parameter whose unchanged String identities are aliases passed through by the
   * source, including corresponding elements in a batch. A negative value tracks every result.
   */
  int passthroughParameter() default -1;
}
