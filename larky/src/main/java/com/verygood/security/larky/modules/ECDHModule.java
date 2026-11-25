package com.verygood.security.larky.modules;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import com.google.common.flogger.FluentLogger;
import java.util.concurrent.TimeUnit;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.stream.Collectors;
import javax.crypto.KeyAgreement;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

@StarlarkBuiltin(
        name = "ECDH",
        category = "BUILTIN",
        doc = "A backend for ECDH operations."
)
@com.google.errorprone.annotations.Immutable
public final class ECDHModule implements StarlarkValue {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private static final String ECC_ALGORITHM = "EC";

    public static final ECDHModule INSTANCE = new ECDHModule();

    @StarlarkMethod(name="key_exchange",
    doc = "Load Keys and Exchange values",
        parameters = {
            @Param(name="privKey", allowedTypes = {
                    @ParamType(type = StarlarkBytes.class),
                    @ParamType(type = String.class)
            }),
            @Param(name="privKeyType", allowedTypes = {
                    @ParamType(type = String.class),
                    @ParamType(type = StarlarkBytes.class)
            }),
            @Param(name="pubKey", allowedTypes = {
                    @ParamType(type = StarlarkBytes.class)
            }),
            @Param(name = "pubKeyType", allowedTypes = {
                    @ParamType(type = String.class),
                    @ParamType(type = StarlarkBytes.class)
            }),
            @Param(name = "privPass", allowedTypes = {
                    @ParamType(type = String.class),
            })
        }, useStarlarkThread = true)
    public static StarlarkBytes key_exchange(Object privKey,
                                Object privKeyType,
                                StarlarkBytes pubKey,
                                Object pubKeyType,
                                String privPass,
                                StarlarkThread thread) throws Exception {
        String privType = privKeyType.toString();
        String pubType = pubKeyType.toString();
        StarlarkBytes prkBytes = null;
        String prkStr = null;
        if (privKey instanceof StarlarkBytes){
            prkBytes = (StarlarkBytes) privKey;
        } else if (privKey instanceof String){
            prkStr = (String) privKey;
        }
        PrivateKey privateKey = null;
        PublicKey publicKey = null;

        switch (privType){
            case "PKCS8":
                privateKey = loadPrivateKeyPKCS8(prkBytes);
                break;
            case "PEM":
                privateKey = loadPrivateKeySEC1(prkStr);
                break;
            case "PKCS12":
                privateKey = loadPrivateKeyPKCS12(prkBytes, privPass.toCharArray());
                break;
        }

        switch (pubType){
            case "X509":
                publicKey = loadPublicKeyX509(pubKey);
        }

        byte[] bytes = ellipticCurveDHExchange(privateKey, publicKey);
        return StarlarkBytes.of(thread.mutability(), bytes);
    }

    private static byte[] ellipticCurveDHExchange(PrivateKey privateKey, PublicKey publicKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        try {
            // The provider is the first one that accepts the key: the JDK's for a key the JDK made (PKCS8,
            // PKCS12), BouncyCastle's for a key BouncyCastle made (a SEC1 PEM).
            ka.init(privateKey);
        } catch (InvalidKeyException e) {
            ka = agreementWithJdkKey(privateKey, e, ECDHModule::bouncyCastleKeyAgreement);
        }
        ka.doPhase(publicKey, true);
        return ka.generateSecret();
    }

    /** Asks BouncyCastle for a key agreement, which is where a JVM refuses a provider it does not accept. */
    interface ProviderProbe {
        void run() throws GeneralSecurityException;
    }

    static void bouncyCastleKeyAgreement() throws GeneralSecurityException {
        KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
    }

    /**
     * The key agreement for a key that no provider accepted, when that is because the JVM refuses BouncyCastle.
     *
     * <p>A SEC1 PEM key is a BouncyCastle key, and BouncyCastle is the provider that takes it. Oracle's JDK refuses
     * {@code javax.crypto} services from a provider whose jar is not signed, as BouncyCastle's classes are when
     * they are repackaged into another jar ("JCE cannot authenticate the provider BC"); the only error is then the
     * JDK provider's "Not an EC key: ECDSA". The key itself is fine: re-read from its PKCS8 encoding by the JDK's key
     * factory, it is a key the JDK's provider accepts, and the secret is the same. So that is done here, and a
     * warning tells the host to run with BouncyCastle's signed jars.
     *
     * <p>Any other failure is thrown as it is: no key, a key that is not BouncyCastle's, or a JVM that accepts
     * BouncyCastle (the key was refused for another reason). If the JDK cannot take the key either (a curve it
     * does not support), the error says so to the script's author, and its cause is what the JVM refused.
     */
    static KeyAgreement agreementWithJdkKey(
            PrivateKey privateKey, InvalidKeyException failure, ProviderProbe bouncyCastle)
            throws InvalidKeyException, NoSuchAlgorithmException {
        // null: the key type was not one this module loads, or a PKCS12 held no key
        if (privateKey == null || !isBouncyCastle(privateKey)) {
            throw failure;
        }
        Exception refused = null;
        try {
            bouncyCastle.run();
        } catch (GeneralSecurityException | SecurityException e) {
            refused = e;
        }
        if (refused == null) {
            throw failure;
        }
        try {
            // A provider other than BouncyCastle that has both the key factory and the key agreement, named for
            // both: not whichever provider takes the key, which could be BouncyCastle's key factory again.
            Provider provider = null;
            for (Provider candidate : Security.getProviders()) {
                if (!BouncyCastleProvider.PROVIDER_NAME.equals(candidate.getName())
                        && candidate.getService("KeyFactory", ECC_ALGORITHM) != null
                        && candidate.getService("KeyAgreement", "ECDH") != null) {
                    provider = candidate;
                    break;
                }
            }
            byte[] pkcs8 = privateKey.getEncoded();
            if (provider == null || pkcs8 == null) {
                throw new InvalidKeyException(provider == null ? "no other provider has EC" : "the key has no encoding");
            }
            KeyFactory jdk = KeyFactory.getInstance(ECC_ALGORITHM, provider);
            PrivateKey jdkKey = jdk.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
            KeyAgreement ka = KeyAgreement.getInstance("ECDH", provider);
            ka.init(jdkKey);
            logger.atWarning().atMostEvery(1, TimeUnit.HOURS).log(
                "This JVM does not accept the BouncyCastle provider (%s). ECDH with a SEC1 PEM key is using the"
                    + " JDK's provider instead. Run with BouncyCastle's own signed jars on the class path: classes"
                    + " repackaged into another jar are not signed.",
                refused.getMessage());
            return ka;
        } catch (GeneralSecurityException jdkFailure) {
            String forTheHost =
                "This JVM does not accept the BouncyCastle provider (" + refused.getMessage() + "), and the JDK's"
                    + " provider does not take the key (" + jdkFailure.getMessage() + "). Run with BouncyCastle's own"
                    + " signed jars on the class path.";
            // logged here: a host that runs scripts through javax.script gets the message below without its cause
            logger.atWarning().atMostEvery(1, TimeUnit.MINUTES).log("ECDH: %s", forTheHost);
            InvalidKeyException explained = new InvalidKeyException(
                "ECDH: this private key cannot be used here: its curve, or the way the key states it, is not one"
                    + " this runtime supports. Use a key on the named curve P-256, P-384 or P-521.",
                new GeneralSecurityException(forTheHost, refused));
            explained.addSuppressed(failure);
            explained.addSuppressed(jdkFailure);
            throw explained;
        }
    }

    private static boolean isBouncyCastle(PrivateKey key) {
        return key.getClass().getName().startsWith("org.bouncycastle.");
    }


    private static PrivateKey loadPrivateKeyPKCS8(StarlarkBytes privKey) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privKey.toByteArray());
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(spec);
    }

    private static PrivateKey loadPrivateKeySEC1(String pem) throws Exception {
        // Trim each line
        pem = Arrays.stream(pem.split("\n"))
            .map(String::trim)
            .collect(Collectors.joining("\n"));

        final PEMParser pemParser = new PEMParser(new StringReader(pem));
        final Object parsedPem = pemParser.readObject();
        if (!(parsedPem instanceof PEMKeyPair)) {
            throw new IOException("Attempted to parse PEM string as a keypair, but it's actually a " + parsedPem.getClass());
        }
        final JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

        return converter.getKeyPair((PEMKeyPair) parsedPem).getPrivate();
    }

    private static PrivateKey loadPrivateKeyPKCS12(StarlarkBytes privKey, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        InputStream inputStream = new ByteArrayInputStream(privKey.toByteArray());
        keyStore.load(inputStream, password);
        Enumeration<String> aliases = keyStore.aliases();
        PrivateKey privateKey = null;
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            // try:
            try {
                privateKey = (PrivateKey) keyStore.getKey(alias, password);
                return privateKey;
            } catch(Exception e){
                continue;
            }
        }
        return privateKey;
    }

    private static PublicKey loadPublicKeyX509(StarlarkBytes pubKey) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKey.toByteArray());
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePublic(keySpec);
    }
}
