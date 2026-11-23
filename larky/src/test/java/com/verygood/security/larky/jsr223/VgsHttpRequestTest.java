/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.jsr223;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.modules.vgs.HttpRequest;
import com.verygood.security.larky.modules.vgs.VgsHttpRequest;
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
        dir_names = dir(req)
        """);
    assertThat(file.getGlobalEnvironmentVariable("same_class", Boolean.class)).isTrue();
    assertThat(file.getModule().getGlobal("initial")).isEqualTo(Tuple.of(
        "https://example.test:8443/path?x=1#fragment", "example.test:8443", "example.test", "POST"));
    assertThat(file.getGlobalEnvironmentVariable("unchanged_length", String.class)).isEqualTo("3");
    assertThat(file.getModule().getGlobal("names")).isEqualTo(Tuple.pair("dict", "dict"));
    assertThat(file.getGlobalEnvironmentVariable("removed", Boolean.class)).isTrue();
    assertThat((List<?>) file.getModule().getGlobal("headers"))
        .containsExactly(Tuple.pair("X-mixed", "initial"));
    // as in Python, __dict__ is the instance's state; the class and its methods are not in it
    assertThat((List<?>) file.getModule().getGlobal("dict_names"))
        .containsExactly("_data", "_full_url", "_tunnel_host", "fragment", "headers", "host",
            "origin_req_host", "selector", "type", "unredirected_hdrs", "unverifiable");
    assertThat((List<?>) file.getModule().getGlobal("dir_names"))
        .containsAtLeast("__class__", "__init__", "__name__", "full_url", "get_method", "headers");
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

  private static final String CUSTOM_PARSER = """
      load("@vgs//jhttprequest", "jhttprequest")
      load("@stdlib//larky", "larky")
      load("@stdlib//urllib/parse", "parse")
      load("@stdlib//urllib/request", "request")
      def parser(**overrides):
          functions = dict(unwrap=parse.unwrap, _splittag=parse._splittag, _splittype=parse._splittype,
                           _splithost=parse._splithost, unquote=parse.unquote)
          functions.update(overrides)
          return larky.struct(**functions)
      def build(url, p):
          return jhttprequest.Request(request.Request, p, request.request_host,
                                             request._copy_header_items, url, None, {}, "origin", False, None)
      """;

  @Test
  public void anOversizedHostGoesThroughUrllibUnquote() throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval(CUSTOM_PARSER + """
        calls = []
        def unquote(value):
            calls.append(len(value))
            return value
        req = build("https://" + "x" * 17000 + "/", parser(unquote=unquote))
        """);
    assertThat((List<?>) file.getModule().getGlobal("calls"))
        .containsExactly(net.starlark.java.eval.StarlarkInt.of(17000));
  }

  @Test
  public void malformedUrllibParseResultsAreRejected() {
    var engine = new LarkyScriptEngine();
    // a non-ASCII URL is parsed by the urllib.parse functions, not natively
    for (String body : List.of(
        "build('https://example.test/é', parser(_splittype=lambda url: None))",
        "build('https://example.test/é', parser(_splittype=lambda url: ('https', url), "
            + "_splithost=lambda rest: ('host',)))",
        "build('https://example.test/é', parser(unwrap=lambda url: url, _splittag=lambda url: [url]))")) {
      var error = assertThrows(ScriptException.class, () -> engine.eval(CUSTOM_PARSER + body + "\n"));
      assertThat(error).hasMessageThat().contains("expected `tuple` of length 2; got");
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
        .hasMessageThat().contains("trying to mutate a frozen VGSHttpRequest value");
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
        var overridden = (VgsHttpRequest) engine.invokeMethodInScope(thread, file, "build", "items");
        pairs.clear();
        assertThat(overridden.visitHeaderLines((name, value) -> pairs.add(Tuple.pair(name, value)))).isFalse();
        assertThat(pairs).isEmpty();
        assertThat((List<?>) overridden.headerItems(thread)).containsExactly(Tuple.pair("custom", "value"));
        // a replaced container is still a native multidict, read directly
        var replaced = (VgsHttpRequest) engine.invokeMethodInScope(thread, file, "build", "backing");
        pairs.clear();
        assertThat(replaced.visitHeaderLines((name, value) -> pairs.add(Tuple.pair(name, value)))).isTrue();
        assertThat(pairs).containsExactly(Tuple.pair("custom", "value"));
      }
    }
  }

  @Test
  public void aHostReadsTheUrlAfterTheEvaluation() throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@vgs//http/request", "VGSHttpRequest")
        request = VGSHttpRequest("https://example.test/path?x=1#part")
        state = request.__dict__
        state["extra"] = 1
        """);
    var request = (VgsHttpRequest) file.getModule().getGlobal("request");
    // no evaluation is active here
    assertThat(request.getValue("url")).isEqualTo("https://example.test/path?x=1#part");
    assertThat(request.getValue("full_url")).isEqualTo("https://example.test/path?x=1#part");
    // __dict__ is a copy the script may change, as a struct's was
    assertThat(file.getModule().getGlobal("state").toString()).contains("\"extra\": 1");
  }

  @Test
  public void urlAndBodyFollowFullUrlAndData() throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@vgs//http/request", "VGSHttpRequest")
        request = VGSHttpRequest("https://example.test/initial?a=1#frag", data="one")
        url = request.url
        request.url = "https://example.test/new?x=1"
        parts = (request.path, request.query_string, request.full_url)
        request.body = "two"
        data = request.data
        request.data = "three"
        body = request.body
        """);
    assertThat(file.getGlobalEnvironmentVariable("url", String.class))
        .isEqualTo("https://example.test/initial?a=1#frag");
    assertThat(file.getGlobalEnvironmentVariable("parts", Object.class))
        .isEqualTo(Tuple.of("/new", "x=1", "https://example.test/new?x=1"));
    assertThat(file.getGlobalEnvironmentVariable("data", String.class)).isEqualTo("two");
    assertThat(file.getGlobalEnvironmentVariable("body", String.class)).isEqualTo("three");
  }
}
