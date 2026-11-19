package net.starlark.java.eval;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/**
 * Per-run secret taint: which objects carry a value revealed from the vault, and whether the run
 * has branched on or produced an untaggable value from one.
 *
 * <p>Strings have no room for a flag, so tags are identity entries beside the value. {@code
 * StarlarkThread} holds this object only once the run has revealed something, so runs that never
 * reveal pay one field read per hooked interpreter operation. See {@link Taint}.
 *
 * <p>Tags are never trusted to be complete. The plaintext of every revealed value is kept too, and
 * sink text is scanned for it and for its common encodings: raw, URL-encoded, Base64, Base64URL and
 * hex. A run can also become <em>secret-dependent</em> (it branched on a secret, or a secret
 * produced a value that cannot carry a tag) or <em>saturated</em> (the tag table is full or taint
 * itself failed); either way, later sinks in the run count as tainted.
 *
 * <p>The host creates one instance per script execution and binds it under {@link #BINDING_NAME}.
 * Like the rest of a run, an instance is confined to one Java thread at a time. No method throws;
 * taint must never fail a customer request.
 */
public final class SecretTaint implements StarlarkValue {

  /** The script-binding name under which the host passes this object to the interpreter. */
  public static final String BINDING_NAME = "__vgs_secret_taint__";

  /**
   * The fixed mask that replaces a secret in output, whatever the secret's length. It also stands
   * for a whole text or argument when no secret can be located in it.
   */
  public static final String REDACTED = "***REDACTED***";

  /** The default limit on tagged objects, after which a run is saturated. */
  public static final int DEFAULT_TAG_LIMIT = 100_000;

  /**
   * Whole revealed values shorter than this are not matched by the value scan: a one- or two-digit
   * plaintext appears in nearly any text. Tags still protect them.
   */
  static final int MIN_PLAINTEXT_LENGTH = 3;

  private static final int MAX_DEPTH = 8;
  private static final int MAX_WALKED_ELEMENTS = 10_000;

  /** The output channels that are checked. */
  public enum Sink {
    PRINT,
    ERROR_TEXT
  }

  /** Why an output was blocked or would have been. */
  public enum Reason {
    TAGGED,
    VALUE_MATCH,
    SECRET_DEPENDENT,
    SATURATED
  }

  private static final Set<Class<?>> GLOBAL_MODULE_CLASSES = ConcurrentHashMap.newKeySet();

  private final TaintMode mode;
  private final TaintObserver observer;
  private final int tagLimit;
  private final Set<Object> tagged = Collections.newSetFromMap(new IdentityHashMap<>());
  // Text forms of revealed values, for scanning sink text. Lowercased forms are in hexForms.
  private final List<String> plaintextForms = new ArrayList<>();
  private final List<String> hexForms = new ArrayList<>();
  private boolean active;
  private boolean secretDependent;
  private boolean saturated;
  private int walkBudget;

  public SecretTaint(TaintMode mode, TaintObserver observer) {
    this(mode, observer, DEFAULT_TAG_LIMIT);
  }

  public SecretTaint(TaintMode mode, TaintObserver observer, int tagLimit) {
    this.mode = mode == null ? TaintMode.OFF : mode;
    this.observer = observer == null ? TaintObserver.NOOP : observer;
    this.tagLimit = tagLimit;
  }

  /**
   * Declares that {@code module} is a process-wide singleton, whose methods a tainted argument
   * must not taint. Without this, calling a stateless module with a secret would mark the module
   * as carrying one, and every later result from it would be tainted.
   */
  public static void registerGlobalModule(Object module) {
    GLOBAL_MODULE_CLASSES.add(module.getClass());
  }

  static boolean isGlobalModule(Object value) {
    return GLOBAL_MODULE_CLASSES.contains(value.getClass());
  }

  public TaintMode mode() {
    return mode;
  }

  /** Returns whether a source has fired in this run. */
  public boolean isActive() {
    return active;
  }

  /** Returns whether blocked output is really withheld, rather than only reported. */
  public boolean isEnforcing() {
    return mode == TaintMode.ENFORCE;
  }

  /** Returns whether the run has lost precise tracking; see the class comment. */
  public boolean isSaturated() {
    return saturated;
  }

  /** Returns whether the run branched on or computed an untaggable value from a secret. */
  public boolean isSecretDependent() {
    return secretDependent;
  }

  /** Marks the run secret-dependent: every later print and event counts as tainted. */
  public void markSecretDependent() {
    secretDependent = true;
  }

  /** Marks the run saturated: taint can no longer be trusted, so every sink counts as tainted. */
  void saturate() {
    saturated = true;
  }

  void markActive() {
    active = true;
  }

  /**
   * Tags {@code value} and, for containers, its elements. Returns the object to keep using, which
   * differs from {@code value} only for an unshareable copy of a memoized single-character string.
   */
  public Object tag(Object value) {
    try {
      return tagDeep(value, 0);
    } catch (RuntimeException | StackOverflowError e) {
      saturate();
      return value;
    }
  }

  /** Tags {@code value} alone, leaving its elements as they are. */
  void tagShallow(Object value) {
    if (isTaggable(value)) {
      addTag(value);
    }
  }

  /** Records a revealed value: tags it and remembers its plaintext for scanning sink text. */
  Object onSource(Object value) {
    try {
      rememberPlaintext(value, 0);
      return tagDeep(value, 0);
    } catch (RuntimeException | StackOverflowError e) {
      saturate();
      return value;
    }
  }

  /** Returns whether {@code value} is tagged, searching containers. Walk exhaustion counts. */
  public boolean isTainted(Object value) {
    return anyTainted(value);
  }

  /** Returns whether any of {@code values} is tagged, searching containers. */
  boolean anyTainted(Object... values) {
    walkBudget = MAX_WALKED_ELEMENTS;
    try {
      for (Object value : values) {
        if (walkHits(value, 0)) {
          return true;
        }
      }
      return false;
    } catch (RuntimeException | StackOverflowError e) {
      saturate();
      return true;
    }
  }

  /** Returns whether {@code value} itself is tagged, without searching inside it. */
  boolean isTaggedShallow(Object value) {
    return value != null && tagged.contains(value);
  }

  /**
   * Whether output built from {@code values} must count as tainted: a tag on any of them, or a
   * run-wide secret-dependent or saturated state. Reports a hit to the observer. Returns the
   * reason, or null if there is no hit from tags alone; callers scan the final text after this.
   */
  @Nullable
  public Reason checkOutput(Sink sink, Object... values) {
    Reason reason = null;
    if (saturated) {
      reason = Reason.SATURATED;
    } else if (secretDependent) {
      reason = Reason.SECRET_DEPENDENT;
    } else if (anyTainted(values)) {
      reason = Reason.TAGGED;
    }
    if (reason != null) {
      report(sink, reason);
    }
    return reason;
  }

  /**
   * The text to emit at {@code sink}, given the result of {@link #checkOutput} for the same output.
   * Scans the text for revealed values when no tag fired. Under {@link TaintMode#ENFORCE}, a
   * {@link Reason#VALUE_MATCH} or {@link Reason#TAGGED} hit returns the text with every known form
   * of a revealed value masked; if none can be located in it, or the reason is {@link
   * Reason#SECRET_DEPENDENT} or {@link Reason#SATURATED}, the whole text becomes {@link #REDACTED}.
   */
  public String sanitize(Sink sink, String text, @Nullable Reason earlierHit) {
    Reason reason = earlierHit;
    if (reason == null && containsSecretValue(text)) {
      reason = Reason.VALUE_MATCH;
      report(sink, reason);
    }
    if (reason == null || !isEnforcing()) {
      return text;
    }
    if (reason == Reason.SECRET_DEPENDENT || reason == Reason.SATURATED) {
      return REDACTED;
    }
    String masked = mask(text);
    return masked.equals(text) ? REDACTED : masked;
  }

  /**
   * Returns {@code arg}'s printed {@code text} for a print argument under {@link
   * TaintMode#ENFORCE}: unchanged if {@code arg} carries no tag, masked in place if it does and a
   * known form is found in the text, and {@link #REDACTED} if it does and none is found (for
   * example, a transformed value). Does not report; the caller reports through {@link
   * #checkOutput}.
   */
  String maskTaggedArgument(Object arg, String text) {
    if (!isEnforcing() || !anyTainted(arg)) {
      return text;
    }
    String masked = mask(text);
    return masked.equals(text) ? REDACTED : masked;
  }

  /**
   * Replaces every occurrence of each known plaintext form of a revealed value (raw, URL-encoded,
   * Base64, Base64URL, and hex in either case) with {@link #REDACTED}. Matches are found against
   * the original text and overlapping or touching ones are merged into one mask, so the mask is
   * never re-matched and no tail of an overlapped form is left behind. Returns {@code text}
   * itself when no form occurs in it.
   */
  public String mask(String text) {
    if (text == null || text.isEmpty() || (plaintextForms.isEmpty() && hexForms.isEmpty())) {
      return text;
    }
    try {
      List<int[]> spans = new ArrayList<>();
      collectSpans(text, plaintextForms, false, spans);
      collectSpans(text, hexForms, true, spans);
      if (spans.isEmpty()) {
        return text;
      }
      spans.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(b[1], a[1]));
      StringBuilder sb = new StringBuilder(text.length());
      int copied = 0;
      int start = spans.get(0)[0];
      int end = spans.get(0)[1];
      for (int i = 1; i <= spans.size(); i++) {
        if (i < spans.size() && spans.get(i)[0] <= end) {
          end = Math.max(end, spans.get(i)[1]);
          continue;
        }
        sb.append(text, copied, start).append(REDACTED);
        copied = end;
        if (i < spans.size()) {
          start = spans.get(i)[0];
          end = spans.get(i)[1];
        }
      }
      return sb.append(text, copied, text.length()).toString();
    } catch (RuntimeException | StackOverflowError e) {
      saturate();
      return REDACTED;
    }
  }

  // Every occurrence of every form, longest forms first, as [start, end) spans.
  private static void collectSpans(
      String text, List<String> forms, boolean ignoreCase, List<int[]> spans) {
    List<String> longestFirst = new ArrayList<>(forms);
    longestFirst.sort((a, b) -> Integer.compare(b.length(), a.length()));
    for (String form : longestFirst) {
      int n = form.length();
      for (int i = 0; i + n <= text.length(); i++) {
        if (text.regionMatches(ignoreCase, i, form, 0, n)) {
          spans.add(new int[] {i, i + n});
        }
      }
    }
  }

  /**
   * Scans {@code text} for revealed values, reporting a match to the observer. Returns whether the
   * output must be withheld, which is only ever true under {@link TaintMode#ENFORCE}.
   */
  public boolean blocksText(Sink sink, String text) {
    if (!containsSecretValue(text)) {
      return false;
    }
    report(sink, Reason.VALUE_MATCH);
    return isEnforcing();
  }

  /**
   * Sanitizes host-side error text, where value identity is gone: only the value-set scan applies.
   */
  public String sanitizeError(String text) {
    return sanitize(Sink.ERROR_TEXT, text, null);
  }

  /** Returns whether {@code text} contains a revealed value in a checked encoding. */
  public boolean containsSecretValue(String text) {
    if (text == null || text.isEmpty()) {
      return false;
    }
    try {
      for (String form : plaintextForms) {
        if (text.contains(form)) {
          return true;
        }
      }
      if (!hexForms.isEmpty()) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String form : hexForms) {
          if (lower.contains(form)) {
            return true;
          }
        }
      }
      return false;
    } catch (RuntimeException | StackOverflowError e) {
      saturate();
      return true;
    }
  }

  private void report(Sink sink, Reason reason) {
    try {
      observer.onHit(sink, reason);
    } catch (RuntimeException | Error e) {
      saturate();
    }
  }

  // Returns the object to use; see tag().
  private Object tagDeep(Object value, int depth) {
    if (value instanceof String s) {
      if (s.isEmpty()) {
        return value;
      }
      String safe = StringModule.isMemoized(s) ? new String(s) : s;
      addTag(safe);
      return safe;
    }
    if (!isTaggable(value)) {
      return value;
    }
    addTag(value);
    if (depth < MAX_DEPTH) {
      for (Object child : children(value)) {
        tagDeep(child, depth + 1);
      }
    }
    return value;
  }

  private void addTag(Object value) {
    if (tagged.size() >= tagLimit) {
      saturate();
      return;
    }
    tagged.add(value);
  }

  // Returns true if a tag was found, or the walk ran out of budget or depth.
  private boolean walkHits(Object value, int depth) {
    if (value == null || value instanceof Boolean || value instanceof NoneType) {
      return false;
    }
    if (tagged.contains(value)) {
      return true;
    }
    if (depth >= MAX_DEPTH || --walkBudget < 0) {
      return true;
    }
    for (Object child : children(value)) {
      if (walkHits(child, depth + 1)) {
        return true;
      }
    }
    return false;
  }

  // The interpreter's own containers only: walking must never run script code or use up an
  // iterator, and bytes and ranges hold no tagged elements.
  private static Iterable<?> children(Object value) {
    if (value instanceof StarlarkBytes || value instanceof RangeList) {
      return List.of();
    }
    if (value instanceof Dict<?, ?> dict) {
      List<Object> pairs = new ArrayList<>(dict.size() * 2);
      for (Map.Entry<?, ?> e : dict.entrySet()) {
        pairs.add(e.getKey());
        pairs.add(e.getValue());
      }
      return pairs;
    }
    if (value instanceof StarlarkList<?> || value instanceof Tuple || value instanceof StarlarkSet<?>) {
      return (Iterable<?>) value;
    }
    return List.of();
  }

  /** Returns whether {@code value} is a fresh object that a tag can stick to. */
  static boolean isTaggable(Object value) {
    if (value == null || value instanceof Boolean || value instanceof NoneType) {
      return false;
    }
    if (value instanceof String s) {
      return !s.isEmpty() && !StringModule.isMemoized(s);
    }
    if (value instanceof StarlarkInt i) {
      return !i.isSharedSmallValue();
    }
    return true;
  }

  private void rememberPlaintext(Object value, int depth) {
    if (depth >= MAX_DEPTH) {
      return;
    }
    if (value instanceof String s) {
      rememberString(s);
    } else if (value instanceof StarlarkBytes b) {
      rememberString(new String(b.toByteArray(), StandardCharsets.UTF_8));
    } else {
      for (Object child : children(value)) {
        rememberPlaintext(child, depth + 1);
      }
    }
  }

  private void rememberString(String s) {
    if (s.length() < MIN_PLAINTEXT_LENGTH) {
      return;
    }
    addForm(plaintextForms, s);
    addForm(plaintextForms, URLEncoder.encode(s, StandardCharsets.UTF_8));
    byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
    // Unpadded, so a form cut off from its padding is still found, and padded, so that masking
    // takes the "=" padding with the form instead of leaving it behind.
    for (Base64.Encoder encoder : List.of(Base64.getEncoder(), Base64.getUrlEncoder())) {
      String padded = encoder.encodeToString(utf8);
      addForm(plaintextForms, padded);
      addForm(plaintextForms, stripPadding(padded));
    }
    addForm(hexForms, toHex(utf8));
  }

  private static void addForm(List<String> forms, String form) {
    if (form.length() >= MIN_PLAINTEXT_LENGTH && !forms.contains(form)) {
      forms.add(form);
    }
  }

  private static String stripPadding(String base64) {
    int end = base64.length();
    while (end > 0 && base64.charAt(end - 1) == '=') {
      end--;
    }
    return base64.substring(0, end);
  }

  private static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(Character.forDigit((b >> 4) & 0xf, 16));
      sb.append(Character.forDigit(b & 0xf, 16));
    }
    return sb.toString();
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    printer.append("<secret taint>");
  }

  @Override
  public boolean isImmutable() {
    return true;
  }
}
