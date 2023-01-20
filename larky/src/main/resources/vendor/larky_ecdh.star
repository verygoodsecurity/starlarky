load("@stdlib//larky", "larky")
load("@stdlib//base64", "base64")
load("@stdlib//builtins", "builtins")
load("@stdlib//re", "re")

load("@stdlib//ECDH", ECDH="ECDH")


def _strip(string):
    string = string.decode("utf-8")
    headers = re.findall("(-----.*-----)", string)
    for header in headers:
        string = string.replace(header,"")
    string.replace("\n","")
    return base64.b64decode(string)

def exchange(private, privType, public, pubType, private_pass=None):
    if privType != "PEM":
        """
        Strip the newlines and header/footer, then base64 decode
        if it is PKCS8 or PKCS12. 
        """
        private = _strip(private)
    else:
        """
        The whole PEM string has to be passed as-is if it's PEM/SEC1. 
        """
        private = private.decode('utf-8')
    public = _strip(public)

    if private_pass == None:
        private_pass = ""

    return ECDH.key_exchange(private,
                             privType,
                             public,
                             pubType,
                             private_pass)

ecdh = larky.struct(
    exchange=exchange,
)