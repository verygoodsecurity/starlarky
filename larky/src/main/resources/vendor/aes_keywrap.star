'''
Key wrapping and unwrapping as defined in RFC 3394.
Also a padding mechanism that was used in openssl at one time.
The purpose of this algorithm is to encrypt a key multiple times to add an extra layer of security.
'''
load("@stdlib//larky", "larky")
load("@stdlib//struct", struct="struct")
load("@stdlib//builtins","builtins")
load("@stdlib//binascii","binascii")
load("@vendor//Crypto/Cipher/AES", AES="AES")

def aes_unwrap_key_and_iv(kek, wrapped):
    n = len(wrapped)//8 - 1
    #NOTE: R[0] is never accessed, left in for consistency with RFC indices
    R = [None]+[wrapped[i*8:i*8+8] for i in range(1, n+1)]
    # [None, binascii.unhexlify('aef34bd8fb5a7b82'), binascii.unhexlify('9d3e862371d2cfe5')]
    
    A = struct.unpack(builtins.bytes('>Q'), wrapped[:8])[0]
    decrypt = AES.new(kek, AES.MODE_ECB).decrypt
    for j in range(5,-1,-1): #counting down
        for i in range(n, 0, -1): #(n, n-1, ..., 1) 
            ciphertext = bytearray(struct.pack(builtins.bytes('>Q'), A^(n*j+i))) + bytearray(R[i])
            B = bytes(decrypt(ciphertext))
            A = struct.unpack('>Q', B[:8])[0]
            return A, A
            '''
            R[i] = B[8:]
            z = r"".join(R[1:])
    ret = r"".join(R[1:]), A
    return ret
            '''

def aes_unwrap_key(kek, wrapped, iv=0xa6a6a6a6a6a6a6a6):
    '''
    key wrapping as defined in RFC 3394
    http://www.ietf.org/rfc/rfc3394.txt
    '''
    key, key_iv = aes_unwrap_key_and_iv(kek, wrapped)

    if key_iv != iv:
        string = " ValueError(\"Integrity Check Failed: \"" + str(key_iv) + "\" (expected \"" + str(iv) + ")\")"
        fail(string)

    return key


def aes_unwrap_key_withpad(kek, wrapped):
    '''
    alternate initial value for aes key wrapping, as defined in RFC 5649 section 3
    http://www.ietf.org/rfc/rfc5649.txt
    '''
    if len(wrapped) == 16:
        plaintext = AES.new(kek, AES.MODE_ECB).decrypt(wrapped)
        key, key_iv = plaintext[8:], struct.unpack('>Q', plaintext[:8])[0]
    else:
        key, key_iv = aes_unwrap_key_and_iv(kek, wrapped)
    key_iv = "{0:016X}".format(key_iv)
    if key_iv[:8] != "A65959A6":
        fail(" ValueError(\"Integrity Check Failed: \"+key_iv[:8]+\" (expected A65959A6)\")")
    key_len = int(key_iv[8:], 16)
    return key[:key_len]

def aes_wrap_key(kek, plaintext, iv=0xa6a6a6a6a6a6a6a6):
    n = len(plaintext)//8
    R = [None]+[plaintext[i*8:i*8+8] for i in range(0, n)]
    A = iv
    encrypt = AES.new(kek, AES.MODE_ECB).encrypt
    for j in range(6):
        for i in range(1, n+1):
            B = encrypt(struct.pack('>Q', A) + R[i])
            A = struct.unpack('>Q', B[:8])[0] ^ (n*j + i)
            R[i] = B[8:]
    return struct.pack('>Q', A) + bytes(r"", encoding='utf-8').join(R[1:])

def aes_wrap_key_withpad(kek, plaintext):
    iv = 0xA65959A600000000 + len(plaintext)
    plaintext = plaintext + bytes([0x00]) * ((8 - len(plaintext)) % 8)
    if len(plaintext) == 8:
        return AES.new(kek, AES.MODE_ECB).encrypt(struct.pack('>Q', iv) + plaintext)
    return aes_wrap_key(kek, plaintext, iv)

aes_keywrap = larky.struct (
  aes_unwrap_key_and_iv = aes_unwrap_key_and_iv,
  aes_unwrap_key = aes_unwrap_key,
  aes_unwrap_key_withpad = aes_unwrap_key_withpad,
  aes_wrap_key = aes_wrap_key
)
