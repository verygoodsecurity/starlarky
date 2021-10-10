package com.verygood.security.larky.modules;

import com.google.common.collect.Iterables;
import java.util.Map;

import com.verygood.security.larky.modules.types.structs.SimpleStruct;

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
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkSequence;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;


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

  public static class LarkyNamedTuple extends SimpleStruct
    implements StarlarkSequence<Tuple, Object> {

    final Tuple backingTuple;
    protected LarkyNamedTuple(Map<String, Object> fields, Iterable<?> obj, StarlarkThread currentThread) {
      super(fields, currentThread);
      backingTuple = Tuple.copyOf(obj);
    }

    public static LarkyNamedTuple create(Dict<String, Object> cls_ns,
                                         Tuple args,
                                         Map<String, Object> kwargs,
                                         StarlarkThread thread) throws EvalException {

      Iterable<?> values = Iterables.concat(args, kwargs.values());
      Sequence<String> _fields = Sequence.cast(
        cls_ns.get("_fields"), String.class, "_fields"
      );
      int argLength = _fields.size();
      if(argLength < args.size() + kwargs.size()) {
        throw Starlark.errorf(
          "TypeError: __new__() takes %d positional arguments but %d were given",
          argLength,
          args.size() + kwargs.size()
          );
      } else if(argLength > args.size() + kwargs.size()) {
        // TODO...
        throw Starlark.errorf(
          "TypeError: __new__() missing %d required positional arguments",
          args.size() + kwargs.size() - argLength
        );
      }

      return new LarkyNamedTuple(cls_ns, values, thread);
    }

    @Override
    public Object getIndex(StarlarkSemantics semantics, Object key) throws EvalException {
      return StarlarkSequence.super.getIndex(semantics, key);
    }

    @Override
    public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
      return StarlarkSequence.super.containsKey(semantics, key);
    }

    @Override
    public Object getValue(String name) throws EvalException {
      final Sequence<String> _names = namedFields();
      final int pos = _names.indexOf(name);
      if(pos != -1) {
        return this.backingTuple.get(pos);
      }
      return super.getValue(name);
    }

    @Override
    public Tuple collection() {
      return this.backingTuple;
    }

    @Override
    public Sequence<Object> getSlice(Mutability mu, int start, int stop, int step) {
      return this.backingTuple.getSlice(mu,start,stop,step);
    }

    public Sequence<String> namedFields() throws EvalException {
      Sequence<String> _namedFields = Sequence.cast(
        super.getValue("_fields"), String.class, "_fields"
      );
      assert _namedFields != null;
      return _namedFields;
    }

    @StarlarkMethod(name="_as_dict")
    public Dict<String, Object> asDict() throws EvalException {
      Dict.Builder<String, Object> asDictBuilder = Dict.<String, Object>builder();
      final Sequence<String> _namedFields = namedFields();
      for (int i = 0, fieldsSize = _namedFields.size(); i < fieldsSize; i++) {
        asDictBuilder.put(_namedFields.get(i), this.get(i));
      }
      return asDictBuilder.build(getCurrentThread().mutability());
    }

    @StarlarkMethod(name = "_make", parameters = {@Param(name = "iterable", allowedTypes = {@ParamType(type = StarlarkIterable.class)})})
    public LarkyNamedTuple make(StarlarkIterable<?> iterable) throws EvalException {
      Dict<String, Object> cls_ns = Dict.<String, Object>builder()
        .put("__name__", getField("__name__"))
        .put("_fields", namedFields())
        .put("__match_args__", namedFields())
        .put("_field_defaults", getField("_field_defaults"))
        .buildImmutable();
      Tuple args = Tuple.copyOf(iterable);
      if (args.size() != namedFields().size()) {
        throw Starlark.errorf("TypeError: Expected %d arguments, got %d",
          namedFields().size(), args.size());
      }
      return create(cls_ns, args, Dict.empty(), getCurrentThread());
    }

    @StarlarkMethod(
      name = "_replace",
      extraKeywords =  @Param(name = "kwds"),
      useStarlarkThread = true)
    public LarkyNamedTuple replace(Dict<String, Object> kwds, StarlarkThread thread) throws EvalException {
      final Sequence<String> fields = namedFields();
      Dict.Builder<String, Object> values = Dict.builder();
      for (int i = 0, fieldsSize = fields.size(); i < fieldsSize; i++) {
        String key = fields.get(i);
        values.put(key, kwds.get2(key, this.get(i), thread));
      }
      return make(values.buildImmutable().values0(thread));
    }
    @Override
    public void repr(Printer p) {
      p.append(type()).append('(');

      try {
        final Sequence<String> _namedFields = namedFields();
        for (int i = 0, fieldsSize = _namedFields.size(); i < fieldsSize; i++) {
          String field = _namedFields.get(i);
          p.append(field).append("=").repr(this.get(i));
          if(i + 1 < fieldsSize) {
            p.append(", ");
          }
        }
      } catch (EvalException e) {
        throw new RuntimeException(e);
      }
      p.append(")");
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
    return new StarlarkCallable() {
      @Override
      public String getName() {
        return typename;
      }

      @Override
      public void repr(Printer printer) {
        printer.append(getName());
      }

      @Override
      public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
        Dict.Builder<String, Object> class_namespace = Dict.<String, Object>builder()
          .put("__name__", typename)
          .put("_fields", fieldNames)
          .put("__match_args__", fieldNames)
          .put("_field_defaults", defaultsO)
          ;

        return LarkyNamedTuple.create(class_namespace.buildImmutable(), args, kwargs, thread);
      }
    };

  }


}
