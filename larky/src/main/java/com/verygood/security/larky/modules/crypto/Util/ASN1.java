package com.verygood.security.larky.modules.crypto.Util;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteArray;
import com.verygood.security.larky.modules.types.LarkyByteLike;
import com.verygood.security.larky.modules.types.LarkyObject;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetStringParser;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DLSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;


public class ASN1 {

  public interface LarkyASN1Value extends ASN1Encodable, StarlarkValue {}

  public static class LarkyASN1Encodable extends ASN1Object implements LarkyASN1Value {
    private final ASN1Encodable encodable;

    LarkyASN1Encodable(ASN1Encodable encodable) {
      this.encodable = encodable;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
      return this.encodable.toASN1Primitive();
    }
  }

  public static class LarkyDerInteger extends LarkyASN1Encodable {

    private ASN1Integer asn1int;

    @VisibleForTesting
    LarkyDerInteger(ASN1Integer asn1int) {
      super(asn1int);  //TODO: check to make sure it is a positive value!!
      this.asn1int = asn1int;
    }

    public static LarkyDerInteger fromStarlarkInt(StarlarkInt n) {
      return fromBigInteger(n.toBigInteger());
    }

    public static LarkyDerInteger fromBigInteger(BigInteger n) {
      return new LarkyDerInteger(new ASN1Integer(n));
    }

    @StarlarkMethod(name="encode", useStarlarkThread = true)
    public LarkyByte encode(StarlarkThread thread) throws EvalException {
      LarkyByte b;
      try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        ASN1OutputStream os = ASN1OutputStream.create(baos);
        os.writeObject(this.asn1int);
        os.flush();
        b = (LarkyByte) LarkyByte.builder(thread).setSequence(baos.toByteArray()).build();
      } catch (IOException e) {
        throw Starlark.errorf(e.getMessage());
      }
      return b;
    }

    @StarlarkMethod(name="as_int")
    public StarlarkInt value() {
      return StarlarkInt.of(this.asn1int.getPositiveValue());
    }

    @StarlarkMethod(name="decode", parameters = {
        @Param(name="barr"),
        @Param(name="strict", named = true, defaultValue = "False")
    })
    public LarkyDerInteger decode(LarkyByteLike barr, Boolean strict) throws EvalException {
      try {
        if(strict)
        asn1int = (ASN1Integer) ASN1Primitive.fromByteArray(barr.getBytes());
      } catch (IOException e) {
        throw Starlark.errorf(e.getMessage());
      }
      //System.out.println(ASN1Dump.dumpAsString(intgr, true));
      return this;
    }
  }

  public static class LarkyOctetString extends LarkyASN1Encodable implements ASN1OctetStringParser {

    private DEROctetString string;

    public LarkyOctetString(DEROctetString string) {
      super(string);
      this.string = string;
    }

    @Override
    public InputStream getOctetStream() {
      return this.string.getOctetStream();
    }

    @Override
    public ASN1Primitive getLoadedObject() throws IOException {
      return this.string.getLoadedObject();
    }
  }

  public static class LarkyDerBitString extends LarkyASN1Encodable implements ASN1String {

    private final DERBitString derBitString;

    public LarkyDerBitString(DERBitString derBitString) {
      super(derBitString);
      this.derBitString = derBitString;
    }
    static public LarkyDerBitString fromStarlarkByteLike(LarkyByteLike b) {
      return new LarkyDerBitString(new DERBitString(b.getBytes()));
    }
    @Override
    public String getString() {
      return this.derBitString.getString();
    }
  }

  public static class LarkyDerUTF8String extends LarkyASN1Encodable implements ASN1String {

    private final DERUTF8String derutf8String;

    public LarkyDerUTF8String(DERUTF8String derutf8String) {
      super(derutf8String);
      this.derutf8String = derutf8String;
    }

    @Override
    public String getString() {
      return this.derutf8String.getString();
    }
  }

  public static class LarkyDerNull implements StarlarkValue {
    static DERNull NULL;
  }

  public static class LarkySetOf extends DERSet implements StarlarkValue {

  }

  public static class LarkyDerObjectId extends ASN1ObjectIdentifier implements StarlarkValue {

    /**
     * Create an OID based on the passed in String.
     *
     * @param identifier a string representation of an OID.
     */
    public LarkyDerObjectId(String identifier) {
      super(identifier);
    }
  }

  public static class LarkyASN1Sequence extends LarkyASN1Encodable implements org.bouncycastle.util.Iterable<ASN1Encodable>  {

    static public LarkyASN1Sequence fromList(StarlarkList<?> obj) throws EvalException {
      ASN1Encodable[] encodables = new ASN1Encodable[obj.size()];
      for (int i = 0; i < obj.size(); ++i) {
        encodables[i] = LarkyASN1Sequence.asASN1Encodable(obj.get(i));
      }
      DERSequence asn1Encodables = new DERSequence(encodables);
      return new LarkyASN1Sequence(asn1Encodables);
    }

    static public LarkyASN1Value asASN1Encodable(Object obj) throws EvalException {
      if(obj instanceof StarlarkInt) {
          StarlarkInt i = (StarlarkInt) obj;
          return LarkyDerInteger.fromStarlarkInt(i);
      }
      else if (obj instanceof LarkyDerInteger) {
        return (LarkyDerInteger) obj;
      }
      // it's a binary string
      else if (obj instanceof LarkyByteLike){
        LarkyByteLike b = (LarkyByteLike) obj;
        return new LarkyOctetString(new DEROctetString(b.getBytes()));
      }
      else if(obj instanceof LarkyOctetString) {
        return (LarkyOctetString) obj;
      }
      else if(obj instanceof LarkyObject) {
        LarkyObject lobj = ((LarkyObject) obj);
        switch(lobj.type()) {
          case "DerInteger":
            long value2 = Long.parseLong(String.valueOf(lobj.getField("value")));
            ASN1Integer value1 = new ASN1Integer(value2);
            LarkyDerInteger value = new LarkyDerInteger(value1);
            return value;
          case "DerSequence":
            StarlarkList<?> seq = (StarlarkList<?>) lobj.getField("_seq");
            Objects.requireNonNull(seq);
            return LarkyASN1Sequence.fromList(seq);
          default:
            throw Starlark.errorf("Unknown type %s to convert to asASN1Encodable", Starlark.type(obj));
        }
      }
      else if(obj instanceof DERBitString) {
        System.out.println("DID I GET HERE????");
        DERBitString dbs = (DERBitString) obj;
        return new LarkyDerBitString(dbs);
      }
      else {
        throw Starlark.errorf("Unknown type %s to convert to asASN1Encodable", Starlark.type(obj));
      }
    }

    private ASN1Sequence seq;

    public LarkyASN1Sequence(ASN1Sequence seq) {
      super(seq);
      this.seq = seq;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
      return this.seq;
    }

    @StarlarkMethod(
        name = "append",
        doc = "Adds an item to the end of the list.",
        parameters = {@Param(name = "item", doc = "Item to add at the end.")})
    @SuppressWarnings("unchecked")
    public void append(Object item) throws EvalException {
      ASN1Encodable atom;
      if(item instanceof String) {
        String s = (String) item;
        LarkyOctetString octetString = new LarkyOctetString(new DEROctetString(s.getBytes()));
        atom = new LarkyASN1Encodable(asASN1Encodable(octetString));
      }
      else {
        atom = new LarkyASN1Encodable(asASN1Encodable(item));
      }
      this.seq = new DLSequence(
          Stream.concat(
              Arrays.stream(this.seq.toArray()), Arrays.stream(new ASN1Encodable[]{atom}))
              .toArray(ASN1Encodable[]::new)
      );
    }

    @StarlarkMethod(name="decode", parameters = {
        @Param(name="barr"),
        @Param(name="strict", named = true, defaultValue = "False")
    })
    public LarkyASN1Sequence decode(LarkyByteLike barr, Boolean strict) throws EvalException {
      try {
        this.seq = ((ASN1Sequence) DLSequence.fromByteArray(barr.getBytes()));
      } catch (IOException e) {
        throw Starlark.errorf(e.getMessage());
      }
      //System.out.println(ASN1Dump.dumpAsString(this.seq, true));
      return this;
    }

    @StarlarkMethod(
        name = "encode",
        useStarlarkThread = true)
    public LarkyByteLike encode(StarlarkThread thread) throws EvalException, IOException {
      //System.out.println(ASN1Dump.dumpAsString(this.getSeq(), true));
      LarkyByteArray b;
       try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
         ASN1OutputStream os = ASN1OutputStream.create(baos);
         os.writeObject(this.seq);
         os.flush();
         b = (LarkyByteArray) LarkyByteArray.builder(thread).setSequence(baos.toByteArray()).build();
       } catch (IOException e) {
         throw Starlark.errorf(e.getMessage());
       }
       return b;
    }

    @NotNull
    @Override
    public Iterator<ASN1Encodable> iterator() {
      return this.seq.iterator();
    }
  }

}
