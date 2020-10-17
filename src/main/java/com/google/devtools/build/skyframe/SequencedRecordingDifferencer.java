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
package com.google.devtools.build.skyframe;

import com.google.common.collect.Iterables;
import com.google.devtools.build.lib.concurrent.ThreadSafety;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A simple Differencer which just records the invalidated values it's been given. */
@ThreadSafety.ThreadCompatible
public class SequencedRecordingDifferencer implements RecordingDifferencer {

  private List<SkyKey> valuesToInvalidate;
  private Map<SkyKey, SkyValue> valuesToInject;

  public SequencedRecordingDifferencer() {
    clear();
  }

  private void clear() {
    valuesToInvalidate = new ArrayList<>();
    valuesToInject = new HashMap<>();
  }

  @Override
  public Diff getDiff(WalkableGraph fromGraph, Version fromVersion, Version toVersion) {
    Diff diff = new ImmutableDiff(valuesToInvalidate, valuesToInject);
    clear();
    return diff;
  }

  @Override
  public void invalidate(Iterable<SkyKey> values) {
    Iterables.addAll(valuesToInvalidate, values);
  }

  @Override
  public void inject(Map<SkyKey, ? extends SkyValue> values) {
    valuesToInject.putAll(values);
  }

  @Override
  public void inject(SkyKey key, SkyValue value) {
    valuesToInject.put(key, value);
  }
}
