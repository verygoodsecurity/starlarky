package com.verygood.security.larky.stdtypes;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import com.verygood.security.larky.nativelib.LarkyProperty;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Structure;
import net.starlark.java.syntax.Location;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** An struct-like Info (LarkyValue instance) for providers defined in Starlark. */
public class LarkyObject implements LarkyValue, Structure, HasBinary {

  public Dict<String, StarlarkValue> __dict__;


  private final LarkyType larkyType;

  // For a n-element info, the table contains n key strings, sorted,
  // followed by the n corresponding legal Starlark values.
  private final Object[] table;

  @Nullable
  private final Location loc;
  // A format string with one %s placeholder for the missing field name.
  // If null, uses the default format specified by the LarkyValue.
  // TODO(adonovan): make the LarkyValue determine the error message
  // (but: this has implications for struct+struct, the equivalence
  // relation, and other observable behaviors).
  // Perhaps it should be a property of the LarkyObject instance, but
  // defined by a subclass?
  @Nullable
  private final String unknownFieldError;

  private LarkyObject(
      LarkyType type,
      Object[] table,
      @Nullable Location loc,
      @Nullable String unknownFieldError) {
    this.larkyType = type;
    this.table = table;
    this.loc = loc;
    this.unknownFieldError = unknownFieldError;
    this.__dict__ = Dict.of(null);
  }

//  @Override
//  public ImmutableCollection<String> getFieldNames() {
//    // TODO(adonovan): opt: can we avoid allocating three objects?
//    @SuppressWarnings("unchecked")
//    List<String> keys = (List<String>) (List<?>) Arrays.asList(table).subList(0, table.length / 2);
//    return ImmutableList.copyOf(keys);
//  }
  @Override
  public ImmutableList<String> getFieldNames() {
      return ImmutableList.copyOf(__dict__.keySet());
    }

  @Override
  public Object getValue(String name) throws EvalException {
    if(name == null
        || !__dict__.containsKey(name)
        || __dict__.getOrDefault(name, null) == null) {
      return null;
    }

    Object field = __dict__.get(name);
    /* if we have assigned a field that is a descriptor, we can invoke it */
    if (field == null
        || !LarkyProperty.class.isAssignableFrom(field.getClass())) {
      return field;
    }

    try {
      return ((LarkyProperty) field).call();
    } catch (
        NoSuchMethodException
            | EvalException exception) {
      throw new RuntimeException(exception);
    }
  }
  @Override
  public void setField(String name, Object value) throws EvalException {
    Object field = this.__dict__.get(name);
    /* if we have assigned a field that is a descriptor, we can invoke it */
    if (field == null
        || !LarkyProperty.class.isAssignableFrom(field.getClass())) {
      this.__dict__.putEntry(name, (StarlarkValue) value);
      return;
    }

    try {
      ((LarkyProperty) field).call(new Object[]{value, name}, null);
    } catch (NoSuchMethodException exception) {
      throw new RuntimeException(exception);
    }
  }

  // Converts a map to a table of sorted keys followed by corresponding values.
  private static Object[] toTable(Map<String, Object> values) {
    int n = values.size();
    Object[] table = new Object[n + n];
    int i = 0;
    for (Map.Entry<String, Object> e : values.entrySet()) {
      table[i] = e.getKey();
      table[n + i] = Starlark.checkValid(e.getValue());
      i++;
    }
    // Sort keys, permuting values in parallel.
    if (n > 1) {
      sortPairs(table, 0, n - 1);
    }
    return table;
  }

  /**
   * Constructs a LarkyObject from an array of alternating key/value pairs as provided by
   * Starlark.fastcall. Checks that each key is provided at most once, and is defined by the
   * optional schema, which must be sorted. This optimized zero-allocation function exists solely
   * for the StarlarkProvider constructor.
   */
  static LarkyObject createFromNamedArgs(
      LarkyType type, Object[] table, @Nullable ImmutableList<String> schema, Location loc)
      throws EvalException {
    // Permute fastcall form (k, v, ..., k, v) into table form (k, k, ..., v, v).
    permute(table);

    int n = table.length >> 1; // number of K/V pairs

    // Sort keys, permuting values in parallel.
    if (n > 1) {
      sortPairs(table, 0, n - 1);
    }

    // Check for duplicate keys, which are now adjacent.
    for (int i = 0; i < n - 1; i++) {
      if (table[i].equals(table[i + 1])) {
        throw Starlark.errorf(
            "got multiple values for parameter %s in call to instantiate LarkyValue %s",
            table[i], type.getPrintableName());
      }
    }

    // Check that schema is a superset of the table's keys.
    if (schema != null) {
      List<String> unexpected = unexpectedKeys(schema, table, n);
      if (unexpected != null) {
        throw Starlark.errorf(
            "unexpected keyword%s %s in call to instantiate LarkyValue %s",
            unexpected.size() > 1 ? "s" : "",
            Joiner.on(", ").join(unexpected),
            type.getPrintableName());
      }
    }

    return new LarkyObject(type, table, loc, /*unknownFieldError=*/ null);
  }

  // Permutes array elements from alternating keys/values form,
  // (as used by fastcall's named array) into keys-then-corresponding-values form,
  // as used by LarkyObject.table.
  // The permutation preserves the key/value association but not the order of keys.
  static void permute(Object[] named) {
    int n = named.length >> 1; // number of K/V pairs

    // Thanks to Murali Ganapathy for the algorithm.
    // See https://play.golang.org/p/QOKnrj_bIwk.
    //
    // i and j are the indices bracketing successive pairs of cells,
    // working from the outside to the middle.
    //
    //   i                  j
    //   [KV]KVKVKVKVKVKV[KV]
    //     i              j
    //   KK[KV]KVKVKVKV[KV]VV
    //       i          j
    //   KKKK[KV]KVKV[KV]VVVV
    //   etc...
    for (int i = 0; i < n - 1; i += 2) {
      int j = named.length - i;
      // rotate two pairs [KV]...[kv] -> [Kk]...[vV]
      Object tmp = named[i + 1];
      named[i + 1] = named[j - 2];
      named[j - 2] = named[j - 1];
      named[j - 1] = tmp;
    }
    // reverse lower half containing keys: [KkvV] -> [kKvV]
    for (int i = 0; i < n >> 1; i++) {
      Object tmp = named[n - 1 - i];
      named[n - 1 - i] = named[i];
      named[i] = tmp;
    }
  }

  // Sorts non-empty slice a[lo:hi] (inclusive) in place.
  // Elements a[n:2n) are permuted the same way as a[0:n),
  // where n = a.length / 2. The lower half must be strings.
  // Precondition: 0 <= lo <= hi < n.
  static void sortPairs(Object[] a, int lo, int hi) {
    String pivot = (String) a[lo + (hi - lo) / 2];

    int i = lo;
    int j = hi;
    while (i <= j) {
      while (((String) a[i]).compareTo(pivot) < 0) {
        i++;
      }
      while (((String) a[j]).compareTo(pivot) > 0) {
        j--;
      }
      if (i <= j) {
        int n = a.length >> 1;
        swap(a, i, j);
        swap(a, i + n, j + n);
        i++;
        j--;
      }
    }
    if (lo < j) {
      sortPairs(a, lo, j);
    }
    if (i < hi) {
      sortPairs(a, i, hi);
    }
  }

  private static void swap(Object[] a, int i, int j) {
    Object tmp = a[i];
    a[i] = a[j];
    a[j] = tmp;
  }

  // Returns the list of keys in table[0:n) not defined by the schema,
  // or null on success.
  // Allocates no memory on success.
  // Both table[0:n) and schema are sorted lists of strings.
  @Nullable
  private static List<String> unexpectedKeys(ImmutableList<String> schema, Object[] table, int n) {
    int si = 0;
    List<String> unexpected = null;
    table:
    for (int ti = 0; ti < n; ti++) {
      String t = (String) table[ti];
      while (si < schema.size()) {
        String s = schema.get(si++);
        int cmp = s.compareTo(t);
        if (cmp == 0) {
          // table key matches schema
          continue table;
        } else if (cmp > 0) {
          // table contains unexpected key
          if (unexpected == null) {
            unexpected = new ArrayList<>();
          }
          unexpected.add(t);
        } else {
          // skip over schema key not provided by table
        }
      }
      if (unexpected == null) {
        unexpected = new ArrayList<>();
      }
      unexpected.add(t);
    }
    return unexpected;
  }

  @Override
  public void repr(Printer printer) {
    printer.append("<types.");
    printer.append(getType().getPrintableName());
    printer.append(" object>");
  }

  @Override
  public LarkyType getType() {
    return larkyType;
  }

  @Override
  public Location getLocation() {
    return loc != null ? loc : getType().getLocation();
  }

  @Override
  public boolean isExported() {
    return getType().isExported();
  }

  @Override
  public void export(String extensionLabel, String exportedName) throws EvalException {
    getType().export(extensionLabel, exportedName);
  }

  @Override
  public Key getKey() {
    return getType().getKey();
  }

  @Override
  public String getPrintableName() {
    return getType().getPrintableName();
  }

  @Nullable
  @Override
  public String getErrorMessageForUnknownField(String field) {
    return getType().getErrorMessageForUnknownField(field);
  }

  @Override
  public boolean isImmutable() {
    // If the LarkyType is not yet exported, the hash code of the object is subject to change.
    if (!isExported()) {
      return false;
    }
    for (int i = table.length / 2; i < table.length; i++) {
      if (!Starlark.isImmutable(table[i])) {
        return false;
      }
    }
    return true;
  }


  /**
   * Creates a schemaless LarkyValue instance with the given LarkyValue type and field values.
   *
   * <p>{@code loc} is the creation location for this instance. Built-in LarkyValue instances may use
   * {@link Location#BUILTIN}, which is the default if null.
   */
  public static LarkyObject create(
      LarkyType type, Map<String, Object> values, @Nullable Location loc) {
    return new LarkyObject(type, toTable(values), loc, /*unknownFieldError=*/ null);
  }

  /**
   * Creates a schemaless LarkyValue instance with the given LarkyValue type, field values, and
   * unknown-field error message.
   *
   * <p>This is used to create structs for special purposes, such as {@code ctx.attr} and the {@code
   * native} module. The creation location will be {@link Location#BUILTIN}.
   *
   * <p>{@code unknownFieldError} is a string format, as for {LarkyValue}.
   *
   * @deprecated Do not use this method. Instead, create a new subclass of {BuiltinProvider}
   *     with the desired error message format, and create a corresponding {NativeInfo}
   *     subclass.
   */
  // TODO(bazel-team): Make the special structs that need a custom error message use a different
  // LarkyValue (subclassing BuiltinProvider) and a different StructImpl implementation. Then remove
  // this functionality, thereby saving a string pointer field for the majority of providers that
  // don't need it. However, this is tricky: if the error message is a property of the LarkyValue,
  // then each flavor of struct must have a distinct LarkyValue of a unique class, and this would be
  // observable to Starlark code. What would be their names: "struct", or something else? Should
  // struct+struct fail when different flavors are mixed (as happens today when adding info
  // instances of different providers)? Or should it return a new struct picking the LarkyValue of one
  // operand arbitrarily (as it does today for custom error strings)? Or ignore providers and return
  // a plain old struct, always? Or only if they differ? Or should we abolish struct+struct
  // altogether? In other words, the advice in the @deprecated tag above is not compatible.
  //
  // brandjon notes: nearly all the uses of custom errors are for objects that properly should be
  // Structures but not structs. They only leveraged the struct machinery for historical reasons and
  // convenience.
  // For instance, ctx.attr should have a custom error message, but should not support concatenation
  // (it fails today but only because you can't produce two ctx.attr's that don't have common
  // fields). It also should not support to_json().
  // It's possible someone was crazy enough to take ctx.attr.to_json(), but we can probably break
  // that case without causing too much trouble.
  // If we migrate all these cases of non-providers away, whatever is left should be happy to use a
  // default error message, and we can eliminate this extra detail.
  @Deprecated
  public static LarkyObject createWithCustomMessage(
      LarkyType type, Map<String, Object> values, String unknownFieldError) {
    Preconditions.checkNotNull(unknownFieldError);
    return new LarkyObject(type, toTable(values), Location.BUILTIN, unknownFieldError);
  }

  @Override
  public LarkyObject binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    if (op == TokenKind.PLUS && that instanceof LarkyObject) {
      return thisLeft
          ? plus(this, (LarkyObject) that) //
          : plus((LarkyObject) that, this);
    }
    return null;
  }

  private static LarkyObject plus(LarkyObject x, LarkyObject y) throws EvalException {
    LarkyType xprov = x.getType();
    LarkyType yprov = y.getType();
    if (!xprov.equals(yprov)) {
      throw Starlark.errorf(
          "Cannot use '+' operator on instances of different providers (%s and %s)",
          xprov.getPrintableName(), yprov.getPrintableName());
    }

    // ztable = merge(x.table, y.table)
    int xsize = x.table.length / 2;
    int ysize = y.table.length / 2;
    int zsize = xsize + ysize;
    Object[] ztable = new Object[zsize + zsize];
    int xi = 0;
    int yi = 0;
    int zi = 0;
    while (xi < xsize && yi < ysize) {
      String xk = (String) x.table[xi];
      String yk = (String) y.table[yi];
      int cmp = xk.compareTo(yk);
      if (cmp < 0) {
        ztable[zi] = xk;
        ztable[zi + zsize] = x.table[xi + xsize];
        xi++;
      } else if (cmp > 0) {
        ztable[zi] = yk;
        ztable[zi + zsize] = y.table[yi + ysize];
        yi++;
      } else {
        throw Starlark.errorf("cannot add struct instances with common field '%s'", xk);
      }
      zi++;
    }
    while (xi < xsize) {
      ztable[zi] = x.table[xi];
      ztable[zi + zsize] = x.table[xi + xsize];
      xi++;
      zi++;
    }
    while (yi < ysize) {
      ztable[zi] = y.table[yi];
      ztable[zi + zsize] = y.table[yi + ysize];
      yi++;
      zi++;
    }

    return new LarkyObject(xprov, ztable, Location.BUILTIN, x.unknownFieldError);
  }
}

