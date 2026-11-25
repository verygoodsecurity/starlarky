// Copyright 2026 Very Good Security Authors
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

package com.verygood.security.larky.modules.multidict;

import java.util.Map;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.Tuple;

/** Binds a call's arguments to named parameters, as a Starlark def with those parameters would. */
final class Args {

  private Args() {}

  /**
   * Returns the arguments for {@code params}, of which the first {@code required} are required; an
   * optional parameter that was not passed is null.
   */
  static Object[] bind(
      String fn, Tuple args, Dict<String, Object> kwargs, int required, String... params)
      throws EvalException {
    if (args.size() > params.length) {
      throw Starlark.errorf(
          "%s() accepts no more than %d positional argument%s but got %d",
          fn, params.length, params.length == 1 ? "" : "s", args.size());
    }
    Object[] bound = new Object[params.length];
    for (int i = 0; i < args.size(); i++) {
      bound[i] = args.get(i);
    }
    for (Map.Entry<String, Object> e : kwargs.entrySet()) {
      int i = indexOf(params, e.getKey());
      if (i < 0) {
        throw Starlark.errorf("%s() got unexpected keyword argument: %s", fn, e.getKey());
      }
      if (bound[i] != null) {
        throw Starlark.errorf("%s() got multiple values for argument '%s'", fn, e.getKey());
      }
      bound[i] = e.getValue();
    }
    for (int i = 0; i < required; i++) {
      if (bound[i] == null) {
        throw Starlark.errorf(
            "%s() missing 1 required positional argument: %s", fn, params[i]);
      }
    }
    return bound;
  }

  private static int indexOf(String[] params, String name) {
    for (int i = 0; i < params.length; i++) {
      if (params[i].equals(name)) {
        return i;
      }
    }
    return -1;
  }
}
