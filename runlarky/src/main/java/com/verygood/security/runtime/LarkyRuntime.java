package com.verygood.security.runtime;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.parser.ParsedStarFile;

import java.io.Reader;

import javax.script.ScriptContext;
import javax.script.ScriptException;

public class LarkyRuntime extends LarkyScriptEngine {

  private Object extractStarfileOutput(ParsedStarFile starfile, String outputVar) {
    return starfile.getGlobalEnvironmentVariable(outputVar, Object.class);
  }

  public Object executeScript(String script, String outputVar) throws ScriptException {
    return extractStarfileOutput((ParsedStarFile) eval(script), outputVar);
  }

  public Object executeScript(String script, String outputVar, ScriptContext context) throws ScriptException {
    setContext(context);
    return executeScript(script, outputVar);
  }

  public Object executeScript(Reader script, String outputVar, ScriptContext context) throws ScriptException {
    setContext(context);
    return executeScript(script, outputVar);
  }

  public Object executeScript(Reader script, String outputVar) throws ScriptException {
    return extractStarfileOutput((ParsedStarFile) eval(script), outputVar);
  }
}
