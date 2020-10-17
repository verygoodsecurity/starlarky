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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.actions.MutableActionGraph.ActionConflictException;
import com.google.devtools.build.lib.analysis.AliasProvider;
import com.google.devtools.build.lib.analysis.AspectResolver;
import com.google.devtools.build.lib.analysis.AspectValue;
import com.google.devtools.build.lib.analysis.CachingAnalysisEnvironment;
import com.google.devtools.build.lib.analysis.CachingAnalysisEnvironment.MissingDepException;
import com.google.devtools.build.lib.analysis.ConfiguredAspect;
import com.google.devtools.build.lib.analysis.ConfiguredAspectFactory;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.DependencyKind;
import com.google.devtools.build.lib.analysis.DuplicateException;
import com.google.devtools.build.lib.analysis.InconsistentAspectOrderException;
import com.google.devtools.build.lib.analysis.PlatformOptions;
import com.google.devtools.build.lib.analysis.ResolvedToolchainContext;
import com.google.devtools.build.lib.analysis.RuleContext.InvalidExecGroupException;
import com.google.devtools.build.lib.analysis.TargetAndConfiguration;
import com.google.devtools.build.lib.analysis.ToolchainCollection;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.analysis.config.BuildOptions;
import com.google.devtools.build.lib.analysis.config.ConfigMatchingProvider;
import com.google.devtools.build.lib.analysis.config.DependencyEvaluationException;
import com.google.devtools.build.lib.analysis.config.InvalidConfigurationException;
import com.google.devtools.build.lib.analysis.configuredtargets.MergedConfiguredTarget;
import com.google.devtools.build.lib.analysis.starlark.StarlarkTransition.TransitionException;
import com.google.devtools.build.lib.causes.Cause;
import com.google.devtools.build.lib.causes.LabelCause;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.StoredEventHandler;
import com.google.devtools.build.lib.packages.Aspect;
import com.google.devtools.build.lib.packages.AspectDefinition;
import com.google.devtools.build.lib.packages.AspectDescriptor;
import com.google.devtools.build.lib.packages.BuildFileContainsErrorsException;
import com.google.devtools.build.lib.packages.NativeAspectClass;
import com.google.devtools.build.lib.packages.NoSuchTargetException;
import com.google.devtools.build.lib.packages.NoSuchThingException;
import com.google.devtools.build.lib.packages.OutputFile;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.RuleClassProvider;
import com.google.devtools.build.lib.packages.StarlarkAspect;
import com.google.devtools.build.lib.packages.StarlarkAspectClass;
import com.google.devtools.build.lib.packages.StarlarkDefinedAspect;
import com.google.devtools.build.lib.packages.Target;
import com.google.devtools.build.lib.packages.Type.ConversionException;
import com.google.devtools.build.lib.profiler.memory.CurrentRuleTracker;
import com.google.devtools.build.lib.skyframe.AspectValueKey.AspectKey;
import com.google.devtools.build.lib.skyframe.BzlLoadFunction.BzlLoadFailedException;
import com.google.devtools.build.lib.skyframe.SkyframeExecutor.BuildViewProvider;
import com.google.devtools.build.lib.util.OrderedSetMultimap;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import com.google.devtools.build.skyframe.ValueOrException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * The Skyframe function that generates aspects.
 *
 * This class, together with {@link ConfiguredTargetFunction} drives the analysis phase. For more
 * information, see {@link com.google.devtools.build.lib.analysis.RuleConfiguredTargetFactory}.
 *
 * {@link AspectFunction} takes a SkyKey containing an {@link AspectKey} [a tuple of
 * (target label, configurations, aspect class and aspect parameters)],
 * loads an {@link Aspect} from aspect class and aspect parameters,
 * gets a {@link ConfiguredTarget} for label and configurations, and then creates
 * a {@link ConfiguredAspect} for a given {@link AspectKey}.
 *
 * See {@link com.google.devtools.build.lib.packages.AspectClass} documentation
 * for an overview of aspect-related classes
 *
 * @see com.google.devtools.build.lib.analysis.RuleConfiguredTargetFactory
 * @see com.google.devtools.build.lib.packages.AspectClass
 */
public final class AspectFunction implements SkyFunction {
  private final BuildViewProvider buildViewProvider;
  private final RuleClassProvider ruleClassProvider;
  private final BuildOptions defaultBuildOptions;
  /**
   * Indicates whether the set of packages transitively loaded for a given {@link AspectValue} will
   * be needed for package root resolution later in the build. If not, they are not collected and
   * stored.
   */
  private final boolean storeTransitivePackagesForPackageRootResolution;

  AspectFunction(
      BuildViewProvider buildViewProvider,
      RuleClassProvider ruleClassProvider,
      boolean storeTransitivePackagesForPackageRootResolution,
      BuildOptions defaultBuildOptions) {
    this.buildViewProvider = buildViewProvider;
    this.ruleClassProvider = ruleClassProvider;
    this.storeTransitivePackagesForPackageRootResolution =
        storeTransitivePackagesForPackageRootResolution;
    this.defaultBuildOptions = defaultBuildOptions;
  }

  /**
   * Load Starlark-defined aspect from an extension file. Is to be called from a SkyFunction.
   *
   * @return {@code null} if dependencies cannot be satisfied.
   * @throws AspectCreationException if the value loaded is not a {@link StarlarkDefinedAspect}.
   */
  @Nullable
  static StarlarkDefinedAspect loadStarlarkDefinedAspect(
      Environment env, StarlarkAspectClass starlarkAspectClass)
      throws AspectCreationException, InterruptedException {
    Label extensionLabel = starlarkAspectClass.getExtensionLabel();
    String starlarkValueName = starlarkAspectClass.getExportedName();

    StarlarkAspect starlarkAspect = loadStarlarkAspect(env, extensionLabel, starlarkValueName);
    if (starlarkAspect == null) {
      return null;
    }
    if (!(starlarkAspect instanceof StarlarkDefinedAspect)) {
      throw new AspectCreationException(
          String.format(
              "%s from %s is not a Starlark-defined aspect", starlarkValueName, extensionLabel),
          extensionLabel);
    } else {
      return (StarlarkDefinedAspect) starlarkAspect;
    }
  }

  /**
   * Load Starlark aspect from an extension file. Is to be called from a SkyFunction.
   *
   * @return {@code null} if dependencies cannot be satisfied.
   */
  @Nullable
  static StarlarkAspect loadStarlarkAspect(
      Environment env, Label extensionLabel, String starlarkValueName)
      throws AspectCreationException, InterruptedException {
    SkyKey importFileKey = BzlLoadValue.keyForBuild(extensionLabel);
    try {
      BzlLoadValue bzlLoadValue =
          (BzlLoadValue) env.getValueOrThrow(importFileKey, BzlLoadFailedException.class);
      if (bzlLoadValue == null) {
        Preconditions.checkState(
            env.valuesMissing(), "no Starlark import value for %s", importFileKey);
        return null;
      }

      Object starlarkValue = bzlLoadValue.getModule().getGlobal(starlarkValueName);
      if (starlarkValue == null) {
        throw new ConversionException(
            String.format(
                "%s is not exported from %s", starlarkValueName, extensionLabel.toString()));
      }
      if (!(starlarkValue instanceof StarlarkAspect)) {
        throw new ConversionException(
            String.format(
                "%s from %s is not an aspect", starlarkValueName, extensionLabel.toString()));
      }
      return (StarlarkAspect) starlarkValue;
    } catch (BzlLoadFailedException e) {
      env.getListener().handle(Event.error(e.getMessage()));
      throw new AspectCreationException(e.getMessage(), extensionLabel, e.getDetailedExitCode());
    } catch (ConversionException e) {
      env.getListener().handle(Event.error(e.getMessage()));
      throw new AspectCreationException(e.getMessage(), extensionLabel);
    }
  }

  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws AspectFunctionException, InterruptedException {
    SkyframeBuildView view = buildViewProvider.getSkyframeBuildView();
    NestedSetBuilder<Cause> transitiveRootCauses = NestedSetBuilder.stableOrder();
    AspectKey key = (AspectKey) skyKey.argument();
    ConfiguredAspectFactory aspectFactory;
    Aspect aspect;
    if (key.getAspectClass() instanceof NativeAspectClass) {
      NativeAspectClass nativeAspectClass = (NativeAspectClass) key.getAspectClass();
      aspectFactory = (ConfiguredAspectFactory) nativeAspectClass;
      aspect = Aspect.forNative(nativeAspectClass, key.getParameters());
    } else if (key.getAspectClass() instanceof StarlarkAspectClass) {
      StarlarkAspectClass starlarkAspectClass = (StarlarkAspectClass) key.getAspectClass();
      StarlarkDefinedAspect starlarkAspect;
      try {
        starlarkAspect = loadStarlarkDefinedAspect(env, starlarkAspectClass);
      } catch (AspectCreationException e) {
        throw new AspectFunctionException(e);
      }
      if (starlarkAspect == null) {
        return null;
      }

      aspectFactory = new StarlarkAspectFactory(starlarkAspect);
      aspect =
          Aspect.forStarlark(
              starlarkAspect.getAspectClass(),
              starlarkAspect.getDefinition(key.getParameters()),
              key.getParameters());
    } else {
      throw new IllegalStateException();
    }

    // Keep this in sync with the same code in ConfiguredTargetFunction.
    PackageValue packageValue =
        (PackageValue) env.getValue(PackageValue.key(key.getLabel().getPackageIdentifier()));
    if (packageValue == null) {
      return null;
    }
    Package pkg = packageValue.getPackage();
    if (pkg.containsErrors()) {
      throw new AspectFunctionException(
          new BuildFileContainsErrorsException(key.getLabel().getPackageIdentifier()));
    }

    boolean aspectHasConfiguration = key.getAspectConfigurationKey() != null;

    ImmutableSet<SkyKey> keys =
        aspectHasConfiguration
            ? ImmutableSet.of(key.getBaseConfiguredTargetKey(), key.getAspectConfigurationKey())
            : ImmutableSet.of(key.getBaseConfiguredTargetKey());

    Map<SkyKey, ValueOrException<ConfiguredValueCreationException>> baseAndAspectValues =
        env.getValuesOrThrow(keys, ConfiguredValueCreationException.class);
    if (env.valuesMissing()) {
      return null;
    }

    ConfiguredTargetValue baseConfiguredTargetValue;
    BuildConfiguration aspectConfiguration = null;

    try {
      baseConfiguredTargetValue =
          (ConfiguredTargetValue) baseAndAspectValues.get(key.getBaseConfiguredTargetKey()).get();
    } catch (ConfiguredValueCreationException e) {
      throw new AspectFunctionException(
          new AspectCreationException(e.getMessage(), e.getRootCauses(), e.getDetailedExitCode()));
    }

    if (aspectHasConfiguration) {
      try {
        aspectConfiguration =
            ((BuildConfigurationValue)
                    baseAndAspectValues.get(key.getAspectConfigurationKey()).get())
                .getConfiguration();
      } catch (ConfiguredValueCreationException e) {
        throw new IllegalStateException("Unexpected exception from BuildConfigurationFunction when "
            + "computing " + key.getAspectConfigurationKey(), e);
      }
      if (aspectConfiguration.trimConfigurationsRetroactively()) {
        throw new AssertionError("Aspects should NEVER be evaluated in retroactive trimming mode.");
      }
    }

    ConfiguredTarget associatedTarget = baseConfiguredTargetValue.getConfiguredTarget();

    ConfiguredTargetAndData associatedConfiguredTargetAndData;
    Package targetPkg;
    BuildConfiguration configuration = null;
    PackageValue.Key packageKey =
        PackageValue.key(associatedTarget.getLabel().getPackageIdentifier());
    if (associatedTarget.getConfigurationKey() == null) {
      PackageValue val = ((PackageValue) env.getValue(packageKey));
      if (val == null) {
        // Unexpected in Bazel logic, but Skyframe makes no guarantees that this package is
        // actually present.
        return null;
      }
      targetPkg = val.getPackage();
    } else {
      Map<SkyKey, SkyValue> result =
          env.getValues(ImmutableSet.of(packageKey, associatedTarget.getConfigurationKey()));
      if (env.valuesMissing()) {
        // Unexpected in Bazel logic, but Skyframe makes no guarantees that this package and
        // configuration are actually present.
        return null;
      }
      targetPkg = ((PackageValue) result.get(packageKey)).getPackage();
      configuration =
          ((BuildConfigurationValue) result.get(associatedTarget.getConfigurationKey()))
              .getConfiguration();
      if (configuration.trimConfigurationsRetroactively()) {
        throw new AssertionError("Aspects should NEVER be evaluated in retroactive trimming mode.");
      }
    }
    try {
      associatedConfiguredTargetAndData =
          new ConfiguredTargetAndData(
              associatedTarget,
              targetPkg.getTarget(associatedTarget.getLabel().getName()),
              configuration,
              null);
    } catch (NoSuchTargetException e) {
      throw new IllegalStateException("Name already verified", e);
    }

    if (baseConfiguredTargetValue.getConfiguredTarget().getProvider(AliasProvider.class) != null) {
      return createAliasAspect(
          env,
          associatedConfiguredTargetAndData.getTarget(),
          aspect,
          key,
          baseConfiguredTargetValue.getConfiguredTarget());
    }


    ImmutableList.Builder<Aspect> aspectPathBuilder = ImmutableList.builder();

    if (!key.getBaseKeys().isEmpty()) {
      // We transitively collect all required aspects to reduce the number of restarts.
      // Semantically it is enough to just request key.getBaseKeys().
      ImmutableList.Builder<SkyKey> aspectPathSkyKeysBuilder = ImmutableList.builder();
      ImmutableMap<AspectDescriptor, SkyKey> aspectKeys =
          getSkyKeysForAspectsAndCollectAspectPath(key.getBaseKeys(), aspectPathSkyKeysBuilder);

      Map<SkyKey, SkyValue> values = env.getValues(aspectKeys.values());
      if (env.valuesMissing()) {
        return null;
      }
      ImmutableList<SkyKey> aspectPathSkyKeys = aspectPathSkyKeysBuilder.build();
      for (SkyKey aspectPathSkyKey : aspectPathSkyKeys) {
        aspectPathBuilder.add(((AspectValue) values.get(aspectPathSkyKey)).getAspect());
      }
      try {
        associatedTarget = getBaseTarget(
            associatedTarget, key.getBaseKeys(), values);
      } catch (DuplicateException e) {
        env.getListener()
            .handle(
                Event.error(
                    associatedConfiguredTargetAndData.getTarget().getLocation(), e.getMessage()));

        throw new AspectFunctionException(
            new AspectCreationException(
                e.getMessage(), associatedTarget.getLabel(), aspectConfiguration));
      }
    }
    associatedConfiguredTargetAndData =
        associatedConfiguredTargetAndData.fromConfiguredTarget(associatedTarget);
    aspectPathBuilder.add(aspect);

    SkyframeDependencyResolver resolver = view.createDependencyResolver(env);
    NestedSetBuilder<Package> transitivePackagesForPackageRootResolution =
        storeTransitivePackagesForPackageRootResolution ? NestedSetBuilder.stableOrder() : null;

    // When getting the dependencies of this hybrid aspect+base target, use the aspect's
    // configuration. The configuration of the aspect will always be a superset of the target's
    // (trimmed configuration mode: target is part of the aspect's config fragment requirements;
    // untrimmed mode: target is the same configuration as the aspect), so the fragments
    // required by all dependencies (both those of the aspect and those of the base target)
    // will be present this way.
    TargetAndConfiguration originalTargetAndAspectConfiguration =
        new TargetAndConfiguration(
            associatedConfiguredTargetAndData.getTarget(), aspectConfiguration);
    ImmutableList<Aspect> aspectPath = aspectPathBuilder.build();
    try {
      // Determine what toolchains are needed by this target.
      UnloadedToolchainContext unloadedToolchainContext = null;
      if (configuration != null) {
        // Configuration can be null in the case of aspects applied to input files. In this case,
        // there are no chances of toolchains being used, so skip it.
        try {
          ImmutableSet<Label> requiredToolchains = aspect.getDefinition().getRequiredToolchains();
          unloadedToolchainContext =
              (UnloadedToolchainContext)
                  env.getValueOrThrow(
                      ToolchainContextKey.key()
                          .configurationKey(BuildConfigurationValue.key(configuration))
                          .requiredToolchainTypeLabels(requiredToolchains)
                          .shouldSanityCheckConfiguration(
                              configuration.trimConfigurationsRetroactively())
                          .build(),
                      ToolchainException.class);
        } catch (ToolchainException e) {
          // TODO(katre): better error handling
          throw new AspectCreationException(
              e.getMessage(), new LabelCause(key.getLabel(), e.getDetailedExitCode()));
        }
      }
      if (env.valuesMissing()) {
        return null;
      }

      // Get the configuration targets that trigger this rule's configurable attributes.
      ImmutableMap<Label, ConfigMatchingProvider> configConditions =
          ConfiguredTargetFunction.getConfigConditions(
              env,
              originalTargetAndAspectConfiguration,
              transitivePackagesForPackageRootResolution,
              unloadedToolchainContext == null ? null : unloadedToolchainContext.targetPlatform(),
              transitiveRootCauses);
      if (configConditions == null) {
        // Those targets haven't yet been resolved.
        return null;
      }

      OrderedSetMultimap<DependencyKind, ConfiguredTargetAndData> depValueMap;
      try {
        depValueMap =
            ConfiguredTargetFunction.computeDependencies(
                env,
                resolver,
                originalTargetAndAspectConfiguration,
                aspectPath,
                configConditions,
                unloadedToolchainContext == null
                    ? null
                    : ToolchainCollection.builder()
                        .addDefaultContext(unloadedToolchainContext)
                        .build(),
                shouldUseToolchainTransition(configuration, aspect.getDefinition()),
                ruleClassProvider,
                view.getHostConfiguration(originalTargetAndAspectConfiguration.getConfiguration()),
                transitivePackagesForPackageRootResolution,
                transitiveRootCauses,
                defaultBuildOptions);
      } catch (ConfiguredValueCreationException e) {
        throw new AspectCreationException(
            e.getMessage(), key.getLabel(), aspectConfiguration, e.getDetailedExitCode());
      }
      if (depValueMap == null) {
        return null;
      }
      if (!transitiveRootCauses.isEmpty()) {
        NestedSet<Cause> causes = transitiveRootCauses.build();
        throw new AspectFunctionException(
            new AspectCreationException(
                "Loading failed",
                causes,
                ConfiguredTargetFunction.getPrioritizedDetailedExitCode(causes)));
      }

      // Load the requested toolchains into the ToolchainContext, now that we have dependencies.
      ResolvedToolchainContext toolchainContext = null;
      if (unloadedToolchainContext != null) {
        String targetDescription =
            String.format(
                "aspect %s applied to %s",
                aspect.getDescriptor().getDescription(),
                associatedConfiguredTargetAndData.getTarget());
        toolchainContext =
            ResolvedToolchainContext.load(
                targetPkg.getRepositoryMapping(),
                unloadedToolchainContext,
                targetDescription,
                // TODO(161222568): Support exec groups on aspects.
                depValueMap.get(DependencyKind.defaultExecGroupToolchain()));
      }

      return createAspect(
          env,
          key,
          aspectPath,
          aspect,
          aspectFactory,
          associatedConfiguredTargetAndData,
          aspectConfiguration,
          configConditions,
          toolchainContext,
          depValueMap,
          transitivePackagesForPackageRootResolution);
    } catch (DependencyEvaluationException e) {
      if (e.getCause() instanceof ConfiguredValueCreationException) {
        ConfiguredValueCreationException cause = (ConfiguredValueCreationException) e.getCause();
        throw new AspectFunctionException(
            new AspectCreationException(
                cause.getMessage(), cause.getRootCauses(), cause.getDetailedExitCode()));
      } else if (e.getCause() instanceof InconsistentAspectOrderException) {
        InconsistentAspectOrderException cause = (InconsistentAspectOrderException) e.getCause();
        throw new AspectFunctionException(
            new AspectCreationException(cause.getMessage(), key.getLabel(), aspectConfiguration));
      } else if (e.getCause() instanceof TransitionException) {
        TransitionException cause = (TransitionException) e.getCause();
        throw new AspectFunctionException(
            new AspectCreationException(cause.getMessage(), key.getLabel(), aspectConfiguration));
      } else {
        // Cast to InvalidConfigurationException as a consistency check. If you add any
        // DependencyEvaluationException constructors, you may need to change this code, too.
        InvalidConfigurationException cause = (InvalidConfigurationException) e.getCause();
        throw new AspectFunctionException(
            new AspectCreationException(
                cause.getMessage(),
                key.getLabel(),
                aspectConfiguration,
                cause.getDetailedExitCode()));
      }
    } catch (AspectCreationException e) {
      throw new AspectFunctionException(e);
    } catch (ToolchainException e) {
      throw new AspectFunctionException(
          new AspectCreationException(
              e.getMessage(), new LabelCause(key.getLabel(), e.getDetailedExitCode())));
    }
  }

  /**
   * Returns whether or not to use the new toolchain transition. Checks the global incompatible
   * change flag and the aspect's toolchain transition readiness attribute.
   */
  // TODO(#10523): Remove this when the migration period for toolchain transitions has ended.
  private static boolean shouldUseToolchainTransition(
      @Nullable BuildConfiguration configuration, AspectDefinition definition) {
    // Check whether the global incompatible change flag is set.
    if (configuration != null) {
      PlatformOptions platformOptions = configuration.getOptions().get(PlatformOptions.class);
      if (platformOptions != null && platformOptions.overrideToolchainTransition) {
        return true;
      }
    }

    // Check the aspect definition to see if it is ready.
    return definition.useToolchainTransition();
  }

  /**
   * Merges aspects defined by {@code aspectKeys} into the {@code target} using previously computed
   * {@code values}.
   *
   * @return A {@link ConfiguredTarget} that is a result of a merge.
   * @throws DuplicateException if there is a duplicate provider provided by aspects.
   */
  private static ConfiguredTarget getBaseTarget(
      ConfiguredTarget target, ImmutableList<AspectKey> aspectKeys, Map<SkyKey, SkyValue> values)
      throws DuplicateException {
    ArrayList<ConfiguredAspect> aspectValues = new ArrayList<>();
    for (AspectKey aspectKey : aspectKeys) {
      AspectValue aspectValue = (AspectValue) values.get(aspectKey);
      ConfiguredAspect configuredAspect = aspectValue.getConfiguredAspect();
      aspectValues.add(configuredAspect);
    }
    return MergedConfiguredTarget.of(target, aspectValues);
  }

  /**
   *  Collect all SkyKeys that are needed for a given list of AspectKeys,
   *  including transitive dependencies.
   *
   *  Also collects all propagating aspects in correct order.
   */
  private ImmutableMap<AspectDescriptor, SkyKey> getSkyKeysForAspectsAndCollectAspectPath(
      ImmutableList<AspectKey> keys,
      ImmutableList.Builder<SkyKey> aspectPathBuilder) {
    HashMap<AspectDescriptor, SkyKey> result = new HashMap<>();
    for (AspectKey key : keys) {
      buildSkyKeys(key, result, aspectPathBuilder);
    }
    return ImmutableMap.copyOf(result);
  }

  private void buildSkyKeys(AspectKey key, HashMap<AspectDescriptor, SkyKey> result,
      ImmutableList.Builder<SkyKey> aspectPathBuilder) {
    if (result.containsKey(key.getAspectDescriptor())) {
      return;
    }
    ImmutableList<AspectKey> baseKeys = key.getBaseKeys();
    result.put(key.getAspectDescriptor(), key);
    for (AspectKey baseKey : baseKeys) {
      buildSkyKeys(baseKey, result, aspectPathBuilder);
    }
    // Post-order list of aspect SkyKeys gives the order of propagating aspects:
    // the aspect comes after all aspects it transitively sees.
    aspectPathBuilder.add(key);
  }

  private SkyValue createAliasAspect(
      Environment env,
      Target originalTarget,
      Aspect aspect,
      AspectKey originalKey,
      ConfiguredTarget configuredTarget)
      throws InterruptedException {
    ImmutableList<Label> aliasChain = configuredTarget.getProvider(AliasProvider.class)
        .getAliasChain();
    // Find the next alias in the chain: either the next alias (if there are two) or the name of
    // the real configured target.
    Label aliasLabel = aliasChain.size() > 1 ? aliasChain.get(1) : configuredTarget.getLabel();

    return createAliasAspect(env, originalTarget, aspect, originalKey, aliasLabel);
  }

  private AspectValue createAliasAspect(
      Environment env,
      Target originalTarget,
      Aspect aspect,
      AspectKey originalKey,
      Label aliasLabel)
      throws InterruptedException {
    SkyKey depKey = originalKey.withLabel(aliasLabel);

    // Compute the AspectValue of the target the alias refers to (which can itself be either an
    // alias or a real target)
    AspectValue real = (AspectValue) env.getValue(depKey);
    if (env.valuesMissing()) {
      return null;
    }

    NestedSet<Package> transitivePackagesForPackageRootResolution =
        storeTransitivePackagesForPackageRootResolution
            ? NestedSetBuilder.<Package>stableOrder()
                .addTransitive(real.getTransitivePackagesForPackageRootResolution())
                .add(originalTarget.getPackage())
                .build()
            : null;

    return new AspectValue(
        originalKey,
        aspect,
        originalTarget.getLocation(),
        ConfiguredAspect.forAlias(real.getConfiguredAspect()),
        transitivePackagesForPackageRootResolution);
  }

  @Nullable
  private AspectValue createAspect(
      Environment env,
      AspectKey key,
      ImmutableList<Aspect> aspectPath,
      Aspect aspect,
      ConfiguredAspectFactory aspectFactory,
      ConfiguredTargetAndData associatedTarget,
      BuildConfiguration aspectConfiguration,
      ImmutableMap<Label, ConfigMatchingProvider> configConditions,
      ResolvedToolchainContext toolchainContext,
      OrderedSetMultimap<DependencyKind, ConfiguredTargetAndData> directDeps,
      @Nullable NestedSetBuilder<Package> transitivePackagesForPackageRootResolution)
      throws AspectFunctionException, InterruptedException {
    SkyframeBuildView view = buildViewProvider.getSkyframeBuildView();

    StoredEventHandler events = new StoredEventHandler();
    CachingAnalysisEnvironment analysisEnvironment = view.createAnalysisEnvironment(
        key, false, events, env, aspectConfiguration);
    if (env.valuesMissing()) {
      return null;
    }

    ConfiguredAspect configuredAspect;
    if (aspect.getDefinition().applyToGeneratingRules()
        && associatedTarget.getTarget() instanceof OutputFile) {
      OutputFile outputFile = (OutputFile) associatedTarget.getTarget();
      Label label = outputFile.getGeneratingRule().getLabel();
      return createAliasAspect(env, associatedTarget.getTarget(), aspect, key, label);
    } else if (AspectResolver.aspectMatchesConfiguredTarget(associatedTarget, aspect)) {
      try {
        CurrentRuleTracker.beginConfiguredAspect(aspect.getAspectClass());
        configuredAspect =
            view.getConfiguredTargetFactory()
                .createAspect(
                    analysisEnvironment,
                    associatedTarget,
                    aspectPath,
                    aspectFactory,
                    aspect,
                    directDeps,
                    configConditions,
                    toolchainContext,
                    aspectConfiguration,
                    view.getHostConfiguration(aspectConfiguration),
                    key);
      } catch (MissingDepException e) {
        Preconditions.checkState(env.valuesMissing());
        return null;
      } catch (ActionConflictException e) {
        throw new AspectFunctionException(e);
      } catch (InvalidExecGroupException e) {
        throw new AspectFunctionException(e);
      } finally {
        CurrentRuleTracker.endConfiguredAspect();
      }
    } else {
      configuredAspect = ConfiguredAspect.forNonapplicableTarget();
    }

    events.replayOn(env.getListener());
    if (events.hasErrors()) {
      analysisEnvironment.disable(associatedTarget.getTarget());
      String msg = "Analysis of target '" + associatedTarget.getTarget().getLabel() + "' failed";
      throw new AspectFunctionException(
          new AspectCreationException(msg, key.getLabel(), aspectConfiguration));
    }
    Preconditions.checkState(!analysisEnvironment.hasErrors(),
        "Analysis environment hasError() but no errors reported");

    if (env.valuesMissing()) {
      return null;
    }

    analysisEnvironment.disable(associatedTarget.getTarget());
    Preconditions.checkNotNull(configuredAspect);

    return new AspectValue(
        key,
        aspect,
        associatedTarget.getTarget().getLocation(),
        configuredAspect,
        transitivePackagesForPackageRootResolution == null
            ? null
            : transitivePackagesForPackageRootResolution.build());
  }

  @Override
  public String extractTag(SkyKey skyKey) {
    AspectKey aspectKey = (AspectKey) skyKey.argument();
    return Label.print(aspectKey.getLabel());
  }

  /** Used to indicate errors during the computation of an {@link AspectValue}. */
  public static final class AspectFunctionException extends SkyFunctionException {
    public AspectFunctionException(NoSuchThingException e) {
      super(e, Transience.PERSISTENT);
    }

    public AspectFunctionException(AspectCreationException e) {
      super(e, Transience.PERSISTENT);
    }

    public AspectFunctionException(InvalidExecGroupException e) {
      super(e, Transience.PERSISTENT);
    }

    public AspectFunctionException(ActionConflictException cause) {
      super(cause, Transience.PERSISTENT);
    }
  }
}
