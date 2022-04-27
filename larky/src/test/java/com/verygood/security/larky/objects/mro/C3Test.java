package com.verygood.security.larky.objects.mro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.objects.LarkyPyObject;
import com.verygood.security.larky.objects.LarkyTypeObject;
import com.verygood.security.larky.objects.type.LarkyType;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * These examples are taken from the Python docs on MRO:
 *
 * <a href="https://www.python.org/download/releases/2.3/mro/">https://www.python.org/download/releases/2.3/mro/</a>
 */
class C3Test {

  private static Mutability mutability;
  private static StarlarkThread thread;

  @BeforeAll
  static void setUp() {
    mutability = Mutability.create("C3Test::setup");
    thread = new StarlarkThread(mutability, LarkySemantics.LARKY_SEMANTICS);

  }

  @AfterAll
  static void tearDown() {
    mutability.close();
  }

  @Test
  void testMROAlwaysIncludesObjectAsDefaultBase() throws EvalException {
    LarkyType O = LarkyTypeObject.create(thread, "O", Tuple.empty(), Dict.empty());
    assertEquals(Arrays.asList(O, LarkyPyObject.getInstance()), C3.calculateMRO(O));
  }

  @Test
  void testCalculateMRO_Example_1() throws EvalException {
    LarkyType O = LarkyTypeObject.create(thread, "O", Tuple.empty(), Dict.empty());
    LarkyType F = LarkyTypeObject.create(thread, "F", Tuple.of(O), Dict.empty());
    LarkyType E = LarkyTypeObject.create(thread, "E", Tuple.of(O), Dict.empty());
    LarkyType D = LarkyTypeObject.create(thread, "D", Tuple.of(O), Dict.empty());
    LarkyType C = LarkyTypeObject.create(thread, "C", Tuple.of(D, F), Dict.empty());
    LarkyType B = LarkyTypeObject.create(thread, "B", Tuple.of(D, E), Dict.empty());
    LarkyType A = LarkyTypeObject.create(thread, "B", Tuple.of(B, C), Dict.empty());

    assertEquals(Arrays.asList(A, B, C, D, E, F, O, LarkyPyObject.getInstance()), C3.calculateMRO(A));
  }



  @Test
  void testCalculateMRO_Example_2() throws EvalException {
    LarkyType O = LarkyTypeObject.create(thread, "O", Tuple.empty(), Dict.empty());
    LarkyType F = LarkyTypeObject.create(thread, "F", Tuple.of(O), Dict.empty());
    LarkyType E = LarkyTypeObject.create(thread, "E", Tuple.of(O), Dict.empty());
    LarkyType D = LarkyTypeObject.create(thread, "D", Tuple.of(O), Dict.empty());
    LarkyType C = LarkyTypeObject.create(thread, "C", Tuple.of(D, F), Dict.empty());
    // note that B has bases of "E, D", while example 1 above extends "D, E"
    LarkyType B = LarkyTypeObject.create(thread, "B", Tuple.of(E, D), Dict.empty());
    LarkyType A = LarkyTypeObject.create(thread, "B", Tuple.of(B, C), Dict.empty());

    assertEquals(Arrays.asList(A, B, E, C, D, F, O, LarkyPyObject.getInstance()), C3.calculateMRO(A));
  }

  @Test
  public void testCalculateMRO_Example_3() throws EvalException {
    LarkyType O = LarkyTypeObject.create(thread, "O", Tuple.empty(), Dict.empty());
    LarkyType A = LarkyTypeObject.create(thread, "A", Tuple.of(O), Dict.empty());
    LarkyType B = LarkyTypeObject.create(thread, "B", Tuple.of(O), Dict.empty());
    LarkyType C = LarkyTypeObject.create(thread, "C", Tuple.of(O), Dict.empty());
    LarkyType D = LarkyTypeObject.create(thread, "D", Tuple.of(O), Dict.empty());
    LarkyType E = LarkyTypeObject.create(thread, "E", Tuple.of(O), Dict.empty());
    LarkyType K1 = LarkyTypeObject.create(thread, "K1", Tuple.of(A, B, C), Dict.empty());
    LarkyType K2 = LarkyTypeObject.create(thread, "K2", Tuple.of(D, B, E), Dict.empty());
    LarkyType K3 = LarkyTypeObject.create(thread, "K3", Tuple.of(D, A), Dict.empty());
    LarkyType Z = LarkyTypeObject.create(thread, "Z", Tuple.of(K1, K2, K3), Dict.empty());
    assertEquals(Arrays.asList(A, O, LarkyPyObject.getInstance()), C3.calculateMRO(A));
    assertEquals(Arrays.asList(B, O, LarkyPyObject.getInstance()), C3.calculateMRO(B));
    assertEquals(Arrays.asList(C, O, LarkyPyObject.getInstance()), C3.calculateMRO(C));
    assertEquals(Arrays.asList(D, O, LarkyPyObject.getInstance()), C3.calculateMRO(D));
    assertEquals(Arrays.asList(E, O, LarkyPyObject.getInstance()), C3.calculateMRO(E));
    assertEquals(Arrays.asList(K1, A, B, C, O, LarkyPyObject.getInstance()), C3.calculateMRO(K1));
    assertEquals(Arrays.asList(K2, D, B, E, O, LarkyPyObject.getInstance()), C3.calculateMRO(K2));
    assertEquals(Arrays.asList(K3, D, A, O, LarkyPyObject.getInstance()), C3.calculateMRO(K3));
    assertEquals(Arrays.asList(Z, K1, K2, K3, D, A, B, C, E, O, LarkyPyObject.getInstance()), C3.calculateMRO(Z));
  }


  @Test
  public void testCalculateMRO_Example_4() {
    // test order disagreement here
    LarkyType O = LarkyTypeObject.create(thread, "O", Tuple.empty(), Dict.empty());
    LarkyType X = LarkyTypeObject.create(thread, "X", Tuple.of(O), Dict.empty());
    LarkyType Y = LarkyTypeObject.create(thread, "Y", Tuple.of(O), Dict.empty());
    LarkyType A = LarkyTypeObject.create(thread, "A", Tuple.of(X, Y), Dict.empty());
    LarkyType B = LarkyTypeObject.create(thread, "B", Tuple.of(Y, X), Dict.empty());
    Assertions.assertThrows(
      EvalException.class,
      () -> LarkyTypeObject.create(thread, "Z", Tuple.of(A, B), Dict.empty()),
      "Inconsistent hierarchy" +
        " - Unable to compute a consistent method resolution"
        + " order for Z");
  }
}