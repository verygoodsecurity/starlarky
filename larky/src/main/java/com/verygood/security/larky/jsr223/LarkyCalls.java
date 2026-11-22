package com.verygood.security.larky.jsr223;

import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.modules.utils.Reporter;
import com.verygood.security.larky.runtime.EvaluationMetadata;
import com.verygood.security.larky.runtime.EvaluationSettings;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.runtime.CurrentEvaluation;
import com.verygood.security.larky.parser.StarlarkUtil;
import com.verygood.security.larky.runtime.LarkyEvaluator;
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
import javax.script.ScriptContext;
import javax.script.ScriptException;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;

/**
 * Calls a function of an evaluated script from Java, as {@link javax.script.Invocable} does: on a thread of its own,
 * with the print handler, step limit and expiration the interpreter gives a script's own top level.
 */
final class LarkyCalls {

  /** Runs a call of a function by name. */
  @FunctionalInterface
  interface Invoker {
    Object invoke(String name, Object[] args) throws ScriptException, NoSuchMethodException;
  }

  private LarkyCalls() {
  }

  /** The context's writer, or standard output when it has none. */
  static java.io.Writer writer(javax.script.ScriptContext context) {
    final java.io.Writer writer = context.getWriter();
    return writer != null ? writer : new java.io.PrintWriter(System.out, true);
  }

  /** Runs a call with mutability owned by Larky and publishes metadata to a Larky context. */
  static Object call(StarlarkCallable function, Object[] args, Console console, Map<String, ?> bindings,
      ScriptContext callContext) throws ScriptException {
    LarkyEvaluator.refuseRetiredSettings(bindings.keySet());
    var context = callContext instanceof LarkyScriptContext larky ? larky : null;
    var settings = context == null ? EvaluationSettings.NONE : context.getSettings();
    try (Mutability mu = Mutability.create("larky-call")) {
      var thread = StarlarkThread.createTransient(mu, LarkySemantics.LARKY_SEMANTICS);
      var reporter = new Reporter(console);
      thread.setThreadLocal(Reporter.class, reporter);
      thread.setPrintHandler(reporter::report);
      settings.moduleProviders().installOn(thread);
      settings.stepLimit().ifPresent(thread::setMaxExecutionSteps);
      settings.expirationMs().ifPresent(thread::setExpirationMs);
      try (CurrentEvaluation ignored = CurrentEvaluation.enter(thread)) {
        return callActive(thread, function, args, context, settings);
      }
    }
  }

  static Object callMethodInScope(StarlarkThread thread, Object receiver, String name, Object[] args)
      throws ScriptException, NoSuchMethodException {
    Mutability mutability = thread.mutability();
    if (mutability.isFrozen()) {
      throw new IllegalStateException("invocation requires an open Starlark mutability");
    }
    try (CurrentEvaluation ignored = CurrentEvaluation.enter(thread)) {
      var function = method(receiver, name, mutability, thread.getSemantics());
      return callActive(thread, function, args, null, EvaluationSettings.NONE);
    }
  }

  private static Object callActive(
      StarlarkThread thread, StarlarkCallable function, Object[] args,
      @Nullable LarkyScriptContext context, EvaluationSettings settings) throws ScriptException {
    EvaluationMetadata.Outcome outcome = EvaluationMetadata.Outcome.INTERNAL;
    LarkyEvaluationScriptException failure = null;
    long start = context == null ? 0 : System.nanoTime();
    try {
      List<Object> starlarkArgs = List.of();
      if (args != null && args.length > 0) {
        starlarkArgs = new ArrayList<>(args.length);
        for (Object arg : args) {
          // Existing Starlark values keep their identity and owning mutability.
          starlarkArgs.add(Starlark.valid(arg)
              ? arg : StarlarkUtil.valueToStarlark(arg, thread.mutability()));
        }
      }
      Object result = Starlark.call(thread, function, starlarkArgs, Map.of());
      outcome = EvaluationMetadata.Outcome.OK;
      return result;
    } catch (EvalException e) {
      outcome = LarkyEvaluator.failureOf(thread, e);
      failure = LarkyEvaluationScriptException.of(new StarlarkEvalWrapper.Exc.RuntimeEvalException(e, thread));
      throw failure;
    } catch (StarlarkEvalWrapper.Exc.RuntimeEvalException | Starlark.UncheckedEvalException e) {
      if (e.getCause() instanceof EvalException cause) {
        outcome = LarkyEvaluator.failureOf(thread, cause);
      }
      failure = LarkyEvaluationScriptException.of(e);
      throw failure;
    } catch (InterruptedException e) {
      outcome = EvaluationMetadata.Outcome.INTERRUPTED;
      Thread.currentThread().interrupt();
      failure = LarkyEvaluationScriptException.of(e);
      throw failure;
    } finally {
      if (context != null) {
        var metadata = new EvaluationMetadata(outcome, thread.getExecutedSteps(), 0,
            System.nanoTime() - start, EvaluationMetadata.TREE_WALKER, settings, Map.of());
        context.setMetadata(metadata);
        if (failure != null) {
          failure.withMetadataIfAbsent(metadata);
        }
      }
    }
  }

  /** The named method of a Starlark value or global function of an evaluated file. */
  static StarlarkCallable method(Object thiz, String name) throws NoSuchMethodException {
    return method(thiz, name, Mutability.IMMUTABLE, LarkySemantics.LARKY_SEMANTICS);
  }

  private static StarlarkCallable method(
      Object thiz, String name, Mutability mutability, StarlarkSemantics semantics)
      throws NoSuchMethodException {
    if (thiz == null) {
      throw new IllegalArgumentException("no object to call " + name + " on");
    }
    if (attribute(thiz, name, mutability, semantics) instanceof StarlarkCallable callable) {
      return callable;
    }
    throw new NoSuchMethodException(Starlark.type(thiz) + " has no method " + name);
  }

  /** Whether the named method or global function is callable. */
  static boolean hasMethod(Object thiz, String name) {
    return attribute(thiz, name, Mutability.IMMUTABLE, LarkySemantics.LARKY_SEMANTICS)
        instanceof StarlarkCallable;
  }

  @Nullable
  private static Object attribute(
      Object thiz, String name, Mutability mutability, StarlarkSemantics semantics) {
    if (thiz instanceof ParsedStarFile file) {
      return file.getModule().getGlobal(name);
    }
    try {
      return Starlark.getattr(mutability, semantics, thiz, name, Starlark.NONE);
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
      if (Modifier.isAbstract(method.getModifiers()) && !isObjectMethod(method) && !defines.test(method.getName())) {
        return null;
      }
    }
    final InvocationHandler handler = (proxy, method, args) -> {
      if (method.getDeclaringClass() == Object.class || isObjectMethod(method)) {
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

  /**
   * Whether {@code method} is one of Object's public methods; an interface may redeclare them (Comparator declares
   * equals), and the proxy answers them itself.
   */
  private static boolean isObjectMethod(Method method) {
    final Class<?>[] params = method.getParameterTypes();
    return switch (method.getName()) {
      case "equals" -> params.length == 1 && params[0] == Object.class;
      case "hashCode", "toString" -> params.length == 0;
      default -> false;
    };
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
        return i.toBigInteger().intValueExact();
      }
      if (type == long.class || type == Long.class) {
        return i.toBigInteger().longValueExact();
      }
      if (type == BigInteger.class) {
        return i.toBigInteger();
      }
      if (type == short.class || type == Short.class) {
        return i.toBigInteger().shortValueExact();
      }
      if (type == byte.class || type == Byte.class) {
        return i.toBigInteger().byteValueExact();
      }
      if (type == double.class || type == Double.class) {
        return i.toBigInteger().doubleValue();
      }
      if (type == float.class || type == Float.class) {
        return i.toBigInteger().floatValue();
      }
    }
    if (value instanceof StarlarkFloat f) {
      if (type == double.class || type == Double.class) {
        return f.toDouble();
      }
      if (type == float.class || type == Float.class) {
        return (float) f.toDouble();
      }
    }
    if (type.isInstance(value) || (type == boolean.class && value instanceof Boolean)) {
      return value;
    }
    throw new ClassCastException(
        method.getName() + " returned " + Starlark.type(value) + ", not " + type.getName());
  }
}
