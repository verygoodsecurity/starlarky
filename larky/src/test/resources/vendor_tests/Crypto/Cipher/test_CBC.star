# # ===================================================================
# #
# # Copyright (c) 2014, Legrandin <helderijs@gmail.com>
# # All rights reserved.
# #
# # Redistribution and use in source and binary forms, with or without
# # modification, are permitted provided that the following conditions
# # are met:
# #
# # 1. Redistributions of source code must retain the above copyright
# #    notice, this list of conditions and the following disclaimer.
# # 2. Redistributions in binary form must reproduce the above copyright
# #    notice, this list of conditions and the following disclaimer in
# #    the documentation and/or other materials provided with the
# #    distribution.
# #
# # THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# # "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# # LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# # FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# # COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# # INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# # BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# # LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# # CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# # LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# # ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# # POSSIBILITY OF SUCH DAMAGE.
# # ===================================================================
#
# load("@stdlib//binascii", unhexlify="unhexlify")
# load("@stdlib//larky", larky="larky")
# load("@stdlib//unittest", unittest="unittest")
# load("@vendor//Crypto/Cipher", AES="AES", DES3="DES3", DES="DES")
# load("@vendor//Crypto/Hash", SHAKE128="SHAKE128")
# load("@vendor//Crypto/SelfTest/loader", load_test_vectors="load_test_vectors")
# load("@vendor//Crypto/SelfTest/st_common", list_test_cases="list_test_cases")
# load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", is_string="is_string")
# load("@vendor//asserts", asserts="asserts")
#
#
# def get_tag_random(tag, length):
#     return SHAKE128.new(data=tobytes(tag)).read(length)
#
# key_128 = get_tag_random("key_128", 16)
# key_192 = get_tag_random("key_192", 24)
# iv_128 = get_tag_random("iv_128", 16)
# iv_64 = get_tag_random("iv_64", 8)
# data_128 = get_tag_random("data_128", 16)
#
# def BlockChainingTests_test_loopback_128():
#     cipher = AES.new(key_128, aes_mode, iv_128)
#     pt = get_tag_random("plaintext", 16 * 100)
#     ct = cipher.encrypt(pt)
#
#     cipher = AES.new(key_128, aes_mode, iv_128)
#     pt2 = cipher.decrypt(ct)
#     asserts.assert_that(pt).is_equal_to(pt2)
#
# def BlockChainingTests_test_loopback_64():
#     cipher = DES3.new(key_192, des3_mode, iv_64)
#     pt = get_tag_random("plaintext", 8 * 100)
#     ct = cipher.encrypt(pt)
#
#     cipher = DES3.new(key_192, des3_mode, iv_64)
#     pt2 = cipher.decrypt(ct)
#     asserts.assert_that(pt).is_equal_to(pt2)
#
# def BlockChainingTests_test_iv():
#     # If not passed, the iv is created randomly
#     cipher = AES.new(key_128, aes_mode)
#     iv1 = cipher.iv
#     cipher = AES.new(key_128, aes_mode)
#     iv2 = cipher.iv
#     asserts.assert_that(iv1).is_not_equal_to(iv2)
#     asserts.assert_that(len(iv1)).is_equal_to(16)
#
#     # IV can be passed in uppercase or lowercase
#     cipher = AES.new(key_128, aes_mode, iv_128)
#     ct = cipher.encrypt(data_128)
#
#     cipher = AES.new(key_128, aes_mode, iv=iv_128)
#     asserts.assert_that(ct).is_equal_to(cipher.encrypt(data_128))
#
#     cipher = AES.new(key_128, aes_mode, IV=iv_128)
#     asserts.assert_that(ct).is_equal_to(cipher.encrypt(data_128))
#
# def BlockChainingTests_test_iv_must_be_bytes():
#     asserts.assert_fails(lambda: AES.new(key_128, aes_mode,
#                       iv = u'test1234567890-*'), ".*?TypeError")
#
# def BlockChainingTests_test_only_one_iv():
#     # Only one IV/iv keyword allowed
#     asserts.assert_fails(lambda: AES.new(key_128, aes_mode,
#                       iv=iv_128, IV=iv_128), ".*?TypeError")
#
# def BlockChainingTests_test_iv_with_matching_length():
#     asserts.assert_fails(lambda: AES.new(key_128, aes_mode,
#                       b""), ".*?ValueError")
#     asserts.assert_fails(lambda: AES.new(key_128, aes_mode,
#                       iv_128[:15]), ".*?ValueError")
#     asserts.assert_fails(lambda: AES.new(key_128, aes_mode,
#                       iv_128 + b"0"), ".*?ValueError")
#
# def BlockChainingTests_test_block_size_128():
#     cipher = AES.new(key_128, aes_mode, iv_128)
#     asserts.assert_that(cipher.block_size).is_equal_to(AES.block_size)
#
# def BlockChainingTests_test_block_size_64():
#     cipher = DES3.new(key_192, des3_mode, iv_64)
#     asserts.assert_that(cipher.block_size).is_equal_to(DES3.block_size)
#
# def BlockChainingTests_test_unaligned_data_128():
#     cipher = AES.new(key_128, aes_mode, iv_128)
#     for wrong_length in range(1,16):
#         asserts.assert_fails(lambda: cipher.encrypt(b"5" * wrong_length), ".*?ValueError")
#
#     cipher = AES.new(key_128, aes_mode, iv_128)
#     for wrong_length in range(1,16):
#         asserts.assert_fails(lambda: cipher.decrypt(b"5" * wrong_length), ".*?ValueError")
#
# def BlockChainingTests_test_unaligned_data_64():
#     cipher = DES3.new(key_192, des3_mode, iv_64)
#     for wrong_length in range(1,8):
#         asserts.assert_fails(lambda: cipher.encrypt(b"5" * wrong_length), ".*?ValueError")
#
#     cipher = DES3.new(key_192, des3_mode, iv_64)
#     for wrong_length in range(1,8):
#         asserts.assert_fails(lambda: cipher.decrypt(b"5" * wrong_length), ".*?ValueError")
#
# def BlockChainingTests_test_IV_iv_attributes():
#     data = get_tag_random("data", 16 * 100)
#     for func in "encrypt", "decrypt":
#         cipher = AES.new(key_128, aes_mode, iv_128)
#         getattr(cipher, func)(data)
#         asserts.assert_that(cipher.iv).is_equal_to(iv_128)
#         asserts.assert_that(cipher.IV).is_equal_to(iv_128)
#
# def BlockChainingTests_test_unknown_parameters():
#     asserts.assert_fails(lambda: AES.new(key_128, aes_mode,
#                       iv_128, 7), ".*?TypeError")
#     asserts.assert_fails(lambda: AES.new(key_128, aes_mode,
#                       iv=iv_128, unknown=7), ".*?TypeError")
#     # But some are only known by the base cipher (e.g. use_aesni consumed by the AES module)
#     AES.new(key_128, aes_mode, iv=iv_128, use_aesni=False)
#
# def BlockChainingTests_test_null_encryption_decryption():
#     for func in "encrypt", "decrypt":
#         cipher = AES.new(key_128, aes_mode, iv_128)
#         result = getattr(cipher, func)(b"")
#         asserts.assert_that(result).is_equal_to(b"")
#
# def BlockChainingTests_test_either_encrypt_or_decrypt():
#     cipher = AES.new(key_128, aes_mode, iv_128)
#     cipher.encrypt(b"")
#     asserts.assert_fails(lambda: cipher.decrypt(b""), ".*?TypeError")
#
#     cipher = AES.new(key_128, aes_mode, iv_128)
#     cipher.decrypt(b"")
#     asserts.assert_fails(lambda: cipher.encrypt(b""), ".*?TypeError")
#
# def BlockChainingTests_test_data_must_be_bytes():
#     cipher = AES.new(key_128, aes_mode, iv_128)
#     asserts.assert_fails(lambda: cipher.encrypt(u'test1234567890-*'), ".*?TypeError")
#
#     cipher = AES.new(key_128, aes_mode, iv_128)
#     asserts.assert_fails(lambda: cipher.decrypt(u'test1234567890-*'), ".*?TypeError")
#
# def BlockChainingTests_test_bytearray():
#     data = b"1" * 128
#     data_ba = bytearray(data)
#
#     # Encrypt
#     key_ba = bytearray(key_128)
#     iv_ba = bytearray(iv_128)
#
#     cipher1 = AES.new(key_128, aes_mode, iv_128)
#     ref1 = cipher1.encrypt(data)
#
#     cipher2 = AES.new(key_ba, aes_mode, iv_ba)
#     key_ba[:3] = b'\xFF\xFF\xFF'
#     iv_ba[:3] = b'\xFF\xFF\xFF'
#     ref2 = cipher2.encrypt(data_ba)
#
#     asserts.assert_that(ref1).is_equal_to(ref2)
#     asserts.assert_that(cipher1.iv).is_equal_to(cipher2.iv)
#
#     # Decrypt
#     key_ba = bytearray(key_128)
#     iv_ba = bytearray(iv_128)
#
#     cipher3 = AES.new(key_128, aes_mode, iv_128)
#     ref3 = cipher3.decrypt(data)
#
#     cipher4 = AES.new(key_ba, aes_mode, iv_ba)
#     key_ba[:3] = b'\xFF\xFF\xFF'
#     iv_ba[:3] = b'\xFF\xFF\xFF'
#     ref4 = cipher4.decrypt(data_ba)
#
#     asserts.assert_that(ref3).is_equal_to(ref4)
#
# def BlockChainingTests_test_memoryview():
#     data = b"1" * 128
#     data_mv = memoryview(bytearray(data))
#
#     # Encrypt
#     key_mv = memoryview(bytearray(key_128))
#     iv_mv = memoryview(bytearray(iv_128))
#
#     cipher1 = AES.new(key_128, aes_mode, iv_128)
#     ref1 = cipher1.encrypt(data)
#
#     cipher2 = AES.new(key_mv, aes_mode, iv_mv)
#     key_mv[:3] = b'\xFF\xFF\xFF'
#     iv_mv[:3] = b'\xFF\xFF\xFF'
#     ref2 = cipher2.encrypt(data_mv)
#
#     asserts.assert_that(ref1).is_equal_to(ref2)
#     asserts.assert_that(cipher1.iv).is_equal_to(cipher2.iv)
#
#     # Decrypt
#     key_mv = memoryview(bytearray(key_128))
#     iv_mv = memoryview(bytearray(iv_128))
#
#     cipher3 = AES.new(key_128, aes_mode, iv_128)
#     ref3 = cipher3.decrypt(data)
#
#     cipher4 = AES.new(key_mv, aes_mode, iv_mv)
#     key_mv[:3] = b'\xFF\xFF\xFF'
#     iv_mv[:3] = b'\xFF\xFF\xFF'
#     ref4 = cipher4.decrypt(data_mv)
#
#     asserts.assert_that(ref3).is_equal_to(ref4)
#
# def BlockChainingTests_test_output_param():
#
#     pt = b'5' * 128
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     ct = cipher.encrypt(pt)
#
#     output = bytearray(128)
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     res = cipher.encrypt(pt, output=output)
#     asserts.assert_that(ct).is_equal_to(output)
#     asserts.assert_that(res).is_equal_to(None)
#
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     res = cipher.decrypt(ct, output=output)
#     asserts.assert_that(pt).is_equal_to(output)
#     asserts.assert_that(res).is_equal_to(None)
#
#
# def BlockChainingTests_test_output_param_same_buffer():
#
#     pt = b'5' * 128
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     ct = cipher.encrypt(pt)
#
#     pt_ba = bytearray(pt)
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     res = cipher.encrypt(pt_ba, output=pt_ba)
#     asserts.assert_that(ct).is_equal_to(pt_ba)
#     asserts.assert_that(res).is_equal_to(None)
#
#     ct_ba = bytearray(ct)
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     res = cipher.decrypt(ct_ba, output=ct_ba)
#     asserts.assert_that(pt).is_equal_to(ct_ba)
#     asserts.assert_that(res).is_equal_to(None)
#
#
# def BlockChainingTests_test_output_param_memoryview():
#
#     pt = b'5' * 128
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     ct = cipher.encrypt(pt)
#
#     output = memoryview(bytearray(128))
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     cipher.encrypt(pt, output=output)
#     asserts.assert_that(ct).is_equal_to(output)
#
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     cipher.decrypt(ct, output=output)
#     asserts.assert_that(pt).is_equal_to(output)
#
# def BlockChainingTests_test_output_param_neg():
#     LEN_PT = 128
#
#     pt = b'5' * LEN_PT
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     ct = cipher.encrypt(pt)
#
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     asserts.assert_fails(lambda: cipher.encrypt(pt, output=b'0' * LEN_PT), ".*?TypeError")
#
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     asserts.assert_fails(lambda: cipher.decrypt(ct, output=b'0' * LEN_PT), ".*?TypeError")
#
#     shorter_output = bytearray(LEN_PT - 1)
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     asserts.assert_fails(lambda: cipher.encrypt(pt, output=shorter_output), ".*?ValueError")
#     cipher = AES.new(b'4'*16, aes_mode, iv=iv_128)
#     asserts.assert_fails(lambda: cipher.decrypt(ct, output=shorter_output), ".*?ValueError")
#
# def NistBlockChainingVectors__do_kat_aes_test(file_name):
#
#     test_vectors = load_test_vectors(("Cipher", "AES"),
#                         file_name,
#                         "AES CBC KAT",
#                         { "count" : lambda x: int(x) } )
#     if test_vectors == None:
#         return
#
#     direction = None
#     for tv in test_vectors:
#
#         # The test vector file contains some directive lines
#         if is_string(tv):
#             direction = tv
#             continue
#
#         description = tv.desc
#
#         cipher = AES.new(tv.key, aes_mode, tv.iv)
#         if direction == "[ENCRYPT]":
#             asserts.assert_that(cipher.encrypt(tv.plaintext)).is_equal_to(tv.ciphertext)
#         elif direction == "[DECRYPT]":
#             asserts.assert_that(cipher.decrypt(tv.ciphertext)).is_equal_to(tv.plaintext)
#         else:
#             assert False
#
#     # See Section 6.4.2 in AESAVS
# def NistBlockChainingVectors__do_mct_aes_test(file_name):
#
#     test_vectors = load_test_vectors(("Cipher", "AES"),
#                         file_name,
#                         "AES CBC Montecarlo",
#                         { "count" : lambda x: int(x) } )
#     if test_vectors == None:
#         return
#
#     direction = None
#     for tv in test_vectors:
#
#         # The test vector file contains some directive lines
#         if is_string(tv):
#             direction = tv
#             continue
#
#         description = tv.desc
#         cipher = AES.new(tv.key, aes_mode, tv.iv)
#
#         if direction == '[ENCRYPT]':
#             cts = [ tv.iv ]
#             for count in range(1000):
#                 cts.append(cipher.encrypt(tv.plaintext))
#                 tv.plaintext = cts[-2]
#             asserts.assert_that(cts[-1]).is_equal_to(tv.ciphertext)
#         elif direction == '[DECRYPT]':
#             pts = [ tv.iv]
#             for count in range(1000):
#                 pts.append(cipher.decrypt(tv.ciphertext))
#                 tv.ciphertext = pts[-2]
#             asserts.assert_that(pts[-1]).is_equal_to(tv.plaintext)
#         else:
#             assert False
#
# def NistBlockChainingVectors__do_tdes_test(file_name):
#
#     test_vectors = load_test_vectors(("Cipher", "TDES"),
#                         file_name,
#                         "TDES CBC KAT",
#                         { "count" : lambda x: int(x) } )
#     if test_vectors == None:
#         return
#
#     direction = None
#     for tv in test_vectors:
#
#         # The test vector file contains some directive lines
#         if is_string(tv):
#             direction = tv
#             continue
#
#         description = tv.desc
#         if hasattr(tv, "keys"):
#             cipher = DES.new(tv.keys, des_mode, tv.iv)
#         else:
#             if tv.key1 != tv.key3:
#                 key = tv.key1 + tv.key2 + tv.key3  # Option 3
#             else:
#                 key = tv.key1 + tv.key2            # Option 2
#             cipher = DES3.new(key, des3_mode, tv.iv)
#
#         if direction == "[ENCRYPT]":
#             asserts.assert_that(cipher.encrypt(tv.plaintext)).is_equal_to(tv.ciphertext)
#         elif direction == "[DECRYPT]":
#             asserts.assert_that(cipher.decrypt(tv.ciphertext)).is_equal_to(tv.plaintext)
#         else:
#             assert False
#
#
# # class NistCbcVectors(NistBlockChainingVectors):
# #     aes_mode = AES.MODE_CBC
# #     des_mode = DES.MODE_CBC
# #     des3_mode = DES3.MODE_CBC
#
#
# # Create one test method per file
# nist_aes_kat_mmt_files = (
#     # KAT
#     "CBCGFSbox128.rsp",
#     "CBCGFSbox192.rsp",
#     "CBCGFSbox256.rsp",
#     "CBCKeySbox128.rsp",
#     "CBCKeySbox192.rsp",
#     "CBCKeySbox256.rsp",
#     "CBCVarKey128.rsp",
#     "CBCVarKey192.rsp",
#     "CBCVarKey256.rsp",
#     "CBCVarTxt128.rsp",
#     "CBCVarTxt192.rsp",
#     "CBCVarTxt256.rsp",
#     # MMT
#     "CBCMMT128.rsp",
#     "CBCMMT192.rsp",
#     "CBCMMT256.rsp",
#     )
# nist_aes_mct_files = (
#     "CBCMCT128.rsp",
#     "CBCMCT192.rsp",
#     "CBCMCT256.rsp",
#     )
#
# for file_name in nist_aes_kat_mmt_files:
#     def NistBlockChainingVectors_new_func(file_name=file_name):
#         _do_kat_aes_test(file_name)
#     setattr(NistCbcVectors, "test_AES_" + file_name, new_func)
#
# for file_name in nist_aes_mct_files:
#     def NistBlockChainingVectors_new_func(file_name=file_name):
#         _do_mct_aes_test(file_name)
#     setattr(NistCbcVectors, "test_AES_" + file_name, new_func)
# del file_name, new_func
#
# nist_tdes_files = (
#     "TCBCMMT2.rsp",    # 2TDES
#     "TCBCMMT3.rsp",    # 3TDES
#     "TCBCinvperm.rsp", # Single DES
#     "TCBCpermop.rsp",
#     "TCBCsubtab.rsp",
#     "TCBCvarkey.rsp",
#     "TCBCvartext.rsp",
#     )
#
# for file_name in nist_tdes_files:
#     def NistBlockChainingVectors_new_func(file_name=file_name):
#         _do_tdes_test(file_name)
#     setattr(NistCbcVectors, "test_TDES_" + file_name, new_func)
# """Class exercising the CBC test vectors found in Section F.2
#     of NIST SP 800-3A"""
#
# def SP800TestVectors_test_aes_128():
#     key =           '2b7e151628aed2a6abf7158809cf4f3c'
#     iv =            '000102030405060708090a0b0c0d0e0f'
#     plaintext =     '6bc1bee22e409f96e93d7e117393172a' +\
#                         'ae2d8a571e03ac9c9eb76fac45af8e51' +\
#                         '30c81c46a35ce411e5fbc1191a0a52ef' +\
#                         'f69f2445df4f9b17ad2b417be66c3710'
#     ciphertext =    '7649abac8119b246cee98e9b12e9197d' +\
#                         '5086cb9b507219ee95db113a917678b2' +\
#                         '73bed6b8e3c1743b7116e69e22229516' +\
#                         '3ff1caa1681fac09120eca307586e1a7'
#
#     key = unhexlify(key)
#     iv = unhexlify(iv)
#     plaintext = unhexlify(plaintext)
#     ciphertext = unhexlify(ciphertext)
#
#     cipher = AES.new(key, AES.MODE_CBC, iv)
#     asserts.assert_that(cipher.encrypt(plaintext)).is_equal_to(ciphertext)
#     cipher = AES.new(key, AES.MODE_CBC, iv)
#     asserts.assert_that(cipher.decrypt(ciphertext)).is_equal_to(plaintext)
#
# def SP800TestVectors_test_aes_192():
#     key =           '8e73b0f7da0e6452c810f32b809079e562f8ead2522c6b7b'
#     iv =            '000102030405060708090a0b0c0d0e0f'
#     plaintext =     '6bc1bee22e409f96e93d7e117393172a' +\
#                         'ae2d8a571e03ac9c9eb76fac45af8e51' +\
#                         '30c81c46a35ce411e5fbc1191a0a52ef' +\
#                         'f69f2445df4f9b17ad2b417be66c3710'
#     ciphertext =    '4f021db243bc633d7178183a9fa071e8' +\
#                         'b4d9ada9ad7dedf4e5e738763f69145a' +\
#                         '571b242012fb7ae07fa9baac3df102e0' +\
#                         '08b0e27988598881d920a9e64f5615cd'
#
#     key = unhexlify(key)
#     iv = unhexlify(iv)
#     plaintext = unhexlify(plaintext)
#     ciphertext = unhexlify(ciphertext)
#
#     cipher = AES.new(key, AES.MODE_CBC, iv)
#     asserts.assert_that(cipher.encrypt(plaintext)).is_equal_to(ciphertext)
#     cipher = AES.new(key, AES.MODE_CBC, iv)
#     asserts.assert_that(cipher.decrypt(ciphertext)).is_equal_to(plaintext)
#
# def SP800TestVectors_test_aes_256():
#     key =           '603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4'
#     iv =            '000102030405060708090a0b0c0d0e0f'
#     plaintext =     '6bc1bee22e409f96e93d7e117393172a' +\
#                         'ae2d8a571e03ac9c9eb76fac45af8e51' +\
#                         '30c81c46a35ce411e5fbc1191a0a52ef' +\
#                         'f69f2445df4f9b17ad2b417be66c3710'
#     ciphertext =    'f58c4c04d6e5f1ba779eabfb5f7bfbd6' +\
#                         '9cfc4e967edb808d679f777bc6702c7d' +\
#                         '39f23369a9d9bacfa530e26304231461' +\
#                         'b2eb05e2c39be9fcda6c19078c6a9d1b'
#
#     key = unhexlify(key)
#     iv = unhexlify(iv)
#     plaintext = unhexlify(plaintext)
#     ciphertext = unhexlify(ciphertext)
#
#     cipher = AES.new(key, AES.MODE_CBC, iv)
#     asserts.assert_that(cipher.encrypt(plaintext)).is_equal_to(ciphertext)
#     cipher = AES.new(key, AES.MODE_CBC, iv)
#     asserts.assert_that(cipher.decrypt(ciphertext)).is_equal_to(plaintext)
#
#
# def SP800TestVectors_get_tests(config={}):
#     tests = []
#     tests += list_test_cases(CbcTests)
#     if config.get('slow_tests'):
#         tests += list_test_cases(NistCbcVectors)
#     tests += list_test_cases(SP800TestVectors)
#     return tests
#
# def _testsuite():
#     _suite = unittest.TestSuite()
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_loopback_128))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_loopback_64))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_iv))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_iv_must_be_bytes))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_only_one_iv))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_iv_with_matching_length))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_block_size_128))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_block_size_64))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_unaligned_data_128))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_unaligned_data_64))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_IV_iv_attributes))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_unknown_parameters))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_null_encryption_decryption))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_either_encrypt_or_decrypt))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_data_must_be_bytes))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_bytearray))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_memoryview))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_output_param))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_output_param_same_buffer))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_output_param_memoryview))
#     _suite.addTest(unittest.FunctionTestCase(BlockChainingTests_test_output_param_neg))
#     _suite.addTest(unittest.FunctionTestCase(NistBlockChainingVectors__do_kat_aes_test))
#     _suite.addTest(unittest.FunctionTestCase(NistBlockChainingVectors__do_mct_aes_test))
#     _suite.addTest(unittest.FunctionTestCase(NistBlockChainingVectors__do_tdes_test))
#     _suite.addTest(unittest.FunctionTestCase(SP800TestVectors_test_aes_128))
#     _suite.addTest(unittest.FunctionTestCase(SP800TestVectors_test_aes_192))
#     _suite.addTest(unittest.FunctionTestCase(SP800TestVectors_test_aes_256))
#     _suite.addTest(unittest.FunctionTestCase(SP800TestVectors_get_tests))
#     return _suite
#
# _runner = unittest.TextTestRunner()
# _runner.run(_testsuite())
