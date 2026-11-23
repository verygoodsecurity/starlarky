"""Unit tests for proxy.star"""
load("@vendor//asserts", "asserts")
load("@stdlib//builtins", "builtins")
load("@stdlib//unittest", "unittest")
load("@vgs//http/response", "VGSHttpResponse")
load("@vgs//proxy", "proxy")


def _test_short_circuit_response_exposes_response_fields():
    short_circuit = proxy.ShortCircuitResponse(VGSHttpResponse(body=b"blocked", status_code=403))
    asserts.assert_that(short_circuit.__name__).is_equal_to("ShortCircuitResponse")
    asserts.assert_that(short_circuit.status_code).is_equal_to(403)
    asserts.assert_that(short_circuit.body).is_equal_to(b"blocked")
    asserts.assert_that(hasattr(short_circuit, "response")).is_equal_to(False)


def _test_short_circuit_response_is_a_vgs_http_response():
    response = proxy.ShortCircuitResponse(VGSHttpResponse())
    asserts.assert_that(builtins.isinstance(response, VGSHttpResponse)).is_equal_to(True)
    asserts.assert_that(response.status_code).is_equal_to(200)
    asserts.assert_that(response.body).is_equal_to(None)


def _test_short_circuit_response_preserves_ordered_headers():
    response = proxy.ShortCircuitResponse(VGSHttpResponse(headers=[("Set-Cookie", "a=1"), ("X-Marker", "middle")]))
    response.append_header("set-cookie", "b=2")
    asserts.assert_that(response.headers.items()).is_equal_to(
        [("Set-Cookie", "a=1"), ("X-Marker", "middle"), ("set-cookie", "b=2")],
    )
    asserts.assert_that(response.get_all_headers("SET-COOKIE")).is_equal_to(["a=1", "b=2"])


def _test_short_circuit_response_clears_length_when_body_changes():
    response = proxy.ShortCircuitResponse(VGSHttpResponse(body=b"old", headers=[("Content-Length", "3"), ("X-Trace", "one")]))
    response.body = b"updated"
    asserts.assert_that(response.headers.items()).is_equal_to([("X-Trace", "one")])


def _test_short_circuit_response_accepts_a_vgs_http_response():
    # the form of Larky 1.3 and earlier
    wrapped = VGSHttpResponse(
        body=b"blocked",
        headers=[("Set-Cookie", "a=1"), ("X-Marker", "middle"), ("set-cookie", "b=2")],
        status_code=403,
    )
    response = proxy.ShortCircuitResponse(wrapped)
    asserts.assert_that(response.__name__).is_equal_to("ShortCircuitResponse")
    asserts.assert_that(builtins.isinstance(response, VGSHttpResponse)).is_equal_to(True)
    asserts.assert_that(response.status_code).is_equal_to(403)
    asserts.assert_that(response.body).is_equal_to(b"blocked")
    asserts.assert_that(response.headers.items()).is_equal_to(
        [("Set-Cookie", "a=1"), ("X-Marker", "middle"), ("set-cookie", "b=2")],
    )


def _test_short_circuit_response_decorates_the_response():
    wrapped = VGSHttpResponse(body=b"blocked", headers=[("X-Trace", "one")], status_code=403)
    response = proxy.ShortCircuitResponse(wrapped)
    # one response: a change through either is seen through both
    wrapped.append_header("X-Trace", "two")
    response.status_code = 401
    response.body = b"denied"
    asserts.assert_that(response.headers.items()).is_equal_to([("X-Trace", "one"), ("X-Trace", "two")])
    asserts.assert_that(wrapped.status_code).is_equal_to(401)
    asserts.assert_that(wrapped.body).is_equal_to(b"denied")
    asserts.assert_that(wrapped.__name__).is_equal_to("VGSHttpResponse")
    asserts.assert_that(response.__name__).is_equal_to("ShortCircuitResponse")
    # wrapping a short-circuit response again returns it
    asserts.assert_that(proxy.ShortCircuitResponse(response) == response).is_equal_to(True)


def _test_a_response_is_not_a_body():
    wrapped = VGSHttpResponse(body=b"blocked", status_code=403)
    asserts.assert_fails(lambda: VGSHttpResponse(body=wrapped), ".*body must not be a VGSHttpResponse.*")


def _test_short_circuit_response_refuses_what_is_not_a_protocol_response():
    asserts.assert_fails(lambda: proxy.ShortCircuitResponse(b"blocked"),
                         ".*takes a protocol response, one of \\[VGSHttpResponse\\], got bytes")
    asserts.assert_fails(lambda: proxy.ShortCircuitResponse({"status_code": 403}),
                         ".*takes a protocol response.*got dict")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_exposes_response_fields))
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_is_a_vgs_http_response))
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_preserves_ordered_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_clears_length_when_body_changes))
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_accepts_a_vgs_http_response))
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_decorates_the_response))
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_refuses_what_is_not_a_protocol_response))
    _suite.addTest(unittest.FunctionTestCase(_test_a_response_is_not_a_body))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
