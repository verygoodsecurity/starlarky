package com.verygood.security.larky;

import net.starlark.java.eval.PythonStrings;
import net.starlark.java.eval.PercentFormat;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.syntax.FileOptions;

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
      // str.find/count/startswith/... match nothing when start > end, as in Python
      // ("abc".find("", 4) == -1; Starlark clamps start and finds "" at 3).
      .setBool(PythonStrings.PYTHON_STRING_BOUNDS, true)
      // str case mapping (upper, lower, title, capitalize), classification (isalpha, isdigit,
      // islower, ...) and strip() follow Python's Unicode rules, not ASCII only.
      .setBool(PythonStrings.PYTHON_UNICODE_STRINGS, true)
      // str % args follows Python's printf-style formatting (flags, width, precision, %(key)s).
      .setBool(PercentFormat.PYTHON_PERCENT_FORMAT, true)
      // Upstream Starlark turned static type checking on by default; Larky keeps it off unless a
      // host asks for it (see LarkyScript#scriptFileOptions).
      .setBool(StarlarkSemantics.EXPERIMENTAL_STARLARK_STATIC_TYPE_CHECKING, false)
      .build();

  /**
   * Upstream's default semantics with static type checking off, for the entry points that run
   * plain Starlark rather than {@link #LARKY_SEMANTICS} (the Larky REPL and the runner's).
   */
  public static final StarlarkSemantics STARLARK_SEMANTICS = StarlarkSemantics.DEFAULT
      .toBuilder()
      .setBool(StarlarkSemantics.EXPERIMENTAL_STARLARK_STATIC_TYPE_CHECKING, false)
      .build();

  /**
   * The file options Larky starts from. Upstream Starlark now allows and resolves type syntax by
   * default, which makes {@code isinstance} a keyword and rejects scripts that call
   * {@code isinstance(...)} as a function; Larky turns type syntax on only when type checking is
   * enabled (see LarkyScript#scriptFileOptions).
   */
  public static final FileOptions FILE_OPTIONS =
      FileOptions.DEFAULT.toBuilder().allowTypeSyntax(false).resolveTypeSyntax(false).build();

}
