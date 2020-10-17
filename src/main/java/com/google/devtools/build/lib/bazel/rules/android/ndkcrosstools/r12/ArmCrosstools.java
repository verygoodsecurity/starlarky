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

package com.google.devtools.build.lib.bazel.rules.android.ndkcrosstools.r12;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.bazel.rules.android.ndkcrosstools.NdkPaths;
import com.google.devtools.build.lib.bazel.rules.android.ndkcrosstools.StlImpl;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig.CToolchain;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig.CompilationMode;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig.CompilationModeFlags;
import java.util.List;

/**
 * Crosstool definitions for ARM. These values are based on the setup.mk files in the Android NDK
 * toolchain directories.
 */
class ArmCrosstools {

  private final NdkPaths ndkPaths;
  private final StlImpl stlImpl;

  ArmCrosstools(NdkPaths ndkPaths, StlImpl stlImpl) {
    this.ndkPaths = ndkPaths;
    this.stlImpl = stlImpl;
  }

  ImmutableList<CToolchain.Builder> createCrosstools() {
    ImmutableList.Builder<CToolchain.Builder> toolchains = ImmutableList.builder();

    toolchains.add(createAarch64Toolchain());
    toolchains.add(createAarch64ClangToolchain());

    toolchains.addAll(createArmeabiToolchains());
    toolchains.addAll(createArmeabiClangToolchain());

    return toolchains.build();
  }

  private CToolchain.Builder createAarch64Toolchain() {
    String toolchainName = "aarch64-linux-android-4.9";
    String targetPlatform = "aarch64-linux-android";

    CToolchain.Builder toolchain =
        CToolchain.newBuilder()
            .setToolchainIdentifier("aarch64-linux-android-4.9")
            .setTargetSystemName("aarch64-linux-android")
            .setTargetCpu("arm64-v8a")
            .setCompiler("gcc-4.9")
            .addAllToolPath(ndkPaths.createToolpaths(toolchainName, targetPlatform))
            .addAllCxxBuiltinIncludeDirectory(
                ndkPaths.createGccToolchainBuiltinIncludeDirectories(
                    toolchainName, targetPlatform, "4.9.x"))
            .setBuiltinSysroot(ndkPaths.createBuiltinSysroot("arm64"))

            // Compiler flags
            .addCompilerFlag("-fpic")
            .addCompilerFlag("-ffunction-sections")
            .addCompilerFlag("-funwind-tables")
            .addCompilerFlag("-fstack-protector-strong")
            .addCompilerFlag("-no-canonical-prefixes")
            .addCompilerFlag("-fno-canonical-system-headers")

            // Linker flags
            .addLinkerFlag("-no-canonical-prefixes")

            // Additional release flags
            .addCompilationModeFlags(
                CompilationModeFlags.newBuilder()
                    .setMode(CompilationMode.OPT)
                    .addCompilerFlag("-O2")
                    .addCompilerFlag("-g")
                    .addCompilerFlag("-DNDEBUG"))

            // Additional debug flags
            .addCompilationModeFlags(
                CompilationModeFlags.newBuilder()
                    .setMode(CompilationMode.DBG)
                    .addCompilerFlag("-O0")
                    .addCompilerFlag("-g")
                    .addCompilerFlag("-UNDEBUG"));

    stlImpl.addStlImpl(toolchain, "4.9");
    return toolchain;
  }

  private CToolchain.Builder createAarch64ClangToolchain() {
    String toolchainName = "aarch64-linux-android-4.9";
    String targetPlatform = "aarch64-linux-android";
    String gccToolchain = ndkPaths.createGccToolchainPath(toolchainName);
    String llvmTriple = "aarch64-none-linux-android";

    CToolchain.Builder toolchain =
        CToolchain.newBuilder()
            .setToolchainIdentifier("aarch64-linux-android-clang3.8")
            .setTargetSystemName("aarch64-linux-android")
            .setTargetCpu("arm64-v8a")
            .setCompiler("clang3.8")
            .addAllToolPath(ndkPaths.createClangToolpaths(toolchainName, targetPlatform, null))
            .setBuiltinSysroot(ndkPaths.createBuiltinSysroot("arm64"))

            // Compiler flags
            .addCompilerFlag("-gcc-toolchain")
            .addCompilerFlag(gccToolchain)
            .addCompilerFlag("-target")
            .addCompilerFlag(llvmTriple)
            .addCompilerFlag("-ffunction-sections")
            .addCompilerFlag("-funwind-tables")
            .addCompilerFlag("-fstack-protector-strong")
            .addCompilerFlag("-fpic")
            .addCompilerFlag("-Wno-invalid-command-line-argument")
            .addCompilerFlag("-Wno-unused-command-line-argument")
            .addCompilerFlag("-no-canonical-prefixes")

            // Linker flags
            .addLinkerFlag("-gcc-toolchain")
            .addLinkerFlag(gccToolchain)
            .addLinkerFlag("-target")
            .addLinkerFlag(llvmTriple)
            .addLinkerFlag("-no-canonical-prefixes")

            // Additional release flags
            .addCompilationModeFlags(
                CompilationModeFlags.newBuilder()
                    .setMode(CompilationMode.OPT)
                    .addCompilerFlag("-O2")
                    .addCompilerFlag("-g")
                    .addCompilerFlag("-DNDEBUG"))

            // Additional debug flags
            .addCompilationModeFlags(
                CompilationModeFlags.newBuilder()
                    .setMode(CompilationMode.DBG)
                    .addCompilerFlag("-O0")
                    .addCompilerFlag("-UNDEBUG"));

    stlImpl.addStlImpl(toolchain, "4.9");
    return toolchain;
  }

  private List<CToolchain.Builder> createArmeabiToolchains() {
    ImmutableList<CToolchain.Builder> toolchains =
        ImmutableList.of(
            createBaseArmeabiToolchain()
                .setToolchainIdentifier("arm-linux-androideabi-4.9")
                .setTargetCpu("armeabi")
                .addCompilerFlag("-march=armv5te")
                .addCompilerFlag("-mtune=xscale")
                .addCompilerFlag("-msoft-float"),
            createBaseArmeabiToolchain()
                .setToolchainIdentifier("arm-linux-androideabi-4.9-v7a")
                .setTargetCpu("armeabi-v7a")
                .addCompilerFlag("-march=armv7-a")
                .addCompilerFlag("-mfpu=vfpv3-d16")
                .addCompilerFlag("-mfloat-abi=softfp")
                .addLinkerFlag("-march=armv7-a")
                .addLinkerFlag("-Wl,--fix-cortex-a8"));
    stlImpl.addStlImpl(toolchains, "4.9");
    return toolchains;
  }

  /** Flags common to arm-linux-androideabi* */
  private CToolchain.Builder createBaseArmeabiToolchain() {
    String toolchainName = "arm-linux-androideabi-4.9";
    String targetPlatform = "arm-linux-androideabi";

    CToolchain.Builder toolchain =
        CToolchain.newBuilder()
            .setTargetSystemName(targetPlatform)
            .setCompiler("gcc-4.9")
            .addAllToolPath(ndkPaths.createToolpaths(toolchainName, targetPlatform))
            .addAllCxxBuiltinIncludeDirectory(
                ndkPaths.createGccToolchainBuiltinIncludeDirectories(
                    toolchainName, targetPlatform, "4.9.x"))
            .setBuiltinSysroot(ndkPaths.createBuiltinSysroot("arm"))

            // Compiler flags
            .addCompilerFlag("-fstack-protector-strong")
            .addCompilerFlag("-fpic")
            .addCompilerFlag("-ffunction-sections")
            .addCompilerFlag("-funwind-tables")
            .addCompilerFlag("-no-canonical-prefixes")
            .addCompilerFlag("-fno-canonical-system-headers")

            // Linker flags
            .addLinkerFlag("-no-canonical-prefixes");

    toolchain.addCompilationModeFlags(
        CompilationModeFlags.newBuilder()
            .setMode(CompilationMode.OPT)
            .addCompilerFlag("-mthumb")
            .addCompilerFlag("-Os")
            .addCompilerFlag("-g")
            .addCompilerFlag("-DNDEBUG"));

    toolchain.addCompilationModeFlags(
        CompilationModeFlags.newBuilder()
            .setMode(CompilationMode.DBG)
            .addCompilerFlag("-g")
            .addCompilerFlag("-mthumb")
            .addCompilerFlag("-O0")
            .addCompilerFlag("-UNDEBUG"));

    return toolchain;
  }

  private List<CToolchain.Builder> createArmeabiClangToolchain() {
    ImmutableList<CToolchain.Builder> toolchains =
        ImmutableList.of(
            createBaseArmeabiClangToolchain()
                .setToolchainIdentifier("arm-linux-androideabi-clang3.8")
                .setTargetCpu("armeabi")
                .addCompilerFlag("-target")
                .addCompilerFlag("armv5te-none-linux-androideabi") // LLVM_TRIPLE
                .addCompilerFlag("-march=armv5te")
                .addCompilerFlag("-mtune=xscale")
                .addCompilerFlag("-msoft-float")
                .addLinkerFlag("-target")
                // LLVM_TRIPLE
                .addLinkerFlag("armv5te-none-linux-androideabi"),
            createBaseArmeabiClangToolchain()
                .setToolchainIdentifier("arm-linux-androideabi-clang3.8-v7a")
                .setTargetCpu("armeabi-v7a")
                .addCompilerFlag("-target")
                .addCompilerFlag("armv7-none-linux-androideabi") // LLVM_TRIPLE
                .addCompilerFlag("-march=armv7-a")
                .addCompilerFlag("-mfloat-abi=softfp")
                .addCompilerFlag("-mfpu=vfpv3-d16")
                .addLinkerFlag("-target")
                .addLinkerFlag("armv7-none-linux-androideabi") // LLVM_TRIPLE
                .addLinkerFlag("-Wl,--fix-cortex-a8"));
    stlImpl.addStlImpl(toolchains, "4.9");
    return toolchains;
  }

  private CToolchain.Builder createBaseArmeabiClangToolchain() {
    String toolchainName = "arm-linux-androideabi-4.9";
    String targetPlatform = "arm-linux-androideabi";
    String gccToolchain = ndkPaths.createGccToolchainPath("arm-linux-androideabi-4.9");

    CToolchain.Builder toolchain =
        CToolchain.newBuilder()
            .setTargetSystemName("arm-linux-androideabi")
            .setCompiler("clang3.8")
            .addAllToolPath(ndkPaths.createClangToolpaths(toolchainName, targetPlatform, null))
            .addCxxBuiltinIncludeDirectory(
                ndkPaths.createClangToolchainBuiltinIncludeDirectory(
                    AndroidNdkCrosstoolsR12.CLANG_VERSION))
            .setBuiltinSysroot(ndkPaths.createBuiltinSysroot("arm"))

            // Compiler flags
            .addCompilerFlag("-gcc-toolchain")
            .addCompilerFlag(gccToolchain)
            .addCompilerFlag("-fpic")
            .addCompilerFlag("-ffunction-sections")
            .addCompilerFlag("-funwind-tables")
            .addCompilerFlag("-fstack-protector-strong")
            .addCompilerFlag("-Wno-invalid-command-line-argument")
            .addCompilerFlag("-Wno-unused-command-line-argument")
            .addCompilerFlag("-no-canonical-prefixes")
            .addCompilerFlag("-fno-integrated-as")

            // Linker flags
            .addLinkerFlag("-gcc-toolchain")
            .addLinkerFlag(gccToolchain)
            .addLinkerFlag("-no-canonical-prefixes");

    toolchain.addCompilationModeFlags(
        CompilationModeFlags.newBuilder()
            .setMode(CompilationMode.OPT)
            .addCompilerFlag("-mthumb")
            .addCompilerFlag("-Os")
            .addCompilerFlag("-g")
            .addCompilerFlag("-DNDEBUG"));
    toolchain.addCompilationModeFlags(
        CompilationModeFlags.newBuilder()
            .setMode(CompilationMode.DBG)
            .addCompilerFlag("-g")
            .addCompilerFlag("-fno-strict-aliasing")
            .addCompilerFlag("-O0")
            .addCompilerFlag("-UNDEBUG"));
    return toolchain;
  }
}
