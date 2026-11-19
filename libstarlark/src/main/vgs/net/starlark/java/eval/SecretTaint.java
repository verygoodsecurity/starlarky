package net.starlark.java.eval;

import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
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
 * produced a value that cannot carry a tag) or <em>saturated</em> (a resource budget ran out or
 * taint itself failed); either way, later sinks in the run count as tainted.
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
  private static final int MAX_RUN_WALKED_ELEMENTS = 1_000_000;
  private static final int MAX_SOURCE_LENGTH = 16 * 1024;
  private static final int MAX_FORMS = 1024;
  private static final int MAX_FORM_CHARS = 256 * 1024;
  private static final int MAX_SCAN_TEXT_LENGTH = 1024 * 1024;
  private static final long MAX_SCAN_WORK = 32_000_000;
  private static final int MAX_MATCH_SPANS = 10_000;
  private static final int MAX_ERROR_SPANS = 64;
  private static final int MAX_CONCAT_SPANS = 32;

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
  // Precise ranges for String concatenation. A tag without ranges covers the whole value.
  private final Map<String, int[]> concatSpans = new IdentityHashMap<>();
  // Text forms of revealed values, for scanning sink text. Lowercased forms are in hexForms.
  private final Set<String> plaintextForms = new HashSet<>();
  private final Set<String> hexForms = new HashSet<>();
  private int formChars;
  private boolean active;
  private boolean secretDependent;
  private boolean saturated;
  private int walkBudget;
  private int remainingWalkWork = MAX_RUN_WALKED_ELEMENTS;
  private long remainingScanWork = MAX_SCAN_WORK;
  private boolean taintedError;
  @Nullable private ErrorSpans renderedError;
  @Nullable private WeakReference<RuntimeException> taintedNativeError;

  private record ErrorSpans(WeakReference<String> original, List<int[]> spans) {}

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

  /** Marks the run secret-dependent: subsequent diagnostic output counts as tainted. */
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
    walkBudget = MAX_WALKED_ELEMENTS;
    try {
      return tagDeep(value, 0);
    } catch (RuntimeException | StackOverflowError e) {
      saturate();
      return value;
    }
  }

  /** Tags a string assembled by concatenation, keeping the operand boundaries. */
  String tagConcatenation(String left, String right, String result) {
    if (!isEnforcing()) {
      return (String) tag(result);
    }
    int[] leftSpans = rawStringSpans(left);
    int[] rightSpans = rawStringSpans(right);
    String taggedResult = (String) tag(result);
    int length = leftSpans.length + rightSpans.length;
    if (!isSaturated() && length <= 2 * MAX_CONCAT_SPANS) {
      int[] combined = Arrays.copyOf(leftSpans, length);
      for (int i = 0; i < rightSpans.length; i++) {
        combined[leftSpans.length + i] = left.length() + rightSpans[i];
      }
      concatSpans.put(taggedResult, combined);
    }
    return taggedResult;
  }

  private int[] rawStringSpans(String value) {
    int[] spans = concatSpans.get(value);
    return spans != null ? spans : isTaggedShallow(value) ? new int[] {0, value.length()} : new int[0];
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
      walkBudget = MAX_WALKED_ELEMENTS;
      rememberPlaintext(value, 0);
      walkBudget = MAX_WALKED_ELEMENTS;
      return tagDeep(value, 0);
    } catch (RuntimeException | StackOverflowError e) {
      saturate();
      return value;
    }
  }

  /** Captures the original list elements so a provider can return a changed list in place. */
  Object[] snapshotSourceBatch(Object input) {
    if (saturated || !(input instanceof StarlarkList<?> || input instanceof Tuple)) {
      return null;
    }
    try {
      List<?> aliases = (List<?>) input;
      return spendSourceBatchWork(aliases.size()) ? aliases.toArray() : null;
    } catch (RuntimeException | StackOverflowError e) {
      markActive();
      saturate();
      return null;
    }
  }

  /** Tracks a source result, excluding known passthrough aliases. */
  Object onSource(Object value, Object passthroughInput) {
    try {
      if (isUnchangedAlias(value, passthroughInput)) {
        return value;
      }
      if (passthroughInput instanceof Object[] aliases && value instanceof StarlarkList<?> batch
          && aliases.length == batch.size()) {
        return recordSourceBatch(aliases, batch);
      }
      return recordSource(value);
    } catch (RuntimeException | StackOverflowError e) {
      markActive();
      saturate();
      return value;
    }
  }

  private Object recordSourceBatch(Object[] aliases, StarlarkList<?> batch) {
    if (!spendSourceBatchWork(aliases.length)) {
      return batch;
    }
    int unchanged = 0;
    for (int i = 0; i < aliases.length; i++) {
      if (isUnchangedAlias(batch.get(i), aliases[i])) {
        unchanged++;
      }
    }
    if (unchanged == aliases.length) {
      return batch;
    }
    if (unchanged == 0) {
      return recordSource(batch);
    }
    if (!spendSourceBatchWork(aliases.length)) {
      return batch;
    }
    Object[] replacements = null;
    for (int i = 0; i < aliases.length && !saturated; i++) {
      Object element = batch.get(i);
      if (!isUnchangedAlias(element, aliases[i])) {
        Object taggedElement = recordSource(element);
        if (taggedElement != element) {
          if (replacements == null) {
            if (!spendSourceBatchWork(aliases.length)) {
              return batch;
            }
            replacements = batch.toArray();
          }
          replacements[i] = taggedElement;
        }
      }
    }
    return replacements == null ? batch : StarlarkList.wrap(batch.mutability(), replacements);
  }

  private boolean spendSourceBatchWork(int elements) {
    if (elements <= MAX_WALKED_ELEMENTS && spendWalkWork(elements)) {
      return true;
    }
    markActive();
    saturate();
    return false;
  }

  private static boolean isUnchangedAlias(Object value, Object input) {
    return value instanceof String && value == input;
  }

  private Object recordSource(Object value) {
    markActive();
    if (value instanceof Boolean || value instanceof StarlarkInt && !isTaggable(value)) {
      markSecretDependent();
    }
    return onSource(value);
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
    return checkOutput(sink, false, values);
  }

  @Nullable
  private Reason checkOutput(Sink sink, boolean renderedTag, Object[] values) {
    Reason reason = null;
    if (!saturated && secretDependent) {
      reason = Reason.SECRET_DEPENDENT;
    } else if (!saturated && (renderedTag || anyTainted(values))) {
      reason = Reason.TAGGED;
    }
    if (saturated) {
      reason = Reason.SATURATED;
    }
    if (reason != null) {
      report(sink, reason);
    }
    return reason;
  }

  String sanitizePrint(Printer printer, Object... values) {
    OutputPrinter output =
        printer instanceof OutputPrinter tracked && tracked.taint == this ? tracked : null;
    // A plain printer could have triggered the first reveal. Its output has no precise ranges.
    Reason hit = checkOutput(Sink.PRINT, output == null || !output.spans.isEmpty(), values);
    return output != null && isEnforcing() && hit == Reason.TAGGED
        ? output.maskedText()
        : sanitize(Sink.PRINT, printer.toString(), hit);
  }

  /**
   * The text to emit at {@code sink}, given the result of {@link #checkOutput} for the same output.
   * Scans the text for revealed values when no tag fired. Under {@link TaintMode#ENFORCE}, a
   * {@link Reason#VALUE_MATCH} hit masks every occurrence of a known form. For other hits, this
   * method has no rendering provenance, so the whole text becomes {@link #REDACTED}.
   */
  public String sanitize(Sink sink, String text, @Nullable Reason earlierHit) {
    if (mode == TaintMode.OFF) {
      return text;
    }
    if (earlierHit == Reason.SATURATED || earlierHit == Reason.SECRET_DEPENDENT
        || (earlierHit == Reason.TAGGED && isEnforcing())) {
      return isEnforcing() ? REDACTED : text;
    }
    List<int[]> spans = isEnforcing() ? new ArrayList<>() : null;
    int matches = scan(text, spans);
    if (saturated) {
      report(sink, Reason.SATURATED);
    } else if (earlierHit == null && matches > 0) {
      report(sink, Reason.VALUE_MATCH);
    }
    if (spans == null) {
      return text;
    }
    return saturated || (earlierHit == Reason.VALUE_MATCH && spans.isEmpty())
        ? REDACTED
        : applySpans(text, spans);
  }

  /**
   * Renders the original text and records which rendered ranges came from tagged values. Tags
   * cover an opaque value's whole rendering. The value scan is additional evidence, never a reason
   * to show parts of a tagged value.
   */
  static final class OutputPrinter extends Printer {
    private final SecretTaint taint;
    private final StringBuilder buffer;
    private final List<int[]> spans = new ArrayList<>();

    OutputPrinter(SecretTaint taint, StarlarkThread thread) {
      this(taint, thread, new StringBuilder());
    }

    private OutputPrinter(SecretTaint taint, StarlarkThread thread, StringBuilder buffer) {
      super(buffer, thread);
      this.taint = taint;
      this.buffer = buffer;
    }

    OutputPrinter appendValue(String value) {
      int offset = buffer.length();
      append(value);
      if (taint.isTaggedShallow(value)) {
        addStringSpans(value, offset);
      }
      return this;
    }

    @Override
    public Printer appendRenderedValue(Object value, StarlarkThread producer) {
      boolean current = isCurrentDiagnosticThread(producer);
      if (current && value instanceof String text) {
        return appendValue(text);
      }
      int offset = buffer.length();
      super.appendRenderedValue(value, producer);
      if (!current || taint.isTainted(value)) {
        addSpan(offset, buffer.length());
      }
      return this;
    }

    private void addStringSpans(String value, int offset) {
      int[] stringSpans = taint.rawStringSpans(value);
      for (int i = 0; i < stringSpans.length; i += 2) {
        addSpan(offset + stringSpans[i], offset + stringSpans[i + 1]);
      }
    }

    @Override
    public Printer debugPrint(Object value, StarlarkThread thread) {
      if (value instanceof String s) {
        return appendValue(s);
      }
      int start = buffer.length();
      int savedSpans = spans.size();
      super.debugPrint(value, thread);
      if (taint.isTaggedShallow(value)
          || (taint.isEnforcing() && savedSpans == spans.size() && taint.isTainted(value))) {
        replaceSpans(savedSpans, start, buffer.length());
      }
      return this;
    }

    @Override
    public Printer repr(Object value, StarlarkSemantics semantics) {
      int start = buffer.length();
      int savedSpans = spans.size();
      super.repr(value, semantics);
      if (taint.isTaggedShallow(value)) {
        if (value instanceof String s) {
          int bodyStart = start + 1;
          int bodyEnd = buffer.length() - 1;
          // Escapes expand the string. When it grew, the original offsets are unsafe to reuse.
          if (bodyEnd - bodyStart == s.length()) {
            addStringSpans(s, bodyStart);
          } else {
            replaceSpans(savedSpans, bodyStart, bodyEnd);
          }
        } else {
          replaceSpans(savedSpans, start, buffer.length());
        }
      }
      return this;
    }

    private void replaceSpans(int from, int start, int end) {
      spans.subList(from, spans.size()).clear();
      addSpan(start, end);
    }

    private void addSpan(int start, int end) {
      if (spans.size() >= MAX_MATCH_SPANS) {
        taint.saturate();
      } else {
        spans.add(new int[] {start, end});
      }
    }

    /** Redacts a known hit, falling back to the whole text if the tag could not be located. */
    String maskedText() {
      if (taint.isSaturated()) {
        taint.report(Sink.PRINT, Reason.SATURATED);
        return REDACTED;
      }
      return taint.isSecretDependent() || spans.isEmpty()
          ? REDACTED
          : taint.maskOutput(Sink.PRINT, toString(), spans);
    }
  }

  /**
   * Replaces every occurrence of each known plaintext form of a revealed value (raw, URL-encoded,
   * Base64, Base64URL, and hex in either case) with {@link #REDACTED}. Matches are found against
   * the original text and overlapping or touching ones are merged into one mask, so the mask is
   * never re-matched and no tail of an overlapped form is left behind. Returns {@code text}
   * itself when no form occurs in it.
   */
  public String mask(String text) {
    return mask(text, new ArrayList<>());
  }

  private String mask(String text, List<int[]> spans) {
    scan(text, spans);
    return saturated ? REDACTED : applySpans(text, spans);
  }

  private String maskOutput(Sink sink, String text, List<int[]> spans) {
    String masked = mask(text, spans);
    if (saturated) {
      report(sink, Reason.SATURATED);
    }
    return masked;
  }

  private String applySpans(String text, List<int[]> spans) {
    if (spans.isEmpty()) {
      return text;
    }
    try {
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

  private int scan(String text, @Nullable List<int[]> spans) {
    int matches = spans == null ? 0 : spans.size();
    if (saturated || text == null || text.isEmpty() || (formChars == 0 && matches == 0)) {
      return matches;
    }
    if (text.length() > MAX_SCAN_TEXT_LENGTH) {
      saturate();
      return matches;
    }
    try {
      matches = collectSpans(text, plaintextForms, spans, matches);
      // Folding visits every code unit and can copy it into and out of a char array.
      if (!saturated && !hexForms.isEmpty() && spendScanWork(3L * text.length())) {
        matches = collectSpans(foldHex(text), hexForms, spans, matches);
      }
      return matches;
    } catch (RuntimeException | StackOverflowError e) {
      saturate();
      return matches;
    }
  }

  private int collectSpans(String text, Set<String> forms, @Nullable List<int[]> spans, int matches) {
    for (String form : forms) {
      int starts = text.length() - form.length() + 1;
      // Include the per-form probe even when the form is longer than the diagnostic.
      long work = 1 + (long) Math.max(starts, 0) * form.length();
      if (!spendScanWork(work)) {
        return matches;
      }
      if (starts <= 0) {
        continue;
      }
      for (int i = text.indexOf(form); i >= 0; i = text.indexOf(form, i + 1)) {
        if (matches >= MAX_MATCH_SPANS) {
          saturate();
          return matches;
        }
        matches++;
        if (spans != null) {
          spans.add(new int[] {i, i + form.length()});
        }
      }
    }
    return matches;
  }

  private boolean spendScanWork(long work) {
    if (work > remainingScanWork) {
      saturate();
      return false;
    }
    remainingScanWork -= work;
    return true;
  }

  private static String foldHex(String text) {
    char[] folded = null;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c >= 'A' && c <= 'F') {
        if (folded == null) {
          folded = text.toCharArray();
        }
        folded[i] = (char) (c + ('a' - 'A'));
      }
    }
    // Only fold ASCII hex digits so match offsets remain valid for the original text.
    return folded == null ? text : new String(folded);
  }

  /**
   * Scans {@code text} for revealed values, reporting a match to the observer. Returns whether the
   * output must be withheld, which is only ever true under {@link TaintMode#ENFORCE}.
   */
  public boolean blocksText(Sink sink, String text) {
    if (mode == TaintMode.OFF || !containsSecretValue(text)) {
      return false;
    }
    report(sink, saturated ? Reason.SATURATED : Reason.VALUE_MATCH);
    return isEnforcing();
  }

  /**
   * Sanitizes host-side error text. Supply the original {@link EvalException#getMessage()} from
   * {@code fail()} to preserve known spans. A wrapped or reformatted tagged error is masked whole
   * because its offsets are no longer known. Untagged errors use the value scan.
   */
  public String sanitizeError(String text) {
    if (mode == TaintMode.OFF || text == null) {
      return text;
    }
    if (checkOutput(Sink.ERROR_TEXT) != null) {
      return isEnforcing() ? REDACTED : text;
    }
    if (!taintedError) {
      String sanitized = sanitize(Sink.ERROR_TEXT, text, null);
      return saturated && isEnforcing() ? REDACTED : sanitized;
    }
    report(Sink.ERROR_TEXT, Reason.TAGGED);
    if (!isEnforcing()) {
      return text;
    }
    if (saturated || renderedError == null || renderedError.original().get() != text) {
      return REDACTED;
    }
    return maskOutput(Sink.ERROR_TEXT, text, new ArrayList<>(renderedError.spans()));
  }

  /** Remembers the offsets from the most recent tagged fail() without retaining its raw text. */
  void noteRenderedError(EvalException failure, Printer printer) {
    if (saturated || secretDependent
        || !(printer instanceof OutputPrinter output) || output.taint != this) {
      noteUnlocatedError(failure);
    } else if (!output.spans.isEmpty()) {
      failure.markTaintedDiagnostic();
      taintedError = true;
      renderedError =
          output.spans.size() <= MAX_ERROR_SPANS
              ? new ErrorSpans(new WeakReference<>(failure.getMessage()), List.copyOf(output.spans))
              : null;
    }
  }

  /** A native error can format its inputs without carrying their tags into its message. */
  void noteBuiltinError(Object receiver, Object[] args, Throwable failure) {
    if (renderedError != null
        && failure instanceof EvalException error
        && renderedError.original().get() == error.getMessage()) {
      return;
    }
    if (anyTainted(receiver) || anyTainted(args)) {
      noteUnlocatedError(failure);
    }
  }

  /** Records a diagnostic built by the interpreter from tagged values. */
  void noteValueError(EvalException failure, Object... values) {
    if (anyTainted(values)) {
      noteUnlocatedError(failure);
    }
  }

  private void noteUnlocatedError(Throwable failure) {
    if (failure instanceof EvalException error) {
      error.markTaintedDiagnostic();
    } else if (failure instanceof RuntimeException error
        && !(error instanceof Starlark.UncheckedEvalException)) {
      taintedNativeError = new WeakReference<>(error);
    }
    taintedError = true;
    renderedError = null;
  }

  /** Whether this run marked the given native failure when it handled tainted values. */
  public boolean hasTaintedNativeDiagnostic(Throwable failure) {
    return failure != null && taintedNativeError != null && taintedNativeError.get() == failure;
  }

  /** Returns whether {@code text} matched a checked encoding or the scan could not finish. */
  public boolean containsSecretValue(String text) {
    return scan(text, null) > 0 || saturated;
  }

  private void report(Sink sink, Reason reason) {
    try {
      observer.onHit(sink, reason);
    } catch (RuntimeException | StackOverflowError e) {
      saturate();
    }
  }

  // Returns the object to use; see tag().
  private Object tagDeep(Object value, int depth) {
    if (!visit()) {
      return value;
    }
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
    if (depth >= MAX_DEPTH) {
      saturateIfSkippedChildren(value);
    } else if (value instanceof Dict<?, ?> dict) {
      for (Map.Entry<?, ?> entry : dict.entrySet()) {
        if (saturated) {
          break;
        }
        tagDeep(entry.getKey(), depth + 1);
        tagDeep(entry.getValue(), depth + 1);
      }
    } else {
      for (Object child : children(value)) {
        if (saturated) {
          break;
        }
        tagDeep(child, depth + 1);
      }
    }
    return value;
  }

  private void addTag(Object value) {
    if (saturated) {
      return;
    }
    if (tagged.size() >= tagLimit) {
      saturate();
      return;
    }
    tagged.add(value);
    // Other operations do not prove which parts of a string came from tainted inputs.
    concatSpans.remove(value);
  }

  // Returns true if a tag was found, or the walk ran out of budget or depth.
  private boolean walkHits(Object value, int depth) {
    if (!visit()) {
      return true;
    }
    if (value == null || value instanceof Boolean || value instanceof NoneType) {
      return false;
    }
    if (tagged.contains(value)) {
      return true;
    }
    if (depth >= MAX_DEPTH) {
      return true;
    }
    if (value instanceof Dict<?, ?> dict) {
      for (Map.Entry<?, ?> entry : dict.entrySet()) {
        if (walkHits(entry.getKey(), depth + 1) || walkHits(entry.getValue(), depth + 1)) {
          return true;
        }
      }
    } else {
      for (Object child : children(value)) {
        if (walkHits(child, depth + 1)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean visit() {
    if (saturated) {
      return false;
    }
    if (walkBudget == 0) {
      saturate();
      return false;
    }
    walkBudget--;
    return spendWalkWork(1);
  }

  private boolean spendWalkWork(int elements) {
    if (saturated || elements > remainingWalkWork) {
      saturate();
      return false;
    }
    remainingWalkWork -= elements;
    return true;
  }

  private void saturateIfSkippedChildren(Object value) {
    if (value instanceof Dict<?, ?> dict ? !dict.isEmpty() : children(value).iterator().hasNext()) {
      saturate();
    }
  }

  // The interpreter's own containers only: walking must never run script code or use up an
  // iterator, and bytes and ranges hold no tagged elements.
  private static Iterable<?> children(Object value) {
    if (value instanceof StarlarkBytes || value instanceof RangeList) {
      return List.of();
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
    if (!visit()) {
      return;
    }
    if (value instanceof String s) {
      rememberString(s);
    } else if (value instanceof StarlarkBytes b) {
      if (b.size() > MAX_SOURCE_LENGTH) {
        saturate();
      } else {
        rememberBytes(b.toByteArray());
      }
    } else if (depth >= MAX_DEPTH) {
      saturateIfSkippedChildren(value);
    } else if (value instanceof Dict<?, ?> dict) {
      for (Map.Entry<?, ?> entry : dict.entrySet()) {
        if (saturated) {
          break;
        }
        rememberPlaintext(entry.getKey(), depth + 1);
        rememberPlaintext(entry.getValue(), depth + 1);
      }
    } else {
      for (Object child : children(value)) {
        if (saturated) {
          break;
        }
        rememberPlaintext(child, depth + 1);
      }
    }
  }

  private void rememberString(String s) {
    if (s.length() < MIN_PLAINTEXT_LENGTH) {
      return;
    }
    if (s.length() > MAX_SOURCE_LENGTH) {
      saturate();
      return;
    }
    rememberTextForms(s);
    if (!saturated) {
      rememberByteForms(s.getBytes(StandardCharsets.UTF_8));
    }
  }

  private void rememberBytes(byte[] bytes) {
    String text = new String(bytes, StandardCharsets.UTF_8);
    boolean validUtf8 = Arrays.equals(bytes, text.getBytes(StandardCharsets.UTF_8));
    int plaintextLength = validUtf8 ? text.length() : bytes.length;
    if (plaintextLength < MIN_PLAINTEXT_LENGTH) {
      return;
    }
    // A replacement-decoded string does not represent the revealed bytes.
    if (validUtf8) {
      rememberTextForms(text);
    }
    rememberByteForms(bytes);
  }

  private void rememberTextForms(String s) {
    addForm(plaintextForms, s);
    if (!saturated) {
      addForm(plaintextForms, URLEncoder.encode(s, StandardCharsets.UTF_8));
    }
  }

  private void rememberByteForms(byte[] bytes) {
    // Unpadded, so a form cut off from its padding is still found, and padded, so that masking
    // takes the "=" padding with the form instead of leaving it behind.
    for (Base64.Encoder encoder : List.of(Base64.getEncoder(), Base64.getUrlEncoder())) {
      if (saturated) {
        return;
      }
      String padded = encoder.encodeToString(bytes);
      addForm(plaintextForms, padded);
      addForm(plaintextForms, stripPadding(padded));
    }
    if (!saturated) {
      addForm(hexForms, toHex(bytes));
    }
  }

  private void addForm(Set<String> forms, String form) {
    if (saturated || form.length() < MIN_PLAINTEXT_LENGTH || forms.contains(form)) {
      return;
    }
    if (plaintextForms.size() + hexForms.size() >= MAX_FORMS
        || form.length() > MAX_FORM_CHARS - formChars) {
      saturate();
      return;
    }
    forms.add(form);
    formChars += form.length();
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
