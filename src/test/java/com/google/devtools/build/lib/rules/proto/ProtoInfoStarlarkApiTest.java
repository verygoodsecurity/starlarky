// Copyright 2016 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.rules.proto;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.util.ActionsTestUtil;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.RepositoryName;
import com.google.devtools.build.lib.packages.Provider;
import com.google.devtools.build.lib.packages.StarlarkProvider;
import com.google.devtools.build.lib.packages.StructImpl;
import com.google.devtools.build.lib.packages.util.MockProtoSupport;
import com.google.devtools.build.lib.starlarkbuildapi.proto.ProtoCommonApi;
import com.google.devtools.build.lib.testutil.TestConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ProtoInfoStarlarkApiTest extends BuildViewTestCase {

  @Before
  public void setUp() throws Exception {
    useConfiguration("--proto_compiler=//proto:compiler"); // TODO check do we need that.
    scratch.file("proto/BUILD", "licenses(['notice'])", "exports_files(['compiler'])");
    scratch.file("myinfo/myinfo.bzl", "MyInfo = provider()");
    scratch.file("myinfo/BUILD");
    MockProtoSupport.setup(mockToolsConfig);

    MockProtoSupport.setupWorkspace(scratch);
    invalidatePackages();
  }

  private StructImpl getMyInfoFromTarget(ConfiguredTarget configuredTarget) throws Exception {
    Provider.Key key =
        new StarlarkProvider.Key(
            Label.parseAbsolute("//myinfo:myinfo.bzl", ImmutableMap.of()), "MyInfo");
    return (StructImpl) configuredTarget.get(key);
  }

  @Test
  public void testProtoCommon() throws Exception {
    scratch.file(
        "foo/test.bzl",
        "load('//myinfo:myinfo.bzl', 'MyInfo')",
        "def _impl(ctx):",
        "  return MyInfo(proto_common=proto_common)",
        "test = rule(implementation = _impl, attrs = {})");

    scratch.file("foo/BUILD", "load(':test.bzl', 'test')", "test(name='test')");

    ConfiguredTarget test = getConfiguredTarget("//foo:test");
    Object protoCommon = getMyInfoFromTarget(test).getValue("proto_common");
    assertThat(protoCommon).isInstanceOf(ProtoCommonApi.class);
  }

  @Test
  public void testProvider() throws Exception {
    scratch.file(
        "foo/test.bzl",
        "load('//myinfo:myinfo.bzl', 'MyInfo')",
        "def _impl(ctx):",
        "  provider = ctx.attr.dep[ProtoInfo]", // NB: This is the modern provider
        "  return MyInfo(direct_sources=provider.direct_sources)",
        "test = rule(implementation = _impl, attrs = {'dep': attr.label()})");

    scratch.file(
        "foo/BUILD",
        TestConstants.LOAD_PROTO_LIBRARY,
        "load(':test.bzl', 'test')",
        "test(name='test', dep=':proto')",
        "proto_library(name='proto', srcs=['p.proto'])");

    ConfiguredTarget test = getConfiguredTarget("//foo:test");
    @SuppressWarnings("unchecked")
    Iterable<Artifact> directSources =
        (Iterable<Artifact>) getMyInfoFromTarget(test).getValue("direct_sources");
    assertThat(ActionsTestUtil.baseArtifactNames(directSources)).containsExactly("p.proto");
  }

  @Test
  public void testProtoSourceRootExportedInStarlark() throws Exception {
    scratch.file(
        "third_party/foo/myTestRule.bzl",
        "load('//myinfo:myinfo.bzl', 'MyInfo')",
        "",
        "def _my_test_rule_impl(ctx):",
        "    return MyInfo(",
        "        fetched_proto_source_root = ctx.attr.protodep[ProtoInfo].proto_source_root",
        "    )",
        "",
        "my_test_rule = rule(",
        "    implementation = _my_test_rule_impl,",
        "    attrs = {'protodep': attr.label()},",
        ")");

    scratch.file(
        "third_party/foo/BUILD",
        TestConstants.LOAD_PROTO_LIBRARY,
        "licenses(['unencumbered'])",
        "load(':myTestRule.bzl', 'my_test_rule')",
        "my_test_rule(",
        "  name = 'myRule',",
        "  protodep = ':myProto',",
        ")",
        "proto_library(",
        "  name = 'myProto',",
        "  srcs = ['myProto.proto'],",
        "  strip_import_prefix = '/third_party/foo',",
        ")");

    ConfiguredTarget ct = getConfiguredTarget("//third_party/foo:myRule");
    String protoSourceRoot = (String) getMyInfoFromTarget(ct).getValue("fetched_proto_source_root");
    String genfiles = getTargetConfiguration().getGenfilesFragment(RepositoryName.MAIN).toString();

    assertThat(protoSourceRoot).isEqualTo(genfiles + "/third_party/foo/_virtual_imports/myProto");
  }
}
