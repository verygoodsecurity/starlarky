def _VoidPointer(object):
    """
    Return the memory location we point to
    """
    def address_of(self):
        """
        Return a raw pointer to this pointer
        """
    def load_lib(name, cdecl):
        """
        Load a shared library and return a handle to it.

                @name,  either an absolute path or the name of a library
                        in the system search path.

                @cdecl, the C function declarations.
        
        """
    def c_ulong(x):
        """
        Convert a Python integer to unsigned long
        """
    def c_size_t(x):
        """
        Convert a Python integer to size_t
        """
    def create_string_buffer(init_or_size, size=None):
        """
        Allocate the given amount of bytes (initially set to 0)
        """
    def get_c_string(c_string):
        """
        Convert a C string into a Python byte sequence
        """
    def get_raw_buffer(buf):
        """
        Convert a C buffer into a Python byte sequence
        """
    def c_uint8_ptr(data):
        """
         This only works for cffi >= 1.7

        """
    def VoidPointer_cffi(_VoidPointer):
    """
    Model a newly allocated pointer to void
    """
        def __init__(self):
            """
            void *[1]
            """
        def get(self):
            """
            cffi
            """
    def load_lib(name, cdecl):
        """
         platform.architecture() creates a subprocess, so caching the
         result makes successive imports faster.

        """
    def get_c_string(c_string):
        """
         ---- Get raw pointer ---


        """
    def _Py_buffer(ctypes.Structure):
    """
    'buf'
    """
    def c_uint8_ptr(data):
        """
        Object type %s cannot be passed to C code
        """
    def VoidPointer_ctypes(_VoidPointer):
    """
    Model a newly allocated pointer to void
    """
        def __init__(self):
            """
            ctypes
            """
def SmartPointer(object):
    """
    Class to hold a non-managed piece of memory
    """
    def __init__(self, raw_pointer, destructor):
        """
        Load a shared library and return a handle to it.

            @name,  the name of the library expressed as a PyCryptodome module,
                    for instance Crypto.Cipher._raw_cbc.

            @cdecl, the C function declarations.
    
        """
def is_buffer(x):
    """
    Return True if object x supports the buffer interface
    """
