package com.verygood.security.larky.objects.type;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.objects.LarkyPyObject;
import com.verygood.security.larky.objects.PyObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;
import org.jetbrains.annotations.Nullable;


final public class LarkyBaseObjectType
  implements LarkyType,
               StarlarkCallable {

  private static final Origin origin = Origin.BUILTIN;
  private static final ImmutableSet<SpecialMethod> specialMethods = Sets.immutableEnumSet(
    SpecialMethod.dunder_repr,
    SpecialMethod.dunder_hash,
    SpecialMethod.dunder_str,
    SpecialMethod.dunder_getattribute,
    SpecialMethod.dunder_setattr,
    SpecialMethod.dunder_delattr,
    // none of the below is supported, but it is on the object.__dict__
    SpecialMethod.dunder_lt,
    SpecialMethod.dunder_le,
    SpecialMethod.dunder_eq,
    SpecialMethod.dunder_ne,
    SpecialMethod.dunder_gt,
    SpecialMethod.dunder_ge
  );
  private final Set<LarkyType> allSubclasses = Collections.synchronizedSet(Collections.newSetFromMap(
      new MapMaker().weakKeys().makeMap()));

  private final Map<String, Object> __dict__;
  private final LarkyType type;

  private LarkyBaseObjectType(LarkyTypeObject type) {
    this.type = type;
    this.__dict__ = Maps.newHashMap();
  } // cannot inherit from this.

  public static PyObject getInstance() {
    return LarkyBaseObjectTypeSingleton.INSTANCE.get();
  }

  @Override
  public String __repr__() {
    return String.format("<class '%s'>", __name__());
  }

  @Override
  public Map<String, Object> getInternalDictUnsafe() {
    return this.__dict__;
  }

  @Override
  public Origin getOrigin() {
    return origin;
  }

  @Override
  public LarkyType typeClass() {
    return __class__();
  }

  @Override
  public LarkyType __class__() {
    return LarkyTypeObject.getInstance();
  }

  @Override
  public Set<LarkyType> getAllSubclasses() {
    return allSubclasses;
  }

  @Override
  public void setBaseClasses(LarkyType[] parentClasses) {
    throw new UnsupportedOperationException("cannot set base classes on " + this);

  }

  @Override
  public String __name__() {
    return type.typeName();
  }

  @Override
  public ImmutableSet<SpecialMethod> getSpecialMethods() {
    return specialMethods;
  }

  @Override
  public Object getBase() {
    return Starlark.NONE;
  }

  @Override
  public Tuple getBases() {
    return Tuple.empty();
  }

  @Override
  public Tuple getMRO() {
    return Tuple.of(this);
  }

  @Override
  public void setMRO(List<LarkyType> mro) {
    throw new UnsupportedOperationException("cannot set MRO on " + this);
  }

  @Override
  public StarlarkThread getCurrentThread() {
    return null;
  }

  @Override
  public String typeName() {
    return type.typeName();
  }

  @Override
  public PyObject __new__(Tuple args, Dict<String, Object> kwargs, StarlarkThread thread) {
    return new LarkyPyObject(this.typeClass(), thread);
  }

  @Override
  public String getName() {
    return this.__repr__();
  }

  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    if (!args.isEmpty() || !kwargs.isEmpty()) {
      throw Starlark.errorf("object() takes no arguments");
    }
    final PyObject newInstance = __new__(args, kwargs, thread);
    newInstance.__init__(args, kwargs);
    return newInstance;
  }

  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    throw Starlark.errorf("TypeError: '%s' not supported between instances of '%s' and '%s'", op, this.__name__(), that);
  }

  @Override
  public ImmutableCollection<String> getFieldNames() {
    return Stream.concat(
      specialMethods.stream().map(Object::toString),
      Stream.of(
        PyProtocols.__INIT__,
        PyProtocols.__NEW__,
        PyProtocols.__SUBCLASSHOOK__,
        PyProtocols.__INIT_SUBCLASS__,
        PyProtocols.__FORMAT__,
        PyProtocols.__DIR__,
        PyProtocols.__CLASS__)
    ).collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LarkyBaseObjectType)) {
      return false;
    }
    return this == obj;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }


  enum LarkyBaseObjectTypeSingleton {
    INSTANCE;

    private final PyObject inst;

    LarkyBaseObjectTypeSingleton() {
      LarkyTypeObject type = new LarkyTypeObject(LarkyType.Origin.BUILTIN, "object", Dict.empty());
      LarkyType.setupInheritanceHierarchy(type, new LarkyType[0]);
      inst = new LarkyBaseObjectType(type);
    }

    public PyObject get() {
      return inst;
    }
  }
}
