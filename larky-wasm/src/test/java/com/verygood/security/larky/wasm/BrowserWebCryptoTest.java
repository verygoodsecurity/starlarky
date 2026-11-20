/*
 * Copyright 2026 Very Good Security Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.verygood.security.larky.wasm;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import org.junit.Test;
import run.endive.wabt.Wat2Wasm;

public class BrowserWebCryptoTest {
  private static final int OUTPUT = 1024;

  @Test
  public void aesGcmMatchesKnownAnswerThroughBrowserImport() throws Exception {
    var result = run(aesGuest(0, 12, OUTPUT, 32));
    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(HexFormat.of().formatHex(result.stdout())).isEqualTo(
        "0388dace60b6a392f328c2b971b2fe78ab6e47d42cec13bdf53a67b21257bddf");
  }

  @Test
  public void aesGcmRejectsInvalidRangesAndInsufficientCapacity() throws Exception {
    for (byte[] guest : new byte[][] {
        aesGuest(-1, 12, OUTPUT, 32), aesGuest(0, -1, OUTPUT, 32),
        aesGuest(0, 12, 65520, 32), aesGuest(0, 12, OUTPUT, 31)}) {
      var result = run(guest);
      assertThat(result.exitCode()).isEqualTo(1);
      assertThat(result.stdout()).isEmpty();
    }
  }

  @Test
  public void rsaOaepUsesSha256ForTheMessageAndMgf1() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var keyPair = generator.generateKeyPair();
    var publicKey = (RSAPublicKey) keyPair.getPublic();
    byte[] modulus = publicKey.getModulus().toByteArray();
    if (modulus[0] == 0) {
      modulus = Arrays.copyOfRange(modulus, 1, modulus.length);
    }
    byte[] exponent = publicKey.getPublicExponent().toByteArray();
    byte[] plaintext = "browser payload".getBytes(StandardCharsets.UTF_8);
    var guest = encryptGuest("rsa_oaep_256_encrypt", 8,
        data(0, modulus) + data(512, exponent) + data(768, plaintext),
        args(0, modulus.length, 512, exponent.length, 768, plaintext.length, OUTPUT, 256));

    var result = run(guest);
    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.stdout()).hasLength(256);
    Cipher decrypt = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
    decrypt.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), new OAEPParameterSpec(
        "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
    assertThat(decrypt.doFinal(result.stdout())).isEqualTo(plaintext);
  }

  @Test
  public void browserClockReturnsEpochMilliseconds() throws Exception {
    long before = System.currentTimeMillis();
    var result = run(Wat2Wasm.parse("""
        (module
          (import "vgs_browser_v1" "wall_clock_ms" (func $clock (param i32) (result i32)))
          (import "wasi_snapshot_preview1" "fd_write"
            (func $write (param i32 i32 i32 i32) (result i32)))
          (memory (export "memory") 1)
          (func (export "_initialize"))
          (func (export "vgs_browser_step")
            (if (i32.ne (call $clock (i32.const 65529)) (i32.const -1)) (then unreachable))
            (if (call $clock (i32.const 16)) (then unreachable))
            (i32.store (i32.const 0) (i32.const 16))
            (i32.store (i32.const 4) (i32.const 8))
            (drop (call $write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 8)))))
        """));
    long after = System.currentTimeMillis();
    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.stdout()).hasLength(8);
    long guestTime = ByteBuffer.wrap(result.stdout()).order(ByteOrder.LITTLE_ENDIAN).getLong();
    assertThat(guestTime).isAtLeast(before);
    assertThat(guestTime).isAtMost(after);
  }

  @Test
  public void ordinaryWasmCannotImportBrowserCrypto() {
    var guest = new EndiveWasmRuntime();
    var exception = assertThrows(WasmRuntime.WasmException.class,
        () -> guest.compile(aesGuest(0, 12, OUTPUT, 32)));
    assertThat(exception).hasMessageThat().contains("vgs_browser_v1.aes_gcm_encrypt");
  }

  private static WasmRuntime.Result run(byte[] wasm) throws Exception {
    var session = new BrowserWasmRuntime.Session(Set.of(), 1, (uri, deadline, maxBytes) -> {
      throw new AssertionError("Browser primitives must not fetch");
    });
    var limits = new WasmRuntime.Limits(1 << 20, System.currentTimeMillis() + 10_000, 1024, null);
    try (var guest = BrowserWasmRuntime.compile(wasm).open(limits, session)) {
      return guest.step(new byte[0]);
    }
  }

  private static byte[] aesGuest(int keyAddress, int ivLength, int outputAddress, int capacity) {
    return encryptGuest("aes_gcm_encrypt", 11, "",
        args(keyAddress, 16, 32, ivLength, 0, 0, 128, 64, 16, outputAddress, capacity));
  }

  private static byte[] encryptGuest(String name, int parameters, String data, String args) {
    return Wat2Wasm.parse("""
        (module
          (import "vgs_browser_v1" "%s" (func $encrypt (param %s) (result i32)))
          (import "wasi_snapshot_preview1" "fd_write"
            (func $write (param i32 i32 i32 i32) (result i32)))
          (import "wasi_snapshot_preview1" "proc_exit" (func $exit (param i32)))
          (memory (export "memory") 1)
          %s
          (func (export "_initialize"))
          (func (export "vgs_browser_step") (local $length i32)
            (local.set $length (call $encrypt %s))
            (if (i32.lt_s (local.get $length) (i32.const 0)) (then (call $exit (i32.const 1))))
            (i32.store (i32.const 0) (i32.const 1024))
            (i32.store (i32.const 4) (local.get $length))
            (drop (call $write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 8)))))
        """.formatted(name, "i32 ".repeat(parameters).stripTrailing(), data, args));
  }

  private static String args(int... values) {
    var text = new StringBuilder();
    for (int value : values) {
      text.append("(i32.const ").append(value).append(") ");
    }
    return text.toString();
  }

  private static String data(int address, byte[] bytes) {
    return "(data (i32.const %d) \"%s\") ".formatted(address,
        HexFormat.of().withPrefix("\\").formatHex(bytes));
  }
}
