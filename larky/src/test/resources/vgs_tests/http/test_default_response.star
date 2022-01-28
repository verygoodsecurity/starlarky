"""Unit tests for response.star"""
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vgs//http/response", "VGSHttpResponse")


def _create_simple_response():
    body = builtins.bytes('response body')
    headers = {
        'header1': 'key1',
        'header2': 'key2',
    }
    return VGSHttpResponse(body=body, headers=headers)


def _test_response_get_body():
    response = _create_simple_response()
    asserts.assert_that(str(response.body)).is_equal_to('response body')


def _test_response_set_body():
    response = _create_simple_response()
    new_body_str = 'new response body'
    response.body = builtins.bytes(new_body_str)
    asserts.assert_that(str(response.body)).is_equal_to(new_body_str)


def _test_response_headers_setter():
    response = _create_simple_response()

    new_headers = {
        'header3': 'key3',
        'header4': 'key4',
    }
    response.headers = new_headers

    headers = {
        'header3': 'key3',
        'header4': 'key4',
    }
    asserts.assert_that(response.headers.items()).is_equal_to(headers.items())


def _test_response_headers_property_add_header():
    response = _create_simple_response()

    h = response.headers

    h['header3'] = 'key3'
    h['header4'] = 'key4'

    headers = {
        'header1': 'key1',
        'header2': 'key2',
        'header3': 'key3',
        'header4': 'key4',
    }
    asserts.assert_that(response.headers.items()).is_equal_to(headers.items())


def _test_response_get_status_code():
    response = _create_simple_response()
    asserts.assert_that(response.status_code).is_equal_to(200)


def _test_is_instance():
    response = _create_simple_response()
    asserts.assert_that(builtins.isinstance(response, VGSHttpResponse)).is_true()


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_response_get_body))
    _suite.addTest(unittest.FunctionTestCase(_test_response_set_body))
    _suite.addTest(unittest.FunctionTestCase(_test_response_headers_setter))
    _suite.addTest(unittest.FunctionTestCase(_test_response_headers_property_add_header))
    _suite.addTest(unittest.FunctionTestCase(_test_response_get_status_code))
    _suite.addTest(unittest.FunctionTestCase(_test_is_instance))


    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
