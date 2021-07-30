package com.verygood.security.larkyapi;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.parser.ParsedStarFile;
import lombok.extern.slf4j.Slf4j;

import javax.script.ScriptContext;
import javax.script.ScriptException;
import java.io.Reader;

@Slf4j
public class LarkyAPIEngine extends LarkyScriptEngine {

    private Object extractStarfileOutput(ParsedStarFile starfile, String outputVar) {
        return starfile.getGlobalEnvironmentVariable(outputVar, Object.class);
    }

    public Object executeScript(String script, String outputVar)
        throws ScriptException, NullPointerException {
        return extractStarfileOutput((ParsedStarFile) eval(script), outputVar);
    }

    public Object executeScript(String script, String outputVar, ScriptContext context)
        throws ScriptException, NullPointerException {
        return extractStarfileOutput((ParsedStarFile) eval(script, context), outputVar);
    }

    public Object executeScript(Reader script, String outputVar, ScriptContext context)
                throws ScriptException, NullPointerException {
        return extractStarfileOutput((ParsedStarFile) eval(script, context), outputVar);
    }

    public Object executeScript(Reader script, String outputVar)
        throws ScriptException, NullPointerException {
        return extractStarfileOutput((ParsedStarFile) eval(script), outputVar);
    }


}
