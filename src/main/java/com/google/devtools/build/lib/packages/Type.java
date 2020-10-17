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

package com.google.devtools.build.lib.packages;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.collect.nestedset.Depset;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.util.LoggingUtil;
import com.google.devtools.build.lib.util.StringCanonicalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;

/**
 * Root of Type symbol hierarchy for values in the build language.
 *
 * <p>Type symbols are primarily used for their <code>convert</code> method, which is a kind of cast
 * operator enabling conversion from untyped (Object) references to values in the build language, to
 * typed references.
 *
 * <p>For example, this code type-converts a value <code>x</code> returned by the evaluator, to a
 * list of strings:
 *
 * <pre>
 *  Object x = expr.eval(env);
 *  List&lt;String&gt; s = Type.STRING_LIST.convert(x);
 *  </pre>
 *
 * <p><b>BEFORE YOU ADD A NEW TYPE:</b>
 *
 * <p>We frequently get requests to create a new kind of attribute type whenever a use case doesn't
 * seem to fit into one of the existing types. This is almost always a bad idea. The most complex
 * type we currently have is probably STRING_LIST_DICT or maybe LABEL_KEYED_STRING_DICT. But no
 * matter what you support, someone will always want to add another layer of structure. It's even
 * been suggested to allow JSON or arbitrary Starlark values in attributes.
 *
 * <p>Adding a new type has implications for many different systems. The whole of the loading phase
 * needs to know about the type -- how to serialize it, how to format it for `bazel query`, how to
 * traverse label dependencies embedded within it. Then you need to think about how to represent
 * attribute values of that type in Starlark within a rule implementation function, and come up with
 * a good name for that type in the Starlark `attr` module. All of the tooling for formatting,
 * linting, and analyzing BUILD files may need to be updated.
 *
 * <p>It's usually possible to accomplish the end goal without making the target attribute grammar
 * more expressive. If it's not, that may be a sign that attributes are not the right mechanism to
 * use, and perhaps instead you should use opaque string identifiers, or labels to sub-targets with
 * more structure (think toolchains, platforms, config_setting).
 *
 * <p>Any new attribute type should be general-purpose and meet a high bar of usefulness (unlikely
 * since we seem to be doing fine so far without it), and not overly complicate BUILD files or rule
 * implementation functions.
 */
public abstract class Type<T> {

  protected Type() {}

  /**
   * Converts a legal Starlark value x into an Java value of type T.
   *
   * <p>x must be directly convertible to this type. This therefore disqualifies "selector
   * expressions" of the form "{ config1: 'value1_of_orig_type', config2: 'value2_of_orig_type; }"
   * (which support configurable attributes). To handle those expressions, see {@link
   * com.google.devtools.build.lib.packages.BuildType#selectableConvert}.
   *
   * @param x The Starlark value to convert.
   * @param what An object whose toString method returns a description of the purpose of x.
   *     Typically it is the name of a function parameter or struct field. The method is called only
   *     in case of error.
   * @param context the label of the current BUILD rule; must be non-null if resolution of
   *     package-relative label strings is required
   * @throws ConversionException if there was a problem performing the type conversion
   * @throws NullPointerException if x is null.
   */
  public abstract T convert(Object x, Object what, @Nullable Object context)
      throws ConversionException;
  // TODO(bazel-team): Check external calls (e.g. in PackageFactory), verify they always want
  // this over selectableConvert.

  /**
   * Equivalent to {@link #convert(Object, Object, Object)} where the label is {@code null}.
   * Useful for converting values to types that do not involve the type {@code LABEL}
   * and hence do not require the label of the current package.
   */
  public final T convert(Object x, Object what) throws ConversionException {
    return convert(x, what, null);
  }

  /**
   * Like {@link #convert(Object, Object, Object)}, but converts Starlark {@code None} to given
   * {@code defaultValue}.
   */
  @Nullable
  public final T convertOptional(Object x, String what, @Nullable Object context, T defaultValue)
      throws ConversionException {
    if (Starlark.isNullOrNone(x)) {
      return defaultValue;
    }
    return convert(x, what, context);
  }

  /**
   * Like {@link #convert(Object, Object, Object)}, but converts Starlark {@code None} to java
   * {@code null}.
   */
  @Nullable
  public final T convertOptional(Object x, String what, @Nullable Object context)
      throws ConversionException {
    return convertOptional(x, what, context, null);
  }

  /**
   * Like {@link #convert(Object, Object)}, but converts Starlark {@code NONE} to java {@code null}.
   */
  @Nullable
  public final T convertOptional(Object x, String what) throws ConversionException {
    return convertOptional(x, what, null);
  }

  public abstract T cast(Object value);

  @Override
  public abstract String toString();

  /**
   * Returns the default value for this type; may return null iff no default is defined for this
   * type.
   */
  public abstract T getDefaultValue();

  /**
   * Function accepting a (potentially null) {@link Label} and an arbitrary context object. Used by
   * {@link #visitLabels}.
   */
  public interface LabelVisitor<C> {
    void visit(@Nullable Label label, @Nullable C context);
  }

  /**
   * Invokes {@code visitor.visit(label, context)} for each {@link Label} {@code label} associated
   * with {@code value}, which is assumed an instance of this {@link Type}.
   *
   * <p>This is used to support reliable label visitation in {@link
   * com.google.devtools.build.lib.packages.AbstractAttributeMapper#visitLabels}. To preserve that
   * reliability, every type should faithfully define its own instance of this method. In other
   * words, be careful about defining default instances in base types that get auto-inherited by
   * their children. Keep all definitions as explicit as possible.
   */
  public abstract <C> void visitLabels(LabelVisitor<C> visitor, Object value, @Nullable C context);

  /** Classifications of labels by their usage. */
  public enum LabelClass {
    /** Used for types which are not labels. */
    NONE,
    /** Used for types which use labels to declare a dependency. */
    DEPENDENCY,
    /**
     * Used for types which use labels to reference another target but do not declare a dependency,
     * in cases where doing so would cause a dependency cycle.
     */
    NONDEP_REFERENCE,
    /** Used for types which use labels to declare an output path. */
    OUTPUT,
    /**
     * Used for types which contain Fileset entries, which contain labels but do not produce
     * normal dependencies.
     */
    FILESET_ENTRY
  }

  /** Returns the class of labels contained by this type, if any. */
  public LabelClass getLabelClass() {
    return LabelClass.NONE;
  }

  /**
   * Implementation of concatenation for this type, as if by {@code elements[0] + ... +
   * elements[n-1]}). Returns null to indicate concatenation isn't supported. This method exists to
   * support deferred additions {@code select + T} for catenable types T such as string, int, and
   * list.
   */
  public T concat(Iterable<T> elements) {
    return null;
  }

  /**
   * Converts an initialized Type object into a tag set representation.
   * This operation is only valid for certain sub-Types which are guaranteed
   * to be properly initialized.
   *
   * @param value the actual value
   * @throws UnsupportedOperationException if the concrete type does not support
   * tag conversion or if a convertible type has no initialized value.
   */
  public Set<String> toTagSet(Object value, String name) {
    String msg = "Attribute " + name + " does not support tag conversion.";
    throw new UnsupportedOperationException(msg);
  }

  /** The type of a Starlark integer in the signed 32-bit range. */
  @AutoCodec public static final Type<StarlarkInt> INTEGER = new IntegerType();

  /** The type of a string. */
  @AutoCodec public static final Type<String> STRING = new StringType();

  /** The type of a boolean. */
  @AutoCodec public static final Type<Boolean> BOOLEAN = new BooleanType();

  /** The type of a list of not-yet-typed objects. */
  @AutoCodec public static final ObjectListType OBJECT_LIST = new ObjectListType();

  /** The type of a list of strings. */
  @AutoCodec public static final ListType<String> STRING_LIST = ListType.create(STRING);

  /** The type of a list of signed 32-bit Starlark integer values. */
  @AutoCodec public static final ListType<StarlarkInt> INTEGER_LIST = ListType.create(INTEGER);

  /** The type of a dictionary of {@linkplain #STRING strings}. */
  @AutoCodec
  public static final DictType<String, String> STRING_DICT = DictType.create(STRING, STRING);

  /** The type of a dictionary of {@linkplain #STRING_LIST label lists}. */
  @AutoCodec
  public static final DictType<String, List<String>> STRING_LIST_DICT =
      DictType.create(STRING, STRING_LIST);

  /**
   *  For ListType objects, returns the type of the elements of the list; for
   *  all other types, returns null.  (This non-obvious implementation strategy
   *  is necessitated by the wildcard capture rules of the Java type system,
   *  which disallow conversion from Type{List{ELEM}} to Type{List{?}}.)
   */
  public Type<?> getListElementType() {
    return null;
  }

  /**
   * ConversionException is thrown when a type conversion fails; it contains an explanatory error
   * message.
   */
  public static class ConversionException extends EvalException {
    private static String message(Type<?> type, Object value, @Nullable Object what) {
      Printer printer = new Printer();
      printer.append("expected value of type '").append(type.toString()).append("'");
      if (what != null) {
        printer.append(" for ").append(what.toString());
      }
      printer.append(", but got ");
      printer.repr(value);
      printer.append(" (").append(Starlark.type(value)).append(")");
      return printer.toString();
    }

    /** Contructs a conversion error. Throws NullPointerException if value is null. */
    public ConversionException(Type<?> type, Object value, @Nullable Object what) {
      super(message(type, Preconditions.checkNotNull(value), what));
    }

    public ConversionException(String message) {
      super(message);
    }
  }

  /********************************************************************
   *                                                                  *
   *                            Subclasses                            *
   *                                                                  *
   ********************************************************************/

  private static class ObjectType extends Type<Object> {
    @Override
    public Object cast(Object value) {
      return value;
    }

    @Override
    public String getDefaultValue() {
      throw new UnsupportedOperationException(
          "ObjectType has no default value");
    }

    @Override
    public <T> void visitLabels(LabelVisitor<T> visitor, Object value, T context) {
    }

    @Override
    public String toString() {
      return "object";
    }

    @Override
    public Object convert(Object x, Object what, Object context) {
      return Preconditions.checkNotNull(x);
    }
  }

  // A Starlark integer in the signed 32-bit range (like Java int).
  private static class IntegerType extends Type<StarlarkInt> {
    @Override
    public StarlarkInt cast(Object value) {
      // This cast will fail if passed a java.lang.Integer,
      // as it is not a legal Starlark value. Use StarlarkInt.
      return (StarlarkInt) value;
    }

    @Override
    public StarlarkInt getDefaultValue() {
      return StarlarkInt.of(0);
    }

    @Override
    public <T> void visitLabels(LabelVisitor<T> visitor, Object value, T context) {
    }

    @Override
    public String toString() {
      return "int";
    }

    @Override
    public StarlarkInt convert(Object x, Object what, Object context) throws ConversionException {
      if (x instanceof StarlarkInt) {
        StarlarkInt i = (StarlarkInt) x;
        try {
          i.toIntUnchecked(); // assert signed 32-bit
        } catch (
            @SuppressWarnings("UnusedException")
            IllegalArgumentException ex) {
          String prefix = what != null ? ("for " + what + ", ") : "";
          throw new ConversionException(
              String.format("%sgot %s, want value in signed 32-bit range", prefix, i));
        }
        return i;
      }
      if (x instanceof Integer) {
        throw new IllegalArgumentException("Integer is not a legal Starlark value");
      }
      throw new ConversionException(this, x, what);
    }

    @Override
    public StarlarkInt concat(Iterable<StarlarkInt> elements) {
      StarlarkInt sum = StarlarkInt.of(0);
      for (StarlarkInt elem : elements) {
        sum = StarlarkInt.add(sum, elem);
      }
      // Perform narrowing conversion to ensure that the result
      // remains in the signed 32-bit range. This means that
      // s=select(0x7fffffff); s+s may yield a negative result.
      return StarlarkInt.of(sum.truncateToInt());
    }
  }

  private static class BooleanType extends Type<Boolean> {
    @Override
    public Boolean cast(Object value) {
      return (Boolean) value;
    }

    @Override
    public Boolean getDefaultValue() {
      return false;
    }

    @Override
    public <T> void visitLabels(LabelVisitor<T> visitor, Object value, T context) {
    }

    @Override
    public String toString() {
      return "boolean";
    }

    // Conversion to boolean must also tolerate integers of 0 and 1 only.
    @Override
    public Boolean convert(Object x, Object what, Object context)
        throws ConversionException {
      if (x instanceof Boolean) {
        return (Boolean) x;
      }
      int xAsInteger = INTEGER.convert(x, what, context).toIntUnchecked();
      if (xAsInteger == 0) {
        return false;
      } else if (xAsInteger == 1) {
        return true;
      }
      throw new ConversionException("boolean is not one of [0, 1]");
    }

    /**
     * Booleans attributes are converted to tags based on their names.
     */
    @Override
    public Set<String> toTagSet(Object value, String name) {
      if (value == null) {
        String msg = "Illegal tag conversion from null on Attribute " + name  + ".";
        throw new IllegalStateException(msg);
      }
      String tag = (Boolean) value ? name : "no" + name;
      return ImmutableSet.of(tag);
    }
  }

  private static class StringType extends Type<String> {
    @Override
    public String cast(Object value) {
      return (String) value;
    }

    @Override
    public String getDefaultValue() {
      return "";
    }

    @Override
    public <T> void visitLabels(LabelVisitor<T> visitor, Object value, T context) {
    }

    @Override
    public String toString() {
      return "string";
    }

    @Override
    public String convert(Object x, Object what, Object context)
        throws ConversionException {
      if (!(x instanceof String)) {
        throw new ConversionException(this, x, what);
      }
      return StringCanonicalizer.intern((String) x);
    }

    @Override
    public String concat(Iterable<String> elements) {
      return Joiner.on("").join(elements);
    }

    /**
     * A String is representable as a set containing its value.
     */
    @Override
    public Set<String> toTagSet(Object value, String name) {
      if (value == null) {
        String msg = "Illegal tag conversion from null on Attribute " + name + ".";
        throw new IllegalStateException(msg);
      }
      return ImmutableSet.of((String) value);
    }
  }

  /**
   * A type to support dictionary attributes.
   */
  public static class DictType<KeyT, ValueT> extends Type<Map<KeyT, ValueT>> {

    private final Type<KeyT> keyType;
    private final Type<ValueT> valueType;

    private final Map<KeyT, ValueT> empty = ImmutableMap.of();

    private final LabelClass labelClass;

    @Override
    public <T> void visitLabels(LabelVisitor<T> visitor, Object value, T context) {
      if (labelClass != LabelClass.NONE) {
        for (Map.Entry<KeyT, ValueT> entry : cast(value).entrySet()) {
          keyType.visitLabels(visitor, entry.getKey(), context);
          valueType.visitLabels(visitor, entry.getValue(), context);
        }
      }
    }

    public static <KEY, VALUE> DictType<KEY, VALUE> create(
        Type<KEY> keyType, Type<VALUE> valueType) {
      LabelClass keyLabelClass = keyType.getLabelClass();
      LabelClass valueLabelClass = valueType.getLabelClass();
      Preconditions.checkArgument(
          keyLabelClass == LabelClass.NONE
              || valueLabelClass == LabelClass.NONE
              || keyLabelClass == valueLabelClass,
          "A DictType's keys and values must be the same class of label if both contain labels, "
              + "but the key type %s contains %s labels, while "
              + "the value type %s contains %s labels.",
          keyType,
          keyLabelClass,
          valueType,
          valueLabelClass);
      LabelClass labelClass = (keyLabelClass != LabelClass.NONE) ? keyLabelClass : valueLabelClass;

      return new DictType<>(keyType, valueType, labelClass);
    }

    protected DictType(Type<KeyT> keyType, Type<ValueT> valueType, LabelClass labelClass) {
      this.keyType = keyType;
      this.valueType = valueType;
      this.labelClass = labelClass;
    }

    public Type<KeyT> getKeyType() {
      return keyType;
    }

    public Type<ValueT> getValueType() {
      return valueType;
    }

    @Override
    public LabelClass getLabelClass() {
      return labelClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<KeyT, ValueT> cast(Object value) {
      return (Map<KeyT, ValueT>) value;
    }

    @Override
    public String toString() {
      return "dict(" + keyType + ", " + valueType + ")";
    }

    @Override
    public Map<KeyT, ValueT> convert(Object x, Object what, Object context)
        throws ConversionException {
      if (!(x instanceof Map)) {
        throw new ConversionException(this, x, what);
      }
      Map<?, ?> o = (Map<?, ?>) x;
      // It's possible that #convert() calls transform non-equal keys into equal ones so we can't
      // just use ImmutableMap.Builder() here (that throws on collisions).
      LinkedHashMap<KeyT, ValueT> result = new LinkedHashMap<>();
      for (Map.Entry<?, ?> elem : o.entrySet()) {
        result.put(
            keyType.convert(elem.getKey(), "dict key element", context),
            valueType.convert(elem.getValue(), "dict value element", context));
      }
      return ImmutableMap.copyOf(result);
    }

    @Override
    public Map<KeyT, ValueT> getDefaultValue() {
      return empty;
    }
  }

  /** A type for lists of a given element type */
  public static class ListType<ElemT> extends Type<List<ElemT>> {

    private final Type<ElemT> elemType;

    private final List<ElemT> empty = ImmutableList.of();

    public static <ELEM> ListType<ELEM> create(Type<ELEM> elemType) {
      return new ListType<>(elemType);
    }

    private ListType(Type<ElemT> elemType) {
      this.elemType = elemType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ElemT> cast(Object value) {
      return (List<ElemT>) value;
    }

    @Override
    public Type<ElemT> getListElementType() {
      return elemType;
    }

    @Override
    public LabelClass getLabelClass() {
      return elemType.getLabelClass();
    }

    @Override
    public List<ElemT> getDefaultValue() {
      return empty;
    }

    @Override
    public <T> void visitLabels(LabelVisitor<T> visitor, Object value, T context) {
      if (elemType.getLabelClass() == LabelClass.NONE) {
        return;
      }

      List<ElemT> elems = cast(value);
      // Hot code path. Optimize for lists with O(1) access to avoid iterator garbage.
      if (elems instanceof RandomAccess) {
        for (int i = 0; i < elems.size(); i++) {
          elemType.visitLabels(visitor, elems.get(i), context);
        }
      } else {
        for (ElemT elem : elems) {
          elemType.visitLabels(visitor, elem, context);
        }
      }
    }

    @Override
    public String toString() {
      return "list(" + elemType + ")";
    }

    @Override
    public List<ElemT> convert(Object x, Object what, Object context)
        throws ConversionException {
      Iterable<?> iterable;

      if (x instanceof Iterable) {
        iterable = (Iterable<?>) x;
      } else if (x instanceof Depset) {
        iterable = ((Depset) x).toList();
      } else {
        throw new ConversionException(this, x, what);
      }

      int index = 0;
      List<ElemT> result = new ArrayList<>(Iterables.size(iterable));
      ListConversionContext conversionContext = new ListConversionContext(what);
      for (Object elem : iterable) {
        conversionContext.update(index);
        ElemT converted = elemType.convert(elem, conversionContext, context);
        if (converted != null) {
          result.add(converted);
        } else {
          // shouldn't happen but it does, rarely
          String message = "Converting a list with a null element: "
              + "element " + index + " of " + what + " in " + context;
          LoggingUtil.logToRemote(Level.WARNING, message,
              new ConversionException(message));
        }
        ++index;
      }
      return result;
    }

    @Override
    public List<ElemT> concat(Iterable<List<ElemT>> elements) {
      ImmutableList.Builder<ElemT> builder = ImmutableList.builder();
      for (List<ElemT> list : elements) {
        builder.addAll(list);
      }
      return builder.build();
    }

    /**
     * A list is representable as a tag set as the contents of itself expressed
     * as Strings. So a {@code List<String>} is effectively converted to a {@code Set<String>}.
     */
    @Override
    public Set<String> toTagSet(Object items, String name) {
      if (items == null) {
        String msg = "Illegal tag conversion from null on Attribute" + name + ".";
        throw new IllegalStateException(msg);
      }
      Set<String> tags = new LinkedHashSet<>();
      @SuppressWarnings("unchecked")
      List<ElemT> itemsAsListofElem = (List<ElemT>) items;
      for (ElemT element : itemsAsListofElem) {
        tags.add(element.toString());
      }
      return tags;
    }

    /**
     * Provides a {@link #toString()} description of the context of the value in a list being
     * converted. This is preferred over a raw string to avoid uselessly constructing strings which
     * are never used. This class is mutable (the index is updated).
     */
    private static class ListConversionContext {
      private final Object what;
      private int index = 0;

      ListConversionContext(Object what) {
        this.what = what;
      }

      void update(int index) {
        this.index = index;
      }

      @Override
      public String toString() {
        return "element " + index + " of " + what;
      }

    }
  }

  /** Type for lists of arbitrary objects */
  public static class ObjectListType extends ListType<Object> {

    private static final Type<Object> elemType = new ObjectType();

    private ObjectListType() {
      super(elemType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> convert(Object x, Object what, Object context)
        throws ConversionException {
      // TODO(adonovan): converge on Starlark.toIterable.
      if (x instanceof Sequence) {
        return ((Sequence) x).getImmutableList();
      } else if (x instanceof List) {
        return (List<Object>) x;
      } else if (x instanceof Iterable) {
        return ImmutableList.copyOf((Iterable<?>) x);
      } else {
        throw new ConversionException(this, x, what);
      }
    }
  }
}
