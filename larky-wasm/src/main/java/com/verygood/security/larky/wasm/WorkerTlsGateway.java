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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** Authenticates the client and forwards bounded frames; it never loads customer WASM. */
public final class WorkerTlsGateway implements AutoCloseable {
  private static final System.Logger LOGGER = System.getLogger(WorkerTlsGateway.class.getName());
  private final SSLServerSocket listener;
  private final int workerPort;
  private final long maxRequestBytes;
  private final long maxResponseBytes;
  private final int timeoutMillis;
  private final Semaphore admission;
  private final ExecutorService handlers = Executors.newVirtualThreadPerTaskExecutor();
  private final ScheduledExecutorService deadlines = Executors.newSingleThreadScheduledExecutor(
      task -> Thread.ofPlatform().daemon().name("wasm-gateway-deadline").unstarted(task));

  WorkerTlsGateway(WorkerTlsConfig tls, InetAddress address, int port, int workerPort)
      throws IOException {
    if (workerPort <= 0 || workerPort > 65535) throw new IllegalArgumentException("bad worker port");
    this.listener = (SSLServerSocket) tls.context().getServerSocketFactory()
        .createServerSocket(port, 16, address);
    this.listener.setNeedClientAuth(true);
    this.listener.setEnabledProtocols(new String[] {"TLSv1.3"});
    this.workerPort = workerPort;
    WasmHostLimits caps = WasmHostLimits.configured();
    this.maxRequestBytes = 8192L + caps.maxModuleBytes()
        + WorkerWasmRuntime.configuredMaxInputBytes();
    this.maxResponseBytes = 32L + 2L * WorkerWasmRuntime.configuredMaxOutputBytes()
        + WorkerProtocol.MAX_MESSAGE_BYTES;
    this.timeoutMillis = (int) Math.min(Integer.MAX_VALUE,
        WorkerTlsServer.positiveLong("maxRequestMillis", 10_000));
    long maxConcurrent = WorkerTlsServer.positiveLong("maxConcurrent", 1);
    if (maxConcurrent > 32) throw new IllegalArgumentException("gateway concurrency exceeds 32");
    this.admission = new Semaphore((int) maxConcurrent);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 0) throw new IllegalArgumentException("TLS gateway takes no arguments");
    int port = (int) WorkerTlsServer.positiveLong("port", 8443);
    int workerPort = (int) WorkerTlsServer.positiveLong("plainPort", 8081);
    if (port > 65535 || workerPort > 65535) throw new IllegalArgumentException("port exceeds 65535");
    try (WorkerTlsGateway gateway = new WorkerTlsGateway(WorkerTlsConfig.configured(),
        InetAddress.getByName("0.0.0.0"), port, workerPort)) {
      gateway.serve();
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
        while (!listener.isClosed() && !admission.tryAcquire(100, TimeUnit.MILLISECONDS)) { }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        socket.close();
        return;
      }
      if (listener.isClosed()) { socket.close(); return; }
      try {
        handlers.submit(() -> {
          AtomicReference<Socket> workerSocket = new AtomicReference<>();
          ScheduledFuture<?> deadline = null;
          try (socket) {
            deadline = deadlines.schedule(() -> {
              closeQuietly(socket);
              closeQuietly(workerSocket.get());
            }, timeoutMillis, TimeUnit.MILLISECONDS);
            socket.setSoTimeout(timeoutMillis);
            socket.startHandshake();
            DataInputStream input = new DataInputStream(socket.getInputStream());
            int size = input.readInt();
            if (size <= 0 || size > maxRequestBytes) {
              throw new IOException("gateway request exceeded limit");
            }
            byte[] frame = input.readNBytes(size);
            if (frame.length != size) throw new IOException("truncated gateway request");
            byte[] response;
            try (Socket worker = new Socket()) {
              workerSocket.set(worker);
              if (socket.isClosed()) throw new IOException("gateway deadline exceeded");
              worker.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), workerPort),
                  timeoutMillis);
              worker.setSoTimeout(timeoutMillis);
              DataOutputStream toWorker = new DataOutputStream(worker.getOutputStream());
              toWorker.writeInt(size);
              toWorker.write(frame);
              toWorker.flush();
              DataInputStream fromWorker = new DataInputStream(worker.getInputStream());
              int responseSize = fromWorker.readInt();
              if (responseSize <= 0 || responseSize > maxResponseBytes) {
                throw new IOException("gateway response exceeded limit");
              }
              response = fromWorker.readNBytes(responseSize);
              if (response.length != responseSize) throw new IOException("truncated gateway response");
            } catch (IOException backendFailure) {
              if (!socket.isClosed()) {
                // Exception details can contain peer-controlled data; log only a fixed category.
                LOGGER.log(System.Logger.Level.WARNING, "WASM worker loopback communication failed");
              }
              throw backendFailure;
            }
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(response.length);
            out.write(response);
            out.flush();
          } catch (IOException ignored) {
            // A malformed or disconnected client has no result channel. Never log guest bytes.
          } finally {
            if (deadline != null) deadline.cancel(false);
            admission.release();
          }
        });
      } catch (RejectedExecutionException closed) {
        admission.release();
        socket.close();
        if (listener.isClosed()) return;
        throw closed;
      }
    }
  }

  @Override public void close() throws IOException {
    listener.close();
    handlers.shutdownNow();
    deadlines.shutdownNow();
  }

  private static void closeQuietly(Socket socket) {
    if (socket == null) return;
    try { socket.close(); } catch (IOException ignored) { }
  }
}
