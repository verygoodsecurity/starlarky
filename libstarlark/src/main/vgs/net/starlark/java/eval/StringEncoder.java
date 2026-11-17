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

package net.starlark.java.eval;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * {@code str.encode} without Larky's codec registry: UTF-8, Latin-1 and ASCII with CPython's names,
 * error handlers and error messages. Larky installs {@link StarlarkBytes#setEncoder} to cover every
 * codec {@code codecs.encode} and {@code bytes(s, encoding)} support; this is what a bare
 * libstarlark does.
 */
final class StringEncoder {
  private StringEncoder() {}

  private enum Codec {
    UTF_8("utf-8", 0x110000, "surrogates not allowed"),
    LATIN_1("latin-1", 0x100, "ordinal not in range(256)"),
    ASCII("ascii", 0x80, "ordinal not in range(128)");

    final String name;
    final int limit; // code points below this encode as themselves (Latin-1 and ASCII)
    final String reason;

    Codec(String name, int limit, String reason) {
      this.name = name;
      this.limit = limit;
      this.reason = reason;
    }
  }

  /** CPython's codec lookup normalization: case-folded, runs of punctuation become '_'. */
  private static Codec lookup(String encoding) throws EvalException {
    StringBuilder sb = new StringBuilder();
    boolean punct = false;
    for (int i = 0; i < encoding.length(); i++) {
      char c = Character.toLowerCase(encoding.charAt(i));
      if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.') {
        if (punct && sb.length() > 0) {
          sb.append('_');
        }
        sb.append(c);
        punct = false;
      } else if (!Character.isLetterOrDigit(c)) {
        punct = true;
      } else {
        sb.append(c);
        punct = false;
      }
    }
    switch (sb.toString().toLowerCase(Locale.ROOT)) {
      case "utf_8":
      case "utf8":
      case "u8":
      case "utf":
      case "utf8_ucs2":
      case "utf8_ucs4":
      case "cp65001":
        return Codec.UTF_8;
      case "latin_1":
      case "latin1":
      case "latin":
      case "l1":
      case "iso_8859_1":
      case "iso8859_1":
      case "8859":
      case "cp819":
      case "iso_ir_100":
        return Codec.LATIN_1;
      case "ascii":
      case "us_ascii":
      case "646":
      case "ansi_x3.4_1968":
      case "iso646_us":
        return Codec.ASCII;
      default:
        throw Starlark.errorf("unknown encoding: %s", encoding);
    }
  }

  static byte[] encode(String s, String encoding, String errors) throws EvalException {
    Codec codec = lookup(encoding);
    ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
    int n = s.length();
    int i = 0;
    while (i < n) {
      int cp = s.codePointAt(i);
      int next = i + Character.charCount(cp);
      if (cp < codec.limit && !isSurrogate(cp)) {
        put(out, codec, cp);
        i = next;
        continue;
      }
      // The run of unencodable code points starting here, as CPython reports it.
      int end = next;
      while (end < n) {
        int c = s.codePointAt(end);
        if (c < codec.limit && !isSurrogate(c)) {
          break;
        }
        end += Character.charCount(c);
      }
      handle(errors, codec, s, i, end, out);
      i = end;
    }
    return out.toByteArray();
  }

  private static boolean isSurrogate(int cp) {
    return cp >= 0xD800 && cp <= 0xDFFF;
  }

  private static void put(ByteArrayOutputStream out, Codec codec, int cp) {
    if (codec == Codec.UTF_8) {
      byte[] b = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8);
      out.write(b, 0, b.length);
    } else {
      out.write(cp);
    }
  }

  private static void putAll(ByteArrayOutputStream out, Codec codec, String text)
      throws EvalException {
    // The replacement text is itself encoded; it is ASCII for every handler but a replacement that
    // cannot be encoded, which cannot occur with these three codecs.
    for (int i = 0; i < text.length(); i = text.offsetByCodePoints(i, 1)) {
      put(out, codec, text.codePointAt(i));
    }
  }

  private static void handle(
      String errors, Codec codec, String s, int start, int end, ByteArrayOutputStream out)
      throws EvalException {
    StringBuilder sb = new StringBuilder();
    switch (errors) {
      case "ignore":
        return;
      case "replace":
        for (int i = start; i < end; i = s.offsetByCodePoints(i, 1)) {
          sb.append('?');
        }
        putAll(out, codec, sb.toString());
        return;
      case "backslashreplace":
        for (int i = start; i < end; i = s.offsetByCodePoints(i, 1)) {
          sb.append(escape(s.codePointAt(i)));
        }
        putAll(out, codec, sb.toString());
        return;
      case "xmlcharrefreplace":
        for (int i = start; i < end; i = s.offsetByCodePoints(i, 1)) {
          sb.append("&#").append(s.codePointAt(i)).append(';');
        }
        putAll(out, codec, sb.toString());
        return;
      case "namereplace":
        for (int i = start; i < end; i = s.offsetByCodePoints(i, 1)) {
          int cp = s.codePointAt(i);
          String name = Character.getName(cp);
          sb.append(name != null ? "\\N{" + name + "}" : escape(cp));
        }
        putAll(out, codec, sb.toString());
        return;
      case "surrogateescape":
        for (int i = start; i < end; i++) {
          char ch = s.charAt(i);
          if (ch < 0xDC80 || ch > 0xDCFF) {
            throw strict(codec, s, start, end);
          }
        }
        for (int i = start; i < end; i++) {
          out.write(s.charAt(i) - 0xDC00);
        }
        return;
      case "strict":
        throw strict(codec, s, start, end);
      default:
        throw Starlark.errorf("unknown error handler name '%s'", errors);
    }
  }

  private static EvalException strict(Codec codec, String s, int start, int end) {
    if (s.codePointCount(start, end) == 1) {
      return Starlark.errorf(
          "'%s' codec can't encode character '%s' in position %d: %s",
          codec.name, escape(s.codePointAt(start)), start, codec.reason);
    }
    return Starlark.errorf(
        "'%s' codec can't encode characters in position %d-%d: %s",
        codec.name, start, end - 1, codec.reason);
  }

  private static String escape(int cp) {
    if (cp < 0x100) {
      return String.format("\\x%02x", cp);
    } else if (cp < 0x10000) {
      return String.format("\\u%04x", cp);
    }
    return String.format("\\U%08x", cp);
  }
}
