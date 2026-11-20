package com.verygood.security.larky.jsr223;

import static com.google.common.truth.Truth.assertThat;

import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.util.List;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.Tuple;
import org.junit.Test;

public class OrderedResponseHeadersTest {
  private static final String BINDING = LarkyEvaluator.ORDERED_RESPONSE_HEADERS;
  private static final String IMPORT = "load('@vgs//http/response', 'VGSHttpResponse')\n";
  private static final String COOKIE_WITH_EXPIRES = "a=1; Expires=Wed, 09 Jun 2027 10:18:14 GMT";

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
  public void requiresBooleanOptInOnEachEvaluation() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    CompiledScript script = engine.compile(IMPORT + """
        response = VGSHttpResponse(headers={"Set-Cookie": "a=1"})
        header_type = type(response.headers)
        """);
    assertThat(headerType(script.eval(orderedBindings()))).isEqualTo("VGSCIMultiDict");
    assertThat(headerType(script.eval(new SimpleBindings()))).isEqualTo("dict");
    Bindings disabled = new SimpleBindings();
    disabled.put(BINDING, false);
    assertThat(headerType(script.eval(disabled))).isEqualTo("dict");
    disabled.put(BINDING, "true");
    assertThat(headerType(script.eval(disabled))).isEqualTo("dict");
    assertThat(headerType(script.eval(orderedBindings()))).isEqualTo("VGSCIMultiDict");
  }

  @Test
  public void settingTheNameInScriptCannotEnableOrderedHeadersOnReusedBindings()
      throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    Bindings bindings = new SimpleBindings();
    engine.eval("LARKY_ORDERED_RESPONSE_HEADERS = True\n", bindings);
    assertThat(headerType(engine.eval(
        IMPORT + "header_type = type(VGSHttpResponse().headers)\n", bindings)))
        .isEqualTo("dict");
  }

  @Test
  public void invokingAnExistingFunctionUsesTheCallMode() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    engine.eval(IMPORT + """
        def header_type():
            return type(VGSHttpResponse().headers)
        """);
    assertThat(engine.invokeFunction("header_type")).isEqualTo("dict");
    engine.put(BINDING, true);
    assertThat(engine.invokeFunction("header_type")).isEqualTo("VGSCIMultiDict");
    engine.getBindings(ScriptContext.ENGINE_SCOPE).remove(BINDING);
    assertThat(engine.invokeFunction("header_type")).isEqualTo("dict");
  }

  @Test
  public void legacyCopiesConstructorDictAndAliasesAssignedDict() throws Exception {
    ParsedStarFile result = (ParsedStarFile) new LarkyScriptEngine().eval(IMPORT + """
        def exercise():
            original = {"Set-Cookie": "a=1"}
            response = VGSHttpResponse(headers=original)
            original["Set-Cookie"] = "b=2"
            original_cookie = response.headers["Set-Cookie"]
            response.headers = original
            response.add_header("set-cookie", "c=3")
            return [original_cookie, type(response.headers), original.items(), response.body]
        actual = exercise()
        """);
    List<?> actual = result.getGlobalEnvironmentVariable("actual", List.class);
    assertThat(actual.get(0)).isEqualTo("a=1");
    assertThat(actual.get(1)).isEqualTo("dict");
    assertThat((List<?>) actual.get(2))
        .containsExactly(Tuple.of("Set-Cookie", "b=2"), Tuple.of("set-cookie", "c=3"))
        .inOrder();
    assertThat(actual.get(3)).isEqualTo(Starlark.NONE);
  }

  @Test
  public void changingLegacyBodyClearsStaleLengthsFromTheAssignedDict() throws Exception {
    ParsedStarFile result = (ParsedStarFile) new LarkyScriptEngine().eval(IMPORT + """
        def exercise():
            headers = {"Content-length": "3", "CONTENT-LENGTH": "3", "X-Trace": "trace"}
            response = VGSHttpResponse("old")
            response.headers = headers
            response.body = "new"
            return headers.items()
        items = exercise()
        """);
    assertThat(result.getGlobalEnvironmentVariable("items", List.class))
        .containsExactly(Tuple.of("X-Trace", "trace"));
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
    return (ParsedStarFile) new LarkyScriptEngine().eval(IMPORT + script, orderedBindings());
  }

  private static Bindings orderedBindings() {
    Bindings bindings = new SimpleBindings();
    bindings.put(BINDING, true);
    return bindings;
  }

  private static String headerType(Object result) {
    return ((ParsedStarFile) result).getGlobalEnvironmentVariable("header_type", String.class);
  }
}
