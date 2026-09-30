// Copyright 2025 The Bazel Authors. All rights reserved.
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
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Edge case tests for the Starlark interpreter, focusing on boundary conditions, special values,
 * and uncommon execution paths that ensure the interpreter handles all scenarios correctly.
 */
@RunWith(JUnit4.class)
public final class InterpreterEdgeCasesTest {

  private final EvaluationTestCase ev = new EvaluationTestCase();

  // ---- Empty and Null Cases ----

  @Test
  public void testEmptyFunctionBody() throws Exception {
    ev.new Scenario()
        .setUp("def empty_func():", "  pass", "result = empty_func()")
        .testLookup("result", Starlark.NONE);
  }

  @Test
  public void testEmptyListOperations() throws Exception {
    ev.new Scenario()
        .setUp("empty = []")
        .testEval("len(empty)", "0")
        .testEval("list(empty)", "[]")
        .testEval("empty + []", "[]")
        .testEval("[] + empty", "[]")
        .testEval("empty * 5", "[]")
        .testEval("5 * empty", "[]");
  }

  @Test
  public void testEmptyDictOperations() throws Exception {
    ev.new Scenario()
        .setUp("empty = {}")
        .testEval("len(empty)", "0")
        .testEval("list(empty.keys())", "[]")
        .testEval("list(empty.values())", "[]")
        .testEval("list(empty.items())", "[]");
  }

  @Test
  public void testEmptyStringOperations() throws Exception {
    ev.new Scenario()
        .setUp("empty = ''")
        .testEval("len(empty)", "0")
        .testEval("empty + ''", "''")
        .testEval("'' + empty", "''")
        .testEval("empty * 10", "''")
        .testEval("'a' in empty", "False");
  }

  @Test
  public void testNoneComparisons() throws Exception {
    ev.new Scenario()
        .testExpression("None == None", true)
        .testExpression("None != None", false)
        .testExpression("None == 0", false)
        .testExpression("None == False", false)
        .testExpression("None == ''", false)
        .testExpression("None == []", false);
  }

  // ---- Boundary Value Tests ----

  @Test
  public void testLargeIntegers() throws Exception {
    ev.new Scenario()
        .testExpression("2147483647", StarlarkInt.of(2147483647))
        .testExpression("2147483647 + 1", StarlarkInt.of(2147483648L))
        .testExpression("-2147483648", StarlarkInt.of(-2147483648))
        .testExpression("-2147483648 - 1", StarlarkInt.of(-2147483649L));
  }

  @Test
  public void testVeryLargeNumbers() throws Exception {
    // Test that Starlark handles arbitrarily large integers
    ev.exec("huge = int('1' + '0' * 100)");
    Object result = ev.lookup("huge");
    assertThat(result).isInstanceOf(StarlarkInt.class);
  }

  @Test
  public void testZeroInVariousContexts() throws Exception {
    ev.new Scenario()
        .testExpression("0 + 0", StarlarkInt.of(0))
        .testExpression("0 * 100", StarlarkInt.of(0))
        .testExpression("not 0", true)
        .testExpression("0 or 42", StarlarkInt.of(42))
        .testExpression("42 or 0", StarlarkInt.of(42))
        .testExpression("0 and 42", StarlarkInt.of(0));
  }

  @Test
  public void testNegativeIndices() throws Exception {
    ev.new Scenario()
        .setUp("lst = [1, 2, 3, 4, 5]")
        .testEval("lst[-1]", "5")
        .testEval("lst[-2]", "4")
        .testEval("lst[-5]", "1")
        .testEval("lst[-3:-1]", "[3, 4]");
  }

  @Test
  public void testNegativeSliceSteps() throws Exception {
    ev.new Scenario()
        .setUp("lst = [1, 2, 3, 4, 5]")
        .testEval("lst[::-1]", "[5, 4, 3, 2, 1]")
        .testEval("lst[::-2]", "[5, 3, 1]")
        .testEval("lst[4:0:-1]", "[5, 4, 3, 2]");
  }

  // ---- Unicode and Special Characters ----

  @Test
  public void testUnicodeStrings() throws Exception {
    ev.new Scenario()
        .testExpression("'hello' == 'hello'", true)
        .testExpression("len('hello')", StarlarkInt.of(5))
        .testExpression("'café'", "café")
        .testExpression("'hello\\nworld'", "hello\nworld")
        .testExpression("'hello\\tworld'", "hello\tworld");
  }

  @Test
  public void testStringEscapeSequences() throws Exception {
    ev.new Scenario()
        .testExpression("'\\n'", "\n")
        .testExpression("'\\t'", "\t")
        .testExpression("'\\r'", "\r")
        .testExpression("'\\\\'", "\\")
        .testExpression("'\\''", "'")
        .testExpression("'\\\"'", "\"");
  }

  // ---- Operator Edge Cases ----

  @Test
  public void testPowerOperatorIsNotStarlark() throws Exception {
    // Starlark has no ** operator (unlike Python).
    ev.new Scenario()
        .testIfErrorContains("syntax error at '**'", "2 ** 10")
        .testIfErrorContains("syntax error at '**'", "x = 3", "y = x ** 2");
  }

  @Test
  public void testModuloWithNegatives() throws Exception {
    ev.new Scenario()
        .testExpression("7 % 3", StarlarkInt.of(1))
        .testExpression("-7 % 3", StarlarkInt.of(2))
        .testExpression("7 % -3", StarlarkInt.of(-2))
        .testExpression("-7 % -3", StarlarkInt.of(-1));
  }

  @Test
  public void testFloorDivisionEdgeCases() throws Exception {
    ev.new Scenario()
        .testExpression("7 // 3", StarlarkInt.of(2))
        .testExpression("-7 // 3", StarlarkInt.of(-3))
        .testExpression("7 // -3", StarlarkInt.of(-3))
        .testExpression("-7 // -3", StarlarkInt.of(2))
        .testExpression("0 // 5", StarlarkInt.of(0))
        .testExpression("5 // 10", StarlarkInt.of(0));
  }

  @Test
  public void testComparisonChaining() throws Exception {
    // Starlark comparison operators are non-associative: chains need parentheses or 'and'.
    ev.new Scenario()
        .testIfErrorContains("Operator '<' is not associative with operator '<'", "1 < 2 < 3")
        .testIfErrorContains("Operator '>' is not associative with operator '=='", "5 > 3 == 3")
        .testExpression("1 < 2 and 2 < 3", true)
        .testExpression("(1 < 2) == True", true)
        .testExpression("1 < 2 and 2 < 1", false)
        .testExpression("(5 > 3) == 3", false);
  }

  // ---- Collection Edge Cases ----

  @Test
  public void testSingleElementTuple() throws Exception {
    ev.new Scenario()
        .testExpression("(1,)", Tuple.of(StarlarkInt.of(1)))
        .testExpression("('a',)", Tuple.of("a"))
        .testExpression("(1)", StarlarkInt.of(1)) // Not a tuple
        .testExpression("((1,),)", Tuple.of(Tuple.of(StarlarkInt.of(1))));
  }

  @Test
  public void testListRepeatingWithZeroAndNegative() throws Exception {
    ev.new Scenario()
        .testEval("[1, 2] * 0", "[]")
        .testEval("[1, 2] * -1", "[]")
        .testEval("0 * [1, 2]", "[]")
        .testEval("-5 * [1, 2]", "[]");
  }

  @Test
  public void testStringRepeatingWithZeroAndNegative() throws Exception {
    ev.new Scenario()
        .testEval("'abc' * 0", "''")
        .testEval("'abc' * -1", "''")
        .testEval("0 * 'abc'", "''")
        .testEval("-5 * 'abc'", "''");
  }

  @Test
  public void testDictWithDuplicateKeys() throws Exception {
    ev.new Scenario()
        .testIfErrorContains("dictionary expression has duplicate key: \"a\"", "{'a': 1, 'a': 2}")
        .testIfErrorContains("dictionary expression has duplicate key: 1", "{1: 'a', 1: 'b'}")
        .testEval("dict([('a', 1), ('a', 2)])", "{'a': 2}"); // dict() keeps the last value
  }

  @Test
  public void testSliceWithOutOfBoundsIndices() throws Exception {
    ev.new Scenario()
        .setUp("lst = [1, 2, 3]")
        .testEval("lst[0:100]", "[1, 2, 3]")
        .testEval("lst[-100:100]", "[1, 2, 3]")
        .testEval("lst[10:20]", "[]")
        .testEval("lst[-100:-200]", "[]");
  }

  // ---- Function Edge Cases ----

  @Test
  public void testFunctionWithNoReturn() throws Exception {
    ev.new Scenario()
        .setUp(
            "def no_return(x):",
            "  y = x * 2",  // Does something but doesn't return
            "result = no_return(5)")
        .testLookup("result", Starlark.NONE);
  }

  @Test
  public void testFunctionReturningNone() throws Exception {
    ev.new Scenario()
        .setUp(
            "def returns_none():",
            "  return None",
            "result = returns_none()")
        .testLookup("result", Starlark.NONE);
  }

  @Test
  public void testFunctionWithOnlyDefaultParams() throws Exception {
    ev.new Scenario()
        .setUp(
            "def all_defaults(a=1, b=2, c=3):",
            "  return a + b + c",
            "r1 = all_defaults()",
            "r2 = all_defaults(10)",
            "r3 = all_defaults(10, 20)",
            "r4 = all_defaults(10, 20, 30)")
        .testLookup("r1", StarlarkInt.of(6))
        .testLookup("r2", StarlarkInt.of(15))
        .testLookup("r3", StarlarkInt.of(33))
        .testLookup("r4", StarlarkInt.of(60));
  }

  @Test
  public void testLambdaExpressions() throws Exception {
    ev.new Scenario()
        .testExpression("(lambda x: x + 1)(5)", StarlarkInt.of(6))
        .testExpression("(lambda x, y: x * y)(3, 4)", StarlarkInt.of(12))
        .testExpression("(lambda: 42)()", StarlarkInt.of(42));
  }

  @Test
  public void testLambdaWithDefaultParameters() throws Exception {
    ev.new Scenario()
        .testExpression("(lambda x=5: x * 2)()", StarlarkInt.of(10))
        .testExpression("(lambda x=5: x * 2)(3)", StarlarkInt.of(6));
  }

  @Test
  public void testNestedLambdas() throws Exception {
    ev.new Scenario()
        .testExpression("(lambda x: (lambda y: x + y)(2))(3)", StarlarkInt.of(5));
  }

  // ---- Boolean and Truthiness Edge Cases ----

  @Test
  public void testTruthinessOfValues() throws Exception {
    ev.new Scenario()
        .testExpression("bool(0)", false)
        .testExpression("bool(1)", true)
        .testExpression("bool(-1)", true)
        .testExpression("bool('')", false)
        .testExpression("bool('a')", true)
        .testExpression("bool([])", false)
        .testExpression("bool([1])", true)
        .testExpression("bool({})", false)
        .testExpression("bool({'a': 1})", true)
        .testExpression("bool(None)", false);
  }

  @Test
  public void testBooleanIdentity() throws Exception {
    ev.new Scenario()
        .testExpression("True == True", true)
        .testExpression("False == False", true)
        .testExpression("True == False", false)
        .testExpression("True != False", true)
        .testExpression("bool(1) == True", true)
        .testExpression("bool(0) == False", true);
  }

  // ---- Iteration Edge Cases ----

  @Test
  public void testIteratingOverEmptyCollections() throws Exception {
    ev.new Scenario()
        .setUp(
            "def iterate_empty():",
            "  count = 0",
            "  for x in []:",
            "    count += 1",
            "  return count",
            "result = iterate_empty()")
        .testLookup("result", StarlarkInt.of(0));
  }

  @Test
  public void testBreakInFirstIteration() throws Exception {
    ev.new Scenario()
        .setUp(
            "def break_immediately():",
            "  for i in range(10):",
            "    break",
            "  return i",
            "result = break_immediately()")
        .testLookup("result", StarlarkInt.of(0));
  }

  @Test
  public void testContinueInAllIterations() throws Exception {
    ev.new Scenario()
        .setUp(
            "def continue_all():",
            "  count = 0",
            "  for i in range(5):",
            "    count += 1",
            "    continue",
            "    count += 100",  // Never executed
            "  return count",
            "result = continue_all()")
        .testLookup("result", StarlarkInt.of(5));
  }

  // ---- String Method Edge Cases ----

  @Test
  public void testStringMethods() throws Exception {
    ev.new Scenario()
        .testExpression("'hello'.upper()", "HELLO")
        .testExpression("'HELLO'.lower()", "hello")
        .testExpression("'hello world'.split(' ')", StarlarkList.of(null, "hello", "world"))
        .testExpression("' '.join(['a', 'b', 'c'])", "a b c")
        .testExpression("'hello'.replace('l', 'L')", "heLLo")
        .testExpression("'hello'.startswith('he')", true)
        .testExpression("'hello'.endswith('lo')", true)
        .testExpression("'hello'.find('ll')", StarlarkInt.of(2))
        .testExpression("'hello'.find('x')", StarlarkInt.of(-1));
  }

  @Test
  public void testStringSplitEdgeCases() throws Exception {
    ev.new Scenario()
        .testExpression("''.split(',')", StarlarkList.of(null, ""))
        .testExpression("' '.split(' ')", StarlarkList.of(null, "", ""))
        .testExpression("'a'.split(',')", StarlarkList.of(null, "a"))
        .testExpression("'a,b,c'.split(',')", StarlarkList.of(null, "a", "b", "c"))
        .testExpression("'a,,c'.split(',')", StarlarkList.of(null, "a", "", "c"));
  }

  // ---- List Method Edge Cases ----

  @Test
  public void testListAppendAndExtend() throws Exception {
    ev.new Scenario()
        .setUp(
            "lst = [1, 2, 3]",
            "lst.append(4)",
            "lst.extend([5, 6])",
            "lst.append([7, 8])")
        .testEval("lst", "[1, 2, 3, 4, 5, 6, [7, 8]]");
  }

  @Test
  public void testListInsert() throws Exception {
    ev.new Scenario()
        .setUp(
            "lst = [1, 2, 3]",
            "lst.insert(0, 0)",
            "lst.insert(10, 99)",
            "lst.insert(-1, -1)")
        .testEval("lst", "[0, 1, 2, 3, -1, 99]"); // insert(-1, x) goes before the last element
  }

  @Test
  public void testListPop() throws Exception {
    ev.new Scenario()
        .setUp(
            "lst = [1, 2, 3, 4, 5]",
            "a = lst.pop()",
            "b = lst.pop(0)",
            "c = lst.pop(1)")
        .testLookup("a", StarlarkInt.of(5))
        .testLookup("b", StarlarkInt.of(1))
        .testLookup("c", StarlarkInt.of(3))
        .testEval("lst", "[2, 4]");
  }

  // ---- Dict Method Edge Cases ----

  @Test
  public void testDictGetMethod() throws Exception {
    ev.new Scenario()
        .setUp("d = {'a': 1, 'b': 2}")
        .testEval("d.get('a')", "1")
        .testEval("d.get('c')", "None")
        .testEval("d.get('c', 'default')", "'default'")
        .testEval("d.get('a', 'default')", "1");
  }

  @Test
  public void testDictPopMethod() throws Exception {
    ev.new Scenario()
        .setUp(
            "d = {'a': 1, 'b': 2, 'c': 3}",
            "val = d.pop('b')")
        .testLookup("val", StarlarkInt.of(2))
        .testEval("d", "{'a': 1, 'c': 3}");
  }

  @Test
  public void testDictSetdefault() throws Exception {
    ev.new Scenario()
        .setUp(
            "d = {'a': 1}",
            "v1 = d.setdefault('a', 100)",
            "v2 = d.setdefault('b', 200)")
        .testLookup("v1", StarlarkInt.of(1))
        .testLookup("v2", StarlarkInt.of(200))
        .testEval("d", "{'a': 1, 'b': 200}");
  }

  // ---- Comprehension Edge Cases ----

  @Test
  public void testComprehensionWithNoElements() throws Exception {
    ev.new Scenario()
        .testEval("[x for x in [] if True]", "[]")
        .testEval("[x for x in range(10) if False]", "[]")
        .testEval("{x: x for x in [] if True}", "{}");
  }

  @Test
  public void testComprehensionWithMultipleFilters() throws Exception {
    ev.new Scenario()
        .testEval(
            "[x for x in range(31) if x % 2 == 0 if x % 3 == 0 if x % 5 == 0]",
            "[0, 30]")
        .testEval(
            "[x for x in range(20) if x > 5 if x < 15 if x % 2 == 0]",
            "[6, 8, 10, 12, 14]");
  }

  // ---- Miscellaneous Edge Cases ----

  @Test
  public void testInOperatorWithDifferentTypes() throws Exception {
    ev.new Scenario()
        .testExpression("1 in [1, 2, 3]", true)
        .testExpression("'a' in 'abc'", true)
        .testExpression("'a' in {'a': 1, 'b': 2}", true)
        .testExpression("1 in {'a': 1, 'b': 2}", false)
        .testExpression("'d' not in 'abc'", true);
  }

  @Test
  public void testRangeFunction() throws Exception {
    ev.new Scenario()
        .testEval("list(range(5))", "[0, 1, 2, 3, 4]")
        .testEval("list(range(2, 5))", "[2, 3, 4]")
        .testEval("list(range(0, 10, 2))", "[0, 2, 4, 6, 8]")
        .testEval("list(range(10, 0, -1))", "[10, 9, 8, 7, 6, 5, 4, 3, 2, 1]")
        .testEval("list(range(0))", "[]")
        .testEval("list(range(-5))", "[]");
  }

  @Test
  public void testEnumerateFunction() throws Exception {
    ev.new Scenario()
        .testEval("list(enumerate([]))", "[]")
        .testEval("list(enumerate(['a', 'b', 'c']))", "[(0, 'a'), (1, 'b'), (2, 'c')]");
  }

  @Test
  public void testZipFunction() throws Exception {
    ev.new Scenario()
        .testEval("list(zip([1, 2, 3], ['a', 'b', 'c']))", "[(1, 'a'), (2, 'b'), (3, 'c')]")
        .testEval("list(zip([1, 2], ['a', 'b', 'c']))", "[(1, 'a'), (2, 'b')]")
        .testEval("list(zip([], []))", "[]");
  }

  @Test
  public void testMinMaxFunctions() throws Exception {
    ev.new Scenario()
        .testExpression("min([1, 2, 3])", StarlarkInt.of(1))
        .testExpression("max([1, 2, 3])", StarlarkInt.of(3))
        .testExpression("min([3, 1, 2])", StarlarkInt.of(1))
        .testExpression("max([3, 1, 2])", StarlarkInt.of(3))
        .testExpression("min(['a', 'z', 'm'])", "a")
        .testExpression("max(['a', 'z', 'm'])", "z");
  }

  @Test
  public void testSortedFunction() throws Exception {
    ev.new Scenario()
        .testEval("sorted([3, 1, 2])", "[1, 2, 3]")
        .testEval("sorted([3, 1, 2], reverse=True)", "[3, 2, 1]")
        .testEval("sorted(['c', 'a', 'b'])", "['a', 'b', 'c']")
        .testEval("sorted([])", "[]");
  }

  @Test
  public void testReversedFunction() throws Exception {
    ev.new Scenario()
        .testEval("list(reversed([1, 2, 3]))", "[3, 2, 1]")
        .testEval("list(reversed([]))", "[]")
        .testEval("list(reversed(['a', 'b', 'c']))", "['c', 'b', 'a']");
  }

  @Test
  public void testAllAndAnyFunctions() throws Exception {
    ev.new Scenario()
        .testExpression("all([True, True, True])", true)
        .testExpression("all([True, False, True])", false)
        .testExpression("all([])", true)
        .testExpression("any([False, False, False])", false)
        .testExpression("any([False, True, False])", true)
        .testExpression("any([])", false);
  }

  @Test
  public void testTypeFunction() throws Exception {
    ev.new Scenario()
        .testExpression("type(1)", "int")
        .testExpression("type('a')", "string")
        .testExpression("type([])", "list")
        .testExpression("type({})", "dict")
        .testExpression("type(())", "tuple")
        .testExpression("type(True)", "bool")
        .testExpression("type(None)", "NoneType");
  }
}
