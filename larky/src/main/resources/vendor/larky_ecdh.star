load("@stdlib//larky", "larky")
load("@stdlib//base64", "base64")
load("@stdlib//builtins", "builtins")
load("@stdlib//re", "re")

load("@stdlib//ECDH", ECDH="ECDH")


def LarkyECDH():
    self = larky.mutablestruct(__name__='LarkyECDH', __class__=LarkyECDH)
    def __init__():
        self.private_key = None
        self.public_key = None
        self.private_key_type = "PEM"
        self.public_key_type = "X509"
        self.private_pass = ""
        self.private_types = ["PEM", "PKCS8", "PKCS12"]
        self.public_types = ["X509"]
    self.__init__ = __init__
    self.__init__()

    def set_private_key(private_key, type="PEM", passwd=""):
        if type == "PKCS12" and passwd == "":
            fail("Error: PKCS12 Keystores require a password. Keystores without a password are not supported.")
        if type == "PKCS12" or type == "PKCS8":
            private_key = self._strip(private_key)
        elif type == "PEM":
            private_key = private_key.decode("utf-8")
        elif type not in self.private_types:
            fail("Unsupported private key type: %s. Available types: %s" % (type, str([x for x in self.private_types])))
        self.private_key = private_key
        self.private_key_type = type
        self.private_pass = passwd
    self.set_private_key = set_private_key

    def set_public_key(public_key, type="X509"):
        if type == "X509":
            self.public_key = self._strip(public_key)
        elif type not in self.public_types:
            fail("Unsupported public key type: %s. Available types: %s" % (type, str([x for x in self.public_types])))
    self.set_public_key = set_public_key

    def _strip(string):
        string = string.decode("utf-8")
        headers = re.findall("(-----.*-----)", string)
        for header in headers:
            string = string.replace(header,"")
        string.replace("\n","")
        return base64.b64decode(string)
    self._strip = _strip

    def exchange():
        if self.private_key == None:
            fail("Error: No private key set. Use LarkyECDH.set_private_key() to set it.")
        elif self.public_key == None:
            fail("Error: No public key set. Use LarkyECDH.set_public_key() to set it.")

        return ECDH.key_exchange(self.private_key,
                                 self.private_key_type,
                                 self.public_key,
                                 self.public_key_type,
                                 self.private_pass)
    self.exchange = exchange

    return self