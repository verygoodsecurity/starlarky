package com.verygood.security.larky.core;

import com.google.common.collect.ImmutableMap;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

public class CallableMutableStruct extends MutableStruct implements StarlarkCallable {

  private static final String FUNCTION = "function";
  private static final String PARAMETERS = "parameters";
  private final StarlarkFunction method;

  CallableMutableStruct(Dict<String, Object> fields) {
    super(fields);
    this.method = (StarlarkFunction) fields.get("function");
  }

  public static CallableMutableStruct create(StarlarkThread thread,
                                             StarlarkFunction func,
                                             Dict<String, Object> parameters) {

    return new CallableMutableStruct(
        Dict.copyOf(
            thread.mutability(),
            ImmutableMap.of(FUNCTION, func, PARAMETERS, parameters)));
  }

  @Override
  public Object getValue(String name) {
    System.out.println("====> here? " + name);
    return super.getValue(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object call(StarlarkThread thread, Tuple<Object> args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    Dict<String, Object> _params = (Dict<String, Object>) this.fields.get(PARAMETERS);
    kwargs.update(Dict.empty(), _params, thread);
    return Starlark.call(thread, this.method, args, kwargs);
  }


  /**
   * Returns the form this callable value should take in a stack trace.
   */
  @Override
  public String getName() {
    return CallableMutableStruct.class.getName();
  }

}
