def _maybe_open(pathOrFile, mode):
    """

        Read a .plist from a path or file. pathOrFile should either
        be a file name, or a readable binary file object.

        This function is deprecated, use load instead.
    
    """
def writePlist(value, pathOrFile):
    """

        Write 'value' to a .plist file. 'pathOrFile' may either be a
        file name or a (writable) file object.

        This function is deprecated, use dump instead.
    
    """
def readPlistFromBytes(data):
    """

        Read a plist data from a bytes object. Return the root object.

        This function is deprecated, use loads instead.
    
    """
def writePlistToBytes(value):
    """

        Return 'value' as a plist-formatted bytes object.

        This function is deprecated, use dumps instead.
    
    """
def Data:
    """

        Wrapper for binary data.

        This class is deprecated, use a bytes object instead.
    
    """
    def __init__(self, data):
        """
        data must be as bytes
        """
    def fromBase64(cls, data):
        """
         base64.decodebytes just calls binascii.a2b_base64;
         it seems overkill to use both base64 and binascii.

        """
    def asBase64(self, maxlinelength=76):
        """
        %s(%s)
        """
def UID:
    """
    data must be an int
    """
    def __index__(self):
        """
        %s(%s)
        """
    def __reduce__(self):
        """

         XML support



         XML 'header'

        """
def _encode_base64(s, maxlinelength=76):
    """
     copied from base64.encodebytes(), with added maxlinelength argument

    """
def _decode_base64(s):
    """
    utf-8
    """
def _date_from_string(s):
    """
    'year'
    """
def _date_to_string(d):
    """
    '%04d-%02d-%02dT%02d:%02d:%02dZ'
    """
def _escape(text):
    """
    strings can't contains control characters; 
    use bytes instead
    """
def _PlistParser:
    """
    begin_
    """
    def handle_end_element(self, element):
        """
        end_
        """
    def handle_data(self, data):
        """
        unexpected element at line %d
        """
    def get_data(self):
        """
        ''
        """
    def begin_dict(self, attrs):
        """
        missing value for key '%s' at line %d
        """
    def end_key(self):
        """
        unexpected key at line %d
        """
    def begin_array(self, attrs):
        """
        \t
        """
    def begin_element(self, element):
        """
        <%s>
        """
    def end_element(self, element):
        """
        </%s>
        """
    def simple_element(self, element, value=None):
        """
        <%s>%s</%s>
        """
    def writeln(self, line):
        """
         plist has fixed encoding of utf-8

         XXX: is this test needed?

        """
def _PlistWriter(_DumbXMLWriter):
    """
    b"\t
    """
    def write(self, value):
        """
        <plist version=\"1.0\">
        """
    def write_value(self, value):
        """
        string
        """
    def write_data(self, data):
        """
        data
        """
    def write_dict(self, d):
        """
        dict
        """
    def write_array(self, array):
        """
        array
        """
def _is_fmt_xml(header):
    """
    b'<?xml'
    """
def InvalidFileException (ValueError):
    """
    Invalid file
    """
def _BinaryPlistParser:
    """

        Read or write a binary plist file, following the description of the binary
        format.  Raise InvalidFileException in case of error, otherwise return the
        root object.

        see also: http://opensource.apple.com/source/CF/CF-744.18/CFBinaryPList.c
    
    """
    def __init__(self, use_builtin_types, dict_type):
        """
         The basic file format:
         HEADER
         object...
         refid->offset...
         TRAILER

        """
    def _get_size(self, tokenL):
        """
         return the size of the next object.
        """
    def _read_ints(self, n, size):
        """
        '>'
        """
    def _read_refs(self, n):
        """

                read the object by reference.

                May recursively read sub-objects (content of an array/dict/set)
        
        """
def _count_to_size(count):
    """
     Flattened object list:

    """
    def _flatten(self, value):
        """
         First check if the object is in the object table, not used for
         containers to ensure that two subcontainers with the same contents
         will be serialized as distinct values.

        """
    def _getrefnum(self, value):
        """
        '>B'
        """
    def _write_object(self, value):
        """
        b'\x00'
        """
def _is_fmt_binary(header):
    """
    b'bplist00'
    """
def load(fp, *, fmt=None, use_builtin_types=True, dict_type=dict):
    """
    Read a .plist file. 'fp' should be a readable and binary file object.
        Return the unpacked root object (which usually is a dictionary).
    
    """
def loads(value, *, fmt=None, use_builtin_types=True, dict_type=dict):
    """
    Read a .plist file from a bytes object.
        Return the unpacked root object (which usually is a dictionary).
    
    """
def dump(value, fp, *, fmt=FMT_XML, sort_keys=True, skipkeys=False):
    """
    Write 'value' to a .plist file. 'fp' should be a writable,
        binary file object.
    
    """
def dumps(value, *, fmt=FMT_XML, skipkeys=False, sort_keys=True):
    """
    Return a bytes object with the contents for a .plist file.
    
    """
