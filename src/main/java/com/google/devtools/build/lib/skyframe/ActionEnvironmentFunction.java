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

package com.google.devtools.build.lib.skyframe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Interner;
import com.google.devtools.build.lib.concurrent.BlazeInterners;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.skyframe.AbstractSkyKey;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Skyframe function that provides the effective value for a client environment variable. This will
 * either be the value coming from the default client environment, or the value coming from the
 * --action_env flag, if the variable's value is explicitly set.
 */
public final class ActionEnvironmentFunction implements SkyFunction {

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }

  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env) throws InterruptedException {
    Map<String, String> actionEnv = PrecomputedValue.ACTION_ENV.get(env);
    String key = (String) skyKey.argument();
    if (actionEnv.containsKey(key) && actionEnv.get(key) != null) {
      return new ClientEnvironmentValue(actionEnv.get(key));
    }
    return env.getValue(ClientEnvironmentFunction.key(key));
  }

  /** @return the SkyKey to invoke this function for the environment variable {@code variable}. */
  public static Key key(String variable) {
    return Key.create(variable);
  }

  @AutoCodec.VisibleForSerialization
  @AutoCodec
  static class Key extends AbstractSkyKey<String> {
    private static final Interner<Key> interner = BlazeInterners.newWeakInterner();

    private Key(String arg) {
      super(arg);
    }

    @AutoCodec.VisibleForSerialization
    @AutoCodec.Instantiator
    static Key create(String arg) {
      return interner.intern(new Key(arg));
    }

    @Override
    public SkyFunctionName functionName() {
      return SkyFunctions.ACTION_ENVIRONMENT_VARIABLE;
    }
  }

  /**
   * Returns a map of environment variable key => values, getting them from Skyframe. Returns null
   * if and only if some dependencies from Skyframe still need to be resolved.
   */
  public static Map<String, String> getEnvironmentView(Environment env, Iterable<String> keys)
      throws InterruptedException {
    ImmutableList.Builder<SkyKey> skyframeKeysBuilder = ImmutableList.builder();
    for (String key : keys) {
      skyframeKeysBuilder.add(key(key));
    }
    ImmutableList<SkyKey> skyframeKeys = skyframeKeysBuilder.build();
    Map<SkyKey, SkyValue> values = env.getValues(skyframeKeys);
    if (env.valuesMissing()) {
      return null;
    }
    // To return the initial order and support null values, we use a LinkedHashMap.
    LinkedHashMap<String, String> result = new LinkedHashMap<>();
    for (SkyKey key : skyframeKeys) {
      ClientEnvironmentValue value = (ClientEnvironmentValue) values.get(key);
      result.put(key.argument().toString(), value.getValue());
    }
    return Collections.unmodifiableMap(result);
  }
}
