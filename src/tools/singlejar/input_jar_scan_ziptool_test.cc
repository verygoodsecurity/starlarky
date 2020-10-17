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

/* Verify that InputJar can scan zip/jar files created by JDK's jar tool.  */

#include <stdarg.h>
#include <stdlib.h>

#include "src/tools/singlejar/input_jar_scan_entries_test.h"

/* Creates jar files using zip.  */
class ZiptoolCreator {
 public:
  static void SetUpTestCase() {
  }

  static void TearDownTestCase() {
  }

  static int Jar(bool compress, const char *output_jar, ...) {
    std::string command("zip -qr");
    if (access(output_jar, F_OK) == 0) {
      command = compress ? "u " : "u0 ";
    } else {
      command += compress ? " " : "0 ";
    }
    command += output_jar;
    va_list paths;
    va_start(paths, output_jar);
    char *path;
    while ((path = va_arg(paths, char *))) {
      command += ' ';
      command += path;
    }
    return system(command.c_str());
  }
};

typedef testing::Types<ZiptoolCreator> Creators;
INSTANTIATE_TYPED_TEST_SUITE_P(Jartool, InputJarScanEntries, Creators);
