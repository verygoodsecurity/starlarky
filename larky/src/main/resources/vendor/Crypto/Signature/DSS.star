load("@stdlib//larky", larky="larky")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//Crypto/Math/Numbers", Integer="Integer")
load("@vendor//option/result", Error="Error")

def DssSigScheme(key, encoding, order):
    """
    A (EC)DSA signature object.
        Do not instantiate directly.
        Use :func:`Crypto.Signature.DSS.new`.
    
    """
    self = larky.mutablestruct(__class__='DssSigScheme')

    def __init__(key, encoding, order):
        """
        Create a new Digital Signature Standard (DSS) object.

                Do not instantiate this object directly,
                use `Crypto.Signature.DSS.new` instead.
        
        """
        self._key = key
        self._encoding = encoding
        self._order = order

        self._order_bits = self._order.size_in_bits()
        self._order_bytes = (self._order_bits - 1) // 8 + 1

        return self

    self = __init__(key, encoding, order)

    def can_sign():
        """
        Return ``True`` if this signature object can be used
                for signing messages.
        """
        return self._key.has_private()

    def _compute_nonce(msg_hash):
        """
        To be provided by subclasses
        """
        fail('NotImplementedError("To be provided by subclasses")')

    def _valid_hash(msg_hash):
        """
        To be provided by subclasses
        """
        fail('NotImplementedError("To be provided by subclasses")')

    def sign(msg_hash):
        """
        Produce the DSA/ECDSA signature of a message.

                :parameter msg_hash:
                    The hash that was carried out over the message.
                    The object belongs to the :mod:`Crypto.Hash` package.

                    Under mode *'fips-186-3'*, the hash must be a FIPS
                    approved secure hash (SHA-1 or a member of the SHA-2 family),
                    of cryptographic strength appropriate for the DSA key.
                    For instance, a 3072/256 DSA key can only be used
                    in combination with SHA-512.
                :type msg_hash: hash object

                :return: The signature as a *byte string*
                :raise ValueError: if the hash algorithm is incompatible to the (EC)DSA key
                :raise TypeError: if the (EC)DSA key has no private half
        
        """
        if not self._key.has_private():
            fail('TypeError("Private key is needed to sign")')
        
        if not self._valid_hash(msg_hash):
            fail('ValueError("Hash is not sufficiently strong")')

        # Generate the nonce k (critical!)
        nonce = self._compute_nonce(msg_hash)

        # Perform signature using the raw API
        z = Integer.from_bytes(msg_hash.digest()[:self._order_bytes])
        sig_pair = self._key._sign(z, nonce)
        
        # Encode the signature into a single byte string
        if self._encoding == 'binary':
            output = b"".join([long_to_bytes(x, self._order_bytes)] for x in sig_pair)
        else:
            # Dss-sig  ::=  SEQUENCE  {
            #   r   INTEGER,
            #   s   INTEGER
            # }
            # Ecdsa-Sig-Value  ::=  SEQUENCE  {
            #   r   INTEGER,
            #   s   INTEGER
            # }
            output = DerSequence(sig_pair).encode()
        
        return output

    def verify(msg_hash, signature):
        """
        Check if a certain (EC)DSA signature is authentic.

                :parameter msg_hash:
                    The hash that was carried out over the message.
                    This is an object belonging to the :mod:`Crypto.Hash` module.

                    Under mode *'fips-186-3'*, the hash must be a FIPS
                    approved secure hash (SHA-1 or a member of the SHA-2 family),
                    of cryptographic strength appropriate for the DSA key.
                    For instance, a 3072/256 DSA key can only be used in
                    combination with SHA-512.
                :type msg_hash: hash object

                :parameter signature:
                    The signature that needs to be validated
                :type signature: byte string

                :raise ValueError: if the signature is not authentic
        
        """
        
def DeterministicDsaSigScheme(DssSigScheme):
    """
     Also applicable to ECDSA


    """
    def __init__(self, key, encoding, order, private_key):
        """
        See 2.3.2 in RFC6979
        """
    def _int2octets(self, int_mod_q):
        """
        See 2.3.3 in RFC6979
        """
    def _bits2octets(self, bstr):
        """
        See 2.3.4 in RFC6979
        """
    def _compute_nonce(self, mhash):
        """
        Generate k in a deterministic way
        """
    def _valid_hash(self, msg_hash):
        """
        : List of L (bit length of p) and N (bit length of q) combinations
        : that are allowed by FIPS 186-3. The security level is provided in
        : Table 2 of FIPS 800-57 (rev3).

        """
    def __init__(self, key, encoding, order, randfunc):
        """
        L/N (%d, %d) is not compliant to FIPS 186-3

        """
    def _compute_nonce(self, msg_hash):
        """
         hash is not used

        """
    def _valid_hash(self, msg_hash):
        """
        Verify that SHA-1, SHA-2 or SHA-3 are used
        """
def FipsEcDsaSigScheme(DssSigScheme):
    """
    Verify that SHA-[23] (256|384|512) bits are used to
            match the security of P-256 (128 bits), P-384 (192 bits)
            or P-521 (256 bits)
    """
def new(key, mode, encoding='binary', randfunc=None):
    """
    Create a signature object :class:`DSS_SigScheme` that
        can perform (EC)DSA signature or verification.

        .. note::
            Refer to `NIST SP 800 Part 1 Rev 4`_ (or newer release) for an
            overview of the recommended key lengths.

        :parameter key:
            The key to use for computing the signature (*private* keys only)
            or verifying one: it must be either
            :class:`Crypto.PublicKey.DSA` or :class:`Crypto.PublicKey.ECC`.

            For DSA keys, let ``L`` and ``N`` be the bit lengths of the modulus ``p``
            and of ``q``: the pair ``(L,N)`` must appear in the following list,
            in compliance to section 4.2 of `FIPS 186-4`_:

            - (1024, 160) *legacy only; do not create new signatures with this*
            - (2048, 224) *deprecated; do not create new signatures with this*
            - (2048, 256)
            - (3072, 256)

            For ECC, only keys over P-256, P384, and P-521 are accepted.
        :type key:
            a key object

        :parameter mode:
            The parameter can take these values:

            - *'fips-186-3'*. The signature generation is randomized and carried out
              according to `FIPS 186-3`_: the nonce ``k`` is taken from the RNG.
            - *'deterministic-rfc6979'*. The signature generation is not
              randomized. See RFC6979_.
        :type mode:
            string

        :parameter encoding:
            How the signature is encoded. This value determines the output of
            :meth:`sign` and the input to :meth:`verify`.

            The following values are accepted:

            - *'binary'* (default), the signature is the raw concatenation
              of ``r`` and ``s``. It is defined in the IEEE P.1363 standard.

              For DSA, the size in bytes of the signature is ``N/4`` bytes
              (e.g. 64 for ``N=256``).

              For ECDSA, the signature is always twice the length of a point
              coordinate (e.g. 64 bytes for P-256).

            - *'der'*, the signature is a ASN.1 DER SEQUENCE
              with two INTEGERs (``r`` and ``s``). It is defined in RFC3279_.
              The size of the signature is variable.
        :type encoding: string

        :parameter randfunc:
            A function that returns random *byte strings*, of a given length.
            If omitted, the internal RNG is used.
            Only applicable for the *'fips-186-3'* mode.
        :type randfunc: callable

        .. _FIPS 186-3: http://csrc.nist.gov/publications/fips/fips186-3/fips_186-3.pdf
        .. _FIPS 186-4: http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf
        .. _NIST SP 800 Part 1 Rev 4: http://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-57pt1r4.pdf
        .. _RFC6979: http://tools.ietf.org/html/rfc6979
        .. _RFC3279: https://tools.ietf.org/html/rfc3279#section-2.2.2
    
    """
