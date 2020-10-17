# How to update the C++ sources of gRPC:

1. Update the gRPC definitions in WORKSPACE file, currently we use 
   https://github.com/grpc/grpc/archive/v1.32.2.tar.gz
2. Update the gRPC patch file if necessary, it mostly helps avoid unnecessary dependencies.
3. Update third_party/grpc/BUILD to redirect targets to @com_github_grpc_grpc if necessary.

# How to update the BUILD/bzl sources of gRPC:

1. `git clone http://github.com/grpc/grpc.git` in a convenient directory
2. `git checkout <tag>` (current is `v1.32.0`, commithash `414bb8322d`)
3. `mkdir -p third_party/grpc/bazel`
4. `cp <gRPC git tree>/bazel/{BUILD,cc_grpc_library.bzl,generate_cc.bzl,protobuf.bzl} third_party/grpc/bazel`
5. In the `third_party/grpc` directory, apply local patches:
   `patch -p3 < bazel_1.32.0.patch`

# How to update the Java plugin:

1. Checkout tag `v1.32.2` from https://github.com/grpc/grpc-java
2. `cp -R <grpc-java git tree>/compiler/src/java_plugin third_party/grpc/compiler/src`

# How to update the Java code:

Download the necessary jars at version `1.32.2` from maven central.

# Submitting the change needs 3 pull requests

1. Update third_party/grpc to include files from new version
2. Switch WORKSPACE, scripts/bootstrap/compile.sh and any other references to new version
3. Remove older version from third_party/grpc
