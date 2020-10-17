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

package com.google.devtools.build.skydoc.fakebuildapi.android;

import com.google.devtools.build.lib.collect.nestedset.Depset;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.android.AndroidLibraryAarInfoApi;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;

/** Fake implementation of {@link AndroidLibraryAarInfoApi}. */
public class FakeAndroidLibraryAarInfo implements AndroidLibraryAarInfoApi<FileApi> {

  @Override
  public FileApi getAarArtifact() {
    return null;
  }

  @Override
  public Depset getTransitiveAarArtifactsForStarlark() {
    return null;
  }

  @Override
  public String toProto() throws EvalException {
    return null;
  }

  @Override
  public String toJson() throws EvalException {
    return null;
  }

  /** Fake implementation of {@link AndroidLibraryAarInfoApi.Provider}. */
  public static class FakeProvider implements AndroidLibraryAarInfoApi.Provider<FileApi> {

    @Override
    public AndroidLibraryAarInfoApi<FileApi> create(
        FileApi aar,
        FileApi manifest,
        Sequence<? extends AndroidLibraryAarInfoApi<FileApi>> infosFromDeps,
        Boolean definesLocalResources)
        throws EvalException {
      return new FakeAndroidLibraryAarInfo();
    }
  }
}
