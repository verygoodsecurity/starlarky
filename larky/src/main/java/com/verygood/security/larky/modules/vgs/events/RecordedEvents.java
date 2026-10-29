package com.verygood.security.larky.modules.vgs.events;

import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

/**
 * Per-execution event buffer for guarded-route script telemetry.
 *
 * <p>Event names match {@code ^[a-z][a-z_]{0,63}$} and field keys match {@code
 * ^[a-z][a-z_]{0,31}$} (lowercase letters and underscores only; no digits). Each field value may
 * contain at most eight ASCII digits; the field values of one accepted event may contain at most
 * eleven digits together; values are at most 64 printable ASCII characters. No single script
 * execution's recorded events contain more than eleven digits in total, so they cannot carry a
 * card number in plain digits. This does not prevent a script from leaking data across many
 * executions or by encoding it in letters; only trusted (VGS template) scripts should run on
 * guarded routes.
 *
 * <p>One {@code RecordedEvents} instance is intended for a single script execution on one thread;
 * it is not safe for concurrent use.
 */
public final class RecordedEvents implements StarlarkValue {

  public static final String BINDING_NAME = "__vgs_recorded_events__";
  private static final int MAX_ACCEPTED_EVENTS = 10;
  private static final int MAX_FIELD_ENTRIES = 8;
  private static final int MAX_FIELD_VALUE_LENGTH = 64;
  private static final int MAX_DIGITS_PER_VALUE = 8;
  private static final int MAX_DIGITS_PER_EVENT = 11;
  private static final int MAX_DIGITS_PER_EXECUTION = 11;

  private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z_]{0,63}$");
  private static final Pattern FIELD_KEY_PATTERN = Pattern.compile("^[a-z][a-z_]{0,31}$");

  private final List<Event> accepted = new ArrayList<>();
  private int dropped;
  private int acceptedDigitCount;

  public record Event(String name, Map<String, String> fields) {
    public Event {
      fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }
  }

  public List<Event> events() {
    return Collections.unmodifiableList(accepted);
  }

  public int dropped() {
    return dropped;
  }

  /**
   * Validates and stores one event, or increments {@link #dropped()} without throwing.
   */
  public void tryRecord(String name, Map<String, String> fields) {
    if (!isValidName(name)) {
      dropped++;
      return;
    }
    Map<String, String> normalized = normalizeFields(fields);
    if (normalized == null) {
      dropped++;
      return;
    }
    int eventDigits = countDigitsInEvent(normalized);
    if (eventDigits > MAX_DIGITS_PER_EVENT) {
      dropped++;
      return;
    }
    if (acceptedDigitCount + eventDigits > MAX_DIGITS_PER_EXECUTION) {
      dropped++;
      return;
    }
    if (accepted.size() >= MAX_ACCEPTED_EVENTS) {
      dropped++;
      return;
    }
    accepted.add(new Event(name, normalized));
    acceptedDigitCount += eventDigits;
  }

  /** Entry point from {@link EventsModule#record}. Never throws for bad input. */
  public void recordStarlark(Object nameArg, Object fieldsArg) throws EvalException {
    String name = coerceName(nameArg);
    Map<String, String> fields;
    if (fieldsArg == null || fieldsArg instanceof NoneType) {
      fields = Map.of();
    } else if (!(fieldsArg instanceof Dict<?, ?>)) {
      dropped++;
      return;
    } else {
      fields = fieldsFromStarlark(fieldsArg);
      if (fields == null) {
        dropped++;
        return;
      }
    }
    tryRecord(name != null ? name : "", fields);
  }

  @StarlarkMethod(name = "events", documented = false, useStarlarkThread = true)
  public StarlarkList<?> starlarkEvents(StarlarkThread thread) throws EvalException {
    Mutability mu = thread.mutability();
    List<Object> out = new ArrayList<>(accepted.size());
    for (Event event : accepted) {
      Dict.Builder<String, Object> fieldsBuilder = Dict.builder();
      for (Map.Entry<String, String> entry : event.fields().entrySet()) {
        fieldsBuilder.put(entry.getKey(), entry.getValue());
      }
      Dict<String, Object> fieldsDict = fieldsBuilder.buildImmutable();
      Dict.Builder<String, Object> eventBuilder = Dict.builder();
      eventBuilder.put("name", event.name());
      eventBuilder.put("fields", fieldsDict);
      out.add(eventBuilder.buildImmutable());
    }
    return StarlarkList.copyOf(mu, out);
  }

  @StarlarkMethod(name = "dropped", documented = false)
  public StarlarkInt starlarkDropped() {
    return StarlarkInt.of(dropped);
  }

  private static boolean isValidName(String name) {
    return name != null && NAME_PATTERN.matcher(name).matches();
  }

  private static Map<String, String> normalizeFields(Map<String, String> fields) {
    if (fields == null || fields.isEmpty()) {
      return Map.of();
    }
    if (fields.size() > MAX_FIELD_ENTRIES) {
      return null;
    }
    LinkedHashMap<String, String> out = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : fields.entrySet()) {
      String key = entry.getKey();
      if (key == null || !FIELD_KEY_PATTERN.matcher(key).matches()) {
        return null;
      }
      String value = entry.getValue();
      if (value == null || !isValidFieldValue(value)) {
        return null;
      }
      out.put(key, value);
    }
    return out;
  }

  static Map<String, String> fieldsFromStarlark(Object fieldsArg) throws EvalException {
    if (fieldsArg == null || fieldsArg instanceof NoneType) {
      return Map.of();
    }
    if (!(fieldsArg instanceof Dict<?, ?> dict)) {
      return null;
    }
    LinkedHashMap<String, String> out = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : dict.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        return null;
      }
      String value = coerceFieldValue(entry.getValue());
      if (value == null) {
        return null;
      }
      out.put(key, value);
    }
    return out;
  }

  static String coerceFieldValue(Object value) {
    if (value == null || value instanceof NoneType) {
      return null;
    }
    if (value instanceof String s) {
      return s;
    }
    if (value instanceof StarlarkInt) {
      return value.toString();
    }
    if (value instanceof Boolean b) {
      return b ? "True" : "False";
    }
    return null;
  }

  static String coerceName(Object nameArg) {
    if (nameArg == null || nameArg instanceof NoneType) {
      return null;
    }
    if (nameArg instanceof String s) {
      return s;
    }
    return null;
  }

  private static boolean isValidFieldValue(String value) {
    if (value.length() > MAX_FIELD_VALUE_LENGTH) {
      return false;
    }
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c < 0x20 || c > 0x7E) {
        return false;
      }
    }
    return countAsciiDigits(value) <= MAX_DIGITS_PER_VALUE;
  }

  private static int countDigitsInEvent(Map<String, String> fields) {
    int total = 0;
    for (String value : fields.values()) {
      total += countAsciiDigits(value);
    }
    return total;
  }

  static int countAsciiDigits(String value) {
    int count = 0;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c >= '0' && c <= '9') {
        count++;
      }
    }
    return count;
  }
}
