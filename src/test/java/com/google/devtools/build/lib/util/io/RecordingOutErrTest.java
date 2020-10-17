// Copyright 2014 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.util.io;

import static com.google.common.truth.Truth.assertThat;

import java.io.PrintWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * A test for {@link RecordingOutErr}.
 */
@RunWith(JUnit4.class)
public class RecordingOutErrTest {

  protected RecordingOutErr getRecordingOutErr() {
    return new RecordingOutErr();
  }

  @Test
  public void testRecordingOutErrRecords() {
    RecordingOutErr outErr = getRecordingOutErr();

    outErr.printOut("Test");
    outErr.printOutLn("out1");
    PrintWriter writer = new PrintWriter(outErr.getOutputStream());
    writer.println("Testout2");
    writer.flush();

    outErr.printErr("Test");
    outErr.printErrLn("err1");
    writer = new PrintWriter(outErr.getErrorStream());
    writer.println("Testerr2");
    writer.flush();

    assertThat(outErr.outAsLatin1()).isEqualTo("Testout1\nTestout2\n");
    assertThat(outErr.errAsLatin1()).isEqualTo("Testerr1\nTesterr2\n");

    assertThat(outErr.hasRecordedOutput()).isTrue();

    outErr.reset();

    assertThat(outErr.outAsLatin1()).isEmpty();
    assertThat(outErr.errAsLatin1()).isEmpty();
    assertThat(outErr.hasRecordedOutput()).isFalse();
  }

}
