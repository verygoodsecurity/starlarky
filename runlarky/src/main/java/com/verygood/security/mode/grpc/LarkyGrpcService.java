package com.verygood.security.mode.grpc;

import com.google.common.collect.ImmutableMap;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.mode.grpc.LarkyProcessServiceGrpc.LarkyProcessServiceImplBase;

import java.util.Map;
import java.util.UUID;

import javax.script.Bindings;

import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;

public class LarkyGrpcService extends LarkyProcessServiceImplBase {

  private final LarkyScriptEngine engine = new LarkyScriptEngine();
  private static final String SCRIPT_OUTPUT_FORMAT = "script_output_%s";
  private static final String SCRIPT_INPUT_FORMAT = "script_input_%s";
  private static final String SCRIPT_CONTEXT_FORMAT = "script_context_%s";
  private static final String FUNCTION_HANDLER = "process";
  private static final String INVOKER_FORMAT = "%s\n%s = %s(%s, %s)";

  @SneakyThrows
  @Override
  public void process(LarkyProcessRequest request, StreamObserver<LarkyProcessResponse> responseObserver) {
    String executionID = UUID.randomUUID().toString().replace("-", "_");

    final String inputScript = request.getScript();
    final String inputPayload = request.getInput();
    Map<String, String> inputContext = new ContextMap<>(request.getContextMap());

    String output = String.format(SCRIPT_OUTPUT_FORMAT, executionID);
    String input = String.format(SCRIPT_INPUT_FORMAT, executionID);
    String context = String.format(SCRIPT_CONTEXT_FORMAT, executionID);

    String invocableScript = String.format(
        INVOKER_FORMAT, inputScript, output, FUNCTION_HANDLER, input, inputContext);

    final ImmutableMap<String, Object> scriptBindings = ImmutableMap.<String, Object>builder()
        .put(context, inputContext)
        .put(input, inputPayload)
        .build();

    final Bindings bindings = engine.createBindings();
    bindings.putAll(scriptBindings);

    engine.eval(invocableScript, bindings);
    String scriptOutput = (String) bindings.get(output);
    inputContext = (Map<String, String>) bindings.get(context);

    responseObserver.onNext(successResponse(scriptOutput, inputContext));
    responseObserver.onCompleted();
  }

  private LarkyProcessResponse successResponse(String payload, Map<String, String> context) {
    return LarkyProcessResponse.newBuilder()
        .setOutput(payload)
        .putAllContext(context)
        .build();
  }
}
