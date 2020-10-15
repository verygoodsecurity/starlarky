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

package com.verygood.security.larky.console;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * A {@link Console} that captures the error/warn/info messages preserving the order.
 *
 * <p>All the methods delegate on another {@link Console}.
 *
 * <p>Uses an {@link ArrayList} behind a lock and it's unbounded.
 */
public class CapturingConsole extends DelegateConsole {

  protected static final ImmutableSet<Message.MessageType> ALL_TYPES =
      ImmutableSet.copyOf(Message.MessageType.values());

  private final ArrayList<Message> messages = new ArrayList<>();
  private final Set<Message.MessageType> captureTypes;

  /**
   * Creates a new {@link CapturingConsole} that captures all {@link Message.MessageType}s.
   */
  public static CapturingConsole captureAllConsole(Console delegate) {
    return new CapturingConsole(delegate, ALL_TYPES);
  }

  /**
   * Creates a new {@link CapturingConsole} that captures only the specified {@link Message.MessageType}s.
   */
  public static CapturingConsole captureOnlyConsole(
      Console delegate, Message.MessageType first, Message.MessageType... others) {
    ImmutableSet<Message.MessageType> messageTypes = ImmutableSet.<Message.MessageType>builder()
        .add(first)
        .addAll(Arrays.asList(others))
        .build();
    return new CapturingConsole(delegate, messageTypes);
  }

  protected CapturingConsole(Console delegate, Set<Message.MessageType> captureTypes) {
    super(delegate);
    this.captureTypes = captureTypes;
  }

  public synchronized ImmutableList<Message> getMessages() {
    return ImmutableList.copyOf(messages);
  }

  public synchronized void clearMessages() {
    messages.clear();
  }

  @Override
  protected synchronized void handleMessage(Message.MessageType type, String message) {
    if (captureTypes.contains(type)) {
      messages.add(new Message(type, message));
    }
  }
}
