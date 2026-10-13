/*
 * Copyright 2026 Very Good Security Authors
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

package com.verygood.security.run.repl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/** Where the REPL reads its inputs from: a line editor on a terminal, or plain lines. */
public interface ReplInput extends AutoCloseable {

  String PROMPT = ">>> ";
  String CONTINUATION_PROMPT = "... ";

  /** Thrown when the user cancels the input being typed (Ctrl-C). */
  final class CancelledException extends Exception {
    public CancelledException() {
      super("cancelled", null, false, false);
    }
  }

  /**
   * Reads the next complete input: a REPL command, or code that {@link ReplSession#isComplete}
   * accepts. Returns null at the end of the input.
   */
  @Nullable
  String read() throws IOException, CancelledException;

  @Override
  default void close() {}

  /** Whether {@code text} is a whole input: a one-line :command, or complete code. */
  static boolean isWholeInput(String text, Predicate<String> isCompleteCode) {
    return (text.stripLeading().startsWith(":") && !text.contains("\n"))
        || isCompleteCode.test(text);
  }

  /** Reads lines from a reader, joining them until the input is complete. */
  final class Plain implements ReplInput {
    private final BufferedReader in;
    @Nullable private final PrintWriter prompts;
    private final Predicate<String> isCompleteCode;

    /** {@code prompts} is where to print prompts, or null for none (input is not a terminal). */
    public Plain(BufferedReader in, @Nullable PrintWriter prompts, Predicate<String> isCompleteCode) {
      this.in = in;
      this.prompts = prompts;
      this.isCompleteCode = isCompleteCode;
    }

    @Override
    @Nullable
    public String read() throws IOException {
      StringBuilder text = null;
      while (true) {
        if (prompts != null) {
          prompts.print(text == null ? PROMPT : CONTINUATION_PROMPT);
          prompts.flush();
        }
        String line = in.readLine();
        if (line == null) {
          // At the end of the input, run what was typed, even if it is incomplete.
          return text == null ? null : text.toString();
        }
        if (text == null) {
          if (line.isBlank()) {
            continue;
          }
          text = new StringBuilder(line);
        } else {
          text.append('\n').append(line);
        }
        if (isWholeInput(text.toString(), isCompleteCode)) {
          return text.toString();
        }
      }
    }
  }
}
