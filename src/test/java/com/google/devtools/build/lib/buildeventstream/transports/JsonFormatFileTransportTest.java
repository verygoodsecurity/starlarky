// Copyright 2016 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.buildeventstream.transports;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.devtools.build.lib.buildeventstream.ArtifactGroupNamer;
import com.google.devtools.build.lib.buildeventstream.BuildEvent;
import com.google.devtools.build.lib.buildeventstream.BuildEventContext;
import com.google.devtools.build.lib.buildeventstream.BuildEventProtocolOptions;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildStarted;
import com.google.devtools.build.lib.buildeventstream.LocalFilesArtifactUploader;
import com.google.devtools.build.lib.buildeventstream.PathConverter;
import com.google.devtools.common.options.Options;
import com.google.protobuf.util.JsonFormat;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/** Tests {@link TextFormatFileTransport}. * */
@RunWith(JUnit4.class)
public class JsonFormatFileTransportTest {
  private final BuildEventProtocolOptions defaultOpts =
      Options.getDefaults(BuildEventProtocolOptions.class);

  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  @Mock public BuildEvent buildEvent;

  @Mock public PathConverter pathConverter;
  @Mock public ArtifactGroupNamer artifactGroupNamer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void tearDown() {
    Mockito.validateMockitoUsage();
  }

  @Test
  public void testCreatesFileAndWritesProtoJsonFormat() throws Exception {
    File output = tmp.newFile();
    BufferedOutputStream outputStream =
        new BufferedOutputStream(Files.newOutputStream(Paths.get(output.getAbsolutePath())));

    BuildEventStreamProtos.BuildEvent started =
        BuildEventStreamProtos.BuildEvent.newBuilder()
            .setStarted(BuildStarted.newBuilder().setCommand("build"))
            .build();
    when(buildEvent.asStreamProto(ArgumentMatchers.<BuildEventContext>any())).thenReturn(started);
    JsonFormatFileTransport transport =
        new JsonFormatFileTransport(
            outputStream,
            defaultOpts,
            new LocalFilesArtifactUploader(),
            artifactGroupNamer);
    transport.sendBuildEvent(buildEvent);

    transport.close().get();
    try (Reader reader = new InputStreamReader(new FileInputStream(output))) {
      JsonFormat.Parser parser = JsonFormat.parser();
      BuildEventStreamProtos.BuildEvent.Builder builder =
          BuildEventStreamProtos.BuildEvent.newBuilder();
      parser.merge(reader, builder);
      assertThat(builder.build()).isEqualTo(started);
    }
  }

  /**
   * A thin wrapper around an OutputStream that counts number of bytes written and verifies flushes.
   *
   * <p>The methods below need to be syncrhonized because they override methods from {@link
   * BufferedOutputStream} *not* because there's a concurrent access to the stream.
   */
  private static final class WrappedOutputStream extends BufferedOutputStream {
    private long byteCount;
    private int flushCount;

    WrappedOutputStream(OutputStream out) {
      super(out);
      this.out = out;
    }

    @Override
    public synchronized void write(int b) throws IOException {
      out.write(b);
      byteCount++;
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
      out.write(b);
      byteCount += b.length;
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
      out.write(b, off, len);
      byteCount += len;
    }

    @Override
    public synchronized void flush() throws IOException {
      out.flush();
      flushCount++;
    }
  }

  @Test
  public void testFlushesStreamAfterSmallWrites() throws Exception {
    File output = tmp.newFile();
    BufferedOutputStream outputStream =
        new BufferedOutputStream(Files.newOutputStream(Paths.get(output.getAbsolutePath())));
    WrappedOutputStream wrappedOutputStream = new WrappedOutputStream(outputStream);

    BuildEventStreamProtos.BuildEvent started =
        BuildEventStreamProtos.BuildEvent.newBuilder()
            .setStarted(BuildStarted.newBuilder().setCommand("build"))
            .build();
    when(buildEvent.asStreamProto(ArgumentMatchers.<BuildEventContext>any())).thenReturn(started);

    JsonFormatFileTransport transport =
        new JsonFormatFileTransport(
            wrappedOutputStream, defaultOpts, new LocalFilesArtifactUploader(), artifactGroupNamer);

    transport.sendBuildEvent(buildEvent);
    Thread.sleep(transport.getFlushInterval().toMillis() * 3);

    // Some users, e.g. Tulsi, use JSON build event output for interactive use and expect the stream
    // to be flushed at regular short intervals.
    assertThat(wrappedOutputStream.flushCount).isGreaterThan(0);

    // We know that large writes get flushed; test is valuable only if we check small writes,
    // meaning smaller than 8192, the default buffer size used by BufferedOutputStream.
    assertThat(wrappedOutputStream.byteCount).isLessThan(8192L);
    assertThat(wrappedOutputStream.byteCount).isGreaterThan(0L);

    transport.close().get();
  }
}
