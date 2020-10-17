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
package com.google.devtools.build.lib.bazel.repository.downloader;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link UrlRewriter} */
@RunWith(JUnit4.class)
public class UrlRewriterTest {

  @Test
  public void byDefaultTheUrlRewriterDoesNothing() throws MalformedURLException {
    UrlRewriter munger = new UrlRewriter(str -> {}, new StringReader(""));

    List<URL> urls = ImmutableList.of(new URL("http://example.com"));
    List<URL> amended = munger.amend(urls);

    assertThat(amended).isEqualTo(urls);
  }

  @Test
  public void shouldBeAbleToBlockParticularHostsRegardlessOfScheme() throws MalformedURLException {
    String config = "block example.com";
    UrlRewriter munger = new UrlRewriter(str -> {}, new StringReader(config));

    List<URL> urls =
        ImmutableList.of(
            new URL("http://example.com"),
            new URL("https://example.com"),
            new URL("http://localhost"));
    List<URL> amended = munger.amend(urls);

    assertThat(amended).containsExactly(new URL("http://localhost"));
  }

  @Test
  public void shouldAllowAUrlToBeRewritten() throws MalformedURLException {
    String config = "rewrite example.com/foo/(.*) mycorp.com/$1/foo";
    UrlRewriter munger = new UrlRewriter(str -> {}, new StringReader(config));

    List<URL> urls = ImmutableList.of(new URL("https://example.com/foo/bar"));
    List<URL> amended = munger.amend(urls);

    assertThat(amended).containsExactly(new URL("https://mycorp.com/bar/foo"));
  }

  @Test
  public void rewritesCanExpandToMoreThanOneUrl() throws MalformedURLException {
    String config =
        "rewrite example.com/foo/(.*) mycorp.com/$1/somewhere\n"
            + "rewrite example.com/foo/(.*) mycorp.com/$1/elsewhere";
    UrlRewriter munger = new UrlRewriter(str -> {}, new StringReader(config));

    List<URL> urls = ImmutableList.of(new URL("https://example.com/foo/bar"));
    List<URL> amended = munger.amend(urls);

    // There's no guarantee about the ordering of the rewrites
    assertThat(amended).contains(new URL("https://mycorp.com/bar/somewhere"));
    assertThat(amended).contains(new URL("https://mycorp.com/bar/elsewhere"));
  }

  @Test
  public void shouldBlockAllUrlsOtherThanSpecificOnes() throws MalformedURLException {
    String config = "" + "block *\n" + "allow example.com";

    UrlRewriter munger = new UrlRewriter(str -> {}, new StringReader(config));

    List<URL> urls =
        ImmutableList.of(
            new URL("https://foo.com"),
            new URL("https://example.com/foo/bar"),
            new URL("https://subdomain.example.com/qux"));
    List<URL> amended = munger.amend(urls);

    assertThat(amended)
        .containsExactly(
            new URL("https://example.com/foo/bar"), new URL("https://subdomain.example.com/qux"));
  }

  @Test
  public void commentsArePrecededByTheHashCharacter() throws MalformedURLException {
    String config =
        ""
            + "# Block everything\n"
            + "block *\n"
            + "# But allow example.com\n"
            + "allow example.com";

    UrlRewriter munger = new UrlRewriter(str -> {}, new StringReader(config));

    List<URL> urls = ImmutableList.of(new URL("https://foo.com"), new URL("https://example.com"));
    List<URL> amended = munger.amend(urls);

    assertThat(amended).containsExactly(new URL("https://example.com"));
  }

  @Test
  public void allowListAppliesToSubdomainsToo() throws MalformedURLException {
    String config = "" + "block *\n" + "allow example.com";

    UrlRewriter munger = new UrlRewriter(str -> {}, new StringReader(config));

    List<URL> amended = munger.amend(ImmutableList.of(new URL("https://subdomain.example.com")));

    assertThat(amended).containsExactly(new URL("https://subdomain.example.com"));
  }

  @Test
  public void blockListAppliesToSubdomainsToo() throws MalformedURLException {
    String config = "block example.com";

    UrlRewriter munger = new UrlRewriter(str -> {}, new StringReader(config));

    List<URL> amended = munger.amend(ImmutableList.of(new URL("https://subdomain.example.com")));

    assertThat(amended).isEmpty();
  }

  @Test
  public void emptyLinesAreFine() throws MalformedURLException {
    String config = "" + "\n" + "   \n" + "block *\n" + "\t  \n" + "allow example.com";

    UrlRewriter munger = new UrlRewriter(str -> {}, new StringReader(config));

    List<URL> amended = munger.amend(ImmutableList.of(new URL("https://subdomain.example.com")));

    assertThat(amended).containsExactly(new URL("https://subdomain.example.com"));
  }

  @Test
  public void rewritingUrlsIsAppliedBeforeBlocking() throws MalformedURLException {
    String config = "" + "block bad.com\n" + "rewrite bad.com/foo/(.*) mycorp.com/$1";

    UrlRewriter munger = new UrlRewriter(str -> {}, new StringReader(config));

    List<URL> amended =
        munger.amend(
            ImmutableList.of(new URL("https://www.bad.com"), new URL("https://bad.com/foo/bar")));

    assertThat(amended).containsExactly(new URL("https://mycorp.com/bar"));
  }

  @Test
  public void rewritingUrlsIsAppliedBeforeAllowing() throws MalformedURLException {
    String config =
        "" + "block *\n" + "allow mycorp.com\n" + "rewrite bad.com/foo/(.*) mycorp.com/$1";

    UrlRewriter munger = new UrlRewriter(str -> {}, new StringReader(config));

    List<URL> amended =
        munger.amend(
            ImmutableList.of(new URL("https://www.bad.com"), new URL("https://bad.com/foo/bar")));

    assertThat(amended).containsExactly(new URL("https://mycorp.com/bar"));
  }
}
