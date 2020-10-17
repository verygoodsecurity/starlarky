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
package com.google.devtools.build.lib.rules.python;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.ArtifactRoot;
import com.google.devtools.build.lib.analysis.FilesToRunProvider;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.Runfiles;
import com.google.devtools.build.lib.analysis.actions.CustomCommandLine;
import com.google.devtools.build.lib.analysis.actions.SpawnAction;
import com.google.devtools.build.lib.util.FileType;
import com.google.devtools.build.lib.vfs.PathFragment;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/** Various utility methods for Python support. */
public final class PythonUtils {
  public static final PathFragment INIT_PY = PathFragment.create("__init__.py");
  public static final PathFragment INIT_PYC = PathFragment.create("__init__.pyc");
  public static final PathFragment PYCACHE = PathFragment.create("__pycache__");

  private static final FileType REQUIRES_INIT_PY = FileType.of(".py", ".so", ".pyc");

  /**
   * Used to get the set of empty __init__.py files to be added to a given set of files to allow the
   * Python runtime to import subdirectories potentially containing Python code to be imported as
   * packages. Ideally this feature goes away with --incompatible_default_to_explicit_init_py as the
   * long term default behavior.
   */
  public static class GetInitPyFiles implements Runfiles.EmptyFilesSupplier {
    private final Predicate<PathFragment> isPackageInit;

    /**
     * The Predicate isPackageInit's .test(source) should be true when a given source is known to be
     * a valid __init__.py file equivalent, meaning no empty __init__.py file need be created.
     * Useful for custom Python runtimes that may have non-standard Python package import logic.
     */
    public GetInitPyFiles(Predicate<PathFragment> isPackageInit) {
      this.isPackageInit = isPackageInit;
    }

    @Override
    public Set<PathFragment> getExtraPaths(Set<PathFragment> manifestPaths) {
      return getInitPyFiles(manifestPaths);
    }

    /**
     * Returns the set of empty __init__.py(c) files to be added to a given set of files to allow
     * the Python runtime to find the <code>.py</code> and <code>.so</code> files present in the
     * tree.
     */
    private ImmutableSet<PathFragment> getInitPyFiles(Set<PathFragment> manifestFiles) {
      Set<PathFragment> result = new HashSet<>();
      // A set of directories that already have package init files.
      Set<PathFragment> hasPackageInitDirs = new HashSet<>(); // For b/142135992.

      // Find directories containing Python package init files based on a caller supplied test in
      // order to support non-standard Python package init naming schemes.
      // This loop is done prior to the one below as we assume no order in the set and that we may
      // find inits in parent directories listed after subdirectories which the nested loop below
      // would need to know of.
      for (PathFragment source : manifestFiles) {
        if (isPackageInit.test(source)) {
          hasPackageInitDirs.add(source.getParentDirectory());
        }
      }

      for (PathFragment source : manifestFiles) {
        // If we have a python or .so file at this level...
        if (REQUIRES_INIT_PY.matches(source)) {
          // ...then record that we need an __init__.py in this and all parents directories...
          while (source.segmentCount() > 1) {
            source = source.getParentDirectory();
            // ...unless it's a Python .pyc cache or we already have __init__ there.
            if (!source.endsWith(PYCACHE) && !hasPackageInitDirs.contains(source)) {
              PathFragment initpy = source.getRelative(INIT_PY);
              PathFragment initpyc = source.getRelative(INIT_PYC);

              if (!manifestFiles.contains(initpy) && !manifestFiles.contains(initpyc)) {
                result.add(initpy);
              }
            }
          }
        }
      }

      return ImmutableSet.copyOf(result);
    }
  } // class GetInitPyFiles

  private PythonUtils() {
    // This is a utility class, not to be instantiated.
  }

  /**
   * Get the artifact generated by the 2to3 action.
   *
   * <p>There might be conflicts eg. when the input file is generated, but that case is unsupported
   * because 2to3 is obsolete.
   *
   * <p>Returns null and reports a rule error if the output file cannot be created because it is not
   * underneath the target's package.
   */
  @Nullable
  private static Artifact get2to3OutputArtifact(RuleContext ruleContext, Artifact input) {
    PathFragment rootRelativePath = input.getRootRelativePath();
    if (!rootRelativePath.startsWith(ruleContext.getPackageDirectory())) {
      ruleContext.ruleError(
          String.format(
              "cannot perform 2to3 conversion on source file %s from another package",
              rootRelativePath));
      return null;
    }
    ArtifactRoot root = ruleContext.getGenfilesDirectory();
    return ruleContext.getDerivedArtifact(rootRelativePath, root);
  }

  /**
   * Creates an action for each Python 2 file to convert to Python 3.
   *
   * <p>Returns null and reports a rule error if an output file cannot be created.
   */
  @Nullable
  public static Map<PathFragment, Artifact> generate2to3Actions(
      RuleContext ruleContext, Iterable<Artifact> inputs) {
    // This creates many actions, but this is fine. Creating one action per library leads
    // to some problems (when the same file is generated by two different actions), with
    // little benefits and negligible memory improvement.

    Map<PathFragment, Artifact> symlinks = new HashMap<>();
    for (Artifact input : inputs) {
      Artifact output = generate2to3Action(ruleContext, input);
      if (output == null) {
        return null;
      }
      symlinks.put(input.getRootRelativePath(), output);
    }
    return symlinks;
  }

  /**
   * Generates and registers one 2to3 action.
   *
   * <p>Returns null and reports an error if the output file cannot be created.
   */
  @Nullable
  private static Artifact generate2to3Action(RuleContext ruleContext, Artifact input) {
    FilesToRunProvider py2to3converter = ruleContext.getExecutablePrerequisite("$python2to3");
    Artifact output = get2to3OutputArtifact(ruleContext, input);
    if (output == null) {
      return null;
    }

    CustomCommandLine.Builder commandLine =
        CustomCommandLine.builder()
            .add("--no-diffs")
            .add("--nobackups")
            .add("--write")
            .addPath("--output-dir", output.getExecPath().getParentDirectory())
            .add("--write-unchanged-files")
            .addExecPath(input);

    ruleContext.registerAction(
        new SpawnAction.Builder()
            .addInput(input)
            .addOutput(output)
            .setExecutable(py2to3converter)
            .setProgressMessage("Converting to Python 3: %s", input.prettyPrint())
            .setMnemonic("2to3")
            .addCommandLine(commandLine.build())
            .build(ruleContext));
    return output;
  }
}
