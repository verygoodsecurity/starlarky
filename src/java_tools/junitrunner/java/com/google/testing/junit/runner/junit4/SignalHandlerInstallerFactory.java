// Copyright 2016 The Bazel Authors. All Rights Reserved.
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

import com.google.testing.junit.runner.internal.SignalHandlers;
import com.google.testing.junit.runner.util.Factory;

/**
 * A factory that supplies {@link SignalHandlers.HandlerInstaller}.
 */
public enum SignalHandlerInstallerFactory
    implements Factory<SignalHandlers.HandlerInstaller> {
  INSTANCE;

  @Override
  public SignalHandlers.HandlerInstaller get() {
    SignalHandlers.HandlerInstaller handlerInstaller = JUnit4RunnerModule.signalHandlerInstaller();
    assert handlerInstaller != null;
    return handlerInstaller;
  }

  public static Factory<SignalHandlers.HandlerInstaller> create() {
    return INSTANCE;
  }
}
