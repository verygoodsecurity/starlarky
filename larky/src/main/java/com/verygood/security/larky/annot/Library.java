package com.verygood.security.larky.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for a class that causes its StarlarkMethod-annotated methods to be predeclared in
 * the environment (by SkylarkParser) and added to the documentation (by MarkdownGenerator).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Library {}

