# Ported from pycryptodome <= 3.12.0 (no discriminating lines), pinned to 3.12.0 (d8edf1a): lib/Crypto/SelfTest/Hash/common.py. See larky/src/main/resources/VENDORED.md.
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes")
load("@stdlib//builtins", builtins="builtins")
load("@vendor//asserts", asserts="asserts")

map = builtins.map
eq = asserts.eq


def make_hash_tests(hashmod, module_name, test_data, digest_size, oid=None):

    for i in range(len(test_data)):
        row = test_data[i]
        (expected, input) = map(tobytes, row[0:2])
        h = hashmod.new()
        h.update(input)
        eq(tobytes(h.hexdigest()), expected)
