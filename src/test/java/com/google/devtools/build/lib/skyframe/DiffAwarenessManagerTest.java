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
package com.google.devtools.build.lib.skyframe;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.devtools.build.lib.events.util.EventCollectionApparatus;
import com.google.devtools.build.lib.skyframe.DiffAwarenessManager.ProcessableModifiedFileSet;
import com.google.devtools.build.lib.vfs.DigestHashFunction;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.ModifiedFileSet;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.lib.vfs.inmemoryfs.InMemoryFileSystem;
import com.google.devtools.common.options.OptionsProvider;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link DiffAwarenessManager}, especially of the fact that it works in a sequential
 * manner and of its correctness in the presence of unprocesed diffs.
 */
@RunWith(JUnit4.class)
public class DiffAwarenessManagerTest {
  private FileSystem fs;
  protected EventCollectionApparatus events;

  @Before
  public final void createFileSystem() throws Exception  {
    fs = new InMemoryFileSystem(DigestHashFunction.SHA256);
  }

  @Before
  public final void initializeEventCollectionApparatus() {
    events = new EventCollectionApparatus();
    events.setFailFast(false);
  }

  @Test
  public void testEverythingModifiedIfNoDiffAwareness() throws Exception {
    Root pathEntry = Root.fromPath(fs.getPath("/pathEntry"));
    DiffAwarenessFactoryStub factory = new DiffAwarenessFactoryStub();
    DiffAwarenessManager manager = new DiffAwarenessManager(ImmutableList.of(factory));
    assertWithMessage("Expected EVERYTHING_MODIFIED since there are no factories")
        .that(
            manager
                .getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY)
                .getModifiedFileSet())
        .isEqualTo(ModifiedFileSet.EVERYTHING_MODIFIED);
    events.assertNoWarningsOrErrors();
  }

  @Test
  public void testResetAndSetPathEntriesCallClose() throws Exception {
    Root pathEntry = Root.fromPath(fs.getPath("/pathEntry"));
    ModifiedFileSet diff = ModifiedFileSet.NOTHING_MODIFIED;
    DiffAwarenessStub diffAwareness1 = new DiffAwarenessStub(ImmutableList.of(diff));
    DiffAwarenessStub diffAwareness2 = new DiffAwarenessStub(ImmutableList.of(diff));
    DiffAwarenessFactoryStub factory = new DiffAwarenessFactoryStub();
    factory.inject(pathEntry, diffAwareness1);
    DiffAwarenessManager manager = new DiffAwarenessManager(ImmutableList.of(factory));
    manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    assertWithMessage("diffAwareness1 shouldn't have been closed yet")
        .that(diffAwareness1.closed())
        .isFalse();
    manager.reset();
    assertWithMessage("diffAwareness1 should have been closed by reset")
        .that(diffAwareness1.closed())
        .isTrue();
    factory.inject(pathEntry, diffAwareness2);
    manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    assertWithMessage("diffAwareness2 shouldn't have been closed yet")
        .that(diffAwareness2.closed())
        .isFalse();
    events.assertNoWarningsOrErrors();
  }

  @Test
  public void testHandlesUnprocessedDiffs() throws Exception {
    Root pathEntry = Root.fromPath(fs.getPath("/pathEntry"));
    ModifiedFileSet diff1 = ModifiedFileSet.builder().modify(PathFragment.create("file1")).build();
    ModifiedFileSet diff2 = ModifiedFileSet.builder().modify(PathFragment.create("file2")).build();
    ModifiedFileSet diff3 = ModifiedFileSet.builder().modify(PathFragment.create("file3")).build();
    DiffAwarenessStub diffAwareness =
        new DiffAwarenessStub(ImmutableList.of(diff1, diff2, diff3, DiffAwarenessStub.BROKEN_DIFF));
    DiffAwarenessFactoryStub factory = new DiffAwarenessFactoryStub();
    factory.inject(pathEntry, diffAwareness);
    DiffAwarenessManager manager = new DiffAwarenessManager(ImmutableList.of(factory));
    ProcessableModifiedFileSet firstProcessableDiff =
        manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    assertWithMessage("Expected EVERYTHING_MODIFIED on first call to getDiff")
        .that(firstProcessableDiff.getModifiedFileSet())
        .isEqualTo(ModifiedFileSet.EVERYTHING_MODIFIED);
    firstProcessableDiff.markProcessed();
    ProcessableModifiedFileSet processableDiff1 =
        manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    assertThat(processableDiff1.getModifiedFileSet()).isEqualTo(diff1);
    ProcessableModifiedFileSet processableDiff2 =
        manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    assertThat(processableDiff2.getModifiedFileSet())
        .isEqualTo(ModifiedFileSet.union(diff1, diff2));
    processableDiff2.markProcessed();
    ProcessableModifiedFileSet processableDiff3 =
        manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    assertThat(processableDiff3.getModifiedFileSet()).isEqualTo(diff3);
    events.assertNoWarningsOrErrors();
    ProcessableModifiedFileSet processableDiff4 =
        manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    assertThat(processableDiff4.getModifiedFileSet())
        .isEqualTo(ModifiedFileSet.EVERYTHING_MODIFIED);
    events.assertContainsWarning("error");
  }

  @Test
  public void testHandlesBrokenDiffs() throws Exception {
    Root pathEntry = Root.fromPath(fs.getPath("/pathEntry"));
    DiffAwarenessFactoryStub factory1 = new DiffAwarenessFactoryStub();
    DiffAwarenessStub diffAwareness1 =
        new DiffAwarenessStub(ImmutableList.<ModifiedFileSet>of(), 1);
    factory1.inject(pathEntry, diffAwareness1);
    DiffAwarenessFactoryStub factory2 = new DiffAwarenessFactoryStub();
    ModifiedFileSet diff2 = ModifiedFileSet.builder().modify(PathFragment.create("file2")).build();
    DiffAwarenessStub diffAwareness2 =
        new DiffAwarenessStub(ImmutableList.of(diff2, DiffAwarenessStub.BROKEN_DIFF));
    factory2.inject(pathEntry, diffAwareness2);
    DiffAwarenessFactoryStub factory3 = new DiffAwarenessFactoryStub();
    ModifiedFileSet diff3 = ModifiedFileSet.builder().modify(PathFragment.create("file3")).build();
    DiffAwarenessStub diffAwareness3 = new DiffAwarenessStub(ImmutableList.of(diff3));
    factory3.inject(pathEntry, diffAwareness3);
    DiffAwarenessManager manager =
        new DiffAwarenessManager(ImmutableList.of(factory1, factory2, factory3));

    ProcessableModifiedFileSet processableDiff =
        manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    events.assertNoWarningsOrErrors();
    assertWithMessage("Expected EVERYTHING_MODIFIED on first call to getDiff for diffAwareness1")
        .that(processableDiff.getModifiedFileSet())
        .isEqualTo(ModifiedFileSet.EVERYTHING_MODIFIED);
    processableDiff.markProcessed();

    processableDiff = manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    events.assertContainsEventWithFrequency("error in getCurrentView", 1);
    assertWithMessage("Expected EVERYTHING_MODIFIED because of broken getCurrentView")
        .that(processableDiff.getModifiedFileSet())
        .isEqualTo(ModifiedFileSet.EVERYTHING_MODIFIED);
    processableDiff.markProcessed();
    factory1.remove(pathEntry);

    processableDiff = manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    assertWithMessage("Expected EVERYTHING_MODIFIED on first call to getDiff for diffAwareness2")
        .that(processableDiff.getModifiedFileSet())
        .isEqualTo(ModifiedFileSet.EVERYTHING_MODIFIED);
    processableDiff.markProcessed();

    processableDiff = manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    assertThat(processableDiff.getModifiedFileSet()).isEqualTo(diff2);
    processableDiff.markProcessed();

    processableDiff = manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    events.assertContainsEventWithFrequency("error in getDiff", 1);
    assertWithMessage("Expected EVERYTHING_MODIFIED because of broken getDiff")
        .that(processableDiff.getModifiedFileSet())
        .isEqualTo(ModifiedFileSet.EVERYTHING_MODIFIED);
    processableDiff.markProcessed();
    factory2.remove(pathEntry);

    processableDiff = manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    assertWithMessage("Expected EVERYTHING_MODIFIED on first call to getDiff for diffAwareness3")
        .that(processableDiff.getModifiedFileSet())
        .isEqualTo(ModifiedFileSet.EVERYTHING_MODIFIED);
    processableDiff.markProcessed();

    processableDiff = manager.getDiff(events.reporter(), pathEntry, OptionsProvider.EMPTY);
    assertThat(processableDiff.getModifiedFileSet()).isEqualTo(diff3);
    processableDiff.markProcessed();
  }

  private static class DiffAwarenessFactoryStub implements DiffAwareness.Factory {

    private final Map<Root, DiffAwareness> diffAwarenesses = Maps.newHashMap();

    public void inject(Root pathEntry, DiffAwareness diffAwareness) {
      diffAwarenesses.put(pathEntry, diffAwareness);
    }

    public void remove(Root pathEntry) {
      diffAwarenesses.remove(pathEntry);
    }

    @Override
    @Nullable
    public DiffAwareness maybeCreate(Root pathEntry) {
      return diffAwarenesses.get(pathEntry);
    }
  }

  private static class DiffAwarenessStub implements DiffAwareness {

    public static final ModifiedFileSet BROKEN_DIFF =
        ModifiedFileSet.builder().modify(PathFragment.create("special broken marker")).build();

    private boolean closed = false;
    private int curSequenceNum = 0;
    private final List<ModifiedFileSet> sequentialDiffs;
    private final int brokenViewNum;

    public DiffAwarenessStub(List<ModifiedFileSet> sequentialDiffs) {
      this(sequentialDiffs, -1);
    }

    public DiffAwarenessStub(List<ModifiedFileSet> sequentialDiffs, int brokenViewNum) {
      this.sequentialDiffs = sequentialDiffs;
      this.brokenViewNum = brokenViewNum;
    }

    private static class ViewStub implements DiffAwareness.View {
      private final int sequenceNum;

      public ViewStub(int sequenceNum) {
        this.sequenceNum = sequenceNum;
      }
    }

    @Override
    public View getCurrentView(OptionsProvider options) throws BrokenDiffAwarenessException {
      if (curSequenceNum == brokenViewNum) {
        throw new BrokenDiffAwarenessException("error in getCurrentView");
      }
      return new ViewStub(curSequenceNum++);
    }

    @Override
    public ModifiedFileSet getDiff(View oldView, View newView) throws BrokenDiffAwarenessException {
      assertThat(oldView).isInstanceOf(ViewStub.class);
      assertThat(newView).isInstanceOf(ViewStub.class);
      ViewStub oldViewStub = (ViewStub) oldView;
      ViewStub newViewStub = (ViewStub) newView;
      Preconditions.checkState(newViewStub.sequenceNum >= oldViewStub.sequenceNum);
      ModifiedFileSet diff = ModifiedFileSet.NOTHING_MODIFIED;
      for (int num = oldViewStub.sequenceNum; num < newViewStub.sequenceNum; num++) {
        ModifiedFileSet incrementalDiff = sequentialDiffs.get(num);
        if (incrementalDiff == BROKEN_DIFF) {
          throw new BrokenDiffAwarenessException("error in getDiff");
        }
        diff = ModifiedFileSet.union(diff, incrementalDiff);
      }
      return diff;
    }

    @Override
    public String name() {
      return "testingstub";
    }

    @Override
    public void close() {
      closed = true;
    }

    public boolean closed() {
      return closed;
    }
  }
}
