/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.modules.urllib;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkStrings;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

/**
 * The URL helpers of CPython 3.10's {@code Lib/urllib/parse.py} that {@code urllib.request.Request}
 * uses: {@code unwrap}, {@code _splittype}, {@code _splithost} and {@code _splittag}.
 *
 * <p>This is their only implementation: {@code @stdlib//urllib/parse} calls them through {@code
 * @stdlib//jurllib}, and {@link UrllibRequest} calls them directly.
 */
public final class UrlParts {

  private UrlParts() {}

  /**
   * {@code unwrap('<URL:scheme://host/path>')} is {@code 'scheme://host/path'}. A string that is
   * not a wrapped URL is returned stripped.
   */
  public static String unwrap(String url, StarlarkThread thread) {
    url = StarlarkStrings.strip(url, thread.getSemantics());
    if (url.startsWith("<") && url.endsWith(">")) {
      url = StarlarkStrings.strip(url.substring(1, url.length() - 1), thread.getSemantics());
    }
    if (url.startsWith("URL:")) {
      url = StarlarkStrings.strip(url.substring(4), thread.getSemantics());
    }
    return url;
  }

  /**
   * {@code _splittype('type:opaquestring')} is {@code ('type', 'opaquestring')}, with the type in
   * lower case; {@code (None, url)} without a type. CPython matches {@code ([^/:]+):(.*)}.
   */
  public static Tuple splittype(Object value, StarlarkThread thread) throws EvalException {
    String url = string("_splittype", value);
    int colon = url.indexOf(':');
    int slash = url.indexOf('/');
    if (colon > 0 && (slash < 0 || colon < slash)) {
      return Tuple.pair(StarlarkStrings.lower(url.substring(0, colon), thread), url.substring(colon + 1));
    }
    return Tuple.pair(Starlark.NONE, url);
  }

  /**
   * {@code _splithost('//host[:port]/path')} is {@code ('host[:port]', '/path')}; {@code (None,
   * url)} without a host. CPython matches {@code //([^/#?]*)(.*)}.
   */
  public static Tuple splithost(Object value) throws EvalException {
    String url = string("_splithost", value);
    if (!url.startsWith("//")) {
      return Tuple.pair(Starlark.NONE, url);
    }
    int end = 2;
    while (end < url.length() && "/#?".indexOf(url.charAt(end)) < 0) {
      end++;
    }
    String path = url.substring(end);
    if (!path.isEmpty() && path.charAt(0) != '/') {
      path = "/" + path;
    }
    return Tuple.pair(url.substring(2, end), path);
  }

  /** {@code _splittag('/path#tag')} is {@code ('/path', 'tag')}; {@code (url, None)} without a tag. */
  public static Tuple splittag(Object value) throws EvalException {
    String url = string("_splittag", value);
    int hash = url.lastIndexOf('#');
    return hash < 0
        ? Tuple.pair(url, Starlark.NONE)
        : Tuple.pair(url.substring(0, hash), url.substring(hash + 1));
  }

  private static String string(String function, Object value) throws EvalException {
    if (value instanceof String url) {
      return url;
    }
    throw Starlark.errorf("%s(url=...) expected `str`; got %s", function, Starlark.type(value));
  }
}
