/*
 * Copyright 2026 Very Good Security Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.verygood.security.larky.wasm;

import static com.verygood.security.larky.wasm.BrowserWasmRuntime.u32;
import static com.verygood.security.larky.wasm.BrowserWasmRuntime.validRange;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

/** The bounded encryption primitives used by the browser guest's WebCrypto implementation. */
final class BrowserWebCrypto {
  static final String AES_GCM_ENCRYPT = "aes_gcm_encrypt";
  static final String RSA_OAEP_256_ENCRYPT = "rsa_oaep_256_encrypt";

  private static final int ERROR = -1;
  private static final int MAX_PAYLOAD_BYTES = 1 << 20;
  private static final int MAX_IV_BYTES = 1024;
  private static final int MIN_RSA_BITS = 2048;
  private static final int MAX_RSA_BITS = 4096;
  private static final int OAEP_256_OVERHEAD_BYTES = 2 * 32 + 2;
  private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
      "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

  private BrowserWebCrypto() {}

  static int aesGcmEncrypt(long[] args, WasiHost.GuestMemory memory) {
    Range key = Range.at(args, 0);
    Range iv = Range.at(args, 2);
    Range aad = Range.at(args, 4);
    long tagBits = u32(args[6]);
    Range plaintext = Range.at(args, 7);
    Range output = Range.at(args, 9);
    long required = plaintext.length() + tagBits / 8;
    if ((key.length() != 16 && key.length() != 24 && key.length() != 32)
        || iv.length() == 0 || tagBits < 96 || tagBits > 128 || tagBits % 8 != 0
        || !key.fits(memory, 32) || !iv.fits(memory, MAX_IV_BYTES)
        || !aad.fits(memory, MAX_PAYLOAD_BYTES) || !plaintext.fits(memory, MAX_PAYLOAD_BYTES)
        || !output.canWrite(memory, required)) {
      return ERROR;
    }

    byte[] keyBytes = key.read(memory);
    byte[] data = plaintext.read(memory);
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
          new GCMParameterSpec((int) tagBits, iv.read(memory)));
      cipher.updateAAD(aad.read(memory));
      return output.write(memory, cipher.doFinal(data));
    } catch (GeneralSecurityException | IllegalArgumentException failure) {
      return ERROR;
    } finally {
      Arrays.fill(keyBytes, (byte) 0);
      Arrays.fill(data, (byte) 0);
    }
  }

  static int rsaOaep256Encrypt(long[] args, WasiHost.GuestMemory memory) {
    Range modulus = Range.at(args, 0);
    Range exponent = Range.at(args, 2);
    Range plaintext = Range.at(args, 4);
    Range output = Range.at(args, 6);
    if (modulus.length() < MIN_RSA_BITS / 8 || exponent.length() == 0
        || !modulus.fits(memory, MAX_RSA_BITS / 8) || !exponent.fits(memory, Long.BYTES)
        || !plaintext.fits(memory, modulus.length() - OAEP_256_OVERHEAD_BYTES)
        || !output.canWrite(memory, modulus.length())) {
      return ERROR;
    }

    BigInteger n = new BigInteger(1, modulus.read(memory));
    BigInteger e = new BigInteger(1, exponent.read(memory));
    int modulusBytes = (n.bitLength() + 7) / 8;
    if (n.bitLength() < MIN_RSA_BITS || !n.testBit(0) || !e.testBit(0)
        || e.compareTo(BigInteger.valueOf(3)) < 0
        || plaintext.length() > modulusBytes - OAEP_256_OVERHEAD_BYTES) {
      return ERROR;
    }

    byte[] data = plaintext.read(memory);
    try {
      var key = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
      Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
      cipher.init(Cipher.ENCRYPT_MODE, key, OAEP_SHA256);
      return output.write(memory, cipher.doFinal(data));
    } catch (GeneralSecurityException | IllegalArgumentException failure) {
      return ERROR;
    } finally {
      Arrays.fill(data, (byte) 0);
    }
  }

  private record Range(long start, long length) {
    static Range at(long[] args, int index) {
      return new Range(u32(args[index]), u32(args[index + 1]));
    }

    boolean fits(WasiHost.GuestMemory memory, long maximum) {
      return length <= maximum && validRange(memory, start, length);
    }

    boolean canWrite(WasiHost.GuestMemory memory, long required) {
      return length >= required && validRange(memory, start, length);
    }

    byte[] read(WasiHost.GuestMemory memory) {
      byte[] bytes = new byte[(int) length];
      if (bytes.length != 0) {
        memory.read(start, bytes, 0, bytes.length);
      }
      return bytes;
    }

    int write(WasiHost.GuestMemory memory, byte[] bytes) {
      if (bytes.length > length) {
        return ERROR;
      }
      memory.write(start, bytes, 0, bytes.length);
      return bytes.length;
    }
  }
}
