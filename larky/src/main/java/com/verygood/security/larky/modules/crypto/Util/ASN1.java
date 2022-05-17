package com.verygood.security.larky.modules.crypto.Util;

import static com.verygood.security.larky.modules.crypto.Util.ASN1Utils.ASN1EncodableFactory;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkIndexable;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
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
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.ASN1TaggedObjectParser;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DLTaggedObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import lombok.SneakyThrows;


public class ASN1 {

  public abstract static class LarkyASN1Encodable extends ASN1Object implements ASN1Encodable, StarlarkValue {

    protected ASN1Encodable encodable;

    LarkyASN1Encodable(ASN1Encodable encodable) {
      this.encodable = encodable;
    }

    public Object toStarlarkPrimitive() throws EvalException {
      Object r = this.toStarlark();
      if (r instanceof String || r instanceof StarlarkValue) {
        return r;
      }
      throw Starlark.errorf("Unable to convert %s to Starlark Primitive", r.getClass());
    }

    @StarlarkMethod(
        name = "encode",
        useStarlarkThread = true)
    public StarlarkBytes encode(StarlarkThread thread) throws EvalException, IOException {
//      System.out.println(ASN1Dump.dumpAsString(this.encodable, true));
      StarlarkBytes b;
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        ASN1OutputStream os = ASN1OutputStream.create(baos);
        os.writeObject(this.encodable);
        os.flush();
        b = StarlarkBytes.of(thread.mutability(),baos.toByteArray());
//        b = (StarlarkBytes) StarlarkBytes.builder(thread).setSequence(baos.toByteArray()).build();
      } catch (IOException e) {
        throw Starlark.errorf(e.getMessage());
      }
      return b;
    }

    abstract Object toStarlark() throws EvalException;

    @Override
    public ASN1Primitive toASN1Primitive() {
      return this.encodable.toASN1Primitive();
    }
  }

  public static class LarkyDerInteger extends LarkyASN1Encodable {

    @VisibleForTesting
    LarkyDerInteger(ASN1Integer asn1int) {
      super(asn1int);  //TODO: check to make sure it is a positive value!!
    }

    public static LarkyDerInteger fromStarlarkInt(StarlarkInt n) {
      return fromBigInteger(n.toBigInteger());
    }

    public static LarkyDerInteger fromBigInteger(BigInteger n) {
      return new LarkyDerInteger(new ASN1Integer(n));
    }

    @StarlarkMethod(name = "as_int")
    public StarlarkInt value() {
      return StarlarkInt.of(((ASN1Integer) this.encodable).getPositiveValue());
    }

    @StarlarkMethod(name = "decode", parameters = {
        @Param(name = "barr"),
        @Param(name = "strict", named = true, defaultValue = "False")
    })
    public LarkyDerInteger decode(StarlarkBytes barr, Boolean strict) throws EvalException {
      try {
        encodable = ASN1Primitive.fromByteArray(barr.toByteArray());
      } catch (IOException e) {
        throw Starlark.errorf(e.getMessage());
      }
      return this;
    }

    @Override
    Object toStarlark() {
      return value();
    }
  }

  public static class LarkyOctetString extends LarkyASN1Encodable implements ASN1OctetStringParser {

    public LarkyOctetString(DEROctetString string) {
      super(string);
    }

    @Override
    public InputStream getOctetStream() {
      return ((DEROctetString)this.encodable).getOctetStream();
    }

    @Override
    public ASN1Primitive getLoadedObject() throws IOException {
      return ((DEROctetString)this.encodable).getLoadedObject();
    }

    @Override
    Object toStarlark() {
      return ((DEROctetString)this.encodable).toString();
    }
  }

  public static class LarkyDerBitString extends LarkyASN1Encodable implements ASN1String {


    public LarkyDerBitString(DERBitString derBitString) {
      super(derBitString);
    }

    static public LarkyDerBitString fromStarlarkByteLike(StarlarkBytes b) {
      return new LarkyDerBitString(new DERBitString(b.toByteArray()));
    }

    @Override
    public String getString() {
      return ((DERBitString)this.encodable).getString();
    }

    @Override
    Object toStarlark() throws EvalException {
      return StarlarkBytes.immutableOf(((DERBitString)this.encodable).getOctets());
    }
  }

  public static class LarkyDerUTF8String extends LarkyASN1Encodable implements ASN1String {

    public LarkyDerUTF8String(DERUTF8String derutf8String) {
      super(derutf8String);
    }

    @Override
    public String getString() {
      return ((DERUTF8String)this.encodable).getString();
    }

    @Override
    Object toStarlark() throws EvalException {
      return this.getString();
    }
  }

  public static class LarkyDerNull extends LarkyASN1Encodable {

    LarkyDerNull() {
      super(DERNull.INSTANCE);
    }

    @Override
    Object toStarlark() throws EvalException {
      return DERNull.INSTANCE.toString();
    }
  }

  public static class LarkySetOf extends LarkyASN1Encodable
        implements StarlarkIterable<LarkyASN1Encodable>, StarlarkIndexable {

    LarkySetOf(ASN1Set asn1Set) {
      super(asn1Set);
    }

    @Override
    Object toStarlark() throws EvalException {
      // TODO: implement using hashset or ensure unique list
      List<Object> o = new ArrayList<>();
      for (LarkyASN1Encodable larkyASN1Encodable : this) {
        Object next = larkyASN1Encodable.toStarlark();
        o.add(next);
      }
      return StarlarkList.immutableCopyOf(o);
    }

    @NotNull
    @Override
    public Iterator<LarkyASN1Encodable> iterator() {
      Iterator<ASN1Encodable> x = ((ASN1Set)this.encodable).iterator();
      return new Iterator<LarkyASN1Encodable>() {
        @Override
        public boolean hasNext() {
          return x.hasNext();
        }

        @SneakyThrows
        @Override
        public LarkyASN1Encodable next() {
          return ASN1EncodableFactory.asASN1Encodable(x.next());
        }
      };
    }

    static public LarkySetOf fromList(StarlarkList<?> obj) throws EvalException {
      ASN1Encodable[] encodables = new ASN1Encodable[obj.size()];
      for (int i = 0; i < obj.size(); ++i) {
        encodables[i] = ASN1EncodableFactory.asASN1Encodable(obj.get(i));
      }
      ASN1Set asn1Encodables = new DERSet(encodables);
      return new LarkySetOf(asn1Encodables);
     }

    @Override
    public Object getIndex(StarlarkSemantics semantics, Object key) throws EvalException {
      try {
         return ((ASN1Set)this.encodable).getObjectAt((Integer) key);
       } catch (ArrayIndexOutOfBoundsException e) {
         throw Starlark.errorf(e.getMessage());
       }
     }

    @Override
    public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
      return Iterators.tryFind(((ASN1Set)this.encodable).iterator(), (i) -> i.equals(key)).isPresent();
    }

    @StarlarkMethod(
        name = "append",
        doc = "Adds an item to the end of the list.",
        parameters = {@Param(name = "item", doc = "Item to add at the end.")})
    public void append(Object item) throws EvalException {
      ASN1Encodable atom;
      if (item instanceof String) {
        String s = (String) item;
        LarkyOctetString octetString = new LarkyOctetString(new DEROctetString(s.getBytes()));
        atom = ASN1EncodableFactory.asASN1Encodable(octetString);
      } else {
        atom = ASN1EncodableFactory.asASN1Encodable(item);
      }
      this.encodable = new DERSet(
        Stream.concat(
            Arrays.stream(((ASN1Set)this.encodable).toArray()),
            Arrays.stream(new ASN1Encodable[]{atom})
        ).toArray(ASN1Encodable[]::new)
      );
    }

    @StarlarkMethod(name = "decode", parameters = {
        @Param(name = "barr"),
        @Param(name = "strict", named = true, defaultValue = "False"),
    }, useStarlarkThread = true)
    public StarlarkList<?> decode(StarlarkBytes barr, Boolean strict, StarlarkThread thread) throws EvalException {
      byte[] asbytes = barr.toByteArray();
      try {
        this.encodable = ASN1Set.fromByteArray(asbytes);
      } catch (IOException e) {
        String message = e.getMessage();
        if (message.contains("Extra data detected in stream")) {
          message = "ValueError: Extra data detected in stream";
        } else if (message.contains("end-of-contents marker")) {
          message = "ValueError: end-of-contents marker";
        } else if (message.contains("EOF found when length expected")) {
          message = "ValueError: EOF found when length expected";
        } else if (message.contains("object truncated by")) {
          message = "ValueError: Not all elements are of the same DER type";
        }
        throw Starlark.errorf(message);
      }
      if (this.encodable == null) {
        throw Starlark.errorf("ValueError: Not enough data for DER decoding");
      }
      //System.out.println(this.seq.size());
      //System.out.println(ASN1Dump.dumpAsString(this.seq, true));
      return asList(thread);
    }

    private StarlarkList<?> asList(StarlarkThread thread) {
      List<?> x = Streams.stream(iterator()).map(larkyASN1Encodable -> {
        try {
          return larkyASN1Encodable.toStarlark();
        } catch (EvalException e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList());
      return StarlarkList.copyOf(thread.mutability(), x);
    }

  }

  public static class LarkyDerObjectId extends LarkyASN1Encodable {

    /**
     * Create an OID based on the passed in String.
     *
     * @param identifier a string representation of an OID.
     */
    public LarkyDerObjectId(ASN1ObjectIdentifier identifier) {
      super(identifier);
    }
    @StarlarkMethod(name = "decode", parameters = {
            @Param(name = "barr"),
            @Param(name = "strict", named = true, defaultValue = "False"),
        }, useStarlarkThread = true)
        public LarkyDerObjectId decode(StarlarkBytes barr, Boolean strict, StarlarkThread thread) throws EvalException {
      byte[] asbytes = barr.toByteArray();
          try(ByteArrayInputStream bais = new ByteArrayInputStream(asbytes)) {
            this.encodable = ASN1ObjectIdentifier.fromByteArray(asbytes);
          } catch (IOException e) {
            throw Starlark.errorf(e.getMessage());
          }
//          System.out.println(ASN1Dump.dumpAsString(this.encodable, true));
          return this;
        }

    @Override
    Object toStarlark() throws EvalException {
      return this.encodable.toString();
    }
  }

  public static class LarkyDLTaggedObject extends LarkyASN1Encodable{

    public LarkyDLTaggedObject(DLTaggedObject dlTaggedObject) {
      super(dlTaggedObject);
    }

      @Override
      Object toStarlark() {
        return((DLTaggedObject)this.encodable).toString();
        }
    }


  public static class LarkyASN1Sequence extends LarkyASN1Encodable
      implements StarlarkIterable<LarkyASN1Encodable>, StarlarkIndexable {

    static public LarkyASN1Sequence fromList(StarlarkList<?> obj) throws EvalException {
      ASN1Encodable[] encodables = new ASN1Encodable[obj.size()];
      for (int i = 0; i < obj.size(); ++i) {
        encodables[i] = ASN1EncodableFactory.asASN1Encodable(obj.get(i));
      }
      DERSequence asn1Encodables = new DERSequence(encodables);
      return new LarkyASN1Sequence(asn1Encodables);
    }


    public LarkyASN1Sequence(ASN1Sequence seq) {
      super(seq);
    }

    @Override
    Object toStarlark() throws EvalException {
      List<Object> o = new ArrayList<>();
      for (LarkyASN1Encodable larkyASN1Encodable : this) {
        Object next = larkyASN1Encodable.toStarlark();
        o.add(next);
      }
      return StarlarkList.immutableCopyOf(o);
    }

    @StarlarkMethod(
        name = "append",
        doc = "Adds an item to the end of the list.",
        parameters = {@Param(name = "item", doc = "Item to add at the end.")})
    @SuppressWarnings("unchecked")
    public void append(Object item) throws EvalException {
      ASN1Encodable atom;
      if (item instanceof String) {
        String s = (String) item;
        LarkyOctetString octetString = new LarkyOctetString(new DEROctetString(s.getBytes()));
        atom = ASN1EncodableFactory.asASN1Encodable(octetString);
      } else {
        atom = ASN1EncodableFactory.asASN1Encodable(item);
      }
      this.encodable = new DERSequence(
          Stream.concat(
              Arrays.stream(((ASN1Sequence)this.encodable).toArray()),
              Arrays.stream(new ASN1Encodable[]{atom})
          ).toArray(ASN1Encodable[]::new)
      );
    }

    @StarlarkMethod(name = "decode", parameters = {
        @Param(name = "barr"),
        @Param(name = "strict", named = true, defaultValue = "False"),
    }, useStarlarkThread = true)
    public StarlarkList<?> decode(StarlarkBytes barr, Boolean strict, StarlarkThread thread) throws EvalException {
      byte[] asbytes = barr.toByteArray();
      try(ByteArrayInputStream bais = new ByteArrayInputStream(asbytes)) {
        this.encodable = DERSequence.fromByteArray(asbytes);
      } catch (IOException e) {
        String message = e.getMessage();
        if (message.contains("Extra data detected in stream")) {
          message = "ValueError: Extra data detected in stream";
        } else if (message.contains("end-of-contents marker")) {
          message = "ValueError: end-of-contents marker";
        } else if (message.contains("EOF found when length expected")) {
          message = "ValueError: EOF found when length expected";
        }
        throw Starlark.errorf(message);
      }
      if (this.encodable == null) {
        throw Starlark.errorf("ValueError: Not enough data for DER decoding");
      }
      //System.out.println(this.seq.size());
      //System.out.println(ASN1Dump.dumpAsString(this.seq, true));
      return asList(thread);
    }

    private StarlarkList<?> asList(StarlarkThread thread) {
      List<?> x = Streams.stream(iterator()).map(larkyASN1Encodable -> {
        try {
          return larkyASN1Encodable.toStarlark();
        } catch (EvalException e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList());
      return StarlarkList.copyOf(thread.mutability(), x);
    }

    @NotNull
    @Override
    public Iterator<LarkyASN1Encodable> iterator() {
      Iterator<ASN1Encodable> x = ((ASN1Sequence)this.encodable).iterator();
      return new Iterator<LarkyASN1Encodable>() {
        @Override
        public boolean hasNext() {
          return x.hasNext();
        }

        @SneakyThrows
        @Override
        public LarkyASN1Encodable next() {
          return ASN1EncodableFactory.asASN1Encodable(x.next());
        }
      };
    }

    @Override
    public Object getIndex(StarlarkSemantics semantics, Object key) throws EvalException {
      try {
        return ((ASN1Sequence)this.encodable).getObjectAt((Integer) key);
      } catch (ArrayIndexOutOfBoundsException e) {
        throw Starlark.errorf(e.getMessage());
      }
    }

    @Override
    public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
      return Iterators.tryFind(((ASN1Sequence)this.encodable).iterator(), (i) -> i.equals(key)).isPresent();
    }

  }

}
