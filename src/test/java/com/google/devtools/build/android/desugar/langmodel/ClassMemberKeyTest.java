/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.build.android.desugar.langmodel;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ClassMemberKey}. */
@RunWith(JUnit4.class)
public class ClassMemberKeyTest {

  @Test
  public void fieldKey_bridgeOfInstanceRead() {
    FieldKey fieldKey =
        FieldKey.create(ClassName.create("a/b/Charlie"), "instanceFieldOfLongType", "J");
    assertThat(fieldKey.bridgeOfInstanceRead())
        .isEqualTo(
            MethodKey.create(
                ClassName.create("a/b/Charlie"),
                "instanceFieldOfLongType$bridge_getter",
                "(La/b/Charlie;)J"));
  }

  @Test
  public void fieldKey_bridgeOfInstanceWrite() {

    FieldKey fieldKey =
        FieldKey.create(ClassName.create("a/b/Charlie"), "instanceFieldOfLongType", "J");
    assertThat(fieldKey.bridgeOfInstanceWrite())
        .isEqualTo(
            MethodKey.create(
                ClassName.create("a/b/Charlie"),
                "instanceFieldOfLongType$bridge_setter",
                "(La/b/Charlie;J)J"));
  }

  @Test
  public void fieldKey_bridgeOfStaticRead() {
    FieldKey fieldKey =
        FieldKey.create(ClassName.create("a/b/Charlie"), "staticFieldOfLongType", "J");
    assertThat(fieldKey.bridgeOfStaticRead())
        .isEqualTo(
            MethodKey.create(
                ClassName.create("a/b/Charlie"), "staticFieldOfLongType$bridge_getter", "()J"));
  }

  @Test
  public void fieldKey_bridgeOfStaticWrite() {
    FieldKey fieldKey =
        FieldKey.create(ClassName.create("a/b/Charlie"), "staticFieldOfLongType", "J");
    assertThat(fieldKey.bridgeOfStaticWrite())
        .isEqualTo(
            MethodKey.create(
                ClassName.create("a/b/Charlie"), "staticFieldOfLongType$bridge_setter", "(J)J"));
  }

  @Test
  public void methodKey_bridgeOfClassInstanceMethod() {
    MethodKey methodKey = MethodKey.create(ClassName.create("a/b/Charlie"), "twoLongSum", "(JJ)J");
    assertThat(methodKey.bridgeOfClassInstanceMethod())
        .isEqualTo(
            MethodKey.create(
                ClassName.create("a/b/Charlie"), "twoLongSum$bridge", "(La/b/Charlie;JJ)J"));
  }

  @Test
  public void methodKey_bridgeOfClassStaticMethod() {
    MethodKey methodKey = MethodKey.create(ClassName.create("a/b/Charlie"), "twoLongSum", "(JJ)J");
    assertThat(methodKey.bridgeOfClassStaticMethod())
        .isEqualTo(MethodKey.create(ClassName.create("a/b/Charlie"), "twoLongSum$bridge", "(JJ)J"));
  }

  @Test
  public void methodKey_bridgeOfConstructor() {
    MethodKey methodKey = MethodKey.create(ClassName.create("a/b/Charlie"), "<init>", "(JJ)V");
    assertThat(methodKey.bridgeOfConstructor(ClassName.create("a/b/Charlie$NestCC")))
        .isEqualTo(
            MethodKey.create(
                ClassName.create("a/b/Charlie"), "<init>", "(JJLa/b/Charlie$NestCC;)V"));
  }

  @Test
  public void methodKey_substituteOfInterfaceInstanceMethod() {
    MethodKey methodKey =
        MethodKey.create(ClassName.create("a/b/Charlie"), "instanceInstanceMethod", "(JJ)J");
    assertThat(methodKey.substituteOfInterfaceInstanceMethod())
        .isEqualTo(
            MethodKey.create(
                ClassName.create("a/b/Charlie"), "instanceInstanceMethod", "(La/b/Charlie;JJ)J"));
  }

  @Test
  public void methodKey_substituteOfInterfaceStaticMethod() {

    MethodKey methodKey =
        MethodKey.create(ClassName.create("a/b/Charlie"), "instanceStaticMethod", "(JJ)J");
    assertThat(methodKey.substituteOfInterfaceStaticMethod())
        .isEqualTo(
            MethodKey.create(ClassName.create("a/b/Charlie"), "instanceStaticMethod", "(JJ)J"));
  }
}
