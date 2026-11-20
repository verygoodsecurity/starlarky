# Ported from pydukpt 0.1.0 (3778da71): tests/test_compat.py. See larky/src/main/resources/VENDORED.md.
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
"""pydukpt.compat against dukpt 1.0.1's API and recorded 1.0.1 outputs."""
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//pydukpt", pydukpt="pydukpt")
load("@vendor//pydukpt/compat", "BDK_LEN", "DUKPT", "KSN_LEN", "Client", "InvalidDUKPTArguments", "Server")
load("./conftest", VECTORS="VECTORS", h="h")

BDK = h("0123456789ABCDEFFEDCBA9876543210")
KSN = h("FFFF9876543210E00000")
IPEK = h("6AC292FAA1315B4D858AB3A3D7D5933A")
UPSTREAM = VECTORS["dukpt_1_0_1"]
EXPECTED = {(c["bdk"], c["ksn"]): c for c in VECTORS["cases"]}
_EXHAUSTED = ".*?KSNExhaustedError: KSN counter exhausted"


def FakeBitArray(data):
    """Stands in for bitstring.BitArray, which exposes its value as .bytes."""
    return larky.struct(__name__='FakeBitArray', bytes=data)


def _ctr(ksn_hex):
    return pydukpt.ksn_counter(h(ksn_hex))


LOW = [u for u in UPSTREAM if _ctr(u["ksn"]) < 0x200 and not h(u["ksn"])[7] & 0x1F]


def test_upstream_fixture_shape():
    asserts.assert_that(len(LOW) >= 50).is_true()
    asserts.assert_that(any([_ctr(u["ksn"]) >= 0x200 for u in UPSTREAM])).is_true()


def test_agrees_with_1_0_1_below_0x200(u):
    server = Server(h(u["bdk"]))
    asserts.assert_that(server.generate_ipek(h(u["ksn"]))).is_equal_to(h(u["ipek"]))
    asserts.assert_that(server.gen_key(h(u["ksn"]))).is_equal_to(h(u["key"]))


def test_1_0_1_is_wrong_from_0x200():
    # Every recorded 1.0.1 key for a counter with a bit >= 0x200 set differs
    # from moov-io/dukpt; ours matches moov-io/dukpt.
    high = [u for u in UPSTREAM if _ctr(u["ksn"]) >= 0x200]
    asserts.assert_that(len(high) > 0).is_true()
    for u in high:
        expected = EXPECTED[(u["bdk"], u["ksn"])]
        asserts.assert_that(u["key"]).is_not_equal_to(expected["key"])
        asserts.assert_that(Server(h(u["bdk"])).gen_key(h(u["ksn"]))).is_equal_to(h(expected["key"]))
    # 1.0.1 returns the IPEK itself for counters 0x200 and 0x400.
    for c in (0x200, 0x400):
        ksn = pydukpt.ksn_with_counter(KSN, c).hex()
        u = [u for u in UPSTREAM if u["ksn"] == ksn and u["bdk"] == BDK.hex()][0]
        asserts.assert_that(u["key"]).is_equal_to(u["ipek"])


def test_1_0_1_right_ipek_uses_counter_bits():
    # Counter bit 16 or above set: 1.0.1's IPEK differs; ours ignores the counter.
    wrong = [u for u in UPSTREAM if h(u["ksn"])[7] & 0x1F]
    asserts.assert_that(len(wrong) > 0).is_true()
    for u in wrong:
        expected = EXPECTED[(u["bdk"], u["ksn"])]
        asserts.assert_that(u["ipek"][:16]).is_equal_to(expected["ipek"][:16])
        asserts.assert_that(u["ipek"][16:]).is_not_equal_to(expected["ipek"][16:])
        asserts.assert_that(Server(h(u["bdk"])).generate_ipek(h(u["ksn"]))).is_equal_to(h(expected["ipek"]))


# test_live_dukpt_1_0_1_if_installed: not ported. It runs only when dukpt and
# bitstring are installed (pytest.importorskip); Larky has neither. The
# recorded 1.0.1 outputs above cover the same points.


def test_module_constants():
    # Larky: DUKPT is a constructor function; its class attributes are read
    # from an instance.
    d = DUKPT(bdk=BDK)
    asserts.assert_that(BDK_LEN).is_equal_to(16)
    asserts.assert_that(d.BDK_LEN).is_equal_to(16)
    asserts.assert_that(KSN_LEN).is_equal_to(10)
    asserts.assert_that(d.KSN_LEN).is_equal_to(10)
    asserts.assert_that(d._pin_mask).is_equal_to(pydukpt.PIN_VARIANT)
    asserts.assert_that(d._mac_req_mask).is_equal_to(h("000000000000FF00000000000000FF00"))
    asserts.assert_that(d._mac_resp_mask).is_equal_to(h("00000000FF00000000000000FF000000"))
    asserts.assert_that(d._mac_data_req).is_equal_to(h("0000000000FF00000000000000FF0000"))
    asserts.assert_that(d._mac_data_resp).is_equal_to(h("000000FF00000000000000FF00000000"))


def test_server_methods():
    server = Server(BDK)
    ksn = h("FFFF9876543210E00003")
    asserts.assert_that(server.generate_ipek(ksn)).is_equal_to(IPEK)
    asserts.assert_that(server._tdes_key).is_equal_to(BDK + BDK[:8])
    server.generate_left_ipek(ksn)
    asserts.assert_that(server._ipek).is_equal_to(IPEK[:8])
    server.generate_right_ipek(ksn)
    asserts.assert_that(server._ipek).is_equal_to(IPEK)
    asserts.assert_that(server.gen_key(ksn)).is_equal_to(h("0DF3D9422ACA56E547676D07AD6BADFA"))
    asserts.assert_that(server._cur_key).is_equal_to(h("0DF3D9422ACA56E547676D07AD6BADFA"))
    asserts.assert_that(server.derive_key(IPEK, ksn)).is_equal_to(h("0DF3D9422ACA56E547676D07AD6BADFA"))


def test_bitarray_like_arguments():
    server = Server(FakeBitArray(BDK))
    ksn = FakeBitArray(h("FFFF9876543210E00001"))
    asserts.assert_that(server.gen_key(ksn)).is_equal_to(h("042666B49184CFA368DE9628D0397BC9"))
    asserts.assert_that(server.derive_key(FakeBitArray(IPEK), ksn)).is_equal_to(h("042666B49184CFA368DE9628D0397BC9"))
    client = Client(FakeBitArray(IPEK), FakeBitArray(KSN))
    asserts.assert_that(client.gen_key()["key"]).is_equal_to(h("042666B49184CFA368DE9628D0397BC9"))


def test_random_server():
    server = Server()
    asserts.assert_that(len(server.bdk)).is_equal_to(16)
    asserts.assert_that(server._bdk).is_equal_to(server.bdk)
    ksn = server.generate_ksn()
    asserts.assert_that(len(ksn)).is_equal_to(10)
    asserts.assert_that(pydukpt.ksn_counter(ksn)).is_equal_to(0)
    asserts.assert_that(len(server.generate_bdk())).is_equal_to(16)


def test_counter_helpers():
    d = DUKPT(bdk=BDK)
    asserts.assert_that(d.reset_counter(h("FFFF9876543210FFFFFF"))).is_equal_to(KSN)
    asserts.assert_that(d.reset_counter(h("FFFF"))).is_none()
    asserts.assert_that(d.copy_counter(h("FFFF9876543210FFFFFF"))).is_equal_to(h("1FFFFF"))
    asserts.assert_that(d.copy_counter(h("E00005"))).is_equal_to(h("000005"))
    asserts.assert_fails(lambda: d.copy_counter(h("FF")), ".*?ValueError: .*?at least 3 bytes")


def test_client_sequence():
    client = Client(IPEK, KSN)  # advances to counter 1 on construction
    expected = [
        ("FFFF9876543210E00001", "042666B49184CFA368DE9628D0397BC9"),
        ("FFFF9876543210E00002", "C46551CEF9FD24B0AA9AD834130D3BC7"),
        ("FFFF9876543210E00003", "0DF3D9422ACA56E547676D07AD6BADFA"),
        ("FFFF9876543210E00004", "279C0F6AEED0BE652B2C733E1383AE91"),
        ("FFFF9876543210E00005", "5F8DC6D2C845C125508DDC048093B83F"),
    ]
    for ksn, key in expected:
        asserts.assert_that(client.gen_key()).is_equal_to({"ksn": h(ksn), "key": h(key)})


def test_client_skips_counters_with_more_than_ten_one_bits():
    client = Client(IPEK, pydukpt.ksn_with_counter(KSN, 0x7FD))
    asserts.assert_that(pydukpt.ksn_counter(client.gen_key()["ksn"])).is_equal_to(0x7FE)
    asserts.assert_that(pydukpt.ksn_counter(client.gen_key()["ksn"])).is_equal_to(0x800)  # not 0x7FF


def test_client_exhaustion():
    last = pydukpt.ksn_with_counter(KSN, 0x1FF7FF)
    client = Client(IPEK, last)
    info = client.gen_key()
    asserts.assert_that(pydukpt.ksn_counter(info["ksn"])).is_equal_to(pydukpt.MAX_COUNTER)
    asserts.assert_that(info["key"]).is_equal_to(pydukpt.derive_key(IPEK, info["ksn"]))
    asserts.assert_fails(lambda: client.gen_key(), _EXHAUSTED)
    asserts.assert_fails(lambda: Client(IPEK, pydukpt.ksn_with_counter(KSN, pydukpt.MAX_COUNTER)), _EXHAUSTED)


def test_client_and_server_agree():
    server = Server(BDK)
    client = Client(server.generate_ipek(KSN), KSN)
    for _ in range(20):
        info = client.gen_key()
        asserts.assert_that(server.gen_key(info["ksn"])).is_equal_to(info["key"])


_INVALID_ARGUMENTS = [
    ({}, "Must have either ipek or bdk"),
    ({"bdk": b""}, "Must have either ipek or bdk"),
    ({"bdk": BDK[:15]}, "BDK must have a length of 16"),
    ({"ipek": IPEK[:8], "ksn": KSN}, "IPEK must have a length of 16"),
    ({"ipek": IPEK, "ksn": KSN[:8]}, "KSN must have a length of 10"),
    ({"ipek": IPEK}, "Must have a ksn"),
]


def test_invalid_arguments(kwargs, match):
    asserts.assert_fails(lambda: DUKPT(**kwargs), ".*?InvalidDUKPTArguments: " + match)
    # issubclass(InvalidDUKPTArguments, ValueError): Larky has no classes; the
    # failure names the exception, and calling it fails the same way.
    asserts.assert_fails(lambda: InvalidDUKPTArguments("x"), ".*?InvalidDUKPTArguments: x")


def test_server_rejects_empty_bdk():
    asserts.assert_fails(lambda: Server(b""), ".*?InvalidDUKPTArguments: ")


def test_type_errors():
    asserts.assert_fails(lambda: Server(BDK).gen_key(KSN.hex()), ".*?TypeError: ")
    asserts.assert_fails(lambda: DUKPT(bdk=BDK.hex()), ".*?TypeError: ")


def test_method_length_errors():
    asserts.assert_fails(lambda: Server(BDK).gen_key(KSN[:8]), ".*?ValueError: ksn must be 10 bytes")


def test_increase_counter_without_ksn():
    asserts.assert_fails(lambda: DUKPT(bdk=BDK).increase_counter(), ".*?ValueError: no KSN")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_upstream_fixture_shape))
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase, "u", LOW)(test_agrees_with_1_0_1_below_0x200)
    _suite.addTest(unittest.FunctionTestCase(test_1_0_1_is_wrong_from_0x200))
    _suite.addTest(unittest.FunctionTestCase(test_1_0_1_right_ipek_uses_counter_bits))
    _suite.addTest(unittest.FunctionTestCase(test_module_constants))
    _suite.addTest(unittest.FunctionTestCase(test_server_methods))
    _suite.addTest(unittest.FunctionTestCase(test_bitarray_like_arguments))
    _suite.addTest(unittest.FunctionTestCase(test_random_server))
    _suite.addTest(unittest.FunctionTestCase(test_counter_helpers))
    _suite.addTest(unittest.FunctionTestCase(test_client_sequence))
    _suite.addTest(unittest.FunctionTestCase(test_client_skips_counters_with_more_than_ten_one_bits))
    _suite.addTest(unittest.FunctionTestCase(test_client_exhaustion))
    _suite.addTest(unittest.FunctionTestCase(test_client_and_server_agree))
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase,
                      "kwargs,match", _INVALID_ARGUMENTS)(test_invalid_arguments)
    _suite.addTest(unittest.FunctionTestCase(test_server_rejects_empty_bdk))
    _suite.addTest(unittest.FunctionTestCase(test_type_errors))
    _suite.addTest(unittest.FunctionTestCase(test_method_length_errors))
    _suite.addTest(unittest.FunctionTestCase(test_increase_counter_without_ksn))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
