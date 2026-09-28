package com.verygood.security.larky;

import net.starlark.java.eval.StarlarkSemantics;

public final class LarkySemantics {

  private LarkySemantics() {}

  /**
   * Whether calls to the {@code type()} function in Larky returns String or underlying class.
   */
  public static final String PYCOMPAT_TYPE_BUILTIN_FUNCTION = "-pycompat_type_builtin_function";

  public static final StarlarkSemantics LARKY_SEMANTICS = StarlarkSemantics.DEFAULT
      .toBuilder()
      .setBool(PYCOMPAT_TYPE_BUILTIN_FUNCTION, false)
      // Starlark's fail() omits the Starlark stack trace unless asked; Larky has always
      // reported it (callers rely on the traceback in the error message).
      .setBool(StarlarkSemantics.FORCE_STARLARK_STACK_TRACE, true)
      .build();

}
