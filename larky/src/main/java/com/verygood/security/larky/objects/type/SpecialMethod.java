package com.verygood.security.larky.objects.type;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.verygood.security.larky.modules.types.PyProtocols;

public enum SpecialMethod {
  /*
   * Ported from:
   *
   *   https://github.com/python/cpython/blob/3.10/Objects/typeobject.c#L7910-L8093
   *
   */
  dunder_getattribute(PyProtocols.__GETATTRIBUTE__),
  dunder_getattr(PyProtocols.__GETATTR__),
  dunder_setattr(PyProtocols.__SETATTR__),
  dunder_delattr(PyProtocols.__DELATTR__),
  dunder_repr(PyProtocols.__REPR__),
  dunder_hash(PyProtocols.__HASH__),
  dunder_call(PyProtocols.__CALL__),
  dunder_str(PyProtocols.__STR__),
  dunder_lt(PyProtocols.__LT__),
  dunder_le(PyProtocols.__LE__),
  dunder_eq(PyProtocols.__EQ__),
  dunder_ne(PyProtocols.__NE__),
  dunder_gt(PyProtocols.__GT__),
  dunder_ge(PyProtocols.__GE__),
  dunder_iter(PyProtocols.__ITER__),
  dunder_next(PyProtocols.__NEXT__),
  dunder_get(PyProtocols.__GET__),
  dunder_set(PyProtocols.__SET__),
  dunder_delete(PyProtocols.__DELETE__),
  dunder_init(PyProtocols.__INIT__),
  dunder_radd(PyProtocols.__RADD__),
  dunder_rsub(PyProtocols.__RSUB__),
  dunder_rmul(PyProtocols.__RMUL__),
  dunder_rmod(PyProtocols.__RMOD__),
  dunder_rdivmod(PyProtocols.__RDIVMOD__),
  dunder_rpow(PyProtocols.__RPOW__),
  dunder_rlshift(PyProtocols.__RLSHIFT__),
  dunder_rrshift(PyProtocols.__RRSHIFT__),
  dunder_rand(PyProtocols.__RAND__),
  dunder_rxor(PyProtocols.__RXOR__),
  dunder_ror(PyProtocols.__ROR__),
  dunder_rfloordiv(PyProtocols.__RFLOORDIV__),
  dunder_rtruediv(PyProtocols.__RTRUEDIV__),
  dunder_rmatmul(PyProtocols.__RMATMUL__),
  dunder_add(PyProtocols.__ADD__),
  dunder_sub(PyProtocols.__SUB__),
  dunder_mul(PyProtocols.__MUL__),
  dunder_mod(PyProtocols.__MOD__),
  dunder_divmod(PyProtocols.__DIVMOD__),
  dunder_pow(PyProtocols.__POW__),
  dunder_neg(PyProtocols.__NEG__),
  dunder_pos(PyProtocols.__POS__),
  dunder_abs(PyProtocols.__ABS__),
  dunder_bool(PyProtocols.__BOOL__),
  dunder_invert(PyProtocols.__INVERT__),
  dunder_lshift(PyProtocols.__LSHIFT__),
  dunder_rshift(PyProtocols.__RSHIFT__),
  dunder_and(PyProtocols.__AND__),
  dunder_xor(PyProtocols.__XOR__),
  dunder_or(PyProtocols.__OR__),
  dunder_int(PyProtocols.__INT__),
  dunder_float(PyProtocols.__FLOAT__),
  dunder_iadd(PyProtocols.__IADD__),
  dunder_isub(PyProtocols.__ISUB__),
  dunder_imul(PyProtocols.__IMUL__),
  dunder_imod(PyProtocols.__IMOD__),
  dunder_iand(PyProtocols.__IAND__),
  dunder_ixor(PyProtocols.__IXOR__),
  dunder_ior(PyProtocols.__IOR__),
  dunder_floordiv(PyProtocols.__FLOORDIV__),
  dunder_truediv(PyProtocols.__TRUEDIV__),
  dunder_ifloordiv(PyProtocols.__IFLOORDIV__),
  dunder_itruediv(PyProtocols.__ITRUEDIV__),
  dunder_index(PyProtocols.__INDEX__),
  dunder_matmul(PyProtocols.__MATMUL__),
  dunder_imatmul(PyProtocols.__IMATMUL__),
  dunder_len(PyProtocols.__LEN__),
  dunder_getitem(PyProtocols.__GETITEM__),
  dunder_setitem(PyProtocols.__SETITEM__),
  dunder_delitem(PyProtocols.__DELITEM__),
  dunder_contains(PyProtocols.__CONTAINS__),

  NOT_SET("NOT_SET");

  private final String value;
  private static final Map<String, SpecialMethod> MAP =
    Stream
      .of(SpecialMethod.values())
      .collect(
        Collectors.toMap(Object::toString, Function.identity()));

  SpecialMethod(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static SpecialMethod of(String value) {
    return MAP.getOrDefault(value, NOT_SET);
  }

}
