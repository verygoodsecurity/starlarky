# VGS bytes behavior not covered by upstream's bytes.star.
# Expected values are CPython's (ord) and 32-bit FNV-1a (hash).

def test_hash_is_fnv1a_for_every_byte():
    # bytes.star's vectors are ASCII; bytes >= 0x80 must not be sign-extended.
    assert_eq(hash(b"abc") & 0xffffffff, 0x1a47e90b)
    assert_eq(hash(b"\xe4") & 0xffffffff, 0x610b5af3)
    assert_eq(hash(b"\xff\x00\x80") & 0xffffffff, 0xa36cd47e)
    assert_eq(hash(bytes("hello, 世界")) & 0xffffffff, 0xc5f68969)

test_hash_is_fnv1a_for_every_byte()

def test_ord_returns_the_code_point():
    assert_eq(ord("a"), 97)
    assert_eq(ord("\x7f"), 127)
    assert_eq(ord("é"), 233)
    assert_eq(ord("€"), 8364)
    assert_eq(ord("世"), 19990)
    assert_eq(ord("😿"), 128575)  # one code point, two UTF-16 chars
    assert_eq(ord(b"\xe4"), 228)
    assert_fails(lambda: ord("ab"), "ord: string has length 2, want 1")
    assert_fails(lambda: ord(""), "ord: string has length 0, want 1")
    assert_fails(lambda: ord(b"ab"), "ord: bytes has length 2, want 1")

test_ord_returns_the_code_point()

def test_indexed_bytes_compare_unsigned():
    b = b"A\xe4\x00\xff"
    assert_(b[1] > b[0], "0xe4 > 0x41")
    assert_(b[3] > b[1], "0xff > 0xe4")
    assert_(b[2] < b[0], "0x00 < 0x41")
    assert_eq(sorted([b[1], b[0], b[3], b[2]]), [0, 0x41, 0xe4, 0xff])

test_indexed_bytes_compare_unsigned()

def test_bytes_index_and_iterate_as_ints():
    # As in Python: every way of reading one element of a bytes yields the same int.
    b = b"A\xe4"
    assert_eq(b[0], 65)
    assert_eq(b[-1], 228)
    assert_eq(type(b[0]), "int")
    assert_eq([c for c in b], [65, 228])
    assert_eq(list(b), [65, 228])
    assert_eq(tuple(b), (65, 228))
    assert_eq(sorted(b"BA"), [65, 66])
    assert_eq(list(enumerate(b)), [(0, 65), (1, 228)])
    assert_eq(list(zip(b, b"xy")), [(65, 120), (228, 121)])
    assert_eq(max(b), 228)
    assert_eq(min(b), 65)
    assert_eq(any(b"\x00"), False)
    assert_eq(all(b"\x01\x02"), True)
    assert_eq(list(reversed(b)), [228, 65])
    x, y = b
    assert_eq((x, y), (65, 228))
    assert_eq((lambda *a: a)(*b), (65, 228))
    for c in b:
        assert_eq(type(c), "int")

    # The failure that motivated this: indexed and iterated elements are one key.
    table = {v: i for i, v in enumerate(b"ABC")}
    assert_eq([table[c] for c in b"CAB"], [2, 0, 1])
    assert_eq(table[b"CAB"[0]], 2)
    assert_eq(len(set([b"A"[0], 65] + list(b"A"))), 1)

    # A byte is an int, not a bytes.
    assert_(b[0] != b"A", "b[0] is not a bytes")
    assert_eq(b[0] + 1, 66)
    assert_eq(bytes([b[0], b[1]]), b)
    assert_eq(bytes(list(b)), b)
    assert_fails(lambda: b[0] + b"x", "unsupported binary operation: int \\+ bytes")
    # Only bytes + bytes concatenates, as in the spec (and Python for lists).
    assert_fails(lambda: [1] + b"x", "unsupported binary operation: list \\+ bytes")
    assert_fails(lambda: b"x" + [1], "unsupported binary operation: bytes \\+ list")
    assert_fails(lambda: b"x" + "y", "unsupported binary operation: bytes \\+ string")

test_bytes_index_and_iterate_as_ints()

def test_ord_of_an_indexed_byte():
    # Scripts wrote ord(b[i]) when b[i] was a 1-byte bytes; that keeps working.
    b = b"A\xe4"
    assert_eq(ord(b[0]), 65)
    assert_eq(ord(b[1]), 228)
    assert_fails(lambda: ord(256), "ord: int 256 out of range")
    assert_fails(lambda: ord(-1), "ord: int -1 out of range")

test_ord_of_an_indexed_byte()
