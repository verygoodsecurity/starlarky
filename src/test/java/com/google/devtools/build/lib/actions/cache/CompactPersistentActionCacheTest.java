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
package com.google.devtools.build.lib.actions.cache;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.FileArtifactValue;
import com.google.devtools.build.lib.testutil.ManualClock;
import com.google.devtools.build.lib.testutil.Scratch;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for the CompactPersistentActionCache class. */
@RunWith(JUnit4.class)
public class CompactPersistentActionCacheTest {

  private Scratch scratch = new Scratch();
  private Path dataRoot;
  private Path mapFile;
  private Path journalFile;
  private ManualClock clock = new ManualClock();
  private CompactPersistentActionCache cache;

  @Before
  public final void createFiles() throws Exception  {
    dataRoot = scratch.resolve("/cache/test.dat");
    cache = new CompactPersistentActionCache(dataRoot, clock);
    mapFile = CompactPersistentActionCache.cacheFile(dataRoot);
    journalFile = CompactPersistentActionCache.journalFile(dataRoot);
  }

  @Test
  public void testGetInvalidKey() {
    assertThat(cache.get("key")).isNull();
  }

  @Test
  public void testPutAndGet() {
    String key = "key";
    putKey(key);
    ActionCache.Entry readentry = cache.get(key);
    assertThat(readentry).isNotNull();
    assertThat(readentry.toString()).isEqualTo(cache.get(key).toString());
    assertThat(mapFile.exists()).isFalse();
  }

  @Test
  public void testPutAndRemove() {
    String key = "key";
    putKey(key);
    cache.remove(key);
    assertThat(cache.get(key)).isNull();
    assertThat(mapFile.exists()).isFalse();
  }

  @Test
  public void testSaveDiscoverInputs() throws Exception {
    assertSave(true);
  }

  @Test
  public void testSaveNoDiscoverInputs() throws Exception {
    assertSave(false);
  }

  private void assertSave(boolean discoverInputs) throws Exception {
    String key = "key";
    putKey(key, discoverInputs);
    cache.save();
    assertThat(mapFile.exists()).isTrue();
    assertThat(journalFile.exists()).isFalse();

    CompactPersistentActionCache newcache =
        new CompactPersistentActionCache(dataRoot, clock);
    ActionCache.Entry readentry = newcache.get(key);
    assertThat(readentry).isNotNull();
    assertThat(readentry.toString()).isEqualTo(cache.get(key).toString());
  }

  @Test
  public void testIncrementalSave() throws IOException {
    for (int i = 0; i < 300; i++) {
      putKey(Integer.toString(i));
    }
    assertFullSave();

    // Add 2 entries to 300. Might as well just leave them in the journal.
    putKey("abc");
    putKey("123");
    assertIncrementalSave(cache);

    // Make sure we have all the entries, including those in the journal,
    // after deserializing into a new cache.
    CompactPersistentActionCache newcache =
        new CompactPersistentActionCache(dataRoot, clock);
    for (int i = 0; i < 100; i++) {
      assertKeyEquals(cache, newcache, Integer.toString(i));
    }
    assertKeyEquals(cache, newcache, "abc");
    assertKeyEquals(cache, newcache, "123");
    putKey("xyz", newcache, true);
    assertIncrementalSave(newcache);

    // Make sure we can see previous journal values after a second incremental save.
    CompactPersistentActionCache newerCache =
        new CompactPersistentActionCache(dataRoot, clock);
    for (int i = 0; i < 100; i++) {
      assertKeyEquals(cache, newerCache, Integer.toString(i));
    }
    assertKeyEquals(cache, newerCache, "abc");
    assertKeyEquals(cache, newerCache, "123");
    assertThat(newerCache.get("xyz")).isNotNull();
    assertThat(newerCache.get("not_a_key")).isNull();

    // Add another 10 entries. This should not be incremental.
    for (int i = 300; i < 310; i++) {
      putKey(Integer.toString(i));
    }
    assertFullSave();
  }

  // Regression test to check that CompactActionCacheEntry.toString does not mutate the object.
  // Mutations may result in IllegalStateException.
  @Test
  public void testEntryToStringIsIdempotent() throws Exception {
    ActionCache.Entry entry =
        new ActionCache.Entry("actionKey", ImmutableMap.<String, String>of(), false);
    entry.toString();
    entry.addFile(
        PathFragment.create("foo/bar"), FileArtifactValue.createForDirectoryWithMtime(1234));
    entry.toString();
    entry.getFileDigest();
    entry.toString();
  }

  private void assertToStringIsntTooBig(int numRecords) throws Exception {
    for (int i = 0; i < numRecords; i++) {
      putKey(Integer.toString(i));
    }
    String val = cache.toString();
    assertThat(val).startsWith("Action cache (" + numRecords + " records):\n");
    assertWithMessage(val).that(val.length()).isAtMost(2000);
    // Cache was too big to print out fully.
    if (numRecords > 10) {
      assertThat(val).endsWith("...");
    }
  }

  @Test
  public void testToStringIsntTooBig() throws Exception {
    assertToStringIsntTooBig(3);
    assertToStringIsntTooBig(3000);
  }

  private static void assertKeyEquals(ActionCache cache1, ActionCache cache2, String key) {
    Object entry = cache1.get(key);
    assertThat(entry).isNotNull();
    assertThat(cache2.get(key).toString()).isEqualTo(entry.toString());
  }

  private void assertFullSave() throws IOException {
    cache.save();
    assertThat(mapFile.exists()).isTrue();
    assertThat(journalFile.exists()).isFalse();
  }

  private void assertIncrementalSave(ActionCache ac) throws IOException {
    ac.save();
    assertThat(mapFile.exists()).isTrue();
    assertThat(journalFile.exists()).isTrue();
  }

  private void putKey(String key) {
    putKey(key, cache, false);
  }

  private void putKey(String key, boolean discoversInputs) {
    putKey(key, cache, discoversInputs);
  }

  private void putKey(String key, ActionCache ac, boolean discoversInputs) {
    ActionCache.Entry entry =
        new ActionCache.Entry(key, ImmutableMap.<String, String>of("k", "v"), discoversInputs);
    entry.getFileDigest();
    ac.put(key, entry);
  }
}
