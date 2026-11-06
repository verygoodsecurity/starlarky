# Ported from pydukpt 0.1.0 (3778da71): tests/conftest.py. See larky/src/main/resources/VENDORED.md.
#
# MIT License, Copyright (c) 2026 Mahmoud Abdelkader; see
# larky/src/main/resources/vendor/pydukpt/__init__.star for the full notice.
#
# pydukpt NOTICE (test vectors): the ANSI X9.24-1:2009 Annex A.4.2 values in
# tests/test_published_vectors.py are as transcribed in moov-io/dukpt
# (https://github.com/moov-io/dukpt, Apache License 2.0),
# pkg/des/des_test.go at commit 9cb72e64a3eab8ec2dd1b9824b9f8ebba2b5ad4d.
# tests/fixtures/vectors.json was computed with moov-io/dukpt and
# cross-checked with varhenn/dukpt_des_pinblock
# (https://github.com/varhenn/dukpt_des_pinblock, BSD 2-Clause License,
# Copyright (c) 2022, varhenn) and dukpt 1.0.1; neither is included.
#
# Larky: the fixture is loaded from fixtures/vectors.star, generated from
# tests/fixtures/vectors.json (Larky cannot read files). Hypothesis is not
# available; Examples() stands in for its strategies with a fixed-seed
# generator, so the property tests run on a fixed set of examples.
load("@stdlib//binascii", unhexlify="unhexlify")
load("@stdlib//larky", larky="larky")
load("./fixtures/vectors", _VECTORS="VECTORS")

VECTORS = _VECTORS


def load_vectors():
    return VECTORS


def h(s):
    return unhexlify(s)


def Examples(seed):
    """Deterministic stand-in for Hypothesis strategies (64-bit LCG)."""
    self = larky.mutablestruct(__name__='Examples', __class__=Examples)
    self._state = seed & 0xFFFFFFFFFFFFFFFF

    def _next32():
        self._state = (self._state * 6364136223846793005 + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF
        return self._state >> 32
    self._next32 = _next32

    def integers(lo, hi):
        """An int in lo..hi (inclusive); the bounds come up often."""
        r = _next32() % 8
        if r == 0:
            return lo
        if r == 1:
            return hi
        span = hi - lo + 1
        n = (_next32() << 32) | _next32()
        return lo + n % span
    self.integers = integers

    def binary(size):
        """size bytes; all zero, all 0xFF or random."""
        r = _next32() % 8
        if r == 0:
            return bytes([0] * size)
        if r == 1:
            return bytes([0xFF] * size)
        return bytes([_next32() & 0xFF for _ in range(size)])
    self.binary = binary

    def text(alphabet, min_size, max_size):
        """A string of min_size..max_size characters from alphabet."""
        n = integers(min_size, max_size)
        return "".join([alphabet[_next32() % len(alphabet)] for _ in range(n)])
    self.text = text
    return self
