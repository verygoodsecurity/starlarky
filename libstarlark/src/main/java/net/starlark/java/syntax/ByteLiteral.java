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

import net.starlark.java.eval.StarlarkByte;

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
    this.value = StarlarkByte.UTF16toUTF8(value.toCharArray());
    this.endOffset = endOffset;
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
