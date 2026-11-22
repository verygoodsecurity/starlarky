/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.modules.types.structs;

import com.verygood.security.larky.modules.multidict.LarkyMultiDict;
import com.verygood.security.larky.modules.types.Property;
import com.verygood.security.larky.parser.CurrentEvaluation;
import java.util.function.BiConsumer;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import org.jetbrains.annotations.Nullable;

/** Java backing for the request constructed by {@code @vgs//http/request}. */
public final class VgsHttpRequest extends HttpRequest {
  private static final int URL_SPLITTER = 0;
  private static final int FULL_URL_PROPERTY = 1;
  private static final int DATA_PROPERTY = 2;
  private static final int HEADER_FACTORY = 0;
  private static final int HEADER_OWNER = 1;

  private VgsHttpRequest(StarlarkCallable constructor, StarlarkThread thread) {
    super(constructor, thread);
  }

  /** Creates the object that the shared urllib initializer binds its methods to. */
  public static VgsHttpRequest create(StarlarkCallable constructor, StarlarkThread thread) {
    return new VgsHttpRequest(constructor, thread);
  }

  /** Installs VGS properties after the urllib initializer has populated its base fields. */
  public void installFields(
      Object url, Object headers, StarlarkCallable urlSplitter, StarlarkCallable headerFactory,
      StarlarkThread thread) throws EvalException, InterruptedException {
    requireMutable();
    if (!(fields.get("full_url") instanceof Property fullUrl)
        || !(fields.get("data") instanceof Property data)) {
      throw Starlark.errorf("Expected an initialized urllib request");
    }
    super.setField("url", Tuple.of(urlSplitter, fullUrl, data));
    super.setField("body", Starlark.NONE);
    replaceHeaders(headers, headerFactory, thread);
    updatePath(url, urlSplitter, thread);
  }

  @Override
  public Object getField(String name, @Nullable StarlarkThread thread) {
    if ("body".equals(name) && fields.get("body") == Starlark.NONE) {
      return super.getField("_data", thread);
    }
    if ("headers".equals(name) && fields.get("headers") instanceof Tuple) {
      return super.getField("_headers", thread);
    }
    if (("url".equals(name) || "full_url".equals(name) || "data".equals(name))
        && fields.get("url") instanceof Tuple properties) {
      if ("url".equals(name)) {
        return getField("full_url", thread);
      }
      if ("full_url".equals(name) && fields.get("full_url") == properties.get(FULL_URL_PROPERTY)
          && fields.get("_full_url") instanceof String url) {
        Object fragment = fields.get("fragment");
        if (fragment instanceof String text) {
          return text.isEmpty() ? url : url + "#" + text;
        }
        if (fragment == Starlark.NONE) {
          return url;
        }
      }
      if ("data".equals(name) && fields.get("data") == properties.get(DATA_PROPERTY)) {
        return super.getField("_data", thread);
      }
    }
    return super.getField(name, thread);
  }

  @Override
  public void setField(String name, Object value) throws EvalException {
    if (!(value instanceof Property)) {
      if ("body".equals(name) && fields.get("body") == Starlark.NONE) {
        super.setField("data", value);
        return;
      }
      if ("headers".equals(name) && fields.get("headers") instanceof Tuple property) {
        requireMutable();
        try {
          replaceHeaders(value, (StarlarkCallable) property.get(HEADER_FACTORY),
              CurrentEvaluation.requireThread());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new EvalException(e);
        }
        return;
      }
      if ("url".equals(name) && fields.get("url") instanceof Tuple properties) {
        super.setField("full_url", value);
        try {
          updatePath(value, (StarlarkCallable) properties.get(URL_SPLITTER),
              CurrentEvaluation.requireThread());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new EvalException(e);
        }
        return;
      }
    }
    super.setField(name, value);
  }

  private void requireMutable() throws EvalException {
    if (((Dict<?, ?>) fields).mutability().isFrozen()) {
      throw Starlark.errorf("Attempting to update a frozen structure");
    }
  }

  private void replaceHeaders(
      Object source, StarlarkCallable factory, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Object value = Starlark.positionalOnlyCall(thread, factory, source);
    if (!(value instanceof SimpleStruct headers)) {
      throw Starlark.errorf("Expected a request header struct");
    }
    super.setField("_headers", headers);
    super.setField("headers", Tuple.of(factory, headers));
  }

  private void updatePath(Object url, StarlarkCallable splitter, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Object parsed = Starlark.positionalOnlyCall(thread, splitter, url);
    super.setField("path", Starlark.getattr(thread.mutability(), thread.getSemantics(), parsed, "path", null));
    super.setField("query_string", Starlark.getattr(thread.mutability(), thread.getSemantics(), parsed, "query", null));
  }

  public Object body(StarlarkThread thread) {
    return getField("body", thread);
  }

  /**
   * Streams the default header pairs without allocating an items list. The visitor must not
   * modify the request. Returns false without visiting any pairs when the host should call
   * the script-visible {@code items()} method instead.
   */
  public boolean visitHeaderLines(BiConsumer<Object, Object> visitor) {
    if (!(fields.get("headers") instanceof Tuple property)
        || !(fields.get("_headers") instanceof LarkyMultiDict headers)
        || headers != property.get(HEADER_OWNER)) {
      return false;
    }
    return headers.visitItems(visitor);
  }

  public Object headerItems(StarlarkThread thread) throws EvalException, InterruptedException {
    Object headers = getField("headers", thread);
    if (Starlark.isNullOrNone(headers)) {
      return Starlark.NONE;
    }
    Object items = Starlark.getattr(thread.mutability(), thread.getSemantics(), headers, "items", null);
    return Starlark.call(thread, items, Tuple.empty(), Dict.empty());
  }
}
