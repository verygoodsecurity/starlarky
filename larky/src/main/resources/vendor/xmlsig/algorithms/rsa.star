load("@stdlib//hashlib",  hashlib="hashlib")
load("@stdlib//base64", b64decode="b64decode", b64encode="b64encode")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//Crypto/Signature/pkcs1_15", pkcs1_15="pkcs1_15")
load("@vendor//cryptography/hazmat/primitives/asymmetric", padding="padding", rsa="rsa")
load("@vendor//Crypto/Signature/pkcs1_15", pkcs1_15="pkcs1_15")

load("@vendor//xmlsig/algorithms/base", Algorithm="Algorithm")
load("@vendor//xmlsig/ns", ns="ns")
load("@vendor//xmlsig/utils", b64_print="b64_print", create_node="create_node", long_to_bytes="long_to_bytes", os2ip="os2ip")


NS_MAP = ns.NS_MAP
DSigNs = ns.DSigNs


def _RSAAlgorithm():

    self = Algorithm()
    self.__name__ = 'RSAAlgorithm'
    self.__class__ = _RSAAlgorithm
    self.private_key_class = RSA.RsaKey
    self.public_key_class = RSA.RsaKey

    def sign(data, private_key, digest):
        return private_key.sign(data, padding.PKCS1v15(), digest())
    self.sign = sign

    def verify(signature_value, data, public_key, digest):
        # def verify(msg_hash, signature):
        # pkcs1_15.new(public_key).verify(digest(data), b64decode(signature_value))
        # key = RSA.import_key(key_str)
        # hash = SHA256.new(utf_8_msg)
        public_key.verify(
            b64decode(signature_value), data, padding.PKCS1v15(), digest()
        )
    self.verify = verify

    def key_value(node, public_key):
        result = create_node("RSAKeyValue", node, DSigNs, "\n", "\n")
        create_node(
            "Modulus",
            result,
            DSigNs,
            tail="\n",
            text=b64_print(b64encode(long_to_bytes(public_key.n))),
        )
        create_node(
            "Exponent",
            result,
            DSigNs,
            tail="\n",
            text=b64encode(long_to_bytes(public_key.e)),
        )
        return result
    self.key_value = key_value

    # inherited method..
    self._Algorithm_get_public_key = self.get_public_key

    def get_public_key(key_info, ctx):
        """
        Get the public key if its defined in X509Certificate node. Otherwise,
        take self.public_key element
        :param sign: Signature node
        :type sign: lxml.etree.Element
        :return: Public key to use
        """
        key = key_info.find("ds:KeyInfo/ds:KeyValue/ds:RSAKeyValue", namespaces=NS_MAP)
        if key != None:
            n = os2ip(b64decode(key.find("ds:Modulus", namespaces=NS_MAP).text))
            e = os2ip(b64decode(key.find("ds:Exponent", namespaces=NS_MAP).text))
            return RSA.RSAKey(e=e, n=n)
        return self._Algorithm_get_public_key.get_public_key(key_info, ctx)
    self.get_public_key = get_public_key
    return self


RSAAlgorithm = _RSAAlgorithm()