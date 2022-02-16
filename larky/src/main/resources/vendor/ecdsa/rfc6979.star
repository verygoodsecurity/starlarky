load("@stdlib//larky", larky="larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION")
load("@stdlib//binascii", hexlify="hexlify")
load("@vendor//Crypto/Hash/HMAC", hmac="HMAC")
load("@vendor//ecdsa/_compat", hmac_compat="hmac_compat")
load("@vendor//ecdsa/util", number_to_string="number_to_string", number_to_string_crop="number_to_string_crop", bit_length="bit_length")

def bits2int(data, qlen):
    x = int(hexlify(data), 16)
    l = len(data) * 8

    if l > qlen:
        return x >> (l - qlen)
    return 

def bits2octets(data, order):
    z1 = bits2int(data, bit_length(order))
    z2 = z1 - order

    if z2 < 0:
        z2 = z1

    return number_to_string_crop(z2, order)

# https://tools.ietf.org/html/rfc6979#section-3.2
def generate_k(order, secexp, hash_func, data, retry_gen=0, extra_entropy=b""):
    """
        order - order of the DSA generator used in the signature
        secexp - secure exponent (private key) in numeric form
        hash_func - reference to the same hash function used for generating
            hash
        data - hash in binary form of the signing data
        retry_gen - int - how many good 'k' values to skip before returning
        extra_entropy - extra added data in binary form as per section-3.6 of
            rfc6979
    """

    qlen = bit_length(order)
    holen = hash_func().digest_size
    rolen = (qlen + 7) // 8
    bx = (
        hmac_compat(number_to_string(secexp, order)),
        hmac_compat(bits2octets(data, order)),
        hmac_compat(extra_entropy),
    )

    # Step B
    v = b"\x01" * holen

    # Step C
    k = b"\x00" * holen

    # Step D

    k = hmac.new(k, digestmod=hash_func)
    k.update(v + b"\x00")
    for i in bx:
        k.update(i)
    k = k.digest()

    # Step E
    v = hmac.new(k, v, hash_func).digest()

    # Step F
    k = hmac.new(k, digestmod=hash_func)
    k.update(v + b"\x01")
    for i in bx:
        k.update(i)
    k = k.digest()

    # Step G
    v = hmac.new(k, v, hash_func).digest()

    # Step H
    for __while__ in range(WHILE_LOOP_EMULATION_ITERATION):
        # Step H1
        t = b""

        # Step H2
        for __while__ in range(WHILE_LOOP_EMULATION_ITERATION):
            if len(t) >= rolen:
                break
            v = hmac.new(k, v, hash_func).digest()
            t += v

        # Step H3
        secret = bits2int(t, qlen)

        if (secret >= 1) and (secret < order):
            if retry_gen <= 0:
                return secret
            retry_gen -= 1

        k = hmac.new(k, v + b"\x00", hash_func).digest()
        v = hmac.new(k, v, hash_func).digest()

rfc6979 = larky.struct(
    generate_k = generate_k
)