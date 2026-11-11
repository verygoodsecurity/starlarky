package com.verygood.security.larky.jsr223;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.StarlarkUtil;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.script.Invocable;
import javax.script.ScriptException;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.Structure;

/**
 * A script whose top level has run once, so that Java can call its functions many times, from many threads, without
 * running the file again. {@link LarkyCompiledScript#load} makes one.
 *
 * <p>The top level runs with the bindings of the context it was loaded with, and the module is frozen when it ends.
 * Every value the script's top level defines must then be immutable (functions, strings, numbers, frozen lists and
 * dicts, tuples of those), or {@code load} refuses the script: nothing one call does can reach another through the
 * module. Each call runs on a thread of its own.
 *
 * <p>A <em>per-call name</em> is a predeclared name whose value is given with each call instead of once at load
 * ({@link #invoke(String, Map, Object...)}), for example a service a request is handled with. The script sees a
 * stand-in that forwards every field and method to the value of the call running on its thread, so
 * {@code vault.reveal(x)} reaches that call's {@code vault}. Using a per-call name outside a call (at the top level,
 * say) fails. Only fields and methods are forwarded: a per-call name is not itself iterable, callable or comparable.
 *
 * <p>Calls get {@code print}, and the step limit of the load bindings ({@link LarkyEvaluator#STEP_LIMIT}), as the top
 * level did. The load bindings' {@link LarkyEvaluator#EXPIRATION_MS} is a date for the load, not for later calls; a
 * call that should expire names its own among its per-call values.
 *
 * <p>Results are Starlark values ({@code String}, {@code StarlarkInt}, {@code Dict}, ...), as a script's globals are.
 */
public final class LarkyLoadedScript implements Invocable {

  private final Module module;
  private final PerCallValues perCall;
  private final Console console;
  private final LarkyCalls.Limits limits;

  LarkyLoadedScript(Module module, PerCallValues perCall, Console console, LarkyCalls.Limits limits) {
    this.module = module;
    this.perCall = perCall;
    this.console = console;
    this.limits = limits;
  }

  /** The names whose values are given with each call. */
  public Set<String> perCallNames() {
    return perCall.names;
  }

  /** Whether the script's top level defines a function named {@code name}. */
  public boolean defines(String name) {
    return module.getGlobal(name) instanceof StarlarkCallable;
  }

  /**
   * Calls the script's function {@code name} with {@code args}, and these values for its per-call names. A name left
   * out has no value in this call.
   *
   * <p>{@link LarkyEvaluator#MODULE_PROVIDERS} gives this call its {@code @vgs//} module providers
   * ({@link com.verygood.security.larky.modules.vgs.ModuleProviders}); a call without it uses each module's own.
   *
   * @throws IllegalArgumentException if {@code perCallValues} names a name that was not declared per call at load
   *     (other than {@link LarkyEvaluator#STEP_LIMIT} and {@link LarkyEvaluator#EXPIRATION_MS}, which limit this call,
   *     and {@link LarkyEvaluator#MODULE_PROVIDERS})
   * @throws NoSuchMethodException if the script defines no function {@code name}
   */
  public Object invoke(String name, Map<String, ?> perCallValues, Object... args)
      throws ScriptException, NoSuchMethodException {
    final StarlarkCallable function = function(name);
    final ImmutableMap.Builder<String, Object> values = ImmutableMap.builder();
    perCallValues.forEach((key, value) -> {
      if (perCall.names.contains(key)) {
        values.put(key, StarlarkUtil.valueToStarlark(value));
      } else if (!LarkyEvaluator.STEP_LIMIT.equals(key) && !LarkyEvaluator.EXPIRATION_MS.equals(key)
          && !LarkyEvaluator.MODULE_PROVIDERS.equals(key)) {
        throw new IllegalArgumentException(key + " is not a per-call name of this script: " + perCall.names);
      }
    });
    final Map<String, Object> outer = perCall.current.get();
    perCall.current.set(values.buildOrThrow());
    try {
      return LarkyCalls.call(function, args, console, limits.with(perCallValues));
    } finally {
      if (outer == null) {
        perCall.current.remove();
      } else {
        perCall.current.set(outer);
      }
    }
  }

  /** Calls the script's function {@code name}, with no per-call values. */
  @Override
  public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
    return invoke(name, Map.of(), args);
  }

  /** Calls the method {@code name} of {@code thiz}, a value of this script, with no per-call values. */
  @Override
  public Object invokeMethod(Object thiz, String name, Object... args)
      throws ScriptException, NoSuchMethodException {
    return LarkyCalls.call(LarkyCalls.method(thiz, name), args, console, limits);
  }

  @Override
  public <T> T getInterface(Class<T> type) {
    return LarkyCalls.proxy(type, this::defines, this::invokeFunction);
  }

  @Override
  public <T> T getInterface(Object thiz, Class<T> type) {
    return LarkyCalls.proxy(type, name -> LarkyCalls.hasMethod(thiz, name),
        (name, args) -> invokeMethod(thiz, name, args));
  }

  private StarlarkCallable function(String name) throws NoSuchMethodException {
    if (module.getGlobal(name) instanceof StarlarkCallable callable) {
      return callable;
    }
    throw new NoSuchMethodException("the script defines no function " + name);
  }

  /** The values of the per-call names for the call running on each thread. */
  static final class PerCallValues {
    private final ImmutableSet<String> names;
    private final ThreadLocal<Map<String, Object>> current = new ThreadLocal<>();

    PerCallValues(Set<String> names) {
      this.names = ImmutableSet.copyOf(names);
    }

    /** The stand-ins the script's top level is given for the per-call names. */
    Map<String, Object> standIns() {
      final ImmutableMap.Builder<String, Object> standIns = ImmutableMap.builder();
      names.forEach(name -> standIns.put(name, new PerCallValue(name, this)));
      return standIns.buildOrThrow();
    }

    @Nullable
    Object valueOf(String name) {
      final Map<String, Object> values = current.get();
      return values == null ? null : values.get(name);
    }
  }

  /**
   * The value a script sees for a per-call name: forwards fields and methods to the value of the call running on this
   * thread.
   */
  static final class PerCallValue implements Structure {
    private final String name;
    private final PerCallValues values;

    PerCallValue(String name, PerCallValues values) {
      this.name = name;
      this.values = values;
    }

    private Object current() throws EvalException {
      final Object value = values.valueOf(name);
      if (value == null) {
        throw Starlark.errorf("%s has a value only while a function of the script is called with one", name);
      }
      return value;
    }

    @Nullable
    @Override
    public Object getValue(String field) throws EvalException {
      return getValue(LarkySemantics.LARKY_SEMANTICS, field);
    }

    @Nullable
    @Override
    public Object getValue(StarlarkSemantics semantics, String field) throws EvalException {
      final Object value = current();
      try {
        final Object missing = new Object();
        final Object result = Starlark.getattr(Mutability.IMMUTABLE, semantics, value, field, missing);
        return result == missing ? null : result;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw Starlark.errorf("interrupted while reading %s.%s", name, field);
      }
    }

    @Override
    public ImmutableList<String> getFieldNames() {
      final Object value = values.valueOf(name);
      return value == null
          ? ImmutableList.of()
          : ImmutableList.copyOf(Starlark.dir(Mutability.IMMUTABLE, LarkySemantics.LARKY_SEMANTICS, value));
    }

    @Override
    public String getErrorMessageForUnknownField(String field) {
      final Object value = values.valueOf(name);
      return String.format("'%s' value has no field or method '%s'",
          value == null ? name : Starlark.type(value), field);
    }

    @Override
    public boolean isImmutable() {
      // the stand-in itself never changes; the values it forwards to belong to calls
      return true;
    }

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      final Object value = values.valueOf(name);
      if (value == null) {
        printer.append("<").append(name).append(", given per call>");
      } else {
        printer.repr(value, semantics);
      }
    }
  }
}
