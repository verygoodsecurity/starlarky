// Copyright 2014 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.rules.cpp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.collect.CollectionUtils;
import com.google.devtools.build.lib.concurrent.ThreadSafety;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec.VisibleForSerialization;

/** Factory for creating new {@link LinkerInput} objects. */
public abstract class LinkerInputs {
  /**
   * An opaque linker input that is not a library, for example a linker script or an individual
   * object file.
   */
  @ThreadSafety.Immutable
  @AutoCodec
  public static class SimpleLinkerInput implements LinkerInput {
    private final Artifact artifact;
    private final ArtifactCategory category;
    private final boolean disableWholeArchive;
    private final String libraryIdentifier;

    @AutoCodec.Instantiator
    public SimpleLinkerInput(
        Artifact artifact,
        ArtifactCategory category,
        boolean disableWholeArchive,
        String libraryIdentifier) {
      Preconditions.checkNotNull(libraryIdentifier);
      String basename = artifact.getFilename();
      switch (category) {
        case STATIC_LIBRARY:
          Preconditions.checkState(Link.ARCHIVE_LIBRARY_FILETYPES.matches(basename));
          break;

        case DYNAMIC_LIBRARY:
          Preconditions.checkState(Link.SHARED_LIBRARY_FILETYPES.matches(basename));
          break;

        case OBJECT_FILE:
          // We skip file extension checks for TreeArtifacts because they represent directory
          // artifacts without a file extension.
          Preconditions.checkState(
              artifact.isTreeArtifact() || Link.OBJECT_FILETYPES.matches(basename));
          break;

        default:
          throw new IllegalStateException();
      }
      this.artifact = Preconditions.checkNotNull(artifact);
      this.category = category;
      this.disableWholeArchive = disableWholeArchive;
      this.libraryIdentifier = libraryIdentifier;
    }

    @Override
    public ArtifactCategory getArtifactCategory() {
      return category;
    }

    @Override
    public Artifact getArtifact() {
      return artifact;
    }

    @Override
    public Artifact getOriginalLibraryArtifact() {
      return artifact;
    }

    @Override
    public boolean containsObjectFiles() {
      return false;
    }

    @Override
    public Iterable<Artifact> getObjectFiles() {
      throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object that) {
      if (this == that) {
        return true;
      }

      if (!(that instanceof SimpleLinkerInput)) {
        return false;
      }

      SimpleLinkerInput other = (SimpleLinkerInput) that;
      return artifact.equals(other.artifact);
    }

    @Override
    public int hashCode() {
      return artifact.hashCode();
    }

    @Override
    public String toString() {
      return "SimpleLinkerInput(" + artifact + ")";
    }

    @Override
    public boolean isMustKeepDebug() {
      return false;
    }

    @Override
    public boolean disableWholeArchive() {
      return disableWholeArchive;
    }

    @Override
    public String getLibraryIdentifier() {
      return libraryIdentifier;
    }
  }

  @ThreadSafety.Immutable
  private static class LinkstampLinkerInput extends SimpleLinkerInput {
    private LinkstampLinkerInput(Artifact artifact, String libraryIdentifier) {
      super(
          artifact,
          ArtifactCategory.OBJECT_FILE,
          /* disableWholeArchive= */ false,
          libraryIdentifier);
      Preconditions.checkState(Link.OBJECT_FILETYPES.matches(artifact.getFilename()));
    }

    @Override
    public boolean isLinkstamp() {
      return true;
    }
  }

  /**
   * A library the user can link to. This is different from a simple linker input in that it also
   * has a library identifier.
   */
  public interface LibraryToLink extends LinkerInput {
    LtoCompilationContext getLtoCompilationContext();

    /**
     * Return a map of object file artifacts to associated LTOBackendArtifacts objects generated
     * when LTO backend actions are to be shared among different targets using this library. This is
     * the case when we opt not to perform the LTO indexing step, such as when building tests with
     * static linking. ThinLTO is otherwise too expensive when statically linking tests, due to the
     * number of LTO backends that can be generated for a single blaze test invocation.
     */
    ImmutableMap<Artifact, LtoBackendArtifacts> getSharedNonLtoBackends();
  }

  /**
   * This class represents a solib library symlink. Its library identifier is inherited from the
   * library that it links to.
   */
  @ThreadSafety.Immutable
  @AutoCodec
  public static class SolibLibraryToLink implements LibraryToLink {
    private final Artifact solibSymlinkArtifact;
    private final Artifact libraryArtifact;
    private final String libraryIdentifier;

    @AutoCodec.Instantiator
    @VisibleForSerialization
    SolibLibraryToLink(
        Artifact solibSymlinkArtifact, Artifact libraryArtifact, String libraryIdentifier) {
      Preconditions.checkArgument(
          Link.SHARED_LIBRARY_FILETYPES.matches(solibSymlinkArtifact.getFilename()));
      this.solibSymlinkArtifact = solibSymlinkArtifact;
      this.libraryArtifact = libraryArtifact;
      this.libraryIdentifier = libraryIdentifier;
    }

    @Override
    public String toString() {
      return String.format("SolibLibraryToLink(%s -> %s", solibSymlinkArtifact, libraryArtifact);
    }

    @Override
    public ArtifactCategory getArtifactCategory() {
      return ArtifactCategory.DYNAMIC_LIBRARY;
    }

    @Override
    public Artifact getArtifact() {
      return solibSymlinkArtifact;
    }

    @Override
    public String getLibraryIdentifier() {
      return libraryIdentifier;
    }

    @Override
    public boolean containsObjectFiles() {
      return false;
    }

    @Override
    public LtoCompilationContext getLtoCompilationContext() {
      return LtoCompilationContext.EMPTY;
    }

    @Override
    public Iterable<Artifact> getObjectFiles() {
      throw new IllegalStateException(
          "LinkerInputs: does not support getObjectFiles: " + toString());
    }

    @Override
    public ImmutableMap<Artifact, LtoBackendArtifacts> getSharedNonLtoBackends() {
      throw new IllegalStateException(
          "LinkerInputs: does not support getSharedNonLtoBackends: " + this);
    }

    @Override
    public Artifact getOriginalLibraryArtifact() {
      return libraryArtifact;
    }

    @Override
    public boolean equals(Object that) {
      if (this == that) {
        return true;
      }

      if (!(that instanceof SolibLibraryToLink)) {
        return false;
      }

      SolibLibraryToLink thatSolib = (SolibLibraryToLink) that;
      return solibSymlinkArtifact.equals(thatSolib.solibSymlinkArtifact)
          && libraryArtifact.equals(thatSolib.libraryArtifact);
    }

    @Override
    public int hashCode() {
      return solibSymlinkArtifact.hashCode();
    }

    @Override
    public boolean isMustKeepDebug() {
      return false;
    }

    @Override
    public boolean disableWholeArchive() {
      return false;
    }
  }

  /** This class represents a library that may contain object files. */
  @ThreadSafety.Immutable
  @AutoCodec
  @VisibleForSerialization
  static class CompoundLibraryToLink implements LibraryToLink {
    private final Artifact libraryArtifact;
    private final ArtifactCategory category;
    private final String libraryIdentifier;
    private final Iterable<Artifact> objectFiles;
    private final LtoCompilationContext ltoCompilationContext;
    private final ImmutableMap<Artifact, LtoBackendArtifacts> sharedNonLtoBackends;
    private final boolean mustKeepDebug;
    private final boolean disableWholeArchive;

    @AutoCodec.Instantiator
    @VisibleForSerialization
    CompoundLibraryToLink(
        Artifact libraryArtifact,
        ArtifactCategory category,
        String libraryIdentifier,
        Iterable<Artifact> objectFiles,
        LtoCompilationContext ltoCompilationContext,
        ImmutableMap<Artifact, LtoBackendArtifacts> sharedNonLtoBackends,
        boolean mustKeepDebug,
        boolean disableWholeArchive) {
      this.libraryArtifact = libraryArtifact;
      this.category = category;
      this.libraryIdentifier = libraryIdentifier;
      this.objectFiles = objectFiles;
      this.ltoCompilationContext = ltoCompilationContext;
      this.sharedNonLtoBackends = sharedNonLtoBackends;
      this.mustKeepDebug = mustKeepDebug;
      this.disableWholeArchive = disableWholeArchive;
    }

    private CompoundLibraryToLink(
        Artifact libraryArtifact,
        ArtifactCategory category,
        String libraryIdentifier,
        Iterable<Artifact> objectFiles,
        LtoCompilationContext ltoCompilationContext,
        ImmutableMap<Artifact, LtoBackendArtifacts> sharedNonLtoBackends,
        boolean allowArchiveTypeInAlwayslink,
        boolean mustKeepDebug,
        boolean disableWholeArchive) {
      String basename = libraryArtifact.getFilename();
      switch (category) {
        case ALWAYSLINK_STATIC_LIBRARY:
          Preconditions.checkState(
              Link.LINK_LIBRARY_FILETYPES.matches(basename)
                  || (allowArchiveTypeInAlwayslink && Link.ARCHIVE_FILETYPES.matches(basename)));
          break;

        case STATIC_LIBRARY:
          Preconditions.checkState(Link.ARCHIVE_FILETYPES.matches(basename));
          break;

        case INTERFACE_LIBRARY:
        case DYNAMIC_LIBRARY:
          Preconditions.checkState(Link.SHARED_LIBRARY_FILETYPES.matches(basename));
          break;

        default:
          throw new IllegalStateException();
      }

      this.libraryArtifact = Preconditions.checkNotNull(libraryArtifact);
      this.category = category;
      this.libraryIdentifier = libraryIdentifier;
      this.objectFiles = objectFiles == null ? null : CollectionUtils.makeImmutable(objectFiles);
      this.ltoCompilationContext =
          (ltoCompilationContext == null) ? LtoCompilationContext.EMPTY : ltoCompilationContext;
      this.sharedNonLtoBackends = sharedNonLtoBackends;
      this.mustKeepDebug = mustKeepDebug;
      this.disableWholeArchive = disableWholeArchive;
    }

    @Override
    public String toString() {
      return String.format("CompoundLibraryToLink(%s)", libraryArtifact.toString());
    }

    @Override
    public ArtifactCategory getArtifactCategory() {
      return category;
    }

    @Override
    public Artifact getArtifact() {
      return libraryArtifact;
    }

    @Override
    public Artifact getOriginalLibraryArtifact() {
      return libraryArtifact;
    }

    @Override
    public String getLibraryIdentifier() {
      return libraryIdentifier;
    }

    @Override
    public boolean containsObjectFiles() {
      return objectFiles != null;
    }

    @Override
    public ImmutableMap<Artifact, LtoBackendArtifacts> getSharedNonLtoBackends() {
      return sharedNonLtoBackends;
    }

    @Override
    public Iterable<Artifact> getObjectFiles() {
      Preconditions.checkNotNull(objectFiles);
      return objectFiles;
    }

    @Override
    public LtoCompilationContext getLtoCompilationContext() {
      return ltoCompilationContext;
    }

    @Override
    public boolean equals(Object that) {
      if (this == that) {
        return true;
      }

      if (!(that instanceof CompoundLibraryToLink)) {
        return false;
      }

      return libraryArtifact.equals(((CompoundLibraryToLink) that).libraryArtifact);
    }

    @Override
    public int hashCode() {
      return libraryArtifact.hashCode();
    }

    @Override
    public boolean isMustKeepDebug() {
      return this.mustKeepDebug;
    }

    @Override
    public boolean disableWholeArchive() {
      return disableWholeArchive;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // Public factory constructors:
  //////////////////////////////////////////////////////////////////////////////////////

  /** Creates linker input objects for non-library files. */
  public static Iterable<LinkerInput> simpleLinkerInputs(
      Iterable<Artifact> input, final ArtifactCategory category, boolean disableWholeArchive) {
    return Iterables.transform(
        input,
        artifact ->
            simpleLinkerInput(
                artifact, category, disableWholeArchive, artifact.getRootRelativePathString()));
  }

  public static Iterable<LinkerInput> linkstampLinkerInputs(Iterable<Artifact> input) {
    return Iterables.transform(
        input,
        artifact -> new LinkstampLinkerInput(artifact, artifact.getRootRelativePathString()));
  }

  /** Creates a linker input for which we do not know what objects files it consists of. */
  public static LinkerInput simpleLinkerInput(
      Artifact artifact,
      ArtifactCategory category,
      boolean disableWholeArchive,
      String libraryIdentifier) {
    // This precondition check was in place and *most* of the tests passed with them; the only
    // exception is when you mention a generated .a file in the srcs of a cc_* rule.
    // Preconditions.checkArgument(!ARCHIVE_LIBRARY_FILETYPES.contains(artifact.getFileType()));
    return new SimpleLinkerInput(artifact, category, disableWholeArchive, libraryIdentifier);
  }

  /**
   * Creates input libraries for which we do not know what objects files it consists of.
   */
  public static Iterable<LibraryToLink> opaqueLibrariesToLink(
      final ArtifactCategory category, Iterable<Artifact> input) {
    return Iterables.transform(input, artifact -> precompiledLibraryToLink(artifact, category));
  }

  /** Creates a solib library symlink from the given artifact. */
  public static LibraryToLink solibLibraryToLink(
      Artifact solibSymlink, Artifact original, String libraryIdentifier) {
    return new SolibLibraryToLink(solibSymlink, original, libraryIdentifier);
  }

  /** Creates an input library for which we do not know what objects files it consists of. */
  public static LibraryToLink precompiledLibraryToLink(
      Artifact artifact, ArtifactCategory category) {
    // This precondition check was in place and *most* of the tests passed with them; the only
    // exception is when you mention a generated .a file in the srcs of a cc_* rule.
    // It was very useful for proving that this actually works, though.
    // Preconditions.checkArgument(
    //     !(artifact.getGeneratingAction() instanceof CppLinkAction) ||
    //     !Link.ARCHIVE_LIBRARY_FILETYPES.contains(artifact.getFileType()));
    return new CompoundLibraryToLink(
        artifact,
        category,
        CcLinkingOutputs.libraryIdentifierOf(artifact),
        /* objectFiles= */ null,
        /* ltoCompilationContext= */ null,
        /* sharedNonLtoBackends= */ null,
        /* allowArchiveTypeInAlwayslink= */ false,
        /* mustKeepDebug= */ false,
        /* disableWholeArchive= */ false);
  }

  public static LibraryToLink opaqueLibraryToLink(
      Artifact artifact, ArtifactCategory category, String libraryIdentifier) {
    return new CompoundLibraryToLink(
        artifact,
        category,
        libraryIdentifier,
        /* objectFiles= */ null,
        /* ltoCompilationContext= */ null,
        /* sharedNonLtoBackends= */ null,
        /* allowArchiveTypeInAlwayslink= */ category.equals(
            ArtifactCategory.ALWAYSLINK_STATIC_LIBRARY),
        /* mustKeepDebug= */ false,
        /* disableWholeArchive= */ false);
  }

  public static LibraryToLink opaqueLibraryToLink(
      Artifact artifact,
      ArtifactCategory category,
      String libraryIdentifier,
      CppConfiguration.StripMode stripMode) {
    return new CompoundLibraryToLink(
        artifact,
        category,
        libraryIdentifier,
        /* objectFiles= */ null,
        /* ltoCompilationContext= */ null,
        /* sharedNonLtoBackends= */ null,
        /* allowArchiveTypeInAlwayslink= */ false,
        /* mustKeepDebug= */ stripMode == CppConfiguration.StripMode.NEVER,
        /* disableWholeArchive= */ false);
  }

  /** Creates a library to link with the specified object files. */
  public static LibraryToLink newInputLibrary(
      Artifact library,
      ArtifactCategory category,
      String libraryIdentifier,
      Iterable<Artifact> objectFiles,
      LtoCompilationContext ltoCompilationContext,
      ImmutableMap<Artifact, LtoBackendArtifacts> sharedNonLtoBackends,
      boolean mustKeepDebug) {
    return newInputLibrary(
        library,
        category,
        libraryIdentifier,
        objectFiles,
        ltoCompilationContext,
        sharedNonLtoBackends,
        mustKeepDebug,
        /* disableWholeArchive= */ false);
  }

  /** Creates a library to link with the specified object files. */
  @VisibleForTesting
  public static LibraryToLink newInputLibrary(
      Artifact library,
      ArtifactCategory category,
      String libraryIdentifier,
      Iterable<Artifact> objectFiles,
      LtoCompilationContext ltoCompilationContext,
      ImmutableMap<Artifact, LtoBackendArtifacts> sharedNonLtoBackends,
      boolean mustKeepDebug,
      boolean disableWholeArchive) {
    return new CompoundLibraryToLink(
        library,
        category,
        libraryIdentifier,
        objectFiles,
        ltoCompilationContext,
        sharedNonLtoBackends,
        /* allowArchiveTypeInAlwayslink= */ true,
        mustKeepDebug,
        disableWholeArchive);
  }

  public static Iterable<Artifact> toNonSolibArtifacts(Iterable<LibraryToLink> libraries) {
    return Iterables.transform(libraries, LibraryToLink::getOriginalLibraryArtifact);
  }

  /** Returns the linker input artifacts from a collection of {@link LinkerInput} objects. */
  public static Iterable<Artifact> toLibraryArtifacts(Iterable<? extends LinkerInput> artifacts) {
    return Iterables.transform(artifacts, LinkerInput::getArtifact);
  }
}
