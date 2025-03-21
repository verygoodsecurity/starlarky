load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@vendor//asserts", asserts="asserts")

load("data_test_fixtures", get_file_contents="get_file_contents")


def readLocalFile(filename):
    return get_file_contents(filename)


def TestASCIIArmor_test_unarmor_one():
    armored = readLocalFile("helloKey.asc")
    _, unarmored = OpenPGP.unarmor(armored)[0]
    message = OpenPGP.Message.parse(unarmored)
    asserts.assert_that(message[0].fingerprint()).is_equal_to(
        "421F28FEAAD222F856C8FFD5D4D54EA16F87040E"
    )


def TestASCIIArmor_test_enarmor_one():
    expected = readLocalFile("helloKey.asc")
    messages = OpenPGP.unarmor(expected)  # [(header, data), ...]
    header, data = messages[0]
    actual = OpenPGP.enarmor(
        data, headers=[keyValue.split(": ", 1) for keyValue in header.split("\n")]
    )
    asserts.assert_that(actual).is_equal_to(expected)


def TestASCIIArmor_test_enarmor_headers_none():
    expected = readLocalFile("helloKey.message")
    messages = OpenPGP.unarmor(readLocalFile("helloKey.asc"))
    header, data = messages[0]
    actual = OpenPGP.enarmor(data, marker='MESSAGE', headers = None)
    asserts.assert_that(actual).is_equal_to(expected)


def one_serialization(path):
    inm = OpenPGP.Message.parse(
        get_file_contents(path)
    )
    mid = inm.to_bytes()
    out = OpenPGP.Message.parse(mid)
    inm.force()
    out.force()
    asserts.assert_that(inm).is_equal_to(out)


def TestSerialization_test000001006public_key():
    one_serialization("000001-006.public_key")


def TestSerialization_test000002013user_id():
    one_serialization("000002-013.user_id")


def TestSerialization_test000003002sig():
    one_serialization("000003-002.sig")


def TestSerialization_test000004012ring_trust():
    one_serialization("000004-012.ring_trust")


def TestSerialization_test000005002sig():
    one_serialization("000005-002.sig")


def TestSerialization_test000006012ring_trust():
    one_serialization("000006-012.ring_trust")


def TestSerialization_test000007002sig():
    one_serialization("000007-002.sig")


def TestSerialization_test000008012ring_trust():
    one_serialization("000008-012.ring_trust")


def TestSerialization_test000009002sig():
    one_serialization("000009-002.sig")


def TestSerialization_test000010012ring_trust():
    one_serialization("000010-012.ring_trust")


def TestSerialization_test000011002sig():
    one_serialization("000011-002.sig")


def TestSerialization_test000012012ring_trust():
    one_serialization("000012-012.ring_trust")


def TestSerialization_test000013014public_subkey():
    one_serialization("000013-014.public_subkey")


def TestSerialization_test000014002sig():
    one_serialization("000014-002.sig")


def TestSerialization_test000015012ring_trust():
    one_serialization("000015-012.ring_trust")


def TestSerialization_test000016006public_key():
    one_serialization("000016-006.public_key")


def TestSerialization_test000017002sig():
    one_serialization("000017-002.sig")


def TestSerialization_test000018012ring_trust():
    one_serialization("000018-012.ring_trust")


def TestSerialization_test000019013user_id():
    one_serialization("000019-013.user_id")


def TestSerialization_test000020002sig():
    one_serialization("000020-002.sig")


def TestSerialization_test000021012ring_trust():
    one_serialization("000021-012.ring_trust")


def TestSerialization_test000022002sig():
    one_serialization("000022-002.sig")


def TestSerialization_test000023012ring_trust():
    one_serialization("000023-012.ring_trust")


def TestSerialization_test000024014public_subkey():
    one_serialization("000024-014.public_subkey")


def TestSerialization_test000025002sig():
    one_serialization("000025-002.sig")


def TestSerialization_test000026012ring_trust():
    one_serialization("000026-012.ring_trust")


def TestSerialization_test000027006public_key():
    one_serialization("000027-006.public_key")


def TestSerialization_test000028002sig():
    one_serialization("000028-002.sig")


def TestSerialization_test000029012ring_trust():
    one_serialization("000029-012.ring_trust")


def TestSerialization_test000030013user_id():
    one_serialization("000030-013.user_id")


def TestSerialization_test000031002sig():
    one_serialization("000031-002.sig")


def TestSerialization_test000032012ring_trust():
    one_serialization("000032-012.ring_trust")


def TestSerialization_test000033002sig():
    one_serialization("000033-002.sig")


def TestSerialization_test000034012ring_trust():
    one_serialization("000034-012.ring_trust")


def TestSerialization_test000035006public_key():
    one_serialization("000035-006.public_key")


def TestSerialization_test000036013user_id():
    one_serialization("000036-013.user_id")


def TestSerialization_test000037002sig():
    one_serialization("000037-002.sig")


def TestSerialization_test000038012ring_trust():
    one_serialization("000038-012.ring_trust")


def TestSerialization_test000039002sig():
    one_serialization("000039-002.sig")


def TestSerialization_test000040012ring_trust():
    one_serialization("000040-012.ring_trust")


def TestSerialization_test000041017attribute():
    one_serialization("000041-017.attribute")


def TestSerialization_test000042002sig():
    one_serialization("000042-002.sig")


def TestSerialization_test000043012ring_trust():
    one_serialization("000043-012.ring_trust")


def TestSerialization_test000044014public_subkey():
    one_serialization("000044-014.public_subkey")


def TestSerialization_test000045002sig():
    one_serialization("000045-002.sig")


def TestSerialization_test000046012ring_trust():
    one_serialization("000046-012.ring_trust")


def TestSerialization_test000047005secret_key():
    one_serialization("000047-005.secret_key")


def TestSerialization_test000048013user_id():
    one_serialization("000048-013.user_id")


def TestSerialization_test000049002sig():
    one_serialization("000049-002.sig")


def TestSerialization_test000050012ring_trust():
    one_serialization("000050-012.ring_trust")


def TestSerialization_test000051007secret_subkey():
    one_serialization("000051-007.secret_subkey")


def TestSerialization_test000052002sig():
    one_serialization("000052-002.sig")


def TestSerialization_test000053012ring_trust():
    one_serialization("000053-012.ring_trust")


def TestSerialization_test000054005secret_key():
    one_serialization("000054-005.secret_key")


def TestSerialization_test000055002sig():
    one_serialization("000055-002.sig")


def TestSerialization_test000056012ring_trust():
    one_serialization("000056-012.ring_trust")


def TestSerialization_test000057013user_id():
    one_serialization("000057-013.user_id")


def TestSerialization_test000058002sig():
    one_serialization("000058-002.sig")


def TestSerialization_test000059012ring_trust():
    one_serialization("000059-012.ring_trust")


def TestSerialization_test000060007secret_subkey():
    one_serialization("000060-007.secret_subkey")


def TestSerialization_test000061002sig():
    one_serialization("000061-002.sig")


def TestSerialization_test000062012ring_trust():
    one_serialization("000062-012.ring_trust")


def TestSerialization_test000063005secret_key():
    one_serialization("000063-005.secret_key")


def TestSerialization_test000064002sig():
    one_serialization("000064-002.sig")


def TestSerialization_test000065012ring_trust():
    one_serialization("000065-012.ring_trust")


def TestSerialization_test000066013user_id():
    one_serialization("000066-013.user_id")


def TestSerialization_test000067002sig():
    one_serialization("000067-002.sig")


def TestSerialization_test000068012ring_trust():
    one_serialization("000068-012.ring_trust")


def TestSerialization_test000069005secret_key():
    one_serialization("000069-005.secret_key")


def TestSerialization_test000070013user_id():
    one_serialization("000070-013.user_id")


def TestSerialization_test000071002sig():
    one_serialization("000071-002.sig")


def TestSerialization_test000072012ring_trust():
    one_serialization("000072-012.ring_trust")


def TestSerialization_test000073017attribute():
    one_serialization("000073-017.attribute")


def TestSerialization_test000074002sig():
    one_serialization("000074-002.sig")


def TestSerialization_test000075012ring_trust():
    one_serialization("000075-012.ring_trust")


def TestSerialization_test000076007secret_subkey():
    one_serialization("000076-007.secret_subkey")


def TestSerialization_test000077002sig():
    one_serialization("000077-002.sig")


def TestSerialization_test000078012ring_trust():
    one_serialization("000078-012.ring_trust")


def TestSerialization_test002182002sig():
    one_serialization("002182-002.sig")


def TestSerialization_testpubringgpg():
    one_serialization("pubring.gpg")


def TestSerialization_testsecringgpg():
    one_serialization("secring.gpg")


def TestSerialization_testcompressedsiggpg():
    one_serialization("compressedsig.gpg")


def TestSerialization_testcompressedsigzlibgpg():
    one_serialization("compressedsig-zlib.gpg")


def TestSerialization_testcompressedsigbzip2gpg():
    one_serialization("compressedsig-bzip2.gpg")


def TestSerialization_testonepass_sig():
    one_serialization("onepass_sig")


def TestSerialization_testsymmetrically_encrypted():
    one_serialization("symmetrically_encrypted")


def TestSerialization_testuncompressedopsdsagpg():
    one_serialization("uncompressed-ops-dsa.gpg")


def TestSerialization_testuncompressedopsdsasha384txtgpg():
    one_serialization("uncompressed-ops-dsa-sha384.txt.gpg")


def TestSerialization_testuncompressedopsrsagpg():
    one_serialization("uncompressed-ops-rsa.gpg")


def TestSerialization_testSymmetricAES():
    one_serialization("symmetric-aes.gpg")


def TestSerialization_testSymmetricNoMDC():
    one_serialization("symmetric-no-mdc.gpg")


def TestUserID_test_name_comment_email_id():
    packet = OpenPGP.UserIDPacket("Human Name (With Comment) <and@email.com>")
    asserts.assert_that(packet.name).is_equal_to("Human Name")
    asserts.assert_that(packet.comment).is_equal_to("With Comment")
    asserts.assert_that(packet.email).is_equal_to("and@email.com")


def TestUserID_test_name_email_id():
    packet = OpenPGP.UserIDPacket("Human Name <and@email.com>")
    asserts.assert_that(packet.name).is_equal_to("Human Name")
    asserts.assert_that(packet.comment).is_equal_to(None)
    asserts.assert_that(packet.email).is_equal_to("and@email.com")


def TestUserID_test_name_id():
    packet = OpenPGP.UserIDPacket("Human Name")
    asserts.assert_that(packet.name).is_equal_to("Human Name")
    asserts.assert_that(packet.comment).is_equal_to(None)
    asserts.assert_that(packet.email).is_equal_to(None)


def TestUserID_test_email_id():
    packet = OpenPGP.UserIDPacket("<and@email.com>")
    asserts.assert_that(packet.name).is_equal_to(None)
    asserts.assert_that(packet.comment).is_equal_to(None)
    asserts.assert_that(packet.email).is_equal_to("and@email.com")


def one_fingerprint(path, kf):
    m = OpenPGP.Message.parse(
        get_file_contents(path)
    )
    asserts.assert_that(m[0].fingerprint()).is_equal_to(kf)


def TestFingerprint_test000001006public_key():
    one_fingerprint("000001-006.public_key", "421F28FEAAD222F856C8FFD5D4D54EA16F87040E")


def TestFingerprint_test000016006public_key():
    one_fingerprint("000016-006.public_key", "AF95E4D7BAC521EE9740BED75E9F1523413262DC")


def TestFingerprint_test000027006public_key():
    one_fingerprint("000027-006.public_key", "1EB20B2F5A5CC3BEAFD6E5CB7732CF988A63EA86")


def TestFingerprint_test000035006public_key():
    one_fingerprint("000035-006.public_key", "CB7933459F59C70DF1C3FBEEDEDC3ECF689AF56D")


def TestStreaming_test_partial_results():
    m = OpenPGP.Message.parse(
        OpenPGP.Message(
            [
                OpenPGP.UserIDPacket("My name <e@example.com>"),
                OpenPGP.UserIDPacket("Your name <y@example.com>"),
            ]
        ).to_bytes()
    )
    m[0]  # Just the first one
    asserts.assert_that(len(m.force())).is_equal_to(2)


def TestStreaming_test_file_stream():
    m = OpenPGP.Message.parse(
        get_file_contents("pubring.gpg")
    )
    asserts.assert_that(len(m.force())).is_equal_to(1944)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(TestASCIIArmor_test_unarmor_one))
    _suite.addTest(unittest.FunctionTestCase(TestASCIIArmor_test_enarmor_one))
    _suite.addTest(unittest.FunctionTestCase(TestASCIIArmor_test_enarmor_headers_none))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000001006public_key))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000002013user_id))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000003002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000004012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000005002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000006012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000007002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000008012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000009002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000010012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000011002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000012012ring_trust))
    _suite.addTest(
        unittest.FunctionTestCase(TestSerialization_test000013014public_subkey)
    )
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000014002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000015012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000016006public_key))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000017002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000018012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000019013user_id))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000020002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000021012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000022002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000023012ring_trust))
    _suite.addTest(
        unittest.FunctionTestCase(TestSerialization_test000024014public_subkey)
    )
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000025002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000026012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000027006public_key))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000028002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000029012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000030013user_id))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000031002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000032012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000033002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000034012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000035006public_key))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000036013user_id))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000037002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000038012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000039002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000040012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000041017attribute))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000042002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000043012ring_trust))
    _suite.addTest(
        unittest.FunctionTestCase(TestSerialization_test000044014public_subkey)
    )
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000045002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000046012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000047005secret_key))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000048013user_id))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000049002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000050012ring_trust))
    _suite.addTest(
        unittest.FunctionTestCase(TestSerialization_test000051007secret_subkey)
    )
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000052002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000053012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000054005secret_key))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000055002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000056012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000057013user_id))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000058002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000059012ring_trust))
    _suite.addTest(
        unittest.FunctionTestCase(TestSerialization_test000060007secret_subkey)
    )
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000061002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000062012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000063005secret_key))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000064002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000065012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000066013user_id))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000067002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000068012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000069005secret_key))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000070013user_id))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000071002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000072012ring_trust))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000073017attribute))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000074002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000075012ring_trust))
    _suite.addTest(
        unittest.FunctionTestCase(TestSerialization_test000076007secret_subkey)
    )
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000077002sig))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test000078012ring_trust))


    _suite.addTest(unittest.FunctionTestCase(TestSerialization_test002182002sig))


    _suite.addTest(unittest.FunctionTestCase(TestSerialization_testpubringgpg))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_testsecringgpg))


    _suite.addTest(unittest.FunctionTestCase(TestSerialization_testcompressedsiggpg))
    _suite.addTest(
        unittest.FunctionTestCase(TestSerialization_testcompressedsigzlibgpg)
    )


    _suite.addTest(
        # not implemented yet
        unittest.expectedFailure(
            unittest.FunctionTestCase(TestSerialization_testcompressedsigbzip2gpg)
        )
    )
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_testonepass_sig))
    _suite.addTest(
        unittest.FunctionTestCase(TestSerialization_testsymmetrically_encrypted)
    )
    _suite.addTest(
        unittest.FunctionTestCase(TestSerialization_testuncompressedopsdsagpg)
    )
    _suite.addTest(
        unittest.FunctionTestCase(TestSerialization_testuncompressedopsdsasha384txtgpg)
    )
    _suite.addTest(
        unittest.FunctionTestCase(TestSerialization_testuncompressedopsrsagpg)
    )
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_testSymmetricAES))
    _suite.addTest(unittest.FunctionTestCase(TestSerialization_testSymmetricNoMDC))
    _suite.addTest(unittest.FunctionTestCase(TestUserID_test_name_comment_email_id))
    _suite.addTest(unittest.FunctionTestCase(TestUserID_test_name_email_id))
    _suite.addTest(unittest.FunctionTestCase(TestUserID_test_name_id))
    _suite.addTest(unittest.FunctionTestCase(TestUserID_test_email_id))
    _suite.addTest(unittest.FunctionTestCase(TestFingerprint_test000001006public_key))
    _suite.addTest(unittest.FunctionTestCase(TestFingerprint_test000016006public_key))
    _suite.addTest(unittest.FunctionTestCase(TestFingerprint_test000027006public_key))
    _suite.addTest(unittest.FunctionTestCase(TestFingerprint_test000035006public_key))

    _suite.addTest(unittest.FunctionTestCase(TestStreaming_test_partial_results))
    _suite.addTest(unittest.FunctionTestCase(TestStreaming_test_file_stream))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
