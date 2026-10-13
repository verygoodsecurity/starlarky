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

package com.verygood.security.larky.wasm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Mutual TLS credentials loaded from host-mounted files, never from request bytes. */
record WorkerTlsConfig(SSLContext context) {
  private static final String PREFIX = "larky.wasm.worker.tls.";

  static WorkerTlsConfig configured() {
    char[] keyPassword = null;
    char[] trustPassword = null;
    try {
      keyPassword = password("keyStorePasswordFile");
      trustPassword = password("trustStorePasswordFile");
      KeyStore identity = load("keyStore", keyPassword);
      KeyStore trusted = load("trustStore", trustPassword);
      KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keys.init(identity, keyPassword);
      TrustManagerFactory trusts = TrustManagerFactory.getInstance(
          TrustManagerFactory.getDefaultAlgorithm());
      trusts.init(trusted);
      SSLContext context = SSLContext.getInstance("TLSv1.3");
      context.init(keys.getKeyManagers(), trusts.getTrustManagers(), null);
      return new WorkerTlsConfig(context);
    } catch (IOException | GeneralSecurityException e) {
      throw new IllegalArgumentException("invalid WASM worker TLS configuration", e);
    } finally {
      if (keyPassword != null) Arrays.fill(keyPassword, '\0');
      if (trustPassword != null) Arrays.fill(trustPassword, '\0');
    }
  }

  private static KeyStore load(String suffix, char[] password)
      throws IOException, GeneralSecurityException {
    KeyStore store = KeyStore.getInstance("PKCS12");
    try (var input = Files.newInputStream(configuredPath(suffix))) {
      store.load(input, password);
    }
    return store;
  }

  private static char[] password(String suffix) throws IOException {
    String value = Files.readString(configuredPath(suffix), StandardCharsets.UTF_8);
    if (value.endsWith("\n")) value = value.substring(0, value.length() - 1);
    if (value.endsWith("\r")) value = value.substring(0, value.length() - 1);
    if (value.isEmpty() || value.length() > 1024) {
      throw new IOException("invalid worker TLS password file");
    }
    return value.toCharArray();
  }

  private static Path configuredPath(String suffix) {
    String name = System.getProperty(PREFIX + suffix);
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException(PREFIX + suffix + " is required");
    }
    Path path = Path.of(name);
    if (!path.isAbsolute() || !Files.isRegularFile(path)) {
      throw new IllegalArgumentException(PREFIX + suffix + " must be an absolute file");
    }
    return path;
  }
}
