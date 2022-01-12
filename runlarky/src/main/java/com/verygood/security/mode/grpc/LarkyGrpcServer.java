package com.verygood.security.mode.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.SneakyThrows;

public class LarkyGrpcServer {

  private final Server server;

  public LarkyGrpcServer(int port) {
    server = ServerBuilder
        .forPort(8080)
        .addService(new LarkyGrpcService()).build();
  }

  @SneakyThrows
  public void serve() {
    server.start();
    server.awaitTermination();
  }
}
