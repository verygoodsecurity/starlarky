// Copyright 2015 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.skyframe;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link EmptySkyValue}. */
@RunWith(JUnit4.class)
public class EmptySkyValueTest {

  @Test
  public void testNotSerializable() throws IOException {
    ObjectOutputStream objOut = new ObjectOutputStream(new ByteArrayOutputStream());

    UnsupportedOperationException e =
        assertThrows(
            UnsupportedOperationException.class, () -> objOut.writeObject(EmptySkyValue.INSTANCE));
    assertThat(e).hasMessageThat().isEqualTo("Java serialization not supported");
  }

}

