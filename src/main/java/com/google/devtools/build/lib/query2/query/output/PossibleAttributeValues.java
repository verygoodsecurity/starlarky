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

package com.google.devtools.build.lib.query2.query.output;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.devtools.build.lib.packages.AggregatingAttributeMapper;
import com.google.devtools.build.lib.packages.Attribute;
import com.google.devtools.build.lib.packages.BuildType;
import com.google.devtools.build.lib.packages.Rule;
import java.util.Iterator;

/** Encapsulates possible values for an attribute. */
public class PossibleAttributeValues implements Iterable<Object> {
  private final Iterable<Object> values;
  private final AttributeValueSource source;

  public PossibleAttributeValues(Iterable<Object> values, AttributeValueSource source) {
    this.values = values;
    this.source = source;
  }

  AttributeValueSource getSource() {
    return source;
  }

  @Override
  public Iterator<Object> iterator() {
    return values.iterator();
  }

  /**
   * Returns the possible values of the specified attribute in the specified rule. For simple
   * attributes, this is a single value. For configurable and computed attributes, this may be a
   * list of values. See {@link AggregatingAttributeMapper#visitAttribute} for how the values are
   * determined.
   *
   * <p>This applies an important optimization for label lists: instead of returning all possible
   * values, it only returns possible <i>labels</i>. For example, given:
   *
   * <pre>
   * select({
   *     ":c": ["//a:one", "//a:two"],
   *     ":d": ["//a:two"]
   *     })</pre>
   *
   * it returns:
   *
   * <pre>["//a:one", "//a:two"]</pre>
   *
   * which loses track of which label appears in which branch.
   *
   * <p>This avoids the memory overruns that can happen be iterating over every possible value for
   * an <code>attr = select(...) + select(...) + select(...) + ...</code> expression. Query
   * operations generally don't care about specific attribute values - they just care which labels
   * are possible.
   */
  static PossibleAttributeValues forRuleAndAttribute(Rule rule, Attribute attr) {
    AttributeValueSource source = AttributeValueSource.forRuleAndAttribute(rule, attr);
    AggregatingAttributeMapper attributeMap = AggregatingAttributeMapper.of(rule);
    Iterable<?> list;
    if (attr.getType().equals(BuildType.LABEL_LIST)
        && attributeMap.isConfigurable(attr.getName())) {
      // TODO(gregce): Expand this to all collection types (we don't do this for scalars because
      // there's currently no syntax for expressing multiple scalar values). This unfortunately
      // isn't trivial because Bazel's label visitation logic includes special methods built
      // directly into Type.
      return new PossibleAttributeValues(
          ImmutableList.<Object>of(
              attributeMap.getReachableLabels(attr.getName(), /*includeSelectKeys=*/ false)),
          source);
    } else if ((list =
            attributeMap.getConcatenatedSelectorListsOfListType(
                attr.getName(), attr.getType()))
        != null) {
      return new PossibleAttributeValues(Lists.newArrayList(list), source);
    } else {
      // The call to visitAttributes below is especially slow with selector lists.
      @SuppressWarnings("unchecked") // Casting Iterable<T> -> Iterable<Object>
      Iterable<Object> possibleValues =
          (Iterable<Object>) attributeMap.visitAttribute(attr.getName(), attr.getType());
      return new PossibleAttributeValues(possibleValues, source);
    }
  }
}