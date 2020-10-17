// Copyright 2018 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.packages;

import com.google.devtools.build.lib.starlarkbuildapi.core.StructApi;
import java.util.Map;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.Location;

/**
 * The provider for the built-in type {@code struct}.
 *
 * <p>Its singleton instance is {@link StructProvider#STRUCT}.
 */
public final class StructProvider extends BuiltinProvider<StructImpl>
    implements StructApi.StructProviderApi {

  /** "struct" function. */
  public static final StructProvider STRUCT = new StructProvider();

  StructProvider() {
    super("struct", StructImpl.class);
  }

  @Override
  public StructImpl createStruct(Dict<String, Object> kwargs, StarlarkThread thread)
      throws EvalException {
    return create(kwargs, thread.getCallerLocation());
  }

  // Called from StarlarkRepositoryContext. TODO(adonovan): eliminate.
  public StructImpl createWithBuiltinLocation(Dict<String, Object> kwargs) throws EvalException {
    return create(kwargs, Location.BUILTIN);
  }

  private StructImpl create(Dict<String, Object> kwargs, Location location) throws EvalException {
    if (kwargs.containsKey("to_json")) {
      throw Starlark.errorf("cannot override built-in struct function 'to_json'");
    }
    if (kwargs.containsKey("to_proto")) {
      throw Starlark.errorf("cannot override built-in struct function 'to_proto'");
    }
    return StarlarkInfo.create(this, kwargs, location);
  }

  /**
   * Creates a struct with the given field values and message format for unknown fields.
   *
   * <p>The custom message is useful for objects that have fields but aren't exactly used as
   * providers, such as the {@code native} object, and the struct fields of {@code ctx} like {@code
   * ctx.attr}.
   */
  public StarlarkInfo create(Map<String, Object> values, String errorMessageFormatForUnknownField) {
    return StarlarkInfo.createWithCustomMessage(this, values, errorMessageFormatForUnknownField);
  }
}
