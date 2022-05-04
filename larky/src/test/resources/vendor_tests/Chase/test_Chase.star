load("@stdlib//unittest","unittest")
load("@stdlib//json", "json")
load("@stdlib//base64", "base64")
load("@vendor//asserts","asserts")
load("@vendor//Chase/jwk", get_public_keys="get_keys", decrypt="decrypt")


def test_get_keys():
    get_public_keys()

def test_decryption():
    jwe_string = "eyJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAtMjU2Iiwia2lkIjoid3d3LnZlcnlnb29kc2VjdXJpdHkuY29tIn0.qbtIKSrbt1Gqg5t_pLcQ7249gyIn-YVMk9yfraq2fjPBqFpegiPOP7hXK0Wy2heDtpRSpK-7P94LBf4bR8aBtQZdurpen_TrPaIE2B9nq64NMmoiIN8iJc9pSH__96SoBRx7wueKXS6zvHMZ6j3-WLRr4fBpFMsaKZ7mPgR4x4Cpx8TAG9EBnalXqUnqpHnCAQInsfbbQMWggas36k_PW4PyMZHUVloamcrVYo9M6Ys9VuC_xFR8SJv9yE2ny9q_BYlssVm5Tg0jgzIJGSpE6YLby09KSFuu1VwUFBum4NkVsWpXK1cm4tquaWyKkH12mGA0VYY5qaso61cnDRfQ1g.oF0XygQZExMbH44H.0G__7Qld2Mz-pmgv5EKcle8F_O5u3j_aIOloOgQcW149xL881jDoItQHhLkhUnE5Gmo4MJNCFwRlJ92_8ulKeW1tZQCWb8FUmuxhvLdxQFs.mxLskSP-ei12XGDtD7IcZg"
    s = bytes(jwe_string, 'utf-8')
    decrypted = decrypt(s)
    print(decrypted)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_decryption))
    _suite.addTest(unittest.FunctionTestCase(test_get_keys))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
