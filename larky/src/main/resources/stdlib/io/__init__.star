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


io = larky.struct(
     StringIO=StringIO,
     TextIOBase=TextIOBase,
     IOBase=IOBase,
     BytesIO=BytesIO,
     BufferedIOBase=BufferedIOBase,
     BufferedReader=BufferedReader,
     RawIOBase=RawIOBase,
)