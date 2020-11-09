package com.verygood.security.larky.stdtypes.structs;

import com.google.common.collect.ImmutableMap;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.Location;

public class CallableMutableStruct extends MutableStruct implements StarlarkCallable {

  private static final String FUNCTION = "function";
  private static final String ARGS = "args";
  private static final String KWARGS = "kwargs";
  private final StarlarkFunction method;

  CallableMutableStruct(Dict<String, Object> fields) {
    super(fields);
    this.method = (StarlarkFunction) fields.get(FUNCTION);
  }

  public static CallableMutableStruct create(StarlarkThread thread,
                                             StarlarkFunction func,
                                             Tuple<Object> args,
                                             Dict<String, Object> kwargs) {

    return new CallableMutableStruct(
        Dict.copyOf(
            thread.mutability(),
            ImmutableMap.of(FUNCTION, func, ARGS, args, KWARGS, kwargs)));
  }

  @Override
  public Object getValue(String name) {
    return super.getValue(name);
  }

  @Override
  public void setField(String field, Object value) throws EvalException {
    ((Dict<String, Object>) fields).put(field, value, (Location) null);
  }


  @Override
  @SuppressWarnings("unchecked")
  public Object call(StarlarkThread thread, Tuple<Object> args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    Dict<String, Object> _params = (Dict<String, Object>) this.fields.get(KWARGS);
    kwargs.update(Dict.empty(), _params, thread);

    Tuple<Object> joinedArgs = Tuple.concat(
        (Tuple<Object>) this.fields.get(ARGS),
        args);

    return Starlark.call(thread, this.method, joinedArgs, kwargs);
  }


  /**
   * Returns the form this callable value should take in a stack trace.
   */
  @Override
  public String getName() {
    return method.getName();
  }

}
