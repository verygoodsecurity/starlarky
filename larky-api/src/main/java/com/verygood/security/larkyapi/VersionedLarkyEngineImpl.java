package com.verygood.security.larkyapi;

import lombok.extern.slf4j.Slf4j;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.Bindings;
import javax.script.ScriptEngineFactory;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public class VersionedLarkyEngineImpl implements VersionedLarkyEngine {

  final private Class engineClass;
  final private Class scriptClass;
  final private Class parseClass;
  final private ScriptEngine engineInstanceObj;
  final private String version;

  private static IOException setupException = null;


  // Instantiating the static map
  private static Map<String, URL> larkyJarByVersion;
  static {
    detectVersions();
  }

  private static void detectVersions() {
    larkyJarByVersion = new HashMap<>();
    String larky_lib = System.getProperty("user.home") + "/.larky/lib"; // default dir

    String larky_alt_lib = System.getenv("LARKY_LIB_HOME");
    if ( larky_alt_lib != null ) {
      larky_lib = larky_alt_lib;
    }

    try {
      Stream<Path> paths = Files.walk(Paths.get(larky_lib));
      paths
              // Get jars with format `larky-\d{>=1}.\d{>=1}.\d{>=1}-fat.jar`
              .filter(filePath -> filePath.toString().matches("^.*larky-\\d+.\\d+.\\d+-fat.jar$"))
              .forEach(filePath -> {
                try {
                  String fileName = filePath.toString();
                  URL fileURL = filePath.toUri().toURL();

                  Pattern pattern = Pattern.compile("\\d+.\\d+.\\d+");
                  Matcher matcher = pattern.matcher(fileName);
                  if (matcher.find()) {
                    larkyJarByVersion.put(matcher.group(), fileURL);
                  }
                } catch (Exception e) {
                  log.error("Failed to extract jar file from URL, due to {}",e.getMessage());
                  e.printStackTrace();
                }
              });
    } catch (IOException e) {
      setupException = e;
      log.error("Unable to resolve jar files in path {}, due to {}",larky_lib,e.getMessage());
    }
  }

  public VersionedLarkyEngineImpl(String inputVersion)
          throws IllegalArgumentException, ClassNotFoundException, IllegalAccessException, InstantiationException,
          IOException {

    if (setupException != null) {
      throw setupException;
    }

    if ( !getSupportedVersions().contains(inputVersion) ) {
      throw new IllegalArgumentException("Engine Version not Found");
    }

    this.version = inputVersion;
    URL larkyJarPath = larkyJarByVersion.get(version);
    URLClassLoader childLoader = new URLClassLoader(
            new URL[] {larkyJarPath}
    );

    // equivalent to: import com.verygood.security.larky.jsr223.LarkyCompiledScript;
    this.engineClass = Class.forName(
            "com.verygood.security.larky.jsr223.LarkyScriptEngine",
            true, childLoader);

    // equivalent to: import com.verygood.security.larky.jsr223.LarkyScriptEngine;
    this.scriptClass = Class.forName(
            "com.verygood.security.larky.jsr223.LarkyCompiledScript",
            true, childLoader);

    // equivalent to: import com.verygood.security.larky.parser.ParsedStarFile;
    this.parseClass = Class.forName(
            "com.verygood.security.larky.parser.ParsedStarFile",
            true, childLoader);

    // create engine object
    this.engineInstanceObj = (ScriptEngine) engineClass.newInstance();
  }

  @Override
  public Object executeScript(String script, String outputVar)
          throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
          ScriptException, NullPointerException {

    Method compile = engineClass.getMethod("compile", String.class);
    CompiledScript compiledScript = (CompiledScript) compile.invoke(engineInstanceObj, script);

    Object starFile = compiledScript.eval();

    Method getGlblVar = parseClass.getMethod(
            "getGlobalEnvironmentVariable",
            String.class, Class.class);
    Object result = getGlblVar.invoke(starFile, outputVar, Object.class);

    return result;
  }

  @Override
  public Object executeScript(String script, String outputVar, ScriptContext context)
          throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ScriptException {
    setContext(context);
    return executeScript(script, outputVar);
  }

  @Override
  public Object executeScript(Reader script, String outputVar, ScriptContext context)
          throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ScriptException  {
    setContext(context);
    return executeScript(script, outputVar);
  }

  @Override
  public Object executeScript(Reader script, String outputVar)
          throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ScriptException  {

    Method compile = engineClass.getMethod("compile", Reader.class);
    CompiledScript compiledScript = (CompiledScript) compile.invoke(engineInstanceObj, script);

    Object starFile = compiledScript.eval();

    Method getGlblVar = parseClass.getMethod(
            "getGlobalEnvironmentVariable",
            String.class, Class.class);
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
    return engineInstanceObj.eval(script, context);
  }

  @Override
  public Object eval(Reader reader, ScriptContext context) throws ScriptException {
    return engineInstanceObj.eval(reader, context);
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
    return engineInstanceObj.eval(script, n);
  }

  @Override
  public Object eval(Reader reader, Bindings n) throws ScriptException {
    return engineInstanceObj.eval(reader, n);
  }

  @Override
  public void put(String key, Object value) {
    engineInstanceObj.put(key, value);
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
    engineInstanceObj.setBindings(bindings, scope);
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
  public void setContext(ScriptContext context) {
    engineInstanceObj.setContext(context);
  }

  @Override
  public ScriptEngineFactory getFactory() {
    return null;
  }

}
