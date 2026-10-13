"""Unit tests for response.star"""
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//json", json="json")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vgs//http/request", "VGSCIMultiDict")
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


def _test_response_headers_stay_a_dict():
    """The host reads ``__dict__["headers"]`` as a Map<String, String> (issue #292)."""
    response = _create_simple_response()
    asserts.assert_that(type(response.headers)).is_equal_to("dict")
    asserts.assert_that(type(response.__dict__["headers"])).is_equal_to("dict")
    asserts.assert_that(response.__dict__["headers"]).is_equal_to(
        {'header1': 'key1', 'header2': 'key2'})

    response.headers = [('header3', 'key3')]
    asserts.assert_that(type(response.__dict__["headers"])).is_equal_to("dict")
    asserts.assert_that(dict(response.headers)).is_equal_to({'header3': 'key3'})
    asserts.assert_that(json.dumps(response.headers)).is_equal_to('{"header3":"key3"}')


def _test_response_headers_from_pairs():
    response = VGSHttpResponse(
        body=builtins.bytes('response body'),
        headers=[('Content-Type', 'text/html'), ('Set-Cookie', 'a=1; Path=/')],
    )
    asserts.assert_that(response.headers).is_equal_to(
        {'Content-Type': 'text/html', 'Set-Cookie': 'a=1; Path=/'})


def _test_response_headers_from_multidict():
    response = VGSHttpResponse(headers=VGSCIMultiDict([('A', '1'), ('b', '2')]))
    asserts.assert_that(response.headers).is_equal_to({'A': '1', 'b': '2'})


def _test_response_rejects_a_repeated_header():
    """A dict holds one value per name: say so instead of dropping a Set-Cookie."""
    asserts.assert_fails(
        lambda: VGSHttpResponse(headers=[('Set-Cookie', 'a=1'), ('Set-Cookie', 'b=2')]),
        "VGSHttpResponse headers hold one value per name; got \"Set-Cookie\" more than once")
    response = _create_simple_response()

    def assign():
        response.headers = VGSCIMultiDict([('Set-Cookie', 'a=1'), ('Set-Cookie', 'b=2')])
    asserts.assert_fails(assign, "got \"Set-Cookie\" more than once")


def _test_response_none_headers():
    response = VGSHttpResponse(body=builtins.bytes('x'))
    asserts.assert_that(response.headers).is_equal_to({})
    response = VGSHttpResponse(body=builtins.bytes('x'), headers=[])
    asserts.assert_that(response.headers).is_equal_to({})

    def assign():
        # e.g. `response.headers = headers.update(...)`: the host could not send None
        response.headers = None
    asserts.assert_fails(
        assign, "VGSHttpResponse headers: expected a dict or \\(name, value\\) pairs; got NoneType")


def _test_response_constructor_copies_headers():
    headers = {'A': '1'}
    response = VGSHttpResponse(headers=headers)
    headers['B'] = '2'
    asserts.assert_that(response.headers).is_equal_to({'A': '1'})


def _test_response_assigned_headers_dict_is_kept():
    response = _create_simple_response()
    headers = {'A': '1'}
    response.headers = headers
    headers['B'] = '2'
    asserts.assert_that(response.headers).is_equal_to({'A': '1', 'B': '2'})


def _test_response_header_helpers_ignore_case():
    response = VGSHttpResponse(headers={'Content-Type': 'text/html', 'content-type': 'text/plain'})

    asserts.assert_that(response.has_header('CONTENT-type')).is_true()
    asserts.assert_that(response.has_header('Set-Cookie')).is_false()
    asserts.assert_that(response.get_header('content-TYPE')).is_equal_to('text/html')
    asserts.assert_that(response.get_header('Set-Cookie')).is_none()
    asserts.assert_that(response.get_header('Set-Cookie', 'x')).is_equal_to('x')

    response.remove_header('CONTENT-TYPE')
    asserts.assert_that(response.headers).is_equal_to({})


def _test_response_add_header_replaces():
    response = _create_simple_response()
    response.add_header('header1', 'new')
    asserts.assert_that(response.headers['header1']).is_equal_to('new')
    asserts.assert_that(len(response.headers)).is_equal_to(2)


def _test_response_remove_missing_header_is_a_noop():
    response = _create_simple_response()
    response.remove_header('Set-Cookie')
    asserts.assert_that(response.headers).is_equal_to({'header1': 'key1', 'header2': 'key2'})


def _test_response_set_body_drops_stale_content_length():
    """issue 16464: a Content-length computed for the previous body is wrong."""
    response = VGSHttpResponse(
        body=builtins.bytes('response body'),
        headers={'Content-length': '13', 'Content-Type': 'text/plain'},
    )

    response.body = builtins.bytes('response body')
    asserts.assert_that(response.headers['Content-length']).is_equal_to('13')

    response.body = builtins.bytes('a much longer response body')
    asserts.assert_that(response.headers).is_equal_to({'Content-Type': 'text/plain'})


def _test_response_set_body_drops_other_content_length_spellings():
    """Header names are case-insensitive: every spelling of Content-Length is dropped."""
    for name in ['Content-Length', 'content-length', 'CONTENT-LENGTH']:
        response = VGSHttpResponse(
            body=builtins.bytes('response body'),
            headers={name: '13', 'Content-Type': 'text/plain'},
        )
        response.body = builtins.bytes('a much longer response body')
        asserts.assert_that(response.headers).is_equal_to({'Content-Type': 'text/plain'})


def _test_response_header_helpers_reject_non_string_names():
    response = _create_simple_response()
    asserts.assert_fails(
        lambda: response.has_header(None),
        "TypeError: VGSHttpResponse.has_header\\(header_name\\) expected `string`; got NoneType")
    asserts.assert_fails(
        lambda: response.get_header(1),
        "TypeError: VGSHttpResponse.get_header\\(header_name\\) expected `string`; got int")
    asserts.assert_fails(
        lambda: response.remove_header(['header1']),
        "TypeError: VGSHttpResponse.remove_header\\(header_name\\) expected `string`; got list")


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
    _suite.addTest(unittest.FunctionTestCase(_test_response_headers_stay_a_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_response_headers_from_pairs))
    _suite.addTest(unittest.FunctionTestCase(_test_response_headers_from_multidict))
    _suite.addTest(unittest.FunctionTestCase(_test_response_rejects_a_repeated_header))
    _suite.addTest(unittest.FunctionTestCase(_test_response_none_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_response_constructor_copies_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_response_assigned_headers_dict_is_kept))
    _suite.addTest(unittest.FunctionTestCase(_test_response_header_helpers_ignore_case))
    _suite.addTest(unittest.FunctionTestCase(_test_response_add_header_replaces))
    _suite.addTest(unittest.FunctionTestCase(_test_response_remove_missing_header_is_a_noop))
    _suite.addTest(unittest.FunctionTestCase(_test_response_set_body_drops_stale_content_length))
    _suite.addTest(unittest.FunctionTestCase(_test_response_set_body_drops_other_content_length_spellings))
    _suite.addTest(unittest.FunctionTestCase(_test_response_header_helpers_reject_non_string_names))
    _suite.addTest(unittest.FunctionTestCase(_test_response_get_status_code))
    _suite.addTest(unittest.FunctionTestCase(_test_is_instance))


    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
