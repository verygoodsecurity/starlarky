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

package com.google.devtools.build.lib.rules.objc;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.packages.NativeInfo;
import com.google.devtools.build.lib.packages.NativeProvider;
import com.google.devtools.build.lib.starlarkbuildapi.apple.AppleDylibBinaryApi;

/**
 * Provider containing the executable binary output that was built using an apple_binary target with
 * the 'dylib' type. This provider contains:
 *
 * <ul>
 *   <li>'binary': The dylib artifact output by apple_binary
 *   <li>'objc': An {@link ObjcProvider} which contains information about the transitive
 *       dependencies linked into the dylib, (intended so that binaries depending on this dylib may
 *       avoid relinking symbols included in the dylib
 * </ul>
 */
@Immutable
public final class AppleDylibBinaryInfo extends NativeInfo implements AppleDylibBinaryApi {

  /** Starlark name for the AppleDylibBinaryInfo. */
  public static final String STARLARK_NAME = "AppleDylibBinary";

  /** Starlark constructor and identifier for AppleDylibBinaryInfo. */
  public static final NativeProvider<AppleDylibBinaryInfo> STARLARK_CONSTRUCTOR =
      new NativeProvider<AppleDylibBinaryInfo>(AppleDylibBinaryInfo.class, STARLARK_NAME) {};

  private final Artifact dylibBinary;
  private final ObjcProvider depsObjcProvider;

  public AppleDylibBinaryInfo(Artifact dylibBinary,
      ObjcProvider depsObjcProvider) {
    super(STARLARK_CONSTRUCTOR);
    this.dylibBinary = dylibBinary;
    this.depsObjcProvider = depsObjcProvider;
  }

  /**
   * Returns the multi-architecture dylib binary that apple_binary created.
   */
  @Override
  public Artifact getAppleDylibBinary() {
    return dylibBinary;
  }

  /**
   * Returns the {@link ObjcProvider} which contains information about the transitive dependencies
   * linked into the dylib.
   */
  @Override
  public ObjcProvider getDepsObjcProvider() {
    return depsObjcProvider;
  }
}
