// Copyright 2018 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.includescanning;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.devtools.build.lib.actions.ActionExecutionContext;
import com.google.devtools.build.lib.actions.ActionExecutionMetadata;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.ArtifactFactory;
import com.google.devtools.build.lib.actions.ArtifactRoot;
import com.google.devtools.build.lib.actions.ExecException;
import com.google.devtools.build.lib.actions.MissingDepException;
import com.google.devtools.build.lib.analysis.BlazeDirectories;
import com.google.devtools.build.lib.cmdline.RepositoryName;
import com.google.devtools.build.lib.concurrent.AbstractQueueVisitor;
import com.google.devtools.build.lib.concurrent.ErrorClassifier;
import com.google.devtools.build.lib.concurrent.ThreadSafety;
import com.google.devtools.build.lib.includescanning.IncludeParser.Hints;
import com.google.devtools.build.lib.includescanning.IncludeParser.Inclusion;
import com.google.devtools.build.lib.includescanning.IncludeParser.Inclusion.Kind;
import com.google.devtools.build.lib.rules.cpp.IncludeScanner;
import com.google.devtools.build.lib.vfs.IORuntimeException;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.skyframe.SkyFunction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * C include scanner. Quickly scans C/C++ source files to determine the bounding set of transitively
 * referenced include files.
 *
 * <p>Maintains caches for parses and search-matches for performance.
 *
 * <pre>
 * TODO(bazel-team): (2009) Currently does not evaluate preprocessor symbols, so computed includes
 *                   are ignored.
 * TODO(bazel-team): (2009) Does not handle multiline block comments preceding or around an #include
 * </pre>
 */
public class LegacyIncludeScanner implements IncludeScanner {

  private static final class ArtifactWithInclusionContext {
    private final Artifact artifact;
    private final Kind contextKind;
    private final int contextPathPos;

    private ArtifactWithInclusionContext(Artifact artifact, Kind contextKind, int contextPathPos) {
      this.artifact = artifact;
      this.contextKind = contextKind;
      this.contextPathPos = contextPathPos;
    }

    @Override
    public int hashCode() {
      return contextPathPos + 37 * Objects.hash(contextKind, artifact);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ArtifactWithInclusionContext)) {
        return false;
      }
      ArtifactWithInclusionContext that = (ArtifactWithInclusionContext) obj;
      return this.contextKind == that.contextKind
          && this.contextPathPos == that.contextPathPos
          && this.artifact.equals(that.artifact);
    }
  }

  /**
   * A cache of inclusion lookups, taking care to avoid spurious caching related to generated
   * headers / source files.
   */
  @ThreadSafety.ThreadSafe
  private abstract class InclusionCache {
    /**
     * Locates an included file along the search paths. The result is cacheable.
     *
     * @param inclusion the inclusion to locate
     * @param pathToLegalOutputArtifact generated files which may be reached during scanning
     * @param onlyCheckGenerated if true, only search for generated output files
     * @return a tuple of the found file, the position of the respective include path entry on the
     *     search path (or null if no matching file was found), and whether the scan touched illegal
     *     output files
     */
    protected LocateOnPathResult locateOnPaths(
        InclusionWithContext inclusion,
        Map<PathFragment, Artifact> pathToLegalOutputArtifact,
        boolean onlyCheckGenerated) {
      PathFragment name = inclusion.getInclusion().pathFragment;

      // A framework header must begin with a framework name, followed by a path separator, followed
      // by the rest of the header path.  We do not currently support include_next of framework
      // headers.
      boolean searchFrameworkIncludePaths =
          !frameworkIncludePaths.isEmpty()
              && !inclusion.getInclusion().kind.isNext()
              && !name.containsUplevelReferences()
              && PathFragment.containsSeparator(name.getPathString());

      // For #include_next directives we start searching on the include path where
      // we found the previous inclusion.
      int searchStart = inclusion.getInclusion().kind.isNext() ? inclusion.getContextPathPos() : 0;

      // Search the header on the remaining paths.
      List<PathFragment> paths =
          inclusion.getContextKind() == Kind.QUOTE ? quoteIncludePaths : includePaths;
      int alsoSearchFrameworkAtIndex =
          inclusion.getContextKind() == Kind.QUOTE ? quoteIncludePathsFrameworkIndex : 0;
      alsoSearchFrameworkAtIndex = Math.max(alsoSearchFrameworkAtIndex, searchStart);
      boolean viewedIllegalOutput = false;
      for (int i = searchStart; i < paths.size(); ++i) {
        if (i == alsoSearchFrameworkAtIndex && searchFrameworkIncludePaths) {
          String frameworkName = name.subFragment(0, 1).getPathString() + ".framework";
          PathFragment relHeaderPath = name.subFragment(1);
          LocateOnPathResult result =
              locateOnFrameworkPaths(
                  frameworkName,
                  relHeaderPath,
                  pathToLegalOutputArtifact,
                  onlyCheckGenerated,
                  viewedIllegalOutput);
          if (result.path != null) {
            return result;
          }
          viewedIllegalOutput = viewedIllegalOutput || result.viewedIllegalOutputFile;
        }
        PathFragment fileFragment = paths.get(i).getRelative(name);
        if (fileFragment.containsUplevelReferences()) {
          // TODO(janakr): This branch shouldn't be necessary: we should be able to filter such
          // inclusions out unconditionally.
          // Deal with fragments that escape the execroot. They most likely come right back in.
          Path execRootRelativePath = execRoot.getRelative(fileFragment);
          if (execRootRelativePath.startsWith(execRoot)) {
            // Common case: transform #include "../execroot/foo.h" into #include "foo.h"
            fileFragment = execRootRelativePath.relativeTo(execRoot);
          } else {
            // Ugh: successfully escaped the exec root. It's their funeral.
            fileFragment = execRootRelativePath.asFragment();
          }
          // This can happen when we are processing Windows paths with backslashes on Unix,
          // since we do not do any #ifdef processing.
          // We can safely discard these here.
          if (fileFragment.containsUplevelReferences()) {
            continue;
          }
        }
        if (onlyCheckGenerated && !isRealOutputFile(fileFragment)) {
          continue;
        }
        viewedIllegalOutput =
            viewedIllegalOutput
                || isIllegalOutputFile(fileFragment, pathToLegalOutputArtifact.keySet());
        boolean isOutputDirectory = fileFragment.startsWith(outputPathFragment);
        if (!isFile(fileFragment, name, !isOutputDirectory, pathToLegalOutputArtifact.keySet())) {
          continue;
        }
        Artifact artifact;
        if (isOutputDirectory) {
          // May be a normal output file or an inc_library header.
          artifact = pathToLegalOutputArtifact.get(fileFragment);
          if (artifact == null) {
            // This happens if an included file exists in a cc_inc_library's output directory,
            // but is not an output of the cc_inc_library. This can happen if, for instance, the
            // definition of the cc_inc_library is changed to output different files, but the
            // source file's includes don't change.
            // Often, such an include is conditional, and so failing to find it here will not
            // lead to problems. If this include is actually needed for compilation, then we will
            // emit a somewhat unhelpful error message of a missing file, rather than the more
            // helpful one of an illegal include, but it's hard to emit the illegal include
            // message consistently, and this is a rare occurrence in any case.
            return LocateOnPathResult.createNotFound(viewedIllegalOutput);
          }
        } else if (!fileFragment.isAbsolute()) {
          artifact = artifactFactory.resolveSourceArtifact(fileFragment, RepositoryName.MAIN);
          if (artifact == null) {
            // There was a real file, but we couldn't resolve it, probably because it belonged to
            // a package that wasn't actually loaded this build, so user cannot refer to files in
            // that package.
            continue;
          }
        } else {
          // This file is given with an absolute path. We will error out after transitive scanning
          // of the top-level source is finished unless this corresponds to a built-in include
          // directory, and will ignore this artifact in any case, but track it here so that its
          // includes can be processed.
          artifact = artifactFactory.getSourceArtifact(fileFragment, absoluteRoot);
        }
        // +1 to account for the virtual entry for relative includes.
        return LocateOnPathResult.create(artifact, i + 1, viewedIllegalOutput);
      }

      // Not found.
      return LocateOnPathResult.createNotFound(viewedIllegalOutput);
    }

    /**
     * Locates an included file along the framework search paths. The result is cacheable.
     *
     * @param frameworkName the name of the framework, including the ".framework" suffix
     * @param relHeaderPath the path of the framework header, relative to the framework
     * @param pathToLegalOutputArtifact generated files which may be reached during scanning
     * @param onlyCheckGenerated if true, only search for generated output files
     * @param viewedIllegalOutput whether the scanner has viewed an illegal output file.
     * @return a tuple of the found file, the context path position of the input inclusion, and
     *     whether the scan touched illegal output files
     */
    private LocateOnPathResult locateOnFrameworkPaths(
        String frameworkName,
        PathFragment relHeaderPath,
        Map<PathFragment, Artifact> pathToLegalOutputArtifact,
        boolean onlyCheckGenerated,
        boolean viewedIllegalOutput) {
      Set<PathFragment> outputArtifactPaths = pathToLegalOutputArtifact.keySet();
      for (int i = 0; i < frameworkIncludePaths.size(); ++i) {
        PathFragment includePath = frameworkIncludePaths.get(i);

        // Construct the full framework path path/to/foo.framework.
        PathFragment fullFrameworkPath = includePath.getRelative(frameworkName);

        if (onlyCheckGenerated && !isRealOutputFile(fullFrameworkPath)) {
          return LocateOnPathResult.createNotFound(viewedIllegalOutput);
        }

        // Look for header in path/to/foo.framework/Headers/
        PathFragment foundHeaderPath = null;
        PathFragment fullHeaderPath =
            fullFrameworkPath.getRelative("Headers").getRelative(relHeaderPath);

        viewedIllegalOutput =
            viewedIllegalOutput || isIllegalOutputFile(fullHeaderPath, outputArtifactPaths);
        boolean isOutputDirectory = fullHeaderPath.startsWith(outputPathFragment);
        if (isFile(fullHeaderPath, relHeaderPath, isOutputDirectory, outputArtifactPaths)) {
          foundHeaderPath = fullHeaderPath;
        } else {
          // Look for header in path/to/foo.framework/PrivateHeaders/
          fullHeaderPath =
              fullFrameworkPath.getRelative("PrivateHeaders").getRelative(relHeaderPath);
          viewedIllegalOutput =
              viewedIllegalOutput || isIllegalOutputFile(fullHeaderPath, outputArtifactPaths);
          if (isFile(fullHeaderPath, relHeaderPath, isOutputDirectory, outputArtifactPaths)) {
            foundHeaderPath = fullHeaderPath;
          } else {
            continue;
          }
        }

        Artifact artifact;
        if (isOutputDirectory) {
          artifact = pathToLegalOutputArtifact.get(foundHeaderPath);
          if (artifact == null) {
            // This happens if an included file exists in a framework directory but is not but is
            // not an output of the framework rule.
            // Such an include may be conditional, and so failing to find it here will not lead to
            // problems. If this include is actually needed for compilation, then we will emit a
            // somewhat unhelpful error message of a missing file, rather than the more helpful one
            // of an illegal include, but it's hard to emit the illegal include message
            // consistently, and this is a rare occurrence in any case.

            // Note that the corresponding case for non-framework paths aborts the search here, but
            // for framdwork paths, we keep going like in other cases where we can't find a header
            // we have access to.
            continue;
          }
        } else if (!foundHeaderPath.isAbsolute()) {
          artifact = artifactFactory.resolveSourceArtifact(foundHeaderPath, RepositoryName.MAIN);
          if (artifact == null) {
            // There was a real file, but we couldn't resolve it, probably because it belonged to
            // a package that wasn't actually loaded this build, so user cannot refer to files in
            // that package.
            continue;
          }
        } else {
          // This file is given with an absolute path. We will error out after transitive scanning
          // of the top-level source is finished unless this corresponds to a built-in include
          // directory, and will ignore this artifact in any case, but track it here so that its
          // includes can be processed.
          artifact = artifactFactory.getSourceArtifact(foundHeaderPath, absoluteRoot);
        }
        // Reset contextPathPos to 0 so that include_next in a framework header searches the include
        // paths from the beginning.
        return LocateOnPathResult.create(artifact, 0, viewedIllegalOutput);
      }
      // Not found.
      return LocateOnPathResult.createNotFound(viewedIllegalOutput);
    }

    /**
     * Locates an included file along the search paths.
     *
     * @param inclusion the inclusion to locate
     * @param pathToLegalOutputArtifact generated files which may be reached during scanning
     * @return a LocateOnPathResult
     */
    protected abstract LocateOnPathResult lookup(
        InclusionWithContext inclusion, Map<PathFragment, Artifact> pathToLegalOutputArtifact);

    /**
     * Locates an included file along the search paths.
     *
     * @param inclusion the inclusion to locate
     * @param pathToLegalOutputArtifact generated files which may be reached during scanning
     * @return a LocateOnPathResult
     */
    protected abstract ListenableFuture<LocateOnPathResult> lookupAsync(
        InclusionWithContext inclusion, Map<PathFragment, Artifact> pathToLegalOutputArtifact);
  }

  /**
   * A cache of inclusion lookups, taking care to avoid spurious caching related to generated
   * headers / source files.
   */
  @ThreadSafety.ThreadSafe
  private class LegacyInclusionCache extends InclusionCache {
    private final ConcurrentMap<InclusionWithContext, LocateOnPathResult> cache =
        new ConcurrentHashMap<>();

    @Override
    public LocateOnPathResult lookup(
        InclusionWithContext inclusion, Map<PathFragment, Artifact> pathToLegalOutputArtifact) {
      LocateOnPathResult result =
          cache.computeIfAbsent(
              inclusion, key -> locateOnPaths(key, pathToLegalOutputArtifact, false));
      // If the previous computation for this inclusion had a different pathToLegalOutputArtifact
      // map, result may not be valid for this lookup. Because this is a hot spot, we tolerate a
      // known correctness bug but try to catch most issues.
      // (1) [correct]: The prior computation found an output file, but that file is not in the
      // current lookup's inputs. We don't reuse the computation. b/149935208.
      // (2) [correct]: The prior computation checked an output path not in its legal outputs, and
      // then didn't find a file anywhere. However, that output file is a legal input for this
      // lookup. We don't reuse the computation. b/2097998.
      // (3) [INCORRECT]: Same as (2), except that the prior computation found a file after checking
      // the output path not in its legal inputs. We incorrectly cache this computation, assuming it
      // is very rare. b/150307245.
      if (result.path != null) {
        if (result.path.isSourceArtifact()
            || result.path.equals(pathToLegalOutputArtifact.get(result.path.getExecPath()))) {
          return result;
        }
      } else if (!result.viewedIllegalOutputFile) {
        return result;
      }

      result = locateOnPaths(inclusion, pathToLegalOutputArtifact, true);
      if (result.path != null || !result.viewedIllegalOutputFile) {
        // In this case, the result is now cachable either because a file has been found or
        // because there are no more illegal output files. This is rare in practice. Avoid
        // creating a future and modifying the cache in the common case.
        cache.put(inclusion, result);
      }
      return result;
    }

    @Override
    protected ListenableFuture<LocateOnPathResult> lookupAsync(
        InclusionWithContext inclusion, Map<PathFragment, Artifact> pathToLegalOutputArtifact) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * A cache of inclusion lookups, taking care to avoid spurious caching related to generated
   * headers / source files.
   */
  @ThreadSafety.ThreadSafe
  private class AsyncInclusionCache extends InclusionCache {
    private final ConcurrentMap<InclusionWithContext, ListenableFuture<LocateOnPathResult>> cache =
        new ConcurrentHashMap<>();

    @Override
    protected LocateOnPathResult lookup(
        InclusionWithContext inclusion, Map<PathFragment, Artifact> pathToLegalOutputArtifact) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<LocateOnPathResult> lookupAsync(
        InclusionWithContext inclusion, Map<PathFragment, Artifact> pathToLegalOutputArtifact) {
      SettableFuture<LocateOnPathResult> future = SettableFuture.create();
      ListenableFuture<LocateOnPathResult> previous = cache.putIfAbsent(inclusion, future);
      ListenableFuture<LocateOnPathResult> actualFuture;
      if (previous == null) {
        includePool.execute(
            () -> {
              LocateOnPathResult result =
                  locateOnPaths(inclusion, pathToLegalOutputArtifact, false);
              future.set(result);
            });
        actualFuture = future;
      } else {
        actualFuture = previous;
      }
      // It is not safe to cache lookups which viewed illegal output files. Their nonexistence do
      // not imply nonexistence for actions using the same include scanner, but executed later on.
      // See bug 2097998. For performance reasons, take a small shortcut: only avoid caching when
      // the path lookup result from locateOnPaths() is empty.
      return Futures.transformAsync(
          actualFuture,
          (result) -> {
            if (result.path != null || !result.viewedIllegalOutputFile) {
              return Futures.immediateFuture(result);
            }

            result = locateOnPaths(inclusion, pathToLegalOutputArtifact, true);
            if (result.path != null || !result.viewedIllegalOutputFile) {
              // In this case, the result is now cachable either because a file has been found or
              // because there are no more illegal output files. This is rare in practice. Avoid
              // creating a future and modifying the cache in the common case.
              ListenableFuture<LocateOnPathResult> replacement = Futures.immediateFuture(result);
              cache.put(inclusion, replacement);
              return replacement;
            }
            return Futures.immediateFuture(result);
          },
          MoreExecutors.directExecutor());
    }
  }

  private static class LocateOnPathResult {
    private static final LocateOnPathResult NOT_FOUND_VIEWED_ILLEGAL =
        new LocateOnPathResult(null, -1, true);
    private static final LocateOnPathResult NOT_FOUND_NO_VIEWED_ILLEGAL =
        new LocateOnPathResult(null, -1, false);
    private final Artifact path;
    private final int includePosition;
    private final boolean viewedIllegalOutputFile;

    private LocateOnPathResult(
        Artifact path, int includePosition, boolean viewedIllegalOutputFile) {
      this.path = path;
      this.includePosition = includePosition;
      this.viewedIllegalOutputFile = viewedIllegalOutputFile;
    }

    static LocateOnPathResult create(
        Artifact path, int includePosition, boolean viewedIllegalOutputFile) {
      return new LocateOnPathResult(
          Preconditions.checkNotNull(path), includePosition, viewedIllegalOutputFile);
    }

    static LocateOnPathResult createNotFound(boolean viewedIllegalOutputFile) {
      return viewedIllegalOutputFile ? NOT_FOUND_VIEWED_ILLEGAL : NOT_FOUND_NO_VIEWED_ILLEGAL;
    }
  }

  private final Path execRoot;

  private final ArtifactFactory artifactFactory;
  private final Supplier<SpawnIncludeScanner> spawnIncludeScannerSupplier;

  /**
   * Externally-scoped cache of file path => parsed inclusion set mappings. Saves us from having to
   * parse files more than once, and can be shared by scanners with different search paths.
   */
  private final ConcurrentMap<Artifact, ListenableFuture<Collection<Inclusion>>> fileParseCache;

  private final IncludeParser parser;

  /**
   * Search path for searching for all quoted "xyz.h" includes, composed of all the -iquote, -I and
   * -isystem paths (in this order).
   */
  private final ImmutableList<PathFragment> quoteIncludePaths;

  /**
   * The index position within quoteIncludePaths at which framework paths (-F) should be searched.
   */
  private final int quoteIncludePathsFrameworkIndex;

  /**
   * Search path for searching for all includes, composed of all the -I and -isystem paths (in this
   * order).
   */
  private final List<PathFragment> includePaths;

  /** Search path for searching for all includes from frameworks. */
  private final ImmutableList<PathFragment> frameworkIncludePaths;

  private final PathFragment includeRootFragment;
  private final PathFragment outputPathFragment;
  private final Root absoluteRoot;

  /**
   * Scanner-scoped cache of inclusions with their resolved files and include path entries. This
   * cache is specific to a given pair of search paths, and is thus scanner-local.
   *
   * <p>Each inclusion (name+type+context) is associated with its resolved file here, thus saving
   * redundant path searches. The second entry of the pair is the include path entry on which the
   * file was found.
   */
  private final InclusionCache inclusionCache;

  private final PathExistenceCache pathCache;

  private final ExecutorService includePool;

  private final boolean useAsyncIncludeScanner;

  // We are using this Random just for shuffling, so keep the order deterministic by hardcoding
  // the seed.
  private static final Random CONSTANT_SEED_RANDOM = new Random(88);

  /**
   * Constructs a new IncludeScanner
   *
   * @param cache externally scoped cache of file-path to inclusion-set mappings
   * @param pathCache include path existence cache
   * @param quoteIncludePaths the list of quote search path dirs (-iquote)
   * @param includePaths the list of all other non-framework search path dirs (-I and -isystem)
   * @param frameworkIncludePaths the list of framework other search path dirs (-F)
   */
  LegacyIncludeScanner(
      IncludeParser parser,
      ExecutorService includePool,
      ConcurrentMap<Artifact, ListenableFuture<Collection<Inclusion>>> cache,
      PathExistenceCache pathCache,
      List<PathFragment> quoteIncludePaths,
      List<PathFragment> includePaths,
      List<PathFragment> frameworkIncludePaths,
      Path outputPath,
      Path execRoot,
      ArtifactFactory artifactFactory,
      Supplier<SpawnIncludeScanner> spawnIncludeScannerSupplier,
      boolean useAsyncIncludeScanner) {
    this.parser = parser;
    this.includePool = includePool;
    this.fileParseCache = cache;
    this.pathCache = pathCache;
    this.artifactFactory = Preconditions.checkNotNull(artifactFactory);
    this.spawnIncludeScannerSupplier = spawnIncludeScannerSupplier;
    this.quoteIncludePaths = ImmutableList.<PathFragment>builder()
        .addAll(quoteIncludePaths)
        .addAll(includePaths)
        .build();
    this.quoteIncludePathsFrameworkIndex = quoteIncludePaths.size();
    this.includePaths = ImmutableList.copyOf(includePaths);
    this.frameworkIncludePaths = ImmutableList.copyOf(frameworkIncludePaths);
    this.inclusionCache =
        useAsyncIncludeScanner ? new AsyncInclusionCache() : new LegacyInclusionCache();
    this.execRoot = execRoot;
    this.outputPathFragment = outputPath.relativeTo(execRoot);
    this.includeRootFragment =
        outputPathFragment.getRelative(BlazeDirectories.RELATIVE_INCLUDE_DIR);
    this.absoluteRoot = Root.absoluteRoot(execRoot.getFileSystem());
    this.useAsyncIncludeScanner = useAsyncIncludeScanner;
  }

  /**
   * Locates an included file relative to the including file. The result is not cacheable.
   *
   * @param inclusion the inclusion to locate
   * @param includer the including file
   * @return the resolved Path, or null if no file could be found
   */
  private Artifact locateRelative(
      Inclusion inclusion, Map<PathFragment, Artifact> legalOutputFiles, Artifact includer) {
    if (inclusion.kind != Kind.QUOTE) {
      return null;
    }
    PathFragment name = inclusion.pathFragment;
    PathFragment execPath = includer.getExecPath().getParentDirectory().getRelative(name);
    if (!isFile(execPath, name, includer.isSourceArtifact(), legalOutputFiles.keySet())) {
      return null;
    }
    PathFragment parentDirectory = includer.getRootRelativePath().getParentDirectory();
    PathFragment rootRelativePath = parentDirectory.getRelative(name);
    if (rootRelativePath.containsUplevelReferences()) {
      // An include cannot break out of a (package path) root via a relative inclusion. It should
      // also not break out of the root and then come back into it -- who knows what hardcoded
      // directory names there are in it.
      return null;
    }
    if (legalOutputFiles.containsKey(execPath)) {
      return legalOutputFiles.get(execPath);
    }
    ArtifactRoot root = includer.getRoot();
    Artifact sourceArtifact =
        artifactFactory.resolveSourceArtifactWithAncestor(
            name, parentDirectory, root, RepositoryName.MAIN);
    if (sourceArtifact == null) {
      // If the name had up-level references, this path may not be under any package. Otherwise,
      // we must have gotten an artifact, since it should be under the same package as the
      // including artifact.
      Preconditions.checkState(
          name.containsUplevelReferences(),
          "%s %s %s %s",
          name,
          parentDirectory,
          rootRelativePath,
          root);
    }
    return sourceArtifact;
  }

  /** Returns whether the given path exists in the filesystem. */
  private boolean isFile(
      PathFragment execPath, PathFragment includeAsWritten, boolean isSource,
      Collection<PathFragment> legalOutputFiles) {
    if (isRealOutputFile(execPath)) {
      return legalOutputFiles.contains(execPath);
    }
    // TODO(djasper): This code path cannot be hit with isSource being false. Verify and add
    // Preconditions check.
    if (isSource && !execPath.isAbsolute() && execPath.endsWith(includeAsWritten)) {
      // Verify that the directory of execPath exists as an optimization. Most includes are relative
      // to the workspace and we'd like to avoid stat'ing every such include relative to every
      // include path. If testing whether "a/b/c.h" is a file beneath the include path "e/f/",
      // verify that "e/f/a" and "e/f/a/b" are valid directories (and cache the result).
      int execPathSegments = execPath.segmentCount();
      int nameSegments = includeAsWritten.segmentCount();
      for (int i = execPathSegments - nameSegments + 1; i < execPathSegments; i++) {
        if (!pathCache.directoryExists(execPath.subFragment(0, i))) {
          return false;
        }
      }
    }
    return pathCache.fileExists(execPath, isSource);
  }

  @Override
  public ListenableFuture<?> processAsync(
      Artifact mainSource,
      Collection<Artifact> sources,
      IncludeScanningHeaderData includeScanningHeaderData,
      List<String> cmdlineIncludes,
      Set<Artifact> includes,
      ActionExecutionMetadata actionExecutionMetadata,
      ActionExecutionContext actionExecutionContext,
      Artifact grepIncludes)
      throws IOException, ExecException, InterruptedException {
    ImmutableSet<Artifact> pathHints =
        prepare(actionExecutionContext.getEnvironmentForDiscoveringInputs());
    IncludeVisitor visitor;
    visitor =
        useAsyncIncludeScanner
            ? new AsyncIncludeVisitor(
                actionExecutionMetadata,
                actionExecutionContext,
                grepIncludes,
                includeScanningHeaderData.getPathToLegalOutputArtifact(),
                includeScanningHeaderData.getModularHeaders())
            : new LegacyIncludeVisitor(
                actionExecutionMetadata,
                actionExecutionContext,
                grepIncludes,
                includeScanningHeaderData.getPathToLegalOutputArtifact(),
                includeScanningHeaderData.getModularHeaders());
    return visitor.processInternal(
        mainSource,
        sources,
        cmdlineIncludes,
        includes,
        pathHints);
  }

  private ImmutableSet<Artifact> prepare(SkyFunction.Environment env) throws InterruptedException {
    if (parser.getHints() != null) {
      Collection<Artifact> artifacts =
          parser.getHints().getPathLevelHintedInclusions(quoteIncludePaths, env);
      if (env.valuesMissing()) {
        throw new MissingDepException();
      }
      ImmutableSet.Builder<Artifact> pathHints;
      pathHints = ImmutableSet.builderWithExpectedSize(quoteIncludePaths.size());
      pathHints.addAll(Preconditions.checkNotNull(artifacts, quoteIncludePaths));
      return pathHints.build();
    }
    return ImmutableSet.of();
  }

  private static void checkForInterrupt(String operation, Object source)
      throws InterruptedException {
    // We require passing in the operation and the source Path / Artifact to avoid intermediate
    // String operations. The include scanner is performance critical and this showed up in a
    // profiler.
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException(
          "Include scanning interrupted while " + operation + " " + source);
    }
  }

  private boolean isIllegalOutputFile(
      PathFragment includeFile, Collection<PathFragment> legalOutputFiles) {
    return isRealOutputFile(includeFile) && !legalOutputFiles.contains(includeFile);
  }

  private boolean isRealOutputFile(PathFragment path) {
    return path.startsWith(outputPathFragment) && !isIncPath(path);
  }

  private boolean isIncPath(PathFragment path) {
    // See CreateIncSymlinkAction and where it's used: The symlink trees
    // are always rooted at locations that fit the logic here.
    return path.startsWith(includeRootFragment) && !path.equals(includeRootFragment);
  }

  private interface IncludeVisitor {
    ListenableFuture<?> processInternal(
        Artifact mainSource,
        Collection<Artifact> sources,
        List<String> cmdlineIncludes,
        Set<Artifact> includes,
        ImmutableSet<Artifact> pathHints)
        throws InterruptedException, IOException, ExecException;
  }

  /**
   * Implements a potentially parallel traversal over source files using a thread pool shared across
   * different IncludeScanner instances.
   */
  private class LegacyIncludeVisitor extends AbstractQueueVisitor implements IncludeVisitor {
    private final ActionExecutionMetadata actionExecutionMetadata;
    private final ActionExecutionContext actionExecutionContext;
    private final Artifact grepIncludes;
    private final Map<PathFragment, Artifact> pathToLegalOutputArtifact;
    /** The set of headers known to be part of a C++ module. Scanning can stop here. */
    private final Set<Artifact> modularHeaders;

    /** The set of all processed inclusions, to avoid processing duplicate inclusions. */
    private final Set<ArtifactWithInclusionContext> visitedInclusions = Sets.newConcurrentHashSet();

    LegacyIncludeVisitor(
        ActionExecutionMetadata actionExecutionMetadata,
        ActionExecutionContext actionExecutionContext,
        Artifact grepIncludes,
        final Map<PathFragment, Artifact> pathToLegalOutputArtifact,
        Set<Artifact> modularHeaders) {
      super(
          includePool,
          /*shutdownOnCompletion=*/ false,
          /*failFastOnException=*/ true,
          ErrorClassifier.DEFAULT);
      this.actionExecutionMetadata = actionExecutionMetadata;
      this.actionExecutionContext = actionExecutionContext;
      this.grepIncludes = grepIncludes;
      this.pathToLegalOutputArtifact = pathToLegalOutputArtifact;
      this.modularHeaders = modularHeaders;
    }

    @Override
    public ListenableFuture<?> processInternal(
        Artifact mainSource,
        Collection<Artifact> sources,
        List<String> cmdlineIncludes,
        Set<Artifact> includes,
        ImmutableSet<Artifact> pathHints)
        throws InterruptedException, IOException, ExecException {
      try {
        // Process cmd line includes, if specified.
        if (mainSource != null && !cmdlineIncludes.isEmpty()) {
          processCmdlineIncludes(mainSource, cmdlineIncludes, includes);
          sync();
        }

        processBulkAsync(sources, includes);
        sync();

        // Process include hints
        // TODO(ulfjack): Make this code go away. Use the new hinted inclusions instead.
        Hints hints = parser.getHints();
        if (hints != null) {
          // Follow "path" hints.
          processBulkAsync(pathHints, includes);
          // Follow "file" hints for the primary sources.
          for (Artifact source : sources) {
            processFileLevelHintsAsync(hints, source, includes);
          }
          sync();

          // Follow "file" hints for all included headers, transitively.
          Set<Artifact> frontier = includes;
          while (!frontier.isEmpty()) {
            Set<Artifact> adjacent = Sets.newConcurrentHashSet();
            for (Artifact include : frontier) {
              processFileLevelHintsAsync(hints, include, adjacent);
            }
            sync();
            // Keep novel nodes as the next frontier.
            for (Iterator<Artifact> iter = adjacent.iterator(); iter.hasNext(); ) {
              if (!includes.add(iter.next())) {
                iter.remove();
              }
            }
            frontier = adjacent;
          }
        }
      } catch (IOException | InterruptedException | ExecException | MissingDepException e) {
        // Careful: Do not leak visitation threads if we have an exception in the initial thread.
        sync();
        throw e;
      }
      return Futures.immediateFuture(null);
    }

    /** Block for the completion of all outstanding visitations. */
    private void sync() throws IOException, ExecException, InterruptedException {
      try {
        super.awaitQuiescence(true);
      } catch (InterruptedException e) {
        throw new InterruptedException("Interrupted during include visitation");
      } catch (IORuntimeException e) {
        throw e.getCauseIOException();
      } catch (ExecRuntimeException e) {
        throw e.getRealCause();
      } catch (InterruptedRuntimeException e) {
        throw e.getRealCause();
      }
    }

    /**
     * Processes a given file for includes and populates the provided set with the visited includes.
     *
     * @param source the file to process
     * @param contextPathPos the position on the include path where the containing file was found,
     *     or <code>-1</code> for top-level inclusions
     * @param contextKind the kind how the containing file was included, or null for top-level
     *     inclusions
     * @param visited the set to receive the files that are transitively included by {@code source}
     */
    private void process(
        final Artifact source, int contextPathPos, Kind contextKind, Set<Artifact> visited)
        throws IOException, ExecException, InterruptedException {
      checkForInterrupt("processing", source);

      Collection<Inclusion> inclusions;
      try {
        inclusions =
            fileParseCache
                .computeIfAbsent(
                    source,
                    file -> {
                      try {
                        return Futures.immediateFuture(
                            parser.extractInclusions(
                                file,
                                actionExecutionMetadata,
                                actionExecutionContext,
                                grepIncludes,
                                spawnIncludeScannerSupplier.get(),
                                isRealOutputFile(source.getExecPath())));
                      } catch (IOException e) {
                        throw new IORuntimeException(e);
                      } catch (ExecException e) {
                        throw new ExecRuntimeException(e);
                      } catch (InterruptedException e) {
                        throw new InterruptedRuntimeException(e);
                      }
                    })
                .get();
      } catch (ExecutionException ee) {
        try {
          Throwables.throwIfInstanceOf(ee.getCause(), RuntimeException.class);
          throw new IllegalStateException(ee.getCause());
        } catch (IORuntimeException e) {
          throw e.getCauseIOException();
        } catch (ExecRuntimeException e) {
          throw e.getRealCause();
        } catch (InterruptedRuntimeException e) {
          throw e.getRealCause();
        }
      }
      Preconditions.checkNotNull(inclusions, source);

      // Shuffle the inclusions to get better parallelism. See b/62200470.
      List<Inclusion> shuffledInclusions = new ArrayList<>(inclusions);
      Collections.shuffle(shuffledInclusions, CONSTANT_SEED_RANDOM);

      // For each inclusion: get or locate its target file & recursively process
      IncludeScannerHelper helper =
          new IncludeScannerHelper(includePaths, quoteIncludePaths, source);
      for (Inclusion inclusion : shuffledInclusions) {
        findAndProcess(
            helper.createInclusionWithContext(inclusion, contextPathPos, contextKind),
            source,
            visited);
      }
    }

    /**
     * Same as {@link #process}, but executes asynchronously if the #include lines of {@code source}
     * haven't been extracted yet. For sources that have already been extracted, just continue
     * walking them in the current thread. The overhead of scheduling this on other threads is
     * larger than the gain in concurrency. The only really slow operation is the (possibly remote)
     * extraction of includes.
     */
    private void processAsyncIfNotExtracted(
        final Artifact source, int contextPathPos, Kind contextKind, Set<Artifact> visited)
        throws IOException, ExecException, InterruptedException {
      ListenableFuture<Collection<Inclusion>> cacheResult = fileParseCache.get(source);
      if (cacheResult != null) {
        process(source, contextPathPos, contextKind, visited);
      } else {
        super.execute(
            () -> {
              try {
                process(source, contextPathPos, contextKind, visited);
              } catch (IOException e) {
                throw new IORuntimeException(e);
              } catch (ExecException e) {
                throw new ExecRuntimeException(e);
              } catch (InterruptedException e) {
                throw new InterruptedRuntimeException(e);
              }
            });
      }
    }

    /** Visits an inclusion starting from a source file. */
    private void findAndProcess(
        InclusionWithContext inclusion, Artifact source, Set<Artifact> visited)
        throws IOException, ExecException, InterruptedException {
      // Try to find the included file relative to the file that contains the inclusion. Relative
      // inclusions are handled like the first entry on the quote include path
      Artifact includeFile =
          locateRelative(inclusion.getInclusion(), pathToLegalOutputArtifact, source);
      int contextPathPos = 0;
      Kind contextKind = null;

      checkForInterrupt("visiting", source);

      // If nothing has been found, get an inclusion from the cache. This will automatically search
      // on the include paths and populate the cache if necessary.
      if (includeFile == null) {
        LocateOnPathResult result = inclusionCache.lookup(inclusion, pathToLegalOutputArtifact);
        includeFile = result.path;
        contextPathPos = result.includePosition;
        contextKind = inclusion.getContextKind();
      }

      // Recursively process the found file (if not yet done).
      if (includeFile != null
          && !isIllegalOutputFile(includeFile.getExecPath(), pathToLegalOutputArtifact.keySet())
          && visitedInclusions.add(
              new ArtifactWithInclusionContext(includeFile, contextKind, contextPathPos))) {
        visited.add(includeFile);
        if (modularHeaders.contains(includeFile)) {
          return;
        }
        processAsyncIfNotExtracted(includeFile, contextPathPos, contextKind, visited);
      }
    }

    /**
     * Processes a given list of includes for a given base file and populates the provided set with
     * the visited includes
     *
     * @param source the source file used as a reference for finding includes
     * @param includes the list of -include option strings to locate and process
     * @param visited the set of files that are transitively included by {@code includes} to
     *     populate
     */
    private void processCmdlineIncludes(
        Artifact source, List<String> includes, Set<Artifact> visited)
        throws IOException, ExecException, InterruptedException {
      for (String incl : includes) {
        InclusionWithContext inclusion = new InclusionWithContext(incl, Kind.QUOTE);
        findAndProcess(inclusion, source, visited);
      }
    }

    /**
     * Processes a bunch sources asynchronously and adds them and their included files to the
     * provided set.
     *
     * @param sources the files to process and add to the set
     * @param visited the set to receive the files that are transitively included by {@code sources}
     */
    private void processBulkAsync(Collection<Artifact> sources, final Set<Artifact> visited)
        throws IOException, ExecException, InterruptedException {
      for (final Artifact source : sources) {
        // TODO(djasper): This looks suspicious. We should only stop based on visitedInclusions.
        if (!visited.add(source)) {
          continue;
        }

        processAsyncIfNotExtracted(source, /*contextPathPos=*/ -1, /*contextKind=*/ null, visited);
      }
    }

    private void processFileLevelHintsAsync(
        final Hints hints, final Artifact include, final Set<Artifact> alsoVisited) {
      Collection<Artifact> sources = hints.getFileLevelHintedInclusionsLegacy(include);
      // Early-out if there's nothing to do to avoid enqueuing a closure
      if (sources.isEmpty()) {
        return;
      }
      super.execute(
          () -> {
            try {
              processBulkAsync(sources, alsoVisited);
            } catch (IOException e) {
              throw new IORuntimeException(e);
            } catch (ExecException e) {
              throw new ExecRuntimeException(e);
            } catch (InterruptedException e) {
              throw new InterruptedRuntimeException(e);
            }
          });
    }
  }

  /**
   * Implements a potentially parallel traversal over source files using a thread pool shared across
   * different IncludeScanner instances.
   */
  private class AsyncIncludeVisitor implements IncludeVisitor {
    private final ActionExecutionMetadata actionExecutionMetadata;
    private final ActionExecutionContext actionExecutionContext;
    private final Artifact grepIncludes;
    private final Map<PathFragment, Artifact> pathToLegalOutputArtifact;
    /** The set of headers known to be part of a C++ module. Scanning can stop here. */
    private final Set<Artifact> modularHeaders;

    /** The set of all processed inclusions, to avoid processing duplicate inclusions. */
    private final Set<ArtifactWithInclusionContext> visitedInclusions = Sets.newConcurrentHashSet();

    public AsyncIncludeVisitor(
        ActionExecutionMetadata actionExecutionMetadata,
        ActionExecutionContext actionExecutionContext,
        Artifact grepIncludes,
        Map<PathFragment, Artifact> pathToLegalOutputArtifact,
        Set<Artifact> modularHeaders) {
      this.actionExecutionMetadata = actionExecutionMetadata;
      this.actionExecutionContext = actionExecutionContext;
      this.grepIncludes = grepIncludes;
      this.pathToLegalOutputArtifact = pathToLegalOutputArtifact;
      this.modularHeaders = modularHeaders;
    }

    @Override
    public ListenableFuture<?> processInternal(
        Artifact mainSource,
        Collection<Artifact> sources,
        List<String> cmdlineIncludes,
        Set<Artifact> includes,
        ImmutableSet<Artifact> pathHints)
        throws InterruptedException, IOException, ExecException {
      try {
        ListenableFuture<?> result = Futures.immediateFuture(null);
        // Process cmd line includes, if specified.
        if (mainSource != null && !cmdlineIncludes.isEmpty()) {
          result = processCmdlineIncludesAsync(mainSource, cmdlineIncludes, includes);
        }

        result =
            Futures.transformAsync(result, (v) -> processBulkAsync(sources, includes), includePool);

        // Process include hints
        // TODO(ulfjack): Make this code go away. Use the new hinted inclusions instead.
        Hints hints = parser.getHints();
        if (hints != null) {
          // Follow "path" hints.
          if (!pathHints.isEmpty()) {
            result =
                Futures.transformAsync(
                    result, (v) -> processBulkAsync(pathHints, includes), includePool);
          }

          Collection<Artifact> allHintedIncludes = allHintedIncludes(hints, sources);
          if (!allHintedIncludes.isEmpty()) {
            result =
                Futures.transformAsync(
                    result, (v) -> processBulkAsync(allHintedIncludes, includes), includePool);
          }
          // Follow "file" hints for all included headers, transitively.
          result =
              Futures.transformAsync(
                  result, (v) -> processOnePass(hints, includes, includes), includePool);
        }
        return result;
      } catch (IOException | InterruptedException | ExecException | MissingDepException e) {
        // Careful: Do not leak visitation threads if we have an exception in the initial thread.
        throw e;
      }
    }

    private ListenableFuture<Collection<Artifact>> processOnePass(
        Hints hints, Collection<Artifact> before, Set<Artifact> includes)
        throws InterruptedException, IOException, ExecException {
      Set<Artifact> adjacent = Sets.newConcurrentHashSet();
      ListenableFuture<?> future = processAllFileLevelHintsAsync(hints, before, adjacent);
      return Futures.transformAsync(
          future,
          (v) -> {
            // Keep novel nodes as the next frontier.
            for (Iterator<Artifact> iter = adjacent.iterator(); iter.hasNext(); ) {
              if (!includes.add(iter.next())) {
                iter.remove();
              }
            }
            if (adjacent.isEmpty()) {
              return Futures.immediateFuture(null);
            }
            return processOnePass(hints, adjacent, includes);
          },
          includePool);
    }

    /**
     * Processes a given file for includes and populates the provided set with the visited includes.
     *
     * @param source the file to process
     * @param contextPathPos the position on the include path where the containing file was found,
     *     or <code>-1</code> for top-level inclusions
     * @param contextKind the kind how the containing file was included, or null for top-level
     *     inclusions
     * @param visited the set to receive the files that are transitively included by {@code source}
     */
    private ListenableFuture<?> process(
        final Artifact source, int contextPathPos, Kind contextKind, Set<Artifact> visited)
        throws IOException, InterruptedException {
      checkForInterrupt("processing", source);

      ListenableFuture<Collection<Inclusion>> actualFuture;
      SettableFuture<Collection<Inclusion>> future = SettableFuture.create();
      ListenableFuture<Collection<Inclusion>> previous = fileParseCache.putIfAbsent(source, future);
      if (previous == null) {
        actualFuture = future;
        future.setFuture(
            parser.extractInclusionsAsync(
                includePool,
                source,
                actionExecutionMetadata,
                actionExecutionContext,
                grepIncludes,
                spawnIncludeScannerSupplier.get(),
                isRealOutputFile(source.getExecPath())));
        // When rewinding, we may need to rerun a spawn that previously failed rather than cache the
        // failure here, so we remove the cache entry if the future throws. Unfortunately, we can
        // only detect that case by actually calling get().
        future.addListener(
            () -> {
              try {
                future.get();
              } catch (ExecutionException | InterruptedException e) {
                fileParseCache.remove(source);
              }
            },
            MoreExecutors.directExecutor());
      } else {
        actualFuture = previous;
      }

      if (actualFuture.isDone()) {
        Collection<Inclusion> inclusions;
        try {
          inclusions = actualFuture.get();
        } catch (ExecutionException e) {
          return actualFuture;
        }
        return processInclusions(inclusions, source, contextPathPos, contextKind, visited);
      } else {
        return Futures.transformAsync(
            actualFuture,
            (inclusions) ->
                processInclusions(inclusions, source, contextPathPos, contextKind, visited),
            MoreExecutors.directExecutor());
      }
    }

    private ListenableFuture<?> processInclusions(
        Collection<Inclusion> inclusions,
        Artifact source,
        int contextPathPos,
        Kind contextKind,
        Set<Artifact> visited)
        throws IOException, InterruptedException {
      Preconditions.checkNotNull(inclusions, source);
      // Shuffle the inclusions to get better parallelism. See b/62200470.
      // Be careful not to modify the original collection! It's shared between any number of
      // threads.
      // TODO: Maybe we should shuffle before returning it to avoid the copy?
      List<Inclusion> shuffledInclusions = new ArrayList<>(inclusions);
      Collections.shuffle(shuffledInclusions, CONSTANT_SEED_RANDOM);

      // For each inclusion: get or locate its target file & recursively process
      IncludeScannerHelper helper =
          new IncludeScannerHelper(includePaths, quoteIncludePaths, source);
      List<ListenableFuture<?>> allFutures = new ArrayList<>(shuffledInclusions.size());
      for (Inclusion inclusion : shuffledInclusions) {
        allFutures.add(
            findAndProcess(
                helper.createInclusionWithContext(inclusion, contextPathPos, contextKind),
                source,
                visited));
      }
      return Futures.allAsList(allFutures);
    }

    /** Visits an inclusion starting from a source file. */
    private ListenableFuture<?> findAndProcess(
        InclusionWithContext inclusion, Artifact source, Set<Artifact> visited)
        throws IOException, InterruptedException {
      // Try to find the included file relative to the file that contains the inclusion. Relative
      // inclusions are handled like the first entry on the quote include path
      Artifact includeFile =
          locateRelative(inclusion.getInclusion(), pathToLegalOutputArtifact, source);

      checkForInterrupt("visiting", source);

      // If nothing has been found, get an inclusion from the cache. This will automatically search
      // on the include paths and populate the cache if necessary.
      if (includeFile == null) {
        ListenableFuture<LocateOnPathResult> lookupFuture =
            inclusionCache.lookupAsync(inclusion, pathToLegalOutputArtifact);
        return Futures.transformAsync(
            lookupFuture,
            (locateOnPathResult) ->
                processFound(
                    locateOnPathResult,
                    inclusion.getContextKind(),
                    visited,
                    pathToLegalOutputArtifact,
                    visitedInclusions),
            MoreExecutors.directExecutor());
      } else {
        LocateOnPathResult locateOnPathResult = new LocateOnPathResult(includeFile, 0, false);
        Kind contextKind = null;
        // Recursively process the found file (if not yet done).
        return processFound(
            locateOnPathResult, contextKind, visited, pathToLegalOutputArtifact, visitedInclusions);
      }
    }

    /** Visits an inclusion starting from a source file. */
    private ListenableFuture<?> processFound(
        LocateOnPathResult locateOnPathResult,
        Kind contextKind,
        Set<Artifact> visited,
        Map<PathFragment, Artifact> pathToLegalOutputArtifact,
        Set<ArtifactWithInclusionContext> visitedInclusions)
        throws IOException, InterruptedException {
      // Try to find the included file relative to the file that contains the inclusion. Relative
      // inclusions are handled like the first entry on the quote include path
      Artifact includeFile = locateOnPathResult.path;
      int contextPathPos = locateOnPathResult.includePosition;

      // Recursively process the found file (if not yet done).
      if (includeFile != null
          && !isIllegalOutputFile(includeFile.getExecPath(), pathToLegalOutputArtifact.keySet())
          && visitedInclusions.add(
              new ArtifactWithInclusionContext(includeFile, contextKind, contextPathPos))) {
        visited.add(includeFile);
        if (modularHeaders.contains(includeFile)) {
          return Futures.immediateFuture(null);
        }
        return process(includeFile, contextPathPos, contextKind, visited);
      }
      return Futures.immediateFuture(null);
    }

    /**
     * Processes a given list of includes for a given base file and populates the provided set with
     * the visited includes
     *
     * @param source the source file used as a reference for finding includes
     * @param includes the list of -include option strings to locate and process
     * @param visited the set of files that are transitively included by {@code includes} to
     *     populate
     */
    private ListenableFuture<?> processCmdlineIncludesAsync(
        Artifact source, List<String> includes, Set<Artifact> visited)
        throws IOException, ExecException, InterruptedException {
      List<ListenableFuture<?>> allFutures = new ArrayList<>(includes.size());
      for (String incl : includes) {
        InclusionWithContext inclusion = new InclusionWithContext(incl, Kind.QUOTE);
        allFutures.add(findAndProcess(inclusion, source, visited));
      }
      return Futures.allAsList(allFutures);
    }

    /**
     * Processes a bunch sources asynchronously and adds them and their included files to the
     * provided set.
     *
     * @param sources the files to process and add to the set
     * @param visited the set to receive the files that are transitively included by {@code sources}
     */
    private ListenableFuture<?> processBulkAsync(
        Collection<Artifact> sources, Set<Artifact> visited)
        throws IOException, InterruptedException {
      if (sources.isEmpty()) {
        // Early-out if there's nothing to do.
        return Futures.immediateFuture(null);
      }
      List<ListenableFuture<?>> allFutures = new ArrayList<>(sources.size());
      for (final Artifact source : sources) {
        // TODO(djasper): This looks suspicious. We should only stop based on visitedInclusions.
        if (!visited.add(source)) {
          continue;
        }

        allFutures.add(process(source, /*contextPathPos=*/ -1, /*contextKind=*/ null, visited));
      }
      return Futures.allAsList(allFutures);
    }

    private ListenableFuture<?> processAllFileLevelHintsAsync(
        Hints hints, Collection<Artifact> sources, Set<Artifact> alsoVisited)
        throws InterruptedException, IOException, ExecException {
      return processBulkAsync(allHintedIncludes(hints, sources), alsoVisited);
    }

    private Collection<Artifact> allHintedIncludes(Hints hints, Collection<Artifact> sources) {
      List<Artifact> result = new ArrayList<>();
      for (Artifact source : sources) {
        result.addAll(hints.getFileLevelHintedInclusionsLegacy(source));
      }
      return result;
    }
  }

  private static class ExecRuntimeException extends RuntimeException {
    private final ExecException cause;

    public ExecRuntimeException(ExecException e) {
      super(e);
      this.cause = e;
    }

    public ExecException getRealCause() {
      return cause;
    }
  }

  private static class InterruptedRuntimeException extends RuntimeException {
    private final InterruptedException cause;

    public InterruptedRuntimeException(InterruptedException e) {
      super(e);
      this.cause = e;
    }

    public InterruptedException getRealCause() {
      return cause;
    }
  }
}
