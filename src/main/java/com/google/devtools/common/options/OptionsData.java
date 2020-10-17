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

package com.google.devtools.common.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * This extends IsolatedOptionsData with information that can only be determined once all the {@link
 * OptionsBase} subclasses for a parser are known. In particular, this includes expansion
 * information.
 */
@Immutable
final class OptionsData extends IsolatedOptionsData {

  /** Mapping from each option to the (unparsed) options it expands to, if any. */
  private final ImmutableMap<OptionDefinition, ImmutableList<String>> evaluatedExpansions;

  /** Construct {@link OptionsData} by extending an {@link IsolatedOptionsData} with new info. */
  private OptionsData(
      IsolatedOptionsData base, Map<OptionDefinition, ImmutableList<String>> evaluatedExpansions) {
    super(base);
    this.evaluatedExpansions = ImmutableMap.copyOf(evaluatedExpansions);
  }

  private static final ImmutableList<String> EMPTY_EXPANSION = ImmutableList.<String>of();

  /**
   * Returns the expansion of an options field, regardless of whether it was defined using {@link
   * Option#expansion} or {@link Option#expansionFunction}. If the field is not an expansion option,
   * returns an empty array.
   */
  public ImmutableList<String> getEvaluatedExpansion(OptionDefinition optionDefinition) {
    ImmutableList<String> result = evaluatedExpansions.get(optionDefinition);
    return result != null ? result : EMPTY_EXPANSION;
  }

  /**
   * Constructs an {@link OptionsData} object for a parser that knows about the given {@link
   * OptionsBase} classes. In addition to the work done to construct the {@link
   * IsolatedOptionsData}, this also computes expansion information. If an option has static
   * expansions or uses an expansion function that takes a Void object, try to precalculate the
   * expansion here.
   */
  static OptionsData from(Collection<Class<? extends OptionsBase>> classes) {
    IsolatedOptionsData isolatedData = IsolatedOptionsData.from(classes);

    // All that's left is to compute expansions.
    ImmutableMap.Builder<OptionDefinition, ImmutableList<String>> evaluatedExpansionsBuilder =
        ImmutableMap.builder();
    for (Map.Entry<String, OptionDefinition> entry : isolatedData.getAllOptionDefinitions()) {
      OptionDefinition optionDefinition = entry.getValue();
      // Determine either the hard-coded expansion, or the ExpansionFunction class. The
      // OptionProcessor checks at compile time that these aren't used together.
      String[] constExpansion = optionDefinition.getOptionExpansion();
      Class<? extends ExpansionFunction> expansionFunctionClass =
          optionDefinition.getExpansionFunction();
      if (constExpansion.length > 0) {
        evaluatedExpansionsBuilder.put(optionDefinition, ImmutableList.copyOf(constExpansion));
      } else if (optionDefinition.usesExpansionFunction()) {
        if (Modifier.isAbstract(expansionFunctionClass.getModifiers())) {
          throw new AssertionError(
              "The expansionFunction type " + expansionFunctionClass + " must be a concrete type");
        }
        // Evaluate the ExpansionFunction.
        ExpansionFunction instance;
        try {
          Constructor<?> constructor = expansionFunctionClass.getConstructor();
          instance = (ExpansionFunction) constructor.newInstance();
        } catch (Exception e) {
          // This indicates an error in the ExpansionFunction, and should be discovered the first
          // time it is used.
          throw new AssertionError(e);
        }
        ImmutableList<String> expansion = instance.getExpansion(isolatedData);
        evaluatedExpansionsBuilder.put(optionDefinition, expansion);
      }
    }
    return new OptionsData(isolatedData, evaluatedExpansionsBuilder.build());
  }
}
