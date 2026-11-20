// Copyright 2025 The Bazel Authors. All rights reserved.
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

package net.starlark.java.eval.compiler;

import net.starlark.java.syntax.Location;

/**
 * The lexical scope of a comprehension variable, as source positions: what {@code
 * Resolver.ComprehensionBinding.inScope} computes from the syntax tree, kept in a form that needs
 * no syntax tree (and so can be serialized with the chunk).
 *
 * @param firstIterableStart start of the first for clause's iterable, which is outside the scope
 * @param firstIterableEnd end of that iterable
 * @param start start of the comprehension
 * @param end end of the comprehension
 */
public record ComprehensionScope(
    Location firstIterableStart, Location firstIterableEnd, Location start, Location end) {

  /** Returns true if {@code loc} is within the comprehension's scope (see ComprehensionBinding). */
  public boolean inScope(Location loc) {
    if (!loc.file().equals(start.file())) {
      return false;
    }
    // The first for clause's iterable is resolved outside the comprehension, as in Python 3.
    if (loc.compareTo(firstIterableStart) >= 0 && loc.compareTo(firstIterableEnd) < 0) {
      return false;
    }
    return loc.compareTo(start) >= 0 && loc.compareTo(end) < 0;
  }
}
