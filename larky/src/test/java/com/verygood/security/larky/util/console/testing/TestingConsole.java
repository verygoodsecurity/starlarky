/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.util.console.testing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.verygood.security.larky.util.console.AnsiColor;
import com.verygood.security.larky.util.console.CapturingConsole;
import com.verygood.security.larky.util.console.LogConsole;
import com.verygood.security.larky.util.console.Message.MessageType;

import java.util.ArrayDeque;

import static com.google.common.truth.Truth.assertAbout;
import static com.verygood.security.larky.util.console.Message.MessageType.ERROR;
import static com.verygood.security.larky.util.console.Message.MessageType.INFO;
import static com.verygood.security.larky.util.console.Message.MessageType.PROGRESS;
import static com.verygood.security.larky.util.console.Message.MessageType.PROMPT;
import static com.verygood.security.larky.util.console.Message.MessageType.WARNING;

/**
 * A testing console that allows programming the user input and deletages on a
 * {@link CapturingConsole} to intercept all the messages.
 *
 * <p>It also writes the output to a {@link LogConsole} for debug.
 */
public final class TestingConsole extends CapturingConsole {

  private enum PromptResponse {
    YES, NO,
  }

  private final CapturingConsole outputConsole =
      captureAllConsole(LogConsole.writeOnlyConsole(System.out, /*verbose=*/true));
  private final ArrayDeque<PromptResponse> programmedResponses = new ArrayDeque<>();

  public TestingConsole() {
    super(captureAllConsole(
        LogConsole.writeOnlyConsole(System.out, /*verbose=*/ true)), ALL_TYPES);
  }

  private static final ImmutableSet<MessageType> ALL_BUT_VERBOSE =
      ImmutableSet.of(ERROR, INFO, WARNING, PROGRESS, PROMPT);

  public TestingConsole(boolean verbose) {
    super(captureAllConsole(
        LogConsole.writeOnlyConsole(System.out, verbose)), ALL_BUT_VERBOSE);
  }

  public TestingConsole respondYes() {
    this.programmedResponses.addLast(PromptResponse.YES);
    return this;
  }

  public TestingConsole respondNo() {
    this.programmedResponses.addLast(PromptResponse.NO);
    return this;
  }

  /**
   * Returns a truth subject that provides fluent methods for assertions on this instance.
   *
   * <p>For example:
   *
   *     testConsole.assertThat()
   *       .matchesNext(...)
   *       .equalsNext(...)
   *       .containsNoMoreMessages();
   */
  public LogSubjects.LogSubject assertThat() {
    return assertAbout(LogSubjects.CONSOLE_SUBJECT_FACTORY)
        .that(this);
  }

  @Override
  public boolean promptConfirmation(String message) {
    Preconditions.checkState(!programmedResponses.isEmpty(), "No more programmed responses.");
    // Validate prompt messages with WARN level in tests
    warn(message);
    return programmedResponses.removeFirst() == PromptResponse.YES;
  }

  @Override
  public String colorize(AnsiColor ansiColor, String message) {
    return outputConsole.colorize(ansiColor, message);
  }

  /**
   * Clear messages
   */
  public void reset() {
    clearMessages();
  }
}
