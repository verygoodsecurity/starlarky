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
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.time.Duration;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** The public TLS gateway must reach WASM only through a separate loopback worker server. */
public final class WorkerSplitServerTest {
  private Path files;
  private WorkerPlainServer worker;
  private WorkerTlsGateway gateway;
  private Thread workerThread;
  private Thread gatewayThread;

  @Before public void setUp() throws Exception {
    files = Files.createTempDirectory("larky-wasm-split-");
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
    worker = new WorkerPlainServer(new WorkerWasmRuntime(), InetAddress.getLoopbackAddress(), 0);
    workerThread = Thread.ofVirtual().start(() -> {
      try { worker.serve(); } catch (IOException e) { throw new RuntimeException(e); }
    });
    gateway = new WorkerTlsGateway(WorkerTlsConfig.configured(),
        InetAddress.getLoopbackAddress(), 0, worker.port());
    gatewayThread = Thread.ofVirtual().start(() -> {
      try { gateway.serve(); } catch (IOException e) { throw new RuntimeException(e); }
    });
    System.clearProperty("larky.wasm.worker.launcher");
    System.setProperty("larky.wasm.worker.remote.host", "localhost");
    System.setProperty("larky.wasm.worker.remote.port", Integer.toString(gateway.port()));
  }

  @After public void tearDown() throws Exception {
    if (gateway != null) gateway.close();
    if (worker != null) worker.close();
    if (gatewayThread != null) gatewayThread.join(1000);
    if (workerThread != null) workerThread.join(1000);
    for (String name : new String[] {"launcher", "remote.host", "remote.port",
        "maxRequestMillis", "tls.keyStore", "tls.keyStorePasswordFile", "tls.trustStore",
        "tls.trustStorePasswordFile"}) System.clearProperty("larky.wasm.worker." + name);
    if (files != null) try (var paths = Files.walk(files)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
    }
  }

  @Test public void remoteRunPassesThroughSplitGatewayAndRecoversFromBadModule() throws Exception {
    WasmRuntime remote = WasmRuntime.byName("worker");
    byte[] input = "split echo".getBytes(StandardCharsets.UTF_8);
    assertThat(remote.compile(Fixtures.wasm("echo")).run(input,
        new WasmRuntime.Limits(1 << 20, System.currentTimeMillis() + 5000, 1024, 0L))
        .stdout()).isEqualTo(input);
    WasmException bad = assertThrows(WasmException.class,
        () -> remote.compile(new byte[] {0, 97, 115, 109}));
    assertThat(bad.kind()).isEqualTo(WasmException.Kind.INVALID_MODULE);
    assertThat(remote.compile(Fixtures.wasm("echo")).run(input,
        new WasmRuntime.Limits(1 << 20, System.currentTimeMillis() + 5000, 1024, 0L))
        .stdout()).isEqualTo(input);
  }

  @Test public void oversizedGatewayFrameClosesConnectionWithoutPoisoningNextRequest()
      throws Exception {
    try (SSLSocket socket = (SSLSocket) WorkerTlsConfig.configured().context()
        .getSocketFactory().createSocket("localhost", gateway.port())) {
      socket.setSoTimeout(2000);
      socket.startHandshake();
      socket.getOutputStream().write(new byte[] {0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff});
      socket.getOutputStream().flush();
      assertThat(socket.getInputStream().read()).isEqualTo(-1);
    }
    WasmRuntime remote = WasmRuntime.byName("worker");
    assertThat(remote.compile(Fixtures.wasm("echo")).run(new byte[0],
        new WasmRuntime.Limits(1 << 20, System.currentTimeMillis() + 5000, 1024, 0L))
        .exitCode()).isEqualTo(0);
  }

  @Test public void gatewayRejectsClientsWithoutAClientCertificate() throws Exception {
    KeyStore trusted = KeyStore.getInstance("PKCS12");
    try (var input = Files.newInputStream(files.resolve("trusted.p12"))) {
      trusted.load(input, "local-test-password".toCharArray());
    }
    TrustManagerFactory trusts = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    trusts.init(trusted);
    SSLContext withoutIdentity = SSLContext.getInstance("TLSv1.3");
    withoutIdentity.init(null, trusts.getTrustManagers(), null);
    try (SSLSocket socket = (SSLSocket) withoutIdentity.getSocketFactory()
        .createSocket("localhost", gateway.port())) {
      socket.setSoTimeout(2000);
      assertThrows(IOException.class, () -> {
        socket.startHandshake();
        socket.getOutputStream().write(new byte[] {0, 0, 0, 1, 0});
        socket.getOutputStream().flush();
        if (socket.getInputStream().read() == -1) throw new IOException("gateway refused client");
      });
    }
  }

  @Test public void timedOutLoopDoesNotBlockNextRequest() throws Exception {
    WasmRuntime remote = WasmRuntime.byName("worker");
    var spinning = remote.compile(Fixtures.wasm("spin"));
    long started = System.nanoTime();
    WasmException timeout = assertThrows(WasmException.class, () -> spinning.run(new byte[0],
        new WasmRuntime.Limits(1 << 20, System.currentTimeMillis() + 300, 1024, 0L)));
    assertThat(timeout.kind()).isEqualTo(WasmException.Kind.TIMEOUT);
    assertThat(Duration.ofNanos(System.nanoTime() - started).toSeconds()).isLessThan(3L);
    assertThat(remote.compile(Fixtures.wasm("echo")).run(new byte[0],
        new WasmRuntime.Limits(1 << 20, System.currentTimeMillis() + 5000, 1024, 0L))
        .exitCode()).isEqualTo(0);
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
