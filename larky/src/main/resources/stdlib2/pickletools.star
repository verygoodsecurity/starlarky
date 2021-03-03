def ArgumentDescriptor(object):
    """
     name of descriptor record, also a module global name; a string
    'name'
    """
    def __init__(self, name, n, reader, doc):
        """
        r"""
            >>> import io
            >>> read_uint1(io.BytesIO(b'\xff'))
            255
    
        """
def read_uint2(f):
    """
    r"""
        >>> import io
        >>> read_uint2(io.BytesIO(b'\xff\x00'))
        255
        >>> read_uint2(io.BytesIO(b'\xff\xff'))
        65535
    
    """
def read_int4(f):
    """
    r"""
        >>> import io
        >>> read_int4(io.BytesIO(b'\xff\x00\x00\x00'))
        255
        >>> read_int4(io.BytesIO(b'\x00\x00\x00\x80')) == -(2**31)
        True
    
    """
def read_uint4(f):
    """
    r"""
        >>> import io
        >>> read_uint4(io.BytesIO(b'\xff\x00\x00\x00'))
        255
        >>> read_uint4(io.BytesIO(b'\x00\x00\x00\x80')) == 2**31
        True
    
    """
def read_uint8(f):
    """
    r"""
        >>> import io
        >>> read_uint8(io.BytesIO(b'\xff\x00\x00\x00\x00\x00\x00\x00'))
        255
        >>> read_uint8(io.BytesIO(b'\xff' * 8)) == 2**64-1
        True
    
    """
def read_stringnl(f, decode=True, stripquotes=True):
    """
    r"""
        >>> import io
        >>> read_stringnl(io.BytesIO(b"'abcd'\nefg\n"))
        'abcd'

        >>> read_stringnl(io.BytesIO(b"\n"))
        Traceback (most recent call last):
        ...
        ValueError: no string quotes around b''

        >>> read_stringnl(io.BytesIO(b"\n"), stripquotes=False)
        ''

        >>> read_stringnl(io.BytesIO(b"''\n"))
        ''

        >>> read_stringnl(io.BytesIO(b'"abcd"'))
        Traceback (most recent call last):
        ...
        ValueError: no newline found when trying to read stringnl

        Embedded escapes are undone in the result.
        >>> read_stringnl(io.BytesIO(br"'a\n\\b\x00c\td'" + b"\n'e'"))
        'a\n\\b\x00c\td'
    
    """
def read_stringnl_noescape(f):
    """
    'stringnl_noescape'
    """
def read_stringnl_noescape_pair(f):
    """
    r"""
        >>> import io
        >>> read_stringnl_noescape_pair(io.BytesIO(b"Queue\nEmpty\njunk"))
        'Queue Empty'
    
    """
def read_string1(f):
    """
    r"""
        >>> import io
        >>> read_string1(io.BytesIO(b"\x00"))
        ''
        >>> read_string1(io.BytesIO(b"\x03abcdef"))
        'abc'
    
    """
def read_string4(f):
    """
    r"""
        >>> import io
        >>> read_string4(io.BytesIO(b"\x00\x00\x00\x00abc"))
        ''
        >>> read_string4(io.BytesIO(b"\x03\x00\x00\x00abcdef"))
        'abc'
        >>> read_string4(io.BytesIO(b"\x00\x00\x00\x03abcdef"))
        Traceback (most recent call last):
        ...
        ValueError: expected 50331648 bytes in a string4, but only 6 remain
    
    """
def read_bytes1(f):
    """
    r"""
        >>> import io
        >>> read_bytes1(io.BytesIO(b"\x00"))
        b''
        >>> read_bytes1(io.BytesIO(b"\x03abcdef"))
        b'abc'
    
    """
def read_bytes4(f):
    """
    r"""
        >>> import io
        >>> read_bytes4(io.BytesIO(b"\x00\x00\x00\x00abc"))
        b''
        >>> read_bytes4(io.BytesIO(b"\x03\x00\x00\x00abcdef"))
        b'abc'
        >>> read_bytes4(io.BytesIO(b"\x00\x00\x00\x03abcdef"))
        Traceback (most recent call last):
        ...
        ValueError: expected 50331648 bytes in a bytes4, but only 6 remain
    
    """
def read_bytes8(f):
    """
    r"""
        >>> import io, struct, sys
        >>> read_bytes8(io.BytesIO(b"\x00\x00\x00\x00\x00\x00\x00\x00abc"))
        b''
        >>> read_bytes8(io.BytesIO(b"\x03\x00\x00\x00\x00\x00\x00\x00abcdef"))
        b'abc'
        >>> bigsize8 = struct.pack("<Q", sys.maxsize//3)
        >>> read_bytes8(io.BytesIO(bigsize8 + b"abcdef"))  #doctest: +ELLIPSIS
        Traceback (most recent call last):
        ...
        ValueError: expected ... bytes in a bytes8, but only 6 remain
    
    """
def read_bytearray8(f):
    """
    r"""
        >>> import io, struct, sys
        >>> read_bytearray8(io.BytesIO(b"\x00\x00\x00\x00\x00\x00\x00\x00abc"))
        bytearray(b'')
        >>> read_bytearray8(io.BytesIO(b"\x03\x00\x00\x00\x00\x00\x00\x00abcdef"))
        bytearray(b'abc')
        >>> bigsize8 = struct.pack("<Q", sys.maxsize//3)
        >>> read_bytearray8(io.BytesIO(bigsize8 + b"abcdef"))  #doctest: +ELLIPSIS
        Traceback (most recent call last):
        ...
        ValueError: expected ... bytes in a bytearray8, but only 6 remain
    
    """
def read_unicodestringnl(f):
    """
    r"""
        >>> import io
        >>> read_unicodestringnl(io.BytesIO(b"abc\\uabcd\njunk")) == 'abc\uabcd'
        True
    
    """
def read_unicodestring1(f):
    """
    r"""
        >>> import io
        >>> s = 'abcd\uabcd'
        >>> enc = s.encode('utf-8')
        >>> enc
        b'abcd\xea\xaf\x8d'
        >>> n = bytes([len(enc)])  # little-endian 1-byte length
        >>> t = read_unicodestring1(io.BytesIO(n + enc + b'junk'))
        >>> s == t
        True

        >>> read_unicodestring1(io.BytesIO(n + enc[:-1]))
        Traceback (most recent call last):
        ...
        ValueError: expected 7 bytes in a unicodestring1, but only 6 remain
    
    """
def read_unicodestring4(f):
    """
    r"""
        >>> import io
        >>> s = 'abcd\uabcd'
        >>> enc = s.encode('utf-8')
        >>> enc
        b'abcd\xea\xaf\x8d'
        >>> n = bytes([len(enc), 0, 0, 0])  # little-endian 4-byte length
        >>> t = read_unicodestring4(io.BytesIO(n + enc + b'junk'))
        >>> s == t
        True

        >>> read_unicodestring4(io.BytesIO(n + enc[:-1]))
        Traceback (most recent call last):
        ...
        ValueError: expected 7 bytes in a unicodestring4, but only 6 remain
    
    """
def read_unicodestring8(f):
    """
    r"""
        >>> import io
        >>> s = 'abcd\uabcd'
        >>> enc = s.encode('utf-8')
        >>> enc
        b'abcd\xea\xaf\x8d'
        >>> n = bytes([len(enc)]) + b'\0' * 7  # little-endian 8-byte length
        >>> t = read_unicodestring8(io.BytesIO(n + enc + b'junk'))
        >>> s == t
        True

        >>> read_unicodestring8(io.BytesIO(n + enc[:-1]))
        Traceback (most recent call last):
        ...
        ValueError: expected 7 bytes in a unicodestring8, but only 6 remain
    
    """
def read_decimalnl_short(f):
    """
    r"""
        >>> import io
        >>> read_decimalnl_short(io.BytesIO(b"1234\n56"))
        1234

        >>> read_decimalnl_short(io.BytesIO(b"1234L\n56"))
        Traceback (most recent call last):
        ...
        ValueError: invalid literal for int() with base 10: b'1234L'
    
    """
def read_decimalnl_long(f):
    """
    r"""
        >>> import io

        >>> read_decimalnl_long(io.BytesIO(b"1234L\n56"))
        1234

        >>> read_decimalnl_long(io.BytesIO(b"123456789012345678901234L\n6"))
        123456789012345678901234
    
    """
def read_floatnl(f):
    """
    r"""
        >>> import io
        >>> read_floatnl(io.BytesIO(b"-1.25\n6"))
        -1.25
    
    """
def read_float8(f):
    """
    r"""
        >>> import io, struct
        >>> raw = struct.pack(">d", -1.25)
        >>> raw
        b'\xbf\xf4\x00\x00\x00\x00\x00\x00'
        >>> read_float8(io.BytesIO(raw + b"\n"))
        -1.25
    
    """
def read_long1(f):
    """
    r"""
        >>> import io
        >>> read_long1(io.BytesIO(b"\x00"))
        0
        >>> read_long1(io.BytesIO(b"\x02\xff\x00"))
        255
        >>> read_long1(io.BytesIO(b"\x02\xff\x7f"))
        32767
        >>> read_long1(io.BytesIO(b"\x02\x00\xff"))
        -256
        >>> read_long1(io.BytesIO(b"\x02\x00\x80"))
        -32768
    
    """
def read_long4(f):
    """
    r"""
        >>> import io
        >>> read_long4(io.BytesIO(b"\x02\x00\x00\x00\xff\x00"))
        255
        >>> read_long4(io.BytesIO(b"\x02\x00\x00\x00\xff\x7f"))
        32767
        >>> read_long4(io.BytesIO(b"\x02\x00\x00\x00\x00\xff"))
        -256
        >>> read_long4(io.BytesIO(b"\x02\x00\x00\x00\x00\x80"))
        -32768
        >>> read_long1(io.BytesIO(b"\x00\x00\x00\x00"))
        0
    
    """
def StackObject(object):
    """
     name of descriptor record, for info only
    'name'
    """
    def __init__(self, name, obtype, doc):
        """
        'int'
        """
def OpcodeInfo(object):
    """
     symbolic name of opcode; a string
    'name'
    """
2021-03-02 20:53:48,191 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, name, code, arg,
                 stack_before, stack_after, proto, doc):
        """
         Ways to spell integers.


        """
def assure_pickle_consistency(verbose=False):
    """
    [A-Z][A-Z0-9_]+$
    """
def _genops(data, yield_end_pos=False):
    """
    tell
    """
def genops(pickle):
    """
    Generate all the opcodes in a pickle.

        'pickle' is a file-like object, or string, containing the pickle.

        Each opcode in the pickle is generated, from the current pickle position,
        stopping after a STOP opcode is delivered.  A triple is generated for
        each opcode:

            opcode, arg, pos

        opcode is an OpcodeInfo record, describing the current opcode.

        If the opcode has an argument embedded in the pickle, arg is its decoded
        value, as a Python object.  If the opcode doesn't have an argument, arg
        is None.

        If the pickle has a tell() method, pos was the value of pickle.tell()
        before reading the current opcode.  If the pickle is a bytes object,
        it's wrapped in a BytesIO object, and the latter's tell() result is
        used.  Else (the pickle doesn't have a tell(), and it's not obvious how
        to query its current position) pos is None.
    
    """
def optimize(p):
    """
    'Optimize a pickle string by removing unused PUT opcodes'
    """
def dis(pickle, out=None, memo=None, indentlevel=4, annotate=0):
    """
    Produce a symbolic disassembly of a pickle.

        'pickle' is a file-like object, or string, containing a (at least one)
        pickle.  The pickle is disassembled from the current position, through
        the first STOP opcode encountered.

        Optional arg 'out' is a file-like object to which the disassembly is
        printed.  It defaults to sys.stdout.

        Optional arg 'memo' is a Python dict, used as the pickle's memo.  It
        may be mutated by dis(), if the pickle contains PUT or BINPUT opcodes.
        Passing the same memo object to another dis() call then allows disassembly
        to proceed across multiple pickles that were all created by the same
        pickler with the same memo.  Ordinarily you don't need to worry about this.

        Optional arg 'indentlevel' is the number of blanks by which to indent
        a new MARK level.  It defaults to 4.

        Optional arg 'annotate' if nonzero instructs dis() to add short
        description of the opcode on each line of disassembled output.
        The value given to 'annotate' must be an integer and is used as a
        hint for the column where annotation should start.  The default
        value is 0, meaning no annotations.

        In addition to printing the disassembly, some sanity checks are made:

        + All embedded opcode arguments "make sense".

        + Explicit and implicit pop operations have enough items on the stack.

        + When an opcode implicitly refers to a markobject, a markobject is
          actually on the stack.

        + A memo entry isn't referenced before it's defined.

        + The markobject isn't stored in the memo.

        + A memo entry isn't redefined.
    
    """
def _Example:
    """
    r"""
    >>> import pickle
    >>> x = [1, 2, (3, 4), {b'abc': "def"}]
    >>> pkl0 = pickle.dumps(x, 0)
    >>> dis(pkl0)
        0: (    MARK
        1: l        LIST       (MARK at 0)
        2: p    PUT        0
        5: I    INT        1
        8: a    APPEND
        9: I    INT        2
       12: a    APPEND
       13: (    MARK
       14: I        INT        3
       17: I        INT        4
       20: t        TUPLE      (MARK at 13)
       21: p    PUT        1
       24: a    APPEND
       25: (    MARK
       26: d        DICT       (MARK at 25)
       27: p    PUT        2
       30: c    GLOBAL     '_codecs encode'
       46: p    PUT        3
       49: (    MARK
       50: V        UNICODE    'abc'
       55: p        PUT        4
       58: V        UNICODE    'latin1'
       66: p        PUT        5
       69: t        TUPLE      (MARK at 49)
       70: p    PUT        6
       73: R    REDUCE
       74: p    PUT        7
       77: V    UNICODE    'def'
       82: p    PUT        8
       85: s    SETITEM
       86: a    APPEND
       87: .    STOP
    highest protocol among opcodes = 0

    Try again with a "binary" pickle.

    >>> pkl1 = pickle.dumps(x, 1)
    >>> dis(pkl1)
        0: ]    EMPTY_LIST
        1: q    BINPUT     0
        3: (    MARK
        4: K        BININT1    1
        6: K        BININT1    2
        8: (        MARK
        9: K            BININT1    3
       11: K            BININT1    4
       13: t            TUPLE      (MARK at 8)
       14: q        BINPUT     1
       16: }        EMPTY_DICT
       17: q        BINPUT     2
       19: c        GLOBAL     '_codecs encode'
       35: q        BINPUT     3
       37: (        MARK
       38: X            BINUNICODE 'abc'
       46: q            BINPUT     4
       48: X            BINUNICODE 'latin1'
       59: q            BINPUT     5
       61: t            TUPLE      (MARK at 37)
       62: q        BINPUT     6
       64: R        REDUCE
       65: q        BINPUT     7
       67: X        BINUNICODE 'def'
       75: q        BINPUT     8
       77: s        SETITEM
       78: e        APPENDS    (MARK at 3)
       79: .    STOP
    highest protocol among opcodes = 1

    Exercise the INST/OBJ/BUILD family.

    >>> import pickletools
    >>> dis(pickle.dumps(pickletools.dis, 0))
        0: c    GLOBAL     'pickletools dis'
       17: p    PUT        0
       20: .    STOP
    highest protocol among opcodes = 0

    >>> from pickletools import _Example
    >>> x = [_Example(42)] * 2
    >>> dis(pickle.dumps(x, 0))
        0: (    MARK
        1: l        LIST       (MARK at 0)
        2: p    PUT        0
        5: c    GLOBAL     'copy_reg _reconstructor'
       30: p    PUT        1
       33: (    MARK
       34: c        GLOBAL     'pickletools _Example'
       56: p        PUT        2
       59: c        GLOBAL     '__builtin__ object'
       79: p        PUT        3
       82: N        NONE
       83: t        TUPLE      (MARK at 33)
       84: p    PUT        4
       87: R    REDUCE
       88: p    PUT        5
       91: (    MARK
       92: d        DICT       (MARK at 91)
       93: p    PUT        6
       96: V    UNICODE    'value'
      103: p    PUT        7
      106: I    INT        42
      110: s    SETITEM
      111: b    BUILD
      112: a    APPEND
      113: g    GET        5
      116: a    APPEND
      117: .    STOP
    highest protocol among opcodes = 0

    >>> dis(pickle.dumps(x, 1))
        0: ]    EMPTY_LIST
        1: q    BINPUT     0
        3: (    MARK
        4: c        GLOBAL     'copy_reg _reconstructor'
       29: q        BINPUT     1
       31: (        MARK
       32: c            GLOBAL     'pickletools _Example'
       54: q            BINPUT     2
       56: c            GLOBAL     '__builtin__ object'
       76: q            BINPUT     3
       78: N            NONE
       79: t            TUPLE      (MARK at 31)
       80: q        BINPUT     4
       82: R        REDUCE
       83: q        BINPUT     5
       85: }        EMPTY_DICT
       86: q        BINPUT     6
       88: X        BINUNICODE 'value'
       98: q        BINPUT     7
      100: K        BININT1    42
      102: s        SETITEM
      103: b        BUILD
      104: h        BINGET     5
      106: e        APPENDS    (MARK at 3)
      107: .    STOP
    highest protocol among opcodes = 1

    Try "the canonical" recursive-object test.

    >>> L = []
    >>> T = L,
    >>> L.append(T)
    >>> L[0] is T
    True
    >>> T[0] is L
    True
    >>> L[0][0] is L
    True
    >>> T[0][0] is T
    True
    >>> dis(pickle.dumps(L, 0))
        0: (    MARK
        1: l        LIST       (MARK at 0)
        2: p    PUT        0
        5: (    MARK
        6: g        GET        0
        9: t        TUPLE      (MARK at 5)
       10: p    PUT        1
       13: a    APPEND
       14: .    STOP
    highest protocol among opcodes = 0

    >>> dis(pickle.dumps(L, 1))
        0: ]    EMPTY_LIST
        1: q    BINPUT     0
        3: (    MARK
        4: h        BINGET     0
        6: t        TUPLE      (MARK at 3)
        7: q    BINPUT     1
        9: a    APPEND
       10: .    STOP
    highest protocol among opcodes = 1

    Note that, in the protocol 0 pickle of the recursive tuple, the disassembler
    has to emulate the stack in order to realize that the POP opcode at 16 gets
    rid of the MARK at 0.

    >>> dis(pickle.dumps(T, 0))
        0: (    MARK
        1: (        MARK
        2: l            LIST       (MARK at 1)
        3: p        PUT        0
        6: (        MARK
        7: g            GET        0
       10: t            TUPLE      (MARK at 6)
       11: p        PUT        1
       14: a        APPEND
       15: 0        POP
       16: 0        POP        (MARK at 0)
       17: g    GET        1
       20: .    STOP
    highest protocol among opcodes = 0

    >>> dis(pickle.dumps(T, 1))
        0: (    MARK
        1: ]        EMPTY_LIST
        2: q        BINPUT     0
        4: (        MARK
        5: h            BINGET     0
        7: t            TUPLE      (MARK at 4)
        8: q        BINPUT     1
       10: a        APPEND
       11: 1        POP_MARK   (MARK at 0)
       12: h    BINGET     1
       14: .    STOP
    highest protocol among opcodes = 1

    Try protocol 2.

    >>> dis(pickle.dumps(L, 2))
        0: \x80 PROTO      2
        2: ]    EMPTY_LIST
        3: q    BINPUT     0
        5: h    BINGET     0
        7: \x85 TUPLE1
        8: q    BINPUT     1
       10: a    APPEND
       11: .    STOP
    highest protocol among opcodes = 2

    >>> dis(pickle.dumps(T, 2))
        0: \x80 PROTO      2
        2: ]    EMPTY_LIST
        3: q    BINPUT     0
        5: h    BINGET     0
        7: \x85 TUPLE1
        8: q    BINPUT     1
       10: a    APPEND
       11: 0    POP
       12: h    BINGET     1
       14: .    STOP
    highest protocol among opcodes = 2

    Try protocol 3 with annotations:

    >>> dis(pickle.dumps(T, 3), annotate=1)
        0: \x80 PROTO      3 Protocol version indicator.
        2: ]    EMPTY_LIST   Push an empty list.
        3: q    BINPUT     0 Store the stack top into the memo.  The stack is not popped.
        5: h    BINGET     0 Read an object from the memo and push it on the stack.
        7: \x85 TUPLE1       Build a one-tuple out of the topmost item on the stack.
        8: q    BINPUT     1 Store the stack top into the memo.  The stack is not popped.
       10: a    APPEND       Append an object to a list.
       11: 0    POP          Discard the top stack item, shrinking the stack by one item.
       12: h    BINGET     1 Read an object from the memo and push it on the stack.
       14: .    STOP         Stop the unpickling machine.
    highest protocol among opcodes = 2


    """
def _test():
    """
    __main__
    """
