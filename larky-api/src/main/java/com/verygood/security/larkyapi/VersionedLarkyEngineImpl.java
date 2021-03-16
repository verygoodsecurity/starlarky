package com.verygood.security.larkyapi;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.annotation.Nonnull;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.Bindings;
import javax.script.ScriptEngineFactory;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionedLarkyEngineImpl implements VersionedLarkyEngine {

  final private Class engineClass;
  final private Class scriptClass;
  final private Class parseClass;
  final private ScriptEngine engineInstanceObj;
  final private String version;


  // Instantiating the static map
  private static Map<String, URL> larkyJarByVersion;
  static {
    detectVersions();
  }

  private static void detectVersions() {
    larkyJarByVersion = new HashMap<>();
    try {
      PathMatchingResourcePatternResolver resolver =
              new PathMatchingResourcePatternResolver(VersionedLarkyEngineImpl.class.getClassLoader());
      // Get jars with format `larky-\d{>=1}.\d{>=1}.\d{>=1}-fat.jar`
      Resource[] resources = resolver.getResources( // uses AntPathMatcher
              "classpath*:larky-{\\d+}.{\\d+}.{\\d+}-fat.jar"
      );
      for (Resource resource: resources){
        String file_name = resource.getFilename();
        URL file_url = resource.getURL();

        Pattern pattern = Pattern.compile("\\d+.\\d+.\\d+");
        Matcher matcher = pattern.matcher(file_name);
        if (matcher.find()) {
          larkyJarByVersion.put(matcher.group(),file_url);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public VersionedLarkyEngineImpl(String inputVersion)
          throws IllegalArgumentException,URISyntaxException,MalformedURLException,
          ClassNotFoundException,IllegalAccessException,InstantiationException {

    if ( !getSupportedVersions().contains(inputVersion) ) {
      throw new IllegalArgumentException("Engine Version not Found");
    }

    this.version = inputVersion;
    URL larkyJarPath = larkyJarByVersion.get(version);
    URLClassLoader childLoader = new URLClassLoader(
            new URL[] {larkyJarPath},
            VersionedLarkyEngineImpl.class.getClassLoader()
    );

    // equivalent to: import com.verygood.security.larky.jsr223.LarkyCompiledScript;
    this.engineClass = Class.forName (
            "com.verygood.security.larky.jsr223.LarkyScriptEngine",
            true, childLoader);

    // equivalent to: import com.verygood.security.larky.jsr223.LarkyScriptEngine;
    this.scriptClass = Class.forName (
            "com.verygood.security.larky.jsr223.LarkyCompiledScript",
            true, childLoader);

    // equivalent to: import com.verygood.security.larky.parser.ParsedStarFile;
    this.parseClass = Class.forName (
            "com.verygood.security.larky.parser.ParsedStarFile",
            true, childLoader);

    // create engine object
    this.engineInstanceObj = (ScriptEngine) engineClass.newInstance();
  }

  @Override
  public Object executeScript(String script,String outputVar)
          throws NoSuchMethodException,IllegalAccessException,InvocationTargetException, ScriptException, NullPointerException {

    Method compile = engineClass.getMethod("compile",String.class);
    CompiledScript compiledScript = (CompiledScript) compile.invoke(engineInstanceObj, script);

    Object starFile = compiledScript.eval();

    Method getGlblVar = parseClass.getMethod (
            "getGlobalEnvironmentVariable",
            String.class,Class.class);
    Object result = getGlblVar.invoke (starFile, outputVar, Object.class);

    return result;
  }

  @Override
  public Object executeScript(String script,String outputVar, ScriptContext context)
          throws NoSuchMethodException,IllegalAccessException,InvocationTargetException,ScriptException {
    setContext(context);
    return executeScript(script, outputVar);
  }

  @Override
  public Object executeScript(Reader script, String outputVar, ScriptContext context)
          throws NoSuchMethodException,IllegalAccessException,InvocationTargetException,ScriptException  {
    setContext(context);
    return executeScript(script, outputVar);
  }

  @Override
  public Object executeScript(Reader script, String outputVar)
          throws NoSuchMethodException,IllegalAccessException,InvocationTargetException,ScriptException  {

    Method compile = engineClass.getMethod("compile",Reader.class);
    CompiledScript compiledScript = (CompiledScript) compile.invoke(engineInstanceObj, script);

    Object starFile = compiledScript.eval();

    Method getGlblVar = parseClass.getMethod(
            "getGlobalEnvironmentVariable",
            String.class,Class.class);
    Object result = getGlblVar.invoke(starFile, outputVar, Object.class);

    return result;
  }

  public static Set<String> getSupportedVersions() {
    return larkyJarByVersion.keySet();
  }

  @Override
  public String getVersion() {
    return version;
  }


  @Override
  public Object eval(String script, ScriptContext context) throws ScriptException {
    return engineInstanceObj.eval(script,context);
  }

  @Override
  public Object eval(Reader reader, ScriptContext context) throws ScriptException {
    return engineInstanceObj.eval(reader,context);
  }

  @Override
  public Object eval(String script) throws ScriptException {
    return engineInstanceObj.eval(script);
  }

  @Override
  public Object eval(Reader reader) throws ScriptException {
    return engineInstanceObj.eval(reader);
  }

  @Override
  public Object eval(String script, Bindings n) throws ScriptException {
    return engineInstanceObj.eval(script,n);
  }

  @Override
  public Object eval(Reader reader, Bindings n) throws ScriptException {
    return engineInstanceObj.eval(reader,n);
  }

  @Override
  public void put(String key, Object value) {
    engineInstanceObj.put(key,value);
  }

  @Override
  public Object get(String key) {
    return engineInstanceObj.get(key);
  }

  @Override
  public Bindings getBindings(int scope) {
    return engineInstanceObj.getBindings(scope);
  }

  @Override
  public void setBindings(Bindings bindings, int scope) {
    engineInstanceObj.setBindings(bindings,scope);
  }

  @Override
  public Bindings createBindings() {
    return engineInstanceObj.createBindings();
  }

  @Override
  public ScriptContext getContext() {
    return engineInstanceObj.getContext();
  }

  @Override
  public void setContext(@Nonnull ScriptContext context) {
    engineInstanceObj.setContext(context);
  }

  @Override
  public ScriptEngineFactory getFactory() {
    return null;
  }

}
