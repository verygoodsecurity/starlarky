load("@stdlib//binascii",
     a2b_base64="a2b_base64",
     b2a_base64="b2a_base64",
     unhexlify="unhexlify",
     hexlify="hexlify")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//larky", larky="larky")
load("@stdlib//re", re="re")
load("@vendor//Crypto/Random", get_random_bytes="get_random_bytes")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Cipher/DES", DES="DES")
load("@vendor//Crypto/Cipher/DES3", DES3="DES3")
load("@vendor//Crypto/Hash/MD5", MD5="MD5")
load("@vendor//Crypto/Util/Padding", pad="pad", unpad="unpad")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")


def encode(data, marker, passphrase=None, randfunc=None):
    """
    Encode a piece of binary data into PEM format.

        Args:
          data (byte string):
            The piece of binary data to encode.
          marker (string):
            The marker for the PEM block (e.g. "PUBLIC KEY").
            Note that there is no official master list for all allowed markers.
            Still, you can refer to the OpenSSL_ source code.
          passphrase (byte string):
            If given, the PEM block will be encrypted. The key is derived from
            the passphrase.
          randfunc (callable):
            Random number generation function; it accepts an integer N and returns
            a byte string of random data, N bytes long. If not given, a new one is
            instantiated.

        Returns:
          The PEM block, as a string.

        .. _OpenSSL: https://github.com/openssl/openssl/blob/master/include/openssl/pem.h

    """

    if randfunc == None:
        randfunc = get_random_bytes

    out = "-----BEGIN %s-----\n" % marker
    if passphrase:
        # We only support 3DES for encryption
        salt = randfunc(8)
        key = _JCrypto.Protocol.PBKDF1(passphrase, salt, 16, 1, 'MD5')
        key += _JCrypto.Protocol.PBKDF1(key + passphrase, salt, 8, 1, 'MD5')
        objenc = DES3.new(key, DES3.MODE_CBC, salt)
        out += "Proc-Type: 4,ENCRYPTED\nDEK-Info: DES-EDE3-CBC,%s\n\n" %\
            tostr(hexlify(salt).upper())
        # Encrypt with PKCS#7 padding
        data = objenc.encrypt(pad(data, objenc.block_size))
    elif passphrase != None:
        fail("ValueError: Empty password")

    # Each BASE64 line can take up to 64 characters (=48 bytes of data)
    # b2a_base64 adds a new line character!
    chunks = [tostr(b2a_base64(data[i:i + 48]))
              for i in range(0, len(data), 48)]
    out += "".join(chunks)
    out += "-----END %s-----" % marker
    return out


def _EVP_BytesToKey(data, salt, key_len):
    d = [bytearray('', encoding='utf-8')]
    m = (key_len + 15 ) // 16
    for _ in range(m):
        nd = MD5.new(d[-1] + data + salt).digest()
        # NOTE(Larky-Difference): in larky, bytes are immutable.
        #  so we must use bytearrays instead of bytes.
        d.append(bytearray(nd))
    return bytearray('', encoding='utf-8').join(d)[:key_len]


def decode(pem_data, passphrase=None):
    """Decode a PEM block into binary.

    Args:
      pem_data (string):
        The PEM block.
      passphrase (byte string):
        If given and the PEM block is encrypted,
        the key will be derived from the passphrase.

    Returns:
      A tuple with the binary data, the marker string, and a boolean to
      indicate if decryption was performed.

    Raises:
      ValueError: if decoding fails, if the PEM file is encrypted and no passphrase has
                  been provided or if the passphrase is incorrect.
    """

    # Verify Pre-Encapsulation Boundary
    r = re.compile(r"\s*-----BEGIN (.*)-----\s+")
    m = r.match(pem_data)
    if not m:
        fail('ValueError: Not a valid PEM pre boundary')
    marker = m.group(1)

    # Verify Post-Encapsulation Boundary
    r = re.compile(r"-----END (.*)-----\s*$")
    m = r.search(pem_data)
    if not m or m.group(1) != marker:
        fail('ValueError: Not a valid PEM post boundary')

    # Removes spaces and slit on lines
    lines = pem_data.replace(" ", '')
    lines = re.split(r'(?:\s|\x0B|\r?\n)+', lines)
    # Decrypts, if necessary
    if lines[1].startswith('Proc-Type:4,ENCRYPTED'):
        if not passphrase:
            fail('ValueError: PEM is encrypted, but no passphrase available')
        DEK = lines[2].split(':')
        if len(DEK) != 2 or DEK[0] != 'DEK-Info':
            fail('ValueError: PEM encryption format not supported.')
        algo, salt = DEK[1].split(',')
        salt = unhexlify(tobytes(salt))

        padding = True
        if algo == "DES-CBC":
            key = _EVP_BytesToKey(passphrase, salt, 8)
            objdec = DES.new(key, DES.MODE_CBC, salt)
        elif algo == "DES-EDE3-CBC":
            key = _EVP_BytesToKey(passphrase, salt, 24)
            objdec = DES3.new(key, DES3.MODE_CBC, salt)
        elif algo == "AES-128-CBC":
            key = _EVP_BytesToKey(passphrase, salt[:8], 16)
            objdec = AES.new(key, AES.MODE_CBC, salt)
        elif algo == "AES-192-CBC":
            key = _EVP_BytesToKey(passphrase, salt[:8], 24)
            objdec = AES.new(key, AES.MODE_CBC, salt)
        elif algo == "AES-256-CBC":
            key = _EVP_BytesToKey(passphrase, salt[:8], 32)
            objdec = AES.new(key, AES.MODE_CBC, salt)
        elif algo.lower() == "id-aes256-gcm":
            key = _EVP_BytesToKey(passphrase, salt[:8], 32)
            objdec = AES.new(key, AES.MODE_GCM, nonce=salt)
            padding = False
        else:
            fail("Unsupport PEM encryption algorithm (%s)." % algo)

        lines = lines[2:]
    else:
        objdec = None
        enc_flag = False

    # Decode body
    data = a2b_base64(''.join(lines[1:-1]))
    enc_flag = False
    if objdec:
        if padding:
            data = unpad(objdec.decrypt(data), objdec.block_size)
        else:
            # There is no tag, so we don't use decrypt_and_verify
            data = objdec.decrypt(data)
        enc_flag = True

    return (data, marker, enc_flag)


PEM = larky.struct(
    decode=decode,
    encode=encode
)