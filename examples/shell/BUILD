package(default_visibility = ["//visibility:public"])

sh_binary(
    name = "bin",
    srcs = ["bin.sh"],
    deps = [
        ":lib",
        "@bazel_tools//tools/bash/runfiles",
    ],
)

sh_library(
    name = "lib",
    data = [
        "data/file.txt",
        "lib.sh",
    ],
    deps = ["@bazel_tools//tools/bash/runfiles"],
)

sh_test(
    name = "test",
    size = "small",
    srcs = ["test.sh"],
    data = ["data/test_file.txt"],
    deps = [
        ":lib",
        "@bazel_tools//tools/bash/runfiles",
    ],
)

filegroup(
    name = "srcs",
    srcs = glob(["**"]),
)
