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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.devtools.build.skyframe.EvaluationProgressReceiver.EvaluationState.BUILT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.eventbus.EventBus;
import com.google.devtools.build.lib.actions.ActionAnalysisMetadata;
import com.google.devtools.build.lib.actions.ActionKeyContext;
import com.google.devtools.build.lib.actions.ActionLookupKey;
import com.google.devtools.build.lib.actions.ActionLookupValue;
import com.google.devtools.build.lib.actions.ArtifactFactory;
import com.google.devtools.build.lib.actions.ArtifactPrefixConflictException;
import com.google.devtools.build.lib.actions.MutableActionGraph.ActionConflictException;
import com.google.devtools.build.lib.actions.PackageRoots;
import com.google.devtools.build.lib.analysis.AnalysisFailureEvent;
import com.google.devtools.build.lib.analysis.AspectValue;
import com.google.devtools.build.lib.analysis.BlazeDirectories;
import com.google.devtools.build.lib.analysis.CachingAnalysisEnvironment;
import com.google.devtools.build.lib.analysis.ConfiguredAspect;
import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.ConfiguredTargetFactory;
import com.google.devtools.build.lib.analysis.DependencyKind;
import com.google.devtools.build.lib.analysis.ResolvedToolchainContext;
import com.google.devtools.build.lib.analysis.RuleContext.InvalidExecGroupException;
import com.google.devtools.build.lib.analysis.ToolchainCollection;
import com.google.devtools.build.lib.analysis.TopLevelArtifactContext;
import com.google.devtools.build.lib.analysis.ViewCreationFailedException;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.analysis.config.BuildConfigurationCollection;
import com.google.devtools.build.lib.analysis.config.BuildOptions;
import com.google.devtools.build.lib.analysis.config.BuildOptions.OptionsDiff;
import com.google.devtools.build.lib.analysis.config.ConfigMatchingProvider;
import com.google.devtools.build.lib.analysis.config.CoreOptions;
import com.google.devtools.build.lib.analysis.config.FragmentClassSet;
import com.google.devtools.build.lib.bugreport.BugReport;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEventId.ConfigurationId;
import com.google.devtools.build.lib.buildtool.BuildRequestOptions;
import com.google.devtools.build.lib.causes.AnalysisFailedCause;
import com.google.devtools.build.lib.causes.Cause;
import com.google.devtools.build.lib.causes.LabelCause;
import com.google.devtools.build.lib.causes.LoadingFailedCause;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.collect.nestedset.Order;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.EventHandler;
import com.google.devtools.build.lib.events.ExtendedEventHandler;
import com.google.devtools.build.lib.packages.NoSuchPackageException;
import com.google.devtools.build.lib.packages.NoSuchTargetException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.Target;
import com.google.devtools.build.lib.pkgcache.LoadingFailureEvent;
import com.google.devtools.build.lib.profiler.Profiler;
import com.google.devtools.build.lib.profiler.SilentCloseable;
import com.google.devtools.build.lib.server.FailureDetails.Analysis;
import com.google.devtools.build.lib.server.FailureDetails.Analysis.Code;
import com.google.devtools.build.lib.server.FailureDetails.FailureDetail;
import com.google.devtools.build.lib.skyframe.ArtifactConflictFinder.ConflictException;
import com.google.devtools.build.lib.skyframe.AspectValueKey.AspectKey;
import com.google.devtools.build.lib.util.DetailedExitCode;
import com.google.devtools.build.lib.util.OrderedSetMultimap;
import com.google.devtools.build.lib.util.Pair;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.skyframe.CycleInfo;
import com.google.devtools.build.skyframe.ErrorInfo;
import com.google.devtools.build.skyframe.EvaluationProgressReceiver;
import com.google.devtools.build.skyframe.EvaluationResult;
import com.google.devtools.build.skyframe.SkyFunction.Environment;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import com.google.devtools.build.skyframe.WalkableGraph;
import com.google.devtools.common.options.OptionDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Skyframe-based driver of analysis.
 *
 * <p>Covers enough functionality to work as a substitute for {@code BuildView#configureTargets}.
 */
public final class SkyframeBuildView {
  private final ConfiguredTargetFactory factory;
  private final ArtifactFactory artifactFactory;
  private final SkyframeExecutor skyframeExecutor;
  private final ActionKeyContext actionKeyContext;
  private boolean enableAnalysis = false;

  // This hack allows us to see when a configured target has been invalidated, and thus when the set
  // of artifact conflicts needs to be recomputed (whenever a configured target has been invalidated
  // or newly evaluated).
  private final ConfiguredTargetValueProgressReceiver progressReceiver =
      new ConfiguredTargetValueProgressReceiver();
  // Used to see if checks of graph consistency need to be done after analysis.
  private volatile boolean someConfiguredTargetEvaluated = false;

  // We keep the set of invalidated configuration target keys so that we can know if something
  // has been invalidated after graph pruning has been executed.
  private Set<SkyKey> dirtiedConfiguredTargetKeys = Sets.newConcurrentHashSet();

  private final ConfiguredRuleClassProvider ruleClassProvider;

  // The host configuration containing all fragments used by this build's transitive closure.
  private BuildConfiguration topLevelHostConfiguration;
  // Fragment-limited versions of the host configuration. It's faster to create/cache these here
  // than to store them in Skyframe.
  private Map<BuildConfiguration, BuildConfiguration> hostConfigurationCache =
      Maps.newConcurrentMap();

  private BuildConfigurationCollection configurations;

  /**
   * If the last build was executed with {@code Options#discard_analysis_cache} and we are not
   * running Skyframe full, we should clear the legacy data since it is out-of-sync.
   */
  private boolean skyframeAnalysisWasDiscarded;

  private ImmutableSet<SkyKey> largestTopLevelKeySetCheckedForConflicts = ImmutableSet.of();
  private boolean foundActionConflict;

  public SkyframeBuildView(
      BlazeDirectories directories,
      SkyframeExecutor skyframeExecutor,
      ConfiguredRuleClassProvider ruleClassProvider,
      ActionKeyContext actionKeyContext) {
    this.actionKeyContext = actionKeyContext;
    this.factory = new ConfiguredTargetFactory(ruleClassProvider);
    this.artifactFactory =
        new ArtifactFactory(
            /* execRootParent= */ directories.getExecRootBase(),
            directories.getRelativeOutputPath());
    this.skyframeExecutor = skyframeExecutor;
    this.ruleClassProvider = ruleClassProvider;
  }

  public void resetProgressReceiver() {
    progressReceiver.reset();
  }

  public ImmutableSet<SkyKey> getEvaluatedTargetKeys() {
    return ImmutableSet.copyOf(progressReceiver.evaluatedConfiguredTargets);
  }

  ConfiguredTargetFactory getConfiguredTargetFactory() {
    return factory;
  }

  public int getEvaluatedActionCount() {
    return progressReceiver.evaluatedActionCount.get();
  }

  /**
   * Returns a description of the analysis-cache affecting changes between the current configuration
   * collection and the incoming one.
   *
   * @param maxDifferencesToShow the maximum number of change-affecting options to include in the
   *     returned description
   * @return a description or {@code null} if the configurations have not changed in a way that
   *     requires the analysis cache to be invalidated
   */
  @Nullable
  private String describeConfigurationDifference(
      BuildConfigurationCollection configurations, int maxDifferencesToShow) {
    if (this.configurations == null) {
      return null;
    }
    if (configurations.equals(this.configurations)) {
      return null;
    }

    ImmutableList<BuildConfiguration> oldTargetConfigs =
        this.configurations.getTargetConfigurations();
    ImmutableList<BuildConfiguration> newTargetConfigs = configurations.getTargetConfigurations();

    // TODO(schmitt): We are only checking the first of the new configurations, even though (through
    //  split transitions) we could have more than one. There is some special handling for
    //  --cpu changing below but other options may also be changed and should be covered.
    BuildConfiguration oldConfig = oldTargetConfigs.get(0);
    BuildConfiguration newConfig = newTargetConfigs.get(0);
    OptionsDiff diff = BuildOptions.diff(oldConfig.getOptions(), newConfig.getOptions());

    ImmutableSet<OptionDefinition> nativeCacheInvalidatingDifferences =
        getNativeCacheInvalidatingDifferences(oldTargetConfigs, newTargetConfigs, newConfig, diff);
    if (nativeCacheInvalidatingDifferences.isEmpty()
        && diff.getChangedStarlarkOptions().isEmpty()) {
      // The configuration may have changed, but none of the changes required a cache reset. For
      // example, test trimming was turned on and a test option changed. In this case, nothing needs
      // to be done.
      return null;
    }

    if (maxDifferencesToShow == 0) {
      return "Build options have changed";
    }

    ImmutableList<String> relevantDifferences =
        Streams.concat(
                diff.getChangedStarlarkOptions().stream().map(Label::getCanonicalForm),
                nativeCacheInvalidatingDifferences.stream().map(OptionDefinition::getOptionName))
            .map(s -> "--" + s)
            // Sorting the list to ensure that (if truncated through maxDifferencesToShow) the
            // options in the message remain stable.
            .sorted()
            .collect(toImmutableList());

    if (maxDifferencesToShow > 0 && relevantDifferences.size() > maxDifferencesToShow) {
      return String.format(
          "Build options %s%s and %d more have changed",
          Joiner.on(", ").join(relevantDifferences.subList(0, maxDifferencesToShow)),
          maxDifferencesToShow == 1 ? "" : ",",
          relevantDifferences.size() - maxDifferencesToShow);
    } else if (relevantDifferences.size() == 1) {
      return String.format(
          "Build option %s has changed", Iterables.getOnlyElement(relevantDifferences));
    } else if (relevantDifferences.size() == 2) {
      return String.format(
          "Build options %s have changed", Joiner.on(" and ").join(relevantDifferences));
    } else {
      return String.format(
          "Build options %s, and %s have changed",
          Joiner.on(", ").join(relevantDifferences.subList(0, relevantDifferences.size() - 1)),
          Iterables.getLast(relevantDifferences));
    }
  }

  // TODO(schmitt): This method assumes that the only option that can cause multiple target
  //  configurations is --cpu which (with the presence of split transitions) is no longer true.
  private ImmutableSet<OptionDefinition> getNativeCacheInvalidatingDifferences(
      ImmutableList<BuildConfiguration> oldTargetConfigs,
      ImmutableList<BuildConfiguration> newTargetConfigs,
      BuildConfiguration newConfig,
      OptionsDiff diff) {
    Stream<OptionDefinition> nativeCacheInvalidatingDifferences =
        diff.getFirst().keySet().stream()
            .filter(
                (definition) ->
                    ruleClassProvider.shouldInvalidateCacheForOptionDiff(
                        newConfig.getOptions(),
                        definition,
                        diff.getFirst().get(definition),
                        Iterables.getOnlyElement(diff.getSecond().get(definition))));

    // --experimental_multi_cpu is currently the only way to have multiple configurations, but this
    // code is unable to see whether or how it is set, only infer it from the presence of multiple
    // configurations before or after the values changed and look at what the cpus of those
    // configurations are set to.
    if (Math.max(oldTargetConfigs.size(), newTargetConfigs.size()) > 1) {
      // Ignore changes to --cpu for consistency - depending on the old and new values of
      // --experimental_multi_cpu and how the order of configurations falls, we may or may not
      // register a --cpu change in the diff, and --experimental_multi_cpu overrides --cpu
      // anyway so it's redundant information as long as we have --experimental_multi_cpu change
      // detection.
      nativeCacheInvalidatingDifferences =
          nativeCacheInvalidatingDifferences.filter(
              (definition) -> !CoreOptions.CPU.equals(definition));
      ImmutableSet<String> oldCpus =
          oldTargetConfigs.stream().map(BuildConfiguration::getCpu).collect(toImmutableSet());
      ImmutableSet<String> newCpus =
          newTargetConfigs.stream().map(BuildConfiguration::getCpu).collect(toImmutableSet());
      if (!Objects.equals(oldCpus, newCpus)) {
        // --experimental_multi_cpu has changed, so inject that in the diff stream.
        nativeCacheInvalidatingDifferences =
            Stream.concat(
                Stream.of(BuildRequestOptions.EXPERIMENTAL_MULTI_CPU),
                nativeCacheInvalidatingDifferences);
      }
    }
    return nativeCacheInvalidatingDifferences.collect(toImmutableSet());
  }

  /** Sets the configurations. Not thread-safe. DO NOT CALL except from tests! */
  @VisibleForTesting
  public void setConfigurations(
      EventHandler eventHandler,
      BuildConfigurationCollection configurations,
      int maxDifferencesToShow) {
    if (skyframeAnalysisWasDiscarded) {
      eventHandler.handle(
          Event.info(
              "--discard_analysis_cache was used in the previous build, "
              + "discarding analysis cache."));
      skyframeExecutor.handleAnalysisInvalidatingChange();
    } else {
      String diff = describeConfigurationDifference(configurations, maxDifferencesToShow);
      if (diff != null) {
        eventHandler.handle(Event.info(diff + ", discarding analysis cache."));
        // Note that clearing the analysis cache is currently required for correctness. It is also
        // helpful to save memory.
        //
        // If we had more memory, fixing the correctness issue (see also b/144932999) would allow us
        // to not invalidate the cache, leading to potentially better performance on incremental
        // builds.
        skyframeExecutor.handleAnalysisInvalidatingChange();
      }
    }
    if (configurations.getTargetConfigurations().stream()
        .anyMatch(BuildConfiguration::trimConfigurationsRetroactively)) {
      skyframeExecutor.activateRetroactiveTrimming();
    } else {
      skyframeExecutor.deactivateRetroactiveTrimming();
    }
    skyframeAnalysisWasDiscarded = false;
    this.configurations = configurations;
    setTopLevelHostConfiguration(configurations.getHostConfiguration());
  }

  @VisibleForTesting
  public BuildConfigurationCollection getBuildConfigurationCollection() {
    return configurations;
  }

  /**
   * Sets the host configuration consisting of all fragments that will be used by the top level
   * targets' transitive closures.
   *
   * <p>This is used to power {@link #getHostConfiguration} during analysis, which computes
   * fragment-trimmed host configurations from the top-level one.
   */
  private void setTopLevelHostConfiguration(BuildConfiguration topLevelHostConfiguration) {
    if (topLevelHostConfiguration.equals(this.topLevelHostConfiguration)) {
      return;
    }
    hostConfigurationCache.clear();
    this.topLevelHostConfiguration = topLevelHostConfiguration;
    skyframeExecutor.updateTopLevelHostConfiguration(topLevelHostConfiguration);
  }

  /**
   * Drops the analysis cache. If building with Skyframe, targets in {@code topLevelTargets} may
   * remain in the cache for use during the execution phase.
   *
   * @see com.google.devtools.build.lib.analysis.AnalysisOptions#discardAnalysisCache
   */
  public void clearAnalysisCache(
      Collection<ConfiguredTarget> topLevelTargets, ImmutableSet<AspectKey> topLevelAspects) {
    // TODO(bazel-team): Consider clearing packages too to save more memory.
    skyframeAnalysisWasDiscarded = true;
    skyframeExecutor.clearAnalysisCache(topLevelTargets, topLevelAspects);
  }

  /**
   * Analyzes the specified targets using Skyframe as the driving framework.
   *
   * @return the configured targets that should be built along with a WalkableGraph of the analysis.
   */
  public SkyframeAnalysisResult configureTargets(
      ExtendedEventHandler eventHandler,
      List<ConfiguredTargetKey> ctKeys,
      List<AspectValueKey> aspectKeys,
      Supplier<Map<BuildConfigurationValue.Key, BuildConfiguration>> configurationLookupSupplier,
      TopLevelArtifactContext topLevelArtifactContextForConflictPruning,
      EventBus eventBus,
      boolean keepGoing,
      int numThreads,
      boolean strictConflictChecks)
      throws InterruptedException, ViewCreationFailedException {
    enableAnalysis(true);
    EvaluationResult<ActionLookupValue> result;
    try (SilentCloseable c = Profiler.instance().profile("skyframeExecutor.configureTargets")) {
      result =
          skyframeExecutor.configureTargets(
              eventHandler, ctKeys, aspectKeys, keepGoing, numThreads);
    } finally {
      enableAnalysis(false);
    }

    Map<AspectKey, ConfiguredAspect> aspects = Maps.newHashMapWithExpectedSize(aspectKeys.size());
    Root singleSourceRoot = skyframeExecutor.getForcedSingleSourceRootIfNoExecrootSymlinkCreation();
    NestedSetBuilder<Package> packages =
        singleSourceRoot == null ? NestedSetBuilder.stableOrder() : null;
    for (AspectValueKey aspectKey : aspectKeys) {
      AspectValue value = (AspectValue) result.get(aspectKey);
      if (value == null) {
        // Skip aspects that couldn't be applied to targets.
        continue;
      }
      aspects.put(value.getKey(), value.getConfiguredAspect());
      if (packages != null) {
        packages.addTransitive(value.getTransitivePackagesForPackageRootResolution());
      }
    }

    Collection<ConfiguredTarget> cts = Lists.newArrayListWithCapacity(ctKeys.size());
    for (ConfiguredTargetKey value : ctKeys) {
      ConfiguredTargetValue ctValue = (ConfiguredTargetValue) result.get(value);
      if (ctValue == null) {
        continue;
      }
      cts.add(ctValue.getConfiguredTarget());
      if (packages != null) {
        packages.addTransitive(ctValue.getTransitivePackagesForPackageRootResolution());
      }
    }
    PackageRoots packageRoots =
        singleSourceRoot == null
            ? new MapAsPackageRoots(collectPackageRoots(packages.build().toList()))
            : new PackageRootsNoSymlinkCreation(singleSourceRoot);

    ImmutableMap<ActionAnalysisMetadata, ConflictException> actionConflicts = ImmutableMap.of();
    try (SilentCloseable c =
        Profiler.instance().profile("skyframeExecutor.findArtifactConflicts")) {
      ImmutableSet<SkyKey> newKeys =
          ImmutableSet.<SkyKey>builderWithExpectedSize(ctKeys.size() + aspectKeys.size())
              .addAll(ctKeys)
              .addAll(aspectKeys)
              .build();
      if (shouldCheckForConflicts(newKeys)) {
        largestTopLevelKeySetCheckedForConflicts = newKeys;
        // This operation is somewhat expensive, so we only do it if the graph might have changed in
        // some way -- either we analyzed a new target or we invalidated an old one or are building
        // targets together that haven't been built before.
        actionConflicts =
            ArtifactConflictFinder.findAndStoreArtifactConflicts(
                skyframeExecutor.getActionLookupValuesInBuild(ctKeys, aspectKeys),
                strictConflictChecks,
                actionKeyContext);
        someConfiguredTargetEvaluated = false;
      }
    }
    foundActionConflict = !actionConflicts.isEmpty();

    if (!result.hasError() && !foundActionConflict) {
      return new SkyframeAnalysisResult(
          /*hasLoadingError=*/ false,
          /*hasAnalysisError=*/ false,
          foundActionConflict,
          ImmutableList.copyOf(cts),
          result.getWalkableGraph(),
          ImmutableMap.copyOf(aspects),
          packageRoots);
    }

    Pair<Boolean, ViewCreationFailedException> errors =
        processErrors(
            result,
            configurationLookupSupplier,
            skyframeExecutor,
            eventHandler,
            keepGoing,
            eventBus);
    Collection<Exception> reportedExceptions = Sets.newHashSet();
    for (Map.Entry<ActionAnalysisMetadata, ConflictException> bad : actionConflicts.entrySet()) {
      ConflictException ex = bad.getValue();
      DetailedExitCode detailedExitCode;
      try {
        ex.rethrowTyped();
        throw new IllegalStateException("ConflictException.rethrowTyped must throw");
      } catch (ActionConflictException ace) {
        detailedExitCode = ace.getDetailedExitCode();
        ace.reportTo(eventHandler);
        if (keepGoing) {
          eventHandler.handle(
              Event.warn(
                  "errors encountered while analyzing target '"
                      + bad.getKey().getOwner().getLabel()
                      + "': it will not be built"));
        }
      } catch (ArtifactPrefixConflictException apce) {
        detailedExitCode = apce.getDetailedExitCode();
        if (reportedExceptions.add(apce)) {
          eventHandler.handle(Event.error(apce.getMessage()));
        }
      }
      // TODO(ulfjack): Don't throw here in the nokeep_going case, but report all known issues.
      if (!keepGoing) {
        throw new ViewCreationFailedException(detailedExitCode.getFailureDetail(), ex);
      }
    }

    // This is here for backwards compatibility. The keep_going and nokeep_going code paths were
    // checking action conflicts and analysis errors in different orders, so we only throw the
    // analysis error here after first throwing action conflicts.
    if (!keepGoing) {
      throw errors.second;
    }

    if (foundActionConflict) {
      // In order to determine the set of configured targets transitively error free from action
      // conflict issues, we run a post-processing update() that uses the bad action map.
      Predicate<ActionLookupKey> errorFreePredicate;
      enableAnalysis(true);
      try {
        errorFreePredicate =
            skyframeExecutor.filterActionConflictsForConfiguredTargetsAndAspects(
                eventHandler,
                Iterables.concat(ctKeys, aspectKeys),
                actionConflicts,
                topLevelArtifactContextForConflictPruning);
      } finally {
        enableAnalysis(false);
      }

      cts =
          ctKeys.stream()
              .filter(errorFreePredicate)
              .map(
                  k ->
                      Preconditions.checkNotNull((ConfiguredTargetValue) result.get(k), k)
                          .getConfiguredTarget())
              .collect(toImmutableList());

      aspects =
          aspectKeys.stream()
              .filter(errorFreePredicate)
              .map(result::get)
              .map(AspectValue.class::cast)
              .collect(
                  ImmutableMap.toImmutableMap(
                      AspectValue::getKey, AspectValue::getConfiguredAspect));
    }

    return new SkyframeAnalysisResult(
        errors.first,
        result.hasError() || foundActionConflict,
        foundActionConflict,
        ImmutableList.copyOf(cts),
        result.getWalkableGraph(),
        ImmutableMap.copyOf(aspects),
        packageRoots);
  }

  private boolean shouldCheckForConflicts(ImmutableSet<SkyKey> newKeys) {
    if (someConfiguredTargetEvaluated) {
      // A top-level target was added and may introduce a conflict, or a top-level target was
      // recomputed and may introduce or resolve a conflict.
      return true;
    }

    if (!dirtiedConfiguredTargetKeys.isEmpty()) {
      // No target was (re)computed but at least one was dirtied.
      // Example: (//:x //foo:y) are built, and in conflict (//:x creates foo/C and //foo:y
      // creates C). Then y is removed from foo/BUILD and only //:x is built, so //foo:y is
      // dirtied but not recomputed, and no other nodes are recomputed (and none are deleted).
      // Still we must do the conflict checking because previously there was a conflict but now
      // there isn't.
      return true;
    }

    if (foundActionConflict) {
      // Example sequence:
      // 1.  Build (x y z), and there is a conflict. We store (x y z) as the largest checked key
      //     set, and record the fact that there were bad actions.
      // 2.  Null-build (x z), so we don't evaluate or dirty anything, but because we know there was
      //     some conflict last time but don't know exactly which targets conflicted, it could have
      //     been (x z), so we now check again.
      return true;
    }

    if (!largestTopLevelKeySetCheckedForConflicts.containsAll(newKeys)) {
      // Example sequence:
      // 1.  Build (x y z), and there is a conflict. We store (x y z) as the largest checked key
      //     set, and record the fact that there were bad actions.
      // 2.  Null-build (x z), so we don't evaluate or dirty anything, but because we know there was
      //     some conflict last time but don't know exactly which targets conflicted, it could have
      //     been (x z), so we now check again, and store (x z) as the largest checked key set.
      // 3.  Null-build (y z), so again we don't evaluate or dirty anything, and the previous build
      //     had no conflicts, so no other condition is true. But because (y z) is not a subset of
      //     (x z) and we only keep the most recent largest checked key set, we don't know if (y z)
      //     are conflict free, so we check.
      return true;
    }

    // We believe the conditions above are correct in the sense that we always check for conflicts
    // when we have to. But they are incomplete, so we sometimes check for conflicts even if we
    // wouldn't have to. For example:
    // - if no target was evaluated nor dirtied and build sequence is (x y) [no conflict], (z),
    //   where z is in the transitive closure of (x y), then we shouldn't check.
    // - if no target was evaluated nor dirtied and build sequence is (x y) [no conflict], (w), (x),
    //   then the last build shouldn't conflict-check because (x y) was checked earlier. But it
    //   does, because after the second build we store (w) as the largest checked set, and (x) is
    //   not a subset of that.

    // Case when we DON'T need to re-check:
    // - a configured target is deleted. Deletion can only resolve conflicts, not introduce any, and
    //   if the previuos build had a conflict then foundActionConflict would be true, and if the
    //   previous build had no conflict then deleting a CT won't change that.
    //   Example that triggers this scenario:
    //   1.  genrule(name='x', srcs=['A'], ...)
    //       genrule(name='y', outs=['A'], ...)
    //   2.  Build (x y)
    //   3.  Rename 'x' to 'y', and 'y' to 'z'
    //   4.  Build (y z)
    //   5.  Null-build (y z) again
    // We only delete the old 'x' value in (5), and we don't evaluate nor dirty anything, nor was
    // (4) bad. So there's no reason to re-check just because we deleted something.
    return false;
  }

  /**
   * Process errors encountered during analysis, and return a {@link Pair} indicating the existence
   * of a loading-phase error, if any, and an exception to be thrown to halt the build, if {@code
   * keepGoing} is false.
   *
   * <p>Visible only for use by tests via {@link
   * SkyframeExecutor#getConfiguredTargetMapForTesting(ExtendedEventHandler, BuildConfiguration,
   * Iterable)}. When called there, {@code eventBus} must be null to indicate that this is a test,
   * and so there may be additional {@link SkyKey}s in the {@code result} that are not {@link
   * AspectValueKey}s or {@link ConfiguredTargetKey}s. Those keys will be ignored.
   */
  static Pair<Boolean, ViewCreationFailedException> processErrors(
      EvaluationResult<? extends SkyValue> result,
      Supplier<Map<BuildConfigurationValue.Key, BuildConfiguration>> configurationLookupSupplier,
      SkyframeExecutor skyframeExecutor,
      ExtendedEventHandler eventHandler,
      boolean keepGoing,
      @Nullable EventBus eventBus)
      throws InterruptedException {
    boolean inTest = eventBus == null;
    boolean hasLoadingError = false;
    ViewCreationFailedException noKeepGoingException = null;
    for (Map.Entry<SkyKey, ErrorInfo> errorEntry : result.errorMap().entrySet()) {
      SkyKey errorKey = errorEntry.getKey();
      ErrorInfo errorInfo = errorEntry.getValue();
      assertValidAnalysisException(errorInfo, errorKey, result.getWalkableGraph());
      skyframeExecutor
          .getCyclesReporter().reportCycles(errorInfo.getCycleInfo(), errorKey, eventHandler);
      Exception cause = errorInfo.getException();
      Preconditions.checkState(cause != null || !errorInfo.getCycleInfo().isEmpty(), errorInfo);

      if (errorKey.argument() instanceof AspectValueKey) {
        // We skip Aspects in the keepGoing case; the failures should already have been reported to
        // the event handler.
        if (!keepGoing && noKeepGoingException == null) {
          AspectValueKey aspectKey = (AspectValueKey) errorKey.argument();
          String errorMsg =
              String.format(
                  "Analysis of aspect '%s' failed; build aborted", aspectKey.getDescription());
          noKeepGoingException = createViewCreationFailedException(cause, errorMsg);
        }
        continue;
      }

      if (inTest && !(errorKey.argument() instanceof ConfiguredTargetKey)) {
        // This means that we are in a BuildViewTestCase.
        //
        // Tests don't call target pattern parsing before requesting the analysis of a target.
        // Therefore if the package that contains them cannot be loaded, we get an error key that's
        // not a ConfiguredTargetKey, which cannot happen in production code.
        //
        // If it's an existing target in a nonexistent package, the error is signaled by posting an
        // AnalysisFailureEvent on the event bus, which is null in when running a BuildViewTestCase,
        // so we emit the root cause labels directly to the event handler below.
        eventHandler.handle(Event.error(errorInfo.toString()));
        continue;
      }
      Preconditions.checkState(
          errorKey.argument() instanceof ConfiguredTargetKey,
          "expected '%s' to be a AspectValueKey or ConfiguredTargetKey",
          errorKey.argument());
      ConfiguredTargetKey label = (ConfiguredTargetKey) errorKey.argument();
      Label topLevelLabel = label.getLabel();

      NestedSet<Cause> rootCauses;
      if (cause instanceof ConfiguredValueCreationException) {
        ConfiguredValueCreationException ctCause = (ConfiguredValueCreationException) cause;
        // Previously, the nested set was de-duplicating loading root cause labels. Now that we
        // track Cause instances including a message, we get one event per label and message. In
        // order to keep backwards compatibility, we de-duplicate root cause labels here.
        // TODO(ulfjack): Remove this code once we've migrated to the BEP.
        Set<Label> loadingRootCauses = new HashSet<>();
        for (Cause rootCause : ctCause.getRootCauses().toList()) {
          if (rootCause instanceof LoadingFailedCause) {
            hasLoadingError = true;
            loadingRootCauses.add(rootCause.getLabel());
          }
        }
        if (!inTest) {
          for (Label loadingRootCause : loadingRootCauses) {
            // This event is only for backwards compatibility with the old event protocol. Remove
            // once we've migrated to the build event protocol.
            eventBus.post(new LoadingFailureEvent(topLevelLabel, loadingRootCause));
          }
        }
        rootCauses = ctCause.getRootCauses();
      } else if (!errorInfo.getCycleInfo().isEmpty()) {
        Label analysisRootCause =
            maybeGetConfiguredTargetCycleCulprit(topLevelLabel, errorInfo.getCycleInfo());
        rootCauses =
            analysisRootCause != null
                ? NestedSetBuilder.create(
                    Order.STABLE_ORDER,
                    new LabelCause(
                        analysisRootCause,
                        DetailedExitCode.of(createFailureDetail("Dependency cycle", Code.CYCLE))))
                // TODO(ulfjack): We need to report the dependency cycle here. How?
                : NestedSetBuilder.emptySet(Order.STABLE_ORDER);
      } else if (cause instanceof ActionConflictException) {
        ((ActionConflictException) cause).reportTo(eventHandler);
        // TODO(ulfjack): Report the action conflict.
        rootCauses = NestedSetBuilder.emptySet(Order.STABLE_ORDER);
      } else if (cause instanceof NoSuchPackageException) {
        // This branch is only taken in --nokeep_going builds. In a --keep_going build, the
        // AnalysisFailedCause is properly reported through the ConfiguredValueCreationException.
        BuildConfiguration configuration =
            configurationLookupSupplier.get().get(label.getConfigurationKey());
        ConfigurationId configId = configuration.getEventId().getConfiguration();
        AnalysisFailedCause analysisFailedCause =
            new AnalysisFailedCause(
                topLevelLabel, configId, ((NoSuchPackageException) cause).getDetailedExitCode());
        rootCauses = NestedSetBuilder.create(Order.STABLE_ORDER, analysisFailedCause);
      } else {
        // TODO(ulfjack): Report something!
        rootCauses = NestedSetBuilder.emptySet(Order.STABLE_ORDER);
      }

      if (keepGoing) {
        eventHandler.handle(
            Event.warn(
                "errors encountered while analyzing target '"
                    + topLevelLabel
                    + "': it will not be built"));
      } else if (noKeepGoingException == null) {
        String errorMsg =
            String.format("Analysis of target '%s' failed; build aborted", topLevelLabel);
        noKeepGoingException = createViewCreationFailedException(cause, errorMsg);
      }

      if (!inTest) {
        BuildConfiguration configuration =
            configurationLookupSupplier.get().get(label.getConfigurationKey());
        eventBus.post(
            new AnalysisFailureEvent(
                label, configuration == null ? null : configuration.getEventId(), rootCauses));
      } else {
        // eventBus is null, but test can still assert on the expected root causes being found.
        eventHandler.handle(Event.error(rootCauses.toList().toString()));
      }
    }
    return Pair.of(hasLoadingError, noKeepGoingException);
  }

  private static ViewCreationFailedException createViewCreationFailedException(
      @Nullable Exception e, String errorMsg) {
    if (e == null) {
      return new ViewCreationFailedException(
          errorMsg, createFailureDetail(errorMsg + " due to cycle", Code.CYCLE));
    }
    return new ViewCreationFailedException(
        errorMsg, maybeContextualizeFailureDetail(e, errorMsg), e);
  }

  /**
   * Returns a {@link FailureDetail} with message prefixed by {@code errorMsg} derived from the
   * failure detail in {@code e} if it's a {@link DetailedException}, and otherwise returns one with
   * {@code errorMsg} and {@link Code#UNEXPECTED_ANALYSIS_EXCEPTION}.
   */
  private static FailureDetail maybeContextualizeFailureDetail(
      @Nullable Exception e, String errorMsg) {
    DetailedException detailedException = convertToAnalysisException(e);
    if (detailedException == null) {
      return createFailureDetail(errorMsg, Code.UNEXPECTED_ANALYSIS_EXCEPTION);
    }
    FailureDetail originalFailureDetail =
        detailedException.getDetailedExitCode().getFailureDetail();
    return originalFailureDetail.toBuilder()
        .setMessage(errorMsg + ": " + originalFailureDetail.getMessage())
        .build();
  }

  private static FailureDetail createFailureDetail(String errorMessage, Code code) {
    return FailureDetail.newBuilder()
        .setMessage(errorMessage)
        .setAnalysis(Analysis.newBuilder().setCode(code))
        .build();
  }

  /** Returns a map of collected package names to root paths. */
  private static ImmutableMap<PackageIdentifier, Root> collectPackageRoots(
      Collection<Package> packages) {
    // Make a map of the package names to their root paths.
    ImmutableMap.Builder<PackageIdentifier, Root> packageRoots = ImmutableMap.builder();
    for (Package pkg : packages) {
      if (pkg.getSourceRoot().isPresent()) {
        packageRoots.put(pkg.getPackageIdentifier(), pkg.getSourceRoot().get());
      }
    }
    return packageRoots.build();
  }

  @Nullable
  private static Label maybeGetConfiguredTargetCycleCulprit(
      Label labelToLoad, Iterable<CycleInfo> cycleInfos) {
    for (CycleInfo cycleInfo : cycleInfos) {
      SkyKey culprit = Iterables.getFirst(cycleInfo.getCycle(), null);
      if (culprit == null) {
        continue;
      }
      if (culprit.functionName().equals(SkyFunctions.CONFIGURED_TARGET)) {
        return ((ConfiguredTargetKey) culprit.argument()).getLabel();
      } else if (culprit.functionName().equals(SkyFunctions.TRANSITIVE_TARGET)) {
        return ((TransitiveTargetKey) culprit).getLabel();
      } else {
        return labelToLoad;
      }
    }
    return null;
  }

  private static void assertValidAnalysisException(
      ErrorInfo errorInfo, SkyKey key, WalkableGraph walkableGraph) throws InterruptedException {
    Throwable cause = errorInfo.getException();
    if (cause == null) {
      // Cycle.
      return;
    }

    if (convertToAnalysisException(cause) != null) {
      // Valid exception type.
      return;
    }

    // Walk the graph to find a path to the lowest-level node that threw unexpected exception.
    List<SkyKey> path = new ArrayList<>();
    try {
      SkyKey currentKey = key;
      boolean foundDep;
      do {
        path.add(currentKey);
        foundDep = false;

        Map<SkyKey, Exception> missingMap =
            walkableGraph.getMissingAndExceptions(ImmutableList.of(currentKey));
        if (missingMap.containsKey(currentKey) && missingMap.get(currentKey) == null) {
          // This can happen in a no-keep-going build, where we don't write the bubbled-up error
          // nodes to the graph.
          break;
        }

        for (SkyKey dep : walkableGraph.getDirectDeps(currentKey)) {
          if (cause.equals(walkableGraph.getException(dep))) {
            currentKey = dep;
            foundDep = true;
            break;
          }
        }
      } while (foundDep);
    } finally {
      BugReport.sendBugReport(
          new IllegalStateException(
              "Unexpected analysis error: " + key + " -> " + errorInfo + ", (" + path + ")"));
    }
  }

  @Nullable
  private static DetailedException convertToAnalysisException(Throwable cause) {
    // The cause may be NoSuch{Target,Package}Exception if we run the reduced loading phase and then
    // analyze with --nokeep_going.
    if (cause instanceof SaneAnalysisException
        || cause instanceof NoSuchTargetException
        || cause instanceof NoSuchPackageException) {
      return (DetailedException) cause;
    }
    return null;
  }

  public ArtifactFactory getArtifactFactory() {
    return artifactFactory;
  }

  CachingAnalysisEnvironment createAnalysisEnvironment(
      ActionLookupKey owner,
      boolean isSystemEnv,
      ExtendedEventHandler eventHandler,
      Environment env,
      BuildConfiguration config) {
    boolean extendedSanityChecks = config != null && config.extendedSanityChecks();
    boolean allowAnalysisFailures = config != null && config.allowAnalysisFailures();
    return new CachingAnalysisEnvironment(
        artifactFactory,
        skyframeExecutor.getActionKeyContext(),
        owner,
        isSystemEnv,
        extendedSanityChecks,
        allowAnalysisFailures,
        eventHandler,
        env);
  }

  /**
   * Invokes the appropriate constructor to create a {@link ConfiguredTarget} instance.
   *
   * <p>For use in {@code ConfiguredTargetFunction}.
   *
   * <p>Returns null if Skyframe deps are missing or upon certain errors.
   */
  @Nullable
  ConfiguredTarget createConfiguredTarget(
      Target target,
      BuildConfiguration configuration,
      CachingAnalysisEnvironment analysisEnvironment,
      ConfiguredTargetKey configuredTargetKey,
      OrderedSetMultimap<DependencyKind, ConfiguredTargetAndData> prerequisiteMap,
      ImmutableMap<Label, ConfigMatchingProvider> configConditions,
      @Nullable ToolchainCollection<ResolvedToolchainContext> toolchainContexts)
      throws InterruptedException, ActionConflictException, InvalidExecGroupException {
    Preconditions.checkState(
        enableAnalysis, "Already in execution phase %s %s", target, configuration);
    Preconditions.checkNotNull(analysisEnvironment);
    Preconditions.checkNotNull(target);
    Preconditions.checkNotNull(prerequisiteMap);
    return factory.createConfiguredTarget(
        analysisEnvironment,
        artifactFactory,
        target,
        configuration,
        getHostConfiguration(configuration),
        configuredTargetKey,
        prerequisiteMap,
        configConditions,
        toolchainContexts);
  }

  /**
   * Returns the host configuration trimmed to the same fragments as the input configuration. If
   * the input is null, returns the top-level host configuration.
   *
   * <p>This may only be called after {@link #setTopLevelHostConfiguration} has set the
   * correct host configuration at the top-level.
   */
  public BuildConfiguration getHostConfiguration(BuildConfiguration config) {
    if (config == null) {
      return topLevelHostConfiguration;
    }
    // Currently, a single build doesn't use many different BuildConfiguration instances. Thus,
    // having a cache per BuildConfiguration is efficient. It might lead to instances of otherwise
    // identical configurations if multiple of these configs use the same fragment classes. However,
    // these are cheap especially if there is only a small number of configs. Revisit and turn into
    // a cache per FragmentClassSet if configuration trimming results in a much higher number of
    // configuration instances.
    BuildConfiguration hostConfig = hostConfigurationCache.get(config);
    if (hostConfig != null) {
      return hostConfig;
    }
    // TODO(bazel-team): have the fragment classes be those required by the consuming target's
    // transitive closure. This isn't the same as the input configuration's fragment classes -
    // the latter may be a proper subset of the former.
    //
    // ConfigurationFactory.getConfiguration provides the reason why: if a declared required
    // fragment is evaluated and returns null, it never gets added to the configuration. So if we
    // use the configuration's fragments as the source of truth, that excludes required fragments
    // that never made it in.
    //
    // If we're just trimming an existing configuration, this is no big deal (if the original
    // configuration doesn't need the fragment, the trimmed one doesn't either). But this method
    // trims a host configuration to the same scope as a target configuration. Since their options
    // are different, the host instance may actually be able to produce the fragment. So it's
    // wrong and potentially dangerous to unilaterally exclude it.
    FragmentClassSet fragmentClasses =
        config.trimConfigurations()
            ? config.fragmentClasses()
            : FragmentClassSet.of(ruleClassProvider.getAllFragments());
    // TODO(bazel-team): investigate getting the trimmed config from Skyframe instead of cloning.
    // This is the only place we instantiate BuildConfigurations outside of Skyframe, This can
    // produce surprising effects, such as requesting a configuration that's in the Skyframe cache
    // but still produces a unique instance because we don't check that cache. It'd be nice to
    // guarantee that *all* instantiations happen through Skyframe. That could, for example,
    // guarantee that config1.equals(config2) implies config1 == config2, which is nice for
    // verifying we don't accidentally create extra configurations. But unfortunately,
    // hostConfigurationCache was specifically created because Skyframe is too slow for this use
    // case. So further optimization is necessary to make that viable (proto_library in particular
    // contributes to much of the difference).
    BuildConfiguration trimmedConfig =
        topLevelHostConfiguration.clone(
            fragmentClasses, ruleClassProvider, skyframeExecutor.getDefaultBuildOptions());
    hostConfigurationCache.put(config, trimmedConfig);
    return trimmedConfig;
  }

  SkyframeDependencyResolver createDependencyResolver(Environment env) {
    return new SkyframeDependencyResolver(env);
  }

  /**
   * Workaround to clear all legacy data, like the artifact factory. We need
   * to clear them to avoid conflicts.
   * TODO(bazel-team): Remove this workaround. [skyframe-execution]
   */
  void clearLegacyData() {
    artifactFactory.clear();
  }

  /**
   * Clears any data cached in this BuildView. To be called when the attached SkyframeExecutor is
   * reset.
   */
  void reset() {
    configurations = null;
    skyframeAnalysisWasDiscarded = false;
    clearLegacyData();
  }

  /**
   * Hack to invalidate actions in legacy action graph when their values are invalidated in
   * skyframe.
   */
  EvaluationProgressReceiver getProgressReceiver() {
    return progressReceiver;
  }

  /** Clear the invalidated configured targets detected during loading and analysis phases. */
  public void clearInvalidatedConfiguredTargets() {
    dirtiedConfiguredTargetKeys = Sets.newConcurrentHashSet();
  }

  /**
   * {@link #createConfiguredTarget} will only create configured targets if this is set to true. It
   * should be set to true before any Skyframe update call that might call into {@link
   * #createConfiguredTarget}, and false immediately after the call. Use it to fail-fast in the case
   * that a target is requested for analysis not during the analysis phase.
   */
  public void enableAnalysis(boolean enable) {
    this.enableAnalysis = enable;
  }

  public ActionKeyContext getActionKeyContext() {
    return skyframeExecutor.getActionKeyContext();
  }

  private final class ConfiguredTargetValueProgressReceiver
      extends EvaluationProgressReceiver.NullEvaluationProgressReceiver {
    private final Set<SkyKey> evaluatedConfiguredTargets = Sets.newConcurrentHashSet();
    private final AtomicInteger evaluatedActionCount = new AtomicInteger();

    @Override
    public void invalidated(SkyKey skyKey, InvalidationState state) {
      if (skyKey.functionName().equals(SkyFunctions.CONFIGURED_TARGET)
          && state != InvalidationState.DELETED) {
        // If the value was just dirtied and not deleted, then it may not be truly invalid, since
        // it may later get re-validated. Therefore adding the key to dirtiedConfiguredTargetKeys
        // is provisional--if the key is later evaluated and the value found to be clean, then we
        // remove it from the set.
        dirtiedConfiguredTargetKeys.add(skyKey);
      }
    }

    @Override
    public void evaluated(
        SkyKey skyKey,
        @Nullable SkyValue value,
        Supplier<EvaluationSuccessState> evaluationSuccessState,
        EvaluationState state) {
      if (skyKey.functionName().equals(SkyFunctions.CONFIGURED_TARGET)) {
        switch (state) {
          case BUILT:
            if (evaluationSuccessState.get().succeeded()) {
              evaluatedConfiguredTargets.add(skyKey);
              // During multithreaded operation, this is only set to true, so no concurrency issues.
              someConfiguredTargetEvaluated = true;
            }
            if (value instanceof ConfiguredTargetValue) {
              evaluatedActionCount.addAndGet(((ConfiguredTargetValue) value).getNumActions());
            }
            break;
          case CLEAN:
            // If the configured target value did not need to be rebuilt, then it wasn't truly
            // invalid.
            dirtiedConfiguredTargetKeys.remove(skyKey);
            break;
        }
      } else if (skyKey.functionName().equals(SkyFunctions.ASPECT)
          && state == BUILT
          && value instanceof AspectValue) {
        evaluatedActionCount.addAndGet(((AspectValue) value).getNumActions());
      }
    }

    public void reset() {
      evaluatedConfiguredTargets.clear();
      evaluatedActionCount.set(0);
    }
  }
}
