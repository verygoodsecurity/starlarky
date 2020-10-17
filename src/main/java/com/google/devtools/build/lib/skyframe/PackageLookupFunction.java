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
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.actions.FileValue;
import com.google.devtools.build.lib.actions.InconsistentFilesystemException;
import com.google.devtools.build.lib.cmdline.LabelConstants;
import com.google.devtools.build.lib.cmdline.LabelValidator;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.packages.BuildFileName;
import com.google.devtools.build.lib.packages.BuildFileNotFoundException;
import com.google.devtools.build.lib.packages.ErrorDeterminingRepositoryException;
import com.google.devtools.build.lib.packages.NoSuchPackageException;
import com.google.devtools.build.lib.packages.RepositoryFetchException;
import com.google.devtools.build.lib.packages.semantics.BuildLanguageOptions;
import com.google.devtools.build.lib.pkgcache.PathPackageLocator;
import com.google.devtools.build.lib.repository.ExternalPackageHelper;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyFunctionException.Transience;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkSemantics;

/**
 * SkyFunction for {@link PackageLookupValue}s.
 */
public class PackageLookupFunction implements SkyFunction {
  /** Lists possible ways to handle a package label which crosses into a new repository. */
  public enum CrossRepositoryLabelViolationStrategy {
    /** Ignore the violation. */
    IGNORE,
    /** Generate an error. */
    ERROR;
  }

  private final AtomicReference<ImmutableSet<PackageIdentifier>> deletedPackages;
  private final CrossRepositoryLabelViolationStrategy crossRepositoryLabelViolationStrategy;
  private final ImmutableList<BuildFileName> buildFilesByPriority;
  private final ExternalPackageHelper externalPackageHelper;

  public PackageLookupFunction(
      AtomicReference<ImmutableSet<PackageIdentifier>> deletedPackages,
      CrossRepositoryLabelViolationStrategy crossRepositoryLabelViolationStrategy,
      ImmutableList<BuildFileName> buildFilesByPriority,
      ExternalPackageHelper externalPackageHelper) {
    this.deletedPackages = deletedPackages;
    this.crossRepositoryLabelViolationStrategy = crossRepositoryLabelViolationStrategy;
    this.buildFilesByPriority = buildFilesByPriority;
    this.externalPackageHelper = externalPackageHelper;
  }

  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws PackageLookupFunctionException, InterruptedException {
    PathPackageLocator pkgLocator = PrecomputedValue.PATH_PACKAGE_LOCATOR.get(env);
    StarlarkSemantics semantics = PrecomputedValue.STARLARK_SEMANTICS.get(env);

    PackageIdentifier packageKey = (PackageIdentifier) skyKey.argument();

    String packageNameErrorMsg = LabelValidator.validatePackageName(
        packageKey.getPackageFragment().getPathString());
    if (packageNameErrorMsg != null) {
      return PackageLookupValue.invalidPackageName("Invalid package name '" + packageKey + "': "
          + packageNameErrorMsg);
    }

    if (deletedPackages.get().contains(packageKey)) {
      return PackageLookupValue.DELETED_PACKAGE_VALUE;
    }

    if (!packageKey.getRepository().isMain()) {
      return computeExternalPackageLookupValue(skyKey, env, packageKey);
    }

    if (packageKey.equals(LabelConstants.EXTERNAL_PACKAGE_IDENTIFIER)) {
      return semantics.getBool(BuildLanguageOptions.EXPERIMENTAL_DISABLE_EXTERNAL_PACKAGE)
          ? PackageLookupValue.NO_BUILD_FILE_VALUE
          : computeWorkspacePackageLookupValue(env);
    }

    // Check .bazelignore file under main repository.
    IgnoredPackagePrefixesValue ignoredPatternsValue =
        (IgnoredPackagePrefixesValue) env.getValue(IgnoredPackagePrefixesValue.key());
    if (ignoredPatternsValue == null) {
      return null;
    }

    if (isPackageIgnored(packageKey, ignoredPatternsValue)) {
      return PackageLookupValue.DELETED_PACKAGE_VALUE;
    }

    return findPackageByBuildFile(env, pkgLocator, packageKey);
  }

  /**
   * For a package identifier {@code packageKey} such that the compute for {@code
   * PackageLookupValue.key(packageKey)} returned {@code NO_BUILD_FILE_VALUE}, provide a
   * human-readable error message with more details on where we searched for the package.
   */
  public static String explainNoBuildFileValue(PackageIdentifier packageKey, Environment env)
      throws InterruptedException {
    String educationalMessage = "Add a BUILD file to a directory to mark it as a package.";
    if (packageKey.getRepository().isMain()) {
      PathPackageLocator pkgLocator = PrecomputedValue.PATH_PACKAGE_LOCATOR.get(env);
      StringBuilder message = new StringBuilder();
      message.append("BUILD file not found in any of the following directories. ");
      message.append(educationalMessage);
      for (Root root : pkgLocator.getPathEntries()) {
        message
            .append("\n - ")
            .append(root.asPath().getRelative(packageKey.getPackageFragment()).getPathString());
      }
      return message.toString();
    } else {
      return "BUILD file not found in directory '"
          + packageKey.getPackageFragment()
          + "' of external repository "
          + packageKey.getRepository()
          + ". "
          + educationalMessage;
    }
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }

  @Nullable
  private PackageLookupValue findPackageByBuildFile(
      Environment env, PathPackageLocator pkgLocator, PackageIdentifier packageKey)
      throws PackageLookupFunctionException, InterruptedException {
    // TODO(bazel-team): The following is O(n^2) on the number of elements on the package path due
    // to having restart the SkyFunction after every new dependency. However, if we try to batch
    // the missing value keys, more dependencies than necessary will be declared. This wart can be
    // fixed once we have nicer continuation support [skyframe-loading]
    for (Root packagePathEntry : pkgLocator.getPathEntries()) {

      // This checks for the build file names in the correct precedence order.
      for (BuildFileName buildFileName : buildFilesByPriority) {
        PackageLookupValue result =
            getPackageLookupValue(env, packagePathEntry, packageKey, buildFileName);
        if (result == null) {
          return null;
        }
        if (result != PackageLookupValue.NO_BUILD_FILE_VALUE) {
          return result;
        }
      }
    }

    return PackageLookupValue.NO_BUILD_FILE_VALUE;
  }

  @Nullable
  private static FileValue getFileValue(
      RootedPath fileRootedPath, Environment env, PackageIdentifier packageIdentifier)
      throws PackageLookupFunctionException, InterruptedException {
    String basename = fileRootedPath.asPath().getBaseName();
    SkyKey fileSkyKey = FileValue.key(fileRootedPath);
    FileValue fileValue = null;
    try {
      fileValue = (FileValue) env.getValueOrThrow(fileSkyKey, IOException.class);
    } catch (InconsistentFilesystemException e) {
      // This error is not transient from the perspective of the PackageLookupFunction.
      throw new PackageLookupFunctionException(e, Transience.PERSISTENT);
    } catch (FileSymlinkException e) {
      throw new PackageLookupFunctionException(new BuildFileNotFoundException(packageIdentifier,
          "Symlink cycle detected while trying to find " + basename + " file "
              + fileRootedPath.asPath()),
          Transience.PERSISTENT);
    } catch (IOException e) {
      throw new PackageLookupFunctionException(new BuildFileNotFoundException(packageIdentifier,
          "IO errors while looking for " + basename + " file reading "
              + fileRootedPath.asPath() + ": " + e.getMessage(), e),
          Transience.PERSISTENT);
    }
    return fileValue;
  }

  private PackageLookupValue getPackageLookupValue(
      Environment env,
      Root packagePathEntry,
      PackageIdentifier packageIdentifier,
      BuildFileName buildFileName)
      throws InterruptedException, PackageLookupFunctionException {
    PathFragment buildFileFragment = buildFileName.getBuildFileFragment(packageIdentifier);
    RootedPath buildFileRootedPath = RootedPath.toRootedPath(packagePathEntry, buildFileFragment);

    if (crossRepositoryLabelViolationStrategy == CrossRepositoryLabelViolationStrategy.ERROR) {
      // Is this path part of a local repository?
      RootedPath currentPath =
          RootedPath.toRootedPath(packagePathEntry, buildFileFragment.getParentDirectory());
      SkyKey repositoryLookupKey = LocalRepositoryLookupValue.key(currentPath);

      // TODO(jcater): Consider parallelizing these lookups.
      LocalRepositoryLookupValue localRepository;
      try {
        localRepository =
            (LocalRepositoryLookupValue)
                env.getValueOrThrow(repositoryLookupKey, ErrorDeterminingRepositoryException.class);
        if (localRepository == null) {
          return null;
        }
      } catch (ErrorDeterminingRepositoryException e) {
        // If the directory selected isn't part of a repository, that's an error.
        // TODO(katre): Improve the error message given here.
        throw new PackageLookupFunctionException(
            new BuildFileNotFoundException(
                packageIdentifier,
                "Unable to determine the local repository for directory "
                    + currentPath.asPath().getPathString()),
            Transience.PERSISTENT);
      }

      StarlarkSemantics starlarkSemantics = PrecomputedValue.STARLARK_SEMANTICS.get(env);
      if (starlarkSemantics == null) {
        return null;
      }

      if (localRepository.exists()
          && !localRepository.getRepository().equals(packageIdentifier.getRepository())) {
        // There is a repository mismatch, this is an error.
        // The correct package path is the one originally given, minus the part that is the local
        // repository.
        PathFragment pathToRequestedPackage = packageIdentifier.getSourceRoot();
        PathFragment localRepositoryPath = localRepository.getPath();
        if (localRepositoryPath.isAbsolute()) {
          // We need the package path to also be absolute.
          pathToRequestedPackage =
              packagePathEntry.getRelative(pathToRequestedPackage).asFragment();
        }
        PathFragment remainingPath = pathToRequestedPackage.relativeTo(localRepositoryPath);
        PackageIdentifier correctPackage =
            PackageIdentifier.create(localRepository.getRepository(), remainingPath);
        return PackageLookupValue.incorrectRepositoryReference(packageIdentifier, correctPackage);
      }

      // There's no local repository, keep going.
    } else {
      // Future-proof against adding future values to CrossRepositoryLabelViolationStrategy.
      Preconditions.checkState(
          crossRepositoryLabelViolationStrategy == CrossRepositoryLabelViolationStrategy.IGNORE,
          crossRepositoryLabelViolationStrategy);
    }

    // Check for the existence of the build file.
    FileValue fileValue = getFileValue(buildFileRootedPath, env, packageIdentifier);
    if (fileValue == null) {
      return null;
    }
    if (fileValue.isFile()) {
      return PackageLookupValue.success(buildFileRootedPath.getRoot(), buildFileName);
    }

    return PackageLookupValue.NO_BUILD_FILE_VALUE;
  }

  private static boolean isPackageIgnored(
      PackageIdentifier id, IgnoredPackagePrefixesValue ignoredPatternsValue) {
    PathFragment packageFragment = id.getPackageFragment();
    for (PathFragment pattern : ignoredPatternsValue.getPatterns()) {
      if (packageFragment.startsWith(pattern)) {
        return true;
      }
    }
    return false;
  }

  private PackageLookupValue computeWorkspacePackageLookupValue(Environment env)
      throws InterruptedException {
    RootedPath workspaceFile = externalPackageHelper.findWorkspaceFile(env);
    if (env.valuesMissing()) {
      return null;
    }

    if (workspaceFile == null) {
      return PackageLookupValue.NO_BUILD_FILE_VALUE;
    } else {
      BuildFileName filename = null;
      for (BuildFileName candidate : BuildFileName.values()) {
        if (workspaceFile.getRootRelativePath().equals(candidate.getFilenameFragment())) {
          filename = candidate;
          break;
        }
      }

      // Otherwise ExternalPackageUtil.findWorkspaceFile() returned something whose name is not in
      // BuildFileName
      Verify.verify(filename != null);
      return PackageLookupValue.success(workspaceFile.getRoot(), filename);
    }
  }

  /**
   * Gets a PackageLookupValue from a different Bazel repository.
   *
   * <p>To do this, it looks up the "external" package and finds a path mapping for the repository
   * name.
   */
  private PackageLookupValue computeExternalPackageLookupValue(
      SkyKey skyKey, Environment env, PackageIdentifier packageIdentifier)
      throws PackageLookupFunctionException, InterruptedException {
    PackageIdentifier id = (PackageIdentifier) skyKey.argument();
    SkyKey repositoryKey = RepositoryValue.key(id.getRepository());
    RepositoryValue repositoryValue;
    try {
      repositoryValue = (RepositoryValue) env.getValueOrThrow(
          repositoryKey, NoSuchPackageException.class, IOException.class, EvalException.class);
      if (repositoryValue == null) {
        return null;
      }
    } catch (NoSuchPackageException e) {
      throw new PackageLookupFunctionException(new BuildFileNotFoundException(id, e.getMessage()),
          Transience.PERSISTENT);
    } catch (IOException | EvalException e) {
      throw new PackageLookupFunctionException(
          new RepositoryFetchException(id, e.getMessage()), Transience.PERSISTENT);
    }
    if (!repositoryValue.repositoryExists()) {
      // TODO(ulfjack): Maybe propagate the error message from the repository delegator function?
      return new PackageLookupValue.NoRepositoryPackageLookupValue(id.getRepository().getName());
    }

    // Check .bazelignore file after fetching the external repository.
    IgnoredPackagePrefixesValue ignoredPatternsValue =
        (IgnoredPackagePrefixesValue)
            env.getValue(IgnoredPackagePrefixesValue.key(id.getRepository()));
    if (ignoredPatternsValue == null) {
      return null;
    }

    if (isPackageIgnored(id, ignoredPatternsValue)) {
      return PackageLookupValue.DELETED_PACKAGE_VALUE;
    }

    // This checks for the build file names in the correct precedence order.
    for (BuildFileName buildFileName : buildFilesByPriority) {
      PathFragment buildFileFragment =
          id.getPackageFragment().getRelative(buildFileName.getFilenameFragment());
      RootedPath buildFileRootedPath =
          RootedPath.toRootedPath(Root.fromPath(repositoryValue.getPath()), buildFileFragment);
      FileValue fileValue = getFileValue(buildFileRootedPath, env, packageIdentifier);
      if (fileValue == null) {
        return null;
      }

      if (fileValue.isFile()) {
        return PackageLookupValue.success(
            repositoryValue, Root.fromPath(repositoryValue.getPath()), buildFileName);
      }
    }

    return PackageLookupValue.NO_BUILD_FILE_VALUE;
  }

  /**
   * Used to declare all the exception types that can be wrapped in the exception thrown by
   * {@link PackageLookupFunction#compute}.
   */
  private static final class PackageLookupFunctionException extends SkyFunctionException {
    public PackageLookupFunctionException(BuildFileNotFoundException e, Transience transience) {
      super(e, transience);
    }

    public PackageLookupFunctionException(RepositoryFetchException e, Transience transience) {
      super(e, transience);
    }

    public PackageLookupFunctionException(InconsistentFilesystemException e,
        Transience transience) {
      super(e, transience);
    }
  }
}
