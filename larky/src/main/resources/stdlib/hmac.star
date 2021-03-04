def HMAC:
    """
    RFC 2104 HMAC class.  Also complies with RFC 4231.

        This supports the API for Cryptographic Hash Functions (PEP 247).
    
    """
    def __init__(self, key, msg=None, digestmod=''):
        """
        Create a new HMAC object.

                key: bytes or buffer, key for the keyed hash object.
                msg: bytes or buffer, Initial input for the hash or None.
                digestmod: A hash name suitable for hashlib.new(). *OR*
                           A hashlib constructor returning a new hash object. *OR*
                           A module supporting PEP 247.

                           Required as of 3.8, despite its position after the optional
                           msg argument.  Passing it as a keyword argument is
                           recommended, though not required for legacy API reasons.
        
        """
    def name(self):
        """
        hmac-
        """
    def update(self, msg):
        """
        Feed data from msg into this hashing object.
        """
    def copy(self):
        """
        Return a separate copy of this hashing object.

                An update to this copy won't affect the original object.
        
        """
    def _current(self):
        """
        Return a hash object for the current state.

                To be used only internally with digest() and hexdigest().
        
        """
    def digest(self):
        """
        Return the hash value of this hashing object.

                This returns the hmac value as bytes.  The object is
                not altered in any way by this function; you can continue
                updating the object after calling this function.
        
        """
    def hexdigest(self):
        """
        Like digest(), but returns a string of hexadecimal digits instead.
        
        """
def new(key, msg=None, digestmod=''):
    """
    Create a new hashing object and return it.

        key: bytes or buffer, The starting key for the hash.
        msg: bytes or buffer, Initial input for the hash, or None.
        digestmod: A hash name suitable for hashlib.new(). *OR*
                   A hashlib constructor returning a new hash object. *OR*
                   A module supporting PEP 247.

                   Required as of 3.8, despite its position after the optional
                   msg argument.  Passing it as a keyword argument is
                   recommended, though not required for legacy API reasons.

        You can now feed arbitrary bytes into the object using its update()
        method, and can ask for the hash value at any time by calling its digest()
        or hexdigest() methods.
    
    """
def digest(key, msg, digest):
    """
    Fast inline implementation of HMAC.

        key: bytes or buffer, The key for the keyed hash object.
        msg: bytes or buffer, Input message.
        digest: A hash name suitable for hashlib.new() for best performance. *OR*
                A hashlib constructor returning a new hash object. *OR*
                A module supporting PEP 247.
    
    """
