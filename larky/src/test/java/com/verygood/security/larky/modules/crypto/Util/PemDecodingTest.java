/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.verygood.security.larky.modules.crypto.Util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.modules.openssl.SSLUtils;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

public class PemDecodingTest {

  private static byte[] der;

  @BeforeClass
  public static void makeCertificate() throws Exception {
    final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    final KeyPair keys = generator.generateKeyPair();
    final X500Name name = new X500Name("CN=pem-decoding-test");
    final Date now = new Date();
    der = new JcaX509CertificateConverter().getCertificate(
        new JcaX509v3CertificateBuilder(name, BigInteger.ONE, now, new Date(now.getTime() + 86_400_000L), name,
            keys.getPublic())
            .build(new JcaContentSignerBuilder("SHA256withRSA").build(keys.getPrivate())))
        .getEncoded();
  }

  private static String pem(byte[] body) {
    return "-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder().encodeToString(body)
        + "\n-----END CERTIFICATE-----\n";
  }

  @Test
  public void decodesBase64AsTheJdkBasicDecoderDoes() {
    for (String text : List.of("", "TWFu", "TWE=", "TWE", "TQ==", "TQ", "TWFuTWFu")) {
      assertArrayEquals(text, Base64.getDecoder().decode(text), PemDecoding.decodeBase64(text));
    }
  }

  @Test
  public void refusesInvalidBase64() {
    // the last two have the shape that makes HotSpot's AVX-512 decode intrinsic write past its buffer
    for (String text : List.of("T", "TW!u", "TWFu\nTWE=", "A".repeat(64) + "=", "A".repeat(128) + "=")) {
      assertThrows(text, IllegalArgumentException.class, () -> PemDecoding.decodeBase64(text));
    }
  }

  @Test
  public void parsesADerCertificate() throws Exception {
    assertArrayEquals(der, PemDecoding.certificateFromDer(der).getEncoded());
  }

  @Test
  public void refusesPemTextSoTheJdkNeverDecodesIt() {
    assertThrows(CertificateException.class,
        () -> PemDecoding.certificateFromDer(pem(der).getBytes(StandardCharsets.US_ASCII)));
    assertThrows(CertificateException.class, () -> PemDecoding.certificateFromDer(new byte[0]));
  }

  @Test
  public void certificateChainsAreReadAsDer() throws Exception {
    final List<X509Certificate> chain = SSLUtils.readCertificateChain(pem(der) + pem(der));
    assertEquals(2, chain.size());
    assertArrayEquals(der, chain.get(0).getEncoded());

    // a PEM block whose body decodes to another PEM block
    final String nested = pem(pem(der).getBytes(StandardCharsets.US_ASCII));
    assertThrows(CertificateException.class, () -> SSLUtils.readCertificateChain(nested));
  }
}
