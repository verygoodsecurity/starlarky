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

package net.starlark.java.annot.processor;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import com.google.common.io.Resources;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for StarlarkMethodProcessor. */
@RunWith(JUnit4.class)
public final class StarlarkMethodProcessorTest {

  private static JavaFileObject getFile(String pathToFile) {
    return JavaFileObjects.forResource(Resources.getResource(
        StarlarkMethodProcessorTest.class, "testsources/" + pathToFile));
  }

  @Test
  public void testGoldenCase() throws Exception {
    assertAbout(javaSource())
        .that(getFile("GoldenCase.java"))
        .processedWith(new StarlarkMethodProcessor())
        .compilesWithoutError();
  }

  @Test
  public void testPrivateMethod() throws Exception {
    assertAbout(javaSource())
        .that(getFile("PrivateMethod.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining("StarlarkMethod-annotated methods must be public.");
  }

  @Test
  public void testStaticMethod() throws Exception {
    assertAbout(javaSource())
        .that(getFile("StaticMethod.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining("StarlarkMethod-annotated methods cannot be static.");
  }


  @Test
  public void testStructFieldWithArguments() throws Exception {
    assertAbout(javaSource())
        .that(getFile("StructFieldWithArguments.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "method structFieldMethod is annotated structField=true but also has 1 Param"
                + " annotations");
  }

  @Test
  public void testStructFieldWithInvalidInfo() throws Exception {
    assertAbout(javaSource())
        .that(getFile("StructFieldWithInvalidInfo.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "a StarlarkMethod-annotated method with structField=true may not also specify"
                + " useStarlarkThread");
  }

  @Test
  public void testStructFieldWithExtraArgs() throws Exception {
    assertAbout(javaSource())
        .that(getFile("StructFieldWithExtraArgs.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "a StarlarkMethod-annotated method with structField=true may not also specify"
                + " extraPositionals");
  }

  @Test
  public void testStructFieldWithExtraKeywords() throws Exception {
    assertAbout(javaSource())
        .that(getFile("StructFieldWithExtraKeywords.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "a StarlarkMethod-annotated method with structField=true may not also specify"
                + " extraKeywords");
  }

  @Test
  public void testDocumentationMissing() throws Exception {
    assertAbout(javaSource())
        .that(getFile("DocumentationMissing.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining("The 'doc' string must be non-empty if 'documented' is true.");
  }

  @Test
  public void testArgumentMissing() throws Exception {
    assertAbout(javaSource())
        .that(getFile("ArgumentMissing.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "method methodWithParams has 1 Param annotations but only 0 parameters");
  }

  @Test
  public void testStarlarkThreadMissing() throws Exception {
    assertAbout(javaSource())
        .that(getFile("StarlarkThreadMissing.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "for useStarlarkThread special parameter 'shouldBeThread', got type java.lang.String,"
                + " want StarlarkThread");
  }

  @Test
  public void testStarlarkInfoBeforeParams() throws Exception {
    assertAbout(javaSource())
        .that(getFile("StarlarkInfoBeforeParams.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "for useStarlarkThread special parameter 'three', got type java.lang.String, want"
                + " StarlarkThread");
    // Also reports:
    // - annotated type java.lang.String of parameter 'one' is not assignable
    //   to variable of type net.starlark.java.eval.StarlarkThread
    // - annotated type java.lang.Integer of parameter 'two' is not assignable
    //   to variable of type java.lang.String
  }

  @Test
  public void testTooManyArguments() throws Exception {
    assertAbout(javaSource())
        .that(getFile("TooManyArguments.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "method methodWithTooManyArguments is annotated with 1 Params plus 0 special"
                + " parameters, yet has 2 parameter variables");
  }

  @Test
  public void testParamNeitherNamedNorPositional() throws Exception {
    assertAbout(javaSource())
        .that(getFile("ParamNeitherNamedNorPositional.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining("Parameter 'a_parameter' must be either positional or named");
  }

  @Test
  public void testNonDefaultParamAfterDefault() throws Exception {
    assertAbout(javaSource())
        .that(getFile("NonDefaultParamAfterDefault.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "Positional parameter 'two' has no default value but is specified "
                + "after one or more positional parameters with default values");
  }

  @Test
  public void testPositionalParamAfterNonPositional() throws Exception {
    assertAbout(javaSource())
        .that(getFile("PositionalParamAfterNonPositional.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "Positional parameter 'two' is specified after one or more non-positional parameters");
  }

  @Test
  public void testPositionalOnlyParamAfterNamed() throws Exception {
    assertAbout(javaSource())
        .that(getFile("PositionalOnlyParamAfterNamed.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "Positional-only parameter 'two' is specified after one or more named or undocumented"
                + " parameters");
  }

  @Test
  public void testExtraKeywordsOutOfOrder() throws Exception {
    assertAbout(javaSource())
        .that(getFile("ExtraKeywordsOutOfOrder.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "extraKeywords special parameter 'one' has type java.lang.String, to which"
                + " Dict<String, Object> cannot be assigned");
  }

  @Test
  public void testExtraPositionalsMissing() throws Exception {
    assertAbout(javaSource())
        .that(getFile("ExtraPositionalsMissing.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "method threeArgMethod is annotated with 1 Params plus 2 special parameters, but has"
                + " only 2 parameter variables");
  }

  @Test
  public void testSelfCallWithNoName() throws Exception {
    assertAbout(javaSource())
        .that(getFile("SelfCallWithNoName.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining("StarlarkMethod.name must be non-empty.");
  }

  @Test
  public void testSelfCallWithStructField() throws Exception {
    assertAbout(javaSource())
        .that(getFile("SelfCallWithStructField.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "a StarlarkMethod-annotated method with structField=true may not also specify"
                + " selfCall=true");
  }

  @Test
  public void testMultipleSelfCallMethods() throws Exception {
    assertAbout(javaSource())
        .that(getFile("MultipleSelfCallMethods.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining("Containing class has more than one selfCall method defined.");
  }

  @Test
  public void testEnablingAndDisablingFlag() throws Exception {
    assertAbout(javaSource())
        .that(getFile("EnablingAndDisablingFlag.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "Only one of StarlarkMethod.enableOnlyWithFlag and StarlarkMethod.disableWithFlag may"
                + " be specified.");
  }

  @Test
  public void testEnablingAndDisablingFlag_param() throws Exception {
    assertAbout(javaSource())
        .that(getFile("EnablingAndDisablingFlagParam.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "Parameter 'two' has enableOnlyWithFlag and disableWithFlag set. "
                + "At most one may be set");
  }

  @Test
  public void testConflictingMethodNames() throws Exception {
    assertAbout(javaSource())
        .that(getFile("ConflictingMethodNames.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "Containing class defines more than one method named 'conflicting_method'");
  }

  @Test
  public void testDisabledValueParamNoToggle() throws Exception {
    assertAbout(javaSource())
        .that(getFile("DisabledValueParamNoToggle.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining("Parameter 'two' has valueWhenDisabled set, but is always enabled");
  }

  @Test
  public void testToggledKwargsParam() throws Exception {
    assertAbout(javaSource())
        .that(getFile("ToggledKwargsParam.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining("The extraKeywords parameter may not be toggled by semantic flag");
  }

  @Test
  public void testToggledParamNoDisabledValue() throws Exception {
    assertAbout(javaSource())
        .that(getFile("ToggledParamNoDisabledValue.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "Parameter 'two' may be disabled by semantic flag, "
                + "thus valueWhenDisabled must be set");
  }

  @Test
  public void testSpecifiedGenericType() throws Exception {
    assertAbout(javaSource())
        .that(getFile("SpecifiedGenericType.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "parameter 'one' has generic type "
                + "net.starlark.java.eval.Sequence<java.lang.String>");
  }

  @Test
  public void testKwargsWithUndocumentedParam() throws Exception {
    assertAbout(javaSource())
        .that(getFile("KwargsWithUndocumentedParams.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining(
            "Method 'undocumented_with_kwargs' has undocumented parameters but also allows extra"
                + " keyword parameters");
  }

  @Test
  public void testUndocumentedPositionalParam() throws Exception {
    assertAbout(javaSource())
        .that(getFile("UndocumentedPositionalParam.java"))
        .processedWith(new StarlarkMethodProcessor())
        .failsToCompile()
        .withErrorContaining("Parameter 'one' must be documented because it is positional");
  }
}
