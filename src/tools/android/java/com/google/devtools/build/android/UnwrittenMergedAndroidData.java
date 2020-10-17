// Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.android;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.android.AndroidDataMerger.SourceChecker;
import com.google.devtools.build.android.AndroidResourceMerger.MergingException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Merged Android Data that has yet to written into a {@link MergedAndroidData}. */
public class UnwrittenMergedAndroidData {

  private final Path manifest;
  private final ParsedAndroidData primary;
  private final ParsedAndroidData transitive;
  private final Set<MergeConflict> conflicts;

  public static UnwrittenMergedAndroidData of(
      Path manifest,
      ParsedAndroidData resources,
      ParsedAndroidData deps,
      Set<MergeConflict> conflicts) {
    return new UnwrittenMergedAndroidData(manifest, resources, deps, conflicts);
  }

  public static UnwrittenMergedAndroidData of(
      Path manifest, ParsedAndroidData resources, ParsedAndroidData deps) {
    return of(manifest, resources, deps, ImmutableSet.of());
  }

  private UnwrittenMergedAndroidData(
      Path manifest,
      ParsedAndroidData primary,
      ParsedAndroidData transitive,
      Set<MergeConflict> conflicts) {
    this.manifest = manifest;
    this.primary = primary;
    this.transitive = transitive;
    this.conflicts = conflicts;
  }

  /**
   * Writes the android data to the filesystem.
   *
   * @param mergedDataWriter Destination writer.
   * @return A MergedAndroidData that is ready for further tool processing.
   * @throws IOException when something goes wrong while writing.
   * @throws MergingException when something goes wrong with the merge.
   */
  public MergedAndroidData write(AndroidDataWriter mergedDataWriter)
      throws IOException, MergingException {
    try {
      primary.writeAssetsTo(mergedDataWriter);
      primary.writeResourcesTo(mergedDataWriter);
      transitive.writeAssetsTo(mergedDataWriter);
      transitive.writeResourcesTo(mergedDataWriter);
      return new MergedAndroidData(
          mergedDataWriter.resourceDirectory(),
          mergedDataWriter.assetDirectory(),
          this.manifest != null ? mergedDataWriter.copyManifest(this.manifest) : null);
    } finally {
      // Flush to make sure all writing is completed before returning a MergedAndroidData.
      // If resources aren't fully written, the MergedAndroidData might be invalid.
      mergedDataWriter.flush();
    }
  }

  public void writeResourceClass(AndroidResourceClassWriter resourceClassWriter)
      throws IOException {
    primary.writeResourcesTo(resourceClassWriter);
    transitive.writeResourcesTo(resourceClassWriter);
    resourceClassWriter.flush();
  }

  void writeRTxt(PlaceholderRTxtWriter rTxtWriter) throws IOException {
    primary.writeResourcesTo(rTxtWriter);
    transitive.writeResourcesTo(rTxtWriter);
    rTxtWriter.flush();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("manifest", manifest)
        .add("primary", primary)
        .add("transitive", transitive)
        .add("conflicts", conflicts)
        .toString();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof UnwrittenMergedAndroidData)) {
      return false;
    }
    UnwrittenMergedAndroidData that = (UnwrittenMergedAndroidData) other;
    return Objects.equals(manifest, that.manifest)
        && Objects.equals(primary, that.primary)
        && Objects.equals(transitive, that.transitive)
        && Objects.equals(conflicts, that.conflicts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(manifest, primary, transitive, conflicts);
  }

  @VisibleForTesting
  Path getManifest() {
    return manifest;
  }

  @VisibleForTesting
  ParsedAndroidData getPrimary() {
    return primary;
  }

  @VisibleForTesting
  ParsedAndroidData getTransitive() {
    return transitive;
  }

  public void serializeTo(AndroidDataSerializer serializer) {
    for (Map.Entry<DataKey, DataAsset> entry : primary.iterateAssetEntries()) {
      serializer.queueForSerialization(entry.getKey(), entry.getValue());
    }
    primary.serializeAssetsTo(serializer);
    primary.serializeResourcesTo(serializer);
  }

  public List<String> asConflictMessagesIfValidWith(SourceChecker checker) throws IOException {
    List<String> messages = new ArrayList<>();
    for (MergeConflict conflict : conflicts) {
      if (conflict.isValidWith(checker)) {
        messages.add(conflict.toConflictMessage());
      }
    }
    return messages;
  }
}
