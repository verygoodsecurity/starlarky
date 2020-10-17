// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.rules.java;

import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.FilesToRunProvider;
import com.google.devtools.build.lib.analysis.ProviderCollection;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.RuleErrorConsumer;
import com.google.devtools.build.lib.analysis.TransitiveInfoCollection;
import com.google.devtools.build.lib.analysis.platform.ToolchainInfo;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.collect.nestedset.Depset;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec.VisibleForSerialization;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.java.JavaToolchainStarlarkApiProviderApi;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.syntax.Location;

/** Information about the JDK used by the <code>java_*</code> rules. */
@Immutable
@AutoCodec
public class JavaToolchainProvider extends ToolchainInfo
    implements JavaToolchainStarlarkApiProviderApi {

  /** Returns the Java Toolchain associated with the rule being analyzed or {@code null}. */
  public static JavaToolchainProvider from(RuleContext ruleContext) {
    TransitiveInfoCollection prerequisite =
        ruleContext.getPrerequisite(JavaRuleClasses.JAVA_TOOLCHAIN_ATTRIBUTE_NAME);
    return from(prerequisite, ruleContext);
  }

  public static JavaToolchainProvider from(ProviderCollection collection) {
    return from(collection, null);
  }

  private static JavaToolchainProvider from(
      ProviderCollection collection, @Nullable RuleErrorConsumer errorConsumer) {
    ToolchainInfo toolchainInfo = collection.get(ToolchainInfo.PROVIDER);
    if (toolchainInfo instanceof JavaToolchainProvider) {
      return (JavaToolchainProvider) toolchainInfo;
    }
    if (errorConsumer != null) {
      errorConsumer.ruleError("The selected Java toolchain is not a JavaToolchainProvider");
    }
    return null;
  }

  public static JavaToolchainProvider create(
      Label label,
      ImmutableList<String> javacOptions,
      ImmutableList<String> jvmOptions,
      ImmutableList<String> javabuilderJvmOptions,
      ImmutableList<String> turbineJvmOptions,
      boolean javacSupportsWorkers,
      boolean javacSupportsMultiplexWorkers,
      BootClassPathInfo bootclasspath,
      @Nullable Artifact javac,
      NestedSet<Artifact> tools,
      FilesToRunProvider javaBuilder,
      @Nullable FilesToRunProvider headerCompiler,
      @Nullable FilesToRunProvider headerCompilerDirect,
      ImmutableSet<String> headerCompilerBuiltinProcessors,
      ImmutableSet<String> reducedClasspathIncompatibleProcessors,
      boolean forciblyDisableHeaderCompilation,
      Artifact singleJar,
      @Nullable Artifact oneVersion,
      @Nullable Artifact oneVersionAllowlist,
      Artifact genClass,
      @Nullable Artifact resourceJarBuilder,
      @Nullable Artifact timezoneData,
      FilesToRunProvider ijar,
      ImmutableListMultimap<String, String> compatibleJavacOptions,
      ImmutableList<JavaPackageConfigurationProvider> packageConfiguration,
      FilesToRunProvider jacocoRunner,
      FilesToRunProvider proguardAllowlister,
      JavaSemantics javaSemantics) {
    return new JavaToolchainProvider(
        label,
        bootclasspath,
        javac,
        tools,
        javaBuilder,
        headerCompiler,
        headerCompilerDirect,
        headerCompilerBuiltinProcessors,
        reducedClasspathIncompatibleProcessors,
        forciblyDisableHeaderCompilation,
        singleJar,
        oneVersion,
        oneVersionAllowlist,
        genClass,
        resourceJarBuilder,
        timezoneData,
        ijar,
        compatibleJavacOptions,
        javacOptions,
        jvmOptions,
        javabuilderJvmOptions,
        turbineJvmOptions,
        javacSupportsWorkers,
        javacSupportsMultiplexWorkers,
        packageConfiguration,
        jacocoRunner,
        proguardAllowlister,
        javaSemantics);
  }

  private final Label label;
  private final BootClassPathInfo bootclasspath;
  @Nullable private final Artifact javac;
  private final NestedSet<Artifact> tools;
  private final FilesToRunProvider javaBuilder;
  @Nullable private final FilesToRunProvider headerCompiler;
  @Nullable private final FilesToRunProvider headerCompilerDirect;
  private final ImmutableSet<String> headerCompilerBuiltinProcessors;
  private final ImmutableSet<String> reducedClasspathIncompatibleProcessors;
  private final boolean forciblyDisableHeaderCompilation;
  private final Artifact singleJar;
  @Nullable private final Artifact oneVersion;
  @Nullable private final Artifact oneVersionAllowlist;
  private final Artifact genClass;
  @Nullable private final Artifact resourceJarBuilder;
  @Nullable private final Artifact timezoneData;
  private final FilesToRunProvider ijar;
  private final ImmutableListMultimap<String, String> compatibleJavacOptions;
  private final ImmutableList<String> javacOptions;
  private final ImmutableList<String> jvmOptions;
  private final ImmutableList<String> javabuilderJvmOptions;
  private final ImmutableList<String> turbineJvmOptions;
  private final boolean javacSupportsWorkers;
  private final boolean javacSupportsMultiplexWorkers;
  private final ImmutableList<JavaPackageConfigurationProvider> packageConfiguration;
  private final FilesToRunProvider jacocoRunner;
  private final FilesToRunProvider proguardAllowlister;
  private final JavaSemantics javaSemantics;

  @VisibleForSerialization
  JavaToolchainProvider(
      Label label,
      BootClassPathInfo bootclasspath,
      @Nullable Artifact javac,
      NestedSet<Artifact> tools,
      FilesToRunProvider javaBuilder,
      @Nullable FilesToRunProvider headerCompiler,
      @Nullable FilesToRunProvider headerCompilerDirect,
      ImmutableSet<String> headerCompilerBuiltinProcessors,
      ImmutableSet<String> reducedClasspathIncompatibleProcessors,
      boolean forciblyDisableHeaderCompilation,
      Artifact singleJar,
      @Nullable Artifact oneVersion,
      @Nullable Artifact oneVersionAllowlist,
      Artifact genClass,
      @Nullable Artifact resourceJarBuilder,
      @Nullable Artifact timezoneData,
      FilesToRunProvider ijar,
      ImmutableListMultimap<String, String> compatibleJavacOptions,
      ImmutableList<String> javacOptions,
      ImmutableList<String> jvmOptions,
      ImmutableList<String> javabuilderJvmOptions,
      ImmutableList<String> turbineJvmOptions,
      boolean javacSupportsWorkers,
      boolean javacSupportsMultiplexWorkers,
      ImmutableList<JavaPackageConfigurationProvider> packageConfiguration,
      FilesToRunProvider jacocoRunner,
      FilesToRunProvider proguardAllowlister,
      JavaSemantics javaSemantics) {
    super(ImmutableMap.of(), Location.BUILTIN);

    this.label = label;
    this.bootclasspath = bootclasspath;
    this.javac = javac;
    this.tools = tools;
    this.javaBuilder = javaBuilder;
    this.headerCompiler = headerCompiler;
    this.headerCompilerDirect = headerCompilerDirect;
    this.headerCompilerBuiltinProcessors = headerCompilerBuiltinProcessors;
    this.reducedClasspathIncompatibleProcessors = reducedClasspathIncompatibleProcessors;
    this.forciblyDisableHeaderCompilation = forciblyDisableHeaderCompilation;
    this.singleJar = singleJar;
    this.oneVersion = oneVersion;
    this.oneVersionAllowlist = oneVersionAllowlist;
    this.genClass = genClass;
    this.resourceJarBuilder = resourceJarBuilder;
    this.timezoneData = timezoneData;
    this.ijar = ijar;
    this.compatibleJavacOptions = compatibleJavacOptions;
    this.javacOptions = javacOptions;
    this.jvmOptions = jvmOptions;
    this.javabuilderJvmOptions = javabuilderJvmOptions;
    this.turbineJvmOptions = turbineJvmOptions;
    this.javacSupportsWorkers = javacSupportsWorkers;
    this.javacSupportsMultiplexWorkers = javacSupportsMultiplexWorkers;
    this.packageConfiguration = packageConfiguration;
    this.jacocoRunner = jacocoRunner;
    this.proguardAllowlister = proguardAllowlister;
    this.javaSemantics = javaSemantics;
  }

  /** Returns the label for this {@code java_toolchain}. */
  public Label getToolchainLabel() {
    return label;
  }

  /** @return the target Java bootclasspath */
  public BootClassPathInfo getBootclasspath() {
    return bootclasspath;
  }

  /** Returns the {@link Artifact} of the javac jar */
  @Nullable
  public Artifact getJavac() {
    return javac;
  }

  /** Returns the {@link Artifact}s of compilation tools. */
  public NestedSet<Artifact> getTools() {
    return tools;
  }

  /** Returns the {@link FilesToRunProvider} of JavaBuilder */
  public FilesToRunProvider getJavaBuilder() {
    return javaBuilder;
  }

  /** @return the {@link FilesToRunProvider} of the Header Compiler deploy jar */
  @Nullable
  public FilesToRunProvider getHeaderCompiler() {
    return headerCompiler;
  }

  /**
   * Returns the {@link FilesToRunProvider} of the Header Compiler deploy jar for direct-classpath,
   * non-annotation processing actions.
   */
  @Nullable
  public FilesToRunProvider getHeaderCompilerDirect() {
    return headerCompilerDirect;
  }

  /** Returns class names of annotation processors that are built in to the header compiler. */
  public ImmutableSet<String> getHeaderCompilerBuiltinProcessors() {
    return headerCompilerBuiltinProcessors;
  }

  public ImmutableSet<String> getReducedClasspathIncompatibleProcessors() {
    return reducedClasspathIncompatibleProcessors;
  }


  /**
   * Returns {@code true} if header compilation should be forcibly disabled, overriding
   * --java_header_compilation.
   */
  public boolean getForciblyDisableHeaderCompilation() {
    return forciblyDisableHeaderCompilation;
  }

  /** Returns the {@link Artifact} of the SingleJar deploy jar */
  @Override
  public Artifact getSingleJar() {
    return singleJar;
  }

  /**
   * Return the {@link Artifact} of the binary that enforces one-version compliance of java
   * binaries.
   */
  @Nullable
  public Artifact getOneVersionBinary() {
    return oneVersion;
  }

  /** Return the {@link Artifact} of the allowlist used by the one-version compliance checker. */
  @Nullable
  public Artifact getOneVersionAllowlist() {
    return oneVersionAllowlist;
  }

  /** Return the {@link Artifact} of the allowlist used by the one-version compliance checker. */
  @Nullable
  public Artifact getOneVersionWhitelist() {
    return oneVersionAllowlist;
  }

  /** Returns the {@link Artifact} of the GenClass deploy jar */
  public Artifact getGenClass() {
    return genClass;
  }

  @Nullable
  public Artifact getResourceJarBuilder() {
    return resourceJarBuilder;
  }

  /**
   * Returns the {@link Artifact} of the latest timezone data resource jar that can be loaded by
   * Java 8 binaries.
   */
  @Nullable
  public Artifact getTimezoneData() {
    return timezoneData;
  }

  /** Returns the ijar executable */
  public FilesToRunProvider getIjar() {
    return ijar;
  }

  ImmutableListMultimap<String, String> getCompatibleJavacOptions() {
    return compatibleJavacOptions;
  }

  /** @return the map of target environment-specific javacopts. */
  public ImmutableList<String> getCompatibleJavacOptions(String key) {
    return getCompatibleJavacOptions().get(key);
  }

  /** @return the list of default options for the java compiler */
  public ImmutableList<String> getJavacOptions(RuleContext ruleContext) {
    ImmutableList.Builder<String> result = ImmutableList.<String>builder().addAll(javacOptions);
    if (ruleContext != null) {
      // TODO(b/78512644): require ruleContext to be non-null after java_common.default_javac_opts
      // is turned down
      result.addAll(ruleContext.getFragment(JavaConfiguration.class).getDefaultJavacFlags());
    }
    return result.build();
  }

  /**
   * @return the list of default options for the JVM running the java compiler and associated tools.
   */
  public ImmutableList<String> getJvmOptions() {
    return jvmOptions;
  }

  /** Returns the list of JVM options for running JavaBuilder. */
  public ImmutableList<String> getJavabuilderJvmOptions() {
    return javabuilderJvmOptions;
  }

  public ImmutableList<String> getTurbineJvmOptions() {
    return turbineJvmOptions;
  }

  /** @return whether JavaBuilders supports running as a persistent worker or not */
  public boolean getJavacSupportsWorkers() {
    return javacSupportsWorkers;
  }

  /** Returns whether JavaBuilders supports running persistent workers in multiplex mode */
  public boolean getJavacSupportsMultiplexWorkers() {
    return javacSupportsMultiplexWorkers;
  }

  /** Returns the global {@code java_plugin_configuration} data. */
  public ImmutableList<JavaPackageConfigurationProvider> packageConfiguration() {
    return packageConfiguration;
  }

  public FilesToRunProvider getJacocoRunner() {
    return jacocoRunner;
  }

  public FilesToRunProvider getProguardAllowlister() {
    return proguardAllowlister;
  }

  public JavaSemantics getJavaSemantics() {
    return javaSemantics;
  }

  /** Returns the input Java language level */
  // TODO(cushon): remove this API; it bakes a deprecated detail of the javac API into Bazel
  @Override
  public String getSourceVersion() {
    Iterator<String> it = javacOptions.iterator();
    while (it.hasNext()) {
      if (it.next().equals("-source") && it.hasNext()) {
        return it.next();
      }
    }
    return JAVA_SPECIFICATION_VERSION.value();
  }

  /** Returns the target Java language level */
  // TODO(cushon): remove this API; it bakes a deprecated detail of the javac API into Bazel
  @Override
  public String getTargetVersion() {
    Iterator<String> it = javacOptions.iterator();
    while (it.hasNext()) {
      if (it.next().equals("-target") && it.hasNext()) {
        return it.next();
      }
    }
    return JAVA_SPECIFICATION_VERSION.value();
  }

  @Override
  @Nullable
  public FileApi getJavacJar() {
    return getJavac();
  }

  @Override
  public Depset getStarlarkBootclasspath() {
    return Depset.of(Artifact.TYPE, getBootclasspath().bootclasspath());
  }

  @Override
  public Sequence<String> getStarlarkJvmOptions() {
    return StarlarkList.immutableCopyOf(getJvmOptions());
  }

  @Override
  public Depset getStarlarkTools() {
    return Depset.of(Artifact.TYPE, getTools());
  }
}
