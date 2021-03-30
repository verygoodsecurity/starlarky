// Copyright 2015 The Bazel Authors. All Rights Reserved.
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.util.IllegalFormatException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *  Test properties of the evaluator's datatypes and utility functions
 *  without actually creating any parse trees.
 */
@RunWith(JUnit4.class)
public class PrinterTest {

  @Test
  public void testPrinter() throws Exception {
    // Note that str and repr only differ on behaviour of strings at toplevel.
    assertThat(Starlark.str(createObjWithStr())).isEqualTo("<str marker>");
    assertThat(Starlark.repr(createObjWithStr())).isEqualTo("<repr marker>");

    assertThat(Starlark.str("foo\nbar")).isEqualTo("foo\nbar");
    assertThat(Starlark.repr("foo\nbar")).isEqualTo("\"foo\\nbar\"");
    assertThat(Starlark.str("'")).isEqualTo("'");
    assertThat(Starlark.repr("'")).isEqualTo("\"'\"");
    assertThat(Starlark.str("\"")).isEqualTo("\"");
    assertThat(Starlark.repr("\"")).isEqualTo("\"\\\"\"");
    assertThat(Starlark.str(StarlarkInt.of(3))).isEqualTo("3");
    assertThat(Starlark.repr(StarlarkInt.of(3))).isEqualTo("3");
    assertThat(Starlark.repr(Starlark.NONE)).isEqualTo("None");

    List<?> list = StarlarkList.of(null, "foo", "bar");
    List<?> tuple = Tuple.of("foo", "bar");

    assertThat(Starlark.str(Tuple.of(StarlarkInt.of(1), list, StarlarkInt.of(3))))
        .isEqualTo("(1, [\"foo\", \"bar\"], 3)");
    assertThat(Starlark.repr(Tuple.of(StarlarkInt.of(1), list, StarlarkInt.of(3))))
        .isEqualTo("(1, [\"foo\", \"bar\"], 3)");
    assertThat(Starlark.str(StarlarkList.of(null, StarlarkInt.of(1), tuple, StarlarkInt.of(3))))
        .isEqualTo("[1, (\"foo\", \"bar\"), 3]");
    assertThat(Starlark.repr(StarlarkList.of(null, StarlarkInt.of(1), tuple, StarlarkInt.of(3))))
        .isEqualTo("[1, (\"foo\", \"bar\"), 3]");

    Map<Object, Object> dict =
        ImmutableMap.<Object, Object>of(
            StarlarkInt.of(1), tuple, StarlarkInt.of(2), list, "foo", StarlarkList.of(null));
    assertThat(Starlark.str(dict))
        .isEqualTo("{1: (\"foo\", \"bar\"), 2: [\"foo\", \"bar\"], \"foo\": []}");
    assertThat(Starlark.repr(dict))
        .isEqualTo("{1: (\"foo\", \"bar\"), 2: [\"foo\", \"bar\"], \"foo\": []}");
  }

  private void checkFormatPositionalFails(String errorMessage, String format, Object... arguments) {
    IllegalFormatException e =
        assertThrows(IllegalFormatException.class, () -> Starlark.format(format, arguments));
    assertThat(e).hasMessageThat().isEqualTo(errorMessage);
  }

  @Test
  public void testOutputOrderOfMap() throws Exception {
    Map<Object, Object> map = new LinkedHashMap<>();
    map.put(StarlarkInt.of(5), StarlarkInt.of(5));
    map.put(StarlarkInt.of(3), StarlarkInt.of(3));
    map.put("foo", StarlarkInt.of(42));
    map.put(StarlarkInt.of(7), "bar");
    assertThat(Starlark.str(Starlark.fromJava(map, null)))
        .isEqualTo("{5: 5, 3: 3, \"foo\": 42, 7: \"bar\"}");
  }

  @Test
  public void testFormatPositional() throws Exception {
    assertThat(Starlark.formatWithList("%s %d", Tuple.of("foo", StarlarkInt.of(3))))
        .isEqualTo("foo 3");
    assertThat(Starlark.format("%s %d", "foo", StarlarkInt.of(3))).isEqualTo("foo 3");

    // %d allows Integer or StarlarkInt
    assertThat(Starlark.format("%d %d", StarlarkInt.of(123), 456)).isEqualTo("123 456");

    assertThat(Starlark.format("%s %s %s", StarlarkInt.of(1), null, StarlarkInt.of(3)))
        .isEqualTo("1 null 3");

    // Note: formatToString doesn't perform scalar x -> (x) conversion;
    // The %-operator is responsible for that.
    assertThat(Starlark.formatWithList("", Tuple.of())).isEmpty();
    assertThat(Starlark.format("%s", "foo")).isEqualTo("foo");
    assertThat(Starlark.format("%s", 3.14159)).isEqualTo("3.14159");
    checkFormatPositionalFails("not all arguments converted during string formatting",
        "%s", 1, 2, 3);
    assertThat(Starlark.format("%%%s", "foo")).isEqualTo("%foo");
    checkFormatPositionalFails("not all arguments converted during string formatting",
        "%%s", "foo");
    checkFormatPositionalFails("unsupported format character \" \" at index 1 in \"% %s\"",
        "% %s", "foo");
    assertThat(
            Starlark.format(
                "%s",
                StarlarkList.of(null, StarlarkInt.of(1), StarlarkInt.of(2), StarlarkInt.of(3))))
        .isEqualTo("[1, 2, 3]");
    assertThat(
            Starlark.format(
                "%s", Tuple.of(StarlarkInt.of(1), StarlarkInt.of(2), StarlarkInt.of(3))))
        .isEqualTo("(1, 2, 3)");
    assertThat(Starlark.format("%s", StarlarkList.of(null))).isEqualTo("[]");
    assertThat(Starlark.format("%s", Tuple.of())).isEqualTo("()");
    assertThat(Starlark.format("%% %d %r %s", StarlarkInt.of(1), "2", "3"))
        .isEqualTo("% 1 \"2\" 3");

    checkFormatPositionalFails("got string for '%d' format, want int or float", "%d", "1");
    checkFormatPositionalFails(
        "unsupported format character \".\" at index 1 in \"%.3g\"", "%.3g", 1);
    checkFormatPositionalFails("unsupported format character \".\" at index 1 in \"%.3g\"",
        "%.3g", 1, 2);
    checkFormatPositionalFails(
        "unsupported format character \".\" at index 1 in \"%.s\"", "%.s", 1);
    checkFormatPositionalFails("not enough arguments for format pattern \"%.s\": ()", "%.s");
  }

  private StarlarkValue createObjWithStr() {
    return new StarlarkValue() {
      @Override
      public void repr(Printer printer) {
        printer.append("<repr marker>");
      }

      @Override
      public void str(Printer printer) {
        printer.append("<str marker>");
      }
    };
  }
}
