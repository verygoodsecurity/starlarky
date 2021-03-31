def _create_base_cipher(dict_parameters):
    """
    This method instantiates and returns a smart pointer to
        a low-level base cipher. It will absorb named parameters in
        the process.
    """
def new(key, mode, salt, cost, invert):
    """
    Create a new EKSBlowfish cipher
    
        Args:

          key (bytes, bytearray, memoryview):
            The secret key to use in the symmetric cipher.
            Its length can vary from 0 to 72 bytes.

          mode (one of the supported ``MODE_*`` constants):
            The chaining mode to use for encryption or decryption.

          salt (bytes, bytearray, memoryview):
            The salt that bcrypt uses to thwart rainbow table attacks

          cost (integer):
            The complexity factor in bcrypt

          invert (bool):
            If ``False``, in the inner loop use ``ExpandKey`` first over the salt
            and then over the key, as defined in
            the `original bcrypt specification <https://www.usenix.org/legacy/events/usenix99/provos/provos_html/node4.html>`_.
            If ``True``, reverse the order, as in the first implementation of
            `bcrypt` in OpenBSD.

        :Return: an EKSBlowfish object
    
    """
