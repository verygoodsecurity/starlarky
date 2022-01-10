"""Unit tests for response.star"""
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@stdlib//http/client", "Response", urllib_response="response")
load("@stdlib//builtins", builtins="builtins")


def _create_simple_response():
    body = builtins.bytes('response body')
    headers = {
        'header1': 'key1',
        'header2': 'key2',
    }
    return Response(data=body, status=400, headers=headers)


def _test_response_get_body():
    response = _create_simple_response()
    asserts.assert_that(str(response.body)).is_equal_to('response body')


def _test_response_set_body():
    response = _create_simple_response()
    new_body_str = 'new response body'
    response.body = builtins.bytes(new_body_str)
    asserts.assert_that(str(response.body)).is_equal_to(new_body_str)


def _test_response_has_headers():
    response = _create_simple_response()

    headers = {
        'header1': 'key1',
        'header2': 'key2',
    }
    for h in headers:
        asserts.assert_that(response.has_header(h)).is_true()


def _test_response_get_headers():
    response = _create_simple_response()

    headers = {
        'header1': 'key1',
        'header2': 'key2',
    }
    asserts.assert_that(response.headers).is_equal_to(headers)


def _test_response_remove_header():
    response = _create_simple_response()

    response.remove_header('header2')

    headers = {
        'header1': 'key1',
    }
    asserts.assert_that(response.headers).is_equal_to(headers)


def _test_response_headers_property_set_headers():
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
    asserts.assert_that(response.headers).is_equal_to(headers)


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
    asserts.assert_that(response.headers).is_equal_to(headers)


def _test_response_get_status():
    response = _create_simple_response()
    asserts.assert_that(response.status).is_equal_to(400)


def _test_body_data_and_body_aliases():
    response = _create_simple_response()
    asserts.assert_that(response.data).is_equal_to(response.body)


def _test_status_and_get_status_aliases():
    response = _create_simple_response()
    asserts.assert_that(response.status).is_equal_to(response.get_status())


def _test_is_instance():
    response = _create_simple_response()
    kwargs_response = urllib_response._Response(**response.__dict__)
    asserts.assert_that(builtins.isinstance(response, Response)).is_true()
    asserts.assert_that(builtins.isinstance(kwargs_response, Response)).is_true()


def _test_create_response_with_kwargs():
    response = _create_simple_response()
    kwargs_response = urllib_response._Response(**response.__dict__)

    asserts.assert_that(response.data).is_equal_to(kwargs_response.data)
    asserts.assert_that(response.headers).is_equal_to(kwargs_response.headers)
    asserts.assert_that(response.status).is_equal_to(kwargs_response.status)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_response_get_body))
    _suite.addTest(unittest.FunctionTestCase(_test_response_set_body))
    _suite.addTest(unittest.FunctionTestCase(_test_response_has_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_response_get_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_response_remove_header))
    _suite.addTest(unittest.FunctionTestCase(_test_response_headers_property_set_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_response_headers_property_add_header))
    _suite.addTest(unittest.FunctionTestCase(_test_response_get_status))
    _suite.addTest(unittest.FunctionTestCase(_test_body_data_and_body_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_status_and_get_status_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_is_instance))
    _suite.addTest(unittest.FunctionTestCase(_test_create_response_with_kwargs))


    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
