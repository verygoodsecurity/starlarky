def create_string_buffer(init, size=None):
    """
    create_string_buffer(aBytes) -> character array
        create_string_buffer(anInteger) -> character array
        create_string_buffer(aBytes, anInteger) -> character array
    
    """
def c_buffer(init, size=None):
    """
        "deprecated, use create_string_buffer instead
        import warnings
        warnings.warn("c_buffer is deprecated, use create_string_buffer instead",
                      DeprecationWarning, stacklevel=2)

    """
def CFUNCTYPE(restype, *argtypes, **kw):
    """
    CFUNCTYPE(restype, *argtypes,
                     use_errno=False, use_last_error=False) -> function prototype.

        restype: the result type
        argtypes: a sequence specifying the argument types

        The function prototype can be called in different ways to create a
        callable object:

        prototype(integer address) -> foreign function
        prototype(callable) -> create and return a C callable function from callable
        prototype(integer index, method name[, paramflags]) -> foreign function calling a COM method
        prototype((ordinal number, dll object)[, paramflags]) -> foreign function exported by ordinal
        prototype((function name, dll object)[, paramflags]) -> foreign function exported by name
    
    """
        def CFunctionType(_CFuncPtr):
    """
    nt
    """
    def WINFUNCTYPE(restype, *argtypes, **kw):
        """
         docstring set later (very similar to CFUNCTYPE.__doc__)

        """
            def WinFunctionType(_CFuncPtr):
    """
    CFUNCTYPE
    """
def _check_size(typ, typecode=None):
    """
     Check if sizeof(ctypes_type) against struct.calcsize.  This
     should protect somewhat against a misconfigured libffi.

    """
def py_object(_SimpleCData):
    """
    O
    """
    def __repr__(self):
        """
        %s(<NULL>)
        """
def c_short(_SimpleCData):
    """
    h
    """
def c_ushort(_SimpleCData):
    """
    H
    """
def c_long(_SimpleCData):
    """
    l
    """
def c_ulong(_SimpleCData):
    """
    L
    """
    def c_int(_SimpleCData):
    """
    i
    """
    def c_uint(_SimpleCData):
    """
    I
    """
def c_float(_SimpleCData):
    """
    f
    """
def c_double(_SimpleCData):
    """
    d
    """
def c_longdouble(_SimpleCData):
    """
    g
    """
    def c_longlong(_SimpleCData):
    """
    q
    """
    def c_ulonglong(_SimpleCData):
    """
    Q
    """
def c_ubyte(_SimpleCData):
    """
    B
    """
def c_byte(_SimpleCData):
    """
    b
    """
def c_char(_SimpleCData):
    """
    c
    """
def c_char_p(_SimpleCData):
    """
    z
    """
    def __repr__(self):
        """
        %s(%s)
        """
def c_void_p(_SimpleCData):
    """
    P
    """
def c_bool(_SimpleCData):
    """
    ?
    """
def c_wchar_p(_SimpleCData):
    """
    Z
    """
    def __repr__(self):
        """
        %s(%s)
        """
def c_wchar(_SimpleCData):
    """
    u
    """
def _reset_cache():
    """
    nt
    """
def create_unicode_buffer(init, size=None):
    """
    create_unicode_buffer(aString) -> character array
        create_unicode_buffer(anInteger) -> character array
        create_unicode_buffer(aString, anInteger) -> character array
    
    """
def SetPointerType(pointer, cls):
    """
    This type already exists in the cache
    """
def ARRAY(typ, len):
    """




    """
def CDLL(object):
    """
    An instance of this class represents a loaded dll/shared
        library, exporting functions using the standard C calling
        convention (named 'cdecl' on Windows).

        The exported functions can be accessed as attributes, or by
        indexing with the function name.  Examples:

        <obj>.qsort -> callable object
        <obj>['qsort'] -> callable object

        Calling the functions releases the Python GIL during the call and
        reacquires it afterwards.
    
    """
2021-03-02 20:46:38,229 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:38,229 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:38,229 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, name, mode=DEFAULT_MODE, handle=None,
                 use_errno=False,
                 use_last_error=False,
                 winmode=None):
        """
        aix
        """
        def _FuncPtr(_CFuncPtr):
    """
    <%s '%s', handle %x at %#x>
    """
    def __getattr__(self, name):
        """
        '__'
        """
    def __getitem__(self, name_or_ordinal):
        """
        This class represents the Python library itself.  It allows
            accessing Python API functions.  The GIL is not released, and
            Python exceptions are handled correctly.
    
        """
    def WinDLL(CDLL):
    """
    This class represents a dll exporting functions using the
            Windows stdcall calling convention.
        
    """
    def HRESULT(_SimpleCData):
    """
    l
    """
    def OleDLL(CDLL):
    """
    This class represents a dll exporting functions using the
            Windows stdcall calling convention, and returning HRESULT.
            HRESULT error values are automatically raised as OSError
            exceptions.
        
    """
def LibraryLoader(object):
    """
    '_'
    """
    def __getitem__(self, name):
        """
        nt
        """
    def WinError(code=None, descr=None):
        """
         functions


        """
def PYFUNCTYPE(restype, *argtypes):
    """
    string_at(addr[, size]) -> string

        Return the string at addr.
    """
    def wstring_at(ptr, size=-1):
        """
        wstring_at(addr[, size]) -> string

                Return the string at addr.
        """
    def DllGetClassObject(rclsid, riid, ppv):
        """
        comtypes.server.inprocserver
        """
    def DllCanUnloadNow():
        """
        comtypes.server.inprocserver
        """
