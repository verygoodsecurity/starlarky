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
package com.verygood.security.larky.modules.attrs;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkValue;

/** attrs' {@code NOTHING}: marks the lack of a value where {@code None} is ambiguous. Falsy. */
@StarlarkBuiltin(name = "_Nothing", documented = false)
public final class Nothing implements StarlarkValue {
  public static final Nothing NOTHING = new Nothing();

  private Nothing() {}

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    printer.append("NOTHING");
  }

  @Override
  public boolean truth() {
    return false;
  }

  @Override
  public boolean isImmutable() {
    return true;
  }
}
