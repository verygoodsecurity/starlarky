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
package com.google.devtools.build.lib.remote;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import build.bazel.remote.execution.v2.Digest;
import build.bazel.remote.execution.v2.RequestMetadata;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.devtools.build.lib.actions.ActionInput;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.ArtifactRoot;
import com.google.devtools.build.lib.actions.FileArtifactValue;
import com.google.devtools.build.lib.actions.FileArtifactValue.RemoteFileArtifactValue;
import com.google.devtools.build.lib.actions.MetadataProvider;
import com.google.devtools.build.lib.actions.cache.VirtualActionInput;
import com.google.devtools.build.lib.actions.util.ActionsTestUtil;
import com.google.devtools.build.lib.clock.JavaClock;
import com.google.devtools.build.lib.remote.options.RemoteOptions;
import com.google.devtools.build.lib.remote.util.DigestUtil;
import com.google.devtools.build.lib.remote.util.InMemoryCacheClient;
import com.google.devtools.build.lib.remote.util.StaticMetadataProvider;
import com.google.devtools.build.lib.remote.util.StringActionInput;
import com.google.devtools.build.lib.vfs.DigestHashFunction;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.inmemoryfs.InMemoryFileSystem;
import com.google.devtools.common.options.Options;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link RemoteActionInputFetcher}. */
@RunWith(JUnit4.class)
public class RemoteActionInputFetcherTest {

  private static final DigestHashFunction HASH_FUNCTION = DigestHashFunction.SHA256;

  private Path execRoot;
  private ArtifactRoot artifactRoot;
  private RemoteOptions options;
  private DigestUtil digestUtil;

  @Before
  public void setUp() throws IOException {
    FileSystem fs = new InMemoryFileSystem(new JavaClock(), HASH_FUNCTION);
    execRoot = fs.getPath("/exec");
    execRoot.createDirectoryAndParents();
    artifactRoot = ArtifactRoot.asDerivedRoot(execRoot, "root");
    artifactRoot.getRoot().asPath().createDirectoryAndParents();
    options = Options.getDefaults(RemoteOptions.class);
    digestUtil = new DigestUtil(HASH_FUNCTION);
  }

  @Test
  public void testFetching() throws Exception {
    // arrange
    Map<ActionInput, FileArtifactValue> metadata = new HashMap<>();
    Map<Digest, ByteString> cacheEntries = new HashMap<>();
    Artifact a1 = createRemoteArtifact("file1", "hello world", metadata, cacheEntries);
    Artifact a2 = createRemoteArtifact("file2", "fizz buzz", metadata, cacheEntries);
    MetadataProvider metadataProvider = new StaticMetadataProvider(metadata);
    RemoteCache remoteCache = newCache(options, digestUtil, cacheEntries);
    RemoteActionInputFetcher actionInputFetcher =
        new RemoteActionInputFetcher(remoteCache, execRoot, RequestMetadata.getDefaultInstance());

    // act
    actionInputFetcher.prefetchFiles(metadata.keySet(), metadataProvider);

    // assert
    assertThat(FileSystemUtils.readContent(a1.getPath(), StandardCharsets.UTF_8))
        .isEqualTo("hello world");
    assertThat(a1.getPath().isExecutable()).isTrue();
    assertThat(FileSystemUtils.readContent(a2.getPath(), StandardCharsets.UTF_8))
        .isEqualTo("fizz buzz");
    assertThat(a2.getPath().isExecutable()).isTrue();
    assertThat(actionInputFetcher.downloadedFiles()).hasSize(2);
    assertThat(actionInputFetcher.downloadedFiles()).containsAtLeast(a1.getPath(), a2.getPath());
    assertThat(actionInputFetcher.downloadsInProgress).isEmpty();
  }

  @Test
  public void testStagingVirtualActionInput() throws Exception {
    // arrange
    MetadataProvider metadataProvider = new StaticMetadataProvider(new HashMap<>());
    RemoteCache remoteCache = newCache(options, digestUtil, new HashMap<>());
    RemoteActionInputFetcher actionInputFetcher =
        new RemoteActionInputFetcher(remoteCache, execRoot, RequestMetadata.getDefaultInstance());
    VirtualActionInput a = new StringActionInput("hello world", PathFragment.create("file1"));

    // act
    actionInputFetcher.prefetchFiles(ImmutableList.of(a), metadataProvider);

    // assert
    Path p = execRoot.getRelative(a.getExecPath());
    assertThat(FileSystemUtils.readContent(p, StandardCharsets.UTF_8)).isEqualTo("hello world");
    assertThat(p.isExecutable()).isFalse();
    assertThat(actionInputFetcher.downloadedFiles()).isEmpty();
    assertThat(actionInputFetcher.downloadsInProgress).isEmpty();
  }

  @Test
  public void testFileNotFound() throws Exception {
    // Test that we get an exception if an input file is missing

    // arrange
    Map<ActionInput, FileArtifactValue> metadata = new HashMap<>();
    Artifact a =
        createRemoteArtifact("file1", "hello world", metadata, /* cacheEntries= */ new HashMap<>());
    MetadataProvider metadataProvider = new StaticMetadataProvider(metadata);
    RemoteCache remoteCache = newCache(options, digestUtil, new HashMap<>());
    RemoteActionInputFetcher actionInputFetcher =
        new RemoteActionInputFetcher(remoteCache, execRoot, RequestMetadata.getDefaultInstance());

    // act
    assertThrows(
        BulkTransferException.class,
        () -> actionInputFetcher.prefetchFiles(ImmutableList.of(a), metadataProvider));

    // assert
    assertThat(actionInputFetcher.downloadedFiles()).isEmpty();
    assertThat(actionInputFetcher.downloadsInProgress).isEmpty();
  }

  @Test
  public void testIgnoreNoneRemoteFiles() throws Exception {
    // Test that files that are not remote are not downloaded

    // arrange
    Path p = execRoot.getRelative(artifactRoot.getExecPath()).getRelative("file1");
    FileSystemUtils.writeContent(p, StandardCharsets.UTF_8, "hello world");
    Artifact a = ActionsTestUtil.createArtifact(artifactRoot, p);
    FileArtifactValue f = FileArtifactValue.createForTesting(a);
    MetadataProvider metadataProvider = new StaticMetadataProvider(ImmutableMap.of(a, f));
    RemoteCache remoteCache = newCache(options, digestUtil, new HashMap<>());
    RemoteActionInputFetcher actionInputFetcher =
        new RemoteActionInputFetcher(remoteCache, execRoot, RequestMetadata.getDefaultInstance());

    // act
    actionInputFetcher.prefetchFiles(ImmutableList.of(a), metadataProvider);

    // assert
    assertThat(actionInputFetcher.downloadedFiles()).isEmpty();
    assertThat(actionInputFetcher.downloadsInProgress).isEmpty();
  }

  @Test
  public void testDownloadFile() throws Exception {
    // arrange
    Map<ActionInput, FileArtifactValue> metadata = new HashMap<>();
    Map<Digest, ByteString> cacheEntries = new HashMap<>();
    Artifact a1 = createRemoteArtifact("file1", "hello world", metadata, cacheEntries);
    RemoteCache remoteCache = newCache(options, digestUtil, cacheEntries);
    RemoteActionInputFetcher actionInputFetcher =
        new RemoteActionInputFetcher(remoteCache, execRoot, RequestMetadata.getDefaultInstance());

    // act
    actionInputFetcher.downloadFile(a1.getPath(), metadata.get(a1));

    // assert
    assertThat(FileSystemUtils.readContent(a1.getPath(), StandardCharsets.UTF_8))
        .isEqualTo("hello world");
    assertThat(a1.getPath().isExecutable()).isTrue();
    assertThat(a1.getPath().isReadable()).isTrue();
    assertThat(a1.getPath().isWritable()).isTrue();
  }

  private Artifact createRemoteArtifact(
      String pathFragment,
      String contents,
      Map<ActionInput, FileArtifactValue> metadata,
      Map<Digest, ByteString> cacheEntries) {
    Path p = artifactRoot.getRoot().getRelative(pathFragment);
    Artifact a = ActionsTestUtil.createArtifact(artifactRoot, p);
    byte[] b = contents.getBytes(StandardCharsets.UTF_8);
    HashCode h = HASH_FUNCTION.getHashFunction().hashBytes(b);
    FileArtifactValue f =
        new RemoteFileArtifactValue(h.asBytes(), b.length, /* locationIndex= */ 1, "action-id");
    metadata.put(a, f);
    cacheEntries.put(DigestUtil.buildDigest(h.asBytes(), b.length), ByteString.copyFrom(b));
    return a;
  }

  private static RemoteCache newCache(
      RemoteOptions options, DigestUtil digestUtil, Map<Digest, ByteString> cacheEntries) {
    Map<Digest, byte[]> cacheEntriesByteArray =
        Maps.newHashMapWithExpectedSize(cacheEntries.size());
    for (Map.Entry<Digest, ByteString> entry : cacheEntries.entrySet()) {
      cacheEntriesByteArray.put(entry.getKey(), entry.getValue().toByteArray());
    }
    return new RemoteCache(new InMemoryCacheClient(cacheEntriesByteArray), options, digestUtil);
  }
}
