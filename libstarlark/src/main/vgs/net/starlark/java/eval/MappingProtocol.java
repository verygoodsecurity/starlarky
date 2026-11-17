// Copyright 2026 Very Good Security Authors. All rights reserved.
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

package net.starlark.java.eval;

import java.util.List;
import java.util.Map;

/**
 * Python's mapping protocol for {@code dict(m)} and {@code d.update(m)}: any object with a callable
 * {@code keys} attribute is copied as {@code for k in m.keys(): d[k] = m[k]}.
 */
public final class MappingProtocol {
  private MappingProtocol() {}

  /**
   * If {@code source} has a callable {@code keys} attribute, copies its entries into {@code dict}
   * and returns true. Otherwise returns false and leaves {@code dict} untouched.
   *
   * <p>Real dicts are not handled here (callers copy them directly). Values are read with the
   * same operation as {@code source[k]}, so a repeated key yields whatever {@code __getitem__}
   * returns for it.
   */
  public static boolean copyInto(
      Dict<Object, Object> dict, Object source, StarlarkThread thread) throws EvalException {
    if (source instanceof Map || thread == null) {
      return false;
    }
    Object keysFn;
    try {
      keysFn = Starlark.getattr(thread, source, "keys", Starlark.NONE);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw Starlark.errorf("interrupted");
    }
    if (!(keysFn instanceof StarlarkCallable)) {
      return false;
    }
    Object keys;
    try {
      keys = Starlark.call(thread, keysFn, List.of(), Map.of());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw Starlark.errorf("interrupted");
    }
    // Materialize the keys first, so that the source may be mutated while it is copied.
    Object[] keyArray = Starlark.toArray(keys);
    for (Object key : keyArray) {
      Object value = EvalUtils.index(thread, source, key);
      if (value == null) {
        throw Starlark.errorf("key %s not found in %s", Starlark.repr(key, thread.getSemantics()), Starlark.type(source));
      }
      dict.putEntry(key, value);
    }
    return true;
  }
}
