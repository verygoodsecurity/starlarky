/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.build.android.desugar.langmodel;

import org.objectweb.asm.Type;

/** The key that indexes a class member, including fields, constructors and methods. */
public abstract class ClassMemberKey<T extends ClassMemberKey<T>>
    implements TypeMappable<T>, Comparable<T> {

  /**
   * The class or interface that owns the class member, i.e. the immediate enclosing class of the
   * declaration site of a field, constructor or method.
   */
  public abstract ClassName owner();

  /** The simple name of the class member. */
  public abstract String name();

  /** The descriptor of the class member. */
  public abstract String descriptor();

  /** The binary name of {@link #owner()} */
  public final String ownerName() {
    return owner().binaryName();
  }

  /** The asm type name of {@link #owner()} */
  public final Type ownerAsmObjectType() {
    return owner().toAsmObjectType();
  }

  /** Whether member key represents a constructor. */
  public final boolean isConstructor() {
    return "<init>".equals(name());
  }

  /** The simple name with name suffix. */
  final String nameWithSuffix(String suffix) {
    return name() + '$' + suffix;
  }

  /** Produces a new class member key by mapping this key instance. */
  @Override
  public abstract T acceptTypeMapper(TypeMapper typeMapper);

  @Override
  public int compareTo(T other) {
    int ownerComparison = owner().compareTo(other.owner());
    if (ownerComparison != 0) {
      return ownerComparison;
    }

    int nameComparison = name().compareTo(other.name());
    if (nameComparison != 0) {
      return nameComparison;
    }

    return descriptor().compareTo(other.descriptor());
  }
}
