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

package com.google.devtools.build.lib.rules.proto;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.devtools.build.lib.collect.nestedset.Order.STABLE_ORDER;
import static com.google.devtools.build.lib.rules.proto.ProtoCommon.areDepsStrict;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.AbstractAction;
import com.google.devtools.build.lib.actions.Action;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.CommandLineItem;
import com.google.devtools.build.lib.actions.CommandLineItem.CapturingMapFn;
import com.google.devtools.build.lib.actions.ParamFileInfo;
import com.google.devtools.build.lib.actions.ParameterFile.ParameterFileType;
import com.google.devtools.build.lib.analysis.FilesToRunProvider;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.actions.CustomCommandLine;
import com.google.devtools.build.lib.analysis.actions.CustomCommandLine.VectorArg;
import com.google.devtools.build.lib.analysis.actions.FileWriteAction;
import com.google.devtools.build.lib.analysis.actions.SpawnAction;
import com.google.devtools.build.lib.analysis.stringtemplate.ExpansionException;
import com.google.devtools.build.lib.analysis.stringtemplate.TemplateContext;
import com.google.devtools.build.lib.analysis.stringtemplate.TemplateExpander;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.LabelConstants;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.util.LazyString;
import com.google.devtools.build.lib.util.Pair;
import com.google.devtools.build.lib.vfs.PathFragment;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/** Constructs actions to run the protocol compiler to generate sources from .proto files. */
public class ProtoCompileActionBuilder {
  @VisibleForTesting
  public static final String STRICT_DEPS_FLAG_TEMPLATE =
      "--direct_dependencies_violation_msg=" + StrictProtoDepsViolationMessage.MESSAGE;

  private static final String MNEMONIC = "GenProto";
  private static final Action[] NO_ACTIONS = new Action[0];

  private final RuleContext ruleContext;
  private final ProtoInfo protoInfo;
  private final FilesToRunProvider protoCompiler;
  private final String language;
  private final String langPrefix;
  private final Iterable<Artifact> outputs;
  private Iterable<Artifact> inputs;
  private FilesToRunProvider langPlugin;
  private Supplier<String> langPluginParameter;
  private boolean hasServices;
  private Iterable<String> additionalCommandLineArguments;
  private Iterable<FilesToRunProvider> additionalTools;
  private boolean checkStrictImportPublic;

  public ProtoCompileActionBuilder allowServices(boolean hasServices) {
    this.hasServices = hasServices;
    return this;
  }

  public ProtoCompileActionBuilder setInputs(Iterable<Artifact> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ProtoCompileActionBuilder setLangPlugin(FilesToRunProvider langPlugin) {
    this.langPlugin = langPlugin;
    return this;
  }

  public ProtoCompileActionBuilder setLangPluginParameter(Supplier<String> langPluginParameter) {
    this.langPluginParameter = langPluginParameter;
    return this;
  }

  public ProtoCompileActionBuilder setAdditionalCommandLineArguments(
      Iterable<String> additionalCmdLine) {
    this.additionalCommandLineArguments = additionalCmdLine;
    return this;
  }

  public ProtoCompileActionBuilder setAdditionalTools(
      Iterable<FilesToRunProvider> additionalTools) {
    this.additionalTools = additionalTools;
    return this;
  }

  public ProtoCompileActionBuilder checkStrictImportPublic(boolean checkStrictImportPublic) {
    this.checkStrictImportPublic = checkStrictImportPublic;
    return this;
  }

  public ProtoCompileActionBuilder(
      RuleContext ruleContext,
      ProtoInfo protoInfo,
      FilesToRunProvider protoCompiler,
      String language,
      String langPrefix,
      Iterable<Artifact> outputs) {
    this.ruleContext = ruleContext;
    this.protoInfo = protoInfo;
    this.protoCompiler = protoCompiler;
    this.language = language;
    this.langPrefix = langPrefix;
    this.outputs = outputs;
  }

  /** Static class to avoid keeping a reference to this builder after build() is called. */
  @AutoCodec.VisibleForSerialization
  @AutoCodec
  static class LazyLangPluginFlag extends LazyString {
    private final String langPrefix;
    private final Supplier<String> langPluginParameter;

    @AutoCodec.VisibleForSerialization
    LazyLangPluginFlag(String langPrefix, Supplier<String> langPluginParameter) {
      this.langPrefix = langPrefix;
      this.langPluginParameter = langPluginParameter;
    }

    @Override
    public String toString() {
      return String.format("--%s_out=%s", langPrefix, langPluginParameter.get());
    }
  }

  @AutoCodec.VisibleForSerialization
  @AutoCodec
  static class LazyCommandLineExpansion extends LazyString {
    // E.g., --java_out=%s
    private final String template;
    private final Map<String, ? extends CharSequence> variableValues;

    @AutoCodec.VisibleForSerialization
    LazyCommandLineExpansion(String template, Map<String, ? extends CharSequence> variableValues) {
      this.template = template;
      this.variableValues = variableValues;
    }

    @Override
    public String toString() {
      try {
        return TemplateExpander.expand(
                template,
                new TemplateContext() {
                  @Override
                  public String lookupVariable(String name) throws ExpansionException {
                    CharSequence value = variableValues.get(name);
                    if (value == null) {
                      throw new ExpansionException(String.format("$(%s) not defined", name));
                    }
                    return value.toString();
                  }

                  @Override
                  public String lookupFunction(String name, String param)
                      throws ExpansionException {
                    throw new ExpansionException(String.format("$(%s) not defined", name));
                  }
                })
            .expansion();
      } catch (ExpansionException e) {
        // Squeelch. We don't throw this exception in the lookupMakeVariable implementation above,
        // and we can't report it here anyway, because this code will typically execute in the
        // Execution phase.
      }
      return template;
    }
  }

  public Action[] build() {
    if (isEmpty(outputs)) {
      return NO_ACTIONS;
    }

    try {
      return createAction().build(ruleContext);
    } catch (MissingPrerequisiteException e) {
      return NO_ACTIONS;
    }
  }

  private SpawnAction.Builder createAction() throws MissingPrerequisiteException {
    SpawnAction.Builder result =
        new SpawnAction.Builder().addTransitiveInputs(protoInfo.getTransitiveProtoSources());

    if (langPlugin != null) {
      result.addTool(langPlugin);
    }

    if (inputs != null) {
      result.addInputs(inputs);
    }

    if (this.additionalTools != null) {
      for (FilesToRunProvider tool : additionalTools) {
        result.addTool(tool);
      }
    }

    if (protoCompiler == null) {
      throw new MissingPrerequisiteException();
    }

    result
        .addOutputs(outputs)
        .setResources(AbstractAction.DEFAULT_RESOURCE_SET)
        .useDefaultShellEnvironment()
        .setExecutable(protoCompiler)
        .addCommandLine(
            createProtoCompilerCommandLine().build(),
            ParamFileInfo.builder(ParameterFileType.UNQUOTED).build())
        .setProgressMessage("Generating %s proto_library %s", language, ruleContext.getLabel())
        .setMnemonic(MNEMONIC);

    return result;
  }

  private static String getOutputDirectory(RuleContext ruleContext) {
    return ruleContext.getBinDirectory().getExecPath().getSegment(0);
  }

  /** Commandline generator for protoc invocations. */
  @VisibleForTesting
  CustomCommandLine.Builder createProtoCompilerCommandLine() {
    CustomCommandLine.Builder result = CustomCommandLine.builder();

    if (langPlugin != null) {
      // We pass a separate langPlugin as there are plugins that cannot be overridden
      // and thus we have to deal with "$xx_plugin" and "xx_plugin".
      result.addFormatted(
          "--plugin=protoc-gen-%s=%s", langPrefix, langPlugin.getExecutable().getExecPath());
    }

    if (langPluginParameter != null) {
      result.addLazyString(new LazyLangPluginFlag(langPrefix, langPluginParameter));
    }

    result.addAll(ruleContext.getFragment(ProtoConfiguration.class).protocOpts());

    boolean areDepsStrict = areDepsStrict(ruleContext);

    boolean siblingRepositoryLayout = ruleContext.getConfiguration().isSiblingRepositoryLayout();

    // Add include maps
    addIncludeMapArguments(
        getOutputDirectory(ruleContext),
        result,
        areDepsStrict ? protoInfo.getStrictImportableProtoSourcesImportPaths() : null,
        protoInfo.getStrictImportableProtoSourceRoots(),
        protoInfo.getTransitiveProtoSources(),
        siblingRepositoryLayout);

    if (areDepsStrict) {
      // Note: the %s in the line below is used by proto-compiler. That is, the string we create
      // here should have a literal %s in it.
      result.addFormatted(STRICT_DEPS_FLAG_TEMPLATE, ruleContext.getLabel());
    }

    for (Artifact src : protoInfo.getDirectProtoSources()) {
      result.addPath(src.getExecPath());
    }

    if (!hasServices) {
      result.add("--disallow_services");
    }
    if (checkStrictImportPublic) {
      NestedSet<Pair<Artifact, String>> protosInExports =
          protoInfo.getExportedProtoSourcesImportPaths();
      if (protosInExports.isEmpty()) {
        // This line is necessary to trigger the check.
        result.add("--allowed_public_imports=");
      } else {
        result.addAll(
            "--allowed_public_imports",
            VectorArg.join(":")
                .each(protosInExports)
                .mapped(
                    new ExpandToPathFnWithImports(
                        getOutputDirectory(ruleContext),
                        protoInfo.getTransitiveProtoSourceRoots(),
                        siblingRepositoryLayout)));
      }
    }

    if (additionalCommandLineArguments != null) {
      result.addAll(ImmutableList.copyOf(additionalCommandLineArguments));
    }

    return result;
  }

  /** Signifies that a prerequisite could not be satisfied. */
  private static class MissingPrerequisiteException extends Exception {}

  public static void writeDescriptorSet(
      RuleContext ruleContext, ProtoInfo protoInfo, Services allowServices) {
    Artifact output = protoInfo.getDirectDescriptorSet();
    ImmutableList<ProtoInfo> protoDeps =
        ImmutableList.copyOf(ruleContext.getPrerequisites("deps", ProtoInfo.PROVIDER));
    NestedSet<Artifact> dependenciesDescriptorSets =
        ProtoCommon.computeDependenciesDescriptorSets(protoDeps);
    if (protoInfo.getDirectProtoSources().isEmpty()) {
      ruleContext.registerAction(
          FileWriteAction.createEmptyWithInputs(
              ruleContext.getActionOwner(), dependenciesDescriptorSets, output));
      return;
    }

    SpawnAction.Builder actions =
        createActions(
            ruleContext,
            ImmutableList.of(
                createDescriptorSetToolchain(
                    ruleContext.getFragment(ProtoConfiguration.class), output.getExecPathString())),
            protoInfo,
            ruleContext.getLabel(),
            ImmutableList.of(output),
            "Descriptor Set",
            Exports.DO_NOT_USE,
            allowServices);
    if (actions == null) {
      return;
    }

    actions.setMnemonic("GenProtoDescriptorSet");
    actions.addTransitiveInputs(dependenciesDescriptorSets);
    ruleContext.registerAction(actions.build(ruleContext));
  }

  private static ToolchainInvocation createDescriptorSetToolchain(
      ProtoConfiguration configuration, CharSequence outReplacement) {
    ImmutableList.Builder<String> protocOpts = ImmutableList.builder();
    if (configuration.experimentalProtoDescriptorSetsIncludeSourceInfo()) {
      protocOpts.add("--include_source_info");
    }

    return new ToolchainInvocation(
        "dontcare",
        ProtoLangToolchainProvider.create(
            // Note: adding --include_imports here was requested multiple times, but it'll cause the
            // output size to become quadratic, so don't.
            // A rule that concatenates the artifacts from ctx.deps.proto.transitive_descriptor_sets
            // provides similar results.
            "--descriptor_set_out=$(OUT)",
            /* pluginExecutable= */ null,
            /* runtime= */ null,
            /* blacklistedProtos= */ NestedSetBuilder.<Artifact>emptySet(STABLE_ORDER)),
        outReplacement,
        protocOpts.build());
  }

  /** Whether to use exports in the proto compile action. */
  public enum Exports {
    USE,
    DO_NOT_USE,
  }

  /** Whether to allow services in the proto compiler invocation. */
  public enum Services {
    ALLOW,
    DISALLOW,
  }

  /** Whether to enable strict dependency checking. */
  public enum Deps {
    STRICT,
    NON_STRICT,
  }
  /**
   * Registers actions to generate code from .proto files.
   *
   * <p>This method uses information from proto_lang_toolchain() rules. New rules should use this
   * method instead of the soup of methods above.
   *
   * @param toolchainInvocations See {@link #createCommandLineFromToolchains}.
   * @param ruleLabel See {@link #createCommandLineFromToolchains}.
   * @param outputs The artifacts that the resulting action must create.
   * @param flavorName e.g., "Java (Immutable)"
   * @param allowServices If false, the compilation will break if any .proto file has service
   */
  public static void registerActions(
      RuleContext ruleContext,
      List<ToolchainInvocation> toolchainInvocations,
      ProtoInfo protoInfo,
      Label ruleLabel,
      Iterable<Artifact> outputs,
      String flavorName,
      Exports useExports,
      Services allowServices) {
    SpawnAction.Builder actions =
        createActions(
            ruleContext,
            toolchainInvocations,
            protoInfo,
            ruleLabel,
            outputs,
            flavorName,
            useExports,
            allowServices);
    if (actions != null) {
      ruleContext.registerAction(actions.build(ruleContext));
    }
  }

  @Nullable
  private static SpawnAction.Builder createActions(
      RuleContext ruleContext,
      List<ToolchainInvocation> toolchainInvocations,
      ProtoInfo protoInfo,
      Label ruleLabel,
      Iterable<Artifact> outputs,
      String flavorName,
      Exports useExports,
      Services allowServices) {

    if (isEmpty(outputs)) {
      return null;
    }

    SpawnAction.Builder result =
        new SpawnAction.Builder().addTransitiveInputs(protoInfo.getTransitiveProtoSources());

    for (ToolchainInvocation invocation : toolchainInvocations) {
      ProtoLangToolchainProvider toolchain = invocation.toolchain;
      if (toolchain.pluginExecutable() != null) {
        result.addTool(toolchain.pluginExecutable());
      }
    }

    FilesToRunProvider compilerTarget = ruleContext.getExecutablePrerequisite(":proto_compiler");
    if (compilerTarget == null) {
      return null;
    }

    boolean siblingRepositoryLayout = ruleContext.getConfiguration().isSiblingRepositoryLayout();

    result
        .addOutputs(outputs)
        .setResources(AbstractAction.DEFAULT_RESOURCE_SET)
        .useDefaultShellEnvironment()
        .setExecutable(compilerTarget)
        .addCommandLine(
            createCommandLineFromToolchains(
                toolchainInvocations,
                getOutputDirectory(ruleContext),
                protoInfo,
                ruleLabel,
                areDepsStrict(ruleContext) ? Deps.STRICT : Deps.NON_STRICT,
                arePublicImportsStrict(ruleContext) ? useExports : Exports.DO_NOT_USE,
                allowServices,
                ruleContext.getFragment(ProtoConfiguration.class).protocOpts(),
                siblingRepositoryLayout),
            ParamFileInfo.builder(ParameterFileType.UNQUOTED).build())
        .setProgressMessage("Generating %s proto_library %s", flavorName, ruleContext.getLabel())
        .setMnemonic(MNEMONIC);

    return result;
  }

  public static boolean arePublicImportsStrict(RuleContext ruleContext) {
    return ruleContext.getFragment(ProtoConfiguration.class).strictPublicImports();
  }

  /**
   * Constructs command-line arguments to execute proto-compiler.
   *
   * <ul>
   *   <li>Each toolchain contributes a command-line, formatted from its commandLine() method.
   *   <li>$(OUT) is replaced with the outReplacement field of ToolchainInvocation.
   *   <li>$(PLUGIN_out) is replaced with PLUGIN_<key>_out where 'key' is the key of
   *       toolchainInvocations. The key thus allows multiple plugins in one command-line.
   *   <li>If a toolchain's {@code plugin()} is non-null, we point at it by emitting
   *       --plugin=protoc-gen-PLUGIN_<key>=<location of plugin>.
   * </ul>
   *
   * Note {@code toolchainInvocations} is ordered, and affects the order in which plugins are
   * called. As some plugins rely on output from other plugins, their order matters.
   *
   * @param toolchainInvocations See {@link #createCommandLineFromToolchains}.
   * @param ruleLabel Name of the proto_library for which we're compiling. This string is used to
   *     populate an error message format that's passed to proto-compiler.
   * @param allowServices If false, the compilation will break if any .proto file has
   */
  @VisibleForTesting
  static CustomCommandLine createCommandLineFromToolchains(
      List<ToolchainInvocation> toolchainInvocations,
      String outputDirectory,
      ProtoInfo protoInfo,
      Label ruleLabel,
      Deps strictDeps,
      Exports useExports,
      Services allowServices,
      ImmutableList<String> protocOpts,
      boolean siblingRepositoryLayout) {
    CustomCommandLine.Builder cmdLine = CustomCommandLine.builder();

    cmdLine.addAll(
        VectorArg.of(protoInfo.getTransitiveProtoSourceRoots())
            .mapped(EXPAND_TRANSITIVE_PROTO_PATH_FLAGS));

    // A set to check if there are multiple invocations with the same name.
    HashSet<String> invocationNames = new HashSet<>();

    for (ToolchainInvocation invocation : toolchainInvocations) {
      if (!invocationNames.add(invocation.name)) {
        throw new IllegalStateException(
            "Invocation name "
                + invocation.name
                + " appears more than once. "
                + "This could lead to incorrect proto-compiler behavior");
      }

      ProtoLangToolchainProvider toolchain = invocation.toolchain;

      cmdLine.addLazyString(
          new LazyCommandLineExpansion(
              toolchain.commandLine(),
              ImmutableMap.of(
                  "OUT",
                  invocation.outReplacement,
                  "PLUGIN_OUT",
                  String.format("PLUGIN_%s_out", invocation.name))));

      if (toolchain.pluginExecutable() != null) {
        cmdLine.addFormatted(
            "--plugin=protoc-gen-PLUGIN_%s=%s",
            invocation.name, toolchain.pluginExecutable().getExecutable().getExecPath());
      }

      cmdLine.addAll(invocation.protocOpts);
    }

    cmdLine.addAll(protocOpts);

    // Add include maps
    addIncludeMapArguments(
        outputDirectory,
        cmdLine,
        strictDeps == Deps.STRICT ? protoInfo.getStrictImportableProtoSourcesImportPaths() : null,
        protoInfo.getStrictImportableProtoSourceRoots(),
        protoInfo.getTransitiveProtoSources(),
        siblingRepositoryLayout);

    if (strictDeps == Deps.STRICT) {
      cmdLine.addFormatted(STRICT_DEPS_FLAG_TEMPLATE, ruleLabel);
    }

    if (useExports == Exports.USE) {
      if (protoInfo.getExportedProtoSourcesImportPaths().isEmpty()) {
        // This line is necessary to trigger the check.
        cmdLine.add("--allowed_public_imports=");
      } else {
        cmdLine.addAll(
            "--allowed_public_imports",
            VectorArg.join(":")
                .each(protoInfo.getExportedProtoSourcesImportPaths())
                .mapped(
                    new ExpandToPathFnWithImports(
                        outputDirectory,
                        protoInfo.getExportedProtoSourceRoots(),
                        siblingRepositoryLayout)));
      }
    }

    for (Artifact src : protoInfo.getDirectProtoSources()) {
      cmdLine.addPath(src.getExecPath());
    }

    if (allowServices == Services.DISALLOW) {
      cmdLine.add("--disallow_services");
    }

    return cmdLine.build();
  }

  @VisibleForTesting
  static void addIncludeMapArguments(
      String outputDirectory,
      CustomCommandLine.Builder commandLine,
      @Nullable NestedSet<Pair<Artifact, String>> protosInDirectDependencies,
      NestedSet<String> directProtoSourceRoots,
      NestedSet<Artifact> transitiveImports,
      boolean siblingRepositoryLayout) {
    // For each import, include both the import as well as the import relativized against its
    // protoSourceRoot. This ensures that protos can reference either the full path or the short
    // path when including other protos.
    commandLine.addAll(
        VectorArg.of(transitiveImports)
            .mapped(
                new ExpandImportArgsFn(
                    outputDirectory, directProtoSourceRoots, siblingRepositoryLayout)));
    if (protosInDirectDependencies != null) {
      if (!protosInDirectDependencies.isEmpty()) {
        commandLine.addAll(
            "--direct_dependencies",
            VectorArg.join(":")
                .each(protosInDirectDependencies)
                .mapped(
                    new ExpandToPathFnWithImports(
                        outputDirectory, directProtoSourceRoots, siblingRepositoryLayout)));

      } else {
        // The proto compiler requires an empty list to turn on strict deps checking
        commandLine.add("--direct_dependencies=");
      }
    }
  }

  private static String guessProtoPathUnderRoot(
      String outputDirectory,
      PathFragment sourceRootPath,
      Artifact proto,
      boolean siblingRepositoryLayout) {
    // TODO(lberki): Instead of guesswork like this, we should track which proto belongs to
    // which source root. Unfortunately, that's a non-trivial migration since
    // ProtoInfo is on the Starlark API. Therefore, we hack:
    // - If the source root is under the output directory (itself determined in a hacky way and
    // relying on the fact that the output roots of all repositories are under the same directory
    // under the exec root), we check whether the .proto file is under it. If so, we have a match.
    // - Otherwise, we check whether the .proto file is either under that source directory or under
    // bin or genfiles by prefix-matching its root-relative path.
    if (sourceRootPath.segmentCount() > 0 && sourceRootPath.getSegment(0).equals(outputDirectory)) {
      if (proto.getExecPath().startsWith(sourceRootPath)) {
        return proto.getExecPath().relativeTo(sourceRootPath).getPathString();
      }
    } else {
      PathFragment prefix =
          siblingRepositoryLayout
              ? LabelConstants.EXPERIMENTAL_EXTERNAL_PATH_PREFIX
              : LabelConstants.EXTERNAL_PATH_PREFIX;
      if (proto.getRootRelativePath().startsWith(sourceRootPath)) {
        return proto.getRootRelativePath().relativeTo(sourceRootPath).getPathString();
      } else if (proto.getExecPath().startsWith(prefix)
          && proto.getExecPath().startsWith(sourceRootPath)) {
        return proto.getExecPath().relativeTo(sourceRootPath).getPathString();
      }
    }

    return null;
  }

  @AutoCodec @AutoCodec.VisibleForSerialization
  static final CommandLineItem.MapFn<String> EXPAND_TRANSITIVE_PROTO_PATH_FLAGS =
      (flag, args) -> {
        if (!flag.equals(".")) {
          args.accept("--proto_path=" + flag);
        }
      };


  @AutoCodec
  @AutoCodec.VisibleForSerialization
  static final class ExpandImportArgsFn implements CapturingMapFn<Artifact> {
    private final String outputDirectory;
    private final NestedSet<String> directProtoSourceRoots;
    private final boolean siblingRepositoryLayout;

    public ExpandImportArgsFn(
        String outputDirectory,
        NestedSet<String> directProtoSourceRoots,
        boolean siblingRepositoryLayout) {
      this.outputDirectory = outputDirectory;
      this.directProtoSourceRoots = directProtoSourceRoots;
      this.siblingRepositoryLayout = siblingRepositoryLayout;
    }

    /**
     * Generates up to two import flags for each artifact: one for full path (only relative to the
     * repository root) and one for the path relative to the proto source root (if one exists
     * corresponding to the artifact).
     */
    @Override
    public void expandToCommandLine(Artifact proto, Consumer<String> args) {
      for (String directProtoSourceRoot : directProtoSourceRoots.toList()) {
        PathFragment sourceRootPath = PathFragment.create(directProtoSourceRoot);
        String arg =
            guessProtoPathUnderRoot(
                outputDirectory, sourceRootPath, proto, siblingRepositoryLayout);
        if (arg != null) {
          args.accept("-I" + arg + "=" + proto.getExecPathString());
        }
      }
    }
  }

  @AutoCodec
  @AutoCodec.VisibleForSerialization
  static final class ExpandToPathFnWithImports implements CapturingMapFn<Pair<Artifact, String>> {
    private final String outputDirectory;
    private final NestedSet<String> directProtoSourceRoots;
    private final boolean siblingRepositoryLayout;

    public ExpandToPathFnWithImports(
        String outputDirectory,
        NestedSet<String> directProtoSourceRoots,
        boolean siblingRepositoryLayout) {
      this.outputDirectory = outputDirectory;
      this.directProtoSourceRoots = directProtoSourceRoots;
      this.siblingRepositoryLayout = siblingRepositoryLayout;
    }

    @Override
    public void expandToCommandLine(Pair<Artifact, String> proto, Consumer<String> args) {
      if (proto.second != null) {
        args.accept(proto.second);
      } else {
        for (String directProtoSourceRoot : directProtoSourceRoots.toList()) {
          PathFragment sourceRootPath = PathFragment.create(directProtoSourceRoot);
          String arg =
              guessProtoPathUnderRoot(
                  outputDirectory, sourceRootPath, proto.first, siblingRepositoryLayout);
          if (arg != null) {
            args.accept(arg);
          }
        }
      }
    }
  }

  /**
   * Describes a toolchain and the value to replace for a $(OUT) that might appear in its
   * commandLine() (e.g., "bazel-out/foo.srcjar").
   */
  public static class ToolchainInvocation {
    final String name;
    public final ProtoLangToolchainProvider toolchain;
    final CharSequence outReplacement;
    final ImmutableList<String> protocOpts;

    public ToolchainInvocation(
        String name, ProtoLangToolchainProvider toolchain, CharSequence outReplacement) {
      this(name, toolchain, outReplacement, ImmutableList.of());
    }

    public ToolchainInvocation(
        String name,
        ProtoLangToolchainProvider toolchain,
        CharSequence outReplacement,
        ImmutableList<String> protocOpts) {
      Preconditions.checkState(!name.contains(" "), "Name %s should not contain spaces", name);
      this.name = name;
      this.toolchain = toolchain;
      this.outReplacement = outReplacement;
      this.protocOpts = Preconditions.checkNotNull(protocOpts);
    }
  }
}
