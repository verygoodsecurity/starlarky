// Copyright 2015 The Bazel Authors. All rights reserved.
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.build.lib.packages.Attribute.attr;
import static com.google.devtools.build.lib.packages.Type.BOOLEAN;
import static com.google.devtools.build.lib.packages.Type.INTEGER;
import static com.google.devtools.build.lib.packages.Type.STRING;
import static com.google.devtools.build.lib.packages.Type.STRING_LIST;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.actions.MutableActionGraph.ActionConflictException;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.RuleClass.Builder.RuleClassNamePredicate;
import com.google.devtools.build.lib.packages.RuleClass.Builder.RuleClassType;
import com.google.devtools.build.lib.packages.util.PackageLoadingTestCase;
import net.starlark.java.eval.StarlarkInt;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for the {@link RuleClass.Builder}.
 */
@RunWith(JUnit4.class)
public class RuleClassBuilderTest extends PackageLoadingTestCase {
  private static final RuleClass.ConfiguredTargetFactory<Object, Object, Exception>
      DUMMY_CONFIGURED_TARGET_FACTORY =
          new RuleClass.ConfiguredTargetFactory<Object, Object, Exception>() {
            @Override
            public Object create(Object ruleContext)
                throws InterruptedException, RuleErrorException, ActionConflictException {
              throw new IllegalStateException();
            }
          };

  @Test
  public void testRuleClassBuilderBasics() throws Exception {
    RuleClass ruleClassA =
        new RuleClass.Builder("ruleA", RuleClassType.NORMAL, false)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .add(attr("srcs", BuildType.LABEL_LIST).legacyAllowAnyFileType())
            .add(attr("tags", STRING_LIST))
            .add(attr("X", com.google.devtools.build.lib.packages.Type.INTEGER).mandatory())
            .build();

    assertThat(ruleClassA.getName()).isEqualTo("ruleA");
    assertThat(ruleClassA.getAttributeCount()).isEqualTo(3);
    assertThat(ruleClassA.hasBinaryOutput()).isTrue();

    assertThat((int) ruleClassA.getAttributeIndex("srcs")).isEqualTo(0);
    assertThat(ruleClassA.getAttributeByName("srcs")).isEqualTo(ruleClassA.getAttribute(0));

    assertThat((int) ruleClassA.getAttributeIndex("tags")).isEqualTo(1);
    assertThat(ruleClassA.getAttributeByName("tags")).isEqualTo(ruleClassA.getAttribute(1));

    assertThat((int) ruleClassA.getAttributeIndex("X")).isEqualTo(2);
    assertThat(ruleClassA.getAttributeByName("X")).isEqualTo(ruleClassA.getAttribute(2));
  }

  @Test
  public void testRuleClassBuilderTestIsBinary() throws Exception {
    RuleClass ruleClassA =
        new RuleClass.Builder("rule_test", RuleClassType.TEST, false)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .add(attr("tags", STRING_LIST))
            .add(attr("size", STRING).value("medium"))
            .add(attr("timeout", STRING))
            .add(attr("flaky", BOOLEAN).value(false))
            .add(attr("shard_count", INTEGER).value(StarlarkInt.of(-1)))
            .add(attr("local", BOOLEAN))
            .build();
    assertThat(ruleClassA.hasBinaryOutput()).isTrue();
  }

  @Test
  public void testRuleClassBuilderGenruleIsNotBinary() throws Exception {
    RuleClass ruleClassA =
        new RuleClass.Builder("ruleA", RuleClassType.NORMAL, false)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .setOutputToGenfiles()
            .add(attr("tags", STRING_LIST))
            .build();
    assertThat(ruleClassA.hasBinaryOutput()).isFalse();
  }

  @Test
  public void testRuleClassTestNameValidity() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RuleClass.Builder("ruleA", RuleClassType.TEST, false).build());
  }

  @Test
  public void testRuleClassNormalNameValidity() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RuleClass.Builder("ruleA_test", RuleClassType.NORMAL, false).build());
  }

  @Test
  public void testDuplicateAttribute() throws Exception {
    RuleClass.Builder builder =
        new RuleClass.Builder("ruleA", RuleClassType.NORMAL, false).add(attr("a", STRING));
    assertThrows(IllegalStateException.class, () -> builder.add(attr("a", STRING)));
  }

  @Test
  public void testPropertiesOfAbstractRuleClass() throws Exception {
    assertThrows(
        IllegalStateException.class,
        () -> new RuleClass.Builder("$ruleA", RuleClassType.ABSTRACT, false).setOutputToGenfiles());

    assertThrows(
        IllegalStateException.class,
        () ->
            new RuleClass.Builder("$ruleB", RuleClassType.ABSTRACT, false)
                .setImplicitOutputsFunction(null));
  }

  @Test
  public void testDuplicateInheritedAttribute() throws Exception {
    RuleClass a =
        new RuleClass.Builder("ruleA", RuleClassType.NORMAL, false)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .add(attr("a", STRING).value("A"))
            .add(attr("tags", STRING_LIST))
            .build();
    RuleClass b =
        new RuleClass.Builder("ruleB", RuleClassType.NORMAL, false)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .add(attr("a", STRING).value("B"))
            .add(attr("tags", STRING_LIST))
            .build();
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> new RuleClass.Builder("ruleC", RuleClassType.NORMAL, false, a, b).build());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo("Attribute a is inherited multiple times in ruleC ruleclass");
  }

  @Test
  public void testRemoveAttribute() throws Exception {
    RuleClass a =
        new RuleClass.Builder("rule", RuleClassType.NORMAL, false)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .add(attr("a", STRING))
            .add(attr("b", STRING))
            .add(attr("tags", STRING_LIST))
            .build();
    RuleClass.Builder builder =
        new RuleClass.Builder("c", RuleClassType.NORMAL, false, a)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY);
    RuleClass c = builder.removeAttribute("a").add(attr("a", INTEGER)).removeAttribute("b").build();
    assertThat(c.hasAttr("a", STRING)).isFalse();
    assertThat(c.hasAttr("a", INTEGER)).isTrue();
    assertThat(c.hasAttr("b", STRING)).isFalse();

    assertThrows(IllegalStateException.class, () -> builder.removeAttribute("c"));
  }

  @Test
  public void testRequiredToolchainsAreInherited() throws Exception {
    Label mockToolchainType = Label.parseAbsoluteUnchecked("//mock_toolchain_type");
    RuleClass parent =
        new RuleClass.Builder("$parent", RuleClassType.ABSTRACT, false)
            .add(attr("tags", STRING_LIST))
            .addRequiredToolchains(ImmutableList.of(mockToolchainType))
            .build();
    RuleClass child =
        new RuleClass.Builder("child", RuleClassType.NORMAL, false, parent)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .add(attr("attr", STRING))
            .build();
    assertThat(child.getRequiredToolchains()).contains(mockToolchainType);
  }

  @Test
  public void testExecGroupsAreInherited() throws Exception {
    Label mockToolchainType = Label.parseAbsoluteUnchecked("//mock_toolchain_type");
    Label mockConstraint = Label.parseAbsoluteUnchecked("//mock_constraint");
    ExecGroup parentGroup =
        ExecGroup.create(ImmutableSet.of(mockToolchainType), ImmutableSet.of(mockConstraint));
    ExecGroup childGroup = ExecGroup.create(ImmutableSet.of(), ImmutableSet.of());
    RuleClass parent =
        new RuleClass.Builder("$parent", RuleClassType.ABSTRACT, false)
            .add(attr("tags", STRING_LIST))
            .addExecGroups(ImmutableMap.of("group", parentGroup))
            .build();
    RuleClass child =
        new RuleClass.Builder("child", RuleClassType.NORMAL, false, parent)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .add(attr("attr", STRING))
            .addExecGroups(ImmutableMap.of("child-group", childGroup))
            .build();
    assertThat(child.getExecGroups().get("group")).isEqualTo(parentGroup);
    assertThat(child.getExecGroups().get("child-group")).isEqualTo(childGroup);
  }

  @Test
  public void testDuplicateExecGroupsThatAreEqualIsOk() throws Exception {
    ExecGroup execGroup =
        ExecGroup.create(
            ImmutableSet.of(Label.parseAbsoluteUnchecked("//some/toolchain")),
            ImmutableSet.of(Label.parseAbsoluteUnchecked("//some/constraint")));
    RuleClass a =
        new RuleClass.Builder("ruleA", RuleClassType.NORMAL, false)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .addExecGroups(ImmutableMap.of("blueberry", execGroup))
            .add(attr("tags", STRING_LIST))
            .build();
    RuleClass b =
        new RuleClass.Builder("ruleB", RuleClassType.NORMAL, false)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .addExecGroups(ImmutableMap.of("blueberry", execGroup))
            .add(attr("tags", STRING_LIST))
            .build();
    RuleClass c = new RuleClass.Builder("$ruleC", RuleClassType.ABSTRACT, false, a, b).build();
    assertThat(c.getExecGroups()).containsExactly("blueberry", execGroup);
  }

  @Test
  public void testDuplicateExecGroupsThrowsError() throws Exception {
    RuleClass a =
        new RuleClass.Builder("ruleA", RuleClassType.NORMAL, false)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .addExecGroups(
                ImmutableMap.of(
                    "blueberry",
                    ExecGroup.create(
                        ImmutableSet.of(Label.parseAbsoluteUnchecked("//some/toolchain")),
                        ImmutableSet.of())))
            .add(attr("tags", STRING_LIST))
            .build();
    RuleClass b =
        new RuleClass.Builder("ruleB", RuleClassType.NORMAL, false)
            .factory(DUMMY_CONFIGURED_TARGET_FACTORY)
            .addExecGroups(
                ImmutableMap.of(
                    "blueberry", ExecGroup.create(ImmutableSet.of(), ImmutableSet.of())))
            .add(attr("tags", STRING_LIST))
            .build();
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> new RuleClass.Builder("ruleC", RuleClassType.NORMAL, false, a, b).build());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "An execution group named 'blueberry' is inherited multiple times with different"
                + " requirements in ruleC ruleclass");
  }

  @Test
  public void testBasicRuleNamePredicates() throws Exception {
    Predicate<String> abcdef = nothingBut("abc", "def").asPredicateOfRuleClassName();
    assertThat(abcdef.test("abc")).isTrue();
    assertThat(abcdef.test("def")).isTrue();
    assertThat(abcdef.test("ghi")).isFalse();
  }

  @Test
  public void testTwoRuleNamePredicateFactoriesEquivalent() throws Exception {
    RuleClassNamePredicate a = nothingBut("abc", "def");
    RuleClassNamePredicate b = RuleClassNamePredicate.only(ImmutableList.of("abc", "def"));
    assertThat(a.asPredicateOfRuleClassName()).isEqualTo(b.asPredicateOfRuleClassName());
    assertThat(a.asPredicateOfRuleClass()).isEqualTo(b.asPredicateOfRuleClass());
  }

  @Test
  public void testEverythingButRuleNamePredicates() throws Exception {
    Predicate<String> abcdef = allBut("abc", "def").asPredicateOfRuleClassName();
    assertThat(abcdef.test("abc")).isFalse();
    assertThat(abcdef.test("def")).isFalse();
    assertThat(abcdef.test("ghi")).isTrue();
  }

  @Test
  public void testRuleClassNamePredicateIntersection() {
    // two positives intersect iff they contain any of the same items
    assertThat(nothingBut("abc", "def").consideredOverlapping(nothingBut("abc"))).isTrue();
    assertThat(nothingBut("abc", "def").consideredOverlapping(nothingBut("ghi"))).isFalse();

    // negatives are never considered to overlap...
    assertThat(allBut("abc", "def").consideredOverlapping(allBut("abc", "def"))).isFalse();
    assertThat(allBut("abc", "def").consideredOverlapping(allBut("ghi", "jkl"))).isFalse();

    assertThat(allBut("abc", "def").consideredOverlapping(nothingBut("abc", "def"))).isFalse();
    assertThat(nothingBut("abc", "def").consideredOverlapping(allBut("abc", "def"))).isFalse();

    assertThat(allBut("abc", "def").consideredOverlapping(nothingBut("abc"))).isFalse();
    assertThat(allBut("abc").consideredOverlapping(nothingBut("abc", "def"))).isFalse();
  }

  private RuleClassNamePredicate nothingBut(String... excludedRuleClasses) {
    return RuleClassNamePredicate.only(excludedRuleClasses);
  }

  private RuleClassNamePredicate allBut(String... excludedRuleClasses) {
    return RuleClassNamePredicate.allExcept(excludedRuleClasses);
  }
}
