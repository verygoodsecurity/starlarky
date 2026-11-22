package com.verygood.security.larky.jsr223;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.modules.types.structs.HttpRequest;
import com.verygood.security.larky.modules.types.structs.VgsHttpRequest;
import com.verygood.security.larky.parser.CurrentEvaluation;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.util.ArrayList;
import java.util.List;
import javax.script.ScriptException;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import org.junit.Test;

public class VgsHttpRequestTest {
  @Test
  public void urllibRequestRetainsDictHeaderSemantics() throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@stdlib//urllib/request", "request")
        req = request.Request(
            "<URL:https://example.test:8443/path?x=1#fragment>",
            headers={"X-MiXeD": "initial", "Content-Length": "3"},
            data="old",
        )
        same_class = req.__class__ == request.Request and req.__name__ == "Request"
        initial = (req.get_full_url(), req.host, req.origin_req_host, req.get_method())
        req.add_unredirected_header(key="x-mixed", val="unredirected")
        req.add_unredirected_header(key="content-length", val="4")
        req.data = "old"
        unchanged_length = req.get_header(header_name="Content-length", default="missing")
        req.data = "new"
        names = (type(req.headers), type(req.unredirected_hdrs))
        removed = not req.has_header(header_name="Content-length")
        headers = req.header_items()
        dict_names = sorted(req.__dict__.keys())
        """);
    assertThat(file.getGlobalEnvironmentVariable("same_class", Boolean.class)).isTrue();
    assertThat(file.getModule().getGlobal("initial")).isEqualTo(Tuple.of(
        "https://example.test:8443/path?x=1#fragment", "example.test:8443", "example.test", "POST"));
    assertThat(file.getGlobalEnvironmentVariable("unchanged_length", String.class)).isEqualTo("3");
    assertThat(file.getModule().getGlobal("names")).isEqualTo(Tuple.pair("dict", "dict"));
    assertThat(file.getGlobalEnvironmentVariable("removed", Boolean.class)).isTrue();
    assertThat((List<?>) file.getModule().getGlobal("headers"))
        .containsExactly(Tuple.pair("X-mixed", "initial"));
    assertThat((List<?>) file.getModule().getGlobal("dict_names"))
        .containsAtLeast("__class__", "__init__", "__name__", "headers", "unredirected_hdrs");
  }

  @Test
  public void bothConstructorsShareJavaBacking() throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@stdlib//urllib/request", "request")
        load("@vgs//http/request", "VGSHttpRequest")
        generic = request.Request("https://example.test/")
        vgs = VGSHttpRequest("https://example.test/")
        """);
    assertThat(file.getModule().getGlobal("generic")).isInstanceOf(HttpRequest.class);
    assertThat(file.getModule().getGlobal("vgs")).isInstanceOf(HttpRequest.class);
  }

  @Test
  public void vgsBodyUpdatePreservesOtherHeaderLines() throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@vgs//http/request", "VGSHttpRequest")
        req = VGSHttpRequest(
            "https://example.test/path?x=1", data=b"old",
            headers=[("Content-Length", "3"), ("X-Dup", "first"), ("x-dup", "second")],
        )
        req.add_unredirected_header(key="Content-length", val="other")
        req.add_unredirected_header(key="X-Hidden", val="private")
        req.body = b"old"
        unchanged = req.get_header(header_name="content-length", default="missing")
        req.body = b"new"
        removed = not req.has_header(header_name="Content-length")
        lines = req.headers.items()
        hidden = req.unredirected_hdrs.items()
        values = (req.body, req.data, req.path, req.query_string)
        """);
    assertThat(file.getGlobalEnvironmentVariable("unchanged", String.class)).isEqualTo("3");
    assertThat(file.getGlobalEnvironmentVariable("removed", Boolean.class)).isTrue();
    assertThat((List<?>) file.getModule().getGlobal("lines")).containsExactly(
        Tuple.pair("X-Dup", "first"), Tuple.pair("x-dup", "second")).inOrder();
    assertThat((List<?>) file.getModule().getGlobal("hidden"))
        .containsExactly(Tuple.pair("X-Hidden", "private"));
    Tuple values = (Tuple) file.getModule().getGlobal("values");
    assertThat(values.get(0)).isEqualTo(values.get(1));
    assertThat(values.get(2)).isEqualTo("/path");
    assertThat(values.get(3)).isEqualTo("x=1");
  }

  @Test
  public void headerItemsChargesWorkForListAndTupleMappings() throws Exception {
    var engine = new LarkyScriptEngine();
    var file = engine.eval("""
        load("@stdlib//urllib/request", "Request")
        pairs = zip(range(500), range(500))
        from_list = Request("https://example.test/")
        from_list.headers = pairs
        from_tuple = Request("https://example.test/")
        from_tuple.headers = tuple(pairs)
        def merged(use_list):
            req = from_list if use_list else from_tuple
            return req.header_items()
        """);
    for (boolean useList : List.of(true, false)) {
      try (var mutability = Mutability.create("request-header-step-limit")) {
        var thread = StarlarkThread.createTransient(mutability, LarkySemantics.LARKY_SEMANTICS);
        thread.setMaxExecutionSteps(100);
        var error = assertThrows(ScriptException.class,
            () -> engine.invokeMethodInScope(thread, file, "merged", useList));
        assertThat(error).hasMessageThat().contains("too many steps");
      }
    }
  }

  @Test
  public void headerItemsConsumesCustomIterableOnce() throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@stdlib//larky", "larky")
        load("@stdlib//urllib/request", "Request")
        calls = []
        def content():
            calls.append(True)
            return iter([iter(("X-Custom", "value")), ("X-Second", "second")])
        req = Request("https://example.test/")
        req.headers = larky.struct(__iter__=content)
        result = req.header_items()
        """);
    assertThat((List<?>) file.getModule().getGlobal("calls")).containsExactly(true);
    assertThat((List<?>) file.getModule().getGlobal("result")).containsExactly(
        Tuple.pair("X-Custom", "value"), Tuple.pair("X-Second", "second")).inOrder();
  }

  @Test
  public void legacyParserDelegatesAnOversizedHostToUnquote() throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@stdlib//http_request_native", "http_request_native")
        load("@stdlib//larky", "larky")
        load("@stdlib//urllib/request", "Request")
        req = Request("https://example.test/")
        host = "x" * 17000
        req.host = larky.property(lambda: host, lambda value: None)
        calls = []
        def unquote(value):
            calls.append(len(value))
            return value
        http_request_native.parse_url(req, larky.struct(unquote=unquote))
        """);
    assertThat((List<?>) file.getModule().getGlobal("calls"))
        .containsExactly(net.starlark.java.eval.StarlarkInt.of(17000));
  }

  @Test
  public void legacyParserRejectsMalformedCallbackResults() {
    var engine = new LarkyScriptEngine();
    var script = """
        load("@stdlib//http_request_native", "http_request_native")
        load("@stdlib//larky", "larky")
        load("@stdlib//urllib/request", "Request")
        req = Request("https://example.test/")
        req._full_url = "https://example.test/é"
        """;
    for (String body : List.of(
        "http_request_native.parse_url(req, larky.struct(_splittype=lambda url: None))",
        "http_request_native.parse_url(req, larky.struct(_splittype=lambda url: ('https', url), "
            + "_splithost=lambda rest: ('host',)))",
        "http_request_native.set_full_url(req, req._full_url, larky.struct(unwrap=lambda url: url, "
            + "_splittag=lambda url: [url]))")) {
      var error = assertThrows(ScriptException.class, () -> engine.eval(script + body + "\n"));
      assertThat(error).hasMessageThat().contains("must return two values");
    }
  }

  @Test
  public void nativeRequestKeepsItsStarlarkClass() throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@vgs//http/request", "VGSHttpRequest")
        request = VGSHttpRequest("https://example.test/path?x=1#fragment", method="PUT")
        same_class = request.__class__ == VGSHttpRequest
        """);
    var request = (VgsHttpRequest) file.getModule().getGlobal("request");
    assertThat(file.getGlobalEnvironmentVariable("same_class", Boolean.class)).isTrue();
    assertThat(assertThrows(EvalException.class, () -> request.setField("body", "new")))
        .hasMessageThat().contains("frozen structure");
  }

  @Test
  public void visitHeadersDefersToOverriddenItemsAndBacking() throws Exception {
    var engine = new LarkyScriptEngine();
    var file = (ParsedStarFile) engine.eval("""
        load("@vgs//http/request", "VGSHttpRequest")
        def build(customization):
            request = VGSHttpRequest("https://example.test/", headers=[("X-Trace", "first"), ("x-trace", "second")])
            if customization == "items":
                request.headers.items = lambda: [("custom", "value")]
            if customization == "backing":
                replacement = VGSHttpRequest("https://example.test/", headers=[("custom", "value")])
                # a different header object than the one the headers property installed
                request._headers = replacement.headers
            return request
        """);
    try (var mutability = Mutability.create("request-headers")) {
      var thread = StarlarkThread.createTransient(mutability, LarkySemantics.LARKY_SEMANTICS);
      try (var scope = CurrentEvaluation.enter(thread)) {
        var request = (VgsHttpRequest) engine.invokeMethodInScope(thread, file, "build", "none");
        var pairs = new ArrayList<Tuple>();
        assertThat(request.visitHeaderLines((name, value) -> pairs.add(Tuple.pair(name, value)))).isTrue();
        assertThat(pairs).containsExactly(Tuple.pair("X-Trace", "first"), Tuple.pair("x-trace", "second"))
            .inOrder();
        for (String customization : List.of("items", "backing")) {
          var custom = (VgsHttpRequest) engine.invokeMethodInScope(thread, file, "build", customization);
          pairs.clear();
          assertThat(custom.visitHeaderLines((name, value) -> pairs.add(Tuple.pair(name, value)))).isFalse();
          assertThat(pairs).isEmpty();
          assertThat((List<?>) custom.headerItems(thread)).containsExactly(Tuple.pair("custom", "value"));
        }
      }
    }
  }

  @Test
  public void urlAndBodyKeepCustomUnderlyingAccessors() throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@vgs//http/request", "VGSHttpRequest")
        load("@stdlib//larky", "larky")
        request = VGSHttpRequest("https://example.test/initial")
        request.full_url = larky.property(lambda: "https://example.test/custom", lambda value: None)
        url = request.url
        request.url = "https://example.test/new?x=1"
        parts = (request.path, request.query_string)
        request.data = larky.property(lambda: "custom", lambda value: None)
        data = request.data
        body = request.body
        """);
    assertThat(file.getGlobalEnvironmentVariable("url", String.class))
        .isEqualTo("https://example.test/custom");
    assertThat(file.getGlobalEnvironmentVariable("parts", Object.class)).isEqualTo(Tuple.pair("/new", "x=1"));
    assertThat(file.getGlobalEnvironmentVariable("data", String.class)).isEqualTo("custom");
    assertThat(file.getModule().getGlobal("body")).isEqualTo(net.starlark.java.eval.Starlark.NONE);
  }
}
