/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky;

import static com.google.common.truth.Truth.assertThat;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.verygood.security.larky.objects.LarkyNativeObject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/** Which parts of Larky may depend on which. */
public class ArchitectureTest {

  private static final String LARKY = "com.verygood.security.larky";
  private static final String VGS = LARKY + ".modules.vgs..";

  private static final JavaClasses CLASSES =
      new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages(LARKY);

  /** The classes of the standard library's native modules, e.g. {@code JsonModule}. */
  private static final Set<String> STD_MODULE_CLASSES =
      ModuleSupplier.STD_MODULES.stream().map(module -> module.getClass().getName()).collect(Collectors.toSet());

  /**
   * The Java code of the standard library: its native modules, the packages under {@code modules}
   * that are not VGS's or the test modules', and the object model.
   */
  private static final DescribedPredicate<JavaClass> STDLIB =
      DescribedPredicate.describe("the standard library's native code",
          javaClass -> STD_MODULE_CLASSES.contains(javaClass.getName())
              || resideInAPackage(LARKY + ".objects..").test(javaClass)
              || (resideInAPackage(LARKY + ".modules.*..").test(javaClass)
                  && !resideInAnyPackage(VGS, LARKY + ".modules.testing..").test(javaClass)));

  /** The standard library does not depend on the VGS modules. */
  @Test
  public void stdlibJavaDoesNotDependOnVgs() {
    noClasses().that(STDLIB).should().dependOnClassesThat().resideInAPackage(VGS).check(CLASSES);
  }

  /** Nor do its {@code .star} files: nothing under stdlib loads from @vgs. */
  @Test
  public void stdlibStarlarkDoesNotLoadVgs() throws IOException {
    Path stdlib = Path.of("src/main/resources/stdlib");
    assertThat(Files.isDirectory(stdlib)).isTrue();
    // the whole file, so a load( that continues on the next line is found too
    Pattern loadsVgs = Pattern.compile("load\\(\\s*[\"']@vgs/");
    List<String> files;
    try (Stream<Path> walk = Files.walk(stdlib)) {
      files = walk.filter(file -> file.toString().endsWith(".star"))
          .filter(file -> {
            try {
              return loadsVgs.matcher(Files.readString(file)).find();
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          })
          .map(file -> stdlib.relativize(file).toString())
          .toList();
    }
    assertThat(files).isEmpty();
  }

  /**
   * A class written in Java (a {@link LarkyNativeObject}) says what it has with {@code
   * LarkyNativeClass} and leaves the Python protocols to the object model: it uses neither the
   * descriptors nor the types.
   */
  @Test
  public void nativeClassesLeaveThePythonProtocolsToTheObjectModel() {
    noClasses()
        .that(assignableTo(LarkyNativeObject.class))
        .and().resideOutsideOfPackage(LARKY + ".objects..")
        .should().dependOnClassesThat().resideInAnyPackage(LARKY + ".objects.descriptor..", LARKY + ".objects.type..")
        .check(CLASSES);
  }
}
