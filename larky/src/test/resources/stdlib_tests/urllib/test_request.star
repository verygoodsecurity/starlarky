"""Unit tests for request.star"""
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//operator", operator="operator")
load("@stdlib//unittest", "unittest")
load("@stdlib//urllib/request", "Request", urllib_request="request")
load("@vendor//asserts", "asserts")


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
    asserts.assert_that(str(request.body())).is_equal_to('request body')


def _test_request_set_body():
    request = _create_simple_request()
    new_body_str = 'new request body'
    request.set_body(builtins.bytes(new_body_str))
    asserts.assert_that(str(request.body())).is_equal_to(new_body_str)
    asserts.assert_that(str(request.data)).is_equal_to('new request body')
    new_body_str = 'new new request body'
    request.data = new_body_str
    asserts.assert_that(str(request.data)).is_equal_to(request.body())


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
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())


def _test_request_remove_header():
    request = _create_simple_request()

    request.remove_header('header2')

    headers = {      # key capitalized
        'header1': 'key1',
    }
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())


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
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())


def _test_request_headers_property_add_header():
    request = _create_simple_request()

    h = request.headers
    # valid python.
    operator.setitem(h, 'header3', 'key3')
    operator.setitem(h, 'header4', 'key4')
    headers = {
        'header1': 'key1',
        'header2': 'key2',
        'header3': 'key3',
        'header4': 'key4',
    }
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())

    # backport works for larky sanitized message
    request = _create_simple_request()
    h = request.headers()
    h['header3'] = 'key3'
    h['header4'] = 'key4'
    headers = {
        'header1': 'key1',
        'header2': 'key2',
        'header3': 'key3',
        'header4': 'key4',
    }
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())


def _test_request_get_method():
    request = _create_simple_request()
    asserts.assert_that(request.method).is_equal_to('POST')


def _test_request_get_uri():
    request = _create_simple_request()
    asserts.assert_that(request.uri()).is_equal_to('http://netloc/path;parameters?query=argument#fragment')


def _test_body_data_and_body_aliases():
    request = _create_simple_request()
    asserts.assert_that(request.data).is_equal_to(request.raw_body())
    asserts.assert_that(request.data.decode('utf-8')).is_equal_to(request.body())


def _test_method_and_get_method_aliases():
    request = _create_simple_request()
    asserts.assert_that(request.method).is_equal_to(request.get_method())


def _test_url_and_get_full_url_aliases():
    request = _create_simple_request()
    asserts.assert_that(request.uri()).is_equal_to(request.get_full_url())


def _test_is_instance():
    request = _create_simple_request()
    kwargs_request = urllib_request.Request(**request.__dict__)
    asserts.assert_that(builtins.isinstance(request, Request)).is_true()
    asserts.assert_that(builtins.isinstance(kwargs_request, Request)).is_true()


def _test_create_request_with_kwargs():
    request = _create_simple_request()
    kwargs_request = urllib_request.Request(**request.__dict__)

    asserts.assert_that(request.url).is_equal_to(kwargs_request.url)
    asserts.assert_that(request.data).is_equal_to(kwargs_request.data)
    asserts.assert_that(request.headers.items()).is_equal_to(kwargs_request.headers.items())
    asserts.assert_that(request.header_items()).is_equal_to(kwargs_request.headers.items())
    asserts.assert_that(request.method).is_equal_to(kwargs_request.method)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_body))
    _suite.addTest(unittest.FunctionTestCase(_test_request_set_body))
    _suite.addTest(unittest.FunctionTestCase(_test_request_has_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_request_remove_header))
    _suite.addTest(unittest.FunctionTestCase(_test_request_headers_property_set_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_request_headers_property_add_header))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_method))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_uri))
    _suite.addTest(unittest.FunctionTestCase(_test_body_data_and_body_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_method_and_get_method_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_url_and_get_full_url_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_is_instance))
    _suite.addTest(unittest.FunctionTestCase(_test_create_request_with_kwargs))


    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
