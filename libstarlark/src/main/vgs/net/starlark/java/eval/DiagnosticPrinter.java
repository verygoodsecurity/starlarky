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

import java.util.BitSet;

/** Records the offsets of tagged values while rendering the original diagnostic once. */
final class DiagnosticPrinter extends Printer {
  private final BinaryTaint taint;
  private final StringBuilder buffer;
  private final int maxLength;
  private final BitSet spans = new BitSet();

  DiagnosticPrinter(BinaryTaint taint, int maxLength) {
    this(taint, maxLength, new StringBuilder());
  }

  private DiagnosticPrinter(BinaryTaint taint, int maxLength, StringBuilder buffer) {
    super(buffer);
    this.taint = taint;
    this.buffer = buffer;
    this.maxLength = maxLength;
  }

  BitSet spans() {
    return spans;
  }

  boolean exceedsLimit() {
    return buffer.length() > maxLength;
  }

  @Override
  public Printer debugPrint(Object value, StarlarkThread thread) {
    int start = buffer.length();
    super.debugPrint(value, thread);
    if (!exceedsLimit()) {
      taint.markRendered(value, start, buffer.length(), false, spans);
    }
    return this;
  }

  @Override
  public Printer repr(Object value, StarlarkSemantics semantics) {
    int start = buffer.length();
    super.repr(value, semantics);
    if (!exceedsLimit()) {
      taint.markRendered(value, start, buffer.length(), value instanceof String, spans);
    }
    return this;
  }
}
