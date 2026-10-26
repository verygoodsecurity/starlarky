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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Buffer;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.CompletingParsedLine;
import org.jline.reader.EOFError;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Reads inputs with a JLine line editor: editing, a persistent history and tab completion. A
 * multi-line input is edited as one buffer: Enter on an incomplete input starts a new, indented
 * line (see {@link Python}), and the history keeps the whole input.
 */
final class JLineInput implements ReplInput {

  private static final String INDENT_OR_COMPLETE = "larky-indent-or-complete";

  private final Terminal terminal;
  private final LineReader reader;

  JLineInput(Terminal terminal, ReplSession session, @Nullable Path historyFile) {
    this.terminal = terminal;
    LineReaderBuilder builder =
        LineReaderBuilder.builder()
            .terminal(terminal)
            .appName("larky")
            .parser(new Python(session))
            .completer(new ReplCompletions(session))
            .variable(LineReader.SECONDARY_PROMPT_PATTERN, CONTINUATION_PROMPT)
            // EOFError's "open brackets" carry the number of spaces to indent the next line by.
            .variable(LineReader.INDENTATION, 1)
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true) // '!' is not history
            .option(LineReader.Option.INSERT_TAB, false);
    if (historyFile != null) {
      builder.variable(LineReader.HISTORY_FILE, historyFile);
    }
    reader = builder.build();
    // Tab at the start of a line indents (Starlark does not allow tabs there); elsewhere it
    // completes.
    reader.getWidgets().put(INDENT_OR_COMPLETE, this::indentOrComplete);
    for (String keymap : List.of(LineReader.EMACS, LineReader.VIINS)) {
      KeyMap<Binding> map = reader.getKeyMaps().get(keymap);
      if (map != null) {
        map.bind(new Reference(INDENT_OR_COMPLETE), "\t");
      }
    }
  }

  /**
   * The terminal of this process, or null when standard input is not an interactive terminal
   * (e.g. it is piped). Uses JLine's exec provider (stty), which needs no native library.
   */
  @Nullable
  static Terminal systemTerminal() {
    if (System.console() == null) {
      return null;
    }
    try {
      Terminal terminal =
          TerminalBuilder.builder()
              .name("larky")
              .system(true)
              .jni(false)
              .jna(false)
              .jansi(false)
              .ffm(false)
              .exec(true)
              .dumb(true)
              .build();
      if (terminal.getType().startsWith(Terminal.TYPE_DUMB)) {
        terminal.close();
        return null;
      }
      return terminal;
    } catch (IOException | RuntimeException e) {
      return null;
    }
  }

  Terminal terminal() {
    return terminal;
  }

  @Override
  @Nullable
  public String read() throws CancelledException {
    try {
      return reader.readLine(PROMPT);
    } catch (UserInterruptException e) {
      throw new CancelledException();
    } catch (EndOfFileException e) {
      return null;
    }
  }

  @Override
  public void close() {
    try {
      reader.getHistory().save();
    } catch (IOException | RuntimeException e) {
      // best effort
    }
    try {
      terminal.close();
    } catch (IOException e) {
      // ignore
    }
  }

  private boolean indentOrComplete() {
    Buffer buffer = reader.getBuffer();
    String text = buffer.toString();
    int cursor = buffer.cursor();
    int lineStart = text.lastIndexOf('\n', cursor - 1) + 1;
    if (text.substring(lineStart, cursor).isBlank()) {
      buffer.write("    ");
      return true;
    }
    reader.callWidget(LineReader.COMPLETE_WORD);
    return true;
  }

  /** The number of spaces to indent the line after {@code text}, an incomplete input, by. */
  static int nextIndent(String text) {
    String last = text.substring(text.lastIndexOf('\n') + 1);
    int indent = 0;
    while (indent < last.length() && last.charAt(indent) == ' ') {
      indent++;
    }
    String code = last.strip();
    int comment = code.indexOf('#');
    if (comment >= 0 && code.indexOf('\'') < 0 && code.indexOf('"') < 0) {
      code = code.substring(0, comment).strip();
    }
    return code.endsWith(":") ? indent + 4 : indent;
  }

  /** Splits a buffer for completion, and asks for more lines while its code is incomplete. */
  private static final class Python implements Parser {
    private final ReplSession session;

    Python(ReplSession session) {
      this.session = session;
    }

    @Override
    public ParsedLine parse(String line, int cursor, ParseContext context) throws EOFError {
      if (context == ParseContext.ACCEPT_LINE
          && !ReplInput.isWholeInput(line, session::isComplete)) {
        throw new EOFError(-1, -1, "incomplete input", null, nextIndent(line), null);
      }
      int start = ReplCompleter.wordStart(line, cursor);
      return new Line(line, cursor, line.substring(start, cursor));
    }
  }

  /** A buffer whose word is the name, label or symbol being completed at the cursor. */
  private record Line(String line, int cursor, String word) implements CompletingParsedLine {
    @Override
    public int wordCursor() {
      return word.length();
    }

    @Override
    public CharSequence escape(CharSequence candidate, boolean complete) {
      return candidate; // candidates are inserted as they are
    }

    @Override
    public int rawWordCursor() {
      return word.length();
    }

    @Override
    public int rawWordLength() {
      return word.length();
    }

    @Override
    public int wordIndex() {
      return 0;
    }

    @Override
    public List<String> words() {
      return List.of(word);
    }
  }

  private static final class ReplCompletions implements Completer {
    private final ReplSession session;

    ReplCompletions(ReplSession session) {
      this.session = session;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
      for (String c : ReplCompleter.complete(session, line.line(), line.cursor()).candidates()) {
        // complete=false: no space after the completed name
        candidates.add(new Candidate(c, c, null, null, null, null, false));
      }
    }
  }
}
