"""Unit tests for request.star"""
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@stdlib//urllib/request", "Request", urllib_request="request")
load("@stdlib//builtins", builtins="builtins")

load('@stdlib//json', 'json')
load("@stdlib//hashlib", "hashlib")


def _create_simple_request():
    url = 'http://netloc/path;parameters?query=argument#fragment'
    body = builtins.bytes('request body')
    headers = {
        'header1': 'key1',
        'header2': 'key2',
    }
    return Request(url, data=body, headers=headers, method='POST')


def _test_request_get_data():
    request = _create_simple_request()
    asserts.assert_that(str(request.data)).is_equal_to('request body')


def _test_request_set_data():
    request = _create_simple_request()
    new_body_str = 'new request body'
    request.data = builtins.bytes(new_body_str)
    asserts.assert_that(str(request.data)).is_equal_to(new_body_str)


def _test_request_has_headers():
    request = _create_simple_request()

    headers = {             # urllib capitalize header keys
        'Header1': 'key1',
        'Header2': 'key2',
    }
    for h in headers:
        asserts.assert_that(request.has_header(h)).is_true()


def _test_request_get_headers():
    request = _create_simple_request()


    headers = {            # urllib capitalize header keys
        'Header1': 'key1',
        'Header2': 'key2',
    }
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())


def _test_request_remove_header():
    request = _create_simple_request()

    request.remove_header('Header2')

    headers = {             # urllib capitalize header keys
        'Header1': 'key1',
    }
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())


def _test_request_headers_property_set_headers():
    request = _create_simple_request()

    new_headers = {         # urllib capitalize header keys
        'Header3': 'key3',
        'Header4': 'key4',
    }
    request.headers = new_headers

    headers = {             # urllib capitalize header keys
        'Header3': 'key3',
        'Header4': 'key4',
    }
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())


def _test_request_headers_property_add_header():
    request = _create_simple_request()

    h = request.headers

    h['header3'] = 'key3'
    h['header4'] = 'key4'

    headers = {             # urllib capitalize header keys
        'Header1': 'key1',
        'Header2': 'key2',

        'header3': 'key3', # do not capitalize directly injected keys
        'header4': 'key4',
    }
    asserts.assert_that(request.headers.items()).is_equal_to(headers.items())


def _test_request_get_method():
    request = _create_simple_request()
    asserts.assert_that(request.method).is_equal_to('POST')


def _test_request_get_full_url():
    request = _create_simple_request()
    asserts.assert_that(request.get_full_url()).is_equal_to('http://netloc/path;parameters?query=argument#fragment')


def _test_method_and_get_method_aliases():
    request = _create_simple_request()
    asserts.assert_that(request.method).is_equal_to(request.get_method())


def _test_is_instance():
    request = _create_simple_request()
    asserts.assert_that(builtins.isinstance(request, Request)).is_true()


def _test_request_pylarky():
    ## Code taken from pylarky tests
    def process(input_message):
        #print("start script")
        decoded_payload = json.decode(input_message.data)
        key=input_message.get_header("Key")
        input_message.remove_header("Key")
        signature = hashlib.sha512(bytes(key, encoding='utf-8')).hexdigest()
        #print("changing payload")
        decoded_payload["signature"] = signature
        input_message.data = json.encode(decoded_payload)
        #print("returning payload")
        return input_message

        ## TODO (mahmoudimus): FIX THIS BELOW!
        #return 'url = "%s", data = "%s", headers = %s, object: %s' % (
        #           input_message.get_full_url(),
        #           input_message.data,
        #           json.dumps(dict(input_message.header_items())),
        #           input_message.__dict__)
    request = Request(
            url='http://httpbin.org/post',
            data='{"credit_card": "411111111111111111", "cvv": "043", "expiration_date": "03/43"}',
            headers={'Content-Type': 'application/json', 'Accept': 'application/json', 'Key': '1234567890'})
    modified_request = process(request)

    asserts.assert_that(modified_request.full_url).is_equal_to("http://httpbin.org/post")
    asserts.assert_that(modified_request.data).is_equal_to('{"credit_card":"411111111111111111","cvv":"043","expiration_date":"03/43","signature":"12b03226a6d8be9c6e8cd5e55dc6c7920caaa39df14aab92d5e3ea9340d1c8a4d3d0b8e4314f1f6ef131ba4bf1ceb9186ab87c801af0d5c95b1befb8cedae2b9"}')
    asserts.assert_that(modified_request.headers).is_equal_to({"Content-type": "application/json","Accept": "application/json"})


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_data))
    _suite.addTest(unittest.FunctionTestCase(_test_request_set_data))
    _suite.addTest(unittest.FunctionTestCase(_test_request_has_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_request_remove_header))
    _suite.addTest(unittest.FunctionTestCase(_test_request_headers_property_set_headers))
    _suite.addTest(unittest.FunctionTestCase(_test_request_headers_property_add_header))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_method))
    _suite.addTest(unittest.FunctionTestCase(_test_request_get_full_url))
    _suite.addTest(unittest.FunctionTestCase(_test_method_and_get_method_aliases))
    _suite.addTest(unittest.FunctionTestCase(_test_is_instance))
    _suite.addTest(unittest.FunctionTestCase(_test_request_pylarky))


    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
