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
import static org.junit.Assert.assertThrows;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import javax.crypto.KeyAgreement;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

/**
 * {@link ECDHModule#agreementWithJdkKey}: what happens to a key that no provider accepted. The JVM that refuses
 * BouncyCastle is simulated by the probe; this JVM accepts it.
 */
public class ECDHModuleTest {

  private static final Provider BC = new BouncyCastleProvider();
  private static final InvalidKeyException NOT_AN_EC_KEY = new InvalidKeyException("Not an EC key: ECDSA");
  private static final ECDHModule.ProviderProbe REFUSED = () -> {
    // what JDK 21 throws from KeyAgreement.getInstance("ECDH", "BC") for a provider it refuses
    throw new NoSuchProviderException("JCE cannot authenticate the provider BC");
  };

  private static KeyPair keys(Provider provider, String curve) throws Exception {
    KeyPairGenerator generator =
        provider == null ? KeyPairGenerator.getInstance("EC") : KeyPairGenerator.getInstance("EC", provider);
    generator.initialize(new ECGenParameterSpec(curve));
    return generator.generateKeyPair();
  }

  @Test
  public void aBouncyCastleKeyOnAJvmThatRefusesBouncyCastleIsUsedThroughTheJdk() throws Exception {
    for (String curve : new String[] {"secp256r1", "secp384r1", "secp521r1"}) {
      KeyPair mine = keys(BC, curve);
      KeyPair theirs = keys(null, curve);
      assertThat(mine.getPrivate().getClass().getName()).startsWith("org.bouncycastle.");

      KeyAgreement viaJdk = ECDHModule.agreementWithJdkKey(mine.getPrivate(), NOT_AN_EC_KEY, REFUSED);
      viaJdk.doPhase(theirs.getPublic(), true);

      assertThat(viaJdk.getProvider().getName()).isNotEqualTo(BouncyCastleProvider.PROVIDER_NAME);
      // the same secret BouncyCastle computes with the key as it was
      KeyAgreement viaBouncyCastle = KeyAgreement.getInstance("ECDH", BC);
      viaBouncyCastle.init(mine.getPrivate());
      viaBouncyCastle.doPhase(theirs.getPublic(), true);
      assertThat(viaJdk.generateSecret()).isEqualTo(viaBouncyCastle.generateSecret());
    }
  }

  @Test
  public void theFallbackDoesNotUseBouncyCastleEvenWhenItIsTheFirstProvider() throws Exception {
    Provider registered = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    Security.insertProviderAt(BC, 1);
    try {
      KeyAgreement viaJdk = ECDHModule.agreementWithJdkKey(keys(BC, "secp256r1").getPrivate(), NOT_AN_EC_KEY, REFUSED);
      assertThat(viaJdk.getProvider().getName()).isNotEqualTo(BouncyCastleProvider.PROVIDER_NAME);
    } finally {
      // as it was: after the JDK's providers, where CryptoModule adds it
      Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
      Security.addProvider(registered != null ? registered : BC);
    }
  }

  @Test
  public void aSecurityExceptionFromTheProbeIsARefusalToo() throws Exception {
    KeyAgreement viaJdk = ECDHModule.agreementWithJdkKey(keys(BC, "secp384r1").getPrivate(), NOT_AN_EC_KEY, () -> {
      throw new SecurityException("JCE cannot authenticate the provider BC");
    });
    assertThat(viaJdk.getProvider().getName()).isNotEqualTo(BouncyCastleProvider.PROVIDER_NAME);
  }

  @Test
  public void aCurveTheJdkDoesNotSupportSaysSoAndKeepsTheRefusalAsTheCause() throws Exception {
    // BouncyCastle has secp256k1; the JDK's provider does not (removed in JDK 16)
    KeyPair mine = keys(BC, "secp256k1");

    InvalidKeyException explained = assertThrows(InvalidKeyException.class,
        () -> ECDHModule.agreementWithJdkKey(mine.getPrivate(), NOT_AN_EC_KEY, REFUSED));

    // to the script's author: what they can do
    assertThat(explained).hasMessageThat().contains("is not one this runtime supports");
    assertThat(explained).hasMessageThat().contains("P-256, P-384 or P-521");
    assertThat(explained).hasMessageThat().doesNotContain("BouncyCastle");
    // to the host, in the cause: what the JVM refused and what to run with
    assertThat(explained).hasCauseThat().hasMessageThat().contains("JCE cannot authenticate the provider BC");
    assertThat(explained).hasCauseThat().hasMessageThat().contains("BouncyCastle's own signed jars");
    assertThat(explained).hasCauseThat().hasCauseThat().isInstanceOf(NoSuchProviderException.class);
    assertThat(explained.getSuppressed()).asList().contains(NOT_AN_EC_KEY);
  }

  @Test
  public void aBouncyCastleKeyOnAJvmThatAcceptsBouncyCastleKeepsTheFailure() throws Exception {
    InvalidKeyException thrown = assertThrows(InvalidKeyException.class,
        () -> ECDHModule.agreementWithJdkKey(keys(BC, "secp256r1").getPrivate(), NOT_AN_EC_KEY, () -> {}));
    assertThat(thrown).isSameInstanceAs(NOT_AN_EC_KEY);
  }

  @Test
  public void aJdkKeyKeepsTheFailureWithoutAskingBouncyCastle() throws Exception {
    KeyPair jdk = keys(null, "secp256r1");
    assertThat(jdk.getPrivate().getClass().getName()).doesNotContain("bouncycastle");
    InvalidKeyException thrown = assertThrows(InvalidKeyException.class,
        () -> ECDHModule.agreementWithJdkKey(jdk.getPrivate(), NOT_AN_EC_KEY, () -> {
          throw new AssertionError("BouncyCastle is not asked about a key it did not make");
        }));
    assertThat(thrown).isSameInstanceAs(NOT_AN_EC_KEY);
  }

  @Test
  public void noKeyKeepsTheFailure() {
    // an unknown key type, or a PKCS12 with no key entry: the failure is the JCA's, as before
    InvalidKeyException noProvider = new InvalidKeyException("No installed provider supports this key: (null)");
    InvalidKeyException thrown = assertThrows(InvalidKeyException.class,
        () -> ECDHModule.agreementWithJdkKey(null, noProvider, () -> {
          throw new AssertionError("BouncyCastle is not asked when there is no key");
        }));
    assertThat(thrown).isSameInstanceAs(noProvider);
  }

  @Test
  public void theRealProbeSucceedsWhereBouncyCastleIsAccepted() throws Exception {
    // registered as CryptoModule registers it; this JVM accepts it, or the suite's crypto tests would fail
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
    ECDHModule.bouncyCastleKeyAgreement();
  }
}
