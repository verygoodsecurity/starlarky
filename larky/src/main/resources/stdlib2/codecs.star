def CodecInfo(tuple):
    """
    Codec details when looking up the codec registry
    """
2021-03-02 20:46:57,003 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:57,004 : INFO : tokenize_signature : --> do i ever get here?
    def __new__(cls, encode, decode, streamreader=None, streamwriter=None,
        incrementalencoder=None, incrementaldecoder=None, name=None,
        *, _is_text_encoding=None):
        """
        <%s.%s object for encoding %s at %#x>
        """
def Codec:
    """
     Defines the interface for stateless encoders/decoders.

            The .encode()/.decode() methods may use different error
            handling schemes by providing the errors argument. These
            string values are predefined:

             'strict' - raise a ValueError error (or a subclass)
             'ignore' - ignore the character and continue with the next
             'replace' - replace with a suitable replacement character;
                        Python will use the official U+FFFD REPLACEMENT
                        CHARACTER for the builtin Unicode codecs on
                        decoding and '?' on encoding.
             'surrogateescape' - replace with private code points U+DCnn.
             'xmlcharrefreplace' - Replace with the appropriate XML
                                   character reference (only for encoding).
             'backslashreplace'  - Replace with backslashed escape sequences.
             'namereplace'       - Replace with \\N{...} escape sequences
                                   (only for encoding).

            The set of allowed values can be extended via register_error.

    
    """
    def encode(self, input, errors='strict'):
        """
         Encodes the object input and returns a tuple (output
                    object, length consumed).

                    errors defines the error handling to apply. It defaults to
                    'strict' handling.

                    The method may not store state in the Codec instance. Use
                    StreamWriter for codecs which have to keep state in order to
                    make encoding efficient.

                    The encoder must be able to handle zero length input and
                    return an empty object of the output object type in this
                    situation.

        
        """
    def decode(self, input, errors='strict'):
        """
         Decodes the object input and returns a tuple (output
                    object, length consumed).

                    input must be an object which provides the bf_getreadbuf
                    buffer slot. Python strings, buffer objects and memory
                    mapped files are examples of objects providing this slot.

                    errors defines the error handling to apply. It defaults to
                    'strict' handling.

                    The method may not store state in the Codec instance. Use
                    StreamReader for codecs which have to keep state in order to
                    make decoding efficient.

                    The decoder must be able to handle zero length input and
                    return an empty object of the output object type in this
                    situation.

        
        """
def IncrementalEncoder(object):
    """

        An IncrementalEncoder encodes an input in multiple steps. The input can
        be passed piece by piece to the encode() method. The IncrementalEncoder
        remembers the state of the encoding process between calls to encode().
    
    """
    def __init__(self, errors='strict'):
        """

                Creates an IncrementalEncoder instance.

                The IncrementalEncoder may use different error handling schemes by
                providing the errors keyword argument. See the module docstring
                for a list of possible values.
        
        """
    def encode(self, input, final=False):
        """

                Encodes input and returns the resulting object.
        
        """
    def reset(self):
        """

                Resets the encoder to the initial state.
        
        """
    def getstate(self):
        """

                Return the current state of the encoder.
        
        """
    def setstate(self, state):
        """

                Set the current state of the encoder. state must have been
                returned by getstate().
        
        """
def BufferedIncrementalEncoder(IncrementalEncoder):
    """

        This subclass of IncrementalEncoder can be used as the baseclass for an
        incremental encoder if the encoder must keep some of the output in a
        buffer between calls to encode().
    
    """
    def __init__(self, errors='strict'):
        """
         unencoded input that is kept between calls to encode()

        """
    def _buffer_encode(self, input, errors, final):
        """
         Overwrite this method in subclasses: It must encode input
         and return an (output, length consumed) tuple

        """
    def encode(self, input, final=False):
        """
         encode input (taking the buffer into account)

        """
    def reset(self):
        """

        """
    def getstate(self):
        """

        """
def IncrementalDecoder(object):
    """

        An IncrementalDecoder decodes an input in multiple steps. The input can
        be passed piece by piece to the decode() method. The IncrementalDecoder
        remembers the state of the decoding process between calls to decode().
    
    """
    def __init__(self, errors='strict'):
        """

                Create an IncrementalDecoder instance.

                The IncrementalDecoder may use different error handling schemes by
                providing the errors keyword argument. See the module docstring
                for a list of possible values.
        
        """
    def decode(self, input, final=False):
        """

                Decode input and returns the resulting object.
        
        """
    def reset(self):
        """

                Reset the decoder to the initial state.
        
        """
    def getstate(self):
        """

                Return the current state of the decoder.

                This must be a (buffered_input, additional_state_info) tuple.
                buffered_input must be a bytes object containing bytes that
                were passed to decode() that have not yet been converted.
                additional_state_info must be a non-negative integer
                representing the state of the decoder WITHOUT yet having
                processed the contents of buffered_input.  In the initial state
                and after reset(), getstate() must return (b"", 0).
        
        """
    def setstate(self, state):
        """

                Set the current state of the decoder.

                state must have been returned by getstate().  The effect of
                setstate((b"", 0)) must be equivalent to reset().
        
        """
def BufferedIncrementalDecoder(IncrementalDecoder):
    """

        This subclass of IncrementalDecoder can be used as the baseclass for an
        incremental decoder if the decoder must be able to handle incomplete
        byte sequences.
    
    """
    def __init__(self, errors='strict'):
        """
         undecoded input that is kept between calls to decode()

        """
    def _buffer_decode(self, input, errors, final):
        """
         Overwrite this method in subclasses: It must decode input
         and return an (output, length consumed) tuple

        """
    def decode(self, input, final=False):
        """
         decode input (taking the buffer into account)

        """
    def reset(self):
        """
        b
        """
    def getstate(self):
        """
         additional state info is always 0

        """
    def setstate(self, state):
        """
         ignore additional state info

        """
def StreamWriter(Codec):
    """
    'strict'
    """
    def write(self, object):
        """
         Writes the object's contents encoded to self.stream.
        
        """
    def writelines(self, list):
        """
         Writes the concatenated list of strings to the stream
                    using .write().
        
        """
    def reset(self):
        """
         Flushes and resets the codec buffers used for keeping state.

                    Calling this method should ensure that the data on the
                    output is put into a clean state, that allows appending
                    of new fresh data without having to rescan the whole
                    stream to recover state.

        
        """
    def seek(self, offset, whence=0):
        """
         Inherit all other methods from the underlying stream.
        
        """
    def __enter__(self):
        """



        """
def StreamReader(Codec):
    """
    'strict'
    """
    def decode(self, input, errors='strict'):
        """
         Decodes data from the stream self.stream and returns the
                    resulting object.

                    chars indicates the number of decoded code points or bytes to
                    return. read() will never return more data than requested,
                    but it might return less, if there is not enough available.

                    size indicates the approximate maximum number of decoded
                    bytes or code points to read for decoding. The decoder
                    can modify this setting as appropriate. The default value
                    -1 indicates to read and decode as much as possible.  size
                    is intended to prevent having to decode huge files in one
                    step.

                    If firstline is true, and a UnicodeDecodeError happens
                    after the first line terminator in the input only the first line
                    will be returned, the rest of the input will be kept until the
                    next call to read().

                    The method should use a greedy read strategy, meaning that
                    it should read as much data as is allowed within the
                    definition of the encoding and the given size, e.g.  if
                    optional encoding endings or state markers are available
                    on the stream, these should be read too.
        
        """
    def readline(self, size=None, keepends=True):
        """
         Read one line from the input stream and return the
                    decoded data.

                    size, if given, is passed as size argument to the
                    read() method.

        
        """
    def readlines(self, sizehint=None, keepends=True):
        """
         Read all lines available on the input stream
                    and return them as a list.

                    Line breaks are implemented using the codec's decoder
                    method and are included in the list entries.

                    sizehint, if given, is ignored since there is no efficient
                    way to finding the true end-of-line.

        
        """
    def reset(self):
        """
         Resets the codec buffers used for keeping state.

                    Note that no stream repositioning should take place.
                    This method is primarily intended to be able to recover
                    from decoding errors.

        
        """
    def seek(self, offset, whence=0):
        """
         Set the input stream's current position.

                    Resets the codec buffers used for keeping state.
        
        """
    def __next__(self):
        """
         Return the next decoded line from the input stream.
        """
    def __iter__(self):
        """
         Inherit all other methods from the underlying stream.
        
        """
    def __enter__(self):
        """



        """
def StreamReaderWriter:
    """
     StreamReaderWriter instances allow wrapping streams which
            work in both read and write modes.

            The design is such that one can use the factory functions
            returned by the codec.lookup() function to construct the
            instance.

    
    """
    def __init__(self, stream, Reader, Writer, errors='strict'):
        """
         Creates a StreamReaderWriter instance.

                    stream must be a Stream-like object.

                    Reader, Writer must be factory functions or classes
                    providing the StreamReader, StreamWriter interface resp.

                    Error handling is done in the same way as defined for the
                    StreamWriter/Readers.

        
        """
    def read(self, size=-1):
        """
         Return the next decoded line from the input stream.
        """
    def __iter__(self):
        """
         Inherit all other methods from the underlying stream.
        
        """
    def __enter__(self):
        """



        """
def StreamRecoder:
    """
     StreamRecoder instances translate data from one encoding to another.

            They use the complete set of APIs returned by the
            codecs.lookup() function to implement their task.

            Data written to the StreamRecoder is first decoded into an
            intermediate format (depending on the "decode" codec) and then
            written to the underlying stream using an instance of the provided
            Writer class.

            In the other direction, data is read from the underlying stream using
            a Reader instance and then encoded and returned to the caller.

    
    """
2021-03-02 20:46:57,015 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, stream, encode, decode, Reader, Writer,
                 errors='strict'):
        """
         Creates a StreamRecoder instance which implements a two-way
                    conversion: encode and decode work on the frontend (the
                    data visible to .read() and .write()) while Reader and Writer
                    work on the backend (the data in stream).

                    You can use these objects to do transparent
                    transcodings from e.g. latin-1 to utf-8 and back.

                    stream must be a file-like object.

                    encode and decode must adhere to the Codec interface; Reader and
                    Writer must be factory functions or classes providing the
                    StreamReader and StreamWriter interfaces resp.

                    Error handling is done in the same way as defined for the
                    StreamWriter/Readers.

        
        """
    def read(self, size=-1):
        """
         Return the next decoded line from the input stream.
        """
    def __iter__(self):
        """
        b''
        """
    def reset(self):
        """
         Seeks must be propagated to both the readers and writers
         as they might need to reset their internal buffers.

        """
2021-03-02 20:46:57,017 : INFO : tokenize_signature : --> do i ever get here?
    def __getattr__(self, name,
                    getattr=getattr):
        """
         Inherit all other methods from the underlying stream.
        
        """
    def __enter__(self):
        """
         Shortcuts


        """
def open(filename, mode='r', encoding=None, errors='strict', buffering=-1):
    """
     Open an encoded file using the given mode and return
            a wrapped version providing transparent encoding/decoding.

            Note: The wrapped version will only accept the object format
            defined by the codecs, i.e. Unicode objects for most builtin
            codecs. Output is also codec dependent and will usually be
            Unicode as well.

            Underlying encoded files are always opened in binary mode.
            The default file mode is 'r', meaning to open the file in read mode.

            encoding specifies the encoding which is to be used for the
            file.

            errors may be given to define the error handling. It defaults
            to 'strict' which causes ValueErrors to be raised in case an
            encoding error occurs.

            buffering has the same meaning as for the builtin open() API.
            It defaults to -1 which means that the default buffer size will
            be used.

            The returned wrapped file object provides an extra attribute
            .encoding which allows querying the used encoding. This
            attribute is only available if an encoding was specified as
            parameter.

    
    """
def EncodedFile(file, data_encoding, file_encoding=None, errors='strict'):
    """
     Return a wrapped version of file which provides transparent
            encoding translation.

            Data written to the wrapped file is decoded according
            to the given data_encoding and then encoded to the underlying
            file using file_encoding. The intermediate data type
            will usually be Unicode but depends on the specified codecs.

            Bytes read from the file are decoded using file_encoding and then
            passed back to the caller encoded using data_encoding.

            If file_encoding is not given, it defaults to data_encoding.

            errors may be given to define the error handling. It defaults
            to 'strict' which causes ValueErrors to be raised in case an
            encoding error occurs.

            The returned wrapped file object provides two extra attributes
            .data_encoding and .file_encoding which reflect the given
            parameters of the same name. The attributes can be used for
            introspection by Python programs.

    
    """
def getencoder(encoding):
    """
     Lookup up the codec for the given encoding and return
            its encoder function.

            Raises a LookupError in case the encoding cannot be found.

    
    """
def getdecoder(encoding):
    """
     Lookup up the codec for the given encoding and return
            its decoder function.

            Raises a LookupError in case the encoding cannot be found.

    
    """
def getincrementalencoder(encoding):
    """
     Lookup up the codec for the given encoding and return
            its IncrementalEncoder class or factory function.

            Raises a LookupError in case the encoding cannot be found
            or the codecs doesn't provide an incremental encoder.

    
    """
def getincrementaldecoder(encoding):
    """
     Lookup up the codec for the given encoding and return
            its IncrementalDecoder class or factory function.

            Raises a LookupError in case the encoding cannot be found
            or the codecs doesn't provide an incremental decoder.

    
    """
def getreader(encoding):
    """
     Lookup up the codec for the given encoding and return
            its StreamReader class or factory function.

            Raises a LookupError in case the encoding cannot be found.

    
    """
def getwriter(encoding):
    """
     Lookup up the codec for the given encoding and return
            its StreamWriter class or factory function.

            Raises a LookupError in case the encoding cannot be found.

    
    """
def iterencode(iterator, encoding, errors='strict', **kwargs):
    """

        Encoding iterator.

        Encodes the input strings from the iterator using an IncrementalEncoder.

        errors and kwargs are passed through to the IncrementalEncoder
        constructor.
    
    """
def iterdecode(iterator, encoding, errors='strict', **kwargs):
    """

        Decoding iterator.

        Decodes the input strings from the iterator using an IncrementalDecoder.

        errors and kwargs are passed through to the IncrementalDecoder
        constructor.
    
    """
def make_identity_dict(rng):
    """
     make_identity_dict(rng) -> dict

            Return a dictionary where elements of the rng sequence are
            mapped to themselves.

    
    """
def make_encoding_map(decoding_map):
    """
     Creates an encoding map from a decoding map.

            If a target mapping in the decoding map occurs multiple
            times, then that target is mapped to None (undefined mapping),
            causing an exception when encountered by the charmap codec
            during translation.

            One example where this happens is cp875.py which decodes
            multiple character to \\u001a.

    
    """
