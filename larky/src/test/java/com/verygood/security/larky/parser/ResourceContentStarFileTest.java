package com.verygood.security.larky.parser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import java.util.Random;
import org.junit.Test;

public class ResourceContentStarFileTest {

  // The regex splitLabel replaces.
  private static final Pattern NAMESPACE_PREFIX = Pattern.compile("@(\\w+)/?/(.+)");

  private static String[] regexSplit(String label) {
    Matcher m = NAMESPACE_PREFIX.matcher(label);
    return m.find() ? new String[] {m.group(1), m.group(2)} : null;
  }

  private static void assertSameAsRegex(String label) {
    assertArrayEquals(label, regexSplit(label), ResourceContentStarFile.splitLabel(label));
  }

  @Test
  public void splitsLabels() {
    assertArrayEquals(
        new String[] {"stdlib", "json"}, ResourceContentStarFile.splitLabel("@stdlib//json"));
    assertArrayEquals(
        new String[] {"vgs", "http/request"},
        ResourceContentStarFile.splitLabel("@vgs//http/request"));
    assertArrayEquals(
        new String[] {"vendor", "x"}, ResourceContentStarFile.splitLabel("@vendor/x"));
    assertEquals(null, ResourceContentStarFile.splitLabel("json"));
    assertEquals("vgs/http/request.star", ResourceContentStarFile.resolveResourceName("@vgs//http/request"));
    assertEquals("stdlib/json.star", ResourceContentStarFile.resolveResourceName("json"));
  }

  @Test
  public void matchesTheRegexOnEdgeCases() {
    for (String label :
        new String[] {
          "", "@", "@/", "@//", "@a", "@a/", "@a//", "@a///", "@a//\n", "@a/\n", "@a//\nx",
          "@a///x", "@a//x\ny", "@-a//x", "x@a//y", "@@a//y", "@a@b//c", "@a/@b//c", "@é//x",
          "@aé//x", "@a_1//x", "@a b//x", "@a//x@b//y", "\n@a//x", "@a/\n/x", "@a/b/c",
        }) {
      assertSameAsRegex(label);
    }
  }

  @Test
  public void matchesTheRegexOnGeneratedLabels() {
    char[] alphabet = {'@', '/', '/', 'a', 'Z', '_', '9', '\n', '-', '.', ' ', 'é', ' '};
    Random random = new Random(20260929L);
    for (int i = 0; i < 200_000; i++) {
      StringBuilder label = new StringBuilder();
      for (int n = random.nextInt(12); n > 0; n--) {
        label.append(alphabet[random.nextInt(alphabet.length)]);
      }
      assertSameAsRegex(label.toString());
    }
  }
}
