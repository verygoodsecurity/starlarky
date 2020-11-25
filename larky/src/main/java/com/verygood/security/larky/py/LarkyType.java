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

package com.verygood.security.larky.py;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.Location;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

/**
 * A provider defined in Starlark rather than in native code.
 *
 * <p>This is a result of calling the {@code provider()} function from Starlark ({@link
 * com.google.devtools.build.lib.analysis.starlark.StarlarkRuleClassFunctions#provider}).
 *
 * <p>{@code LarkyType}s may be either schemaless or schemaful. Instances of schemaless
 * providers can have any set of fields on them, whereas instances of schemaful providers may have
 * only the fields that are named in the schema.
 *
 * <p>Exporting a {@code LarkyType} creates a key that is used to uniquely identify it.
 * Usually a provider is exported by calling {@link #export}, but a test may wish to just create a
 * pre-exported provider directly. Exported providers use only their key for {@link #equals} and
 * {@link #hashCode}.
 */

@StarlarkBuiltin(
    name = "LarkyType",
    category = "BUILTIN",
    doc =
        "A constructor for simple value objects, known as provider instances."
            + "<br>"
            + "This value has a dual purpose:"
            + "  <ul>"
            + "     <li>It is a function that can be called to construct 'struct'-like values:"
            + "<pre class=\"language-python\">DataInfo = provider()\n"
            + "d = DataInfo(x = 2, y = 3)\n"
            + "print(d.x + d.y) # prints 5</pre>"
            + "     Note: Some providers, defined internally, do not allow instance creation"
            + "     </li>"
            + "     <li>It is a <i>key</i> to access a provider instance on a"
            + "        <a href=\"Target.html\">Target</a>"
            + "<pre class=\"language-python\">DataInfo = provider()\n"
            + "def _rule_impl(ctx)\n"
            + "  ... ctx.attr.dep[DataInfo]</pre>"
            + "     </li>"
            + "  </ul>"
            + "Create a new <code>LarkyMetaClass</code> using the "
            + "<a href=\"globals.html#provider\">provider</a> function.")
 //org.python.types.Type extends org.python.types.Object
public class LarkyType implements StarlarkCallable, LarkyValue {

  /**
   * A serializable representation of Starlark-defined {@link LarkyType} that uniquely
   * identifies all {@link LarkyType}s that are exposed to SkyFrame.
   *
   * A serializable representation of {@link LarkyType}.
   * */
  public static class Key extends LarkyValue.Key {
    private final String extensionLabel;
    private final String exportedName;

    public Key(String extensionLabel, String exportedName) {
      this.extensionLabel = Preconditions.checkNotNull(extensionLabel);
      this.exportedName = Preconditions.checkNotNull(exportedName);
    }

    public String getExtensionLabel() {
      return extensionLabel;
    }

    public String getExportedName() {
      return exportedName;
    }

    @Override
    public String toString() {
      return exportedName;
    }

    @Override
    public int hashCode() {
      return Objects.hash(extensionLabel, exportedName);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (!(obj instanceof Key)) {
        return false;
      }
      Key other = (Key) obj;
      return Objects.equals(this.extensionLabel, other.extensionLabel)
          && Objects.equals(this.exportedName, other.exportedName);
    }
  }

  /** Null iff this provider has not yet been exported. */
  @Nullable private Key key;

  @Override
  public LarkyValue.Key getKey() {
    return key;
  }

  @Override
  public boolean isExported() {
    return key != null;
  }

  @Override
  public void export(String extensionLabel, String exportedName) throws EvalException {
    key = new Key(extensionLabel, exportedName);
  }

  @Override
  public boolean isImmutable() {
    // Hash code for non exported constructors may be changed
    return isExported();
  }

  private final Location location;

  // For schemaful providers, the sorted list of allowed field names.
  // The requirement for sortedness comes from StarlarkInfo.createFromNamedArgs,
  // as it lets us verify table ⊆ schema in O(n) time without temporaries.
  @Nullable private final ImmutableList<String> schema;


  /**
   * Creates an unexported {@link LarkyType} with no schema.
   *
   * <p>The resulting object needs to be exported later (via {@link #export}).
   *
   * @param location the location of the Starlark definition for this provider (tests may use {@link
   *     Location#BUILTIN})
   */
  public static LarkyType createUnexportedSchemaless(Location location) {
    return new LarkyType(/*key=*/ null, /*schema=*/ null, location);
  }

  /**
   * Creates an unexported {@link LarkyType} with a schema.
   *
   * <p>The resulting object needs to be exported later (via {@link #export}).
   *
   * @param schema the allowed field names for instances of this provider
   * @param location the location of the Starlark definition for this provider (tests may use {@link
   *     Location#BUILTIN})
   */
  // TODO(adonovan): in what sense is this "schemaful" if schema may be null?
  public static LarkyType createUnexportedSchemaful(
      @Nullable Collection<String> schema, Location location) {
    return new LarkyType(
        /*key=*/ null, schema == null ? null : ImmutableList.sortedCopyOf(schema), location);
  }

  /**
   * Creates an exported {@link LarkyType} with no schema.
   *
   * @param key the key that identifies this provider
   * @param location the location of the Starlark definition for this provider (tests may use {@link
   *     Location#BUILTIN})
   */
  public static LarkyType createExportedSchemaless(Key key, Location location) {
    return new LarkyType(key, /*schema=*/ null, location);
  }

  /**
   * Creates an exported {@link LarkyType} with no schema.
   *
   * @param key the key that identifies this provider
   * @param schema the allowed field names for instances of this provider
   * @param location the location of the Starlark definition for this provider (tests may use {@link
   *     Location#BUILTIN})
   */
  // TODO(adonovan): in what sense is this "schemaful" if schema may be null?
  public static LarkyType createExportedSchemaful(
      Key key, @Nullable Collection<String> schema, Location location) {
    return new LarkyType(
        key, schema == null ? null : ImmutableList.sortedCopyOf(schema), location);
  }

  /**
   * Constructs the provider.
   *
   * <p>If {@code key} is null, the provider is unexported. If {@code schema} is null, the provider
   * is schemaless.
   */
  private LarkyType(
      @Nullable Key key, @Nullable ImmutableList<String> schema, Location location) {
    this.schema = schema;
    this.location = location;
    this.key = key;  // possibly null
  }

  @Override
  public Object fastcall(StarlarkThread thread, Object[] positional, Object[] named)
      throws EvalException {
    if (positional.length > 0) {
      throw new EvalException(
          thread.getCallerLocation(),
          String.format("%s: unexpected positional arguments", getName())
      );
    }
    return LarkyObject.createFromNamedArgs(this, named, schema, thread.getCallerLocation());
  }

  @Override
  public String getName() {
    return key != null ? key.getExportedName() : "<no name>";
  }

  /** Returns the list of fields allowed by this provider, or null if the provider is schemaless. */
  @Nullable
  public ImmutableList<String> getSchema() {
    return schema;
  }

  @Override
  public int hashCode() {
    if (isExported()) {
      return getKey().hashCode();
    }
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(@Nullable Object otherObject) {
    if (!(otherObject instanceof LarkyType)) {
      return false;
    }
    LarkyType other = (LarkyType) otherObject;

    if (this.isExported() && other.isExported()) {
      return this.getKey().equals(other.getKey());
    } else {
      return this == other;
    }
  }

  @Override
  public void repr(Printer printer) {
   printer.append("<type: ");
   printer.append(getType().getPrintableName());
   printer.append(">");
  }

  @Override
  public String toString() {
    return Starlark.repr(this);
  }

  @Override
  public LarkyType getType() {
    return this;
  }

  @Override
  public String getPrintableName() {
    return getName();
  }

  @Override
  public Location getLocation() {
    return location;
  }

}
