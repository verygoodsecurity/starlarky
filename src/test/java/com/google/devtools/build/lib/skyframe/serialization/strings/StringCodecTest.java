// Copyright 2017 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.skyframe.serialization.strings;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.skyframe.serialization.ObjectCodec;
import com.google.devtools.build.lib.skyframe.serialization.ObjectCodecRegistry;
import com.google.devtools.build.lib.skyframe.serialization.UnsafeJdk9StringCodec;
import com.google.devtools.build.lib.skyframe.serialization.testutils.SerializationTester;
import com.google.devtools.build.lib.skyframe.serialization.testutils.TestUtils;
import com.google.protobuf.ByteString;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Basic tests for {@link StringCodec} or {@link UnsafeJdk9StringCodec}. */
@RunWith(JUnit4.class)
public class StringCodecTest {
  @Test
  public void testCodec() throws Exception {
    new SerializationTester("usually precomputed and supports weird unicodes: （╯°□°）╯︵┻━┻ ", "")
        .runTests();
  }

  @Test
  public void sizeOk() throws Exception {
    ObjectCodec<String> slowCodec = new StringCodec();
    ObjectCodec<String> fastCodec =
        UnsafeJdk9StringCodec.canUseUnsafeCodec() ? new UnsafeJdk9StringCodec() : slowCodec;
    for (String str :
        ImmutableList.of(
            "//a/b/c/d/e/f/g/h/ijklmn:opqrstuvw.xyz",
            "java/com/google/devtools/build/lib/util/more/strings",
            "java/com/google/devtools/build/lib/util/more/strings/náme_with_àccent")) {
      ByteString withSimple =
          TestUtils.toBytesMemoized(str, new ObjectCodecRegistry.Builder().add(slowCodec).build());
      ByteString withUnsafe =
          TestUtils.toBytesMemoized(str, new ObjectCodecRegistry.Builder().add(fastCodec).build());
      assertWithMessage(str + " too big").that(withUnsafe.size()).isAtMost(withSimple.size());
    }
  }
}
