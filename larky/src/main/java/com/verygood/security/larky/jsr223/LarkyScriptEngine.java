package com.verygood.security.larky.jsr223;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Objects;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import jakarta.annotation.Nonnull;

public class LarkyScriptEngine implements Compilable, ScriptEngine {

  private ScriptContext context = new SimpleScriptContext();


  /**
   * Compiles the script (source represented as a <code>String</code>) for later execution.
   *
   * @param script The source of the script, represented as a <code>String</code>.
   * @return An instance of a subclass of <code>CompiledScript</code> to be executed later using one
   * of the <code>eval</code> methods of <code>CompiledScript</code>.
   * @throws NullPointerException if the argument is null.
   */
  @Override
  public CompiledScript compile(String script) {
    Reader scriptReader = getScriptReader(script);
    context.setReader(scriptReader);
    return new LarkyCompiledScript(this);
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
   *                              thrown by underlying scripting
   *                              implementations.
   * @throws NullPointerException if either argument is null.
   */
  @Override
  public Object eval(String script, ScriptContext context) throws ScriptException {
    return eval(script, context.getBindings(ScriptContext.ENGINE_SCOPE));
  }

  /**
   * Same as <code>eval(String, ScriptContext)</code> where the source of the script is read from a
   * <code>Reader</code>.
   *
   * @param reader  The source of the script to be executed by the script engine.
   * @param context The <code>ScriptContext</code> passed to the script engine.
   * @return The value returned from the execution of the script.
   * @throws ScriptException      if an error occurs in script. ScriptEngines should create and
   *                              throw
   *                              <code>ScriptException</code> wrappers for checked Exceptions
   *                              thrown by underlying scripting
   *                              implementations.
   * @throws NullPointerException if either argument is null.
   */
  @Override
  public Object eval(Reader reader, ScriptContext context) throws ScriptException {
    return eval(readScript(reader), context);
  }

  /**
   * Executes the specified script.  The default <code>ScriptContext</code> for the
   * <code>ScriptEngine</code> is used.
   *
   * @param script The script language source to be executed.
   * @return The value returned from the execution of the script.
   * @throws ScriptException      if an error occurs in script. ScriptEngines should create and
   *                              throw
   *                              <code>ScriptException</code> wrappers for checked Exceptions
   *                              thrown by underlying scripting
   *                              implementations.
   * @throws NullPointerException if the argument is null.
   */
  @Override
  public Object eval(String script) throws ScriptException {
    return eval(script, getContext());
  }

  /**
   * Same as <code>eval(String)</code> except that the source of the script is provided as a
   * <code>Reader</code>
   *
   * @param reader The source of the script.
   * @return The value returned by the script.
   * @throws ScriptException      if an error occurs in script. ScriptEngines should create and
   *                              throw
   *                              <code>ScriptException</code> wrappers for checked Exceptions
   *                              thrown by underlying scripting
   *                              implementations.
   * @throws NullPointerException if the argument is null.
   */
  @Override
  public Object eval(Reader reader) throws ScriptException {
    return eval(readScript(reader));
  }

  /**
   * Executes the script using the <code>Bindings</code> argument as the <code>ENGINE_SCOPE</code>
   * <code>Bindings</code> of the <code>ScriptEngine</code> during the script execution.  The
   * <code>Reader</code>, <code>Writer</code> and non-<code>ENGINE_SCOPE</code>
   * <code>Bindings</code> of the
   * default <code>ScriptContext</code> are used. The <code>ENGINE_SCOPE</code>
   * <code>Bindings</code> of the <code>ScriptEngine</code> is not changed, and its
   * mappings are unaltered by the script execution.
   *
   * @param script The source for the script.
   * @param n      The <code>Bindings</code> of attributes to be used for script execution.
   * @return The value returned by the script.
   * @throws ScriptException      if an error occurs in script. ScriptEngines should create and
   *                              throw
   *                              <code>ScriptException</code> wrappers for checked Exceptions
   *                              thrown by underlying scripting
   *                              implementations.
   * @throws NullPointerException if either argument is null.
   */
  @Override
  public Object eval(String script, Bindings n) throws ScriptException {
    CompiledScript compile = compile(script);
    return compile.eval(n);
  }

  /**
   * Same as <code>eval(String, Bindings)</code> except that the source of the script is provided as
   * a <code>Reader</code>.
   *
   * @param reader The source of the script.
   * @param n      The <code>Bindings</code> of attributes.
   * @return The value returned by the script.
   * @throws ScriptException      if an error occurs in script. ScriptEngines should create and
   *                              throw
   *                              <code>ScriptException</code> wrappers for checked Exceptions
   *                              thrown by underlying scripting
   *                              implementations.
   * @throws NullPointerException if either argument is null.
   */
  @Override
  public Object eval(Reader reader, Bindings n) throws ScriptException {
    return eval(readScript(reader), n);
  }

  /**
   * Sets a key/value pair in the state of the ScriptEngine that may either create a Java Language
   * Binding to be used in the execution of scripts or be used in some other way, depending on
   * whether the key is reserved.  Must have the same effect as
   * <code>getBindings(ScriptContext.ENGINE_SCOPE).put</code>.
   *
   * @param key   The name of named value to add
   * @param value The value of named value to add.
   * @throws NullPointerException     if key is null.
   * @throws IllegalArgumentException if key is empty.
   */
  @Override
  public void put(String key, Object value) {
    getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
  }

  /**
   * Retrieves a value set in the state of this engine.  The value might be one which was set using
   * <code>setValue</code> or some other value in the state of the <code>ScriptEngine</code>,
   * depending on the implementation.  Must have the same effect as <code>getBindings(ScriptContext.ENGINE_SCOPE).get</code>
   *
   * @param key The key whose value is to be returned
   * @return the value for the given key
   * @throws NullPointerException     if key is null.
   * @throws IllegalArgumentException if key is empty.
   */
  @Override
  public Object get(String key) {
    return getBindings(ScriptContext.ENGINE_SCOPE).get(key);
  }

  /**
   * Returns a scope of named values.  The possible scopes are:
   * <br><br>
   * <ul>
   * <li><code>ScriptContext.GLOBAL_SCOPE</code> - The set of named values representing global
   * scope. If this <code>ScriptEngine</code> is created by a <code>ScriptEngineManager</code>,
   * then the manager sets global scope bindings. This may be <code>null</code> if no global
   * scope is associated with this <code>ScriptEngine</code></li>
   * <li><code>ScriptContext.ENGINE_SCOPE</code> - The set of named values representing the state of
   * this <code>ScriptEngine</code>.  The values are generally visible in scripts using
   * the associated keys as variable names.</li>
   * <li>Any other value of scope defined in the default <code>ScriptContext</code> of the <code>ScriptEngine</code>.
   * </li>
   * </ul>
   * <br><br>
   * The <code>Bindings</code> instances that are returned must be identical to those returned by the
   * <code>getBindings</code> method of <code>ScriptContext</code> called with corresponding arguments on
   * the default <code>ScriptContext</code> of the <code>ScriptEngine</code>.
   *
   * @param scope Either <code>ScriptContext.ENGINE_SCOPE</code> or <code>ScriptContext.GLOBAL_SCOPE</code>
   *              which specifies the <code>Bindings</code> to return.  Implementations of
   *              <code>ScriptContext</code> may define additional scopes.  If the default
   *              <code>ScriptContext</code> of the <code>ScriptEngine</code> defines additional
   *              scopes, any of them can be passed to get the corresponding <code>Bindings</code>.
   * @return The <code>Bindings</code> with the specified scope.
   * @throws IllegalArgumentException if specified scope is invalid
   */
  @Override
  public Bindings getBindings(int scope) {
    return getContext().getBindings(scope);
  }

  /**
   * Sets a scope of named values to be used by scripts.  The possible scopes are:
   * <br><br>
   * <ul>
   * <li><code>ScriptContext.ENGINE_SCOPE</code> - The specified <code>Bindings</code> replaces the
   * engine scope of the <code>ScriptEngine</code>.
   * </li>
   * <li><code>ScriptContext.GLOBAL_SCOPE</code> - The specified <code>Bindings</code> must be visible
   * as the <code>GLOBAL_SCOPE</code>.
   * </li>
   * <li>Any other value of scope defined in the default <code>ScriptContext</code> of the <code>ScriptEngine</code>.
   * </li>
   * </ul>
   * <br><br>
   * The method must have the same effect as calling the <code>setBindings</code> method of
   * <code>ScriptContext</code> with the corresponding value of <code>scope</code> on the default
   * <code>ScriptContext</code> of the <code>ScriptEngine</code>.
   *
   * @param bindings The <code>Bindings</code> for the specified scope.
   * @param scope    The specified scope.  Either <code>ScriptContext.ENGINE_SCOPE</code>,
   *                 <code>ScriptContext.GLOBAL_SCOPE</code>, or any other valid value of scope.
   * @throws IllegalArgumentException if the scope is invalid
   * @throws NullPointerException     if the bindings is null and the scope is
   *                                  <code>ScriptContext.ENGINE_SCOPE</code>
   */
  @Override
  public void setBindings(Bindings bindings, int scope) {
    getContext().setBindings(bindings, scope);
  }

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
   * Returns the default <code>ScriptContext</code> of the <code>ScriptEngine</code> whose Bindings,
   * Reader and Writers are used for script executions when no <code>ScriptContext</code> is
   * specified.
   *
   * @return The default <code>ScriptContext</code> of the <code>ScriptEngine</code>.
   */
  @Override
  public ScriptContext getContext() {
    return context;
  }

  /**
   * Sets the default <code>ScriptContext</code> of the <code>ScriptEngine</code> whose Bindings,
   * Reader and Writers are used for script executions when no <code>ScriptContext</code> is
   * specified.
   *
   * @param context A <code>ScriptContext</code> that will replace the default
   *                <code>ScriptContext</code> in the <code>ScriptEngine</code>.
   * @throws NullPointerException if context is null.
   */
  @Override
  public void setContext(@Nonnull ScriptContext context) {
    Objects.requireNonNull(context);
    this.context = context;
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

  private Reader getScriptReader(String script) {
    return new StringReader(script);
  }
}
