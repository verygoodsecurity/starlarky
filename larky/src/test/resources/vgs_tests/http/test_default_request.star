"""Unit tests for request.star"""
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vgs//http/request", "VGSHttpRequest")
load("@stdlib//builtins", builtins="builtins")


def _create_simple_request():
    url = 'http://netloc/path;parameters?query=argument#fragment'
    relative_url = '/path;parameters?query=argument'
    body = builtins.bytes('request body')
    headers = {
        'header1': 'key1',
        'header2': 'key2',
    }
    return VGSHttpRequest(url, data=body, headers=headers, method='POST')


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

    headers = {
        'header1': 'key1',
        'header2': 'key2',
    }
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())


def _test_request_remove_header():
    request = _create_simple_request()

    request.remove_header('header2')

    headers = {
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

    headers = {
        'header3': 'key3',
        'header4': 'key4',
    }
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())


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
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())


def _test_request_get_method():
    request = _create_simple_request()
    asserts.assert_that(request.method).is_equal_to('POST')


def _test_request_get_full_url():
    url = 'http://netloc/path;parameters?query=argument#fragment'
    request = _create_simple_request()
    asserts.assert_that(request.full_url).is_equal_to(url)


def _test_request_get_url():
    url = 'http://netloc/path;parameters?query=argument#fragment'
    request = _create_simple_request()
    asserts.assert_that(request.url).is_equal_to(url)


def _test_request_get_path():
    path = '/path;parameters'
    request = _create_simple_request()
    asserts.assert_that(request.path).is_equal_to(path)


def _test_request_get_query_string():
    query_string = 'query=argument'
    request = _create_simple_request()
    asserts.assert_that(request.query_string).is_equal_to(query_string)


def _test_request_get_fragment():
    fragment = 'fragment'
    request = _create_simple_request()
    asserts.assert_that(request.fragment).is_equal_to(fragment)


def _test_body_data_and_body_aliases():
    request = _create_simple_request()
    asserts.assert_that(request.data).is_equal_to(request.body)


def _test_method_and_get_method_aliases():
    request = _create_simple_request()
    asserts.assert_that(request.method).is_equal_to(request.get_method())


def _test_full_url_and_get_full_url_aliases():
    request = _create_simple_request()
    asserts.assert_that(request.full_url).is_equal_to(request.get_full_url())


def _test_full_url_and_url_aliases():
    request = _create_simple_request()
    asserts.assert_that(request.full_url).is_equal_to(request.url)


def _test_is_instance():
    request = _create_simple_request()
    asserts.assert_that(builtins.isinstance(request, VGSHttpRequest)).is_true()


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
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_full_url))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_url))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_path))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_query_string))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_fragment))
    _suite.addTest(unittest.FunctionTestCase(_test_body_data_and_body_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_method_and_get_method_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_full_url_and_get_full_url_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_full_url_and_url_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_is_instance))


    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
