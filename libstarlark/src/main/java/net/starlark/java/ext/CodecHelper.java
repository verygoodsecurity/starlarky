package net.starlark.java.ext;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

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

  public static final class ThreadLocalCoders {

      private static final int CACHE_SIZE = 10;

      private abstract static class Cache {

        // Thread-local reference to array of cached objects, in LRU order
        private final ThreadLocal<Object[]> cache = new ThreadLocal<>();
        private final int size;

        Cache(int size) {
          this.size = size;
        }

        abstract Object create(Object name);

        private void moveToFront(Object[] oa, int i) {
          Object ob = oa[i];
          System.arraycopy(oa, 0, oa, 1, i);
          oa[0] = ob;
        }

        abstract boolean hasName(Object ob, Object name);

        Object forName(Object name) {
          Object[] oa = cache.get();
          if (oa == null) {
            oa = new Object[size];
            cache.set(oa);
          } else {
            for (int i = 0; i < oa.length; i++) {
              Object ob = oa[i];
              if (ob == null)
                continue;
              if (hasName(ob, name)) {
                if (i > 0)
                  moveToFront(oa, i);
                return ob;
              }
            }
          }

          // Create a new object
          Object ob = create(name);
          oa[oa.length - 1] = ob;
          moveToFront(oa, oa.length - 1);
          return ob;
        }
      }

      private static final ThreadLocalCoders.Cache decoderCache = new ThreadLocalCoders.Cache(CACHE_SIZE) {
        boolean hasName(Object ob, Object name) {
          if (name instanceof String)
            return (((CharsetDecoder) ob).charset().name().equals(name));
          if (name instanceof Charset)
            return ((CharsetDecoder) ob).charset().equals(name);
          return false;
        }

        Object create(Object name) {
          if (name instanceof String)
            return Charset.forName((String) name).newDecoder();
          if (name instanceof Charset)
            return ((Charset) name).newDecoder();
          assert false;
          return null;
        }
      };

      public static CharsetDecoder decoderFor(Object name) {
        CharsetDecoder cd = (CharsetDecoder) decoderCache.forName(name);
        cd.reset();
        return cd;
      }

      private static final ThreadLocalCoders.Cache encoderCache = new ThreadLocalCoders.Cache(CACHE_SIZE) {
        boolean hasName(Object ob, Object name) {
          if (name instanceof String)
            return (((CharsetEncoder) ob).charset().name().equals(name));
          if (name instanceof Charset)
            return ((CharsetEncoder) ob).charset().equals(name);
          return false;
        }

        Object create(Object name) {
          if (name instanceof String)
            return Charset.forName((String) name).newEncoder();
          if (name instanceof Charset)
            return ((Charset) name).newEncoder();
          assert false;
          return null;
        }
      };

      public static CharsetEncoder encoderFor(Object name) {
        CharsetEncoder ce = (CharsetEncoder) encoderCache.forName(name);
        ce.reset();
        return ce;
      }

    }

}