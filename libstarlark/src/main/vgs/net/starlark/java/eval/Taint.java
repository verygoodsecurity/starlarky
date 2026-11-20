package net.starlark.java.eval;

import net.starlark.java.syntax.TokenKind;

/**
 * The interpreter's secret-taint hooks. Each is called only after its caller has checked that the
 * thread holds a {@link SecretTaint}, or that a {@code @SecretSource} just returned; runs that
 * never reveal therefore reach none of this code. None of them throws: a failure saturates the
 * run instead, so that sinks treat it as tainted while the script keeps working.
 */
final class Taint {
  private Taint() {}

  /** Captures potential alias identities before an annotated source can mutate its inputs. */
  static Object snapshotSourceInput(StarlarkThread thread, Object input) {
    if (input instanceof String) {
      return input;
    }
    SecretTaint taint = sourceTaint(thread);
    if (taint == null) {
      return null;
    }
    Object[] snapshot = taint.snapshotSourceBatch(input);
    if (taint.isActive()) {
      thread.secretTaint = taint;
    }
    return snapshot;
  }

  private static SecretTaint sourceTaint(StarlarkThread thread) {
    SecretTaint taint = thread.secretTaint;
    if (taint == null) {
      taint = thread.getThreadLocal(SecretTaint.class);
    }
    return taint != null && taint.mode() != TaintMode.OFF ? taint : null;
  }

  /** After a {@code @StarlarkMethod} call; {@code receiver} is the bound object or string. */
  static Object afterBuiltin(
      StarlarkThread thread, MethodDescriptor desc, Object receiver, Object[] args,
      Object result, Object sourceInput) {
    SecretTaint taint = thread.secretTaint;
    if (desc.isSecretSource()) {
      taint = sourceTaint(thread);
      if (taint == null) {
        return result;
      }
      Object value = taint.onSource(result, sourceInput);
      if (taint.isActive()) {
        thread.secretTaint = taint;
      }
      return value;
    }
    if (taint == null || desc.isDeclassifies()) {
      return result;
    }
    try {
      return propagate(taint, receiver, args, result, desc.returnsVoid());
    } catch (RuntimeException | StackOverflowError e) {
      taint.saturate();
      return result;
    }
  }

  /** After a binary operator; also covers in-place operators, whose receiver is {@code x}. */
  static Object binary(StarlarkThread thread, TokenKind op, Object x, Object y, Object result) {
    SecretTaint taint = thread.secretTaint;
    try {
      if (!taint.anyTainted(x, y)) {
        return result;
      }
      if (op == TokenKind.PLUS && x instanceof String left && y instanceof String right) {
        return taint.tagConcatenation(left, right, (String) result);
      }
      tagIfMutable(taint, x);
      return carry(taint, x, result, false);
    } catch (RuntimeException | StackOverflowError e) {
      taint.saturate();
      return result;
    }
  }

  /** After a unary operator, {@code op x}. */
  static Object unary(StarlarkThread thread, Object x, Object result) {
    SecretTaint taint = thread.secretTaint;
    try {
      return taint.anyTainted(x) ? carry(taint, x, result, false) : result;
    } catch (RuntimeException | StackOverflowError e) {
      taint.saturate();
      return result;
    }
  }

  /** After an index or slice expression; {@code keys} are its index or bounds. */
  static Object readFrom(StarlarkThread thread, Object object, Object result, Object... keys) {
    SecretTaint taint = thread.secretTaint;
    try {
      if (result == null || !(taint.anyTainted(object) || taint.anyTainted(keys))) {
        return result;
      }
      return carry(taint, object, result, false);
    } catch (RuntimeException | StackOverflowError e) {
      taint.saturate();
      return result;
    }
  }

  /** After reading a field or bound method off {@code object}. */
  static Object field(StarlarkThread thread, Object object, Object result) {
    SecretTaint taint = thread.secretTaint;
    try {
      if (result instanceof StarlarkCallable) {
        // A bound built-in or def carries no data itself. A Java callable read off a tainted
        // object (an iterator's __next__, say) has that object's state, and no descriptor.
        if (isOpaqueCallable(result) && taint.isTaggedShallow(object)) {
          taint.tagShallow(result);
        }
        return result;
      }
      return taint.isTaggedShallow(object) ? carry(taint, object, result, false) : result;
    } catch (RuntimeException | StackOverflowError e) {
      taint.saturate();
      return result;
    }
  }

  /** After calling a Java callable, which has no descriptor and is not otherwise hooked. */
  static Object afterCallable(StarlarkThread thread, StarlarkCallable callable, Object result) {
    SecretTaint taint = thread.secretTaint;
    try {
      if (isOpaqueCallable(callable) && taint.isTaggedShallow(callable)) {
        return carry(taint, callable, result, false);
      }
      return result;
    } catch (RuntimeException | StackOverflowError e) {
      taint.saturate();
      return result;
    }
  }

  private static boolean isOpaqueCallable(Object callable) {
    return !(callable instanceof StarlarkFunction) && !(callable instanceof BuiltinFunction);
  }

  /** Before {@code object.field = value}: a tainted value makes the object tainted. */
  static void onSetField(StarlarkThread thread, Object object, Object value) {
    SecretTaint taint = thread.secretTaint;
    try {
      if (taint.anyTainted(value)) {
        tagIfMutable(taint, object);
      }
    } catch (RuntimeException | StackOverflowError e) {
      taint.saturate();
    }
  }

  /**
   * The value of {@code bool(x)} for a branch: if, and, or, not, ternary, comprehension filter.
   * Truth-testing a tagged value makes the run secret-dependent.
   */
  static boolean truth(StarlarkThread thread, Object x) {
    SecretTaint taint = thread.secretTaint;
    if (taint != null) {
      try {
        if (taint.isTaggedShallow(x)) {
          taint.markSecretDependent();
        }
      } catch (RuntimeException | StackOverflowError e) {
        taint.saturate();
      }
    }
    return Starlark.truth(x);
  }

  /** Each element of a {@code for} loop or comprehension over {@code sequence}. */
  static Object element(StarlarkThread thread, Object sequence, Object element) {
    SecretTaint taint = thread.secretTaint;
    try {
      if (!taint.isTaggedShallow(sequence) || element == null) {
        return element;
      }
      return carry(taint, sequence, element, false);
    } catch (RuntimeException | StackOverflowError e) {
      taint.saturate();
      return element;
    }
  }

  private static Object propagate(
      SecretTaint taint, Object receiver, Object[] args, Object result, boolean returnsVoid) {
    if (!taint.anyTainted(receiver) && !taint.anyTainted(args)) {
      return result;
    }
    tagIfMutable(taint, receiver);
    // Native code also writes into output arguments, as a cipher does into a bytearray.
    for (Object arg : args) {
      tagIfMutable(taint, arg);
    }
    return carry(taint, receiver, result, returnsVoid);
  }

  // Tags result (deep) or, when it cannot hold a tag, marks the run secret-dependent.
  private static Object carry(SecretTaint taint, Object source, Object result, boolean returnsVoid) {
    if (result instanceof String s && s.isEmpty()) {
      return result;
    }
    if (result == Starlark.NONE && returnsVoid) {
      return result; // a mutator's None says nothing about the secret
    }
    if (!SecretTaint.isTaggable(result) && !StringModule.isMemoized(result)) {
      taint.markSecretDependent();
      return result;
    }
    if (source instanceof String && result instanceof StarlarkList<?> list) {
      result = copyMemoizedElements(list);
    }
    return taint.tag(result);
  }

  // Shallow: a deep walk on every list.append would be quadratic, and the new element is tagged.
  private static void tagIfMutable(SecretTaint taint, Object receiver) {
    if (receiver == null || receiver instanceof String || SecretTaint.isGlobalModule(receiver)) {
      return;
    }
    if (receiver instanceof StarlarkValue value
        && !value.isImmutable()
        && !(receiver instanceof Mutability.Freezable f && f.mutability().isFrozen())) {
      taint.tagShallow(receiver);
    }
  }

  // str.elems() and friends return shared single-character strings; give this run its own.
  private static StarlarkList<?> copyMemoizedElements(StarlarkList<?> list) {
    Object[] elements = list.toArray();
    boolean copied = false;
    for (int i = 0; i < elements.length; i++) {
      if (elements[i] instanceof String s && StringModule.isMemoized(s)) {
        elements[i] = new String(s);
        copied = true;
      }
    }
    return copied ? StarlarkList.wrap(list.mutability(), elements) : list;
  }
}
