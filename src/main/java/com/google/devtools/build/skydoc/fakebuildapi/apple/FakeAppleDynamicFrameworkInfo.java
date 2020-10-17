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

package com.google.devtools.build.skydoc.fakebuildapi.apple;

import com.google.devtools.build.lib.collect.nestedset.Depset;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.apple.AppleDynamicFrameworkInfoApi;
import com.google.devtools.build.lib.starlarkbuildapi.apple.ObjcProviderApi;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;

/** Fake implementation of {@link AppleDynamicFrameworkInfoApi}. */
public class FakeAppleDynamicFrameworkInfo implements AppleDynamicFrameworkInfoApi<FileApi> {

  @Override
  public Depset /*<String>*/ getDynamicFrameworkDirs() {
    return null;
  }

  @Override
  public Depset /*<FileApi>*/ getDynamicFrameworkFiles() {
    return null;
  }

  @Override
  public FileApi getAppleDylibBinary() {
    return null;
  }

  @Override
  public ObjcProviderApi<FileApi> getDepsObjcProvider() {
    return new FakeObjcProvider();
  }

  @Override
  public String toProto() throws EvalException {
    return "";
  }

  @Override
  public String toJson() throws EvalException {
    return "";
  }

  @Override
  public void repr(Printer printer) {}
}
