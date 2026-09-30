// Copyright 2021 Very Good Security Authors. All rights reserved.
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

package net.starlark.java.ext;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashMap;
import java.util.Map;

public class CodecHelper {

  private CodecHelper() { } // uninstantiable

  public static final String STRICT = "strict";
  public static final String IGNORE = "ignore";
  public static final String REPLACE = "replace";
  public static final String BACKSLASHREPLACE = "backslashreplace";
  public static final String NAMEREPLACE = "namereplace";
  public static final String XMLCHARREFREPLACE = "xmlcharrefreplace";
  public static final String SURROGATEESCAPE = "surrogateescape";
  public static final String SURROGATEPASS = "surrogatepass";

  public static CodingErrorAction convertCodingErrorAction(String errors) {
    CodingErrorAction errorAction;
    switch (errors) {
      case IGNORE:
        errorAction = CodingErrorAction.IGNORE;
        break;
      case REPLACE:
      case NAMEREPLACE:
        errorAction = CodingErrorAction.REPLACE;
        break;
      case STRICT:
      case BACKSLASHREPLACE:
      case SURROGATEPASS:
      case SURROGATEESCAPE:
      case XMLCHARREFREPLACE:
      default:
        errorAction = CodingErrorAction.REPORT;
        break;
    }
    return errorAction;
  }

  /**
   * Per-thread CharsetDecoders and CharsetEncoders, reused across calls (coders are not
   * thread-safe and are costly to create). Each is reset before it is returned.
   */
  public static final class ThreadLocalCoders {

    private ThreadLocalCoders() {}

    private static final ThreadLocal<Map<Charset, CharsetDecoder>> DECODERS =
        ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Map<Charset, CharsetEncoder>> ENCODERS =
        ThreadLocal.withInitial(HashMap::new);

    private static Charset charset(Object name) {
      return name instanceof Charset ? (Charset) name : Charset.forName((String) name);
    }

    public static CharsetDecoder decoderFor(Object name) {
      CharsetDecoder cd = DECODERS.get().computeIfAbsent(charset(name), Charset::newDecoder);
      cd.reset();
      return cd;
    }

    public static CharsetEncoder encoderFor(Object name) {
      CharsetEncoder ce = ENCODERS.get().computeIfAbsent(charset(name), Charset::newEncoder);
      ce.reset();
      return ce;
    }
  }
}