def PKCS115_Cipher:
    """
    This cipher can perform PKCS#1 v1.5 RSA encryption or decryption.
        Do not instantiate directly. Use :func:`Crypto.Cipher.PKCS1_v1_5.new` instead.
    """
    def __init__(self, key, randfunc):
        """
        Initialize this PKCS#1 v1.5 cipher object.

                :Parameters:
                 key : an RSA key object
                  If a private half is given, both encryption and decryption are possible.
                  If a public half is given, only encryption is possible.
                 randfunc : callable
                  Function that returns random bytes.
        
        """
    def can_encrypt(self):
        """
        Return True if this cipher object can be used for encryption.
        """
    def can_decrypt(self):
        """
        Return True if this cipher object can be used for decryption.
        """
    def encrypt(self, message):
        """
        Produce the PKCS#1 v1.5 encryption of a message.

                This function is named ``RSAES-PKCS1-V1_5-ENCRYPT``, and it is specified in
                `section 7.2.1 of RFC8017
                <https://tools.ietf.org/html/rfc8017#page-28>`_.

                :param message:
                    The message to encrypt, also known as plaintext. It can be of
                    variable length, but not longer than the RSA modulus (in bytes) minus 11.
                :type message: bytes/bytearray/memoryview

                :Returns: A byte string, the ciphertext in which the message is encrypted.
                    It is as long as the RSA modulus (in bytes).

                :Raises ValueError:
                    If the RSA key length is not sufficiently long to deal with the given
                    message.
        
        """
    def decrypt(self, ciphertext, sentinel):
        """
        r"""Decrypt a PKCS#1 v1.5 ciphertext.

                This function is named ``RSAES-PKCS1-V1_5-DECRYPT``, and is specified in
                `section 7.2.2 of RFC8017
                <https://tools.ietf.org/html/rfc8017#page-29>`_.

                :param ciphertext:
                    The ciphertext that contains the message to recover.
                :type ciphertext: bytes/bytearray/memoryview

                :param sentinel:
                    The object to return whenever an error is detected.
                :type sentinel: any type

                :Returns: A byte string. It is either the original message or the ``sentinel`` (in case of an error).

                :Raises ValueError:
                    If the ciphertext length is incorrect
                :Raises TypeError:
                    If the RSA key has no private half (i.e. it cannot be used for
                    decyption).

                .. warning::
                    You should **never** let the party who submitted the ciphertext know that
                    this function returned the ``sentinel`` value.
                    Armed with such knowledge (for a fair amount of carefully crafted but invalid ciphertexts),
                    an attacker is able to recontruct the plaintext of any other encryption that were carried out
                    with the same RSA public key (see `Bleichenbacher's`__ attack).

                    In general, it should not be possible for the other party to distinguish
                    whether processing at the server side failed because the value returned
                    was a ``sentinel`` as opposed to a random, invalid message.

                    In fact, the second option is not that unlikely: encryption done according to PKCS#1 v1.5
                    embeds no good integrity check. There is roughly one chance
                    in 2\ :sup:`16` for a random ciphertext to be returned as a valid message
                    (although random looking).

                    It is therefore advisabled to:

                    1. Select as ``sentinel`` a value that resembles a plausable random, invalid message.
                    2. Not report back an error as soon as you detect a ``sentinel`` value.
                       Put differently, you should not explicitly check if the returned value is the ``sentinel`` or not.
                    3. Cover all possible errors with a single, generic error indicator.
                    4. Embed into the definition of ``message`` (at the protocol level) a digest (e.g. ``SHA-1``).
                       It is recommended for it to be the rightmost part ``message``.
                    5. Where possible, monitor the number of errors due to ciphertexts originating from the same party,
                       and slow down the rate of the requests from such party (or even blacklist it altogether).

                    **If you are designing a new protocol, consider using the more robust PKCS#1 OAEP.**

                    .. __: http://www.bell-labs.com/user/bleichen/papers/pkcs.ps

        
        """
def new(key, randfunc=None):
    """
    Create a cipher for performing PKCS#1 v1.5 encryption or decryption.

        :param key:
          The key to use to encrypt or decrypt the message. This is a `Crypto.PublicKey.RSA` object.
          Decryption is only possible if *key* is a private RSA key.
        :type key: RSA key object

        :param randfunc:
          Function that return random bytes.
          The default is :func:`Crypto.Random.get_random_bytes`.
        :type randfunc: callable

        :returns: A cipher object `PKCS115_Cipher`.
    
    """
