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
package com.google.devtools.build.lib.skyframe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.flogger.GoogleLogger;
import com.google.devtools.build.lib.actions.Actions;
import com.google.devtools.build.lib.actions.Actions.GeneratingActions;
import com.google.devtools.build.lib.actions.MutableActionGraph.ActionConflictException;
import com.google.devtools.build.lib.analysis.AnalysisRootCauseEvent;
import com.google.devtools.build.lib.analysis.AspectResolver;
import com.google.devtools.build.lib.analysis.CachingAnalysisEnvironment;
import com.google.devtools.build.lib.analysis.CachingAnalysisEnvironment.MissingDepException;
import com.google.devtools.build.lib.analysis.ConfiguredAspect;
import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.Dependency;
import com.google.devtools.build.lib.analysis.DependencyKey;
import com.google.devtools.build.lib.analysis.DependencyKind;
import com.google.devtools.build.lib.analysis.DependencyResolver;
import com.google.devtools.build.lib.analysis.DuplicateException;
import com.google.devtools.build.lib.analysis.EmptyConfiguredTarget;
import com.google.devtools.build.lib.analysis.InconsistentAspectOrderException;
import com.google.devtools.build.lib.analysis.PlatformConfiguration;
import com.google.devtools.build.lib.analysis.ResolvedToolchainContext;
import com.google.devtools.build.lib.analysis.RuleContext.InvalidExecGroupException;
import com.google.devtools.build.lib.analysis.TargetAndConfiguration;
import com.google.devtools.build.lib.analysis.ToolchainCollection;
import com.google.devtools.build.lib.analysis.ToolchainContext;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.analysis.config.BuildOptions;
import com.google.devtools.build.lib.analysis.config.BuildOptionsView;
import com.google.devtools.build.lib.analysis.config.ConfigMatchingProvider;
import com.google.devtools.build.lib.analysis.config.ConfigurationResolver;
import com.google.devtools.build.lib.analysis.config.DependencyEvaluationException;
import com.google.devtools.build.lib.analysis.config.InvalidConfigurationException;
import com.google.devtools.build.lib.analysis.config.transitions.PatchTransition;
import com.google.devtools.build.lib.analysis.configuredtargets.RuleConfiguredTarget;
import com.google.devtools.build.lib.analysis.platform.ConstraintValueInfo;
import com.google.devtools.build.lib.analysis.platform.PlatformInfo;
import com.google.devtools.build.lib.analysis.starlark.StarlarkTransition.TransitionException;
import com.google.devtools.build.lib.causes.AnalysisFailedCause;
import com.google.devtools.build.lib.causes.Cause;
import com.google.devtools.build.lib.causes.LoadingFailedCause;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.collect.nestedset.Order;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.EventKind;
import com.google.devtools.build.lib.events.StoredEventHandler;
import com.google.devtools.build.lib.packages.Aspect;
import com.google.devtools.build.lib.packages.BuildType;
import com.google.devtools.build.lib.packages.ExecGroup;
import com.google.devtools.build.lib.packages.NoSuchTargetException;
import com.google.devtools.build.lib.packages.NonconfigurableAttributeMapper;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.RawAttributeMapper;
import com.google.devtools.build.lib.packages.Rule;
import com.google.devtools.build.lib.packages.RuleClass;
import com.google.devtools.build.lib.packages.RuleClassProvider;
import com.google.devtools.build.lib.packages.Target;
import com.google.devtools.build.lib.packages.TargetUtils;
import com.google.devtools.build.lib.server.FailureDetails.Analysis;
import com.google.devtools.build.lib.server.FailureDetails.Analysis.Code;
import com.google.devtools.build.lib.server.FailureDetails.FailureDetail;
import com.google.devtools.build.lib.skyframe.SkyframeExecutor.BuildViewProvider;
import com.google.devtools.build.lib.util.DetailedExitCode;
import com.google.devtools.build.lib.util.DetailedExitCode.DetailedExitCodeComparator;
import com.google.devtools.build.lib.util.OrderedSetMultimap;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import com.google.devtools.build.skyframe.ValueOrException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;

/**
 * SkyFunction for {@link ConfiguredTargetValue}s.
 *
 * <p>This class, together with {@link AspectFunction} drives the analysis phase. For more
 * information, see {@link com.google.devtools.build.lib.analysis.RuleConfiguredTargetFactory}.
 *
 * @see com.google.devtools.build.lib.analysis.RuleConfiguredTargetFactory
 */
public final class ConfiguredTargetFunction implements SkyFunction {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private static final ImmutableMap<Label, ConfigMatchingProvider> NO_CONFIG_CONDITIONS =
      ImmutableMap.of();

  /**
   * Attempt to find a {@link ConfiguredValueCreationException} in a {@link ToolchainException}, or
   * its causes.
   *
   * <p>If one cannot be found, null is returned.
   */
  @Nullable
  public static ConfiguredValueCreationException asConfiguredValueCreationException(
      ToolchainException e) {
    for (Throwable cause = e.getCause();
        cause != null && cause != cause.getCause();
        cause = cause.getCause()) {
      if (cause instanceof ConfiguredValueCreationException) {
        return (ConfiguredValueCreationException) cause;
      }
    }
    return null;
  }

  private final BuildViewProvider buildViewProvider;
  private final RuleClassProvider ruleClassProvider;
  private final Semaphore cpuBoundSemaphore;
  private final BuildOptions defaultBuildOptions;
  @Nullable private final ConfiguredTargetProgressReceiver configuredTargetProgress;

  /**
   * Indicates whether the set of packages transitively loaded for a given {@link
   * ConfiguredTargetValue} will be needed for package root resolution later in the build. If not,
   * they are not collected and stored.
   */
  private final boolean storeTransitivePackagesForPackageRootResolution;

  private final boolean shouldUnblockCpuWorkWhenFetchingDeps;

  ConfiguredTargetFunction(
      BuildViewProvider buildViewProvider,
      RuleClassProvider ruleClassProvider,
      Semaphore cpuBoundSemaphore,
      boolean storeTransitivePackagesForPackageRootResolution,
      boolean shouldUnblockCpuWorkWhenFetchingDeps,
      BuildOptions defaultBuildOptions,
      @Nullable ConfiguredTargetProgressReceiver configuredTargetProgress) {
    this.buildViewProvider = buildViewProvider;
    this.ruleClassProvider = ruleClassProvider;
    this.cpuBoundSemaphore = cpuBoundSemaphore;
    this.storeTransitivePackagesForPackageRootResolution =
        storeTransitivePackagesForPackageRootResolution;
    this.shouldUnblockCpuWorkWhenFetchingDeps = shouldUnblockCpuWorkWhenFetchingDeps;
    this.defaultBuildOptions = defaultBuildOptions;
    this.configuredTargetProgress = configuredTargetProgress;
  }

  private void acquireWithLogging(SkyKey key) throws InterruptedException {
    Stopwatch stopwatch = Stopwatch.createStarted();
    cpuBoundSemaphore.acquire();
    long elapsedTime = stopwatch.elapsed().toMillis();
    if (elapsedTime > 5) {
      logger.atInfo().atMostEvery(10, TimeUnit.SECONDS).log(
          "Spent %s milliseconds waiting for lock acquisition for %s", elapsedTime, key);
    }
  }

  @Override
  public SkyValue compute(SkyKey key, Environment env) throws ConfiguredTargetFunctionException,
      InterruptedException {
    if (shouldUnblockCpuWorkWhenFetchingDeps) {
      env =
          new StateInformingSkyFunctionEnvironment(
              env,
              /*preFetch=*/ cpuBoundSemaphore::release,
              /*postFetch=*/ () -> acquireWithLogging(key));
    }
    SkyframeBuildView view = buildViewProvider.getSkyframeBuildView();
    NestedSetBuilder<Package> transitivePackagesForPackageRootResolution =
        storeTransitivePackagesForPackageRootResolution ? NestedSetBuilder.stableOrder() : null;
    NestedSetBuilder<Cause> transitiveRootCauses = NestedSetBuilder.stableOrder();

    ConfiguredTargetKey configuredTargetKey = (ConfiguredTargetKey) key.argument();
    Label label = configuredTargetKey.getLabel();
    BuildConfiguration configuration = null;
    ImmutableSet<SkyKey> packageAndMaybeConfiguration;
    SkyKey packageKey = PackageValue.key(label.getPackageIdentifier());
    SkyKey configurationKeyMaybe = configuredTargetKey.getConfigurationKey();
    if (configurationKeyMaybe == null) {
      packageAndMaybeConfiguration = ImmutableSet.of(packageKey);
    } else {
      packageAndMaybeConfiguration = ImmutableSet.of(packageKey, configurationKeyMaybe);
    }
    Map<SkyKey, SkyValue> packageAndMaybeConfigurationValues =
        env.getValues(packageAndMaybeConfiguration);
    if (env.valuesMissing()) {
      return null;
    }
    PackageValue packageValue = (PackageValue) packageAndMaybeConfigurationValues.get(packageKey);
    if (configurationKeyMaybe != null) {
      configuration =
          ((BuildConfigurationValue) packageAndMaybeConfigurationValues.get(configurationKeyMaybe))
              .getConfiguration();
    }

    // TODO(ulfjack): This tries to match the logic in TransitiveTargetFunction /
    // TargetMarkerFunction. Maybe we can merge the two?
    Package pkg = packageValue.getPackage();
    Target target;
    try {
      target = pkg.getTarget(label.getName());
    } catch (NoSuchTargetException e) {
      throw new ConfiguredTargetFunctionException(
          new ConfiguredValueCreationException(
              e.getMessage(), label, configuration, e.getDetailedExitCode()));
    }
    if (pkg.containsErrors()) {
      FailureDetail failureDetail = pkg.contextualizeFailureDetailForTarget(target);
      transitiveRootCauses.add(new LoadingFailedCause(label, DetailedExitCode.of(failureDetail)));
    }
    if (transitivePackagesForPackageRootResolution != null) {
      transitivePackagesForPackageRootResolution.add(pkg);
    }
    if (target.isConfigurable() == (configuredTargetKey.getConfigurationKey() == null)) {
      // We somehow ended up in a target that requires a non-null configuration as a dependency of
      // one that requires a null configuration or the other way round. This is always an error, but
      // we need to analyze the dependencies of the latter target to realize that. Short-circuit the
      // evaluation to avoid doing useless work and running code with a null configuration that's
      // not prepared for it.
      return new NonRuleConfiguredTargetValue(
          new EmptyConfiguredTarget(target.getLabel(), configuredTargetKey.getConfigurationKey()),
          GeneratingActions.EMPTY,
          transitivePackagesForPackageRootResolution == null
              ? null
              : transitivePackagesForPackageRootResolution.build());
    }

    // This line is only needed for accurate error messaging. Say this target has a circular
    // dependency with one of its deps. With this line, loading this target fails so Bazel
    // associates the corresponding error with this target, as expected. Without this line,
    // the first TransitiveTargetValue call happens on its dep (in trimConfigurations), so Bazel
    // associates the error with the dep, which is misleading.
    if (configuration != null
        && configuration.trimConfigurations()
        && env.getValue(TransitiveTargetKey.of(label)) == null) {
      return null;
    }

    TargetAndConfiguration ctgValue = new TargetAndConfiguration(target, configuration);

    SkyframeDependencyResolver resolver = view.createDependencyResolver(env);

    ToolchainCollection<UnloadedToolchainContext> unloadedToolchainContexts = null;

    // TODO(janakr): this call may tie up this thread indefinitely, reducing the parallelism of
    //  Skyframe. This is a strict improvement over the prior state of the code, in which we ran
    //  with #processors threads, but ideally we would call #tryAcquire here, and if we failed,
    //  would exit this SkyFunction and restart it when permits were available.
    acquireWithLogging(key);
    try {
      // Determine what toolchains are needed by this target.
      unloadedToolchainContexts =
          computeUnloadedToolchainContexts(
              env,
              ruleClassProvider,
              defaultBuildOptions,
              ctgValue,
              configuredTargetKey.getToolchainContextKey());
      if (env.valuesMissing()) {
        return null;
      }

      // Get the configuration targets that trigger this rule's configurable attributes.
      ImmutableMap<Label, ConfigMatchingProvider> configConditions =
          getConfigConditions(
              env,
              ctgValue,
              transitivePackagesForPackageRootResolution,
              unloadedToolchainContexts == null
                  ? null
                  : unloadedToolchainContexts.getTargetPlatform(),
              transitiveRootCauses);
      if (env.valuesMissing()) {
        return null;
      }
      // TODO(ulfjack): ConfiguredAttributeMapper (indirectly used from computeDependencies) isn't
      // safe to use if there are missing config conditions, so we stop here, but only if there are
      // config conditions - though note that we can't check if configConditions is non-empty - it
      // may be empty for other reasons. It would be better to continue here so that we can collect
      // more root causes during computeDependencies.
      // Note that this doesn't apply to AspectFunction, because aspects can't have configurable
      // attributes.
      if (!transitiveRootCauses.isEmpty()
          && !Objects.equals(configConditions, NO_CONFIG_CONDITIONS)) {
        NestedSet<Cause> causes = transitiveRootCauses.build();
        throw new ConfiguredTargetFunctionException(
            new ConfiguredValueCreationException(
                "Cannot compute config conditions",
                configuration,
                causes,
                getPrioritizedDetailedExitCode(causes)));
      }

      // Calculate the dependencies of this target.
      OrderedSetMultimap<DependencyKind, ConfiguredTargetAndData> depValueMap =
          computeDependencies(
              env,
              resolver,
              ctgValue,
              ImmutableList.<Aspect>of(),
              configConditions,
              unloadedToolchainContexts == null
                  ? null
                  : unloadedToolchainContexts.asToolchainContexts(),
              DependencyResolver.shouldUseToolchainTransition(configuration, ctgValue.getTarget()),
              ruleClassProvider,
              view.getHostConfiguration(configuration),
              transitivePackagesForPackageRootResolution,
              transitiveRootCauses,
              defaultBuildOptions);
      if (!transitiveRootCauses.isEmpty()) {
        NestedSet<Cause> causes = transitiveRootCauses.build();
        throw new ConfiguredTargetFunctionException(
            new ConfiguredValueCreationException(
                "Analysis failed", configuration, causes, getPrioritizedDetailedExitCode(causes)));
      }
      if (env.valuesMissing()) {
        return null;
      }
      Preconditions.checkNotNull(depValueMap);

      // Load the requested toolchains into the ToolchainContext, now that we have dependencies.
      ToolchainCollection<ResolvedToolchainContext> toolchainContexts = null;
      if (unloadedToolchainContexts != null) {
        String targetDescription = target.toString();
        ToolchainCollection.Builder<ResolvedToolchainContext> contextsBuilder =
            ToolchainCollection.builder();
        for (Map.Entry<String, UnloadedToolchainContext> unloadedContext :
            unloadedToolchainContexts.getContextMap().entrySet()) {
          Set<ConfiguredTargetAndData> toolchainDependencies =
              depValueMap.get(DependencyKind.forExecGroup(unloadedContext.getKey()));
          contextsBuilder.addContext(
              unloadedContext.getKey(),
              ResolvedToolchainContext.load(
                  target.getPackage().getRepositoryMapping(),
                  unloadedContext.getValue(),
                  targetDescription,
                  toolchainDependencies));
        }
        toolchainContexts = contextsBuilder.build();
      }

      ConfiguredTargetValue ans =
          createConfiguredTarget(
              view,
              env,
              target,
              configuration,
              configuredTargetKey,
              depValueMap,
              configConditions,
              toolchainContexts,
              transitivePackagesForPackageRootResolution);
      if (configuredTargetProgress != null) {
        configuredTargetProgress.doneConfigureTarget();
      }
      return ans;
    } catch (DependencyEvaluationException e) {
      if (e.getCause() instanceof ConfiguredValueCreationException) {
        ConfiguredValueCreationException cvce = (ConfiguredValueCreationException) e.getCause();

        // Check if this is caused by an unresolved toolchain, and report it as such.
        if (unloadedToolchainContexts != null) {
          ImmutableSet<Label> requiredToolchains =
              unloadedToolchainContexts.getResolvedToolchains();
          Set<Label> toolchainDependencyErrors =
              cvce.getRootCauses().toList().stream()
                  .map(Cause::getLabel)
                  .filter(requiredToolchains::contains)
                  .collect(ImmutableSet.toImmutableSet());

          if (!toolchainDependencyErrors.isEmpty()) {
            env.getListener()
                .handle(
                    Event.error(
                        String.format(
                            "While resolving toolchains for target %s: %s",
                            target.getLabel(), e.getCause().getMessage())));
          }
        }

        throw new ConfiguredTargetFunctionException(cvce);
      } else if (e.getCause() instanceof InconsistentAspectOrderException) {
        InconsistentAspectOrderException cause = (InconsistentAspectOrderException) e.getCause();
        throw new ConfiguredTargetFunctionException(
            new ConfiguredValueCreationException(
                cause.getMessage(), target.getLabel(), configuration));
      } else if (e.getCause() instanceof InvalidConfigurationException) {
        InvalidConfigurationException cause = (InvalidConfigurationException) e.getCause();
        env.getListener().handle(Event.error(cause.getMessage()));
        throw new ConfiguredTargetFunctionException(
            new ConfiguredValueCreationException(
                cause.getMessage(), target.getLabel(), configuration, cause.getDetailedExitCode()));
      } else if (e.getCause() instanceof TransitionException) {
        TransitionException cause = (TransitionException) e.getCause();
        env.getListener().handle(Event.error(cause.getMessage()));
        throw new ConfiguredTargetFunctionException(
            new ConfiguredValueCreationException(e.getMessage(), target.getLabel(), configuration));
      } else {
        // Unknown exception type.
        throw new ConfiguredTargetFunctionException(
            new ConfiguredValueCreationException(e.getMessage(), target.getLabel(), configuration));
      }
    } catch (AspectCreationException e) {
      throw new ConfiguredTargetFunctionException(
          new ConfiguredValueCreationException(
              e.getMessage(), configuration, e.getCauses(), e.getDetailedExitCode()));
    } catch (ToolchainException e) {
      // We need to throw a ConfiguredValueCreationException, so either find one or make one.
      ConfiguredValueCreationException cvce = asConfiguredValueCreationException(e);
      if (cvce == null) {
        cvce =
            new ConfiguredValueCreationException(
                e.getMessage(), target.getLabel(), configuration, e.getDetailedExitCode());
      }

      String message =
          String.format(
              "While resolving toolchains for target %s: %s", target.getLabel(), e.getMessage());
      env.getListener().handle(Event.error(message));
      throw new ConfiguredTargetFunctionException(cvce);
    } catch (ConfiguredValueCreationException e) {
      throw new ConfiguredTargetFunctionException(e);
    } finally {
      cpuBoundSemaphore.release();
    }
  }

  /**
   * Returns the {@link UnloadedToolchainContext} for this target, or {@code null} if the target
   * doesn't use toolchains.
   *
   * <p>This involves Skyframe evaluation: callers should check {@link Environment#valuesMissing()
   * to check the result is valid.
   */
  @VisibleForTesting
  @Nullable
  static ToolchainCollection<UnloadedToolchainContext> computeUnloadedToolchainContexts(
      Environment env,
      RuleClassProvider ruleClassProvider,
      BuildOptions defaultBuildOptions,
      TargetAndConfiguration targetAndConfig,
      @Nullable ToolchainContextKey parentToolchainContextKey)
      throws InterruptedException, ToolchainException {
    if (!(targetAndConfig.getTarget() instanceof Rule)) {
      return null;
    }
    Rule rule = ((Rule) targetAndConfig.getTarget());
    if (!rule.getRuleClassObject().useToolchainResolution()) {
      return null;
    }
    BuildConfiguration configuration = targetAndConfig.getConfiguration();

    ImmutableSet<Label> requiredDefaultToolchains =
        rule.getRuleClassObject().getRequiredToolchains();
    // Collect local (target, rule) constraints for filtering out execution platforms.
    ImmutableSet<Label> defaultExecConstraintLabels =
        getExecutionPlatformConstraints(
            rule, configuration.getFragment(PlatformConfiguration.class));

    ImmutableMap<String, ExecGroup> execGroups = rule.getRuleClassObject().getExecGroups();

    // The toolchain context's options are the parent rule's options with manual trimming
    // auto-applied. This means toolchains don't inherit feature flags. This helps build
    // performance: if the toolchain context had the exact same configuration of its parent and that
    // included feature flags, all the toolchain's dependencies would apply this transition
    // individually. That creates a lot more potentially expensive applications of that transition
    // (especially since manual trimming applies to every configured target in the build).
    //
    // In other words: without this modification:
    // parent rule -> toolchain context -> toolchain
    //     -> toolchain dep 1 # applies manual trimming to remove feature flags
    //     -> toolchain dep 2 # applies manual trimming to remove feature flags
    //     ...
    //
    // With this modification:
    // parent rule -> toolchain context # applies manual trimming to remove feature flags
    //     -> toolchain
    //         -> toolchain dep 1
    //         -> toolchain dep 2
    //         ...
    //
    // None of this has any effect on rules that don't utilize manual trimming.
    PatchTransition toolchainTaggedTrimmingTransition =
        ((ConfiguredRuleClassProvider) ruleClassProvider).getToolchainTaggedTrimmingTransition();
    BuildOptions toolchainOptions =
        toolchainTaggedTrimmingTransition.patch(
            new BuildOptionsView(
                configuration.getOptions(),
                toolchainTaggedTrimmingTransition.requiresOptionFragments()),
            env.getListener());

    BuildConfigurationValue.Key toolchainConfig =
        BuildConfigurationValue.keyWithoutPlatformMapping(
            configuration.getFragmentsMap().keySet(),
            BuildOptions.diffForReconstruction(defaultBuildOptions, toolchainOptions));

    Map<String, ToolchainContextKey> toolchainContextKeys = new HashMap<>();
    String targetUnloadedToolchainContext = "target-unloaded-toolchain-context";
    ToolchainContextKey toolchainContextKey;
    if (parentToolchainContextKey != null) {
      toolchainContextKey = parentToolchainContextKey;
    } else {
      toolchainContextKey =
          ToolchainContextKey.key()
              .configurationKey(toolchainConfig)
              .requiredToolchainTypeLabels(requiredDefaultToolchains)
              .execConstraintLabels(defaultExecConstraintLabels)
              .shouldSanityCheckConfiguration(configuration.trimConfigurationsRetroactively())
              .build();
    }
    toolchainContextKeys.put(targetUnloadedToolchainContext, toolchainContextKey);
    for (Map.Entry<String, ExecGroup> group : execGroups.entrySet()) {
      ExecGroup execGroup = group.getValue();
      toolchainContextKeys.put(
          group.getKey(),
          ToolchainContextKey.key()
              .configurationKey(toolchainConfig)
              .requiredToolchainTypeLabels(execGroup.requiredToolchains())
              .execConstraintLabels(execGroup.execCompatibleWith())
              .shouldSanityCheckConfiguration(configuration.trimConfigurationsRetroactively())
              .build());
    }

    Map<SkyKey, ValueOrException<ToolchainException>> values =
        env.getValuesOrThrow(toolchainContextKeys.values(), ToolchainException.class);

    boolean valuesMissing = env.valuesMissing();

    ToolchainCollection.Builder<UnloadedToolchainContext> toolchainContexts =
        valuesMissing ? null : ToolchainCollection.builder();
    for (Map.Entry<String, ToolchainContextKey> unloadedToolchainContextKey :
        toolchainContextKeys.entrySet()) {
      UnloadedToolchainContext unloadedToolchainContext =
          (UnloadedToolchainContext) values.get(unloadedToolchainContextKey.getValue()).get();
      if (!valuesMissing) {
        String execGroup = unloadedToolchainContextKey.getKey();
        if (parentToolchainContextKey != null) {
          // Since we inherited the toolchain context from the parent of the dependency, the current
          // target may also be in the resolved toolchains list. We need to clear it out.
          // TODO(configurability): When updating this for config_setting, only remove the current
          // target, not everything, because config_setting might want to check the toolchain
          // dependencies.
          unloadedToolchainContext = unloadedToolchainContext.withoutResolvedToolchains();
        }
        if (execGroup.equals(targetUnloadedToolchainContext)) {
          toolchainContexts.addDefaultContext(unloadedToolchainContext);
        } else {
          toolchainContexts.addContext(execGroup, unloadedToolchainContext);
        }
      }
    }

    return valuesMissing ? null : toolchainContexts.build();
  }

  /**
   * Returns the target-specific execution platform constraints, based on the rule definition and
   * any constraints added by the target, including those added for the target on the command line.
   */
  public static ImmutableSet<Label> getExecutionPlatformConstraints(
      Rule rule, PlatformConfiguration platformConfiguration) {
    NonconfigurableAttributeMapper mapper = NonconfigurableAttributeMapper.of(rule);
    ImmutableSet.Builder<Label> execConstraintLabels = new ImmutableSet.Builder<>();

    execConstraintLabels.addAll(rule.getRuleClassObject().getExecutionPlatformConstraints());
    if (rule.getRuleClassObject()
        .hasAttr(RuleClass.EXEC_COMPATIBLE_WITH_ATTR, BuildType.LABEL_LIST)) {
      execConstraintLabels.addAll(
          mapper.get(RuleClass.EXEC_COMPATIBLE_WITH_ATTR, BuildType.LABEL_LIST));
    }

    execConstraintLabels.addAll(
        platformConfiguration.getAdditionalExecutionConstraintsFor(rule.getLabel()));

    return execConstraintLabels.build();
  }

  /**
   * Computes the direct dependencies of a node in the configured target graph (a configured target
   * or an aspects).
   *
   * <p>Returns null if Skyframe hasn't evaluated the required dependencies yet. In this case, the
   * caller should also return null to Skyframe.
   *
   * @param env the Skyframe environment
   * @param resolver the dependency resolver
   * @param ctgValue the label and the configuration of the node
   * @param aspects
   * @param configConditions the configuration conditions for evaluating the attributes of the node
   * @param toolchainContexts the toolchain context for this target
   * @param ruleClassProvider rule class provider for determining the right configuration fragments
   *     to apply to deps
   * @param hostConfiguration the host configuration. There's a noticeable performance hit from
   *     instantiating this on demand for every dependency that wants it, so it's best to compute
   *     the host configuration as early as possible and pass this reference to all consumers
   * @param defaultBuildOptions the default build options provided by the server; these are used to
   *     create diffs for {@link BuildConfigurationValue.Key}s to prevent storing the entire
   *     BuildOptions object.
   */
  @Nullable
  static OrderedSetMultimap<DependencyKind, ConfiguredTargetAndData> computeDependencies(
      Environment env,
      SkyframeDependencyResolver resolver,
      TargetAndConfiguration ctgValue,
      Iterable<Aspect> aspects,
      ImmutableMap<Label, ConfigMatchingProvider> configConditions,
      @Nullable ToolchainCollection<ToolchainContext> toolchainContexts,
      boolean useToolchainTransition,
      RuleClassProvider ruleClassProvider,
      BuildConfiguration hostConfiguration,
      @Nullable NestedSetBuilder<Package> transitivePackagesForPackageRootResolution,
      NestedSetBuilder<Cause> transitiveRootCauses,
      BuildOptions defaultBuildOptions)
      throws DependencyEvaluationException, ConfiguredValueCreationException,
          AspectCreationException, InterruptedException {
    // Create the map from attributes to set of (target, transition) pairs.
    OrderedSetMultimap<DependencyKind, DependencyKey> initialDependencies;
    BuildConfiguration configuration = ctgValue.getConfiguration();
    Label label = ctgValue.getLabel();
    try {
      initialDependencies =
          resolver.dependentNodeMap(
              ctgValue,
              hostConfiguration,
              aspects,
              configConditions,
              toolchainContexts,
              useToolchainTransition,
              transitiveRootCauses,
              ((ConfiguredRuleClassProvider) ruleClassProvider).getTrimmingTransitionFactory());
    } catch (EvalException e) {
      // EvalException can only be thrown by computed Starlark attributes in the current rule.
      String msgWithStack = e.getMessageWithStack();
      env.getListener().handle(Event.error(null, msgWithStack));
      env.getListener().post(new AnalysisRootCauseEvent(configuration, label, msgWithStack));
      throw new DependencyEvaluationException(
          new ConfiguredValueCreationException(msgWithStack, label, configuration));
    } catch (InconsistentAspectOrderException e) {
      env.getListener().handle(Event.error(e.getLocation(), e.getMessage()));
      throw new DependencyEvaluationException(e);
    }
    // Trim each dep's configuration so it only includes the fragments needed by its transitive
    // closure.
    ConfigurationResolver configResolver =
        new ConfigurationResolver(
            env, ctgValue, hostConfiguration, defaultBuildOptions, configConditions);
    OrderedSetMultimap<DependencyKind, Dependency> depValueNames =
        configResolver.resolveConfigurations(initialDependencies);

    // Return early in case packages were not loaded yet. In theory, we could start configuring
    // dependent targets in loaded packages. However, that creates an artificial sync boundary
    // between loading all dependent packages (fast) and configuring some dependent targets (can
    // have a long tail).
    if (env.valuesMissing()) {
      return null;
    }

    // Resolve configured target dependencies and handle errors.
    Map<SkyKey, ConfiguredTargetAndData> depValues =
        resolveConfiguredTargetDependencies(
            env,
            ctgValue,
            depValueNames.values(),
            transitivePackagesForPackageRootResolution,
            transitiveRootCauses);
    if (depValues == null) {
      return null;
    }

    // Resolve required aspects.
    OrderedSetMultimap<Dependency, ConfiguredAspect> depAspects =
        AspectResolver.resolveAspectDependencies(
            env, depValues, depValueNames.values(), transitivePackagesForPackageRootResolution);
    if (depAspects == null) {
      return null;
    }

    // Merge the dependent configured targets and aspects into a single map.
    try {
      return AspectResolver.mergeAspects(depValueNames, depValues, depAspects);
    } catch (DuplicateException e) {
      env.getListener().handle(
          Event.error(ctgValue.getTarget().getLocation(), e.getMessage()));

      throw new ConfiguredValueCreationException(e.getMessage(), label, configuration);
    }
  }

  /**
   * Returns the set of {@link ConfigMatchingProvider}s that key the configurable attributes used by
   * this rule.
   *
   * <p>>If the configured targets supplying those providers aren't yet resolved by the dependency
   * resolver, returns null.
   */
  @Nullable
  static ImmutableMap<Label, ConfigMatchingProvider> getConfigConditions(
      Environment env,
      TargetAndConfiguration ctgValue,
      @Nullable NestedSetBuilder<Package> transitivePackagesForPackageRootResolution,
      @Nullable PlatformInfo platformInfo,
      NestedSetBuilder<Cause> transitiveRootCauses)
      throws DependencyEvaluationException, InterruptedException {
    Target target = ctgValue.getTarget();
    if (!(target instanceof Rule)) {
      return NO_CONFIG_CONDITIONS;
    }
    RawAttributeMapper attrs = RawAttributeMapper.of(((Rule) target));
    if (!attrs.has(RuleClass.CONFIG_SETTING_DEPS_ATTRIBUTE)) {
      return NO_CONFIG_CONDITIONS;
    }

    // Collect the labels of the configured targets we need to resolve.
    List<Label> configLabels =
        attrs.get(RuleClass.CONFIG_SETTING_DEPS_ATTRIBUTE, BuildType.LABEL_LIST).stream()
            .map(configLabel -> target.getLabel().resolveRepositoryRelative(configLabel))
            .collect(Collectors.toList());
    if (configLabels.isEmpty()) {
      return NO_CONFIG_CONDITIONS;
    } else if (ctgValue.getConfiguration().trimConfigurationsRetroactively()) {
      String message =
          target.getLabel()
              + " has configurable attributes, but these are not supported in retroactive trimming "
              + "mode.";
      env.getListener().handle(Event.error(TargetUtils.getLocationMaybe(target), message));
      throw new DependencyEvaluationException(
          new ConfiguredValueCreationException(
              message, ctgValue.getLabel(), ctgValue.getConfiguration()));
    }

    // Collect the actual deps without a configuration transition (since by definition config
    // conditions evaluate over the current target's configuration). If the dependency is
    // (erroneously) something that needs the null configuration, its analysis will be
    // short-circuited. That error will be reported later.
    ImmutableList.Builder<Dependency> depsBuilder = ImmutableList.builder();
    for (Label configurabilityLabel : configLabels) {
      Dependency configurabilityDependency =
          Dependency.builder()
              .setLabel(configurabilityLabel)
              .setConfiguration(ctgValue.getConfiguration())
              .build();
      depsBuilder.add(configurabilityDependency);
    }

    ImmutableList<Dependency> configConditionDeps = depsBuilder.build();

    Map<SkyKey, ConfiguredTargetAndData> configValues;
    try {
      configValues =
          resolveConfiguredTargetDependencies(
              env,
              ctgValue,
              configConditionDeps,
              transitivePackagesForPackageRootResolution,
              transitiveRootCauses);
      if (configValues == null) {
        return null;
      }
    } catch (DependencyEvaluationException e) {
      // One of the config dependencies doesn't exist, and we need to report that. Unfortunately,
      // there's not enough information to know which configurable attribute has the problem.
      env.getListener()
          .handle(
              Event.error(
                  String.format(
                      "While resolving configuration keys for %s: %s",
                      target.getLabel(), e.getCause().getMessage())));

      // Re-throw the exception so it is handled by compute().
      throw e;
    }

    Map<Label, ConfigMatchingProvider> configConditions = new LinkedHashMap<>();

    // Get the configured targets as ConfigMatchingProvider interfaces.
    for (Dependency entry : configConditionDeps) {
      SkyKey baseKey = entry.getConfiguredTargetKey();
      // The code above guarantees that value is non-null here.
      ConfiguredTarget value = configValues.get(baseKey).getConfiguredTarget();
      // The below handles config_setting (which nativly provides ConfigMatchingProvider) and
      // constraint_value (which needs a custom-built ConfigMatchingProvider). If we ever add
      // support for more rules we should move resolution logic to ConfigMatchingProvider and
      // simplify the logic here.
      ConfigMatchingProvider matchingProvider = value.getProvider(ConfigMatchingProvider.class);
      ConstraintValueInfo constraintValueInfo = value.get(ConstraintValueInfo.PROVIDER);

      if (matchingProvider != null) {
        configConditions.put(entry.getLabel(), matchingProvider);
      } else if (constraintValueInfo != null && platformInfo != null) {
        // If platformInfo == null, that means the owning target doesn't invoke toolchain
        // resolution, in which case depending on a constraint_value is non-sensical.
        configConditions.put(
            entry.getLabel(), constraintValueInfo.configMatchingProvider(platformInfo));
      } else {
        // Not a valid provider for configuration conditions.
        String message =
            String.format(
                    "%s is not a valid select() condition for %s.\n",
                    entry.getLabel(), target.getLabel())
                + String.format(
                    "To inspect the select(), run: bazel query --output=build %s.\n",
                    target.getLabel())
                + "For more help, see https://docs.bazel.build/be/functions.html#select.\n\n";
        env.getListener().handle(Event.error(TargetUtils.getLocationMaybe(target), message));
        throw new DependencyEvaluationException(
            new ConfiguredValueCreationException(
                message, ctgValue.getLabel(), ctgValue.getConfiguration()));
      }
    }

    return ImmutableMap.copyOf(configConditions);
  }

  /**
   * Resolves the targets referenced in depValueNames and returns their {@link
   * ConfiguredTargetAndData} instances.
   *
   * <p>Returns null if not all instances are available yet.
   */
  @Nullable
  private static Map<SkyKey, ConfiguredTargetAndData> resolveConfiguredTargetDependencies(
      Environment env,
      TargetAndConfiguration ctgValue,
      Collection<Dependency> deps,
      @Nullable NestedSetBuilder<Package> transitivePackagesForPackageRootResolution,
      NestedSetBuilder<Cause> transitiveRootCauses)
      throws DependencyEvaluationException, InterruptedException {
    boolean missedValues = env.valuesMissing();
    String failWithMessage = null;
    DetailedExitCode detailedExitCode = null;
    // Naively we would like to just fetch all requested ConfiguredTargets, together with their
    // Packages. However, some ConfiguredTargets are AliasConfiguredTargets, which means that their
    // associated Targets (and therefore associated Packages) don't correspond to their own Labels.
    // We don't know the associated Package until we fetch the ConfiguredTarget. Therefore, we have
    // to do a potential second pass, in which we fetch all the Packages for AliasConfiguredTargets.
    Iterable<SkyKey> depKeys =
        Iterables.concat(
            Iterables.transform(deps, Dependency::getConfiguredTargetKey),
            Iterables.transform(
                deps, input -> PackageValue.key(input.getLabel().getPackageIdentifier())));
    Map<SkyKey, ValueOrException<ConfiguredValueCreationException>> depValuesOrExceptions =
        env.getValuesOrThrow(depKeys, ConfiguredValueCreationException.class);
    Map<SkyKey, ConfiguredTargetAndData> result = Maps.newHashMapWithExpectedSize(deps.size());
    Set<SkyKey> aliasPackagesToFetch = new HashSet<>();
    List<Dependency> aliasDepsToRedo = new ArrayList<>();
    Map<SkyKey, SkyValue> aliasPackageValues = null;
    Collection<Dependency> depsToProcess = deps;
    for (int i = 0; i < 2; i++) {
      for (Dependency dep : depsToProcess) {
        SkyKey key = dep.getConfiguredTargetKey();
        try {
          ConfiguredTargetValue depValue =
              (ConfiguredTargetValue) depValuesOrExceptions.get(key).get();

          if (depValue == null) {
            missedValues = true;
          } else {
            ConfiguredTarget depCt = depValue.getConfiguredTarget();
            Label depLabel = depCt.getLabel();
            SkyKey packageKey = PackageValue.key(depLabel.getPackageIdentifier());
            PackageValue pkgValue;
            if (i == 0) {
              ValueOrException<ConfiguredValueCreationException> packageResult =
                  depValuesOrExceptions.get(packageKey);
              if (packageResult == null) {
                aliasPackagesToFetch.add(packageKey);
                aliasDepsToRedo.add(dep);
                continue;
              } else {
                pkgValue = (PackageValue) packageResult.get();
                if (pkgValue == null) {
                  // In a race, the getValuesOrThrow call above may have retrieved the package
                  // before it was done but the configured target after it was done. Since
                  // SkyFunctionEnvironment may cache absent values, re-requesting it on this
                  // evaluation may be useless, just treat it as missing.
                  missedValues = true;
                  continue;
                }
              }
            } else {
              // We were doing AliasConfiguredTarget mop-up.
              pkgValue = (PackageValue) aliasPackageValues.get(packageKey);
              if (pkgValue == null) {
                // This is unexpected: on the second iteration, all packages should be present,
                // since the configured targets that depend on them are present. But since that is
                // not a guarantee Skyframe makes, we tolerate their absence.
                missedValues = true;
                continue;
              }
            }
            try {
              BuildConfiguration depConfiguration = dep.getConfiguration();
              BuildConfigurationValue.Key depKey =
                  depValue.getConfiguredTarget().getConfigurationKey();
              // Retroactive trimming may change the configuration associated with the dependency.
              // If it does, we need to get that instance.
              // TODO(b/140632978): doing these individually instead of doing them all at once may
              // end up being wasteful use of Skyframe. Although these configurations are guaranteed
              // to be in the Skyframe cache (because the dependency would have had to retrieve them
              // to be created in the first place), looking them up repeatedly may be slower than
              // just keeping a local cache and assigning the same configuration to all the CTs
              // which need it. Profile this and see if there's a better way.
              if (depKey != null && !depKey.equals(BuildConfigurationValue.key(depConfiguration))) {
                if (!depConfiguration.trimConfigurationsRetroactively()) {
                  throw new AssertionError(
                      "Loading configurations mid-dependency resolution should ONLY happen when "
                          + "retroactive trimming is enabled.");
                }
                depConfiguration =
                    ((BuildConfigurationValue) env.getValue(depKey)).getConfiguration();
              }
              result.put(
                  key,
                  new ConfiguredTargetAndData(
                      depValue.getConfiguredTarget(),
                      pkgValue.getPackage().getTarget(depLabel.getName()),
                      depConfiguration,
                      dep.getTransitionKeys()));
            } catch (NoSuchTargetException e) {
              throw new IllegalStateException("Target already verified for " + dep, e);
            }
            if (transitivePackagesForPackageRootResolution != null) {
              transitivePackagesForPackageRootResolution.addTransitive(
                  depValue.getTransitivePackagesForPackageRootResolution());
            }
          }
        } catch (ConfiguredValueCreationException e) {
          transitiveRootCauses.addTransitive(e.getRootCauses());
          detailedExitCode =
              DetailedExitCodeComparator.chooseMoreImportantWithFirstIfTie(
                  e.getDetailedExitCode(), detailedExitCode);
          if (e.getDetailedExitCode().equals(detailedExitCode)) {
            failWithMessage = e.getMessage();
          }
        }
      }
      if (aliasDepsToRedo.isEmpty()) {
        break;
      }
      aliasPackageValues = env.getValues(aliasPackagesToFetch);
      depsToProcess = aliasDepsToRedo;
    }
    if (failWithMessage != null) {
      throw new DependencyEvaluationException(
          new ConfiguredValueCreationException(
              failWithMessage,
              ctgValue.getConfiguration(),
              transitiveRootCauses.build(),
              detailedExitCode));
    } else if (missedValues) {
      return null;
    } else {
      return result;
    }
  }

  @Override
  public String extractTag(SkyKey skyKey) {
    return Label.print(((ConfiguredTargetKey) skyKey.argument()).getLabel());
  }

  @Nullable
  private ConfiguredTargetValue createConfiguredTarget(
      SkyframeBuildView view,
      Environment env,
      Target target,
      BuildConfiguration configuration,
      ConfiguredTargetKey configuredTargetKey,
      OrderedSetMultimap<DependencyKind, ConfiguredTargetAndData> depValueMap,
      ImmutableMap<Label, ConfigMatchingProvider> configConditions,
      @Nullable ToolchainCollection<ResolvedToolchainContext> toolchainContexts,
      @Nullable NestedSetBuilder<Package> transitivePackagesForPackageRootResolution)
      throws ConfiguredTargetFunctionException, InterruptedException {
    StoredEventHandler events = new StoredEventHandler();
    CachingAnalysisEnvironment analysisEnvironment =
        view.createAnalysisEnvironment(configuredTargetKey, false, events, env, configuration);
    if (env.valuesMissing()) {
      return null;
    }

    Preconditions.checkNotNull(depValueMap);
    ConfiguredTarget configuredTarget;
    try {
      configuredTarget =
          view.createConfiguredTarget(
              target,
              configuration,
              analysisEnvironment,
              configuredTargetKey,
              depValueMap,
              configConditions,
              toolchainContexts);
    } catch (MissingDepException e) {
      Preconditions.checkState(env.valuesMissing(), e.getMessage());
      return null;
    } catch (ActionConflictException e) {
      throw new ConfiguredTargetFunctionException(e);
    } catch (InvalidExecGroupException e) {
      throw new ConfiguredTargetFunctionException(e);
    }

    events.replayOn(env.getListener());
    if (events.hasErrors()) {
      analysisEnvironment.disable(target);
      NestedSet<Cause> rootCauses =
          NestedSetBuilder.wrap(
              Order.STABLE_ORDER,
              events.getEvents().stream()
                  .filter((event) -> event.getKind() == EventKind.ERROR)
                  .map(
                      (event) ->
                          new AnalysisFailedCause(
                              target.getLabel(),
                              configuration == null
                                  ? null
                                  : configuration.getEventId().getConfiguration(),
                              createDetailedExitCode(
                                  event.getMessage(), Code.CONFIGURED_VALUE_CREATION_FAILED)))
                  .collect(Collectors.toList()));
      throw new ConfiguredTargetFunctionException(
          new ConfiguredValueCreationException(
              "Analysis of target '" + target.getLabel() + "' failed", configuration, rootCauses));
    }
    Preconditions.checkState(!analysisEnvironment.hasErrors(),
        "Analysis environment hasError() but no errors reported");
    if (env.valuesMissing()) {
      return null;
    }

    analysisEnvironment.disable(target);
    Preconditions.checkNotNull(configuredTarget, target);

    if (configuredTarget instanceof RuleConfiguredTarget) {
      RuleConfiguredTarget ruleConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
      return new RuleConfiguredTargetValue(
          ruleConfiguredTarget,
          transitivePackagesForPackageRootResolution == null
              ? null
              : transitivePackagesForPackageRootResolution.build());
    } else {
      GeneratingActions generatingActions;
      // Check for conflicting actions within this configured target (that indicates a bug in the
      // rule implementation).
      try {
        generatingActions =
            Actions.assignOwnersAndFilterSharedActionsAndThrowActionConflict(
                analysisEnvironment.getActionKeyContext(),
                analysisEnvironment.getRegisteredActions(),
                configuredTargetKey,
                /*outputFiles=*/ null);
      } catch (ActionConflictException e) {
        throw new ConfiguredTargetFunctionException(e);
      }
      return new NonRuleConfiguredTargetValue(
          configuredTarget,
          generatingActions,
          transitivePackagesForPackageRootResolution == null
              ? null
              : transitivePackagesForPackageRootResolution.build());
    }
  }

  private static DetailedExitCode createDetailedExitCode(String message, Code code) {
    return DetailedExitCode.of(
        FailureDetail.newBuilder()
            .setMessage(message)
            .setAnalysis(Analysis.newBuilder().setCode(code))
            .build());
  }

  static DetailedExitCode getPrioritizedDetailedExitCode(NestedSet<Cause> causes) {
    DetailedExitCode[] prioritizedDetailedExitCodeWrapper = {null};
    causes.forEachElement(
        o -> true,
        c ->
            prioritizedDetailedExitCodeWrapper[0] =
                DetailedExitCodeComparator.chooseMoreImportantWithFirstIfTie(
                    prioritizedDetailedExitCodeWrapper[0], c.getDetailedExitCode()));
    return prioritizedDetailedExitCodeWrapper[0];
  }

  /**
   * Used to declare all the exception types that can be wrapped in the exception thrown by {@link
   * ConfiguredTargetFunction#compute}.
   */
  static final class ConfiguredTargetFunctionException extends SkyFunctionException {
    private ConfiguredTargetFunctionException(ConfiguredValueCreationException e) {
      super(e, Transience.PERSISTENT);
    }

    private ConfiguredTargetFunctionException(ActionConflictException e) {
      super(e, Transience.PERSISTENT);
    }

    private ConfiguredTargetFunctionException(InvalidExecGroupException e) {
      super(e, Transience.PERSISTENT);
    }
  }
}
