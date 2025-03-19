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
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//collections", namedtuple="namedtuple")
load("@stdlib//operator", operator="operator")
load("@stdlib//re", re="re")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//Crypto/Math/Numbers", Integer="Integer")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Util/number", bytes_to_long="bytes_to_long", inverse="inverse")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", Result="Result", Ok="Ok", Error="Error", safe="safe")

load("@vendor//Crypto/PublicKey/ECC",
     ECC="ECC",
     EccPoint="EccPoint",
     _curves="curves",
     EccKey="EccKey"
     )


def test_basic_ECC_generate():
    key = ECC.generate(curve='P-256')
    out = key.export_key(format='PEM')
    asserts.assert_that(out).contains('-----BEGIN PRIVATE KEY-----')
    asserts.assert_that(out).contains('-----END PRIVATE KEY-----')


def test_basic_ECC_construct_example():
# https://verygoodsecurity.atlassian.net/browse/SI-203
# nosemgrep: secrets.misc.generic_private_key.generic_private_key
    expected = r"""-----BEGIN PRIVATE KEY-----
MIGFAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBGswaQIBAQQg12c78JWM96fm6y5F
ERvbYZZ2xjx+0UNGLO9qXymxeIgDQgAEi5xvAqjfAoOG2/SvXDt2t3QsJzXU0rSt
lWwO3LYapfwZu5BghENdUtEMxwpeOf01fmc50RL0sPCsWVf0+CoduQ==
-----END PRIVATE KEY-----"""
    key = ECC.construct(
        curve='P-256',
        d=97429661382245629931385077694012714418891341000979321935485875586665209231496,
        point_x=63147880260728048945060155999382431581790870540307511127381386767481037301244,
        point_y=11639218069252970133283017693609492584733444595055167772364063571679199763897
    )
    output = key.export_key(format='PEM')
    asserts.assert_that(output).is_equal_to(expected)


def TestEccPoint_test_mix():

    p1 = ECC.generate(curve='P-256').pointQ
    p2 = ECC.generate(curve='P-384').pointQ

    res = Result.Ok(operator).map(lambda x: x.add(p1, p2))
    if not res.is_err:
        fail("expected result to give an error")
    asserts.assert_that(Result.error_is("not on the same curve", res)).is_true()

    res = Result.Ok(operator).map(lambda x: x.iadd(p1, p2))
    if not res.is_err:
        fail("expected result to give an error")
    asserts.assert_that(Result.error_is("not on the same curve", res)).is_true()


def TestEccPoint_test_repr():
    p1 = ECC.construct(curve='P-256',
                       d=75467964919405407085864614198393977741148485328036093939970922195112333446269,
                       point_x=20573031766139722500939782666697015100983491952082159880539639074939225934381,
                       point_y=108863130203210779921520632367477406025152638284581252625277850513266505911389)
    asserts.assert_that(repr(p1)).is_equal_to("EccKey(curve='NIST P-256', point_x=20573031766139722500939782666697015100983491952082159880539639074939225934381, point_y=108863130203210779921520632367477406025152638284581252625277850513266505911389, d=75467964919405407085864614198393977741148485328036093939970922195112333446269)")


"""Tests defined in section 4.3 of https://www.nsa.gov/ia/_files/nist-routines.pdf"""


def TestEccPoint_NIST_P256_test_set():
    NIST4_3_pointS = EccPoint(
        0xde2444bebc8d36e682edd27e0f271508617519b3221a8fa0b77cab3989da97c9,
        0xc093ae7ff36e5380fc01a5aad1e66659702de80f53cec576b6350b243042a256)

    NIST4_3_pointT = EccPoint(
        0x55a8b00f8da1d44e62f6b3b25316212e39540dc861c89575bb8cf92e35e0986b,
        0x5421c3209c2d6c704835d82ac4c3dd90f61a8a52598b9e7ab656e9d8c8b24316)

    pointW = EccPoint(0, 0)
    pointW.set(NIST4_3_pointS)
    asserts.assert_that(pointW).is_equal_to(NIST4_3_pointS)


def TestEccPoint_NIST_P256_test_copy():
    NIST4_3_pointS = EccPoint(
        0xde2444bebc8d36e682edd27e0f271508617519b3221a8fa0b77cab3989da97c9,
        0xc093ae7ff36e5380fc01a5aad1e66659702de80f53cec576b6350b243042a256)

    NIST4_3_pointT = EccPoint(
        0x55a8b00f8da1d44e62f6b3b25316212e39540dc861c89575bb8cf92e35e0986b,
        0x5421c3209c2d6c704835d82ac4c3dd90f61a8a52598b9e7ab656e9d8c8b24316)
    pointW = NIST4_3_pointS.copy()
    asserts.assert_that(pointW).is_equal_to(NIST4_3_pointS)
    pointW.set(NIST4_3_pointT)
    asserts.assert_that(pointW).is_equal_to(NIST4_3_pointT)
    asserts.assert_that(NIST4_3_pointS).is_not_equal_to(NIST4_3_pointT)


def TestEccPoint_NIST_P256_test_negate():
    NIST4_3_pointS = EccPoint(
        0xde2444bebc8d36e682edd27e0f271508617519b3221a8fa0b77cab3989da97c9,
        0xc093ae7ff36e5380fc01a5aad1e66659702de80f53cec576b6350b243042a256)

    NIST4_3_pointT = EccPoint(
        0x55a8b00f8da1d44e62f6b3b25316212e39540dc861c89575bb8cf92e35e0986b,
        0x5421c3209c2d6c704835d82ac4c3dd90f61a8a52598b9e7ab656e9d8c8b24316)
    negS = operator.neg(NIST4_3_pointS)
    sum = NIST4_3_pointS + negS
    asserts.assert_that(sum).is_equal_to(NIST4_3_pointS.point_at_infinity())


def TestEccPoint_NIST_P256_test_addition():
    NIST4_3_pointS = EccPoint(
        0xde2444bebc8d36e682edd27e0f271508617519b3221a8fa0b77cab3989da97c9,
        0xc093ae7ff36e5380fc01a5aad1e66659702de80f53cec576b6350b243042a256)

    NIST4_3_pointT = EccPoint(
        0x55a8b00f8da1d44e62f6b3b25316212e39540dc861c89575bb8cf92e35e0986b,
        0x5421c3209c2d6c704835d82ac4c3dd90f61a8a52598b9e7ab656e9d8c8b24316)

    pointRx = 0x72b13dd4354b6b81745195e98cc5ba6970349191ac476bd4553cf35a545a067e
    pointRy = 0x8d585cbb2e1327d75241a8a122d7620dc33b13315aa5c9d46d013011744ac264

    pointR = NIST4_3_pointS + NIST4_3_pointT
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    pai = pointR.point_at_infinity()

    # S + 0
    pointR = NIST4_3_pointS + pai
    asserts.assert_that(pointR).is_equal_to(NIST4_3_pointS)

    # 0 + S
    pointR = pai + NIST4_3_pointS
    asserts.assert_that(pointR).is_equal_to(NIST4_3_pointS)

    # 0 + 0
    pointR = pai + pai
    asserts.assert_that(pointR).is_equal_to(pai)


def TestEccPoint_NIST_P256_test_inplace_addition():
    NIST4_3_pointS = EccPoint(
        0xde2444bebc8d36e682edd27e0f271508617519b3221a8fa0b77cab3989da97c9,
        0xc093ae7ff36e5380fc01a5aad1e66659702de80f53cec576b6350b243042a256)

    NIST4_3_pointT = EccPoint(
        0x55a8b00f8da1d44e62f6b3b25316212e39540dc861c89575bb8cf92e35e0986b,
        0x5421c3209c2d6c704835d82ac4c3dd90f61a8a52598b9e7ab656e9d8c8b24316)

    pointRx = 0x72b13dd4354b6b81745195e98cc5ba6970349191ac476bd4553cf35a545a067e
    pointRy = 0x8d585cbb2e1327d75241a8a122d7620dc33b13315aa5c9d46d013011744ac264

    pointR = NIST4_3_pointS.copy()
    pointR += NIST4_3_pointT
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    pai = pointR.point_at_infinity()

    # S + 0
    pointR = NIST4_3_pointS.copy()
    pointR += pai
    asserts.assert_that(pointR).is_equal_to(NIST4_3_pointS)

    # 0 + S
    pointR = pai.copy()
    pointR += NIST4_3_pointS
    asserts.assert_that(pointR).is_equal_to(NIST4_3_pointS)

    # 0 + 0
    pointR = pai.copy()
    pointR += pai
    asserts.assert_that(pointR).is_equal_to(pai)


def TestEccPoint_NIST_P256_test_doubling():
    NIST4_3_pointS = EccPoint(
        0xde2444bebc8d36e682edd27e0f271508617519b3221a8fa0b77cab3989da97c9,
        0xc093ae7ff36e5380fc01a5aad1e66659702de80f53cec576b6350b243042a256)

    NIST4_3_pointT = EccPoint(
        0x55a8b00f8da1d44e62f6b3b25316212e39540dc861c89575bb8cf92e35e0986b,
        0x5421c3209c2d6c704835d82ac4c3dd90f61a8a52598b9e7ab656e9d8c8b24316)

    pointRx = 0x7669e6901606ee3ba1a8eef1e0024c33df6c22f3b17481b82a860ffcdb6127b0
    pointRy = 0xfa878162187a54f6c39f6ee0072f33de389ef3eecd03023de10ca2c1db61d0c7

    pointR = NIST4_3_pointS.copy()
    pointR.double()
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    # 2*0
    pai = NIST4_3_pointS.point_at_infinity()
    pointR = pai.copy()
    pointR.double()
    asserts.assert_that(pointR).is_equal_to(pai)

    # S + S
    pointR = NIST4_3_pointS.copy()
    pointR += pointR
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)


def TestEccPoint_NIST_P256_test_scalar_multiply():
    NIST4_3_pointS = EccPoint(
        0xde2444bebc8d36e682edd27e0f271508617519b3221a8fa0b77cab3989da97c9,
        0xc093ae7ff36e5380fc01a5aad1e66659702de80f53cec576b6350b243042a256)

    NIST4_3_pointT = EccPoint(
        0x55a8b00f8da1d44e62f6b3b25316212e39540dc861c89575bb8cf92e35e0986b,
        0x5421c3209c2d6c704835d82ac4c3dd90f61a8a52598b9e7ab656e9d8c8b24316)

    d = 0xc51e4753afdec1e6b6c6a5b992f43f8dd0c7a8933072708b6522468b2ffb06fd
    pointRx = 0x51d08d5f2d4278882946d88d83c97d11e62becc3cfc18bedacc89ba34eeca03f
    pointRy = 0x75ee68eb8bf626aa5b673ab51f6e744e06f8fcf8a6c0cf3035beca956a7b41d5

    pointR = NIST4_3_pointS * d
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    # 0*S
    pai = NIST4_3_pointS.point_at_infinity()
    pointR = NIST4_3_pointS * 0
    asserts.assert_that(pointR).is_equal_to(pai)

    # -1*S
    # def neg_1_times_S():
    #     return operator.mul(NIST4_3_pointS, -1)

    asserts.assert_fails(lambda: NIST4_3_pointS * -1, ".*?ValueError")

    # Reverse order
    pointR = d * NIST4_3_pointS
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    pointR = Integer(d) * NIST4_3_pointS
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)


def TestEccPoint_NIST_P256_test_joing_scalar_multiply():
    NIST4_3_pointS = EccPoint(
        0xde2444bebc8d36e682edd27e0f271508617519b3221a8fa0b77cab3989da97c9,
        0xc093ae7ff36e5380fc01a5aad1e66659702de80f53cec576b6350b243042a256)

    NIST4_3_pointT = EccPoint(
        0x55a8b00f8da1d44e62f6b3b25316212e39540dc861c89575bb8cf92e35e0986b,
        0x5421c3209c2d6c704835d82ac4c3dd90f61a8a52598b9e7ab656e9d8c8b24316)

    d = 0xc51e4753afdec1e6b6c6a5b992f43f8dd0c7a8933072708b6522468b2ffb06fd
    e = 0xd37f628ece72a462f0145cbefe3f0b355ee8332d37acdd83a358016aea029db7
    pointRx = 0xd867b4679221009234939221b8046245efcf58413daacbeff857b8588341f6b8
    pointRy = 0xf2504055c03cede12d22720dad69c745106b6607ec7e50dd35d54bd80f615275

    t = NIST4_3_pointS * d

    pointR = (NIST4_3_pointS * d) + (NIST4_3_pointT * e)
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)


def TestEccPoint_NIST_P256_test_sizes():
    NIST4_3_pointS = EccPoint(
        0xde2444bebc8d36e682edd27e0f271508617519b3221a8fa0b77cab3989da97c9,
        0xc093ae7ff36e5380fc01a5aad1e66659702de80f53cec576b6350b243042a256)

    NIST4_3_pointT = EccPoint(
        0x55a8b00f8da1d44e62f6b3b25316212e39540dc861c89575bb8cf92e35e0986b,
        0x5421c3209c2d6c704835d82ac4c3dd90f61a8a52598b9e7ab656e9d8c8b24316)

    asserts.assert_that(NIST4_3_pointS.size_in_bits()).is_equal_to(256)
    asserts.assert_that(NIST4_3_pointS.size_in_bytes()).is_equal_to(32)


"""Tests defined in section 4.4 of https://www.nsa.gov/ia/_files/nist-routines.pdf"""

NIST4_4_pointS = EccPoint(
            0xfba203b81bbd23f2b3be971cc23997e1ae4d89e69cb6f92385dda82768ada415ebab4167459da98e62b1332d1e73cb0e,
            0x5ffedbaefdeba603e7923e06cdb5d0c65b22301429293376d5c6944e3fa6259f162b4788de6987fd59aed5e4b5285e45,
            "p384")

NIST4_4_pointT = EccPoint(
            0xaacc05202e7fda6fc73d82f0a66220527da8117ee8f8330ead7d20ee6f255f582d8bd38c5a7f2b40bcdb68ba13d81051,
            0x84009a263fefba7c2c57cffa5db3634d286131afc0fca8d25afa22a7b5dce0d9470da89233cee178592f49b6fecb5092,
            "p384")

def TestEccPoint_NIST_P384_test_set():
    pointW = EccPoint(0, 0, "p384")
    pointW.set(NIST4_4_pointS)
    asserts.assert_that(pointW).is_equal_to(NIST4_4_pointS)

def TestEccPoint_NIST_P384_test_copy():
    pointW = NIST4_4_pointS.copy()
    asserts.assert_that(pointW).is_equal_to(NIST4_4_pointS)
    pointW.set(NIST4_4_pointT)
    asserts.assert_that(pointW).is_equal_to(NIST4_4_pointT)
    asserts.assert_that(NIST4_4_pointS).is_not_equal_to(NIST4_4_pointT)

def TestEccPoint_NIST_P384_test_negate():
    negS = operator.neg(NIST4_4_pointS)
    sum = NIST4_4_pointS + negS
    asserts.assert_that(sum).is_equal_to(NIST4_4_pointS.point_at_infinity())

def TestEccPoint_NIST_P384_test_addition():
    pointRx = 0x12dc5ce7acdfc5844d939f40b4df012e68f865b89c3213ba97090a247a2fc009075cf471cd2e85c489979b65ee0b5eed
    pointRy = 0x167312e58fe0c0afa248f2854e3cddcb557f983b3189b67f21eee01341e7e9fe67f6ee81b36988efa406945c8804a4b0

    pointR = NIST4_4_pointS + NIST4_4_pointT
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    pai = pointR.point_at_infinity()

    # S + 0
    pointR = NIST4_4_pointS + pai
    asserts.assert_that(pointR).is_equal_to(NIST4_4_pointS)

    # 0 + S
    pointR = pai + NIST4_4_pointS
    asserts.assert_that(pointR).is_equal_to(NIST4_4_pointS)

    # 0 + 0
    pointR = pai + pai
    asserts.assert_that(pointR).is_equal_to(pai)

def TestEccPoint_NIST_P384__test_inplace_addition():
    pointRx = 0x12dc5ce7acdfc5844d939f40b4df012e68f865b89c3213ba97090a247a2fc009075cf471cd2e85c489979b65ee0b5eed
    pointRy = 0x167312e58fe0c0afa248f2854e3cddcb557f983b3189b67f21eee01341e7e9fe67f6ee81b36988efa406945c8804a4b0

    pointR = NIST4_4_pointS.copy()
    pointR += NIST4_4_pointT
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    pai = pointR.point_at_infinity()

    # S + 0
    pointR = NIST4_4_pointS.copy()
    pointR += pai
    asserts.assert_that(pointR).is_equal_to(NIST4_4_pointS)

    # 0 + S
    pointR = pai.copy()
    pointR += NIST4_4_pointS
    asserts.assert_that(pointR).is_equal_to(NIST4_4_pointS)

    # 0 + 0
    pointR = pai.copy()
    pointR += pai
    asserts.assert_that(pointR).is_equal_to(pai)

def TestEccPoint_NIST_P384_test_doubling():
    pointRx = 0x2a2111b1e0aa8b2fc5a1975516bc4d58017ff96b25e1bdff3c229d5fac3bacc319dcbec29f9478f42dee597b4641504c
    pointRy = 0xfa2e3d9dc84db8954ce8085ef28d7184fddfd1344b4d4797343af9b5f9d837520b450f726443e4114bd4e5bdb2f65ddd

    pointR = NIST4_4_pointS.copy()
    pointR.double()
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    # 2*0
    pai = NIST4_4_pointS.point_at_infinity()
    pointR = pai.copy()
    pointR.double()
    asserts.assert_that(pointR).is_equal_to(pai)

    # S + S
    pointR = NIST4_4_pointS.copy()
    pointR += pointR
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

def TestEccPoint_NIST_P384_test_scalar_multiply():
    d = 0xa4ebcae5a665983493ab3e626085a24c104311a761b5a8fdac052ed1f111a5c44f76f45659d2d111a61b5fdd97583480
    pointRx = 0xe4f77e7ffeb7f0958910e3a680d677a477191df166160ff7ef6bb5261f791aa7b45e3e653d151b95dad3d93ca0290ef2
    pointRy = 0xac7dee41d8c5f4a7d5836960a773cfc1376289d3373f8cf7417b0c6207ac32e913856612fc9ff2e357eb2ee05cf9667f

    pointR = NIST4_4_pointS * d
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    # 0*S
    pai = NIST4_4_pointS.point_at_infinity()
    pointR = NIST4_4_pointS * 0
    asserts.assert_that(pointR).is_equal_to(pai)

    # -1*S
    asserts.assert_fails(lambda: NIST4_4_pointS * -1, ".*?ValueError")

def TestEccPoint_NIST_P384_test_joing_scalar_multiply():
    d = 0xa4ebcae5a665983493ab3e626085a24c104311a761b5a8fdac052ed1f111a5c44f76f45659d2d111a61b5fdd97583480
    e = 0xafcf88119a3a76c87acbd6008e1349b29f4ba9aa0e12ce89bcfcae2180b38d81ab8cf15095301a182afbc6893e75385d
    pointRx = 0x917ea28bcd641741ae5d18c2f1bd917ba68d34f0f0577387dc81260462aea60e2417b8bdc5d954fc729d211db23a02dc
    pointRy = 0x1a29f7ce6d074654d77b40888c73e92546c8f16a5ff6bcbd307f758d4aee684beff26f6742f597e2585c86da908f7186

    t = NIST4_4_pointS * d

    pointR = NIST4_4_pointS * d + NIST4_4_pointT * e
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

def TestEccPoint_NIST_P384_test_sizes():
    asserts.assert_that(NIST4_4_pointS.size_in_bits()).is_equal_to(384)
    asserts.assert_that(NIST4_4_pointS.size_in_bytes()).is_equal_to(48)


"""Tests defined in section 4.5 of https://www.nsa.gov/ia/_files/nist-routines.pdf"""

NIST4_5_pointS = EccPoint(
            0x000001d5c693f66c08ed03ad0f031f937443458f601fd098d3d0227b4bf62873af50740b0bb84aa157fc847bcf8dc16a8b2b8bfd8e2d0a7d39af04b089930ef6dad5c1b4,
            0x00000144b7770963c63a39248865ff36b074151eac33549b224af5c8664c54012b818ed037b2b7c1a63ac89ebaa11e07db89fcee5b556e49764ee3fa66ea7ae61ac01823,
            "p521")

NIST4_5_pointT = EccPoint(
            0x000000f411f2ac2eb971a267b80297ba67c322dba4bb21cec8b70073bf88fc1ca5fde3ba09e5df6d39acb2c0762c03d7bc224a3e197feaf760d6324006fe3be9a548c7d5,
            0x000001fdf842769c707c93c630df6d02eff399a06f1b36fb9684f0b373ed064889629abb92b1ae328fdb45534268384943f0e9222afe03259b32274d35d1b9584c65e305,
            "p521")

def TestEccPoint_NIST_P521_test_set():
    pointW = EccPoint(0, 0)
    pointW.set(NIST4_5_pointS)
    asserts.assert_that(pointW).is_equal_to(NIST4_5_pointS)

def TestEccPoint_NIST_P521_test_copy():
    pointW = NIST4_5_pointS.copy()
    asserts.assert_that(pointW).is_equal_to(NIST4_5_pointS)
    pointW.set(NIST4_5_pointT)
    asserts.assert_that(pointW).is_equal_to(NIST4_5_pointT)
    asserts.assert_that(NIST4_5_pointS).is_not_equal_to(NIST4_5_pointT)

def TestEccPoint_NIST_P521_test_negate():
    negS = operator.neg(NIST4_5_pointS)
    sum = NIST4_5_pointS + negS
    asserts.assert_that(sum).is_equal_to(NIST4_5_pointS.point_at_infinity())

def TestEccPoint_NIST_P521_test_addition():
    pointRx = 0x000001264ae115ba9cbc2ee56e6f0059e24b52c8046321602c59a339cfb757c89a59c358a9a8e1f86d384b3f3b255ea3f73670c6dc9f45d46b6a196dc37bbe0f6b2dd9e9
    pointRy = 0x00000062a9c72b8f9f88a271690bfa017a6466c31b9cadc2fc544744aeb817072349cfddc5ad0e81b03f1897bd9c8c6efbdf68237dc3bb00445979fb373b20c9a967ac55

    pointR = NIST4_5_pointS + NIST4_5_pointT
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    pai = pointR.point_at_infinity()

    # S + 0
    pointR = NIST4_5_pointS + pai
    asserts.assert_that(pointR).is_equal_to(NIST4_5_pointS)

    # 0 + S
    pointR = pai + NIST4_5_pointS
    asserts.assert_that(pointR).is_equal_to(NIST4_5_pointS)

    # 0 + 0
    pointR = pai + pai
    asserts.assert_that(pointR).is_equal_to(pai)

def TestEccPoint_NIST_P521_test_inplace_addition():
    pointRx = 0x000001264ae115ba9cbc2ee56e6f0059e24b52c8046321602c59a339cfb757c89a59c358a9a8e1f86d384b3f3b255ea3f73670c6dc9f45d46b6a196dc37bbe0f6b2dd9e9
    pointRy = 0x00000062a9c72b8f9f88a271690bfa017a6466c31b9cadc2fc544744aeb817072349cfddc5ad0e81b03f1897bd9c8c6efbdf68237dc3bb00445979fb373b20c9a967ac55

    pointR = NIST4_5_pointS.copy()
    pointR += NIST4_5_pointT
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    pai = pointR.point_at_infinity()

    # S + 0
    pointR = NIST4_5_pointS.copy()
    pointR += pai
    asserts.assert_that(pointR).is_equal_to(NIST4_5_pointS)

    # 0 + S
    pointR = pai.copy()
    pointR += NIST4_5_pointS
    asserts.assert_that(pointR).is_equal_to(NIST4_5_pointS)

    # 0 + 0
    pointR = pai.copy()
    pointR += pai
    asserts.assert_that(pointR).is_equal_to(pai)

def TestEccPoint_NIST_P521_test_doubling():
    pointRx = 0x0000012879442f2450c119e7119a5f738be1f1eba9e9d7c6cf41b325d9ce6d643106e9d61124a91a96bcf201305a9dee55fa79136dc700831e54c3ca4ff2646bd3c36bc6
    pointRy = 0x0000019864a8b8855c2479cbefe375ae553e2393271ed36fadfc4494fc0583f6bd03598896f39854abeae5f9a6515a021e2c0eef139e71de610143f53382f4104dccb543

    pointR = NIST4_5_pointS.copy()
    pointR.double()
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    # 2*0
    pai = NIST4_5_pointS.point_at_infinity()
    pointR = pai.copy()
    pointR.double()
    asserts.assert_that(pointR).is_equal_to(pai)

    # S + S
    pointR = NIST4_5_pointS.copy()
    pointR += pointR
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

def TestEccPoint_NIST_P521_test_scalar_multiply():
    d = 0x000001eb7f81785c9629f136a7e8f8c674957109735554111a2a866fa5a166699419bfa9936c78b62653964df0d6da940a695c7294d41b2d6600de6dfcf0edcfc89fdcb1
    pointRx = 0x00000091b15d09d0ca0353f8f96b93cdb13497b0a4bb582ae9ebefa35eee61bf7b7d041b8ec34c6c00c0c0671c4ae063318fb75be87af4fe859608c95f0ab4774f8c95bb
    pointRy = 0x00000130f8f8b5e1abb4dd94f6baaf654a2d5810411e77b7423965e0c7fd79ec1ae563c207bd255ee9828eb7a03fed565240d2cc80ddd2cecbb2eb50f0951f75ad87977f

    pointR = NIST4_5_pointS * d
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

    # 0*S
    pai = NIST4_5_pointS.point_at_infinity()
    pointR = NIST4_5_pointS * 0
    asserts.assert_that(pointR).is_equal_to(pai)

    # -1*S
    asserts.assert_fails(lambda: NIST4_5_pointS * -1, ".*?ValueError")

def TestEccPoint_NIST_P521_test_joing_scalar_multiply():
    d = 0x000001eb7f81785c9629f136a7e8f8c674957109735554111a2a866fa5a166699419bfa9936c78b62653964df0d6da940a695c7294d41b2d6600de6dfcf0edcfc89fdcb1
    e = 0x00000137e6b73d38f153c3a7575615812608f2bab3229c92e21c0d1c83cfad9261dbb17bb77a63682000031b9122c2f0cdab2af72314be95254de4291a8f85f7c70412e3
    pointRx = 0x0000009d3802642b3bea152beb9e05fba247790f7fc168072d363340133402f2585588dc1385d40ebcb8552f8db02b23d687cae46185b27528adb1bf9729716e4eba653d
    pointRy = 0x0000000fe44344e79da6f49d87c1063744e5957d9ac0a505bafa8281c9ce9ff25ad53f8da084a2deb0923e46501de5797850c61b229023dd9cf7fc7f04cd35ebb026d89d

    t = NIST4_5_pointS * d

    pointR = NIST4_5_pointS * d
    pointR += NIST4_5_pointT * e
    asserts.assert_that(pointR.x).is_equal_to(pointRx)
    asserts.assert_that(pointR.y).is_equal_to(pointRy)

def TestEccPoint_NIST_P521_test_sizes():
    asserts.assert_that(NIST4_5_pointS.size_in_bits()).is_equal_to(521)
    asserts.assert_that(NIST4_5_pointS.size_in_bytes()).is_equal_to(66)

# TODO: load test vectors.

# """Test vectors from http://point-at-infinity.org/ecc/nisttv"""
#
# curve = _curves['p256']
# pointG = EccPoint(curve.Gx, curve.Gy, "p256")
#
#
# tv_pai = load_test_vectors(("PublicKey", "ECC"),
#                     "point-at-infinity.org-P256.txt",
#                     "P-256 tests from point-at-infinity.org",
#                     {"k": lambda k: int(k),
#                      "x": lambda x: int(x, 16),
#                      "y": lambda y: int(y, 16)}) or []
# for tv in tv_pai:
#     def TestEccPoint_PAI_P256_new_test(scalar=tv.k, x=tv.x, y=tv.y):
#         result = pointG * scalar
#         asserts.assert_that(result.x).is_equal_to(x)
#         asserts.assert_that(result.y).is_equal_to(y)
#     setattr(TestEccPoint_PAI_P256, "test_%d" % tv.count, new_test)
# """Test vectors from http://point-at-infinity.org/ecc/nisttv"""
#
# curve = _curves['p384']
# pointG = EccPoint(curve.Gx, curve.Gy, "p384")
#
#
# tv_pai = load_test_vectors(("PublicKey", "ECC"),
#                     "point-at-infinity.org-P384.txt",
#                     "P-384 tests from point-at-infinity.org",
#                     {"k" : lambda k: int(k),
#                      "x" : lambda x: int(x, 16),
#                      "y" : lambda y: int(y, 16)}) or []
# for tv in tv_pai:
#     def TestEccPoint_PAI_P384_new_test(scalar=tv.k, x=tv.x, y=tv.y):
#         result = pointG * scalar
#         asserts.assert_that(result.x).is_equal_to(x)
#         asserts.assert_that(result.y).is_equal_to(y)
#     setattr(TestEccPoint_PAI_P384, "test_%d" % tv.count, new_test)
# """Test vectors from http://point-at-infinity.org/ecc/nisttv"""
#
# curve = _curves['p521']
# pointG = EccPoint(curve.Gx, curve.Gy, "p521")
#
#
# tv_pai = load_test_vectors(("PublicKey", "ECC"),
#                     "point-at-infinity.org-P521.txt",
#                     "P-521 tests from point-at-infinity.org",
#                     {"k": lambda k: int(k),
#                      "x": lambda x: int(x, 16),
#                      "y": lambda y: int(y, 16)}) or []
# for tv in tv_pai:
#     def TestEccPoint_PAI_P521_new_test(scalar=tv.k, x=tv.x, y=tv.y):
#         result = pointG * scalar
#         asserts.assert_that(result.x).is_equal_to(x)
#         asserts.assert_that(result.y).is_equal_to(y)
#     setattr(TestEccPoint_PAI_P521, "test_%d" % tv.count, new_test)

def TestEccKey_P256_test_private_key():

    key = EccKey(curve="P-256", d=1)
    asserts.assert_that(key.d).is_equal_to(1)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ.x).is_equal_to(_curves['p256'].Gx)
    asserts.assert_that(key.pointQ.y).is_equal_to(_curves['p256'].Gy)

    point = EccPoint(_curves['p256'].Gx, _curves['p256'].Gy)
    key = EccKey(curve="P-256", d=1, point=point)
    asserts.assert_that(key.d).is_equal_to(1)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ).is_equal_to(point)

    # Other names
    key = EccKey(curve="secp256r1", d=1)
    key = EccKey(curve="prime256v1", d=1)

def TestEccKey_P256_test_public_key():

    point = EccPoint(_curves['p256'].Gx, _curves['p256'].Gy)
    key = EccKey(curve="P-256", point=point)
    asserts.assert_that(key.has_private()).is_false()
    asserts.assert_that(key.pointQ).is_equal_to(point)

def TestEccKey_P256_test_public_key_derived():

    priv_key = EccKey(curve="P-256", d=3)
    pub_key = priv_key.public_key()
    asserts.assert_that(pub_key.has_private()).is_false()
    asserts.assert_that(priv_key.pointQ).is_equal_to(pub_key.pointQ)

def TestEccKey_P256_test_invalid_curve():
    asserts.assert_fails(lambda: EccKey(curve="P-257", d=1), ".*?ValueError")


def TestEccKey_P256_test_invalid_d():
    asserts.assert_fails(lambda: EccKey(curve="P-256", d=0), ".*?ValueError")
    asserts.assert_fails(lambda: EccKey(curve="P-256", d=_curves['p256'].order),
                         ".*?ValueError")

def TestEccKey_P256_test_equality():

    private_key = ECC.construct(d=3, curve="P-256")
    private_key2 = ECC.construct(d=3, curve="P-256")
    private_key3 = ECC.construct(d=4, curve="P-256")

    public_key = private_key.public_key()
    public_key2 = private_key2.public_key()
    public_key3 = private_key3.public_key()

    asserts.assert_that(private_key).is_equal_to(private_key2)
    asserts.assert_that(private_key).is_not_equal_to(private_key3)

    asserts.assert_that(public_key).is_equal_to(public_key2)
    asserts.assert_that(public_key).is_not_equal_to(public_key3)

    asserts.assert_that(public_key).is_not_equal_to(private_key)

def TestEccKey_P384_test_private_key():

    p384 = _curves['p384']

    key = EccKey(curve="P-384", d=1)
    asserts.assert_that(key.d).is_equal_to(1)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ.x).is_equal_to(p384.Gx)
    asserts.assert_that(key.pointQ.y).is_equal_to(p384.Gy)

    point = EccPoint(p384.Gx, p384.Gy, "p384")
    key = EccKey(curve="P-384", d=1, point=point)
    asserts.assert_that(key.d).is_equal_to(1)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ).is_equal_to(point)

    # Other names
    key = EccKey(curve="p384", d=1)
    key = EccKey(curve="secp384r1", d=1)
    key = EccKey(curve="prime384v1", d=1)

def TestEccKey_P384_test_public_key():

    p384 = _curves['p384']
    point = EccPoint(p384.Gx, p384.Gy, 'p384')
    key = EccKey(curve="P-384", point=point)
    asserts.assert_that(key.has_private()).is_false()
    asserts.assert_that(key.pointQ).is_equal_to(point)

def TestEccKey_P384_test_public_key_derived():

    priv_key = EccKey(curve="P-384", d=3)
    pub_key = priv_key.public_key()
    asserts.assert_that(pub_key.has_private()).is_false()
    asserts.assert_that(priv_key.pointQ).is_equal_to(pub_key.pointQ)

def TestEccKey_P384_test_invalid_curve():
    asserts.assert_fails(lambda: EccKey(curve="P-385", d=1), ".*?ValueError")


def TestEccKey_P384_test_invalid_d():
    asserts.assert_fails(lambda: EccKey(curve="P-384", d=0), ".*?ValueError")
    asserts.assert_fails(lambda: EccKey(curve="P-384", d=_curves['p384'].order),
                         ".*?ValueError")

def TestEccKey_P384_test_equality():

    private_key = ECC.construct(d=3, curve="P-384")
    private_key2 = ECC.construct(d=3, curve="P-384")
    private_key3 = ECC.construct(d=4, curve="P-384")

    public_key = private_key.public_key()
    public_key2 = private_key2.public_key()
    public_key3 = private_key3.public_key()

    asserts.assert_that(private_key).is_equal_to(private_key2)
    asserts.assert_that(private_key).is_not_equal_to(private_key3)

    asserts.assert_that(public_key).is_equal_to(public_key2)
    asserts.assert_that(public_key).is_not_equal_to(public_key3)

    asserts.assert_that(public_key).is_not_equal_to(private_key)

def TestEccKey_P521_test_private_key():

    p521 = _curves['p521']

    key = EccKey(curve="P-521", d=1)
    asserts.assert_that(key.d).is_equal_to(1)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ.x).is_equal_to(p521.Gx)
    asserts.assert_that(key.pointQ.y).is_equal_to(p521.Gy)

    point = EccPoint(p521.Gx, p521.Gy, "p521")
    key = EccKey(curve="P-521", d=1, point=point)
    asserts.assert_that(key.d).is_equal_to(1)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ).is_equal_to(point)

    # Other names
    key = EccKey(curve="p521", d=1)
    key = EccKey(curve="secp521r1", d=1)
    key = EccKey(curve="prime521v1", d=1)

def TestEccKey_P521_test_public_key():

    p521 = _curves['p521']
    point = EccPoint(p521.Gx, p521.Gy, 'p521')
    key = EccKey(curve="P-384", point=point)
    asserts.assert_that(key.has_private()).is_false()
    asserts.assert_that(key.pointQ).is_equal_to(point)

def TestEccKey_P521_test_public_key_derived():

    priv_key = EccKey(curve="P-521", d=3)
    pub_key = priv_key.public_key()
    asserts.assert_that(pub_key.has_private()).is_false()
    asserts.assert_that(priv_key.pointQ).is_equal_to(pub_key.pointQ)

def TestEccKey_P521_test_invalid_curve():
    asserts.assert_fails(lambda: EccKey(curve="P-522", d=1), ".*?ValueError")


def TestEccKey_P521_test_invalid_d():
    asserts.assert_fails(lambda: EccKey(curve="P-521", d=0), ".*?ValueError")
    asserts.assert_fails(lambda: EccKey(curve="P-521", d=_curves['p521'].order),
                         ".*?ValueError")

def TestEccKey_P521_test_equality():

    private_key = ECC.construct(d=3, curve="P-521")
    private_key2 = ECC.construct(d=3, curve="P-521")
    private_key3 = ECC.construct(d=4, curve="P-521")

    public_key = private_key.public_key()
    public_key2 = private_key2.public_key()
    public_key3 = private_key3.public_key()

    asserts.assert_that(private_key).is_equal_to(private_key2)
    asserts.assert_that(private_key).is_not_equal_to(private_key3)

    asserts.assert_that(public_key).is_equal_to(public_key2)
    asserts.assert_that(public_key).is_not_equal_to(public_key3)

    asserts.assert_that(public_key).is_not_equal_to(private_key)

def TestEccModule_P256_test_generate():

    key = ECC.generate(curve="P-256")
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ).is_equal_to(EccPoint(_curves['p256'].Gx,
                                          _curves['p256'].Gy) * key.d)

    # Other names
    ECC.generate(curve="secp256r1")
    ECC.generate(curve="prime256v1")

def TestEccModule_P256_test_construct():

    key = ECC.construct(curve="P-256", d=1)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ).is_equal_to(_curves['p256'].G)

    key = ECC.construct(curve="P-256", point_x=_curves['p256'].Gx,
                        point_y=_curves['p256'].Gy)
    asserts.assert_that(key.has_private()).is_false()
    asserts.assert_that(key.pointQ).is_equal_to(_curves['p256'].G)

    # Other names
    ECC.construct(curve="p256", d=1)
    ECC.construct(curve="secp256r1", d=1)
    ECC.construct(curve="prime256v1", d=1)

def TestEccModule_P256_test_negative_construct():
    coord = dict(point_x=10, point_y=4)
    coordG = dict(point_x=_curves['p256'].Gx, point_y=_curves['p256'].Gy)

    asserts.assert_fails(lambda: ECC.construct(curve="P-256", **coord), ".*?ValueError")
    asserts.assert_fails(lambda: ECC.construct(curve="P-256", d=2, **coordG), ".*?ValueError")

def TestEccModule_P384_test_generate():

    curve = _curves['p384']
    key = ECC.generate(curve="P-384")
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ).is_equal_to(EccPoint(curve.Gx, curve.Gy, "p384") * key.d)

    # Other names
    ECC.generate(curve="secp384r1")
    ECC.generate(curve="prime384v1")

def TestEccModule_P384_test_construct():

    curve = _curves['p384']
    key = ECC.construct(curve="P-384", d=1)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ).is_equal_to(_curves['p384'].G)

    key = ECC.construct(curve="P-384", point_x=curve.Gx, point_y=curve.Gy)
    asserts.assert_that(key.has_private()).is_false()
    asserts.assert_that(key.pointQ).is_equal_to(curve.G)

    # Other names
    ECC.construct(curve="p384", d=1)
    ECC.construct(curve="secp384r1", d=1)
    ECC.construct(curve="prime384v1", d=1)

def TestEccModule_P384_test_negative_construct():
    coord = dict(point_x=10, point_y=4)
    coordG = dict(point_x=_curves['p384'].Gx, point_y=_curves['p384'].Gy)

    asserts.assert_fails(lambda: ECC.construct(curve="P-384", **coord), ".*?ValueError")
    asserts.assert_fails(lambda: ECC.construct(curve="P-384", d=2, **coordG), ".*?ValueError")

def TestEccModule_P521_test_generate():

    curve = _curves['p521']
    key = ECC.generate(curve="P-521")
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ).is_equal_to(EccPoint(curve.Gx, curve.Gy, "p521") * key.d)

    # Other names
    ECC.generate(curve="secp521r1")
    ECC.generate(curve="prime521v1")

def TestEccModule_P521_test_construct():

    curve = _curves['p521']
    key = ECC.construct(curve="P-521", d=1)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ).is_equal_to(_curves['p521'].G)

    key = ECC.construct(curve="P-521", point_x=curve.Gx, point_y=curve.Gy)
    asserts.assert_that(key.has_private()).is_false()
    asserts.assert_that(key.pointQ).is_equal_to(curve.G)

    # Other names
    ECC.construct(curve="p521", d=1)
    ECC.construct(curve="secp521r1", d=1)
    ECC.construct(curve="prime521v1", d=1)

def TestEccModule_P521_test_negative_construct():
    coord = dict(point_x=10, point_y=4) # type: Dict
    # type: Dict
    coordG = dict(point_x=_curves['p521'].Gx, point_y=_curves['p521'].Gy)

    asserts.assert_fails(lambda: ECC.construct(curve="P-521", **coord), ".*?ValueError")
    asserts.assert_fails(lambda: ECC.construct(curve="P-521", d=2, **coordG), ".*?ValueError")



def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P384_test_set))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P384_test_copy))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P384_test_negate))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P384_test_addition))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P384__test_inplace_addition))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P384_test_doubling))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P384_test_scalar_multiply))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P384_test_joing_scalar_multiply))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P384_test_sizes))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P521_test_set))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P521_test_copy))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccPoint_NIST_P521_test_negate))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccPoint_NIST_P521_test_addition))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccPoint_NIST_P521_test_inplace_addition))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccPoint_NIST_P521_test_doubling))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccPoint_NIST_P521_test_scalar_multiply))
    _suite.addTest(unittest.FunctionTestCase(
        TestEccPoint_NIST_P521_test_joing_scalar_multiply))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_NIST_P521_test_sizes))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P256_test_private_key))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P256_test_public_key))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccKey_P256_test_public_key_derived))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccKey_P256_test_invalid_curve))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P256_test_invalid_d))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P256_test_equality))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P384_test_private_key))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P384_test_public_key))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccKey_P384_test_public_key_derived))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccKey_P384_test_invalid_curve))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P384_test_invalid_d))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P384_test_equality))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P521_test_private_key))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P521_test_public_key))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccKey_P521_test_public_key_derived))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccKey_P521_test_invalid_curve))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P521_test_invalid_d))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_P521_test_equality))
    _suite.addTest(unittest.FunctionTestCase(TestEccModule_P256_test_generate))
    _suite.addTest(unittest.FunctionTestCase(TestEccModule_P256_test_construct))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccModule_P256_test_negative_construct))
    _suite.addTest(unittest.FunctionTestCase(TestEccModule_P384_test_generate))
    _suite.addTest(unittest.FunctionTestCase(TestEccModule_P384_test_construct))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccModule_P384_test_negative_construct))
    _suite.addTest(unittest.FunctionTestCase(TestEccModule_P521_test_generate))
    _suite.addTest(unittest.FunctionTestCase(TestEccModule_P521_test_construct))
    _suite.addTest(
        unittest.FunctionTestCase(TestEccModule_P521_test_negative_construct))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
