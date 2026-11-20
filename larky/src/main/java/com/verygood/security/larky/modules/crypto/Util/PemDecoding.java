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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.DecoderException;

/**
 * Decodes PEM data that a script supplies without handing it to {@link java.util.Base64}'s basic decoder.
 *
 * <p>On x86-64 CPUs with AVX-512 VBMI, HotSpot's intrinsic for that decoder writes past its output buffer for some
 * malformed inputs (CVE-2026-114938). The JDK's X.509 certificate factory uses the same decoder for PEM input, so
 * certificates are only given to it as DER, which it reads without decoding Base64.
 */
public final class PemDecoding {

  private static final byte DER_SEQUENCE = 0x30;

  private PemDecoding() {
  }

  /**
   * Decodes Base64 text with no whitespace, as {@code java.util.Base64.getDecoder()} does: padding at the end may be
   * left out.
   *
   * @throws IllegalArgumentException if {@code base64} is not valid Base64
   */
  public static byte[] decodeBase64(String base64) {
    final StringBuilder padded = new StringBuilder(base64);
    while (padded.length() % 4 != 0) {
      padded.append('=');
    }
    try {
      return Base64.decode(padded.toString());
    } catch (DecoderException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  /**
   * Parses one DER-encoded X.509 certificate.
   *
   * @throws CertificateException if {@code der} is not a DER certificate (PEM text included)
   */
  public static X509Certificate certificateFromDer(byte[] der) throws CertificateException {
    if (der.length == 0 || der[0] != DER_SEQUENCE) {
      throw new CertificateException("certificate is not DER encoded");
    }
    return (X509Certificate) CertificateFactory.getInstance("X.509")
        .generateCertificate(new ByteArrayInputStream(der));
  }
}
