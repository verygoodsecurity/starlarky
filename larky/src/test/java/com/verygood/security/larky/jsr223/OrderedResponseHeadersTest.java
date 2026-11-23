package com.verygood.security.larky.jsr223;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.modules.vgs.VgsHttpResponse;
import com.verygood.security.larky.parser.CurrentEvaluation;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.util.ArrayList;
import java.util.List;
import javax.script.CompiledScript;
import javax.script.SimpleBindings;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import org.junit.Test;

public class OrderedResponseHeadersTest {
  private static final String IMPORT = "load('@vgs//http/response', 'VGSHttpResponse')\n";
  private static final String COOKIE_WITH_EXPIRES = "a=1; Expires=Wed, 09 Jun 2027 10:18:14 GMT";

  @Test
  public void responseHasAJavaTypeAndKeepsItsStarlarkClass() throws Exception {
    ParsedStarFile file = eval("""
        response = VGSHttpResponse("response", status_code=201)
        same_class = response.__class__ == VGSHttpResponse
        """);
    var response = (VgsHttpResponse) file.getModule().getGlobal("response");
    assertThat(file.getGlobalEnvironmentVariable("same_class", Boolean.class)).isTrue();
    assertThat(assertThrows(EvalException.class, () -> response.setField("body", "changed")))
        .hasMessageThat().contains("trying to mutate a frozen VGSHttpResponse value");
  }

  @Test
  public void javaHeaderVisitorPreservesOrderAndDefersToCustomizedHeaders() throws Exception {
    var engine = new LarkyScriptEngine();
    var file = engine.eval(IMPORT + """
        def build(customization):
            response = VGSHttpResponse(headers=[("Old", "discard")])
            response.headers = [("Set-Cookie", "first=1"), ("set-cookie", "second=2")]
            if customization == "items":
                response.headers.items = lambda: [("Overridden", "yes")]
            if customization == "backing":
                replacement = VGSHttpResponse(headers=[("Replaced", "yes")])
                # a different header object than the one the headers property installed
                response._headers = replacement.headers
            return response
        """);
    try (var mutability = Mutability.create("response-headers")) {
      var thread = StarlarkThread.createTransient(mutability, LarkySemantics.LARKY_SEMANTICS);
      try (var scope = CurrentEvaluation.enter(thread)) {
        var ordinary = (VgsHttpResponse) engine.invokeMethodInScope(thread, file, "build", "none");
        var visited = new ArrayList<Tuple>();
        assertThat(ordinary.visitHeaderLines((name, value) -> visited.add(Tuple.pair(name, value))))
            .isTrue();
        assertThat(visited)
            .containsExactly(Tuple.pair("Set-Cookie", "first=1"), Tuple.pair("set-cookie", "second=2"))
            .inOrder();

        var customized = (VgsHttpResponse) engine.invokeMethodInScope(thread, file, "build", "items");
        visited.clear();
        assertThat(customized.visitHeaderLines((name, value) -> visited.add(Tuple.pair(name, value))))
            .isFalse();
        assertThat(visited).isEmpty();
        assertThat((List<?>) customized.headerItems(thread)).containsExactly(Tuple.pair("Overridden", "yes"));

        // a replaced container is still a native multidict, read directly
        var replaced = (VgsHttpResponse) engine.invokeMethodInScope(thread, file, "build", "backing");
        assertThat(replaced.visitHeaderLines((name, value) -> visited.add(Tuple.pair(name, value))))
            .isTrue();
        assertThat(visited).containsExactly(Tuple.pair("Replaced", "yes"));
        assertThat((List<?>) replaced.headerItems(thread)).containsExactly(Tuple.pair("Replaced", "yes"));
      }
    }
  }

  @Test
  public void preservesOrderedPairsAndCaseInsensitiveLookup() throws Exception {
    ParsedStarFile result = eval("""
        response = VGSHttpResponse(headers=[
            ("Set-Cookie", "%s"),
            ("X-Trace", "trace"),
            ("set-cookie", "b=2"),
        ])
        items = response.headers.items()
        cookies = response.get_all_headers("SET-COOKIE")
        first = response.headers["sEt-CoOkIe"]
        missing = response.get_all_headers("missing")
        """.formatted(COOKIE_WITH_EXPIRES));
    assertThat(result.getGlobalEnvironmentVariable("items", List.class))
        .containsExactly(
            Tuple.of("Set-Cookie", COOKIE_WITH_EXPIRES),
            Tuple.of("X-Trace", "trace"),
            Tuple.of("set-cookie", "b=2"))
        .inOrder();
    assertThat(result.getGlobalEnvironmentVariable("cookies", List.class))
        .containsExactly(COOKIE_WITH_EXPIRES, "b=2")
        .inOrder();
    assertThat(result.getGlobalEnvironmentVariable("first", Object.class))
        .isEqualTo(COOKIE_WITH_EXPIRES);
    assertThat(result.getGlobalEnvironmentVariable("missing", List.class)).isEmpty();
  }

  @Test
  public void caseOnlyRenameRetainsAllValuesWhenOldValuesAreRemovedFirst() throws Exception {
    ParsedStarFile result = eval("""
        def rename_headers():
            response = VGSHttpResponse(headers=[("X-Name", "first"), ("x-name", "second")])
            headers = response.headers
            old_values = headers.popall("X-Name")
            for value in old_values:
                headers.add("x-name", value)
            return list(headers.items())
        items = rename_headers()
        """);

    assertThat(result.getGlobalEnvironmentVariable("items", List.class))
        .containsExactly(Tuple.of("x-name", "first"), Tuple.of("x-name", "second"))
        .inOrder();
  }

  @Test
  public void replacementAppendAndRemovalPreserveUnrelatedOrder() throws Exception {
    ParsedStarFile result = eval("""
        def exercise():
            response = VGSHttpResponse(headers={
                "Set-Cookie": "a=1",
                "X-Trace": "trace",
                "set-cookie": "b=2",
            })
            response.headers["SET-COOKIE"] = "c=3"
            assignment = response.headers.items()
            response.append_header("set-cookie", "d=4")
            appended = response.headers.items()
            response.add_header("Set-Cookie", "e=5")
            replaced = response.headers.items()
            response.remove_header("set-COOKIE")
            response.remove_header("missing")
            return [assignment, appended, replaced, response.headers.items()]

        stages = exercise()
        """);
    List<?> stages = result.getGlobalEnvironmentVariable("stages", List.class);
    assertThat((List<?>) stages.get(0))
        .containsExactly(Tuple.of("SET-COOKIE", "c=3"), Tuple.of("X-Trace", "trace"))
        .inOrder();
    assertThat((List<?>) stages.get(1))
        .containsExactly(
            Tuple.of("SET-COOKIE", "c=3"),
            Tuple.of("X-Trace", "trace"),
            Tuple.of("set-cookie", "d=4"))
        .inOrder();
    assertThat((List<?>) stages.get(2))
        .containsExactly(Tuple.of("Set-Cookie", "e=5"), Tuple.of("X-Trace", "trace"))
        .inOrder();
    assertThat((List<?>) stages.get(3)).containsExactly(Tuple.of("X-Trace", "trace"));
  }

  @Test
  public void reassigningHeadersKeepsOrderedMultidictAndBodyChangeRemovesAllLengths()
      throws Exception {
    ParsedStarFile result = eval("""
        def exercise():
            response = VGSHttpResponse("old", status_code=201)
            response.headers = [
                ("Content-Length", "3"),
                ("X-Trace", "trace"),
                ("content-length", "3"),
            ]
            response.body = "old"
            unchanged = response.headers.items()
            response.body = "new"
            return [unchanged, response.headers.items(), response.body, str(response.status_code)]
        stages = exercise()
        """);
    List<?> stages = result.getGlobalEnvironmentVariable("stages", List.class);
    assertThat((List<?>) stages.get(0))
        .containsExactly(
            Tuple.of("Content-Length", "3"),
            Tuple.of("X-Trace", "trace"),
            Tuple.of("content-length", "3"))
        .inOrder();
    assertThat((List<?>) stages.get(1)).containsExactly(Tuple.of("X-Trace", "trace"));
    assertThat(stages.get(2)).isEqualTo("new");
    assertThat(stages.get(3)).isEqualTo("201");
  }

  @Test
  public void oldHostBindingCannotDisableOrderedHeaders() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    CompiledScript script = engine.compile(IMPORT + """
        header_type = type(VGSHttpResponse().headers)
        """);
    var bindings = new SimpleBindings();
    bindings.put("LARKY_ORDERED_RESPONSE_HEADERS", false);
    var result = (ParsedStarFile) script.eval(bindings);
    assertThat(result.getGlobalEnvironmentVariable("header_type", String.class))
        .isEqualTo("VGSCIMultiDict");
  }

  @Test
  public void invokingAnExistingFunctionUsesOrderedHeaders() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    engine.eval(IMPORT + """
        def header_type():
            return type(VGSHttpResponse().headers)
        """);
    assertThat(engine.invokeFunction("header_type")).isEqualTo("VGSCIMultiDict");
  }

  @Test
  public void copiesHeaderDictDuringConstructionAndAssignment() throws Exception {
    ParsedStarFile result = (ParsedStarFile) new LarkyScriptEngine().eval(IMPORT + """
        def exercise():
            original = {"Set-Cookie": "a=1"}
            response = VGSHttpResponse(headers=original)
            original["Set-Cookie"] = "b=2"
            original_cookie = response.headers["Set-Cookie"]
            response.headers = original
            response.add_header("set-cookie", "c=3")
            return [original_cookie, original.items(), response.headers.items(), response.body]
        actual = exercise()
        """);
    List<?> actual = result.getGlobalEnvironmentVariable("actual", List.class);
    assertThat(actual.get(0)).isEqualTo("a=1");
    assertThat((List<?>) actual.get(1)).containsExactly(Tuple.of("Set-Cookie", "b=2"));
    assertThat((List<?>) actual.get(2)).containsExactly(Tuple.of("set-cookie", "c=3"));
    assertThat(actual.get(3)).isEqualTo(Starlark.NONE);
  }

  @Test
  public void publicImportsRemainAvailableAfterSplittingHeaderDependencies() throws Exception {
    ParsedStarFile result = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@vgs//http/request", "CIMultiDict")
        load("@vendor//multidict", "types", "operator", "Error", "Ok")
        load("@stdlib//operator", "eq")
        load("@vendor//option/result", "Result")

        values = [
            types.is_string("value"),
            operator.eq(2, 2),
            eq("value", "value"),
            Error("problem").unwrap_err(),
            Ok("ready").unwrap(),
            Result.Error("another problem").unwrap_err(),
            CIMultiDict([("Name", "first")]).getone("NAME"),
        ]
        """);
    assertThat(result.getGlobalEnvironmentVariable("values", List.class))
        .containsExactly(true, true, true, "problem", "ready", "another problem", "first")
        .inOrder();
  }

  private static ParsedStarFile eval(String script) throws Exception {
    return (ParsedStarFile) new LarkyScriptEngine().eval(IMPORT + script);
  }
}
