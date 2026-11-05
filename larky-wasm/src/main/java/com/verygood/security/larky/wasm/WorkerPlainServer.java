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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** Guest-processing server reachable only over Pod loopback from the TLS gateway. */
public final class WorkerPlainServer implements AutoCloseable {
  private static final System.Logger LOGGER = System.getLogger(WorkerPlainServer.class.getName());
  private final ServerSocket listener;
  private final WorkerRequestService service;
  private final long maxRequestMillis;
  private final Semaphore admission;
  private final ExecutorService handlers = Executors.newVirtualThreadPerTaskExecutor();

  WorkerPlainServer(WorkerWasmRuntime runtime, InetAddress address, int port) throws IOException {
    if (!address.isLoopbackAddress()) {
      throw new IllegalArgumentException("plaintext worker must bind to loopback");
    }
    this.listener = new ServerSocket(port, 16, address);
    this.maxRequestMillis = WorkerTlsServer.positiveLong("maxRequestMillis", 10_000);
    this.service = new WorkerRequestService(runtime, maxRequestMillis);
    long maxConcurrent = WorkerTlsServer.positiveLong("maxConcurrent", 1);
    if (maxConcurrent > 32) throw new IllegalArgumentException("worker concurrency exceeds 32");
    this.admission = new Semaphore((int) maxConcurrent);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 0) throw new IllegalArgumentException("worker server takes no arguments");
    int port = (int) WorkerTlsServer.positiveLong("plainPort", 8081);
    if (port > 65535) throw new IllegalArgumentException("worker port exceeds 65535");
    try (WorkerPlainServer server = new WorkerPlainServer(new WorkerWasmRuntime(),
        InetAddress.getLoopbackAddress(), port)) {
      server.serve();
    }
  }

  int port() { return listener.getLocalPort(); }

  void serve() throws IOException {
    while (!listener.isClosed()) {
      Socket socket;
      try {
        socket = listener.accept();
      } catch (SocketException e) {
        if (listener.isClosed()) return;
        throw e;
      }
      try {
        while (!listener.isClosed() && !admission.tryAcquire(100, TimeUnit.MILLISECONDS)) { }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        socket.close();
        return;
      }
      if (listener.isClosed()) { socket.close(); return; }
      handlers.submit(() -> {
        try (socket) {
          socket.setSoTimeout((int) Math.min(Integer.MAX_VALUE, maxRequestMillis));
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
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
        } catch (IOException ignored) {
          // Malformed frames and client disconnects are expected; never log guest input.
        } catch (RuntimeException unexpected) {
          // Do not log the exception or its message: they can include guest-controlled data.
          LOGGER.log(System.Logger.Level.WARNING, "Unexpected WASM worker request failure: "
              + unexpected.getClass().getName());
        } finally {
          admission.release();
        }
      });
    }
  }

  @Override public void close() throws IOException {
    listener.close();
    handlers.shutdownNow();
  }
}
