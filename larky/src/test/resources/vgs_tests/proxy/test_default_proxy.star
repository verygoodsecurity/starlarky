"""Unit tests for proxy.star"""
load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//http/request", "VGSHttpRequest")
load("@vgs//http/response", "VGSHttpResponse")
load("@vgs//proxy", "proxy")


def _test_short_circuit_response_wraps_response():
    response = VGSHttpResponse(body=b"blocked", headers={"Content-Type": "text/plain"}, status_code=403)
    short_circuit = proxy.ShortCircuitResponse(response)
    asserts.assert_that(short_circuit.__name__).is_equal_to("ShortCircuitResponse")
    asserts.assert_that(short_circuit.response.status_code).is_equal_to(403)
    asserts.assert_that(short_circuit.response.body).is_equal_to(b"blocked")


def _test_short_circuit_response_requires_response():
    asserts.assert_fails(lambda: proxy.ShortCircuitResponse(None), "ShortCircuitResponse requires a response")


def _test_short_circuit_response_rejects_request():
    request = VGSHttpRequest(url="https://example.com", data=b"x", headers={}, method="POST")
    asserts.assert_fails(
        lambda: proxy.ShortCircuitResponse(request),
        r"ShortCircuitResponse must wrap one of \[VGSHttpResponse\], got VGSHttpRequest",
    )


def _test_short_circuit_response_rejects_non_response():
    asserts.assert_fails(
        lambda: proxy.ShortCircuitResponse(b"blocked"),
        r"ShortCircuitResponse must wrap one of \[VGSHttpResponse\], got bytes",
    )
    asserts.assert_fails(
        lambda: proxy.ShortCircuitResponse({"status_code": 403}),
        r"ShortCircuitResponse must wrap one of \[VGSHttpResponse\], got dict",
    )


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_wraps_response))
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_requires_response))
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_rejects_request))
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_rejects_non_response))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
