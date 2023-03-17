package com.verygood.security.larky.modules.vgs.jks;

import com.verygood.security.larky.modules.x509.LarkyKeyPair;
import com.verygood.security.larky.modules.x509.LarkyX509Certificate;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class JKS implements StarlarkValue {

    public static final JKS INSTANCE = new JKS();

    @StarlarkMethod(
            name = "load_key_and_certificates",
            parameters = {
                    @Param(name = "keystore_bytes", allowedTypes = {
                            @ParamType(type = StarlarkBytes.class)
                    }),
                    @Param(name = "keystore_password", allowedTypes = {
                            @ParamType(type = StarlarkBytes.class),
                            @ParamType(type = NoneType.class),
                    }),
                    @Param(name = "key_alias", allowedTypes = {
                            @ParamType(type = StarlarkBytes.class),
                    }),
                    @Param(name = "key_password", allowedTypes = {
                            @ParamType(type = StarlarkBytes.class),
                    }),
            },
            useStarlarkThread = true
    )
    public Tuple loadKeyAndCertificates(
            final StarlarkBytes larkyKeystoreBytes,
            final Object larkyKeystorePassword,
            final StarlarkBytes larkyKeyAlias,
            final StarlarkBytes larkyKeyPassword,
            final StarlarkThread thread
    ) throws EvalException {

        byte[] keystoreBytes = larkyKeystoreBytes.toByteArray();
        final char[] keystorePassword = Starlark.isNullOrNone(larkyKeystorePassword)
                ? null
                : ((StarlarkBytes) larkyKeystorePassword).decode("utf-8", "report").toCharArray();
        String keyAlias = larkyKeyAlias.decode("utf-8", "report");
        char[] keyPassword = larkyKeyPassword.toCharArray();

        LarkyKeyPair keyPair;
        List<LarkyX509Certificate> certificateChain = new ArrayList<>();

        try (InputStream is = new ByteArrayInputStream(keystoreBytes)) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(is, keystorePassword);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword);
            PublicKey publicKey = keyStore.getCertificate(keyAlias).getPublicKey();
            keyPair = new LarkyKeyPair(publicKey, privateKey);
            for (Certificate certificate : keyStore.getCertificateChain(keyAlias)) {
                LarkyX509Certificate larkyCertificate = LarkyX509Certificate.of((X509Certificate) certificate);
                certificateChain.add(larkyCertificate);
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | IOException |
                 CertificateException e) {
            throw new EvalException(e);
        }

        return Tuple.of(
                keyPair,
                certificateChain.remove(0),
                certificateChain.isEmpty()
                        ? StarlarkList.empty()
                        : StarlarkList.immutableCopyOf(certificateChain)
        );
    }
}
