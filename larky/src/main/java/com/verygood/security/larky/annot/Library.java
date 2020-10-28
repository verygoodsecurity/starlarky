package com.verygood.security.larky.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for a class that causes its StarlarkMethod-annotated methods to be predeclared in
 * the environment (by SkylarkParser) and added to the documentation (by MarkdownGenerator).
 *
 * See usage of <a href="https://github.com/google/copybara/blob/6b0c2867ad86059be4564611202f46fef9c8065e/java/com/google/copybara/CoreGlobal.java#L38>the Library annotation here</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Library {}

