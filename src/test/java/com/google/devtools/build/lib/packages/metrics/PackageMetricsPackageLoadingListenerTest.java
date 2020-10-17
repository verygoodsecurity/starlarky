// Copyright 2020 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.packages.metrics;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.collect.ImmutableSortedKeyMap;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.Target;
import com.google.protobuf.util.Durations;
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.StarlarkSemantics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PackageMetricsPackageLoadingListener}. */
@RunWith(JUnit4.class)
public class PackageMetricsPackageLoadingListenerTest {

  private final PackageMetricsPackageLoadingListener underTest =
      PackageMetricsPackageLoadingListener.getInstance();

  @Test
  public void testRecordsTopSlowestPackagesPerBuild_extrema() {
    PackageMetricsRecorder recorder = new ExtremaPackageMetricsRecorder(2);
    underTest.setPackageMetricsRecorder(recorder);

    recordSlowPackages();

    assertThat(underTest.getPackageMetricsRecorder().getLoadTimes())
        .containsExactlyEntriesIn(
            ImmutableMap.of(
                PackageIdentifier.createInMainRepo("my/pkg3"),
                Durations.fromMillis(44),
                PackageIdentifier.createInMainRepo("my/pkg2"),
                Durations.fromMillis(43)))
        .inOrder();

    recorder.loadingFinished();
    assertAllMapsEmpty(recorder);
  }

  @Test
  public void testRecordsTopSlowestPackagesPerBuild_complete() {
    PackageMetricsRecorder recorder = new CompletePackageMetricsRecorder();
    underTest.setPackageMetricsRecorder(recorder);

    recordSlowPackages();

    assertThat(underTest.getPackageMetricsRecorder().getLoadTimes())
        .containsExactly(
            PackageIdentifier.createInMainRepo("my/pkg1"),
            Durations.fromMillis(42),
            PackageIdentifier.createInMainRepo("my/pkg2"),
            Durations.fromMillis(43),
            PackageIdentifier.createInMainRepo("my/pkg3"),
            Durations.fromMillis(44));
    recorder.clear();
    assertAllMapsEmpty(recorder);
  }

  private void recordSlowPackages() {
    underTest.onLoadingCompleteAndSuccessful(
        mockPackage(
            "my/pkg1",
            /*targets=*/ ImmutableMap.of(),
            /*starlarkDependencies=*/ ImmutableList.of()),
        StarlarkSemantics.DEFAULT,
        /*loadTimeNanos=*/ 42_000_000);

    underTest.onLoadingCompleteAndSuccessful(
        mockPackage(
            "my/pkg2",
            /*targets=*/ ImmutableMap.of(),
            /*starlarkDependencies=*/ ImmutableList.of()),
        StarlarkSemantics.DEFAULT,
        /*loadTimeNanos=*/ 43_000_000);

    underTest.onLoadingCompleteAndSuccessful(
        mockPackage(
            "my/pkg3",
            /*targets=*/ ImmutableMap.of(),
            /*starlarkDependencies=*/ ImmutableList.of()),
        StarlarkSemantics.DEFAULT,
        /*loadTimeNanos=*/ 44_000_000);
  }

  @Test
  public void testRecordsTopLargestPackagesPerBuild_extrema() {
    PackageMetricsRecorder recorder = new ExtremaPackageMetricsRecorder(2);
    underTest.setPackageMetricsRecorder(recorder);

    recordLargePackages();

    assertThat(underTest.getPackageMetricsRecorder().getNumTargets())
        .containsExactlyEntriesIn(
            ImmutableMap.of(
                PackageIdentifier.createInMainRepo("my/pkg3"),
                3L,
                PackageIdentifier.createInMainRepo("my/pkg2"),
                2L))
        .inOrder();
    recorder.loadingFinished();
    ;
    assertAllMapsEmpty(recorder);
  }

  @Test
  public void testRecordsTopLargestPackagesPerBuild_complete() {
    PackageMetricsRecorder recorder = new CompletePackageMetricsRecorder();
    underTest.setPackageMetricsRecorder(recorder);

    recordLargePackages();

    assertThat(underTest.getPackageMetricsRecorder().getNumTargets())
        .containsExactly(
            PackageIdentifier.createInMainRepo("my/pkg3"),
            3L,
            PackageIdentifier.createInMainRepo("my/pkg2"),
            2L,
            PackageIdentifier.createInMainRepo("my/pkg1"),
            1L);
  }

  private void recordLargePackages() {
    underTest.onLoadingCompleteAndSuccessful(
        mockPackage(
            "my/pkg1",
            ImmutableMap.of("target1", mock(Target.class)),
            /*starlarkDependencies=*/ ImmutableList.of()),
        StarlarkSemantics.DEFAULT,
        /*loadTimeNanos=*/ 100);

    underTest.onLoadingCompleteAndSuccessful(
        mockPackage(
            "my/pkg2",
            ImmutableMap.of("target1", mock(Target.class), "target2", mock(Target.class)),
            /*starlarkDependencies=*/ ImmutableList.of()),
        StarlarkSemantics.DEFAULT,
        /*loadTimeNanos=*/ 100);

    underTest.onLoadingCompleteAndSuccessful(
        mockPackage(
            "my/pkg3",
            ImmutableMap.of(
                "target1",
                mock(Target.class),
                "target2",
                mock(Target.class),
                "target3",
                mock(Target.class)),
            /*starlarkDependencies=*/ ImmutableList.of()),
        StarlarkSemantics.DEFAULT,
        /*loadTimeNanos=*/ 100);
  }

  @Test
  public void testRecordsTransitiveLoadsPerBuild_extrema() {
    PackageMetricsRecorder recorder = new ExtremaPackageMetricsRecorder(2);
    underTest.setPackageMetricsRecorder(recorder);

    recordTransitiveLoads();

    assertThat(underTest.getPackageMetricsRecorder().getNumTransitiveLoads())
        .containsExactlyEntriesIn(
            ImmutableMap.of(
                PackageIdentifier.createInMainRepo("my/pkg3"),
                3L,
                PackageIdentifier.createInMainRepo("my/pkg2"),
                2L))
        .inOrder();
    recorder.loadingFinished();
    assertAllMapsEmpty(recorder);
  }

  @Test
  public void testRecordsTransitiveLoadsPerBuild_complete() {
    PackageMetricsRecorder recorder = new CompletePackageMetricsRecorder();
    underTest.setPackageMetricsRecorder(recorder);

    recordTransitiveLoads();

    assertThat(underTest.getPackageMetricsRecorder().getNumTransitiveLoads())
        .containsExactly(
            PackageIdentifier.createInMainRepo("my/pkg3"),
            3L,
            PackageIdentifier.createInMainRepo("my/pkg2"),
            2L,
            PackageIdentifier.createInMainRepo("my/pkg1"),
            1L);
  }

  private void recordTransitiveLoads() {
    underTest.onLoadingCompleteAndSuccessful(
        mockPackage(
            "my/pkg1",
            /*targets=*/ ImmutableMap.of(),
            ImmutableList.of(Label.parseAbsoluteUnchecked("//load:1.bzl"))),
        StarlarkSemantics.DEFAULT,
        /*loadTimeNanos=*/ 100);

    underTest.onLoadingCompleteAndSuccessful(
        mockPackage(
            "my/pkg2",
            /*targets=*/ ImmutableMap.of(),
            ImmutableList.of(
                Label.parseAbsoluteUnchecked("//load:1.bzl"),
                Label.parseAbsoluteUnchecked("//load:2.bzl"))),
        StarlarkSemantics.DEFAULT,
        /*loadTimeNanos=*/ 100);

    underTest.onLoadingCompleteAndSuccessful(
        mockPackage(
            "my/pkg3",
            /*targets=*/ ImmutableMap.of(),
            ImmutableList.of(
                Label.parseAbsoluteUnchecked("//load:1.bzl"),
                Label.parseAbsoluteUnchecked("//load:2.bzl"),
                Label.parseAbsoluteUnchecked("//load:3.bzl"))),
        StarlarkSemantics.DEFAULT,
        /*loadTimeNanos=*/ 100);
  }

  @Test
  public void testRecordsMostComputationStepsPerBuild_extrema() {
    PackageMetricsRecorder recorder = new ExtremaPackageMetricsRecorder(2);
    underTest.setPackageMetricsRecorder(recorder);

    recordComputationSteps();

    assertThat(underTest.getPackageMetricsRecorder().getComputationSteps())
        .containsExactlyEntriesIn(
            ImmutableMap.of(
                PackageIdentifier.createInMainRepo("my/pkg1"),
                1000L,
                PackageIdentifier.createInMainRepo("my/pkg2"),
                100L))
        .inOrder();
    recorder.loadingFinished();
    ;
    assertAllMapsEmpty(recorder);
  }

  @Test
  public void testRecordsMostComputationStepsPerBuild_comlete() {
    PackageMetricsRecorder recorder = new CompletePackageMetricsRecorder();
    underTest.setPackageMetricsRecorder(recorder);

    recordComputationSteps();

    assertThat(underTest.getPackageMetricsRecorder().getComputationSteps())
        .containsExactly(
            PackageIdentifier.createInMainRepo("my/pkg1"),
            1000L,
            PackageIdentifier.createInMainRepo("my/pkg2"),
            100L,
            PackageIdentifier.createInMainRepo("my/pkg3"),
            10L);
    recorder.loadingFinished();
    ;
    assertAllMapsEmpty(recorder);
  }

  void recordComputationSteps() {
    Package mockPackage1 =
        mockPackage(
            "my/pkg1",
            /*targets=*/ ImmutableMap.of(),
            /*starlarkDependencies=*/ ImmutableList.of());
    when(mockPackage1.getComputationSteps()).thenReturn(1000L);
    underTest.onLoadingCompleteAndSuccessful(
        mockPackage1, StarlarkSemantics.DEFAULT, /*loadTimeNanos=*/ 100);

    Package mockPackage2 =
        mockPackage(
            "my/pkg2",
            /*targets=*/ ImmutableMap.of(),
            /*starlarkDependencies=*/ ImmutableList.of());
    when(mockPackage2.getComputationSteps()).thenReturn(100L);
    underTest.onLoadingCompleteAndSuccessful(
        mockPackage2, StarlarkSemantics.DEFAULT, /*loadTimeNanos=*/ 100);

    Package mockPackage3 =
        mockPackage(
            "my/pkg3",
            /*targets=*/ ImmutableMap.of(),
            /*starlarkDependencies=*/ ImmutableList.of());
    when(mockPackage3.getComputationSteps()).thenReturn(10L);
    underTest.onLoadingCompleteAndSuccessful(
        mockPackage3, StarlarkSemantics.DEFAULT, /*loadTimeNanos=*/ 100);
  }

  @Test
  public void metricMap_extrema() {
    PackageMetricsRecorder recorder = new ExtremaPackageMetricsRecorder(2);
    underTest.setPackageMetricsRecorder(recorder);

    recordEverything();

    PackageMetrics pkg1 =
        PackageMetrics.newBuilder()
            .setName("my/pkg1")
            .setLoadDuration(Durations.fromMillis(42))
            .setComputationSteps(1000)
            .setNumTargets(1)
            .setNumTransitiveLoads(1)
            .build();

    PackageMetrics pkg2 =
        PackageMetrics.newBuilder()
            .setName("my/pkg2")
            .setLoadDuration(Durations.fromMillis(43))
            .setComputationSteps(100)
            .setNumTargets(2)
            .setNumTransitiveLoads(2)
            .build();

    PackageMetrics pkg3 =
        PackageMetrics.newBuilder()
            .setName("my/pkg3")
            .setLoadDuration(Durations.fromMillis(44))
            .setComputationSteps(10)
            .setNumTargets(3)
            .setNumTransitiveLoads(3)
            .build();

    assertThat(underTest.getPackageMetricsRecorder().getPackageMetrics())
        .containsExactly(pkg1, pkg2, pkg3);
    recorder.loadingFinished();
    assertAllMapsEmpty(recorder);
  }

  @Test
  public void metricMap_complete() {
    PackageMetricsRecorder recorder = new CompletePackageMetricsRecorder();
    underTest.setPackageMetricsRecorder(recorder);

    recordEverything();

    PackageMetrics pkg1 =
        PackageMetrics.newBuilder()
            .setName("my/pkg1")
            .setLoadDuration(Durations.fromMillis(42))
            .setComputationSteps(1000)
            .setNumTargets(1)
            .setNumTransitiveLoads(1)
            .build();

    PackageMetrics pkg2 =
        PackageMetrics.newBuilder()
            .setName("my/pkg2")
            .setLoadDuration(Durations.fromMillis(43))
            .setComputationSteps(100)
            .setNumTargets(2)
            .setNumTransitiveLoads(2)
            .build();

    PackageMetrics pkg3 =
        PackageMetrics.newBuilder()
            .setName("my/pkg3")
            .setLoadDuration(Durations.fromMillis(44))
            .setComputationSteps(10)
            .setNumTargets(3)
            .setNumTransitiveLoads(3)
            .build();

    assertThat(underTest.getPackageMetricsRecorder().getPackageMetrics())
        .containsExactly(pkg1, pkg2, pkg3);
    recorder.loadingFinished();
    assertAllMapsEmpty(recorder);
  }

  void recordEverything() {
    Package mockPackage1 =
        mockPackage(
            "my/pkg1",
            /*targets=*/ ImmutableMap.of("target1", mock(Target.class)),
            /*starlarkDependencies=*/ ImmutableList.of(
                Label.parseAbsoluteUnchecked("//load:1.bzl")));
    when(mockPackage1.getComputationSteps()).thenReturn(1000L);
    underTest.onLoadingCompleteAndSuccessful(
        mockPackage1, StarlarkSemantics.DEFAULT, /*loadTimeNanos=*/ 42_000_000);

    Package mockPackage2 =
        mockPackage(
            "my/pkg2",
            /*targets=*/ ImmutableMap.of(
                "target1", mock(Target.class), "target2", mock(Target.class)),
            /*starlarkDependencies=*/ ImmutableList.of(
                Label.parseAbsoluteUnchecked("//load:1.bzl"),
                Label.parseAbsoluteUnchecked("//load:2.bzl")));
    when(mockPackage2.getComputationSteps()).thenReturn(100L);
    underTest.onLoadingCompleteAndSuccessful(
        mockPackage2, StarlarkSemantics.DEFAULT, /*loadTimeNanos=*/ 43_000_000);

    Package mockPackage3 =
        mockPackage(
            "my/pkg3",
            /*targets=*/ ImmutableMap.of(
                "target1",
                mock(Target.class),
                "target2",
                mock(Target.class),
                "target3",
                mock(Target.class)),
            /*starlarkDependencies=*/ ImmutableList.of(
                Label.parseAbsoluteUnchecked("//load:1.bzl"),
                Label.parseAbsoluteUnchecked("//load:2.bzl"),
                Label.parseAbsoluteUnchecked("//load:3.bzl")));
    when(mockPackage3.getComputationSteps()).thenReturn(10L);
    underTest.onLoadingCompleteAndSuccessful(
        mockPackage3, StarlarkSemantics.DEFAULT, /*loadTimeNanos=*/ 44_000_000);
  }

  @Test
  public void testDoesntRecordAnythingWhenNumPackagesToTrackIsZero() {
    PackageMetricsRecorder recorder = new ExtremaPackageMetricsRecorder(0);
    underTest.setPackageMetricsRecorder(recorder);

    underTest.onLoadingCompleteAndSuccessful(
        mockPackage(
            "my/pkg1",
            /*targets=*/ ImmutableMap.of(),
            /*starlarkDependencies=*/ ImmutableList.of()),
        StarlarkSemantics.DEFAULT,
        /*loadTimeNanos=*/ 42_000_000);

    assertAllMapsEmpty(underTest.getPackageMetricsRecorder());
  }

  private static void assertAllMapsEmpty(PackageMetricsRecorder recorder) {
    assertThat(recorder.getLoadTimes()).isEmpty();
    assertThat(recorder.getComputationSteps()).isEmpty();
    assertThat(recorder.getNumTargets()).isEmpty();
    assertThat(recorder.getNumTransitiveLoads()).isEmpty();
  }

  private static Package mockPackage(
      String pkgIdString, Map<String, Target> targets, List<Label> starlarkDependencies) {
    Package mockPackage = mock(Package.class);
    when(mockPackage.getPackageIdentifier())
        .thenReturn(PackageIdentifier.createInMainRepo(pkgIdString));
    when(mockPackage.getTargets()).thenReturn(ImmutableSortedKeyMap.copyOf(targets));
    when(mockPackage.getStarlarkFileDependencies())
        .thenReturn(ImmutableList.copyOf(starlarkDependencies));
    return mockPackage;
  }
}
