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

package com.google.devtools.build.lib.rules.platform;

import static com.google.devtools.build.lib.packages.Attribute.attr;

import com.google.devtools.build.lib.analysis.RuleDefinition;
import com.google.devtools.build.lib.analysis.RuleDefinitionEnvironment;
import com.google.devtools.build.lib.analysis.platform.ConstraintSettingInfo;
import com.google.devtools.build.lib.analysis.platform.ConstraintValueInfo;
import com.google.devtools.build.lib.packages.BuildType;
import com.google.devtools.build.lib.packages.RuleClass;
import com.google.devtools.build.lib.util.FileTypeSet;

/** Rule definition for {@link ConstraintValue}. */
public class ConstraintValueRule implements RuleDefinition {
  public static final String RULE_NAME = "constraint_value";
  public static final String CONSTRAINT_SETTING_ATTR = "constraint_setting";

  @Override
  public RuleClass build(RuleClass.Builder builder, RuleDefinitionEnvironment env) {
    return builder
        .advertiseProvider(ConstraintValueInfo.class)
        /* <!-- #BLAZE_RULE(constraint_value).ATTRIBUTE(constraint_setting) -->
        The <code>constraint_setting</code> for which this <code>constraint_value</code> is a
        possible choice.
        <!-- #END_BLAZE_RULE.ATTRIBUTE --> */
        .add(
            attr(CONSTRAINT_SETTING_ATTR, BuildType.LABEL)
                .mandatory()
                .allowedRuleClasses(ConstraintSettingRule.RULE_NAME)
                .allowedFileTypes(FileTypeSet.NO_FILE)
                .mandatoryProviders(ConstraintSettingInfo.PROVIDER.id()))
        .build();
  }

  @Override
  public Metadata getMetadata() {
    return Metadata.builder()
        .name(RULE_NAME)
        .ancestors(PlatformBaseRule.class)
        .factoryClass(ConstraintValue.class)
        .build();
  }
}
/*<!-- #BLAZE_RULE (NAME = constraint_value, FAMILY = Platform)[GENERIC_RULE] -->

This rule introduces a new value for a given constraint type. See the
<a href="https://docs.bazel.build/versions/master/platforms.html">Platforms</a> page for more
details.

<h4 id="constraint_value_examples">Example</h4>
<p>The following creates a new possible value for the predefined <code>constraint_value</code>
representing cpu architecture.
<pre class="code">
constraint_value(
    name = "mips",
    constraint_setting = "@platforms//cpu:cpu",
)
</pre>

Platforms can then declare that they have the <code>mips</code> architecture as an alternative to
<code>x86_64</code>, <code>arm</code>, and so on.

<!-- #END_BLAZE_RULE -->*/
