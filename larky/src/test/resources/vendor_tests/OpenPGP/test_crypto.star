load("@vendor//OpenPGP/Crypto", Crypto="Crypto")
load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts",  "asserts")


def PGP_test():

    k = '''-----BEGIN PGP PUBLIC KEY BLOCK-----
    mQENBGFFE1YBCACtpBRTbhH8E0tcwQLvV9seNHS7aE65+e+icuaBoClyjMJ393As
    sHVaii/KpZLlRlcraHnOtsF7cGahn+De/cadkkHUotYQP/08tH9PGFWsub1lDGqV
    vQs//Kwdu6dX6rNxV+538sulL8AEZyUma7aggfFrCvpRL45WBH3W9i/mQyHRSkCS
    Sh76/HazPgCMnh3YgkoOEubjTD7zEiWK86/aR9KTfnZfjfjkeQLnHOKq8j3W9KuT
    5/0qjo1mzT/VV8EqNJRF0+qMFKumB5LCAvS9JoGaGNRiZNlSF64LQ/q4annMBLOr
    IHB/m6Jkmz0GeA4c5rlMFUofAfXmd4clKhJrABEBAAG0C3Rlc3RAdmdzLmlviQEc
    BBABAgAGBQJhRRNWAAoJEDbLv90jVDPuvPIH/0q9tnc9TsnA27T89gpApRQX5ncf
    GFFRiOUPYrieoEgso/pJGhHF97KkEA0OSJhM4ItZT2GqbUOvFGSXpUYX+qlud8g6
    uj7kCRriF7VFzap6YcHM+uINDDH5njI2GrxS3WN01J1hj6zJmJqo+e6W2W0gLN9j
    swVaTNi6kqZQrXJUluOx+KVlSKeQ4uMHNsPKWy0z6EioIaUbakGvCYyGCd81nSBg
    Mvq8iNi9XyY1jN19U4v6Ql/kJthsDxlfB5tG2HBr/1xWS5SR7uZMs3GkiNQGrfFa
    x+azHUi7YnXbASHofGrrSqkbLTQkz+NTjK9oIWpLafxseYFUI/f4UdySq24=
    =ACke
    -----END PGP PUBLIC KEY BLOCK-----'''

    # wkey = OpenPGP.Message.parse(open('key', 'rb').read())[0]
    # wkey = OpenPGP.Message().parse(k).__getitem__(0)
    # print('parse key:', OpenPGP.Message().parse(k))

    data = OpenPGP.LiteralDataPacket('This is text.', 'u', 'stuff.txt', 1000)
    encrypt = Crypto.Wrapper(data)
    encrypted = encrypt.encrypt([k])

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