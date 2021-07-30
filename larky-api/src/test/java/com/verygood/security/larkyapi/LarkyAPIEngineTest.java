package com.verygood.security.larkyapi;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;


public class LarkyAPIEngineTest {

  private LarkyAPIEngine engine;

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    synchronized (this) {
      // General
      engine = new LarkyAPIEngine();
    }
  }

  @After
  public void tearDown() {
    engine = null;
  }

  @Test
  public void testEngine_ok() {
    // Assert
    assertNotNull(engine);
  }

  @Test
  public void testEngine_execScript_V0_2_1_ok() throws Exception {
    // Expect
    String expResult = "1243\\*\\&\\[\\]_dsfAd";

    // Setup
    String regxScript = String.join("\n",
            "load(\"@stdlib/re\", \"re\")",
            "def re_escape():",
            "    return re.escape(r\"1243*&[]_dsfAd\")",
            "output = re_escape()"
    );
    String v_2_1 = "0.2.1";

    // Execute
    Object output = engine.executeScript(regxScript, "output");

    // Assert
    assertEquals(expResult,output.toString());
  }


  @Test
  public void testEngine_setBindings_ok() throws Exception {
    // Expect
    Object expResult = "message: Hello From LarkyEngine!";

    // Setup
    String msgScript =
            "def message():\n" +
                    "    return \"message: {}\".format(msg)\n" +
                    "\n" +
                    "output = message()\n";
    Bindings bindings = new SimpleBindings();
    bindings.put("msg", "Hello From LarkyEngine!");

    // Execute
    engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    Object output = engine.executeScript(msgScript, "output");

    // Assert
    assertEquals(output.toString(), expResult);
  }

  @Test
  public void testEngine_context_ok() throws Exception {
    // Expect
    String expResult = "context_keys: [\"value2\", \"value1\"] context_values: [\"key2\", \"key1\"]\n";

    // Setup
    String ctxScript =
            "def process(input, ctx):\n" +
                    "    print( " +
                    "'{}: {} context_values: {}'" +
                    ".format(str(input),str(ctx.keys()), str(ctx.values()))" +
                    ")\n" +
                    "process(msg,my_ctx)";


    String larkyMsg = "context_keys";
    ConcurrentMap<String, Object> larkyCTX = new ConcurrentHashMap<>();
    larkyCTX.put("value1", "key1");
    larkyCTX.put("value2", "key2");

    // Execute
    SimpleScriptContext context = new SimpleScriptContext();
    context.setAttribute("my_ctx", larkyCTX, ScriptContext.ENGINE_SCOPE);
    context.setAttribute("msg", larkyMsg, ScriptContext.ENGINE_SCOPE);
    StringWriter sw = new StringWriter();
    context.setWriter(new PrintWriter(sw));

    engine.executeScript(ctxScript, "output", context);

    // Assert
    assertEquals(expResult, sw.toString());
  }


  @Test
  public void testEngine_readerBasic_ok() throws Exception {
    // Expect
    String expResult = "Hello World";

    // Setup
    String helloScript =
            "def hello():\n" +
            "    return \"Hello World\"\n" +
            "output=hello()";

    Reader helloReader = new StringReader(helloScript);

    // Execute
    Object output = engine.executeScript(helloReader, "output");

    // Assert
    assertEquals(expResult, output.toString());
  }

  @Test
  public void testEngine_readerContext_ok() throws Exception {
    // Expect
    String expResult = "Hello World, I am Larky!";

    // Setup
    String helloContextScript =
            "def hello(msg):\n" +
                    "    return \"Hello World, {}!\".format(msg)\n" +
                    "output=hello(my_msg)";

    Reader helloContextReader = new StringReader(helloContextScript);
    Bindings bindings = new SimpleBindings();
    bindings.put("my_msg", "I am Larky");
    SimpleScriptContext context = new SimpleScriptContext();
    context.setBindings(bindings,ScriptContext.ENGINE_SCOPE);

    // Execute
    Object output = engine.executeScript(helloContextReader, "output", context);

    // Assert
    assertEquals(expResult, output.toString());
  }

  @Test
  public void testEngine_createBindings_ok() {
    // Execute
    Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
    Bindings result = engine.createBindings();

    // Assert
    assertNotNull(result);
    assertNotSame(bindings, result);
  }

  @Test
  public void testEngine_getContext_ok() {
    // Execute
    ScriptContext context = engine.getContext();

    // Assert
    assertNotNull(context);
  }

  @Test
  public void testEngine_getBindings_ok() {
    // Execute
    Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

    // Assert
    assertNotNull(bindings);
    assertTrue(bindings instanceof SimpleBindings);
    assertEquals(0, bindings.size());
  }

  @Test
  public void testEngine_put_exceptionNull() {
    // Expect
    exceptionRule.expect(NullPointerException.class);
    exceptionRule.expectMessage("key can not be null");

    // Setup
    String key = null;
    Object value = null;

    // Execute
    engine.put(key,value);

  }

  @Test
  public void testEngine_put_exceptionIllegalArgument() {
    // Expect
    exceptionRule.expect(IllegalArgumentException.class);
    exceptionRule.expectMessage("key can not be empty");

    // Setup
    String key = "";
    Object value = null;

    // Execute
    engine.put(key,value);

  }

  @Test
  public void testEngine_get_ok() {

    // Expect
    String expResult1 = "result1";
    String expResult2 = "result2_2";
    String expResult3 = "result3_3";

    // Setup
    engine.put("value1", "result1");
    engine.put("value2", new ArrayList<String>() {{
      add("result2_1");
      add("result2_2");
      add("result2_3");
    }});
    engine.put("value3",  new HashMap<String,String>() {{
      put("key3_1","result3_1");
      put("key3_2","result3_2");
      put("key3_3","result3_3");
    }});

    // Execute
    String result1 = (String) engine.get("value1");
    String result2 = ((ArrayList<String>) engine.get("value2")).get(1);
    String result3 = ((HashMap<String,String>) engine.get("value3")).get("key3_3");

    // Assert
    assertEquals(expResult1, result1);
    assertEquals(expResult2, result2);
    assertEquals(expResult3, result3);
  }

}
