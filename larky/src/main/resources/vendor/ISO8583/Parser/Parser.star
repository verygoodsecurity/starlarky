load("@stdlib//larky", larky="larky")
load("@stdlib//jiso8583", _JISO8583="jiso8583")

decode = _JISO8583.Parser.decode

def _decode(bytes, packager_xml_string):
    return decode(bytes, packager_xml_string)

Parser = larky.struct(
    decode=_decode,
)

# decode = Parser.decode