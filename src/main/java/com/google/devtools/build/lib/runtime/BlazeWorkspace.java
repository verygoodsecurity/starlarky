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
package com.google.devtools.build.lib.runtime;

import static com.google.devtools.build.lib.profiler.GoogleAutoProfilerUtils.profiledAndLogged;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.google.common.flogger.GoogleLogger;
import com.google.devtools.build.lib.actions.cache.ActionCache;
import com.google.devtools.build.lib.actions.cache.CompactPersistentActionCache;
import com.google.devtools.build.lib.analysis.BlazeDirectories;
import com.google.devtools.build.lib.analysis.WorkspaceStatusAction;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.Reporter;
import com.google.devtools.build.lib.exec.BinTools;
import com.google.devtools.build.lib.profiler.AutoProfiler;
import com.google.devtools.build.lib.profiler.ProfilerTask;
import com.google.devtools.build.lib.profiler.memory.AllocationTracker;
import com.google.devtools.build.lib.skyframe.SkyframeExecutor;
import com.google.devtools.build.lib.util.LoggingUtil;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.common.options.OptionsParsingResult;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nullable;

/**
 * This class represents a workspace, and contains operations and data related to it. In contrast,
 * the BlazeRuntime class represents the Blaze server, and contains operations and data that are
 * (supposed to be) independent of the workspace or the current command.
 *
 * <p>At this time, there is still a 1:1 relationship between the BlazeRuntime and the
 * BlazeWorkspace, but the introduction of this class is a step towards allowing 1:N relationships.
 */
public final class BlazeWorkspace {
  public static final String DO_NOT_BUILD_FILE_NAME = "DO_NOT_BUILD_HERE";

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final BlazeRuntime runtime;
  private final SubscriberExceptionHandler eventBusExceptionHandler;
  private final WorkspaceStatusAction.Factory workspaceStatusActionFactory;
  private final BinTools binTools;
  @Nullable private final AllocationTracker allocationTracker;

  private final BlazeDirectories directories;
  private final SkyframeExecutor skyframeExecutor;
  /** The action cache is loaded lazily on the first build command. */
  private ActionCache actionCache;
  /** The execution time range of the previous build command in this server, if any. */
  @Nullable private Range<Long> lastExecutionRange = null;

  private final String outputBaseFilesystemTypeName;

  public BlazeWorkspace(
      BlazeRuntime runtime,
      BlazeDirectories directories,
      SkyframeExecutor skyframeExecutor,
      SubscriberExceptionHandler eventBusExceptionHandler,
      WorkspaceStatusAction.Factory workspaceStatusActionFactory,
      BinTools binTools,
      @Nullable AllocationTracker allocationTracker) {
    this.runtime = runtime;
    this.eventBusExceptionHandler = Preconditions.checkNotNull(eventBusExceptionHandler);
    this.workspaceStatusActionFactory = workspaceStatusActionFactory;
    this.binTools = binTools;
    this.allocationTracker = allocationTracker;

    this.directories = directories;
    this.skyframeExecutor = skyframeExecutor;

    if (directories.inWorkspace()) {
      writeOutputBaseReadmeFile();
      writeDoNotBuildHereFile();
    }

    // Here we use outputBase instead of outputPath because we need a file system to create the
    // latter.
    this.outputBaseFilesystemTypeName = FileSystemUtils.getFileSystem(getOutputBase());
  }

  public BlazeRuntime getRuntime() {
    return runtime;
  }

  /**
   * Returns the Blaze directories object for this runtime.
   */
  public BlazeDirectories getDirectories() {
    return directories;
  }

  public SkyframeExecutor getSkyframeExecutor() {
    return skyframeExecutor;
  }

  public WorkspaceStatusAction.Factory getWorkspaceStatusActionFactory() {
    return workspaceStatusActionFactory;
  }

  public BinTools getBinTools() {
    return binTools;
  }

  /**
   * Returns the working directory of the server.
   *
   * <p>This is often the first entry on the {@code --package_path}, but not always.
   * Callers should certainly not make this assumption. The Path returned may be null.
   */
  public Path getWorkspace() {
    return directories.getWorkspace();
  }

  /**
   * Returns the output base directory associated with this Blaze server
   * process. This is the base directory for shared Blaze state as well as tool
   * and strategy specific subdirectories.
   */
  public Path getOutputBase() {
    return directories.getOutputBase();
  }

  /**
   * Returns the cached value of
   * {@code getOutputBase().getFilesystem().getFileSystemType(getOutputBase())}, which is assumed
   * to be constant for a fixed workspace for the life of the Blaze server.
   */
  public String getOutputBaseFilesystemTypeName() {
    return outputBaseFilesystemTypeName;
  }

  public Path getInstallBase() {
    return directories.getInstallBase();
  }

  /**
   * Returns path to the cache directory. Path must be inside output base to
   * ensure that users can run concurrent instances of blaze in different
   * clients without attempting to concurrently write to the same action cache
   * on disk, which might not be safe.
   */
  Path getCacheDirectory() {
    return getOutputBase().getChild("action_cache");
  }

  void recordLastExecutionTime(long commandStartTime) {
    long currentTimeMillis = runtime.getClock().currentTimeMillis();
    lastExecutionRange =
        currentTimeMillis >= commandStartTime
            ? Range.closed(commandStartTime, currentTimeMillis)
            : null;
  }

  /**
   * Range that represents the last execution time of a build in millis since epoch.
   */
  @Nullable
  public Range<Long> getLastExecutionTimeRange() {
    return lastExecutionRange;
  }

  /**
   * Initializes a CommandEnvironment to execute a command in this workspace.
   *
   * <p>This method should be called from the "main" thread on which the command will execute; that
   * thread will receive interruptions if a module requests an early exit.
   *
   * @param warnings a list of warnings to which the CommandEnvironment can add any warning
   *     generated during initialization. This is needed because Blaze's output handling is not yet
   *     fully configured at this point.
   */
  public CommandEnvironment initCommand(
      Command command,
      OptionsParsingResult options,
      List<String> warnings,
      long waitTimeInMs,
      long commandStartTime) {
    CommandEnvironment env =
        new CommandEnvironment(
            runtime,
            this,
            new EventBus(eventBusExceptionHandler),
            Thread.currentThread(),
            command,
            options,
            warnings,
            waitTimeInMs,
            commandStartTime);
    skyframeExecutor.setClientEnv(env.getClientEnv());
    return env;
  }

  void clearEventBus() {
    // EventBus does not have an unregister() method, so this is how we release memory associated
    // with handlers.
    skyframeExecutor.setEventBus(null);
  }

  /**
   * Reinitializes the Skyframe evaluator.
   */
  public void resetEvaluator() {
    skyframeExecutor.resetEvaluator();
  }

  /** Removes in-memory and on-disk action caches. */
  public void clearCaches() throws IOException {
    if (actionCache != null) {
      actionCache.clear();
    }
    actionCache = null;
    getCacheDirectory().deleteTree();
  }

  /**
   * Returns reference to the lazily instantiated persistent action cache instance. Note, that
   * method may recreate instance between different build requests, so return value should not be
   * cached.
   */
  ActionCache getPersistentActionCache(Reporter reporter) throws IOException {
    if (actionCache == null) {
      try (AutoProfiler p = profiledAndLogged("Loading action cache", ProfilerTask.INFO)) {
        try {
          actionCache = new CompactPersistentActionCache(getCacheDirectory(), runtime.getClock());
        } catch (IOException e) {
          logger.atWarning().withCause(e).log("Failed to load action cache");
          LoggingUtil.logToRemote(
              Level.WARNING, "Failed to load action cache: " + e.getMessage(), e);
          reporter.handle(
              Event.error(
                  "Error during action cache initialization: "
                      + e.getMessage()
                      + ". Corrupted files were renamed to '"
                      + getCacheDirectory()
                      + "/*.bad'. "
                      + "Bazel will now reset action cache data, causing a full rebuild"));
          actionCache = new CompactPersistentActionCache(getCacheDirectory(), runtime.getClock());
        }
      }
    }
    return actionCache;
  }

  /**
   * Generates a README file in the output base directory. This README file
   * contains the name of the workspace directory, so that users can figure out
   * which output base directory corresponds to which workspace.
   */
  private void writeOutputBaseReadmeFile() {
    Preconditions.checkNotNull(getWorkspace());
    Path outputBaseReadmeFile = getOutputBase().getRelative("README");
    try {
      FileSystemUtils.writeIsoLatin1(
          outputBaseReadmeFile,
          "WORKSPACE: " + getWorkspace(),
          "",
          "The first line of this file is intentionally easy to parse for various",
          "interactive scripting and debugging purposes.  But please DO NOT write programs",
          "that exploit it, as they will be broken by design: it is not possible to",
          "reverse engineer the set of source trees or the --package_path from the output",
          "tree, and if you attempt it, you will fail, creating subtle and",
          "hard-to-diagnose bugs, that will no doubt get blamed on changes made by the",
          "Bazel team.",
          "",
          "This directory was generated by Bazel.",
          "Do not attempt to modify or delete any files in this directory.",
          "Among other issues, Bazel's file system caching assumes that",
          "only Bazel will modify this directory and the files in it,",
          "so if you change anything here you may mess up Bazel's cache.");
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Couldn't write to '%s'", outputBaseReadmeFile);
    }
  }

  private void writeDoNotBuildHereFile(Path filePath) {
    try {
      FileSystemUtils.createDirectoryAndParents(filePath.getParentDirectory());
      FileSystemUtils.writeContent(filePath, ISO_8859_1, getWorkspace().toString());
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Couldn't write to '%s'", filePath);
    }
  }

  private void writeDoNotBuildHereFile() {
    Preconditions.checkNotNull(getWorkspace());
    writeDoNotBuildHereFile(getOutputBase().getRelative(DO_NOT_BUILD_FILE_NAME));
    writeDoNotBuildHereFile(
        getOutputBase().getRelative("execroot").getRelative(DO_NOT_BUILD_FILE_NAME));
  }

  @Nullable
  public AllocationTracker getAllocationTracker() {
    return allocationTracker;
  }
}

