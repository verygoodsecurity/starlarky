package com.verygood.security.larky.modules.path;

import com.google.common.collect.ImmutableList;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

import java.io.IOException;

/** A structure representing a file to be used inside a repository. */
@StarlarkBuiltin(
    name = "path",
    category = "BUILTIN",
    doc = "A structure representing a file to be used inside a repository.")
public interface RepositoryPathApi<RepositoryPathApiT extends RepositoryPathApi<?>>
    extends StarlarkValue {

  @StarlarkMethod(
      name = "basename",
      structField = true,
      doc = "A string giving the basename of the file.")
  String getBasename();

  @StarlarkMethod(
      name = "readdir",
      structField = false,
      doc = "The list of entries in the directory denoted by this path.")
  ImmutableList<RepositoryPathApiT> readdir() throws IOException;

  @StarlarkMethod(
      name = "dirname",
      structField = true,
      doc = "The parent directory of this file, or None if this file does not have a parent.")
  RepositoryPathApi<?> getDirname();

  @StarlarkMethod(
      name = "get_child",
      doc = "Append the given path to this path and return the resulted path.",
      parameters = {
        @Param(
            name = "child_path",
            positional = true,
            named = false,
            type = String.class,
            doc = "The path to append to this path."),
      })
  RepositoryPathApi<?> getChild(String childPath);

  @StarlarkMethod(
      name = "exists",
      structField = true,
      doc = "Returns true if the file denoted by this path exists.")
  boolean exists();

  @StarlarkMethod(
      name = "realpath",
      structField = true,
      doc =
          "Returns the canonical path for this path by repeatedly replacing all symbolic links "
              + "with their referents.")
  RepositoryPathApi<?> realpath() throws IOException;
}
