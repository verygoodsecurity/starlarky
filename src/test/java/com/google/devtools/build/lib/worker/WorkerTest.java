// Copyright 2020 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.worker;

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.build.lib.actions.ExecutionRequirements.WorkerProtocolFormat.JSON;
import static com.google.devtools.build.lib.actions.ExecutionRequirements.WorkerProtocolFormat.PROTO;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.HashCode;
import com.google.devtools.build.lib.actions.ExecutionRequirements.WorkerProtocolFormat;
import com.google.devtools.build.lib.sandbox.SandboxHelpers.SandboxInputs;
import com.google.devtools.build.lib.sandbox.SandboxHelpers.SandboxOutputs;
import com.google.devtools.build.lib.shell.Subprocess;
import com.google.devtools.build.lib.vfs.DigestHashFunction;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.inmemoryfs.InMemoryFileSystem;
import com.google.devtools.build.lib.worker.WorkerProtocol.Input;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse;
import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Worker}. */
@RunWith(JUnit4.class)
public final class WorkerTest {
  final FileSystem fs = new InMemoryFileSystem(DigestHashFunction.SHA256);

  /** A worker that uses a fake subprocess for I/O. */
  private static class TestWorker extends Worker {
    private final FakeSubprocess fakeSubprocess;

    public TestWorker(
        WorkerKey workerKey,
        int workerId,
        final Path workDir,
        Path logFile,
        FakeSubprocess fakeSubprocess) {
      super(workerKey, workerId, workDir, logFile);
      this.fakeSubprocess = fakeSubprocess;
    }

    @Override
    Subprocess createProcess() {
      return fakeSubprocess;
    }
  }

  private TestWorker workerForCleanup = null;

  @After
  public void destroyWorker() throws IOException {
    if (workerForCleanup != null) {
      workerForCleanup.destroy();
      workerForCleanup = null;
    }
  }

  private WorkerKey createWorkerKey(WorkerProtocolFormat protocolFormat) {
    return new WorkerKey(
        /* args= */ ImmutableList.of("arg1", "arg2", "arg3"),
        /* env= */ ImmutableMap.of("env1", "foo", "env2", "bar"),
        /* execRoot= */ fs.getPath("/outputbase/execroot/workspace"),
        /* mnemonic= */ "dummy",
        /* workerFilesCombinedHash= */ HashCode.fromInt(0),
        /* workerFilesWithHashes= */ ImmutableSortedMap.of(),
        /* mustBeSandboxed= */ true,
        /* proxied= */ true,
        protocolFormat);
  }

  /**
   * The {@link Worker} object uses a {@link Subprocess} to interact with persistent worker
   * binaries. Since this test is strictly testing {@link Worker} and not any outside persistent
   * worker binaries, a {@link FakeSubprocess} instance is used to fake the {@link InputStream} and
   * {@link OutputStream} that normally write and read from a persistent worker.
   */
  private static class FakeSubprocess implements Subprocess {
    private final ByteArrayInputStream inputStream;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayInputStream errStream = new ByteArrayInputStream(new byte[0]);
    private boolean wasDestroyed = false;

    public FakeSubprocess(byte[] bytes) throws IOException {
      inputStream = new ByteArrayInputStream(bytes);
    }

    @Override
    public InputStream getInputStream() {
      return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
      return outputStream;
    }

    @Override
    public InputStream getErrorStream() {
      return errStream;
    }

    @Override
    public boolean destroy() {
      for (Closeable stream : new Closeable[] {inputStream, outputStream, errStream}) {
        try {
          stream.close();
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }

      wasDestroyed = true;
      return true;
    }

    @Override
    public int exitValue() {
      return 0;
    }

    @Override
    public boolean finished() {
      return true;
    }

    @Override
    public boolean timedout() {
      return false;
    }

    @Override
    public void waitFor() throws InterruptedException {
      // Do nothing.
    }

    @Override
    public void close() {
      // Do nothing.
    }

    @Override
    public boolean isAlive() {
      return wasDestroyed;
    }
  }

  private static byte[] serializeResponseToProtoBytes(WorkResponse response) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    response.writeDelimitedTo(baos);
    return baos.toByteArray();
  }

  private TestWorker createTestWorker(byte[] outputStreamBytes, WorkerProtocolFormat protocolFormat)
      throws IOException {
    Preconditions.checkState(
        workerForCleanup == null, "createTestWorker can only be called once per test");

    WorkerKey key = createWorkerKey(protocolFormat);

    FakeSubprocess fakeSubprocess = new FakeSubprocess(outputStreamBytes);

    Path workerBaseDir = fs.getPath("/outputbase/bazel-workers");
    int workerId = 1;
    Path logFile = workerBaseDir.getRelative("test-log-file.log");

    TestWorker worker = new TestWorker(key, workerId, key.getExecRoot(), logFile, fakeSubprocess);

    SandboxInputs sandboxInputs = null;
    SandboxOutputs sandboxOutputs = null;
    worker.prepareExecution(sandboxInputs, sandboxOutputs, key.getWorkerFilesWithHashes().keySet());

    workerForCleanup = worker;

    return worker;
  }

  @Test
  public void testPutRequest_success() throws IOException {
    WorkRequest request = WorkRequest.getDefaultInstance();

    TestWorker testWorker = createTestWorker(new byte[0], PROTO);
    testWorker.putRequest(request);

    OutputStream stdout = testWorker.fakeSubprocess.getOutputStream();
    WorkRequest requestFromStdout =
        WorkRequest.parseDelimitedFrom(new ByteArrayInputStream(stdout.toString().getBytes(UTF_8)));

    assertThat(requestFromStdout).isEqualTo(request);
  }

  @Test
  public void testGetResponse_success() throws IOException {
    WorkResponse response = WorkResponse.getDefaultInstance();

    TestWorker testWorker = createTestWorker(serializeResponseToProtoBytes(response), PROTO);
    WorkResponse readResponse = testWorker.getResponse();

    assertThat(readResponse).isEqualTo(response);
  }

  @Test
  public void testPutRequest_json_success() throws IOException {
    TestWorker testWorker = createTestWorker(new byte[0], JSON);
    testWorker.putRequest(WorkRequest.getDefaultInstance());

    OutputStream stdout = testWorker.fakeSubprocess.getOutputStream();
    assertThat(stdout.toString()).isEqualTo("{}");
  }

  @Test
  public void testGetResponse_json_success() throws IOException {
    TestWorker testWorker = createTestWorker("{}".getBytes(UTF_8), JSON);
    WorkResponse readResponse = testWorker.getResponse();
    WorkResponse response = WorkResponse.getDefaultInstance();

    assertThat(readResponse).isEqualTo(response);
  }

  @Test
  public void testPutRequest_json_populatedFields_success() throws IOException {
    WorkRequest request =
        WorkRequest.newBuilder()
            .addArguments("testRequest")
            .addInputs(
                Input.newBuilder()
                    .setPath("testPath")
                    .setDigest(ByteString.copyFromUtf8("testDigest"))
                    .build())
            .setRequestId(1)
            .build();

    TestWorker testWorker = createTestWorker(new byte[0], JSON);
    testWorker.putRequest(request);

    OutputStream stdout = testWorker.fakeSubprocess.getOutputStream();
    String requestJsonString =
        "{\"arguments\":[\"testRequest\"],\"inputs\":"
            + "[{\"path\":\"testPath\",\"digest\":\"dGVzdERpZ2VzdA==\"}],\"requestId\":1}";
    assertThat(stdout.toString()).isEqualTo(requestJsonString);
  }

  @Test
  public void testGetResponse_json_populatedFields_success() throws IOException {
    TestWorker testWorker =
        createTestWorker(
            "{\"exitCode\":1,\"output\":\"test output\",\"requestId\":1}".getBytes(UTF_8), JSON);
    WorkResponse readResponse = testWorker.getResponse();
    WorkResponse response =
        WorkResponse.newBuilder().setExitCode(1).setOutput("test output").setRequestId(1).build();

    assertThat(readResponse).isEqualTo(response);
  }

  private void verifyGetResponseFailure(String responseString, String expectedError)
      throws IOException {
    TestWorker testWorker = createTestWorker(responseString.getBytes(UTF_8), JSON);
    IOException ex = assertThrows(IOException.class, testWorker::getResponse);
    assertThat(ex).hasMessageThat().contains(expectedError);
  }

  @Test
  public void testGetResponse_json_emptyString_throws() throws IOException {
    verifyGetResponseFailure("", "Could not parse json work request correctly");
  }

  @Test
  public void testGetResponse_badJson_throws() throws IOException {
    verifyGetResponseFailure(
        "{ \"output\": \"I'm missing a bracket\"", "Could not parse json work request correctly");
  }

  @Test
  public void testGetResponse_json_multipleExitCode_fails() throws IOException {
    verifyGetResponseFailure(
        "{\"exitCode\":1,\"exitCode\":1}", "Work response cannot have more than one exit code");
  }

  @Test
  public void testGetResponse_json_multipleOutput_fails() throws IOException {
    verifyGetResponseFailure(
        "{\"output\":\"\",\"output\":\"\"}", "Work response cannot have more than one output");
  }

  @Test
  public void testGetResponse_json_multipleRequestId_fails() throws IOException {
    verifyGetResponseFailure(
        "{\"requestId\":0,\"requestId\":0}", "Work response cannot have more than one requestId");
  }

  @Test
  public void testGetResponse_json_incorrectFields_fails() throws IOException {
    verifyGetResponseFailure(
        "{\"testField\":0}", "testField is an incorrect field in work response");
  }
}
