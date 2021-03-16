package com.verygood.security.larkyapi;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;

public interface VersionedLarkyEngine extends ScriptEngine {

    Object executeScript(String script, String outputVar, ScriptContext context)
            throws NoSuchMethodException,IllegalAccessException, InvocationTargetException, ScriptException, NullPointerException;
    Object executeScript(String script, String outputVar)
            throws NoSuchMethodException,IllegalAccessException,InvocationTargetException, ScriptException, NullPointerException;

    Object executeScript(Reader script, String outputVar, ScriptContext context)
            throws NoSuchMethodException,IllegalAccessException,InvocationTargetException, ScriptException, NullPointerException;
    Object executeScript(Reader script, String outputVar)
            throws NoSuchMethodException,IllegalAccessException,InvocationTargetException, ScriptException, NullPointerException;

    String getVersion();
}
