package com.verygood.security.larky.modules.crypto;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.AESDecrypter;
import com.nimbusds.jose.crypto.AESEncrypter;
import com.nimbusds.jose.util.Base64URL;
import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import lombok.SneakyThrows;

public class CryptoJWEModule implements StarlarkValue {

  public static final CryptoJWEModule INSTANCE = new CryptoJWEModule();

  @SneakyThrows
  @StarlarkMethod(name = "encrypt", parameters = {
      @Param(name = "payload", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
      @Param(name = "key", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
  }, useStarlarkThread = true)
  public LarkyByteLike encrypt(LarkyByteLike payload, LarkyByteLike key, StarlarkThread thread) throws EvalException {
    JWEHeader header = new JWEHeader(JWEAlgorithm.A256GCMKW, EncryptionMethod.A256GCM);

    AESEncrypter aesEncrypter = new AESEncrypter(key.getBytes());
    Payload payloadObj = new Payload(payload.getBytes());
    JWEObject jweObject = new JWEObject(header, payloadObj);
    jweObject.encrypt(aesEncrypter);

    return LarkyByte.builder(thread).setSequence(jweObject.serialize()).build();
  }

  @SneakyThrows
  @StarlarkMethod(name = "decrypt", parameters = {
      @Param(name = "data", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
      @Param(name = "key", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
  }, useStarlarkThread = true)
  public Dict<String, LarkyByteLike> decrypt(LarkyByteLike data, LarkyByteLike key, StarlarkThread thread) {
    String dataStr = new String(data.getBytes());
    String[] dataArr = dataStr.split("\\.", 5);

    AESDecrypter aesDecrypter = new AESDecrypter(key.getBytes());
    JWEObject jweObject = new JWEObject(new Base64URL(dataArr[0]),
        new Base64URL(dataArr[1]), new Base64URL(dataArr[2]),
        new Base64URL(dataArr[3]), new Base64URL(dataArr[4]));

    jweObject.decrypt(aesDecrypter);

    return Dict.<String, LarkyByteLike>builder()
        .put("header", LarkyByte.builder(thread).setSequence(jweObject.getHeader().toString()).build())
        .put("payload", LarkyByte.builder(thread).setSequence(jweObject.getPayload().toString()).build())
        .build(thread.mutability());
  }
}
