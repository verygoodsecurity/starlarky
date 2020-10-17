# Bazel - Google's Build System

load("//tools/distributions:distribution_rules.bzl", "distrib_jar_filegroup")
load("//tools/python:private/defs.bzl", "py_binary")
load("@rules_pkg//:pkg.bzl", "pkg_tar")

package(default_visibility = ["//scripts/release:__pkg__"])

exports_files(["LICENSE"])

filegroup(
    name = "srcs",
    srcs = glob(
        ["*"],
        exclude = [
            "WORKSPACE",  # Needs to be filtered.
            "bazel-*",  # convenience symlinks
            "out",  # IntelliJ with setup-intellij.sh
            "output",  # output of compile.sh
            ".*",  # mainly .git* files
        ],
    ) + [
        "//:WORKSPACE.filtered",
        "//examples:srcs",
        "//scripts:srcs",
        "//site:srcs",
        "//src:srcs",
        "//tools:srcs",
        "//third_party:srcs",
    ] + glob([".bazelci/*"]) + [".bazelrc"],
    visibility = ["//src/test/shell/bazel:__pkg__"],
)

filegroup(
    name = "git",
    srcs = glob(
        [".git/**"],
        exclude = [".git/**/*[*"],  # gitk creates temp files with []
    ),
)

filegroup(
    name = "dummy",
    visibility = ["//visibility:public"],
)

filegroup(
    name = "workspace-file",
    srcs = [
        ":WORKSPACE",
        ":distdir.bzl",
    ],
    visibility = [
        "//src/test/shell/bazel:__subpackages__",
    ],
)

filegroup(
    name = "changelog-file",
    srcs = [":CHANGELOG.md"],
    visibility = [
        "//scripts/packages:__subpackages__",
    ],
)

genrule(
    name = "filtered_WORKSPACE",
    srcs = ["WORKSPACE"],
    outs = ["WORKSPACE.filtered"],
    cmd = "\n".join([
        "cp $< $@",
        # Comment out the android repos if they exist.
        "sed -i.bak -e 's/^android_sdk_repository/# android_sdk_repository/' -e 's/^android_ndk_repository/# android_ndk_repository/' $@",
    ]),
)

pkg_tar(
    name = "bootstrap-jars",
    srcs = [
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
        "@com_google_protobuf//:protobuf_javalite",
    ],
    remap_paths = {
        "..": "derived/jars",
    },
    strip_prefix = ".",
    # Public but bazel-only visibility.
    visibility = ["//:__subpackages__"],
)

distrib_jar_filegroup(
    name = "bootstrap-derived-java-jars",
    srcs = glob(
        ["derived/jars/**/*.jar"],
        allow_empty = True,
    ),
    enable_distributions = ["debian"],
    visibility = ["//:__subpackages__"],
)

filegroup(
    name = "bootstrap-derived-java-srcs",
    srcs = glob(
        ["derived/**/*.java"],
        allow_empty = True,
    ),
    visibility = ["//:__subpackages__"],
)

pkg_tar(
    name = "bazel-srcs",
    srcs = [":srcs"],
    remap_paths = {
        "WORKSPACE.filtered": "WORKSPACE",
        # Rewrite paths coming from local repositories back into third_party.
        "../googleapis": "third_party/googleapis",
        "../remoteapis": "third_party/remoteapis",
    },
    strip_prefix = ".",
    # Public but bazel-only visibility.
    visibility = ["//:__subpackages__"],
)

pkg_tar(
    name = "platforms-srcs",
    srcs = ["@platforms//:srcs"],
    package_dir = "platforms",
    strip_prefix = ".",
    visibility = ["//:__subpackages__"],
)

py_binary(
    name = "combine_distfiles",
    srcs = ["combine_distfiles.py"],
    visibility = ["//visibility:private"],
    deps = ["//src:create_embedded_tools_lib"],
)

genrule(
    name = "bazel-distfile",
    srcs = [
        ":bazel-srcs",
        ":bootstrap-jars",
        ":platforms-srcs",
        "//src:derived_java_srcs",
        "//src/main/java/com/google/devtools/build/lib/skyframe/serialization/autocodec:bootstrap_autocodec.tar",
        "@additional_distfiles//:archives.tar",
    ],
    outs = ["bazel-distfile.zip"],
    cmd = "$(location :combine_distfiles) $@ $(SRCS)",
    tools = [":combine_distfiles"],
    # Public but bazel-only visibility.
    visibility = ["//:__subpackages__"],
)

genrule(
    name = "bazel-distfile-tar",
    srcs = [
        ":bazel-srcs",
        ":bootstrap-jars",
        ":platforms-srcs",
        "//src:derived_java_srcs",
        "//src/main/java/com/google/devtools/build/lib/skyframe/serialization/autocodec:bootstrap_autocodec.tar",
        "@additional_distfiles//:archives.tar",
    ],
    outs = ["bazel-distfile.tar"],
    cmd = "$(location :combine_distfiles_to_tar.sh) $@ $(SRCS)",
    tools = ["combine_distfiles_to_tar.sh"],
    # Public but bazel-only visibility.
    visibility = ["//:__subpackages__"],
)

# This is a workaround for fetching Bazel toolchains, for remote execution.
# See https://github.com/bazelbuild/bazel/issues/3246.
# Will be removed once toolchain fetching is supported.
filegroup(
    name = "dummy_toolchain_reference",
    srcs = ["@bazel_toolchains//configs/debian8_clang/0.2.0/bazel_0.9.0:empty"],
    visibility = ["//visibility:public"],
)

constraint_setting(name = "machine_size")

# A machine with "high cpu count".
constraint_value(
    name = "highcpu_machine",
    constraint_setting = ":machine_size",
    visibility = ["//visibility:public"],
)

platform(
    name = "default_host_platform",
    constraint_values = [
        ":highcpu_machine",
    ],
    parents = ["@local_config_platform//:host"],
)

REMOTE_PLATFORMS = ("rbe_ubuntu1604_java8", "rbe_ubuntu1804_java11")

[
    platform(
        name = platform_name + "_platform",
        parents = ["@" + platform_name + "//config:platform"],
        remote_execution_properties = """
            {PARENT_REMOTE_EXECUTION_PROPERTIES}
            properties: {
                name: "dockerNetwork"
                value: "standard"
            }
            properties: {
                name: "dockerPrivileged"
                value: "true"
            }
            """,
    )
    for platform_name in REMOTE_PLATFORMS
]

[
    # The highcpu RBE platform where heavy actions run on. In order to
    # use this platform add the highcpu_machine constraint to your target.
    platform(
        name = platform_name + "_highcpu_platform",
        constraint_values = [
            "//:highcpu_machine",
        ],
        parents = ["//:" + platform_name + "_platform"],
        remote_execution_properties = """
            {PARENT_REMOTE_EXECUTION_PROPERTIES}
            properties: {
                name: "gceMachineType"
                value: "n1-highcpu-32"
            }
            """,
    )
    for platform_name in REMOTE_PLATFORMS
]
