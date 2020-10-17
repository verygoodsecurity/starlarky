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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.devtools.build.lib.actions.FileValue;
import com.google.devtools.build.lib.actions.InconsistentFilesystemException;
import com.google.devtools.build.lib.clock.BlazeClock;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.LabelConstants;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.cmdline.RepositoryName;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.ExtendedEventHandler.Postable;
import com.google.devtools.build.lib.packages.BuildFileContainsErrorsException;
import com.google.devtools.build.lib.packages.BuildFileNotFoundException;
import com.google.devtools.build.lib.packages.CachingPackageLocator;
import com.google.devtools.build.lib.packages.Globber;
import com.google.devtools.build.lib.packages.InvalidPackageNameException;
import com.google.devtools.build.lib.packages.LegacyGlobber;
import com.google.devtools.build.lib.packages.NoSuchPackageException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.PackageFactory;
import com.google.devtools.build.lib.packages.PackageValidator.InvalidPackageException;
import com.google.devtools.build.lib.packages.RuleVisibility;
import com.google.devtools.build.lib.packages.Target;
import com.google.devtools.build.lib.packages.WorkspaceFileValue;
import com.google.devtools.build.lib.packages.semantics.BuildLanguageOptions;
import com.google.devtools.build.lib.profiler.Profiler;
import com.google.devtools.build.lib.profiler.ProfilerTask;
import com.google.devtools.build.lib.profiler.SilentCloseable;
import com.google.devtools.build.lib.repository.ExternalPackageHelper;
import com.google.devtools.build.lib.rules.repository.WorkspaceFileHelper;
import com.google.devtools.build.lib.server.FailureDetails.FailureDetail;
import com.google.devtools.build.lib.server.FailureDetails.PackageLoading;
import com.google.devtools.build.lib.server.FailureDetails.PackageLoading.Code;
import com.google.devtools.build.lib.skyframe.BzlLoadFunction.BzlLoadFailedException;
import com.google.devtools.build.lib.skyframe.GlobValue.InvalidGlobPatternException;
import com.google.devtools.build.lib.util.DetailedExitCode;
import com.google.devtools.build.lib.util.Pair;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.build.lib.vfs.UnixGlob;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyFunctionException.Transience;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import com.google.devtools.build.skyframe.ValueOrException;
import com.google.devtools.build.skyframe.ValueOrException2;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.Location;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.StarlarkFile;

/** A SkyFunction for {@link PackageValue}s. */
public class PackageFunction implements SkyFunction {

  private final PackageFactory packageFactory;
  private final CachingPackageLocator packageLocator;
  private final Cache<PackageIdentifier, LoadedPackageCacheEntry> packageFunctionCache;
  private final Cache<PackageIdentifier, StarlarkFile> fileSyntaxCache;
  private final AtomicBoolean showLoadingProgress;
  private final AtomicInteger numPackagesLoaded;
  @Nullable private final PackageProgressReceiver packageProgress;
  private final ExternalPackageHelper externalPackageHelper;

  // Not final only for testing.
  @Nullable private BzlLoadFunction bzlLoadFunctionForInlining;

  private final ActionOnIOExceptionReadingBuildFile actionOnIOExceptionReadingBuildFile;

  private final IncrementalityIntent incrementalityIntent;

  public PackageFunction(
      PackageFactory packageFactory,
      CachingPackageLocator pkgLocator,
      AtomicBoolean showLoadingProgress,
      Cache<PackageIdentifier, LoadedPackageCacheEntry> packageFunctionCache,
      Cache<PackageIdentifier, StarlarkFile> fileSyntaxCache,
      AtomicInteger numPackagesLoaded,
      @Nullable BzlLoadFunction bzlLoadFunctionForInlining,
      @Nullable PackageProgressReceiver packageProgress,
      ActionOnIOExceptionReadingBuildFile actionOnIOExceptionReadingBuildFile,
      IncrementalityIntent incrementalityIntent,
      ExternalPackageHelper externalPackageHelper) {
    this.bzlLoadFunctionForInlining = bzlLoadFunctionForInlining;
    this.packageFactory = packageFactory;
    this.packageLocator = pkgLocator;
    this.showLoadingProgress = showLoadingProgress;
    this.packageFunctionCache = packageFunctionCache;
    this.fileSyntaxCache = fileSyntaxCache;
    this.numPackagesLoaded = numPackagesLoaded;
    this.packageProgress = packageProgress;
    this.actionOnIOExceptionReadingBuildFile = actionOnIOExceptionReadingBuildFile;
    this.incrementalityIntent = incrementalityIntent;
    this.externalPackageHelper = externalPackageHelper;
  }

  @VisibleForTesting
  public PackageFunction(
      PackageFactory packageFactory,
      CachingPackageLocator pkgLocator,
      AtomicBoolean showLoadingProgress,
      Cache<PackageIdentifier, LoadedPackageCacheEntry> packageFunctionCache,
      Cache<PackageIdentifier, StarlarkFile> fileSyntaxCache,
      AtomicInteger numPackagesLoaded,
      @Nullable BzlLoadFunction bzlLoadFunctionForInlining,
      ExternalPackageHelper externalPackageHelper) {
    this(
        packageFactory,
        pkgLocator,
        showLoadingProgress,
        packageFunctionCache,
        fileSyntaxCache,
        numPackagesLoaded,
        bzlLoadFunctionForInlining,
        /*packageProgress=*/ null,
        ActionOnIOExceptionReadingBuildFile.UseOriginalIOException.INSTANCE,
        IncrementalityIntent.INCREMENTAL,
        externalPackageHelper);
  }

  public void setBzlLoadFunctionForInliningForTesting(BzlLoadFunction bzlLoadFunctionForInlining) {
    this.bzlLoadFunctionForInlining = bzlLoadFunctionForInlining;
  }

  /**
   * What to do when encountering an {@link IOException} trying to read the contents of a BUILD
   * file.
   *
   * <p>Any choice besides {@link
   * ActionOnIOExceptionReadingBuildFile.UseOriginalIOException#INSTANCE} is potentially
   * incrementally unsound: if the initial {@link IOException} is transient, then Blaze will
   * "incorrectly" not attempt to redo package loading for this BUILD file on incremental builds.
   *
   * <p>The fact that this behavior is configurable and potentially unsound is a concession to
   * certain desired use cases with fancy filesystems.
   */
  public interface ActionOnIOExceptionReadingBuildFile {
    /**
     * Given the {@link IOException} encountered when reading the contents of the given BUILD file,
     * returns the contents that should be used, or {@code null} if the original {@link IOException}
     * should be respected (that is, we should error-out with a package loading error).
     */
    @Nullable
    byte[] maybeGetBuildFileContentsToUse(
        PathFragment buildFilePathFragment, IOException originalExn);

    /**
     * A {@link ActionOnIOExceptionReadingBuildFile} whose {@link #maybeGetBuildFileContentsToUse}
     * has the sensible behavior of always respecting the initial {@link IOException}.
     */
    public static class UseOriginalIOException implements ActionOnIOExceptionReadingBuildFile {
      public static final UseOriginalIOException INSTANCE = new UseOriginalIOException();

      private UseOriginalIOException() {}

      @Override
      @Nullable
      public byte[] maybeGetBuildFileContentsToUse(
          PathFragment buildFilePathFragment, IOException originalExn) {
        return null;
      }
    }
  }

  /** An entry in {@link PackageFunction} internal cache. */
  public static class LoadedPackageCacheEntry {
    private final Package.Builder builder;
    private final Set<SkyKey> globDepKeys;
    private final long loadTimeNanos;

    private LoadedPackageCacheEntry(
        Package.Builder builder, Set<SkyKey> globDepKeys, long loadTimeNanos) {
      this.builder = builder;
      this.globDepKeys = globDepKeys;
      this.loadTimeNanos = loadTimeNanos;
    }
  }

  /**
   * A declaration to {@link PackageFunction} about how it will be used, for the sake of making
   * use-case-driven performance optimizations.
   */
  public enum IncrementalityIntent {
    /**
     * {@link PackageFunction} will be used to load packages incrementally (e.g. on both clean
     * builds and incremental builds, perhaps with cached globs). This is Bazel's normal use-case.
     */
    INCREMENTAL,

    /**
     * {@link PackageFunction} will never be used to load packages incrementally.
     *
     * <p>Do not use this unless you know what you are doing; Bazel will be intentionally
     * incrementally incorrect!
     */
    NON_INCREMENTAL
  }

  private static void maybeThrowFilesystemInconsistency(
      PackageIdentifier pkgId, Exception skyframeException, boolean packageWasInError)
      throws InternalInconsistentFilesystemException {
    if (!packageWasInError) {
      throw new InternalInconsistentFilesystemException(
          pkgId,
          "Encountered error '"
              + skyframeException.getMessage()
              + "' but didn't encounter it when doing the same thing earlier in the build");
    }
  }

  /**
   * These deps have already been marked (see {@link SkyframeHybridGlobber}) but we need to properly
   * handle symlink issues that legacy globbing can't handle gracefully.
   */
  private static void handleGlobDepsAndPropagateFilesystemExceptions(
      PackageIdentifier packageIdentifier,
      Iterable<SkyKey> depKeys,
      Environment env,
      boolean packageWasInError)
      throws InternalInconsistentFilesystemException, FileSymlinkException, InterruptedException {
    checkState(Iterables.all(depKeys, SkyFunctions.isSkyFunction(SkyFunctions.GLOB)), depKeys);
    FileSymlinkException arbitraryFse = null;
    for (Map.Entry<SkyKey, ValueOrException2<IOException, BuildFileNotFoundException>> entry :
        env.getValuesOrThrow(depKeys, IOException.class, BuildFileNotFoundException.class)
            .entrySet()) {
      try {
        entry.getValue().get();
      } catch (InconsistentFilesystemException e) {
        throw new InternalInconsistentFilesystemException(packageIdentifier, e);
      } catch (FileSymlinkException e) {
        // Legacy globbing doesn't explicitly detect symlink issues, but certain filesystems might
        // detect some symlink issues. For example, many filesystems have a hardcoded bound on the
        // number of symlink hops they will follow when resolving paths (e.g. Unix's ELOOP). Since
        // Skyframe globbing does explicitly detect symlink issues, we are able to:
        //   (1) Provide a more informative error message.
        //   (2) Confidently act as though the symlink issue is non-transient.
        arbitraryFse = e;
      } catch (IOException | BuildFileNotFoundException e) {
        maybeThrowFilesystemInconsistency(packageIdentifier, e, packageWasInError);
      }
    }
    if (arbitraryFse != null) {
      // If there was at least one symlink issue and no inconsistent filesystem issues, arbitrarily
      // rethrow one of the symlink issues.
      throw arbitraryFse;
    }
  }

  /**
   * Adds a dependency on the WORKSPACE file, representing it as a special type of package.
   *
   * @throws PackageFunctionException if there is an error computing the workspace file or adding
   *     its rules to the //external package.
   */
  private SkyValue getExternalPackage(Environment env)
      throws PackageFunctionException, InterruptedException {
    StarlarkSemantics starlarkSemantics = PrecomputedValue.STARLARK_SEMANTICS.get(env);
    RootedPath workspacePath = externalPackageHelper.findWorkspaceFile(env);
    if (env.valuesMissing()) {
      return null;
    }

    SkyKey workspaceKey = ExternalPackageFunction.key(workspacePath);
    PackageValue workspace = null;
    try {
      // This may throw a NoSuchPackageException if the WORKSPACE file was malformed or had other
      // problems. Since this function can't add much context, we silently bubble it up.
      workspace =
          (PackageValue)
              env.getValueOrThrow(
                  workspaceKey,
                  IOException.class,
                  EvalException.class,
                  BzlLoadFailedException.class);
    } catch (IOException | EvalException | BzlLoadFailedException e) {
      String message = "Error encountered while dealing with the WORKSPACE file: " + e.getMessage();
      throw PackageFunctionException.builder()
          .setType(PackageFunctionException.Type.NO_SUCH_PACKAGE)
          .setTransience(Transience.PERSISTENT)
          .setPackageIdentifier(LabelConstants.EXTERNAL_PACKAGE_IDENTIFIER)
          .setMessage(message)
          .setPackageLoadingCode(PackageLoading.Code.WORKSPACE_FILE_ERROR)
          .build();
    }
    if (workspace == null) {
      return null;
    }

    Package pkg = workspace.getPackage();
    if (packageFactory != null) {
      try {
        packageFactory.afterDoneLoadingPackage(
            pkg,
            starlarkSemantics,
            // This is a lie.
            /*loadTimeNanos=*/ 0L,
            env.getListener());
      } catch (InvalidPackageException e) {
        throw new PackageFunctionException(e, Transience.PERSISTENT);
      }
    }
    return new PackageValue(pkg);
  }

  @Override
  public SkyValue compute(SkyKey key, Environment env)
      throws PackageFunctionException, InterruptedException {
    PackageIdentifier packageId = (PackageIdentifier) key.argument();
    if (packageId.equals(LabelConstants.EXTERNAL_PACKAGE_IDENTIFIER)) {
      return getExternalPackage(env);
    }

    // TODO(adonovan): opt: can't all the following statements be moved
    // into the packageFunctionCache.getIfPresent cache-miss case?

    SkyKey packageLookupKey = PackageLookupValue.key(packageId);
    PackageLookupValue packageLookupValue;
    try {
      packageLookupValue =
          (PackageLookupValue)
              env.getValueOrThrow(
                  packageLookupKey,
                  BuildFileNotFoundException.class,
                  InconsistentFilesystemException.class);
    } catch (BuildFileNotFoundException e) {
      throw new PackageFunctionException(e, Transience.PERSISTENT);
    } catch (InconsistentFilesystemException e) {
      // This error is not transient from the perspective of the PackageFunction.
      throw PackageFunctionException.builder()
          .setType(PackageFunctionException.Type.NO_SUCH_PACKAGE)
          .setTransience(Transience.PERSISTENT)
          .setPackageIdentifier(packageId)
          .setMessage(e.getMessage())
          .setException(e)
          .setPackageLoadingCode(PackageLoading.Code.PERSISTENT_INCONSISTENT_FILESYSTEM_ERROR)
          .build();
    }
    if (packageLookupValue == null) {
      return null;
    }

    if (!packageLookupValue.packageExists()) {
      PackageFunctionException.Builder exceptionBuilder =
          PackageFunctionException.builder()
              .setPackageIdentifier(packageId)
              .setTransience(Transience.PERSISTENT);
      switch (packageLookupValue.getErrorReason()) {
        case NO_BUILD_FILE:
          String message = PackageLookupFunction.explainNoBuildFileValue(packageId, env);
          throw exceptionBuilder
              .setType(PackageFunctionException.Type.BUILD_FILE_NOT_FOUND)
              .setMessage(message)
              .setPackageLoadingCode(PackageLoading.Code.BUILD_FILE_MISSING)
              .build();
        case DELETED_PACKAGE:
        case REPOSITORY_NOT_FOUND:
          throw exceptionBuilder
              .setType(PackageFunctionException.Type.BUILD_FILE_NOT_FOUND)
              .setMessage(packageLookupValue.getErrorMsg())
              .setPackageLoadingCode(PackageLoading.Code.REPOSITORY_MISSING)
              .build();
        case INVALID_PACKAGE_NAME:
          throw exceptionBuilder
              .setType(PackageFunctionException.Type.INVALID_PACKAGE_NAME)
              .setMessage(packageLookupValue.getErrorMsg())
              .setPackageLoadingCode(PackageLoading.Code.INVALID_NAME)
              .build();
      }
      // We should never get here.
      throw new IllegalStateException();
    }

    WorkspaceNameValue workspaceNameValue =
        (WorkspaceNameValue) env.getValue(WorkspaceNameValue.key());

    RepositoryMappingValue repositoryMappingValue =
        (RepositoryMappingValue)
            env.getValue(RepositoryMappingValue.key(packageId.getRepository()));
    RootedPath buildFileRootedPath = packageLookupValue.getRootedPath(packageId);

    FileValue buildFileValue = getBuildFileValue(env, buildFileRootedPath);
    RuleVisibility defaultVisibility = PrecomputedValue.DEFAULT_VISIBILITY.get(env);
    StarlarkSemantics starlarkSemantics = PrecomputedValue.STARLARK_SEMANTICS.get(env);
    IgnoredPackagePrefixesValue repositoryIgnoredPackagePrefixes =
        (IgnoredPackagePrefixesValue)
            env.getValue(IgnoredPackagePrefixesValue.key(packageId.getRepository()));
    if (env.valuesMissing()) {
      return null;
    }

    String workspaceName = workspaceNameValue.getName();
    ImmutableMap<RepositoryName, RepositoryName> repositoryMapping =
        repositoryMappingValue.getRepositoryMapping();

    Label preludeLabel = null;
    // Can be null in tests.
    if (packageFactory != null) {
      // Load the prelude from the same repository as the package being loaded.  Can't use
      // Label.resolveRepositoryRelative because rawPreludeLabel is in the main repository, not the
      // default one, so it is resolved to itself.
      // TODO(brandjon): Why can't we just replace the use of the main repository with the default
      // repository in the prelude label?
      Label rawPreludeLabel = packageFactory.getRuleClassProvider().getPreludeLabel();
      if (rawPreludeLabel != null) {
        PackageIdentifier preludePackage =
            PackageIdentifier.create(
                packageId.getRepository(), rawPreludeLabel.getPackageFragment());
        preludeLabel = Label.createUnvalidated(preludePackage, rawPreludeLabel.getName());
      }
    }

    LoadedPackageCacheEntry packageCacheEntry = packageFunctionCache.getIfPresent(packageId);
    if (packageCacheEntry == null) {
      packageCacheEntry =
          loadPackage(
              workspaceName,
              repositoryMapping,
              repositoryIgnoredPackagePrefixes.getPatterns(),
              packageId,
              buildFileRootedPath,
              buildFileValue,
              defaultVisibility,
              starlarkSemantics,
              preludeLabel,
              packageLookupValue.getRoot(),
              env);
      if (packageCacheEntry == null) {
        return null;
      }
      packageFunctionCache.put(packageId, packageCacheEntry);
    }
    PackageFunctionException pfeFromLegacyPackageLoading = null;
    Package.Builder pkgBuilder = packageCacheEntry.builder;
    try {
      pkgBuilder.buildPartial();
    } catch (NoSuchPackageException e) {
      // If legacy globbing encounters an IOException, #buildPartial with throw a
      // NoSuchPackageException. If that happens, we prefer throwing an exception derived from
      // Skyframe globbing. See the comments in #handleGlobDepsAndPropagateFilesystemExceptions.
      // Therefore we store the exception encountered here and maybe use it later.
      pfeFromLegacyPackageLoading =
          new PackageFunctionException(
              e,
              e.getCause() instanceof SkyframeGlobbingIOException
                  ? Transience.PERSISTENT
                  : Transience.TRANSIENT);
    }
    try {
      // Since the Skyframe dependencies we request below in
      // handleGlobDepsAndPropagateFilesystemExceptions are requested independently of
      // the ones requested here in
      // handleLabelsCrossingSubpackagesAndPropagateInconsistentFilesystemExceptions, we don't
      // bother checking for missing values and instead piggyback on the env.missingValues() call
      // for the former. This avoids a Skyframe restart.
      handleLabelsCrossingSubpackagesAndPropagateInconsistentFilesystemExceptions(
          packageLookupValue.getRoot(), packageId, pkgBuilder, env);
    } catch (InternalInconsistentFilesystemException e) {
      packageFunctionCache.invalidate(packageId);
      PackageLoading.Code packageLoadingCode =
          e.isTransient()
              ? PackageLoading.Code.TRANSIENT_INCONSISTENT_FILESYSTEM_ERROR
              : PackageLoading.Code.PERSISTENT_INCONSISTENT_FILESYSTEM_ERROR;
      throw new PackageFunctionException(
          e.toNoSuchPackageException(packageLoadingCode),
          e.isTransient() ? Transience.TRANSIENT : Transience.PERSISTENT);
    }
    Set<SkyKey> globKeys = packageCacheEntry.globDepKeys;
    try {
      handleGlobDepsAndPropagateFilesystemExceptions(
          packageId, globKeys, env, pkgBuilder.containsErrors());
    } catch (InternalInconsistentFilesystemException e) {
      packageFunctionCache.invalidate(packageId);
      PackageLoading.Code packageLoadingCode =
          e.isTransient()
              ? PackageLoading.Code.TRANSIENT_INCONSISTENT_FILESYSTEM_ERROR
              : PackageLoading.Code.PERSISTENT_INCONSISTENT_FILESYSTEM_ERROR;
      throw new PackageFunctionException(
          e.toNoSuchPackageException(packageLoadingCode),
          e.isTransient() ? Transience.TRANSIENT : Transience.PERSISTENT);
    } catch (FileSymlinkException e) {
      packageFunctionCache.invalidate(packageId);
      String message = "Symlink issue while evaluating globs: " + e.getUserFriendlyMessage();
      throw PackageFunctionException.builder()
          .setType(PackageFunctionException.Type.NO_SUCH_PACKAGE)
          .setTransience(Transience.PERSISTENT)
          .setPackageIdentifier(packageId)
          .setMessage(message)
          .setPackageLoadingCode(PackageLoading.Code.EVAL_GLOBS_SYMLINK_ERROR)
          .build();
    }
    if (env.valuesMissing()) {
      return null;
    }

    // We know this SkyFunction will not be called again, so we can remove the cache entry.
    packageFunctionCache.invalidate(packageId);

    if (pfeFromLegacyPackageLoading != null) {
      throw pfeFromLegacyPackageLoading;
    }

    Package pkg = pkgBuilder.finishBuild();

    Event.replayEventsOn(env.getListener(), pkgBuilder.getEvents());
    for (Postable post : pkgBuilder.getPosts()) {
      env.getListener().post(post);
    }

    try {
      packageFactory.afterDoneLoadingPackage(
          pkg, starlarkSemantics, packageCacheEntry.loadTimeNanos, env.getListener());
    } catch (InvalidPackageException e) {
      throw new PackageFunctionException(e, Transience.PERSISTENT);
    }

    return new PackageValue(pkg);
  }

  private static FileValue getBuildFileValue(Environment env, RootedPath buildFileRootedPath)
      throws InterruptedException {
    FileValue buildFileValue;
    try {
      buildFileValue =
          (FileValue) env.getValueOrThrow(FileValue.key(buildFileRootedPath), IOException.class);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Package lookup succeeded but encountered error when "
              + "getting FileValue for BUILD file directly.",
          e);
    }
    if (buildFileValue == null) {
      return null;
    }
    checkState(buildFileValue.exists(), "Package lookup succeeded but BUILD file doesn't exist");
    return buildFileValue;
  }

  /** Value class for the result of {@link #fetchLoadsFromBuildFile}. */
  // Non-private because used in WorkspaceFileFunction. */
  static final class BzlLoadResult {
    final Module preludeModule;
    final ImmutableMap<String, Module> loadedModules;

    BzlLoadResult(Module preludeModule, ImmutableMap<String, Module> loadedModules) {
      this.preludeModule = preludeModule;
      this.loadedModules = loadedModules;
    }
  }

  /**
   * Fetch the Starlark loads for this BUILD file, in source order. If any of them haven't been
   * computed yet, returns null.
   */
  @Nullable
  static BzlLoadResult fetchLoadsFromBuildFile(
      RootedPath buildFilePath,
      PackageIdentifier packageId,
      ImmutableMap<RepositoryName, RepositoryName> repoMapping,
      StarlarkFile file,
      @Nullable Label preludeLabel,
      int workspaceChunk,
      Environment env,
      BzlLoadFunction bzlLoadFunctionForInlining)
      throws NoSuchPackageException, InterruptedException {
    checkArgument(!packageId.getRepository().isDefault());

    // Parse the labels in the file's load statements.
    List<Pair<String, Label>> loads =
        BzlLoadFunction.getLoadLabels(env.getListener(), file, packageId, repoMapping);
    if (loads == null) {
      throw PackageFunctionException.builder()
          .setType(PackageFunctionException.Type.BUILD_FILE_CONTAINS_ERRORS)
          .setPackageIdentifier(packageId)
          .setMessage("malformed load statements")
          .setPackageLoadingCode(PackageLoading.Code.IMPORT_STARLARK_FILE_ERROR)
          .buildCause();
    }

    // Compute key for each label in loads, plus the prelude, which is treated as a load.
    // The ith entry of keys corresponds to the ith entry in loads. If preludeLabel is not null,
    // keys has an additional entry at the end for it.
    List<BzlLoadValue.Key> keys = Lists.newArrayListWithExpectedSize(loads.size() + 1);
    boolean inWorkspace =
        WorkspaceFileHelper.endsWithWorkspaceFileName(buildFilePath.getRootRelativePath());
    for (Pair<String, Label> load : loads) {
      Label bzlLabel = load.second;
      if (inWorkspace) {
        int originalChunk = getOriginalWorkspaceChunk(env, buildFilePath, workspaceChunk, bzlLabel);
        keys.add(BzlLoadValue.keyForWorkspace(bzlLabel, originalChunk, buildFilePath));
      } else {
        keys.add(BzlLoadValue.keyForBuild(bzlLabel));
      }
    }
    if (preludeLabel != null) {
      keys.add(BzlLoadValue.keyForBuildPrelude(preludeLabel));
    }

    // Load .bzl modules in parallel.
    List<BzlLoadValue> bzlLoads;
    try {
      bzlLoads =
          bzlLoadFunctionForInlining == null
              ? computeBzlLoadsNoInlining(env, keys)
              : computeBzlLoadsWithInlining(env, keys, bzlLoadFunctionForInlining);
    } catch (BzlLoadFailedException e) {
      Throwable rootCause = Throwables.getRootCause(e);
      throw PackageFunctionException.builder()
          .setType(PackageFunctionException.Type.BUILD_FILE_CONTAINS_ERRORS)
          .setPackageIdentifier(packageId)
          .setException(rootCause instanceof IOException ? (IOException) rootCause : null)
          .setMessage(e.getMessage())
          .setPackageLoadingCode(PackageLoading.Code.IMPORT_STARLARK_FILE_ERROR)
          .buildCause();
    }
    if (bzlLoads == null) {
      return null; // Skyframe deps unavailable
    }

    // Process the loaded modules.
    Map<String, Module> loadedModules = Maps.newLinkedHashMapWithExpectedSize(loads.size());
    for (int i = 0; i < loads.size(); i++) {
      String loadString = loads.get(i).first;
      BzlLoadValue v = bzlLoads.get(i);
      loadedModules.put(loadString, v.getModule()); // dups ok
    }
    Module preludeModule =
        preludeLabel != null ? bzlLoads.get(loads.size()).getModule() : Module.create();
    return new BzlLoadResult(preludeModule, ImmutableMap.copyOf(loadedModules));
  }

  /**
   * Compute the BzlLoadValue for all given keys using vanilla Skyframe evaluation, returning {@code
   * null} if Skyframe deps were missing and have been requested.
   */
  @Nullable
  private static List<BzlLoadValue> computeBzlLoadsNoInlining(
      Environment env, List<BzlLoadValue.Key> keys)
      throws InterruptedException, BzlLoadFailedException {
    List<BzlLoadValue> bzlLoads = Lists.newArrayListWithExpectedSize(keys.size());
    Map<SkyKey, ValueOrException<BzlLoadFailedException>> starlarkLookupResults =
        env.getValuesOrThrow(keys, BzlLoadFailedException.class);
    for (BzlLoadValue.Key key : keys) {
      bzlLoads.add((BzlLoadValue) starlarkLookupResults.get(key).get());
    }
    return env.valuesMissing() ? null : bzlLoads;
  }

  /**
   * Compute the BzlLoadValue for all given keys by "inlining" the BzlLoadFunction and bypassing
   * traditional Skyframe evaluation, returning {@code null} if Skyframe deps were missing and have
   * been requested.
   */
  @Nullable
  private static List<BzlLoadValue> computeBzlLoadsWithInlining(
      Environment env, List<BzlLoadValue.Key> keys, BzlLoadFunction bzlLoadFunctionForInlining)
      throws InterruptedException, BzlLoadFailedException {
    List<BzlLoadValue> bzlLoads = Lists.newArrayListWithExpectedSize(keys.size());
    // See the comment about the desire for deterministic graph structure in BzlLoadFunction for the
    // motivation of this approach to exception handling.
    BzlLoadFailedException deferredException = null;
    // Compute BzlLoadValue for each key, sharing the same inlining state, i.e. cache of loaded
    // modules. This ensures that each .bzl is loaded only once, regardless of diamond dependencies
    // or cache eviction. (Multiple loads of the same .bzl would screw up identity equality of some
    // Starlark symbols -- see comments in BzlLoadFunction#computeInline.)
    BzlLoadFunction.InliningState inliningState = BzlLoadFunction.InliningState.create();
    for (BzlLoadValue.Key key : keys) {
      SkyValue skyValue;
      try {
        // Will complete right away if this key has been seen before in inliningState -- regardless
        // of whether it was evaluated successfully, had missing deps, or was found to be in error.
        skyValue = bzlLoadFunctionForInlining.computeInline(key, env, inliningState);
      } catch (BzlLoadFailedException e) {
        if (deferredException == null) {
          deferredException = e;
        }
        continue;
      }
      if (skyValue != null) {
        bzlLoads.add((BzlLoadValue) skyValue);
      }
      // A null value for `skyValue` can occur when it (or its transitive loads) has a Skyframe dep
      // that is missing or in error. It can also occur if there's a transitive load on a bzl that
      // was already seen by inliningState and which returned null. In both these cases, we want to
      // continue making our inline calls, so as to maximize the number of dependent (non-inlined)
      // SkyFunctions that are requested and avoid a quadratic number of restarts.
    }
    if (deferredException != null) {
      throw deferredException;
    }
    return env.valuesMissing() ? null : bzlLoads;
  }

  private static int getOriginalWorkspaceChunk(
      Environment env, RootedPath workspacePath, int workspaceChunk, Label loadLabel)
      throws InterruptedException {
    if (workspaceChunk < 1) {
      return workspaceChunk;
    }
    // If we got here, we are already computing workspaceChunk "workspaceChunk", and so we know
    // that the value for "workspaceChunk-1" has already been computed so we don't need to check
    // for nullness
    SkyKey workspaceFileKey = WorkspaceFileValue.key(workspacePath, workspaceChunk - 1);
    WorkspaceFileValue workspaceFileValue = (WorkspaceFileValue) env.getValue(workspaceFileKey);
    ImmutableMap<String, Integer> loadToChunkMap = workspaceFileValue.getLoadToChunkMap();
    String loadString = loadLabel.toString();
    return loadToChunkMap.getOrDefault(loadString, workspaceChunk);
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }

  private static void handleLabelsCrossingSubpackagesAndPropagateInconsistentFilesystemExceptions(
      Root pkgRoot, PackageIdentifier pkgId, Package.Builder pkgBuilder, Environment env)
      throws InternalInconsistentFilesystemException, InterruptedException {
    PathFragment pkgDir = pkgId.getPackageFragment();
    Set<SkyKey> containingPkgLookupKeys = Sets.newHashSet();
    Map<Target, SkyKey> targetToKey = new HashMap<>();
    for (Target target : pkgBuilder.getTargets()) {
      PathFragment dir = Label.getContainingDirectory(target.getLabel());
      if (dir.equals(pkgDir)) {
        continue;
      }
      PackageIdentifier dirId = PackageIdentifier.create(pkgId.getRepository(), dir);
      SkyKey key = ContainingPackageLookupValue.key(dirId);
      targetToKey.put(target, key);
      containingPkgLookupKeys.add(key);
    }
    Map<SkyKey, ValueOrException2<BuildFileNotFoundException, InconsistentFilesystemException>>
        containingPkgLookupValues =
            env.getValuesOrThrow(
                containingPkgLookupKeys,
                BuildFileNotFoundException.class,
                InconsistentFilesystemException.class);
    if (env.valuesMissing() || containingPkgLookupKeys.isEmpty()) {
      return;
    }
    for (Iterator<Target> iterator = pkgBuilder.getTargets().iterator(); iterator.hasNext(); ) {
      Target target = iterator.next();
      SkyKey key = targetToKey.get(target);
      if (!containingPkgLookupValues.containsKey(key)) {
        continue;
      }
      ContainingPackageLookupValue containingPackageLookupValue =
          getContainingPkgLookupValueAndPropagateInconsistentFilesystemExceptions(
              pkgId, containingPkgLookupValues.get(key), env);
      if (maybeAddEventAboutLabelCrossingSubpackage(
          pkgBuilder,
          pkgRoot,
          target.getLabel(),
          target.getLocation(),
          containingPackageLookupValue)) {
        iterator.remove();
        pkgBuilder.setContainsErrors();
      }
    }
  }

  @Nullable
  private static ContainingPackageLookupValue
      getContainingPkgLookupValueAndPropagateInconsistentFilesystemExceptions(
          PackageIdentifier packageIdentifier,
          ValueOrException2<BuildFileNotFoundException, InconsistentFilesystemException>
              containingPkgLookupValueOrException,
          Environment env)
          throws InternalInconsistentFilesystemException {
    try {
      return (ContainingPackageLookupValue) containingPkgLookupValueOrException.get();
    } catch (BuildFileNotFoundException e) {
      env.getListener().handle(Event.error(null, e.getMessage()));
      return null;
    } catch (InconsistentFilesystemException e) {
      throw new InternalInconsistentFilesystemException(packageIdentifier, e);
    }
  }

  private static boolean maybeAddEventAboutLabelCrossingSubpackage(
      Package.Builder pkgBuilder,
      Root pkgRoot,
      Label label,
      @Nullable Location location,
      @Nullable ContainingPackageLookupValue containingPkgLookupValue) {
    if (containingPkgLookupValue == null) {
      return true;
    }
    if (!containingPkgLookupValue.hasContainingPackage()) {
      // The missing package here is a problem, but it's not an error from the perspective of
      // PackageFunction.
      return false;
    }
    PackageIdentifier containingPkg = containingPkgLookupValue.getContainingPackageName();
    if (containingPkg.equals(label.getPackageIdentifier())) {
      // The label does not cross a subpackage boundary.
      return false;
    }
    if (!containingPkg.getSourceRoot().startsWith(label.getPackageIdentifier().getSourceRoot())) {
      // This label is referencing an imaginary package, because the containing package should
      // extend the label's package: if the label is //a/b:c/d, the containing package could be
      // //a/b/c or //a/b, but should never be //a. Usually such errors will be caught earlier, but
      // in some exceptional cases (such as a Python-aware BUILD file catching its own io
      // exceptions), it reaches here, and we tolerate it.
      return false;
    }
    String message =
        ContainingPackageLookupValue.getErrorMessageForLabelCrossingPackageBoundary(
            pkgRoot, label, containingPkgLookupValue);
    pkgBuilder.addEvent(Package.error(location, message, Code.LABEL_CROSSES_PACKAGE_BOUNDARY));
    return true;
  }

  private interface GlobberWithSkyframeGlobDeps extends Globber {
    Set<SkyKey> getGlobDepsRequested();
  }

  private static class LegacyGlobberWithNoGlobDeps implements GlobberWithSkyframeGlobDeps {
    private final LegacyGlobber delegate;

    private LegacyGlobberWithNoGlobDeps(LegacyGlobber delegate) {
      this.delegate = delegate;
    }

    @Override
    public Set<SkyKey> getGlobDepsRequested() {
      return ImmutableSet.of();
    }

    @Override
    public Token runAsync(
        List<String> includes, List<String> excludes, boolean excludeDirs, boolean allowEmpty)
        throws BadGlobException, InterruptedException {
      return delegate.runAsync(includes, excludes, excludeDirs, allowEmpty);
    }

    @Override
    public List<String> fetchUnsorted(Token token)
        throws BadGlobException, IOException, InterruptedException {
      return delegate.fetchUnsorted(token);
    }

    @Override
    public void onInterrupt() {
      delegate.onInterrupt();
    }

    @Override
    public void onCompletion() {
      delegate.onCompletion();
    }
  }

  /**
   * A {@link Globber} implemented on top of skyframe that falls back to a {@link LegacyGlobber} on
   * a skyframe cache-miss. This way we don't require a skyframe restart after a call to {@link
   * Globber#runAsync} and before/during a call to {@link Globber#fetch}.
   *
   * <p>There are three advantages to this hybrid approach over the more obvious approach of solely
   * using a {@link LegacyGlobber}:
   *
   * <ul>
   *   <li>We trivially have the proper Skyframe {@link GlobValue} deps, whereas we would need to
   *       request them after-the-fact if we solely used a {@link LegacyGlobber}.
   *   <li>We don't need to re-evaluate globs whose expression hasn't changed (e.g. in the common
   *       case of a BUILD file edit that doesn't change a glob expression), whereas legacy package
   *       loading with a {@link LegacyGlobber} would naively re-evaluate globs when re-evaluating
   *       the BUILD file.
   *   <li>We don't need to re-evaluate invalidated globs *twice* (the single re-evaluation via our
   *       GlobValue deps is sufficient and optimal). See above for why the second evaluation would
   *       happen.
   * </ul>
   */
  private static class SkyframeHybridGlobber implements GlobberWithSkyframeGlobDeps {
    private final PackageIdentifier packageId;
    private final Root packageRoot;
    private final Environment env;
    private final LegacyGlobber legacyGlobber;
    private final Set<SkyKey> globDepsRequested = Sets.newConcurrentHashSet();

    private SkyframeHybridGlobber(
        PackageIdentifier packageId,
        Root packageRoot,
        Environment env,
        LegacyGlobber legacyGlobber) {
      this.packageId = packageId;
      this.packageRoot = packageRoot;
      this.env = env;
      this.legacyGlobber = legacyGlobber;
    }

    @Override
    public Set<SkyKey> getGlobDepsRequested() {
      return ImmutableSet.copyOf(globDepsRequested);
    }

    private SkyKey getGlobKey(String pattern, boolean excludeDirs) throws BadGlobException {
      try {
        return GlobValue.key(
            packageId, packageRoot, pattern, excludeDirs, PathFragment.EMPTY_FRAGMENT);
      } catch (InvalidGlobPatternException e) {
        throw new BadGlobException(e.getMessage());
      }
    }

    @Override
    public Token runAsync(
        List<String> includes, List<String> excludes, boolean excludeDirs, boolean allowEmpty)
        throws BadGlobException, InterruptedException {
      LinkedHashSet<SkyKey> globKeys = Sets.newLinkedHashSetWithExpectedSize(includes.size());
      Map<SkyKey, String> globKeyToPatternMap = Maps.newHashMapWithExpectedSize(includes.size());

      for (String pattern : includes) {
        SkyKey globKey = getGlobKey(pattern, excludeDirs);
        globKeys.add(globKey);
        globKeyToPatternMap.put(globKey, pattern);
      }

      globDepsRequested.addAll(globKeys);

      Map<SkyKey, ValueOrException2<IOException, BuildFileNotFoundException>> globValueMap =
          env.getValuesOrThrow(globKeys, IOException.class, BuildFileNotFoundException.class);

      // For each missing glob, evaluate it asynchronously via the delegate.
      Collection<SkyKey> missingKeys = getMissingKeys(globKeys, globValueMap);
      List<String> globsToDelegate = new ArrayList<>(missingKeys.size());
      for (SkyKey missingKey : missingKeys) {
        String missingPattern = globKeyToPatternMap.get(missingKey);
        if (missingPattern != null) {
          globsToDelegate.add(missingPattern);
          globKeys.remove(missingKey);
        }
      }
      Token legacyIncludesToken =
          globsToDelegate.isEmpty()
              ? null
              : legacyGlobber.runAsync(
                  globsToDelegate, ImmutableList.of(), excludeDirs, allowEmpty);
      return new HybridToken(globValueMap, globKeys, legacyIncludesToken, excludes, allowEmpty);
    }

    private static Collection<SkyKey> getMissingKeys(
        Collection<SkyKey> globKeys,
        Map<SkyKey, ValueOrException2<IOException, BuildFileNotFoundException>> globValueMap) {
      List<SkyKey> missingKeys = new ArrayList<>(globKeys.size());
      for (SkyKey globKey : globKeys) {
        ValueOrException2<IOException, BuildFileNotFoundException> valueOrException =
            globValueMap.get(globKey);
        if (valueOrException == null) {
          missingKeys.add(globKey);
        }
        try {
          if (valueOrException.get() == null) {
            missingKeys.add(globKey);
          }
        } catch (IOException | BuildFileNotFoundException doesntMatter) {
          continue;
        }
      }
      return missingKeys;
    }

    @Override
    public List<String> fetchUnsorted(Token token)
        throws BadGlobException, IOException, InterruptedException {
      HybridToken hybridToken = (HybridToken) token;
      return hybridToken.resolve(legacyGlobber);
    }

    @Override
    public void onInterrupt() {
      legacyGlobber.onInterrupt();
    }

    @Override
    public void onCompletion() {
      legacyGlobber.onCompletion();
    }

    /**
     * A {@link Globber.Token} that encapsulates the result of a single {@link Globber#runAsync}
     * call via the fetching of some globs from skyframe, and some other globs via a{@link
     * LegacyGlobber}. 'exclude' patterns are evaluated using {@link UnixGlob#removeExcludes} after
     * merging legacy and skyframe glob results in {@link #resolve}.
     */
    private static class HybridToken extends Globber.Token {
      // The result of the Skyframe lookup for all the needed glob patterns.
      private final Map<SkyKey, ValueOrException2<IOException, BuildFileNotFoundException>>
          globValueMap;
      // The skyframe keys corresponding to the 'includes' patterns fetched from Skyframe
      // (this is includes_sky above).
      private final Iterable<SkyKey> includesGlobKeys;
      // A token for computing legacy globs.
      @Nullable private final Token legacyIncludesToken;

      private final List<String> excludes;

      private final boolean allowEmpty;

      private HybridToken(
          Map<SkyKey, ValueOrException2<IOException, BuildFileNotFoundException>> globValueMap,
          Iterable<SkyKey> includesGlobKeys,
          @Nullable Token delegateIncludesToken,
          List<String> excludes,
          boolean allowEmpty) {
        this.globValueMap = globValueMap;
        this.includesGlobKeys = includesGlobKeys;
        this.legacyIncludesToken = delegateIncludesToken;
        this.excludes = excludes;
        this.allowEmpty = allowEmpty;
      }

      private List<String> resolve(Globber delegate)
          throws BadGlobException, IOException, InterruptedException {
        HashSet<String> matches = new HashSet<>();
        for (SkyKey includeGlobKey : includesGlobKeys) {
          // TODO(bazel-team): NestedSet expansion here is suboptimal.
          boolean foundMatch = false;
          for (PathFragment match : getGlobMatches(includeGlobKey, globValueMap).toList()) {
            matches.add(match.getPathString());
            foundMatch = true;
          }
          if (!allowEmpty && !foundMatch) {
            throw new BadGlobException(
                "glob pattern '"
                    + ((GlobDescriptor) includeGlobKey.argument()).getPattern()
                    + "' didn't match anything, but allow_empty is set to False "
                    + "(the default value of allow_empty can be set with "
                    + "--incompatible_disallow_empty_glob).");
          }
        }
        if (legacyIncludesToken != null) {
          matches.addAll(delegate.fetchUnsorted(legacyIncludesToken));
        }
        try {
          UnixGlob.removeExcludes(matches, excludes);
        } catch (UnixGlob.BadPattern ex) {
          throw new BadGlobException(ex.getMessage());
        }
        List<String> result = new ArrayList<>(matches);

        if (!allowEmpty && result.isEmpty()) {
          throw new BadGlobException(
              "all files in the glob have been excluded, but allow_empty is set to False "
                  + "(the default value of allow_empty can be set with "
                  + "--incompatible_disallow_empty_glob).");
        }
        return result;
      }

      private static NestedSet<PathFragment> getGlobMatches(
          SkyKey globKey,
          Map<SkyKey, ValueOrException2<IOException, BuildFileNotFoundException>> globValueMap)
          throws SkyframeGlobbingIOException {
        ValueOrException2<IOException, BuildFileNotFoundException> valueOrException =
            checkNotNull(globValueMap.get(globKey), "%s should not be missing", globKey);
        try {
          return checkNotNull(
                  (GlobValue) valueOrException.get(), "%s should not be missing", globKey)
              .getMatches();
        } catch (BuildFileNotFoundException | IOException e) {
          // Legacy package loading is only able to handle an IOException, so a rethrow here is the
          // best we can do.
          throw new SkyframeGlobbingIOException(e);
        }
      }
    }
  }

  private static class SkyframeGlobbingIOException extends IOException {
    private SkyframeGlobbingIOException(Exception cause) {
      super(cause);
    }
  }

  private GlobberWithSkyframeGlobDeps makeGlobber(
      Path buildFilePath,
      PackageIdentifier packageId,
      ImmutableSet<PathFragment> repositoryIgnoredPatterns,
      Root packageRoot,
      SkyFunction.Environment env) {
    LegacyGlobber legacyGlobber =
        packageFactory.createLegacyGlobber(
            buildFilePath.getParentDirectory(),
            packageId,
            repositoryIgnoredPatterns,
            packageLocator);
    switch (incrementalityIntent) {
      case INCREMENTAL:
        return new SkyframeHybridGlobber(packageId, packageRoot, env, legacyGlobber);
      case NON_INCREMENTAL:
        // Skyframe globbing is only useful for incremental correctness and performance. The
        // first time Bazel loads a package ever, Skyframe globbing is actually pure overhead
        // (SkyframeHybridGlobber will make full use of LegacyGlobber).
        return new LegacyGlobberWithNoGlobDeps(legacyGlobber);
      default:
        throw new IllegalStateException(incrementalityIntent.toString());
    }
  }

  /**
   * Constructs a {@link Package} object for the given package. Note that the returned package may
   * be in error.
   *
   * <p>May return null if the computation has to be restarted.
   *
   * <p>Exactly one of {@code replacementContents} and {@code buildFileValue} will be non-{@code
   * null}. The former indicates that we have a faux BUILD file with the given contents and the
   * latter indicates that we have a legitimate BUILD file and should actually read its contents.
   */
  @Nullable
  private LoadedPackageCacheEntry loadPackage(
      String workspaceName,
      ImmutableMap<RepositoryName, RepositoryName> repositoryMapping,
      ImmutableSet<PathFragment> repositoryIgnoredPatterns,
      PackageIdentifier packageId,
      RootedPath buildFilePath,
      @Nullable FileValue buildFileValue,
      RuleVisibility defaultVisibility,
      StarlarkSemantics starlarkSemantics,
      @Nullable Label preludeLabel,
      Root packageRoot,
      Environment env)
      throws InterruptedException, PackageFunctionException {
    if (packageProgress != null) {
      packageProgress.startReadPackage(packageId);
    }
    try (SilentCloseable c =
        Profiler.instance().profile(ProfilerTask.CREATE_PACKAGE, packageId.toString())) {
      StarlarkFile file = fileSyntaxCache.getIfPresent(packageId);
      Path inputFile = buildFilePath.asPath();
      if (file == null) {
        if (showLoadingProgress.get()) {
          env.getListener().handle(Event.progress("Loading package: " + packageId));
        }
        ParserInput input;
        checkNotNull(buildFileValue, packageId);
        byte[] buildFileBytes = null;
        try {
          buildFileBytes =
              buildFileValue.isSpecialFile()
                  ? FileSystemUtils.readContent(inputFile)
                  : FileSystemUtils.readWithKnownFileSize(inputFile, buildFileValue.getSize());
        } catch (IOException e) {
          buildFileBytes =
              actionOnIOExceptionReadingBuildFile.maybeGetBuildFileContentsToUse(
                  inputFile.asFragment(), e);
          if (buildFileBytes == null) {
            // Note that we did the work that led to this IOException, so we should
            // conservatively report this error as transient.
            throw PackageFunctionException.builder()
                .setType(PackageFunctionException.Type.BUILD_FILE_CONTAINS_ERRORS)
                .setTransience(Transience.TRANSIENT)
                .setPackageIdentifier(packageId)
                .setMessage(e.getMessage())
                .setException(e)
                .setPackageLoadingCode(PackageLoading.Code.BUILD_FILE_MISSING)
                .build();
          }
          // If control flow reaches here, we're in territory that is deliberately unsound.
          // See the javadoc for ActionOnIOExceptionReadingBuildFile.
        }
        input = ParserInput.fromLatin1(buildFileBytes, inputFile.toString());

        // Options for processing BUILD files.
        FileOptions options =
            FileOptions.builder()
                // TODO(adonovan): remove recordScope opt-out. But first we need to fix the
                // preexisting
                // problem that fileSyntaxCache shares syntax trees which createPackageFromAst
                // mutates.
                .recordScope(false) // don't mutate BUILD syntax
                .requireLoadStatementsFirst(false)
                .allowToplevelRebinding(true)
                .restrictStringEscapes(
                    starlarkSemantics.getBool(
                        BuildLanguageOptions.INCOMPATIBLE_RESTRICT_STRING_ESCAPES))
                .build();
        file = StarlarkFile.parse(input, options);
        fileSyntaxCache.put(packageId, file);
      }
      BzlLoadResult bzlLoadResult = null;
      try {
        bzlLoadResult =
            fetchLoadsFromBuildFile(
                buildFilePath,
                packageId,
                repositoryMapping,
                file,
                preludeLabel,
                /*workspaceChunk=*/ -1,
                env,
                bzlLoadFunctionForInlining);
      } catch (NoSuchPackageException e) {
        throw new PackageFunctionException(e, Transience.PERSISTENT);
      } catch (InterruptedException e) {
        fileSyntaxCache.invalidate(packageId);
        throw e;
      }
      if (bzlLoadResult == null) {
        return null;
      }
      // From here on, either of the following must happen:
      // 1. An InterruptedException or PackageFunctionException gets thrown in the code below
      // before completion of this method.
      // 2. The packageCacheEnty is successfully created from the AST and put into
      // packageFunctionCache, so future Skyframe restarts don't need to parse the AST again.
      //
      // Therefore, it is safe to invalidate the astCache entry for this packageId here.
      fileSyntaxCache.invalidate(packageId);
      GlobberWithSkyframeGlobDeps globberWithSkyframeGlobDeps =
          makeGlobber(inputFile, packageId, repositoryIgnoredPatterns, packageRoot, env);
      long startTimeNanos = BlazeClock.nanoTime();
      Package.Builder pkgBuilder =
          packageFactory.createPackageFromAst(
              workspaceName,
              repositoryMapping,
              packageId,
              buildFilePath,
              file, // becomes resolved as a side effect
              bzlLoadResult.preludeModule,
              bzlLoadResult.loadedModules,
              defaultVisibility,
              starlarkSemantics,
              globberWithSkyframeGlobDeps);
      long loadTimeNanos = Math.max(BlazeClock.nanoTime() - startTimeNanos, 0L);
      LoadedPackageCacheEntry packageCacheEntry =
          new LoadedPackageCacheEntry(
              pkgBuilder, globberWithSkyframeGlobDeps.getGlobDepsRequested(), loadTimeNanos);
      numPackagesLoaded.incrementAndGet();
      if (packageProgress != null) {
        packageProgress.doneReadPackage(packageId);
      }
      return packageCacheEntry;
    } catch (InterruptedException | PackageFunctionException e) {
      if (packageProgress != null) {
        packageProgress.doneReadPackage(packageId);
      }
      throw e;
    }
  }

  private static class InternalInconsistentFilesystemException extends Exception {
    private boolean isTransient;
    private final PackageIdentifier packageIdentifier;

    /**
     * Used to represent a filesystem inconsistency discovered outside the {@link PackageFunction}.
     */
    public InternalInconsistentFilesystemException(
        PackageIdentifier packageIdentifier, InconsistentFilesystemException e) {
      super(e.getMessage(), e);
      this.packageIdentifier = packageIdentifier;
      // This is not a transient error from the perspective of the PackageFunction.
      this.isTransient = false;
    }

    /** Used to represent a filesystem inconsistency discovered by the {@link PackageFunction}. */
    public InternalInconsistentFilesystemException(
        PackageIdentifier packageIdentifier, String inconsistencyMessage) {
      this(packageIdentifier, new InconsistentFilesystemException(inconsistencyMessage));
      this.isTransient = true;
    }

    public boolean isTransient() {
      return isTransient;
    }

    private NoSuchPackageException toNoSuchPackageException(
        PackageLoading.Code packageLoadingCode) {
      return PackageFunctionException.builder()
          .setType(PackageFunctionException.Type.NO_SUCH_PACKAGE)
          .setPackageIdentifier(packageIdentifier)
          .setMessage(this.getMessage())
          .setException((Exception) this.getCause())
          .setPackageLoadingCode(packageLoadingCode)
          .buildCause();
    }
  }

  /**
   * Used to declare all the exception types that can be wrapped in the exception thrown by {@link
   * PackageFunction#compute}.
   */
  static class PackageFunctionException extends SkyFunctionException {
    public PackageFunctionException(NoSuchPackageException e, Transience transience) {
      super(e, transience);
    }

    static Builder builder() {
      return new Builder();
    }

    /**
     * An enum to help create the different types of {@link NoSuchPackageException}. PackageFunction
     * contains a myriad of different types of exceptions that extend NoSuchPackageException for
     * different scenarios.
     */
    static enum Type {
      BUILD_FILE_CONTAINS_ERRORS {
        @Override
        BuildFileContainsErrorsException create(
            PackageIdentifier packId, String msg, DetailedExitCode detailedExitCode, Exception e) {
          return e instanceof IOException
              ? new BuildFileContainsErrorsException(packId, msg, (IOException) e, detailedExitCode)
              : new BuildFileContainsErrorsException(packId, msg, detailedExitCode);
        }
      },
      BUILD_FILE_NOT_FOUND {
        @Override
        BuildFileNotFoundException create(
            PackageIdentifier packId, String msg, DetailedExitCode detailedExitCode, Exception e) {
          return new BuildFileNotFoundException(packId, msg, detailedExitCode);
        }
      },
      INVALID_PACKAGE_NAME {
        @Override
        InvalidPackageNameException create(
            PackageIdentifier packId, String msg, DetailedExitCode detailedExitCode, Exception e) {
          return new InvalidPackageNameException(packId, msg, detailedExitCode);
        }
      },
      NO_SUCH_PACKAGE {
        @Override
        NoSuchPackageException create(
            PackageIdentifier packId, String msg, DetailedExitCode detailedExitCode, Exception e) {
          return e != null
              ? new NoSuchPackageException(packId, msg, e, detailedExitCode)
              : new NoSuchPackageException(packId, msg, detailedExitCode);
        }
      };

      abstract NoSuchPackageException create(
          PackageIdentifier packId, String msg, DetailedExitCode detailedExitCode, Exception e);
    }

    /**
     * The builder class for {@link PackageFunctionException} and its {@link NoSuchPackageException}
     * cause.
     */
    static class Builder {
      private Type exceptionType;
      private PackageIdentifier packageIdentifier;
      private Transience transience;
      private Exception exception;
      private String message;
      private PackageLoading.Code packageLoadingCode;

      Builder setType(Type exceptionType) {
        this.exceptionType = exceptionType;
        return this;
      }

      Builder setPackageIdentifier(PackageIdentifier packageIdentifier) {
        this.packageIdentifier = packageIdentifier;
        return this;
      }

      Builder setTransience(Transience transience) {
        this.transience = transience;
        return this;
      }

      Builder setException(Exception exception) {
        this.exception = exception;
        return this;
      }

      Builder setMessage(String message) {
        this.message = message;
        return this;
      }

      Builder setPackageLoadingCode(PackageLoading.Code packageLoadingCode) {
        this.packageLoadingCode = packageLoadingCode;
        return this;
      }

      @Override
      public int hashCode() {
        return Objects.hash(
            exceptionType, packageIdentifier, transience, exception, message, packageLoadingCode);
      }

      @Override
      public boolean equals(Object other) {
        if (this == other) {
          return true;
        }
        if (!(other instanceof PackageFunctionException.Builder)) {
          return false;
        }
        PackageFunctionException.Builder otherBuilder = (PackageFunctionException.Builder) other;
        return Objects.equals(exceptionType, otherBuilder.exceptionType)
            && Objects.equals(packageIdentifier, otherBuilder.packageIdentifier)
            && Objects.equals(transience, otherBuilder.transience)
            && Objects.equals(exception, otherBuilder.exception)
            && Objects.equals(message, otherBuilder.message)
            && Objects.equals(packageLoadingCode, otherBuilder.packageLoadingCode);
      }

      NoSuchPackageException buildCause() {
        checkNotNull(exceptionType, "The NoSuchPackageException type must be set.");
        checkNotNull(packageLoadingCode, "The PackageLoading code must be set.");
        DetailedExitCode detailedExitCode = createDetailedExitCode(message, packageLoadingCode);
        return exceptionType.create(packageIdentifier, message, detailedExitCode, exception);
      }

      PackageFunctionException build() {
        return new PackageFunctionException(
            buildCause(), checkNotNull(transience, "Transience must be set"));
      }

      private static DetailedExitCode createDetailedExitCode(
          String message, PackageLoading.Code packageLoadingCode) {
        return DetailedExitCode.of(
            FailureDetail.newBuilder()
                .setMessage(message)
                .setPackageLoading(PackageLoading.newBuilder().setCode(packageLoadingCode).build())
                .build());
      }
    }
  }
}
