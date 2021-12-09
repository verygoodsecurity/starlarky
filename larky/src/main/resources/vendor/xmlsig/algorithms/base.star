load("@stdlib//larky", larky="larky")
load("@stdlib//base64", base64="base64")
load("@stdlib//types", types="types")
load("@vendor//jopenssl", _JOpenSSL="jopenssl")
load("@vendor//option/result", Error="Error")
load("@vendor//xmlsig/ns", ns="ns")


def Algorithm():
    self = larky.mutablestruct(__name__='Algorithm', __class__=Algorithm)

    self.private_key_class = None
    self.public_key_class = None

    def sign(data, private_key, digest):
        fail("Exception: Sign function must be redefined")
    self.sign = sign

    def verify(signature_value, data, public_key, digest):
        fail("Exception: Verify function must be redefined")
    self.verify = verify

    def key_value(node, public_key):
        fail("Exception: Key Value function must be redefined")
    self.key_value = key_value

    def get_public_key(key_info, ctx):
        """
        Get the public key if its defined in X509Certificate node. Otherwise,
        take self.public_key element
        :param sign: Signature node
        :type sign: lxml.etree.Element
        :return: Public key to use
        """
        x509_certificate = key_info.find(
            "ds:KeyInfo/ds:X509Data/ds:X509Certificate", namespaces={"ds": ns.DSigNs}
        )
        if x509_certificate != None:
            return _JOpenSSL.load_der_x509_certificate(base64.b64decode(x509_certificate.text)).public_key()
        if ctx.public_key != None:
            return ctx.public_key
        if types.is_string(ctx.private_key) or types.is_bytelike(ctx.private_key):
            return ctx.private_key
        return ctx.private_key.public_key()
    self.get_public_key = get_public_key
    return self

