package com.verygood.security.larky.objects.type;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.starlark.java.syntax.StarlarkType;
import net.starlark.java.syntax.TypeConstructor;
import net.starlark.java.syntax.TypeContext;
import net.starlark.java.syntax.Types;

/**
 * The Starlark type of the instances of a class defined with {@code type(name, bases, dict)} or
 * {@code types.new_class}: a nominal type whose supertypes are those of the class's MRO, so an
 * instance of a subclass is accepted where a base class is declared.
 *
 * <p>Instances have dynamic attributes, so every field is typed {@code Any}. Instances that are
 * callable (functions and methods) are also of type {@code Callable}, as StarlarkCallable
 * implementations are (CallUtils.buildSupertypes).
 */
public final class LarkyClassType extends StarlarkType {

  private final LarkyType cls;
  private final boolean callable;

  private LarkyClassType(LarkyType cls, boolean callable) {
    this.cls = cls;
    this.callable = callable;
  }

  /** Returns the type of the instances of {@code cls}. */
  public static LarkyClassType of(LarkyType cls) {
    return of(cls, false);
  }

  /** Returns the type of the instances of {@code cls}, which are {@code callable} or not. */
  public static LarkyClassType of(LarkyType cls, boolean callable) {
    return new LarkyClassType(unwrap(cls), callable);
  }

  /** A type constructor for {@code cls}, which takes no arguments. */
  public static StarlarkType create(LarkyType cls, ImmutableList<TypeConstructor.Term> args)
      throws TypeConstructor.Failure {
    if (!args.isEmpty()) {
      throw new TypeConstructor.Failure(
          String.format("'%s' does not accept arguments", cls.__name__()));
    }
    return of(cls);
  }

  private static LarkyType unwrap(LarkyType cls) {
    while (cls instanceof ForwardingLarkyType forwarding) {
      cls = forwarding.delegate();
    }
    return cls;
  }

  @Override
  public String typeRepr() {
    return cls.__name__();
  }

  @Override
  public List<StarlarkType> getSupertypes(TypeContext context) {
    ImmutableList.Builder<StarlarkType> supertypes = ImmutableList.builder();
    for (Object base : cls.getMRO()) {
      LarkyType baseType = unwrap((LarkyType) base);
      if (baseType != cls && !(baseType instanceof LarkyBaseObjectType)) {
        supertypes.add(new LarkyClassType(baseType, false));
      }
    }
    if (callable) {
      supertypes.add(Types.ANY_CALLABLE);
    }
    return supertypes.build();
  }

  @Override
  public StarlarkType getField(String name, TypeContext context) {
    return Types.ANY;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof LarkyClassType that && that.cls == cls;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(cls);
  }
}
