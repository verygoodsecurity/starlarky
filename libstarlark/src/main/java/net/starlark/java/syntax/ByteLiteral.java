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
package net.starlark.java.syntax;

import java.io.ByteArrayOutputStream;
import java.util.stream.IntStream;

/** Syntax node for a bytes literal. */
public final class ByteLiteral extends Expression {

  private final int startOffset;
  private final byte[] value;
  private final int endOffset;
  private final String raw;

  ByteLiteral(FileLocations locs, int startOffset, String value, int endOffset) {
    super(locs);
    this.startOffset = startOffset;
    this.raw = value;
    this.value = str2bytearr(value.codePoints());
    this.endOffset = endOffset;
  }

  /**
   * The Starlark specification allows invalid UTF-8 sequences stuffed into UTF-k strings. In Java,
   * this means that there will be invalid UTF-8 sequences in UTF-16 strings. The lexer uses
   * StringBuilder which will produce a UTF-16 encoded string that is shuttling embedded UTF-8
   * sequences.
   *
   * As a result, we cannot simply "encode" the string into UTF-8 because it will give us garbage.
   *
   * So, we will create a byte array representation consisting of the underlying
   * bytes of the string, assuming that they're already encoded as UTF-8. If we see a byte greater
   * than the maximum unsigned byte size, we will then convert that codepoint from UTF-k
   * (UTF-16 in Java-land) to its equivalent UTF-8 sequence.
   *
   * The resulting byte array _should_ be the intended UTF-8 sequence. From there, we can proceed
   * as normal.
   */
  static byte[] str2bytearr(IntStream codePoints) {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    // The following is a little trick that will throw IndexOutOfBounds if too large code point
    //  and as a result avoids having to do bounds checking
    final byte[] cpBytes = new byte[6];
    codePoints.forEach((cp) -> {
      if (cp < 0) {
        throw new IllegalStateException("No negative code point allowed");
      }
      else if(cp <= 0xFF) {
        baos.write(cp);
      }
      else {
        int bi = 0;
        int lastPrefix = 0xC0;
        int lastMask = 0x1F;
        for (;;) {
            int b = 0x80 | (cp & 0x3F);
            cpBytes[bi] = (byte)b;
            ++bi;
            cp >>= 6;
            if ((cp & ~lastMask) == 0) {
                cpBytes[bi] = (byte) (lastPrefix | cp);
                ++bi;
                break;
            }
            lastPrefix = 0x80 | (lastPrefix >> 1);
            lastMask >>= 1;
        }
        while (bi > 0) {
            --bi;
            baos.write(cpBytes[bi]);
        }
      }
    });
    return baos.toByteArray();
  }

  /** Returns the value denoted by the byte literal */
  public byte[] getValue() {
    return value;
  }

  /** Returns the raw source text of the literal. */
  public String getRaw() {
    return this.raw;
  }

  public Location getLocation() {
    return locs.getLocation(startOffset);
  }

  @Override
  public int getStartOffset() {
    return startOffset;
  }

  @Override
  public int getEndOffset() {
    // TODO(adonovan): when we switch to compilation,
    // making syntax trees ephemeral, we can afford to
    // record the raw literal. This becomes:
    //   return startOffset + raw.length().
    return endOffset;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Kind kind() {
    return Kind.BYTE_LITERAL;
  }

  // -- hooks to support Skyframe serialization without creating a dependency --

  /** Returns an opaque serializable object that may be passed to {@link #fromSerialization}. */
  public Object getFileLocations() {
    return locs;
  }

  /** Constructs a ByteLiteral from its serialized components. */
  public static ByteLiteral fromSerialization(
      Object fileLocations, int startOffset, String value, int endOffset) {
    return new ByteLiteral((FileLocations) fileLocations, startOffset, value, endOffset);
  }
}
