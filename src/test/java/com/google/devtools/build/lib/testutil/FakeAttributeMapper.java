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
package com.google.devtools.build.lib.testutil;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.Attribute;
import com.google.devtools.build.lib.packages.AttributeMap;
import com.google.devtools.build.lib.packages.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** Faked implementation of {@link AttributeMap} for use in testing. */
public class FakeAttributeMapper implements AttributeMap {
  private final Map<String, FakeAttributeMapperEntry<?>> attrs;

  private FakeAttributeMapper(Map<String, FakeAttributeMapperEntry<?>> attrs) {
    this.attrs = ImmutableMap.copyOf(attrs);
  }

  @Override
  public String getName() {
    return "name";
  }

  @Override
  public Label getLabel() {
    return Label.parseAbsoluteUnchecked("//fake:rule");
  }

  @Override
  public String getRuleClassName() {
    return "fake_rule";
  }

  @Override
  public boolean has(String attrName) {
    return attrs.containsKey(attrName);
  }

  @Override
  public <T> boolean has(String attrName, Type<T> type) {
    FakeAttributeMapperEntry<?> entry = attrs.get(attrName);
    if (entry == null) {
      return false;
    }
    return entry.type.equals(type);
  }

  @Override
  @Nullable
  public <T> T get(String attributeName, Type<T> type) {
    FakeAttributeMapperEntry<?> entry = attrs.get(attributeName);
    if (entry == null) {
      // Not specified in attributes or defaults
      assertWithMessage("Attribute " + attributeName + " not in attributes!").fail();
      return null;
    }

    return entry.validateAndGet(type);
  }

  @Override
  public boolean isConfigurable(String attributeName) {
    return false;
  }

  @Override
  public Iterable<String> getAttributeNames() {
    return attrs.keySet();
  }

  @Nullable
  @Override
  public Type<?> getAttributeType(String attrName) {
    FakeAttributeMapperEntry<?> entry = attrs.get(attrName);
    return entry == null ? null : entry.type;
  }

  @Nullable
  @Override
  public Attribute getAttributeDefinition(String attrName) {
    return null;
  }

  @Override
  public boolean isAttributeValueExplicitlySpecified(String attributeName) {
    return attrs.containsKey(attributeName);
  }

  @Override
  public Collection<DepEdge> visitLabels() throws InterruptedException {
    return ImmutableList.of();
  }

  @Override
  public Collection<DepEdge> visitLabels(Attribute attribute) throws InterruptedException {
    return ImmutableList.of();
  }

  @Override
  public String getPackageDefaultHdrsCheck() {
    return "???";
  }

  @Override
  public Boolean getPackageDefaultTestOnly() {
    return false;
  }

  @Override
  public String getPackageDefaultDeprecation() {
    return "???";
  }

  @Override
  public ImmutableList<String> getPackageDefaultCopts() {
    return ImmutableList.of();
  }

  public static FakeAttributeMapper empty() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to construct a {@link FakeAttributeMapper}. If no attributes are needed, use {@link
   * #empty()} instead.
   */
  public static class Builder {
    private final ImmutableMap.Builder<String, FakeAttributeMapperEntry<?>> mapBuilder =
        ImmutableMap.builder();

    private Builder() { }

    public Builder withStringList(String attribute, List<String> value) {
      mapBuilder.put(attribute, FakeAttributeMapperEntry.forStringList(value));
      return this;
    }

    public FakeAttributeMapper build() {
      return new FakeAttributeMapper(mapBuilder.build());
    }
  }

  private static class FakeAttributeMapperEntry<T> {
    private final Type<T> type;
    private final T value;

    private FakeAttributeMapperEntry(Type<T> type, T value) {
      this.type = type;
      this.value = value;
    }

    private static FakeAttributeMapperEntry<List<String>> forStringList(List<String> list) {
      return new FakeAttributeMapperEntry<>(Type.STRING_LIST, list);
    }

    private <U> U validateAndGet(Type<U> otherType) {
      assertThat(type).isSameInstanceAs(otherType);
      return otherType.cast(value);
    }
  }
}
