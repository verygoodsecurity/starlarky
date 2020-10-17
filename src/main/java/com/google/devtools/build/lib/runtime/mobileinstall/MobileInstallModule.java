// Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.runtime.mobileinstall;

import com.google.devtools.build.lib.runtime.BlazeModule;
import com.google.devtools.build.lib.runtime.ServerBuilder;
import com.google.devtools.common.options.OptionsParsingResult;

/**
 * Module for mobile_install.
 */
public final class MobileInstallModule extends BlazeModule {
  @Override
  public void serverInit(OptionsParsingResult startupOptions, ServerBuilder builder) {
    builder.addCommands(
        new MobileInstallCommand());
  }
}