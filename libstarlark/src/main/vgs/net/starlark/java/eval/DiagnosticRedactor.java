package net.starlark.java.eval;

import com.google.auto.value.AutoValue;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.starlark.java.syntax.TokenKind;

/** Diagnostic masking for one logical Larky run. The host retains this through error handling. */
public final class DiagnosticRedactor {
  public static final String BINDING_NAME = "__vgs_diagnostic_redactor__";
  public static final String MASK = "***REDACTED***";

  public enum Mode { OFF, OBSERVE, ENFORCE }
  public enum Sink { PRINT, ERROR_TEXT }
  public enum Reason { VALUE_MATCH, TAGGED, LIMIT }

  @FunctionalInterface
  public interface Observer {
    void onHit(Sink sink, Reason reason);
  }

  public static final class Scope implements AutoCloseable {
    @Nullable private DiagnosticRedactor redactor;
    @Nullable private Attachment attachment;

    private Scope(@Nullable DiagnosticRedactor redactor) {
      this.redactor = redactor;
    }

    public boolean isTracking() {
      return redactor != null;
    }

    public String sanitizePrint(String message) {
      if (redactor == null) {
        return message;
      }
      TaggedText rendered = attachment == null ? null : attachment.print;
      if (attachment != null) {
        attachment.print = null;
      }
      return redactor.sanitize(Sink.PRINT, message, rendered);
    }

    public void registerRevealed(List<Object> values) {
      if (redactor != null) {
        redactor.registerRevealed(values);
      }
    }

    /** Registers confirmed hits before returning the provider result to the script. */
    public Object registerOutcome(StarlarkThread thread, Object value, List<Object> hits) {
      if (redactor == null || hits.isEmpty()) {
        return value;
      }
      redactor.registerRevealed(hits);
      if (redactor.isLimited()) {
        return value;
      }
      if (redactor.taint == null) {
        redactor.taint = new BinaryTaint(redactor.limits.maxForms(),
            redactor.limits.maxRegistrationInput());
      }
      Object tagged = redactor.taint.onReveal(value, hits);
      if (attachment == null && redactor.taint.hasTags()) {
        attach(thread);
      }
      return tagged;
    }

    private void attach(StarlarkThread thread) {
      Scope previous = thread.getThreadLocal(Scope.class);
      EvaluationExtensions.BinaryObserver current = thread.binaryObserver;
      EvaluationExtensions.BinaryObserver external =
          previous != null && previous.attachment.observer == current
              ? previous.attachment.external : current;
      EvaluationExtensions.BinaryObserver observer = external == null
          ? this::afterBinary
          : (execution, op, left, right, result, augmented) ->
              afterBinary(execution, op, left, right,
                  external.afterBinary(execution, op, left, right, result, augmented), augmented);
      attachment = new Attachment(
          () -> thread.setThreadLocal(Scope.class, previous),
          EvaluationExtensions.observeBinary(thread, observer),
          observer, external);
      thread.setThreadLocal(Scope.class, this);
    }

    private Object afterBinary(
        StarlarkThread thread, TokenKind op, Object left, Object right, Object result,
        boolean augmented) {
      return redactor == null || redactor.taint == null || redactor.isLimited()
          ? result
          : redactor.taint.afterBinary(thread, op, left, right, result, augmented);
    }

    @Nullable
    DiagnosticPrinter printer() {
      return redactor == null || redactor.taint == null || redactor.isLimited()
          ? null
          : new DiagnosticPrinter(redactor.taint, redactor.limits.maxTextLength());
    }

    void print(StarlarkThread thread, DiagnosticPrinter printer) {
      String text = printer.toString();
      if (printer.exceedsLimit() && redactor != null) {
        redactor.limited = true;
      }
      if (attachment == null || printer.spans().isEmpty()) {
        thread.getPrintHandler().print(thread, text);
        return;
      }
      Attachment bound = attachment;
      TaggedText previous = bound.print;
      bound.print = new TaggedText(text, printer.spans());
      try {
        thread.getPrintHandler().print(thread, text);
      } finally {
        bound.print = previous;
      }
    }

    void recordFailure(EvalException failure, DiagnosticPrinter printer) {
      if (redactor == null) {
        return;
      }
      if (printer.exceedsLimit()) {
        redactor.limited = true;
      } else if (!printer.spans().isEmpty()) {
        redactor.noteFailure(failure, printer.spans());
      }
    }

    @Override
    public void close() {
      try {
        if (attachment != null) {
          try {
            attachment.binary.close();
          } finally {
            attachment.restore.run();
          }
        }
      } finally {
        attachment = null;
        redactor = null;
      }
    }
  }

  private static final class Attachment {
    private final Runnable restore;
    private final EvaluationExtensions.Scope binary;
    private final EvaluationExtensions.BinaryObserver observer;
    @Nullable private final EvaluationExtensions.BinaryObserver external;
    @Nullable private TaggedText print;

    private Attachment(Runnable restore, EvaluationExtensions.Scope binary,
        EvaluationExtensions.BinaryObserver observer,
        @Nullable EvaluationExtensions.BinaryObserver external) {
      this.restore = restore;
      this.binary = binary;
      this.observer = observer;
      this.external = external;
    }
  }

  private record TaggedText(String text, BitSet spans) {
    void addMatches(BitSet matches, int offset) {
      for (int start = spans.nextSetBit(0); start >= 0; ) {
        int end = spans.nextClearBit(start);
        matches.set(offset + start, offset + end);
        start = spans.nextSetBit(end);
      }
    }
  }

  /** Host-configurable limits. An exhausted budget applies to the whole run. */
  @AutoValue
  public abstract static class Limits {
    Limits() {}

    public abstract int minLength();
    public abstract int maxSourceLength();
    public abstract int maxForms();
    public abstract int maxFormChars();
    public abstract int maxRegistrationInput();
    public abstract int maxTextLength();
    public abstract long maxScanWork();

    public static Builder builder() {
      return new AutoValue_DiagnosticRedactor_Limits.Builder()
          .minLength(3)
          .maxSourceLength(16 * 1024)
          .maxForms(1_024)
          .maxFormChars(256 * 1024)
          .maxRegistrationInput(256 * 1024)
          .maxTextLength(1024 * 1024)
          .maxScanWork(32_000_000);
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder minLength(int value);
      public abstract Builder maxSourceLength(int value);
      public abstract Builder maxForms(int value);
      public abstract Builder maxFormChars(int value);
      public abstract Builder maxRegistrationInput(int value);
      public abstract Builder maxTextLength(int value);
      public abstract Builder maxScanWork(long value);

      abstract Limits autoBuild();

      public final Limits build() {
        Limits limits = autoBuild();
        requirePositive("minLength", limits.minLength());
        requirePositive("maxSourceLength", limits.maxSourceLength());
        requirePositive("maxForms", limits.maxForms());
        requirePositive("maxFormChars", limits.maxFormChars());
        requirePositive("maxRegistrationInput", limits.maxRegistrationInput());
        requirePositive("maxTextLength", limits.maxTextLength());
        requirePositive("maxScanWork", limits.maxScanWork());
        return limits;
      }

      private static void requirePositive(String name, long value) {
        if (value <= 0) {
          throw new IllegalArgumentException(name + " must be positive");
        }
      }
    }
  }

  private final Mode mode;
  private final Observer observer;
  private final Limits limits;
  private final Set<String> forms = new LinkedHashSet<>();
  private final Set<String> hexForms = new LinkedHashSet<>();
  private int formChars;
  private int remainingRegistration;
  private long remainingWork;
  private boolean limited;
  @Nullable private BinaryTaint taint;
  @Nullable private WeakHashMap<EvalException, BitSet> renderedFailures;
  @Nullable private TaggedText escapedFailure;

  public DiagnosticRedactor(Mode mode) {
    this(mode, Limits.builder().build());
  }

  public DiagnosticRedactor(Mode mode, Limits limits) {
    this(mode, limits, (sink, reason) -> {});
  }

  public DiagnosticRedactor(Mode mode, Observer observer) {
    this(mode, Limits.builder().build(), observer);
  }

  public DiagnosticRedactor(Mode mode, Limits limits, Observer observer) {
    this.mode = Objects.requireNonNull(mode);
    this.limits = Objects.requireNonNull(limits);
    this.observer = Objects.requireNonNull(observer);
    this.remainingRegistration = limits.maxRegistrationInput();
    this.remainingWork = limits.maxScanWork();
  }

  /** Reads a host binding. A missing binding disables tracking. */
  @Nullable
  public static DiagnosticRedactor given(@Nullable Object binding) {
    if (binding == null) {
      return null;
    }
    if (binding instanceof DiagnosticRedactor redactor) {
      return redactor;
    }
    throw new IllegalArgumentException(BINDING_NAME + " requires a DiagnosticRedactor");
  }

  public boolean isTracking() {
    return mode != Mode.OFF;
  }

  /** Gives execution callbacks access to the active redactor until the scope closes. */
  public static Scope openScope(@Nullable DiagnosticRedactor redactor) {
    return new Scope(redactor != null && redactor.isTracking() ? redactor : null);
  }

  /**
   * Opens an execution scope and restores any enclosing scope when it closes. The binary callback
   * is installed after a hit or when a nested scope needs to replace the enclosing callback.
   */
  public static Scope openScope(StarlarkThread thread, @Nullable DiagnosticRedactor redactor) {
    Scope scope = openScope(redactor);
    if (scope.redactor != null) {
      scope.redactor.clearFailure();
    }
    if (thread.getThreadLocal(Scope.class) != null
        || (scope.redactor != null && scope.redactor.taint != null
            && scope.redactor.taint.hasTags())) {
      scope.attach(thread);
    }
    return scope;
  }

  /** Registers the scalar or flat batch result of an authorized reveal. */
  public void registerRevealed(Object value) {
    if (!isTracking() || limited) {
      return;
    }
    if (!(value instanceof StarlarkBytes) && value instanceof List<?> batch) {
      if (batch.size() > limits.maxForms()) {
        limited = true;
        return;
      }
      for (Object item : batch) {
        registerScalar(item);
        if (limited) {
          return;
        }
      }
    } else {
      registerScalar(value);
    }
  }

  private void noteFailure(EvalException failure, BitSet spans) {
    int length = failure.getMessage().length();
    if (length >= remainingRegistration) {
      limited = true;
      return;
    }
    remainingRegistration -= length + 1;
    if (renderedFailures == null) {
      renderedFailures = new WeakHashMap<>();
    }
    renderedFailures.put(failure, spans);
  }

  /** Clears the previous escaped diagnostic at the start of an evaluation or call. */
  public void clearFailure() {
    escapedFailure = null;
  }

  /** Associates provenance with the failure escaping the execution. */
  public void captureFailure(Throwable failure) {
    escapedFailure = null;
    if (renderedFailures == null) {
      return;
    }
    // Wrappers can carry the original EvalException. Check a bounded cause chain.
    for (int depth = 0; failure != null && depth < 16; depth++, failure = failure.getCause()) {
      if (failure instanceof EvalException eval) {
        BitSet spans = renderedFailures.get(eval);
        if (spans != null) {
          escapedFailure = new TaggedText(eval.getMessage(), spans);
          return;
        }
      }
    }
  }

  /** Hosts pass the final error diagnostic through this method before emitting it. */
  @Nullable
  public String sanitizeError(@Nullable String message) {
    return sanitize(Sink.ERROR_TEXT, message, escapedFailure);
  }

  public String sanitizePrint(String message) {
    return sanitize(Sink.PRINT, message, null);
  }

  private void registerScalar(Object value) {
    if (value instanceof String text) {
      if (chargeRegistration(text.length()) && text.length() >= limits.minLength()) {
        addText(text);
        addBytes(text.getBytes(StandardCharsets.UTF_8));
      }
    } else if (value instanceof StarlarkBytes bytes) {
      if (chargeRegistration(bytes.size()) && bytes.size() >= limits.minLength()) {
        registerBinary(bytes.toByteArray());
      }
    } else if (value instanceof byte[] raw) {
      if (chargeRegistration(raw.length) && raw.length >= limits.minLength()) {
        registerBinary(raw);
      }
    } else {
      chargeRegistration(0);
    }
  }

  private boolean chargeRegistration(int length) {
    if (length > limits.maxSourceLength() || length >= remainingRegistration) {
      limited = true;
      return false;
    }
    remainingRegistration -= length + 1;
    return true;
  }

  private void registerBinary(byte[] bytes) {
    String text = new String(bytes, StandardCharsets.UTF_8);
    if (Arrays.equals(bytes, text.getBytes(StandardCharsets.UTF_8))) {
      addText(text);
    }
    String quoted = StarlarkBytes.quoteBytes(bytes);
    addForm(forms, quoted.substring(2, quoted.length() - 1));
    addBytes(bytes);
  }

  private void addText(String value) {
    addForm(forms, value);
    String quoted = Starlark.repr(value, StarlarkSemantics.DEFAULT);
    addForm(forms, quoted.substring(1, quoted.length() - 1));
    addForm(forms, URLEncoder.encode(value, StandardCharsets.UTF_8));
  }

  private void addBytes(byte[] value) {
    addForm(forms, percentEncode(value, true, false));
    addForm(forms, percentEncode(value, false, false));
    addForm(forms, percentEncode(value, false, true));
    addForm(forms, Base64.getEncoder().encodeToString(value));
    addForm(forms, Base64.getEncoder().withoutPadding().encodeToString(value));
    addForm(forms, Base64.getUrlEncoder().encodeToString(value));
    addForm(forms, Base64.getUrlEncoder().withoutPadding().encodeToString(value));
    addForm(hexForms, HexFormat.of().formatHex(value));
  }

  private static String percentEncode(byte[] value, boolean allowSlash, boolean plusForSpace) {
    HexFormat hex = HexFormat.of().withUpperCase();
    StringBuilder output = new StringBuilder(value.length);
    for (byte raw : value) {
      int c = Byte.toUnsignedInt(raw);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
          || c == '-' || c == '.' || c == '_' || c == '~' || (allowSlash && c == '/')) {
        output.append((char) c);
      } else if (plusForSpace && c == ' ') {
        output.append('+');
      } else {
        hex.toHexDigits(output.append('%'), raw);
      }
    }
    return output.toString();
  }

  private void addForm(Set<String> target, String form) {
    if (limited || form.isEmpty() || target.contains(form)) {
      return;
    }
    if (forms.size() + (long) hexForms.size() >= limits.maxForms()
        || form.length() > limits.maxFormChars() - formChars) {
      limited = true;
      return;
    }
    target.add(form);
    formChars += form.length();
  }

  private boolean isLimited() {
    return limited || (taint != null && taint.isLimited());
  }

  @Nullable
  private String sanitize(Sink sink, @Nullable String message, @Nullable TaggedText tagged) {
    if (!isTracking() || message == null || message.isEmpty()) {
      return message;
    }
    if (!isLimited() && formChars == 0 && tagged == null) {
      return message;
    }
    long characters = formChars + 1L;
    if (tagged != null) {
      // A print is a suffix check. Error wrappers may contain the message anywhere.
      characters += sink == Sink.PRINT ? 1 : tagged.text().length();
    }
    if (isLimited() || message.length() > limits.maxTextLength()
        || characters > remainingWork / message.length()) {
      limited = true;
      report(sink, Reason.LIMIT);
      return mode == Mode.ENFORCE ? MASK : message;
    }
    remainingWork -= characters * message.length();

    BitSet matches = new BitSet();
    if (tagged != null && !collectTaggedMatches(sink, message, tagged, matches)) {
      report(sink, Reason.TAGGED);
      return mode == Mode.ENFORCE ? MASK : message;
    }
    collectMatches(message, forms, matches);
    if (!hexForms.isEmpty()) {
      collectMatches(foldHex(message), hexForms, matches);
    }
    if (matches.isEmpty()) {
      return message;
    }
    report(sink, tagged == null ? Reason.VALUE_MATCH : Reason.TAGGED);
    return mode == Mode.ENFORCE ? applyMask(message, matches) : message;
  }

  private static boolean collectTaggedMatches(
      Sink sink, String message, TaggedText tagged, BitSet matches) {
    if (sink == Sink.PRINT) {
      // Reporter may prepend the caller location before passing the line to the console.
      if (message.endsWith(tagged.text())) {
        tagged.addMatches(matches, message.length() - tagged.text().length());
        return true;
      }
      return false;
    }
    boolean found = false;
    for (int offset = message.indexOf(tagged.text());
        offset >= 0;
        offset = message.indexOf(tagged.text(), offset + 1)) {
      tagged.addMatches(matches, offset);
      found = true;
    }
    return found;
  }

  private static void collectMatches(String text, Set<String> forms, BitSet matches) {
    for (String form : forms) {
      for (int offset = text.indexOf(form); offset >= 0; offset = text.indexOf(form, offset + 1)) {
        matches.set(offset, offset + form.length());
      }
    }
  }

  private static String applyMask(String text, BitSet matches) {
    StringBuilder result = new StringBuilder(text.length());
    int copied = 0;
    for (int start = matches.nextSetBit(0); start >= 0; start = matches.nextSetBit(copied)) {
      result.append(text, copied, start).append(MASK);
      copied = matches.nextClearBit(start);
    }
    return result.append(text, copied, text.length()).toString();
  }

  private static String foldHex(String text) {
    // ASCII folding preserves offsets even for diagnostics containing non-ASCII text.
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
    return folded == null ? text : new String(folded);
  }

  private void report(Sink sink, Reason reason) {
    try {
      observer.onHit(sink, reason);
    } catch (RuntimeException ignored) {
      // An observer failure cannot change the diagnostic or fail the script.
    }
  }
}
