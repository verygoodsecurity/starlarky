// Copyright 2026 Very Good Security Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Atlas Horizon's script execution, in process, from the request or response protobuf to the
// serialized output, with both Larky caches warm. Scenarios (the spec): response,
// short-response, ordinary, terminal. Needs Atlas's service classes and dependencies on
// --extra-cp, and -Dlarky.moduleCache.enable=true; see bench/README.md. fingerprint() checks the
// output, that both compile caches hold one program each and keep their identity, and that a
// warm call runs no library initialization steps.
import com.google.common.cache.Cache;
import com.google.protobuf.ByteString;
import com.verygood.security.atlas.horizon.Payload;
import com.verygood.security.atlas.horizon.payload.HTTPHeaderBlock;
import com.verygood.security.atlas.horizon.payload.HTTPHeaderLine;
import com.verygood.security.atlas.horizon.payload.HTTPRequest;
import com.verygood.security.atlas.horizon.payload.HTTPResponse;
import com.verygood.security.atlas.horizon.service.grpc.scripting.ExecutionWrapper;
import com.verygood.security.atlas.horizon.service.grpc.types.ScriptPayloadWrapper;
import com.verygood.security.atlas.horizon.service.grpc.types.StarlarkyHttpRequestScriptPayload;
import com.verygood.security.atlas.horizon.service.grpc.types.StarlarkyHttpResponseScriptPayload;
import com.verygood.security.larky.jsr223.LarkyCompiledScript;
import com.verygood.security.larky.jsr223.LarkyScriptContext;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AtlasRequestProfile {
    private static final String HOST = "fixture.example.test:8443";
    private static final String PATH = "/v1/orders";
    private static final String QUERY = "region=us-east-1&at=21%3A20";
    private static final String URL = "https://" + HOST + PATH + "?" + QUERY;
    private static final ByteString BODY = ByteString.copyFromUtf8("{\"fixture\":true,\"count\":2}");
    private static final Map<String, String> CONTEXT =
            Map.of("attributes", "{\"decision\":\"forward\",\"stage\":\"request\"}");
    private static final HTTPRequest REQUEST = buildRequest();
    private static final String FORWARD_SCRIPT = """
            def process(request, ctx):
                request.add_header("X-Atlas-Decision", ctx["attributes"]["decision"])
                return request
            """;
    private static final String STOP_SCRIPT = """
            load("@vgs//http/response", "VGSHttpResponse")
            load("@vgs//proxy", "proxy")

            def process(request, ctx):
                return proxy.ShortCircuitResponse(VGSHttpResponse(
                    status_code=403,
                    body=b"request blocked",
                    headers=[
                        ("Set-Cookie", "first=one; Path=/; HttpOnly"),
                        ("X-Reject-Stage", ctx["attributes"]["stage"]),
                        ("Set-Cookie", "second=two; Path=/; Secure"),
                    ],
                ))
            """;
    private static HTTPRequest buildRequest() {
        var h = new LinkedHashMap<String, String>();
        h.put("Host", HOST);
        h.put("Accept", "application/json");
        h.put("Content-Type", "application/json");
        h.put("Content-Length", Integer.toString(BODY.size()));
        h.put("User-Agent", "atlas-request-benchmark");
        h.put("X-Request-ID", "fixture-42");
        h.put("X-Region", "us-east-1");
        h.put("X-Retry", "0");
        h.put("X-Api-Version", "2026-01");
        h.put("Cache-Control", "no-store");
        h.put("Accept-Language", "en");
        h.put("X-Correlation-ID", "fixture-correlation");
        return HTTPRequest.newBuilder().setUrl(URL).setHost(HOST).setPort(8443).setPath(PATH)
                .setQuery(QUERY).setMethod("POST").setBody(BODY).putAllHeaders(h).build();
    }


    private static final String OLD_FINGERPRINT =
            "cf0626e88dc746ef27a5f1e604446dd1db08b0fa2ca9acc877322b53655eb7dd";
    private static final List<String> RESPONSE_LINES = List.of(
            "Set-Cookie=session=1; Expires=Wed, 21 Oct 2015 07:28:00 GMT",
            "X-Marker=middle", "set-cookie=pref=2", "X-Bench=benchmark");
    private static final HTTPResponse RESPONSE = HTTPResponse.newBuilder()
            .setStatus(202).setBody(ByteString.copyFromUtf8("body"))
            .setOrderedHeaders(HTTPHeaderBlock.newBuilder()
                .addEntries(header("Set-Cookie", "session=1; Expires=Wed, 21 Oct 2015 07:28:00 GMT"))
                .addEntries(header("X-Marker", "middle"))
                .addEntries(header("set-cookie", "pref=2")))
            .build();
    private static final ScriptPayloadWrapper<?> RESPONSE_INPUT =
            StarlarkyHttpResponseScriptPayload.from(RESPONSE);
    private static final Map<String, String> RESPONSE_CONTEXT =
            Map.of("attributes", "{\"route\":\"benchmark\",\"attempt\":\"1\"}");
    private static final String RESPONSE_SCRIPT = """
            def process(response, ctx):
                response.headers["X-Bench"] = ctx["attributes"]["route"]
                return response
            """;
    private static final String SHORT_RESPONSE_SCRIPT = """
            load("@vgs//proxy", "proxy")

            def process(response, ctx):
                response.headers["X-Bench"] = ctx["attributes"]["route"]
                return proxy.ShortCircuitResponse(response)
            """;

    private static volatile Payload payload;
    private static volatile byte[] wireBytes;
    private static volatile String logsSink;
    private static volatile Map<String, String> contextSink;
    private static volatile int sizeSink;
    private static boolean stopped;
    private static long libraryInitSteps;
    private static Object wrapperProgram;
    private static Object evaluatorProgram;
    private static Map<?, ?> loadedModules;

    private static HTTPHeaderLine header(String name, String value) {
        return HTTPHeaderLine.newBuilder().setName(name).setValue(value).build();
    }

    public static Runnable prepare(String scenario) {
        String script = switch (scenario) {
            case "ordinary" -> FORWARD_SCRIPT;
            case "terminal" -> STOP_SCRIPT;
            case "response" -> RESPONSE_SCRIPT;
            case "short-response" -> SHORT_RESPONSE_SCRIPT;
            default -> throw new IllegalArgumentException(scenario);
        };
        boolean responsePhase = scenario.endsWith("response");
        return () -> {
            try {
                var input = responsePhase ? RESPONSE_INPUT : StarlarkyHttpRequestScriptPayload.from(REQUEST);
                var wrapper = new ExecutionWrapper(script, input,
                        responsePhase ? RESPONSE_CONTEXT : CONTEXT, responsePhase ? 4096 : 0,
                        System.currentTimeMillis() + 60_000);
                wrapper.execute();
                Payload output = wrapper.getOutput().toPayload();
                if (responsePhase) {
                    logsSink = wrapper.getScriptContext().getWriter().toString();
                    contextSink = wrapper.getOriginalContext();
                    sizeSink = output.getSerializedSize();
                    payload = output;
                } else {
                    wireBytes = output.toByteArray();
                }
                stopped = wrapper.isShortCircuit();
                var metadata = ((LarkyScriptContext) wrapper.getScriptContext()).metadata();
                libraryInitSteps = metadata == null ? -1 : metadata.libraryInitSteps();
            } catch (Exception e) {
                throw new IllegalStateException("Atlas execution failed", e);
            }
        };
    }

    public static String fingerprint(String scenario) throws Exception {
        boolean responsePhase = scenario.endsWith("response");
        byte[] bytes = responsePhase ? payload.toByteArray() : wireBytes;
        var output = Payload.parseFrom(bytes);
        if (scenario.equals("ordinary")) {
            require(!stopped && output.hasHttpRequest(), "ordinary case and stop flag");
            var request = output.getHttpRequest();
            require(request.getUrl().equals(URL) && request.getHost().equals(HOST)
                    && request.getPath().equals(PATH) && request.getQuery().equals(QUERY), "request URL fields");
            require(request.getMethod().equals("POST") && request.getBody().equals(BODY), "request method/body");
            var headers = new LinkedHashMap<>(REQUEST.getHeadersMap());
            headers.put("X-Atlas-Decision", "forward");
            require(request.getHeadersMap().equals(headers), "request header parity");
        } else {
            boolean shouldStop = !scenario.equals("response");
            require(stopped == shouldStop && output.hasHttpResponse(), "response and stop flag");
            var response = output.getHttpResponse();
            require(response.getStatus() == (responsePhase ? 202 : 403), "response status");
            require(response.getBody().toStringUtf8().equals(responsePhase ? "body" : "request blocked"),
                    "response body");
            require(response.hasOrderedHeaders(), "ordered response header block");
            var headers = response.getOrderedHeaders().getEntriesList().stream()
                    .map(line -> line.getName() + "=" + line.getValue()).toList();
            var expected = responsePhase ? RESPONSE_LINES : List.of(
                    "Set-Cookie=first=one; Path=/; HttpOnly", "X-Reject-Stage=request",
                    "Set-Cookie=second=two; Path=/; Secure");
            require(headers.equals(expected), "ordered response headers differ: " + headers);
            System.out.printf("response-proof,%s,status=%d,stop=%s,headers=%s%n",
                    scenario, response.getStatus(), stopped, headers);
        }
        assertWarmCaches();
        String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        System.out.printf("payload-proof,%s,bytes=%d,historical_wire_equal=%s%n",
                scenario, bytes.length, fingerprint.equals(OLD_FINGERPRINT));
        return fingerprint;
    }

    private static Object field(Class<?> owner, String name, Object instance) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static Class<?> cacheClass(String name) throws ClassNotFoundException {
        try {
            return Class.forName("com.verygood.security.larky.runtime." + name);
        } catch (ClassNotFoundException e) {
            return Class.forName("com.verygood.security.larky.parser." + name);
        }
    }

    private static void assertWarmCaches() throws Exception {
        require(Boolean.getBoolean("larky.moduleCache.enable"), "verified module cache flag");
        require(libraryInitSteps == 0, "warm library init steps: " + libraryInitSteps);
        var wrapperCache = (Cache<?, ?>) field(LarkyCompiledScript.class, "COMPILED", null);
        var parsedCache = (Cache<?, ?>) field(cacheClass("ProgramCache"),
                "SCRIPTS", null);
        require(wrapperCache.size() == 1 && parsedCache.size() == 1,
                "each compile cache must have one entry: " + wrapperCache.size() + "/" + parsedCache.size());
        Object newWrapperProgram = wrapperCache.asMap().values().iterator().next();
        Object newEvaluatorProgram = parsedCache.asMap().values().iterator().next();
        Class<?> moduleCacheType = cacheClass("ModuleCache");
        var moduleCache = (Map<?, ?>) field(moduleCacheType, "cache", field(moduleCacheType, "SHARED", null));
        require(!moduleCache.isEmpty(), "verified module cache contains no modules");
        if (wrapperProgram == null) {
            wrapperProgram = newWrapperProgram;
            evaluatorProgram = newEvaluatorProgram;
            loadedModules = Map.copyOf(moduleCache);
        } else {
            require(wrapperProgram == newWrapperProgram && evaluatorProgram == newEvaluatorProgram,
                    "program identity changed");
            require(loadedModules.size() == moduleCache.size(), "module admission count changed");
            for (var entry : loadedModules.entrySet()) {
                require(moduleCache.get(entry.getKey()) == entry.getValue(), "evaluated module identity changed");
            }
        }
        System.out.printf("cache-proof,wrapper-programs=%d,evaluator-programs=%d,admitted=%d,library-init=%d%n",
                wrapperCache.size(), parsedCache.size(), moduleCache.size(), libraryInitSteps);
    }

    public static String describe() {
        return LarkyScriptEngine.class.getProtectionDomain().getCodeSource().getLocation().toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
