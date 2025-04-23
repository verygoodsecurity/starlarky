package com.verygood.security.larky.modules.vgs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.QueueInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;

/**
 * Module that exposes PGP functionality from Bouncycastle to Larky
 */
@StarlarkBuiltin(
    name = "pgp",
    category = "BUILTIN",
    doc = "Module for PGP operations using the Bouncycastle library"
)
public class PGPModule implements StarlarkValue {

    public static final PGPModule INSTANCE = new PGPModule();
    
    /**
     * Get the internal hash algorithm ID from a string name
     * 
     * @param hashAlgoString The hash algorithm name
     * @return The internal hash algorithm ID
     * @throws EvalException If the algorithm is not supported
     */
    @StarlarkMethod(
        name = "get_hash_algorithm",
        doc = "Gets the hash algorithm ID from a string name",
        parameters = {
            @Param(name = "algorithm", allowedTypes = {@ParamType(type = String.class)})
        }
    )
    public int getHashAlgorithm(String hashAlgoString) throws EvalException {
        String algoString = hashAlgoString.toUpperCase().trim();
        switch (algoString) {
            case "SHA1", "SHA-1":
                return HashAlgorithmTags.SHA1;
            case "SHA224", "SHA-224":
                return HashAlgorithmTags.SHA224;
            case "SHA256", "SHA-256":
                return HashAlgorithmTags.SHA256;
            case "SHA384", "SHA-384":
                return HashAlgorithmTags.SHA384;
            case "SHA512", "SHA-512":
                return HashAlgorithmTags.SHA512;
            default:
                throw Starlark.errorf("Unsupported hash algorithm: %s. Supported algorithms: SHA1, SHA-1, SHA224, SHA-224, SHA256, SHA-256, SHA384, SHA-384, SHA512, SHA-512", algoString);
        }
    }

    /**
     * Signs a message using a PGP private key
     * 
     * @param message The message to sign
     * @param privateKeyArmored The ASCII-armored private key
     * @param passphrase The passphrase for the private key
     * @param hashAlgorithm The hash algorithm to use
     * @param armor Whether to ASCII-armor the output
     * @param thread The Starlark thread
     * @return The signed message
     * @throws EvalException If signing fails
     */
    @StarlarkMethod(
        name = "sign",
        doc = "Signs a message with a PGP private key",
        parameters = {
            @Param(name = "message", named = true, allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
            @Param(name = "private_key", named = true, allowedTypes = {@ParamType(type = String.class)}),
            @Param(name = "passphrase",
            named = true,
            allowedTypes = {
                @ParamType(type = String.class),
                @ParamType(type = NoneType.class)
            }, defaultValue = "None"),
            @Param(name = "hash_algorithm",
            named = true,
            allowedTypes = {
                @ParamType(type = String.class),
                @ParamType(type = NoneType.class)
            }, defaultValue = "None"),
            @Param(name = "armor",
            named = true,
            defaultValue = "True")
        },
        useStarlarkThread = true
    )
    public StarlarkBytes sign(
        StarlarkBytes message,
        String privateKeyArmored,
        Object passphraseObj,
        Object hashAlgorithmObj,
        boolean armor,
        StarlarkThread thread
    ) throws EvalException {
        char[] passphrase = Starlark.isNullOrNone(passphraseObj) 
            ? new char[0] 
            : ((String) passphraseObj).toCharArray();
        
        int hashAlgorithm = HashAlgorithmTags.SHA256; // Default
        if (!Starlark.isNullOrNone(hashAlgorithmObj)) {
            String hashAlgoString = (String) hashAlgorithmObj;
            hashAlgorithm = getHashAlgorithm(hashAlgoString);
        }
        
        try {
            // Load the private key
            PGPSecretKey secretKey = readSecretKey(privateKeyArmored);
            PGPPrivateKey privateKey = extractPrivateKey(secretKey, passphrase);
            
            // Sign the data
            ByteArrayOutputStream signedOut = new ByteArrayOutputStream();
            
            if (armor) {
                ArmoredOutputStream armoredOut = new ArmoredOutputStream(signedOut);
                sign(message.toByteArray(), privateKey, secretKey.getPublicKey(), hashAlgorithm, armoredOut);
                armoredOut.close();
            } else {
                sign(message.toByteArray(), privateKey, secretKey.getPublicKey(), hashAlgorithm, signedOut);
            }
            
            return StarlarkBytes.of(thread.mutability(), signedOut.toByteArray());
        } catch (IOException | PGPException e) {
            throw Starlark.errorf("PGP signing failed: %s", e.getMessage());
        }
    }

    /**
     * Verifies a signed message using a PGP public key
     * 
     * @param signedMessage The signed message to verify
     * @param publicKeyArmored The ASCII-armored public key
     * @param thread The Starlark thread
     * @return True if signature is valid, false otherwise
     * @throws EvalException If verification fails
     */
    @StarlarkMethod(
        name = "verify",
        doc = "Verifies a signed message with a PGP public key",
        parameters = {
            @Param(name = "signed_message", named = true, allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
            @Param(name = "public_key", named = true, allowedTypes = {@ParamType(type = String.class)})
        },
        useStarlarkThread = true
    )
    public boolean verify(
        StarlarkBytes signedMessage,
        String publicKeyArmored,
        StarlarkThread thread
    ) throws EvalException {
        try {
            // Load the public key
            PGPPublicKey publicKey = readPublicKey(publicKeyArmored);
            
            // Verify the signature
            return verifySignature(signedMessage.toByteArray(), publicKey);
        } catch (IOException | PGPException e) {
            throw Starlark.errorf("PGP verification failed: %s", e.getMessage());
        }
    }

    /**
     * Encrypts a message using a PGP public key
     *
     * @param message      The message to encrypt
     * @param publicKeyArmored The ASCII-armored PGP public key
     * @param armor        Whether to ASCII-armor the output
     * @param withIntegrityCheck Whether to add an integrity check
     * @param thread       The Starlark thread
     * @return             The encrypted message
     * @throws EvalException If encryption fails
     */
    @StarlarkMethod(
        name = "encrypt",
        doc = "Encrypts a message with a PGP public key",
        parameters = {
            @Param(name = "message", named = true, allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
            @Param(name = "public_key", named = true, allowedTypes = {@ParamType(type = String.class)}),
            @Param(name = "armor", named = true, defaultValue = "True"),
            @Param(name = "integrity_check", named = true, defaultValue = "True"),
            @Param(name = "algorithm",named = true, allowedTypes = {
                @ParamType(type = String.class),
                @ParamType(type = NoneType.class)
            }, defaultValue = "None"),
            @Param(name = "private_key", named = true, allowedTypes = {
                @ParamType(type = String.class),
                @ParamType(type = NoneType.class)
            }, defaultValue = "None"),
            @Param(name = "passphrase", named = true, allowedTypes = {
                @ParamType(type = String.class),
                @ParamType(type = NoneType.class)
            }, defaultValue = "None"),
            @Param(name = "hash_algorithm", named = true, allowedTypes = {
                @ParamType(type = String.class),
                @ParamType(type = NoneType.class)
            }, defaultValue = "None")
        },
        useStarlarkThread = true
    )
    public StarlarkBytes encrypt(
        StarlarkBytes message,
        String publicKeyArmored,
        boolean armor,
        boolean withIntegrityCheck,
        Object algorithmObj,
        Object privateKeyObj,
        Object passphraseObj,
        Object hashAlgorithmObj,
        StarlarkThread thread
    ) throws EvalException {
        int algorithm = SymmetricKeyAlgorithmTags.AES_256; // Default
        
        if (!Starlark.isNullOrNone(algorithmObj)) {
            String algoString = ((String) algorithmObj).toUpperCase().trim();
            switch (algoString) {
                case "AES128", "AES-128":
                    algorithm = SymmetricKeyAlgorithmTags.AES_128;
                    break;
                case "AES192", "AES-192":
                    algorithm = SymmetricKeyAlgorithmTags.AES_192;
                    break;
                case "AES256", "AES-256":
                    algorithm = SymmetricKeyAlgorithmTags.AES_256;
                    break;
                case "BLOWFISH":
                    algorithm = SymmetricKeyAlgorithmTags.BLOWFISH;
                    break;
                case "CAMELLIA128", "CAMELLIA-128":
                    algorithm = SymmetricKeyAlgorithmTags.CAMELLIA_128;
                    break;
                case "CAMELLIA192", "CAMELLIA-192":
                    algorithm = SymmetricKeyAlgorithmTags.CAMELLIA_192;
                    break;
                case "CAMELLIA256", "CAMELLIA-256":
                    algorithm = SymmetricKeyAlgorithmTags.CAMELLIA_256;
                    break;
                case "TWOFISH":
                    algorithm = SymmetricKeyAlgorithmTags.TWOFISH;
                    break;
                default:
                    throw Starlark.errorf("Unsupported encryption algorithm: %s. Supported algorithms: AES128, AES-128, AES192, AES-192, AES256, AES-256, BLOWFISH, CAMELLIA128, CAMELLIA-128, CAMELLIA192, CAMELLIA-192, CAMELLIA256, CAMELLIA-256, TWOFISH", algoString);
            }
        }
        
        try {
            // Load the public key
            PGPPublicKey publicKey = readPublicKey(publicKeyArmored);
            
            // Check if we need to sign first
            if (!Starlark.isNullOrNone(privateKeyObj)) {
                // First sign the message, then encrypt
                String privateKeyArmored = (String) privateKeyObj;
                char[] passphrase = Starlark.isNullOrNone(passphraseObj) 
                    ? new char[0] 
                    : ((String) passphraseObj).toCharArray();
                
                int hashAlgorithm = HashAlgorithmTags.SHA256; // Default
                if (!Starlark.isNullOrNone(hashAlgorithmObj)) {
                    String hashAlgoString = (String) hashAlgorithmObj;
                    hashAlgorithm = getHashAlgorithm(hashAlgoString);
                }
                
                // Load the private key for signing
                PGPSecretKey secretKey = readSecretKey(privateKeyArmored);
                PGPPrivateKey privateKey = extractPrivateKey(secretKey, passphrase);
                
                // Sign and encrypt
                ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();
                
                if (armor) {
                    encryptedOut = new ByteArrayOutputStream();
                    ArmoredOutputStream armoredOut = new ArmoredOutputStream(encryptedOut);
                    signAndEncrypt(message.toByteArray(), privateKey, secretKey.getPublicKey(), 
                                 publicKey, armoredOut, withIntegrityCheck, algorithm, hashAlgorithm);
                    armoredOut.close();
                } else {
                    signAndEncrypt(message.toByteArray(), privateKey, secretKey.getPublicKey(), 
                                 publicKey, encryptedOut, withIntegrityCheck, algorithm, hashAlgorithm);
                }
                
                return StarlarkBytes.of(thread.mutability(), encryptedOut.toByteArray());
            } else {
                // Just encrypt without signing
                ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();
                
                if (armor) {
                    encryptedOut = new ByteArrayOutputStream();
                    ArmoredOutputStream armoredOut = new ArmoredOutputStream(encryptedOut);
                    encrypt(message.toByteArray(), publicKey, armoredOut, withIntegrityCheck, algorithm);
                    armoredOut.close();
                } else {
                    encrypt(message.toByteArray(), publicKey, encryptedOut, withIntegrityCheck, algorithm);
                }
                
                return StarlarkBytes.of(thread.mutability(), encryptedOut.toByteArray());
            }
        } catch (IOException | PGPException e) {
            throw Starlark.errorf("PGP encryption failed: %s", e.getMessage());
        }
    }

    /**
     * Decrypts a PGP encrypted message using a private key
     *
     * @param encryptedMessage The encrypted message
     * @param privateKeyArmored The ASCII-armored private key
     * @param passphrase The passphrase for the private key
     * @param thread The Starlark thread
     * @return The decrypted message
     * @throws EvalException If decryption fails
     */
    @StarlarkMethod(
        name = "decrypt",
        doc = "Decrypts a PGP encrypted message",
        parameters = {
            @Param(name = "encrypted_message", named = true, allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
            @Param(name = "private_key", named = true, allowedTypes = {@ParamType(type = String.class)}),
            @Param(name = "passphrase", named = true, allowedTypes = {
                @ParamType(type = String.class),
                @ParamType(type = NoneType.class)
            }, defaultValue = "None"),
            @Param(name = "verify", named = true, defaultValue = "False"),
            @Param(name = "public_key", named = true, allowedTypes = {
                @ParamType(type = String.class),
                @ParamType(type = NoneType.class)
            }, defaultValue = "None")
        },
        useStarlarkThread = true
    )
    public StarlarkBytes decrypt(
        StarlarkBytes encryptedMessage,
        String privateKeyArmored,
        Object passphraseObj,
        boolean verify,
        Object publicKeyObj,
        StarlarkThread thread
    ) throws EvalException {
        char[] passphrase = Starlark.isNullOrNone(passphraseObj) 
            ? new char[0] 
            : ((String) passphraseObj).toCharArray();

        try {
            // Load the private key
            PGPSecretKey secretKey = readSecretKey(privateKeyArmored);
            PGPPrivateKey privateKey = extractPrivateKey(secretKey, passphrase);
            
            // If verify is true, we need a public key
            PGPPublicKey publicKey = null;
            if (verify) {
                if (Starlark.isNullOrNone(publicKeyObj)) {
                    // Use the public key from the secret key
                    publicKey = secretKey.getPublicKey();
                } else {
                    // Use the provided public key
                    String publicKeyArmored = (String) publicKeyObj;
                    publicKey = readPublicKey(publicKeyArmored);
                }
            }
            
            // Decrypt the data
            byte[] decryptedData;
            if (verify && publicKey != null) {
                decryptedData = decryptAndVerify(encryptedMessage.toByteArray(), privateKey, publicKey);
            } else {
                decryptedData = decrypt(encryptedMessage.toByteArray(), privateKey);
            }
            
            return StarlarkBytes.of(thread.mutability(), decryptedData);
        } catch (IOException | PGPException e) {
            throw Starlark.errorf("PGP decryption failed: %s", e.getMessage());
        }
    }

    /**
     * Signs the data with a private key
     */
    private void sign(
        byte[] data,
        PGPPrivateKey privateKey,
        PGPPublicKey publicKey,
        int hashAlgorithm,
        OutputStream out
    ) throws IOException, PGPException {
        // Create a signature generator
        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
            new BcPGPContentSignerBuilder(publicKey.getAlgorithm(), hashAlgorithm),
            publicKey
        );
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
        
        // Add hashed subpackets to the signature
        PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
        signatureGenerator.setHashedSubpackets(spGen.generate());
        
        // Compress the data
        PGPCompressedDataGenerator compressGen = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);
        try (OutputStream compressOut = compressGen.open(out)) {
            // Start the signature
            signatureGenerator.generateOnePassVersion(false).encode(compressOut);
            
            // Create a literal data packet
            PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
            try (OutputStream literalOut = literalGen.open(
                compressOut,
                PGPLiteralData.BINARY,
                "data",
                data.length,
                new Date()
            )) {
                // Write the data and update the signature
                literalOut.write(data);
                signatureGenerator.update(data);
            }
            
            // Generate the signature
            signatureGenerator.generate().encode(compressOut);
        }
        
        compressGen.close();
    }

    /**
     * Verifies a signed message
     */
    private boolean verifySignature(byte[] signedData, PGPPublicKey publicKey) 
            throws PGPException, IOException {
        InputStream in = PGPUtil.getDecoderStream(new ByteArrayInputStream(signedData));
        PGPObjectFactory pgpFact = new BcPGPObjectFactory(in);

        // Process the message
        Object message = pgpFact.nextObject();
        if (message instanceof PGPCompressedData compressedData) {
          pgpFact = new BcPGPObjectFactory(compressedData.getDataStream());
        }

        // Get the signature and verify
        PGPOnePassSignatureList onePassSignatureList = (PGPOnePassSignatureList) pgpFact.nextObject();
        PGPOnePassSignature onePassSignature = onePassSignatureList.get(0);
        
        // Set up the verifier
        onePassSignature.init(new BcPGPContentVerifierBuilderProvider(), publicKey);
        
        // Read the data
        PGPLiteralData literalData = (PGPLiteralData) pgpFact.nextObject();
        InputStream dataIn = literalData.getInputStream();
        int ch;
        while ((ch = dataIn.read()) >= 0) {
            onePassSignature.update((byte) ch);
        }
        
        // Verify the signature
        PGPSignatureList signatureList = (PGPSignatureList) pgpFact.nextObject();
        PGPSignature signature = signatureList.get(0);
        
        return onePassSignature.verify(signature);
    }

    /**
     * Sign and encrypt data in a single operation
     */
    private void signAndEncrypt(
        byte[] data,
        PGPPrivateKey privateKey,
        PGPPublicKey signingKey,
        PGPPublicKey encryptionKey,
        OutputStream out,
        boolean withIntegrityCheck,
        int algorithm,
        int hashAlgorithm
    ) throws IOException, PGPException {
        // Set up the encryptor
        PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
            new BcPGPDataEncryptorBuilder(algorithm)
                .setWithIntegrityPacket(withIntegrityCheck)
                .setSecureRandom(new SecureRandom())
        );
        
        encGen.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(encryptionKey));
        
        try (OutputStream encOut = encGen.open(out, new byte[4096])) {
            // First sign the data
            ByteArrayOutputStream signedOut = new ByteArrayOutputStream();
            sign(data, privateKey, signingKey, hashAlgorithm, signedOut);
            
            // Now write the signed data to the encrypted output
            encOut.write(signedOut.toByteArray());
        }
    }

    /**
     * Decrypt and verify a message in a single operation
     */
    private byte[] decryptAndVerify(byte[] encryptedData, PGPPrivateKey privateKey, PGPPublicKey publicKey) 
            throws PGPException, IOException {
        // First decrypt
        byte[] decryptedData = decrypt(encryptedData, privateKey);
        
        // Then verify the signature in the decrypted data
        boolean verified = verifySignature(decryptedData, publicKey);
        if (!verified) {
            throw new PGPException("Signature verification failed");
        }
        
        return extractMessageFromSignedData(decryptedData);
    }

    /**
     * Extract the actual message content from signed data
     */
    private byte[] extractMessageFromSignedData(byte[] signedData) 
            throws PGPException, IOException {
        InputStream in = PGPUtil.getDecoderStream(new ByteArrayInputStream(signedData));
        PGPObjectFactory pgpFact = new BcPGPObjectFactory(in);
        
        // Process the message
        PGPCompressedData compressedData = (PGPCompressedData) pgpFact.nextObject();
        pgpFact = new BcPGPObjectFactory(compressedData.getDataStream());
        
        // Skip the signature
        pgpFact.nextObject(); // Skip one-pass signature list
        
        // Get the literal data
        PGPLiteralData literalData = (PGPLiteralData) pgpFact.nextObject();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Streams.pipeAll(literalData.getInputStream(), out);
        
        return out.toByteArray();
    }

    /**
     * Core encryption implementation
     */
    private void encrypt(
        byte[] clearData,
        PGPPublicKey encKey,
        OutputStream out,
        boolean withIntegrityCheck,
        int algorithm
    ) throws IOException, PGPException {
        // Create an encryptor with the specified algorithm
        PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
            new BcPGPDataEncryptorBuilder(algorithm)
                .setWithIntegrityPacket(withIntegrityCheck)
                .setSecureRandom(new SecureRandom())
        );
        
        encGen.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(encKey));
        
        OutputStream encOut = encGen.open(out, new byte[4096]);
        
        // Compress the data
        PGPCompressedDataGenerator compressGen = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);
        OutputStream compressOut = compressGen.open(encOut);
        
        // Create a literal data packet
        PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
        try (OutputStream literalOut = literalGen.open(
            compressOut,
            PGPLiteralData.BINARY,
            "data",
            clearData.length,
            new Date()
        )) {
            // Write the data
            literalOut.write(clearData);
        }
        
        compressGen.close();
        encGen.close();
    }

    /**
     * Core decryption implementation
     */
    private byte[] decrypt(byte[] encryptedData, PGPPrivateKey privateKey) 
            throws PGPException, IOException {
        InputStream in = PGPUtil.getDecoderStream(new ByteArrayInputStream(encryptedData));
        PGPObjectFactory pgpF = new BcPGPObjectFactory(in);
        
        // The first object might be the encrypted data list or a marker packet
        Object o = pgpF.nextObject();
        if (o instanceof PGPEncryptedDataList) {
            PGPEncryptedDataList enc = (PGPEncryptedDataList) o;
            
            // Find the data encrypted to our key
            Iterator<PGPEncryptedData> it = enc.getEncryptedDataObjects();
            PGPPrivateKey key = privateKey;
            PGPPublicKeyEncryptedData pbe = null;
            
            while (it.hasNext()) {
                PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) it.next();
                
                if (encData.getKeyID() == key.getKeyID()) {
                    pbe = encData;
                    break;
                }
            }
            
            if (pbe == null) {
                throw new PGPException("No encrypted data found for the provided key");
            }

            // Decrypt the data
            InputStream decrypted = pbe.getDataStream(new BcPublicKeyDataDecryptorFactory(key));

            // We need to copy in case data is signed so we can resend it for verification
            ByteArrayOutputStream copyOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(decrypted, copyOutputStream);
            decrypted = IOUtils.copy(copyOutputStream);
            InputStream decryptedCopy = IOUtils.copy(copyOutputStream);

            PGPObjectFactory plainFact = new BcPGPObjectFactory(decrypted);

            Object message = plainFact.nextObject();

            if (message instanceof PGPCompressedData compressedData) {
                plainFact = new BcPGPObjectFactory(compressedData.getDataStream());
                message = plainFact.nextObject();
            }
            
            if (message instanceof PGPLiteralData literalData) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Streams.pipeAll(literalData.getInputStream(), out);
                return out.toByteArray();
            } else if (message instanceof PGPOnePassSignatureList) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                // resend copy of decrypted signed data for verification
                Streams.pipeAll(decryptedCopy, out);
                return out.toByteArray();
            } else {
                throw new PGPException("Unknown message type: " + message.getClass().getName());
            }
        } else {
            throw new PGPException("Invalid PGP data format");
        }
    }

    /**
     * Read a public key from ASCII-armored format
     */
    private PGPPublicKey readPublicKey(String armoredKey) 
            throws IOException, PGPException {
        InputStream keyIn = new ByteArrayInputStream(armoredKey.getBytes(StandardCharsets.UTF_8));
        InputStream in = PGPUtil.getDecoderStream(keyIn);
        
        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in, new BcKeyFingerprintCalculator());
        
        // Find the first valid encryption key in the key ring
        Iterator<PGPPublicKeyRing> keyRings = pgpPub.getKeyRings();
        while (keyRings.hasNext()) {
            PGPPublicKeyRing keyRing = keyRings.next();
            Iterator<PGPPublicKey> keys = keyRing.getPublicKeys();
            
            while (keys.hasNext()) {
                PGPPublicKey key = keys.next();
                if (key.isEncryptionKey()) {
                    return key;
                }
            }
        }
        
        throw new PGPException("No encryption key found in key ring");
    }

    /**
     * Read a secret key from ASCII-armored format
     */
    private PGPSecretKey readSecretKey(String armoredKey) 
            throws IOException, PGPException {
        InputStream keyIn = new ByteArrayInputStream(armoredKey.getBytes(StandardCharsets.UTF_8));
        InputStream in = PGPUtil.getDecoderStream(keyIn);
        
        PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(in, new BcKeyFingerprintCalculator());
        
        // Find the first valid secret key in the key ring
        Iterator<PGPSecretKeyRing> keyRings = pgpSec.getKeyRings();
        while (keyRings.hasNext()) {
            PGPSecretKeyRing keyRing = keyRings.next();
            Iterator<PGPSecretKey> keys = keyRing.getSecretKeys();
            
            while (keys.hasNext()) {
                PGPSecretKey key = keys.next();
                if (key.isSigningKey()) {
                    return key;
                }
            }
        }
        
        throw new PGPException("No signing key found in key ring");
    }

    /**
     * Extract a private key from a secret key using the passphrase
     */
    private PGPPrivateKey extractPrivateKey(PGPSecretKey secretKey, char[] passphrase) 
            throws PGPException {
        return secretKey.extractPrivateKey(
            new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())
                .build(passphrase));
    }
}