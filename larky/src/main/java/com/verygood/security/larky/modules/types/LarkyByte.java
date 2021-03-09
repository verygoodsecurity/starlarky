package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;

import com.verygood.security.larky.modules.io.TextUtil;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import javax.annotation.Nonnull;

public class LarkyByte extends LarkyObject {

  private String _string;


  /**
   * bytes() -> empty bytes object
   */
  public LarkyByte(StarlarkThread thread) throws EvalException {
    this(thread, "", true);
  }

  /**
   * bytes(int) -> bytes object of size given by the parameter initialized with null bytes
   *
   * @param sizeof ->  size given by the parameter initialized
   */
  public LarkyByte(StarlarkThread thread, int sizeof) {
    this(thread, ByteBuffer.allocate(sizeof));
  }

  /**
   * bytes(bytes_or_buffer) -> immutable copy of bytes_or_buffer
   */
  public LarkyByte(StarlarkThread thread, byte[] buf) {
    this(thread, buf, 0, buf.length);
  }

  public LarkyByte(StarlarkThread thread, byte[] buf, int off, int ending) {
    super(thread);
    StringBuilder v = new StringBuilder(buf.length);
    for (int i = off; i < ending; i++) {
      v.appendCodePoint(buf[i] & 0xFF);
    }
    _string = v.toString();
  }

  public LarkyByte(StarlarkThread thread, ByteBuffer buf) {
    super(thread);
    StringBuilder v = new StringBuilder(buf.limit());
    for (int i = 0; i < buf.limit(); i++) {
      v.appendCodePoint(buf.get(i) & 0xFF);
    }
    _string = v.toString();
  }

  /**
   * bytes(iterable_of_ints) -> bytes
   */
  public LarkyByte(StarlarkThread thread, int[] iterable_of_ints) {
    super(thread);
    StringBuilder v = new StringBuilder(iterable_of_ints.length);
    for (int i : iterable_of_ints) {
      v.appendCodePoint(i);
    }
    _string = v.toString();
  }

  /**
   * bytes(string, encoding[, errors]) -> bytes
   */
  public LarkyByte(StarlarkThread thread, @Nonnull CharSequence string) throws EvalException {
    super(thread);
    if (!TextUtil.isBytes(string)) {
      throw Starlark.errorf("Cannot create byte with non-byte value");
    }
    this._string = string.toString();
  }


  /**
   * Local-use constructor in which the client is allowed to guarantee that the
   * <code>String</code> argument contains only characters in the byte range. We do not then
   * range-check the characters.
   *
   * @param string  a Java String to be wrapped (not null)
   * @param isBytes true if the client guarantees we are dealing with bytes
   */
  private LarkyByte(StarlarkThread thread, CharSequence string, boolean isBytes) throws EvalException {
    super(thread);
    if (isBytes || TextUtil.isBytes(string)) {
      this._string = string.toString();
    } else {
      throw Starlark.errorf("Cannot create byte with non-byte value");
    }
  }

  final ImmutableMap<String, Object> of = ImmutableMap.of(
      "values_only_field",
      "fromValues",
      "values_only_method",
      returnFromValues,
      "collision_field",
      "fromValues",
      "collision_method",
      returnFromValues);

  // A function that returns "fromValues".
  private static final Object returnFromValues =
      new StarlarkCallable() {
        @Override
        public String getName() {
          return "returnFromValues";
        }

        @Override
        public Object fastcall(StarlarkThread thread, Object[] positional, Object[] named) {
          return "bar";
        }
  };

  @StarlarkMethod(name = "callable_only_field", documented = false, structField = true)
  public String getCallableOnlyField() {
    return "fromStarlarkMethod";
  }

  @StarlarkMethod(name = "callable_only_method", documented = false, structField = false)
  public String getCallableOnlyMethod() {
    return "fromStarlarkMethod";
  }

  @StarlarkMethod(name = "collision_field", documented = false, structField = true)
  public String getCollisionField() {
    return "fromStarlarkMethod";
  }

  @StarlarkMethod(name = "collision_method", documented = false, structField = false)
  public String getCollisionMethod() {
    return "fromStarlarkMethod";
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
}
