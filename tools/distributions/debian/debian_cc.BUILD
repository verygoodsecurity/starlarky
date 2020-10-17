# Copyright 2020 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

package(default_visibility = ["//visibility:public"])

# zlib1g-dev
cc_library(
    name = "zlib",
    linkopts = ["-lz"],
    visibility = ["//visibility:public"],
)

# libprotobuf-dev
cc_library(
    name = "protobuf",
    linkopts = ["-lprotobuf"],
    visibility = ["//visibility:public"],
)

# libprotoc-dev
cc_library(
    name = "protoc_lib",
    linkopts = ["-lprotoc", "-lpthread", "-lm"],
    deps = [":protobuf"],
    visibility = ["//visibility:public"],
)

# libgrpc++-dev
cc_library(
    name = "grpc++_unsecure",
    linkopts = ["-lgrpc++_unsecure", "-lgrpc_unsecure", "-lgpr"],
    visibility = ["//visibility:public"],
)
