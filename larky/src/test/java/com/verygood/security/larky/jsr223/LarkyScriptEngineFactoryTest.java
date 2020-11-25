package com.verygood.security.larky.jsr223;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import javax.script.ScriptEngine;

public class LarkyScriptEngineFactoryTest {

  private LarkyScriptEngineFactory factory;

  @Before
  public void setUp() {
    factory = new LarkyScriptEngineFactory();

  }

  @Test
  public void getEngineName() {
    Assert.assertEquals(factory.getEngineName(), "Larky ScriptEngine");
  }

  @Test
  public void getEngineVersion() {
    Assert.assertEquals(factory.getEngineVersion(), "1.0");
  }

  @Test
  public void getExtensions() {
    Assert.assertEquals(factory.getExtensions(), Arrays.asList("star", "star.py"));
  }

  @Test
  public void getMimeTypes() {
    Assert.assertEquals(
        factory.getMimeTypes(),
        Arrays.asList("text/x-starlarky-source", "text/x-larky-source"));
  }

  @Test
  public void getNames() {
    Assert.assertEquals(
        factory.getNames(),
        Arrays.asList("Starlarky", "Larky", "starlarky", "larky"));
  }

  @Test
  public void getLanguageName() {
    Assert.assertEquals(factory.getLanguageName(), "Larky");
  }

  @Test
  public void getLanguageVersion() {
    Assert.assertEquals(factory.getLanguageVersion(), "1.0");
  }

  @Test
  public void getParameter() {
    Assert.assertEquals(factory.getParameter(ScriptEngine.ENGINE), factory.getEngineName());
    Assert.assertEquals(factory.getParameter(ScriptEngine.ENGINE_VERSION), factory.getEngineVersion());
    Assert.assertEquals(factory.getParameter(ScriptEngine.LANGUAGE), factory.getLanguageName());
    Assert.assertEquals(factory.getParameter(ScriptEngine.LANGUAGE_VERSION), factory.getLanguageVersion());
    Assert.assertEquals(factory.getParameter(ScriptEngine.NAME), "Larky ScriptEngine");
    Assert.assertNull(factory.getParameter("unknown"));
  }

  @Test
  public void getMethodCallSyntax() {
    Assert.assertEquals(
        factory.getMethodCallSyntax("obj", "method"),
        "obj.method(*args, **kwargs)");
    Assert.assertEquals(
        factory.getMethodCallSyntax("obj", "method", "first"),
        "obj.method(first, *args, **kwargs)");
    Assert.assertEquals(factory.getMethodCallSyntax(
        "obj", "method", "first", "second"),
        "obj.method(first, second, *args, **kwargs)");
  }

  @Test
  public void getOutputStatement() {
    Assert.assertEquals(factory.getOutputStatement("arg"), "print(arg)");
  }

  @Test
  public void getProgram() {
    Assert.assertEquals(
        factory.getProgram(),
        "");
    Assert.assertEquals(
        factory.getProgram("first"),
        "first\n");
    Assert.assertEquals(
        factory.getProgram("first = 1", "second = 2"),
        "first = 1\nsecond = 2\n");
  }

  @Test
  public void getScriptEngine() {
    Assert.assertTrue(factory.getScriptEngine() instanceof LarkyScriptEngine);

  }
}