package net.starlark.java.ext;

import java.nio.charset.CodingErrorAction;

class CodecHelper {

  private CodecHelper() { } // uninstantiable

  public static final String STRICT = "strict";
  public static final String IGNORE = "ignore";
  public static final String REPLACE = "replace";
  public static final String BACKSLASHREPLACE = "backslashreplace";
  public static final String NAMEREPLACE = "namereplace";
  public static final String XMLCHARREFREPLACE = "xmlcharrefreplace";
  public static final String SURROGATEESCAPE = "surrogateescape";
  public static final String SURROGATEPASS = "surrogatepass";

  static CodingErrorAction convertCodingErrorAction(String errors) {
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

}