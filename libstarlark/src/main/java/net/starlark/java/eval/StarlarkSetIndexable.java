// Copyright 2015 The Bazel Authors. All rights reserved.
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

/**
 * A Starlark value that support indexed access ({@code object[key]}), membership tests ({@code
 * key in object}) and indexed assignment ({@code object[key] = value}).
 */
public interface StarlarkSetIndexable extends StarlarkIndexable {

  /**
   * Updates an object as if by the Starlark statement {@code object[key] = value}.
   *
   * @throws EvalException if underlying object is immutable.
   */
  void setIndex(StarlarkSemantics semantics, Object key, Object value) throws EvalException;

  /**
   * A variant of {@link StarlarkSetIndexable} that also provides a StarlarkThread instance on method
   * calls.
   */
  // TODO(mahmoudimus): Similar to brandjon's comment on StarlarkIndexable, consider
  //  replacing this subinterface by changing StarlarkSetIndexable's methods'
  // signatures to take StarlarkThread in place of StarlarkSemantics.
  interface Threaded extends StarlarkIndexable.Threaded {
    /** {@see StarlarkSetIndexable.setIndex} */
    void setIndex(StarlarkThread starlarkThread, StarlarkSemantics semantics, Object key, Object value) throws EvalException;
  }

}
