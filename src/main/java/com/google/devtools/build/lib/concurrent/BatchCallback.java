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
package com.google.devtools.build.lib.concurrent;

import com.google.devtools.build.lib.concurrent.ThreadSafety.ThreadSafe;

/**
 * Callback to be invoked when part of a result has been computed. Allows a client interested in
 * the result to process it as it is computed, for instance by streaming it, if it is too big to
 * fit in memory.
 */
@ThreadSafe
public interface BatchCallback<T, E extends Exception> {
  /**
   * Called when part of a result has been computed.
   *
   * <p>Note that this method can be called several times for a single {@code BatchCallback}.
   * Implementations should assume that multiple calls can happen.
   *
   * @param partialResult Part of the result. May contain duplicates, either in the same call or
   * across calls.
   */
  void process(Iterable<T> partialResult) throws E, InterruptedException;

  /** {@link BatchCallback} that does precisely nothing. */
  class NullCallback<T> implements BatchCallback<T, RuntimeException> {
    private static final NullCallback<Object> INSTANCE = new NullCallback<>();

    @Override
    public void process(Iterable<T> partialResult) {}

    @SuppressWarnings("unchecked")
    public static <T> NullCallback<T> instance() {
      return (NullCallback<T>) INSTANCE;
    }
  }
}
