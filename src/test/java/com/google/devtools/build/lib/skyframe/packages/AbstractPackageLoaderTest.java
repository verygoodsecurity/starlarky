// Copyright 2017 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.skyframe.packages;

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.build.lib.testutil.MoreAsserts.assertContainsEvent;
import static com.google.devtools.build.lib.testutil.MoreAsserts.assertNoEvents;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.events.Reporter;
import com.google.devtools.build.lib.events.StoredEventHandler;
import com.google.devtools.build.lib.packages.NoSuchPackageException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.skyframe.ExternalFilesHelper.ExternalFileAction;
import com.google.devtools.build.lib.vfs.DigestHashFunction;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.lib.vfs.inmemoryfs.InMemoryFileSystem;
import java.util.concurrent.ForkJoinPool;
import org.junit.Before;
import org.junit.Test;

/** Abstract base class of a unit test for a {@link AbstractPackageLoader} implementation. */
public abstract class AbstractPackageLoaderTest {
  protected Path workspaceDir;
  protected StoredEventHandler handler;
  protected FileSystem fs;
  protected Root root;
  private Reporter reporter;

  @Before
  public final void init() throws Exception {
    fs = new InMemoryFileSystem(DigestHashFunction.SHA256);
    workspaceDir = fs.getPath("/workspace/");
    workspaceDir.createDirectoryAndParents();
    root = Root.fromPath(workspaceDir);
    reporter = new Reporter(new EventBus());
    handler = new StoredEventHandler();
    reporter.addHandler(handler);
  }

  protected abstract AbstractPackageLoader.Builder newPackageLoaderBuilder(Root workspaceDir);

  protected abstract ForkJoinPool extractLegacyGlobbingForkJoinPool(PackageLoader packageLoader);

  protected AbstractPackageLoader.Builder newPackageLoaderBuilder() {
    return newPackageLoaderBuilder(root).useDefaultStarlarkSemantics().setCommonReporter(reporter);
  }

  protected PackageLoader newPackageLoader() {
    return newPackageLoaderBuilder().build();
  }

  @Test
  public void simpleNoPackage() {
    PackageIdentifier pkgId = PackageIdentifier.createInMainRepo(PathFragment.create("nope"));
    NoSuchPackageException expected;
    try (PackageLoader pkgLoader = newPackageLoader()) {
      expected = assertThrows(NoSuchPackageException.class, () -> pkgLoader.loadPackage(pkgId));
    }
    assertThat(expected)
        .hasMessageThat()
        .startsWith("no such package 'nope': BUILD file not found");
    assertNoEvents(handler.getEvents());
  }

  @Test
  public void simpleBadPackage() throws Exception {
    file("bad/BUILD", "invalidBUILDsyntax");
    PackageIdentifier pkgId = PackageIdentifier.createInMainRepo(PathFragment.create("bad"));
    Package badPkg;
    try (PackageLoader pkgLoader = newPackageLoader()) {
      badPkg = pkgLoader.loadPackage(pkgId);
    }
    assertThat(badPkg.containsErrors()).isTrue();
    assertContainsEvent(handler.getEvents(), "invalidBUILDsyntax");
  }

  @Test
  public void simpleGoodPackage() throws Exception {
    file("good/BUILD", "sh_library(name = 'good')");
    PackageIdentifier pkgId = PackageIdentifier.createInMainRepo(PathFragment.create("good"));
    Package goodPkg;
    try (PackageLoader pkgLoader = newPackageLoader()) {
      goodPkg = pkgLoader.loadPackage(pkgId);
    }
    assertThat(goodPkg.containsErrors()).isFalse();
    assertThat(goodPkg.getTarget("good").getAssociatedRule().getRuleClass())
        .isEqualTo("sh_library");
    assertNoEvents(handler.getEvents());
  }

  @Test
  public void simpleMultipleGoodPackage() throws Exception {
    file("good1/BUILD", "sh_library(name = 'good1')");
    file("good2/BUILD", "sh_library(name = 'good2')");
    PackageIdentifier pkgId1 = PackageIdentifier.createInMainRepo(PathFragment.create("good1"));
    PackageIdentifier pkgId2 = PackageIdentifier.createInMainRepo(PathFragment.create("good2"));
    PackageLoader.Result result;
    try (PackageLoader pkgLoader = newPackageLoader()) {
      result = pkgLoader.loadPackages(ImmutableList.of(pkgId1, pkgId2));
    }
    ImmutableMap<PackageIdentifier, PackageLoader.PackageOrException> pkgs =
        result.getLoadedPackages();

    assertThat(pkgs.get(pkgId1).get().containsErrors()).isFalse();
    assertThat(pkgs.get(pkgId2).get().containsErrors()).isFalse();
    assertThat(pkgs.get(pkgId1).get().getTarget("good1").getAssociatedRule().getRuleClass())
        .isEqualTo("sh_library");
    assertThat(pkgs.get(pkgId2).get().getTarget("good2").getAssociatedRule().getRuleClass())
        .isEqualTo("sh_library");

    assertNoEvents(result.getEvents());
    assertNoEvents(handler.getEvents());
  }

  @Test
  public void testGoodAndBadAndMissingPackages() throws Exception {
    file("bad/BUILD", "invalidBUILDsyntax");
    PackageIdentifier badPkgId = PackageIdentifier.createInMainRepo(PathFragment.create("bad"));

    file("good/BUILD", "sh_library(name = 'good')");
    PackageIdentifier goodPkgId = PackageIdentifier.createInMainRepo(PathFragment.create("good"));

    PackageIdentifier missingPkgId = PackageIdentifier.createInMainRepo("missing");

    PackageLoader.Result result;
    try (PackageLoader pkgLoader = newPackageLoader()) {
      result = pkgLoader.loadPackages(ImmutableList.of(badPkgId, goodPkgId, missingPkgId));
    }

    Package goodPkg = result.getLoadedPackages().get(goodPkgId).get();
    assertThat(goodPkg.containsErrors()).isFalse();

    Package badPkg = result.getLoadedPackages().get(badPkgId).get();
    assertThat(badPkg.containsErrors()).isTrue();

    assertThrows(
        NoSuchPackageException.class, () -> result.getLoadedPackages().get(missingPkgId).get());

    assertContainsEvent(result.getEvents(), "invalidBUILDsyntax");
    assertContainsEvent(handler.getEvents(), "invalidBUILDsyntax");
  }

  @Test
  public void loadPackagesToleratesDuplicates() throws Exception {
    file("good1/BUILD", "sh_library(name = 'good1')");
    PackageIdentifier pkgId = PackageIdentifier.createInMainRepo(PathFragment.create("good1"));
    PackageLoader.Result result;
    try (PackageLoader pkgLoader = newPackageLoader()) {
      result = pkgLoader.loadPackages(ImmutableList.of(pkgId, pkgId));
    }
    ImmutableMap<PackageIdentifier, PackageLoader.PackageOrException> pkgs =
        result.getLoadedPackages();
    assertThat(pkgs.get(pkgId).get().containsErrors()).isFalse();
    assertThat(pkgs.get(pkgId).get().getTarget("good1").getAssociatedRule().getRuleClass())
        .isEqualTo("sh_library");
    assertNoEvents(result.getEvents());
    assertNoEvents(handler.getEvents());
  }

  @Test
  public void simpleGoodPackage_Starlark() throws Exception {
    file("good/good.bzl", "def f(x):", "  native.sh_library(name = x)");
    file("good/BUILD", "load('//good:good.bzl', 'f')", "f('good')");
    PackageIdentifier pkgId = PackageIdentifier.createInMainRepo(PathFragment.create("good"));
    Package goodPkg;
    try (PackageLoader pkgLoader = newPackageLoader()) {
      goodPkg = pkgLoader.loadPackage(pkgId);
    }
    assertThat(goodPkg.containsErrors()).isFalse();
    assertThat(goodPkg.getTarget("good").getAssociatedRule().getRuleClass())
        .isEqualTo("sh_library");
    assertNoEvents(handler.getEvents());
  }

  @Test
  public void externalFile_SupportedByDefault() throws Exception {
    Path externalPath = file(absolutePath("/external/BUILD"), "sh_library(name = 'foo')");
    symlink("foo/BUILD", externalPath);
    PackageIdentifier pkgId = PackageIdentifier.createInMainRepo(PathFragment.create("foo"));
    Package fooPkg;
    try (PackageLoader pkgLoader = newPackageLoader()) {
      fooPkg = pkgLoader.loadPackage(pkgId);
    }
    assertThat(fooPkg.containsErrors()).isFalse();
    assertThat(fooPkg.getTarget("foo").getTargetKind()).isEqualTo("sh_library rule");
    assertNoEvents(handler.getEvents());
  }

  @Test
  public void externalFile_AssumeNonExistentAndImmutable() throws Exception {
    Path externalPath = file(absolutePath("/external/BUILD"), "sh_library(name = 'foo')");
    symlink("foo/BUILD", externalPath);
    PackageIdentifier pkgId = PackageIdentifier.createInMainRepo(PathFragment.create("foo"));
    NoSuchPackageException expected;
    try (PackageLoader pkgLoader =
        newPackageLoaderBuilder()
            .setExternalFileAction(
                ExternalFileAction.ASSUME_NON_EXISTENT_AND_IMMUTABLE_FOR_EXTERNAL_PATHS)
            .build()) {
      expected = assertThrows(NoSuchPackageException.class, () -> pkgLoader.loadPackage(pkgId));
    }
    assertThat(expected).hasMessageThat().contains("no such package 'foo': BUILD file not found");
  }

  @Test
  public void testNonPackageEventsReported() throws Exception {
    path("foo").createDirectoryAndParents();
    symlink("foo/infinitesymlinkpkg", path("foo/infinitesymlinkpkg/subdir"));
    PackageIdentifier pkgId = PackageIdentifier.createInMainRepo("foo/infinitesymlinkpkg");
    PackageLoader.Result result;
    try (PackageLoader pkgLoader = newPackageLoader()) {
      result = pkgLoader.loadPackages(ImmutableList.of(pkgId));
    }
    assertThrows(NoSuchPackageException.class, () -> result.getLoadedPackages().get(pkgId).get());
    assertContainsEvent(result.getEvents(), "infinite symlink expansion detected");
  }

  @Test
  public void testClosesForkJoinPool() throws Exception {
    PackageLoader pkgLoader = newPackageLoader();
    ForkJoinPool forkJoinPool = extractLegacyGlobbingForkJoinPool(pkgLoader);
    assertThat(forkJoinPool.isShutdown()).isFalse();
    pkgLoader.close();
    assertThat(forkJoinPool.isShutdown()).isTrue();
  }

  protected Path path(String rootRelativePath) {
    return workspaceDir.getRelative(PathFragment.create(rootRelativePath));
  }

  protected Path absolutePath(String absolutePath) {
    return fs.getPath(absolutePath);
  }

  protected Path file(String fileName, String... contents) throws Exception {
    return file(path(fileName), contents);
  }

  protected Path file(Path path, String... contents) throws Exception {
    path.getParentDirectory().createDirectoryAndParents();
    FileSystemUtils.writeContentAsLatin1(path, Joiner.on("\n").join(contents));
    return path;
  }

  protected Path symlink(String linkPathString, Path linkTargetPath) throws Exception {
    Path path = path(linkPathString);
    FileSystemUtils.ensureSymbolicLink(path, linkTargetPath);
    return path;
  }
}
