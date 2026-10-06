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


def _test_response_keeps_duplicate_headers():
    """A response may legitimately carry the same header more than once.

    ``Set-Cookie`` is the canonical example: RFC 6265 requires one header per
    cookie, so collapsing them logs the user out.
    """
    response = VGSHttpResponse(
        body=builtins.bytes('response body'),
        headers=[
            ('Content-Type', 'text/html'),
            ('Set-Cookie', 'a=1; Path=/'),
            ('Set-Cookie', 'b=2; Path=/'),
        ],
    )

    asserts.assert_that(response.headers.getall('Set-Cookie')).is_equal_to(
        ['a=1; Path=/', 'b=2; Path=/']
    )
    asserts.assert_that(response.headers.items()).is_equal_to([
        ('Content-Type', 'text/html'),
        ('Set-Cookie', 'a=1; Path=/'),
        ('Set-Cookie', 'b=2; Path=/'),
    ])


def _test_response_headers_are_case_insensitive():
    response = _create_simple_response()
    response.add_header('Content-Type', 'text/html')

    asserts.assert_that(response.get_header('content-TYPE')).is_equal_to('text/html')
    asserts.assert_that(response.has_header('CONTENT-type')).is_true()


def _test_response_append_header():
    response = _create_simple_response()

    response.append_header('Set-Cookie', 'a=1')
    response.append_header('set-cookie', 'b=2')

    asserts.assert_that(response.get_all_headers('Set-Cookie')).is_equal_to(['a=1', 'b=2'])
    asserts.assert_that(response.get_header('Set-Cookie')).is_equal_to('a=1')


def _test_response_add_header_replaces_duplicates():
    response = _create_simple_response()

    response.append_header('Set-Cookie', 'a=1')
    response.append_header('Set-Cookie', 'b=2')
    response.add_header('Set-Cookie', 'c=3')

    asserts.assert_that(response.get_all_headers('Set-Cookie')).is_equal_to(['c=3'])


def _test_response_remove_header():
    response = _create_simple_response()

    response.append_header('Set-Cookie', 'a=1')
    response.append_header('Set-Cookie', 'b=2')
    response.remove_header('set-cookie')

    asserts.assert_that(response.has_header('Set-Cookie')).is_false()
    asserts.assert_that(response.get_all_headers('Set-Cookie')).is_equal_to([])


def _test_response_remove_missing_header_is_a_noop():
    response = _create_simple_response()
    response.remove_header('Set-Cookie')
    asserts.assert_that(response.has_header('Set-Cookie')).is_false()


def _test_response_set_body_drops_stale_content_length():
    """issue 16464: a Content-Length computed for the previous body is wrong."""
    response = VGSHttpResponse(
        body=builtins.bytes('response body'),
        headers={'Content-Length': '13'},
    )

    response.body = builtins.bytes('a much longer response body')

    asserts.assert_that(response.has_header('Content-Length')).is_false()


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
    _suite.addTest(unittest.FunctionTestCase(_test_response_keeps_duplicate_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_response_headers_are_case_insensitive))
    _suite.addTest(unittest.FunctionTestCase(_test_response_append_header))
    _suite.addTest(unittest.FunctionTestCase(_test_response_add_header_replaces_duplicates))
    _suite.addTest(unittest.FunctionTestCase(_test_response_remove_header))
    _suite.addTest(unittest.FunctionTestCase(_test_response_remove_missing_header_is_a_noop))
    _suite.addTest(unittest.FunctionTestCase(_test_response_set_body_drops_stale_content_length))
    _suite.addTest(unittest.FunctionTestCase(_test_response_get_status_code))
    _suite.addTest(unittest.FunctionTestCase(_test_is_instance))


    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
