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
package com.google.devtools.build.lib.exec;

import build.bazel.remote.execution.v2.Platform;
import com.google.common.base.Preconditions;
import com.google.common.flogger.GoogleLogger;
import com.google.common.hash.HashCode;
import com.google.devtools.build.lib.actions.ActionContext;
import com.google.devtools.build.lib.actions.ActionInput;
import com.google.devtools.build.lib.actions.ExecException;
import com.google.devtools.build.lib.actions.FileArtifactValue;
import com.google.devtools.build.lib.actions.MetadataProvider;
import com.google.devtools.build.lib.actions.Spawn;
import com.google.devtools.build.lib.actions.SpawnResult;
import com.google.devtools.build.lib.actions.Spawns;
import com.google.devtools.build.lib.actions.cache.VirtualActionInput;
import com.google.devtools.build.lib.analysis.platform.PlatformUtils;
import com.google.devtools.build.lib.exec.Protos.Digest;
import com.google.devtools.build.lib.exec.Protos.File;
import com.google.devtools.build.lib.exec.Protos.SpawnExec;
import com.google.devtools.build.lib.remote.options.RemoteOptions;
import com.google.devtools.build.lib.util.io.MessageOutputStream;
import com.google.devtools.build.lib.vfs.DigestHashFunction;
import com.google.devtools.build.lib.vfs.DigestUtils;
import com.google.devtools.build.lib.vfs.Dirent;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Symlinks;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * A logging utility for spawns.
 */
public class SpawnLogContext implements ActionContext {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private final Path execRoot;
  private final MessageOutputStream executionLog;
  @Nullable private final RemoteOptions remoteOptions;

  public SpawnLogContext(
      Path execRoot, MessageOutputStream executionLog, @Nullable RemoteOptions remoteOptions) {
    this.execRoot = execRoot;
    this.executionLog = executionLog;
    this.remoteOptions = remoteOptions;
  }

  /** Log the executed spawn to the output stream. */
  public void logSpawn(
      Spawn spawn,
      MetadataProvider metadataProvider,
      SortedMap<PathFragment, ActionInput> inputMap,
      Duration timeout,
      SpawnResult result)
      throws IOException, ExecException {
    SortedMap<Path, ActionInput> existingOutputs = listExistingOutputs(spawn);
    SpawnExec.Builder builder = SpawnExec.newBuilder();
    builder.addAllCommandArgs(spawn.getArguments());

    Map<String, String> env = spawn.getEnvironment();
    // Sorting the environment pairs by variable name.
    TreeSet<String> variables = new TreeSet<>(env.keySet());
    for (String var : variables) {
      builder.addEnvironmentVariablesBuilder().setName(var).setValue(env.get(var));
    }

    try {
      for (Map.Entry<PathFragment, ActionInput> e : inputMap.entrySet()) {
        ActionInput input = e.getValue();
        Path inputPath = execRoot.getRelative(input.getExecPathString());
        if (inputPath.isDirectory()) {
          listDirectoryContents(inputPath, builder::addInputs, metadataProvider);
        } else {
          Digest digest = computeDigest(input, null, metadataProvider);
          builder.addInputsBuilder().setPath(input.getExecPathString()).setDigest(digest);
        }
      }
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Error computing spawn inputs");
    }
    ArrayList<String> outputPaths = new ArrayList<>();
    for (ActionInput output : spawn.getOutputFiles()) {
      outputPaths.add(output.getExecPathString());
    }
    Collections.sort(outputPaths);
    builder.addAllListedOutputs(outputPaths);
    for (Map.Entry<Path, ActionInput> e : existingOutputs.entrySet()) {
      Path path = e.getKey();
      if (path.isDirectory()) {
        listDirectoryContents(path, builder::addActualOutputs, metadataProvider);
      } else {
        File.Builder outputBuilder = builder.addActualOutputsBuilder();
        outputBuilder.setPath(path.relativeTo(execRoot).toString());
        try {
          outputBuilder.setDigest(computeDigest(e.getValue(), path, metadataProvider));
        } catch (IOException ex) {
          logger.atWarning().withCause(ex).log("Error computing spawn event output properties");
        }
      }
    }
    builder.setRemotable(Spawns.mayBeExecutedRemotely(spawn));

    Platform execPlatform = PlatformUtils.getPlatformProto(spawn, remoteOptions);
    if (execPlatform != null) {
      builder.setPlatform(buildPlatform(execPlatform));
    }
    if (result.status() != SpawnResult.Status.SUCCESS) {
      builder.setStatus(result.status().toString());
    }
    if (!timeout.isZero()) {
      builder.setTimeoutMillis(timeout.toMillis());
    }
    builder.setCacheable(Spawns.mayBeCached(spawn));
    builder.setExitCode(result.exitCode());
    builder.setRemoteCacheHit(result.isCacheHit());
    builder.setRunner(result.getRunnerName());
    String progressMessage = spawn.getResourceOwner().getProgressMessage();
    if (progressMessage != null) {
      builder.setProgressMessage(progressMessage);
    }
    builder.setMnemonic(spawn.getMnemonic());
    executionLog.write(builder.build());
  }

  public void close() throws IOException {
    executionLog.close();
  }

  private static Protos.Platform buildPlatform(Platform platform) {
    Protos.Platform.Builder platformBuilder = Protos.Platform.newBuilder();
    for (Platform.Property p : platform.getPropertiesList()) {
      platformBuilder.addPropertiesBuilder().setName(p.getName()).setValue(p.getValue());
    }
    return platformBuilder.build();
  }

  private SortedMap<Path, ActionInput> listExistingOutputs(Spawn spawn) {
    TreeMap<Path, ActionInput> result = new TreeMap<>();
    for (ActionInput output : spawn.getOutputFiles()) {
      Path outputPath = execRoot.getRelative(output.getExecPathString());
      // TODO(olaola): once symlink API proposal is implemented, report symlinks here.
      if (outputPath.exists()) {
        result.put(outputPath, output);
      }
    }
    return result;
  }

  private void listDirectoryContents(
      Path path, Consumer<File> addFile, MetadataProvider metadataProvider) {
    try {
      // TODO(olaola): once symlink API proposal is implemented, report symlinks here.
      List<Dirent> sortedDirent = new ArrayList<>(path.readdir(Symlinks.NOFOLLOW));
      sortedDirent.sort(Comparator.comparing(Dirent::getName));
      for (Dirent dirent : sortedDirent) {
        String name = dirent.getName();
        Path child = path.getRelative(name);
        if (dirent.getType() == Dirent.Type.DIRECTORY) {
          listDirectoryContents(child, addFile, metadataProvider);
        } else {
          addFile.accept(
              File.newBuilder()
                  .setPath(child.relativeTo(execRoot).toString())
                  .setDigest(computeDigest(null, child, metadataProvider))
                  .build());
        }
      }
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Error computing spawn event file properties");
    }
  }

  /**
   * Computes the digest of the given ActionInput or corresponding path. Will try to access the
   * Metadata cache first, if it is available, and fall back to digesting the contents manually.
   */
  private Digest computeDigest(
      @Nullable ActionInput input, @Nullable Path path, MetadataProvider metadataProvider)
      throws IOException {
    Preconditions.checkArgument(input != null || path != null);
    DigestHashFunction hashFunction = execRoot.getFileSystem().getDigestFunction();
    Digest.Builder digest = Digest.newBuilder().setHashFunctionName(hashFunction.toString());
    if (input != null) {
      if (input instanceof VirtualActionInput) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ((VirtualActionInput) input).writeTo(buffer);
        byte[] blob = buffer.toByteArray();
        return digest
            .setHash(hashFunction.getHashFunction().hashBytes(blob).toString())
            .setSizeBytes(blob.length)
            .build();
      }
      // Try to access the cached metadata, otherwise fall back to local computation.
      try {
        FileArtifactValue metadata = metadataProvider.getMetadata(input);
        if (metadata != null) {
          byte[] hash = metadata.getDigest();
          if (hash != null) {
            return digest
                .setHash(HashCode.fromBytes(hash).toString())
                .setSizeBytes(metadata.getSize())
                .build();
          }
        }
      } catch (IOException | IllegalStateException e) {
        // Pass through to local computation.
      }
    }
    if (path == null) {
      path = execRoot.getRelative(input.getExecPath());
    }
    // Compute digest manually.
    long fileSize = path.getFileSize();
    return digest
        .setHash(
            HashCode.fromBytes(DigestUtils.getDigestWithManualFallback(path, fileSize)).toString())
        .setSizeBytes(fileSize)
        .build();
  }
}
