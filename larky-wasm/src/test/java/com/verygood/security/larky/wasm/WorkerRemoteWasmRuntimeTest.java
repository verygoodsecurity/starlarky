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

package com.verygood.security.larky.wasm;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import com.verygood.security.larky.wasm.conformance.Fixtures;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Exercises mutual TLS and the complete remote-to-one-shot-JVM path on JDK 21. */
public final class WorkerRemoteWasmRuntimeTest {
  private Path files;
  private WorkerTlsServer server;
  private Thread serverThread;

  @Before
  public void setUp() throws Exception {
    files = Files.createTempDirectory("larky-wasm-remote-");
    String javaHome = System.getProperty("java.home");
    String password = "local-test-password";
    Path identity = files.resolve("identity.p12");
    Path cert = files.resolve("cert.der");
    Path trusted = files.resolve("trusted.p12");
    keytool(javaHome, "-genkeypair", "-alias", "worker", "-keyalg", "RSA", "-keysize", "2048",
        "-dname", "CN=localhost", "-ext", "SAN=dns:localhost", "-validity", "1",
        "-storetype", "PKCS12", "-keystore", identity.toString(), "-storepass", password,
        "-keypass", password, "-noprompt");
    keytool(javaHome, "-exportcert", "-alias", "worker", "-keystore", identity.toString(),
        "-storepass", password, "-file", cert.toString());
    keytool(javaHome, "-importcert", "-alias", "worker", "-file", cert.toString(),
        "-keystore", trusted.toString(), "-storetype", "PKCS12", "-storepass", password,
        "-noprompt");
    Path passwordFile = files.resolve("password");
    Files.writeString(passwordFile, password + "\n");
    System.setProperty("larky.wasm.worker.tls.keyStore", identity.toString());
    System.setProperty("larky.wasm.worker.tls.keyStorePasswordFile", passwordFile.toString());
    System.setProperty("larky.wasm.worker.tls.trustStore", trusted.toString());
    System.setProperty("larky.wasm.worker.tls.trustStorePasswordFile", passwordFile.toString());

    String java = Path.of(javaHome, "bin", "java").toString();
    String classpath = System.getProperty("java.class.path");
    Path launcher = files.resolve("launch.sh");
    Files.writeString(launcher, "#!/bin/sh\nexec '" + java.replace("'", "'\\''")
        + "' --add-modules jdk.incubator.vector -cp '" + classpath.replace("'", "'\\''")
        + "' com.verygood.security.larky.wasm.WorkerMain\n");
    launcher.toFile().setExecutable(true);
    System.setProperty("larky.wasm.worker.launcher", launcher.toString());
    System.setProperty("larky.wasm.worker.maxRequestMillis", "5000");
    WorkerWasmRuntime local = new WorkerWasmRuntime();
    server = new WorkerTlsServer(WorkerTlsConfig.configured(), local,
        InetAddress.getLoopbackAddress(), 0);
    serverThread = Thread.ofVirtual().start(() -> {
      try { server.serve(); } catch (Exception e) { throw new RuntimeException(e); }
    });
    System.clearProperty("larky.wasm.worker.launcher");
    System.setProperty("larky.wasm.worker.remote.host", "localhost");
    System.setProperty("larky.wasm.worker.remote.port", Integer.toString(server.port()));
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) server.close();
    if (serverThread != null) serverThread.join(1000);
    for (String name : new String[] {"launcher", "remote.host", "remote.port",
        "maxRequestMillis", "tls.keyStore", "tls.keyStorePasswordFile", "tls.trustStore",
        "tls.trustStorePasswordFile"}) {
      System.clearProperty("larky.wasm.worker." + name);
    }
    if (files != null) {
      try (var paths = Files.walk(files)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
      }
    }
  }

  @Test
  public void remoteWorkerRunsAndRejectsMalformedModules() throws Exception {
    var remote = WasmRuntime.byName("worker");
    byte[] input = "remote echo".getBytes(StandardCharsets.UTF_8);
    var result = remote.compile(Fixtures.wasm("echo")).run(input,
        new WasmRuntime.Limits(1 << 20, System.currentTimeMillis() + 5000, 1024, 0L));
    assertThat(result.stdout()).isEqualTo(input);
    var error = assertThrows(WasmException.class,
        () -> remote.compile(new byte[] {0, 97, 115, 109}));
    assertThat(error.kind()).isEqualTo(WasmException.Kind.INVALID_MODULE);
  }

  @Test
  public void remoteDeadlineLeavesServerAvailableForNextRequest() throws Exception {
    var remote = WasmRuntime.byName("worker");
    var spinning = remote.compile(Fixtures.wasm("spin"));
    var error = assertThrows(WasmException.class, () -> spinning.run(new byte[0],
        new WasmRuntime.Limits(1 << 20, System.currentTimeMillis() + 100, 1024, 0L)));
    assertThat(error.kind()).isEqualTo(WasmException.Kind.TIMEOUT);
    var healthy = remote.compile(Fixtures.wasm("echo"));
    var result = healthy.run(new byte[0], new WasmRuntime.Limits(1 << 20,
        System.currentTimeMillis() + 5000, 1024, 0L));
    assertThat(result.exitCode()).isEqualTo(0);
  }

  @Test
  public void oversizedTlsPasswordFileIsRejected() throws Exception {
    Files.writeString(files.resolve("password"), "x".repeat(1025));
    assertThrows(IllegalArgumentException.class, WorkerTlsConfig::configured);
  }

  private static void keytool(String home, String... args) throws Exception {
    String[] command = new String[args.length + 1];
    command[0] = Path.of(home, "bin", "keytool").toString();
    System.arraycopy(args, 0, command, 1, args.length);
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (process.waitFor() != 0) throw new AssertionError("keytool failed: " + output);
  }
}
