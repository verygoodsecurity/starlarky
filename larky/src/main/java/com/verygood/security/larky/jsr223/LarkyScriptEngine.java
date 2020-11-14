package com.verygood.security.larky.jsr223;

import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.StreamWriterConsole;
import com.verygood.security.larky.nativelib.LarkyGlobals;
import com.verygood.security.larky.nativelib.PythonBuiltins;
import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.parser.StarFile;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.SyntaxError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

public class LarkyScriptEngine extends AbstractScriptEngine implements Invocable, Compilable, ScriptEngine {
  private static final String DEFAULT_SCRIPT_NAME = "larky.star";
  private ParsedStarFile result;

  /**
   * Returns an uninitialized <code>Bindings</code>.
   *
   * @return A <code>Bindings</code> that can be used to replace the state of this
   * <code>ScriptEngine</code>.
   **/
  @Override
  public Bindings createBindings() {
    //TODO(mahmoud): Should we return this as a StarlarkValue?
    return new SimpleBindings();
  }

  /**
   * Returns a <code>ScriptEngineFactory</code> for the class to which this
   * <code>ScriptEngine</code> belongs.
   *
   * @return The <code>ScriptEngineFactory</code>
   */
  @Override
  public ScriptEngineFactory getFactory() {
    return new LarkyScriptEngineFactory();
  }


  /**
   * Compiles the script (source read from <code>Reader</code>) for later execution.  Functionality
   * is identical to
   * <code>compile(String)</code> other than the way in which the source is
   * passed.
   *
   * @param script The reader from which the script source is obtained.
   * @return An instance of a subclass of <code>CompiledScript</code> to be executed later using one
   * of its <code>eval</code> methods of <code>CompiledScript</code>.
   * @throws ScriptException      if compilation fails.
   * @throws NullPointerException if argument is null.
   */
  @Override
  public CompiledScript compile(Reader script) throws ScriptException {
    return compile(readScript(script));
  }

  /**
     * Compiles the script (source represented as a <code>String</code>) for later execution.
     *
     * @param script The source of the script, represented as a <code>String</code>.
     * @return An instance of a subclass of <code>CompiledScript</code> to be executed later using one
     * of the <code>eval</code> methods of <code>CompiledScript</code>.
     * @throws ScriptException      if compilation fails.
     * @throws NullPointerException if the argument is null.
     */
    @Override
    public CompiledScript compile(String script) throws ScriptException {
      return new LarkyCompiledScript(this, script);
    }

  /**
   * Causes the immediate execution of the script whose source is the String passed as the first
   * argument.  The script may be reparsed or recompiled before execution.  State left in the engine
   * from previous executions, including variable values and compiled procedures may be visible
   * during this execution.
   *
   * @param script  The script to be executed by the script engine.
   * @param context A <code>ScriptContext</code> exposing sets of attributes in different scopes.
   *                The meanings of the scopes <code>ScriptContext.GLOBAL_SCOPE</code>, and
   *                <code>ScriptContext.ENGINE_SCOPE</code> are defined in the specification.
   *                <br><br>
   *                The <code>ENGINE_SCOPE</code> <code>Bindings</code> of the
   *                <code>ScriptContext</code> contains the bindings of scripting variables to
   *                application objects to be used during this script execution.
   * @return The value returned from the execution of the script.
   * @throws ScriptException      if an error occurs in script. ScriptEngines should create and
   *                              throw
   *                              <code>ScriptException</code> wrappers for checked Exceptions
   *                              thrown by underlying scripting implementations.
   * @throws NullPointerException if either argument is null.
   */
  @Override
  public Object eval(String script, ScriptContext context) throws ScriptException {
    //StarlarkFile.parse(ParserInput.fromString(content, ""));
    StarFile larkyScript = InMemMapBackedStarFile.createStarFile(DEFAULT_SCRIPT_NAME, script);

    Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
    Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
    Map<String, Object> mergedBindings = mergeBindings(globalBindings, engineBindings);
    Map<String, Object> globalStarlarkValues = mergedBindings
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> StarlarkUtil.valueToStarlark(entry.getValue()), (a, b) -> b));

    LarkyScript interpreter = new LarkyScript(
        ImmutableSet.of(
            PythonBuiltins.class,
            LarkyGlobals.class
        ),
        LarkyScript.StarlarkMode.STRICT,
        globalStarlarkValues);

    ModuleSupplier.ModuleSet moduleSet = new ModuleSupplier().create();
    Writer writer = context.getWriter();

    try {
      result = interpreter.evaluate(
          larkyScript,
          moduleSet,
          new StreamWriterConsole(writer)
      );
    } catch (IOException e) {
      throw new ScriptException(e);
    }
    setBindingsValue(globalBindings, engineBindings, result.getGlobals());
    return result;
  }

  private void setBindingsValue(Bindings globalBindings, Bindings engineBindings, Map<String, Object> moduleGlobals) {
    for (Map.Entry<String, Object> entry : moduleGlobals.entrySet()) {
      String name = entry.getKey();
      Object value = entry.getValue();
      if (globalBindings != null && globalBindings.containsKey(name)) {
        globalBindings.put(name, value);
      }
      // by default, if defined values are not globals, they belong in engine binding scope
      // to allow for multiple evals() of an instance
      // TODO(mahmoudimus): is this threadsafe?
      else if (engineBindings != null) {
        engineBindings.put(name, value);
      }
    }
  }

  private Map<String, Object> mergeBindings(Bindings... bindingsToMerge) {
    Map<String, Object> variables = new HashMap<>();

    for (Bindings bindings : bindingsToMerge) {
      if (bindings != null) {
        for (Map.Entry<String, Object> globalEntry : bindings.entrySet()) {
          variables.put(globalEntry.getKey(), globalEntry.getValue());
        }
      }
    }

    return variables;
  }

  @Override
  public Object eval(Reader reader, ScriptContext context) throws ScriptException {
    return eval(readScript(reader), context);
  }

  private String readScript(Reader reader) throws ScriptException {
    try {
      StringBuilder s = new StringBuilder();
      BufferedReader bufferedReader = new BufferedReader(reader);
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        s.append(line);
        s.append("\n");
      }
      return s.toString();
    } catch (IOException e) {
      throw new ScriptException(e);
    }
  }

  // invocable interface

  /**
   * Calls a method on a script object compiled during a previous script execution, which is
   * retained in the state of the <code>ScriptEngine</code>.
   *
   * @param name The name of the procedure to be called.
   * @param thiz If the procedure is a member of a class defined in the script and thiz is an
   *             instance of that class returned by a previous execution or invocation, the named
   *             method is called through that instance.
   * @param args Arguments to pass to the procedure.  The rules for converting the arguments to
   *             scripting variables are implementation-specific.
   * @return The value returned by the procedure.  The rules for converting the scripting variable
   * returned by the script method to a Java Object are implementation-specific.
   * @throws ScriptException          if an error occurs during invocation of the method.
   * @throws NoSuchMethodException    if method with given name or matching argument types cannot be
   *                                  found.
   * @throws NullPointerException     if the method name is null.
   * @throws IllegalArgumentException if the specified thiz is null or the specified Object is does
   *                                  not represent a scripting object.
   */
  public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException,
      NoSuchMethodException {
    throw new ScriptException("There are no methods in Starlark: " + name);
    //
    //         try {
    //             interp.setLocals(new PyScriptEngineScope(this, context));
    //             if (!(thiz instanceof PyObject)) {
    //                 thiz = Py.java2py(thiz);
    //             }
    //             PyObject method = ((PyObject) thiz).__findattr__(name);
    //             if (method == null) {
    //                 throw new NoSuchMethodException(name);
    //             }
    //             //return method.__call__(Py.javas2pys(args)).__tojava__(Object.class);
    //             PyObject result;
    //             if(args != null) {
    //                result = method.__call__(Py.javas2pys(args));
    //             } else {
    //                result = method.__call__();
    //             }
    //             return result.__tojava__(Object.class);
    //         } catch (PyException pye) {
    //             throw scriptException(pye);
    //         }
  }

  /**
   * Used to call top-level procedures and functions defined in scripts.
   *
   * @param name of the procedure or function to call
   * @param args Arguments to pass to the procedure or function
   * @return The value returned by the procedure or function
   * @throws ScriptException       if an error occurs during invocation of the method.
   * @throws NoSuchMethodException if method with given name or matching argument types cannot be
   *                               found.
   * @throws NullPointerException  if method name is null.
   */
  public Object invokeFunction(String name, Object... args) throws ScriptException,
      NoSuchMethodException {
    List<String> params = new ArrayList<>();
    for (Object param : args) {
      params.add(String.valueOf(param));
    }

    try (Mutability mu = Mutability.create("InvokeFunction")) {
      StarlarkThread thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
      ParserInput input = ParserInput.fromLines(String.join("\n",
          String.format("output = %1$s(%2$s)",
              name,
              String.join(", ", params)
          )));
      Starlark.execFile(input, LarkyScript.STARLARK_STRICT_FILE_OPTIONS, result.getModule(), thread);
      return result.getModule().getGlobal("output");

//    }
//    interpreter.
//     try {
//         interp.setLocals(new PyScriptEngineScope(this, context));
//         PyObject function = interp.get(name);
//         if (function == null) {
//             throw new NoSuchMethodException(name);
//         }
//         PyObject result;
//         if(args != null) {
//             result = function.__call__(Py.javas2pys(args));
//         } else {
//             result = function.__call__();
//         }
//         return result.__tojava__(Object.class);
//     } catch (PyException pye) {
//         throw scriptException(pye);
//     }
    } catch (SyntaxError.Exception e) {
      throw new NoSuchMethodException(e.getMessage());
    } catch (EvalException e) {
      throw new ScriptException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns an implementation of an interface using functions compiled in the interpreter. The
   * methods of the interface may be implemented using the <code>invokeFunction</code> method.
   *
   * @param <T>   the type of the interface to return
   * @param clasz The <code>Class</code> object of the interface to return.
   * @return An instance of requested interface - null if the requested interface is unavailable, i.
   * e. if compiled functions in the <code>ScriptEngine</code> cannot be found matching the ones in
   * the requested interface.
   * @throws IllegalArgumentException if the specified <code>Class</code> object is null or is not
   *                                  an interface.
   */
  public <T> T getInterface(Class<T> clasz) {
    return null;
    //return getInterface(new PyModule("__jsr223__", interp.getLocals()), clazz);
  }

  /**
   * Returns an implementation of an interface using member functions of a scripting object compiled
   * in the interpreter. The methods of the interface may be implemented using the
   * <code>invokeMethod</code> method.
   *
   * @param <T>   the type of the interface to return
   * @param thiz  The scripting object whose member functions are used to implement the methods of
   *              the interface.
   * @param clasz The <code>Class</code> object of the interface to return.
   * @return An instance of requested interface - null if the requested interface is unavailable, i.
   * e. if compiled methods in the <code>ScriptEngine</code> cannot be found matching the ones in
   * the requested interface.
   * @throws IllegalArgumentException if the specified <code>Class</code> object is null or is not
   *                                  an interface, or if the specified Object is null or does not
   *                                  represent a scripting object.
   */
  public <T> T getInterface(Object thiz, Class<T> clasz) {
    if (thiz == null) {
      throw new IllegalArgumentException("object expected");
    }
    if (clasz == null || !clasz.isInterface()) {
      throw new IllegalArgumentException("interface expected");
    }
    return null;
//     interp.setLocals(new PyScriptEngineScope(this, context));
//     final PyObject thiz = Py.java2py(obj);
//     @SuppressWarnings("unchecked")
//     T proxy = (T) Proxy.newProxyInstance(
//         clazz.getClassLoader(),
//         new Class[] { clazz },
//         new InvocationHandler() {
//             public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//                 try {
//                     interp.setLocals(new PyScriptEngineScope(PyScriptEngine.this, context));
//                     PyObject pyMethod = thiz.__findattr__(method.getName());
//                     if (pyMethod == null)
//                         throw new NoSuchMethodException(method.getName());
//                     PyObject result;
//                     if(args != null) {
//                         result = pyMethod.__call__(Py.javas2pys(args));
//                     } else {
//                         result = pyMethod.__call__();
//                     }
//                     return result.__tojava__(Object.class);
//                 } catch (PyException pye) {
//                     throw scriptException(pye);
//                 }
//             }
//         });
//     return proxy;
  }

}
