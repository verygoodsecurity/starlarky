package com.verygood.security.larky.objects;

import com.verygood.security.larky.objects.type.LarkyProvidedTypeClass;
import com.verygood.security.larky.objects.type.LarkyType;

public interface LarkyBindable {

  LarkyType getBoundOwner();

  void bindToOwner(LarkyType cls);

  /**
   * When we set a function to a type, we must bind it to the type.
   *
   * This is done in {@link LarkyProvidedTypeClass}
   */
  default void bind(LarkyType cls) {
    bindToOwner(cls);
  }

  default boolean isBound() {
    return getBoundOwner() != null;
  }

  default LarkyType bindToOwnerIfNotBound(LarkyType clsToBindTo) {
    LarkyType previous = getBoundOwner();
    if(previous == null) {
      bindToOwner(clsToBindTo);
    }
    return previous;
  }
}
