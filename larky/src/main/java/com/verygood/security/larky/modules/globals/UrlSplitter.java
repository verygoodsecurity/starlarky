package com.verygood.security.larky.modules.globals;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.Tuple;

/** Performs the string-only portion of urllib.parse.urlsplit. */
final class UrlSplitter {
  // Bound work in a single native call. The facade handles larger inputs in Starlark.
  private static final int MAX_INPUT_LENGTH = 16 * 1024;
  private static final CharMatcher C0_OR_SPACE = CharMatcher.inRange('\u0000', ' ');
  private static final CharMatcher UNSAFE = CharMatcher.anyOf("\t\r\n").precomputed();
  private static final CharMatcher SCHEME_CHAR = CharMatcher.inRange('a', 'z')
      .or(CharMatcher.inRange('A', 'Z'))
      .or(CharMatcher.inRange('0', '9'))
      .or(CharMatcher.anyOf("+-."))
      .precomputed();
  private static final CharMatcher AUTHORITY_END = CharMatcher.anyOf("/?#");

  private UrlSplitter() {}

  static Object split(Object value, Object defaultScheme, Object fragments) {
    if (!(value instanceof String url)
        || !(defaultScheme instanceof String scheme)
        || !(fragments instanceof Boolean allowFragments)
        || url.length() > MAX_INPUT_LENGTH || scheme.length() > MAX_INPUT_LENGTH) {
      return Starlark.NONE;
    }

    // urllib preserves trailing spaces in the URL but trims both ends of the default scheme.
    url = UNSAFE.removeFrom(C0_OR_SPACE.trimLeadingFrom(url));
    scheme = UNSAFE.removeFrom(C0_OR_SPACE.trimFrom(scheme));

    int start = 0;
    int colon = url.indexOf(':');
    if (colon > 0 && isScheme(url, colon)) {
      scheme = Ascii.toLowerCase(url.substring(0, colon));
      start = colon + 1;
    }

    String netloc = "";
    if (url.startsWith("//", start)) {
      int hostStart = start + 2;
      int end = AUTHORITY_END.indexIn(url, hostStart);
      start = end < 0 ? url.length() : end;
      netloc = url.substring(hostStart, start);
      if ((netloc.indexOf('[') >= 0) != (netloc.indexOf(']') >= 0)) {
        // Let the facade raise the same ValueError at the same Starlark frame.
        return Starlark.NONE;
      }
    }

    int fragmentStart = allowFragments ? url.indexOf('#', start) : -1;
    int end = fragmentStart < 0 ? url.length() : fragmentStart;
    int queryStart = url.indexOf('?', start, end);
    return Tuple.of(
        scheme,
        netloc,
        url.substring(start, queryStart < 0 ? end : queryStart),
        queryStart < 0 ? "" : url.substring(queryStart + 1, end),
        fragmentStart < 0 ? "" : url.substring(fragmentStart + 1));
  }

  private static boolean isScheme(String url, int end) {
    for (int i = 0; i < end; i++) {
      if (!SCHEME_CHAR.matches(url.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
