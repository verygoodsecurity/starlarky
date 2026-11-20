# Ported from CPython 2.7.x, pinned to 2.7.18 (8d21aa2): Lib/StringIO.py. See larky/src/main/resources/VENDORED.md.
load("@stdlib//io/_io", _StringIO="StringIO")

# for backward compatibility

StringIO = _StringIO