package com.verygood.security.larkyapi;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.annotation.Nonnull;
import javax.script.*;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionedLarkyEngineImpl implements VersionedLarkyEngine {

  private Class engineClass;
  private Class scriptClass;
  private Class parseClass;
  private Object engineInstanceObj;
  private String version;


  // Instantiating the static map
  private static Map<String, URL> larkyJarByVersion;
  private static void detectVersions() {
    larkyJarByVersion = new HashMap<>();
    try {
      PathMatchingResourcePatternResolver resolver =
              new PathMatchingResourcePatternResolver(VersionedLarkyEngineImpl.class.getClassLoader());
      Resource[] resources = resolver.getResources("classpath*:larky-?.?.?-*.jar"); // uses AntPathMatcher
      for (Resource resource: resources){
        String file_name = resource.getFilename();
        URL file_url = resource.getURL();

        Pattern pattern = Pattern.compile("\\d{1}.\\d{1}.\\d{1}");
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
    this.engineInstanceObj = engineClass.newInstance();
  }

  @Override
  public Object executeScript(String script,String outputVar)
          throws NoSuchMethodException,IllegalAccessException,InvocationTargetException {

    Method compile = engineClass.getMethod("compile",String.class);
    Object compiledScript = compile.invoke(engineInstanceObj, (Object) script);

    Method eval = scriptClass.getMethod("eval");
    Object starFile = eval.invoke(compiledScript);

    Method getGlblVar = parseClass.getMethod (
            "getGlobalEnvironmentVariable",
            String.class,Class.class);
    Object result = getGlblVar.invoke (starFile, outputVar, Object.class);

    return result;
  }

  @Override
  public Object executeScript(String script,String outputVar, ScriptContext context)
          throws NoSuchMethodException,IllegalAccessException,InvocationTargetException {
    setContext(context);
    return executeScript(script, outputVar);
  }

  @Override
  public Object executeScript(Reader script, String outputVar, ScriptContext context)
          throws NoSuchMethodException,IllegalAccessException,InvocationTargetException  {
    setContext(context);
    return executeScript(script, outputVar);
  }

  @Override
  public Object executeScript(Reader script, String outputVar)
          throws NoSuchMethodException,IllegalAccessException,InvocationTargetException  {

    Method compile = engineClass.getMethod("compile",Reader.class);
    Object compiledScript = compile.invoke(engineInstanceObj, (Object) script);

    Method eval = scriptClass.getMethod("eval");
    Object starFile = eval.invoke(compiledScript);

    Method getGlblVar = parseClass.getMethod(
            "getGlobalEnvironmentVariable",
            String.class,Class.class);
    Object result = getGlblVar.invoke(starFile, outputVar, Object.class);

    return result;
  }

  public static Set<String> getSupportedVersions() {
    detectVersions();
    return larkyJarByVersion.keySet();
  }

  @Override
  public String getVersion() {
    return version;
  }


  @Override
  public Object eval(String script, ScriptContext context) throws ScriptException {
    try {
      return engineClass.getMethod("eval",String.class,ScriptContext.class)
              .invoke(engineInstanceObj, script, context);
    } catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof ScriptException) {
        throw (ScriptException) ite.getCause();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Object eval(Reader reader, ScriptContext context) throws ScriptException {
    try {
      return engineClass.getMethod("eval",Reader.class,ScriptContext.class)
              .invoke(engineInstanceObj, reader, context);
    } catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof ScriptException) {
        throw (ScriptException) ite.getCause();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Object eval(String script) throws ScriptException {
    try {
      return engineClass.getMethod("eval",String.class)
              .invoke(engineInstanceObj, script);
    } catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof ScriptException) {
        throw (ScriptException) ite.getCause();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Object eval(Reader reader) throws ScriptException {
    try {
      return engineClass.getMethod("eval",Reader.class)
              .invoke(engineInstanceObj, reader);
    } catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof ScriptException) {
        throw (ScriptException) ite.getCause();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Object eval(String script, Bindings n) throws ScriptException {
    try {
      return engineClass.getMethod("eval",String.class,Bindings.class)
              .invoke(engineInstanceObj, script, n);
    } catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof ScriptException) {
        throw (ScriptException) ite.getCause();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Object eval(Reader reader, Bindings n) throws ScriptException {
    try {
      return engineClass.getMethod("eval",Reader.class,Bindings.class)
              .invoke(engineInstanceObj, reader, n);
    } catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof ScriptException) {
        throw (ScriptException) ite.getCause();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void put(String key, Object value) {
    try {
      engineClass.getMethod("put",String.class,Object.class)
              .invoke(engineInstanceObj, key, value);
    } catch (Exception e) {
      if (e instanceof InvocationTargetException) {
        if (e.getCause() instanceof NullPointerException) {
          throw (NullPointerException) e.getCause();
        } else if (e.getCause() instanceof IllegalArgumentException) {
          throw (IllegalArgumentException) e.getCause();
        }
      }
      e.printStackTrace();
    }
  }

  @Override
  public Object get(String key) {
    try {
      return engineClass.getMethod("get",String.class)
              .invoke(engineInstanceObj, key);
    } catch (Exception e) {
      if (e instanceof InvocationTargetException) {
        if (e.getCause() instanceof NullPointerException) {
          throw (NullPointerException) e.getCause();
        } else if (e.getCause() instanceof IllegalArgumentException) {
          throw (IllegalArgumentException) e.getCause();
        }
      }
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Bindings getBindings(int scope) {
    try {
      return (Bindings) engineClass.getMethod("getBindings", int.class)
              .invoke(engineInstanceObj, scope);
    } catch (Exception e) {
      if (e instanceof InvocationTargetException) {
        if (e.getCause() instanceof IllegalArgumentException) {
          throw (IllegalArgumentException) e.getCause();
        }
      }
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void setBindings(Bindings bindings, int scope) {
    try {
      engineClass.getMethod("setBindings", Bindings.class, int.class)
              .invoke(engineInstanceObj, bindings, scope);
    } catch (Exception e) {
      if (e instanceof InvocationTargetException) {
        if (e.getCause() instanceof NullPointerException) {
          throw (NullPointerException) e.getCause();
        } else if (e.getCause() instanceof IllegalArgumentException) {
          throw (IllegalArgumentException) e.getCause();
        }
      }
      e.printStackTrace();
    }
  }

  @Override
  public Bindings createBindings() {
    try {
      return (Bindings) engineClass.getMethod("createBindings")
              .invoke(engineInstanceObj);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public ScriptContext getContext() {
    try {
      return (ScriptContext) engineClass.getMethod("getContext")
              .invoke(engineInstanceObj);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void setContext(@Nonnull ScriptContext context) {
    try {
      engineClass.getMethod("setContext", ScriptContext.class)
              .invoke(engineInstanceObj, context);
    } catch (Exception e) {
      if (e instanceof InvocationTargetException) {
        if (e.getCause() instanceof NullPointerException) {
          throw (NullPointerException) e.getCause();
        }
      }
      e.printStackTrace();
    }
  }

  @Override
  public ScriptEngineFactory getFactory() {
    return null;
  }

}
