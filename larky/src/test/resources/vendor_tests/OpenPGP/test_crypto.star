load("@vendor//OpenPGP/Crypto", Crypto="Crypto")
load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts",  "asserts")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io/StringIO", StringIO="StringIO")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")


def PGP_test():
    # k = ''.join(('9502 3904 4f77 1224 0104 e0cc 4734 72ab\n',
    #             'e3aa bca9 76d7 238c 5835 81f0 a35b 00c0\n',
    #             '720a 3654 6c53 0829 0a3e 86ea ac99 f790\n',
    #             'd0df 97c8 d147 9905 4b0c 29f6 e3cf 9259\n',
    #             '4aab 914a 931e be97 61b0 229b 74f6 f21a\n',
    #             '8d75 773f 2315 8c10 df34 f3fe a160 68bd\n',
    #             '2950 a985 7f56 73cc e335 ad55 d75c a2ec\n',
    #             '1776 0221 1d10 4a1c 266c f0a6 6da4 a149\n',
    #             'dd0a 03e7 0c56 4315 34f3 a0ae 806b 11d5\n',
    #             '631c f18f c7f9 6e54 107e 3c81 6eb9 45f8\n',
    #             'b6f9 834a abb6 f700 1101 0001 0004 d40c\n',
    #             'fd73 414d 228d fbcd 8cae a2c6 cd64 2af8\n',
    #             '87eb ee83 7a2e 04dc 572e 4f13 a6f8 981c\n',
    #             'e7d1 63ce d1b0 d73f e6fe e038 bb22 e5fb\n',
    #             '913e 2555 7f0b 85a0 6b22 5fd9 b875 f083\n',
    #             'c8b2 a884 7d5c 6060 1475 966b aa41 1b04\n',
    #             '87ba 4968 0ba5 c647 4035 9024 85e8 4e87\n',
    #             'bc6e f138 3ac4 3716 8f71 9805 7462 a791\n',
    #             '42fe 778a 9e8d 9f44 ae7d 71db cd8f de41\n',
    #             'c4a4 9eb0 8c0d b3eb 7070 6220 a2f4 0129\n',
    #             'ec5f 5ba7 0505 0f0d 16e9 0270 df58 9682\n',
    #             '7704 0979 c5af bb2c ebe8 37c8 85a4 cb4e\n',
    #             '60f1 e54c ab89 b075 d824 6beb babb 86bf\n',
    #             '6e80 88bc 5ffa 181f cf22 87cf 8d73 b884\n',
    #             '5fc9 68e5 fc5e 9a22 d404 7cdf b4ab 3252\n',
    #             '339b fe7b ff91 0b82 1c6b 0270 ea24 f12d\n',
    #             'aec9 7240 30ba 5a91 de3e 1463 24c1 32bb\n',
    #             'd95d 8373 e1a7 11dd 0ca3 67a3 cbd5 3f08\n',
    #             '209c 5f02 4630 087b 976a 67ec 991b 443d\n',
    #             '3a4e cf28 7adb 1105 5e0a 167d 27c1 9f4c\n',
    #             '552e 0c60 4a75 2b67 b2a5 026e 2414 2ad3\n',
    #             'd769 85ae fe25 8f89 dfe9 39de 1f05 2014\n',
    #             '09b5 83b9 2d70 0efc 70df d5d6 ceea 571c\n',
    #             '98fd d94c b824 dac1 431c a965 acfc 3950\n',
    #             '59e3 f596 1cb9 86a1 95c5 053a 0e1f d3a9\n',
    #             '971c 777d b6cf ee70 81bc c381 b424 5465\n',
    #             '7374 204b 6579 2028 5253 4129 203c 7465\n',
    #             '7374 6b65 7940 6578 616d 706c 652e 6f72\n',
    #             '673e 88da 0413 0102 0028 0502 4f77 1224\n',
    #             '021b 0305 0910 eacf 8006 0b09 0807 0302\n',
    #             '0615 0802 090a 0b04 1602 0301 021e 0102\n',
    #             '1780 000a 0910 d4d5 4ea1 6f87 040e 953d\n',
    #             '04de 38f3 0dad df5c df2d ea9a f839 ae1d\n',
    #             '3701 4564 84ac aa3b b37c dacc 996a f4fa\n',
    #             'b625 6039 a15e e9ff 8861 0afa f29a 891e\n',
    #             '4d1e 7e9f 9da1 8e86 5502 3370 b8a0 ead1\n',
    #             'b075 68ee 78e1 b6b1 d85b d60f 5788 ae8a\n',
    #             '8b6b b5e3 85c6 5366 f242 d629 ac43 331c\n',
    #             'dd0b 1cee 259a a708 430c 5d68 2c0f ee9f\n',
    #             '11ff 78f1 2d93 66c9 97f7 10be 490a 515d\n',
    #             '95b6 979b 2d16 6c69 7543 d961 a724 606d\n',
    #             'ca6c 35c5 6531 bcce a517 a1b1 ef6e 9d02\n',
    #             '3a04 4f77 1224 0104 e0ba 9e9a 6f23 93e6\n',
    #             '209a aa83 095c f058 f922 0bbe 6cb6 01ec\n',
    #             'ee39 079d 934e ebf1 cdf9 7660 e678 ee5d\n',
    #             '7082 42f4 41f3 84eb 09bb 68c9 ffb4 7a53\n',
    #             'b758 d75b 73ef d327 ade5 1553 1c9d 347e\n',
    #             '1123 1093 c1e8 7be7 dc1d 540d 5660 3b7e\n',
    #             '92b2 1f20 e427 e9ff b36f b85d fc33 ec55\n',
    #             '2e8d 2db5 5073 8ac0 daae 0fda fd3e 53f9\n',
    #             '7d7f dde0 2519 4e62 0878 2539 6dcb a3b7\n',
    #             '1c5e 08ff 6cc5 1b11 8b22 f93a 430e 83ff\n',
    #             'd02a 1f03 3d00 1101 0001 0004 df53 41aa\n',
    #             'f866 50f7 5c18 6475 ed59 6f48 e26c d3fc\n',
    #             '7297 88a8 6167 4404 82d1 b4e5 bdd4 cb81\n',
    #             '5391 2416 bfb8 fd91 e752 733c 40ad 4184\n',
    #             'ccf5 8195 93b8 a8eb 7dff c29e 7330 3ea1\n',
    #             '43a4 c6e4 6f54 cb72 8011 ed62 6a13 bdfb\n',
    #             'f872 72a9 61d0 2d50 f2b4 a479 8183 542e\n',
    #             '3454 7c20 2ccf b042 be54 81c2 ac0a ea1f\n',
    #             'de42 c68a b1b6 34bd eafb 3ecf f391 18ff\n',
    #             '301b 88f6 be39 5fe2 e2dc 8919 53d9 4a35\n',
    #             '2970 d860 caa4 bfac 2902 70d7 b2d2 08ca\n',
    #             '4c3a a613 960d c1ea c14b 3d8f 67ce ec05\n',
    #             'e513 f138 f149 7fb4 5438 5ec0 6ad4 96af\n',
    #             '036b 483d 2923 139e d3a4 e9be 26d0 acb6\n',
    #             '6fd7 0466 2bc7 cb43 59fc f259 18cd 838b\n',
    #             '0492 6bf3 fd2f 0a20 0302 70dd 7ce4 7757\n',
    #             '3a00 8090 31cf 8997 d994 841b c489 94a9\n',
    #             '68b8 e78b 52e4 6871 ce71 fb61 2c03 8451\n',
    #             'd98b 8e9a 7a48 ce36 2eed eca8 9fad e8f7\n',
    #             'bfad b109 0b7b c34a d1fa 1e9e 19e0 c7c6\n',
    #             '38e1 8a15 7fc0 bb0b bf02 70c2 8b58 984f\n',
    #             '5383 5698 63ce 403b 4a5b 798b de62 f0fd\n',
    #             '0bcb 152d 7838 331f 12eb ca1e 2337 ac13\n',
    #             'ec2c a0b4 c0e2 24c6 ab37 93d3 e82e 2867\n',
    #             'd171 3fd9 36f8 db2d 7d76 990b 821e d5b3\n',
    #             '5f0e a552 4f0c 57c1 85cc 7c88 c104 1801\n',
    #             '0200 0f05 024f 7712 2402 1b0c 0509 10ea\n',
    #             'cf80 000a 0910 d4d5 4ea1 6f87 040e 3908\n',
    #             '04dd 12b6 5209 b79a 5c52 d45e 12a4 016d\n',
    #             '94f8 f9f8 9ca3 4eca 96c8 0e6c 7907 73cb\n',
    #             '01ca cfd4 c951 2578 fbf0 bbd4 3d0e d3bf\n',
    #             '6247 226c a589 66d5 0e21 25a1 462a 95a0\n',
    #             '7794 fe83 b0d5 cd8e 67bc b8c4 4c23 21f7\n',
    #             '5ecc ffe0 5f14 aaa5 dc71 076c 3571 57ab\n',
    #             'cdfa 652f 3cab 0ba0 80b4 306b 2973 dfe1\n',
    #             '144c 33a2 34a2 caba f36d d3cc 4579 8a24\n',
    #             'f09e 81c3 a9d1 4823 5450 2e9e 0e66 5c1b\n',
    #             'de77 3d90 8fc8 08f3 6aba b834 616b'))
        
    # wkey = OpenPGP.Message.parse(open('key', 'rb').read())[0]
    wkey = OpenPGP.Message().parse(b'\x95\x029\x04Ow\x12$\x01\x04\xe0\xccG4r\xab\xe3\xaa\xbc\xa9v\xd7#\x8cX5\x81\xf0\xa3[\x00\xc0r\n6TlS\x08)\n>\x86\xea\xac\x99\xf7\x90\xd0\xdf\x97\xc8\xd1G\x99\x05K\x0c)\xf6\xe3\xcf\x92YJ\xab\x91J\x93\x1e\xbe\x97a\xb0"\x9bt\xf6\xf2\x1a\x8duw?#\x15\x8c\x10\xdf4\xf3\xfe\xa1`h\xbd)P\xa9\x85\x7fVs\xcc\xe35\xadU\xd7\\\xa2\xec\x17v\x02!\x1d\x10J\x1c&l\xf0\xa6m\xa4\xa1I\xdd\n\x03\xe7\x0cVC\x154\xf3\xa0\xae\x80k\x11\xd5c\x1c\xf1\x8f\xc7\xf9nT\x10~<\x81n\xb9E\xf8\xb6\xf9\x83J\xab\xb6\xf7\x00\x11\x01\x00\x01\x00\x04\xd4\x0c\xfdsAM"\x8d\xfb\xcd\x8c\xae\xa2\xc6\xcdd*\xf8\x87\xeb\xee\x83z.\x04\xdcW.O\x13\xa6\xf8\x98\x1c\xe7\xd1c\xce\xd1\xb0\xd7?\xe6\xfe\xe08\xbb"\xe5\xfb\x91>%U\x7f\x0b\x85\xa0k"_\xd9\xb8u\xf0\x83\xc8\xb2\xa8\x84}\\``\x14u\x96k\xaaA\x1b\x04\x87\xbaIh\x0b\xa5\xc6G@5\x90$\x85\xe8N\x87\xbcn\xf18:\xc47\x16\x8fq\x98\x05tb\xa7\x91B\xfew\x8a\x9e\x8d\x9fD\xae}q\xdb\xcd\x8f\xdeA\xc4\xa4\x9e\xb0\x8c\r\xb3\xebppb \xa2\xf4\x01)\xec_[\xa7\x05\x05\x0f\r\x16\xe9\x02p\xdfX\x96\x82w\x04\ty\xc5\xaf\xbb,\xeb\xe87\xc8\x85\xa4\xcbN`\xf1\xe5L\xab\x89\xb0u\xd8$k\xeb\xba\xbb\x86\xbfn\x80\x88\xbc_\xfa\x18\x1f\xcf"\x87\xcf\x8ds\xb8\x84_\xc9h\xe5\xfc^\x9a"\xd4\x04|\xdf\xb4\xab2R3\x9b\xfe{\xff\x91\x0b\x82\x1ck\x02p\xea$\xf1-\xae\xc9r@0\xbaZ\x91\xde>\x14c$\xc12\xbb\xd9]\x83s\xe1\xa7\x11\xdd\x0c\xa3g\xa3\xcb\xd5?\x08 \x9c_\x02F0\x08{\x97jg\xec\x99\x1bD=:N\xcf(z\xdb\x11\x05^\n\x16}\'\xc1\x9fLU.\x0c`Ju+g\xb2\xa5\x02n$\x14*\xd3\xd7i\x85\xae\xfe%\x8f\x89\xdf\xe99\xde\x1f\x05 \x14\t\xb5\x83\xb9-p\x0e\xfcp\xdf\xd5\xd6\xce\xeaW\x1c\x98\xfd\xd9L\xb8$\xda\xc1C\x1c\xa9e\xac\xfc9PY\xe3\xf5\x96\x1c\xb9\x86\xa1\x95\xc5\x05:\x0e\x1f\xd3\xa9\x97\x1cw}\xb6\xcf\xeep\x81\xbc\xc3\x81\xb4$Test Key (RSA) <testkey@example.org>\x88\xda\x04\x13\x01\x02\x00(\x05\x02Ow\x12$\x02\x1b\x03\x05\t\x10\xea\xcf\x80\x06\x0b\t\x08\x07\x03\x02\x06\x15\x08\x02\t\n\x0b\x04\x16\x02\x03\x01\x02\x1e\x01\x02\x17\x80\x00\n\t\x10\xd4\xd5N\xa1o\x87\x04\x0e\x95=\x04\xde8\xf3\r\xad\xdf\\\xdf-\xea\x9a\xf89\xae\x1d7\x01Ed\x84\xac\xaa;\xb3|\xda\xcc\x99j\xf4\xfa\xb6%`9\xa1^\xe9\xff\x88a\n\xfa\xf2\x9a\x89\x1eM\x1e~\x9f\x9d\xa1\x8e\x86U\x023p\xb8\xa0\xea\xd1\xb0uh\xeex\xe1\xb6\xb1\xd8[\xd6\x0fW\x88\xae\x8a\x8bk\xb5\xe3\x85\xc6Sf\xf2B\xd6)\xacC3\x1c\xdd\x0b\x1c\xee%\x9a\xa7\x08C\x0c]h,\x0f\xee\x9f\x11\xffx\xf1-\x93f\xc9\x97\xf7\x10\xbeI\nQ]\x95\xb6\x97\x9b-\x16liuC\xd9a\xa7$`m\xcal5\xc5e1\xbc\xce\xa5\x17\xa1\xb1\xefn\x9d\x02:\x04Ow\x12$\x01\x04\xe0\xba\x9e\x9ao#\x93\xe6 \x9a\xaa\x83\t\\\xf0X\xf9"\x0b\xbel\xb6\x01\xec\xee9\x07\x9d\x93N\xeb\xf1\xcd\xf9v`\xe6x\xee]p\x82B\xf4A\xf3\x84\xeb\t\xbbh\xc9\xff\xb4zS\xb7X\xd7[s\xef\xd3\'\xad\xe5\x15S\x1c\x9d4~\x11#\x10\x93\xc1\xe8{\xe7\xdc\x1dT\rV`;~\x92\xb2\x1f \xe4\'\xe9\xff\xb3o\xb8]\xfc3\xecU.\x8d-\xb5Ps\x8a\xc0\xda\xae\x0f\xda\xfd>S\xf9}\x7f\xdd\xe0%\x19Nb\x08x%9m\xcb\xa3\xb7\x1c^\x08\xffl\xc5\x1b\x11\x8b"\xf9:C\x0e\x83\xff\xd0*\x1f\x03=\x00\x11\x01\x00\x01\x00\x04\xdfSA\xaa\xf8fP\xf7\\\x18du\xedYoH\xe2l\xd3\xfcr\x97\x88\xa8agD\x04\x82\xd1\xb4\xe5\xbd\xd4\xcb\x81S\x91$\x16\xbf\xb8\xfd\x91\xe7Rs<@\xadA\x84\xcc\xf5\x81\x95\x93\xb8\xa8\xeb}\xff\xc2\x9es0>\xa1C\xa4\xc6\xe4oT\xcbr\x80\x11\xedbj\x13\xbd\xfb\xf8rr\xa9a\xd0-P\xf2\xb4\xa4y\x81\x83T.4T| ,\xcf\xb0B\xbeT\x81\xc2\xac\n\xea\x1f\xdeB\xc6\x8a\xb1\xb64\xbd\xea\xfb>\xcf\xf3\x91\x18\xff0\x1b\x88\xf6\xbe9_\xe2\xe2\xdc\x89\x19S\xd9J5)p\xd8`\xca\xa4\xbf\xac)\x02p\xd7\xb2\xd2\x08\xcaL:\xa6\x13\x96\r\xc1\xea\xc1K=\x8fg\xce\xec\x05\xe5\x13\xf18\xf1I\x7f\xb4T8^\xc0j\xd4\x96\xaf\x03kH=)#\x13\x9e\xd3\xa4\xe9\xbe&\xd0\xac\xb6o\xd7\x04f+\xc7\xcbCY\xfc\xf2Y\x18\xcd\x83\x8b\x04\x92k\xf3\xfd/\n \x03\x02p\xdd|\xe4wW:\x00\x80\x901\xcf\x89\x97\xd9\x94\x84\x1b\xc4\x89\x94\xa9h\xb8\xe7\x8bR\xe4hq\xceq\xfba,\x03\x84Q\xd9\x8b\x8e\x9azH\xce6.\xed\xec\xa8\x9f\xad\xe8\xf7\xbf\xad\xb1\t\x0b{\xc3J\xd1\xfa\x1e\x9e\x19\xe0\xc7\xc68\xe1\x8a\x15\x7f\xc0\xbb\x0b\xbf\x02p\xc2\x8bX\x98OS\x83V\x98c\xce@;J[y\x8b\xdeb\xf0\xfd\x0b\xcb\x15-x83\x1f\x12\xeb\xca\x1e#7\xac\x13\xec,\xa0\xb4\xc0\xe2$\xc6\xab7\x93\xd3\xe8.(g\xd1q?\xd96\xf8\xdb-}v\x99\x0b\x82\x1e\xd5\xb3_\x0e\xa5RO\x0cW\xc1\x85\xcc|\x88\xc1\x04\x18\x01\x02\x00\x0f\x05\x02Ow\x12$\x02\x1b\x0c\x05\t\x10\xea\xcf\x80\x00\n\t\x10\xd4\xd5N\xa1o\x87\x04\x0e9\x08\x04\xdd\x12\xb6R\t\xb7\x9a\\R\xd4^\x12\xa4\x01m\x94\xf8\xf9\xf8\x9c\xa3N\xca\x96\xc8\x0ely\x07s\xcb\x01\xca\xcf\xd4\xc9Q%x\xfb\xf0\xbb\xd4=\x0e\xd3\xbfbG"l\xa5\x89f\xd5\x0e!%\xa1F*\x95\xa0w\x94\xfe\x83\xb0\xd5\xcd\x8eg\xbc\xb8\xc4L#!\xf7^\xcc\xff\xe0_\x14\xaa\xa5\xdcq\x07l5qW\xab\xcd\xfae/<\xab\x0b\xa0\x80\xb40k)s\xdf\xe1\x14L3\xa24\xa2\xca\xba\xf3m\xd3\xccEy\x8a$\xf0\x9e\x81\xc3\xa9\xd1H#TP.\x9e\x0ef\\\x1b\xdew=\x90\x8f\xc8\x08\xf3j\xba\xb84ak').__getitem__(0)
    # wkey should be of <class 'OpenPGP.SecretKeyPacket'>
    print('parsed key:', wkey)

    # data = OpenPGP.LiteralDataPacket('This is text.', 'u', 'stuff.txt', 1000000)
    # encrypt = Crypto.Wrapper(data)
    # encrypted = encrypt.encrypt([wkey])

    # print('pgp encrypted:', encrypted)

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