package com.verygood.security.larky.modules.types;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

@StarlarkBuiltin(name="type")
public class LarkyType implements StarlarkValue {

  //override built-in type
  @StarlarkMethod(
    name = "type",
    selfCall = true,
    doc =
      "Returns the type name of its argument. This is useful for debugging and "
        + "type-checking. Examples:"
        + "<pre class=\"language-python\">"
        + "type(2) == \"int\"\n"
        + "type([1]) == \"list\"\n"
        + "type(struct(a = 2)) == \"struct\""
        + "</pre>"
        + "This function might change in the future. To write Python-compatible code and "
        + "be future-proof, use it only to compare return values: "
        + "<pre class=\"language-python\">"
        + "if type(x) == type([]):  # if x is a list"
        + "</pre>" +
        "\n" +
        "Otherwise, the type will default to the default Starlark::type() method invocation",
    parameters = {
      @Param(name = "x", doc = "The object to check type of."),
      @Param(name = "bases", defaultValue = "None"),
      @Param(name = "dict", defaultValue = "None")
    },
    extraKeywords = @Param(name = "kwargs", defaultValue = "{}"),
    useStarlarkThread = true
  )
  public Object type(Object object, Object bases, Object dict, Dict<String, Object> kwargs, StarlarkThread thread) throws EvalException {
    if (Starlark.isNullOrNone(bases) && Starlark.isNullOrNone(dict) && kwargs.size() == 0) {
      // There is no 'type' type in Starlark, so we return a string with the type name.
      if (LarkyObject.class.isAssignableFrom(object.getClass())) {
        return ((LarkyObject) object).typeName();
      }
      return Starlark.type(object);
    } else if (kwargs.size() != 0) {
      throw Starlark.errorf("type() takes 1 or 3 arguments");
    }

    return Starlark.type(object); // TODO: fix.

  }
//
//  protected LarkyClass2 pythonClass;
//
//  public LarkyType(LarkyClass2 pythonClass) {
//    this.pythonClass = pythonClass;
//  }
//
//  public final LarkyClass2 getPythonClass() {
//    assert pythonClass != null;
//    return pythonClass;
//  }


}
