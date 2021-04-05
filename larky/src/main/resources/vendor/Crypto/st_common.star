load("@stdlib//builtins","builtins")
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//re", re="re")
load("@stdlib//types", types="types")


def strip_whitespace(s):
    """Remove whitespace from a text or byte string"""
    if types.is_string(s):
        return bytes(re.sub(r'(\s|\x0B|\r?\n)+', '', s), encoding='utf-8')
    else:
        b = bytes('', encoding='utf-8')
        return b.join(s.split())


def a2b_hex(s):
    return binascii.a2b_hex(strip_whitespace(s))


def b2a_hex(s):
    return binascii.b2a_hex(s)

