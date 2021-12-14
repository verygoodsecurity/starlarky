r"""File-like objects that read from or write to a string buffer.

This implements (nearly) all stdio methods.

f = StringIO()      # ready for writing
f = StringIO(buf)   # ready for reading
f.close()           # explicitly release resources held
flag = f.isatty()   # always false
pos = f.tell()      # get current position
f.seek(pos)         # set current position
f.seek(pos, mode)   # mode 0: absolute; 1: relative; 2: relative to EOF
buf = f.read()      # read until EOF
buf = f.read(n)     # read up to n bytes
buf = f.readline()  # read until end of line ('\n') or EOF
list = f.readlines()# list of f.readline() results until EOF
f.truncate([size])  # truncate file at to at most size (default: current pos)
f.write(buf)        # write at current position
f.writelines(list)  # for line in list: f.write(line)
f.getvalue()        # return whole file's contents as a string

Notes:
- Using a real file is often faster (but less convenient).
- There's also a much faster implementation in C, called cStringIO, but
  it's not subclassable.
- fileno() is left unimplemented so that code which uses it triggers
  an exception early.
- Seeking far beyond EOF and then writing will insert real null
  bytes that occupy space in the buffer.
- There's a simple test set (see end of this file).
"""
# TODO: Port over https://github.com/python/cpython/blob/34bbc87b2ddbaf245fbed6443c3e620f80c6a843/Lib/_pyio.py
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//types", types="types")
load("@stdlib//larky", larky="larky")
load("@vendor//option/result", Error="Error")

EINVAL = 22
_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION

__all__ = ["StringIO"]

def _complain_ifclosed(closed):
    if closed:
        return Error("ValueError: I/O operation on closed file").unwrap()


def IOBase(buf = ''):
    self = larky.mutablestruct(__name__='IOBase', __class__=IOBase)
    def __init__(buf):
        self.buf = buf
        self.len = len(buf)
        self.buflist = []
        self.pos = 0
        self.closed = False
        self.softspace = 0
        self.empty_char = ''
        self.zero_char = '\0'
        self.newline = '\n'
        return self
    self = __init__(buf)

    def __iter__():
        return self
    self.__iter__ = __iter__

    def __next__():
        """A file object is its own iterator, for example iter(f) returns f
        (unless f is closed). When a file is used as an iterator, typically
        in a for loop (for example, for line in f: print line), the next()
        method is called repeatedly. This method returns the next input line,
        or raises StopIteration when EOF is hit.
        """
        _complain_ifclosed(self.closed)
        r = self.readline()
        if not r:
            return StopIteration()
        return r
    self.__next__ = __next__

    def close():
        """Free the memory buffer.
        """
        if not self.closed:
            self.closed = True
            self.buf = None
            self.pos = 0
    self.close = close

    def isatty():
        """Returns False because StringIO objects are not connected to a
        tty-like device.
        """
        _complain_ifclosed(self.closed)
        return False
    self.isatty = isatty

    def seek(pos, mode=0):
        """Set the file's current position.

        The mode argument is optional and defaults to 0 (absolute file
        positioning); other values are 1 (seek relative to the current
        position) and 2 (seek relative to the file's end).

        There is no return value.
        """
        _complain_ifclosed(self.closed)
        if self.buflist:
            self.buf += self.empty_char.join(self.buflist)
            self.buflist = []
        if mode == 1:
            pos += self.pos
        elif mode == 2:
            pos += self.len
        self.pos = max(0, pos)
        return self.pos
    self.seek = seek

    def tell():
        """Return the file's current position."""
        _complain_ifclosed(self.closed)
        return self.pos
    self.tell = tell

    def read1(size=-1):
        """This is the same as read.
        """
        return self.read(size)
    self.read1 = read1

    def read(n=-1):
        """Read at most size bytes from the file
        (less if the read hits EOF before obtaining size bytes).

        If the size argument is negative or omitted, read all data until EOF
        is reached. The bytes are returned as a string object. An empty
        string is returned when EOF is encountered immediately.
        """
        _complain_ifclosed(self.closed)
        if self.buflist:
            self.buf += self.empty_char.join(self.buflist)
            self.buflist = []
        if n == None or n < 0:
            newpos = self.len
        else:
            newpos = min(self.pos+n, self.len)
        r = self.buf[self.pos:newpos]
        self.pos = newpos
        return r
    self.read = read

    def readline(length=None):
        r"""Read one entire line from the file.

        A trailing newline character is kept in the string (but may be absent
        when a file ends with an incomplete line). If the size argument is
        present and non-negative, it is a maximum byte count (including the
        trailing newline) and an incomplete line may be returned.

        An empty string is returned only when EOF is encountered immediately.

        Note: Unlike stdio's fgets(), the returned string contains null
        characters ('\0') if they occurred in the input.
        """
        _complain_ifclosed(self.closed)
        if self.buflist:
            self.buf += self.empty_char.join(self.buflist)
            self.buflist = []
        i = self.buf.find(self.newline, self.pos)
        if i < 0:
            newpos = self.len
        else:
            newpos = i+1
        if length != None and length >= 0:
            if self.pos + length < newpos:
                newpos = self.pos + length
        r = self.buf[self.pos:newpos]
        self.pos = newpos
        return r
    self.readline = readline

    def readlines(sizehint = 0):
        """Read until EOF using readline() and return a list containing the
        lines thus read.

        If the optional sizehint argument is present, instead of reading up
        to EOF, whole lines totalling approximately sizehint bytes (or more
        to accommodate a final whole line).
        """
        total = 0
        lines = []
        line = self.readline()
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if not line:
                break
            lines.append(line)
            total += len(line)
            if (0 < sizehint) and (sizehint <= total):
                break
            line = self.readline()
        return lines
    self.readlines = readlines

    def truncate(size=None):
        """Truncate the file's size.

        If the optional size argument is present, the file is truncated to
        (at most) that size. The size defaults to the current position.
        The current file position is not changed unless the position
        is beyond the new file size.

        If the specified size exceeds the file's current size, the
        file remains unchanged.
        """
        _complain_ifclosed(self.closed)
        if size == None:
            size = self.pos
        elif size < 0:
            return Error(
                "IOError: (%s) - Negative size not allowed" % EINVAL
            ).unwrap()
        elif size < self.pos:
            self.pos = size
        self.buf = self.getvalue()[:size]
        self.len = size
        return size
    self.truncate = truncate

    def write(s):
        """Write the given length of the buffer to the IO stream.

        Return the number written, which is always the length of the
        buffer passed.
        """
        _complain_ifclosed(self.closed)
        if not s: return
        spos = self.pos
        slen = self.len
        n = len(s)
        if spos == slen:
            self.buflist.append(s)
            self.len = spos + n
            self.pos = self.len
            return
        if spos > slen:
            self.buflist.append(self.zero_char*(spos - slen))
            slen = spos
        newpos = spos + n
        if spos < slen:
            if self.buflist:
                self.buf += self.empty_char.join(self.buflist)
            self.buflist = [self.buf[:spos], s, self.buf[newpos:]]
            self.buf = self.empty_char
            if newpos > slen:
                slen = newpos
        else:
            self.buflist.append(s)
            slen = newpos
        self.len = slen
        self.pos = newpos
        return n
    self.write = write

    def writelines(iterable):
        """Write a sequence of strings to the file. The sequence can be any
        iterable object producing strings, typically a list of strings. There
        is no return value.

        (The name is intended to match readlines(); writelines() does not add
        line separators.)
        """
        write = self.write
        for line in iterable:
            write(line)
    self.writelines = writelines

    def flush():
        """Flush the internal buffer
        """
        _complain_ifclosed(self.closed)
    self.flush = flush

    def getvalue():
        """
        Retrieve the entire contents of the "file" at any time before
        the StringIO object's close() method is called.

        The StringIO object can accept either Unicode or 8-bit strings,
        but mixing the two may take some care. If both are used, 8-bit
        strings that cannot be interpreted as 7-bit ASCII (that use the
        8th bit) will cause a UnicodeError to be raised when getvalue()
        is called.
        """
        _complain_ifclosed(self.closed)
        if self.buflist:
            self.buf += self.empty_char.join(self.buflist)
            self.buflist = []
        return self.buf
    self.getvalue = getvalue

    def readable():
       _complain_ifclosed(self.closed)
       return True
    self.readable = readable

    def writable():
        _complain_ifclosed(self.closed)
        return True
    self.writable = writable

    def seekable():
        _complain_ifclosed(self.closed)
        return True
    self.seekable = seekable

    return self

def TextIOBase(buf=""):
    self = IOBase(buf)
    self.__name__ = 'TextIOBase'
    self.__class__ = TextIOBase
    return self


def StringIO(buf=""):
    """class StringIO([buffer])

    When a StringIO object is created, it can be initialized to an existing
    string by passing the string to the constructor. If no string is given,
    the StringIO will start empty.

    The StringIO object can accept either Unicode or 8-bit strings, but
    mixing the two may take some care. If both are used, 8-bit strings that
    cannot be interpreted as 7-bit ASCII (that use the 8th bit) will cause
    a UnicodeError to be raised when getvalue() is called.
    """
    self = IOBase(buf)
    self.__name__ = 'StringIO'
    self.__class__ = StringIO
    return self


TextIOWrapper = StringIO


def RawIOBase():
    self = larky.mutablestruct(__name__='RawIOBase', __class__=RawIOBase)

    def read(n=-1):
        pass
    self.read = read

    def readall():
        pass
    self.readall = readall

    def readinto(b):
        pass
    self.readinto = readinto

    def write(b):
        pass
    self.write = write
    return self


BufferedIOBase = RawIOBase
BufferedReader = RawIOBase


def BytesIO(buf=b''):

    self = IOBase(buf)
    self.__name__ = 'BytesIO'
    self.__class__ = BytesIO
    self.empty_char = b''
    self.zero_char = b'\0'
    self.newline = b'\n'

    self.super_write = self.write
    def write(s):
        if not types.is_bytelike(s):
            s = codecs.encode(s, encoding='utf-8')
        return self.super_write(s)
    self.write = write
    return self
