"""Unit tests for proxy.star"""
load("@vendor//asserts", "asserts")
load("@stdlib//builtins", "builtins")
load("@stdlib//unittest", "unittest")
load("@vgs//http/response", "VGSHttpResponse")
load("@vgs//proxy", "proxy")


def _test_short_circuit_response_holds_the_response():
    response = VGSHttpResponse(body=b"blocked", status_code=403)
    short_circuit = proxy.ShortCircuitResponse(response)
    asserts.assert_that(short_circuit.__name__).is_equal_to("ShortCircuitResponse")
    asserts.assert_that(short_circuit.response == response).is_equal_to(True)
    asserts.assert_that(short_circuit.response.status_code).is_equal_to(403)
    asserts.assert_that(short_circuit.response.body).is_equal_to(b"blocked")
    asserts.assert_that(builtins.isinstance(short_circuit, VGSHttpResponse)).is_equal_to(False)
    asserts.assert_that(builtins.isinstance(short_circuit.response, VGSHttpResponse)).is_equal_to(True)


def _test_the_held_response_keeps_ordered_headers():
    short_circuit = proxy.ShortCircuitResponse(VGSHttpResponse(headers=[("Set-Cookie", "a=1"), ("X-Marker", "middle")]))
    short_circuit.response.append_header("set-cookie", "b=2")
    asserts.assert_that(short_circuit.response.headers.items()).is_equal_to(
        [("Set-Cookie", "a=1"), ("X-Marker", "middle"), ("set-cookie", "b=2")],
    )
    asserts.assert_that(short_circuit.response.get_all_headers("SET-COOKIE")).is_equal_to(["a=1", "b=2"])


def _test_changes_to_the_response_reach_the_short_circuit():
    wrapped = VGSHttpResponse(body=b"old", headers=[("Content-Length", "3"), ("X-Trace", "one")], status_code=403)
    short_circuit = proxy.ShortCircuitResponse(wrapped)
    wrapped.status_code = 401
    short_circuit.response.body = b"updated"
    asserts.assert_that(short_circuit.response.status_code).is_equal_to(401)
    asserts.assert_that(wrapped.body).is_equal_to(b"updated")
    # the stored length applied to the previous body
    asserts.assert_that(wrapped.headers.items()).is_equal_to([("X-Trace", "one")])


def _test_a_short_circuit_cannot_be_changed():
    short_circuit = proxy.ShortCircuitResponse(VGSHttpResponse(status_code=403))

    def change():
        short_circuit.status_code = 401

    asserts.assert_fails(change, ".*ShortCircuitResponse value does not support field assignment.*")


def _test_wrapping_a_short_circuit_again_returns_it():
    short_circuit = proxy.ShortCircuitResponse(VGSHttpResponse())
    asserts.assert_that(proxy.ShortCircuitResponse(short_circuit) == short_circuit).is_equal_to(True)


def _test_a_body_can_be_any_value():
    # as in Larky 1.3: the host decides what it can send
    inner = VGSHttpResponse(status_code=403)
    response = VGSHttpResponse(body=inner)
    asserts.assert_that(response.body == inner).is_equal_to(True)


def _test_reprs_show_the_status():
    response = VGSHttpResponse(status_code=403)
    asserts.assert_that(str(response)).is_equal_to("<VGSHttpResponse [403]>")
    asserts.assert_that(repr(proxy.ShortCircuitResponse(response))).is_equal_to(
        "ShortCircuitResponse(<VGSHttpResponse [403]>)")


def _test_short_circuit_response_refuses_what_is_not_a_protocol_response():
    asserts.assert_fails(lambda: proxy.ShortCircuitResponse(b"blocked"),
                         ".*ShortCircuitResponse.response=.... expected `VGSHttpResponse`; got bytes")
    asserts.assert_fails(lambda: proxy.ShortCircuitResponse({"status_code": 403}),
                         ".*expected `VGSHttpResponse`; got dict")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_holds_the_response))
    _suite.addTest(unittest.FunctionTestCase(_test_the_held_response_keeps_ordered_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_changes_to_the_response_reach_the_short_circuit))
    _suite.addTest(unittest.FunctionTestCase(_test_a_short_circuit_cannot_be_changed))
    _suite.addTest(unittest.FunctionTestCase(_test_wrapping_a_short_circuit_again_returns_it))
    _suite.addTest(unittest.FunctionTestCase(_test_a_body_can_be_any_value))
    _suite.addTest(unittest.FunctionTestCase(_test_reprs_show_the_status))
    _suite.addTest(unittest.FunctionTestCase(_test_short_circuit_response_refuses_what_is_not_a_protocol_response))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
