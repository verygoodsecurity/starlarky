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
package com.google.devtools.build.lib.rules.java;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.ProviderCollection;
import com.google.devtools.build.lib.analysis.Runfiles;
import com.google.devtools.build.lib.analysis.TransitiveInfoCollection;
import com.google.devtools.build.lib.analysis.TransitiveInfoProvider;
import com.google.devtools.build.lib.analysis.TransitiveInfoProviderMap;
import com.google.devtools.build.lib.analysis.TransitiveInfoProviderMapBuilder;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.collect.nestedset.Depset;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.collect.nestedset.Order;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.packages.BuiltinProvider;
import com.google.devtools.build.lib.packages.NativeInfo;
import com.google.devtools.build.lib.rules.java.JavaPluginInfoProvider.JavaPluginInfo;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec.VisibleForSerialization;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.java.JavaInfoApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.syntax.Location;

/** A Starlark declared provider that encapsulates all providers that are needed by Java rules. */
@Immutable
@AutoCodec
public final class JavaInfo extends NativeInfo implements JavaInfoApi<Artifact> {

  public static final String STARLARK_NAME = "JavaInfo";

  public static final JavaInfoProvider PROVIDER = new JavaInfoProvider();

  @Nullable
  private static <T> T nullIfNone(Object object, Class<T> type) {
    return object != Starlark.NONE ? type.cast(object) : null;
  }

  public static final JavaInfo EMPTY = JavaInfo.Builder.create().build();

  private static final ImmutableSet<Class<? extends TransitiveInfoProvider>> ALLOWED_PROVIDERS =
      ImmutableSet.of(
          JavaCompilationArgsProvider.class,
          JavaSourceJarsProvider.class,
          JavaRuleOutputJarsProvider.class,
          JavaRunfilesProvider.class,
          JavaPluginInfoProvider.class,
          JavaGenJarsProvider.class,
          JavaExportsProvider.class,
          JavaCompilationInfoProvider.class,
          JavaStrictCompilationArgsProvider.class,
          JavaSourceInfoProvider.class);

  private final TransitiveInfoProviderMap providers;

  /*
   * Contains the .jar files to be put on the runtime classpath by the configured target.
   * <p>Unlike {@link JavaCompilationArgs#getRuntimeJars()}, it does not contain transitive runtime
   * jars, only those produced by the configured target itself.
   *
   * <p>The reason why this field exists is that neverlink libraries do not contain the compiled jar
   * in {@link JavaCompilationArgs#getRuntimeJars()} and those are sometimes needed, for example,
   * for Proguarding (the compile time classpath is not enough because that contains only ijars)
   */
  private final ImmutableList<Artifact> directRuntimeJars;

  /**
   * A set of runtime jars corresponding to the transitive dependencies of a certain target,
   * excluding the runtime jars for the target itself and its direct dependencies.
   *
   * <p>This set is required only when the persistent test runner is enabled. It is used to create a
   * custom classloader for loading the jars in the transitive dependencies. The persistent test
   * runner creates a separate classloader for the target itself and its direct dependencies.
   */
  private final NestedSet<Artifact> transitiveOnlyRuntimeJars;

  /** Java constraints (e.g. "android") that are present on the target. */
  private final ImmutableList<String> javaConstraints;

  // Whether or not this library should be used only for compilation and not at runtime.
  private final boolean neverlink;

  /** Returns the instance for the provided providerClass, or <tt>null</tt> if not present. */
  @Nullable
  public <P extends TransitiveInfoProvider> P getProvider(Class<P> providerClass) {
    return providers.getProvider(providerClass);
  }

  public TransitiveInfoProviderMap getProviders() {
    return providers;
  }

  /**
   * Merges the given providers into one {@link JavaInfo}. All the providers with the same type in
   * the given list are merged into one provider that is added to the resulting {@link JavaInfo}.
   */
  public static JavaInfo merge(List<JavaInfo> providers) {
    List<JavaCompilationArgsProvider> javaCompilationArgsProviders =
        JavaInfo.fetchProvidersFromList(providers, JavaCompilationArgsProvider.class);
    List<JavaStrictCompilationArgsProvider> javaStrictCompilationArgsProviders =
        JavaInfo.fetchProvidersFromList(providers, JavaStrictCompilationArgsProvider.class);
    List<JavaSourceJarsProvider> javaSourceJarsProviders =
        JavaInfo.fetchProvidersFromList(providers, JavaSourceJarsProvider.class);
    List<JavaRunfilesProvider> javaRunfilesProviders =
        JavaInfo.fetchProvidersFromList(providers, JavaRunfilesProvider.class);
    List<JavaPluginInfoProvider> javaPluginInfoProviders =
        JavaInfo.fetchProvidersFromList(providers, JavaPluginInfoProvider.class);
    List<JavaExportsProvider> javaExportsProviders =
        JavaInfo.fetchProvidersFromList(providers, JavaExportsProvider.class);
    List<JavaRuleOutputJarsProvider> javaRuleOutputJarsProviders =
        JavaInfo.fetchProvidersFromList(providers, JavaRuleOutputJarsProvider.class);
    List<JavaSourceInfoProvider> sourceInfos =
        JavaInfo.fetchProvidersFromList(providers, JavaSourceInfoProvider.class);

    Runfiles mergedRunfiles = Runfiles.EMPTY;
    for (JavaRunfilesProvider javaRunfilesProvider : javaRunfilesProviders) {
      Runfiles runfiles = javaRunfilesProvider.getRunfiles();
      mergedRunfiles = mergedRunfiles == Runfiles.EMPTY ? runfiles : mergedRunfiles.merge(runfiles);
    }

    ImmutableList.Builder<Artifact> runtimeJars = ImmutableList.builder();
    ImmutableList.Builder<String> javaConstraints = ImmutableList.builder();
    for (JavaInfo javaInfo : providers) {
      runtimeJars.addAll(javaInfo.getDirectRuntimeJars());
      javaConstraints.addAll(javaInfo.getJavaConstraints());
    }

    return JavaInfo.Builder.create()
        .addProvider(
            JavaCompilationArgsProvider.class,
            JavaCompilationArgsProvider.merge(javaCompilationArgsProviders))
        .addProvider(
            JavaStrictCompilationArgsProvider.class,
            JavaStrictCompilationArgsProvider.merge(javaStrictCompilationArgsProviders))
        .addProvider(
            JavaSourceJarsProvider.class, JavaSourceJarsProvider.merge(javaSourceJarsProviders))
        .addProvider(
            JavaRuleOutputJarsProvider.class,
            JavaRuleOutputJarsProvider.merge(javaRuleOutputJarsProviders))
        .addProvider(JavaRunfilesProvider.class, new JavaRunfilesProvider(mergedRunfiles))
        .addProvider(
            JavaPluginInfoProvider.class, JavaPluginInfoProvider.merge(javaPluginInfoProviders))
        .addProvider(JavaExportsProvider.class, JavaExportsProvider.merge(javaExportsProviders))
        .addProvider(JavaSourceInfoProvider.class, JavaSourceInfoProvider.merge(sourceInfos))
        // TODO(b/65618333): add merge function to JavaGenJarsProvider. See #3769
        // TODO(iirina): merge or remove JavaCompilationInfoProvider
        .setRuntimeJars(runtimeJars.build())
        .setJavaConstraints(javaConstraints.build())
        .build();
  }

  /**
   * Returns a list of providers of the specified class, fetched from the given list of {@link
   * JavaInfo}s. Returns an empty list if no providers can be fetched. Returns a list of the same
   * size as the given list if the requested providers are of type JavaCompilationArgsProvider.
   */
  public static <T extends TransitiveInfoProvider> ImmutableList<T> fetchProvidersFromList(
      Iterable<JavaInfo> javaProviders, Class<T> providerClass) {
    return streamProviders(javaProviders, providerClass).collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns a stream of providers of the specified class, fetched from the given list of {@link
   * JavaInfo}.
   */
  public static <C extends TransitiveInfoProvider> Stream<C> streamProviders(
      Iterable<JavaInfo> javaProviders, Class<C> providerClass) {
    return Streams.stream(javaProviders)
        .map(javaInfo -> javaInfo.getProvider(providerClass))
        .filter(Objects::nonNull);
  }

  /**
   * Returns a provider of the specified class, fetched from the specified target or, if not found,
   * from the JavaInfo of the given target. JavaInfo can be found as a declared provider in
   * StarlarkProviders. Returns null if no such provider exists.
   *
   * <p>A target can either have both the specified provider and JavaInfo that encapsulates the same
   * information, or just one of them.
   */
  @Nullable
  public static <T extends TransitiveInfoProvider> T getProvider(
      Class<T> providerClass, ProviderCollection providers) {
    T provider = providers.getProvider(providerClass);
    if (provider != null) {
      return provider;
    }
    JavaInfo javaInfo = (JavaInfo) providers.get(JavaInfo.PROVIDER.getKey());
    if (javaInfo == null) {
      return null;
    }
    return javaInfo.getProvider(providerClass);
  }

  public static JavaInfo getJavaInfo(TransitiveInfoCollection target) {
    return (JavaInfo) target.get(JavaInfo.PROVIDER.getKey());
  }

  public static <T extends TransitiveInfoProvider> T getProvider(
      Class<T> providerClass, TransitiveInfoProviderMap providerMap) {
    T provider = providerMap.getProvider(providerClass);
    if (provider != null) {
      return provider;
    }
    JavaInfo javaInfo = (JavaInfo) providerMap.get(JavaInfo.PROVIDER.getKey());
    if (javaInfo == null) {
      return null;
    }
    return javaInfo.getProvider(providerClass);
  }

  public static <T extends TransitiveInfoProvider> List<T> getProvidersFromListOfTargets(
      Class<T> providerClass, Iterable<? extends TransitiveInfoCollection> targets) {
    List<T> providersList = new ArrayList<>();
    for (TransitiveInfoCollection target : targets) {
      T provider = getProvider(providerClass, target);
      if (provider != null) {
        providersList.add(provider);
      }
    }
    return providersList;
  }

  @VisibleForSerialization
  @AutoCodec.Instantiator
  JavaInfo(
      TransitiveInfoProviderMap providers,
      ImmutableList<Artifact> directRuntimeJars,
      NestedSet<Artifact> transitiveOnlyRuntimeJars,
      boolean neverlink,
      ImmutableList<String> javaConstraints,
      Location location) {
    super(PROVIDER, location);
    this.directRuntimeJars = directRuntimeJars;
    this.transitiveOnlyRuntimeJars = transitiveOnlyRuntimeJars;
    this.providers = providers;
    this.neverlink = neverlink;
    this.javaConstraints = javaConstraints;
  }

  public Boolean isNeverlink() {
    return neverlink;
  }

  @Override
  public Depset /*<Artifact>*/ getTransitiveRuntimeJars() {
    return getTransitiveRuntimeDeps();
  }

  @Override
  public Depset /*<Artifact>*/ getTransitiveCompileTimeJars() {
    return getTransitiveDeps();
  }

  @Override
  public Depset /*<Artifact>*/ getCompileTimeJars() {
    NestedSet<Artifact> compileTimeJars =
        getProviderAsNestedSet(
            JavaCompilationArgsProvider.class,
            JavaCompilationArgsProvider::getDirectCompileTimeJars);
    return Depset.of(Artifact.TYPE, compileTimeJars);
  }

  @Override
  public Depset getFullCompileTimeJars() {
    NestedSet<Artifact> fullCompileTimeJars =
        getProviderAsNestedSet(
            JavaCompilationArgsProvider.class,
            JavaCompilationArgsProvider::getDirectFullCompileTimeJars);
    return Depset.of(Artifact.TYPE, fullCompileTimeJars);
  }

  @Override
  public Sequence<Artifact> getSourceJars() {
    // TODO(#4221) change return type to NestedSet<Artifact>
    JavaSourceJarsProvider provider = providers.getProvider(JavaSourceJarsProvider.class);
    ImmutableList<Artifact> sourceJars =
        provider == null ? ImmutableList.of() : provider.getSourceJars();
    return StarlarkList.immutableCopyOf(sourceJars);
  }

  @Override
  public JavaRuleOutputJarsProvider getOutputJars() {
    return getProvider(JavaRuleOutputJarsProvider.class);
  }

  @Override
  public JavaGenJarsProvider getGenJarsProvider() {
    return getProvider(JavaGenJarsProvider.class);
  }

  @Override
  public JavaCompilationInfoProvider getCompilationInfoProvider() {
    return getProvider(JavaCompilationInfoProvider.class);
  }

  @Override
  public Sequence<Artifact> getRuntimeOutputJars() {
    return StarlarkList.immutableCopyOf(getDirectRuntimeJars());
  }

  public ImmutableList<Artifact> getDirectRuntimeJars() {
    return directRuntimeJars;
  }

  // Do not expose to Starlark.
  public NestedSet<Artifact> getTransitiveOnlyRuntimeJars() {
    return transitiveOnlyRuntimeJars;
  }

  @Override
  public Depset /*<Artifact>*/ getTransitiveDeps() {
    return Depset.of(
        Artifact.TYPE,
        getProviderAsNestedSet(
            JavaCompilationArgsProvider.class,
            JavaCompilationArgsProvider::getTransitiveCompileTimeJars));
  }

  @Override
  public Depset /*<Artifact>*/ getTransitiveRuntimeDeps() {
    return Depset.of(
        Artifact.TYPE,
        getProviderAsNestedSet(
            JavaCompilationArgsProvider.class, JavaCompilationArgsProvider::getRuntimeJars));
  }

  @Override
  public Depset /*<Artifact>*/ getTransitiveSourceJars() {
    return Depset.of(
        Artifact.TYPE,
        getProviderAsNestedSet(
            JavaSourceJarsProvider.class, JavaSourceJarsProvider::getTransitiveSourceJars));
  }

  @Override
  public Depset /*<Label>*/ getTransitiveExports() {
    return Depset.of(
        Depset.ElementType.of(Label.class),
        getProviderAsNestedSet(
            JavaExportsProvider.class, JavaExportsProvider::getTransitiveExports));
  }

  /** Returns all constraints set on the associated target. */
  public ImmutableList<String> getJavaConstraints() {
    return javaConstraints;
  }

  /**
   * Gets Provider, check it for not null and call function to get NestedSet&lt;S&gt; from it.
   *
   * <p>Gets provider from map. If Provider is null, return default, empty, stabled ordered
   * NestedSet. If provider is not null, then delegates to mapper all responsibility to fetch
   * required NestedSet from provider.
   *
   * @see JavaInfo#getProviderAsNestedSet(Class, Function, Function)
   * @param providerClass provider class. used as key to look up for provider.
   * @param mapper Function used to convert provider to NesteSet&lt;S&gt;
   * @param <P> type of Provider
   * @param <S> type of returned NestedSet items
   */
  private <P extends TransitiveInfoProvider, S extends StarlarkValue>
      NestedSet<S> getProviderAsNestedSet(
          Class<P> providerClass, Function<P, NestedSet<S>> mapper) {

    P provider = getProvider(providerClass);
    if (provider == null) {
      return NestedSetBuilder.<S>stableOrder().build();
    }
    return mapper.apply(provider);
  }

  @Override
  public boolean equals(Object otherObject) {
    if (this == otherObject) {
      return true;
    }
    if (!(otherObject instanceof JavaInfo)) {
      return false;
    }

    JavaInfo other = (JavaInfo) otherObject;
    return providers.equals(other.providers);
  }

  @Override
  public int hashCode() {
    return providers.hashCode();
  }

  /** Provider class for {@link JavaInfo} objects. */
  public static class JavaInfoProvider extends BuiltinProvider<JavaInfo>
      implements JavaInfoProviderApi {
    private JavaInfoProvider() {
      super(STARLARK_NAME, JavaInfo.class);
    }

    private void checkSequenceOfJavaInfo(Sequence<?> seq, String field) throws EvalException {
      for (Object v : seq) {
        if (!(v instanceof JavaInfo)) {
          throw Starlark.errorf("Expected 'sequence of JavaInfo' for '%s'", field);
        }
      }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public JavaInfo javaInfo(
        FileApi outputJarApi,
        Object compileJarApi,
        Object sourceJarApi,
        Boolean neverlink,
        Sequence<?> deps,
        Sequence<?> runtimeDeps,
        Sequence<?> exports,
        Object jdepsApi,
        StarlarkThread thread)
        throws EvalException {
      Artifact outputJar = (Artifact) outputJarApi;
      @Nullable Artifact compileJar = nullIfNone(compileJarApi, Artifact.class);
      @Nullable Artifact sourceJar = nullIfNone(sourceJarApi, Artifact.class);
      @Nullable Artifact jdeps = nullIfNone(jdepsApi, Artifact.class);
      if (compileJar == null) {
        throw Starlark.errorf("Expected 'File' for 'compile_jar', found 'None'");
      }
      checkSequenceOfJavaInfo(deps, "deps");
      checkSequenceOfJavaInfo(runtimeDeps, "runtime_deps");
      checkSequenceOfJavaInfo(exports, "exports");
      return JavaInfoBuildHelper.getInstance()
          .createJavaInfo(
              outputJar,
              compileJar,
              sourceJar,
              neverlink,
              (Sequence<JavaInfo>) deps,
              (Sequence<JavaInfo>) runtimeDeps,
              (Sequence<JavaInfo>) exports,
              jdeps,
              thread.getCallerLocation());
    }
  }

  /** A Builder for {@link JavaInfo}. */
  public static class Builder {
    TransitiveInfoProviderMapBuilder providerMap;
    private ImmutableList<Artifact> runtimeJars;
    private ImmutableList<String> javaConstraints;
    private final NestedSetBuilder<Artifact> transitiveOnlyRuntimeJars =
        new NestedSetBuilder<>(Order.STABLE_ORDER);
    private boolean neverlink;
    private Location location = Location.BUILTIN;

    private Builder(TransitiveInfoProviderMapBuilder providerMap) {
      this.providerMap = providerMap;
    }

    public static Builder create() {
      return new Builder(new TransitiveInfoProviderMapBuilder())
          .setRuntimeJars(ImmutableList.of())
          .setJavaConstraints(ImmutableList.of());
    }

    public static Builder copyOf(JavaInfo javaInfo) {
      return new Builder(new TransitiveInfoProviderMapBuilder().addAll(javaInfo.getProviders()))
          .setRuntimeJars(javaInfo.getDirectRuntimeJars())
          .addTransitiveOnlyRuntimeJars(javaInfo.getTransitiveOnlyRuntimeJars())
          .setNeverlink(javaInfo.isNeverlink())
          .setJavaConstraints(javaInfo.getJavaConstraints())
          .setLocation(javaInfo.getCreationLoc());
    }

    public Builder setRuntimeJars(ImmutableList<Artifact> runtimeJars) {
      this.runtimeJars = runtimeJars;
      return this;
    }

    public Builder setNeverlink(boolean neverlink) {
      this.neverlink = neverlink;
      return this;
    }

    public Builder maybeTransitiveOnlyRuntimeJarsToJavaInfo(
        List<? extends TransitiveInfoCollection> deps, boolean shouldAdd) {
      // TODO(b/149926109): Currently all callers call with shouldAdd=true as a temporary workaround
      // to make --trim_test_configuration work again.
      if (shouldAdd) {
        deps.stream()
            .map(JavaInfo::getJavaInfo)
            .filter(Objects::nonNull)
            .map(j -> j.getProvider(JavaCompilationArgsProvider.class))
            .filter(Objects::nonNull)
            .map(JavaCompilationArgsProvider::getRuntimeJars)
            .forEach(this::addTransitiveOnlyRuntimeJars);
      }
      return this;
    }

    private Builder addTransitiveOnlyRuntimeJars(NestedSet<Artifact> runtimeJars) {
      this.transitiveOnlyRuntimeJars.addTransitive(runtimeJars);
      return this;
    }

    public Builder setJavaConstraints(ImmutableList<String> javaConstraints) {
      this.javaConstraints = javaConstraints;
      return this;
    }

    public Builder experimentalDisableAnnotationProcessing() {
      JavaPluginInfoProvider provider = providerMap.getProvider(JavaPluginInfoProvider.class);
      if (provider != null) {
        JavaPluginInfo plugins = provider.plugins();
        providerMap.put(
            JavaPluginInfoProvider.class,
            JavaPluginInfoProvider.create(
                JavaPluginInfo.create(
                    /* processorClasses= */ NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER),
                    // Preserve the processor path, since it may contain Error Prone plugins which
                    // will be service-loaded by JavaBuilder.
                    plugins.processorClasspath(),
                    /* data= */ NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER)),
                /* generatesApi= */ false));
      }
      return this;
    }

    public Builder setLocation(Location location) {
      this.location = location;
      return this;
    }

    public <P extends TransitiveInfoProvider> Builder addProvider(
        Class<P> providerClass, TransitiveInfoProvider provider) {
      Preconditions.checkArgument(ALLOWED_PROVIDERS.contains(providerClass));
      providerMap.put(providerClass, provider);
      return this;
    }

    public JavaInfo build() {
      // TODO(twerth): Clean up after we remove java_proto_library.strict_deps.
      // Instead of teaching every (potential Starlark) caller to also create the provider for
      // strict deps we wrap the non strict provider instead.
      if (!providerMap.contains(JavaStrictCompilationArgsProvider.class)
          && providerMap.contains(JavaCompilationArgsProvider.class)) {
        JavaStrictCompilationArgsProvider javaStrictCompilationArgsProvider =
            new JavaStrictCompilationArgsProvider(
                providerMap.getProvider(JavaCompilationArgsProvider.class));
        addProvider(JavaStrictCompilationArgsProvider.class, javaStrictCompilationArgsProvider);
      }
      return new JavaInfo(
          providerMap.build(),
          runtimeJars,
          transitiveOnlyRuntimeJars.build(),
          neverlink,
          javaConstraints,
          location);
    }
  }
}
