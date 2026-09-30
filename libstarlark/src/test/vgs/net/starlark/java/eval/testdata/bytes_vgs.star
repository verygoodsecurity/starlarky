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
    assert_eq(sorted([b[1], b[0], b[3], b[2]]), [b[2], b[0], b[1], b[3]])

test_indexed_bytes_compare_unsigned()
