package com.verygood.security.larky.modules.re;

import com.google.common.primitives.ImmutableIntArray;
import net.starlark.java.eval.StarlarkValue;

/** A regex input and its mapping between Python positions and RE2/J offsets. */
final class PyRegexInput implements StarlarkValue {
  final String text;
  final boolean bytes;
  final int length;
  private final ImmutableIntArray lowSurrogates;

  PyRegexInput(String text, boolean bytes, int length) {
    this.text = text;
    this.bytes = bytes;
    this.length = length;
    int pairCount = text.length() - length;
    if (pairCount == 0) {
      lowSurrogates = ImmutableIntArray.of();
      return;
    }
    ImmutableIntArray.Builder pairs = ImmutableIntArray.builder(pairCount);
    for (int i = 1; i < text.length(); i++) {
      if (Character.isSurrogatePair(text.charAt(i - 1), text.charAt(i))) {
        pairs.add(i);
      }
    }
    lowSurrogates = pairs.build();
  }

  int toOffset(int position) {
    // A pair's low surrogate minus its preceding pairs is the position just after the pair.
    int low = 0;
    int high = lowSurrogates.length();
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (lowSurrogates.get(middle) - middle <= position) {
        low = middle + 1;
      } else {
        high = middle;
      }
    }
    return position + low;
  }

  boolean hasSupplementaryCharacters() {
    return !lowSurrogates.isEmpty();
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  int toPosition(int offset) {
    int low = 0;
    int high = lowSurrogates.length();
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (lowSurrogates.get(middle) < offset) {
        low = middle + 1;
      } else {
        high = middle;
      }
    }
    return offset - low;
  }
}
