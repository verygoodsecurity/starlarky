load("@vendor//OpenPGP/Crypto", Crypto="Crypto")
load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts",  "asserts")
load("@stdlib//codecs", codecs="codecs")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")


def PGP_test():

    k = '''
        9502 3904 4f77 1224 0104 e0cc 4734 72ab
        e3aa bca9 76d7 238c 5835 81f0 a35b 00c0
        720a 3654 6c53 0829 0a3e 86ea ac99 f790
        d0df 97c8 d147 9905 4b0c 29f6 e3cf 9259
        4aab 914a 931e be97 61b0 229b 74f6 f21a
        8d75 773f 2315 8c10 df34 f3fe a160 68bd
        2950 a985 7f56 73cc e335 ad55 d75c a2ec
        1776 0221 1d10 4a1c 266c f0a6 6da4 a149
        dd0a 03e7 0c56 4315 34f3 a0ae 806b 11d5
        631c f18f c7f9 6e54 107e 3c81 6eb9 45f8
        b6f9 834a abb6 f700 1101 0001 0004 d40c
        fd73 414d 228d fbcd 8cae a2c6 cd64 2af8
        87eb ee83 7a2e 04dc 572e 4f13 a6f8 981c
        e7d1 63ce d1b0 d73f e6fe e038 bb22 e5fb
        913e 2555 7f0b 85a0 6b22 5fd9 b875 f083
        c8b2 a884 7d5c 6060 1475 966b aa41 1b04
        87ba 4968 0ba5 c647 4035 9024 85e8 4e87
        bc6e f138 3ac4 3716 8f71 9805 7462 a791
        42fe 778a 9e8d 9f44 ae7d 71db cd8f de41
        c4a4 9eb0 8c0d b3eb 7070 6220 a2f4 0129
        ec5f 5ba7 0505 0f0d 16e9 0270 df58 9682
        7704 0979 c5af bb2c ebe8 37c8 85a4 cb4e
        60f1 e54c ab89 b075 d824 6beb babb 86bf
        6e80 88bc 5ffa 181f cf22 87cf 8d73 b884
        5fc9 68e5 fc5e 9a22 d404 7cdf b4ab 3252
        339b fe7b ff91 0b82 1c6b 0270 ea24 f12d
        aec9 7240 30ba 5a91 de3e 1463 24c1 32bb
        d95d 8373 e1a7 11dd 0ca3 67a3 cbd5 3f08
        209c 5f02 4630 087b 976a 67ec 991b 443d
        3a4e cf28 7adb 1105 5e0a 167d 27c1 9f4c
        552e 0c60 4a75 2b67 b2a5 026e 2414 2ad3
        d769 85ae fe25 8f89 dfe9 39de 1f05 2014
        09b5 83b9 2d70 0efc 70df d5d6 ceea 571c
        98fd d94c b824 dac1 431c a965 acfc 3950
        59e3 f596 1cb9 86a1 95c5 053a 0e1f d3a9
        971c 777d b6cf ee70 81bc c381 b424 5465
        7374 204b 6579 2028 5253 4129 203c 7465
        7374 6b65 7940 6578 616d 706c 652e 6f72
        673e 88da 0413 0102 0028 0502 4f77 1224
        021b 0305 0910 eacf 8006 0b09 0807 0302
        0615 0802 090a 0b04 1602 0301 021e 0102
        1780 000a 0910 d4d5 4ea1 6f87 040e 953d
        04de 38f3 0dad df5c df2d ea9a f839 ae1d
        3701 4564 84ac aa3b b37c dacc 996a f4fa
        b625 6039 a15e e9ff 8861 0afa f29a 891e
        4d1e 7e9f 9da1 8e86 5502 3370 b8a0 ead1
        b075 68ee 78e1 b6b1 d85b d60f 5788 ae8a
        8b6b b5e3 85c6 5366 f242 d629 ac43 331c
        dd0b 1cee 259a a708 430c 5d68 2c0f ee9f
        11ff 78f1 2d93 66c9 97f7 10be 490a 515d
        95b6 979b 2d16 6c69 7543 d961 a724 606d
        ca6c 35c5 6531 bcce a517 a1b1 ef6e 9d02
        3a04 4f77 1224 0104 e0ba 9e9a 6f23 93e6
        209a aa83 095c f058 f922 0bbe 6cb6 01ec
        ee39 079d 934e ebf1 cdf9 7660 e678 ee5d
        7082 42f4 41f3 84eb 09bb 68c9 ffb4 7a53
        b758 d75b 73ef d327 ade5 1553 1c9d 347e
        1123 1093 c1e8 7be7 dc1d 540d 5660 3b7e
        92b2 1f20 e427 e9ff b36f b85d fc33 ec55
        2e8d 2db5 5073 8ac0 daae 0fda fd3e 53f9
        7d7f dde0 2519 4e62 0878 2539 6dcb a3b7
        1c5e 08ff 6cc5 1b11 8b22 f93a 430e 83ff
        d02a 1f03 3d00 1101 0001 0004 df53 41aa
        f866 50f7 5c18 6475 ed59 6f48 e26c d3fc
        7297 88a8 6167 4404 82d1 b4e5 bdd4 cb81
        5391 2416 bfb8 fd91 e752 733c 40ad 4184
        ccf5 8195 93b8 a8eb 7dff c29e 7330 3ea1
        43a4 c6e4 6f54 cb72 8011 ed62 6a13 bdfb
        f872 72a9 61d0 2d50 f2b4 a479 8183 542e
        3454 7c20 2ccf b042 be54 81c2 ac0a ea1f
        de42 c68a b1b6 34bd eafb 3ecf f391 18ff
        301b 88f6 be39 5fe2 e2dc 8919 53d9 4a35
        2970 d860 caa4 bfac 2902 70d7 b2d2 08ca
        4c3a a613 960d c1ea c14b 3d8f 67ce ec05
        e513 f138 f149 7fb4 5438 5ec0 6ad4 96af
        036b 483d 2923 139e d3a4 e9be 26d0 acb6
        6fd7 0466 2bc7 cb43 59fc f259 18cd 838b
        0492 6bf3 fd2f 0a20 0302 70dd 7ce4 7757
        3a00 8090 31cf 8997 d994 841b c489 94a9
        68b8 e78b 52e4 6871 ce71 fb61 2c03 8451
        d98b 8e9a 7a48 ce36 2eed eca8 9fad e8f7
        bfad b109 0b7b c34a d1fa 1e9e 19e0 c7c6
        38e1 8a15 7fc0 bb0b bf02 70c2 8b58 984f
        5383 5698 63ce 403b 4a5b 798b de62 f0fd
        0bcb 152d 7838 331f 12eb ca1e 2337 ac13
        ec2c a0b4 c0e2 24c6 ab37 93d3 e82e 2867
        d171 3fd9 36f8 db2d 7d76 990b 821e d5b3
        5f0e a552 4f0c 57c1 85cc 7c88 c104 1801
        0200 0f05 024f 7712 2402 1b0c 0509 10ea
        cf80 000a 0910 d4d5 4ea1 6f87 040e 3908
        04dd 12b6 5209 b79a 5c52 d45e 12a4 016d
        94f8 f9f8 9ca3 4eca 96c8 0e6c 7907 73cb
        01ca cfd4 c951 2578 fbf0 bbd4 3d0e d3bf
        6247 226c a589 66d5 0e21 25a1 462a 95a0
        7794 fe83 b0d5 cd8e 67bc b8c4 4c23 21f7
        5ecc ffe0 5f14 aaa5 dc71 076c 3571 57ab
        cdfa 652f 3cab 0ba0 80b4 306b 2973 dfe1
        144c 33a2 34a2 caba f36d d3cc 4579 8a24
        f09e 81c3 a9d1 4823 5450 2e9e 0e66 5c1b
        de77 3d90 8fc8 08f3 6aba b834 616b
        '''

    # wkey = OpenPGP.Message.parse(open('key', 'rb').read())[0]
    wkey = OpenPGP.Message().parse(codecs.encode(k, encoding="utf-8")).__getitem__(0)
    # wkey should be of <class 'OpenPGP.SecretKeyPacket'>
    print('parsed key:', wkey)

    data = OpenPGP.LiteralDataPacket('This is text.', 'u', 'stuff.txt', 1000000)
    encrypt = Crypto.Wrapper(data)
    encrypted = encrypt.encrypt([wkey])

    print('pgp encrypted:', encrypted)

#     # Now decrypt it with the same key
#     decryptor = OpenPGP.Crypto.Wrapper(wkey)
#     decrypted = decryptor.decrypt(encrypted)

#     print(list(decrypted))

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(PGP_test))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())