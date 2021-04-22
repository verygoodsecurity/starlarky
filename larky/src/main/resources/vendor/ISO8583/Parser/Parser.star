load("@stdlib//larky", larky="larky")
# load("@stdlib//jcrypto", _JCrypto="jcrypto")
# load("@vendor//Crypto/Random", Random="Random")
#
load("@stdlib//jiso8583", _JISO8583="jiso8583")
# load("@vendor//ISO8583/Parser", Parser="Parser")
# load("@vendor//ISO8583/Decode", Decode="Decode")

decode = _JISO8583.Parser.decode

def _decode(n, m):
    return decode(n, m)

Parser = larky.struct(
    decode=_decode,
)

# decode = Parser.decode