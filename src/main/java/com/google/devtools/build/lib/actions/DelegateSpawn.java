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

package com.google.devtools.build.lib.actions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.analysis.platform.PlatformInfo;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import java.util.Collection;
import javax.annotation.Nullable;

/**
 * A delegating spawn that allow us to overwrite certain methods while maintaining the original
 * behavior for non-overwritten methods.
 */
public class DelegateSpawn implements Spawn {

  private final Spawn spawn;

  public DelegateSpawn(Spawn spawn){
    this.spawn = spawn;
  }

  @Override
  public ImmutableMap<String, String> getExecutionInfo() {
    return spawn.getExecutionInfo();
  }

  @Override
  public RunfilesSupplier getRunfilesSupplier() {
    return spawn.getRunfilesSupplier();
  }

  @Override
  public ImmutableList<String> getArguments() {
    return spawn.getArguments();
  }

  @Override
  public ImmutableMap<String, String> getEnvironment() {
    return spawn.getEnvironment();
  }

  @Override
  public ImmutableMap<Artifact, ImmutableList<FilesetOutputSymlink>> getFilesetMappings() {
    return spawn.getFilesetMappings();
  }

  @Override
  public NestedSet<? extends ActionInput> getToolFiles() {
    return spawn.getToolFiles();
  }

  @Override
  public NestedSet<? extends ActionInput> getInputFiles() {
    return spawn.getInputFiles();
  }

  @Override
  public Collection<? extends ActionInput> getOutputFiles() {
    return spawn.getOutputFiles();
  }

  @Override
  public ActionExecutionMetadata getResourceOwner() {
    return spawn.getResourceOwner();
  }

  @Override
  public ResourceSet getLocalResources() {
    return spawn.getLocalResources();
  }

  @Override
  public String getMnemonic() {
    return spawn.getMnemonic();
  }

  @Override
  public ImmutableMap<String, String> getCombinedExecProperties() {
    return spawn.getCombinedExecProperties();
  }

  @Override
  @Nullable
  public PlatformInfo getExecutionPlatform() {
    return spawn.getExecutionPlatform();
  }
}
