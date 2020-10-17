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
package com.google.devtools.build.lib.actions;

import javax.annotation.Nullable;

/** A context that provides services for actions during execution. */
public interface ActionContext {

  /**
   * Performs any actions conditional on this context not only being registered but triggered as
   * used because its identifier was requested and it was not overridden.
   *
   * @param actionContextRegistry a complete registry containing all used contexts (including this)
   */
  // TODO(schmitt): Remove once contexts are only instantiated if used, the callback can then be
  //  done upon construction.
  default void usedContext(ActionContextRegistry actionContextRegistry) {}

  /**
   * A registry allowing the lookup of action contexts by identifying type during the execution
   * phase.
   */
  interface ActionContextRegistry {

    /**
     * Returns context registered for the given type.
     *
     * <p>Note that multiple contexts could have been registered for the same identifying type. In
     * this case the last such registered context will be returned here. Contexts of the same type
     * can also be distinguished using command-line identifiers and some can be {@linkplain
     * com.google.devtools.build.lib.exec.ExecutorBuilder#addStrategyByContext excluded} from the
     * registry based on those identifiers.
     */
    @Nullable
    <T extends ActionContext> T getContext(Class<T> identifyingType);
  }
}
