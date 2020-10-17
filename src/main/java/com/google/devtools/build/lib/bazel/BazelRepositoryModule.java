// Copyright 2019 The Bazel Authors. All rights reserved.
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
//

package com.google.devtools.build.lib.bazel;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.devtools.build.lib.analysis.BlazeDirectories;
import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import com.google.devtools.build.lib.analysis.RuleDefinition;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.bazel.commands.FetchCommand;
import com.google.devtools.build.lib.bazel.commands.SyncCommand;
import com.google.devtools.build.lib.bazel.repository.LocalConfigPlatformFunction;
import com.google.devtools.build.lib.bazel.repository.LocalConfigPlatformRule;
import com.google.devtools.build.lib.bazel.repository.RepositoryOptions;
import com.google.devtools.build.lib.bazel.repository.RepositoryOptions.RepositoryOverride;
import com.google.devtools.build.lib.bazel.repository.cache.RepositoryCache;
import com.google.devtools.build.lib.bazel.repository.downloader.DelegatingDownloader;
import com.google.devtools.build.lib.bazel.repository.downloader.DownloadManager;
import com.google.devtools.build.lib.bazel.repository.downloader.HttpDownloader;
import com.google.devtools.build.lib.bazel.repository.downloader.UrlRewriter;
import com.google.devtools.build.lib.bazel.repository.starlark.StarlarkRepositoryFunction;
import com.google.devtools.build.lib.bazel.repository.starlark.StarlarkRepositoryModule;
import com.google.devtools.build.lib.bazel.rules.android.AndroidNdkRepositoryFunction;
import com.google.devtools.build.lib.bazel.rules.android.AndroidNdkRepositoryRule;
import com.google.devtools.build.lib.bazel.rules.android.AndroidSdkRepositoryFunction;
import com.google.devtools.build.lib.bazel.rules.android.AndroidSdkRepositoryRule;
import com.google.devtools.build.lib.cmdline.RepositoryName;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.pkgcache.PackageOptions;
import com.google.devtools.build.lib.rules.repository.LocalRepositoryFunction;
import com.google.devtools.build.lib.rules.repository.LocalRepositoryRule;
import com.google.devtools.build.lib.rules.repository.ManagedDirectoriesKnowledgeImpl;
import com.google.devtools.build.lib.rules.repository.ManagedDirectoriesKnowledgeImpl.ManagedDirectoriesListener;
import com.google.devtools.build.lib.rules.repository.NewLocalRepositoryFunction;
import com.google.devtools.build.lib.rules.repository.NewLocalRepositoryRule;
import com.google.devtools.build.lib.rules.repository.RepositoryDelegatorFunction;
import com.google.devtools.build.lib.rules.repository.RepositoryDirectoryDirtinessChecker;
import com.google.devtools.build.lib.rules.repository.RepositoryFunction;
import com.google.devtools.build.lib.rules.repository.RepositoryLoaderFunction;
import com.google.devtools.build.lib.runtime.BlazeModule;
import com.google.devtools.build.lib.runtime.BlazeRuntime;
import com.google.devtools.build.lib.runtime.Command;
import com.google.devtools.build.lib.runtime.CommandEnvironment;
import com.google.devtools.build.lib.runtime.InfoItem;
import com.google.devtools.build.lib.runtime.ProcessWrapper;
import com.google.devtools.build.lib.runtime.RepositoryRemoteExecutor;
import com.google.devtools.build.lib.runtime.RepositoryRemoteExecutorFactory;
import com.google.devtools.build.lib.runtime.ServerBuilder;
import com.google.devtools.build.lib.runtime.WorkspaceBuilder;
import com.google.devtools.build.lib.server.FailureDetails.ExternalRepository;
import com.google.devtools.build.lib.server.FailureDetails.ExternalRepository.Code;
import com.google.devtools.build.lib.server.FailureDetails.FailureDetail;
import com.google.devtools.build.lib.skyframe.BazelSkyframeExecutorConstants;
import com.google.devtools.build.lib.skyframe.MutableSupplier;
import com.google.devtools.build.lib.skyframe.PrecomputedValue;
import com.google.devtools.build.lib.skyframe.PrecomputedValue.Injected;
import com.google.devtools.build.lib.skyframe.SkyFunctions;
import com.google.devtools.build.lib.starlarkbuildapi.repository.RepositoryBootstrap;
import com.google.devtools.build.lib.util.AbruptExitException;
import com.google.devtools.build.lib.util.DetailedExitCode;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParsingResult;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/** Adds support for fetching external code. */
public class BazelRepositoryModule extends BlazeModule {
  // Default location (relative to output user root) of the repository cache.
  public static final String DEFAULT_CACHE_LOCATION = "cache/repos/v1";

  // A map of repository handlers that can be looked up by rule class name.
  private final ImmutableMap<String, RepositoryFunction> repositoryHandlers;
  private final AtomicBoolean isFetch = new AtomicBoolean(false);
  private final StarlarkRepositoryFunction starlarkRepositoryFunction;
  private final RepositoryCache repositoryCache = new RepositoryCache();
  private final HttpDownloader httpDownloader = new HttpDownloader();
  private final DelegatingDownloader delegatingDownloader =
      new DelegatingDownloader(httpDownloader);
  private final DownloadManager downloadManager =
      new DownloadManager(repositoryCache, delegatingDownloader);
  private final MutableSupplier<Map<String, String>> clientEnvironmentSupplier =
      new MutableSupplier<>();
  private ImmutableMap<RepositoryName, PathFragment> overrides = ImmutableMap.of();
  private Optional<RootedPath> resolvedFile = Optional.empty();
  private Optional<RootedPath> resolvedFileReplacingWorkspace = Optional.empty();
  private Set<String> outputVerificationRules = ImmutableSet.of();
  private FileSystem filesystem;
  // We hold the precomputed value of the managed directories here, so that the dependency
  // on WorkspaceFileValue is not registered for each FileStateValue.
  private final ManagedDirectoriesKnowledgeImpl managedDirectoriesKnowledge;

  public BazelRepositoryModule() {
    this.starlarkRepositoryFunction = new StarlarkRepositoryFunction(downloadManager);
    this.repositoryHandlers = repositoryRules();
    ManagedDirectoriesListener listener =
        repositoryNamesWithManagedDirs -> {
          Set<String> conflicting =
              overrides.keySet().stream()
                  .filter(repositoryNamesWithManagedDirs::contains)
                  .map(RepositoryName::getName)
                  .collect(Collectors.toSet());
          if (!conflicting.isEmpty()) {
            String message =
                "Overriding repositories is not allowed"
                    + " for the repositories with managed directories.\n"
                    + "The following overridden external repositories have managed directories: "
                    + String.join(", ", conflicting.toArray(new String[0]));
            throw new AbruptExitException(
                detailedExitCode(message, Code.OVERRIDE_DISALLOWED_MANAGED_DIRECTORIES));
          }
        };
    managedDirectoriesKnowledge = new ManagedDirectoriesKnowledgeImpl(listener);
  }

  private static DetailedExitCode detailedExitCode(String message, ExternalRepository.Code code) {
    return DetailedExitCode.of(
        FailureDetail.newBuilder()
            .setMessage(message)
            .setExternalRepository(ExternalRepository.newBuilder().setCode(code))
            .build());
  }

  public static ImmutableMap<String, RepositoryFunction> repositoryRules() {
    return ImmutableMap.<String, RepositoryFunction>builder()
        .put(LocalRepositoryRule.NAME, new LocalRepositoryFunction())
        .put(NewLocalRepositoryRule.NAME, new NewLocalRepositoryFunction())
        .put(AndroidSdkRepositoryRule.NAME, new AndroidSdkRepositoryFunction())
        .put(AndroidNdkRepositoryRule.NAME, new AndroidNdkRepositoryFunction())
        .put(LocalConfigPlatformRule.NAME, new LocalConfigPlatformFunction())
        .build();
  }

  private static class RepositoryCacheInfoItem extends InfoItem {
    private final RepositoryCache repositoryCache;

    RepositoryCacheInfoItem(RepositoryCache repositoryCache) {
      super("repository_cache", "The location of the repository download cache used");
      this.repositoryCache = repositoryCache;
    }

    @Override
    public byte[] get(Supplier<BuildConfiguration> configurationSupplier, CommandEnvironment env)
        throws AbruptExitException, InterruptedException {
      return print(repositoryCache.getRootPath());
    }
  }

  @Override
  public void serverInit(OptionsParsingResult startupOptions, ServerBuilder builder) {
    builder.addCommands(new FetchCommand());
    builder.addCommands(new SyncCommand());
    builder.addInfoItems(new RepositoryCacheInfoItem(repositoryCache));
  }

  @Override
  public void workspaceInit(
      BlazeRuntime runtime, BlazeDirectories directories, WorkspaceBuilder builder) {
    builder.setManagedDirectoriesKnowledge(managedDirectoriesKnowledge);

    RepositoryDirectoryDirtinessChecker customDirtinessChecker =
        new RepositoryDirectoryDirtinessChecker(
            directories.getWorkspace(), managedDirectoriesKnowledge);
    builder.addCustomDirtinessChecker(customDirtinessChecker);
    // Create the repository function everything flows through.
    builder.addSkyFunction(SkyFunctions.REPOSITORY, new RepositoryLoaderFunction());
    RepositoryDelegatorFunction repositoryDelegatorFunction =
        new RepositoryDelegatorFunction(
            repositoryHandlers,
            starlarkRepositoryFunction,
            isFetch,
            clientEnvironmentSupplier,
            directories,
            managedDirectoriesKnowledge,
            BazelSkyframeExecutorConstants.EXTERNAL_PACKAGE_HELPER);
    builder.addSkyFunction(SkyFunctions.REPOSITORY_DIRECTORY, repositoryDelegatorFunction);
    filesystem = runtime.getFileSystem();
  }

  @Override
  public void initializeRuleClasses(ConfiguredRuleClassProvider.Builder builder) {
    for (Map.Entry<String, RepositoryFunction> handler : repositoryHandlers.entrySet()) {
      RuleDefinition ruleDefinition;
      try {
        ruleDefinition =
            handler.getValue().getRuleDefinition().getDeclaredConstructor().newInstance();
      } catch (IllegalAccessException
          | InstantiationException
          | NoSuchMethodException
          | InvocationTargetException e) {
        throw new IllegalStateException(e);
      }
      builder.addRuleDefinition(ruleDefinition);
    }
    builder.addStarlarkBootstrap(new RepositoryBootstrap(new StarlarkRepositoryModule()));
  }

  @Override
  public void beforeCommand(CommandEnvironment env) {
    clientEnvironmentSupplier.set(env.getRepoEnv());
    PackageOptions pkgOptions = env.getOptions().getOptions(PackageOptions.class);
    isFetch.set(pkgOptions != null && pkgOptions.fetch);
    resolvedFile = Optional.empty();
    resolvedFileReplacingWorkspace = Optional.empty();
    outputVerificationRules = ImmutableSet.of();

    starlarkRepositoryFunction.setProcessWrapper(ProcessWrapper.fromCommandEnvironment(env));

    RepositoryOptions repoOptions = env.getOptions().getOptions(RepositoryOptions.class);
    if (repoOptions != null) {
      repositoryCache.setHardlink(repoOptions.useHardlinks);
      if (repoOptions.experimentalScaleTimeouts > 0.0) {
        starlarkRepositoryFunction.setTimeoutScaling(repoOptions.experimentalScaleTimeouts);
      } else {
        env.getReporter()
            .handle(
                Event.warn(
                    "Ignoring request to scale timeouts for repositories by a non-positive"
                        + " factor"));
        starlarkRepositoryFunction.setTimeoutScaling(1.0);
      }
      if (repoOptions.experimentalRepositoryCache != null) {
        Path repositoryCachePath;
        if (repoOptions.experimentalRepositoryCache.isEmpty()) {
          // A set but empty path indicates a request to disable the repository cache.
          repositoryCachePath = null;
        } else if (repoOptions.experimentalRepositoryCache.isAbsolute()) {
          repositoryCachePath = filesystem.getPath(repoOptions.experimentalRepositoryCache);
        } else {
          repositoryCachePath =
              env.getBlazeWorkspace()
                  .getWorkspace()
                  .getRelative(repoOptions.experimentalRepositoryCache);
        }
        repositoryCache.setRepositoryCachePath(repositoryCachePath);
      } else {
        Path repositoryCachePath =
            env.getDirectories()
                .getServerDirectories()
                .getOutputUserRoot()
                .getRelative(DEFAULT_CACHE_LOCATION);
        try {
          FileSystemUtils.createDirectoryAndParents(repositoryCachePath);
          repositoryCache.setRepositoryCachePath(repositoryCachePath);
        } catch (IOException e) {
          env.getReporter()
              .handle(
                  Event.warn(
                      "Failed to set up cache at "
                          + repositoryCachePath.toString()
                          + ": "
                          + e.getMessage()));
        }
      }

      UrlRewriter rewriter =
          UrlRewriter.getDownloaderUrlRewriter(
              repoOptions == null ? null : repoOptions.downloaderConfig, env.getReporter());
      downloadManager.setUrlRewriter(rewriter);

      if (repoOptions.experimentalDistdir != null) {
        downloadManager.setDistdir(
            repoOptions.experimentalDistdir.stream()
                .map(
                    path ->
                        path.isAbsolute()
                            ? filesystem.getPath(path)
                            : env.getBlazeWorkspace().getWorkspace().getRelative(path))
                .collect(Collectors.toList()));
      } else {
        downloadManager.setDistdir(ImmutableList.<Path>of());
      }

      if (repoOptions.httpTimeoutScaling > 0) {
        httpDownloader.setTimeoutScaling((float) repoOptions.httpTimeoutScaling);
      } else {
        env.getReporter()
            .handle(Event.warn("Ignoring request to scale http timeouts by a non-positive factor"));
        httpDownloader.setTimeoutScaling(1.0f);
      }

      if (repoOptions.repositoryOverrides != null) {
        // To get the usual latest-wins semantics, we need a mutable map, as the builder
        // of an immutable map does not allow redefining the values of existing keys.
        // We use a LinkedHashMap to preserve the iteration order.
        Map<RepositoryName, PathFragment> overrideMap = new LinkedHashMap<>();
        for (RepositoryOverride override : repoOptions.repositoryOverrides) {
          overrideMap.put(override.repositoryName(), override.path());
        }
        ImmutableMap<RepositoryName, PathFragment> newOverrides = ImmutableMap.copyOf(overrideMap);
        if (!Maps.difference(overrides, newOverrides).areEqual()) {
          overrides = newOverrides;
        }
      } else {
        overrides = ImmutableMap.of();
      }

      if (!Strings.isNullOrEmpty(repoOptions.repositoryHashFile)) {
        Path hashFile;
        if (env.getWorkspace() != null) {
          hashFile = env.getWorkspace().getRelative(repoOptions.repositoryHashFile);
        } else {
          hashFile = filesystem.getPath(repoOptions.repositoryHashFile);
        }
        resolvedFile =
            Optional.of(RootedPath.toRootedPath(Root.absoluteRoot(filesystem), hashFile));
      }

      if (!Strings.isNullOrEmpty(repoOptions.experimentalResolvedFileInsteadOfWorkspace)) {
        Path resolvedFile;
        if (env.getWorkspace() != null) {
          resolvedFile =
              env.getWorkspace()
                  .getRelative(repoOptions.experimentalResolvedFileInsteadOfWorkspace);
        } else {
          resolvedFile = filesystem.getPath(repoOptions.experimentalResolvedFileInsteadOfWorkspace);
        }
        resolvedFileReplacingWorkspace =
            Optional.of(RootedPath.toRootedPath(Root.absoluteRoot(filesystem), resolvedFile));
      }

      if (repoOptions.experimentalVerifyRepositoryRules != null) {
        outputVerificationRules =
            ImmutableSet.copyOf(repoOptions.experimentalVerifyRepositoryRules);
      }

      RepositoryRemoteExecutorFactory remoteExecutorFactory =
          env.getRuntime().getRepositoryRemoteExecutorFactory();
      RepositoryRemoteExecutor remoteExecutor = null;
      if (remoteExecutorFactory != null) {
        remoteExecutor = remoteExecutorFactory.create();
      }
      starlarkRepositoryFunction.setRepositoryRemoteExecutor(remoteExecutor);
      delegatingDownloader.setDelegate(env.getRuntime().getDownloaderSupplier().get());
    }
  }

  @Override
  public ImmutableList<Injected> getPrecomputedValues() {
    return ImmutableList.of(
        PrecomputedValue.injected(RepositoryDelegatorFunction.REPOSITORY_OVERRIDES, overrides),
        PrecomputedValue.injected(
            RepositoryDelegatorFunction.RESOLVED_FILE_FOR_VERIFICATION, resolvedFile),
        PrecomputedValue.injected(
            RepositoryDelegatorFunction.OUTPUT_VERIFICATION_REPOSITORY_RULES,
            outputVerificationRules),
        PrecomputedValue.injected(
            RepositoryDelegatorFunction.RESOLVED_FILE_INSTEAD_OF_WORKSPACE,
            resolvedFileReplacingWorkspace),
        // That key will be reinjected by the sync command with a universally unique identifier.
        // Nevertheless, we need to provide a default value for other commands.
        PrecomputedValue.injected(
            RepositoryDelegatorFunction.DEPENDENCY_FOR_UNCONDITIONAL_FETCHING,
            RepositoryDelegatorFunction.DONT_FETCH_UNCONDITIONALLY),
        PrecomputedValue.injected(
            RepositoryDelegatorFunction.DEPENDENCY_FOR_UNCONDITIONAL_CONFIGURING,
            RepositoryDelegatorFunction.DONT_FETCH_UNCONDITIONALLY));
  }

  @Override
  public Iterable<Class<? extends OptionsBase>> getCommandOptions(Command command) {
    return ImmutableList.<Class<? extends OptionsBase>>of(RepositoryOptions.class);
  }
}
