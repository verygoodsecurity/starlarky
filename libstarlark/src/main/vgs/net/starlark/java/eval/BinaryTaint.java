package net.starlark.java.eval;

import com.google.common.collect.MapMaker;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.syntax.TokenKind;

/** Tracks the string and bytes identities that contribute sensitive data to diagnostics. */
final class BinaryTaint {
  // Entries are pairs of UTF-16 offsets. The whole-value marker also covers bytes.
  private final int[] whole = new int[0];

  private final IdentityHashMap<String, int[]> stringTags = new IdentityHashMap<>();
  @Nullable private Map<StarlarkBytes, int[]> bytesTags;
  private final int maxTags;
  private int tagCount;
  private int remainingWork;
  private int remainingRetainedBytes;
  private boolean limited;

  BinaryTaint(int maxTags, int maxWork) {
    this.maxTags = maxTags;
    this.remainingWork = maxWork;
    this.remainingRetainedBytes = maxWork;
  }

  boolean hasTags() {
    return !stringTags.isEmpty() || (bytesTags != null && !bytesTags.isEmpty());
  }

  boolean isLimited() {
    return limited;
  }

  Object onReveal(Object original, List<Object> hits) {
    if (limited || hits.isEmpty()) {
      return original;
    }
    if (canTag(original)) {
      if (!charge(hits.size())) {
        return original;
      }
      for (Object hit : hits) {
        if (hit == original) {
          return tagReveal(original, /* detach= */ true);
        }
      }
      return matchesContent(original, hits) ? tagReveal(original, /* detach= */ true) : original;
    }
    if (!(original instanceof List<?> values)
        || original instanceof StarlarkBytes
        || !charge(2L * hits.size())) {
      return original;
    }

    var remainingHits = new IdentityHashMap<Object, Integer>();
    for (Object hit : hits) {
      if (canTag(hit)) {
        remainingHits.merge(hit, 1, Integer::sum);
      }
    }
    if (remainingHits.isEmpty() || !charge(2L * values.size())) {
      return original;
    }
    for (Object value : values) {
      Integer count = remainingHits.get(value);
      if (count != null && count > 0) {
        remainingHits.put(value, count - 1);
      }
    }
    // Consume each exact occurrence before comparing content. Any remaining hit carries no
    // position, so every equal result is a possible hit.
    List<Object> unresolved = new ArrayList<>();
    remainingHits.forEach((hit, count) -> {
      if (count > 0) {
        unresolved.add(hit);
      }
    });

    boolean detach = !(original instanceof StarlarkValue);
    var replacements = new IdentityHashMap<Object, Object>();
    List<Object> copy = null;
    int index = 0;
    for (Object value : values) {
      Object tagged = replacements.get(value);
      if (tagged == null
          && (remainingHits.containsKey(value) || matchesContent(value, unresolved))) {
        tagged = tagReveal(value, detach);
        replacements.put(value, tagged);
      }
      if (limited) {
        return original;
      }
      if (tagged != null && tagged != value) {
        if (copy == null) {
          if (!charge(values.size())) {
            return original;
          }
          copy = new ArrayList<>(values);
        }
        copy.set(index, tagged);
      }
      index++;
    }
    return copy == null ? original : copy;
  }

  private boolean matchesContent(Object value, List<Object> candidates) {
    if (!canTag(value)) {
      return false;
    }
    for (Object candidate : candidates) {
      if (!charge(1)) {
        return false;
      }
      boolean equal;
      if (value instanceof String text && candidate instanceof String hit
          && text.length() == hit.length()) {
        equal = charge(text.length()) && text.equals(hit);
      } else if (value instanceof StarlarkBytes bytes && candidate instanceof StarlarkBytes hit
          && bytes.size() == hit.size()) {
        equal = charge(bytes.size()) && bytes.equals(hit);
      } else {
        continue;
      }
      if (equal || limited) {
        return equal;
      }
    }
    return false;
  }

  private Object tagReveal(Object value, boolean detach) {
    if (detach && value instanceof String text) {
      if (!reserveTag(value)) {
        return value;
      }
      // An interned String returned by a provider may also be used as a public literal.
      value = new String(text);
    } else if (tag(value) != null || !reserveTag(value)) {
      return value;
    }
    remember(value, whole);
    return value;
  }

  Object afterBinary(
      StarlarkThread thread, TokenKind op, Object left, Object right, Object result,
      boolean augmented) {
    if (limited || !hasTags() || !canTag(result)) {
      return result;
    }
    int[] leftTag = tag(left);
    int[] rightTag = tag(right);
    boolean stringFormat = op == TokenKind.PERCENT && left instanceof String;
    if (leftTag == null && rightTag == null && !(stringFormat && hasTaggedArgument(right))) {
      return result;
    }
    if (tag(result) != null || !reserveTag(result)) {
      return result;
    }
    if (op == TokenKind.PLUS && left instanceof String ls && right instanceof String rs) {
      int[] ranges = concat(leftTag, ls.length(), rightTag, rs.length());
      if (!limited) {
        remember(result, ranges);
      }
      return result;
    }

    boolean stringRepeat = op == TokenKind.STAR
        && ((left instanceof String && right instanceof StarlarkInt)
            || (left instanceof StarlarkInt && right instanceof String));
    if (result instanceof String text && !stringFormat && !stringRepeat) {
      // A custom binary implementation can return a shared host or script string.
      result = new String(text);
    }
    remember(result, whole);
    return result;
  }

  private int[] concat(int[] left, int leftLength, int[] right, int rightLength) {
    if (leftLength == 0) {
      return right;
    }
    if (rightLength == 0) {
      return left;
    }
    if (left == whole && right == whole) {
      return whole;
    }
    int leftOffsets = offsetCount(left);
    int rightOffsets = offsetCount(right);
    long count = (long) leftOffsets + rightOffsets;
    if (!charge(count)) {
      return whole;
    }
    int[] result = new int[(int) count];
    if (left == whole) {
      result[1] = leftLength;
    } else if (left != null) {
      System.arraycopy(left, 0, result, 0, leftOffsets);
    }
    if (right == whole) {
      result[leftOffsets] = leftLength;
      result[leftOffsets + 1] = leftLength + rightLength;
    } else if (right != null) {
      for (int i = 0; i < rightOffsets; i++) {
        result[leftOffsets + i] = leftLength + right[i];
      }
    }
    return result;
  }

  private int offsetCount(int[] tag) {
    return tag == null ? 0 : tag == whole ? 2 : tag.length;
  }

  private boolean hasTaggedArgument(Object root) {
    if (!isArgumentContainer(root)) {
      return false;
    }
    var pending = new ArrayDeque<Object>();
    var seen = new IdentityHashMap<Object, Boolean>();
    pending.push(root);
    while (!pending.isEmpty()) {
      Object value = pending.pop();
      if (seen.put(value, Boolean.TRUE) != null) {
        continue;
      }
      if (value instanceof Dict<?, ?> dict) {
        if (!charge(2L * dict.size())) {
          return false;
        }
        for (var entry : dict.entrySet()) {
          if (visitArgument(pending, entry.getKey()) || visitArgument(pending, entry.getValue())) {
            return true;
          }
        }
      } else {
        Sequence<?> sequence = (Sequence<?>) value;
        if (!charge(sequence.size())) {
          return false;
        }
        for (Object item : sequence) {
          if (visitArgument(pending, item)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private boolean visitArgument(ArrayDeque<Object> pending, Object value) {
    if (tag(value) != null) {
      return true;
    }
    if (isArgumentContainer(value)) {
      pending.push(value);
    }
    return false;
  }

  private static boolean isArgumentContainer(Object value) {
    return value instanceof StarlarkList<?> || value instanceof Tuple || value instanceof Dict;
  }

  void markRendered(Object value, int start, int end, boolean quoted, BitSet spans) {
    if (limited || start >= end) {
      return;
    }
    int[] ranges = tag(value);
    if (ranges == null) {
      return;
    }
    if (!(value instanceof String text)) {
      spans.set(start, end);
      return;
    }
    int bodyStart = quoted ? start + 1 : start;
    int bodyEnd = quoted ? end - 1 : end;
    int width = bodyEnd - bodyStart;
    if (width <= 0) {
      return;
    }
    if (ranges == whole) {
      spans.set(bodyStart, bodyEnd);
      return;
    }
    if (!charge(ranges.length)) {
      return;
    }
    if (quoted && width != text.length()) {
      if (charge(text.length())) {
        markEscaped(text, bodyStart, bodyEnd, ranges, spans);
      }
      return;
    }
    for (int i = 0; i < ranges.length && ranges[i] < width; i += 2) {
      spans.set(bodyStart + ranges[i], bodyStart + Math.min(ranges[i + 1], width));
    }
  }

  private static void markEscaped(String text, int start, int end, int[] ranges, BitSet spans) {
    int source = 0;
    long rendered = start;
    int spanStart = start;
    for (int i = 0; i < ranges.length; i++) {
      while (source < ranges[i]) {
        rendered += escapedWidth(text.charAt(source++));
      }
      if (rendered > end) {
        spans.set(start, end);
        return;
      }
      if (i % 2 == 0) {
        spanStart = (int) rendered;
      } else {
        spans.set(spanStart, (int) rendered);
      }
    }
    while (source < text.length()) {
      rendered += escapedWidth(text.charAt(source++));
    }
    if (rendered != end) {
      spans.set(start, end);
    }
  }

  // Match Printer.escapeCharacter so offsets follow UTF-16 characters into their quoted form.
  private static int escapedWidth(char c) {
    return switch (c) {
      case '"', '\\', '\r', '\n', '\t' -> 2;
      default -> c < 32 ? 4 : 1;
    };
  }

  private static boolean canTag(Object value) {
    return value instanceof String text && !text.isEmpty()
        || value instanceof StarlarkBytes bytes && !bytes.isEmpty();
  }

  private boolean reserveTag(Object value) {
    long retainedBytes = value instanceof String text
        ? 2L * text.length() : ((StarlarkBytes) value).size();
    if (tagCount >= maxTags || retainedBytes > remainingRetainedBytes) {
      limited = true;
      return false;
    }
    remainingRetainedBytes -= (int) retainedBytes;
    return true;
  }

  private int[] tag(Object value) {
    return bytesTags != null && value instanceof StarlarkBytes bytes
        ? bytesTags.get(bytes) : stringTags.get(value);
  }

  private void remember(Object value, int[] ranges) {
    if (value instanceof StarlarkBytes bytes) {
      // A slice can retain a larger backing array and a bytearray can grow after registration.
      if (bytesTags == null) {
        bytesTags = new MapMaker().weakKeys().makeMap();
      }
      bytesTags.put(bytes, ranges);
    } else {
      stringTags.put((String) value, ranges);
    }
    tagCount++;
  }

  private boolean charge(long work) {
    if (work > remainingWork) {
      limited = true;
      return false;
    }
    remainingWork -= (int) work;
    return true;
  }
}
