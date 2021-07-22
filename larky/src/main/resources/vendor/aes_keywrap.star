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
    A = struct.unpack('>Q', wrapped[:8])[0]
    decrypt = AES.new(kek, AES.MODE_ECB).decrypt
    for j in range(5,-1,-1): #counting down
        for i in range(n, 0, -1): #(n, n-1, ..., 1)
            ciphertext = struct.pack('>Q', A^(n*j+i)) + R[i]
            B = decrypt(ciphertext)
            print(B.hex(), ">Q: ", struct.unpack('>Q', B[:8])[0], ">q: ", struct.unpack('>q', B[:8])[0])
            A = struct.unpack('>Q', B[:8])[0]
            R[i] = B[8:]
    return b"".join(R[1:]), A


def aes_unwrap_key(kek, wrapped, iv=0xa6a6a6a6a6a6a6a6):
    '''
    key wrapping as defined in RFC 3394
    http://www.ietf.org/rfc/rfc3394.txt
    '''
    key, key_iv = aes_unwrap_key_and_iv(kek, wrapped)
    if key_iv != iv:
        fail("Integrity Check Failed: "+hex(key_iv)+" (expected "+hex(iv)+")")
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
    key_iv = "%X" % key_iv
    if key_iv[:8] != "A65959A6":
        fail("Integrity Check Failed: "+key_iv[:8]+" (expected A65959A6)")
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
    return struct.pack('>Q', A) + b"".join(R[1:])


def aes_wrap_key_withpad(kek, plaintext):
    iv = 0xA65959A600000000 + len(plaintext)
    plaintext = plaintext + b"\0" * ((8 - len(plaintext)) % 8)
    if len(plaintext) == 8:
        return AES.new(kek, AES.MODE_ECB).encrypt(struct.pack('>Q', iv) + plaintext)
    return aes_wrap_key(kek, plaintext, iv)


aes_keywrap = larky.struct (
  aes_unwrap_key_and_iv = aes_unwrap_key_and_iv,
  aes_unwrap_key = aes_unwrap_key,
  aes_unwrap_key_withpad = aes_unwrap_key_withpad,
  aes_wrap_key = aes_wrap_key
)