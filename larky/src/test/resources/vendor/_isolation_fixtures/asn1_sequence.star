# A native ASN.1 sequence built at load time, which a call decodes into (replacing its contents).
load("@stdlib//jcrypto", _JCrypto="jcrypto")

_SEQ = _JCrypto.Util.ASN1.DerSequence([1, 2])

def decode_shared(data):
    _SEQ.decode(bytes([0x30, 0x03, 0x02, 0x01, 0x07]))
    return str(len([x for x in _SEQ]))
