load("@stdlib//unittest","unittest")
load("@stdlib//base64","base64")
load("@stdlib//codecs","codecs")
load("@vendor//asserts","asserts")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//OpenSSL/crypto", crypto="crypto")


sample_private_key = \
        ("-----BEGIN RSA PRIVATE KEY-----\n" +
     "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\n" +
     "-----END RSA PRIVATE KEY-----")

def TestRSA_test_create_x509_pyopenssl():
    # Using pyopenssl to create private key
    # k = crypto.PKey()
    # Load private key from pycryptodome generated key:
    new_private_key = RSA.import_key(sample_private_key)
    new_private_key_pem = new_private_key.export_key("PEM")
    k = crypto.load_privatekey(crypto.FILETYPE_PEM, new_private_key_pem, None)
    cert = crypto.X509()
    cert.get_subject().C = "US"
    cert.get_subject().ST = "VIRGINIA"
    cert.get_subject().L = "RICHMOND"
    cert.get_subject().O = "VERY GOOD SECURITY"
    cert.get_subject().OU = "VERY GOOD SECURITY"
    cert.get_subject().CN = "www.verygoodsecurity.com"
    cert.get_subject().emailAddress = "donotspam@example.com"

    cert.gmtime_adj_notBefore(0)
    cert.gmtime_adj_notAfter(2*365*24*60*60)
    cert.set_issuer(cert.get_subject())
    cert.set_pubkey(k)
    cert.sign(k, "sha256")
    pem_cert = codecs.decode(crypto.dump_certificate(crypto.FILETYPE_PEM, cert), encoding="utf-8")
    asserts.assert_that(("-----BEGIN CERTIFICATE-----" in pem_cert)).is_true()
    # pem format private key from pyopenssl
    # pem_private_key = crypto.dump_privatekey(crypto.FILETYPE_PEM, k).decode("utf-8")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(TestRSA_test_create_x509_pyopenssl))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
