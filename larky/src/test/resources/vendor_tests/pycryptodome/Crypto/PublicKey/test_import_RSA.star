# -*- coding: utf-8 -*-
#
#  SelfTest/PublicKey/test_importKey.py: Self-test for importing RSA keys
#
# ===================================================================
# The contents of this file are dedicated to the public domain.  To
# the extent that dedication to the public domain is not available,
# everyone is granted a worldwide, perpetual, royalty-free,
# non-exclusive license to exercise all rights associated with the
# contents of this file for any purpose whatsoever.
# No rights are reserved.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
# BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
# ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
# ===================================================================
load("@stdlib//larky", larky="larky")
load("@stdlib//re", re="re")
load("@stdlib//builtins", "builtins")
load("@stdlib//unittest", "unittest")
load("@stdlib//binascii", binascii="binascii")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//Crypto/st_common", a2b_hex="a2b_hex")
load(
    "@vendor//Crypto/Util/py3compat",
    b="b",
    tostr="tostr",
    FileNotFoundError="FileNotFoundError",
)
load("@vendor//Crypto/Util/number", inverse="inverse")
load("@vendor//Crypto/Util/asn1", asn1="asn1")
load("@vendor//asserts", "asserts")


def der2pem(der, text="PUBLIC"):
    chunks = [binascii.b2a_base64(der[i : i + 48]) for i in range(0, len(der), 48)]
    pem = bytearray("-----BEGIN %s KEY-----\n" % text, encoding='utf-8')
    pem += bytearray("", encoding='utf-8').join(chunks)
    pem += bytearray("-----END %s KEY-----" % text, encoding='utf-8')
    return pem


# 512-bit RSA key generated with openssl
rsaKeyPEM = """-----BEGIN RSA PRIVATE KEY-----
MIIBOwIBAAJBAL8eJ5AKoIsjURpcEoGubZMxLD7+kT+TLr7UkvEtFrRhDDKMtuII
q19FrL4pUIMymPMSLBn3hJLe30Dw48GQM4UCAwEAAQJACUSDEp8RTe32ftq8IwG8
Wojl5mAd1wFiIOrZ/Uv8b963WJOJiuQcVN29vxU5+My9GPZ7RA3hrDBEAoHUDPrI
OQIhAPIPLz4dphiD9imAkivY31Rc5AfHJiQRA7XixTcjEkojAiEAyh/pJHks/Mlr
+rdPNEpotBjfV4M4BkgGAA/ipcmaAjcCIQCHvhwwKVBLzzTscT2HeUdEeBMoiXXK
JACAr3sJQJGxIQIgarRp+m1WSKV1MciwMaTOnbU7wxFs9DP1pva76lYBzgUCIQC9
n0CnZCJ6IZYqSt0H5N7+Q+2Ro64nuwV/OSQfM6sBwQ==
-----END RSA PRIVATE KEY-----"""

# As above, but this is actually an unencrypted PKCS#8 key
rsaKeyPEM8 = """-----BEGIN PRIVATE KEY-----
MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAvx4nkAqgiyNRGlwS
ga5tkzEsPv6RP5MuvtSS8S0WtGEMMoy24girX0WsvilQgzKY8xIsGfeEkt7fQPDj
wZAzhQIDAQABAkAJRIMSnxFN7fZ+2rwjAbxaiOXmYB3XAWIg6tn9S/xv3rdYk4mK
5BxU3b2/FTn4zL0Y9ntEDeGsMEQCgdQM+sg5AiEA8g8vPh2mGIP2KYCSK9jfVFzk
B8cmJBEDteLFNyMSSiMCIQDKH+kkeSz8yWv6t080Smi0GN9XgzgGSAYAD+KlyZoC
NwIhAIe+HDApUEvPNOxxPYd5R0R4EyiJdcokAICvewlAkbEhAiBqtGn6bVZIpXUx
yLAxpM6dtTvDEWz0M/Wm9rvqVgHOBQIhAL2fQKdkInohlipK3Qfk3v5D7ZGjrie7
BX85JB8zqwHB
-----END PRIVATE KEY-----"""

# The same RSA private key as in rsaKeyPEM, but now encrypted
rsaKeyEncryptedPEM = (
    # PEM encryption
    # With DES and passphrase 'test'
    (
        "test",
        """-----BEGIN RSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: DES-CBC,AF8F9A40BD2FA2FC

Ckl9ex1kaVEWhYC2QBmfaF+YPiR4NFkRXA7nj3dcnuFEzBnY5XULupqQpQI3qbfA
u8GYS7+b3toWWiHZivHbAAUBPDIZG9hKDyB9Sq2VMARGsX1yW1zhNvZLIiVJzUHs
C6NxQ1IJWOXzTew/xM2I26kPwHIvadq+/VaT8gLQdjdH0jOiVNaevjWnLgrn1mLP
BCNRMdcexozWtAFNNqSzfW58MJL2OdMi21ED184EFytIc1BlB+FZiGZduwKGuaKy
9bMbdb/1PSvsSzPsqW7KSSrTw6MgJAFJg6lzIYvR5F4poTVBxwBX3+EyEmShiaNY
IRX3TgQI0IjrVuLmvlZKbGWP18FXj7I7k9tSsNOOzllTTdq3ny5vgM3A+ynfAaxp
dysKznQ6P+IoqML1WxAID4aGRMWka+uArOJ148Rbj9s=
-----END RSA PRIVATE KEY-----""",
    ),
    # PKCS8 encryption
    (
        "winter",
        """-----BEGIN ENCRYPTED PRIVATE KEY-----
MIIBpjBABgkqhkiG9w0BBQ0wMzAbBgkqhkiG9w0BBQwwDgQIeZIsbW3O+JcCAggA
MBQGCCqGSIb3DQMHBAgSM2p0D8FilgSCAWBhFyP2tiGKVpGj3mO8qIBzinU60ApR
3unvP+N6j7LVgnV2lFGaXbJ6a1PbQXe+2D6DUyBLo8EMXrKKVLqOMGkFMHc0UaV6
R6MmrsRDrbOqdpTuVRW+NVd5J9kQQh4xnfU/QrcPPt7vpJvSf4GzG0n666Ki50OV
M/feuVlIiyGXY6UWdVDpcOV72cq02eNUs/1JWdh2uEBvA9fCL0c07RnMrdT+CbJQ
NjJ7f8ULtp7xvR9O3Al/yJ4Wv3i4VxF1f3MCXzhlUD4I0ONlr0kJWgeQ80q/cWhw
ntvgJwnCn2XR1h6LA8Wp+0ghDTsL2NhJpWd78zClGhyU4r3hqu1XDjoXa7YCXCix
jCV15+ViDJzlNCwg+W6lRg18sSLkCT7alviIE0U5tHc6UPbbHwT5QqAxAABaP+nZ
CGqJGyiwBzrKebjgSm/KRd4C91XqcsysyH2kKPfT51MLAoD4xelOURBP
-----END ENCRYPTED PRIVATE KEY-----""",
    ),
)

rsaPublicKeyPEM = """-----BEGIN PUBLIC KEY-----
MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAL8eJ5AKoIsjURpcEoGubZMxLD7+kT+T
Lr7UkvEtFrRhDDKMtuIIq19FrL4pUIMymPMSLBn3hJLe30Dw48GQM4UCAwEAAQ==
-----END PUBLIC KEY-----"""

# Obtained using 'ssh-keygen -i -m PKCS8 -f rsaPublicKeyPEM'
rsaPublicKeyOpenSSH = b(
    """ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAQQC/HieQCqCLI1EaXBKBrm2TMSw+/pE/ky6+1JLxLRa0YQwyjLbiCKtfRay+KVCDMpjzEiwZ94SS3t9A8OPBkDOF comment\n"""
)

# The private key, in PKCS#1 format encoded with DER
rsaKeyDER = a2b_hex(
    """3082013b020100024100bf1e27900aa08b23511a5c1281ae6d93312c3efe
        913f932ebed492f12d16b4610c328cb6e208ab5f45acbe2950833298f312
        2c19f78492dedf40f0e3c190338502030100010240094483129f114dedf6
        7edabc2301bc5a88e5e6601dd7016220ead9fd4bfc6fdeb75893898ae41c
        54ddbdbf1539f8ccbd18f67b440de1ac30440281d40cfac839022100f20f
        2f3e1da61883f62980922bd8df545ce407c726241103b5e2c53723124a23
        022100ca1fe924792cfcc96bfab74f344a68b418df578338064806000fe2
        a5c99a023702210087be1c3029504bcf34ec713d877947447813288975ca
        240080af7b094091b12102206ab469fa6d5648a57531c8b031a4ce9db53b
        c3116cf433f5a6f6bbea5601ce05022100bd9f40a764227a21962a4add07
        e4defe43ed91a3ae27bb057f39241f33ab01c1
        """.replace(
        " ", ""
    )
)

# The private key, in unencrypted PKCS#8 format encoded with DER
rsaKeyDER8 = a2b_hex(
    """30820155020100300d06092a864886f70d01010105000482013f3082013
        b020100024100bf1e27900aa08b23511a5c1281ae6d93312c3efe913f932
        ebed492f12d16b4610c328cb6e208ab5f45acbe2950833298f3122c19f78
        492dedf40f0e3c190338502030100010240094483129f114dedf67edabc2
        301bc5a88e5e6601dd7016220ead9fd4bfc6fdeb75893898ae41c54ddbdb
        f1539f8ccbd18f67b440de1ac30440281d40cfac839022100f20f2f3e1da
        61883f62980922bd8df545ce407c726241103b5e2c53723124a23022100c
        a1fe924792cfcc96bfab74f344a68b418df578338064806000fe2a5c99a0
        23702210087be1c3029504bcf34ec713d877947447813288975ca240080a
        f7b094091b12102206ab469fa6d5648a57531c8b031a4ce9db53bc3116cf
        433f5a6f6bbea5601ce05022100bd9f40a764227a21962a4add07e4defe4
        3ed91a3ae27bb057f39241f33ab01c1
        """.replace(
        " ", ""
    )
)

rsaPublicKeyDER = a2b_hex(
    """305c300d06092a864886f70d0101010500034b003048024100bf1e27900a
        a08b23511a5c1281ae6d93312c3efe913f932ebed492f12d16b4610c328c
        b6e208ab5f45acbe2950833298f3122c19f78492dedf40f0e3c190338502
        03010001
        """.replace(
        " ", ""
    )
)

n = int(
    "BF 1E 27 90 0A A0 8B 23 51 1A 5C 12 81 AE 6D 93 31 2C 3E FE 91 3F 93 2E BE D4 92 F1 2D 16 B4 61 0C 32 8C B6 E2 08 AB 5F 45 AC BE 29 50 83 32 98 F3 12 2C 19 F7 84 92 DE DF 40 F0 E3 C1 90 33 85".replace(
        " ", ""
    ),
    16,
)
e = 65537
d = int(
    "09 44 83 12 9F 11 4D ED F6 7E DA BC 23 01 BC 5A 88 E5 E6 60 1D D7 01 62 20 EA D9 FD 4B FC 6F DE B7 58 93 89 8A E4 1C 54 DD BD BF 15 39 F8 CC BD 18 F6 7B 44 0D E1 AC 30 44 02 81 D4 0C FA C8 39".replace(
        " ", ""
    ),
    16,
)
p = int(
    "00 F2 0F 2F 3E 1D A6 18 83 F6 29 80 92 2B D8 DF 54 5C E4 07 C7 26 24 11 03 B5 E2 C5 37 23 12 4A 23".replace(
        " ", ""
    ),
    16,
)
q = int(
    "00 CA 1F E9 24 79 2C FC C9 6B FA B7 4F 34 4A 68 B4 18 DF 57 83 38 06 48 06 00 0F E2 A5 C9 9A 02 37".replace(
        " ", ""
    ),
    16,
)

    # This is q^{-1} mod p). fastmath and slowmath use pInv (p^{-1}
    # mod q) instead!
qInv = int('00 BD 9F 40 A7 64 22 7A 21 96 2A 4A DD 07 E4 DE FE 43 ED 91 A3 AE 27 BB 05 7F 39 24 1F 33 AB 01 C1'.replace(" ",""),16)
pInv = inverse(p, q)


def ImportKeyTests_testImportKey1():
    """Verify import of RSAPrivateKey DER SEQUENCE"""
    key = RSA.importKey(rsaKeyDER)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.n).is_equal_to(n)
    asserts.assert_that(key.e).is_equal_to(e)
    asserts.assert_that(key.d).is_equal_to(d)
    asserts.assert_that(key.p).is_equal_to(p)
    asserts.assert_that(key.q).is_equal_to(q)


def ImportKeyTests_testImportKey2():
    """Verify import of SubjectPublicKeyInfo DER SEQUENCE"""
    key = RSA.importKey(rsaPublicKeyDER)
    asserts.assert_that(key.has_private()).is_false()
    asserts.assert_that(key.n).is_equal_to(n)
    asserts.assert_that(key.e).is_equal_to(e)


def ImportKeyTests_testImportKey3unicode():
    """Verify import of RSAPrivateKey DER SEQUENCE, encoded with PEM as unicode"""
    key = RSA.importKey(rsaKeyPEM)
    asserts.assert_that(key.has_private()).is_equal_to(True)  # assert_
    asserts.assert_that(key.n).is_equal_to(n)
    asserts.assert_that(key.e).is_equal_to(e)
    asserts.assert_that(key.d).is_equal_to(d)
    asserts.assert_that(key.p).is_equal_to(p)
    asserts.assert_that(key.q).is_equal_to(q)


def ImportKeyTests_testImportKey3bytes():
    """Verify import of RSAPrivateKey DER SEQUENCE, encoded with PEM as byte string"""
    key = RSA.importKey(b(rsaKeyPEM))
    asserts.assert_that(key.has_private()).is_equal_to(True)  # assert_
    asserts.assert_that(key.n).is_equal_to(n)
    asserts.assert_that(key.e).is_equal_to(e)
    asserts.assert_that(key.d).is_equal_to(d)
    asserts.assert_that(key.p).is_equal_to(p)
    asserts.assert_that(key.q).is_equal_to(q)


def ImportKeyTests_testImportKey4unicode():
    """Verify import of RSAPrivateKey DER SEQUENCE, encoded with PEM as unicode"""
    key = RSA.importKey(rsaPublicKeyPEM)
    asserts.assert_that(key.has_private()).is_equal_to(False)  # failIf
    asserts.assert_that(key.n).is_equal_to(n)
    asserts.assert_that(key.e).is_equal_to(e)


def ImportKeyTests_testImportKey4bytes():
    """Verify import of SubjectPublicKeyInfo DER SEQUENCE, encoded with PEM as byte string"""
    key = RSA.importKey(b(rsaPublicKeyPEM))
    asserts.assert_that(key.has_private()).is_equal_to(False)  # failIf
    asserts.assert_that(key.n).is_equal_to(n)
    asserts.assert_that(key.e).is_equal_to(e)


def ImportKeyTests_testImportKey5():
    """Verifies that the imported key is still a valid RSA pair"""
    key = RSA.importKey(rsaKeyPEM)
    idem = key._encrypt(key._decrypt(89))
    asserts.assert_that(idem).is_equal_to(89)


def ImportKeyTests_testImportKey6():
    """Verifies that the imported key is still a valid RSA pair"""
    key = RSA.importKey(rsaKeyDER)
    idem = key._encrypt(key._decrypt(65))
    asserts.assert_that(idem).is_equal_to(65)


def ImportKeyTests_testImportKey7():
    """Verify import of OpenSSH public key"""
    key = RSA.importKey(rsaPublicKeyOpenSSH)
    asserts.assert_that(key.n).is_equal_to(n)
    asserts.assert_that(key.e).is_equal_to(e)


def ImportKeyTests_testImportKey8():
    """Verify import of encrypted PrivateKeyInfo DER SEQUENCE"""
    for t in rsaKeyEncryptedPEM:
        key = RSA.importKey(t[1], t[0])
        asserts.assert_that(key.has_private()).is_true()
        asserts.assert_that(key.n).is_equal_to(n)
        asserts.assert_that(key.e).is_equal_to(e)
        asserts.assert_that(key.d).is_equal_to(d)
        asserts.assert_that(key.p).is_equal_to(p)
        asserts.assert_that(key.q).is_equal_to(q)


def ImportKeyTests_testImportKey9():
    """Verify import of unencrypted PrivateKeyInfo DER SEQUENCE"""
    key = RSA.importKey(rsaKeyDER8)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.n).is_equal_to(n)
    asserts.assert_that(key.e).is_equal_to(e)
    asserts.assert_that(key.d).is_equal_to(d)
    asserts.assert_that(key.p).is_equal_to(p)
    asserts.assert_that(key.q).is_equal_to(q)


def ImportKeyTests_testImportKey10():
    """Verify import of unencrypted PrivateKeyInfo DER SEQUENCE, encoded with PEM"""
    key = RSA.importKey(rsaKeyPEM8)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.n).is_equal_to(n)
    asserts.assert_that(key.e).is_equal_to(e)
    asserts.assert_that(key.d).is_equal_to(d)
    asserts.assert_that(key.p).is_equal_to(p)
    asserts.assert_that(key.q).is_equal_to(q)


def ImportKeyTests_testImportKey11():
    """Verify import of RSAPublicKey DER SEQUENCE"""
    der = asn1.DerSequence([17, 3]).encode()
    key = RSA.importKey(der)
    asserts.assert_that(key.n).is_equal_to(17)
    asserts.assert_that(key.e).is_equal_to(3)


def ImportKeyTests_testImportKey12():
    """Verify import of RSAPublicKey DER SEQUENCE, encoded with PEM"""
    der = asn1.DerSequence([17, 3]).encode()
    pem = der2pem(der)
    key = RSA.importKey(pem)
    asserts.assert_that(key.n).is_equal_to(17)
    asserts.assert_that(key.e).is_equal_to(3)


def ImportKeyTests_test_import_key_windows_cr_lf():
    pem_cr_lf = "\r\n".join(rsaKeyPEM.splitlines())
    key = RSA.importKey(pem_cr_lf)
    asserts.assert_that(key.n).is_equal_to(n)
    asserts.assert_that(key.e).is_equal_to(e)
    asserts.assert_that(key.d).is_equal_to(d)
    asserts.assert_that(key.p).is_equal_to(p)
    asserts.assert_that(key.q).is_equal_to(q)


def ImportKeyTests_test_import_empty():
    asserts.assert_fails(
        lambda: RSA.import_key(bytes(r"", encoding="utf-8")), ".*?ValueError"
    )

    ###


def ImportKeyTests_testExportKey1():
    key = RSA.construct([n, e, d, p, q, pInv])
    derKey = key.export_key("DER")
    asserts.assert_that(derKey).is_equal_to(rsaKeyDER)


def ImportKeyTests_testExportKey2():
    key = RSA.construct([n, e])
    derKey = key.export_key("DER")
    asserts.assert_that(derKey).is_equal_to(rsaPublicKeyDER)


def ImportKeyTests_testExportKey3():
    key = RSA.construct([n, e, d, p, q, pInv])
    pemKey = key.export_key("PEM")
    asserts.assert_that(pemKey).is_equal_to(b(rsaKeyPEM))


def ImportKeyTests_testExportKey4():
    key = RSA.construct([n, e])
    pemKey = key.export_key("PEM")
    asserts.assert_that(pemKey).is_equal_to(b(rsaPublicKeyPEM))


def ImportKeyTests_testExportKey5():
    key = RSA.construct([n, e])
    openssh_1 = key.export_key("OpenSSH").split()
    openssh_2 = rsaPublicKeyOpenSSH.split()
    asserts.assert_that(openssh_1[0]).is_equal_to(openssh_2[0])
    asserts.assert_that(openssh_1[1]).is_equal_to(openssh_2[1])


def ImportKeyTests_testExportKey7():
    key = RSA.construct([n, e, d, p, q, pInv])
    derKey = key.export_key("DER", pkcs=8)
    asserts.assert_that(derKey).is_equal_to(rsaKeyDER8)


def ImportKeyTests_testExportKey8():
    key = RSA.construct([n, e, d, p, q, pInv])
    pemKey = key.export_key("PEM", pkcs=8)
    asserts.assert_that(pemKey).is_equal_to(b(rsaKeyPEM8))


def ImportKeyTests_testExportKey9():
    key = RSA.construct([n, e, d, p, q, pInv])
    asserts.assert_fails(lambda: key.export_key("invalid-format"), ".*?ValueError")


def ImportKeyTests_testExportKey10():
    # Export and re-import the encrypted key. It must match.
    # PEM envelope, PKCS#1, old PEM encryption
    key = RSA.construct([n, e, d, p, q, pInv])
    outkey = key.export_key("PEM", "test")
    # print(tostr(outkey))
    asserts.assert_that((tostr(outkey).find("4,ENCRYPTED") != -1)).is_true()
    asserts.assert_that((tostr(outkey).find("BEGIN RSA PRIVATE KEY") != -1)).is_true()
    inkey = RSA.importKey(outkey, "test")
    asserts.assert_that(key.n).is_equal_to(inkey.n)
    asserts.assert_that(key.e).is_equal_to(inkey.e)
    asserts.assert_that(key.d).is_equal_to(inkey.d)


def ImportKeyTests_testExportKey11():
    # Export and re-import the encrypted key. It must match.
    # PEM envelope, PKCS#1, old PEM encryption
    key = RSA.construct([n, e, d, p, q, pInv])
    outkey = key.export_key("PEM", "test", pkcs=1)
    asserts.assert_that((tostr(outkey).find("4,ENCRYPTED") != -1)).is_true()
    asserts.assert_that((tostr(outkey).find("BEGIN RSA PRIVATE KEY") != -1)).is_true()
    inkey = RSA.importKey(outkey, "test")
    asserts.assert_that(key.n).is_equal_to(inkey.n)
    asserts.assert_that(key.e).is_equal_to(inkey.e)
    asserts.assert_that(key.d).is_equal_to(inkey.d)


def ImportKeyTests_testExportKey12():
    # Export and re-import the encrypted key. It must match.
    # PEM envelope, PKCS#8, old PEM encryption
    key = RSA.construct([n, e, d, p, q, pInv])
    outkey = key.export_key("PEM", "test", pkcs=8)
    asserts.assert_that((tostr(outkey).find("4,ENCRYPTED") != -1)).is_true()
    asserts.assert_that((tostr(outkey).find("BEGIN PRIVATE KEY") != -1)).is_true()
    inkey = RSA.importKey(outkey, "test")
    asserts.assert_that(key.n).is_equal_to(inkey.n)
    asserts.assert_that(key.e).is_equal_to(inkey.e)
    asserts.assert_that(key.d).is_equal_to(inkey.d)


def ImportKeyTests_testExportKey13():
    # Export and re-import the encrypted key. It must match.
    # PEM envelope, PKCS#8, PKCS#8 encryption
    key = RSA.construct([n, e, d, p, q, pInv])
    outkey = key.export_key(
        "PEM", "test", pkcs=8, protection="PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC"
    )
    asserts.assert_that((tostr(outkey).find("4,ENCRYPTED") == -1)).is_true()
    asserts.assert_that(
        (tostr(outkey).find("BEGIN ENCRYPTED PRIVATE KEY") != -1)
    ).is_true()
    inkey = RSA.importKey(outkey, "test")
    asserts.assert_that(key.n).is_equal_to(inkey.n)
    asserts.assert_that(key.e).is_equal_to(inkey.e)
    asserts.assert_that(key.d).is_equal_to(inkey.d)


def ImportKeyTests_testExportKey14():
    # Export and re-import the encrypted key. It must match.
    # DER envelope, PKCS#8, PKCS#8 encryption
    key = RSA.construct([n, e, d, p, q, pInv])
    outkey = key.export_key("DER", "test", pkcs=8)
    inkey = RSA.importKey(outkey, "test")
    asserts.assert_that(key.n).is_equal_to(inkey.n)
    asserts.assert_that(key.e).is_equal_to(inkey.e)
    asserts.assert_that(key.d).is_equal_to(inkey.d)


def ImportKeyTests_testExportKey15():
    # Verify that that error an condition is detected when trying to
    # use a password with DER encoding and PKCS#1.
    key = RSA.construct([n, e, d, p, q, pInv])
    asserts.assert_fails(lambda: key.export_key("DER", "test", 1), ".*?ValueError")


def ImportKeyTests_test_import_key():
    """Verify that import_key is an alias to importKey"""
    key = RSA.import_key(rsaPublicKeyDER)
    asserts.assert_that(key.has_private()).is_false()
    asserts.assert_that(key.n).is_equal_to(n)
    asserts.assert_that(key.e).is_equal_to(e)


def ImportKeyTests_test_exportKey():
    key = RSA.construct([n, e, d, p, q, pInv])
    asserts.assert_that(key.export_key()).is_equal_to(key.exportKey())


def ImportKeyFromX509Cert_test_x509v1():
    # Sample V1 certificate with a 1024 bit RSA key
    x509_v1_cert = """
-----BEGIN CERTIFICATE-----
MIICOjCCAaMCAQEwDQYJKoZIhvcNAQEEBQAwfjENMAsGA1UEChMEQWNtZTELMAkG
A1UECxMCUkQxHDAaBgkqhkiG9w0BCQEWDXNwYW1AYWNtZS5vcmcxEzARBgNVBAcT
Ck1ldHJvcG9saXMxETAPBgNVBAgTCE5ldyBZb3JrMQswCQYDVQQGEwJVUzENMAsG
A1UEAxMEdGVzdDAeFw0xNDA3MTExOTU3MjRaFw0xNzA0MDYxOTU3MjRaME0xCzAJ
BgNVBAYTAlVTMREwDwYDVQQIEwhOZXcgWW9yazENMAsGA1UEChMEQWNtZTELMAkG
A1UECxMCUkQxDzANBgNVBAMTBmxhdHZpYTCBnzANBgkqhkiG9w0BAQEFAAOBjQAw
gYkCgYEAyG+kytdRj3TFbRmHDYp3TXugVQ81chew0qeOxZWOz80IjtWpgdOaCvKW
NCuc8wUR9BWrEQW+39SaRMLiQfQtyFSQZijc3nsEBu/Lo4uWZ0W/FHDRVSvkJA/V
Ex5NL5ikI+wbUeCV5KajGNDalZ8F1pk32+CBs8h1xNx5DyxuEHUCAwEAATANBgkq
hkiG9w0BAQQFAAOBgQCVQF9Y//Q4Psy+umEM38pIlbZ2hxC5xNz/MbVPwuCkNcGn
KYNpQJP+JyVTsPpO8RLZsAQDzRueMI3S7fbbwTzAflN0z19wvblvu93xkaBytVok
9VBAH28olVhy9b1MMeg2WOt5sUEQaFNPnwwsyiY9+HsRpvpRnPSQF+kyYVsshQ==
-----END CERTIFICATE-----
        """.strip()

    # RSA public key as dumped by openssl
    exponent = 65537
    modulus_str = """
00:c8:6f:a4:ca:d7:51:8f:74:c5:6d:19:87:0d:8a:
77:4d:7b:a0:55:0f:35:72:17:b0:d2:a7:8e:c5:95:
8e:cf:cd:08:8e:d5:a9:81:d3:9a:0a:f2:96:34:2b:
9c:f3:05:11:f4:15:ab:11:05:be:df:d4:9a:44:c2:
e2:41:f4:2d:c8:54:90:66:28:dc:de:7b:04:06:ef:
cb:a3:8b:96:67:45:bf:14:70:d1:55:2b:e4:24:0f:
d5:13:1e:4d:2f:98:a4:23:ec:1b:51:e0:95:e4:a6:
a3:18:d0:da:95:9f:05:d6:99:37:db:e0:81:b3:c8:
75:c4:dc:79:0f:2c:6e:10:75
        """
    modulus = int(re.sub("[^0-9a-f]", "", modulus_str), 16)

    key = RSA.importKey(x509_v1_cert)
    asserts.assert_that(key.e).is_equal_to(exponent)
    asserts.assert_that(key.n).is_equal_to(modulus)
    asserts.assert_that(key.has_private()).is_false()


def ImportKeyFromX509Cert_test_x509v3():
    # Sample V3 certificate with a 1024 bit RSA key
    x509_v3_cert = """
-----BEGIN CERTIFICATE-----
MIIEcjCCAlqgAwIBAgIBATANBgkqhkiG9w0BAQsFADBhMQswCQYDVQQGEwJVUzEL
MAkGA1UECAwCTUQxEjAQBgNVBAcMCUJhbHRpbW9yZTEQMA4GA1UEAwwHVGVzdCBD
QTEfMB0GCSqGSIb3DQEJARYQdGVzdEBleGFtcGxlLmNvbTAeFw0xNDA3MTIwOTM1
MTJaFw0xNzA0MDcwOTM1MTJaMEQxCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJNRDES
MBAGA1UEBwwJQmFsdGltb3JlMRQwEgYDVQQDDAtUZXN0IFNlcnZlcjCBnzANBgkq
hkiG9w0BAQEFAAOBjQAwgYkCgYEA/S7GJV2OcFdyNMQ4K75KrYFtMEn3VnEFdPHa
jyS37XlMxSh0oS4GeTGVUCJInl5Cpsv8WQdh03FfeOdvzp5IZ46OcjeOPiWnmjgl
2G5j7e2bDH7RSchGV+OD6Fb1Agvuu2/9iy8fdf3rPQ/7eAddzKUrzwacVbnW+tg2
QtSXKRcCAwEAAaOB1TCB0jAdBgNVHQ4EFgQU/WwCX7FfWMIPDFfJ+I8a2COG+l8w
HwYDVR0jBBgwFoAUa0hkif3RMaraiWtsOOZZlLu9wJwwCQYDVR0TBAIwADALBgNV
HQ8EBAMCBeAwSgYDVR0RBEMwQYILZXhhbXBsZS5jb22CD3d3dy5leGFtcGxlLmNv
bYIQbWFpbC5leGFtcGxlLmNvbYIPZnRwLmV4YW1wbGUuY29tMCwGCWCGSAGG+EIB
DQQfFh1PcGVuU1NMIEdlbmVyYXRlZCBDZXJ0aWZpY2F0ZTANBgkqhkiG9w0BAQsF
AAOCAgEAvO6xfdsGbnoK4My3eJthodTAjMjPwFVY133LH04QLcCv54TxKhtUg1fi
PgdjVe1HpTytPBfXy2bSZbXAN0abZCtw1rYrnn7o1g2pN8iypVq3zVn0iMTzQzxs
zEPO3bpR/UhNSf90PmCsS5rqZpAAnXSaAy1ClwHWk/0eG2pYkhE1m1ABVMN2lsAW
e9WxGk6IFqaI9O37NYQwmEypMs4DC+ECJEvbPFiqi3n0gbXCZJJ6omDA5xJldaYK
Oa7KR3s/qjBsu9UAiWpLBuFoSTHIF2aeRKRFmUdmzwo43eVPep65pY6eQ4AdL2RF
rqEuINbGlzI5oQyYhu71IwB+iPZXaZZPlwjLgOsuad/p2hOgDb5WxUi8FnDPursQ
ujfpIpmrOP/zpvvQWnwePI3lI+5n41kTBSbefXEdv6rXpHk3QRzB90uPxnXPdxSC
16ASA8bQT5an/1AgoE3k9CrcD2K0EmgaX0YI0HUhkyzbkg34EhpWJ6vvRUbRiNRo
9cIbt/ya9Y9u0Ja8GLXv6dwX0l0IdJMkL8KifXUFAVCujp1FBrr/gdmwQn8itANy
+qbnWSxmOvtaY0zcaFAcONuHva0h51/WqXOMO1eb8PhR4HIIYU8p1oBwQp7dSni8
THDi1F+GG5PsymMDj5cWK42f+QzjVw5PrVmFqqrrEoMlx8DWh5Y=
-----END CERTIFICATE-----
""".strip()

    # RSA public key as dumped by openssl
    exponent = 65537
    modulus_str = """
00:fd:2e:c6:25:5d:8e:70:57:72:34:c4:38:2b:be:
4a:ad:81:6d:30:49:f7:56:71:05:74:f1:da:8f:24:
b7:ed:79:4c:c5:28:74:a1:2e:06:79:31:95:50:22:
48:9e:5e:42:a6:cb:fc:59:07:61:d3:71:5f:78:e7:
6f:ce:9e:48:67:8e:8e:72:37:8e:3e:25:a7:9a:38:
25:d8:6e:63:ed:ed:9b:0c:7e:d1:49:c8:46:57:e3:
83:e8:56:f5:02:0b:ee:bb:6f:fd:8b:2f:1f:75:fd:
eb:3d:0f:fb:78:07:5d:cc:a5:2b:cf:06:9c:55:b9:
d6:fa:d8:36:42:d4:97:29:17
        """
    modulus = int(re.sub("[^0-9a-f]", "", modulus_str), 16)

    key = RSA.importKey(x509_v3_cert)
    asserts.assert_that(key.e).is_equal_to(exponent)
    asserts.assert_that(key.n).is_equal_to(modulus)
    asserts.assert_that(key.has_private()).is_false()


rsa2048_priv_pem = """-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEAzf6A6XjM4skBCd6SBt6g8GkO6Zg6sfC+7jvrmEyERWdm2iVP
SDyWONzKprIT8tRaDHbtkhsJnff5F0Pq/gO6lMPKcgADn/DUAW0C8C6y8ns7tkKY
fGbSdJQo/JIA7IEecEV5Jb/1p5GHwUQQIxa5QLqBNXBNsnX27vlVNT2RZ8cos82Y
nKl9YhvjJzdC/G9eYlwe+DrYtVyw5yh6fjJvU1fx5EFbYbLAaSZ9lY7byqIxVFCG
Hmqasx2MVtTdOwVSlA3mxXiMpN2lWPSICbVxiooGxQifBuEnQXYJfWQdGIxjWuqK
J4L4Pn4C16quEVjrwO7psE+DBTZS7m8RWDdfLwIDAQABAoIBAQCwoKLkjgIQCw3q
6n8HiOkyesKgpTjeznDIUXSneCSKZikYr5kVdW3Rf+/7kwHr2w0DVw5Jkwlh+/gH
bOMXvxbC7EawDTcOcy81scUtML2bkPMYSm35bSjTcR7bwk0sfWIeHlPMfJYkgnKG
0teLXralu8WCjrLwbJm5Ou1G423ELsTrCFTvM8n0x2Wgpvevfyuh3un1CO6Hpi7/
o5ZFUt4Vu6MCmQprD7OrsdG4wxIJJpnVDtTa/CE3tiSKG+yrF5FlxKgnAZK90UTJ
AsZsdkVzXwhcbTuVwFghyZa8t90rDKMGPU4XrW8GxN3TTYN9vvPwAPtsXBSE0QYi
+yPPN63BAoGBAOkVFdg+i2y3wL3zJuR9v4IgN+eHW+HysK55nLUN+qXuKZ3xNnrh
Lf1xKp8XOG+qfHolwuqELAbu9q8VoY2qOlZkI2CWJnHESL2oY5Ws2FSFaqUvrgsZ
vj6dYqHR5/vRqvbZWp7kRHtcO97/c91U8qc0u7scgY3V+5nyYsXK5/m/AoGBAOI/
lMFO6EtI8OfiV5AvjS17FaX9C00rVtWL5vLH9q4aq4qBuaB+pnX/zb3FrzxRTxg1
pdghMkj/6ms3cwEahvnnPC4P3FVkjLFpl1rDb3yXCak8sZTekVaRbR8Y1fxvjeQk
es5+bJHNa9ugsWLPwksTeu+cuguqxYBgRoo+w5aRAoGAOvd+o0qPc125gVS2ji/R
91W3TvfLowoG8N0LbDKxKrFqDe8sXUICpI+wvLbfLEDxZOWQvkvZ55clCX5rdK+Q
OrLy0EisSTPjQzgKmZ80y41FQa0iVuSYYLbocQ/tpKOSoQi0CGavfJYE/5GY+nG3
Qd3xcJDxpbRxBq1vl6KRtXkCgYBCVZ/4uXj0MdOoOT2xIbAD6LWlMDbzDkTsYZN6
FVTnIRywhG53dwq5SCH7hQ1m4vQMxhX082655obFnsFSToPKm5iSbMYOJ2f00F+3
FdwHLIWBoDD00/jK5+KVnoOG+vIgNO3owzpz2UXJZCj+LqnptzFxNYN6zjwP8qpb
+CECcQKBgC1i8ptWWx6LVrBTvxdrAI9JeJz/wEvzX2+Ca2lpAlaxW4wM4M9CinGs
MJuv//vv/bO6BcTLnkP3D3K7TmfYF05ECyDIaUEvlcAhO85su6WfJLLJY+bIWC2o
2g0PpIlRhksVTeNfR1eoNUeALvpxZE3VjmU7CVZk0NaI9CZUfCu9
-----END RSA PRIVATE KEY-----"""

rsa2048_pub_openssh = (  "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDN/oDpeMziyQ" +
"EJ3pIG3qDwaQ7pmDqx8L7uO+uYTIRFZ2baJU9IPJY43MqmshPy1FoMdu2SGwmd9/kXQ+r+A7qUw" +
"8pyAAOf8NQBbQLwLrLyezu2Qph8ZtJ0lCj8kgDsgR5wRXklv/WnkYfBRBAjFrlAuoE1cE2ydfbu" +
"+VU1PZFnxyizzZicqX1iG+MnN0L8b15iXB74Oti1XLDnKHp+Mm9TV/HkQVthssBpJn2VjtvKojF" +
"UUIYeapqzHYxW1N07BVKUDebFeIyk3aVY9IgJtXGKigbFCJ8G4SdBdgl9ZB0YjGNa6oongvg+fg" +
"LXqq4RWOvA7umwT4MFNlLubxFYN18v")

rsa2048_private_openssh = """-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAABFwAAAAdzc2gtcn
NhAAAAAwEAAQAAAQEA1G7CSXDPFI6GVaKcD3XsVtmVAXYp7EXzDtNfenpL8gV7ujWzQ2R7
VRM+Owmw8Q4AZX8Ow6pK3j2LfeqrOfN6Ym7QGCnWdzlAFcCLBTvQknxTteMS8rHGlOy+yC
IW21TmKYHO4c8+e/nHzS4iCF9DflmqB3c3HfCIvX5fZ104eQAfwsV3ub8sfFqsskR7EYPk
5mX0XCsxGv35kqpgk52DJUkjMdy097YS8MZVf2Tg7PbbILmNEZPVHmIDl3kzo0ju5Ov+hZ
6lUJ8u/3a23jNmZg5DglItYad99xoqgek2nOsbvsmWTZeZZ7i/sTBEqkeiQk/zznEKixBA
ayoWEItJrwAAA9hiJ0QNYidEDQAAAAdzc2gtcnNhAAABAQDUbsJJcM8UjoZVopwPdexW2Z
UBdinsRfMO0196ekvyBXu6NbNDZHtVEz47CbDxDgBlfw7DqkrePYt96qs583pibtAYKdZ3
OUAVwIsFO9CSfFO14xLyscaU7L7IIhbbVOYpgc7hzz57+cfNLiIIX0N+WaoHdzcd8Ii9fl
9nXTh5AB/CxXe5vyx8WqyyRHsRg+TmZfRcKzEa/fmSqmCTnYMlSSMx3LT3thLwxlV/ZODs
9tsguY0Rk9UeYgOXeTOjSO7k6/6FnqVQny7/drbeM2ZmDkOCUi1hp333GiqB6Tac6xu+yZ
ZNl5lnuL+xMESqR6JCT/POcQqLEEBrKhYQi0mvAAAAAwEAAQAAAQA+oXESqfnDu8mLUefk
/wVxDbFKvwXZLT5d7p/FwmzFrCwwWEjD48og6Q195nuOdmxTgERgF8L/BvIra5aT/V7lyn
n7xcn/WJe2UhAquNnjdlhP1eTuPM+pdKtC4hoPDFbXgff2x11Ku/fWXHWYNk314IWqsdFE
OHh4Ndv245sUwRSkyWeTerf9CUyeFBcu+VULreGHeh2xNdS7dB5LqhUN+HmzDRRlp3xRfL
0+RvezxyaPWwU9W8sUk1MzKxgv49KIjAtzmc29eDtIRpyg89Dx8ZDWKZgCZL3HfsCLIaSs
jqaZL1R4Ef/yIyMxN4g17UpRaKHnwTdgjSYgAS38emwBAAAAgQCNSfKv4oveyNahxR7886
lM5VCY/mYDGZBYXCIRcV/6i8C2dpZ0THZlJE+ojqWMW8cMx1sEU4Y977bGJffZ3+wGCxig
Y+dWOWInYYp9RvSyaNjEb62C7k1c8nVXuf/8NHzdIp7S3YBgSE8O5a4ZOr38kp0KFjrAbE
j3apiKgMzxQgAAAIEA/P7l6EbzX2dBD06w9NdJMTO6loOCwXCi3kSdBuCllC+UZrw/7OlY
Vj+Uc7arBiAfCCHkt/iarMl2+sqKGpAn5Dud3xvoJbkFbWDzBCLfcZB5Uoq8/b/weO8tqO
6eY9v4/GeNAAutxf2slPln7vf9HpkXh221CgpduyRQU+SEeq8AAACBANb0jM0mtzFGlPFD
qbaSnusQ4pKxQntVaJ8d64PwtFpvqElN2st29NtxnIclfmSODqvTAj4a8mvRZtZeJAYJMS
xqT6IUqcm3sL8zfT9CChSb42RzRWW9ywzRfUQMQSdpv7UVtRRKknXz9O/OuHGAFVsA3Tmu
dyvAQoDc3d2nveEBAAAAHGV0dG9yZUBsb2NhbGhvc3QubG9jYWxkb21haW4BAgMEBQY=
-----END OPENSSH PRIVATE KEY-----"""

rsa2048_private_openssh_old = """-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDUbsJJcM8UjoZV
opwPdexW2ZUBdinsRfMO0196ekvyBXu6NbNDZHtVEz47CbDxDgBlfw7DqkrePYt9
6qs583pibtAYKdZ3OUAVwIsFO9CSfFO14xLyscaU7L7IIhbbVOYpgc7hzz57+cfN
LiIIX0N+WaoHdzcd8Ii9fl9nXTh5AB/CxXe5vyx8WqyyRHsRg+TmZfRcKzEa/fmS
qmCTnYMlSSMx3LT3thLwxlV/ZODs9tsguY0Rk9UeYgOXeTOjSO7k6/6FnqVQny7/
drbeM2ZmDkOCUi1hp333GiqB6Tac6xu+yZZNl5lnuL+xMESqR6JCT/POcQqLEEBr
KhYQi0mvAgMBAAECggEAPqFxEqn5w7vJi1Hn5P8FcQ2xSr8F2S0+Xe6fxcJsxaws
MFhIw+PKIOkNfeZ7jnZsU4BEYBfC/wbyK2uWk/1e5cp5+8XJ/1iXtlIQKrjZ43ZY
T9Xk7jzPqXSrQuIaDwxW14H39sddSrv31lx1mDZN9eCFqrHRRDh4eDXb9uObFMEU
pMlnk3q3/QlMnhQXLvlVC63hh3odsTXUu3QeS6oVDfh5sw0UZad8UXy9Pkb3s8cm
j1sFPVvLFJNTMysYL+PSiIwLc5nNvXg7SEacoPPQ8fGQ1imYAmS9x37AiyGkrI6m
mS9UeBH/8iMjMTeINe1KUWih58E3YI0mIAEt/HpsAQKBgQD8/uXoRvNfZ0EPTrD0
10kxM7qWg4LBcKLeRJ0G4KWUL5RmvD/s6VhWP5RztqsGIB8IIeS3+JqsyXb6yooa
kCfkO53fG+gluQVtYPMEIt9xkHlSirz9v/B47y2o7p5j2/j8Z40AC63F/ayU+Wfu
9/0emReHbbUKCl27JFBT5IR6rwKBgQDW9IzNJrcxRpTxQ6m2kp7rEOKSsUJ7VWif
HeuD8LRab6hJTdrLdvTbcZyHJX5kjg6r0wI+GvJr0WbWXiQGCTEsak+iFKnJt7C/
M30/QgoUm+Nkc0VlvcsM0X1EDEEnab+1FbUUSpJ18/TvzrhxgBVbAN05rncrwEKA
3N3dp73hAQKBgQDjbrX5aIbydd0byK71e++1Rn5vPkw2X25ah63t99eB7n/nF0YU
UPTzm/Z1S3pVaFzdL7Lv25IY0IegDqG2HW9vElTqs6iu+LQzTttIFZ1u9uTJ2iTp
rDmeTc1rNw+2T5J2PRSZPOZ7vX7+8XKIdfDbJ97qBSqhmw4F5TA9KooZywKBgDDE
Aj14jw7qyFeD1jjJQqxphD1rYX3Bfp66lvez3/a0ZiVbOEv4jMxMFgrDAs2lPMbW
dCfKzTyQoRf4+4szAqjk5XQL5AkTV1HJSJzVSpwqUYg0boYKbMpXrGeHsDBU2V0n
s5EK6fdAhUzyRP3a5P1kUMwJPJf8YhoCAYOLzpQBAoGBAI1J8q/ii97I1qHFHvzz
qUzlUJj+ZgMZkFhcIhFxX/qLwLZ2lnRMdmUkT6iOpYxbxwzHWwRThj3vtsYl99nf
7AYLGKBj51Y5Yidhin1G9LJo2MRvrYLuTVzydVe5//w0fN0intLdgGBITw7lrhk6
vfySnQoWOsBsSPdqmIqAzPFC
-----END PRIVATE KEY-----"""

rsa2048_private_openssh_pwd = """-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAACmFlczI1Ni1jdHIAAAAGYmNyeXB0AAAAGAAAABAiSHFwF1
yQWybj5A23iTV1AAAAEAAAAAEAAAEXAAAAB3NzaC1yc2EAAAADAQABAAABAQDcFk6O6XGu
NTUmIGWFW/pzXsola0bpaydiIOlTtl34dvZUPRZpQfD+EHTKjkS1FSFklAsKXulcT5gaS3
5eyqxBq7jvs91fELmyt6iXQ60CLD9rY6Ub5GjaqizMdgsvSTW7M4hv4EDChmiB6xn1CSND
ctsaxzSKwJCzqZo+hDJWaUJh7oINkV1X2oMHCCosQbWIjPRqxzL1gdLq2Maamo1W76Mwl9
bCRMbt9nd7zWzM9ytPmY6/6mYdAcjQnRHHfab/YwVCuyWvLhsfROp/gYDJZ7ZjwWa3flWR
ac3Ih1ZN/ce8ljdHb6Q8jY9X1hAJ4MzbDXZ1sOCF1CpwlDfOTdC9AAAD4AE+jGg7O1vQHS
bFkD9h2xNXDzP3AQx5y4O+6h6WwlvAvd2wxtv9uVswxigWnoGFfJ7tDOVuVFUqDolq4Z/E
Xu5jBfHG/JJLy0IN+xWU6WijhZk9mKc/NPETNm961TQp+gF37MAWb8Cut2kerrD25+MZg5
e6tF0QY0IRR3s/vbVDe3MoObkJXg/zaWjO+JYG+v9X6zlVXQ8/LbYW/3JFF2Ot5Y7MlgU3
EFbK981/zwrIP4gbgroouv3wJsdma+6Kbftu6T1rr1iyl+5iM3QV3LVrykcnQ4+zb9rFW+
NizMy6dbtAo/0QCJ0/kfwC17Huthgl6gz4XAHXGLRLXy6dvhZGBFmqNv+xBb+t/25z63Yl
qvnkFXVtnKsPsWWvMkaWMSK/7VRA95vVdUe8kLQvNaAq6NvhDuYILZYLSXBNA4r+W7O54N
jaV5sWSaYuALpdQr9bPa5Aah1XOUhI/flD1wI1h15fCyUgTOI9kp+C/gyrK/axsH79/IIN
MmeMhXD5jf+Erv+swCqepJP6sXqZVdXszHv/Gf85v82niAqhGzEnss48gvAyW0z2CgX+1z
aR6a4PVoIJXa+m6J0R3eMvYRTImaSvHmbc0JXec1PNDePauT/+rSWXF8ocdKNyJdVf93pV
MkYhCGwFMv7V+7ZA53UhXc1N2Y60lSItNATOkFr/W+CP+YxBTqnt3jcr2O3E8zT+EXUMQp
RX62xR9Q1VDsXSvHac0yU22dkHugD/h3sdnOmAk4drGlMha8DZtrRltDTj1PCmJ9DZ8pT+
W+fyzM5Lcgk6mravZjSTcEHZbETr6nMWA4lO6zDAboYg6jS2ujWSLwQCe33pjgCvL2E15M
9dxGQN7tF6VP5LPvdQoNqGr7aPpho+2NH6bWrip7M8Ska+lSCX/zQYte/xWiCiHu08cSFk
6VGOOdBotnTTDPoqj9KBMvq0HO5vtlkB3qKfC1i2Kks1i/QgfWL3zETEjakOoSPtXBrc5q
OjrPaRMFIia43ApFNb5yOIDDbRi9Wuq6scPe4zzwzzvD3oFEY9CetsxP9xl9/9/ySJ5gYL
3c2KALQOEGN8k8PHinGe++x72adzi8OMISYMFyYF28gJ+uUDbPhqD5V9ClvxvWnkvhtWZO
c3pvYipgdgmsN5vi7JO9XRzPviZfdmIaxXm9xRSL+HawWU9bkStN+tCh4hNJMGnKAqN0uV
tlAaLHGCx0HqO4bzS0ZvMMcABulLSctWN/erHU/OK3ulyZxTTmlqsQAIHJPQ00ta6H/dnR
r7IU3dRtY9GCbp+IlQJidrib8f7wDCaySNcYygJru6roFgzFQd
-----END OPENSSH PRIVATE KEY-----"""

rsa2048_private_openssh_pwd_old = """-----BEGIN PRIVATE KEY-----
MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDcFk6O6XGuNTUm
IGWFW/pzXsola0bpaydiIOlTtl34dvZUPRZpQfD+EHTKjkS1FSFklAsKXulcT5ga
S35eyqxBq7jvs91fELmyt6iXQ60CLD9rY6Ub5GjaqizMdgsvSTW7M4hv4EDChmiB
6xn1CSNDctsaxzSKwJCzqZo+hDJWaUJh7oINkV1X2oMHCCosQbWIjPRqxzL1gdLq
2Maamo1W76Mwl9bCRMbt9nd7zWzM9ytPmY6/6mYdAcjQnRHHfab/YwVCuyWvLhsf
ROp/gYDJZ7ZjwWa3flWRac3Ih1ZN/ce8ljdHb6Q8jY9X1hAJ4MzbDXZ1sOCF1Cpw
lDfOTdC9AgMBAAECggEAEMY2eJf8TR3LDjvb4P0wqohn+dAiWHoNR2JgxjuZD+3p
OmRph945wvN4I1QSkoaow+SwrrqrKJj8a8yjNhBWbq7q6oIX9j3tGVz8IYNL9WVv
8/xlQin3f+sGfRLmKVV4HeuAk55Q8UKTRounr9BhequPXYwfShABN2BO3ELxHzrE
U36p7SPth+xev0ruo9t8BCztE9VFR1q9Amiqh1aipDKcFatvd2u4TYJ413Fj5j4p
Rs7UjZ5mzKmKvsSsktInZmuW9wm5pBUzjfZhJ03pXiwIAdxWqnXAKE8seocuodg+
AmHUQzlyewIW+0wBFuG1BrsHEAksLQEp8dJO6gMJdQKBgQD6XK/Ci1FYLPUa1DNZ
XVhGote7q+Kcl3deYNM+Lblxh1oacP8hfFbP1HlhdNyByQY1ZM468dINzfP9AcMp
N9DEP8bj44DNo4EoHKnWrzBERCOh9JNWct7KRVHRk3q0K569Nj0yF0PyOZVyfi6D
DgU7Mu/buIZ8xkVuWkrp9bPagwKBgQDhCxaYxduixdVu+yM7x8gTOeqp67hM8YqD
chqZsl4ciXWkNGw3DHej6DJK3xtjRQb41w6aDCmlfWs0zlE0LsXvr4R72XpficMB
gYHbGEVPyWjeeOZjJtMserchlazjRfIw4UaWiLJL2DJIjDbuknG9ZyBU1Ddypz8/
IyMvy+TDvwKBgQDdJEGbdbPETvGxYP56URlIS06DVrAz4RZvJtdwdLL4tLXB5U12
Jn4H1YXhr3eWrBnvz7raFf+Ucfax5HyeS877idoEMU/0VBghdjAOkW/w3L8crwv1
sEFaKSC8HaikvGLafq5PMH2z12lKWGp9GEVGpRd43OTuEbQCZX8GaSEUQQKBgQCP
shvYqyYmlnpFZjjGODgKBsZPf8Nr5iOS8S4JC/rJ7//dPNgIgn52e5J5emKrjWz5
QaECPlftYtssmb0CPAeJl6JZzrE0Bewtrvsy4hmH68x5metKToUy9pyu5jrB2Gzg
R0hiYKCwizj4WAfPaFUWIp5jbCqHnEFnWFFkeKX3UwKBgQCXPlGQFSJ1YcDiVLSy
oEMmNOsb29xfr6BR830QB1+w55QTcquhSo575to504vrQcEles3Q5xJodkSStn+b
LT/WAVIYbVy2GNKG1zmDLyoEBM2YNwpiR/nWU79cqgaQpUfwmErvNl1f1aeF5dyS
Ph2nM0Aup3dWPEah4fzW5jOMZQ==
-----END PRIVATE KEY-----"""


def TestImport_2048_test_import_openssh_public():
    key_file_ref = rsa2048_priv_pem
    key_file = rsa2048_pub_openssh

    key_ref = RSA.import_key(key_file_ref).public_key()
    key = RSA.import_key(key_file)
    asserts.assert_that(key_ref).is_equal_to(key)


def TestImport_2048_test_import_openssh_private_clear():
    key_file = rsa2048_private_openssh
    key_file_old = rsa2048_private_openssh_old

    key = RSA.import_key(key_file)
    key_old = RSA.import_key(key_file_old)

    asserts.assert_that(key).is_equal_to(key_old)


def TestImport_2048_test_import_openssh_private_password():
    key_file = rsa2048_private_openssh_pwd
    key_file_old = rsa2048_private_openssh_pwd_old

    key = RSA.import_key(key_file, bytes("password", encoding="utf-8"))
    key_old = RSA.import_key(key_file_old)
    asserts.assert_that(key).is_equal_to(key_old)


# noinspection DuplicatedCode
def _testsuite():
    _suite = unittest.TestSuite()
    _case = unittest.FunctionTestCase
    _suite.addTest(_case(ImportKeyTests_testImportKey1))
    _suite.addTest(_case(ImportKeyTests_testImportKey2))
    _suite.addTest(_case(ImportKeyTests_testImportKey3unicode))
    _suite.addTest(_case(ImportKeyTests_testImportKey3bytes))
    _suite.addTest(_case(ImportKeyTests_testImportKey4unicode))
    _suite.addTest(_case(ImportKeyTests_testImportKey4bytes))
    _suite.addTest(_case(ImportKeyTests_testImportKey5))
    _suite.addTest(_case(ImportKeyTests_testImportKey6))
    _suite.addTest(_case(ImportKeyTests_testImportKey7))
    _suite.addTest(_case(ImportKeyTests_testImportKey8))
    _suite.addTest(_case(ImportKeyTests_testImportKey9))
    _suite.addTest(_case(ImportKeyTests_testImportKey10))
    _suite.addTest(_case(ImportKeyTests_testImportKey11))
    _suite.addTest(_case(ImportKeyTests_testImportKey12))
    _suite.addTest(
        _case(ImportKeyTests_test_import_key_windows_cr_lf)
    )
    _suite.addTest(_case(ImportKeyTests_test_import_empty))
    _suite.addTest(_case(ImportKeyTests_testExportKey1))
    _suite.addTest(_case(ImportKeyTests_testExportKey2))
    _suite.addTest(_case(ImportKeyTests_testExportKey3))
    _suite.addTest(_case(ImportKeyTests_testExportKey4))
    _suite.addTest(_case(ImportKeyTests_testExportKey5))
    _suite.addTest(_case(ImportKeyTests_testExportKey7))
    _suite.addTest(_case(ImportKeyTests_testExportKey8))
    _suite.addTest(_case(ImportKeyTests_testExportKey9))
    _suite.addTest(_case(ImportKeyTests_testExportKey10))
    _suite.addTest(_case(ImportKeyTests_testExportKey11))
    _suite.addTest(_case(ImportKeyTests_testExportKey12))
    _suite.addTest(_case(ImportKeyTests_testExportKey13))
    _suite.addTest(_case(ImportKeyTests_testExportKey14))
    _suite.addTest(_case(ImportKeyTests_testExportKey15))
    _suite.addTest(_case(ImportKeyTests_test_import_key))
    _suite.addTest(_case(ImportKeyTests_test_exportKey))
    _suite.addTest(_case(ImportKeyFromX509Cert_test_x509v1))
    _suite.addTest(_case(ImportKeyFromX509Cert_test_x509v3))
    _suite.addTest(_case(TestImport_2048_test_import_openssh_public))
    _suite.addTest(_case(TestImport_2048_test_import_openssh_private_clear))
    _suite.addTest(_case(TestImport_2048_test_import_openssh_private_password))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
