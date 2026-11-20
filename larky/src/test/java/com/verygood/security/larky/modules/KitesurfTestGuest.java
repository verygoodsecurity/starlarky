/*
 * Copyright 2026 Very Good Security Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.verygood.security.larky.modules;

import static org.junit.Assume.assumeNotNull;

import com.google.common.hash.Hashing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

record KitesurfTestGuest(byte[] bytes, String sha256) {
  static KitesurfTestGuest load() throws IOException {
    String path = System.getProperty("larky.kitesurf.wasmGuest");
    assumeNotNull(path);
    byte[] bytes = Files.readAllBytes(Path.of(path));
    String sha256 = System.getProperty("larky.kitesurf.wasmGuestSha256");
    if (sha256 == null) {
      sha256 = Hashing.sha256().hashBytes(bytes).toString();
    }
    return new KitesurfTestGuest(bytes, sha256);
  }
}
