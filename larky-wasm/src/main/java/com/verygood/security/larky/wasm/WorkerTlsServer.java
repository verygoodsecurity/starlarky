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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/**
 * Mutually authenticated gateway for a separately limited worker Pod. It decodes only bounded
 * protocol frames; {@link WorkerWasmRuntime} launches a fresh child JVM for guest compilation and
 * execution. Kubernetes must place this server and its children outside the Atlas service cgroup.
 */
public final class WorkerTlsServer implements AutoCloseable {
  private static final String PREFIX = "larky.wasm.worker.server.";
  private final SSLServerSocket listener;
  private final WorkerRequestService service;
  private final long maxRequestMillis;
  private final Semaphore admission;
  private final ExecutorService handlers = Executors.newVirtualThreadPerTaskExecutor();

  WorkerTlsServer(WorkerTlsConfig tls, WorkerWasmRuntime runtime, InetAddress address, int port)
      throws IOException {
    this.listener = (SSLServerSocket) tls.context().getServerSocketFactory()
        .createServerSocket(port, 16, address);
    this.listener.setNeedClientAuth(true);
    this.listener.setEnabledProtocols(new String[] {"TLSv1.3"});
    this.maxRequestMillis = positiveLong("maxRequestMillis", 10_000);
    this.service = new WorkerRequestService(runtime, maxRequestMillis);
    long maxConcurrent = positiveLong("maxConcurrent", 1);
    if (maxConcurrent > 32) {
      throw new IllegalArgumentException("worker server maxConcurrent exceeds 32");
    }
    this.admission = new Semaphore((int) maxConcurrent);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 0) throw new IllegalArgumentException("worker server takes no arguments");
    WorkerWasmRuntime local = new WorkerWasmRuntime();
    WorkerTlsConfig tls = WorkerTlsConfig.configured();
    int port = (int) positiveLong("port", 8443);
    if (port > 65535) throw new IllegalArgumentException("worker server port exceeds 65535");
    try (WorkerTlsServer server = new WorkerTlsServer(tls, local,
        InetAddress.getByName("0.0.0.0"), port)) {
      server.serve();
    }
  }

  int port() { return listener.getLocalPort(); }

  void serve() throws IOException {
    while (!listener.isClosed()) {
      SSLSocket socket;
      try {
        socket = (SSLSocket) listener.accept();
      } catch (SocketException e) {
        if (listener.isClosed()) return;
        throw e;
      }
      try {
        // Hold at most one accepted connection while all slots are occupied. Other connections
        // remain in the bounded kernel accept backlog rather than spawning unbounded handlers.
        while (!listener.isClosed() && !admission.tryAcquire(100, TimeUnit.MILLISECONDS)) { }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        socket.close();
        return;
      }
      if (listener.isClosed()) {
        socket.close();
        return;
      }
      handlers.submit(() -> {
        try (socket) {
          socket.setSoTimeout((int) Math.min(Integer.MAX_VALUE, maxRequestMillis));
          handle(socket);
        } catch (IOException | InterruptedException ignored) {
          // A disconnected or malformed client has no result channel. Never log guest bytes.
          if (ignored instanceof InterruptedException) Thread.currentThread().interrupt();
        } finally {
          admission.release();
        }
      });
    }
  }

  private void handle(SSLSocket socket) throws IOException, InterruptedException {
    socket.startHandshake();
    DataInputStream in = new DataInputStream(socket.getInputStream());
    int size = in.readInt();
    if (size <= 0 || size > service.maxRequestBytes()) {
      throw new IOException("worker request exceeded limit");
    }
    byte[] frame = in.readNBytes(size);
    if (frame.length != size) throw new IOException("truncated worker request");
    byte[] response = service.handle(frame);
    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
    out.writeInt(response.length);
    out.write(response);
    out.flush();
  }

  static long positiveLong(String suffix, long fallback) {
    String value = System.getProperty(PREFIX + suffix);
    if (value == null) return fallback;
    long parsed = Long.parseLong(value);
    if (parsed <= 0 || parsed > 86_400_000L) {
      throw new IllegalArgumentException(PREFIX + suffix + " must be between 1 and 86400000");
    }
    return parsed;
  }

  @Override public void close() throws IOException {
    listener.close();
    handlers.shutdownNow();
  }
}
