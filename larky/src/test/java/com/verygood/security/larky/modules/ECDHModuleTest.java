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

package com.verygood.security.larky.modules;

import static com.google.common.truth.Truth.assertThat;

import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.ECGenParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

/** {@link ECDHModule#explain}: the cause it reports when no provider accepted the private key. */
public class ECDHModuleTest {

  private static final InvalidKeyException NOT_AN_EC_KEY = new InvalidKeyException("Not an EC key: ECDSA");

  private static PrivateKey key(java.security.Provider provider) throws Exception {
    KeyPairGenerator generator =
        provider == null ? KeyPairGenerator.getInstance("EC") : KeyPairGenerator.getInstance("EC", provider);
    generator.initialize(new ECGenParameterSpec("secp256r1"));
    return generator.generateKeyPair().getPrivate();
  }

  @Test
  public void aBouncyCastleKeyOnAJvmThatRefusesBouncyCastleNamesTheCause() throws Exception {
    PrivateKey bouncyCastleKey = key(new BouncyCastleProvider());
    assertThat(bouncyCastleKey.getClass().getName()).startsWith("org.bouncycastle.");

    // what JDK 21 throws from KeyAgreement.getInstance("ECDH", "BC") for a provider it refuses
    InvalidKeyException explained = ECDHModule.explain(bouncyCastleKey, NOT_AN_EC_KEY, () -> {
      throw new java.security.NoSuchProviderException("JCE cannot authenticate the provider BC");
    });

    assertThat(explained).isNotSameInstanceAs(NOT_AN_EC_KEY);
    assertThat(explained).hasMessageThat().contains("does not accept the BouncyCastle provider");
    assertThat(explained).hasMessageThat().contains("JCE cannot authenticate the provider BC");
    assertThat(explained).hasMessageThat().contains("Not an EC key: ECDSA");
    assertThat(explained).hasCauseThat().isInstanceOf(java.security.NoSuchProviderException.class);
    assertThat(explained.getSuppressed()).asList().containsExactly(NOT_AN_EC_KEY);
  }

  @Test
  public void aBouncyCastleKeyOnAJvmThatAcceptsBouncyCastleKeepsTheFailure() throws Exception {
    assertThat(ECDHModule.explain(key(new BouncyCastleProvider()), NOT_AN_EC_KEY, () -> {}))
        .isSameInstanceAs(NOT_AN_EC_KEY);
  }

  @Test
  public void aJdkKeyKeepsTheFailureWithoutAskingBouncyCastle() throws Exception {
    PrivateKey jdkKey = key(null);
    assertThat(jdkKey.getClass().getName()).doesNotContain("bouncycastle");
    assertThat(ECDHModule.explain(jdkKey, NOT_AN_EC_KEY, () -> {
      throw new AssertionError("BouncyCastle is not asked about a key it did not make");
    })).isSameInstanceAs(NOT_AN_EC_KEY);
  }

  @Test
  public void aSecurityExceptionFromTheProbeNamesTheCauseToo() throws Exception {
    InvalidKeyException explained = ECDHModule.explain(key(new BouncyCastleProvider()), NOT_AN_EC_KEY, () -> {
      throw new SecurityException("JCE cannot authenticate the provider BC");
    });
    assertThat(explained).hasMessageThat().contains("does not accept the BouncyCastle provider");
    assertThat(explained).hasCauseThat().isInstanceOf(SecurityException.class);
  }

  @Test
  public void noKeyKeepsTheFailure() {
    // an unknown key type, or a PKCS12 with no key entry: the failure is the JCA's, as before
    InvalidKeyException noProvider = new InvalidKeyException("No installed provider supports this key: (null)");
    assertThat(ECDHModule.explain(null, noProvider, () -> {
      throw new AssertionError("BouncyCastle is not asked when there is no key");
    })).isSameInstanceAs(noProvider);
  }

  @Test
  public void theRealProbeSucceedsWhereBouncyCastleIsAccepted() throws Exception {
    // registered as CryptoModule registers it; this JVM accepts it, or the suite's crypto tests would fail
    if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      java.security.Security.addProvider(new BouncyCastleProvider());
    }
    ECDHModule.bouncyCastleKeyAgreement();
    assertThat(ECDHModule.explain(key(new BouncyCastleProvider()), NOT_AN_EC_KEY, ECDHModule::bouncyCastleKeyAgreement))
        .isSameInstanceAs(NOT_AN_EC_KEY);
  }
}
