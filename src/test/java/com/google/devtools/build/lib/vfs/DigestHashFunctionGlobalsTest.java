// Copyright 2018 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.vfs;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.hash.Hashing;
import com.google.devtools.build.lib.vfs.DigestHashFunction.DigestFunctionConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for DigestHashFunction, notably that the static instances can be compared with reference
 * equality.
 */
@RunWith(JUnit4.class)
public class DigestHashFunctionGlobalsTest {
  private final DigestFunctionConverter converter = new DigestFunctionConverter();

  @Test
  public void convertReturnsTheSameValueAsTheConstant() throws Exception {
    assertThat(converter.convert("sha-256")).isSameInstanceAs(DigestHashFunction.SHA256);
    assertThat(converter.convert("SHA-256")).isSameInstanceAs(DigestHashFunction.SHA256);
    assertThat(converter.convert("SHA256")).isSameInstanceAs(DigestHashFunction.SHA256);
    assertThat(converter.convert("sha256")).isSameInstanceAs(DigestHashFunction.SHA256);

    assertThat(converter.convert("SHA-1")).isSameInstanceAs(DigestHashFunction.SHA1);
    assertThat(converter.convert("sha-1")).isSameInstanceAs(DigestHashFunction.SHA1);
    assertThat(converter.convert("SHA1")).isSameInstanceAs(DigestHashFunction.SHA1);
    assertThat(converter.convert("sha1")).isSameInstanceAs(DigestHashFunction.SHA1);
  }

  @Test
  public void lateRegistrationGetsPickedUpByConverter() throws Exception {
    DigestHashFunction.register(Hashing.goodFastHash(32), "SHA-512");

    assertThat(converter.convert("SHA-512")).isSameInstanceAs(converter.convert("sha-512"));
  }

  @Test
  public void lateRegistrationWithAlternativeNamesGetsPickedUpByConverter() throws Exception {
    DigestHashFunction.register(Hashing.goodFastHash(64), "SHA-384", "SHA384", "SHA_384");

    assertThat(converter.convert("SHA-384")).isSameInstanceAs(converter.convert("SHA-384"));
    assertThat(converter.convert("Sha-384")).isSameInstanceAs(converter.convert("SHA-384"));
    assertThat(converter.convert("sha-384")).isSameInstanceAs(converter.convert("SHA-384"));

    assertThat(converter.convert("SHA384")).isSameInstanceAs(converter.convert("SHA-384"));
    assertThat(converter.convert("Sha384")).isSameInstanceAs(converter.convert("SHA-384"));
    assertThat(converter.convert("sha384")).isSameInstanceAs(converter.convert("SHA-384"));

    assertThat(converter.convert("SHA_384")).isSameInstanceAs(converter.convert("SHA-384"));
    assertThat(converter.convert("Sha_384")).isSameInstanceAs(converter.convert("SHA-384"));
    assertThat(converter.convert("sha_384")).isSameInstanceAs(converter.convert("SHA-384"));
  }
}
