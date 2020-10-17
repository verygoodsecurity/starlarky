// Copyright 2014 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.packages;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.LabelValidator;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.cmdline.RepositoryName;
import com.google.devtools.build.lib.concurrent.NamedForkJoinPool;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.EventHandler;
import com.google.devtools.build.lib.events.ExtendedEventHandler;
import com.google.devtools.build.lib.events.StoredEventHandler;
import com.google.devtools.build.lib.packages.Globber.BadGlobException;
import com.google.devtools.build.lib.packages.Package.Builder.PackageSettings;
import com.google.devtools.build.lib.packages.PackageValidator.InvalidPackageException;
import com.google.devtools.build.lib.packages.RuleClass.Builder.RuleClassType;
import com.google.devtools.build.lib.packages.RuleFactory.BuildLangTypedAttributeValuesMap;
import com.google.devtools.build.lib.packages.semantics.BuildLanguageOptions;
import com.google.devtools.build.lib.profiler.Profiler;
import com.google.devtools.build.lib.profiler.ProfilerTask;
import com.google.devtools.build.lib.profiler.SilentCloseable;
import com.google.devtools.build.lib.server.FailureDetails.FailureDetail;
import com.google.devtools.build.lib.server.FailureDetails.PackageLoading;
import com.google.devtools.build.lib.server.FailureDetails.PackageLoading.Code;
import com.google.devtools.build.lib.util.DetailedExitCode;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.build.lib.vfs.UnixGlob;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.Argument;
import net.starlark.java.syntax.CallExpression;
import net.starlark.java.syntax.DefStatement;
import net.starlark.java.syntax.Expression;
import net.starlark.java.syntax.ForStatement;
import net.starlark.java.syntax.Identifier;
import net.starlark.java.syntax.IfStatement;
import net.starlark.java.syntax.IntLiteral;
import net.starlark.java.syntax.ListExpression;
import net.starlark.java.syntax.Location;
import net.starlark.java.syntax.NodeVisitor;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import net.starlark.java.syntax.StringLiteral;
import net.starlark.java.syntax.SyntaxError;

/**
 * The package factory is responsible for constructing Package instances from a BUILD file's
 * abstract syntax tree (AST).
 *
 * <p>A PackageFactory is a heavy-weight object; create them sparingly. Typically only one is needed
 * per client application.
 */
public final class PackageFactory {

  /** An extension to the global namespace of the BUILD language. */
  // TODO(bazel-team): this should probably be renamed PackageFactory.RuntimeExtension
  //  since really we're extending the Runtime with more classes.
  public interface EnvironmentExtension {
    /** Update the predeclared environment with the identifiers this extension contributes. */
    void update(ImmutableMap.Builder<String, Object> env);

    /** Update the predeclared environment of WORKSPACE files. */
    void updateWorkspace(ImmutableMap.Builder<String, Object> env);

    /** Update the environment of the native module. */
    void updateNative(ImmutableMap.Builder<String, Object> env);

    /** Returns the extra arguments to the {@code package()} statement. */
    Iterable<PackageArgument<?>> getPackageArguments();
  }

  private final RuleFactory ruleFactory;
  private final ImmutableMap<String, BuiltinRuleFunction> ruleFunctions;
  private final RuleClassProvider ruleClassProvider;

  private AtomicReference<? extends UnixGlob.FilesystemCalls> syscalls;

  private ForkJoinPool executor;

  private int maxDirectoriesToEagerlyVisitInGlobbing;

  private final ImmutableList<EnvironmentExtension> environmentExtensions;
  private final ImmutableMap<String, PackageArgument<?>> packageArguments;

  private final PackageSettings packageSettings;
  private final PackageValidator packageValidator;
  private final PackageLoadingListener packageLoadingListener;

  // PackageFactory is the source of truth for the predeclared environments of the various flavors
  // of BUILD and bzl files, including the available fields on the "native" object. For BUILD files
  // and BUILD-loaded .bzl files, these bindings may be modified by builtins injection; see also
  // StarlarkBuiltinsFunction.
  //
  // We cache in PackageFactory all the predeclared environment information that can be known before
  // builtins injection (i.e., before Skyframe evaluation). The singular StarlarkBuiltinsValue
  // caches the result of performing builtins injection.
  //
  // TODO(#11954): Eventually the BUILD and WORKSPACE bzl dialects should converge. Right now they
  // only differ on the "native" object.

  /** The "native" module fields for a BUILD-loaded bzl module, before builtins injection. */
  private final ImmutableMap<String, Object> uninjectedBuildBzlNativeBindings;
  /** The "native" module fields for a WORKSPACE-loaded bzl module. */
  private final ImmutableMap<String, Object> workspaceBzlNativeBindings;
  /** The top-level predeclared symbols for a BUILD-loaded bzl module, before builtins injection. */
  private final ImmutableMap<String, Object> uninjectedBuildBzlEnv;
  /** The top-level predeclared symbols for a WORKSPACE-loaded bzl module. */
  private final ImmutableMap<String, Object> workspaceBzlEnv;
  /** The top-level predeclared symbols for a bzl module in the {@code @builtins} pseudo-repo. */
  private final ImmutableMap<String, Object> builtinsBzlEnv;

  /** Builder for {@link PackageFactory} instances. Intended to only be used by unit tests. */
  @VisibleForTesting
  public abstract static class BuilderForTesting {
    protected final String version = "test";
    protected Iterable<EnvironmentExtension> environmentExtensions = ImmutableList.of();
    protected PackageValidator packageValidator = PackageValidator.NOOP_VALIDATOR;
    protected boolean doChecksForTesting = true;

    public BuilderForTesting setEnvironmentExtensions(
        Iterable<EnvironmentExtension> environmentExtensions) {
      this.environmentExtensions = environmentExtensions;
      return this;
    }

    public BuilderForTesting disableChecks() {
      this.doChecksForTesting = false;
      return this;
    }

    public BuilderForTesting setPackageValidator(PackageValidator packageValidator) {
      this.packageValidator = packageValidator;
      return this;
    }

    public abstract PackageFactory build(RuleClassProvider ruleClassProvider, FileSystem fs);
  }

  @VisibleForTesting
  public PackageSettings getPackageSettingsForTesting() {
    return packageSettings;
  }

  /**
   * Constructs a {@code PackageFactory} instance with a specific glob path translator and rule
   * factory.
   *
   * <p>Only intended to be called by BlazeRuntime or {@link BuilderForTesting#build}.
   *
   * <p>Do not call this constructor directly in tests; please use
   * TestConstants#PACKAGE_FACTORY_BUILDER_FACTORY_FOR_TESTING instead.
   */
  // TODO(bazel-team): Maybe store `version` in the RuleClassProvider rather than passing it in
  // here? It's an extra constructor parameter that all the tests have to give, and it's only needed
  // so WorkspaceFactory can add an extra top-level builtin.
  public PackageFactory(
      RuleClassProvider ruleClassProvider,
      ForkJoinPool executorForGlobbing,
      Iterable<EnvironmentExtension> environmentExtensions,
      String version,
      PackageSettings packageSettings,
      PackageValidator packageValidator,
      PackageLoadingListener packageLoadingListener) {
    this.ruleFactory = new RuleFactory(ruleClassProvider);
    this.ruleFunctions = buildRuleFunctions(ruleFactory);
    this.ruleClassProvider = ruleClassProvider;
    this.executor = executorForGlobbing;
    this.environmentExtensions = ImmutableList.copyOf(environmentExtensions);
    this.packageArguments = createPackageArguments();
    this.packageSettings = packageSettings;
    this.packageValidator = packageValidator;
    this.packageLoadingListener = packageLoadingListener;

    this.uninjectedBuildBzlNativeBindings =
        createUninjectedBuildBzlNativeBindings(
            ruleFunctions, packageArguments, this.environmentExtensions);
    this.workspaceBzlNativeBindings = createWorkspaceBzlNativeBindings(ruleClassProvider, version);
    this.uninjectedBuildBzlEnv =
        createUninjectedBuildBzlEnv(ruleClassProvider, uninjectedBuildBzlNativeBindings);
    this.workspaceBzlEnv = createWorkspaceBzlEnv(ruleClassProvider, workspaceBzlNativeBindings);
    this.builtinsBzlEnv = createBuiltinsBzlEnv(ruleClassProvider);
  }

  /** Sets the syscalls cache used in globbing. */
  public void setSyscalls(AtomicReference<? extends UnixGlob.FilesystemCalls> syscalls) {
    this.syscalls = Preconditions.checkNotNull(syscalls);
  }

  /**
   * Sets the max number of threads to use for globbing.
   *
   * <p>Internally there is a {@link ForkJoinPool} used for globbing. If the specified {@code
   * globbingThreads} does not match the previous value (initial value is 100), then we {@link
   * ForkJoinPool#shutdown()} the old {@link ForkJoinPool} instance and make a new one.
   */
  public void setGlobbingThreads(int globbingThreads) {
    if (executor == null) {
      executor = makeForkJoinPool(globbingThreads);
      return;
    }
    if (executor.getParallelism() == globbingThreads) {
      return;
    }
    // We don't use ForkJoinPool#shutdownNow since it has a performance bug. See
    // http://b/33482341#comment13.
    executor.shutdown();
    executor = makeForkJoinPool(globbingThreads);
  }

  public static ForkJoinPool makeDefaultSizedForkJoinPoolForGlobbing() {
    return makeForkJoinPool(/*globbingThreads=*/ 100);
  }

  private static ForkJoinPool makeForkJoinPool(int globbingThreads) {
    return NamedForkJoinPool.newNamedPool("globbing pool", globbingThreads);
  }

  /**
   * Sets the number of directories to eagerly traverse on the first glob for a given package, in
   * order to warm the filesystem. -1 means do no eager traversal. See {@link
   * com.google.devtools.build.lib.pkgcache.PackageOptions#maxDirectoriesToEagerlyVisitInGlobbing}.
   * -2 means do the eager traversal using the regular globbing infrastructure, i.e. sharing the
   * globbing threads and caching the actual glob results.
   */
  public void setMaxDirectoriesToEagerlyVisitInGlobbing(
      int maxDirectoriesToEagerlyVisitInGlobbing) {
    this.maxDirectoriesToEagerlyVisitInGlobbing = maxDirectoriesToEagerlyVisitInGlobbing;
  }

  /** Returns the immutable, unordered set of names of all the known rule classes. */
  public Set<String> getRuleClassNames() {
    return ruleFactory.getRuleClassNames();
  }

  /**
   * Returns the {@link com.google.devtools.build.lib.packages.RuleClass} for the specified rule
   * class name.
   */
  public RuleClass getRuleClass(String ruleClassName) {
    return ruleFactory.getRuleClass(ruleClassName);
  }

  /** Returns the {@link RuleClassProvider} of this {@link PackageFactory}. */
  public RuleClassProvider getRuleClassProvider() {
    return ruleClassProvider;
  }

  public ImmutableList<EnvironmentExtension> getEnvironmentExtensions() {
    return environmentExtensions;
  }

  /**
   * Returns the contents of the "native" object for BUILD-loaded bzls, not accounting for builtins
   * injection.
   */
  public ImmutableMap<String, Object> getUninjectedBuildBzlNativeBindings() {
    return uninjectedBuildBzlNativeBindings;
  }

  /** Returns the contents of the "native" object for WORKSPACE-loaded bzls. */
  public ImmutableMap<String, Object> getWorkspaceBzlNativeBindings() {
    return workspaceBzlNativeBindings;
  }

  /**
   * Returns the original environment for BUILD-loaded bzl files, not accounting for builtins
   * injection.
   *
   * <p>The post-injection environment may differ from this one by what symbols a name is bound to,
   * but the set of symbols remains the same.
   */
  public ImmutableMap<String, Object> getUninjectedBuildBzlEnv() {
    return uninjectedBuildBzlEnv;
  }

  /** Returns the environment for WORKSPACE-loaded bzl files. */
  public ImmutableMap<String, Object> getWorkspaceBzlEnv() {
    return workspaceBzlEnv;
  }

  /** Returns the environment for bzl files in the {@code @builtins} pseudo-repository. */
  public ImmutableMap<String, Object> getBuiltinsBzlEnv() {
    return builtinsBzlEnv;
  }

  /** Creates the map of arguments for the 'package' function. */
  private ImmutableMap<String, PackageArgument<?>> createPackageArguments() {
    ImmutableList.Builder<PackageArgument<?>> arguments =
        ImmutableList.<PackageArgument<?>>builder().addAll(DefaultPackageArguments.get());

    for (EnvironmentExtension extension : environmentExtensions) {
      arguments.addAll(extension.getPackageArguments());
    }

    ImmutableMap.Builder<String, PackageArgument<?>> packageArguments = ImmutableMap.builder();
    for (PackageArgument<?> argument : arguments.build()) {
      packageArguments.put(argument.getName(), argument);
    }
    return packageArguments.build();
  }

  /** Returns a function-value implementing "package" in the specified package context. */
  // TODO(cparsons): Migrate this function to be defined with @StarlarkMethod.
  // TODO(adonovan): don't call this function twice (once for BUILD files and
  // once for the native module) as it results in distinct objects. (Using
  // @StarlarkMethod may accomplish that.)
  private static StarlarkCallable newPackageFunction(
      final ImmutableMap<String, PackageArgument<?>> packageArguments) {
    return new StarlarkCallable() {
      @Override
      public String getName() {
        return "package";
      }

      @Override
      public String toString() {
        return "package(...)";
      }

      @Override
      public boolean isImmutable() {
        return true;
      }

      @Override
      public void repr(Printer printer) {
        printer.append("<built-in function package>");
      }

      @Override
      public Object call(StarlarkThread thread, Tuple<Object> args, Dict<String, Object> kwargs)
          throws EvalException {
        if (!args.isEmpty()) {
          throw new EvalException("unexpected positional arguments");
        }
        Package.Builder pkgBuilder = getContext(thread).pkgBuilder;

        // Validate parameter list
        if (pkgBuilder.isPackageFunctionUsed()) {
          throw new EvalException("'package' can only be used once per BUILD file");
        }
        pkgBuilder.setPackageFunctionUsed();

        // Each supplied argument must name a PackageArgument.
        if (kwargs.isEmpty()) {
          throw new EvalException("at least one argument must be given to the 'package' function");
        }
        Location loc = thread.getCallerLocation();
        for (Map.Entry<String, Object> kwarg : kwargs.entrySet()) {
          String name = kwarg.getKey();
          PackageArgument<?> pkgarg = packageArguments.get(name);
          if (pkgarg == null) {
            throw Starlark.errorf("unexpected keyword argument: %s", name);
          }
          pkgarg.convertAndProcess(pkgBuilder, loc, kwarg.getValue());
        }
        return Starlark.NONE;
      }
    };
  }

  /** Get the PackageContext by looking up in the environment. */
  public static PackageContext getContext(StarlarkThread thread) throws EvalException {
    PackageContext value = thread.getThreadLocal(PackageContext.class);
    if (value == null) {
      // if PackageContext is missing, we're not called from a BUILD file. This happens if someone
      // uses native.some_func() in the wrong place.
      throw Starlark.errorf(
          "The native module can be accessed only from a BUILD thread. "
              + "Wrap the function in a macro and call it from a BUILD file");
    }
    return value;
  }

  private static ImmutableMap<String, BuiltinRuleFunction> buildRuleFunctions(
      RuleFactory ruleFactory) {
    ImmutableMap.Builder<String, BuiltinRuleFunction> result = ImmutableMap.builder();
    for (String ruleClassName : ruleFactory.getRuleClassNames()) {
      RuleClass cl = ruleFactory.getRuleClass(ruleClassName);
      if (cl.getRuleClassType() == RuleClassType.NORMAL
          || cl.getRuleClassType() == RuleClassType.TEST) {
        result.put(ruleClassName, new BuiltinRuleFunction(cl));
      }
    }
    return result.build();
  }

  /** A callable Starlark value that creates Rules for native RuleClasses. */
  // TODO(adonovan): why is this distinct from RuleClass itself?
  // Make RuleClass implement StarlarkCallable directly.
  private static class BuiltinRuleFunction implements StarlarkCallable, RuleFunction {
    private final RuleClass ruleClass;

    BuiltinRuleFunction(RuleClass ruleClass) {
      this.ruleClass = Preconditions.checkNotNull(ruleClass);
    }

    @Override
    public NoneType call(StarlarkThread thread, Tuple<Object> args, Dict<String, Object> kwargs)
        throws EvalException, InterruptedException {
      if (!args.isEmpty()) {
        throw Starlark.errorf("unexpected positional arguments");
      }
      BazelStarlarkContext.from(thread).checkLoadingOrWorkspacePhase(ruleClass.getName());
      try {
        RuleFactory.createAndAddRule(
            getContext(thread),
            ruleClass,
            new BuildLangTypedAttributeValuesMap(kwargs),
            thread.getSemantics(),
            thread.getCallStack());
      } catch (RuleFactory.InvalidRuleException | Package.NameConflictException e) {
        throw new EvalException(e);
      }
      return Starlark.NONE;
    }

    @Override
    public RuleClass getRuleClass() {
      return ruleClass;
    }

    @Override
    public String getName() {
      return ruleClass.getName();
    }

    @Override
    public void repr(Printer printer) {
      printer.append("<built-in rule " + getName() + ">");
    }

    @Override
    public String toString() {
      return getName() + "(...)";
    }

    @Override
    public boolean isImmutable() {
      return true;
    }
  }

  /**
   * Creates and populates a Package.Builder by executing the specified BUILD file.
   *
   * <p>This is the sole entrypoint for package creation in production and tests. Do not add others!
   * It is exposed for the benefit of skyframe.PackageFunction, which is logically part of the
   * loading phase, and should in due course be moved to lib.packages, but that cannot happen until
   * skyframe's core interfaces have been separated.
   *
   * <p>Do not call it from elsewhere! It is not in any meaningful sense a public API. In tests, use
   * BuildViewTestCase or PackageLoadingTestCase instead. TODO(adonovan): move PackageFunction into
   * this package and develop a rational API.
   */
  public Package.Builder createPackageFromAst(
      String workspaceName,
      ImmutableMap<RepositoryName, RepositoryName> repositoryMapping,
      PackageIdentifier packageId,
      RootedPath buildFile,
      StarlarkFile file, // becomes resolved as a side effect
      @Nullable Module preludeModule,
      ImmutableMap<String, Module> loadedModules,
      RuleVisibility defaultVisibility,
      StarlarkSemantics starlarkSemantics,
      Globber globber)
      throws InterruptedException {
    try {
      // At this point the package is guaranteed to exist,
      // though it may have parse or evaluation errors.
      return evaluateBuildFile(
          workspaceName,
          packageId,
          file,
          buildFile,
          globber,
          defaultVisibility,
          starlarkSemantics,
          preludeModule,
          loadedModules,
          repositoryMapping);
    } catch (InterruptedException e) {
      globber.onInterrupt();
      throw e;
    } finally {
      globber.onCompletion();
    }
  }

  @VisibleForTesting // exposed to WorkspaceFileFunction
  public Package.Builder newExternalPackageBuilder(
      RootedPath workspacePath, String workspaceName, StarlarkSemantics starlarkSemantics) {
    return Package.newExternalPackageBuilder(
        packageSettings, workspacePath, workspaceName, starlarkSemantics);
  }

  // Do not make this public!
  // TODO(adonovan): refactor Rule{Class,Factory}Test not to need this.
  Package.Builder newPackageBuilder(
      PackageIdentifier packageId, String workspaceName, StarlarkSemantics starlarkSemantics) {
    return new Package.Builder(
        packageSettings,
        packageId,
        workspaceName,
        starlarkSemantics.getBool(BuildLanguageOptions.INCOMPATIBLE_NO_IMPLICIT_FILE_EXPORT),
        Package.Builder.EMPTY_REPOSITORY_MAPPING);
  }

  /** Returns a new {@link LegacyGlobber}. */
  // Exposed to skyframe.PackageFunction.
  public LegacyGlobber createLegacyGlobber(
      Path packageDirectory,
      PackageIdentifier packageId,
      ImmutableSet<PathFragment> ignoredGlobPrefixes,
      CachingPackageLocator locator) {
    return new LegacyGlobber(
        new GlobCache(
            packageDirectory,
            packageId,
            ignoredGlobPrefixes,
            locator,
            syscalls,
            executor,
            maxDirectoriesToEagerlyVisitInGlobbing));
  }

  /**
   * This class holds state associated with the construction of a single package for the duration of
   * execution of one BUILD file. (We use a PackageContext object in preference to storing these
   * values in mutable fields of the PackageFactory.)
   *
   * <p>PLEASE NOTE: the PackageContext is referred to by the StarlarkThread, but should become
   * unreachable once the StarlarkThread is discarded at the end of evaluation. Please be aware of
   * your memory footprint when making changes here!
   */
  // TODO(adonovan): is there any reason not to merge this with Package.Builder?
  public static class PackageContext {
    final Package.Builder pkgBuilder;
    final Globber globber;
    final ExtendedEventHandler eventHandler;

    @VisibleForTesting
    public PackageContext(
        Package.Builder pkgBuilder, Globber globber, ExtendedEventHandler eventHandler) {
      this.pkgBuilder = pkgBuilder;
      this.eventHandler = eventHandler;
      this.globber = globber;
    }

    /** Returns the Label of this Package's BUILD file. */
    public Label getLabel() {
      return pkgBuilder.getBuildFileLabel();
    }

    /** Sets a Make variable. */
    public void setMakeVariable(String name, String value) {
      pkgBuilder.setMakeVariable(name, value);
    }

    /** Returns the builder of this Package. */
    public Package.Builder getBuilder() {
      return pkgBuilder;
    }
  }

  /**
   * Produces everything that would be in the "native" object for BUILD-loaded bzl files if builtins
   * injection didn't happen.
   */
  private static ImmutableMap<String, Object> createUninjectedBuildBzlNativeBindings(
      ImmutableMap<String, BuiltinRuleFunction> ruleFunctions,
      ImmutableMap<String, PackageArgument<?>> packageArguments,
      ImmutableList<EnvironmentExtension> environmentExtensions) {
    ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
    builder.putAll(StarlarkNativeModule.BINDINGS_FOR_BUILD_FILES);
    builder.putAll(ruleFunctions);
    builder.put("package", newPackageFunction(packageArguments));
    for (EnvironmentExtension ext : environmentExtensions) {
      ext.updateNative(builder);
    }
    return builder.build();
  }

  /** Produces everything in the "native" object for WORKSPACE-loaded bzl files. */
  private static ImmutableMap<String, Object> createWorkspaceBzlNativeBindings(
      RuleClassProvider ruleClassProvider, String version) {
    return WorkspaceFactory.createNativeModuleBindings(ruleClassProvider, version);
  }

  private static ImmutableMap<String, Object> createUninjectedBuildBzlEnv(
      RuleClassProvider ruleClassProvider,
      ImmutableMap<String, Object> uninjectedBuildBzlNativeBindings) {
    Map<String, Object> env = new HashMap<>();
    env.putAll(ruleClassProvider.getEnvironment());

    // Determine the "native" module.
    // TODO(#11954): Use the same "native" object for both BUILD- and WORKSPACE-loaded .bzls, and
    // just have it be a dynamic error to call the wrong thing at the wrong time. This is a breaking
    // change.
    env.put("native", createNativeModule(uninjectedBuildBzlNativeBindings));

    return ImmutableMap.copyOf(env);
  }

  private static ImmutableMap<String, Object> createWorkspaceBzlEnv(
      RuleClassProvider ruleClassProvider,
      ImmutableMap<String, Object> workspaceBzlNativeBindings) {
    Map<String, Object> env = new HashMap<>();
    env.putAll(ruleClassProvider.getEnvironment());

    // See above comments for native in BUILD bzls.
    env.put("native", createNativeModule(workspaceBzlNativeBindings));

    return ImmutableMap.copyOf(env);
  }

  private static ImmutableMap<String, Object> createBuiltinsBzlEnv(
      RuleClassProvider ruleClassProvider) {
    Map<String, Object> env = new HashMap<>();
    env.putAll(ruleClassProvider.getEnvironment());

    // Clear out rule-specific symbols like CcInfo.
    env.keySet().removeAll(ruleClassProvider.getNativeRuleSpecificBindings().keySet());

    // TODO(#11437): To support inspection of StarlarkSemantics via _internal, we'll have to let
    // this method be parameterized by the StarlarkSemantics, which means it'll need to be computed
    // on the fly and not initialized on PackageFactory construction. To avoid computing it
    // redundantly for each builtins bzl evaluation, we can either 1) create a second
    // StarlarkBuiltinsValue-like object (which sounds like a lot of work), or 2) create a cache
    // from StarlarkSemantics to builtins predeclared envs (sounds preferable to me).
    env.put("_internal", InternalModule.INSTANCE);

    return ImmutableMap.copyOf(env);
  }

  /** Constructs a "native" module object with the given contents. */
  private static Object createNativeModule(Map<String, Object> bindings) {
    return StructProvider.STRUCT.create(bindings, "no native function or rule '%s'");
  }

  /** Indicates a problem performing builtins injection. */
  public static final class InjectionException extends Exception {
    public InjectionException(String message) {
      super(message);
    }
  }

  /**
   * Constructs an environment for a BUILD-loaded bzl file based on the default environment as well
   * as the given injected top-level symbols and "native" bindings.
   *
   * <p>Injected symbols must override an existing symbol of that name. Furthermore, the overridden
   * symbol must be a rule or a piece of a specific ruleset's logic (e.g., {@code CcInfo} or {@code
   * cc_library}), not a generic built-in (e.g., {@code provider} or {@code glob}). Throws
   * InjectionException if these conditions are not met.
   */
  public ImmutableMap<String, Object> createBuildBzlEnvUsingInjection(
      ImmutableMap<String, Object> injectedToplevels, ImmutableMap<String, Object> injectedRules)
      throws InjectionException {
    // TODO(#11437): Builtins injection should take into account StarlarkSemantics and
    // FlagGuardedValues. If a builtin is disabled by a flag, we can either:
    //
    //   1) Treat it as if it doesn't exist for the purposes of injection. In this case it's an
    //      error to attempt to inject it, so exports.bzl is required to explicitly check the flag's
    //      value (via the _internal module) before exporting it.
    //
    //   2) Allow it to be exported and automatically suppress/omit it from the final environment,
    //      effectively rewrapping the injected builtin in the FlagGuardedValue.

    // Determine top-level symbols.
    Map<String, Object> env = new HashMap<>();
    env.putAll(uninjectedBuildBzlEnv);
    for (Map.Entry<String, Object> symbol : injectedToplevels.entrySet()) {
      String name = symbol.getKey();
      if (!env.containsKey(name) && !Starlark.UNIVERSE.containsKey(name)) {
        throw new InjectionException(
            String.format(
                "Injected top-level symbol '%s' must override an existing symbol by that name",
                name));
      } else if (!ruleClassProvider.getNativeRuleSpecificBindings().containsKey(name)) {
        throw new InjectionException(
            String.format("Cannot override top-level builtin '%s' with an injected value", name));
      } else {
        env.put(name, symbol.getValue());
      }
    }

    // Determine "native" bindings.
    // See above comments for native in BUILD bzls.
    Map<String, Object> nativeBindings = new HashMap<>();
    nativeBindings.putAll(uninjectedBuildBzlNativeBindings);
    for (Map.Entry<String, Object> symbol : injectedRules.entrySet()) {
      String name = symbol.getKey();
      Object preexisting = nativeBindings.put(name, symbol.getValue());
      if (preexisting == null) {
        throw new InjectionException(
            String.format(
                "Injected native module field '%s' must override an existing symbol by that name",
                name));
      } else if (!ruleFunctions.containsKey(name)) {
        throw new InjectionException(
            String.format("Cannot override native module field '%s' with an injected value", name));
      }
    }

    env.put("native", createNativeModule(nativeBindings));
    return ImmutableMap.copyOf(env);
  }

  private void populateEnvironment(ImmutableMap.Builder<String, Object> env) {
    env.putAll(StarlarkLibrary.BUILD); // e.g. rule, select, depset
    env.putAll(StarlarkNativeModule.BINDINGS_FOR_BUILD_FILES);
    env.put("package", newPackageFunction(packageArguments));
    env.putAll(ruleFunctions);

    for (EnvironmentExtension ext : environmentExtensions) {
      ext.update(env);
    }
  }

  /**
   * Runs final validation and administrative tasks on newly loaded package. Called by a caller of
   * {@link #createPackageFromAst} after this caller has fully loaded the package.
   *
   * @throws InvalidPackageException if the package is determined to be invalid
   */
  public void afterDoneLoadingPackage(
      Package pkg,
      StarlarkSemantics starlarkSemantics,
      long loadTimeNanos,
      ExtendedEventHandler eventHandler)
      throws InvalidPackageException {
    packageValidator.validate(pkg, eventHandler);

    // Enforce limit on number of compute steps in BUILD file (b/151622307).
    long maxSteps = starlarkSemantics.get(BuildLanguageOptions.MAX_COMPUTATION_STEPS);
    long steps = pkg.getComputationSteps();
    if (maxSteps > 0 && steps > maxSteps) {
      String message =
          String.format(
              "BUILD file computation took %d steps, but --max_computation_steps=%d",
              steps, maxSteps);
      throw new InvalidPackageException(
          pkg.getPackageIdentifier(),
          message,
          DetailedExitCode.of(
              FailureDetail.newBuilder()
                  .setMessage(message)
                  .setPackageLoading(
                      PackageLoading.newBuilder()
                          .setCode(PackageLoading.Code.MAX_COMPUTATION_STEPS_EXCEEDED)
                          .build())
                  .build()));
    }

    packageLoadingListener.onLoadingCompleteAndSuccessful(pkg, starlarkSemantics, loadTimeNanos);
  }

  /**
   * Constructs a Package instance, evaluates the BUILD-file AST inside the build environment, and
   * populates the package with Rule instances as it goes. As with most programming languages,
   * evaluation stops when an exception is encountered: no further rules after the point of failure
   * will be constructed. We assume that rules constructed before the point of failure are valid;
   * this assumption is not entirely correct, since a "vardef" after a rule declaration can affect
   * the behavior of that rule.
   *
   * <p>Rule attribute checking is performed during evaluation. Each attribute must conform to the
   * type specified for that <i>(rule class, attribute name)</i> pair. Errors reported at this stage
   * include: missing value for mandatory attribute, value of wrong type. Such error cause Rule
   * construction to be aborted, so the resulting package will have missing members.
   *
   * @see PackageFactory#PackageFactory
   */
  // Used by PackageFactoryApparatus. DO NOT make this public!
  Package.Builder evaluateBuildFile(
      String workspaceName,
      PackageIdentifier packageId,
      StarlarkFile file, // becomes resolved as a side effect
      RootedPath buildFilePath,
      Globber globber,
      RuleVisibility defaultVisibility,
      StarlarkSemantics semantics,
      @Nullable Module preludeModule,
      ImmutableMap<String, Module> loadedModules,
      ImmutableMap<RepositoryName, RepositoryName> repositoryMapping)
      throws InterruptedException {
    Package.Builder pkgBuilder =
        new Package.Builder(
                packageSettings,
                packageId,
                workspaceName,
                semantics.getBool(BuildLanguageOptions.INCOMPATIBLE_NO_IMPLICIT_FILE_EXPORT),
                repositoryMapping)
            .setFilename(buildFilePath)
            .setDefaultVisibility(defaultVisibility)
            // "defaultVisibility" comes from the command line.
            // Let's give the BUILD file a chance to set default_visibility once,
            // by resetting the PackageBuilder.defaultVisibilitySet flag.
            .setDefaultVisibilitySet(false)
            // TODO(adonovan): opt: don't precompute this value, which is rarely needed
            // and can be derived from Package.loads (if available) on demand.
            .setStarlarkFileDependencies(transitiveClosureOfLabels(loadedModules))
            .setThirdPartyLicenceExistencePolicy(
                ruleClassProvider.getThirdPartyLicenseExistencePolicy());
    if (packageSettings.recordLoadedModules()) {
      pkgBuilder.setLoads(loadedModules);
    }

    StoredEventHandler eventHandler = new StoredEventHandler();
    if (!buildPackage(
        pkgBuilder,
        packageId,
        file,
        semantics,
        preludeModule,
        loadedModules,
        new PackageContext(pkgBuilder, globber, eventHandler))) {
      pkgBuilder.setContainsErrors();
    }
    pkgBuilder.addPosts(eventHandler.getPosts());
    pkgBuilder.addEvents(eventHandler.getEvents());
    return pkgBuilder;
  }

  private static ImmutableList<Label> transitiveClosureOfLabels(
      ImmutableMap<String, Module> loads) {
    Set<Label> set = Sets.newLinkedHashSet();
    transitiveClosureOfLabelsRec(set, loads);
    return ImmutableList.copyOf(set);
  }

  private static void transitiveClosureOfLabelsRec(
      Set<Label> set, ImmutableMap<String, Module> loads) {
    for (Module m : loads.values()) {
      BazelModuleContext ctx = BazelModuleContext.of(m);
      if (set.add(ctx.label())) {
        transitiveClosureOfLabelsRec(set, ctx.loads());
      }
    }
  }

  // Validates and executes a parsed BUILD file, returning true on success,
  // or reporting errors to pkgContext.eventHandler on failure.
  private boolean buildPackage(
      Package.Builder pkgBuilder,
      PackageIdentifier packageId,
      StarlarkFile file, // becomes resolved as a side effect
      StarlarkSemantics semantics,
      @Nullable Module preludeModule,
      ImmutableMap<String, Module> loadedModules,
      PackageContext pkgContext)
      throws InterruptedException {

    // Report scan/parse errors.
    if (!file.ok()) {
      Event.replayEventsOn(
          pkgContext.eventHandler,
          file.errors(),
          DetailedExitCode.class,
          syntaxError -> Package.createDetailedCode(syntaxError.toString(), Code.SYNTAX_ERROR));
      return false;
    }

    // Validate the package identifier.
    // TODO(adonovan): it's kinda late to be doing this check.
    // after we've parsed the BUILD file and created the Package.
    String error = LabelValidator.validatePackageName(packageId.getPackageFragment().toString());
    if (error != null) {
      pkgContext.eventHandler.handle(
          Package.error(file.getStartLocation(), error, Code.PACKAGE_NAME_INVALID));
      return false;
    }

    // Construct environment.
    // TODO(bazel-team): Have populateEnvironment accept a Map rather than an ImmutableMap.Builder,
    // so we're not forced to create both a builder and map here.
    ImmutableMap.Builder<String, Object> predeclared = ImmutableMap.builder();
    populateEnvironment(predeclared);
    HashMap<String, Object> predeclaredWithPrelude = new HashMap<>();
    predeclaredWithPrelude.putAll(predeclared.build());
    if (preludeModule != null) {
      predeclaredWithPrelude.putAll(preludeModule.getGlobals());
    }
    Module module = Module.withPredeclared(semantics, predeclaredWithPrelude);

    // resolve & compile
    // TODO(adonovan): this mutates the StarlarkFile, which may be shared in the fileSyntaxCache.
    Program prog;
    try {
      prog = Program.compileFile(file, module);
    } catch (SyntaxError.Exception ex) {
      Event.replayEventsOn(
          pkgContext.eventHandler,
          ex.errors(),
          DetailedExitCode.class,
          syntaxError -> Package.createDetailedCode(syntaxError.toString(), Code.SYNTAX_ERROR));
      return false;
    }

    // Check syntax. Make a pass over the syntax tree to:
    // - reject forbidden BUILD syntax
    // - extract literal glob patterns for prefetching
    // - record the generator_name of each top-level macro call
    Set<String> globs = new HashSet<>();
    Set<String> globsWithDirs = new HashSet<>();
    if (!checkBuildSyntax(
        file,
        globs,
        globsWithDirs,
        pkgBuilder.getGeneratorNameByLocation(),
        pkgContext.eventHandler)) {
      return false;
    }

    // Prefetch glob patterns asynchronously.
    if (maxDirectoriesToEagerlyVisitInGlobbing == -2) {
      try {
        pkgContext.globber.runAsync(
            ImmutableList.copyOf(globs),
            ImmutableList.of(),
            /*excludeDirs=*/ true,
            /*allowEmpty=*/ true);
        pkgContext.globber.runAsync(
            ImmutableList.copyOf(globsWithDirs),
            ImmutableList.of(),
            /*excludeDirs=*/ false,
            /*allowEmpty=*/ true);
      } catch (BadGlobException ex) {
        // Ignore exceptions.
        // Errors will be properly reported when the actual globbing is done.
      }
    }

    try (Mutability mu = Mutability.create("package", packageId)) {
      StarlarkThread thread = new StarlarkThread(mu, semantics);
      thread.setLoader(loadedModules::get);
      thread.setPrintHandler(Event.makeDebugPrintHandler(pkgContext.eventHandler));

      new BazelStarlarkContext(
              BazelStarlarkContext.Phase.LOADING,
              ruleClassProvider.getToolsRepository(),
              /*fragmentNameToClass=*/ null,
              pkgBuilder.getRepositoryMapping(),
              new SymbolGenerator<>(packageId),
              /*analysisRuleLabel=*/ null)
          .storeInThread(thread);

      // TODO(adonovan): save this as a field in BazelStarlarkContext.
      // It needn't be a second thread-local.
      thread.setThreadLocal(PackageContext.class, pkgContext);

      // Execute.
      try {
        Starlark.execFileProgram(prog, module, thread);
      } catch (EvalException ex) {
        pkgContext.eventHandler.handle(
            Package.error(null, ex.getMessageWithStack(), Code.STARLARK_EVAL_ERROR));
        return false;
      }

      pkgBuilder.setComputationSteps(thread.getExecutedSteps());
    }

    return true; // success
  }

  /**
   * checkBuildSyntax is a static pass over the syntax tree of a BUILD (not .bzl) file.
   *
   * <p>It reports an error to the event handler if it discovers a {@code def}, {@code if}, or
   * {@code for} statement, or a {@code f(*args)} or {@code f(**kwargs)} call.
   *
   * <p>It extracts literal {@code glob(include="pattern")} patterns and adds them to {@code globs},
   * or to {@code globsWithDirs} if the call had a {@code exclude_directories=0} argument.
   *
   * <p>It records in {@code generatorNameByLocation} all calls of the form {@code f(name="foo",
   * ...)} so that any rules instantiated during the call to {@code f} can be ascribed a "generator
   * name" of {@code "foo"}.
   *
   * <p>It returns true if it reported no errors.
   */
  // TODO(adonovan): restructure so that this is called from the sole place that executes BUILD
  // files. Also, make private; there's reason for tests to call this directly.
  public static boolean checkBuildSyntax(
      StarlarkFile file,
      Collection<String> globs,
      Collection<String> globsWithDirs,
      Map<Location, String> generatorNameByLocation,
      EventHandler eventHandler) {
    final boolean[] success = {true};
    NodeVisitor checker =
        new NodeVisitor() {
          void error(Location loc, String message) {
            eventHandler.handle(Package.error(loc, message, Code.SYNTAX_ERROR));
            success[0] = false;
          }

          // Extract literal glob patterns from calls of the form:
          //   glob(include = ["pattern"])
          //   glob(["pattern"])
          // This may spuriously match user-defined functions named glob;
          // that's ok, it's only a heuristic.
          void extractGlobPatterns(CallExpression call) {
            if (call.getFunction() instanceof Identifier
                && ((Identifier) call.getFunction()).getName().equals("glob")) {
              Expression excludeDirectories = null, include = null;
              List<Argument> arguments = call.getArguments();
              for (int i = 0; i < arguments.size(); i++) {
                Argument arg = arguments.get(i);
                String name = arg.getName();
                if (name == null) {
                  if (i == 0) { // first positional argument
                    include = arg.getValue();
                  }
                } else if (name.equals("include")) {
                  include = arg.getValue();
                } else if (name.equals("exclude_directories")) {
                  excludeDirectories = arg.getValue();
                }
              }
              if (include instanceof ListExpression) {
                for (Expression elem : ((ListExpression) include).getElements()) {
                  if (elem instanceof StringLiteral) {
                    String pattern = ((StringLiteral) elem).getValue();
                    // exclude_directories is (oddly) an int with default 1.
                    boolean exclude = true;
                    if (excludeDirectories instanceof IntLiteral) {
                      Number v = ((IntLiteral) excludeDirectories).getValue();
                      if (v instanceof Integer && (Integer) v == 0) {
                        exclude = false;
                      }
                    }
                    (exclude ? globs : globsWithDirs).add(pattern);
                  }
                }
              }
            }
          }

          // Reject f(*args) and f(**kwargs) calls in BUILD files.
          void rejectStarArgs(CallExpression call) {
            for (Argument arg : call.getArguments()) {
              if (arg instanceof Argument.StarStar) {
                error(
                    arg.getStartLocation(),
                    "**kwargs arguments are not allowed in BUILD files. Pass the arguments in "
                        + "explicitly.");
              } else if (arg instanceof Argument.Star) {
                error(
                    arg.getStartLocation(),
                    "*args arguments are not allowed in BUILD files. Pass the arguments in "
                        + "explicitly.");
              }
            }
          }

          // Record calls of the form f(name="foo", ...)
          // so that we can later ascribe "foo" as the "generator name"
          // of any rules instantiated during the call of f.
          void recordGeneratorName(CallExpression call) {
            for (Argument arg : call.getArguments()) {
              if (arg instanceof Argument.Keyword
                  && arg.getName().equals("name")
                  && arg.getValue() instanceof StringLiteral) {
                generatorNameByLocation.put(
                    call.getLparenLocation(), ((StringLiteral) arg.getValue()).getValue());
              }
            }
          }

          // We prune the traversal if we encounter def/if/for,
          // as we have already reported the root error and there's
          // no point reporting more.

          @Override
          public void visit(DefStatement node) {
            error(
                node.getStartLocation(),
                "function definitions are not allowed in BUILD files. You may move the function to "
                    + "a .bzl file and load it.");
          }

          @Override
          public void visit(ForStatement node) {
            error(
                node.getStartLocation(),
                "for statements are not allowed in BUILD files. You may inline the loop, move it "
                    + "to a function definition (in a .bzl file), or as a last resort use a list "
                    + "comprehension.");
          }

          @Override
          public void visit(IfStatement node) {
            error(
                node.getStartLocation(),
                "if statements are not allowed in BUILD files. You may move conditional logic to a "
                    + "function definition (in a .bzl file), or for simple cases use an if "
                    + "expression.");
          }

          @Override
          public void visit(CallExpression node) {
            extractGlobPatterns(node);
            rejectStarArgs(node);
            recordGeneratorName(node);
            // Continue traversal so as not to miss nested calls
            // like cc_binary(..., f(**kwargs), srcs=glob(...), ...).
            super.visit(node);
          }
        };
    checker.visit(file);
    return success[0];
  }

  // Install profiler hooks into Starlark interpreter.
  static {
    // parser profiler
    StarlarkFile.setParseProfiler(
        new StarlarkFile.ParseProfiler() {
          @Override
          public Object start(String filename) {
            return Profiler.instance().profile(ProfilerTask.STARLARK_PARSER, filename);
          }

          @Override
          public void end(Object span) {
            ((SilentCloseable) span).close();
          }
        });

    // call profiler
    StarlarkThread.setCallProfiler(
        new StarlarkThread.CallProfiler() {
          @Override
          public Object start(StarlarkCallable fn) {
            return Profiler.instance()
                .profile(
                    fn instanceof StarlarkFunction
                        ? ProfilerTask.STARLARK_USER_FN
                        : ProfilerTask.STARLARK_BUILTIN_FN,
                    fn.getName());
          }

          @Override
          public void end(Object span) {
            ((SilentCloseable) span).close();
          }
        });
  }
}
