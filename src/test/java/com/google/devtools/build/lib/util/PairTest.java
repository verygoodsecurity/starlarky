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
package com.google.devtools.build.lib.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link Pair}.
 */
@RunWith(JUnit4.class)
public class PairTest {

  @Test
  public void constructor() {
    Object a = new Object();
    Object b = new Object();
    Pair<Object, Object> p = Pair.of(a, b);
    assertThat(p.first).isSameInstanceAs(a);
    assertThat(p.second).isSameInstanceAs(b);
    assertThat(p).isEqualTo(Pair.of(a, b));
    assertThat(p.hashCode()).isEqualTo(31 * a.hashCode() + b.hashCode());
  }

  @Test
  public void nullable() {
    Pair<Object, Object> p = Pair.of(null, null);
    assertThat(p.first).isNull();
    assertThat(p.second).isNull();
    p.hashCode(); // Should not throw.
    new EqualsTester().addEqualityGroup(p).testEquals();
  }
}
