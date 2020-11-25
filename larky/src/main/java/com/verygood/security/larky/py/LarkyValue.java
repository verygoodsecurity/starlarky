// Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.verygood.security.larky.py;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.syntax.Location;

/**
 * An Info is a unit of information produced by analysis of one configured target and consumed by
 * other targets that depend directly upon it. The result of analysis is a dictionary of Info
 * values, each keyed by its LarkyMetaClass. Every Info is an instance of a LarkyMetaClass: if a
 * LarkyMetaClass is like a Java class, then an Info is like an instance of that class.
 */
// TODO(adonovan): simplify the hierarchies below in these steps:
// - Once to_{json,proto} are gone, StructApi can be deleted; structs should never again have
//   methods.
// - StructImpl.location can be pushed down into subclasses that need it, much as we did for
//   StructImpl.provider in CL 341102857.
// - StructImpl is then really just a collection of helper functions for subclasses
//   getValue(String, Class), repr, equals, hash. Move them, and merge it into Info interface,
//   or rename it InfoStruct or StructuredInfo if we absolutely need inheritance.
// - Move StructProvider.STRUCT and make StructProvider private.
//   The StructProvider.createStruct method could be a simple function like depset, select.
//   StructProviderApi could be eliminated.
// - eliminate StarlarkInfo + StarlarkInfo.
// - NativeInfo's get{FieldNames,Value} methods are not needed by the Starlark interpreter,
//   since all its fields are annotated. They exist for the hash/eq/str implementations
//   defined in StructImpl over all its subclasses, and for json.encode. More thought is
//   needed on how to bridge between annotated methods and user-defined Structures so that
//   they appear similar to clients like json.encode.
//
// Info (result of analysis)
// - StructImpl (structure with fields, to_{json,proto}). Implements Structure, StructApi.
//   - OutputGroupInfo. Fields are output group names.
//   - NativeInfo. Fields are Java annotated methods (tricky).
//     - dozens of subclasses
//   - StarlarkInfo. Has table of k/v pairs. Final. Supports x+y.
//
// LarkyMetaClass (key for analysis result Info; class symbol for StructImpls). Implements ProviderApi.
// - BuiltinType
//   - StructProvider (for basic 'struct' values). Callable. Implements ProviderApi.
//   - dozens of singleton subclasses
// - StarlarkProvider. Callable.
//
public interface LarkyValue extends StarlarkValue {

  /**
   * Returns the LarkyType that instantiated this Info.
   */
  LarkyType getType();

  /**
   * Returns the location at which provider was defined.
   */
  Location getLocation();

  /**
   * Is this value already exported?
   */
  boolean isExported();

  /**
   * Notify the value that it is exported from {@code extensionLabel} extension with name {@code
   * exportedName}.
   */
  void export(String extensionLabel, String exportedName) throws EvalException;

  /**
   * Returns a name of this {@link LarkyType} that should be used in error messages.
   */
  String getPrintableName();

  /**
   * Returns the source location where this Info (provider instance) was created, or BUILTIN if it
   * was instantiated by Java code.
   */
  default Location getCreationLocation() {
    return Location.BUILTIN;
  }

  @Override
  default void repr(Printer printer) {
    printer.append("<");
    printer.append(this.getClass().getName());
    printer.append(": ");
    printer.append(getType().getPrintableName());
    printer.append(">");
  }

  /**
   * Returns an error message for instances to use for their {@link net.starlark.java.eval.Structure#getErrorMessageForUnknownField(String)}.
   */
  default String getErrorMessageForUnknownField(String name) {
    return String.format("'%s' value has no field or method '%s'", getPrintableName(), name);
  }

  /**
   * A serializable representation of {@link LarkyType}.
   */
  abstract class Key {
  }

  /**
   * Returns a serializable representation of this {@link LarkyType}.
   */
  Key getKey();
}
