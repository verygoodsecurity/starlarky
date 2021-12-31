"""Unit tests for request.star"""
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@stdlib//urllib/request", "Request")
load("@stdlib//builtins", builtins="builtins")


def _create_simple_request():
    url = 'http://netloc/path;parameters?query=argument#fragment'
    body = builtins.bytes('request body')
    headers = {
        'header1': 'key1',
        'header2': 'key2',
    }
    return Request(url, data=body, headers=headers, method='POST')


def _test_request_get_body():
    request = _create_simple_request()
    asserts.assert_that(str(request.body)).is_equal_to('request body')


def _test_request_set_body():
    request = _create_simple_request()
    new_body_str = 'new request body'
    request.body = builtins.bytes(new_body_str)
    asserts.assert_that(str(request.body)).is_equal_to(new_body_str)


def _test_request_has_headers():
    request = _create_simple_request()

    headers = {
        'header1': 'key1',
        'header2': 'key2',
    }
    for h in headers:
        asserts.assert_that(request.has_header(h)).is_true()


def _test_request_get_headers():
    request = _create_simple_request()


    headers = {      # key capitalized
        'header1': 'key1',
        'header2': 'key2',
    }
    asserts.assert_that(request.headers).is_equal_to(headers)


def _test_request_remove_header():
    request = _create_simple_request()

    request.remove_header('header2')

    headers = {      # key capitalized
        'header1': 'key1',
    }
    asserts.assert_that(request.headers).is_equal_to(headers)


def _test_request_headers_property_set_headers():
    request = _create_simple_request()

    new_headers = {
        'header3': 'key3',
        'header4': 'key4',
    }
    request.headers = new_headers

    headers = {      # key capitalized
        'header3': 'key3',
        'header4': 'key4',
    }
    asserts.assert_that(request.headers).is_equal_to(headers)


def _test_request_headers_property_add_header():
    request = _create_simple_request()

    h = request.headers

    h['header3'] = 'key3'
    h['header4'] = 'key4'

    headers = {
        'header1': 'key1',
        'header2': 'key2',
        'header3': 'key3',
        'header4': 'key4',
    }
    asserts.assert_that(request.headers).is_equal_to(headers)


def _test_request_add_headers():
    request = _create_simple_request()

    new_headers = {
        'header3': 'key3',
        'header4': 'key4',
    }
    request.add_headers(new_headers)

    headers = {
        'header1': 'key1',
        'header2': 'key2',
        'header3': 'key3',
        'header4': 'key4',
    }
    asserts.assert_that(request.headers).is_equal_to(headers)


def _test_request_get_method():
    request = _create_simple_request()
    asserts.assert_that(request.method).is_equal_to('POST')


def _test_request_get_url():
    request = _create_simple_request()
    asserts.assert_that(request.url).is_equal_to('http://netloc/path;parameters?query=argument#fragment')


def _test_body_data_and_body_aliases():
    request = _create_simple_request()
    asserts.assert_that(request.data).is_equal_to(request.body)


def _test_method_and_get_method_aliases():
    request = _create_simple_request()
    asserts.assert_that(request.method).is_equal_to(request.get_method())


def _test_url_and_get_full_url_aliases():
    request = _create_simple_request()
    asserts.assert_that(request.url).is_equal_to(request.get_full_url())


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_body))
    _suite.addTest(unittest.FunctionTestCase(_test_request_set_body))
    _suite.addTest(unittest.FunctionTestCase(_test_request_has_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_request_remove_header))
    _suite.addTest(unittest.FunctionTestCase(_test_request_headers_property_set_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_request_headers_property_add_header))
    _suite.addTest(unittest.FunctionTestCase(_test_request_add_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_method))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_url))
    _suite.addTest(unittest.FunctionTestCase(_test_body_data_and_body_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_method_and_get_method_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_url_and_get_full_url_aliases))


    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
