package com.verygood.security.larky.modules;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
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
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

@StarlarkBuiltin(
        name = "ECDH",
        category = "BUILTIN",
        doc = "A backend for ECDH operations."
)
public class ECDHModule implements StarlarkValue {

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
        ka.init(privateKey);
        ka.doPhase(publicKey, true);
        return ka.generateSecret();
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
