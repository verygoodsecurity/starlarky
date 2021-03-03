def Error(Exception):
    """
    Exception class for this module. Use:

        except xdrlib.Error as var:
            # var has the Error instance for the exception

        Public ivars:
            msg -- contains the message

    
    """
    def __init__(self, msg):
        """
         Wrap any raised struct.errors in a ConversionError. 
        """
    def result(self, value):
        """
        Pack various data representations into a buffer.
        """
    def __init__(self):
        """
         backwards compatibility

        """
    def pack_uint(self, x):
        """
        '>L'
        """
    def pack_int(self, x):
        """
        '>l'
        """
    def pack_bool(self, x):
        """
        b'\0\0\0\1'
        """
    def pack_uhyper(self, x):
        """
        '>f'
        """
    def pack_double(self, x):
        """
        '>d'
        """
    def pack_fstring(self, n, s):
        """
        'fstring size must be nonnegative'
        """
    def pack_string(self, s):
        """
        'wrong array size'
        """
    def pack_array(self, list, pack_item):
        """
        Unpacks various data representations from the given buffer.
        """
    def __init__(self, data):
        """
        'unextracted data remains'
        """
    def unpack_uint(self):
        """
        '>L'
        """
    def unpack_int(self):
        """
        '>l'
        """
    def unpack_bool(self):
        """
        '>f'
        """
    def unpack_double(self):
        """
        '>d'
        """
    def unpack_fstring(self, n):
        """
        'fstring size must be nonnegative'
        """
    def unpack_string(self):
        """
        '0 or 1 expected, got %r'
        """
    def unpack_farray(self, n, unpack_item):
