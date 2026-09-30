"""
#.  Copyright (C) 2005-2010   Gregory P. Smith (greg@krypto.org)
#  Licensed to PSF under a Contributor Agreement.
#
hashlib module - A common interface to many hash functions.
new(name, data=b'', **kwargs) - returns a new hash object implementing the
                                given hash function; initializing the hash
                                using the given binary data.
Named constructor functions are also available, these are faster
than using new(name):
md5(), sha1(), sha224(), sha256(), sha384(), sha512(), blake2b(), blake2s(),
sha3_224, sha3_256, sha3_384, sha3_512, shake_128, and shake_256.
More algorithms may be available on your platform but the above are guaranteed
to exist.  See the algorithms_guaranteed and algorithms_available attributes
to find out what algorithm names can be passed to new().
NOTE: If you want the adler32 or crc32 hash functions they are available in
the zlib module.
Choose your hash function wisely.  Some have known collision weaknesses.
sha384 and sha512 will be slow on 32 bit platforms.
Hash objects have these methods:
 - update(data): Update the hash object with the bytes in data. Repeated calls
                 are equivalent to a single call with the concatenation of all
                 the arguments.
 - digest():     Return the digest of the bytes passed to the update() method
                 so far as a bytes object.
 - hexdigest():  Like digest() except the digest is returned as a string
                 of double length, containing only hexadecimal digits.
 - copy():       Return a copy (clone) of the hash object. This can be used to
                 efficiently compute the digests of datas that share a common
                 initial substring.
For example, to obtain the digest of the byte string 'Nobody inspects the
spammish repetition':
    >>> import hashlib
    >>> m = hashlib.md5()
    >>> m.update(b"Nobody inspects")
    >>> m.update(b" the spammish repetition")
    >>> m.digest()
    b'\\xbbd\\x9c\\x83\\xdd\\x1e\\xa5\\xc9\\xd9\\xde\\xc9\\xa1\\x8d\\xf0\\xff\\xe9'
More condensed:
    >>> hashlib.sha224(b"Nobody inspects the spammish repetition").hexdigest()
    'a4337bc45a8fc544c03f52dc550cd6e1e87021bc896588bd79e901e2'
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vendor//Crypto/Hash/BLAKE2s", BLAKE2s="BLAKE2s")
load("@vendor//Crypto/Hash/MD5", MD5="MD5")
load("@vendor//Crypto/Hash/SHA1", SHA1="SHA1")
load("@vendor//Crypto/Hash/SHA224", SHA224="SHA224")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//Crypto/Hash/SHA384", SHA384="SHA384")
load("@vendor//Crypto/Hash/SHA512", SHA512="SHA512")
load("@vendor//Crypto/Hash/SHAKE128", SHAKE128="SHAKE128")


def _blake2s(data=b'', **kwargs):
    # Crypto.Hash.BLAKE2s.new takes keyword arguments only
    return BLAKE2s.new(data=data, **kwargs)


__hashes = dict(
    md5=MD5.new,
    sha=SHA1.new,
    sha1=SHA1.new,
    sha224=SHA224.new,
    sha256=SHA256.new,
    sha384=SHA384.new,
    sha512=SHA512.new,
    blake2s=_blake2s,
    shake_128=SHAKE128.new,
)

# Other spellings CPython's hashlib.new accepts (OpenSSL's names, matched
# case-insensitively), mapped to the constructor names above.
_OPENSSL_NAMES = {
    "md5": "md5",
    "ssl3-md5": "md5",
    "sha1": "sha1",
    "sha-1": "sha1",
    "ssl3-sha1": "sha1",
    "sha224": "sha224",
    "sha-224": "sha224",
    "sha2-224": "sha224",
    "sha256": "sha256",
    "sha-256": "sha256",
    "sha2-256": "sha256",
    "sha384": "sha384",
    "sha-384": "sha384",
    "sha2-384": "sha384",
    "sha512": "sha512",
    "sha-512": "sha512",
    "sha2-512": "sha512",
    "blake2s256": "blake2s",
    "shake128": "shake_128",
}


def _new(name, data=b'', **kwargs):
    """new(name, data=b'') - Return a new hashing object using the named algorithm;
    optionally initialized with data (which must be a bytes-like object).
    """
    if not types.is_string(name):
        fail("new() argument 'name' must be str, not %s" % type(name))
    if name not in __hashes:
        canonical = _OPENSSL_NAMES.get(name.lower())
        if canonical == None:
            fail("unsupported hash type %s" % name)
        name = canonical
    return __hashes[name](data, **kwargs)


hashlib = larky.struct(
    __name__='hashlib',
    new=_new,
    **__hashes
)

