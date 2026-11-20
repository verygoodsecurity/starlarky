package net.starlark.java.eval;

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
import javax.annotation.Nullable;

/** Known-value masking for one logical Larky run. The host retains this through error handling. */
public final class DiagnosticRedactor {
  public static final String BINDING_NAME = "__vgs_diagnostic_redactor__";
  public static final String MASK = "***REDACTED***";

  private static final int MIN_LENGTH = 3;
  private static final int MAX_SOURCE_LENGTH = 16 * 1024;
  private static final int MAX_FORMS = 1_024;
  private static final int MAX_FORM_CHARS = 256 * 1024;
  private static final int MAX_REGISTRATION_INPUT = 256 * 1024;
  private static final int MAX_TEXT_LENGTH = 1024 * 1024;
  private static final long MAX_SCAN_WORK = 32_000_000;

  public enum Mode { OFF, OBSERVE, ENFORCE }
  public enum Sink { PRINT, ERROR_TEXT }
  public enum Reason { VALUE_MATCH, LIMIT }

  @FunctionalInterface
  public interface Observer {
    void onHit(Sink sink, Reason reason);
  }

  @FunctionalInterface
  public interface Scope extends AutoCloseable {
    @Override
    void close();
  }

  private final Mode mode;
  private final Observer observer;
  private final Set<String> forms = new LinkedHashSet<>();
  private final Set<String> hexForms = new LinkedHashSet<>();
  private int formChars;
  private int remainingRegistration = MAX_REGISTRATION_INPUT;
  private long remainingWork = MAX_SCAN_WORK;
  private boolean limited;

  public DiagnosticRedactor(Mode mode) {
    this(mode, (sink, reason) -> {});
  }

  public DiagnosticRedactor(Mode mode, Observer observer) {
    this.mode = Objects.requireNonNull(mode);
    this.observer = Objects.requireNonNull(observer);
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

  /** Binds the redactor for a scope and restores the previous binding when the scope closes. */
  public static Scope bind(StarlarkThread thread, @Nullable DiagnosticRedactor redactor) {
    DiagnosticRedactor previous = thread.getThreadLocal(DiagnosticRedactor.class);
    DiagnosticRedactor active = redactor != null && redactor.isTracking() ? redactor : null;
    if (previous == active) {
      return () -> {};
    }
    thread.setThreadLocal(DiagnosticRedactor.class, active);
    return () -> thread.setThreadLocal(DiagnosticRedactor.class, previous);
  }

  /** Registers the scalar or flat batch result of an authorized reveal. */
  public void registerRevealed(Object value) {
    if (!isTracking() || limited) {
      return;
    }
    if (!(value instanceof StarlarkBytes) && value instanceof List<?> batch) {
      if (batch.size() > MAX_FORMS) {
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

  /** Hosts pass the final error diagnostic through this method before emitting it. */
  @Nullable
  public String sanitizeError(@Nullable String message) {
    return sanitize(Sink.ERROR_TEXT, message);
  }

  public String sanitizePrint(String message) {
    return sanitize(Sink.PRINT, message);
  }

  private void registerScalar(Object value) {
    if (value instanceof String text) {
      if (chargeRegistration(text.length()) && text.length() >= MIN_LENGTH) {
        addText(text);
        addBytes(text.getBytes(StandardCharsets.UTF_8));
      }
    } else if (value instanceof StarlarkBytes bytes) {
      if (chargeRegistration(bytes.size()) && bytes.size() >= MIN_LENGTH) {
        registerBinary(bytes.toByteArray());
      }
    } else if (value instanceof byte[] raw) {
      if (chargeRegistration(raw.length) && raw.length >= MIN_LENGTH) {
        registerBinary(raw);
      }
    } else {
      chargeRegistration(0);
    }
  }

  private boolean chargeRegistration(int length) {
    if (length > MAX_SOURCE_LENGTH || length >= remainingRegistration) {
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
    if (forms.size() + hexForms.size() >= MAX_FORMS
        || form.length() > MAX_FORM_CHARS - formChars) {
      limited = true;
      return;
    }
    target.add(form);
    formChars += form.length();
  }

  @Nullable
  private String sanitize(Sink sink, @Nullable String message) {
    if (!isTracking() || message == null || message.isEmpty()) {
      return message;
    }
    if (!limited && formChars == 0) {
      return message;
    }
    long work = (long) message.length() * (formChars + 1);
    if (limited || message.length() > MAX_TEXT_LENGTH || work > remainingWork) {
      limited = true;
      report(sink, Reason.LIMIT);
      return mode == Mode.ENFORCE ? MASK : message;
    }
    remainingWork -= work;

    BitSet matches = new BitSet();
    collectMatches(message, forms, matches);
    if (!hexForms.isEmpty()) {
      collectMatches(foldHex(message), hexForms, matches);
    }
    if (matches.isEmpty()) {
      return message;
    }
    report(sink, Reason.VALUE_MATCH);
    return mode == Mode.ENFORCE ? applyMask(message, matches) : message;
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
