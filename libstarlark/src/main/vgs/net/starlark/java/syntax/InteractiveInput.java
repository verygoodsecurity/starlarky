// Copyright 2026 Very Good Security Authors. All rights reserved.
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
package net.starlark.java.syntax;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides whether the text typed so far at an interactive prompt is a complete input, as the
 * Python and go.starlark.net REPLs do.
 *
 * <p>The text needs more lines when it ends inside an open bracket, an unclosed triple-quoted
 * string or after a line-continuation backslash (whatever the last line is), or when it opens a
 * block: its last line ends with {@code :}, or it parses and contains an indented block or ends in
 * a {@code def}/{@code if}/{@code for}. A block, as in Python, ends at a blank line. Any other text
 * is complete, including text with syntax errors, which the caller reports when it parses it.
 */
public final class InteractiveInput {

  private InteractiveInput() {}

  /**
   * Returns whether {@code text}, the lines typed so far joined by {@code \n} (so text ending in
   * {@code \n} ends with a blank line), is complete.
   */
  public static boolean isComplete(String text, FileOptions options) {
    if (text.isBlank()) {
      return true;
    }
    String src = text + "\n";
    List<SyntaxError> errors = new ArrayList<>();
    Lexer lexer = new Lexer(ParserInput.fromString(src, "<input>"), errors, options);
    int depth = 0;
    boolean unclosedTripleQuote = false;
    boolean indented = false;
    TokenKind last = null;
    int lastEnd = 0;
    while (true) {
      int errorsBefore = errors.size();
      lexer.nextToken();
      TokenKind kind = lexer.kind;
      if (kind == TokenKind.EOF) {
        break;
      }
      switch (kind) {
        case LPAREN, LBRACKET, LBRACE -> depth++;
        case RPAREN, RBRACKET, RBRACE -> depth = Math.max(0, depth - 1);
        case INDENT -> indented = true;
        default -> {}
      }
      if ((kind == TokenKind.STRING || kind == TokenKind.BYTE)
          && errors.size() > errorsBefore
          && opensTripleQuote(src, lexer.start)) {
        unclosedTripleQuote = true;
      }
      if (kind != TokenKind.NEWLINE
          && kind != TokenKind.INDENT
          && kind != TokenKind.OUTDENT
          && kind != TokenKind.DOC_COMMENT_BLOCK
          && kind != TokenKind.DOC_COMMENT_TRAILING) {
        last = kind;
        lastEnd = lexer.end;
      }
    }
    if (depth > 0 || unclosedTripleQuote || endsWithContinuation(text, lastEnd)) {
      return false;
    }
    if (lastLine(text).isBlank()) {
      return true; // a blank line ends a block
    }
    if (last == TokenKind.COLON) {
      return false; // a block's header, awaiting its body
    }
    StarlarkFile file = StarlarkFile.parse(ParserInput.fromString(src, "<input>"), options);
    if (!file.ok() || file.getStatements().isEmpty()) {
      return true;
    }
    Statement.Kind kind = file.getStatements().get(file.getStatements().size() - 1).kind();
    return !(indented
        || kind == Statement.Kind.DEF
        || kind == Statement.Kind.IF
        || kind == Statement.Kind.FOR);
  }

  // Whether the string token starting at 'start' (after any r/b/rb prefix) opens with a triple
  // quote.
  private static boolean opensTripleQuote(String src, int start) {
    int i = start;
    while (i < src.length() && (src.charAt(i) == 'r' || src.charAt(i) == 'b')) {
      i++;
    }
    return src.startsWith("\"\"\"", i) || src.startsWith("'''", i);
  }

  // Whether the text ends with a backslash that is not part of a token (a line continuation).
  private static boolean endsWithContinuation(String text, int lastTokenEnd) {
    int i = text.length() - 1;
    while (i >= 0 && (text.charAt(i) == ' ' || text.charAt(i) == '\t' || text.charAt(i) == '\r')) {
      i--;
    }
    return i >= 0 && text.charAt(i) == '\\' && i >= lastTokenEnd;
  }

  private static String lastLine(String text) {
    return text.substring(text.lastIndexOf('\n') + 1);
  }
}
