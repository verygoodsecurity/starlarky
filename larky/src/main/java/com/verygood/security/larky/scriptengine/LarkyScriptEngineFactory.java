package com.verygood.security.larky.scriptengine;

import java.util.Arrays;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

public class LarkyScriptEngineFactory implements ScriptEngineFactory {
  /**
   * Returns the full  name of the <code>ScriptEngine</code>.  For instance an implementation based
   * on the Mozilla Rhino Javascript engine might return <i>Rhino Mozilla Javascript Engine</i>.
   *
   * @return The name of the engine implementation.
   */
  @Override
  public String getEngineName() {
    return "Larky ScriptEngine";
  }

  /**
   * Returns the version of the <code>ScriptEngine</code>.
   *
   * @return The <code>ScriptEngine</code> implementation version.
   */
  @Override
  public String getEngineVersion() {
    return "1.0";
  }

  /**
   * Returns an immutable list of filename extensions, which generally identify scripts written in
   * the language supported by this <code>ScriptEngine</code>. The array is used by the
   * <code>ScriptEngineManager</code> to implement its
   * <code>getEngineByExtension</code> method.
   *
   * @return The list of extensions.
   */
  @Override
  public List<String> getExtensions() {
    return Arrays.asList("star", "star.py");
  }

  /**
   * Returns an immutable list of mimetypes, associated with scripts that can be executed by the
   * engine.  The list is used by the
   * <code>ScriptEngineManager</code> class to implement its
   * <code>getEngineByMimetype</code> method.
   *
   * @return The list of mime types.
   */
  @Override
  public List<String> getMimeTypes() {
    return Arrays.asList(
        "text/x-starlarky-source",
        "text/x-larky-source"
    );
  }

  /**
   * Returns an immutable list of  short names for the <code>ScriptEngine</code>, which may be used
   * to identify the <code>ScriptEngine</code> by the <code>ScriptEngineManager</code>. For
   * instance, an implementation based on the Mozilla Rhino Javascript engine might return list
   * containing {&quot;javascript&quot;, &quot;rhino&quot;}.
   *
   * @return an immutable list of short names
   */
  @Override
  public List<String> getNames() {
    return Arrays.asList("Starlarky", "Larky", "starlarky", "larky", "vgs-larky");
  }

  /**
   * Returns the name of the scripting language supported by this
   * <code>ScriptEngine</code>.
   *
   * @return The name of the supported language.
   */
  @Override
  public String getLanguageName() {
    return "Larky";
  }

  /**
   * Returns the version of the scripting language supported by this
   * <code>ScriptEngine</code>.
   *
   * @return The version of the supported language.
   */
  @Override
  public String getLanguageVersion() {
    return "1.0";
  }

  /**
   * Returns the value of an attribute whose meaning may be implementation-specific. Keys for which
   * the value is defined in all implementations are:
   * <ul>
   * <li>ScriptEngine.ENGINE</li>
   * <li>ScriptEngine.ENGINE_VERSION</li>
   * <li>ScriptEngine.LANGUAGE</li>
   * <li>ScriptEngine.LANGUAGE_VERSION</li>
   * <li>ScriptEngine.NAME</li>
   * </ul>
   * <p>
   * The values for these keys are the Strings returned by <code>getEngineName</code>,
   * <code>getEngineVersion</code>, <code>getLanguageName</code>,
   * <code>getLanguageVersion</code> for the first four keys respectively. For NAME, one of the Strings
   * returned by <code>getNames</code> is returned.<br><br>
   * A reserved key, <code><b>THREADING</b></code>, whose value describes the behavior of the engine
   * with respect to concurrent execution of scripts and maintenance of state is also defined.
   * These values for the <code><b>THREADING</b></code> key are:<br><br>
   * <ul>
   * <li><code>null</code> - The engine implementation is not thread safe, and cannot
   * be used to execute scripts concurrently on multiple threads.
   * <li><code>&quot;MULTITHREADED&quot;</code> - The engine implementation is internally
   * thread-safe and scripts may execute concurrently although effects of script execution
   * on one thread may be visible to scripts on other threads.
   * <li><code>&quot;THREAD-ISOLATED&quot;</code> - The implementation satisfies the requirements
   * of &quot;MULTITHREADED&quot;, and also, the engine maintains independent values
   * for symbols in scripts executing on different threads.
   * <li><code>&quot;STATELESS&quot;</code> - The implementation satisfies the requirements of
   * <code>&quot;THREAD-ISOLATED&quot;</code>.  In addition, script executions do not alter the
   * mappings in the <code>Bindings</code> which is the engine scope of the
   * <code>ScriptEngine</code>.  In particular, the keys in the <code>Bindings</code>
   * and their associated values are the same before and after the execution of the script.
   * </ul>
   * <br><br>
   * Implementations may define implementation-specific keys.
   *
   * @param key The name of the parameter
   * @return The value for the given parameter. Returns <code>null</code> if no value is assigned to
   * the key.
   * @throws NullPointerException if the key is null.
   */
  @Override
  public Object getParameter(String key) {
    switch (key) {
      case ScriptEngine.ENGINE:
        return getEngineName();
      case ScriptEngine.ENGINE_VERSION:
        return getEngineVersion();
      case ScriptEngine.NAME:
        return getEngineName();
      case ScriptEngine.LANGUAGE:
        return getLanguageName();
      case ScriptEngine.LANGUAGE_VERSION:
        return getLanguageVersion();
      default:
        return null;
    }
  }

  /**
   * Returns a String which can be used to invoke a method of a  Java object using the syntax of the
   * supported scripting language.  For instance, an implementation for a Javascript engine might
   * be;
   *
   * <pre>{@code
   * public String getMethodCallSyntax(String obj,
   *                                   String m, String... args) {
   *      String ret = obj;
   *      ret += "." + m + "(";
   *      for (int i = 0; i < args.length; i++) {
   *          ret += args[i];
   *          if (i < args.length - 1) {
   *              ret += ",";
   *          }
   *      }
   *      ret += ")";
   *      return ret;
   * }
   * } </pre>
   *
   * @param obj  The name representing the object whose method is to be invoked. The name is the one
   *             used to create bindings using the <code>put</code> method of
   *             <code>ScriptEngine</code>, the <code>put</code> method of an
   *             <code>ENGINE_SCOPE</code>
   *             <code>Bindings</code>,or the <code>setAttribute</code> method
   *             of <code>ScriptContext</code>.  The identifier used in scripts may be a decorated
   *             form of the specified one.
   * @param m    The name of the method to invoke.
   * @param args names of the arguments in the method call.
   * @return The String used to invoke the method in the syntax of the scripting language.
   * @throws NullPointerException if obj or m or args or any of the elements of args is null.
   */
  @Override
  public String getMethodCallSyntax(String obj, String m, String... args) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(String.format("%s.%s(", obj, m));
    int i = args.length;
    for (String arg : args) {
        buffer.append(arg);
        if (i-- > 0) {
            buffer.append(", ");
        }
    }
    buffer.append("*args, **kwargs)");
    return buffer.toString();
  }

  /**
   * Returns a String that can be used as a statement to display the specified String  using the
   * syntax of the supported scripting language.  For instance, the implementation for a Perl engine
   * might be;
   *
   * <pre><code>
   * public String getOutputStatement(String toDisplay) {
   *      return "print(" + toDisplay + ")";
   * }
   * </code></pre>
   *
   * @param toDisplay The String to be displayed by the returned statement.
   * @return The string used to display the String in the syntax of the scripting language.
   */
  @Override
  public String getOutputStatement(String toDisplay) {
    return "print(" +
        toDisplay +
        ")";
  }

  /**
   * Returns a valid scripting language executable program with given statements. For instance an
   * implementation for a PHP engine might be:
   *
   * <pre>{@code
   * public String getProgram(String... statements) {
   *      String retval = "<?\n";
   *      int len = statements.length;
   *      for (int i = 0; i < len; i++) {
   *          retval += statements[i] + ";\n";
   *      }
   *      return retval += "?>";
   * }
   * }</pre>
   *
   * @param statements The statements to be executed.  May be return values of calls to the
   *                   <code>getMethodCallSyntax</code> and <code>getOutputStatement</code>
   *                   methods.
   * @return The Program
   * @throws NullPointerException if the <code>statements</code> array or any of its elements is
   *                              null
   */
  @Override
  public String getProgram(String... statements) {
    StringBuilder s = new StringBuilder();
    for(String statement : statements) {
        s.append(statement);
        s.append("\n");
    }
    return s.toString();
  }

  /**
   * Returns an instance of the <code>ScriptEngine</code> associated with this
   * <code>ScriptEngineFactory</code>. A new ScriptEngine is generally
   * returned, but implementations may pool, share or reuse engines.
   *
   * @return A new <code>ScriptEngine</code> instance.
   */
  @Override
  public ScriptEngine getScriptEngine() {
    return new LarkyScriptEngine();
  }
}
