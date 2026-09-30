package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableMap;

import com.verygood.security.larky.modules.types.LarkyIterator;
import com.verygood.security.larky.modules.types.LarkyObject;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkSequence;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;
import net.starlark.java.eval.Structure;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.NamedTuple;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@StarlarkBuiltin(
    name = "jcollections",
    category = "BUILTIN",
    doc = "This module implements specialized container datatypes providing\n" +
            "alternatives to Python's general purpose built-in containers, dict,\n" +
            "list, set, and tuple.\n" +
            "* namedtuple   factory function for creating tuple subclasses with named fields\n" +
            "* deque        list-like container with fast appends and pops on either end\n" +
            "* ChainMap     dict-like class for creating a single view of multiple mappings\n" +
            "* Counter      dict subclass for counting hashable objects\n" +
            "* OrderedDict  dict subclass that remembers the order entries were added\n" +
            "* defaultdict  dict subclass that calls a factory function to supply missing values\n" +
            "* UserDict     wrapper around dictionary objects for easier dict subclassing\n" +
            "* UserList     wrapper around list objects for easier list subclassing\n" +
            "* UserString   wrapper around string objects for easier string subclassing\n"
)
public class CollectionsModule implements StarlarkValue {

  public static final CollectionsModule INSTANCE = new CollectionsModule();

  /**
   * An instance of a {@code collections.namedtuple} type. It is a {@link Tuple} ({@link
   * NamedTuple}), so it equals, hashes like and orders against the plain tuple of its elements, as
   * in Python, and adds access to the elements by field name.
   */
  public static final class LarkyNamedTuple extends NamedTuple implements LarkyObject {

    private final NamedTupleType type;
    private final StarlarkThread currentThread;

    LarkyNamedTuple(NamedTupleType type, Iterable<?> elems, StarlarkThread thread) {
      super(elems);
      this.type = type;
      this.currentThread = thread;
    }

    @Override
    public Object getField(String name, @Nullable StarlarkThread thread) {
      int pos = type.fields.indexOf(name);
      if (pos != -1) {
        return get(pos);
      }
      switch (name) {
        case "__name__":
          return type.typename;
        case "_fields":
          return type.fieldsTuple;
        case "_field_defaults":
          return type.fieldDefaults;
        case "_make": // a classmethod in Python, so instances have it too
          return type.getValue("_make");
        default:
          return null;
      }
    }

    @Override
    public ImmutableCollection<String> getFieldNames() {
      return ImmutableSet.<String>builder()
          .addAll(type.fields)
          .add("__name__", "_fields", "_field_defaults", "_make")
          .build();
    }

    @Override
    public StarlarkThread getCurrentThread() {
      return currentThread;
    }

    @Override
    public String typeName() {
      return type.typename;
    }

    @StarlarkMethod(name = "_asdict", doc = "A dict mapping field names to values.")
    public Dict<String, Object> asDict() {
      Dict.Builder<String, Object> builder = Dict.builder();
      for (int i = 0; i < size(); i++) {
        builder.put(type.fields.get(i), get(i));
      }
      return builder.buildImmutable();
    }

    @StarlarkMethod(name = "_as_dict", documented = false)
    public Dict<String, Object> asDictLegacy() {
      return asDict();
    }

    @StarlarkMethod(
        name = "_replace",
        doc = "A new namedtuple with the given fields replaced.",
        extraKeywords = @Param(name = "kwds"),
        useStarlarkThread = true)
    public LarkyNamedTuple replace(Dict<String, Object> kwds, StarlarkThread thread)
        throws EvalException {
      List<String> unknown = new ArrayList<>();
      for (String k : kwds.keySet()) {
        if (!type.fields.contains(k)) {
          unknown.add(k);
        }
      }
      if (!unknown.isEmpty()) {
        throw Starlark.errorf(
            "ValueError: Got unexpected field names: %s",
            Starlark.repr(StarlarkList.immutableCopyOf(unknown), StarlarkSemantics.DEFAULT));
      }
      Object[] values = toArray();
      for (int i = 0; i < values.length; i++) {
        String field = type.fields.get(i);
        if (kwds.containsKey(field)) {
          values[i] = kwds.get(field);
        }
      }
      return new LarkyNamedTuple(type, Arrays.asList(values), thread);
    }

    @StarlarkMethod(
        name = "count",
        doc = "The number of elements equal to value.",
        parameters = {@Param(name = "value")})
    public StarlarkInt count(Object value) throws EvalException {
      int n = 0;
      for (Object e : this) {
        if (Starlark.checkedEquals(e, value)) {
          n++;
        }
      }
      return StarlarkInt.of(n);
    }

    @StarlarkMethod(
        name = "index",
        doc = "The index of the first element equal to value.",
        parameters = {@Param(name = "value")})
    public StarlarkInt index(Object value) throws EvalException {
      for (int i = 0; i < size(); i++) {
        if (Starlark.checkedEquals(get(i), value)) {
          return StarlarkInt.of(i);
        }
      }
      throw Starlark.errorf("ValueError: tuple.index(x): x not in tuple");
    }

    @Override
    public void repr(Printer p, StarlarkSemantics semantics) {
      p.append(type.typename).append('(');
      for (int i = 0; i < size(); i++) {
        if (i > 0) {
          p.append(", ");
        }
        p.append(type.fields.get(i)).append("=").repr(get(i), semantics);
      }
      p.append(")");
    }

    @Override
    public void str(Printer p, StarlarkSemantics semantics) {
      repr(p, semantics);
    }
  }

  /**
   * A namedtuple type, as returned by {@code collections.namedtuple}: calling it makes an instance,
   * binding arguments to fields as Python's generated {@code __new__} does.
   */
  public static final class NamedTupleType implements StarlarkCallable, Structure {

    private final String typename;
    private final ImmutableList<String> fields;
    private final Tuple fieldsTuple; // _fields
    private final Dict<String, Object> fieldDefaults;

    NamedTupleType(String typename, List<String> fields, Dict<String, Object> fieldDefaults) {
      this.typename = typename;
      this.fields = ImmutableList.copyOf(fields);
      this.fieldsTuple = Tuple.copyOf(fields);
      this.fieldDefaults = fieldDefaults;
    }

    @Override
    public String getName() {
      return typename;
    }

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append(typename);
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
        throws EvalException {
      int n = fields.size();
      if (args.size() > n) {
        throw Starlark.errorf(
            "TypeError: %s.__new__() takes %d positional arguments but %d were given",
            typename, n + 1, args.size() + 1);
      }
      Object[] values = new Object[n];
      for (int i = 0; i < args.size(); i++) {
        values[i] = args.get(i);
      }
      for (Map.Entry<String, Object> e : kwargs.entrySet()) {
        int pos = fields.indexOf(e.getKey());
        if (pos == -1) {
          throw Starlark.errorf(
              "TypeError: %s.__new__() got an unexpected keyword argument '%s'",
              typename, e.getKey());
        }
        if (values[pos] != null) {
          throw Starlark.errorf(
              "TypeError: %s.__new__() got multiple values for argument '%s'",
              typename, e.getKey());
        }
        values[pos] = e.getValue();
      }
      List<String> missing = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        if (values[i] == null) {
          String field = fields.get(i);
          if (fieldDefaults.containsKey(field)) {
            values[i] = fieldDefaults.get(field);
          } else {
            missing.add("'" + field + "'");
          }
        }
      }
      if (!missing.isEmpty()) {
        String names =
            missing.size() == 1
                ? missing.get(0)
                : String.join(", ", missing.subList(0, missing.size() - 1))
                    + " and "
                    + missing.get(missing.size() - 1);
        throw Starlark.errorf(
            "TypeError: %s.__new__() missing %d required positional argument%s: %s",
            typename, missing.size(), missing.size() == 1 ? "" : "s", names);
      }
      return new LarkyNamedTuple(this, Arrays.asList(values), thread);
    }

    /** {@code P._make(iterable)}: an instance from exactly as many values as fields. */
    private LarkyNamedTuple make(Object iterable, StarlarkThread thread) throws EvalException {
      Tuple values = Tuple.copyOf(Starlark.toIterable(iterable));
      if (values.size() != fields.size()) {
        throw Starlark.errorf(
            "TypeError: Expected %d arguments, got %d", fields.size(), values.size());
      }
      return new LarkyNamedTuple(this, values, thread);
    }

    @Nullable
    @Override
    public Object getValue(String name) {
      switch (name) {
        case "__name__":
          return typename;
        case "_fields":
          return fieldsTuple;
        case "_field_defaults":
          return fieldDefaults;
        case "_make":
          return new StarlarkCallable() {
            @Override
            public String getName() {
              return "_make";
            }

            @Override
            public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
                throws EvalException {
              if (args.size() != 1 || !kwargs.isEmpty()) {
                throw Starlark.errorf("_make() takes exactly one argument, an iterable");
              }
              return make(args.get(0), thread);
            }
          };
        default:
          return null;
      }
    }

    @Override
    public ImmutableCollection<String> getFieldNames() {
      return ImmutableSet.of("__name__", "_fields", "_field_defaults", "_make");
    }

    @Nullable
    @Override
    public String getErrorMessageForUnknownField(String field) {
      return String.format("type object '%s' has no attribute '%s'", typename, field);
    }
  }

  @StarlarkMethod(
    name = "namedtuple",
    doc = "Returns a new subclass of tuple with named fields.\n" +
      "    >>> Point = namedtuple('Point', ['x', 'y'])\n" +
      "    >>> Point.__doc__                   # docstring for the new class\n" +
      "    'Point(x, y)'\n" +
      "    >>> p = Point(11, y=22)             # instantiate with positional args or keywords\n" +
      "    >>> p[0] + p[1]                     # indexable like a plain tuple\n" +
      "    33\n" +
      "    >>> x, y = p                        # unpack like a regular tuple\n" +
      "    >>> x, y\n" +
      "    (11, 22)\n" +
      "    >>> p.x + p.y                       # fields also accessible by name\n" +
      "    33\n" +
      "    >>> d = p._asdict()                 # convert to a dictionary\n" +
      "    >>> d['x']\n" +
      "    11\n" +
      "    >>> Point(**d)                      # convert from a dictionary\n" +
      "    Point(x=11, y=22)\n" +
      "    >>> p._replace(x=100)               # _replace() is like str.replace() but targets named fields\n" +
      "    Point(x=100, y=22)",
    //typename, field_names, *, rename=False, defaults=None, module=None
    parameters = {
      @Param(name="typename", allowedTypes = {@ParamType(type = String.class)}),
      @Param(name="field_names", defaultValue = "[]", doc = "List of fields.", allowedTypes = {@ParamType(type = StarlarkList.class, generic1=String.class), @ParamType(type = Tuple.class)}),
      @Param(name="rename", named = true, defaultValue = "False", allowedTypes = {@ParamType(type=Boolean.class)}),
      @Param(name="defaults", named = true, defaultValue = "None", allowedTypes = {@ParamType(type = NoneType.class), @ParamType(type = Dict.class)}),
      @Param(name="module", named = true, defaultValue = "None", allowedTypes = {@ParamType(type = NoneType.class), @ParamType(type = String.class)}),
    },
    useStarlarkThread = true)
  public StarlarkCallable namedTuple(String typename, Sequence<String> fieldNames, boolean rename, Object defaultsO, Object moduleO, StarlarkThread thread) {
    @SuppressWarnings("unchecked") // field names are strings (collections.star)
    Dict<String, Object> defaults =
        defaultsO instanceof Dict
            ? Dict.immutableCopyOf((Dict<String, Object>) defaultsO)
            : Dict.empty();
    return new NamedTupleType(typename, fieldNames, defaults);
  }

}
