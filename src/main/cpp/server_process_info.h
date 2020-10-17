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

#ifndef BAZEL_SRC_MAIN_CPP_SERVER_PROCESS_INFO_H_
#define BAZEL_SRC_MAIN_CPP_SERVER_PROCESS_INFO_H_

#include <sys/types.h>

#include <string>
#include <vector>

#include "src/main/cpp/util/path.h"
#include "src/main/cpp/util/port.h"  // pid_t on Windows/MSVC

namespace blaze {

// Encapsulates information around the blaze server process and its
// configuration.
class ServerProcessInfo final {
 public:
  ServerProcessInfo(const blaze_util::Path &output_base,
                    const blaze_util::Path &server_jvm_out);

  // When running as a daemon, where the deamonized server's stdout and stderr
  // should be written.
  const blaze_util::Path jvm_log_file_;

  // Whether or not the jvm_log_file should be opened with O_APPEND.
  const bool jvm_log_file_append_;

  // TODO(laszlocsomor) 2016-11-28: move pid_t usage out of here and wherever
  // else it appears. Find some way to not have to declare a pid_t here, either
  // by making PID handling platform-independent or some other idea.
  pid_t server_pid_;
};

}  // namespace blaze

#endif  // BAZEL_SRC_MAIN_CPP_SERVER_PROCESS_INFO_H_
