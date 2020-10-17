// Copyright 2019 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.bazel.commands;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionDocumentationCategory;
import com.google.devtools.common.options.OptionEffectTag;
import com.google.devtools.common.options.OptionsBase;
import java.util.List;

/** Defines the options specific to Bazel's sync command */
public class SyncOptions extends OptionsBase {
  @Option(
      name = "configure",
      defaultValue = "False",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.CHANGES_INPUTS},
      help = "Only sync repositories marked as 'configure' for system-configuration purpose.")
  public boolean configure;

  @Option(
      name = "only",
      defaultValue = "null",
      allowMultiple = true,
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.CHANGES_INPUTS},
      help =
          "If this option is given, only sync the repositories specified with this option."
              + " Still consider all (or all configure-like, of --configure is given) outdated.")
  public List<String> only;
}
