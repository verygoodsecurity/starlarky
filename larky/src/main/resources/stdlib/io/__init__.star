load("@stdlib//larky", larky="larky")
load("@stdlib//io/_io",
     StringIO="StringIO",
     TextIOBase="TextIOBase",
     IOBase="IOBase",
     BytesIO="BytesIO",
     BufferedIOBase="BufferedIOBase",
     BufferedReader="BufferedReader",
     RawIOBase="RawIOBase"
)


# for seek()
SEEK_SET = 0
SEEK_CUR = 1
SEEK_END = 2

# open() uses st_blksize whenever we can
DEFAULT_BUFFER_SIZE = 8 * 1024  # bytes

io = larky.struct(
     __name__="io",
     StringIO=StringIO,
     TextIOBase=TextIOBase,
     IOBase=IOBase,
     BytesIO=BytesIO,
     BufferedIOBase=BufferedIOBase,
     BufferedReader=BufferedReader,
     RawIOBase=RawIOBase,
     # for seek()
     SEEK_SET=SEEK_SET,
     SEEK_CUR=SEEK_CUR,
     SEEK_END=SEEK_END,
     # open() uses st_blksize whenever we can
     DEFAULT_BUFFER_SIZE=DEFAULT_BUFFER_SIZE,
)