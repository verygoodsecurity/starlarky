def PickleError(Exception):
    """
    A common base class for the other pickling exceptions.
    """
def PicklingError(PickleError):
    """
    This exception is raised when an unpicklable object is passed to the
        dump() method.

    
    """
def UnpicklingError(PickleError):
    """
    This exception is raised when there is a problem unpickling an object,
        such as a security violation.

        Note that other exceptions may also be raised during unpickling, including
        (but not necessarily limited to) AttributeError, EOFError, ImportError,
        and IndexError.

    
    """
def _Stop(Exception):
    """
     Jython has PyStringMap; it's a dict subclass with string keys

    """
def _Framer:
    """
     Issue a single call to the write method of the underlying
     file object for the frame opcode with the size of the
     frame. The concatenation is expected to be less expensive
     than issuing an additional call to write.

    """
    def write(self, data):
        """
         Terminate the current frame and flush it to the file.

        """
def _Unframer:
    """
    pickle exhausted before end of frame
    """
    def read(self, n):
        """
        pickle exhausted before end of frame
        """
    def readline(self):
        """
        b'\n'
        """
    def load_frame(self, frame_size):
        """
        b''
        """
def _getattribute(obj, name):
    """
    '.'
    """
def whichmodule(obj, name):
    """
    Find the module an object belong to.
    """
def encode_long(x):
    """
    r"""Encode a long to a two's complement little-endian binary string.
        Note that 0 is a special case, returning an empty string, to save a
        byte in the LONG1 pickling context.

        >>> encode_long(0)
        b''
        >>> encode_long(255)
        b'\xff\x00'
        >>> encode_long(32767)
        b'\xff\x7f'
        >>> encode_long(-256)
        b'\x00\xff'
        >>> encode_long(-32768)
        b'\x00\x80'
        >>> encode_long(-128)
        b'\x80'
        >>> encode_long(127)
        b'\x7f'
        >>>
    
    """
def decode_long(data):
    """
    r"""Decode a long from a two's complement little-endian binary string.

        >>> decode_long(b'')
        0
        >>> decode_long(b"\xff\x00")
        255
        >>> decode_long(b"\xff\x7f")
        32767
        >>> decode_long(b"\x00\xff")
        -256
        >>> decode_long(b"\x00\x80")
        -32768
        >>> decode_long(b"\x80")
        -128
        >>> decode_long(b"\x7f")
        127
    
    """
def _Pickler:
    """
    This takes a binary file for writing a pickle data stream.

            The optional *protocol* argument tells the pickler to use the
            given protocol; supported protocols are 0, 1, 2, 3, 4 and 5.
            The default protocol is 4. It was introduced in Python 3.4, and
            is incompatible with previous versions.

            Specifying a negative protocol version selects the highest
            protocol version supported.  The higher the protocol used, the
            more recent the version of Python needed to read the pickle
            produced.

            The *file* argument must have a write() method that accepts a
            single bytes argument. It can thus be a file object opened for
            binary writing, an io.BytesIO instance, or any other custom
            object that meets this interface.

            If *fix_imports* is True and *protocol* is less than 3, pickle
            will try to map the new Python 3 names to the old module names
            used in Python 2, so that the pickle data stream is readable
            with Python 2.

            If *buffer_callback* is None (the default), buffer views are
            serialized into *file* as part of the pickle stream.

            If *buffer_callback* is not None, then it can be called any number
            of times with a buffer view.  If the callback returns a false value
            (such as None), the given buffer is out-of-band; otherwise the
            buffer is serialized in-band, i.e. inside the pickle stream.

            It is an error if *buffer_callback* is not None and *protocol*
            is None or smaller than 5.
        
    """
    def clear_memo(self):
        """
        Clears the pickler's "memo".

                The memo is the data structure that remembers which objects the
                pickler has already seen, so that shared or recursive objects
                are pickled by reference and not by value.  This method is
                useful when re-using picklers.
        
        """
    def dump(self, obj):
        """
        Write a pickled representation of obj to the open file.
        """
    def memoize(self, obj):
        """
        Store an object in the memo.
        """
    def put(self, idx):
        """
        <B
        """
    def get(self, i):
        """
        <B
        """
    def save(self, obj, save_persistent_id=True):
        """
         Check for persistent id (defined by a subclass)

        """
    def persistent_id(self, obj):
        """
         This exists so a subclass can override it

        """
    def save_pers(self, pid):
        """
         Save a persistent id reference

        """
2021-03-02 20:53:56,547 : INFO : tokenize_signature : --> do i ever get here?
    def save_reduce(self, func, args, state=None, listitems=None,
                    dictitems=None, state_setter=None, obj=None):
        """
         This API is called by some subclasses


        """
    def save_none(self, obj):
        """
         If the int is small enough to fit in a signed 4-byte 2's-comp
         format, we can store it more efficiently than the general
         case.
         First one- and two-byte unsigned ints:

        """
    def save_float(self, obj):
        """
        '>d'
        """
    def save_bytes(self, obj):
        """
         bytes object is empty
        """
    def save_bytearray(self, obj):
        """
         bytearray is empty
        """
        def save_picklebuffer(self, obj):
            """
            PickleBuffer can only pickled with 
            protocol >= 5
            """
    def save_str(self, obj):
        """
        'utf-8'
        """
    def save_tuple(self, obj):
        """
         tuple is empty
        """
    def save_list(self, obj):
        """
         proto 0 -- can't use EMPTY_LIST
        """
    def _batch_appends(self, items):
        """
         Helper to batch up APPENDS sequences

        """
    def save_dict(self, obj):
        """
         proto 0 -- can't use EMPTY_DICT
        """
    def _batch_setitems(self, items):
        """
         Helper to batch up SETITEMS sequences; proto >= 1 only

        """
    def save_set(self, obj):
        """
         If the object is already in the memo, this means it is
         recursive. In this case, throw away everything we put on the
         stack, and fetch the object back from the memo.

        """
    def save_global(self, obj, name=None):
        """
        '__qualname__'
        """
    def save_type(self, obj):
        """
         Unpickling machinery


        """
def _Unpickler:
    """
    ASCII
    """
    def load(self):
        """
        Read a pickled object representation from the open file.

                Return the reconstituted object hierarchy specified in the file.
        
        """
    def pop_mark(self):
        """
        unsupported persistent id encountered
        """
    def load_proto(self):
        """
        unsupported pickle protocol: %d
        """
    def load_frame(self):
        """
        '<Q'
        """
    def load_persid(self):
        """
        ascii
        """
    def load_binpersid(self):
        """
        '<i'
        """
    def load_binint1(self):
        """
        '<H'
        """
    def load_long(self):
        """
        b'L'
        """
    def load_long1(self):
        """
        '<i'
        """
    def load_float(self):
        """
        '>d'
        """
    def _decode_string(self, value):
        """
         Used to allow strings from Python 2 to be decoded either as
         bytes or Unicode strings.  This should be used only with the
         STRING, BINSTRING and SHORT_BINSTRING opcodes.

        """
    def load_string(self):
        """
         Strip outermost quotes

        """
    def load_binstring(self):
        """
         Deprecated BINSTRING uses signed 32-bit length

        """
    def load_binbytes(self):
        """
        '<I'
        """
    def load_unicode(self):
        """
        'raw-unicode-escape'
        """
    def load_binunicode(self):
        """
        '<I'
        """
    def load_binunicode8(self):
        """
        '<Q'
        """
    def load_binbytes8(self):
        """
        '<Q'
        """
    def load_bytearray8(self):
        """
        '<Q'
        """
    def load_next_buffer(self):
        """
        pickle stream refers to out-of-band data 
        but no *buffers* argument was given
        """
    def load_readonly_buffer(self):
        """
        'utf-8'
        """
    def load_tuple(self):
        """
         INST and OBJ differ only in how they get a class object.  It's not
         only sensible to do the rest in a common routine, the two routines
         previously diverged and grew different bugs.
         klass is the class to instantiate, and k points to the topmost mark
         object, following which are the arguments for klass.__init__.

        """
    def load_inst(self):
        """
        ascii
        """
    def load_obj(self):
        """
         Stack is ... markobject classobject arg1 arg2 ...

        """
    def load_newobj(self):
        """
        utf-8
        """
    def load_stack_global(self):
        """
        STACK_GLOBAL requires str
        """
    def load_ext1(self):
        """
        '<H'
        """
    def load_ext4(self):
        """
        '<i'
        """
    def get_extension(self, code):
        """
         note that 0 is forbidden
        """
    def find_class(self, module, name):
        """
         Subclasses may override this.

        """
    def load_reduce(self):
        """
        '<I'
        """
    def load_put(self):
        """
        negative PUT argument
        """
    def load_binput(self):
        """
        negative BINPUT argument
        """
    def load_long_binput(self):
        """
        '<I'
        """
    def load_memoize(self):
        """
         Even if the PEP 307 requires extend() and append() methods,
         fall back on append() if the object has no extend() method
         for backward compatibility.

        """
    def load_setitem(self):
        """
        __setstate__
        """
    def load_mark(self):
        """
         Shorthands


        """
def _dump(obj, file, protocol=None, *, fix_imports=True, buffer_callback=None):
    """
    ASCII
    """
2021-03-02 20:53:56,577 : INFO : tokenize_signature : --> do i ever get here?
def _loads(s, *, fix_imports=True, encoding="ASCII", errors="strict",
           buffers=None):
    """
    Can't load pickle from unicode string
    """
def _test():
    """
    __main__
    """
