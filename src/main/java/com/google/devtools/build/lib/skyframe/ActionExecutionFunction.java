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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.devtools.build.lib.actions.Action;
import com.google.devtools.build.lib.actions.ActionCacheChecker.Token;
import com.google.devtools.build.lib.actions.ActionCompletionEvent;
import com.google.devtools.build.lib.actions.ActionExecutedEvent;
import com.google.devtools.build.lib.actions.ActionExecutedEvent.ErrorTiming;
import com.google.devtools.build.lib.actions.ActionExecutionException;
import com.google.devtools.build.lib.actions.ActionInput;
import com.google.devtools.build.lib.actions.ActionInputDepOwnerMap;
import com.google.devtools.build.lib.actions.ActionInputDepOwners;
import com.google.devtools.build.lib.actions.ActionInputMap;
import com.google.devtools.build.lib.actions.ActionInputMapSink;
import com.google.devtools.build.lib.actions.ActionLookupData;
import com.google.devtools.build.lib.actions.ActionRewoundEvent;
import com.google.devtools.build.lib.actions.Actions;
import com.google.devtools.build.lib.actions.AlreadyReportedActionExecutionException;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.Artifact.ArchivedTreeArtifact;
import com.google.devtools.build.lib.actions.Artifact.ArtifactExpander;
import com.google.devtools.build.lib.actions.Artifact.SpecialArtifact;
import com.google.devtools.build.lib.actions.ArtifactPathResolver;
import com.google.devtools.build.lib.actions.DiscoveredInputsEvent;
import com.google.devtools.build.lib.actions.FileArtifactValue;
import com.google.devtools.build.lib.actions.FilesetOutputSymlink;
import com.google.devtools.build.lib.actions.LostInputsActionExecutionException;
import com.google.devtools.build.lib.actions.MissingDepException;
import com.google.devtools.build.lib.actions.MissingInputFileException;
import com.google.devtools.build.lib.actions.PackageRootResolver;
import com.google.devtools.build.lib.actions.SpawnMetrics;
import com.google.devtools.build.lib.actionsketch.ActionSketch;
import com.google.devtools.build.lib.analysis.BlazeDirectories;
import com.google.devtools.build.lib.bugreport.BugReport;
import com.google.devtools.build.lib.causes.Cause;
import com.google.devtools.build.lib.causes.LabelCause;
import com.google.devtools.build.lib.clock.BlazeClock;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.collect.compacthashset.CompactHashSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.collect.nestedset.Order;
import com.google.devtools.build.lib.events.ExtendedEventHandler;
import com.google.devtools.build.lib.packages.semantics.BuildLanguageOptions;
import com.google.devtools.build.lib.profiler.Profiler;
import com.google.devtools.build.lib.profiler.ProfilerTask;
import com.google.devtools.build.lib.profiler.SilentCloseable;
import com.google.devtools.build.lib.rules.cpp.IncludeScannable;
import com.google.devtools.build.lib.server.FailureDetails.Execution;
import com.google.devtools.build.lib.server.FailureDetails.Execution.Code;
import com.google.devtools.build.lib.server.FailureDetails.FailureDetail;
import com.google.devtools.build.lib.skyframe.ActionRewindStrategy.RewindPlan;
import com.google.devtools.build.lib.skyframe.ArtifactFunction.MissingFileArtifactValue;
import com.google.devtools.build.lib.skyframe.ArtifactNestedSetFunction.ArtifactNestedSetEvalException;
import com.google.devtools.build.lib.skyframe.SkyframeActionExecutor.ActionPostprocessing;
import com.google.devtools.build.lib.util.DetailedExitCode;
import com.google.devtools.build.lib.util.DetailedExitCode.DetailedExitCodeComparator;
import com.google.devtools.build.lib.util.Pair;
import com.google.devtools.build.lib.util.io.FileOutErr;
import com.google.devtools.build.lib.util.io.TimestampGranularityMonitor;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import com.google.devtools.build.skyframe.ValueOrException;
import com.google.devtools.build.skyframe.ValueOrException2;
import com.google.devtools.build.skyframe.ValueOrException3;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.starlark.java.eval.StarlarkSemantics;

/**
 * A {@link SkyFunction} that creates {@link ActionExecutionValue}s. There are four points where
 * this function can abort due to missing values in the graph:
 *
 * <ol>
 *   <li>For actions that discover inputs, if missing metadata needed to resolve an artifact from a
 *       string input in the action cache.
 *   <li>If missing metadata for artifacts in inputs (including the artifacts above).
 *   <li>For actions that discover inputs, if missing metadata for inputs discovered prior to
 *       execution.
 *   <li>For actions that discover inputs, but do so during execution, if missing metadata for
 *       inputs discovered during execution.
 * </ol>
 */
public class ActionExecutionFunction implements SkyFunction {

  private final ActionRewindStrategy actionRewindStrategy = new ActionRewindStrategy();
  private final SkyframeActionExecutor skyframeActionExecutor;
  private final BlazeDirectories directories;
  private final AtomicReference<TimestampGranularityMonitor> tsgm;
  private ConcurrentMap<Action, ContinuationState> stateMap;

  public ActionExecutionFunction(
      SkyframeActionExecutor skyframeActionExecutor,
      BlazeDirectories directories,
      AtomicReference<TimestampGranularityMonitor> tsgm) {
    this.skyframeActionExecutor = skyframeActionExecutor;
    this.directories = directories;
    this.tsgm = tsgm;
    // TODO(b/136156191): This stays in RAM while the SkyFunction of the action is pending, which
    // can result in a lot of memory pressure if a lot of actions are pending.
    stateMap = Maps.newConcurrentMap();
  }

  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws ActionExecutionFunctionException, InterruptedException {
    ActionLookupData actionLookupData = (ActionLookupData) skyKey.argument();
    Action action = ActionUtils.getActionForLookupData(env, actionLookupData);
    if (action == null) {
      return null;
    }
    skyframeActionExecutor.noteActionEvaluationStarted(actionLookupData, action);
    if (Actions.dependsOnBuildId(action)) {
      PrecomputedValue.BUILD_ID.get(env);
    }

    if (skyframeActionExecutor.isBazelRemoteExecutionEnabled()) {
      // Declaring a dependency on the precomputed value so that all actions are invalidated if
      // the value of the flag changes. We are doing this conditionally only in Bazel if remote
      // execution is available in order to not introduce additional skyframe edges in Blaze.
      PrecomputedValue.REMOTE_OUTPUTS_MODE.get(env);
      PrecomputedValue.REMOTE_DEFAULT_PLATFORM_PROPERTIES.get(env);
    }

    // Look up the parts of the environment that influence the action.
    Map<SkyKey, SkyValue> clientEnvLookup =
        env.getValues(
            Iterables.transform(
                action.getClientEnvironmentVariables(), ClientEnvironmentFunction::key));
    if (env.valuesMissing()) {
      return null;
    }
    Map<String, String> clientEnv = new HashMap<>();
    for (Map.Entry<SkyKey, SkyValue> entry : clientEnvLookup.entrySet()) {
      ClientEnvironmentValue envValue = (ClientEnvironmentValue) entry.getValue();
      if (envValue.getValue() != null) {
        clientEnv.put((String) entry.getKey().argument(), envValue.getValue());
      }
    }

    ActionSketch sketch = null;
    TopDownActionCache topDownActionCache = skyframeActionExecutor.getTopDownActionCache();
    if (topDownActionCache != null) {
      sketch = (ActionSketch) env.getValue(ActionSketchFunction.key(actionLookupData));
      if (sketch == null) {
        return null;
      }
      ActionExecutionValue actionExecutionValue = topDownActionCache.get(sketch);
      if (actionExecutionValue != null) {
        return actionExecutionValue.transformForSharedAction(action.getOutputs());
      }
    }

    // For restarts of this ActionExecutionFunction we use a ContinuationState variable, below, to
    // avoid redoing work.
    //
    // However, if two actions are shared and the first one executes, when the
    // second one goes to execute, we should detect that and short-circuit, even without taking
    // ContinuationState into account.
    //
    // Additionally, if an action restarted (in the Skyframe sense) after it executed because it
    // discovered new inputs during execution, we should detect that and short-circuit.
    ActionExecutionState previousExecution = skyframeActionExecutor.probeActionExecution(action);

    // If this action was previously completed this build, then this evaluation must be happening
    // because of rewinding. Prevent any ProgressLike events from being published a second time for
    // this action; downstream consumers of action events reasonably don't expect them.
    if (!skyframeActionExecutor.shouldEmitProgressEvents(action)) {
      env = new ProgressEventSuppressingEnvironment(env);
    }

    if (action.discoversInputs()) {
      // If this action previously failed due to a lost input found during input discovery, ensure
      // that the input is regenerated before attempting discovery again.
      if (declareDepsOnLostDiscoveredInputsIfAny(env, action)) {
        return null;
      }
    }

    ContinuationState state;
    if (action.discoversInputs()) {
      state = getState(action);
    } else {
      // Because this is a new state, all conditionals below about whether state has already done
      // something will return false, and so we will execute all necessary steps.
      state = new ContinuationState();
    }
    if (!state.hasCollectedInputs()) {
      state.allInputs = collectInputs(action, env);
      state.requestedArtifactNestedSetKeys = null;
      if (state.allInputs == null) {
        // Missing deps.
        return null;
      }
    } else if (state.allInputs.keysRequested != null) {
      // Preserve the invariant that we ask for the same deps each build.
      env.getValues(state.allInputs.keysRequested);
      Preconditions.checkState(!env.valuesMissing(), "%s %s", action, state);
    }
    CheckInputResults checkedInputs = null;
    @Nullable
    ImmutableSet<Artifact> mandatoryInputs =
        action.discoversInputs() ? action.getMandatoryInputs().toSet() : null;

    NestedSet<Artifact> allInputs = state.allInputs.getAllInputs();

    Map<
            SkyKey,
            ValueOrException3<
                IOException, ActionExecutionException, ArtifactNestedSetEvalException>>
        inputDeps = getInputDeps(env, allInputs, state);

    try {
      if (previousExecution == null && !state.hasArtifactData()) {
        // Do we actually need to find our metadata?
        checkedInputs = checkInputs(env, action, inputDeps, allInputs, mandatoryInputs);
      }
    } catch (ActionExecutionException e) {
      // Remove action from state map in case it's there (won't be unless it discovers inputs).
      stateMap.remove(action);
      throw new ActionExecutionFunctionException(e);
    }

    if (env.valuesMissing()) {
      // There was missing artifact metadata in the graph. Wait for it to be present.
      // We must check this and return here before attempting to establish any Skyframe dependencies
      // of the action; see establishSkyframeDependencies why.
      return null;
    }

    Object skyframeDepsResult;
    try {
      skyframeDepsResult = establishSkyframeDependencies(env, action);
    } catch (ActionExecutionException e) {
      // Remove action from state map in case it's there (won't be unless it discovers inputs).
      stateMap.remove(action);
      throw new ActionExecutionFunctionException(
          skyframeActionExecutor.processAndGetExceptionToThrow(
              env.getListener(),
              /*primaryOutputPath=*/ null,
              action,
              actionLookupData,
              e,
              new FileOutErr(),
              ErrorTiming.BEFORE_EXECUTION));
    }
    if (env.valuesMissing()) {
      return null;
    }

    if (checkedInputs != null) {
      Preconditions.checkState(!state.hasArtifactData(), "%s %s", state, action);
      state.inputArtifactData = checkedInputs.actionInputMap;
      state.expandedArtifacts = checkedInputs.expandedArtifacts;
      state.archivedTreeArtifacts = checkedInputs.archivedTreeArtifacts;
      state.filesetsInsideRunfiles = checkedInputs.filesetsInsideRunfiles;
      state.topLevelFilesets = checkedInputs.topLevelFilesets;
      if (skyframeActionExecutor.actionFileSystemType().isEnabled()) {
        state.actionFileSystem =
            skyframeActionExecutor.createActionFileSystem(
                directories.getRelativeOutputPath(),
                checkedInputs.actionInputMap,
                action.getOutputs(),
                env.restartPermitted());
      }
    }

    long actionStartTime = BlazeClock.nanoTime();
    ActionExecutionValue result;
    try {
      result =
          checkCacheAndExecuteIfNeeded(
              action,
              state,
              env,
              clientEnv,
              actionLookupData,
              previousExecution,
              skyframeDepsResult,
              actionStartTime);
    } catch (LostInputsActionExecutionException e) {
      return handleLostInputs(
          e, actionLookupData, action, actionStartTime, env, inputDeps, allInputs, state);
    } catch (ActionExecutionException e) {
      // Remove action from state map in case it's there (won't be unless it discovers inputs).
      stateMap.remove(action);
      // In this case we do not report the error to the action reporter because we have already
      // done it in SkyframeActionExecutor.reportErrorIfNotAbortingMode() method. That method
      // prints the error in the top-level reporter and also dumps the recorded StdErr for the
      // action. Label can be null in the case of, e.g., the SystemActionOwner (for build-info.txt).
      throw new ActionExecutionFunctionException(new AlreadyReportedActionExecutionException(e));
    }

    if (env.valuesMissing()) {
      // Only input-discovering actions are present in the stateMap. Other actions may have
      // valuesMissing() here in rare circumstances related to Fileset inputs being unavailable.
      // See comments in ActionInputMapHelper#getFilesets().
      Preconditions.checkState(!action.discoversInputs() || stateMap.containsKey(action), action);
      return null;
    }

    // Remove action from state map in case it's there (won't be unless it discovers inputs).
    stateMap.remove(action);
    if (sketch != null && result.dataIsShareable()) {
      topDownActionCache.put(sketch, result);
    }
    return result;
  }

  /**
   * Evaluate the supplied input deps. Declare deps on known inputs to action. We do this
   * unconditionally to maintain our invariant of asking for the same deps each build.
   */
  private static Map<
          SkyKey,
          ValueOrException3<IOException, ActionExecutionException, ArtifactNestedSetEvalException>>
      getInputDeps(Environment env, NestedSet<Artifact> allInputs, ContinuationState state)
          throws InterruptedException {
    if (evalInputsAsNestedSet(allInputs)) {
      // We "unwrap" the NestedSet and evaluate the first layer of direct Artifacts here in order
      // to save memory:
      // - This top layer costs 1 extra ArtifactNestedSetKey node.
      // - It's uncommon that 2 actions share the exact same set of inputs
      //   => the top layer offers little in terms of reusability.
      // More details: b/143205147.
      Iterable<SkyKey> directKeys = Artifact.keys(allInputs.getLeaves());
      if (state.requestedArtifactNestedSetKeys == null) {
        state.requestedArtifactNestedSetKeys = CompactHashSet.create();
        for (NestedSet<Artifact> nonLeaf : allInputs.getNonLeaves()) {
          state.requestedArtifactNestedSetKeys.add(
              new ArtifactNestedSetKey(nonLeaf, nonLeaf.toNode()));
        }
      }

      return env.getValuesOrThrow(
          Iterables.concat(directKeys, state.requestedArtifactNestedSetKeys),
          IOException.class,
          ActionExecutionException.class,
          ArtifactNestedSetEvalException.class);
    }

    return env.getValuesOrThrow(
        Artifact.keys(allInputs.toList()),
        IOException.class,
        ActionExecutionException.class,
        ArtifactNestedSetEvalException.class);
  }

  /**
   * Do one traversal of the set to get the size. The traversal costs CPU time so only do it when
   * necessary. The default case (without --experimental_nestedset_as_skykey_threshold) will ignore
   * this path.
   */
  private static boolean evalInputsAsNestedSet(NestedSet<Artifact> inputs) {
    int nestedSetSizeThreshold = ArtifactNestedSetFunction.getSizeThreshold();
    if (nestedSetSizeThreshold == 1) {
      // Don't even flatten in this case.
      return true;
    }
    return nestedSetSizeThreshold > 0
        && (inputs.memoizedFlattenAndGetSize() >= nestedSetSizeThreshold);
  }

  private boolean declareDepsOnLostDiscoveredInputsIfAny(Environment env, Action action)
      throws InterruptedException, ActionExecutionFunctionException {
    ImmutableList<SkyKey> previouslyLostDiscoveredInputs =
        skyframeActionExecutor.getLostDiscoveredInputs(action);
    if (previouslyLostDiscoveredInputs != null) {
      Map<SkyKey, ValueOrException2<MissingInputFileException, ActionExecutionException>>
          lostInputValues =
              env.getValuesOrThrow(
                  previouslyLostDiscoveredInputs,
                  MissingInputFileException.class,
                  ActionExecutionException.class);
      if (env.valuesMissing()) {
        return true;
      }
      for (Map.Entry<SkyKey, ValueOrException2<MissingInputFileException, ActionExecutionException>>
          lostInput : lostInputValues.entrySet()) {
        try {
          lostInput.getValue().get();
        } catch (MissingInputFileException e) {
          // MissingInputFileException comes from problems with source artifact construction.
          // Rewinding never invalidates source artifacts.
          throw new IllegalStateException(
              "MissingInputFileException unexpected from rewound generated discovered input. key="
                  + lostInput.getKey(),
              e);
        } catch (ActionExecutionException e) {
          throw new ActionExecutionFunctionException(e);
        }
      }
    }
    return false;
  }

  /**
   * Clean up state associated with the current action execution attempt and return a {@link
   * Restart} value which rewinds the actions that generate the lost inputs.
   */
  private SkyFunction.Restart handleLostInputs(
      LostInputsActionExecutionException e,
      ActionLookupData actionLookupData,
      Action action,
      long actionStartTime,
      Environment env,
      Map<
              SkyKey,
              ValueOrException3<
                  IOException, ActionExecutionException, ArtifactNestedSetEvalException>>
          inputDeps,
      NestedSet<Artifact> allInputs,
      ContinuationState state)
      throws InterruptedException, ActionExecutionFunctionException {
    // Remove action from state map in case it's there (won't be unless it discovers inputs).
    stateMap.remove(action);

    boolean isPrimaryAction = e.isPrimaryAction(actionLookupData);
    RewindPlan rewindPlan = null;
    try {
      ActionInputDepOwners inputDepOwners =
          createAugmentedInputDepOwners(e, action, env, inputDeps, allInputs);

      // Collect the set of direct deps of this action which may be responsible for the lost inputs,
      // some of which may be discovered.
      ImmutableList<SkyKey> lostDiscoveredInputs = ImmutableList.of();
      Iterable<? extends SkyKey> failedActionDeps;
      if (e.isFromInputDiscovery()) {
        // Lost inputs found during input discovery are necessarily ordinary derived artifacts.
        // Their keys may not be direct deps yet, but the next time this Skyframe node is evaluated
        // they will be. See SkyframeActionExecutor's lostDiscoveredInputsMap.
        lostDiscoveredInputs =
            e.getLostInputs().values().stream()
                .map(i -> (Artifact) i)
                .map(Artifact::key)
                .collect(ImmutableList.toImmutableList());
        failedActionDeps = lostDiscoveredInputs;
      } else if (state.discoveredInputs != null) {
        failedActionDeps =
            Iterables.concat(
                inputDeps.keySet(),
                Iterables.transform(state.discoveredInputs.toList(), Artifact::key));
      } else {
        failedActionDeps = inputDeps.keySet();
      }

      try {
        rewindPlan =
            actionRewindStrategy.getRewindPlan(
                action, actionLookupData, failedActionDeps, e, inputDepOwners, env);
      } catch (ActionExecutionException rewindingFailedException) {
        // This call to processAndGetExceptionToThrow will emit an ActionExecutedEvent and report
        // the error. The previous call to processAndGetExceptionToThrow didn't.
        throw new ActionExecutionFunctionException(
            new AlreadyReportedActionExecutionException(
                skyframeActionExecutor.processAndGetExceptionToThrow(
                    env.getListener(),
                    e.getPrimaryOutputPath(),
                    action,
                    actionLookupData,
                    rewindingFailedException,
                    e.getFileOutErr(),
                    ActionExecutedEvent.ErrorTiming.AFTER_EXECUTION)));
      }

      if (isPrimaryAction) {
        // This action is the "winner" amongst its set of shared actions. Only it must post events
        // and clean up state associated with its shared action set.
        if (e.isActionStartedEventAlreadyEmitted()) {
          env.getListener().post(new ActionRewoundEvent(actionStartTime, action));
        }
        skyframeActionExecutor.resetFailedActionExecution(action, lostDiscoveredInputs);
        for (Action actionToRestart : rewindPlan.getAdditionalActionsToRestart()) {
          skyframeActionExecutor.resetPreviouslyCompletedActionExecution(actionToRestart);
        }
      }
      return rewindPlan.getNodesToRestart();
    } finally {
      if (rewindPlan == null && isPrimaryAction && e.isActionStartedEventAlreadyEmitted()) {
        // Rewinding was unsuccessful. SkyframeActionExecutor's ActionRunner didn't emit an
        // ActionCompletionEvent because it hoped rewinding would fix things. Because it won't, this
        // must emit one to compensate.
        env.getListener()
            .post(new ActionCompletionEvent(actionStartTime, action, actionLookupData));
      }
    }
  }

  /**
   * Returns an augmented version of {@code e.getOwners()}'s {@link ActionInputDepOwners}, adding
   * ownership information from {@code inputDeps}.
   *
   * <p>This compensates for how the ownership information in {@code e.getOwners()} is potentially
   * incomplete. E.g., it may lack knowledge of a runfiles middleman owning a fileset, even if it
   * knows that fileset owns a lost input.
   */
  private ActionInputDepOwners createAugmentedInputDepOwners(
      LostInputsActionExecutionException e,
      Action action,
      Environment env,
      Map<
              SkyKey,
              ValueOrException3<
                  IOException, ActionExecutionException, ArtifactNestedSetEvalException>>
          inputDeps,
      NestedSet<Artifact> allInputs)
      throws InterruptedException {

    Set<ActionInput> lostInputsAndOwnersSoFar = new HashSet<>();
    ActionInputDepOwners owners = e.getOwners();
    for (ActionInput lostInput : e.getLostInputs().values()) {
      lostInputsAndOwnersSoFar.add(lostInput);
      lostInputsAndOwnersSoFar.addAll(owners.getDepOwners(lostInput));
    }

    ActionInputDepOwnerMap inputDepOwners;
    try {
      inputDepOwners =
          getInputDepOwners(
              env,
              action,
              inputDeps,
              allInputs,
              action.discoversInputs() ? action.getMandatoryInputs().toSet() : null,
              lostInputsAndOwnersSoFar);
    } catch (ActionExecutionException unexpected) {
      // getInputDepOwners should not be able to throw, because it does the same work as
      // checkInputs, so if getInputDepOwners throws then checkInputs should have thrown, and if
      // checkInputs threw then we shouldn't have reached this point in action execution.
      throw new IllegalStateException(unexpected);
    }

    // Ownership information from inputDeps may be incomplete. Notably, it does not expand
    // filesets. Fileset and other ownership relationships should have been captured in the
    // exception's ActionInputDepOwners, and this copies that knowledge into the augmented version.
    for (ActionInput lostInput : e.getLostInputs().values()) {
      for (Artifact depOwner : owners.getDepOwners(lostInput)) {
        inputDepOwners.addOwner(lostInput, depOwner);
      }
    }
    return inputDepOwners;
  }

  /**
   * An action's inputs needed for execution. May not just be the result of Action#getInputs(). If
   * the action cache's view of this action contains additional inputs, it will request metadata for
   * them, so we consider those inputs as dependencies of this action as well. Returns null if some
   * dependencies were missing and this ActionExecutionFunction needs to restart.
   */
  @Nullable
  private AllInputs collectInputs(Action action, Environment env) throws InterruptedException {
    NestedSet<Artifact> allKnownInputs = action.getInputs();
    if (action.inputsDiscovered()) {
      return new AllInputs(allKnownInputs);
    }

    Preconditions.checkState(action.discoversInputs(), action);
    PackageRootResolverWithEnvironment resolver = new PackageRootResolverWithEnvironment(env);
    List<Artifact> actionCacheInputs =
        skyframeActionExecutor.getActionCachedInputs(action, resolver);
    if (actionCacheInputs == null) {
      Preconditions.checkState(env.valuesMissing(), action);
      return null;
    }
    return new AllInputs(allKnownInputs, actionCacheInputs, resolver.keysRequested);
  }

  private static class AllInputs {
    final NestedSet<Artifact> defaultInputs;
    @Nullable final List<Artifact> actionCacheInputs;
    @Nullable final List<SkyKey> keysRequested;

    AllInputs(NestedSet<Artifact> defaultInputs) {
      this.defaultInputs = checkNotNull(defaultInputs);
      this.actionCacheInputs = null;
      this.keysRequested = null;
    }

    AllInputs(
        NestedSet<Artifact> defaultInputs,
        List<Artifact> actionCacheInputs,
        List<SkyKey> keysRequested) {
      this.defaultInputs = checkNotNull(defaultInputs);
      this.actionCacheInputs = checkNotNull(actionCacheInputs);
      this.keysRequested = keysRequested;
    }

    NestedSet<Artifact> getAllInputs() {
      if (actionCacheInputs == null) {
        return defaultInputs;
      }

      NestedSetBuilder<Artifact> builder = new NestedSetBuilder<>(Order.STABLE_ORDER);
      // actionCacheInputs is never a NestedSet.
      builder.addAll(actionCacheInputs);
      builder.addTransitive(defaultInputs);
      return builder.build();
    }
  }

  /**
   * Skyframe implementation of {@link PackageRootResolver}. Should be used only from SkyFunctions,
   * because it uses SkyFunction.Environment for evaluation of ContainingPackageLookupValue.
   */
  private static class PackageRootResolverWithEnvironment implements PackageRootResolver {
    final List<SkyKey> keysRequested = new ArrayList<>();
    private final Environment env;

    private PackageRootResolverWithEnvironment(Environment env) {
      this.env = env;
    }

    @Override
    public Map<PathFragment, Root> findPackageRootsForFiles(Iterable<PathFragment> execPaths)
        throws InterruptedException {
      Preconditions.checkState(
          keysRequested.isEmpty(),
          "resolver should only be called once: %s %s",
          keysRequested,
          execPaths);
      StarlarkSemantics starlarkSemantics = PrecomputedValue.STARLARK_SEMANTICS.get(env);
      if (starlarkSemantics == null) {
        return null;
      }

      boolean siblingRepositoryLayout =
          starlarkSemantics.getBool(BuildLanguageOptions.EXPERIMENTAL_SIBLING_REPOSITORY_LAYOUT);

      // Create SkyKeys list based on execPaths.
      Map<PathFragment, SkyKey> depKeys = new HashMap<>();
      for (PathFragment path : execPaths) {
        PathFragment parent =
            checkNotNull(path.getParentDirectory(), "Must pass in files, not root directory");
        Preconditions.checkArgument(!parent.isAbsolute(), path);
        SkyKey depKey =
            ContainingPackageLookupValue.key(
                PackageIdentifier.discoverFromExecPath(path, true, siblingRepositoryLayout));
        depKeys.put(path, depKey);
        keysRequested.add(depKey);
      }

      Map<SkyKey, SkyValue> values = env.getValues(depKeys.values());
      if (env.valuesMissing()) {
        return null;
      }

      Map<PathFragment, Root> result = new HashMap<>();
      for (PathFragment path : execPaths) {
        if (!depKeys.containsKey(path)) {
          continue;
        }
        ContainingPackageLookupValue value =
            (ContainingPackageLookupValue) values.get(depKeys.get(path));
        if (value.hasContainingPackage()) {
          // We have found corresponding root for current execPath.
          result.put(
              path,
              SkyframeExecutor.maybeTransformRootForRepository(
                  value.getContainingPackageRoot(),
                  value.getContainingPackageName().getRepository()));
        } else {
          // We haven't found corresponding root for current execPath.
          result.put(path, null);
        }
      }
      return result;
    }
  }

  private ActionExecutionValue checkCacheAndExecuteIfNeeded(
      Action action,
      ContinuationState state,
      Environment env,
      Map<String, String> clientEnv,
      ActionLookupData actionLookupData,
      @Nullable ActionExecutionState previousAction,
      Object skyframeDepsResult,
      long actionStartTime)
      throws ActionExecutionException, InterruptedException {
    if (previousAction != null) {
      // There are two cases where we can already have an executing action for a specific output:
      // 1. Another instance of a shared action won the race and got executed first.
      // 2. The action was already started earlier, and this SkyFunction got restarted since
      //    there's progress to be made.
      // In either case, we must use this continuation to continue. Note that in the first case,
      // we don't have any input metadata available, so we couldn't re-execute the action even if we
      // wanted to.
      return previousAction.getResultOrDependOnFuture(
          env,
          actionLookupData,
          action,
          skyframeActionExecutor.getSharedActionCallback(
              env.getListener(), state.discoveredInputs != null, action, actionLookupData));
    }

    ImmutableMap<Artifact, ImmutableList<FilesetOutputSymlink>> expandedFilesets =
        state.getExpandedFilesets();

    ArtifactExpander artifactExpander =
        new Artifact.ArtifactExpanderImpl(
            Collections.unmodifiableMap(state.expandedArtifacts),
            Collections.unmodifiableMap(state.archivedTreeArtifacts),
            expandedFilesets);

    ArtifactPathResolver pathResolver =
        ArtifactPathResolver.createPathResolver(
            state.actionFileSystem, skyframeActionExecutor.getExecRoot());
    ActionMetadataHandler metadataHandler =
        ActionMetadataHandler.create(
            state.inputArtifactData,
            action.discoversInputs(),
            skyframeActionExecutor.useArchivedTreeArtifacts(),
            action.getOutputs(),
            tsgm.get(),
            pathResolver,
            skyframeActionExecutor.getExecRoot().asFragment(),
            PathFragment.create(directories.getRelativeOutputPath()),
            expandedFilesets);

    // We only need to check the action cache if we haven't done it on a previous run.
    if (!state.hasCheckedActionCache()) {
      state.token =
          skyframeActionExecutor.checkActionCache(
              env.getListener(),
              action,
              metadataHandler,
              artifactExpander,
              actionStartTime,
              state.allInputs.actionCacheInputs,
              clientEnv,
              pathResolver);
    }

    if (state.token == null) {
      // We got a hit from the action cache -- no need to execute.
      Preconditions.checkState(
          !(action instanceof SkyframeAwareAction),
          "Error, we're not re-executing a "
              + "SkyframeAwareAction which should be re-executed unconditionally. Action: %s",
          action);
      return ActionExecutionValue.createFromOutputStore(
          metadataHandler.getOutputStore(),
          /*outputSymlinks=*/ null,
          (action instanceof IncludeScannable)
              ? ((IncludeScannable) action).getDiscoveredModules()
              : null,
          Actions.dependsOnBuildId(action));
    }

    metadataHandler.prepareForActionExecution();

    if (action.discoversInputs()) {
      Duration discoveredInputsDuration = Duration.ZERO;
      if (state.discoveredInputs == null) {
        try (SilentCloseable c = Profiler.instance().profile(ProfilerTask.INFO, "discoverInputs")) {
          state.discoveredInputs =
              skyframeActionExecutor.discoverInputs(
                  action, actionLookupData, metadataHandler, env, state.actionFileSystem);
        } catch (MissingDepException e) {
          Preconditions.checkState(env.valuesMissing(), action);
          return null;
        }
        discoveredInputsDuration = Duration.ofNanos(BlazeClock.nanoTime() - actionStartTime);
        if (env.valuesMissing()) {
          Preconditions.checkState(
              state.discoveredInputs == null,
              "Inputs were discovered but more deps were requested by %s",
              action);
          return null;
        }
        Preconditions.checkNotNull(
            state.discoveredInputs,
            "Input discovery returned null but no more deps were requested by %s",
            action);
      }
      switch (addDiscoveredInputs(
          state.inputArtifactData,
          state.expandedArtifacts,
          state.archivedTreeArtifacts,
          state.filterKnownDiscoveredInputs(),
          env,
          action)) {
        case VALUES_MISSING:
          return null;
        case NO_DISCOVERED_DATA:
          break;
        case DISCOVERED_DATA:
          metadataHandler = metadataHandler.transformAfterInputDiscovery(new OutputStore());
          metadataHandler.prepareForActionExecution();
      }
      // When discover inputs completes, post an event with the duration values.
      env.getListener()
          .post(
              new DiscoveredInputsEvent(
                  SpawnMetrics.Builder.forOtherExec()
                      .setParseTime(discoveredInputsDuration)
                      .setTotalTime(discoveredInputsDuration)
                      .build(),
                  action,
                  actionStartTime));
    }

    return skyframeActionExecutor.executeAction(
        env,
        action,
        metadataHandler,
        actionStartTime,
        actionLookupData,
        artifactExpander,
        expandedFilesets,
        state.topLevelFilesets,
        state.actionFileSystem,
        skyframeDepsResult,
        new ActionPostprocessingImpl(state),
        state.discoveredInputs != null);
  }

  /** Implementation of {@link ActionPostprocessing}. */
  private final class ActionPostprocessingImpl implements ActionPostprocessing {
    private final ContinuationState state;

    ActionPostprocessingImpl(ContinuationState state) {
      this.state = state;
    }

    @Override
    public void run(
        Environment env,
        Action action,
        ActionMetadataHandler metadataHandler,
        Map<String, String> clientEnv)
        throws InterruptedException, ActionExecutionException {
      // TODO(b/160603797): For the sake of action key computation, we should not need
      //  state.filesetsInsideRunfiles. In fact, for the metadataHandler, we are guaranteed to not
      //  expand any filesets since we request metadata for input/output Artifacts only.
      ImmutableMap<Artifact, ImmutableList<FilesetOutputSymlink>> expandedFilesets =
          state.getExpandedFilesets();
      if (action.discoversInputs()) {
        state.discoveredInputs = action.getInputs();
        switch (addDiscoveredInputs(
            state.inputArtifactData,
            state.expandedArtifacts,
            state.archivedTreeArtifacts,
            state.filterKnownDiscoveredInputs(),
            env,
            action)) {
          case VALUES_MISSING:
            return;
          case NO_DISCOVERED_DATA:
            break;
          case DISCOVERED_DATA:
            // We are in the interesting case of an action that discovered its inputs during
            // execution, and found some new ones, but the new ones were already present in the
            // graph. We must therefore cache the metadata for those new ones.
            metadataHandler =
                metadataHandler.transformAfterInputDiscovery(metadataHandler.getOutputStore());
        }
      }
      Preconditions.checkState(!env.valuesMissing(), action);
      skyframeActionExecutor.updateActionCache(
          action,
          metadataHandler,
          new Artifact.ArtifactExpanderImpl(
              // Skipping the filesets in runfiles since those cannot participate in command line
              // creation.
              Collections.unmodifiableMap(state.expandedArtifacts),
              Collections.unmodifiableMap(state.archivedTreeArtifacts),
              expandedFilesets),
          state.token,
          clientEnv);
    }
  }

  private enum DiscoveredState {
    VALUES_MISSING,
    NO_DISCOVERED_DATA,
    DISCOVERED_DATA
  }

  private DiscoveredState addDiscoveredInputs(
      ActionInputMap inputData,
      Map<Artifact, ImmutableCollection<Artifact>> expandedArtifacts,
      Map<SpecialArtifact, ArchivedTreeArtifact> archivedTreeArtifacts,
      Iterable<Artifact> discoveredInputs,
      Environment env,
      Action actionForError)
      throws InterruptedException, ActionExecutionException {
    // In most cases we don't need to handle exceptions here, because derived inputs were already
    // (transitively) requested, so we shouldn't have reached here, and non-mandatory inputs don't
    // throw exceptions. However, in nokeep-going mode, a missing discovered input will result in an
    // IOException that won't turn into a MissingInputFileArtifactValue, and so we have to transform
    // it here.
    Map<SkyKey, ValueOrException<IOException>> nonMandatoryDiscovered =
        env.getValuesOrThrow(
            Iterables.transform(discoveredInputs, Artifact::key), IOException.class);
    if (nonMandatoryDiscovered.isEmpty()) {
      return DiscoveredState.NO_DISCOVERED_DATA;
    }
    for (Artifact input : discoveredInputs) {
      SkyValue retrievedMetadata;
      try {
        retrievedMetadata = nonMandatoryDiscovered.get(Artifact.key(input)).get();
      } catch (IOException e) {
        if (!input.isSourceArtifact()) {
          BugReport.sendBugReport(
              new IllegalStateException("Non-source artifact had IO Exception" + input, e));
        }

        skyframeActionExecutor.printError(
            ArtifactFunction.makeIOExceptionInputFileMessage(input, e), actionForError, null);
        // We don't create a specific cause for the artifact as we do in #handleMissingFile because
        // it likely has no label, so we'd have to use the Action's label anyway. Just use the
        // default ActionFailed event constructed by ActionExecutionException.
        String message = "discovered input file does not exist";
        DetailedExitCode code =
            createDetailedExitCode(message, Code.DISCOVERED_INPUT_DOES_NOT_EXIST);
        throw new ActionExecutionException(message, actionForError, false, code);
      }
      if (retrievedMetadata == null) {
        Preconditions.checkState(
            env.valuesMissing(),
            "%s had no metadata but all values were present for %s",
            input,
            actionForError);
        continue;
      }
      if (retrievedMetadata instanceof TreeArtifactValue) {
        TreeArtifactValue treeValue = (TreeArtifactValue) retrievedMetadata;
        expandedArtifacts.put(input, ImmutableSet.copyOf(treeValue.getChildren()));
        for (Map.Entry<Artifact.TreeFileArtifact, FileArtifactValue> child :
            treeValue.getChildValues().entrySet()) {
          inputData.putWithNoDepOwner(child.getKey(), child.getValue());
        }
        inputData.putWithNoDepOwner(input, treeValue.getMetadata());
        treeValue
            .getArchivedRepresentation()
            .ifPresent(
                archivedRepresentation -> {
                  inputData.putWithNoDepOwner(
                      archivedRepresentation.archivedTreeFileArtifact(),
                      archivedRepresentation.archivedFileValue());
                  archivedTreeArtifacts.put(
                      (SpecialArtifact) input, archivedRepresentation.archivedTreeFileArtifact());
                });
      } else if (retrievedMetadata instanceof ActionExecutionValue) {
        inputData.putWithNoDepOwner(
            input, ((ActionExecutionValue) retrievedMetadata).getExistingFileArtifactValue(input));
      } else if (retrievedMetadata instanceof MissingFileArtifactValue) {
        inputData.putWithNoDepOwner(input, FileArtifactValue.MISSING_FILE_MARKER);
      } else if (retrievedMetadata instanceof FileArtifactValue) {
        inputData.putWithNoDepOwner(input, (FileArtifactValue) retrievedMetadata);
      } else {
        throw new IllegalStateException(
            "unknown metadata for " + input.getExecPathString() + ": " + retrievedMetadata);
      }
    }
    return env.valuesMissing() ? DiscoveredState.VALUES_MISSING : DiscoveredState.DISCOVERED_DATA;
  }

  private static <E extends Exception> Object establishSkyframeDependencies(
      Environment env, Action action) throws ActionExecutionException, InterruptedException {
    // Before we may safely establish Skyframe dependencies, we must build all action inputs by
    // requesting their ArtifactValues.
    // This is very important to do, because the establishSkyframeDependencies method may request
    // FileValues for input files of this action (directly requesting them, or requesting some other
    // SkyValue whose builder requests FileValues), which may not yet exist if their generating
    // actions have not yet run.
    // See SkyframeAwareActionTest.testRaceConditionBetweenInputAcquisitionAndSkyframeDeps
    Preconditions.checkState(!env.valuesMissing(), action);

    if (action instanceof SkyframeAwareAction) {
      // Skyframe-aware actions should be executed unconditionally, i.e. bypass action cache
      // checking. See documentation of SkyframeAwareAction.
      Preconditions.checkState(action.executeUnconditionally(), action);

      @SuppressWarnings("unchecked")
      SkyframeAwareAction<E> skyframeAwareAction = (SkyframeAwareAction<E>) action;
      ImmutableList<? extends SkyKey> keys = skyframeAwareAction.getDirectSkyframeDependencies();
      Map<SkyKey, ValueOrException<E>> values =
          env.getValuesOrThrow(keys, skyframeAwareAction.getExceptionType());

      try {
        return skyframeAwareAction.processSkyframeValues(keys, values, env.valuesMissing());
      } catch (SkyframeAwareAction.ExceptionBase e) {
        throw new ActionExecutionException(
            e, action, false, DetailedExitCode.of(e.getFailureDetail()));
      }
    }
    return null;
  }

  private static class CheckInputResults {
    /** Metadata about Artifacts consumed by this Action. */
    private final ActionInputMap actionInputMap;
    /** Artifact expansion mapping for Runfiles tree and tree artifacts. */
    private final Map<Artifact, ImmutableCollection<Artifact>> expandedArtifacts;
    /** Archived representations for tree artifacts. */
    private final Map<SpecialArtifact, ArchivedTreeArtifact> archivedTreeArtifacts;
    /** Artifact expansion mapping for Filesets embedded in Runfiles. */
    private final ImmutableMap<Artifact, ImmutableList<FilesetOutputSymlink>>
        filesetsInsideRunfiles;
    /** Artifact expansion mapping for top level filesets. */
    private final ImmutableMap<Artifact, ImmutableList<FilesetOutputSymlink>> topLevelFilesets;

    CheckInputResults(
        ActionInputMap actionInputMap,
        Map<Artifact, ImmutableCollection<Artifact>> expandedArtifacts,
        Map<SpecialArtifact, ArchivedTreeArtifact> archivedTreeArtifacts,
        Map<Artifact, ImmutableList<FilesetOutputSymlink>> filesetsInsideRunfiles,
        Map<Artifact, ImmutableList<FilesetOutputSymlink>> topLevelFilesets) {
      this.actionInputMap = actionInputMap;
      this.expandedArtifacts = expandedArtifacts;
      this.archivedTreeArtifacts = archivedTreeArtifacts;
      this.filesetsInsideRunfiles = ImmutableMap.copyOf(filesetsInsideRunfiles);
      this.topLevelFilesets = ImmutableMap.copyOf(topLevelFilesets);
    }
  }

  private interface AccumulateInputResultsFactory<S extends ActionInputMapSink, R> {
    R create(
        S actionInputMapSink,
        Map<Artifact, ImmutableCollection<Artifact>> expandedArtifacts,
        Map<SpecialArtifact, ArchivedTreeArtifact> archivedTreeArtifacts,
        Map<Artifact, ImmutableList<FilesetOutputSymlink>> filesetsInsideRunfiles,
        Map<Artifact, ImmutableList<FilesetOutputSymlink>> topLevelFilesets);
  }

  /**
   * Declare dependency on all known inputs of action. Throws exception if any are known to be
   * missing. Some inputs may not yet be in the graph, in which case the builder should abort.
   */
  private CheckInputResults checkInputs(
      Environment env,
      Action action,
      Map<
              SkyKey,
              ValueOrException3<
                  IOException, ActionExecutionException, ArtifactNestedSetEvalException>>
          inputDeps,
      NestedSet<Artifact> allInputs,
      ImmutableSet<Artifact> mandatoryInputs)
      throws ActionExecutionException, InterruptedException {
    return accumulateInputs(
        env,
        action,
        inputDeps,
        allInputs,
        mandatoryInputs,
        ActionInputMap::new,
        CheckInputResults::new);
  }

  /**
   * Reconstructs the relationships between lost inputs and the direct deps responsible for them.
   */
  private ActionInputDepOwnerMap getInputDepOwners(
      Environment env,
      Action action,
      Map<
              SkyKey,
              ValueOrException3<
                  IOException, ActionExecutionException, ArtifactNestedSetEvalException>>
          inputDeps,
      NestedSet<Artifact> allInputs,
      ImmutableSet<Artifact> mandatoryInputs,
      Collection<ActionInput> lostInputs)
      throws ActionExecutionException, InterruptedException {
    return accumulateInputs(
        env,
        action,
        inputDeps,
        allInputs,
        mandatoryInputs,
        ignoredInputDepsSize -> new ActionInputDepOwnerMap(lostInputs),
        (actionInputMapSink,
            expandedArtifacts,
            archivedArtifacts,
            filesetsInsideRunfiles,
            topLevelFilesets) -> actionInputMapSink);
  }

  private <S extends ActionInputMapSink, R> R accumulateInputs(
      Environment env,
      Action action,
      Map<
              SkyKey,
              ValueOrException3<
                  IOException, ActionExecutionException, ArtifactNestedSetEvalException>>
          inputDeps,
      NestedSet<Artifact> allInputs,
      ImmutableSet<Artifact> mandatoryInputs,
      IntFunction<S> actionInputMapSinkFactory,
      AccumulateInputResultsFactory<S, R> accumulateInputResultsFactory)
      throws ActionExecutionException, InterruptedException {

    if (evalInputsAsNestedSet(allInputs)) {
      return accumulateInputsWithNestedSet(
          env,
          action,
          inputDeps,
          allInputs,
          mandatoryInputs,
          actionInputMapSinkFactory,
          accumulateInputResultsFactory);
    }
    // Only populate input data if we have the input values, otherwise they'll just go unused.
    // We still want to loop through the inputs to collect missing deps errors. During the
    // evaluator "error bubbling", we may get one last chance at reporting errors even though
    // some deps are still missing.
    boolean populateInputData = !env.valuesMissing();
    // Errors unexpected: save garbage on initialization.
    List<LabelCause> missingArtifactCauses = Lists.newArrayListWithCapacity(0);
    List<NestedSet<Cause>> transitiveCauses = Lists.newArrayListWithCapacity(0);
    ImmutableList<Artifact> allInputsList = allInputs.toList();
    S inputArtifactData =
        actionInputMapSinkFactory.apply(populateInputData ? allInputsList.size() : 0);
    Map<Artifact, ImmutableCollection<Artifact>> expandedArtifacts =
        Maps.newHashMapWithExpectedSize(populateInputData ? 128 : 0);
    Map<SpecialArtifact, ArchivedTreeArtifact> archivedTreeArtifacts =
        Maps.newHashMapWithExpectedSize(128);
    Map<Artifact, ImmutableList<FilesetOutputSymlink>> filesetsInsideRunfiles =
        Maps.newHashMapWithExpectedSize(0);
    Map<Artifact, ImmutableList<FilesetOutputSymlink>> topLevelFilesets =
        Maps.newHashMapWithExpectedSize(0);

    ActionExecutionException firstActionExecutionException = null;

    for (Artifact input : allInputsList) {
      ValueOrException3<IOException, ActionExecutionException, ArtifactNestedSetEvalException>
          valueOrException = inputDeps.get(Artifact.key(input));
      if (valueOrException == null) {
        continue;
      }

      // Some inputs do not need to exist: we depend on the inputs of the action as registered in
      // the action cache so that we can verify the validity of the cache entry, but if the
      // reference to the file went away together with the file itself (e.g. when deleting a file
      // and removing the #include statement referencing it), we re-execute the action anyway so it
      // does not matter if the file is missing.
      //
      // This mechanism fails, though, if we remove a #include statement referencing a header and
      // then introduce a symlink cycle in its place: then there will be an IOException which will
      // be propagated even though we shouldn't have read the file in the first place. This is not
      // really avoidable (at least not without redesigning the action cache), because once the
      // ArtifactFunction throws an exception, Skyframe evaluation must stop, so all we can do is
      // signal the error in a more meaningful way.
      //
      // In particular, making it possible to check only the up-to-dateness of mandatory inputs in
      // the action cache is not enough: it can be that the reference to the symlink cycle arose
      // from a discovered input, so even though no mandatory inputs change, it can still be that
      // the need to read the newly introduced symlink cycle went away.
      boolean mandatory =
          !input.isSourceArtifact() || mandatoryInputs == null || mandatoryInputs.contains(input);
      SkyValue value = FileArtifactValue.MISSING_FILE_MARKER;
      try {
        value = valueOrException.get();
      } catch (IOException e) {
        if (!input.isSourceArtifact()) {
          BugReport.sendBugReport(
              new IllegalStateException(
                  "Unexpected IOException for generated artifact: " + input + ", " + action, e));
        }
        if (mandatory) {
          missingArtifactCauses.add(
              createLabelCause(
                  input,
                  ArtifactFunction.makeIOExceptionSourceInputFileValue(input, e),
                  action.getOwner().getLabel()));
          continue;
        }
      } catch (ActionExecutionException e) {
        if (mandatory) {
          // Prefer a catastrophic exception as the one we propagate.
          if (firstActionExecutionException == null
              || (!firstActionExecutionException.isCatastrophe() && e.isCatastrophe())) {
            firstActionExecutionException = e;
          }
          transitiveCauses.add(e.getRootCauses());
          continue;
        }
      } catch (ArtifactNestedSetEvalException e) {
        throw new IllegalStateException(
            "Unexpected ArtifactNestedSetEvalException for non-NSOS build: "
                + input
                + ", "
                + action,
            e);
      }

      if (value instanceof MissingFileArtifactValue) {
        if (mandatory) {
          missingArtifactCauses.add(
              createLabelCause(
                  input, (MissingFileArtifactValue) value, action.getOwner().getLabel()));
          continue;
        } else {
          value = FileArtifactValue.MISSING_FILE_MARKER;
        }
      }

      if (populateInputData) {
        ActionInputMapHelper.addToMap(
            inputArtifactData,
            expandedArtifacts,
            archivedTreeArtifacts,
            filesetsInsideRunfiles,
            topLevelFilesets,
            input,
            value,
            env);
      }
    }

    if (!missingArtifactCauses.isEmpty()) {
      for (LabelCause missingInput : missingArtifactCauses) {
        skyframeActionExecutor.printError(missingInput.getMessage(), action, null);
      }
    }
    // We need to rethrow first exception because it can contain useful error message
    if (firstActionExecutionException != null) {
      if (missingArtifactCauses.isEmpty() && (checkNotNull(transitiveCauses, action).size() == 1)) {
        // In the case a single action failed, just propagate the exception upward. This avoids
        // having to copy the root causes to the upwards transitive closure.
        throw firstActionExecutionException;
      }
      NestedSetBuilder<Cause> allCauses =
          NestedSetBuilder.<Cause>stableOrder().addAll(missingArtifactCauses);
      transitiveCauses.forEach(allCauses::addTransitive);
      throw new ActionExecutionException(
          firstActionExecutionException.getMessage(),
          firstActionExecutionException.getCause(),
          action,
          allCauses.build(),
          firstActionExecutionException.isCatastrophe(),
          firstActionExecutionException.getDetailedExitCode());
    }

    if (!missingArtifactCauses.isEmpty()) {
      throw createMissingInputsException(action, missingArtifactCauses);
    }
    return accumulateInputResultsFactory.create(
        inputArtifactData,
        expandedArtifacts,
        archivedTreeArtifacts,
        filesetsInsideRunfiles,
        topLevelFilesets);
  }

  /**
   * A refactoring of the existing #accumulateInputs to separate error-handling and
   * input-accumulating steps. Check if there's any value missing in the env after error handling is
   * done before proceeding.
   */
  private <S extends ActionInputMapSink, R> R accumulateInputsWithNestedSet(
      Environment env,
      Action action,
      Map<
              SkyKey,
              ValueOrException3<
                  IOException, ActionExecutionException, ArtifactNestedSetEvalException>>
          inputDeps,
      NestedSet<Artifact> allInputs,
      ImmutableSet<Artifact> mandatoryInputs,
      IntFunction<S> actionInputMapSinkFactory,
      AccumulateInputResultsFactory<S, R> accumulateInputResultsFactory)
      throws ActionExecutionException, InterruptedException {
    ImmutableList<Artifact> allInputsList = allInputs.toList();

    // Some keys have more than 1 corresponding Artifact (e.g. actions with 2 outputs).
    // For Artifacts whose Artifact::key isn't itself.
    Multimap<SkyKey, Artifact> skyKeyToArtifactSet =
        MultimapBuilder.hashKeys().hashSetValues().build();
    allInputsList.forEach(
        input -> {
          SkyKey key = Artifact.key(input);
          if (key != input) {
            skyKeyToArtifactSet.put(key, input);
          }
        });

    ActionExecutionFunctionExceptionHandler actionExecutionFunctionExceptionHandler =
        new ActionExecutionFunctionExceptionHandler(
            skyKeyToArtifactSet, inputDeps, action, mandatoryInputs, skyframeActionExecutor);

    actionExecutionFunctionExceptionHandler.accumulateAndThrowExceptions();

    // All exceptions from dependencies handled, it's now safe to check for missing values.
    if (env.valuesMissing()) {
      return null;
    }

    // When there are no missing values, we can start populating input data.
    Map<Artifact, ImmutableList<FilesetOutputSymlink>> filesetsInsideRunfiles =
        Maps.newHashMapWithExpectedSize(0);
    Map<Artifact, ImmutableList<FilesetOutputSymlink>> topLevelFilesets =
        Maps.newHashMapWithExpectedSize(0);
    S inputArtifactData = actionInputMapSinkFactory.apply(allInputsList.size());
    Map<Artifact, ImmutableCollection<Artifact>> expandedArtifacts =
        Maps.newHashMapWithExpectedSize(128);
    Map<SpecialArtifact, ArchivedTreeArtifact> archivedTreeArtifacts =
        Maps.newHashMapWithExpectedSize(128);

    for (Artifact input : allInputsList) {
      SkyValue value = ArtifactNestedSetFunction.getInstance().getValueForKey(Artifact.key(input));
      if (value instanceof MissingFileArtifactValue) {
        if (actionExecutionFunctionExceptionHandler.isMandatory(input)) {
          actionExecutionFunctionExceptionHandler.accumulateMissingFileArtifactValue(
              input, (MissingFileArtifactValue) value);
          continue;
        } else {
          value = FileArtifactValue.MISSING_FILE_MARKER;
        }
      }
      ActionInputMapHelper.addToMap(
          inputArtifactData,
          expandedArtifacts,
          archivedTreeArtifacts,
          filesetsInsideRunfiles,
          topLevelFilesets,
          input,
          value,
          env);
    }
    // After accumulating the inputs, we might find some mandatory artifact with
    // MissingFileArtifactValue.
    actionExecutionFunctionExceptionHandler.maybeThrowException();

    return accumulateInputResultsFactory.create(
        inputArtifactData,
        expandedArtifacts,
        archivedTreeArtifacts,
        filesetsInsideRunfiles,
        topLevelFilesets);
  }

  static LabelCause createLabelCause(
      Artifact input, MissingFileArtifactValue missingValue, Label labelInCaseOfBug) {
    Label inputLabel = input.getOwner();
    if (inputLabel == null) {
      BugReport.sendBugReport(
          new IllegalStateException(
              String.format(
                  "Artifact %s with missing value %s should have owner (%s)",
                  input, missingValue.getMessage(), labelInCaseOfBug)));
      inputLabel = labelInCaseOfBug;
    }
    return new LabelCause(inputLabel, missingValue.getDetailedExitCode());
  }

  @Override
  public String extractTag(SkyKey skyKey) {
    // The return value from this method is only applied to non-error, non-debug events that are
    // posted through the EventHandler associated with the SkyFunction.Environment. For those
    // events, this setting overrides whatever tag is set.
    //
    // If action out/err replay is enabled, then we intentionally post through the Environment to
    // ensure that the output is replayed on subsequent builds. In that case, we need this to be the
    // action owner's label.
    //
    // Otherwise, Events from action execution are posted to the global Reporter rather than through
    // the Environment, so this setting is ignored. Note that the SkyframeActionExecutor manually
    // checks the action owner's label against the Reporter's output filter in that case, which has
    // the same effect as setting it as a tag on the corresponding event.
    return Label.print(((ActionLookupData) skyKey).getActionLookupKey().getLabel());
  }

  /**
   * Should be called once execution is over, and the intra-build cache of in-progress computations
   * should be discarded. If the cache is non-empty (due to an interrupted/failed build), failure to
   * call complete() can both cause a memory leak and incorrect results on the subsequent build.
   */
  public void complete(ExtendedEventHandler eventHandler) {
    // Discard all remaining state (there should be none after a successful execution).
    stateMap = Maps.newConcurrentMap();
    actionRewindStrategy.reset(eventHandler);
  }

  private ContinuationState getState(Action action) {
    ContinuationState state = stateMap.get(action);
    if (state == null) {
      state = new ContinuationState();
      Preconditions.checkState(stateMap.put(action, state) == null, action);
    }
    return state;
  }

  /**
   * State to save work across restarts of ActionExecutionFunction due to missing values in the
   * graph for actions that discover inputs. There are three places where we save work, all for
   * actions that discover inputs:
   *
   * <ol>
   *   <li>If not all known input metadata (coming from Action#getInputs) is available yet, then the
   *       calculated set of inputs (including the inputs resolved from the action cache) is saved.
   *   <li>If not all discovered inputs' metadata is available yet, then the known input metadata
   *       together with the set of discovered inputs is saved, as well as the Token used to
   *       identify this action to the action cache.
   *   <li>If, after execution, new inputs are discovered whose metadata is not yet available, then
   *       the same data as in the previous case is saved, along with the actual result of
   *       execution.
   * </ol>
   */
  private static class ContinuationState {
    AllInputs allInputs;
    /** Mutable map containing metadata for known artifacts. */
    ActionInputMap inputArtifactData = null;

    Map<Artifact, ImmutableCollection<Artifact>> expandedArtifacts = null;
    Map<SpecialArtifact, ArchivedTreeArtifact> archivedTreeArtifacts = null;
    ImmutableMap<Artifact, ImmutableList<FilesetOutputSymlink>> filesetsInsideRunfiles = null;
    ImmutableMap<Artifact, ImmutableList<FilesetOutputSymlink>> topLevelFilesets = null;
    Token token = null;
    NestedSet<Artifact> discoveredInputs = null;
    FileSystem actionFileSystem = null;

    /**
     * Stores the ArtifactNestedSetKeys created from the inputs of this actions. Objective: avoid
     * creating a new ArtifactNestedSetKey for the same NestedSet each time we run
     * ActionExecutionFunction for the same action. This is wiped everytime allInputs is updated.
     */
    CompactHashSet<SkyKey> requestedArtifactNestedSetKeys = null;

    boolean hasCollectedInputs() {
      return allInputs != null;
    }

    boolean hasArtifactData() {
      boolean result = inputArtifactData != null;
      Preconditions.checkState(result == (expandedArtifacts != null), this);
      Preconditions.checkState(result == (archivedTreeArtifacts != null), this);
      return result;
    }

    boolean hasCheckedActionCache() {
      // If token is null because there was an action cache hit, this method is never called again
      // because we return immediately.
      return token != null;
    }

    Iterable<Artifact> filterKnownDiscoveredInputs() {
      return Iterables.filter(
          discoveredInputs.toList(), input -> inputArtifactData.getMetadata(input) == null);
    }

    ImmutableMap<Artifact, ImmutableList<FilesetOutputSymlink>> getExpandedFilesets() {
      if (topLevelFilesets == null || topLevelFilesets.isEmpty()) {
        return filesetsInsideRunfiles;
      }

      Map<Artifact, ImmutableList<FilesetOutputSymlink>> filesetsMap =
          Maps.newHashMapWithExpectedSize(filesetsInsideRunfiles.size() + topLevelFilesets.size());
      filesetsMap.putAll(filesetsInsideRunfiles);
      filesetsMap.putAll(topLevelFilesets);
      return ImmutableMap.copyOf(filesetsMap);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("token", token)
          .add("allInputs", allInputs)
          .add("inputArtifactData", inputArtifactData)
          .add("discoveredInputs", discoveredInputs)
          .toString();
    }
  }

  /**
   * Used to declare all the exception types that can be wrapped in the exception thrown by {@link
   * ActionExecutionFunction#compute}.
   */
  static final class ActionExecutionFunctionException extends SkyFunctionException {

    private final ActionExecutionException actionException;

    ActionExecutionFunctionException(ActionExecutionException e) {
      // We conservatively assume that the error is transient. We don't have enough information to
      // distinguish non-transient errors (e.g. compilation error from a deterministic compiler)
      // from transient ones (e.g. IO error).
      // TODO(bazel-team): Have ActionExecutionExceptions declare their transience.
      super(e, Transience.TRANSIENT);
      this.actionException = e;
    }

    @Override
    public boolean isCatastrophic() {
      return actionException.isCatastrophe();
    }
  }

  /** Helper subclass for the error-handling logic for ActionExecutionFunction#accumulateInputs. */
  private static final class ActionExecutionFunctionExceptionHandler {
    private final Multimap<SkyKey, Artifact> skyKeyToDerivedArtifactSet;
    private final Map<
            SkyKey,
            ValueOrException3<
                IOException, ActionExecutionException, ArtifactNestedSetEvalException>>
        inputDeps;
    private final Action action;
    private final Set<Artifact> mandatoryInputs;
    private final SkyframeActionExecutor skyframeActionExecutor;
    List<LabelCause> missingArtifactCauses = Lists.newArrayListWithCapacity(0);
    List<NestedSet<Cause>> transitiveCauses = Lists.newArrayListWithCapacity(0);
    private ActionExecutionException firstActionExecutionException;

    ActionExecutionFunctionExceptionHandler(
        Multimap<SkyKey, Artifact> skyKeyToDerivedArtifactSet,
        Map<
                SkyKey,
                ValueOrException3<
                    IOException, ActionExecutionException, ArtifactNestedSetEvalException>>
            inputDeps,
        Action action,
        Set<Artifact> mandatoryInputs,
        SkyframeActionExecutor skyframeActionExecutor) {
      this.skyKeyToDerivedArtifactSet = skyKeyToDerivedArtifactSet;
      this.inputDeps = inputDeps;
      this.action = action;
      this.mandatoryInputs = mandatoryInputs;
      this.skyframeActionExecutor = skyframeActionExecutor;
    }

    /**
     * Go through the list of evaluated SkyKeys and handle any exception that arises, taking into
     * account whether the corresponding artifact(s) is a mandatory input.
     *
     * <p>This also updates ArtifactNestedSetFunction#skyKeyToSkyValue if an Artifact's value is
     * non-null.
     *
     * @throws ActionExecutionException if the eval of any mandatory artifact threw an exception.
     */
    void accumulateAndThrowExceptions() throws ActionExecutionException {
      for (SkyKey key : inputDeps.keySet()) {
        try {
          SkyValue value = inputDeps.get(key).get();
          if (key instanceof ArtifactNestedSetKey || value == null) {
            continue;
          }
          ArtifactNestedSetFunction.getInstance().updateValueForKey(key, value);
        } catch (IOException e) {
          handleIOExceptionFromSkykey(key, e);
        } catch (ActionExecutionException e) {
          handleActionExecutionExceptionFromSkykey(key, e);
        } catch (ArtifactNestedSetEvalException e) {
          for (Pair<SkyKey, Exception> skyKeyAndException : e.getNestedExceptions().toList()) {
            SkyKey skyKey = skyKeyAndException.getFirst();
            Exception inputException = skyKeyAndException.getSecond();
            Preconditions.checkState(
                inputException instanceof IOException
                    || inputException instanceof ActionExecutionException,
                "Unexpected exception type: %s, key: %s",
                inputException,
                skyKey);
            if (inputException instanceof IOException) {
              handleIOExceptionFromSkykey(skyKey, (IOException) inputException);
              continue;
            }
            handleActionExecutionExceptionFromSkykey(
                skyKey, (ActionExecutionException) inputException);
          }
        }
      }

      maybeThrowException();
    }

    private void handleActionExecutionExceptionFromSkykey(SkyKey key, ActionExecutionException e) {
      if (key instanceof Artifact) {
        handleActionExecutionExceptionPerArtifact((Artifact) key, e);
        return;
      }
      for (Artifact input : skyKeyToDerivedArtifactSet.get(key)) {
        handleActionExecutionExceptionPerArtifact(input, e);
      }
    }

    private void handleIOExceptionFromSkykey(SkyKey key, IOException e) {
      if (key instanceof Artifact) {
        handleIOExceptionPerArtifact((Artifact) key, e);
        return;
      }
      for (Artifact input : skyKeyToDerivedArtifactSet.get(key)) {
        handleIOExceptionPerArtifact(input, e);
      }
    }

    void accumulateMissingFileArtifactValue(Artifact input, MissingFileArtifactValue value) {
      missingArtifactCauses.add(createLabelCause(input, value, action.getOwner().getLabel()));
    }

    /** @throws ActionExecutionException if there is any accumulated exception from the inputs. */
    void maybeThrowException() throws ActionExecutionException {
      // We need to rethrow the first exception because it can contain a useful error message.
      if (firstActionExecutionException != null) {
        if (missingArtifactCauses.isEmpty()
            && (checkNotNull(transitiveCauses, action).size() == 1)) {
          // In the case a single action failed, just propagate the exception upward. This avoids
          // having to copy the root causes to the upwards transitive closure.
          throw firstActionExecutionException;
        }
        NestedSetBuilder<Cause> allCauses =
            NestedSetBuilder.<Cause>stableOrder().addAll(missingArtifactCauses);
        transitiveCauses.forEach(allCauses::addTransitive);
        throw new ActionExecutionException(
            firstActionExecutionException.getMessage(),
            firstActionExecutionException.getCause(),
            action,
            allCauses.build(),
            firstActionExecutionException.isCatastrophe(),
            firstActionExecutionException.getDetailedExitCode());
      }

      if (!missingArtifactCauses.isEmpty()) {
        for (LabelCause missingInput : missingArtifactCauses) {
          skyframeActionExecutor.printError(missingInput.getMessage(), action, null);
        }
        throw createMissingInputsException(action, missingArtifactCauses);
      }
    }

    boolean isMandatory(Artifact input) {
      return !input.isSourceArtifact()
          || mandatoryInputs == null
          || mandatoryInputs.contains(input);
    }

    private void handleActionExecutionExceptionPerArtifact(
        Artifact input, ActionExecutionException e) {
      if (isMandatory(input)) {
        // Prefer a catastrophic exception as the one we propagate.
        if (firstActionExecutionException == null
            || (!firstActionExecutionException.isCatastrophe() && e.isCatastrophe())) {
          firstActionExecutionException = e;
        }
        transitiveCauses.add(e.getRootCauses());
      }
    }

    private void handleIOExceptionPerArtifact(Artifact input, IOException e) {
      if (!input.isSourceArtifact()) {
        BugReport.sendBugReport(
            new IllegalStateException(
                "Unexpected IOException for generated artifact: " + input + ", " + action, e));
      }

      if (isMandatory(input)) {
        missingArtifactCauses.add(
            createLabelCause(
                input,
                ArtifactFunction.makeIOExceptionSourceInputFileValue(input, e),
                action.getOwner().getLabel()));
      }
    }
  }

  private static ActionExecutionException createMissingInputsException(
      Action action, List<LabelCause> missingArtifactCauses) {
    DetailedExitCode prioritizedDetailedExitCode =
        missingArtifactCauses.stream()
            .map(LabelCause::getDetailedExitCode)
            .max(DetailedExitCodeComparator.INSTANCE)
            .get();
    return new ActionExecutionException(
        missingArtifactCauses.size() + " input file(s) do not exist",
        action,
        NestedSetBuilder.wrap(Order.STABLE_ORDER, missingArtifactCauses),
        /*catastrophe=*/ false,
        prioritizedDetailedExitCode);
  }

  private static DetailedExitCode createDetailedExitCode(String message, Code detailedCode) {
    return DetailedExitCode.of(
        FailureDetail.newBuilder()
            .setMessage(message)
            .setExecution(Execution.newBuilder().setCode(detailedCode))
            .build());
  }
}
