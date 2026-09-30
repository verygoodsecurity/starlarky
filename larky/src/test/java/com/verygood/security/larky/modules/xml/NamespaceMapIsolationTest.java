package com.verygood.security.larky.modules.xml;

import static org.junit.Assert.assertEquals;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.junit.Test;

/**
 * ElementTree's namespace registry is per evaluation: what one script registers or changes must not
 * be visible to the next script run in the same JVM.
 */
public class NamespaceMapIsolationTest {

  private static Object eval(String script) throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    engine.setContext(new SimpleScriptContext());
    SimpleBindings bindings = new SimpleBindings();
    engine.eval(script, bindings);
    return bindings.get("OUT");
  }

  private static final String SERIALIZE =
      String.join(
          "\n",
          "load('@stdlib//xml/etree/ElementTree', ET='ElementTree')",
          "def main():",
          "    nsmap = ET._namespace_map()",
          "    return '%s|%s|%s' % (",
          "        nsmap.get('http://www.w3.org/2001/XMLSchema'),",
          "        nsmap.get('urn:evil'),",
          "        ET.tostring(ET.Element('{http://www.w3.org/2001/XMLSchema}x')))",
          "OUT = main()");

  private static final String DEFAULTS =
      "xs|None|<xs:x xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" />";

  @Test
  public void changesDoNotLeakIntoTheNextEvaluation() throws Exception {
    assertEquals(DEFAULTS, eval(SERIALIZE));
    Object changed =
        eval(
            String.join(
                "\n",
                "load('@stdlib//xml/etree/ElementTree', ET='ElementTree')",
                "def main():",
                "    nsmap = ET._namespace_map()",
                "    nsmap['urn:evil'] = 'evil'",
                "    nsmap.update({'http://www.w3.org/2001/XMLSchema': 'HIJACK'})",
                "    ET.register_namespace('ev3', 'urn:evil3')",
                "    return ET.tostring(ET.Element('{http://www.w3.org/2001/XMLSchema}x'))",
                "OUT = main()"));
    // Within the evaluation that made them, the changes apply, as in Python.
    assertEquals("<HIJACK:x xmlns:HIJACK=\"http://www.w3.org/2001/XMLSchema\" />", changed);
    assertEquals(DEFAULTS, eval(SERIALIZE));
  }

  @Test
  public void clearingDoesNotLeakIntoTheNextEvaluation() throws Exception {
    eval(
        String.join(
            "\n",
            "load('@stdlib//xml/etree/ElementTree', ET='ElementTree')",
            "def main():",
            "    ET._namespace_map().clear()",
            "OUT = main()"));
    assertEquals(DEFAULTS, eval(SERIALIZE));
  }

  @Test
  public void registerNamespaceReplacesAnExistingPrefix() throws Exception {
    // Python removes any mapping for the prefix or the URI before adding the new one.
    Object out =
        eval(
            String.join(
                "\n",
                "load('@stdlib//xml/etree/ElementTree', ET='ElementTree')",
                "def main():",
                "    ET.register_namespace('xs', 'urn:other')",
                "    nsmap = ET._namespace_map()",
                "    return '%s|%s' % (nsmap.get('http://www.w3.org/2001/XMLSchema'), nsmap.get('urn:other'))",
                "OUT = main()"));
    assertEquals("None|xs", out);
  }
}
