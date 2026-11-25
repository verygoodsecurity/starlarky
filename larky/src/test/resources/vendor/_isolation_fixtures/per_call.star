# Mutable native objects created inside the function: each call (so each evaluation) gets its own,
# owned by the calling evaluation, and may change them freely.
load("@stdlib//binascii", "binascii")
load("@stdlib//larky", "larky")
load("@stdlib//zlib", "zlib")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")

def digest(data):
    h = SHA256.new()
    h.update(data)
    h.update(data)
    cipher = AES.new(b"0123456789abcdef", AES.MODE_ECB)
    first = cipher.encrypt(data)
    second = cipher.encrypt(data)
    counter = larky.utils.Counter()
    counter.add_and_get()
    packed = zlib.decompress(zlib.compress(data * 4))
    return "%s:%s:%s:%d:%d" % (
        h.hexdigest(),
        str(binascii.hexlify(first)),
        str(binascii.hexlify(second)),
        counter.get(),
        len(packed),
    )
