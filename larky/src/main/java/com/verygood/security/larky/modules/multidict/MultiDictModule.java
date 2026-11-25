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

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

/** The native side of {@code @vendor//multidict}, which is its only intended user. */
@StarlarkBuiltin(name = "jmultidict", documented = false)
public final class MultiDictModule implements StarlarkValue {

  public static final MultiDictModule INSTANCE = new MultiDictModule();

  private MultiDictModule() {}

  @StarlarkMethod(
      name = "new_multidict",
      documented = false,
      parameters = {
        @Param(name = "cls"),
        @Param(name = "name"),
        @Param(name = "case_insensitive"),
        @Param(name = "args"),
        @Param(name = "kwargs"),
      },
      useStarlarkThread = true)
  public LarkyMultiDict newMultiDict(
      Object cls,
      String name,
      Boolean caseInsensitive,
      Tuple args,
      Dict<?, ?> kwargs,
      StarlarkThread thread)
      throws EvalException, InterruptedException {
    return LarkyMultiDict.create(
        cls, name, caseInsensitive, args, Dict.cast(kwargs, String.class, Object.class, "kwargs"),
        thread);
  }

  @StarlarkMethod(
      name = "new_proxy",
      documented = false,
      parameters = {
        @Param(name = "cls"),
        @Param(name = "name"),
        @Param(name = "case_insensitive"),
        @Param(name = "copy_class"),
        @Param(name = "md"),
      })
  public LarkyMultiDict newProxy(
      Object cls, String name, Boolean caseInsensitive, Object copyClass, Object md)
      throws EvalException {
    return LarkyMultiDict.proxy(cls, name, caseInsensitive, copyClass, md);
  }

  @StarlarkMethod(name = "getversion", documented = false, parameters = {@Param(name = "md")})
  public StarlarkInt getversion(Object md) throws EvalException {
    if (md instanceof LarkyMultiDict multidict) {
      return StarlarkInt.of(multidict.version());
    }
    throw Starlark.errorf("getversion() expects a multidict, got %s", Starlark.type(md));
  }
}
