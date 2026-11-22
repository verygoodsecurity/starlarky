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

import com.verygood.security.larky.jsr223.LarkyScriptEngineFactory;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

/**
 * Calls one function of a {@code .star} file. The spec is {@code path/to/file.star#function}: the
 * file is evaluated once through the JSR-223 engine, and each call invokes {@code function()} with
 * no arguments ({@link Invocable#invokeFunction}). The fingerprint is a hash of {@code
 * repr(function())}, computed in Starlark, so the builds must return equal values.
 *
 * <p>Compiled by {@code larky-bench} against each build with {@code --workload-src}.
 */
public final class StarlarkCall {

  private static final String REPR = "__larky_bench_repr__";

  private StarlarkCall() {}

  private static Invocable engine;
  private static String function;
  private static volatile Object sink;

  private static synchronized Invocable engine(String spec) throws Exception {
    if (engine != null) {
      return engine;
    }
    int hash = spec.lastIndexOf('#');
    if (hash < 0) {
      throw new IllegalArgumentException("spec must be path/to/file.star#function: " + spec);
    }
    function = spec.substring(hash + 1);
    String source = Files.readString(Path.of(spec.substring(0, hash)), StandardCharsets.UTF_8);
    ScriptEngine e = new LarkyScriptEngineFactory().getScriptEngine();
    ScriptContext context = new SimpleScriptContext();
    context.setWriter(new StringWriter());
    context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
    e.setContext(context);
    e.eval(source + "\n\ndef " + REPR + "(x):\n    return repr(x)\n");
    if (!(e instanceof Invocable invocable)) {
      throw new IllegalStateException("this build's engine is not Invocable");
    }
    engine = invocable;
    return engine;
  }

  public static Runnable prepare(String spec) throws Exception {
    Invocable e = engine(spec);
    String fn = function;
    return () -> {
      try {
        sink = e.invokeFunction(fn);
      } catch (Exception ex) {
        throw new IllegalStateException(fn + "() failed", ex);
      }
    };
  }

  public static String fingerprint(String spec) throws Exception {
    Invocable e = engine(spec);
    Object repr = e.invokeFunction(REPR, e.invokeFunction(function));
    byte[] digest = MessageDigest.getInstance("SHA-256")
        .digest(String.valueOf(repr).getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest);
  }

  public static String describe() {
    return LarkyScriptEngineFactory.class.getProtectionDomain().getCodeSource().getLocation().toString();
  }
}
