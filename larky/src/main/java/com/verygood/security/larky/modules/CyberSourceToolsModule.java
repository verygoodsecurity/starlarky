package com.verygood.security.larky.modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Base64;
import java.util.Collections;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.xml.security.Init;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;

/**
 * Module that provides tools for working with CyberSource payment gateway operations.
 */
@StarlarkBuiltin(
    name = "cybersource_tools",
    category = "BUILTIN",
    doc = "Module providing tools for CyberSource payment gateway operations"
)
public class CyberSourceToolsModule implements StarlarkValue {

    public static final CyberSourceToolsModule INSTANCE = new CyberSourceToolsModule();
    
    private static final String SIGNATURE_ALGORITHM = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    private static final String DIGEST_ALGORITHM = "http://www.w3.org/2001/04/xmlenc#sha256";
    private static final String KEY_ALIAS = "signer";
    private static final String KEY_PASSWORD = "";
    
    static {
        Security.addProvider(new BouncyCastleProvider());
        Init.init();
    }
    
    /**
     * Parse PEM formatted RSA private key (PKCS#1 format)
     */
    private PrivateKey parsePrivateKey(String pemKey) throws Exception {
        // Remove PEM headers/footers and decode base64
        pemKey = pemKey.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                      .replace("-----END RSA PRIVATE KEY-----", "")
                      .replace("-----BEGIN PRIVATE KEY-----", "")
                      .replace("-----END PRIVATE KEY-----", "")
                      .replaceAll("\\s", "");
        
        byte[] keyBytes = Base64.getDecoder().decode(pemKey);
        
        // Try to parse as PKCS#1 format
        try {
            org.bouncycastle.asn1.pkcs.RSAPrivateKey pkcs1Key = 
                org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(keyBytes);
            
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(
                pkcs1Key.getModulus(),
                pkcs1Key.getPrivateExponent()
            );
            
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            // If PKCS#1 fails, try PKCS#8
            java.security.spec.PKCS8EncodedKeySpec spec = 
                new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(spec);
        }
    }
    
    /**
     * Parse PEM formatted X509 certificate
     */
    private X509Certificate parseCertificate(String pemCert) throws Exception {
        // Remove PEM headers/footers if present
        pemCert = pemCert.replace("-----BEGIN CERTIFICATE-----", "")
                        .replace("-----END CERTIFICATE-----", "")
                        .replaceAll("\\s", "");
        
        // Add them back in proper format for CertificateFactory
        String formattedCert = "-----BEGIN CERTIFICATE-----\n" + 
                              pemCert + "\n" +
                              "-----END CERTIFICATE-----";
        
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(
            new ByteArrayInputStream(formattedCert.getBytes(StandardCharsets.UTF_8))
        );
    }
    
    /**
     * Create in-memory Crypto provider with the private key and certificate
     */
    private Crypto createInMemoryCrypto(String privateKeyPem, String certificatePem) throws Exception {
        // Parse the private key
        PrivateKey privateKey = parsePrivateKey(privateKeyPem);
        
        // Parse the certificate
        X509Certificate certificate = parseCertificate(certificatePem);
        
        // Create in-memory KeyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, KEY_PASSWORD.toCharArray());
        
        // Add private key and certificate to keystore
        keyStore.setKeyEntry(
            KEY_ALIAS, 
            privateKey, 
            KEY_PASSWORD.toCharArray(),
            new X509Certificate[]{certificate}
        );
        
        // Create Merlin instance with the keystore
        Merlin crypto = new Merlin();
        crypto.setKeyStore(keyStore);
        
        return crypto;
    }
    
    /**
     * Sign SOAP document with the provided private key and certificate
     */
    private Document signDocument(Document soapDocument, String privateKeyPem, String certificatePem) 
            throws Exception {
        // Create crypto provider
        Crypto crypto = createInMemoryCrypto(privateKeyPem, certificatePem);
        
        // Set up WS-Security header
        WSSecHeader secHeader = new WSSecHeader(soapDocument);
        secHeader.insertSecurityHeader();
        
        // Configure signature
        WSSecSignature signature = new WSSecSignature(secHeader);
        signature.setUserInfo(KEY_ALIAS, KEY_PASSWORD);
        signature.setDigestAlgo(DIGEST_ALGORITHM);
        signature.setSignatureAlgorithm(SIGNATURE_ALGORITHM);
        signature.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
        signature.setUseSingleCertificate(true);
        signature.setWsDocInfo(new WSDocInfo(soapDocument));
        
        // Specify parts to sign (SOAP Body)
        WSEncryptionPart bodyPart = new WSEncryptionPart(
            WSConstants.ELEM_BODY, 
            WSConstants.URI_SOAP11_ENV, 
            ""
        );
        signature.addReferencesToSign(Collections.singletonList(bodyPart));
        
        // Build signed document
        return signature.build(crypto);
    }
    
    /**
     * Convert Document to string
     */
    private String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = tf.newTransformer();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        
        return baos.toString(StandardCharsets.UTF_8);
    }
    
    /**
     * Sign a CyberSource request with the provided credentials
     * 
     * @param request The request payload (SOAP XML)
     * @param privateKey The private key for signing (PEM format)
     * @param certificate The certificate for signing (PEM format)
     * @param thread The Starlark thread
     * @return Signed SOAP XML request
     * @throws EvalException If signing fails
     */
    @StarlarkMethod(
        name = "sign",
        doc = """
            Signs a CyberSource SOAP request with the provided private key and certificate.
            
            Example:
              signed_request = cybersource_tools.sign(
                  '<SOAP-ENV:Envelope>...</SOAP-ENV:Envelope>',
                  '-----BEGIN RSA PRIVATE KEY-----...',
                  '-----BEGIN CERTIFICATE-----...'
              )""",
        parameters = {
            @Param(
                name = "request",
                doc = "The SOAP XML request payload"
            ),
            @Param(
                name = "private_key", 
                doc = "The private key for signing (PEM format)"
            ),
            @Param(
                name = "certificate",
                doc = "The certificate for signing (PEM format)"
            )
        },
        useStarlarkThread = true
    )
    public String sign(String request, String privateKey, String certificate, 
            StarlarkThread thread) throws EvalException {
        
        if (request == null || request.isEmpty()) {
            throw Starlark.errorf("Request cannot be empty");
        }
        
        if (privateKey == null || privateKey.isEmpty()) {
            throw Starlark.errorf("Private key cannot be empty");
        }
        
        if (certificate == null || certificate.isEmpty()) {
            throw Starlark.errorf("Certificate cannot be empty");
        }
        
        try {
            // Parse XML to Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            byte[] requestBytes = request.getBytes(StandardCharsets.UTF_8);
            Document soapDocument = builder.parse(new ByteArrayInputStream(requestBytes));
            
            // Sign the document
            Document signedDoc = signDocument(soapDocument, privateKey, certificate);
            
            // Convert back to string
            return documentToString(signedDoc);
            
        } catch (Exception e) {
            throw Starlark.errorf("Failed to sign request: %s", e.getMessage());
        }
    }
}