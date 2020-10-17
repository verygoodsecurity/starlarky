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
package com.google.devtools.build.lib.actions;

import com.google.devtools.build.lib.actions.Artifact.ArtifactExpander;
import com.google.devtools.build.lib.util.Fingerprint;
import javax.annotation.Nullable;

/**
 * An implementation of {@link ActionAnalysisMetadata} that caches its {@linkplain #getKey key} so
 * that it is only computed once.
 */
public abstract class ActionKeyCacher implements ActionAnalysisMetadata {

  @Nullable private volatile String cachedKey = null;

  @Override
  public final String getKey(
      ActionKeyContext actionKeyContext, @Nullable ArtifactExpander artifactExpander) {
    // Only cache the key when it is given all necessary information to compute a correct key.
    // Practically, most of the benefit of the cache comes from execution, which does provide the
    // artifactExpander.
    if (artifactExpander == null) {
      return computeActionKey(actionKeyContext, null);
    }

    if (cachedKey == null) {
      synchronized (this) {
        if (cachedKey == null) {
          cachedKey = computeActionKey(actionKeyContext, artifactExpander);
        }
      }
    }
    return cachedKey;
  }

  private String computeActionKey(
      ActionKeyContext actionKeyContext, @Nullable ArtifactExpander artifactExpander) {
    try {
      Fingerprint fp = new Fingerprint();
      computeKey(actionKeyContext, artifactExpander, fp);

      // Add a bool indicating whether the execution platform was set.
      fp.addBoolean(getExecutionPlatform() != null);
      if (getExecutionPlatform() != null) {
        // Add the execution platform information.
        getExecutionPlatform().addTo(fp);
      }

      fp.addStringMap(getExecProperties());
      // Compute the actual key and store it.
      return fp.hexDigestAndReset();
    } catch (CommandLineExpansionException e) {
      return KEY_ERROR;
    }
  }

  /**
   * See the javadoc for {@link Action} and {@link ActionAnalysisMetadata#getKey} for the contract
   * of this method.
   *
   * <p>TODO(b/150305897): subtypes of this are not consistent about adding the UUID as stated in
   * the ActionAnalysisMetadata. Perhaps ActionKeyCacher should just mandate subclasses provide a
   * UUID and then add that UUID itself in getKey.
   */
  protected abstract void computeKey(
      ActionKeyContext actionKeyContext,
      @Nullable ArtifactExpander artifactExpander,
      Fingerprint fp)
      throws CommandLineExpansionException;
}
