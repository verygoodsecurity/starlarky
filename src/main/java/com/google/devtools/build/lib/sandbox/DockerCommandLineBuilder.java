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

package com.google.devtools.build.lib.sandbox;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.runtime.ProcessWrapper;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class DockerCommandLineBuilder {
  private ProcessWrapper processWrapper;
  private Path dockerClient;
  private String imageName;
  private List<String> commandArguments;
  private Path sandboxExecRoot;
  private Map<String, String> environmentVariables;
  private Duration timeout;
  private boolean createNetworkNamespace;
  private UUID uuid;
  private int uid;
  private int gid;
  private String commandId;
  private boolean privileged;
  private List<Map.Entry<String, String>> additionalMounts;

  public DockerCommandLineBuilder setProcessWrapper(ProcessWrapper processWrapper) {
    this.processWrapper = processWrapper;
    return this;
  }

  public DockerCommandLineBuilder setDockerClient(Path dockerClient) {
    this.dockerClient = dockerClient;
    return this;
  }

  public DockerCommandLineBuilder setImageName(String imageName) {
    this.imageName = imageName;
    return this;
  }

  public DockerCommandLineBuilder setCommandArguments(List<String> commandArguments) {
    this.commandArguments = commandArguments;
    return this;
  }

  public DockerCommandLineBuilder setSandboxExecRoot(Path sandboxExecRoot) {
    this.sandboxExecRoot = sandboxExecRoot;
    return this;
  }

  public DockerCommandLineBuilder setEnvironmentVariables(
      Map<String, String> environmentVariables) {
    this.environmentVariables = environmentVariables;
    return this;
  }

  public DockerCommandLineBuilder setTimeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  public DockerCommandLineBuilder setCreateNetworkNamespace(boolean createNetworkNamespace) {
    this.createNetworkNamespace = createNetworkNamespace;
    return this;
  }

  public DockerCommandLineBuilder setUuid(UUID uuid) {
    this.uuid = uuid;
    return this;
  }

  public DockerCommandLineBuilder setUid(int uid) {
    this.uid = uid;
    return this;
  }

  public DockerCommandLineBuilder setGid(int gid) {
    this.gid = gid;
    return this;
  }

  public DockerCommandLineBuilder setCommandId(String commandId) {
    this.commandId = commandId;
    return this;
  }

  public DockerCommandLineBuilder setPrivileged(boolean privileged) {
    this.privileged = privileged;
    return this;
  }

  public DockerCommandLineBuilder setAdditionalMounts(
      List<Map.Entry<String, String>> additionalMounts) {
    this.additionalMounts = additionalMounts;
    return this;
  }

  public List<String> build() {
    Preconditions.checkNotNull(sandboxExecRoot, "sandboxExecRoot must be set");
    Preconditions.checkState(!imageName.isEmpty(), "imageName must be set");
    Preconditions.checkState(!commandArguments.isEmpty(), "commandArguments must be set");

    ImmutableList.Builder<String> dockerCmdLine = ImmutableList.builder();

    dockerCmdLine.add(dockerClient.getPathString());
    dockerCmdLine.add("run");
    dockerCmdLine.add("--rm");
    if (createNetworkNamespace) {
      dockerCmdLine.add("--network=none");
    } else {
      dockerCmdLine.add("--network=host");
    }
    if (privileged) {
      dockerCmdLine.add("--privileged");
    }
    for (Map.Entry<String, String> env : environmentVariables.entrySet()) {
      dockerCmdLine.add("-e", env.getKey() + "=" + env.getValue());
    }
    PathFragment execRootInsideDocker =
        PathFragment.create("/execroot/").getRelative(sandboxExecRoot.getBaseName());
    dockerCmdLine.add(
        "-v", sandboxExecRoot.getPathString() + ":" + execRootInsideDocker.getPathString());
    dockerCmdLine.add("-w", execRootInsideDocker.getPathString());

    for (ImmutableMap.Entry<String, String> additionalMountPath : additionalMounts) {
      final String mountTarget = additionalMountPath.getValue();
      final String mountSource = additionalMountPath.getKey();
      dockerCmdLine.add("-v", mountSource + ":" + mountTarget);
    }

    StringBuilder uidGidFlagBuilder = new StringBuilder();
    if (uid != 0) {
      uidGidFlagBuilder.append(uid);
    }
    if (gid != 0) {
      uidGidFlagBuilder.append(":");
      uidGidFlagBuilder.append(gid);
    }
    String uidGidFlag = uidGidFlagBuilder.toString();
    if (!uidGidFlag.isEmpty()) {
      dockerCmdLine.add("-u", uidGidFlagBuilder.toString());
    }

    if (!commandId.isEmpty()) {
      dockerCmdLine.add("-l", "command_id=" + commandId);
    }
    if (uuid != null) {
      dockerCmdLine.add("--name", uuid.toString());
    }
    dockerCmdLine.add(imageName);
    dockerCmdLine.addAll(commandArguments);

    ProcessWrapper.CommandLineBuilder processWrapperCmdLine =
        processWrapper.commandLineBuilder(dockerCmdLine.build());
    if (timeout != null) {
      processWrapperCmdLine.setTimeout(timeout);
    }
    return processWrapperCmdLine.build();
  }
}
