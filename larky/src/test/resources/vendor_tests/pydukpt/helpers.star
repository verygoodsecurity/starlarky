# Ported from pydukpt 0.1.0 (3778da71): tests/helpers.py. See larky/src/main/resources/VENDORED.md.
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
"""Test-only DES helpers for checking the data and MAC key variants."""
load("@vendor//Crypto/Cipher/DES", DES="DES")
load("@vendor//Crypto/Cipher/DES3", DES3="DES3")


def zero_pad(data):
    return data + b"\0" * ((8 - len(data) % 8) % 8)


def tdes_cbc_encrypt(key, data):
    """TDES-CBC, zero IV, zero padding (as in the ANSI X9.24-1 A.4 examples)."""
    return DES3.new(key, DES3.MODE_CBC, iv=b"\0" * 8).encrypt(zero_pad(data))


def retail_mac(key, data):
    """ANSI X9.19 retail MAC (ISO 9797-1 algorithm 3), zero padding, full 8 bytes."""
    left, right = key[:8], key[8:]
    state = DES.new(left, DES.MODE_CBC, iv=b"\0" * 8).encrypt(zero_pad(data))[-8:]
    state = DES.new(right, DES.MODE_ECB).decrypt(state)
    return DES.new(left, DES.MODE_ECB).encrypt(state)
