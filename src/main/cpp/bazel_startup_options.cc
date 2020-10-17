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
#include "src/main/cpp/bazel_startup_options.h"

#include <cassert>

#include "src/main/cpp/blaze_util.h"
#include "src/main/cpp/util/logging.h"
#include "src/main/cpp/workspace_layout.h"

namespace blaze {

BazelStartupOptions::BazelStartupOptions(
    const WorkspaceLayout *workspace_layout)
    : StartupOptions("Bazel", workspace_layout),
      user_bazelrc_(""),
      use_system_rc(true),
      use_workspace_rc(true),
      use_home_rc(true),
      use_master_bazelrc_(true) {
  RegisterNullaryStartupFlagNoRc("home_rc", &use_home_rc);
  RegisterNullaryStartupFlagNoRc("master_bazelrc", &use_master_bazelrc_);
  OverrideOptionSourcesKey("master_bazelrc", "blazerc");
  RegisterNullaryStartupFlagNoRc("system_rc", &use_system_rc);
  RegisterNullaryStartupFlagNoRc("workspace_rc", &use_workspace_rc);
  RegisterUnaryStartupFlag("bazelrc");
}

blaze_exit_code::ExitCode BazelStartupOptions::ProcessArgExtra(
    const char *arg, const char *next_arg, const std::string &rcfile,
    const char **value, bool *is_processed, std::string *error) {
  assert(value);
  assert(is_processed);

  if ((*value = GetUnaryOption(arg, next_arg, "--bazelrc")) != nullptr) {
    if (!rcfile.empty()) {
      *error = "Can't specify --bazelrc in the RC file.";
      return blaze_exit_code::BAD_ARGV;
    }
    user_bazelrc_ = *value;
  } else {
    *is_processed = false;
    return blaze_exit_code::SUCCESS;
  }

  *is_processed = true;
  return blaze_exit_code::SUCCESS;
}

void BazelStartupOptions::MaybeLogStartupOptionWarnings() const {
  if (ignore_all_rc_files) {
    if (!user_bazelrc_.empty()) {
      BAZEL_LOG(WARNING) << "Value of --bazelrc is ignored, since "
                            "--ignore_all_rc_files is on.";
    }
    if ((use_home_rc) &&
        option_sources.find("home_rc") != option_sources.end()) {
      BAZEL_LOG(WARNING) << "Explicit value of --home_rc is "
                            "ignored, since --ignore_all_rc_files is on.";
    }
    if ((use_system_rc) &&
        option_sources.find("system_rc") != option_sources.end()) {
      BAZEL_LOG(WARNING) << "Explicit value of --system_rc is "
                            "ignored, since --ignore_all_rc_files is on.";
    }
    if ((use_workspace_rc) &&
        option_sources.find("workspace_rc") != option_sources.end()) {
      BAZEL_LOG(WARNING) << "Explicit value of --workspace_rc is "
                            "ignored, since --ignore_all_rc_files is on.";
    }
  }
  bool output_user_root_has_space =
      output_user_root.find_first_of(' ') != std::string::npos;
  if (output_user_root_has_space) {
    BAZEL_LOG(WARNING)
        << "Output user root \"" << output_user_root
        << "\" contains a space. This will probably break the build. "
           "You should set a different --output_user_root.";
  } else if (output_base.Contains(' ')) {
    // output_base is computed from output_user_root by default.
    // If output_user_root was bad, don't check output_base: while output_base
    // may also be bad, we already warned about output_user_root so there's no
    // point in another warning.
    BAZEL_LOG(WARNING)
        << "Output base \"" << output_base.AsPrintablePath()
        << "\" contains a space. This will probably break the build. "
           "You should not set --output_base and let Bazel use the default, or "
           "set --output_base to a path without space.";
  }
}

void BazelStartupOptions::AddExtraOptions(
    std::vector<std::string> *result) const {
  StartupOptions::AddExtraOptions(result);
}

}  // namespace blaze
