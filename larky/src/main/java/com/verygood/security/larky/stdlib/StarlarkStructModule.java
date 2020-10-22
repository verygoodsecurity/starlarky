package com.verygood.security.larky.stdlib;

import com.google.common.collect.ImmutableCollection;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.ClassObject;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.syntax.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * An base class for Starlark values that have fields, have to_json and to_proto methods, have an
 * associated provider (type symbol).
 *
 * <p>StructImpl does not specify how the fields are represented; subclasses must define {@code
 * getValue} and {@code getFieldNames}. For example, {@code NativeInfo} supplies fields from the
 * subclass's {@code StarlarkMethod(structField=true)} annotations, and {@code StarlarkInfo}
 * supplies fields from the map provided at its construction.
 *
 * <p>Two StructImpls are equivalent if they have the same provider and, for each field name
 * reported by {@code getFieldNames} their corresponding field values are equivalent, or accessing
 * them both returns an error.
 */
@StarlarkBuiltin(
    name = "struct",
    category = "BUILTIN",
    doc =
        "A generic object with fields."
            + "<p>Structs fields cannot be reassigned once the struct is created. Two structs are "
            + "equal if they have the same fields and if corresponding field values are equal.")
public class StarlarkStructModule implements StarlarkValue, ClassObject {

  private final Provider provider;
  private final Location location;

  /**
   * Constructs an {@link StarlarkStructModule}.
   *
   * @param provider the provider describing the type of this instance
   * @param location the Starlark location where this instance is created. If null, defaults to
   *                 {@link Location#BUILTIN}.
   */
  public StarlarkStructModule(Provider provider, @Nullable Location location) {
    this.provider = provider;
    this.location = location != null ? location : Location.BUILTIN;
  }

  @StarlarkMethod(
      name = "to_proto",
      doc =
          "Creates a text message from the struct parameter. This method only works if all "
              + "struct elements (recursively) are strings, ints, booleans, "
              + "other structs or dicts or lists of these types. "
              + "Quotes and new lines in strings are escaped. "
              + "Struct keys are iterated in the sorted order. "
              + "Examples:<br><pre class=language-python>"
              + "struct(key=123).to_proto()\n# key: 123\n\n"
              + "struct(key=True).to_proto()\n# key: true\n\n"
              + "struct(key=[1, 2, 3]).to_proto()\n# key: 1\n# key: 2\n# key: 3\n\n"
              + "struct(key='text').to_proto()\n# key: \"text\"\n\n"
              + "struct(key=struct(inner_key='text')).to_proto()\n"
              + "# key {\n#   inner_key: \"text\"\n# }\n\n"
              + "struct(key=[struct(inner_key=1), struct(inner_key=2)]).to_proto()\n"
              + "# key {\n#   inner_key: 1\n# }\n# key {\n#   inner_key: 2\n# }\n\n"
              + "struct(key=struct(inner_key=struct(inner_inner_key='text'))).to_proto()\n"
              + "# key {\n#    inner_key {\n#     inner_inner_key: \"text\"\n#   }\n# }\n\n"
              + "struct(foo={4: 3, 2: 1}).to_proto()\n"
              + "# foo: {\n"
              + "#   key: 4\n"
              + "#   value: 3\n"
              + "# }\n"
              + "# foo: {\n"
              + "#   key: 2\n"
              + "#   value: 1\n"
              + "# }\n"
              + "</pre>")
  String toProto() throws EvalException {
    StringBuilder sb = new StringBuilder();
    printProtoTextMessage(this, sb, 0);
    return sb.toString();
  }

  private static void printProtoTextMessage(ClassObject object, StringBuilder sb, int indent)
      throws EvalException {
    // For determinism sort the fields alphabetically.
    List<String> fields = new ArrayList<>(object.getFieldNames());
    Collections.sort(fields);
    for (String field : fields) {
      printProtoTextMessage(field, object.getValue(field), sb, indent);
    }
  }

  private static void printProtoTextMessage(
      String key, Object value, StringBuilder sb, int indent, String container)
      throws EvalException {
    if (value instanceof Map.Entry) {
      Map.Entry<?, ?> entry = (Map.Entry<?, ?>) value;
      print(sb, key + " {", indent);
      printProtoTextMessage("key", entry.getKey(), sb, indent + 1);
      printProtoTextMessage("value", entry.getValue(), sb, indent + 1);
      print(sb, "}", indent);
    } else if (value instanceof ClassObject) {
      print(sb, key + " {", indent);
      printProtoTextMessage((ClassObject) value, sb, indent + 1);
      print(sb, "}", indent);
    } else if (value instanceof String) {
      print(
          sb,
          key + ": \"" + escapeDoubleQuotesAndBackslashesAndNewlines((String) value) + "\"",
          indent);
    } else if (value instanceof StarlarkInt) {
      print(sb, key + ": " + value, indent);
    } else if (value instanceof Boolean) {
      // We're relying on the fact that Java converts Booleans to Strings in the same way
      // as the protocol buffers do.
      print(sb, key + ": " + value, indent);
    } else {
      throw Starlark.errorf(
          "Invalid text format, expected a struct, a dict, a string, a bool, or an int but got a"
              + " %s for %s '%s'",
          Starlark.type(value), container, key);
    }
  }

  private static void printProtoTextMessage(String key, Object value, StringBuilder sb, int indent)
      throws EvalException {
    if (value instanceof Sequence) {
      for (Object item : (Sequence<?>) value) {
        // TODO(bazel-team): There should be some constraint on the fields of the structs
        // in the same list but we ignore that for now.
        printProtoTextMessage(key, item, sb, indent, "list element in struct field");
      }
    } else if (value instanceof Dict) {
      for (Map.Entry<?, ?> entry : ((Dict<?, ?>) value).entrySet()) {
        printProtoTextMessage(key, entry, sb, indent, "entry of dictionary");
      }
    } else {
      printProtoTextMessage(key, value, sb, indent, "struct field");
    }
  }

  private static void print(StringBuilder sb, String text, int indent) {
    for (int i = 0; i < indent; i++) {
      sb.append("  ");
    }
    sb.append(text);
    sb.append("\n");
  }

  @StarlarkMethod(
      name = "to_json",
      doc =
          "Creates a JSON string from the struct parameter. This method only works if all "
              + "struct elements (recursively) are strings, ints, booleans, other structs, a "
              + "list of these types or a dictionary with string keys and values of these types. "
              + "Quotes and new lines in strings are escaped. "
              + "Examples:<br><pre class=language-python>"
              + "struct(key=123).to_json()\n# {\"key\":123}\n\n"
              + "struct(key=True).to_json()\n# {\"key\":true}\n\n"
              + "struct(key=[1, 2, 3]).to_json()\n# {\"key\":[1,2,3]}\n\n"
              + "struct(key='text').to_json()\n# {\"key\":\"text\"}\n\n"
              + "struct(key=struct(inner_key='text')).to_json()\n"
              + "# {\"key\":{\"inner_key\":\"text\"}}\n\n"
              + "struct(key=[struct(inner_key=1), struct(inner_key=2)]).to_json()\n"
              + "# {\"key\":[{\"inner_key\":1},{\"inner_key\":2}]}\n\n"
              + "struct(key=struct(inner_key=struct(inner_inner_key='text'))).to_json()\n"
              + "# {\"key\":{\"inner_key\":{\"inner_inner_key\":\"text\"}}}\n</pre>")
  String toJson() throws EvalException {
    StringBuilder sb = new StringBuilder();
    printJson(this, sb, "struct field", null);
    return sb.toString();
  }

  private static void printJson(Object value, StringBuilder sb, String container, String key)
      throws EvalException {
    if (value == Starlark.NONE) {
      sb.append("null");
    } else if (value instanceof ClassObject) {
      sb.append("{");

      String join = "";
      for (String field : ((ClassObject) value).getFieldNames()) {
        sb.append(join);
        join = ",";
        appendJSONStringLiteral(sb, field);
        sb.append(':');
        printJson(((ClassObject) value).getValue(field), sb, "struct field", field);
      }
      sb.append("}");
    } else if (value instanceof Dict) {
      sb.append("{");
      String join = "";
      for (Map.Entry<?, ?> entry : ((Dict<?, ?>) value).entrySet()) {
        sb.append(join);
        join = ",";
        if (!(entry.getKey() instanceof String)) {
          throw Starlark.errorf(
              "Keys must be a string but got a %s for %s%s",
              Starlark.type(entry.getKey()), container, key != null ? " '" + key + "'" : "");
        }
        appendJSONStringLiteral(sb, (String) entry.getKey());
        sb.append(':');
        printJson(entry.getValue(), sb, "dict value", String.valueOf(entry.getKey()));
      }
      sb.append("}");
    } else if (value instanceof List) {
      sb.append("[");
      String join = "";
      for (Object item : (List<?>) value) {
        sb.append(join);
        join = ",";
        printJson(item, sb, "list element in struct field", key);
      }
      sb.append("]");
    } else if (value instanceof String) {
      appendJSONStringLiteral(sb, (String) value);
    } else if (value instanceof StarlarkInt || value instanceof Boolean) {
      sb.append(value);
    } else {
      throw Starlark.errorf(
          "Invalid text format, expected a struct, a string, a bool, or an int but got a %s for"
              + " %s%s",
          Starlark.type(value), container, key != null ? " '" + key + "'" : "");
    }
  }

  private static void appendJSONStringLiteral(StringBuilder out, String s) {
    out.append('"');
    out.append(
        escapeDoubleQuotesAndBackslashesAndNewlines(s)
            .replace("\r", "\\r")
            .replace("\t", "\\t"));
    out.append('"');
  }

  /**
   * Returns the result of {@link #getValue(String)}, cast as the given type, throwing {@link
   * EvalException} if the cast fails.
   */
  @Nullable
  public final <T> T getValue(String key, Class<T> type) throws EvalException {
    Object obj = getValue(key);
    if (obj == null) {
      return null;
    }
    try {
      return type.cast(obj);
    } catch (
        @SuppressWarnings("UnusedException")
            ClassCastException unused) {
      throw Starlark.errorf(
          "for %s field, got %s, want %s", key, Starlark.type(obj), Starlark.classType(type));
    }
  }

  @Nullable
  @Override
  public Object getValue(String name) throws EvalException {
    return null;
  }

  @Override
  public ImmutableCollection<String> getFieldNames() {
    return null;
  }

  @Nullable
  @Override
  public String getErrorMessageForUnknownField(String field) {
    return null;
  }

  /**
   * Escapes the given string for use in proto/JSON string.
   *
   * <p>This escapes double quotes, backslashes, and newlines.
   */
  private static String escapeDoubleQuotesAndBackslashesAndNewlines(String string) {
    return escapeDoubleQuotesAndBackslashes(string).replace("\n", "\\n");
  }

  /**
   * Escape double quotes and backslashes in a String for unicode output of a message.
   */
  private static String escapeDoubleQuotesAndBackslashes(final String input) {
    return input.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}

