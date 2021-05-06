// Copyright 2006 The Bazel Authors. All Rights Reserved.
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests of tokenization behavior of the {@link Lexer}.
 */
@RunWith(JUnit4.class)
public class LexerTest {

  // TODO(adonovan): make these these tests less unnecessarily stateful.

  private final List<SyntaxError> errors = new ArrayList<>();

  /**
   * Create a lexer which takes input from the specified string. Resets the
   * error handler beforehand.
   */
  private Lexer createLexer(String input) {
    ParserInput inputSource = ParserInput.fromString(input, "");
    errors.clear();
    return new Lexer(inputSource, FileOptions.DEFAULT, errors);
  }

  private static class Token {
    TokenKind kind;
    int start;
    int end;
    Object value;

    @Override
    public String toString() {
      return kind == TokenKind.STRING
          ? "\"" + value + "\""
          : value == null ? kind.toString() : value.toString();
    }
  }

  private ArrayList<Token> allTokens(Lexer lexer) {
    ArrayList<Token> result = new ArrayList<>();
    do {
      lexer.nextToken();
      Token tok = new Token();
      tok.kind = lexer.kind;
      tok.start = lexer.start;
      tok.end = lexer.end;
      tok.value = lexer.value;
      result.add(tok);
    } while (lexer.kind != TokenKind.EOF);
    return result;
  }

  private Token[] tokens(String input) {
    ArrayList<Token> result = allTokens(createLexer(input));
    return result.toArray(new Token[0]);
  }

  /**
   * Lexes the specified input string, and returns a string containing just the line numbers of each
   * token.
   */
  private String linenums(String input) {
    Lexer lexer = createLexer(input);
    StringBuilder buf = new StringBuilder();
    for (Token tok : allTokens(lexer)) {
      if (buf.length() > 0) {
        buf.append(' ');
      }
      int line = lexer.locs.getLocation(tok.start).line();
      buf.append(line);
    }
    return buf.toString();
  }

  /**
   * Returns a string containing the names of the tokens and their associated
   * values. (String-literals are printed without escaping.)
   */
  private static String values(Token[] tokens) {
    StringBuilder buffer = new StringBuilder();
    for (Token token : tokens) {
      if (buffer.length() > 0) {
        buffer.append(' ');
      }
      buffer.append(token.kind.name());
      if (token.value != null) {
        buffer.append('(').append(token.value).append(')');
      }
    }
    return buffer.toString();
  }

  // Scans src, and asserts that the tokens match wantTokens
  // and that there are no errors.
  private void check(String src, String wantTokens) {
    assertThat(values(tokens(src))).isEqualTo(wantTokens);
    assertThat(errors).isEmpty();
  }

  // Scans src, and asserts that the tokens match wantTokens
  // and the errors match wantErrors.
  // Errors are formatted with a caret ^ under the errant column.
  private void checkErrors(String src, String wantTokens, String... wantErrors) {
    assertThat(values(tokens(src))).isEqualTo(wantTokens);

    List<String> gotErrors = new ArrayList<>();
    for (SyntaxError err : errors) {
      String msg = spaces(err.location().column() - 1) + "^ " + err.message();
      if (err.location().line() != 1) {
        msg = String.format("%s (line %d)", msg, err.location().line());
      }
      gotErrors.add(msg);
    }
    assertThat(gotErrors).isEqualTo(Arrays.asList(wantErrors));
  }

  private static String spaces(int n) {
    return new String(new char[n]).replace('\0', ' ');
  }

  /**
   * Returns a string containing just the half-open position intervals of each
   * token. e.g. "[3,4) [4,9)".
   */
  private static String positions(Token[] tokens) {
    StringBuilder buf = new StringBuilder();
    for (Token tok : tokens) {
      if (buf.length() > 0) {
        buf.append(' ');
      }
      buf.append('[').append(tok.start).append(',').append(tok.end).append(')');
    }
    return buf.toString();
  }

  @Test
  public void testBasics1() throws Exception {
    checkErrors(
        "wiz) ", //
        "IDENTIFIER(wiz) RPAREN NEWLINE EOF",
        "   ^ indentation error");
    checkErrors(
        "wiz )", //
        "IDENTIFIER(wiz) RPAREN NEWLINE EOF",
        "    ^ indentation error");
    checkErrors(
        " wiz)", //
        "INDENT IDENTIFIER(wiz) RPAREN NEWLINE OUTDENT NEWLINE EOF",
        "    ^ indentation error");
    checkErrors(
        " wiz ) ", //
        "INDENT IDENTIFIER(wiz) RPAREN NEWLINE OUTDENT NEWLINE EOF",
        "     ^ indentation error");
    checkErrors(
        "wiz\t)", //
        "IDENTIFIER(wiz) RPAREN NEWLINE EOF",
        "    ^ indentation error");
  }

  @Test
  public void testBasics2() throws Exception {
    checkErrors(
        ")", //
        "RPAREN NEWLINE EOF",
        "^ indentation error");
    checkErrors(
        " )", //
        "INDENT RPAREN NEWLINE OUTDENT NEWLINE EOF",
        " ^ indentation error");
    checkErrors(
        " ) ", //
        "INDENT RPAREN NEWLINE OUTDENT NEWLINE EOF",
        " ^ indentation error");
    checkErrors(
        ") ", //
        "RPAREN NEWLINE EOF",
        "^ indentation error");
  }

  @Test
  public void testBasics3() throws Exception {
    check("123#456\n789", "INT(123) NEWLINE INT(789) NEWLINE EOF");
    check("123 #456\n789", "INT(123) NEWLINE INT(789) NEWLINE EOF");
    check("123#456 \n789", "INT(123) NEWLINE INT(789) NEWLINE EOF");
    check("123#456\n 789", "INT(123) NEWLINE INDENT INT(789) NEWLINE OUTDENT NEWLINE EOF");
    check("123#456\n789 ", "INT(123) NEWLINE INT(789) NEWLINE EOF");
  }

  private static String zeroes(int n) {
    return new String(new char[n]).replace('\0', '0');
  }

  @Test
  public void testBasics4() throws Exception {
    check("", "NEWLINE EOF");
    check("# foo", "NEWLINE EOF");
    check("1 2 3 4", "INT(1) INT(2) INT(3) INT(4) NEWLINE EOF");
    check("1.234", "FLOAT(1.234) NEWLINE EOF");
    check(
        "foo(bar, wiz)",
        "IDENTIFIER(foo) LPAREN IDENTIFIER(bar) COMMA IDENTIFIER(wiz) RPAREN NEWLINE EOF");
    check("1.0e308 1" + zeroes(308) + ".0", "FLOAT(1.0E308) FLOAT(1.0E308) NEWLINE EOF");
    checkErrors(
        "1.0e309 1" + zeroes(309) + ".0",
        "FLOAT(Infinity) FLOAT(Infinity) NEWLINE EOF",
        "^ floating-point literal too large",
        "        ^ floating-point literal too large");
  }

  @Test
  public void testNoWhiteSpaceBetweenTokens() throws Exception {
    check("6or()", "INT(6) OR LPAREN RPAREN NEWLINE EOF");
    check("0in(''and[])", "INT(0) IN LPAREN STRING() AND LBRACKET RBRACKET RPAREN NEWLINE EOF");

    checkErrors(
        "0or()",
        "INT(0) IDENTIFIER(r) LPAREN RPAREN NEWLINE EOF",
        "^ invalid base-8 integer literal: 0o");
  }

  @Test
  public void testNonAsciiIdentifiers() throws Exception {
    checkErrors(
        "ümlaut", //
        "IDENTIFIER(mlaut) NEWLINE EOF",
        "^ invalid character: 'ü'");
    checkErrors(
        "umläut", //
        "IDENTIFIER(uml) IDENTIFIER(ut) NEWLINE EOF",
        "   ^ invalid character: 'ä'");
  }

  @Test
  public void testCrLf() throws Exception {
    check("\r\n\r\n", "NEWLINE EOF");
    check("\r\n\r1\r\r\n", "INT(1) NEWLINE EOF");
    check("# foo\r\n# bar\r\n", "NEWLINE EOF");
  }

  @Test
  public void testIntegers() throws Exception {
    // Detection of MINUS immediately following integer constant proves we
    // don't consume too many chars.

    // decimal
    check("12345-", "INT(12345) MINUS NEWLINE EOF");

    // TODO(adonovan): add tests for 0b binary literals

    // octal
    check("0o12345-", "INT(5349) MINUS NEWLINE EOF");
    check("0O77", "INT(63) NEWLINE EOF");
    check("0o1o2349-", "INT(1) IDENTIFIER(o2349) MINUS NEWLINE EOF");
    checkErrors(
        "0o12349-", //
        "INT(0) MINUS NEWLINE EOF",
        "^ invalid base-8 integer literal: 0o12349");
    checkErrors(
        "0o", //
        "INT(0) NEWLINE EOF",
        "^ invalid base-8 integer literal: 0o");
    checkErrors(
        "012345", //
        "INT(0) NEWLINE EOF",
        "^ invalid octal literal: 012345 (use '0o12345')");

    // hexadecimal (uppercase)
    check("0X12345F-", "INT(1193055) MINUS NEWLINE EOF");

    // hexadecimal (lowercase)
    check("0x12345f-", "INT(1193055) MINUS NEWLINE EOF");

    // hexadecimal (lowercase) [note: "g" cause termination of token]
    check("0x12345g-", "INT(74565) IDENTIFIER(g) MINUS NEWLINE EOF");

    // long
    check("1234567890 0x123456789ABCDEF", "INT(1234567890) INT(81985529216486895) NEWLINE EOF");
    // big
    check(
        "123456789123456789123456789 0xABCDEFABCDEFABCDEFABCDEFABCDEF",
        "INT(123456789123456789123456789) INT(892059645479943313385225296292859375) NEWLINE EOF");
  }

  @Test
  public void testNumbersAndDot() throws Exception {
    check("0", "INT(0) NEWLINE EOF");
    check("0.", "FLOAT(0.0) NEWLINE EOF");
    check(".0", "FLOAT(0.0) NEWLINE EOF");
    checkErrors(
        "1e", //
        "FLOAT(0.0) NEWLINE EOF",
        "^ invalid float literal");
    checkErrors(
        "1e+x", //
        "FLOAT(0.0) IDENTIFIER(x) NEWLINE EOF",
        "^ invalid float literal");
    check("1e1", "FLOAT(10.0) NEWLINE EOF");
    check(".e1", "DOT IDENTIFIER(e1) NEWLINE EOF");
    check("1.e1", "FLOAT(10.0) NEWLINE EOF");
    check("1.e+1", "FLOAT(10.0) NEWLINE EOF");
    check("1.e-1", "FLOAT(0.1) NEWLINE EOF");

    check("1.2345", "FLOAT(1.2345) NEWLINE EOF");
    check("1.2.345", "FLOAT(1.2) FLOAT(0.345) NEWLINE EOF");

    check("1.0E10", "FLOAT(1.0E10) NEWLINE EOF");
    check("1.03E-10", "FLOAT(1.03E-10) NEWLINE EOF");

    check(". 123", "DOT INT(123) NEWLINE EOF");
    check(".123", "FLOAT(0.123) NEWLINE EOF");
    check(".abc", "DOT IDENTIFIER(abc) NEWLINE EOF");

    check("foo.123", "IDENTIFIER(foo) FLOAT(0.123) NEWLINE EOF");
    check("foo.bcd", "IDENTIFIER(foo) DOT IDENTIFIER(bcd) NEWLINE EOF"); // 'b' are hex chars
    check("foo.xyz", "IDENTIFIER(foo) DOT IDENTIFIER(xyz) NEWLINE EOF");
  }

  @Test
  public void testStringDelimiters() throws Exception {
    check("\"foo\"", "STRING(foo) NEWLINE EOF");
    check("'foo'", "STRING(foo) NEWLINE EOF");
  }

  @Test
  public void testQuotesInStrings() throws Exception {
    check("'foo\\'bar'", "STRING(foo'bar) NEWLINE EOF");
    check("\"foo'bar\"", "STRING(foo'bar) NEWLINE EOF");
    check("'foo\"bar'", "STRING(foo\"bar) NEWLINE EOF");
    check("\"foo\\\"bar\"", "STRING(foo\"bar) NEWLINE EOF");
  }

  @Test
  public void testStringEscapes() throws Exception {
    check("'a\\tb\\nc\\rd'", "STRING(a\tb\nc\rd) NEWLINE EOF"); // \t \r \n
    checkErrors(
        "'x\\hx'", //
        "STRING(x\\hx) NEWLINE EOF",
        "   ^ invalid escape sequence: \\h. You can enable unknown escape sequences by passing the"
            + " flag --incompatible_restrict_string_escapes=false");
    checkErrors(
        "'\\$$'", //
        "STRING(\\$$) NEWLINE EOF",
        "  ^ invalid escape sequence: \\$. You can enable unknown escape sequences by passing the"
            + " flag --incompatible_restrict_string_escapes=false");
    check("'a\\\nb'", "STRING(ab) NEWLINE EOF"); // escape end of line
    checkErrors("\"ab\\ucd\"", "STRING(abcd) NEWLINE EOF", "    ^ truncated escape sequence \\ucd");

    // hex escapes
    check("'\\x00\\x20\\x09\\x41\\x7e\\x7f'", "STRING(\u0000 \tA~\u007F) NEWLINE EOF"); // DEL is non-printable
    checkErrors("'\\x80'", "STRING(80) NEWLINE EOF", "  ^ non-ASCII hex escape \\x80 (use \\u0080 for the UTF-8 encoding of U+0080)");
    checkErrors("'\\xff'", "STRING(ff) NEWLINE EOF", "  ^ non-ASCII hex escape \\xff (use \\u00FF for the UTF-8 encoding of U+00FF)");
    checkErrors("'\\xFf'", "STRING(Ff) NEWLINE EOF", "  ^ non-ASCII hex escape \\xFf (use \\u00FF for the UTF-8 encoding of U+00FF)");
    checkErrors("'\\xF'", "STRING(F) NEWLINE EOF", "  ^ truncated escape sequence \\xF");
    checkErrors("'\\x'", "STRING() NEWLINE EOF", "  ^ truncated escape sequence \\x");
    checkErrors("'\\xfg'", "STRING(fg) NEWLINE EOF", "  ^ invalid escape sequence \\xfg");
    // Unicode escapes
    // \\uXXXX
    check("'\\u0400'", "STRING(Ѐ) NEWLINE EOF");
    checkErrors("'\\u100'", "STRING(100) NEWLINE EOF", "  ^ truncated escape sequence \\u100");
    check("'\\u04000'", "STRING(Ѐ0) NEWLINE EOF"); // = U+0400 + '0'
    checkErrors("'\\u100g'", "STRING(100g) NEWLINE EOF", "  ^ invalid escape sequence \\u100g");
    check("'\\u4E16'", "STRING(世) NEWLINE EOF");
    checkErrors("'\\udc00'", "STRING(dc00) NEWLINE EOF", "  ^ invalid Unicode code point U+DC00"); // surrogate
    // \\UXXXXXXXX
    check("'\\U00000400'", "STRING(Ѐ) NEWLINE EOF");
    checkErrors("'\\U0000400'", "STRING(0000400) NEWLINE EOF", "  ^ truncated escape sequence \\U0000400");
    check("'\\U000004000'", "STRING(Ѐ0) NEWLINE EOF"); // = U+0400 + '0'
    checkErrors("'\\U1000000g'", "STRING(1000000g) NEWLINE EOF", "  ^ invalid escape sequence \\U1000000g");
    check("'\\U0010FFFF'", "STRING(\uDBFF\uDFFF) NEWLINE EOF");
    checkErrors("'\\U00110000'", "STRING(00110000) NEWLINE EOF", "  ^ code point out of range: \\U00110000 (max \\U00110000)");
    check("'\\U0001F63F'", "STRING(\uD83D\uDE3F) NEWLINE EOF");
    checkErrors("'\\U0000dc00'", "STRING(0000dc00) NEWLINE EOF", "  ^ invalid Unicode code point U+DC00"); // surrogate
  }

  @Test
  public void testEscapedCrlfInString() throws Exception {
    check("'a\\\r\nb'", "STRING(ab) NEWLINE EOF");
    check("\"a\\\r\nb\"", "STRING(ab) NEWLINE EOF");
    check("\"\"\"a\\\r\nb\"\"\"", "STRING(ab) NEWLINE EOF");
    check("'''a\\\r\nb'''", "STRING(ab) NEWLINE EOF");
    check("r'a\\\r\nb'", "STRING(a\\\nb) NEWLINE EOF");
    check("r\"a\\\r\nb\"", "STRING(a\\\nb) NEWLINE EOF");
    check("r\"a\\\r\n\\\nb\"", "STRING(a\\\n\\\nb) NEWLINE EOF");
  }

  @Test
  public void testRawString() throws Exception {
    check("r'abcd'", "STRING(abcd) NEWLINE EOF");
    check("r\"abcd\"", "STRING(abcd) NEWLINE EOF");
    check("r'a\\tb\\nc\\rd'", "STRING(a\\tb\\nc\\rd) NEWLINE EOF"); // r'a\tb\nc\rd'
    check("r\"a\\\"\"", "STRING(a\\\") NEWLINE EOF"); // r"a\""
    check("r'a\\\\b'", "STRING(a\\\\b) NEWLINE EOF"); // r'a\\b'
    check("r'ab'r", "STRING(ab) IDENTIFIER(r) NEWLINE EOF");

    // Unclosed raw string
    checkErrors(
        "+ r'\\'", // r'\'
        "PLUS STRING(\\') NEWLINE EOF",
        "  ^ unclosed string literal");
  }

  @Test
  public void testBytePrefix() throws Exception {
    check("b'abcd'", "BYTE(abcd) NEWLINE EOF");
    check("b\'AЀ世\uD83D\uDE3F\'", "BYTE(AЀ世\uD83D\uDE3F) NEWLINE EOF");
    check("b\'\\x41\\u0400\\u4e16\\U0001F63F\'", "BYTE(AЀ世\uD83D\uDE3F) NEWLINE EOF");
    check("b\'\\377\\378\\x80\\xff\\xFf\'", "BYTE(ÿ\u001F8\u0080ÿÿ) NEWLINE EOF");
    checkErrors("b\'\\400\'", "BYTE() NEWLINE EOF", "     ^ octal escape sequence out of range (maximum is \\377)");
    checkErrors("b\'\\udc00\'", "BYTE(dc00) NEWLINE EOF", "   ^ invalid Unicode code point U+DC00");
  }

  @Test
  public void testTripleRawString() throws Exception {
    // r'''a\ncd'''
    check("r'''ab\\ncd'''", "STRING(ab\\ncd) NEWLINE EOF");
    // r"""ab
    // cd"""
    check("\"\"\"ab\ncd\"\"\"", "STRING(ab\ncd) NEWLINE EOF");

    // Unclosed raw string
    checkErrors(
        "r'''\\'''", // r'''\'''
        "STRING(\\''') NEWLINE EOF",
        "^ unclosed string literal");
  }

  @Test
  public void testOctalEscapes() throws Exception {
    // Test boundaries (non-octal char, EOF).
    check("'\\1b \\1'", "STRING(\1b \1) NEWLINE EOF");
    check("'\\037'", "STRING(\u001F) NEWLINE EOF");
    check("'\\378'", "STRING(\u001F8) NEWLINE EOF");  // = '\37' + '8'
  }

  @Test
  public void testOctalEscapeOutOfRange() throws Exception {
    // Regression test for a bug.
    checkErrors(
        "'\\0 \\1 \\11 \\77 \\111 \\1111 \\377'",
        "STRING(\0 \1 \t \u003f I I1 ) NEWLINE EOF",
      "                             ^ non-ASCII octal " +
        "escape \\377 (use \\u00FF for the UTF-8 encoding of U+00FF)");
    checkErrors(
        "'\\777'",
        "STRING() NEWLINE EOF",
        "    ^ non-ASCII octal escape \\777 (use \\u01FF for the UTF-8 encoding of U+01FF)");
    checkErrors(
      "'\\377'",
      "STRING() NEWLINE EOF",
      "    ^ non-ASCII octal escape \\377 (use \\u00FF for the UTF-8 encoding of U+00FF)");
    checkErrors(
      "'\\400'",
      "STRING() NEWLINE EOF",
      "    ^ non-ASCII octal escape \\400 (use \\u0100 for the UTF-8 encoding of U+0100)");
  }

  @Test
  public void testTripleQuotedStrings() throws Exception {
    check("\"\"\"a\"b'c \n d\"\"e\"\"\"", "STRING(a\"b'c \n d\"\"e) NEWLINE EOF");
    check("'''a\"b'c \n d\"\"e'''", "STRING(a\"b'c \n d\"\"e) NEWLINE EOF");
  }

  @Test
  public void testBadChar() throws Exception {
    checkErrors(
        "a$b", //
        "IDENTIFIER(a) IDENTIFIER(b) NEWLINE EOF",
        " ^ invalid character: '$'");
  }

  @Test
  public void testIndentation() throws Exception {
    check("1\n2\n3", "INT(1) NEWLINE INT(2) NEWLINE INT(3) NEWLINE EOF");
    check(
        "1\n  2\n  3\n4 ",
        "INT(1) NEWLINE INDENT INT(2) NEWLINE INT(3) NEWLINE OUTDENT " + "INT(4) NEWLINE EOF");
    check(
        "1\n  2\n  3",
        "INT(1) NEWLINE INDENT INT(2) NEWLINE INT(3) NEWLINE OUTDENT " + "NEWLINE EOF");
    check(
        "1\n  2\n    3",
        "INT(1) NEWLINE INDENT INT(2) NEWLINE INDENT INT(3) NEWLINE "
            + "OUTDENT OUTDENT NEWLINE EOF");
    check(
        "1\n  2\n    3\n  4\n5",
        "INT(1) NEWLINE INDENT INT(2) NEWLINE INDENT INT(3) NEWLINE "
            + "OUTDENT INT(4) NEWLINE OUTDENT INT(5) NEWLINE EOF");

    checkErrors(
        "1\n  2\n    3\n   4\n5",
        "INT(1) NEWLINE INDENT INT(2) NEWLINE INDENT INT(3) NEWLINE "
            + "OUTDENT INT(4) NEWLINE OUTDENT INT(5) NEWLINE EOF",
        "  ^ indentation error (line 4)");
  }

  @Test
  public void testIndentationWithTab() throws Exception {
    checkErrors(
        "def x():\n" + "\tpass", //
        "DEF IDENTIFIER(x) LPAREN RPAREN COLON NEWLINE "
            + "INDENT PASS NEWLINE OUTDENT NEWLINE EOF",
        " ^ Tab characters are not allowed for indentation. Use spaces instead. (line 2)");
  }

  @Test
  public void testIndentationWithCrLf() throws Exception {
    check("1\r\n  2\r\n", "INT(1) NEWLINE INDENT INT(2) NEWLINE OUTDENT NEWLINE EOF");
    check("1\r\n  2\r\n\r\n", "INT(1) NEWLINE INDENT INT(2) NEWLINE OUTDENT NEWLINE EOF");
    check(
        "1\r\n  2\r\n    3\r\n  4\r\n5",
        "INT(1) NEWLINE INDENT INT(2) NEWLINE INDENT INT(3) NEWLINE OUTDENT INT(4) "
            + "NEWLINE OUTDENT INT(5) NEWLINE EOF");
    check(
        "1\r\n  2\r\n\r\n  3\r\n4",
        "INT(1) NEWLINE INDENT INT(2) NEWLINE INT(3) NEWLINE OUTDENT INT(4) NEWLINE EOF");
  }

  @Test
  public void testIndentationInsideParens() throws Exception {
    // Indentation is ignored inside parens:
    check("1 (\n  2\n    3\n  4\n5", "INT(1) LPAREN INT(2) INT(3) INT(4) INT(5) NEWLINE EOF");
    check("1 {\n  2\n    3\n  4\n5", "INT(1) LBRACE INT(2) INT(3) INT(4) INT(5) NEWLINE EOF");
    check("1 [\n  2\n    3\n  4\n5", "INT(1) LBRACKET INT(2) INT(3) INT(4) INT(5) NEWLINE EOF");
    check(
        "1 [\n  2]\n    3\n    4\n5",
        "INT(1) LBRACKET INT(2) RBRACKET NEWLINE INDENT INT(3) "
            + "NEWLINE INT(4) NEWLINE OUTDENT INT(5) NEWLINE EOF");
  }

  @Test
  public void testIndentationAtEOF() throws Exception {
    // Matching OUTDENTS are created at EOF:
    check("\n  1", "INDENT INT(1) NEWLINE OUTDENT NEWLINE EOF");
  }

  @Test
  public void testIndentationOnFirstLine() throws Exception {
    check("    1", "INDENT INT(1) NEWLINE OUTDENT NEWLINE EOF");
    check("\n\n    1", "INDENT INT(1) NEWLINE OUTDENT NEWLINE EOF");
  }

  @Test
  public void testBlankLineIndentation() throws Exception {
    // Blank lines and comment lines should not generate any newlines indents
    // (but note that every input ends with NEWLINE EOF).
    check("\n      #\n", "NEWLINE EOF");
    check("      #", "NEWLINE EOF");
    check("      #\n", "NEWLINE EOF");
    check("      #comment\n", "NEWLINE EOF");
    check(
        "def f(x):\n"
            + //
            "  # comment\n"
            + //
            "\n"
            + //
            "  \n"
            + //
            "  return x\n",
        "DEF IDENTIFIER(f) LPAREN IDENTIFIER(x) RPAREN COLON NEWLINE "
            + "INDENT RETURN IDENTIFIER(x) NEWLINE "
            + "OUTDENT NEWLINE EOF");
  }

  @Test
  public void testBackslash() throws Exception {
    check("a\\\nb", "IDENTIFIER(a) IDENTIFIER(b) NEWLINE EOF");
    check("a\\\r\nb", "IDENTIFIER(a) IDENTIFIER(b) NEWLINE EOF");
    check("a\\ b", "IDENTIFIER(a) ILLEGAL(\\) IDENTIFIER(b) NEWLINE EOF");
    check("a(\\\n2)", "IDENTIFIER(a) LPAREN INT(2) RPAREN NEWLINE EOF");
  }

  @Test
  public void testTokenPositions() throws Exception {
    assertThat(positions(tokens("foo(bar, {1: 'quux'}, \"\"\"b\"\"\", r\"\")")))
        .isEqualTo(
            // foo (     bar   ,     {      1       :
            "[0,3) [3,4) [4,7) [7,8) [9,10) [10,11) [11,12)"
                //  'quux'  }       ,       """b""" ,       r""     )       NEWLINE EOF
                + " [13,19) [19,20) [20,21) [22,29) [29,30) [31,34) [34,35) [35,35) [35,35)");
  }

  @Test
  public void testLineNumbers() throws Exception {
    assertThat(linenums("foo = 1\nbar = 2\n\nwiz = 3")).isEqualTo("1 1 1 1 2 2 2 2 4 4 4 4 4");

    checkErrors(
        "foo = 1\n" + "bar = 2\n" + "\n" + "wiz = $\n" + "bar = 2",
        "IDENTIFIER(foo) EQUALS INT(1) NEWLINE "
            + "IDENTIFIER(bar) EQUALS INT(2) NEWLINE "
            + "IDENTIFIER(wiz) EQUALS NEWLINE "
            + "IDENTIFIER(bar) EQUALS INT(2) NEWLINE EOF",
        "      ^ invalid character: '$' (line 4)");

    // '\\n' in string should not increment linenum:
    String s = //
        "1\n'foo\\nbar'\3";
    checkErrors(
        s, //
        "INT(1) NEWLINE STRING(foo\nbar) NEWLINE EOF",
        "          ^ invalid character: '\3' (line 2)");
    assertThat(linenums(s)).isEqualTo("1 1 2 2 2");
  }

  @Test
  public void testContainsErrors() throws Exception {
    check("foo", "IDENTIFIER(foo) NEWLINE EOF");
    checkErrors(
        "f$o", //
        "IDENTIFIER(f) IDENTIFIER(o) NEWLINE EOF",
        " ^ invalid character: '$'");
    checkErrors(
        "+ 'unterminated", "PLUS STRING(unterminated) NEWLINE EOF", "  ^ unclosed string literal");
  }

  @Test
  public void testUnclosedRawStringWithEscapingError() throws Exception {
    checkErrors(
        "r'\\",
        "STRING(\\) NEWLINE EOF", //
        "^ unclosed string literal");
  }

  @Test
  public void testFirstCharIsTab() {
    checkErrors(
        "\t", //
        "NEWLINE EOF",
        " ^ Tab characters are not allowed for indentation. Use spaces instead.");
  }

  /**
   * Returns the first error whose string form contains the specified substring, or throws an
   * informative AssertionError if there is none.
   *
   * <p>Exposed for use by other frontend tests.
   */
  // TODO(adonovan): move to ParserTest
  static SyntaxError assertContainsError(List<SyntaxError> errors, String substr) {
    for (SyntaxError error : errors) {
      if (error.toString().contains(substr)) {
        return error;
      }
    }
    if (errors.isEmpty()) {
      throw new AssertionError("no errors, want '" + substr + "'");
    } else {
      throw new AssertionError(
          "error '" + substr + "' not found, but got these:\n" + Joiner.on("\n").join(errors));
    }
  }

  @Test
  public void testStringLiteralUnquote() {
    // Coverage here needn't be exhaustive,
    // as the underlying logic is that of the Lexer.
    assertUnquoteEquals("'hello'", "hello");
    assertUnquoteEquals("\"hello\"", "hello");
    assertUnquoteEquals("r'a\\b\"c'", "a\\b\"c");

    assertUnquoteError("", "invalid syntax"); // empty
    assertUnquoteError(" 'hello'", "invalid syntax"); // leading space
    assertUnquoteError("'hello' ", "invalid syntax"); // trailing space
    assertUnquoteError("x", "invalid syntax"); // identifier
    assertUnquoteError("r", "invalid syntax"); // identifier (same prefix as r'...')
    assertUnquoteError("r2", "invalid syntax"); // identifier
    assertUnquoteError("1", "invalid syntax"); // number
    assertUnquoteError("'", "unclosed string literal");
    assertUnquoteError("\"", "unclosed string literal");
    assertUnquoteError("'abc", "unclosed string literal");
    assertUnquoteError(
        "'\\g'",
        "invalid escape sequence: \\g. You can enable unknown escape sequences by passing the flag"
            + " --incompatible_restrict_string_escapes=false"); // this temporary hint is a lie
  }

  private static void assertUnquoteEquals(String literal, String value) {
    assertThat(StringLiteral.unquote(literal)).isEqualTo(value);
  }

  private static void assertUnquoteError(String badLiteral, String errorSubstring) {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> StringLiteral.unquote(badLiteral));
    assertThat(ex).hasMessageThat().contains(errorSubstring);
  }
}
