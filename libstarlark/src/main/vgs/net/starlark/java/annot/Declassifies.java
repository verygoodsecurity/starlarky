package net.starlark.java.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link StarlarkMethod} whose result is safe to emit even when an argument is secret, such
 * as {@code vault.redact}, whose result is an alias. Taint does not propagate through it.
 *
 * <p>Read once, when the method's descriptor is built. Same lookup as {@link SecretSource}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Declassifies {}
