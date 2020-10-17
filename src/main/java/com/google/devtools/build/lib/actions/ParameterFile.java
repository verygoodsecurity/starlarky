// Copyright 2014 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.actions;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.devtools.build.lib.unsafe.StringUnsafe;
import com.google.devtools.build.lib.util.FileType;
import com.google.devtools.build.lib.util.GccParamFileEscaper;
import com.google.devtools.build.lib.util.ShellEscaper;
import com.google.devtools.build.lib.vfs.PathFragment;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Support for parameter file generation (as used by gcc and other tools, e.g.
 * {@code gcc @param_file}. Note that the parameter file needs to be explicitly
 * deleted after use. Different tools require different parameter file formats,
 * which can be selected via the {@link ParameterFileType} enum.
 *
 * <p>The default charset is ISO-8859-1 (latin1). This also has to match the
 * expectation of the tool.
 *
 * <p>Don't use this class for new code. Use the ParameterFileWriteAction
 * instead!
 */
public class ParameterFile {

  /** Different styles of parameter files. */
  public enum ParameterFileType {
    /**
     * A parameter file with every parameter on a separate line. This format
     * cannot handle newlines in parameters. It is currently used for most
     * tools, but may not be interpreted correctly if parameters contain
     * white space or other special characters. It should be avoided for new
     * development.
     */
    UNQUOTED,

    /**
     * A parameter file where each parameter is correctly quoted for shell use, and separated by
     * white space (space, tab, newline). This format is safe for all characters, but must be
     * specially supported by the tool. In particular, it must not be used with gcc and related
     * tools, which do not support this format as it is.
     */
    SHELL_QUOTED,

    /**
     * A parameter file where each parameter is correctly quoted for gcc or clang use, and separated
     * by white space (space, tab, newline).
     */
    GCC_QUOTED;
  }

  @VisibleForTesting
  public static final FileType PARAMETER_FILE = FileType.of(".params");

  /**
   * Creates a parameter file with the given parameters.
   */
  private ParameterFile() {
  }
  /**
   * Derives an path from a given path by appending <code>".params"</code>.
   */
  public static PathFragment derivePath(PathFragment original) {
    return derivePath(original, "2");
  }

  /**
   * Derives an path from a given path by appending <code>".params"</code>.
   */
  public static PathFragment derivePath(PathFragment original, String flavor) {
    return original.replaceName(original.getBaseName() + "-" + flavor + ".params");
  }

  /** Writes an argument list to a parameter file. */
  public static void writeParameterFile(
      OutputStream out, Iterable<String> arguments, ParameterFileType type, Charset charset)
      throws IOException {
    OutputStream bufferedOut = new BufferedOutputStream(out);
    switch (type) {
      case SHELL_QUOTED:
        writeContent(bufferedOut, ShellEscaper.escapeAll(arguments), charset);
        break;
      case GCC_QUOTED:
        writeContent(bufferedOut, GccParamFileEscaper.escapeAll(arguments), charset);
        break;
      case UNQUOTED:
        writeContent(bufferedOut, arguments, charset);
        break;
    }
  }

  private static void writeContent(
      OutputStream outputStream, Iterable<String> arguments, Charset charset) throws IOException {
    if (charset.equals(StandardCharsets.ISO_8859_1) && StringUnsafe.canUse()) {
      writeContentLatin1Jdk9(outputStream, arguments);
    } else if (charset.equals(StandardCharsets.UTF_8) && StringUnsafe.canUse()) {
      writeContentUtf8Jdk9(outputStream, arguments);
    } else {
      // Generic charset support
      OutputStreamWriter out = new OutputStreamWriter(outputStream, charset);
      for (String line : arguments) {
        out.write(line);
        out.write('\n');
      }
      out.flush();
    }
  }

  /**
   * Fast LATIN-1 path that avoids GC overhead. This takes advantage of the fact that strings are
   * encoded as either LATIN-1 or UTF-16 under JDK9. When LATIN-1 we can simply copy the byte
   * buffer, when UTF-16 we can fail loudly.
   */
  private static void writeContentLatin1Jdk9(OutputStream outputStream, Iterable<String> arguments)
      throws IOException {
    StringUnsafe stringUnsafe = StringUnsafe.getInstance();
    for (String line : arguments) {
      if (stringUnsafe.getCoder(line) == StringUnsafe.LATIN1) {
        byte[] bytes = stringUnsafe.getByteArray(line);
        outputStream.write(bytes);
      } else {
        // Error case, encode with '?' characters
        ByteBuffer encodedBytes = StandardCharsets.ISO_8859_1.encode(CharBuffer.wrap(line));
        outputStream.write(
            encodedBytes.array(),
            encodedBytes.arrayOffset(),
            encodedBytes.arrayOffset() + encodedBytes.limit());
      }
      outputStream.write('\n');
    }
    outputStream.flush();
  }

  /**
   * Fast UTF-8 path that tries to coder GC overhead. This takes advantage of the fact that strings
   * are encoded as either LATIN-1 or UTF-16 under JDK9. When LATIN-1 we can check if the buffer is
   * ASCII and copy that directly (since this is both valid LATIN-1 and UTF-8), in all other cases
   * we must re-encode.
   */
  private static void writeContentUtf8Jdk9(OutputStream outputStream, Iterable<String> arguments)
      throws IOException {
    CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
    StringUnsafe stringUnsafe = StringUnsafe.getInstance();
    for (String line : arguments) {
      byte[] bytes = stringUnsafe.getByteArray(line);
      if (stringUnsafe.getCoder(line) == StringUnsafe.LATIN1 && isAscii(bytes)) {
        outputStream.write(bytes);
      } else {
        ByteBuffer encodedBytes = encoder.encode(CharBuffer.wrap(line));
        outputStream.write(
            encodedBytes.array(),
            encodedBytes.arrayOffset(),
            encodedBytes.arrayOffset() + encodedBytes.limit());
      }
      outputStream.write('\n');
    }
    outputStream.flush();
  }

  private static boolean isAscii(byte[] latin1Bytes) {
    boolean hiBitSet = false;
    int n = latin1Bytes.length;
    for (int i = 0; i < n; ++i) {
      hiBitSet |= ((latin1Bytes[i] & 0x80) != 0);
    }
    return !hiBitSet;
  }

  /** Criterion shared by {@link #flagsOnly} and {@link #nonFlags}. */
  private static boolean isFlag(String arg) {
    return arg.startsWith("--");
  }

  /**
   * Extracts the args from the given list that are flags (i.e. start with "--"). Note, this makes
   * sense only if flags with values have previously been joined, e.g."--foo=bar" rather than
   * "--foo", "bar".
   */
  public static ImmutableList<String> flagsOnly(Iterable<String> args) {
    return Streams.stream(args).filter(ParameterFile::isFlag).collect(toImmutableList());
  }

  /**
   * Extracts the args from the given list that are not flags (i.e. do not start with "--"). Note,
   * this makes sense only if flags with values have previously been joined, e.g."--foo=bar" rather
   * than "--foo", "bar".
   */
  public static ImmutableList<String> nonFlags(Iterable<String> args) {
    return Streams.stream(args).filter(arg -> !isFlag(arg)).collect(toImmutableList());
  }
}
