load("@stdlib//binascii",
     a2b_base64="a2b_base64",
     b2a_base64="b2a_base64",
     unhexlify="unhexlify",
     hexlify="hexlify")
load("@stdlib//larky", larky="larky")
load("@stdlib//re", re="re")
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

def _EVP_BytesToKey(data, salt, key_len):
    d = [bytearray('', encoding='utf-8')]
    m = (key_len + 15 ) // 16
    for _ in range(m):
        nd = MD5.new(d[-1] + data + salt).digest()
        d.append(nd)
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
        # TODO(mahmoudimus): IMPLEMENT ME
        # if algo == "DES-CBC":
        #     key = _EVP_BytesToKey(passphrase, salt, 8)
        #     objdec = DES.new(key, DES.MODE_CBC, salt)
        # elif algo == "DES-EDE3-CBC":
        #     key = _EVP_BytesToKey(passphrase, salt, 24)
        #     objdec = DES3.new(key, DES3.MODE_CBC, salt)
        # elif algo == "AES-128-CBC":
        #     key = _EVP_BytesToKey(passphrase, salt[:8], 16)
        #     objdec = AES.new(key, AES.MODE_CBC, salt)
        # elif algo == "AES-192-CBC":
        #     key = _EVP_BytesToKey(passphrase, salt[:8], 24)
        #     objdec = AES.new(key, AES.MODE_CBC, salt)
        # elif algo == "AES-256-CBC":
        #     key = _EVP_BytesToKey(passphrase, salt[:8], 32)
        #     objdec = AES.new(key, AES.MODE_CBC, salt)
        # elif algo.lower() == "id-aes256-gcm":
        #     key = _EVP_BytesToKey(passphrase, salt[:8], 32)
        #     objdec = AES.new(key, AES.MODE_GCM, nonce=salt)
        #     padding = False
        # else:
        #     raise ValueError("Unsupport PEM encryption algorithm (%s)." % algo)
        lines = lines[2:]
        # TODO(mahmoudimus) remove me (👇) when algos implemented
        objdec = None
        enc_flag = True
    else:
        objdec = None
        enc_flag = False

    # Decode body
    data = a2b_base64(''.join(lines[1:-1]))
    # TODO(mahmoudimus): remove comment (👇) when algos implemented
    #enc_flag = False
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