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
package com.google.devtools.build.lib.packages;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.devtools.build.lib.analysis.config.ConfigMatchingProvider;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.BuildType.Selector;
import com.google.devtools.build.lib.packages.BuildType.SelectorList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;

/**
 * {@link AttributeMap} implementation that binds a rule's attribute as follows:
 *
 * <ol>
 *   <li>If the attribute is selectable (i.e. its BUILD declaration is of the form
 *   "attr = { config1: "value1", "config2: "value2", ... }", returns the subset of values
 *   chosen by the current configuration in accordance with Bazel's documented policy on
 *   configurable attribute selection.
 *   <li>If the attribute is not selectable (i.e. its value is static), returns that value with
 *   no additional processing.
 * </ol>
 *
 * <p>Example usage:
 * <pre>
 *   Label fooLabel = ConfiguredAttributeMapper.of(ruleConfiguredTarget).get("foo", Type.LABEL);
 * </pre>
 */
public class ConfiguredAttributeMapper extends AbstractAttributeMapper {

  private final Map<Label, ConfigMatchingProvider> configConditions;
  private Rule rule;

  private ConfiguredAttributeMapper(Rule rule,
      ImmutableMap<Label, ConfigMatchingProvider> configConditions) {
    super(Preconditions.checkNotNull(rule));
    this.configConditions = configConditions;
    this.rule = rule;
  }

  /**
   * "Manual" constructor that requires the caller to pass the set of configurability conditions
   * that trigger this rule's configurable attributes.
   *
   * <p>If you don't know how to do this, you really want to use one of the "do-it-all"
   * constructors.
   */
  public static ConfiguredAttributeMapper of(
      Rule rule, ImmutableMap<Label, ConfigMatchingProvider> configConditions) {
    return new ConfiguredAttributeMapper(rule, configConditions);
  }

  /**
   * Checks that all attributes can be mapped to their configured values. This is
   * useful for checking that the configuration space in a configured attribute doesn't
   * contain unresolvable contradictions.
   *
   * @throws EvalException if any attribute's value can't be resolved under this mapper
   */
  public void validateAttributes() throws EvalException {
    for (String attrName : getAttributeNames()) {
      getAndValidate(attrName, getAttributeType(attrName));
    }
  }

  /**
   * Variation of {@link #get} that throws an informative exception if the attribute can't be
   * resolved due to intrinsic contradictions in the configuration.
   */
  @SuppressWarnings("unchecked")
  private <T> T getAndValidate(String attributeName, Type<T> type) throws EvalException {
    SelectorList<T> selectorList = getSelectorList(attributeName, type);
    if (selectorList == null) {
      // This is a normal attribute.
      return super.get(attributeName, type);
    }

    List<T> resolvedList = new ArrayList<>();
    for (Selector<T> selector : selectorList.getSelectors()) {
      ConfigKeyAndValue<T> resolvedPath = resolveSelector(attributeName, selector);
      if (!selector.isValueSet(resolvedPath.configKey)) {
        // Use the default. We don't have access to the rule here, so pass null to
        // Attribute.getValue(). This has the result of making attributes with condition
        // predicates ineligible for "None" values. But no user-facing attributes should
        // do that anyway, so that isn't a loss.
        Attribute attr = getAttributeDefinition(attributeName);
        if (attr.isMandatory()) {
          throw new EvalException(
              rule.getLocation(),
              String.format(
                  "Mandatory attribute '%s' resolved to 'None' after evaluating 'select'"
                      + " expression",
                  attributeName));
        }
        Verify.verify(attr.getCondition() == Predicates.<AttributeMap>alwaysTrue());
        resolvedList.add((T) attr.getDefaultValue(null)); // unchecked cast
      } else {
        resolvedList.add(resolvedPath.value);
      }
    }
    return resolvedList.size() == 1 ? resolvedList.get(0) : type.concat(resolvedList);
  }

  private static class ConfigKeyAndValue<T> {
    final Label configKey;
    final T value;
    /** If null, this means the default condition (doesn't correspond to a config_setting). * */
    @Nullable final ConfigMatchingProvider provider;

    ConfigKeyAndValue(Label key, T value, @Nullable ConfigMatchingProvider provider) {
      this.configKey = key;
      this.value = value;
      this.provider = provider;
    }
  }

  private <T> ConfigKeyAndValue<T> resolveSelector(String attributeName, Selector<T> selector)
      throws EvalException {
    Map<Label, ConfigKeyAndValue<T>> matchingConditions = new LinkedHashMap<>();
    Set<Label> conditionLabels = new LinkedHashSet<>();
    ConfigKeyAndValue<T> matchingResult = null;

    // Find the matching condition and record its value (checking for duplicates).
    for (Map.Entry<Label, T> entry : selector.getEntries().entrySet()) {
      Label selectorKey = entry.getKey();
      if (BuildType.Selector.isReservedLabel(selectorKey)) {
        continue;
      }

      ConfigMatchingProvider curCondition = configConditions.get(
          rule.getLabel().resolveRepositoryRelative(selectorKey));
      if (curCondition == null) {
        // This can happen if the rule is in error
        continue;
      }
      conditionLabels.add(selectorKey);

      if (curCondition.matches()) {
        // We keep track of all matches which are more precise than any we have found so far.
        // Therefore, we remove any previous matches which are strictly less precise than this
        // one, and only add this one if none of the previous matches are more precise.
        // It is an error if we do not end up with only one most-precise match.
        boolean suppressed = false;
        Iterator<Map.Entry<Label, ConfigKeyAndValue<T>>> it =
            matchingConditions.entrySet().iterator();
        while (it.hasNext()) {
          ConfigMatchingProvider existingMatch = it.next().getValue().provider;
          if (curCondition.refines(existingMatch)) {
            it.remove();
          } else if (existingMatch.refines(curCondition)) {
            suppressed = true;
            break;
          }
        }
        if (!suppressed) {
          matchingConditions.put(
              selectorKey, new ConfigKeyAndValue<>(selectorKey, entry.getValue(), curCondition));
        }
      }
    }

    if (matchingConditions.size() > 1) {
      throw new EvalException(
          rule.getLocation(),
          "Illegal ambiguous match on configurable attribute \""
              + attributeName
              + "\" in "
              + getLabel()
              + ":\n"
              + Joiner.on("\n").join(matchingConditions.keySet())
              + "\nMultiple matches are not allowed unless one is unambiguously more specialized.");
    } else if (matchingConditions.size() == 1) {
      matchingResult = Iterables.getOnlyElement(matchingConditions.values());
    }

    // If nothing matched, choose the default condition.
    if (matchingResult == null) {
      if (!selector.hasDefault()) {
        String noMatchMessage =
            "Configurable attribute \"" + attributeName + "\" doesn't match this configuration";
        if (!selector.getNoMatchError().isEmpty()) {
          noMatchMessage += ": " + selector.getNoMatchError();
        } else {
          noMatchMessage += " (would a default condition help?).\nConditions checked:\n "
              + Joiner.on("\n ").join(conditionLabels);
        }
        throw new EvalException(rule.getLocation(), noMatchMessage);
      }
      matchingResult =
          selector.hasDefault()
              ? new ConfigKeyAndValue<>(
                  Selector.DEFAULT_CONDITION_LABEL, selector.getDefault(), null)
              : null;
    }

    return matchingResult;
  }

  @Override
  public <T> T get(String attributeName, Type<T> type) {
    try {
      return getAndValidate(attributeName, type);
    } catch (EvalException e) {
      // Callers that reach this branch should explicitly validate the attribute through an
      // appropriate call and handle the exception directly. This method assumes
      // pre-validated attributes.
      throw new IllegalStateException(
          "lookup failed on attribute " + attributeName + ": " + e.getMessage());
    }
  }

  @Override
  public boolean isAttributeValueExplicitlySpecified(String attributeName) {
    SelectorList<?> selectorList = getSelectorList(attributeName, getAttributeType(attributeName));
    if (selectorList == null) {
      // This is a normal attribute.
      return super.isAttributeValueExplicitlySpecified(attributeName);
    }
    for (Selector<?> selector : selectorList.getSelectors()) {
      try {
        ConfigKeyAndValue<?> resolvedPath = resolveSelector(attributeName, selector);
        if (selector.isValueSet(resolvedPath.configKey)) {
          return true;
        }
      } catch (EvalException e) {
        // This will trigger an error via any other call, so the actual return doesn't matter much.
        return true;
      }
    }
    return false; // Every select() in this list chooses a path with value "None".
  }
}
