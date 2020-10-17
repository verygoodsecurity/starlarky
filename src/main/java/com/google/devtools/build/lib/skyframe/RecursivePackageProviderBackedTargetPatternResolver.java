// Copyright 2015 The Bazel Authors. All rights reserved.
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

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.cmdline.RepositoryName;
import com.google.devtools.build.lib.cmdline.ResolvedTargets;
import com.google.devtools.build.lib.cmdline.TargetParsingException;
import com.google.devtools.build.lib.cmdline.TargetPatternResolver;
import com.google.devtools.build.lib.concurrent.BatchCallback;
import com.google.devtools.build.lib.concurrent.MultisetSemaphore;
import com.google.devtools.build.lib.concurrent.ParallelVisitor.UnusedException;
import com.google.devtools.build.lib.concurrent.ThreadSafety.ThreadCompatible;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.ExtendedEventHandler;
import com.google.devtools.build.lib.packages.NoSuchPackageException;
import com.google.devtools.build.lib.packages.NoSuchThingException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.Target;
import com.google.devtools.build.lib.pkgcache.FilteringPolicies;
import com.google.devtools.build.lib.pkgcache.FilteringPolicy;
import com.google.devtools.build.lib.pkgcache.RecursivePackageProvider;
import com.google.devtools.build.lib.pkgcache.TargetPatternResolverUtil;
import com.google.devtools.build.lib.server.FailureDetails.TargetPatterns;
import com.google.devtools.build.lib.vfs.PathFragment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * A {@link TargetPatternResolver} backed by a {@link RecursivePackageProvider}.
 */
@ThreadCompatible
public class RecursivePackageProviderBackedTargetPatternResolver
    extends TargetPatternResolver<Target> {

  // TODO(janakr): Move this to a more generic place and unify with SkyQueryEnvironment's value?
  static final int MAX_PACKAGES_BULK_GET = 1000;

  protected final FilteringPolicy policy;
  private final RecursivePackageProvider recursivePackageProvider;
  private final ExtendedEventHandler eventHandler;
  private final MultisetSemaphore<PackageIdentifier> packageSemaphore;

  public RecursivePackageProviderBackedTargetPatternResolver(
      RecursivePackageProvider recursivePackageProvider,
      ExtendedEventHandler eventHandler,
      FilteringPolicy policy,
      MultisetSemaphore<PackageIdentifier> packageSemaphore) {
    this.recursivePackageProvider = recursivePackageProvider;
    this.eventHandler = eventHandler;
    this.policy = policy;
    this.packageSemaphore = packageSemaphore;
  }

  @Override
  public void warn(String msg) {
    eventHandler.handle(Event.warn(msg));
  }

  /**
   * Gets a {@link Package} from the {@link RecursivePackageProvider}. May return a {@link Package}
   * that has errors.
   */
  private Package getPackage(PackageIdentifier pkgIdentifier)
      throws NoSuchPackageException, InterruptedException {
    return recursivePackageProvider.getPackage(eventHandler, pkgIdentifier);
  }

  private Map<PackageIdentifier, Package> bulkGetPackages(Iterable<PackageIdentifier> pkgIds)
          throws NoSuchPackageException, InterruptedException {
    return recursivePackageProvider.bulkGetPackages(pkgIds);
  }

  @Override
  public Target getTargetOrNull(Label label) throws InterruptedException {
    try {
      if (!isPackage(label.getPackageIdentifier())) {
        return null;
      }
      return recursivePackageProvider.getTarget(eventHandler, label);
    } catch (NoSuchThingException e) {
      return null;
    }
  }

  @Override
  public ResolvedTargets<Target> getExplicitTarget(Label label)
      throws TargetParsingException, InterruptedException {
    try {
      Target target = recursivePackageProvider.getTarget(eventHandler, label);
      return policy.shouldRetain(target, true)
          ? ResolvedTargets.of(target)
          : ResolvedTargets.empty();
    } catch (NoSuchThingException e) {
      throw new TargetParsingException(e.getMessage(), e, e.getDetailedExitCode());
    }
  }

  @Override
  public Collection<Target> getTargetsInPackage(
      String originalPattern, PackageIdentifier packageIdentifier, boolean rulesOnly)
      throws TargetParsingException, InterruptedException {
    FilteringPolicy actualPolicy = rulesOnly
        ? FilteringPolicies.and(FilteringPolicies.RULES_ONLY, policy)
        : policy;
    try {
      Package pkg = getPackage(packageIdentifier);
      return TargetPatternResolverUtil.resolvePackageTargets(pkg, actualPolicy);
    } catch (NoSuchThingException e) {
      String message = TargetPatternResolverUtil.getParsingErrorMessage(
          e.getMessage(), originalPattern);
      throw new TargetParsingException(message, e, e.getDetailedExitCode());
    }
  }

  private Map<PackageIdentifier, Collection<Target>> bulkGetTargetsInPackage(
      String originalPattern, Iterable<PackageIdentifier> pkgIds, FilteringPolicy policy)
      throws InterruptedException {
    try {
      Map<PackageIdentifier, Package> pkgs = bulkGetPackages(pkgIds);
      if (pkgs.size() != Iterables.size(pkgIds)) {
        throw new IllegalStateException("Bulk package retrieval missing results: "
            + Sets.difference(ImmutableSet.copyOf(pkgIds), pkgs.keySet()));
      }
      ImmutableMap.Builder<PackageIdentifier, Collection<Target>> result = ImmutableMap.builder();
      for (PackageIdentifier pkgId : pkgIds) {
        Package pkg = pkgs.get(pkgId);
        result.put(pkgId,  TargetPatternResolverUtil.resolvePackageTargets(pkg, policy));
      }
      return result.build();
    } catch (NoSuchThingException e) {
      String message = TargetPatternResolverUtil.getParsingErrorMessage(
              e.getMessage(), originalPattern);
      throw new IllegalStateException(
          "Mismatch: Expected given pkgIds to correspond to valid Packages. " + message, e);
    }
  }

  @Override
  public boolean isPackage(PackageIdentifier packageIdentifier) throws InterruptedException {
    return recursivePackageProvider.isPackage(eventHandler, packageIdentifier);
  }

  @Override
  public String getTargetKind(Target target) {
    return target.getTargetKind();
  }

  @Override
  public <E extends Exception> void findTargetsBeneathDirectory(
      final RepositoryName repository,
      final String originalPattern,
      String directory,
      boolean rulesOnly,
      ImmutableSet<PathFragment> blacklistedSubdirectories,
      ImmutableSet<PathFragment> excludedSubdirectories,
      BatchCallback<Target, E> callback,
      Class<E> exceptionClass)
      throws TargetParsingException, E, InterruptedException {
    try {
      findTargetsBeneathDirectoryAsyncImpl(
              repository,
              originalPattern,
              directory,
              rulesOnly,
              blacklistedSubdirectories,
              excludedSubdirectories,
              callback,
              MoreExecutors.newDirectExecutorService())
          .get();
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e.getCause(), TargetParsingException.class, exceptionClass);
      throw new IllegalStateException(e.getCause());
    }
  }

  @Override
  public <E extends Exception> ListenableFuture<Void> findTargetsBeneathDirectoryAsync(
      RepositoryName repository,
      String originalPattern,
      String directory,
      boolean rulesOnly,
      ImmutableSet<PathFragment> blacklistedSubdirectories,
      ImmutableSet<PathFragment> excludedSubdirectories,
      BatchCallback<Target, E> callback,
      Class<E> exceptionClass,
      ListeningExecutorService executor) {
    return findTargetsBeneathDirectoryAsyncImpl(
        repository,
        originalPattern,
        directory,
        rulesOnly,
        blacklistedSubdirectories,
        excludedSubdirectories,
        callback,
        executor);
  }

  private <E extends Exception> ListenableFuture<Void> findTargetsBeneathDirectoryAsyncImpl(
      RepositoryName repository,
      String pattern,
      String directory,
      boolean rulesOnly,
      ImmutableSet<PathFragment> blacklistedSubdirectories,
      ImmutableSet<PathFragment> excludedSubdirectories,
      BatchCallback<Target, E> callback,
      ListeningExecutorService executor) {
    FilteringPolicy actualPolicy =
        rulesOnly ? FilteringPolicies.and(FilteringPolicies.RULES_ONLY, policy) : policy;

    ArrayList<ListenableFuture<Void>> futures = new ArrayList<>();
    BatchCallback<PackageIdentifier, UnusedException> getPackageTargetsCallback =
        (pkgIdBatch) ->
            futures.add(
                executor.submit(
                    new GetTargetsInPackagesTask<>(pkgIdBatch, pattern, actualPolicy, callback)));

    PathFragment pathFragment;
    try (PackageIdentifierBatchingCallback batchingCallback =
        new PackageIdentifierBatchingCallback(getPackageTargetsCallback, MAX_PACKAGES_BULK_GET)) {
      pathFragment = TargetPatternResolverUtil.getPathFragment(directory);
      recursivePackageProvider.streamPackagesUnderDirectory(
          batchingCallback,
          eventHandler,
          repository,
          pathFragment,
          blacklistedSubdirectories,
          excludedSubdirectories);
    } catch (TargetParsingException e) {
      return Futures.immediateFailedFuture(e);
    } catch (InterruptedException e) {
      return Futures.immediateCancelledFuture();
    }

    if (futures.isEmpty()) {
      return Futures.immediateFailedFuture(
          new TargetParsingException(
              "no targets found beneath '" + pathFragment + "'",
              TargetPatterns.Code.TARGETS_MISSING));
    }

    return Futures.whenAllSucceed(futures).call(() -> null, directExecutor());
  }

  /**
   * Task to get all matching targets in the given packages, filter them, and pass them to the
   * target batch callback.
   */
  private class GetTargetsInPackagesTask<E extends Exception> implements Callable<Void> {

    private final Iterable<PackageIdentifier> packageIdentifiers;
    private final String originalPattern;
    private final FilteringPolicy actualPolicy;
    private final BatchCallback<Target, E> callback;

    GetTargetsInPackagesTask(
        Iterable<PackageIdentifier> packageIdentifiers,
        String originalPattern,
        FilteringPolicy actualPolicy,
        BatchCallback<Target, E> callback) {
      this.packageIdentifiers = packageIdentifiers;
      this.originalPattern = originalPattern;
      this.actualPolicy = actualPolicy;
      this.callback = callback;
    }

    @Override
    public Void call() throws Exception {
      ImmutableSet<PackageIdentifier> pkgIdBatchSet = ImmutableSet.copyOf(packageIdentifiers);
      packageSemaphore.acquireAll(pkgIdBatchSet);
      try {
        Iterable<Collection<Target>> resolvedTargets =
            RecursivePackageProviderBackedTargetPatternResolver.this
                .bulkGetTargetsInPackage(originalPattern, packageIdentifiers, actualPolicy)
                .values();
        List<Target> filteredTargets = new ArrayList<>(calculateSize(resolvedTargets));
        for (Collection<Target> targets : resolvedTargets) {
          filteredTargets.addAll(targets);
        }
        // TODO(bazel-core): Invoking the callback while holding onto the package
        // semaphore can lead to deadlocks.
        //
        // Also, if the semaphore has a small count, acquireAll can also lead to problems if we
        // don't batch appropriately. Note: We default to an unbounded semaphore for SkyQuery.
        //
        // TODO(b/168142585): Make this code strictly correct in the situation where the semaphore
        // is bounded.
        callback.process(filteredTargets);
      } finally {
        packageSemaphore.releaseAll(pkgIdBatchSet);
      }
      return null;
    }
  }

  private static <T> int calculateSize(Iterable<Collection<T>> resolvedTargets) {
    int size = 0;
    for (Collection<T> targets : resolvedTargets) {
      size += targets.size();
    }
    return size;
  }
}

