# ===================================================================
#
# Copyright (c) 2015, Legrandin <helderijs@gmail.com>
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in
#    the documentation and/or other materials provided with the
#    distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
# ===================================================================
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bchr="bchr")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//codecs", codecs="codecs")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//Crypto/Hash/SHAKE128", SHAKE128="SHAKE128")
load("@vendor//Crypto/Util/strxor", strxor="strxor", strxor_c="strxor_c")
load("@vendor//asserts","asserts")
load("@stdlib//builtins","builtins")
load("@stdlib//unittest","unittest")


def get_tag_random(tag, length):
    return SHAKE128.new(data=tobytes(tag)).read(length)


key_128 = get_tag_random("key_128", 16)
nonce_96 = get_tag_random("nonce_128", 12)
data_128 = get_tag_random("data_128", 16)


def GcmTests_test_loopback_128():
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    pt = get_tag_random("plaintext", 16 * 100)
    ct = cipher.encrypt(pt)

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    pt2 = cipher.decrypt(ct)
    asserts.assert_that(pt).is_equal_to(pt2)


def GcmTests_test_nonce():
        # Nonce is optional (a random one will be created)
    AES.new(key_128, AES.MODE_GCM)

    cipher = AES.new(key_128, AES.MODE_GCM, nonce_96)
    ct = cipher.encrypt(data_128)

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    asserts.assert_that(ct).is_equal_to(cipher.encrypt(data_128))


def GcmTests_test_nonce_must_be_bytes():
    asserts.assert_fails(lambda : AES.new(key_128, AES.MODE_GCM,
                      nonce='test12345678'), ".*?TypeError")


def GcmTests_test_nonce_length():
        # nonce can be of any length (but not empty)
    asserts.assert_fails(lambda : AES.new(key_128, AES.MODE_GCM,
                      nonce=bytes(r"", encoding='utf-8')), ".*?ValueError")

    for x in range(1, 128):
        cipher = AES.new(key_128, AES.MODE_GCM, nonce=bchr(1) * x)
        cipher.encrypt(bchr(1))


def GcmTests_test_block_size_128():
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    asserts.assert_that(cipher.block_size).is_equal_to(AES.block_size)


def GcmTests_test_nonce_attribute():
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    asserts.assert_that(cipher.nonce).is_equal_to(nonce_96)

    # By default, a 15 bytes long nonce is randomly generated
    nonce1 = AES.new(key_128, AES.MODE_GCM).nonce
    nonce2 = AES.new(key_128, AES.MODE_GCM).nonce
    asserts.assert_that(len(nonce1)).is_equal_to(16)
    asserts.assert_that(nonce1).is_not_equal_to(nonce2)


def GcmTests_test_unknown_parameters():
    asserts.assert_fails(lambda : AES.new(key_128, AES.MODE_GCM,
                      nonce_96, 7), ".*?TypeError")
    asserts.assert_fails(lambda : AES.new(key_128, AES.MODE_GCM,
                      nonce=nonce_96, unknown=7), ".*?TypeError")

    # TODO(Larky-Difference): This test is not needed.
    # But some are only known by the base cipher
    # (e.g. use_aesni consumed by the AES module)
    # AES.new(key_128, AES.MODE_GCM, nonce=nonce_96,
    #         use_aesni=False)


def GcmTests_test_null_encryption_decryption():
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    result = cipher.encrypt(bytes("", encoding='utf-8'))
    asserts.assert_that(result[:-len(cipher.digest())]).is_equal_to(bytes("", encoding='utf-8'))

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    result = cipher.decrypt(bytes("", encoding='utf-8'))
    asserts.assert_that(result).is_equal_to(bytes("", encoding='utf-8'))


def GcmTests_test_either_encrypt_or_decrypt():
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    cipher.encrypt(bytes(r"", encoding='utf-8'))
    asserts.assert_fails(lambda : cipher.decrypt(bytes(r"", encoding='utf-8')), ".*?TypeError")

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    cipher.decrypt(bytes(r"", encoding='utf-8'))
    asserts.assert_fails(lambda : cipher.encrypt(bytes(r"", encoding='utf-8')), ".*?TypeError")


def GcmTests_test_data_must_be_bytes():
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    asserts.assert_fails(lambda : cipher.encrypt('test1234567890-*'), ".*?TypeError")

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    asserts.assert_fails(lambda : cipher.decrypt('test1234567890-*'), ".*?TypeError")


def GcmTests_test_mac_len():
    # Invalid MAC length
    asserts.assert_fails(lambda : AES.new(key_128, AES.MODE_GCM,
                      nonce=nonce_96, mac_len=3), ".*?ValueError")
    asserts.assert_fails(lambda : AES.new(key_128, AES.MODE_GCM,
                      nonce=nonce_96, mac_len=16+1), ".*?ValueError")

    # Valid MAC length
    for mac_len in range(5, 16 + 1):
        cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96,
                         mac_len=mac_len)
        _, mac = cipher.encrypt_and_digest(data_128)
        asserts.assert_that(len(mac)).is_equal_to(mac_len)

    # Default MAC length
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    _, mac = cipher.encrypt_and_digest(data_128)
    asserts.assert_that(len(mac)).is_equal_to(16)


def GcmTests_test_invalid_mac():
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    ct, mac = cipher.encrypt_and_digest(data_128)

    invalid_mac = strxor_c(mac, 0x01)

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    asserts.assert_fails(lambda : cipher.decrypt_and_verify(ct,
                      invalid_mac), ".*?ValueError")


def GcmTests_test_hex_mac():
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    mac_hex = cipher.hexdigest()
    asserts.assert_that(cipher.digest()).is_equal_to(unhexlify(mac_hex))

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    cipher.hexverify(mac_hex)


def GcmTests_test_message_chunks():
    # Validate that both associated data and plaintext/ciphertext
    # can be broken up in chunks of arbitrary length
    auth_data = get_tag_random("authenticated data", 127)
    plaintext = get_tag_random("plaintext", 127)

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    cipher.update(auth_data)
    ciphertext, ref_mac = cipher.encrypt_and_digest(plaintext)

    def break_up(data, chunk_length):
        return [
            data[i:i+chunk_length] for i in range(0, len(data), chunk_length)
        ]

    # Encryption
    for chunk_length in 1, 2, 3, 7, 10, 13, 16, 40, 80, 128:

        cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
        for chunk in break_up(auth_data, chunk_length):
            cipher.update(chunk)
        pt2 = bytearray(r"", encoding="utf-8")
        for chunk in break_up(ciphertext, chunk_length):
            pt2 += cipher.decrypt(chunk)
        asserts.assert_that(plaintext).is_equal_to(pt2)
        cipher.verify(ref_mac)

    # Decryption
    for chunk_length in 1, 2, 3, 7, 10, 13, 16, 40, 80, 128:

        cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)

        for chunk in break_up(auth_data, chunk_length):
            cipher.update(chunk)
        ct2 = bytes(r"", encoding='utf-8')
        for chunk in break_up(plaintext, chunk_length):
            ct2 += cipher.encrypt(chunk)
        asserts.assert_that(ciphertext).is_equal_to(ct2)
        asserts.assert_that(cipher.digest()).is_equal_to(ref_mac)


def GcmTests_test_bytearray():

        # Encrypt
    key_ba = bytearray(key_128)
    nonce_ba = bytearray(nonce_96)
    header_ba = bytearray(data_128)
    data_ba = bytearray(data_128)

    cipher1 = AES.new(key_128,
                      AES.MODE_GCM,
                      nonce=nonce_96)
    cipher1.update(data_128)
    ct = cipher1.encrypt(data_128)
    tag = cipher1.digest()

    cipher2 = AES.new(key_ba,
                      AES.MODE_GCM,
                      nonce=nonce_ba)
    key_ba = bytearray(key_ba[:3]) + bytearray([0xff, 0xff, 0xff])
    nonce_ba = bytearray(nonce_ba[:3]) + bytes([0xff, 0xff, 0xff])
    cipher2.update(header_ba)
    header_ba = bytearray(header_ba[:3]) + bytearray([0xff, 0xff, 0xff])
    ct_test = cipher2.encrypt(data_ba)
    data_ba = bytearray(data_ba[:3]) + bytes([0xff, 0xff, 0xff])
    tag_test = cipher2.digest()

    asserts.assert_that(ct).is_equal_to(ct_test)
    asserts.assert_that(tag).is_equal_to(tag_test)
    asserts.assert_that(cipher1.nonce).is_equal_to(cipher2.nonce)

    # Decrypt
    key_ba = bytearray(key_128)
    nonce_ba = bytearray(nonce_96)
    header_ba = bytearray(data_128)

    cipher4 = AES.new(key_ba,
                      AES.MODE_GCM,
                      nonce=nonce_ba)
    key_ba = bytearray(key_ba[:3]) + bytearray([0xff, 0xff, 0xff])
    nonce_ba = bytearray(nonce_ba[:3]) + bytes([0xff, 0xff, 0xff])
    cipher4.update(header_ba)
    header_ba = bytearray(header_ba[:3]) + bytearray([0xff, 0xff, 0xff])
    pt_test = cipher4.decrypt_and_verify(bytearray(ct_test), bytearray(tag_test))

    asserts.assert_that(data_128).is_equal_to(pt_test)

def GcmTests_test_output_param():

    pt = bytes([0x35]) * 16
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    ct = cipher.encrypt(pt)
    tag = cipher.digest()

    output = bytearray([0x00] * 16)
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    res = cipher.encrypt(pt, output=output)
    asserts.assert_that(ct).is_equal_to(output)
    asserts.assert_that(res).is_equal_to(None)

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    res = cipher.decrypt(ct, output=output)
    asserts.assert_that(pt).is_equal_to(output)
    asserts.assert_that(res).is_equal_to(None)

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    res, tag_out = cipher.encrypt_and_digest(pt, output=output)
    asserts.assert_that(ct).is_equal_to(output)
    asserts.assert_that(res).is_equal_to(None)
    asserts.assert_that(tag).is_equal_to(tag_out)

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    res = cipher.decrypt_and_verify(ct, tag, output=output)
    asserts.assert_that(pt).is_equal_to(output)
    asserts.assert_that(res).is_equal_to(None)


def GcmTests_test_output_param_neg():

    pt = bytes([0x35]) * 16
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    ct = cipher.encrypt(pt)

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    asserts.assert_fails(lambda : cipher.encrypt(pt, output=bytes([0x30])*16), ".*?TypeError")

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    asserts.assert_fails(lambda : cipher.decrypt(ct, output=bytes([0x30])*16), ".*?TypeError")

    shorter_output = bytearray([0x00] * 15)
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    asserts.assert_fails(lambda : cipher.encrypt(pt, output=shorter_output), ".*?ValueError")
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    asserts.assert_fails(lambda : cipher.decrypt(ct, output=shorter_output), ".*?ValueError")


def GcmFSMTests_test_valid_init_encrypt_decrypt_digest_verify():
        # No authenticated data, fixed plaintext
        # Verify path INIT->ENCRYPT->DIGEST
    cipher = AES.new(key_128, AES.MODE_GCM,
                     nonce=nonce_96)
    ct = cipher.encrypt(data_128)
    mac = cipher.digest()

        # Verify path INIT->DECRYPT->VERIFY
    cipher = AES.new(key_128, AES.MODE_GCM,
                     nonce=nonce_96)
    cipher.decrypt(ct)
    cipher.verify(mac)


def GcmFSMTests_test_valid_init_update_digest_verify():
        # No plaintext, fixed authenticated data
        # Verify path INIT->UPDATE->DIGEST
    cipher = AES.new(key_128, AES.MODE_GCM,
                     nonce=nonce_96)
    cipher.update(data_128)
    mac = cipher.digest()

        # Verify path INIT->UPDATE->VERIFY
    cipher = AES.new(key_128, AES.MODE_GCM,
                     nonce=nonce_96)
    cipher.update(data_128)
    cipher.verify(mac)


def GcmFSMTests_test_valid_full_path():
        # Fixed authenticated data, fixed plaintext
        # Verify path INIT->UPDATE->ENCRYPT->DIGEST
    cipher = AES.new(key_128, AES.MODE_GCM,
                     nonce=nonce_96)
    cipher.update(data_128)
    ct = cipher.encrypt(data_128)
    mac = cipher.digest()

        # Verify path INIT->UPDATE->DECRYPT->VERIFY
    cipher = AES.new(key_128, AES.MODE_GCM,
                     nonce=nonce_96)
    cipher.update(data_128)
    cipher.decrypt(ct)
    cipher.verify(mac)

def GcmFSMTests_test_valid_init_digest():
        # Verify path INIT->DIGEST
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    cipher.digest()

def GcmFSMTests_test_valid_init_verify():
        # Verify path INIT->VERIFY
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    mac = cipher.digest()

    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    cipher.verify(mac)

def GcmFSMTests_test_valid_multiple_encrypt_or_decrypt():
    for method_name in "encrypt", "decrypt":
        for auth_data in (None, bytes([0x33, 0x33, 0x33]), data_128,
                          data_128 + bytearray([0x33])):
            if auth_data == None:
                assoc_len = None
            else:
                assoc_len = len(auth_data)
            cipher = AES.new(key_128, AES.MODE_GCM,
                             nonce=nonce_96)
            if auth_data != None:
                cipher.update(auth_data)
            method = getattr(cipher, method_name)
            method(data_128)
            method(data_128)
            method(data_128)
            method(data_128)

def GcmFSMTests_test_valid_multiple_digest_or_verify():
        # Multiple calls to digest
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    cipher.update(data_128)
    first_mac = cipher.digest()
    for x in range(4):
        asserts.assert_that(first_mac).is_equal_to(cipher.digest())

        # Multiple calls to verify
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    cipher.update(data_128)
    for x in range(5):
        cipher.verify(first_mac)

def GcmFSMTests_test_valid_encrypt_and_digest_decrypt_and_verify():
        # encrypt_and_digest
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    cipher.update(data_128)
    ct, mac = cipher.encrypt_and_digest(data_128)

        # decrypt_and_verify
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    cipher.update(data_128)
    pt = cipher.decrypt_and_verify(ct, mac)
    asserts.assert_that(data_128).is_equal_to(pt)

def GcmFSMTests_test_invalid_mixing_encrypt_decrypt():
        # Once per method, with or without assoc. data
    for method1_name, method2_name in (("encrypt", "decrypt"),
                                       ("decrypt", "encrypt")):
        for assoc_data_present in (True, False):
            cipher = AES.new(key_128, AES.MODE_GCM,
                             nonce=nonce_96)
            if assoc_data_present:
                cipher.update(data_128)
            getattr(cipher, method1_name)(data_128)
            asserts.assert_fails(lambda : getattr(cipher, method2_name)(data_128), ".*?TypeError")

def GcmFSMTests_test_invalid_encrypt_or_update_after_digest():
    for method_name in "encrypt", "update":
        cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
        cipher.encrypt(data_128)
        cipher.digest()
        asserts.assert_fails(lambda : getattr(cipher, method_name)(data_128), ".*?TypeError")

        cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
        cipher.encrypt_and_digest(data_128)

def GcmFSMTests_test_invalid_decrypt_or_update_after_verify():
    cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
    ct = cipher.encrypt(data_128)
    mac = cipher.digest()

    for method_name in "decrypt", "update":
        cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
        cipher.decrypt(ct)
        cipher.verify(mac)
        asserts.assert_fails(lambda : getattr(cipher, method_name)(data_128), ".*?TypeError")

        cipher = AES.new(key_128, AES.MODE_GCM, nonce=nonce_96)
        cipher.decrypt_and_verify(ct, mac)
        asserts.assert_fails(lambda : getattr(cipher, method_name)(data_128), ".*?TypeError")


"""Class exercising the GCM test vectors found in
       http://csrc.nist.gov/groups/ST/toolkit/BCM/documents/proposedmodes/gcm/gcm-revised-spec.pdf"""

# List of test vectors, each made up of:
# - authenticated data
# - plaintext
# - ciphertext
# - MAC
# - AES key
# - nonce
test_vectors_hex = [
    (
        '',
        '',
        '',
        '58e2fccefa7e3061367f1d57a4e7455a',
        '00000000000000000000000000000000',
        '000000000000000000000000'
    ),
    (
        '',
        '00000000000000000000000000000000',
        '0388dace60b6a392f328c2b971b2fe78',
        'ab6e47d42cec13bdf53a67b21257bddf',
        '00000000000000000000000000000000',
        '000000000000000000000000'
    ),
    (
        '',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b391aafd255',
        '42831ec2217774244b7221b784d0d49ce3aa212f2c02a4e035c17e2329aca12e' +
        '21d514b25466931c7d8f6a5aac84aa051ba30b396a0aac973d58e091473f5985',
        '4d5c2af327cd64a62cf35abd2ba6fab4',
        'feffe9928665731c6d6a8f9467308308',
        'cafebabefacedbaddecaf888'
    ),
    (
        'feedfacedeadbeeffeedfacedeadbeefabaddad2',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39',
        '42831ec2217774244b7221b784d0d49ce3aa212f2c02a4e035c17e2329aca12e' +
        '21d514b25466931c7d8f6a5aac84aa051ba30b396a0aac973d58e091',
        '5bc94fbc3221a5db94fae95ae7121a47',
        'feffe9928665731c6d6a8f9467308308',
        'cafebabefacedbaddecaf888'
    ),
    (
        'feedfacedeadbeeffeedfacedeadbeefabaddad2',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39',
        '61353b4c2806934a777ff51fa22a4755699b2a714fcdc6f83766e5f97b6c7423' +
        '73806900e49f24b22b097544d4896b424989b5e1ebac0f07c23f4598',
        '3612d2e79e3b0785561be14aaca2fccb',
        'feffe9928665731c6d6a8f9467308308',
        'cafebabefacedbad'
    ),
    (
        'feedfacedeadbeeffeedfacedeadbeefabaddad2',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39',
        '8ce24998625615b603a033aca13fb894be9112a5c3a211a8ba262a3cca7e2ca7' +
        '01e4a9a4fba43c90ccdcb281d48c7c6fd62875d2aca417034c34aee5',
        '619cc5aefffe0bfa462af43c1699d050',
        'feffe9928665731c6d6a8f9467308308',
        '9313225df88406e555909c5aff5269aa' +
        '6a7a9538534f7da1e4c303d2a318a728c3c0c95156809539fcf0e2429a6b5254' +
        '16aedbf5a0de6a57a637b39b'
    ),
    (
        '',
        '',
        '',
        'cd33b28ac773f74ba00ed1f312572435',
        '000000000000000000000000000000000000000000000000',
        '000000000000000000000000'
    ),
    (
        '',
        '00000000000000000000000000000000',
        '98e7247c07f0fe411c267e4384b0f600',
        '2ff58d80033927ab8ef4d4587514f0fb',
        '000000000000000000000000000000000000000000000000',
        '000000000000000000000000'
    ),
    (
        '',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b391aafd255',
        '3980ca0b3c00e841eb06fac4872a2757859e1ceaa6efd984628593b40ca1e19c' +
        '7d773d00c144c525ac619d18c84a3f4718e2448b2fe324d9ccda2710acade256',
        '9924a7c8587336bfb118024db8674a14',
        'feffe9928665731c6d6a8f9467308308feffe9928665731c',
        'cafebabefacedbaddecaf888'
    ),
    (
        'feedfacedeadbeeffeedfacedeadbeefabaddad2',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39',
        '3980ca0b3c00e841eb06fac4872a2757859e1ceaa6efd984628593b40ca1e19c' +
        '7d773d00c144c525ac619d18c84a3f4718e2448b2fe324d9ccda2710',
        '2519498e80f1478f37ba55bd6d27618c',
        'feffe9928665731c6d6a8f9467308308feffe9928665731c',
        'cafebabefacedbaddecaf888'
    ),
    (
        'feedfacedeadbeeffeedfacedeadbeefabaddad2',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39',
        '0f10f599ae14a154ed24b36e25324db8c566632ef2bbb34f8347280fc4507057' +
        'fddc29df9a471f75c66541d4d4dad1c9e93a19a58e8b473fa0f062f7',
        '65dcc57fcf623a24094fcca40d3533f8',
        'feffe9928665731c6d6a8f9467308308feffe9928665731c',
        'cafebabefacedbad'
    ),
    (
        'feedfacedeadbeeffeedfacedeadbeefabaddad2',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39',
        'd27e88681ce3243c4830165a8fdcf9ff1de9a1d8e6b447ef6ef7b79828666e45' +
        '81e79012af34ddd9e2f037589b292db3e67c036745fa22e7e9b7373b',
        'dcf566ff291c25bbb8568fc3d376a6d9',
        'feffe9928665731c6d6a8f9467308308feffe9928665731c',
        '9313225df88406e555909c5aff5269aa' +
        '6a7a9538534f7da1e4c303d2a318a728c3c0c95156809539fcf0e2429a6b5254' +
        '16aedbf5a0de6a57a637b39b'
    ),
    (
        '',
        '',
        '',
        '530f8afbc74536b9a963b4f1c4cb738b',
        '0000000000000000000000000000000000000000000000000000000000000000',
        '000000000000000000000000'
    ),
    (
        '',
        '00000000000000000000000000000000',
        'cea7403d4d606b6e074ec5d3baf39d18',
        'd0d1c8a799996bf0265b98b5d48ab919',
        '0000000000000000000000000000000000000000000000000000000000000000',
        '000000000000000000000000'
    ),
    (   '',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b391aafd255',
        '522dc1f099567d07f47f37a32a84427d643a8cdcbfe5c0c97598a2bd2555d1aa' +
        '8cb08e48590dbb3da7b08b1056828838c5f61e6393ba7a0abcc9f662898015ad',
        'b094dac5d93471bdec1a502270e3cc6c',
        'feffe9928665731c6d6a8f9467308308feffe9928665731c6d6a8f9467308308',
        'cafebabefacedbaddecaf888'
    ),
    (
        'feedfacedeadbeeffeedfacedeadbeefabaddad2',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39',
        '522dc1f099567d07f47f37a32a84427d643a8cdcbfe5c0c97598a2bd2555d1aa' +
        '8cb08e48590dbb3da7b08b1056828838c5f61e6393ba7a0abcc9f662',
        '76fc6ece0f4e1768cddf8853bb2d551b',
        'feffe9928665731c6d6a8f9467308308feffe9928665731c6d6a8f9467308308',
        'cafebabefacedbaddecaf888'
    ),
    (
        'feedfacedeadbeeffeedfacedeadbeefabaddad2',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39',
        'c3762df1ca787d32ae47c13bf19844cbaf1ae14d0b976afac52ff7d79bba9de0' +
        'feb582d33934a4f0954cc2363bc73f7862ac430e64abe499f47c9b1f',
        '3a337dbf46a792c45e454913fe2ea8f2',
        'feffe9928665731c6d6a8f9467308308feffe9928665731c6d6a8f9467308308',
        'cafebabefacedbad'
    ),
    (
        'feedfacedeadbeeffeedfacedeadbeefabaddad2',
        'd9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72' +
        '1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39',
        '5a8def2f0c9e53f1f75d7853659e2a20eeb2b22aafde6419a058ab4f6f746bf4' +
        '0fc0c3b780f244452da3ebf1c5d82cdea2418997200ef82e44ae7e3f',
        'a44a8266ee1c8eb0c8b5d4cf5ae9f19a',
        'feffe9928665731c6d6a8f9467308308feffe9928665731c6d6a8f9467308308',
        '9313225df88406e555909c5aff5269aa' +
        '6a7a9538534f7da1e4c303d2a318a728c3c0c95156809539fcf0e2429a6b5254' +
        '16aedbf5a0de6a57a637b39b'
    )
]

test_vectors = [[unhexlify(x) for x in tv] for tv in test_vectors_hex]

def TestVectors_runTest():
    for assoc_data, pt, ct, mac, key, nonce in test_vectors:
        # Encrypt
        cipher = AES.new(key, AES.MODE_GCM, nonce, mac_len=len(mac))
        cipher.update(assoc_data)
        ct2, mac2 = cipher.encrypt_and_digest(pt)
        asserts.assert_that(ct).is_equal_to(ct2)
        asserts.assert_that(mac).is_equal_to(mac2)

        # Decrypt
        cipher = AES.new(key, AES.MODE_GCM, nonce, mac_len=len(mac))
        cipher.update(assoc_data)
        pt2 = cipher.decrypt_and_verify(ct, mac)
        asserts.assert_that(pt).is_equal_to(pt2)


"""Class exercising the GCM test vectors found in
       'The fragility of AES-GCM authentication algorithm', Gueron, Krasnov
       https://eprint.iacr.org/2013/157.pdf"""

def TestVectorsGueronKrasnov_test_1():
    key = unhexlify("3da6c536d6295579c0959a7043efb503")
    iv  = unhexlify("2b926197d34e091ef722db94")
    aad = unhexlify("00000000000000000000000000000000" +
                    "000102030405060708090a0b0c0d0e0f" +
                    "101112131415161718191a1b1c1d1e1f" +
                    "202122232425262728292a2b2c2d2e2f" +
                    "303132333435363738393a3b3c3d3e3f")
    digest = unhexlify("69dd586555ce3fcc89663801a71d957b")

    cipher = AES.new(key, AES.MODE_GCM, iv).update(aad)
    asserts.assert_that(digest).is_equal_to(cipher.digest())

def TestVectorsGueronKrasnov_test_2():
    key = unhexlify("843ffcf5d2b72694d19ed01d01249412")
    iv  = unhexlify("dbcca32ebf9b804617c3aa9e")
    aad = unhexlify("00000000000000000000000000000000" +
                    "101112131415161718191a1b1c1d1e1f")
    pt  = unhexlify("000102030405060708090a0b0c0d0e0f" +
                    "101112131415161718191a1b1c1d1e1f" +
                    "202122232425262728292a2b2c2d2e2f" +
                    "303132333435363738393a3b3c3d3e3f" +
                    "404142434445464748494a4b4c4d4e4f")
    ct  = unhexlify("6268c6fa2a80b2d137467f092f657ac0" +
                    "4d89be2beaa623d61b5a868c8f03ff95" +
                    "d3dcee23ad2f1ab3a6c80eaf4b140eb0" +
                    "5de3457f0fbc111a6b43d0763aa422a3" +
                    "013cf1dc37fe417d1fbfc449b75d4cc5")
    digest = unhexlify("3b629ccfbc1119b7319e1dce2cd6fd6d")

    cipher = AES.new(key, AES.MODE_GCM, iv).update(aad)
    ct2, digest2 = cipher.encrypt_and_digest(pt)

    asserts.assert_that(ct).is_equal_to(ct2)
    asserts.assert_that(digest).is_equal_to(digest2)


# TODO(mahmoudimus): put this somewhere in a shared utility class
def zfill(x, leading=4):
    if len(str(x)) < leading:
        return (('0' * leading) + str(x))[-leading:]
    else:
        return str(x)


def TestVariableLength_runTest():
    _extra_params = {}
    key = bytes([0x30]) * 16
    h = SHA256.new()

    for length in range(160):
        nonce = codecs.encode(zfill(length), encoding='utf-8')
        data = bchr(length) * length
        cipher = AES.new(key, AES.MODE_GCM, nonce=nonce, **_extra_params)
        ct, tag = cipher.encrypt_and_digest(data)
        h.update(ct)
        h.update(tag)

    asserts.assert_that(h.hexdigest()).is_equal_to("7b7eb1ffbe67a2e53a912067c0ec8e62ebc7ce4d83490ea7426941349811bdf4")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_loopback_128))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_nonce))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_nonce_must_be_bytes))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_nonce_length))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_block_size_128))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_nonce_attribute))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_unknown_parameters))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_null_encryption_decryption))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_either_encrypt_or_decrypt))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_data_must_be_bytes))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_mac_len))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_invalid_mac))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_hex_mac))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_message_chunks))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_bytearray))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_output_param))
    _suite.addTest(unittest.FunctionTestCase(GcmTests_test_output_param_neg))
    _suite.addTest(unittest.FunctionTestCase(GcmFSMTests_test_valid_init_encrypt_decrypt_digest_verify))
    _suite.addTest(unittest.FunctionTestCase(GcmFSMTests_test_valid_init_update_digest_verify))
    _suite.addTest(unittest.FunctionTestCase(GcmFSMTests_test_valid_full_path))
    _suite.addTest(unittest.FunctionTestCase(GcmFSMTests_test_valid_init_digest))
    _suite.addTest(unittest.FunctionTestCase(GcmFSMTests_test_valid_init_verify))
    _suite.addTest(unittest.FunctionTestCase(GcmFSMTests_test_valid_multiple_encrypt_or_decrypt))
    _suite.addTest(unittest.FunctionTestCase(GcmFSMTests_test_valid_multiple_digest_or_verify))
    _suite.addTest(unittest.FunctionTestCase(GcmFSMTests_test_valid_encrypt_and_digest_decrypt_and_verify))
    _suite.addTest(unittest.FunctionTestCase(GcmFSMTests_test_invalid_mixing_encrypt_decrypt))
    _suite.addTest(unittest.FunctionTestCase(GcmFSMTests_test_invalid_encrypt_or_update_after_digest))
    _suite.addTest(unittest.FunctionTestCase(GcmFSMTests_test_invalid_decrypt_or_update_after_verify))
    _suite.addTest(unittest.FunctionTestCase(TestVectorsGueronKrasnov_test_1))
    _suite.addTest(unittest.FunctionTestCase(TestVectorsGueronKrasnov_test_2))
    _suite.addTest(unittest.FunctionTestCase(_testsuite))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
