package com.verygood.security.larky.objects.type;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.verygood.security.larky.objects.PyObject;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


//override built-in type and mimic python built-in type
@StarlarkBuiltin(
  name = "type",
  doc =
    "Returns the type name of its argument. This is useful for debugging and "
      + "type-checking. Examples:"
      + "<pre class=\"language-python\">"
      + "type(2) == \"int\"\n"
      + "type([1]) == \"list\"\n"
      + "type(struct(a = 2)) == \"struct\""
      + "</pre>"
      + "This function might change in the future. To write Python-compatible code and "
      + "be future-proof, use it only to compare return values: "
      + "<pre class=\"language-python\">"
      + "if type(x) == type([]):  # if x is a list"
      + "</pre>" +
      "\n" +
      "Otherwise, the type will default to the default Starlark::type() method invocation"
)
final public class LarkyTypeObject implements LarkyType {

  private static final Supplier<LarkyType[]> DEFAULT_HIERARCHY = Suppliers.memoize(
    () -> new LarkyType[]{(LarkyType) LarkyBaseObjectType.getInstance()}
  );

  private final Supplier<ImmutableSet<SpecialMethod>> specialMethods =
    Suppliers.memoize(
      () -> getFieldNames()
            .stream()
            .map(SpecialMethod::of)
            .filter(p -> p != SpecialMethod.NOT_SET)
            .collect(Sets.toImmutableEnumSet()));


  private Origin origin;
  private Map<String, Object> __dict__;
  private final Set<LarkyType> allSubclasses = new HashSet<>();
  private List<LarkyType> __mro__;
  private String name;
  private LarkyType[] __bases__;
  private LarkyType __base__;

  public LarkyTypeObject(Origin origin, String typeName, Dict<String, Object> dikt) {
    this.origin = origin;
    this.name = typeName;
    this.__dict__ = new HashMap<>(dikt);
    this.__new__(Tuple.of(this), dikt, null);
  }

  public static LarkyTypeObject getInstance() {
    return LarkyTypeSingleton.INSTANCE.get();
  }

  public static LarkyType create(
    StarlarkThread thread,
    String name,
    Tuple bases,
    Dict<String, Object> dict // Dict<? extends CharSequence, ?> dict,
  ) {

    return LarkyTypeObject.create(
      name,
      bases,
      dict,
      (type) -> new LarkyProvidedTypeClass(thread, type)
    );
  }

  public static LarkyType create(
    @Nullable String name,
    @NotNull Tuple bases,
    @NotNull Dict<String, Object> dict,
    @NotNull Function<LarkyTypeObject, ForwardingLarkyType> constructor
  ) {
    LarkyType[] basesArr;
    // Set __base__ and __bases__ for the type
    if (bases.size() == 0) {
      basesArr = new LarkyType[]{(LarkyType) LarkyBaseObjectType.getInstance()};
    } else {
      basesArr = new LarkyType[bases.size()];
      for (int i = 0; i < bases.size(); i++) {
        Object b = bases.get(i);
        basesArr[i] = (LarkyType) b;
      }
    }
    final LarkyTypeObject newType = new LarkyTypeObject(Origin.LARKY, name, dict);
    final ForwardingLarkyType result = constructor.apply(newType);
    LarkyType.setupInheritanceHierarchy(result, basesArr);
    return result;
  }

  public static @NotNull LarkyType createBuiltinType(@NotNull String name) {
    final LarkyTypeObject type = new LarkyTypeObject(Origin.BUILTIN, name, Dict.empty());
    LarkyType.setupInheritanceHierarchy(type, DEFAULT_HIERARCHY.get());
    return type;
  }

  @Override
  public void setMRO(List<LarkyType> mro) {
    this.__mro__ = mro;
  }

  @Override
  public void setBaseClasses(LarkyType[] parentClasses) {
    this.__bases__ = parentClasses;
    this.__base__ = null;
  }

  //override built-in type
  @StarlarkMethod(
    name = "type",
    selfCall = true,
    doc =
      "Returns the type name of its argument. This is useful for debugging and "
        + "type-checking. Examples:"
        + "<pre class=\"language-python\">"
        + "type(2) == \"int\"\n"
        + "type([1]) == \"list\"\n"
        + "type(struct(a = 2)) == \"struct\""
        + "</pre>"
        + "This function might change in the future. To write Python-compatible code and "
        + "be future-proof, use it only to compare return values: "
        + "<pre class=\"language-python\">"
        + "if type(x) == type([]):  # if x is a list"
        + "</pre>" +
        "\n" +
        "Otherwise, the type will default to the default Starlark::type() method invocation",
    parameters = {
      @Param(name = "object_or_name", doc = "The object to check type of."),
      @Param(name = "bases", defaultValue = "None"),
      @Param(name = "ns", defaultValue = "None")
    },
    extraKeywords = @Param(name = "kwargs", defaultValue = "{}"),
    useStarlarkThread = true
  )
  public Object type(Object objectOrName, Object bases, Object ns, Dict<String, Object> kwargs, StarlarkThread thread) throws EvalException {
    if (kwargs.size() != 0) {
      throw Starlark.errorf("type() takes 1 or 3 arguments");
    }
    Object result;
    if (Starlark.isNullOrNone(bases) && Starlark.isNullOrNone(ns)) {
      result = TypeClassLookup.type(objectOrName, thread);
    } else {
      result = create(
        thread,
        StarlarkUtil.convertOptionalString(objectOrName),
        (Tuple) Sequence.cast(bases, Object.class, "in type(), could not cast bases to Tuple"),
        Dict.cast(ns, String.class, Object.class, "in type(), could not cast ns to Dict<String, Object>")
      );
    }

    return result;
  }

  @Override
  public LarkyType typeClass() {
    return this;
  }

  @Override
  public PyObject __new__(Tuple args, Dict<String, Object> kwargs, StarlarkThread thread) {
    LarkyType __class__ = (LarkyType) args.get(0);
    return __class__;
  }

  @Override
  public String toString() {
    return __repr__();
  }


  @Override
  public Map<String, Object> getInternalDictUnsafe() {
    return __dict__;
  }

  @Override
  public Origin getOrigin() {
    return this.origin;
  }

  @Override
  public Object getBase() {
    if (this.__base__ == null && this.__bases__ != null) {
      this.__base__ = this.__bases__[0];
    }
    return this.__base__;
  }

  @Override
  public Tuple getBases() {
    if (__bases__ != null) {
      return Tuple.of((Object[]) __bases__);
    }
    return Tuple.empty();
  }

  @SuppressWarnings("scwjava_CollectionsDonotexposeinternalSets")
  @Override
  public Set<LarkyType> getAllSubclasses() {
    return this.allSubclasses;
  }


  @Override
  public Tuple getMRO() {
    return Tuple.copyOf(__mro__);
  }

  @Override
  public String __name__() {
    return typeName();
  }

  @Override
  public String typeName() {
    return this.name;
  }

  public StarlarkThread getCurrentThread() {
    return null;
  }

  @Override
  public ImmutableCollection<String> getFieldNames() {
    return ImmutableSortedSet.of(
//      "__abstractmethods__",
      "__base__",
      "__bases__",
//      "__basicsize__",
      "__call__",
      "__delattr__",
      "__dict__",
//      "__dictoffset__",
      "__dir__",
//      "__doc__",
//      "__flags__",
      "__getattribute__",
      "__init__",
//      "__instancecheck__",
//      "__itemsize__",
//      "__module__",
      "__mro__",
      "__name__",
      "__new__",
//      "__prepare__",
//      "__qualname__",
      "__repr__",
      "__setattr__",
//      "__subclasscheck__",
      "__subclasses__",
//      "__text_signature__",
//      "__weakrefoffset__",
      "mro"
    );
  }

  @Override
  public ImmutableSet<SpecialMethod> getSpecialMethods() {
    return specialMethods.get();
  }

  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    throw Starlark.errorf(
        "unsupported binary operation: %s %s %s", Starlark.type(this), op, Starlark.type(that));
  }

  private enum LarkyTypeSingleton {
    INSTANCE;

    private final LarkyTypeObject type;

    LarkyTypeSingleton() {
      type = new LarkyTypeObject(Origin.BUILTIN, "type", Dict.empty());
      LarkyType.setupInheritanceHierarchy(type, DEFAULT_HIERARCHY.get());
    }

    public LarkyTypeObject get() {
      return type;
    }
  }

}
