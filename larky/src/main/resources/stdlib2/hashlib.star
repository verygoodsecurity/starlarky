def __get_builtin_constructor(name):
    """
    'SHA1'
    """
def __get_openssl_constructor(name):
    """
     Prefer our blake2 and sha3 implementation.

    """
def __py_new(name, data=b'', **kwargs):
    """
    new(name, data=b'', **kwargs) - Return a new hashing object using the
        named algorithm; optionally initialized with data (which must be
        a bytes-like object).
    
    """
def __hash_new(name, data=b'', **kwargs):
    """
    new(name, data=b'') - Return a new hashing object using the named algorithm;
        optionally initialized with data (which must be a bytes-like object).
    
    """
    def pbkdf2_hmac(hash_name, password, salt, iterations, dklen=None):
        """
        Password based key derivation function 2 (PKCS #5 v2.0)

                This Python implementations based on the hmac module about as fast
                as OpenSSL's PKCS5_PBKDF2_HMAC for short passwords and much faster
                for long passwords.
        
        """
        def prf(msg, inner=inner, outer=outer):
            """
             PBKDF2_HMAC uses the password as key. We can re-use the same
             digest objects and just update copies to skip initialization.

            """
