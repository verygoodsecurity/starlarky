// Copyright 2019 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.query2.engine;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import com.google.devtools.build.lib.analysis.config.BuildOptions;
import com.google.devtools.build.lib.analysis.util.DefaultBuildOptionsForTesting;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.events.ExtendedEventHandler;
import com.google.devtools.build.lib.packages.CachingPackageLocator;
import com.google.devtools.build.lib.packages.PackageFactory.EnvironmentExtension;
import com.google.devtools.build.lib.packages.Target;
import com.google.devtools.build.lib.packages.util.MockToolsConfig;
import com.google.devtools.build.lib.pkgcache.PathPackageLocator;
import com.google.devtools.build.lib.pkgcache.TargetPatternPreloader;
import com.google.devtools.build.lib.pkgcache.TargetProvider;
import com.google.devtools.build.lib.pkgcache.TransitivePackageLoader;
import com.google.devtools.build.lib.query2.QueryEnvironmentFactory;
import com.google.devtools.build.lib.query2.common.AbstractBlazeQueryEnvironment;
import com.google.devtools.build.lib.query2.common.UniverseScope;
import com.google.devtools.build.lib.query2.engine.QueryEnvironment.QueryFunction;
import com.google.devtools.build.lib.query2.engine.QueryEnvironment.Setting;
import com.google.devtools.build.lib.query2.query.GraphlessBlazeQueryEnvironment;
import com.google.devtools.build.lib.query2.testutil.AbstractQueryTest;
import com.google.devtools.build.lib.query2.testutil.SkyframeQueryHelper;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.skyframe.WalkableGraph.WalkableGraphFactory;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for the query engine, generic over the result type. This allows us to share the tests
 * between the different implementations, and also parameterize it over the set of options, such as
 * {@code --keep_going}.
 */
@RunWith(JUnit4.class)
public class GraphlessQueryTest extends AbstractQueryTest<Target> {
  @Override
  @Test
  public void testGraphOrderOfWildcards() {
    // Test assumes that the result is of type DigraphQueryEvalResult, which is not true for the
    // GraphlessBlazeQueryEnvironment.
  }

  @Override
  @Test
  public void testFilesetPackageDeps() {
    // Fileset doesn't exist in Bazel.
  }

  @Override
  @Test
  public void testRegressionBug1686119() {
    // Fileset doesn't exist in Bazel.
  }

  @Override
  @Test
  public void testDefaultCopts() {
    // There's no default_copts attribute in Bazel.
  }

  @Override
  @Test
  public void testHdrsCheck() throws Exception {
    // There's no hdrs_check attribute in Bazel.
  }

  @Override
  protected QueryHelper<Target> createQueryHelper() {
    return new SkyframeQueryHelper() {
      @Override
      protected String getRootDirectoryNameForSetup() {
        return "/workspace";
      }

      @Override
      protected void performAdditionalClientSetup(MockToolsConfig mockToolsConfig) {}

      @Override
      protected QueryEnvironmentFactory makeQueryEnvironmentFactory() {
        return new QueryEnvironmentFactory() {
          @Override
          public AbstractBlazeQueryEnvironment<Target> create(
              TransitivePackageLoader transitivePackageLoader,
              WalkableGraphFactory graphFactory,
              TargetProvider targetProvider,
              CachingPackageLocator cachingPackageLocator,
              TargetPatternPreloader targetPatternPreloader,
              PathFragment relativeWorkingDirectory,
              boolean keepGoing,
              boolean strictScope,
              boolean orderedResults,
              UniverseScope universeScope,
              int loadingPhaseThreads,
              Predicate<Label> labelFilter,
              ExtendedEventHandler eventHandler,
              Set<Setting> settings,
              Iterable<QueryFunction> extraFunctions,
              @Nullable PathPackageLocator packagePath,
              boolean blockUniverseEvaluationErrors,
              boolean useGraphlessQuery) {
            return new GraphlessBlazeQueryEnvironment(
                transitivePackageLoader,
                targetProvider,
                cachingPackageLocator,
                targetPatternPreloader,
                relativeWorkingDirectory,
                keepGoing,
                strictScope,
                loadingPhaseThreads,
                labelFilter,
                eventHandler,
                settings,
                extraFunctions);
          }
        };
      }

      @Override
      protected Iterable<QueryFunction> getExtraQueryFunctions() {
        return ImmutableList.of();
      }

      @Override
      protected Iterable<EnvironmentExtension> getEnvironmentExtensions() {
        return ImmutableList.of();
      }

      @Override
      protected BuildOptions getDefaultBuildOptions(ConfiguredRuleClassProvider ruleClassProvider) {
        return DefaultBuildOptionsForTesting.getDefaultBuildOptionsForTest(ruleClassProvider);
      }
    };
  }
}
