package com.verygood.security.larky.modules;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// nosemgrep: secrets.misc.generic_private_key.generic_private_key
public class CyberSourceModuleTest {

    private Mutability mutability;
    private StarlarkThread thread;
    private CyberSourceModule module;
    private static final String TEST_PRIVATE_KEY = """
        -----BEGIN PRIVATE KEY-----
        MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCYoc5Ue4MKxHIQ
        eSESKQiIv341EFDtfAlAsXP74modJuwnSLOfSkFNgKH4y6vSKiUK7BxU2KFy7FkR
        J9/vceJmP9MD6bWPgT2Wg4iSQxgPtAHEVps9MYvkhW0lt0hyhAcGLUR3kb4YjSkG
        fa8EzG/G2g+/VKdL0mnSgWhCnSBnR0xRwWccgdRTLm20/jzXkmHD92DBR7kDgiBU
        rPWTfLHDnsVoIUut6BAPI83TIjHjVG1Jn8K0prbGeQU9ALwaL36qvppYpmCqaAGH
        OM2fXsEPFNhEZxQpbyW2M4PtXHnjSqlNOKN2tmdF3jWwm9hKZ9xeaWJkBmBnLe3t
        Nz0OdO0pAgMBAAECggEBAJHQGn5JFJJnw5SLM5XWz4lcb2SgNr/5/BjqriQXVEqP
        UZHh+X+Wf7ZbyeEWKgp4KrU5hYNlBS/2LMyf7GYixSfrl1qoncP/suektwcLw+PU
        ks+P8XRPbhadhP1AEJ0eFlvHSR51hEaOLIA/98C80ZgF4H9njv93f5MT/5eL5lXi
        pFX1dcxUB55q9QOtQ7uCg++NyG5F6u4FxbNtOtsjyNzWZSjYsjSyGHDip9ScDOPN
        sGQfznxo/oifdXvc25BgWvRflIIYEP08eeUSuGW2nUnx+Joc0oZTkC0wfU+aqKla
        Zp8zfOEIm0gUDgWtgnq5I5JHJMuW6BtA4K3E+nyP0lECgYEAzIbNx/lVxmFPbPp+
        AG9LD3JLycjdmTzwpHK44MsaUBOZ9PkLZs0NpR5z0/qcFb8YGGz3qN6E/TTydmfX
        CpZ3bxP3+x81gL9SVG/y2GP/ky/REA0jFycwVlONeVnd09xPNNLZLUgZhWyAQIA2
        pmVMh8W+pX6ojxGgOe+KIGutJCUCgYEAvwuNciTzkjBz9nFCjLONvP05WMdIAXo1
        uxd17iQ0lhRtmHbphojFAPcHYocm2oUXJo5nLvy+u8xnxbyXaZHmRqm98AzmBTtp
        phFtgfTtv/cSvOsBpdyyaJaN12IUs2XYACGBRa2DUkgxxvHtbmjFGFIU+5VgjOG8
        g0LfoPhLM7UCgYAmdRaOioihY7zOjg9RP5wKjIBJsfZREQ9irJus0SPieL0TPhzx
        uI7fRGmdK1tcD3GVbi/nVegFwIXy07WwrPhKL6QKWSTzT4ZIkEBGhg8RewVBkmbN
        vLWvFcjdT5ORebR/B0KE7DC4UN2Qw0sDYLrSMNGXRsilFjhdjHgZfoWw7QKBgAZr
        QvNk3nI5AoxzPcMwfUCuWXDsMTUrgAarQSEhQksQoKYQyMPmcIgZxLvAwsNw2VhI
        TJs9jsMMmSgBsCyx5ETXizQ3mrruRhx4VW+aZSqgCJckZkfGZJAzDsz/1KY6c8l9
        VrSaoeDv4AxJMKsXBhhNGbtiR340T3sxkgX8kbpJAoGBAII2aFeQ4oE8DhSZZo2b
        pJxO072xy1P9PRlyasYBJ2sNiF0TTguXJB1Ncu0TM0+FLZXIFddalPgv1hY98vNX
        22dZWKvD3xJ7HRUx/Hyk+VEkH11lsLZ/8AhcwZAr76cE/HLz1XtkKKBCnnlOLPZN
        03j+WKU3p1fzeWqfW4nyCALQ
        -----END PRIVATE KEY-----""";

    /*
    Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            41:26:36:06:cf:1b:3b:81:9b:85:f0:fe:fb:8e:d8:7f:79:d1:9c:a8
        Signature Algorithm: sha256WithRSAEncryption
        Issuer: C=US, ST=Some-State, O=Internet Widgits Pty Ltd
        Validity
            Not Before: Sep 12 12:28:45 2025 GMT
            Not After : Sep 12 12:28:45 2026 GMT
        Subject: C=US, ST=Some-State, O=Internet Widgits Pty Ltd
     */
    private static final String TEST_CERTIFICATE = """
        -----BEGIN CERTIFICATE-----
        MIIDazCCAlOgAwIBAgIUQSY2Bs8bO4GbhfD++47Yf3nRnKgwDQYJKoZIhvcNAQEL
        BQAwRTELMAkGA1UEBhMCVVMxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoM
        GEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yNTA5MTIxMjI4NDVaFw0yNjA5
        MTIxMjI4NDVaMEUxCzAJBgNVBAYTAlVTMRMwEQYDVQQIDApTb21lLVN0YXRlMSEw
        HwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggEiMA0GCSqGSIb3DQEB
        AQUAA4IBDwAwggEKAoIBAQCYoc5Ue4MKxHIQeSESKQiIv341EFDtfAlAsXP74mod
        JuwnSLOfSkFNgKH4y6vSKiUK7BxU2KFy7FkRJ9/vceJmP9MD6bWPgT2Wg4iSQxgP
        tAHEVps9MYvkhW0lt0hyhAcGLUR3kb4YjSkGfa8EzG/G2g+/VKdL0mnSgWhCnSBn
        R0xRwWccgdRTLm20/jzXkmHD92DBR7kDgiBUrPWTfLHDnsVoIUut6BAPI83TIjHj
        VG1Jn8K0prbGeQU9ALwaL36qvppYpmCqaAGHOM2fXsEPFNhEZxQpbyW2M4PtXHnj
        SqlNOKN2tmdF3jWwm9hKZ9xeaWJkBmBnLe3tNz0OdO0pAgMBAAGjUzBRMB0GA1Ud
        DgQWBBSDGv6WihmH3vwAiHynmqExEmmclDAfBgNVHSMEGDAWgBSDGv6WihmH3vwA
        iHynmqExEmmclDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQAx
        CpMM5w+vmMbsUnq6/vFdaY17PAYL3k1lrVDIkWZa5hVd7/ZaF2GbYIRLSy7j+37J
        b+/x0Fqx2S/IPooO6u3ASl9y7+IFW8ampAsLH2wvQ6X7xaP2EH24+pT3aceGyFB5
        xptK9O0MpmXD0JMbJvyGkWo2wXhZr6sYb89n2KfyvchaWCc0E83PxMUNSnrLw8Nz
        vsbLcR4UPV6y1W9+Viu1iiUxGsAck5q/UItS0qN9L8jfzhkaFg8OPSaKm3PUde2c
        g+PaDPWu+J/2rdWK/Zx3osMiXHYUWDOA7XeBQ4F4tetwmiSIdZEjJtA5TQ4Yr3Dd
        ZkVx8LN7GvhT2Hj2WsT/
        -----END CERTIFICATE-----
        """;

    private static final String TEST_SOAP_XML = """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <requestMessage xmlns="urn:schemas-cybersource-com:transaction-data-1.219">
              <merchantID>test_merchant_id</merchantID>
              <merchantReferenceCode>test_merchant_reference_code</merchantReferenceCode>
              <billTo>
                <firstName>John</firstName>
                <lastName>Doe</lastName>
                <street1>1295 Charleston Road</street1>
                <city>Mountain View</city>
                <state>CA</state>
                <postalCode>94043</postalCode>
                <country>US</country>
                <phoneNumber>650-121-6000</phoneNumber>
                <email>nobody@hello.com</email>
                <ipAddress>10.7.7.7</ipAddress>
              </billTo>
              <shipTo>
                <firstName>Jane</firstName>
                <lastName>Doe</lastName>
                <street1>100 Elm Street</street1>
                <city>San Mateo</city>
                <state>CA</state>
                <postalCode>94401</postalCode>
                <country>US</country>
              </shipTo>
              <item id="0">
                <unitPrice>12.34</unitPrice>
              </item>
              <item id="1">
                <unitPrice>56.78</unitPrice>
              </item>
              <purchaseTotals>
                <currency>USD</currency>
              </purchaseTotals>
              <card>
                <accountNumber>4111111111111111</accountNumber>
                <expirationMonth>12</expirationMonth>
                <expirationYear>2025</expirationYear>
              </card>
              <ccAuthService run="true">
              </ccAuthService>
            </requestMessage>
          </soap:Body>
        </soap:Envelope>
        """;

    @Before
    public void setUp() {
        mutability = Mutability.create("CyberSourceModuleTest");
        thread = new StarlarkThread(mutability, StarlarkSemantics.DEFAULT);
        module = CyberSourceModule.INSTANCE;
    }

    @After
    public void tearDown() {
        mutability.close();
    }

    @Test
    public void testSignWithValidInputs() throws EvalException {
        String signedRequest = module.sign(TEST_SOAP_XML, TEST_PRIVATE_KEY, TEST_CERTIFICATE, thread);
        System.out.println(signedRequest);
        assertNotNull("Signed request should not be null", signedRequest);
        assertTrue("Signed request should contain soap:Envelope", 
                signedRequest.contains("soap:Envelope"));
        assertTrue("Signed request should contain wsse:Security header", 
                signedRequest.contains("wsse:Security"));
        assertTrue("Signed request should contain ds:Signature", 
                signedRequest.contains("ds:Signature"));
        assertTrue("Signed request should contain the original merchantID", 
                signedRequest.contains("test_merchant_id"));
        assertTrue("Signed request should contain BinarySecurityToken", 
                signedRequest.contains("BinarySecurityToken"));
        assertTrue("Signed request should contain SignedInfo", 
                signedRequest.contains("SignedInfo"));
        assertTrue("Signed request should contain SignatureValue", 
                signedRequest.contains("SignatureValue"));
    }

    @Test
    public void testSignWithEmptyRequest() {
        try {
            module.sign("", TEST_PRIVATE_KEY, TEST_CERTIFICATE, thread);
            fail("Should throw EvalException for empty request");
        } catch (EvalException e) {
            assertTrue("Error message should mention empty request", 
                    e.getMessage().contains("Request cannot be empty"));
        }
    }

    @Test
    public void testSignWithNullRequest() {
        try {
            module.sign(null, TEST_PRIVATE_KEY, TEST_CERTIFICATE, thread);
            fail("Should throw EvalException for null request");
        } catch (EvalException e) {
            assertTrue("Error message should mention empty request", 
                    e.getMessage().contains("Request cannot be empty"));
        }
    }

    @Test
    public void testSignWithEmptyPrivateKey() {
        try {
            module.sign(TEST_SOAP_XML, "", TEST_CERTIFICATE, thread);
            fail("Should throw EvalException for empty private key");
        } catch (EvalException e) {
            assertTrue("Error message should mention empty private key", 
                    e.getMessage().contains("Private key cannot be empty"));
        }
    }

    @Test
    public void testSignWithNullPrivateKey() {
        try {
            module.sign(TEST_SOAP_XML, null, TEST_CERTIFICATE, thread);
            fail("Should throw EvalException for null private key");
        } catch (EvalException e) {
            assertTrue("Error message should mention empty private key", 
                    e.getMessage().contains("Private key cannot be empty"));
        }
    }

    @Test
    public void testSignWithEmptyCertificate() {
        try {
            module.sign(TEST_SOAP_XML, TEST_PRIVATE_KEY, "", thread);
            fail("Should throw EvalException for empty certificate");
        } catch (EvalException e) {
            assertTrue("Error message should mention empty certificate", 
                    e.getMessage().contains("Certificate cannot be empty"));
        }
    }

    @Test
    public void testSignWithNullCertificate() {
        try {
            module.sign(TEST_SOAP_XML, TEST_PRIVATE_KEY, null, thread);
            fail("Should throw EvalException for null certificate");
        } catch (EvalException e) {
            assertTrue("Error message should mention empty certificate", 
                    e.getMessage().contains("Certificate cannot be empty"));
        }
    }

    @Test
    public void testSignWithInvalidXML() {
        String invalidXml = "This is not valid XML";
        try {
            module.sign(invalidXml, TEST_PRIVATE_KEY, TEST_CERTIFICATE, thread);
            fail("Should throw EvalException for invalid XML");
        } catch (EvalException e) {
            assertTrue("Error message should mention failed signing", 
                    e.getMessage().contains("Failed to sign request"));
        }
    }

    @Test
    public void testSignWithInvalidPrivateKey() {
        String invalidKey = "-----BEGIN RSA PRIVATE KEY-----\nINVALID_KEY_DATA\n-----END RSA PRIVATE KEY-----";
        try {
            module.sign(TEST_SOAP_XML, invalidKey, TEST_CERTIFICATE, thread);
            fail("Should throw EvalException for invalid private key");
        } catch (EvalException e) {
            assertTrue("Error message should mention failed signing", 
                    e.getMessage().contains("Failed to sign request"));
        }
    }

    @Test
    public void testSignWithInvalidCertificate() {
        String invalidCert = "-----BEGIN CERTIFICATE-----\nINVALID_CERT_DATA\n-----END CERTIFICATE-----";
        try {
            module.sign(TEST_SOAP_XML, TEST_PRIVATE_KEY, invalidCert, thread);
            fail("Should throw EvalException for invalid certificate");
        } catch (EvalException e) {
            assertTrue("Error message should mention failed signing", 
                    e.getMessage().contains("Failed to sign request"));
        }
    }

    @Test
    public void testSignPreservesOriginalContent() throws EvalException {
        String signedRequest = module.sign(TEST_SOAP_XML, TEST_PRIVATE_KEY, TEST_CERTIFICATE, thread);
        
        // Verify that original content is preserved
        assertTrue("Should preserve merchantID", signedRequest.contains("test_merchant_id"));
        assertTrue("Should preserve merchantReferenceCode", signedRequest.contains("test_merchant_reference_code"));
        assertTrue("Should preserve firstName", signedRequest.contains("John"));
        assertTrue("Should preserve lastName", signedRequest.contains("Doe"));
        assertTrue("Should preserve email", signedRequest.contains("nobody@hello.com"));
        assertTrue("Should preserve currency", signedRequest.contains("USD"));
        assertTrue("Should preserve card number", signedRequest.contains("4111111111111111"));
        assertTrue("Should preserve street address", signedRequest.contains("1295 Charleston Road"));
        assertTrue("Should preserve city", signedRequest.contains("Mountain View"));
        assertTrue("Should preserve state", signedRequest.contains("CA"));
        assertTrue("Should preserve postal code", signedRequest.contains("94043"));
        assertTrue("Should preserve ship to first name", signedRequest.contains("Jane"));
        assertTrue("Should preserve unit prices", signedRequest.contains("12.34"));
        assertTrue("Should preserve unit prices", signedRequest.contains("56.78"));
    }

    @Test
    public void testSignatureAlgorithm() throws EvalException {
        String signedRequest = module.sign(TEST_SOAP_XML, TEST_PRIVATE_KEY, TEST_CERTIFICATE, thread);
        
        // Verify the signature algorithm is RSA-SHA256
        assertTrue("Should use RSA-SHA256 algorithm", 
                signedRequest.contains("rsa-sha256") || 
                signedRequest.contains("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"));
    }

    @Test
    public void testDigestAlgorithm() throws EvalException {
        String signedRequest = module.sign(TEST_SOAP_XML, TEST_PRIVATE_KEY, TEST_CERTIFICATE, thread);
        
        // Verify the digest algorithm is SHA256
        assertTrue("Should use SHA256 digest", 
                signedRequest.contains("sha256") || 
                signedRequest.contains("http://www.w3.org/2001/04/xmlenc#sha256"));
    }
}