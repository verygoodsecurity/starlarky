// Copyright 2017 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.rules.apple;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.ExecutionRequirements;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.packages.BuiltinProvider;
import com.google.devtools.build.lib.packages.NativeInfo;
import com.google.devtools.build.lib.rules.apple.ApplePlatform.PlatformType;
import com.google.devtools.build.lib.starlarkbuildapi.apple.XcodeConfigInfoApi;
import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;

/**
 * The set of Apple versions computed from command line options and the {@code xcode_config} rule.
 */
@Immutable
public class XcodeConfigInfo extends NativeInfo
    implements XcodeConfigInfoApi<ApplePlatform, PlatformType> {
  /** Starlark name for this provider. */
  public static final String STARLARK_NAME = "XcodeVersionConfig";

  /** Provider identifier for {@link XcodeConfigInfo}. */
  public static final BuiltinProvider<XcodeConfigInfo> PROVIDER = new XcodeConfigProvider();

  private final DottedVersion iosSdkVersion;
  private final DottedVersion iosMinimumOsVersion;
  private final DottedVersion watchosSdkVersion;
  private final DottedVersion watchosMinimumOsVersion;
  private final DottedVersion tvosSdkVersion;
  private final DottedVersion tvosMinimumOsVersion;
  private final DottedVersion macosSdkVersion;
  private final DottedVersion macosMinimumOsVersion;
  @Nullable private final DottedVersion xcodeVersion;
  @Nullable private final Availability availability;
  @Nullable private final Map<String, String> executionRequirements;

  public XcodeConfigInfo(
      DottedVersion iosSdkVersion,
      DottedVersion iosMinimumOsVersion,
      DottedVersion watchosSdkVersion,
      DottedVersion watchosMinimumOsVersion,
      DottedVersion tvosSdkVersion,
      DottedVersion tvosMinimumOsVersion,
      DottedVersion macosSdkVersion,
      DottedVersion macosMinimumOsVersion,
      DottedVersion xcodeVersion,
      Availability availability) {
    super(PROVIDER);
    this.iosSdkVersion = Preconditions.checkNotNull(iosSdkVersion);
    this.iosMinimumOsVersion = Preconditions.checkNotNull(iosMinimumOsVersion);
    this.watchosSdkVersion = Preconditions.checkNotNull(watchosSdkVersion);
    this.watchosMinimumOsVersion = Preconditions.checkNotNull(watchosMinimumOsVersion);
    this.tvosSdkVersion = Preconditions.checkNotNull(tvosSdkVersion);
    this.tvosMinimumOsVersion = Preconditions.checkNotNull(tvosMinimumOsVersion);
    this.macosSdkVersion = Preconditions.checkNotNull(macosSdkVersion);
    this.macosMinimumOsVersion = Preconditions.checkNotNull(macosMinimumOsVersion);
    this.xcodeVersion = xcodeVersion;
    this.availability = availability;

    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    builder.put(ExecutionRequirements.REQUIRES_DARWIN, "");
    switch (availability) {
      case LOCAL:
        builder.put(ExecutionRequirements.NO_REMOTE, "");
        break;
      case REMOTE:
        builder.put(ExecutionRequirements.NO_LOCAL, "");
        break;
      default:
        break;
    }
    builder.put(ExecutionRequirements.REQUIREMENTS_SET, "");
    this.executionRequirements = builder.build();
  }

  /** Indicates the platform(s) on which an Xcode version is available. */
  public static enum Availability {
    LOCAL("local"),
    REMOTE("remote"),
    BOTH("both"),
    UNKNOWN("unknown");

    public final String name;

    Availability(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  /** Provider for class {@link XcodeConfigInfo} objects. */
  private static class XcodeConfigProvider extends BuiltinProvider<XcodeConfigInfo>
      implements XcodeConfigProviderApi {
    XcodeConfigInfo xcodeConfigInfo;

    private XcodeConfigProvider() {
      super(STARLARK_NAME, XcodeConfigInfo.class);
    }

    @Override
    public XcodeConfigInfoApi<?, ?> xcodeConfigInfo(
        String iosSdkVersion,
        String iosMinimumOsVersion,
        String watchosSdkVersion,
        String watchosMinimumOsVersion,
        String tvosSdkVersion,
        String tvosMinimumOsVersion,
        String macosSdkVersion,
        String macosMinimumOsVersion,
        String xcodeVersion)
        throws EvalException {
      try {
        return new XcodeConfigInfo(
            DottedVersion.fromString(iosSdkVersion),
            DottedVersion.fromString(iosMinimumOsVersion),
            DottedVersion.fromString(watchosSdkVersion),
            DottedVersion.fromString(watchosMinimumOsVersion),
            DottedVersion.fromString(tvosSdkVersion),
            DottedVersion.fromString(tvosMinimumOsVersion),
            DottedVersion.fromString(macosSdkVersion),
            DottedVersion.fromString(macosMinimumOsVersion),
            DottedVersion.fromString(xcodeVersion),
            Availability.UNKNOWN);
      } catch (DottedVersion.InvalidDottedVersionException e) {
        throw new EvalException(e);
      }
    }
  }
  /**
   * Returns the value of the xcode version, if available. This is determined based on a combination
   * of the {@code --xcode_version} build flag and the {@code xcode_config} target defined in the
   * {@code --xcode_version_config} flag. Returns null if no xcode is available.
   */
  @Override
  public DottedVersion getXcodeVersion() {
    return xcodeVersion;
  }

  /**
   * Returns the minimum compatible OS version for target simulator and devices for a particular
   * platform type.
   */
  @Override
  public DottedVersion getMinimumOsForPlatformType(ApplePlatform.PlatformType platformType) {
    // TODO(b/37240784): Look into using only a single minimum OS flag tied to the current
    // apple_platform_type.
    switch (platformType) {
      case IOS:
      case CATALYST:
        /*
         * Catalyst builds require usage of the iOS minimum version when building, but require
         * the usage of the macOS SDK to actually do the build. This means that the particular
         * version used for Catalyst differs based on what you are using the version number for -
         * the SDK or the actual application. In this method we return the OS version used for the
         * application, and so return the iOS version.
         */
        return iosMinimumOsVersion;
      case TVOS:
        return tvosMinimumOsVersion;
      case WATCHOS:
        return watchosMinimumOsVersion;
      case MACOS:
        return macosMinimumOsVersion;
    }
    throw new IllegalArgumentException("Unhandled platform type: " + platformType);
  }

  /**
   * Returns the SDK version for a platform (whether they be for simulator or device). This is
   * directly derived from command line args.
   */
  @Override
  public DottedVersion getSdkVersionForPlatform(ApplePlatform platform) {
    switch (platform) {
      case IOS_DEVICE:
      case IOS_SIMULATOR:
        return iosSdkVersion;
      case TVOS_DEVICE:
      case TVOS_SIMULATOR:
        return tvosSdkVersion;
      case WATCHOS_DEVICE:
      case WATCHOS_SIMULATOR:
        return watchosSdkVersion;
      case MACOS:
      case CATALYST:
        /*
         * Catalyst builds require usage of the iOS minimum version when building, but require
         * the usage of the macOS SDK to actually do the build. This means that the particular
         * version used for Catalyst differs based on what you are using the version for. As this
         * is the SDK version specifically, we use the macOS version here.
         */
        return macosSdkVersion;
    }
    throw new IllegalArgumentException("Unhandled platform: " + platform);
  }

  /** Returns the availability of this Xcode version. */
  public Availability getAvailability() {
    return availability;
  }

  /** Returns the availability of this Xcode version. */
  @Override
  public String getAvailabilityString() {
    return availability.toString();
  }

  /** Returns the execution requirements for actions that use this Xcode version. */
  public Map<String, String> getExecutionRequirements() {
    return executionRequirements;
  }

  @Override
  public Dict<String, String> getExecutionRequirementsDict() {
    return Dict.copyOf(null, executionRequirements);
  }

  public static XcodeConfigInfo fromRuleContext(RuleContext ruleContext) {
    return ruleContext.getPrerequisite(
        XcodeConfigRule.XCODE_CONFIG_ATTR_NAME, XcodeConfigInfo.PROVIDER);
  }
}
