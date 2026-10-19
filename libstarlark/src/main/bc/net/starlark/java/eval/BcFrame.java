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

package net.starlark.java.eval;

import java.util.Iterator;
import java.util.Map;
import net.starlark.java.eval.compiler.BytecodeChunk;
import net.starlark.java.syntax.Location;

/**
 * The execution state of one bytecode chunk activation that {@link BcOps} needs: implemented by
 * the interpreting VMs ({@link AbstractBytecodeVM}) and by JVM-compiled code ({@link JvmFrame}).
 */
interface BcFrame {

  StarlarkThread thread();

  BytecodeChunk chunk();

  Map<String, Object> globals();

  String filename();

  /** Cells captured from enclosing functions, indexed by LOAD_FREE/STORE_FREE operands. */
  Tuple freevars();

  /** Iteration locks held by this activation: iterator to the iterable it locks. */
  Map<Iterator<?>, Object> iterators();

  Object getLocal(int index);

  void storeLocal(int index, Object value);

  /** The location of the instruction being executed. */
  Location currentLocation();
}
