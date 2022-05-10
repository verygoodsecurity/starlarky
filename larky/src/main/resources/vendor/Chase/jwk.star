load("@stdlib//larky", larky="larky")
load("@stdlib//builtins", buildins="builtins")

load("@vgs//chase", Chase="chase")

def get_public_keys():
    keys = Chase.get_keys()
    return keys

def decrypt(jwe_bytes):
    if types.is_string(jwe_bytes):
        jwe_bytes = bytes(jwe_bytes, 'utf-8')
    decrypted = Chase.decrypt(jwe_bytes)
    return decrypted

jwk = larky.struct(
  get_public_keys=get_public_keys,
  decrypt=decrypt
)
