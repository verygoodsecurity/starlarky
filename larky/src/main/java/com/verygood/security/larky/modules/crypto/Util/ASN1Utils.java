package com.verygood.security.larky.modules.crypto.Util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.verygood.security.larky.modules.types.LarkyObject;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Null;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERUTF8String;

public class ASN1Utils {

  private ASN1Utils() {}

  static public Integer ensureDEREncoded(InputStream bais) throws IOException, EvalException {
    Integer tag = ASN1Utils.getTag(bais);
    int tagNo;
    if(tag != null) {
      tagNo = ASN1Utils.readTagNumber(bais, tag);
    }
    int rval = ASN1Utils.readLength(bais);// 2nd byte is the length
    bais.mark(0);
    bais.reset();
    return rval;
  }

  static Integer getTag(InputStream s) throws IOException {
    int tag = s.read();
    if (tag <= 0)
    {
        if (tag == 0)
        {
            throw new IOException("unexpected end-of-contents marker");
        }

        return null;
    }
    return tag;
  }

  static int readTagNumber(InputStream s, int tag) throws IOException {
      int tagNo = tag & 0x1f;

      //
      // with tagged object tag number is bottom 5 bits, or stored at the start of the content
      //
      if (tagNo == 0x1f)
      {
         tagNo = 0;

         int b = s.read();

         // X.690-0207 8.1.2.4.2
         // "c) bits 7 to 1 of the first subsequent octet shall not all be zero."
         if ((b & 0x7f) == 0) // Note: -1 will pass
         {
             throw new IOException("corrupted stream - invalid high tag number found");
         }

         while ((b >= 0) && ((b & 0x80) != 0))
         {
             tagNo |= (b & 0x7f);
             tagNo <<= 7;
             b = s.read();
         }

         if (b < 0)
         {
             throw new EOFException("EOF found inside tag value.");
         }

         tagNo |= (b & 0x7f);
      }
      return tagNo;
    }

  static int readLength(InputStream s) throws IOException, EvalException {
    int length = s.read();
    if (length < 0) {
      throw new EOFException("EOF found when length expected");
    }

    if (length == 0x80) {
      return -1;      // indefinite-length encoding
    }

    if (length > 127) {
      int size = length & 0x7f;

      // Note: The invalid long form "0xff" (see X.690 8.1.3.5c) will be caught here
      if (size > 4) {
        throw new EvalException("ValueError: DER length more than 4 bytes: " + size);
      }

      length = 0;
      for (int i = 0; i < size; i++) {
        int next = s.read();

        if (next < 0) {
          throw new EvalException("ValueError: EOF found reading length");
        }

        length = (length << 8) + next;
      }

      if (length < 0) {
        throw new EvalException("ValueError: corrupted stream - negative length found");
      }
      if (length <= 127) {
        throw new EvalException("ValueError: Invalid DER - length in long form but smaller than 128");
      }

    }

    return length;
  }

  public static class ASN1EncodableFactory {
    static public ASN1.LarkyASN1Encodable asASN1Encodable(Object obj) throws EvalException {
      // TODO: refactor this maybe into visitor pattern?
      // https://stackoverflow.com/questions/38920520/visitor-pattern-implementation-in-case-of-source-code-un-availability
      // https://stackoverflow.com/questions/51165280/best-design-pattern-to-avoid-instanceof-when-working-with-classes-that-cannot-be
      // https://stackoverflow.com/questions/3930808/how-to-avoid-large-if-statements-and-instanceof
      if (obj instanceof StarlarkInt) {
        StarlarkInt i = (StarlarkInt) obj;
        return ASN1.LarkyDerInteger.fromStarlarkInt(i);
      } else if (obj instanceof ASN1.LarkyDerInteger) {
        return (ASN1.LarkyDerInteger) obj;
      }
      // it's a binary string
      else if (obj instanceof StarlarkBytes) {
        StarlarkBytes b = (StarlarkBytes) obj;
        return new ASN1.LarkyOctetString(new DEROctetString(b.toByteArray()));
      } else if (obj instanceof ASN1.LarkyOctetString) {
        return (ASN1.LarkyOctetString) obj;
      } else if (obj instanceof LarkyObject) {
        LarkyObject lobj = ((LarkyObject) obj);
        switch (lobj.type()) {
          case "DerInteger":
            long value2 = Long.parseLong(String.valueOf(lobj.getField("value")));
            ASN1Integer value1 = new ASN1Integer(value2);
            ASN1.LarkyDerInteger value = new ASN1.LarkyDerInteger(value1);
            return value;
          case "DerSequence":
            StarlarkList<?> seq = (StarlarkList<?>) lobj.getField("_seq");
            Objects.requireNonNull(seq);
            return ASN1.LarkyASN1Sequence.fromList(seq);
          case "DerObjectId":
            String derObjectId = String.valueOf(lobj.getField("value"));
            return new ASN1.LarkyDerObjectId(new ASN1ObjectIdentifier(derObjectId));
          case "DerNull":
            return new ASN1.LarkyDerNull();
          case "DerBitString":
            StarlarkBytes bitstr = (StarlarkBytes) lobj.getField("value");
            Objects.requireNonNull(bitstr);
            return new ASN1.LarkyDerBitString(new DERBitString(bitstr.toByteArray()));
          case "DerOctetString":
            StarlarkBytes value3 = (StarlarkBytes) lobj.getField("value");
            Objects.requireNonNull(value3);
            return new ASN1.LarkyOctetString(new DEROctetString(value3.toByteArray()));
            // fall through
          case "DerUTF8String":
            String value4 = (String) lobj.getField("value");
            Objects.requireNonNull(value4);
            return new ASN1.LarkyDerUTF8String(new DERUTF8String(value4));
          case "DerSetOf":
            StarlarkList<?> setz = (StarlarkList<?>) lobj.getField("_seq");
            Objects.requireNonNull(setz);
            return ASN1.LarkySetOf.fromList(setz);
          default:
            throw Starlark.errorf("Unknown starlark type (%s) __class__ (%s) to convert to asASN1Encodable",
                Starlark.type(obj),
                lobj.type()
            );
        }
      }
      else if (obj instanceof ASN1Null) {
        return new ASN1.LarkyDerNull();
      }
      else if (obj instanceof ASN1Integer) {
        return new ASN1.LarkyDerInteger((ASN1Integer) obj);
      } else if (obj instanceof ASN1Sequence) {
        return new ASN1.LarkyASN1Sequence((ASN1Sequence) obj);
      }
      else if (obj instanceof ASN1ObjectIdentifier) {
        return new ASN1.LarkyDerObjectId((ASN1ObjectIdentifier) obj);
      }
      else if (obj instanceof DERBitString) {
        DERBitString dbs = (DERBitString) obj;
        return new ASN1.LarkyDerBitString(dbs);
      } else if (obj instanceof DEROctetString) {
        DEROctetString dbs = (DEROctetString) obj;
        return new ASN1.LarkyOctetString(dbs);
      } else {
        throw Starlark.errorf("Unknown type %s to convert to asASN1Encodable", Starlark.type(obj));
      }
    }
  }
}
