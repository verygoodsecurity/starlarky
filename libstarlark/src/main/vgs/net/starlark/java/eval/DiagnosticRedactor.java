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
import javax.annotation.Nullable;

/** Known-value masking for one logical Larky run. The host retains this through error handling. */
public final class DiagnosticRedactor {
  public static final String BINDING_NAME = "__vgs_diagnostic_redactor__";
  public static final String MASK = "***REDACTED***";

  public enum Mode { OFF, OBSERVE, ENFORCE }
  public enum Sink { PRINT, ERROR_TEXT }
  public enum Reason { VALUE_MATCH, LIMIT }

  @FunctionalInterface
  public interface Observer {
    void onHit(Sink sink, Reason reason);
  }

  public static final class Scope implements AutoCloseable {
    @Nullable private DiagnosticRedactor redactor;

    private Scope(@Nullable DiagnosticRedactor redactor) {
      this.redactor = redactor;
    }

    public boolean isTracking() {
      return redactor != null;
    }

    public String sanitizePrint(String message) {
      return redactor == null ? message : redactor.sanitizePrint(message);
    }

    public void registerRevealed(List<Object> values) {
      if (redactor != null) {
        redactor.registerRevealed(values);
      }
    }

    @Override
    public void close() {
      redactor = null;
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
    String escaped = percentEncode(value);
    // The encoder escapes literal '%' as '%25', so these replacements match whole escapes.
    addForm(forms, escaped.replace("%2F", "/"));
    addForm(forms, escaped);
    addForm(forms, escaped.replace("%20", "+"));

    String base64 = Base64.getEncoder().encodeToString(value);
    addBase64Forms(base64);
    // The URL alphabet differs only for those two digits.
    if (base64.indexOf('+') >= 0 || base64.indexOf('/') >= 0) {
      addBase64Forms(Base64.getUrlEncoder().encodeToString(value));
    }
    addForm(hexForms, HexFormat.of().formatHex(value));
  }

  private void addBase64Forms(String padded) {
    addForm(forms, padded);
    int end = padded.length();
    while (end > 0 && padded.charAt(end - 1) == '=') {
      end--;
    }
    if (end != padded.length()) {
      addForm(forms, padded.substring(0, end));
    }
  }

  private static String percentEncode(byte[] value) {
    HexFormat hex = HexFormat.of().withUpperCase();
    StringBuilder output = new StringBuilder(value.length);
    for (byte raw : value) {
      int c = Byte.toUnsignedInt(raw);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
          || c == '-' || c == '.' || c == '_' || c == '~') {
        output.append((char) c);
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

  @Nullable
  private String sanitize(Sink sink, @Nullable String message) {
    if (!isTracking() || message == null || message.isEmpty()) {
      return message;
    }
    if (!limited && formChars == 0) {
      return message;
    }
    long work = message.length() * (formChars + 1L);
    if (limited || message.length() > limits.maxTextLength() || work > remainingWork) {
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
