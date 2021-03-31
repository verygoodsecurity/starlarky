package com.verygood.security.larky.modules.crypto;

import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.encoders.DecoderException;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;

public class CryptoPublicKeyModule implements StarlarkValue {

  public static final CryptoPublicKeyModule INSTANCE = new CryptoPublicKeyModule();

  public void RSA_generate(int bits, BigInteger e) {
    e = (e == null) ? BigInteger.valueOf(65537) : e;
    if(bits != 1024 || bits != 2048 || bits != 3072 || bits != 4096) {
      // do some shit
    }
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
    RSAKeyPairGenerator rsaKeyPairGenerator = new RSAKeyPairGenerator();
    RSAKeyGenerationParameters rsaKeyGenerationParameters = new RSAKeyGenerationParameters(e, secureRandom, bits, 100);
    rsaKeyPairGenerator.init(rsaKeyGenerationParameters);
    AsymmetricCipherKeyPair asymmetricCipherKeyPair = rsaKeyPairGenerator.generateKeyPair();
  }
  public void RSA_construct(BigInteger n, BigInteger e, BigInteger d, BigInteger p, BigInteger q, BigInteger qInv) {
    BigInteger pSub1 = p.subtract(BigInteger.ONE);
    BigInteger qSub1 = q.subtract(BigInteger.ONE);
    BigInteger dP = d.remainder(pSub1);
    BigInteger dQ = d.remainder(qSub1);
    //qInv = BigIntegers.modOddInverse(p, q);
    new AsymmetricCipherKeyPair(
                   new RSAKeyParameters(false, n, e),
                   new RSAPrivateCrtKeyParameters(n, e, d, p, q, dP, dQ, qInv));
  }

  public RSAPrivateKey RSA_import_key(String externKey, String passPhrase) throws SignatureException {
    StringReader stringReader = new StringReader(externKey);
    try(PEMParser pemParser = new PEMParser(stringReader)) {
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        Object pemKeyPairObject = pemParser.readObject();
        if (pemKeyPairObject instanceof SubjectPublicKeyInfo) {
            throw new SignatureException("Input is an RSA Public Key, but private key is expected");
        }
        PEMKeyPair pemKeyPair = (PEMKeyPair) pemKeyPairObject;
        KeyPair keyPair = converter.getKeyPair(pemKeyPair);
        return (RSAPrivateKey) keyPair.getPrivate();
    } catch (IOException e) {
        throw new SignatureException("Unable to parse RSA private key", e);
    } catch (IllegalArgumentException | NullPointerException | DecoderException e) {
        throw new SignatureException("Unable to parse RSA private key. Input is malformed", e);
    }
  }
}