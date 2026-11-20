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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class CappedOutputStreamTest {

  @Test
  public void singleByteWritesStopAtTheLimit() {
    CappedOutputStream out = new CappedOutputStream("stdout", 1000);
    for (int i = 0; i < 1000; i++) {
      out.write(i);
    }
    assertThrows(CappedOutputStream.OutputLimitExceeded.class, () -> out.write(0));
    assertThat(out.exceeded()).isTrue();
    byte[] written = out.toByteArray();
    assertThat(written).hasLength(1000);
    assertThat(written[999]).isEqualTo((byte) 999);
  }

  @Test
  public void zeroLimitRejectsTheFirstByte() {
    CappedOutputStream out = new CappedOutputStream("stderr", 0);
    assertThrows(CappedOutputStream.OutputLimitExceeded.class, () -> out.write(1));
    assertThat(out.toByteArray()).isEmpty();
  }
}
