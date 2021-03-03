def _key2bin(s):
    """
    Convert a key into a string of binary digits
    """
def _extract(key, start, length):
    """
    Extract a bitstring(2.x)/bytestring(2.x) from a string of binary digits, and return its
        numeric value.
    """
def key_to_english(key):
    """
    Transform an arbitrary key into a string containing English words.

        Example::

            >>> from Crypto.Util.RFC1751 import key_to_english
            >>> key_to_english(b'66666666')
            'RAM LOIS GOAD CREW CARE HIT'

        Args:
          key (byte string):
            The key to convert. Its length must be a multiple of 8.
        Return:
          A string of English words.
    
    """
def english_to_key(s):
    """
    Transform a string into a corresponding key.

        Example::

            >>> from Crypto.Util.RFC1751 import english_to_key
            >>> english_to_key('RAM LOIS GOAD CREW CARE HIT')
            b'66666666'

        Args:
          s (string): the string with the words separated by whitespace;
                      the number of words must be a multiple of 6.
        Return:
          A byte string.
    
    """
