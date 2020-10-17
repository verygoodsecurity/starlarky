// Copyright 2012 The Bazel Authors. All Rights Reserved.
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

package com.google.testing.junit.runner.junit4;

import static com.google.testing.junit.runner.sharding.ShardingFilters.DEFAULT_SHARDING_STRATEGY;

import com.google.testing.junit.runner.internal.Stdout;
import com.google.testing.junit.runner.internal.junit4.MemoizingRequest;
import com.google.testing.junit.runner.model.TestSuiteModel;
import com.google.testing.junit.runner.sharding.api.ShardingFilterFactory;
import com.google.testing.junit.runner.util.MemoizingSupplier;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.Supplier;
import javax.inject.Singleton;
import org.junit.internal.TextListener;
import org.junit.runner.Request;
import org.junit.runner.notification.RunListener;

/**
 * Utility class for creating a {@link JUnit4Runner}. This contains the common bindings used when
 * either the runner runs actual tests or when we do integration tests of the runner itself.
 * This is a legacy Dagger module.
 */
public abstract class JUnit4RunnerBaseModule {

  abstract Set<JUnit4Runner.Initializer> initializers();
  
  static ShardingFilterFactory shardingFilterFactory() {
    return DEFAULT_SHARDING_STRATEGY;
  }

  static RunListener textListener(TextListener impl) {
    return impl;
  }

  @Singleton
  static Supplier<TestSuiteModel> provideTestSuiteModelSupplier(JUnit4TestModelBuilder builder) {
    return new MemoizingSupplier<>(builder);
  }

  @Singleton
  static TextListener provideTextListener(@Stdout PrintStream testRunnerOut) {
    return new TextListener(asUtf8PrintStream(testRunnerOut));
  }

  private static PrintStream asUtf8PrintStream(OutputStream stream) {
    try {
      return new PrintStream(stream, false /* autoFlush */, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("UTF-8 must be supported as per the java language spec", e);
    }
  }

  @Singleton
  static Request provideRequest(@TopLevelSuite Class<?> suiteClass) {
    /*
     * JUnit4Runner requests the Runner twice, once to build the model (before
     * filtering) and once to run the tests (after filtering). Constructing the
     * Runner can be expensive, so Memoize the Runner.
     *
     * <p>Note that as of JUnit 4.11, Request.aClass() will memoize the runner,
     * but users of Bazel might use an earlier version of JUnit, so to be safe
     * we keep the memoization here.
     */
    Request request = Request.aClass(suiteClass);
    return new MemoizingRequest(request);
  }

  private JUnit4RunnerBaseModule() {} // no instances
}
