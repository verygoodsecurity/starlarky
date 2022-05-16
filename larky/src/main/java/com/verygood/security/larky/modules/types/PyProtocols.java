package com.verygood.security.larky.modules.types;

public final class PyProtocols {
  //see: https://github.com/verygoodsecurity/starlarky/blob/cd0885efd3f99d1aecccf651d33cfb870f779961/larky/src/main/java/com/verygood/security/larky/py/PyProtocols.java
  public static final String __NAME__ = "__name__";
  public static final String __MAIN__ = "__main__";
  public static final String __MODULE__ = "__module__";

  public static final String __CLASS__ = "__class__";
  public static final String __CALL__ = "__call__";

  /* strings, bytes, and representations */
  public static final String __STR__ = "__str__";
  public static final String __REPR__ = "__repr__";
  public static final String __BYTES__ = "__bytes__";

  /* object and inheritance */
  public static final String __MRO__ = "__mro__";
  public static final String __BASES__ = "__bases__";
  public static final String __NEW__ = "__new__";
  public static final String __INIT__ = "__init__";

  /* descriptor */
  public static final String __GET__ = "__get__";
  public static final String __SET__ = "__set__";

  /* object field operators */
  public static final String __GETATTR__ = "__getattr__";
  public static final String __GETATTRIBUTE__ = "__getattribute__";
  public static final String __GETITEM__ = "__getitem__";
  public static final String __SETATTR__ = "__setattr__";
  public static final String __SETITEM__ = "__setitem__";

  /* delete operators */
  public static final String __DEL__ = "__del__";
  public static final String __DELETE__ = "__delete__";
  public static final String __DELITEM__ = "__delitem__";
  public static final String __DELATTR__ = "__delattr__";

  /* duck-typing operators */
  public static final String __CONTAINS__ = "__contains__";
  public static final String __DICT__ = "__dict__";
  public static final String __DOC__ = "__doc__";
  public static final String __FORMAT__ = "__format__";
  public static final String __HASH__ = "__hash__";
  public static final String __LEN__ = "__len__";
  public static final String __REVERSED__ = "__reversed__";
  public static final String __LENGTH_HINT__ = "__length_hint__";

  /* mathematical + binary operators */
  public static final String __ADD__ = "__add__";
  public static final String __AND__ = "__and__";
  public static final String __FLOORDIV__ = "__floordiv__";
  public static final String __INT__ = "__int__";
  public static final String __LSHIFT__ = "__lshift__";
  public static final String __MATMUL__ = "__matmul__";
  public static final String __MOD__ = "__mod__";
  public static final String __MUL__ = "__mul__";
  public static final String __OR__ = "__or__";
  public static final String __POW__ = "__pow__";
  public static final String __RADD__ = "__radd__";
  public static final String __RFLOORDIV__ = "__rfloordiv__";
  public static final String __RMOD__ = "__rmod__";
  public static final String __RMUL__ = "__rmul__";
  public static final String __RSHIFT__ = "__rshift__";
  public static final String __RSUB__ = "__rsub__";
  public static final String __SUB__ = "__sub__";
  public static final String __TRUEDIV__ = "__truediv__";
  public static final String __XOR__ = "__xor__";

  /* in place mathematical binary operators */
  public static final String __IADD__ = "__iadd__";
  public static final String __IAND__ = "__iand__";
  public static final String __IFLOORDIV__ = "__ifloordiv__";
  public static final String __ILSHIFT__ = "__ilshift__";
  public static final String __IMATMUL__ = "__imatmul__";
  public static final String __IMOD__ = "__imod__";
  public static final String __IMUL__ = "__imul__";
  public static final String __IOR__ = "__ior__";
  public static final String __IPOW__ = "__ipow__";
  public static final String __IRSHIFT__ = "__irshift__";
  public static final String __ISUB__ = "__isub__";
  public static final String __ITRUEDIV__ = "__itruediv__";
  public static final String __IXOR__ = "__ixor__";

  public static final String __INVERT__ = "__invert__";
  public static final String __NEG__ = "__neg__";
  public static final String __POS__ = "__pos__";

  /* comparison && truthiness operators */

  public static final String __BOOL__ = "__bool__";
  public static final String __INDEX__ = "__index__";
  public static final String __EQ__ = "__eq__";
  public static final String __GE__ = "__ge__";
  public static final String __GT__ = "__gt__";
  public static final String __ITER__ = "__iter__";
  public static final String __LE__ = "__le__";
  public static final String __LT__ = "__lt__";
  public static final String __NE__ = "__ne__";
  public static final String __NEXT__ = "__next__";

  public static final String __FLOAT__ = "__float__";
  public static final String __ABS__ = "__abs__";
  public static final String __DIVMOD__ = "__divmod__";
  public static final String __RMATMUL__ = "__rmatmul__";
  public static final String __RTRUEDIV__ = "__rtruediv__";
  public static final String __RDIVMOD__ = "__rdivmod__";
  public static final String __RPOW__ = "__rpow__";
  public static final String __RLSHIFT__ = "__rlshift__";
  public static final String __RRSHIFT__ = "__rrshift__";
  public static final String __RAND__ = "__rand__";
  public static final String __RXOR__ = "__rxor__";
  public static final String __ROR__ = "__ror__";
  public static final String __SUBCLASSHOOK__ = "__subclasshook__";
  public static final String __INSTANCECHECK__ = "__instancecheck__";
  public static final String __SUBCLASSCHECK__ = "__subclasscheck__";
  public static final String __INIT_SUBCLASS__ = "__init_subclass__";
  public static final String __DIR__ = "__dir__";


  private PyProtocols() {
  }
}
