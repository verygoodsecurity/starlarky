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

package com.google.devtools.build.lib.events;

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * Passes through any events, and notes if any of them were errors. It is thread-safe as long as the
 * target eventHandler is thread-safe.
 *
 * <p>Optionally retains the first error event property value associated with a specified class.
 */
public final class ErrorSensingEventHandler<T> extends DelegatingEventHandler {

  @Nullable private final Class<T> errorPropertyClass;
  private final AtomicReference<T> errorProperty = new AtomicReference<>(null);
  private volatile boolean hasErrors;

  public ErrorSensingEventHandler(
      ExtendedEventHandler eventHandler, @Nullable Class<T> errorPropertyClass) {
    super(eventHandler);
    this.errorPropertyClass = errorPropertyClass;
  }

  public static ErrorSensingEventHandler<Void> withoutPropertyValueTracking(
      ExtendedEventHandler eventHandler) {
    return new ErrorSensingEventHandler<>(eventHandler, /*errorPropertyClass=*/ null);
  }

  @Override
  public void handle(Event e) {
    if (e.getKind() == EventKind.ERROR) {
      hasErrors = true;
      if (errorPropertyClass != null) {
        T propertyValue = e.getProperty(errorPropertyClass);
        if (propertyValue != null) {
          errorProperty.compareAndSet(/*expect=*/ null, /*update=*/ propertyValue);
        }
      }
    }
    super.handle(e);
  }

  /** Returns whether any of the events on this objects were errors. */
  public boolean hasErrors() {
    return hasErrors;
  }

  /** Returns the retained error event property value, if any. */
  @Nullable
  public T getErrorProperty() {
    return errorProperty.get();
  }
}
