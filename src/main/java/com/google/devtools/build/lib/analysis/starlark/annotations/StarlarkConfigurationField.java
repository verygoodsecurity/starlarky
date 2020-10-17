// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.analysis.starlark.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker interface for Java methods of Starlark-exposed configuration fragments which denote
 * Starlark "configuration fields": late-bound attribute defaults that depend on configuration.
 *
 * <p>Methods annotated with this annotation have a few constraints:
 *
 * <ul>
 *   <li>The annotated method must be on a configuration fragment exposed to Starlark.
 *   <li>The method must have return type Label.
 *   <li>The method must be public.
 *   <li>The method must have zero arguments.
 *   <li>The method must not throw exceptions.
 * </ul>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StarlarkConfigurationField {

  /** Name of the configuration field, as exposed to Starlark. */
  String name();

  /**
   * The default label associated with this field, corresponding to the value of this configuration
   * field with default command line flags.
   *
   * <p>If the default label is under the tools repository, omit the tools repository prefix
   * from this default, but set {@link #defaultInToolRepository} to true.</p>
   */
  String defaultLabel() default "";

  /**
   * Whether the default label as defined in {@link #defaultLabel} should be prefixed with
   * the tools repository.
   */
  boolean defaultInToolRepository() default false;

  /**
   * The documentation text in Starlark. It can contain HTML tags for special formatting.
   *
   * <p>It is allowed to be empty only if {@link #documented()} is false.
   */
  String doc() default "";

  /**
   * If true, the function will appear in the Starlark documentation. Set this to false if the
   * function is experimental or an overloading and doesn't need to be documented.
   */
  boolean documented() default true;
}
