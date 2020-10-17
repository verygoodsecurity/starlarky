// Copyright 2020 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.analysis.stringtemplate;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

/**
 * Holds the result of expanding a string with make variables: both the new (expanded) string and
 * the set of variables that were expanded.
 */
@AutoValue
public abstract class Expansion {
  public static Expansion create(String expansion, ImmutableSet<String> lookedUpVariables) {
    return new AutoValue_Expansion(expansion, lookedUpVariables);
  }

  public abstract String expansion();

  public abstract ImmutableSet<String> lookedUpVariables();
}
