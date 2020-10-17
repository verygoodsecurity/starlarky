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
package com.google.devtools.build.lib.util.io;

import java.io.IOException;

/**
 * Wrap an {@link AnsiTerminalWriter} into one that is aware of the position
 * within the current line. Newline characters, which presumably are supposed
 * to end a line, are translated into calls to the {@link AnsiTerminalWriter#newline()}
 * method.
 */
public class PositionAwareAnsiTerminalWriter implements AnsiTerminalWriter {

  private final AnsiTerminalWriter terminalWriter;
  private int position;

  public PositionAwareAnsiTerminalWriter(AnsiTerminalWriter terminalWriter) {
    this.terminalWriter = terminalWriter;
    this.position = 0;
  }

  @Override
  public AnsiTerminalWriter append(String text) throws IOException {
    int i = 0;
    while (i < text.length()) {
       int next = text.indexOf('\n', i);
       if (next == -1) {
         terminalWriter.append(text.substring(i));
         position += text.length() - i;
         i = text.length();
       } else {
         terminalWriter.append(text.substring(i, next));
         terminalWriter.newline();
         i = next + 1;
         position = 0;
       }
    }

    return this;
  }

  @Override
  public AnsiTerminalWriter newline() throws IOException {
    terminalWriter.newline();
    position = 0;
    return this;
  }

  @Override
  public AnsiTerminalWriter okStatus() throws IOException {
    terminalWriter.okStatus();
    return this;
  }

  @Override
  public AnsiTerminalWriter failStatus() throws IOException {
    terminalWriter.failStatus();
    return this;
  }

  @Override
  public AnsiTerminalWriter normal() throws IOException {
    terminalWriter.normal();
    return this;
  }

  public int getPosition() {
    return position;
  }
}
