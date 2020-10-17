// Copyright 2017 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.analysis.starlark;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Interner;
import com.google.common.collect.Sets;
import com.google.common.flogger.GoogleLogger;
import com.google.devtools.build.lib.actions.ActionKeyContext;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.Artifact.ArtifactExpander;
import com.google.devtools.build.lib.actions.Artifact.MissingExpansionException;
import com.google.devtools.build.lib.actions.CommandLine;
import com.google.devtools.build.lib.actions.CommandLineExpansionException;
import com.google.devtools.build.lib.actions.CommandLineItem;
import com.google.devtools.build.lib.actions.FilesetManifest;
import com.google.devtools.build.lib.actions.FilesetManifest.RelativeSymlinkBehavior;
import com.google.devtools.build.lib.actions.FilesetOutputSymlink;
import com.google.devtools.build.lib.actions.SingleStringArgFormatter;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.concurrent.BlazeInterners;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.starlarkbuildapi.DirectoryExpander;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.FileRootApi;
import com.google.devtools.build.lib.util.Fingerprint;
import com.google.devtools.build.lib.vfs.PathFragment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.Location;

/** Supports ctx.actions.args() from Starlark. */
@AutoCodec
public class StarlarkCustomCommandLine extends CommandLine {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final StarlarkSemantics starlarkSemantics;
  private final ImmutableList<Object> arguments;
  /**
   * If non-empty, an extra level of grouping on top of the 'arguments' list. Each element is the
   * beginning of a group of args. For example, if this contains 0 and 3, then arguments 0, 1 and 2
   * constitute the first group, and arguments 3 to the end constitute the next. The expanded
   * version of these arguments will be concatenated together to support flag_per_line format.
   */
  private final ImmutableList<Integer> argStartIndexes;

  private static final Joiner LINE_JOINER = Joiner.on("\n").skipNulls();
  private static final Joiner FIELD_JOINER = Joiner.on(": ").skipNulls();

  @AutoCodec
  static final class VectorArg {
    private static final Interner<VectorArg> interner = BlazeInterners.newStrongInterner();

    private static final int HAS_LOCATION = 1;
    // Deleted HAS_MAP_ALL = 1 << 1;
    private static final int HAS_MAP_EACH = 1 << 2;
    private static final int IS_NESTED_SET = 1 << 3;
    private static final int EXPAND_DIRECTORIES = 1 << 4;
    private static final int UNIQUIFY = 1 << 5;
    private static final int OMIT_IF_EMPTY = 1 << 6;
    private static final int HAS_ARG_NAME = 1 << 7;
    private static final int HAS_FORMAT_EACH = 1 << 8;
    private static final int HAS_BEFORE_EACH = 1 << 9;
    private static final int HAS_JOIN_WITH = 1 << 10;
    private static final int HAS_FORMAT_JOINED = 1 << 11;
    private static final int HAS_TERMINATE_WITH = 1 << 12;

    private static final UUID EXPAND_DIRECTORIES_UUID =
        UUID.fromString("9d7520d2-a187-11e8-98d0-529269fb1459");
    private static final UUID UNIQUIFY_UUID =
        UUID.fromString("7f494c3e-faea-4498-a521-5d3bc6ee19eb");
    private static final UUID OMIT_IF_EMPTY_UUID =
        UUID.fromString("923206f1-6474-4a8f-b30f-4dd3143622e6");
    private static final UUID ARG_NAME_UUID =
        UUID.fromString("2bc00382-7199-46ec-ad52-1556577cde1a");
    private static final UUID FORMAT_EACH_UUID =
        UUID.fromString("8e974aec-df07-4a51-9418-f4c1172b4045");
    private static final UUID BEFORE_EACH_UUID =
        UUID.fromString("f7e101bc-644d-4277-8562-6515ad55a988");
    private static final UUID JOIN_WITH_UUID =
        UUID.fromString("c227dbd3-edad-454e-bc8a-c9b5ba1c38a3");
    private static final UUID FORMAT_JOINED_UUID =
        UUID.fromString("528af376-4233-4c27-be4d-b0ff24ed68db");
    private static final UUID TERMINATE_WITH_UUID =
        UUID.fromString("a4e5e090-0dbd-4d41-899a-77cfbba58655");

    private final int features;

    private VectorArg(int features) {
      this.features = features;
    }

    @AutoCodec.VisibleForSerialization
    @AutoCodec.Instantiator
    static VectorArg create(int features) {
      return interner.intern(new VectorArg(features));
    }

    private static void push(List<Object> arguments, Builder arg) {
      int features = 0;
      features |= arg.mapEach != null ? HAS_MAP_EACH : 0;
      features |= arg.nestedSet != null ? IS_NESTED_SET : 0;
      features |= arg.expandDirectories ? EXPAND_DIRECTORIES : 0;
      features |= arg.uniquify ? UNIQUIFY : 0;
      features |= arg.omitIfEmpty ? OMIT_IF_EMPTY : 0;
      features |= arg.argName != null ? HAS_ARG_NAME : 0;
      features |= arg.formatEach != null ? HAS_FORMAT_EACH : 0;
      features |= arg.beforeEach != null ? HAS_BEFORE_EACH : 0;
      features |= arg.joinWith != null ? HAS_JOIN_WITH : 0;
      features |= arg.formatJoined != null ? HAS_FORMAT_JOINED : 0;
      features |= arg.terminateWith != null ? HAS_TERMINATE_WITH : 0;
      boolean hasLocation =
          arg.location != null
              && (features & (HAS_FORMAT_EACH | HAS_FORMAT_JOINED | HAS_MAP_EACH)) != 0;
      features |= hasLocation ? HAS_LOCATION : 0;
      VectorArg vectorArg = VectorArg.create(features);
      arguments.add(vectorArg);
      if (hasLocation) {
        arguments.add(arg.location);
      }
      if (arg.mapEach != null) {
        arguments.add(arg.mapEach);
      }
      if (arg.nestedSet != null) {
        arguments.add(arg.nestedSet);
      } else {
        List<?> list = arg.list;
        int count = list.size();
        arguments.add(count);
        for (int i = 0; i < count; ++i) {
          arguments.add(list.get(i));
        }
      }
      if (arg.argName != null) {
        arguments.add(arg.argName);
      }
      if (arg.formatEach != null) {
        arguments.add(arg.formatEach);
      }
      if (arg.beforeEach != null) {
        arguments.add(arg.beforeEach);
      }
      if (arg.joinWith != null) {
        arguments.add(arg.joinWith);
      }
      if (arg.formatJoined != null) {
        arguments.add(arg.formatJoined);
      }
      if (arg.terminateWith != null) {
        arguments.add(arg.terminateWith);
      }
    }

    private int eval(
        List<Object> arguments,
        int argi,
        List<String> builder,
        @Nullable ArtifactExpander artifactExpander,
        StarlarkSemantics starlarkSemantics)
        throws CommandLineExpansionException {
      final Location location =
          ((features & HAS_LOCATION) != 0) ? (Location) arguments.get(argi++) : null;
      final List<Object> originalValues;
      StarlarkCallable mapEach =
          ((features & HAS_MAP_EACH) != 0) ? (StarlarkCallable) arguments.get(argi++) : null;
      if ((features & IS_NESTED_SET) != 0) {
        @SuppressWarnings("unchecked")
        NestedSet<Object> nestedSet = (NestedSet<Object>) arguments.get(argi++);
        originalValues = nestedSet.toList();
      } else {
        int count = (Integer) arguments.get(argi++);
        originalValues = arguments.subList(argi, argi + count);
        argi += count;
      }
      List<Object> expandedValues = maybeExpandDirectories(artifactExpander, originalValues);
      List<String> stringValues;
      if (mapEach != null) {
        stringValues = new ArrayList<>(expandedValues.size());
        applyMapEach(
            mapEach,
            expandedValues,
            stringValues::add,
            location,
            artifactExpander,
            starlarkSemantics);
      } else {
        int count = expandedValues.size();
        stringValues = new ArrayList<>(expandedValues.size());
        for (int i = 0; i < count; ++i) {
          stringValues.add(CommandLineItem.expandToCommandLine(expandedValues.get(i)));
        }
      }
      // It's safe to uniquify at this stage, any transformations after this
      // will ensure continued uniqueness of the values
      if ((features & UNIQUIFY) != 0) {
        int count = stringValues.size();
        HashSet<String> seen = Sets.newHashSetWithExpectedSize(count);
        int addIndex = 0;
        for (int i = 0; i < count; ++i) {
          String val = stringValues.get(i);
          if (seen.add(val)) {
            stringValues.set(addIndex++, val);
          }
        }
        stringValues = stringValues.subList(0, addIndex);
      }
      boolean isEmptyAndShouldOmit = stringValues.isEmpty() && (features & OMIT_IF_EMPTY) != 0;
      if ((features & HAS_ARG_NAME) != 0) {
        String argName = (String) arguments.get(argi++);
        if (!isEmptyAndShouldOmit) {
          builder.add(argName);
        }
      }
      if ((features & HAS_FORMAT_EACH) != 0) {
        String formatStr = (String) arguments.get(argi++);
        try {
          int count = stringValues.size();
          for (int i = 0; i < count; ++i) {
            stringValues.set(i, SingleStringArgFormatter.format(formatStr, stringValues.get(i)));
          }
        } catch (IllegalFormatException e) {
          throw new CommandLineExpansionException(errorMessage(e.getMessage(), location, null));
        }
      }
      if ((features & HAS_BEFORE_EACH) != 0) {
        String beforeEach = (String) arguments.get(argi++);
        int count = stringValues.size();
        for (int i = 0; i < count; ++i) {
          builder.add(beforeEach);
          builder.add(stringValues.get(i));
        }
      } else if ((features & HAS_JOIN_WITH) != 0) {
        String joinWith = (String) arguments.get(argi++);
        String formatJoined =
            ((features & HAS_FORMAT_JOINED) != 0) ? (String) arguments.get(argi++) : null;
        if (!isEmptyAndShouldOmit) {
          String result = Joiner.on(joinWith).join(stringValues);
          if (formatJoined != null) {
            try {
              result = SingleStringArgFormatter.format(formatJoined, result);
            } catch (IllegalFormatException e) {
              throw new CommandLineExpansionException(errorMessage(e.getMessage(), location, null));
            }
          }
          builder.add(result);
        }
      } else {
        builder.addAll(stringValues);
      }
      if ((features & HAS_TERMINATE_WITH) != 0) {
        String terminateWith = (String) arguments.get(argi++);
        if (!isEmptyAndShouldOmit) {
          builder.add(terminateWith);
        }
      }
      return argi;
    }

    /**
     * Expands the directories if {@code expand_directories} feature is enabled and a
     * ArtifactExpander is available.
     *
     * <p>Technically, we should always expand the directories if the feature is requested, however
     * we cannot do that in the absence of the {@link ArtifactExpander}.
     */
    private List<Object> maybeExpandDirectories(
        @Nullable ArtifactExpander artifactExpander, List<Object> originalValues)
        throws CommandLineExpansionException {
      if ((features & EXPAND_DIRECTORIES) == 0
          || artifactExpander == null
          || !hasDirectory(originalValues)) {
        return originalValues;
      }

      return expandDirectories(artifactExpander, originalValues);
    }

    private static boolean hasDirectory(List<Object> originalValues) {
      int n = originalValues.size();
      for (int i = 0; i < n; ++i) {
        Object object = originalValues.get(i);
        if (isDirectory(object)) {
          return true;
        }
      }
      return false;
    }

    private static boolean isDirectory(Object object) {
      return ((object instanceof Artifact) && ((Artifact) object).isDirectory());
    }

    private static List<Object> expandDirectories(
        Artifact.ArtifactExpander artifactExpander, List<Object> originalValues)
        throws CommandLineExpansionException {
      List<Object> expandedValues = new ArrayList<>(originalValues.size());
      for (Object object : originalValues) {
        if (isDirectory(object)) {
          Artifact artifact = (Artifact) object;
          if (artifact.isTreeArtifact()) {
            artifactExpander.expand((Artifact) object, expandedValues);
          } else if (artifact.isFileset()) {
            expandFileset(artifactExpander, artifact, expandedValues);
          } else {
            throw new AssertionError("Unknown artifact type.");
          }
        } else {
          expandedValues.add(object);
        }
      }
      return expandedValues;
    }

    private static void expandFileset(
        Artifact.ArtifactExpander artifactExpander, Artifact fileset, List<Object> expandedValues)
        throws CommandLineExpansionException {
      ImmutableList<FilesetOutputSymlink> expandedFileSet;
      try {
        expandedFileSet = artifactExpander.getFileset(fileset);
      } catch (MissingExpansionException e) {
        throw new CommandLineExpansionException(
            String.format(
                "Could not expand fileset: %s. Did you forget to add it as an input of the"
                    + " action?",
                fileset),
            e);
      }
      try {
        FilesetManifest filesetManifest =
            FilesetManifest.constructFilesetManifest(
                expandedFileSet, fileset.getExecPath(), RelativeSymlinkBehavior.IGNORE);
        for (PathFragment relativePath : filesetManifest.getEntries().keySet()) {
          expandedValues.add(new FilesetSymlinkFile(fileset, relativePath));
        }
      } catch (IOException e) {
        throw new CommandLineExpansionException("Could not expand fileset: " + e.getMessage());
      }
    }

    private int addToFingerprint(
        List<Object> arguments,
        int argi,
        ActionKeyContext actionKeyContext,
        Fingerprint fingerprint,
        StarlarkSemantics starlarkSemantics,
        @Nullable ArtifactExpander artifactExpander)
        throws CommandLineExpansionException {
      final Location location =
          ((features & HAS_LOCATION) != 0) ? (Location) arguments.get(argi++) : null;
      StarlarkCallable mapEach =
          ((features & HAS_MAP_EACH) != 0) ? (StarlarkCallable) arguments.get(argi++) : null;
      if ((features & IS_NESTED_SET) != 0) {
        NestedSet<?> values = (NestedSet<?>) arguments.get(argi++);
        if (mapEach != null) {
          CommandLineItemMapEachAdaptor commandLineItemMapFn =
              new CommandLineItemMapEachAdaptor(
                  mapEach,
                  location,
                  starlarkSemantics,
                  (features & EXPAND_DIRECTORIES) != 0 ? artifactExpander : null);
          try {
            actionKeyContext.addNestedSetToFingerprint(commandLineItemMapFn, fingerprint, values);
          } catch (UncheckedCommandLineExpansionException e) {
            // We wrap the CommandLineExpansionException below, unwrap here
            throw e.cause;
          } finally {
            // The cache holds an entry for a NestedSet for every (map_fn, hasArtifactExpanderBit).
            // Clearing the artifactExpander itself saves us from storing the contents of it in the
            // cache keys (it is no longer needed after we evaluate the value).
            // NestedSet cache is cleared after every build, which means that the artifactExpander
            // for a given action, if present, cannot change within the lifetime of the fingerprint
            // cache (we call getKey with artifactExpander to check action key, when we are ready to
            // execute the action in case of a cache miss).
            commandLineItemMapFn.clearArtifactExpander();
          }
        } else {
          actionKeyContext.addNestedSetToFingerprint(fingerprint, values);
        }
      } else {
        int count = (Integer) arguments.get(argi++);
        List<Object> maybeExpandedValues =
            maybeExpandDirectories(artifactExpander, arguments.subList(argi, argi + count));
        argi += count;
        if (mapEach != null) {
          // TODO(b/160181927): If artifactExpander == null (which happens in the analysis phase)
          // but expandDirectories is true, we run the map_each function on directory values without
          // actually expanding them. This differs from the real evaluation behavior. This means
          // that we can erroneously produce the same digest for two command lines that differ only
          // in their directory expansion. Fortunately, this is only a problem for shared action
          // conflict checking/aquery result, since at execution time we have an artifactExpander.
          applyMapEach(
              mapEach,
              maybeExpandedValues,
              fingerprint::addString,
              location,
              artifactExpander,
              starlarkSemantics);
        } else {
          for (Object value : maybeExpandedValues) {
            fingerprint.addString(CommandLineItem.expandToCommandLine(value));
          }
        }
      }
      if ((features & EXPAND_DIRECTORIES) != 0) {
        fingerprint.addUUID(EXPAND_DIRECTORIES_UUID);
      }
      if ((features & UNIQUIFY) != 0) {
        fingerprint.addUUID(UNIQUIFY_UUID);
      }
      if ((features & OMIT_IF_EMPTY) != 0) {
        fingerprint.addUUID(OMIT_IF_EMPTY_UUID);
      }
      if ((features & HAS_ARG_NAME) != 0) {
        String argName = (String) arguments.get(argi++);
        fingerprint.addUUID(ARG_NAME_UUID);
        fingerprint.addString(argName);
      }
      if ((features & HAS_FORMAT_EACH) != 0) {
        String formatStr = (String) arguments.get(argi++);
        fingerprint.addUUID(FORMAT_EACH_UUID);
        fingerprint.addString(formatStr);
      }
      if ((features & HAS_BEFORE_EACH) != 0) {
        String beforeEach = (String) arguments.get(argi++);
        fingerprint.addUUID(BEFORE_EACH_UUID);
        fingerprint.addString(beforeEach);
      } else if ((features & HAS_JOIN_WITH) != 0) {
        String joinWith = (String) arguments.get(argi++);
        fingerprint.addUUID(JOIN_WITH_UUID);
        fingerprint.addString(joinWith);
        if ((features & HAS_FORMAT_JOINED) != 0) {
          String formatJoined = (String) arguments.get(argi++);
          fingerprint.addUUID(FORMAT_JOINED_UUID);
          fingerprint.addString(formatJoined);
        }
      }
      if ((features & HAS_TERMINATE_WITH) != 0) {
        String terminateWith = (String) arguments.get(argi++);
        fingerprint.addUUID(TERMINATE_WITH_UUID);
        fingerprint.addString(terminateWith);
      }
      return argi;
    }

    static class Builder {
      @Nullable private final Sequence<?> list;
      @Nullable private final NestedSet<?> nestedSet;
      private Location location;
      public String argName;
      private boolean expandDirectories;
      private StarlarkCallable mapEach;
      private String formatEach;
      private String beforeEach;
      private String joinWith;
      private String formatJoined;
      private boolean omitIfEmpty;
      private boolean uniquify;
      private String terminateWith;

      Builder(Sequence<?> list) {
        this.list = list;
        this.nestedSet = null;
      }

      Builder(NestedSet<?> nestedSet) {
        this.list = null;
        this.nestedSet = nestedSet;
      }

      Builder setLocation(Location location) {
        this.location = location;
        return this;
      }

      Builder setArgName(String argName) {
        this.argName = argName;
        return this;
      }

      Builder setExpandDirectories(boolean expandDirectories) {
        this.expandDirectories = expandDirectories;
        return this;
      }

      Builder setMapEach(StarlarkCallable mapEach) {
        this.mapEach = mapEach;
        return this;
      }

      Builder setFormatEach(String format) {
        this.formatEach = format;
        return this;
      }

      Builder setBeforeEach(String beforeEach) {
        this.beforeEach = beforeEach;
        return this;
      }

      Builder setJoinWith(String joinWith) {
        this.joinWith = joinWith;
        return this;
      }

      Builder setFormatJoined(String formatJoined) {
        this.formatJoined = formatJoined;
        return this;
      }

      Builder omitIfEmpty(boolean omitIfEmpty) {
        this.omitIfEmpty = omitIfEmpty;
        return this;
      }

      Builder uniquify(boolean uniquify) {
        this.uniquify = uniquify;
        return this;
      }

      Builder setTerminateWith(String terminateWith) {
        this.terminateWith = terminateWith;
        return this;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      VectorArg vectorArg = (VectorArg) o;
      return features == vectorArg.features;
    }

    @Override
    public int hashCode() {
      return Integer.hashCode(features);
    }
  }

  @AutoCodec
  static final class ScalarArg {
    private static final Interner<ScalarArg> interner = BlazeInterners.newStrongInterner();
    private static final UUID FORMAT_UUID = UUID.fromString("8cb96642-a235-4fe0-b3ed-ebfdae8a0bd9");

    private final boolean hasFormat;

    private ScalarArg(boolean hasFormat) {
      this.hasFormat = hasFormat;
    }

    @AutoCodec.VisibleForSerialization
    @AutoCodec.Instantiator
    static ScalarArg create(boolean hasFormat) {
      return interner.intern(new ScalarArg(hasFormat));
    }

    private static void push(List<Object> arguments, Builder arg) {
      ScalarArg scalarArg = ScalarArg.create(arg.format != null);
      arguments.add(scalarArg);
      arguments.add(arg.object);
      if (scalarArg.hasFormat) {
        arguments.add(arg.format);
      }
    }

    private int eval(List<Object> arguments, int argi, List<String> builder)
        throws CommandLineExpansionException {
      Object object = arguments.get(argi++);
      String stringValue = CommandLineItem.expandToCommandLine(object);
      if (hasFormat) {
        String formatStr = (String) arguments.get(argi++);
        stringValue = SingleStringArgFormatter.format(formatStr, stringValue);
      }
      builder.add(stringValue);
      return argi;
    }

    private int addToFingerprint(List<Object> arguments, int argi, Fingerprint fingerprint)
        throws CommandLineExpansionException {
      Object object = arguments.get(argi++);
      String stringValue = CommandLineItem.expandToCommandLine(object);
      fingerprint.addString(stringValue);
      if (hasFormat) {
        String formatStr = (String) arguments.get(argi++);
        fingerprint.addUUID(FORMAT_UUID);
        fingerprint.addString(formatStr);
      }
      return argi;
    }

    static class Builder {
      private final Object object;
      private String format;

      Builder(Object object) {
        this.object = object;
      }

      Builder setFormat(String format) {
        this.format = format;
        return this;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ScalarArg scalarArg = (ScalarArg) o;
      return hasFormat == scalarArg.hasFormat;
    }

    @Override
    public int hashCode() {
      return Boolean.hashCode(hasFormat);
    }
  }

  static class Builder {
    private final StarlarkSemantics starlarkSemantics;
    private final List<Object> arguments = new ArrayList<>();
    // Indexes in arguments list where individual args begin
    private final List<Integer> argStartIndexes = new ArrayList<>();

    public Builder(StarlarkSemantics starlarkSemantics) {
      this.starlarkSemantics = starlarkSemantics;
    }

    Builder recordArgStart() {
      argStartIndexes.add(arguments.size());
      return this;
    }

    Builder add(Object object) {
      arguments.add(object);
      return this;
    }

    Builder add(VectorArg.Builder vectorArg) {
      VectorArg.push(arguments, vectorArg);
      return this;
    }

    Builder add(ScalarArg.Builder scalarArg) {
      ScalarArg.push(arguments, scalarArg);
      return this;
    }

    StarlarkCustomCommandLine build(boolean flagPerLine) {
      return new StarlarkCustomCommandLine(
          starlarkSemantics,
          ImmutableList.copyOf(arguments),
          flagPerLine ? ImmutableList.copyOf(argStartIndexes) : ImmutableList.of());
    }
  }

  @AutoCodec.VisibleForSerialization
  @AutoCodec.Instantiator
  StarlarkCustomCommandLine(
      StarlarkSemantics starlarkSemantics,
      ImmutableList<Object> arguments,
      ImmutableList<Integer> argStartIndexes) {
    this.arguments = arguments;
    this.starlarkSemantics = starlarkSemantics;
    this.argStartIndexes = argStartIndexes;
  }

  @Override
  public Iterable<String> arguments() throws CommandLineExpansionException {
    return arguments(null);
  }

  @Override
  public Iterable<String> arguments(@Nullable ArtifactExpander artifactExpander)
      throws CommandLineExpansionException {
    List<String> result = new ArrayList<>();

    // If we're grouping arguments, keep track of the result indexes corresponding to the
    // argStartIndexes, reflecting VectorArg and ScalarArg expansion.
    List<Integer> resultGroupStarts =
        argStartIndexes.isEmpty() ? ImmutableList.of() : new ArrayList<>();
    Iterator<Integer> startIndexIterator = argStartIndexes.iterator();
    int nextStartIndex = startIndexIterator.hasNext() ? startIndexIterator.next() : -1;

    for (int argi = 0; argi < arguments.size(); ) {

      // If we're grouping arguments, record the actual beginning of each group
      if (argi == nextStartIndex) {
        resultGroupStarts.add(result.size());
        nextStartIndex = startIndexIterator.hasNext() ? startIndexIterator.next() : -1;
      }

      Object arg = arguments.get(argi++);
      if (arg instanceof VectorArg) {
        argi = ((VectorArg) arg).eval(arguments, argi, result, artifactExpander, starlarkSemantics);
      } else if (arg instanceof ScalarArg) {
        argi = ((ScalarArg) arg).eval(arguments, argi, result);
      } else {
        result.add(CommandLineItem.expandToCommandLine(arg));
      }
    }

    if (argStartIndexes.isEmpty()) {
      // Normal case, no further grouping
      return ImmutableList.copyOf(result);
    }

    // Grouped case -- concatenate results.
    ImmutableList.Builder<String> groupedBuilder = ImmutableList.builder();
    int numStarts = resultGroupStarts.size();
    resultGroupStarts.add(result.size());
    for (int i = 0; i < numStarts; i++) {
      // Arguments that constitute a single group
      List<String> group = result.subList(resultGroupStarts.get(i), resultGroupStarts.get(i + 1));
      if (group.size() < 2) {
        groupedBuilder.addAll(group);
      } else {
        // "--x=y z", or just "y z"
        String first = group.get(0);
        String rest = String.join(" ", group.subList(1, group.size()));
        groupedBuilder.add(first.isEmpty() ? rest : (first + '=' + rest));
      }
    }
    return groupedBuilder.build();
  }

  @Override
  public void addToFingerprint(
      ActionKeyContext actionKeyContext,
      @Nullable ArtifactExpander artifactExpander,
      Fingerprint fingerprint)
      throws CommandLineExpansionException {
    for (int argi = 0; argi < arguments.size(); ) {
      Object arg = arguments.get(argi++);
      if (arg instanceof VectorArg) {
        argi =
            ((VectorArg) arg)
                .addToFingerprint(
                    arguments,
                    argi,
                    actionKeyContext,
                    fingerprint,
                    starlarkSemantics,
                    artifactExpander);
      } else if (arg instanceof ScalarArg) {
        argi = ((ScalarArg) arg).addToFingerprint(arguments, argi, fingerprint);
      } else {
        fingerprint.addString(CommandLineItem.expandToCommandLine(arg));
      }
    }
  }

  /** Used during action key evaluation when we don't have an artifact expander. */
  private static class NoopExpander implements DirectoryExpander {
    @Override
    public ImmutableList<FileApi> list(FileApi file) {
      return ImmutableList.of(file);
    }

    static final DirectoryExpander INSTANCE = new NoopExpander();
  }

  private static class FullExpander implements DirectoryExpander {
    ArtifactExpander expander;

    FullExpander(ArtifactExpander expander) {
      this.expander = expander;
    }

    @Override
    public ImmutableList<FileApi> list(FileApi file) {
      Artifact artifact = (Artifact) file;
      if (artifact.isTreeArtifact()) {
        List<Artifact> files = new ArrayList<>(1);
        expander.expand((Artifact) file, files);
        return ImmutableList.<FileApi>copyOf(files);
      } else {
        return ImmutableList.of(file);
      }
    }
  }

  private static void applyMapEach(
      StarlarkCallable mapFn,
      List<Object> originalValues,
      Consumer<String> consumer,
      Location loc,
      @Nullable ArtifactExpander artifactExpander,
      StarlarkSemantics starlarkSemantics)
      throws CommandLineExpansionException {
    try (Mutability mu = Mutability.create("map_each")) {
      StarlarkThread thread = new StarlarkThread(mu, starlarkSemantics);
      // TODO(b/77140311): Error if we issue print statements.
      thread.setPrintHandler((th, msg) -> {});
      int count = originalValues.size();
      // map_each can accept either each object, or each object + a directory expander.
      boolean wantsDirectoryExpander =
          (mapFn instanceof StarlarkFunction)
              && ((StarlarkFunction) mapFn).getParameterNames().size() >= 2;
      // We create a list that we reuse for the args to map_each
      List<Object> args = new ArrayList<>(2);
      args.add(null); // This will be overwritten each iteration.
      if (wantsDirectoryExpander) {
        final DirectoryExpander expander;
        if (artifactExpander != null) {
          expander = new FullExpander(artifactExpander);
        } else {
          expander = NoopExpander.INSTANCE;
        }
        args.add(expander); // This will remain constant each iteration
      }
      for (int i = 0; i < count; ++i) {
        args.set(0, originalValues.get(i));
        Object ret = Starlark.call(thread, mapFn, args, /*kwargs=*/ ImmutableMap.of());
        if (ret instanceof String) {
          consumer.accept((String) ret);
        } else if (ret instanceof Sequence) {
          for (Object val : ((Sequence) ret)) {
            if (!(val instanceof String)) {
              throw new CommandLineExpansionException(
                  "Expected map_each to return string, None, or list of strings, "
                      + "found list containing "
                      + Starlark.type(val));
            }
            consumer.accept((String) val);
          }
        } else if (ret != Starlark.NONE) {
          throw new CommandLineExpansionException(
              "Expected map_each to return string, None, or list of strings, found "
                  + Starlark.type(ret));
        }
      }
    } catch (EvalException e) {
      // TODO(adonovan): consider calling a wrapper function to interpose a fake stack
      // frame that establishes the args.add_all call at loc. Or manipulating the stack
      // before printing it.
      throw new CommandLineExpansionException(
          errorMessage(e.getMessageWithStack(), loc, e.getCause()));
    } catch (InterruptedException e) {
      logger.atWarning().withCause(e).log("Interrupted while applying map_each at %s", loc);
      Thread.currentThread().interrupt();
      throw new CommandLineExpansionException(errorMessage("Thread was interrupted", loc, null));
    }
  }

  private static class CommandLineItemMapEachAdaptor
      extends CommandLineItem.ParametrizedMapFn<Object> {
    private final StarlarkCallable mapFn;
    private final Location location;
    private final StarlarkSemantics starlarkSemantics;
    /**
     * Indicates whether artifactExpander was provided on construction. This is used to distinguish
     * the case where it's not provided from the case where it was provided but subsequently
     * cleared.
     */
    private final boolean hasArtifactExpander;

    @Nullable private ArtifactExpander artifactExpander;

    CommandLineItemMapEachAdaptor(
        StarlarkCallable mapFn,
        Location location,
        StarlarkSemantics starlarkSemantics,
        @Nullable ArtifactExpander artifactExpander) {
      this.mapFn = mapFn;
      this.location = location;
      this.starlarkSemantics = starlarkSemantics;
      this.hasArtifactExpander = artifactExpander != null;
      this.artifactExpander = artifactExpander;
    }

    @Override
    public void expandToCommandLine(Object object, Consumer<String> args) {
      Preconditions.checkState(artifactExpander != null || !hasArtifactExpander);
      try {
        applyMapEach(
            mapFn,
            maybeExpandDirectory(object),
            args,
            location,
            artifactExpander,
            starlarkSemantics);
      } catch (CommandLineExpansionException e) {
        // Rather than update CommandLineItem#expandToCommandLine and the numerous callers,
        // we wrap this in a runtime exception and handle it above
        throw new UncheckedCommandLineExpansionException(e);
      }
    }

    private List<Object> maybeExpandDirectory(Object object) throws CommandLineExpansionException {
      if (artifactExpander == null || !VectorArg.isDirectory(object)) {
        return ImmutableList.of(object);
      }

      return VectorArg.expandDirectories(artifactExpander, ImmutableList.of(object));
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof CommandLineItemMapEachAdaptor)) {
        return false;
      }
      CommandLineItemMapEachAdaptor other = (CommandLineItemMapEachAdaptor) obj;
      // Instance compare intentional
      // The normal implementation uses location + name of function,
      // which can conceivably conflict in tests
      // We only compare presence of artifactExpander vs absence of it since the nestedset
      // fingerprint cache is emptied after every build, therefore if the artifact expander is
      // provided, it will be the same.
      return mapFn == other.mapFn && hasArtifactExpander == other.hasArtifactExpander;
    }

    @Override
    public int hashCode() {
      // Force use of identityHashCode, in case the callable uses a custom hash function. (As of
      // this writing, only providers seem to have a custom hashCode, and those shouldn't be used
      // as map_each functions, but doesn't hurt to be safe...).
      return 31 * System.identityHashCode(mapFn) + Boolean.hashCode(hasArtifactExpander);
    }

    @Override
    public int maxInstancesAllowed() {
      // No limit to these, as this is just a wrapper for Starlark functions, which are
      // always static
      return Integer.MAX_VALUE;
    }

    /**
     * Clears the artifact expander in order not to prolong the lifetime of it unnecessarily.
     *
     * <p>Although this operation technically changes this object, it can be called after we add the
     * object to a {@link HashSet}. Clearing the artifactExpander does not affect the result of
     * {@link #equals} or {@link #hashCode}. Please note that once we call this function, we can no
     * longer call {@link #expandToCommandLine}.
     */
    void clearArtifactExpander() {
      artifactExpander = null;
    }
  }

  private static String errorMessage(
      String message, @Nullable Location location, @Nullable Throwable cause) {
    return LINE_JOINER.join(
        "\n", FIELD_JOINER.join(location, message), getCauseMessage(cause, message));
  }

  private static String getCauseMessage(@Nullable Throwable cause, String message) {
    if (cause == null) {
      return null;
    }
    String causeMessage = cause.getMessage();
    if (causeMessage == null) {
      return null;
    }
    if (message == null) {
      return causeMessage;
    }
    // Skip the cause if it is redundant with the message so far.
    if (message.contains(causeMessage)) {
      return null;
    }
    return causeMessage;
  }

  private static class UncheckedCommandLineExpansionException extends RuntimeException {
    final CommandLineExpansionException cause;

    UncheckedCommandLineExpansionException(CommandLineExpansionException cause) {
      this.cause = cause;
    }
  }

  /**
   * When we expand filesets the user might still expect a File object (since the results may be fed
   * into map_each. Therefore we synthesize a File object from the fileset symlink.
   */
  static class FilesetSymlinkFile implements FileApi, CommandLineItem {
    private final Artifact fileset;
    private final PathFragment execPath;

    FilesetSymlinkFile(Artifact fileset, PathFragment execPath) {
      this.fileset = fileset;
      this.execPath = execPath;
    }

    private PathFragment getExecPath() {
      return execPath;
    }

    @Override
    public String getDirname() {
      PathFragment parent = getExecPath().getParentDirectory();
      return (parent == null) ? "/" : parent.getSafePathString();
    }

    @Override
    public String getFilename() {
      return getExecPath().getBaseName();
    }

    @Override
    public String getExtension() {
      return getExecPath().getFileExtension();
    }

    @Override
    public Label getOwnerLabel() {
      return fileset.getOwnerLabel();
    }

    @Override
    public FileRootApi getRoot() {
      return fileset.getRoot();
    }

    @Override
    public boolean isSourceArtifact() {
      // This information is lost to us.
      // Since the symlinks are always in the output tree, settle for saying "no"
      return false;
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public String getRunfilesPathString() {
      PathFragment relativePath = execPath.relativeTo(fileset.getExecPath());
      return fileset.getRunfilesPath().getRelative(relativePath).getPathString();
    }

    @Override
    public String getExecPathString() {
      return getExecPath().getPathString();
    }

    @Override
    public String getTreeRelativePathString() throws EvalException {
      throw Starlark.errorf(
          "tree_relative_path not allowed for files that are not tree artifact files.");
    }

    @Override
    public String expandToCommandLine() {
      return getExecPathString();
    }

    @Override
    public void repr(Printer printer) {
      if (isSourceArtifact()) {
        printer.append("<source file " + getRunfilesPathString() + ">");
      } else {
        printer.append("<generated file " + getRunfilesPathString() + ">");
      }
    }
  }
}
