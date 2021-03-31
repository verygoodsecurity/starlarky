package com.verygood.security.larky.modules.crypto.Util;

import com.google.common.base.Suppliers;
import com.google.common.collect.ForwardingList;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteArray;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetStringParser;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.util.ASN1Dump;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;


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

    public LarkyDerInteger(ASN1Integer asn1int) {
      super(asn1int);
      this.asn1int = asn1int;
    }

    public static LarkyDerInteger fromStarlarkInt(StarlarkInt n) {
      return new LarkyDerInteger(new ASN1Integer((n.toBigInteger())));
    }

    @StarlarkMethod(name="encode", useStarlarkThread = true)
    public LarkyByte encode(StarlarkThread thread) throws EvalException {
      LarkyByte b;
      try {
         b = (LarkyByte) LarkyByte.builder(thread).setSequence(this.asn1int.getEncoded()).build();
      } catch (IOException e) {
        throw Starlark.errorf(e.getMessage());
      }
      return b;
    }

    @StarlarkMethod(name="as_int")
    public StarlarkInt value() {
      return StarlarkInt.of(this.asn1int.getValue());
    }

    @StarlarkMethod(name="decode", parameters = {
        @Param(name="barr"),
        @Param(name="strict", named = true, defaultValue = "False")
    })
    public LarkyDerInteger decode(LarkyByteLike barr, Boolean strict) throws EvalException {
      try {
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

  public static class LarkyASN1Sequence extends ForwardingList<LarkyASN1Encodable> implements LarkyASN1Value  {

    private final Supplier<List<LarkyASN1Encodable>> memoizedSupplier;

    public LarkyASN1Sequence(ASN1Sequence seq) {
      memoizedSupplier = Suppliers.memoize(() ->
              Arrays.stream(seq.toArray())
              .map(LarkyASN1Encodable.class::cast)
              .collect(Collectors.toList()));
    }

    @Override
    protected List<LarkyASN1Encodable> delegate() {
      return memoizedSupplier.get();
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
      return this.getSeq();
    }

    private DERSequence getSeq() {
      return new DERSequence(this.delegate().toArray(new ASN1Encodable[0]));
    }

    static public LarkyASN1Value asASN1Encodable(Object obj) throws EvalException {
      if(obj instanceof StarlarkInt) {
          StarlarkInt i = (StarlarkInt) obj;
          return new LarkyOctetString(
              new DEROctetString(i.toBigInteger().toByteArray()));
          //return LarkyDerInteger.fromStarlarkInt((StarlarkInt)obj);
      }
      else if (obj instanceof LarkyDerInteger) {
        StarlarkInt i = ((LarkyDerInteger) obj).value();
        return new LarkyOctetString(
            new DEROctetString(i.toBigInteger().toByteArray()));

        //return (LarkyDerInteger) obj;
      }
      // it's a binary string
      else if (obj instanceof LarkyByteLike){
        LarkyByteLike b = (LarkyByteLike) obj;
        return new LarkyOctetString(new DEROctetString(b.getBytes()));
//        ASN1Encodable[] ae = new ASN1Encodable[b.size()];
//        int ii = 0;
//        for(int i = 0; i < b.size(); ++i) {
//          ae[i] = LarkyDerInteger.fromStarlarkInt(b.get(i));
//        }
//        return new LarkyASN1Sequence(new DERSequence(ae));
        //return LarkyDerBitString.fromStarlarkByteLike(b);
      }
//      else if(obj instanceof DERBitString) {
//        DERBitString dbs = (DERBitString) obj;
//        return new LarkyDerBitString(dbs);
//      }
      else if(obj instanceof LarkyOctetString) {
        return (LarkyOctetString) obj;
      }
      else {
        throw Starlark.errorf("Unknown type %s to convert to asASN1Encodable", Starlark.type(obj));
      }
    }

    @StarlarkMethod(
        name = "append",
        doc = "Adds an item to the end of the list.",
        parameters = {@Param(name = "item", doc = "Item to add at the end.")})
    @SuppressWarnings("unchecked")
    public void append(Object item) throws EvalException {
      if(item instanceof String) {
        String s = (String) item;
        LarkyOctetString octetString = new LarkyOctetString(new DEROctetString(s.getBytes()));
        this.delegate().add(new LarkyASN1Encodable(asASN1Encodable(octetString)));
      }
      else {
        this.delegate().add(new LarkyASN1Encodable(asASN1Encodable(item)));
      }
    }

    public byte[] definiteForm(int l){

      if(l > 127) {
        byte[] bytes = BigInteger.valueOf(l).toByteArray();
        byte[] returned = new byte[bytes.length+1];
        returned[0] = (byte)(bytes.length + 128);
        System.arraycopy(bytes,0,returned,1,bytes.length);
        return returned;
      }
      return BigInteger.valueOf(l).toByteArray();
    }

    @StarlarkMethod(
        name = "encode",
        useStarlarkThread = true)
    public LarkyByteLike encode(StarlarkThread thread) throws EvalException, IOException {
      System.out.println(ASN1Dump.dumpAsString(this.getSeq(), true));
//      System.out.println("------------");
//      System.out.println(ASN1Dump.dumpAsString(this.getSeq(), false));
      //DEROctetString octetString =  new DEROctetString(this.getSeq().getObjectAt(0));
//      BEROctetString octetString = new BEROctetString(this.getSeq().getEncoded());
      //byte[] encoded = octetString.getEncoded();
//      byte[] buf = new byte[40];
//      ByteArrayInputStream is = new ByteArrayInputStream(buf);
//      ASN1StreamParser asn1StreamParser = new ASN1StreamParser(is);

//      ByteArrayOutputStream bOut = new ByteArrayOutputStream();
//     DERSequenceGenerator  seqGen = new DERSequenceGenerator(bOut);
//      for (ASN1Encodable asn1Encodable : this.getSeq()) {
//        seqGen.addObject(asn1Encodable);
//      }
//      seqGen.close();
//      return LarkyByteArray.builder(thread).setSequence(bOut.toByteArray()).build();

/*
ByteArrayOutputStream baos = new ByteArrayOutputStream();
var x = ASN1OutputStream.create(baos);
x.writeEncoded(true, BERTags.CONSTRUCTED | BERTags.SEQUENCE, ((LarkyOctetString) encodables[0]).str.getOctets());
baos.toByteArray();
 */
      return LarkyByteArray.builder(thread).setSequence(this.getSeq().getEncoded()).build();
    }

  }

}
