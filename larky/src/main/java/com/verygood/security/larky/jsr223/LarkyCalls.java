package com.verygood.security.larky.jsr223;

import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.modules.utils.Reporter;
import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.StarlarkUtil;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.script.ScriptException;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;

/**
 * Calls a function of an evaluated script from Java, as {@link javax.script.Invocable} does: on a thread of its own,
 * with the print handler, step limit and expiration the interpreter gives a script's own top level.
 */
final class LarkyCalls {

  /** What a call needs besides its function and arguments: its limits and its {@code @vgs//} module providers. */
  record Limits(@Nullable Long stepLimit, @Nullable Long expirationMs, ModuleProviders providers) {

    static final Limits NONE = new Limits(null, null, ModuleProviders.NONE);

    /**
     * The limits and providers named by these bindings (see {@link LarkyEvaluator#STEP_LIMIT} and
     * {@link LarkyEvaluator#MODULE_PROVIDERS}).
     */
    static Limits of(Map<String, ?> bindings) {
      return new Limits(asLong(bindings.get(LarkyEvaluator.STEP_LIMIT)),
          asLong(bindings.get(LarkyEvaluator.EXPIRATION_MS)), providers(bindings));
    }

    /** These limits and providers, with any that {@code bindings} names instead. */
    Limits with(Map<String, ?> bindings) {
      final Limits other = of(bindings);
      return new Limits(other.stepLimit != null ? other.stepLimit : stepLimit,
          other.expirationMs != null ? other.expirationMs : expirationMs,
          bindings.containsKey(LarkyEvaluator.MODULE_PROVIDERS) ? other.providers : providers);
    }

    private static ModuleProviders providers(Map<String, ?> bindings) {
      return ModuleProviders.given(bindings.get(LarkyEvaluator.MODULE_PROVIDERS));
    }

    @Nullable
    private static Long asLong(@Nullable Object value) {
      // the interpreter reads these from its bindings with Long.parseLong(value.toString())
      return value == null ? null : Long.parseLong(value.toString());
    }
  }

  /** Runs a call of a function by name. */
  @FunctionalInterface
  interface Invoker {
    Object invoke(String name, Object[] args) throws ScriptException, NoSuchMethodException;
  }

  private LarkyCalls() {
  }

  /**
   * Calls {@code function} with {@code args}, converted to Starlark values as bindings are, on a new thread whose
   * {@code print} goes to {@code console}.
   */
  static Object call(StarlarkCallable function, Object[] args, Console console, Limits limits)
      throws ScriptException {
    final List<Object> starlarkArgs = new ArrayList<>(args == null ? 0 : args.length);
    if (args != null) {
      for (Object arg : args) {
        starlarkArgs.add(StarlarkUtil.valueToStarlark(arg));
      }
    }
    try (Mutability mu = Mutability.create("larky-call")) {
      final StarlarkThread thread = StarlarkThread.createTransient(mu, LarkySemantics.LARKY_SEMANTICS);
      final Reporter reporter = new Reporter(console);
      thread.setThreadLocal(Reporter.class, reporter);
      thread.setPrintHandler(reporter::report);
      limits.providers().installOn(thread);
      if (limits.stepLimit() != null) {
        thread.setMaxExecutionSteps(limits.stepLimit());
      }
      if (limits.expirationMs() != null) {
        thread.setExpirationMs(limits.expirationMs());
      }
      try {
        return Starlark.call(thread, function, starlarkArgs, Map.of());
      } catch (EvalException e) {
        // as the interpreter reports a script whose top level fails (LarkyEvaluator, then LarkyCompiledScript)
        throw LarkyEvaluationScriptException.of(new StarlarkEvalWrapper.Exc.RuntimeEvalException(e, thread));
      } catch (StarlarkEvalWrapper.Exc.RuntimeEvalException | Starlark.UncheckedEvalException e) {
        throw LarkyEvaluationScriptException.of(e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ScriptException(e);
      }
    }
  }

  /** The method {@code name} of the Starlark value {@code thiz}. */
  static StarlarkCallable method(Object thiz, String name) throws NoSuchMethodException {
    if (thiz == null) {
      throw new IllegalArgumentException("no object to call " + name + " on");
    }
    if (attribute(thiz, name) instanceof StarlarkCallable callable) {
      return callable;
    }
    throw new NoSuchMethodException(Starlark.type(thiz) + " has no method " + name);
  }

  /** Whether the Starlark value {@code thiz} has a method {@code name}. */
  static boolean hasMethod(Object thiz, String name) {
    return attribute(thiz, name) instanceof StarlarkCallable;
  }

  @Nullable
  private static Object attribute(Object thiz, String name) {
    try {
      return Starlark.getattr(Mutability.IMMUTABLE, LarkySemantics.LARKY_SEMANTICS, thiz, name, Starlark.NONE);
    } catch (EvalException e) {
      return null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  /**
   * An implementation of {@code type} whose methods call the functions of the same name, or null when a method of
   * {@code type} has no function ({@link javax.script.Invocable#getInterface}).
   */
  @Nullable
  static <T> T proxy(Class<T> type, Predicate<String> defines, Invoker invoker) {
    if (type == null || !type.isInterface()) {
      throw new IllegalArgumentException("not an interface: " + type);
    }
    for (Method method : type.getMethods()) {
      if (Modifier.isAbstract(method.getModifiers()) && !defines.test(method.getName())) {
        return null;
      }
    }
    final InvocationHandler handler = (proxy, method, args) -> {
      if (method.getDeclaringClass() == Object.class) {
        return switch (method.getName()) {
          case "equals" -> proxy == args[0];
          case "hashCode" -> System.identityHashCode(proxy);
          default -> type.getName() + "@larky";
        };
      }
      if (method.isDefault()) {
        return InvocationHandler.invokeDefault(proxy, method, args);
      }
      return toJava(invoker.invoke(method.getName(), args == null ? new Object[0] : args), method);
    };
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }

  /** A Starlark result as the Java type {@code method} returns. */
  @Nullable
  static Object toJava(Object value, Method method) {
    final Class<?> type = method.getReturnType();
    if (type == void.class) {
      return null;
    }
    if (value == Starlark.NONE || value == null) {
      if (type.isPrimitive()) {
        throw new ClassCastException(method.getName() + " returned None, not " + type.getName());
      }
      return null;
    }
    if (value instanceof StarlarkInt i) {
      if (type == int.class || type == Integer.class) {
        return i.toIntUnchecked();
      }
      if (type == long.class || type == Long.class) {
        return i.toBigInteger().longValueExact();
      }
      if (type == BigInteger.class) {
        return i.toBigInteger();
      }
    }
    if (value instanceof StarlarkFloat f && (type == double.class || type == Double.class)) {
      return f.toDouble();
    }
    if (type.isInstance(value) || (type == boolean.class && value instanceof Boolean)) {
      return value;
    }
    throw new ClassCastException(
        method.getName() + " returned " + Starlark.type(value) + ", not " + type.getName());
  }
}
