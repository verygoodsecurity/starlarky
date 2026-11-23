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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.SyntaxError;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Comprehensive tests for the Starlark interpreter covering control flow, expressions, functions,
 * data structures, error handling, and performance limits.
 */
@RunWith(JUnit4.class)
public final class InterpreterComprehensiveTest {

  private final EvaluationTestCase ev = new EvaluationTestCase();

  // ---- Control Flow Tests ----

  @Test
  public void testDeeplyNestedIfStatements() throws Exception {
    ev.new Scenario()
        .setUp(
            "def deeply_nested(x):",
            "  if x > 0:",
            "    if x > 10:",
            "      if x > 20:",
            "        if x > 30:",
            "          return 'very large'",
            "        return 'large'",
            "      return 'medium'",
            "    return 'small'",
            "  return 'zero or negative'",
            "a = deeply_nested(35)",
            "b = deeply_nested(25)",
            "c = deeply_nested(15)",
            "d = deeply_nested(5)",
            "e = deeply_nested(0)")
        .testLookup("a", "very large")
        .testLookup("b", "large")
        .testLookup("c", "medium")
        .testLookup("d", "small")
        .testLookup("e", "zero or negative");
  }

  @Test
  public void testComplexElifChain() throws Exception {
    ev.new Scenario()
        .setUp(
            "def grade(score):",
            "  if score >= 90:",
            "    return 'A'",
            "  elif score >= 80:",
            "    return 'B'",
            "  elif score >= 70:",
            "    return 'C'",
            "  elif score >= 60:",
            "    return 'D'",
            "  else:",
            "    return 'F'",
            "g1 = grade(95)",
            "g2 = grade(85)",
            "g3 = grade(75)",
            "g4 = grade(65)",
            "g5 = grade(55)")
        .testLookup("g1", "A")
        .testLookup("g2", "B")
        .testLookup("g3", "C")
        .testLookup("g4", "D")
        .testLookup("g5", "F");
  }

  @Test
  public void testNestedLoopsWithBreakAndContinue() throws Exception {
    ev.new Scenario()
        .setUp(
            "def nested_loops():",
            "  result = []",
            "  for i in range(1, 4):",
            "    for j in range(1, 4):",
            "      if i == 2 and j == 2:",
            "        continue",
            "      if i == 3 and j == 2:",
            "        break",
            "      result.append((i, j))",
            "  return result",
            "r = nested_loops()")
        .testEval("r", "[(1, 1), (1, 2), (1, 3), (2, 1), (2, 3), (3, 1)]");
  }

  @Test
  public void testComplexLoopWithMultipleExits() throws Exception {
    ev.new Scenario()
        .setUp(
            "def find_pair_sum(arr, target):",
            "  for i in range(len(arr)):",
            "    for j in range(i + 1, len(arr)):",
            "      if arr[i] + arr[j] == target:",
            "        return [i, j]",
            "  return None",
            "result = find_pair_sum([2, 7, 11, 15], 9)")
        .testEval("result", "[0, 1]");
  }

  @Test
  public void testForLoopWithComplexIterables() throws Exception {
    ev.new Scenario()
        .setUp(
            "def iterate_nested():",
            "  result = []",
            "  data = [[1, 2], [3, 4], [5, 6]]",
            "  for sublist in data:",
            "    for item in sublist:",
            "      result.append(item * 2)",
            "  return result",
            "r = iterate_nested()")
        .testEval("r", "[2, 4, 6, 8, 10, 12]");
  }

  // ---- Expression Tests ----

  @Test
  public void testAllBinaryOperators() throws Exception {
    ev.new Scenario()
        .testExpression("10 + 5", StarlarkInt.of(15))
        .testExpression("10 - 5", StarlarkInt.of(5))
        .testExpression("10 * 5", StarlarkInt.of(50))
        .testExpression("10 / 5", StarlarkFloat.of(2.0)) // '/' is float division
        .testExpression("10 // 3", StarlarkInt.of(3))
        .testExpression("10 % 3", StarlarkInt.of(1))
        .testExpression("1 << 10", StarlarkInt.of(1024))
        .testExpression("10 == 10", true)
        .testExpression("10 != 5", true)
        .testExpression("10 > 5", true)
        .testExpression("10 < 5", false)
        .testExpression("10 >= 10", true)
        .testExpression("5 <= 10", true)
        .testExpression("'hello' + ' world'", "hello world")
        .testExpression("[1, 2] + [3, 4]", StarlarkList.of(null,
            StarlarkInt.of(1), StarlarkInt.of(2), StarlarkInt.of(3), StarlarkInt.of(4)))
        .testExpression("'ab' * 3", "ababab")
        .testExpression("[1, 2] * 3", StarlarkList.of(null,
            StarlarkInt.of(1), StarlarkInt.of(2),
            StarlarkInt.of(1), StarlarkInt.of(2),
            StarlarkInt.of(1), StarlarkInt.of(2)));
  }

  @Test
  public void testOperatorPrecedence() throws Exception {
    ev.new Scenario()
        .testExpression("2 + 3 * 4", StarlarkInt.of(14))
        .testExpression("(2 + 3) * 4", StarlarkInt.of(20))
        .testExpression("10 - 2 * 3", StarlarkInt.of(4))
        .testExpression("10 // 3 * 2", StarlarkInt.of(6))
        .testExpression("10 % 3 + 2", StarlarkInt.of(3))
        .testExpression("5 > 3 and 2 < 4", true)
        .testExpression("5 > 3 or 2 > 4", true)
        .testExpression("not False and True", true);
  }

  @Test
  public void testShortCircuitEvaluation() throws Exception {
    // Test that 'and' short-circuits
    ev.new Scenario()
        .setUp(
            "call_count = [0]",
            "def side_effect():",
            "  call_count[0] += 1",
            "  return True",
            "result = False and side_effect()")
        .testLookup("result", false)
        .testLookup("call_count", StarlarkList.of(null, StarlarkInt.of(0)));

    // Test that 'or' short-circuits
    ev.new Scenario()
        .setUp(
            "call_count = [0]",
            "def side_effect():",
            "  call_count[0] += 1",
            "  return False",
            "result = True or side_effect()")
        .testLookup("result", true)
        .testLookup("call_count", StarlarkList.of(null, StarlarkInt.of(0)));
  }

  @Test
  public void testConditionalExpressionsNested() throws Exception {
    ev.new Scenario()
        .testExpression(
            "'negative' if -5 < 0 else 'zero' if -5 == 0 else 'positive'",
            "negative")
        .testExpression(
            "'negative' if 0 < 0 else 'zero' if 0 == 0 else 'positive'",
            "zero")
        .testExpression(
            "'negative' if 5 < 0 else 'zero' if 5 == 0 else 'positive'",
            "positive");
  }

  @Test
  public void testComplexBooleanExpressions() throws Exception {
    ev.new Scenario()
        .testExpression("(True and False) or (True and True)", true)
        .testExpression("not (False or False) and True", true)
        .testExpression("(5 > 3 and 2 < 1) or (10 >= 10)", true)
        .testExpression("not (not True)", true)
        .testExpression("True and not False and True", true);
  }

  @Test
  public void testUnaryOperators() throws Exception {
    ev.new Scenario()
        .testExpression("-5", StarlarkInt.of(-5))
        .testExpression("--5", StarlarkInt.of(5))
        .testExpression("+5", StarlarkInt.of(5))
        .testExpression("not True", false)
        .testExpression("not False", true)
        .testExpression("not not True", true);
  }

  // ---- Function Tests ----

  @Test
  public void testFunctionWithDefaultParameters() throws Exception {
    ev.new Scenario()
        .setUp(
            "def greet(name, greeting='Hello', punctuation='!'):",
            "  return greeting + ' ' + name + punctuation",
            "a = greet('Alice')",
            "b = greet('Bob', 'Hi')",
            "c = greet('Charlie', 'Hey', '.')",
            "d = greet('David', punctuation='?')")
        .testLookup("a", "Hello Alice!")
        .testLookup("b", "Hi Bob!")
        .testLookup("c", "Hey Charlie.")
        .testLookup("d", "Hello David?");
  }

  @Test
  public void testFunctionWithVariadicArgs() throws Exception {
    ev.new Scenario()
        .setUp(
            "def sum_all(*args):",
            "  total = 0",
            "  for x in args:",
            "    total += x",
            "  return total",
            "a = sum_all()",
            "b = sum_all(1)",
            "c = sum_all(1, 2, 3)",
            "d = sum_all(1, 2, 3, 4, 5)")
        .testLookup("a", StarlarkInt.of(0))
        .testLookup("b", StarlarkInt.of(1))
        .testLookup("c", StarlarkInt.of(6))
        .testLookup("d", StarlarkInt.of(15));
  }

  @Test
  public void testFunctionWithKwargs() throws Exception {
    ev.new Scenario()
        .setUp(
            "def make_dict(**kwargs):",
            "  return kwargs",
            "a = make_dict()",
            "b = make_dict(x=1)",
            "c = make_dict(x=1, y=2, z=3)")
        .testLookup("a", Dict.empty())
        .testEval("b", "{'x': 1}")
        .testEval("c", "{'x': 1, 'y': 2, 'z': 3}");
  }

  @Test
  public void testFunctionWithArgsAndKwargs() throws Exception {
    ev.new Scenario()
        .setUp(
            "def complex_func(required, *args, named=None, **kwargs):",
            "  return {'required': required, 'args': args, 'named': named, 'kwargs': kwargs}",
            "result = complex_func(1, 2, 3, named='value', extra1='a', extra2='b')")
        .testEval("result['required']", "1")
        .testEval("result['args']", "(2, 3)")
        .testEval("result['named']", "'value'")
        .testEval("result['kwargs']", "{'extra1': 'a', 'extra2': 'b'}");
  }

  @Test
  public void testClosures() throws Exception {
    ev.new Scenario()
        .setUp(
            "def make_counter():",
            "  count = [0]",
            "  def increment():",
            "    count[0] += 1",
            "    return count[0]",
            "  return increment",
            "counter1 = make_counter()",
            "counter2 = make_counter()",
            "a = counter1()",
            "b = counter1()",
            "c = counter2()",
            "d = counter1()")
        .testLookup("a", StarlarkInt.of(1))
        .testLookup("b", StarlarkInt.of(2))
        .testLookup("c", StarlarkInt.of(1))
        .testLookup("d", StarlarkInt.of(3));
  }

  @Test
  public void testNestedFunctions() throws Exception {
    ev.new Scenario()
        .setUp(
            "def outer(x):",
            "  def middle(y):",
            "    def inner(z):",
            "      return x + y + z",
            "    return inner",
            "  return middle",
            "result = outer(1)(2)(3)")
        .testLookup("result", StarlarkInt.of(6));
  }

  @Test
  public void testFunctionReturningFunction() throws Exception {
    ev.new Scenario()
        .setUp(
            "def multiplier(factor):",
            "  def multiply(x):",
            "    return x * factor",
            "  return multiply",
            "times3 = multiplier(3)",
            "times5 = multiplier(5)",
            "a = times3(10)",
            "b = times5(10)")
        .testLookup("a", StarlarkInt.of(30))
        .testLookup("b", StarlarkInt.of(50));
  }

  @Test
  public void testRecursionWithFlag() throws Exception {
    ParserInput input =
        ParserInput.fromLines(
            "def factorial(n):",
            "  if n <= 1:",
            "    return 1",
            "  return n * factorial(n - 1)",
            "result = factorial(5)");

    Module module = Module.create();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkSemantics semantics =
          StarlarkSemantics.builder().setBool(StarlarkSemantics.ALLOW_RECURSION, true).build();
      StarlarkThread thread = StarlarkThread.createTransient(mu, semantics);
      Starlark.execFile(input, FileOptions.DEFAULT, module, thread);
    }
    assertThat(module.getGlobal("result")).isEqualTo(StarlarkInt.of(120));
  }

  @Test
  public void testMutualRecursionWithFlag() throws Exception {
    ParserInput input =
        ParserInput.fromLines(
            "def is_even(n):",
            "  if n == 0:",
            "    return True",
            "  return is_odd(n - 1)",
            "def is_odd(n):",
            "  if n == 0:",
            "    return False",
            "  return is_even(n - 1)",
            "a = is_even(10)",
            "b = is_odd(10)",
            "c = is_even(7)",
            "d = is_odd(7)");

    Module module = Module.create();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkSemantics semantics =
          StarlarkSemantics.builder().setBool(StarlarkSemantics.ALLOW_RECURSION, true).build();
      StarlarkThread thread = StarlarkThread.createTransient(mu, semantics);
      Starlark.execFile(input, FileOptions.DEFAULT, module, thread);
    }
    assertThat(module.getGlobal("a")).isEqualTo(true);
    assertThat(module.getGlobal("b")).isEqualTo(false);
    assertThat(module.getGlobal("c")).isEqualTo(false);
    assertThat(module.getGlobal("d")).isEqualTo(true);
  }

  // ---- Data Structure Tests ----

  @Test
  public void testComplexListComprehensions() throws Exception {
    ev.new Scenario()
        .testEval("[x * 2 for x in range(5)]", "[0, 2, 4, 6, 8]")
        .testEval("[x for x in range(10) if x % 2 == 0]", "[0, 2, 4, 6, 8]")
        .testEval("[[y for y in range(x)] for x in range(4)]", "[[], [0], [0, 1], [0, 1, 2]]")
        .testEval(
            "[x + y for x in [1, 2] for y in [10, 20]]",
            "[11, 21, 12, 22]")
        .testEval(
            "[x for x in range(20) if x % 2 == 0 if x % 3 == 0]",
            "[0, 6, 12, 18]");
  }

  @Test
  public void testComplexDictComprehensions() throws Exception {
    ev.new Scenario()
        .testEval("{x: x * x for x in range(5)}", "{0: 0, 1: 1, 2: 4, 3: 9, 4: 16}")
        .testEval(
            "{k: v for k, v in [('a', 1), ('b', 2), ('c', 3)]}",
            "{'a': 1, 'b': 2, 'c': 3}")
        .testEval(
            "{x: y for x in range(3) for y in range(3) if x == y}",
            "{0: 0, 1: 1, 2: 2}")
        .testEval(
            "{k: v for k, v in {'a': 1, 'b': 2, 'c': 3}.items() if v > 1}",
            "{'b': 2, 'c': 3}");
  }

  @Test
  public void testListSlicing() throws Exception {
    ev.new Scenario()
        .setUp("lst = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]")
        .testEval("lst[2:5]", "[2, 3, 4]")
        .testEval("lst[:5]", "[0, 1, 2, 3, 4]")
        .testEval("lst[5:]", "[5, 6, 7, 8, 9]")
        .testEval("lst[::2]", "[0, 2, 4, 6, 8]")
        .testEval("lst[1::2]", "[1, 3, 5, 7, 9]")
        .testEval("lst[::-1]", "[9, 8, 7, 6, 5, 4, 3, 2, 1, 0]")
        .testEval("lst[2:8:2]", "[2, 4, 6]")
        .testEval("lst[-3:]", "[7, 8, 9]")
        .testEval("lst[:-3]", "[0, 1, 2, 3, 4, 5, 6]");
  }

  @Test
  public void testStringSlicing() throws Exception {
    ev.new Scenario()
        .setUp("s = 'abcdefghij'")
        .testEval("s[2:5]", "'cde'")
        .testEval("s[:5]", "'abcde'")
        .testEval("s[5:]", "'fghij'")
        .testEval("s[::2]", "'acegi'")
        .testEval("s[::-1]", "'jihgfedcba'")
        .testEval("s[2:8:2]", "'ceg'");
  }

  @Test
  public void testDictOperations() throws Exception {
    ev.new Scenario()
        .setUp(
            "d = {'a': 1, 'b': 2, 'c': 3}",
            "keys = list(d.keys())",
            "values = list(d.values())",
            "items = list(d.items())")
        .testEval("'a' in d", "True")
        .testEval("'d' in d", "False")
        .testEval("len(d)", "3")
        .testEval("sorted(keys)", "['a', 'b', 'c']")
        .testEval("sorted(values)", "[1, 2, 3]");
  }

  @Test
  public void testTupleUnpacking() throws Exception {
    ev.new Scenario()
        .setUp(
            "a, b = 1, 2",
            "(c, d) = (3, 4)",
            "e, f, g = [5, 6, 7]",
            "(h, (i, j)) = (8, (9, 10))")
        .testLookup("a", StarlarkInt.of(1))
        .testLookup("b", StarlarkInt.of(2))
        .testLookup("c", StarlarkInt.of(3))
        .testLookup("d", StarlarkInt.of(4))
        .testLookup("e", StarlarkInt.of(5))
        .testLookup("f", StarlarkInt.of(6))
        .testLookup("g", StarlarkInt.of(7))
        .testLookup("h", StarlarkInt.of(8))
        .testLookup("i", StarlarkInt.of(9))
        .testLookup("j", StarlarkInt.of(10));
  }

  @Test
  public void testNestedDataStructures() throws Exception {
    ev.new Scenario()
        .setUp(
            "data = {",
            "  'users': [",
            "    {'name': 'Alice', 'age': 30, 'hobbies': ['reading', 'hiking']},",
            "    {'name': 'Bob', 'age': 25, 'hobbies': ['gaming', 'cooking']}",
            "  ],",
            "  'settings': {'theme': 'dark', 'notifications': True}",
            "}")
        .testEval("data['users'][0]['name']", "'Alice'")
        .testEval("data['users'][1]['hobbies'][0]", "'gaming'")
        .testEval("data['settings']['theme']", "'dark'")
        .testEval("len(data['users'])", "2")
        .testEval("data['users'][0]['age']", "30");
  }

  // ---- Error Handling Tests ----

  @Test
  public void testTypeErrors() throws Exception {
    ev.new Scenario()
        .testIfErrorContains("unsupported binary operation: string - int", "'hello' - 5")
        .testIfErrorContains("unsupported binary operation: int + string", "5 + 'hello'")
        .testIfErrorContains("unsupported unary operation: -string", "-'hello'")
        .testIfErrorContains("'int' object is not callable", "(42)()");
  }

  @Test
  public void testNameErrors() throws Exception {
    ev.new Scenario()
        .testIfErrorContains("name 'undefined_var' is not defined", "x = undefined_var");
  }

  @Test
  public void testIndexErrors() throws Exception {
    ev.new Scenario()
        .setUp("lst = [1, 2, 3]")
        .testIfErrorContains("index out of range", "lst[10]")
        .testIfErrorContains("index out of range", "lst[-10]");
  }

  @Test
  public void testKeyErrors() throws Exception {
    ev.new Scenario()
        .setUp("d = {'a': 1, 'b': 2}")
        .testIfErrorContains("key \"c\" not found in dictionary", "d['c']");
  }

  @Test
  public void testDivisionByZero() throws Exception {
    ev.new Scenario()
        .testIfErrorContains("floating-point division by zero", "10 / 0")
        .testIfErrorContains("integer division by zero", "10 // 0")
        .testIfErrorContains("integer modulo by zero", "10 % 0");
  }

  @Test
  public void testAttributeErrors() throws Exception {
    ev.new Scenario()
        .testIfErrorContains(
            "'int' value has no field or method 'nonexistent'",
            "(42).nonexistent");
  }

  @Test
  public void testWrongNumberOfArguments() throws Exception {
    ev.new Scenario()
        .setUp("def func(a, b): return a + b")
        .testIfErrorContains("missing 1 required positional argument", "func(1)")
        .testIfErrorContains("accepts no more than 2 positional arguments but got 3", "func(1, 2, 3)");
  }

  @Test
  public void testInvalidAssignmentTarget() throws Exception {
    // This should be caught at resolution time
    ev.checkEvalErrorContains("cannot assign to", "1 = 2");
  }

  @Test
  public void testUnhashableType() throws Exception {
    ev.new Scenario()
        .testIfErrorContains("unhashable type: 'list'", "{[1, 2]: 'value'}");
  }

  // ---- Performance and Limit Tests ----

  @Test
  public void testExecutionStepLimit() throws Exception {
    Module module = Module.create();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      thread.setMaxExecutionSteps(100);

      ParserInput input = ParserInput.fromLines(
          "def f():",
          "  result = 0",
          "  for i in range(1000):",
          "    result += i",
          "  return result",
          "f()");

      EvalException ex = assertThrows(
          EvalException.class,
          () -> Starlark.execFile(input, FileOptions.DEFAULT, module, thread));
      assertThat(ex).hasMessageThat().contains("too many steps");
    }
  }

  @Test
  public void testExpirationTimeout() throws Exception {
    Module module = Module.create();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      thread.setExpirationMs(1); // Very short timeout

      ParserInput input = ParserInput.fromLines(
          "def f():",
          "  result = 0",
          "  for i in range(10000):",
          "    for j in range(100):",
          "      result += i * j",
          "  return result",
          "f()");

      EvalException ex = assertThrows(
          EvalException.class,
          () -> Starlark.execFile(input, FileOptions.DEFAULT, module, thread));
      assertThat(ex).hasMessageThat().contains("past expiration date");
    }
  }

  @Test
  public void testDeeplyNestedExpressions() throws Exception {
    // Build a deeply nested expression: ((((1 + 1) + 1) + 1) + 1)...
    StringBuilder expr = new StringBuilder();
    int depth = 100;
    for (int i = 0; i < depth; i++) {
      expr.append("(");
    }
    expr.append("1");
    for (int i = 0; i < depth; i++) {
      expr.append(" + 1)");
    }

    ev.exec("result = " + expr.toString());
    assertThat(ev.lookup("result")).isEqualTo(StarlarkInt.of(depth + 1));
  }

  @Test
  public void testLargeListCreation() throws Exception {
    ev.exec("large_list = list(range(10000))");
    Object result = ev.lookup("large_list");
    assertThat(result).isInstanceOf(StarlarkList.class);
    assertThat(((StarlarkList<?>) result).size()).isEqualTo(10000);
  }

  @Test
  public void testLargeDictCreation() throws Exception {
    ev.exec("large_dict = {i: i * 2 for i in range(1000)}");
    Object result = ev.lookup("large_dict");
    assertThat(result).isInstanceOf(Dict.class);
    assertThat(((Dict<?, ?>) result).size()).isEqualTo(1000);
  }

  // ---- Complex Integration Tests ----

  @Test
  public void testFibonacciWithMemoization() throws Exception {
    ParserInput input =
        ParserInput.fromLines(
            "cache = {}",
            "def fib(n):",
            "  if n in cache:",
            "    return cache[n]",
            "  if n <= 1:",
            "    result = n",
            "  else:",
            "    result = fib(n - 1) + fib(n - 2)",
            "  cache[n] = result",
            "  return result",
            "result = fib(20)");

    Module module = Module.create();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkSemantics semantics =
          StarlarkSemantics.builder().setBool(StarlarkSemantics.ALLOW_RECURSION, true).build();
      StarlarkThread thread = StarlarkThread.createTransient(mu, semantics);
      Starlark.execFile(input, FileOptions.DEFAULT, module, thread);
    }
    assertThat(module.getGlobal("result")).isEqualTo(StarlarkInt.of(6765));
  }

  @Test
  public void testQuickSort() throws Exception {
    ParserInput input =
        ParserInput.fromLines(
            "def quicksort(arr):",
            "  if len(arr) <= 1:",
            "    return arr",
            "  pivot = arr[len(arr) // 2]",
            "  left = [x for x in arr if x < pivot]",
            "  middle = [x for x in arr if x == pivot]",
            "  right = [x for x in arr if x > pivot]",
            "  return quicksort(left) + middle + quicksort(right)",
            "result = quicksort([3, 6, 8, 10, 1, 2, 1])");

    Module module = Module.create();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkSemantics semantics =
          StarlarkSemantics.builder().setBool(StarlarkSemantics.ALLOW_RECURSION, true).build();
      StarlarkThread thread = StarlarkThread.createTransient(mu, semantics);
      Starlark.execFile(input, FileOptions.DEFAULT, module, thread);
    }
    assertThat(module.getGlobal("result")).isEqualTo(
        StarlarkList.of(null,
            StarlarkInt.of(1), StarlarkInt.of(1), StarlarkInt.of(2), StarlarkInt.of(3),
            StarlarkInt.of(6), StarlarkInt.of(8), StarlarkInt.of(10)));
  }

  @Test
  public void testMapFilterReduce() throws Exception {
    ev.new Scenario()
        .setUp(
            "def map_func(f, lst):",
            "  return [f(x) for x in lst]",
            "def filter_func(pred, lst):",
            "  return [x for x in lst if pred(x)]",
            "def reduce_func(f, lst, init):",
            "  result = init",
            "  for x in lst:",
            "    result = f(result, x)",
            "  return result",
            "data = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]",
            "doubled = map_func(lambda x: x * 2, data)",
            "evens = filter_func(lambda x: x % 2 == 0, data)",
            "sum_all = reduce_func(lambda a, b: a + b, data, 0)")
        .testEval("doubled", "[2, 4, 6, 8, 10, 12, 14, 16, 18, 20]")
        .testEval("evens", "[2, 4, 6, 8, 10]")
        .testEval("sum_all", "55");
  }

  @Test
  public void testComplexStringFormatting() throws Exception {
    ev.new Scenario()
        .testExpression("'%s %d' % ('hello', 42)", "hello 42")
        .testExpression("'%s' % (42,)", "42")
        .testExpression("'%d %d %d' % (1, 2, 3)", "1 2 3")
        .testExpression("'%%%s%%' % 'test'", "%test%")
        .testExpression("'%r' % 'test'", "\"test\"");
  }

  @Test
  public void testComplexMutationScenarios() throws Exception {
    ev.new Scenario()
        .setUp(
            "lst = [1, 2, 3]",
            "lst.append(4)",
            "lst.extend([5, 6])",
            "lst.insert(0, 0)",
            "popped = lst.pop()",
            "removed = lst.remove(2)")
        .testEval("lst", "[0, 1, 3, 4, 5]")
        .testLookup("popped", StarlarkInt.of(6));
  }

  @Test
  public void testScopeChain() throws Exception {
    ev.new Scenario()
        .setUp(
            "x = 'global'",
            "def outer():",
            "  x = 'outer'",
            "  def middle():",
            "    x = 'middle'",
            "    def inner():",
            "      return x",
            "    return inner()",
            "  return middle()",
            "result = outer()")
        .testLookup("result", "middle")
        .testLookup("x", "global");
  }

  @Test
  public void testGeneratorLikeComprehensions() throws Exception {
    ev.new Scenario()
        .setUp(
            "matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]",
            "flattened = [item for row in matrix for item in row]",
            "filtered = [item for row in matrix for item in row if item % 2 == 0]",
            "transformed = [[item * 2 for item in row] for row in matrix]")
        .testEval("flattened", "[1, 2, 3, 4, 5, 6, 7, 8, 9]")
        .testEval("filtered", "[2, 4, 6, 8]")
        .testEval("transformed", "[[2, 4, 6], [8, 10, 12], [14, 16, 18]]");
  }
}
