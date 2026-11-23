"""Request, VGSHttpRequest and VGSHttpResponse as Python objects.

Their classes are written in Java on the object model of type(): plain attributes and properties
are descriptors of the class, and VGSHttpRequest's class extends Request's. __dict__ lists the
plain attributes that are set and the attributes a script added.
"""
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//urllib/request", "Request")
load("@vendor//asserts", asserts="asserts")
load("@vendor//multidict", "CIMultiDict", "CIMultiDictProxy")
load("@vgs//http/request", "VGSHttpRequest", "VGSCIMultiDict")
load("@vgs//http/response", "VGSHttpResponse")


def _test_request_state_is_its_dict():
    request = Request("https://example.test/p?q=1#frag", data=b"d", method="PUT")
    state = request.__dict__
    asserts.assert_that(sorted(state.keys())).is_equal_to([
        "_data", "_full_url", "_tunnel_host", "fragment", "headers", "host", "method",
        "origin_req_host", "selector", "type", "unredirected_hdrs", "unverifiable",
    ])
    asserts.assert_that(state["_full_url"]).is_equal_to("https://example.test/p?q=1")
    asserts.assert_that(state["host"]).is_equal_to("example.test")

    # an attribute a script adds is listed after them
    request.tag = "t"
    asserts.assert_that(list(request.__dict__.keys())[-1]).is_equal_to("tag")
    asserts.assert_that(request.__dict__["tag"]).is_equal_to("t")

    # the dict is a copy: changing it does not change the request
    request.__dict__["other"] = 1
    request.__dict__["host"] = "changed"
    asserts.assert_that(hasattr(request, "other")).is_false()
    asserts.assert_that(request.host).is_equal_to("example.test")

    # a request built without a method has no method attribute
    asserts.assert_that("method" in Request("https://example.test/").__dict__).is_false()
    asserts.assert_that(hasattr(Request("https://example.test/"), "method")).is_false()


def _test_properties_are_the_class_s():
    request = Request("https://example.test/a")
    # a property is not part of the state
    asserts.assert_that("full_url" in request.__dict__).is_false()
    asserts.assert_that("data" in request.__dict__).is_false()

    request.full_url = "<URL:http://other.test/b#f>"
    asserts.assert_that((request.type, request.host, request.selector, request.fragment)).is_equal_to(
        ("http", "other.test", "/b", "f"))
    asserts.assert_that(request._full_url).is_equal_to("http://other.test/b")

    request.add_header("Content-length", "1")
    request.data = b"new"
    asserts.assert_that(request._data).is_equal_to(b"new")
    asserts.assert_that(request.has_header("Content-length")).is_false()

    names = dir(request)
    for name in ["full_url", "data", "host", "get_method", "add_header", "__class__", "__dict__", "__name__"]:
        asserts.assert_that(name in names).is_true()


def _test_an_instance_attribute_comes_before_a_method():
    request = Request("https://example.test/")
    asserts.assert_that(request.get_method()).is_equal_to("GET")
    request.get_method = lambda: "CUSTOM"
    asserts.assert_that(request.get_method()).is_equal_to("CUSTOM")

    # the class calls methods on self, so it calls the instance's
    seen = []
    request.has_header = lambda name: seen.append(name) or False
    request.data = b"x"
    asserts.assert_that(seen).is_equal_to(["Content-length"])


def _test_vgs_request_extends_request():
    request = VGSHttpRequest("https://example.test/p?q=1#frag", data=b"d", headers={"x-a": "1"})
    asserts.assert_that(request.__class__ == VGSHttpRequest).is_true()
    asserts.assert_that(request.__name__).is_equal_to("VGSHttpRequest")

    # Request's properties and methods
    asserts.assert_that(request.full_url).is_equal_to("https://example.test/p?q=1#frag")
    asserts.assert_that(request.data).is_equal_to(b"d")
    asserts.assert_that(request.get_method()).is_equal_to("POST")
    asserts.assert_that(request.has_header("X-A")).is_true()

    # its own: headers is a property over _headers, so the dict has no headers entry
    state = request.__dict__
    asserts.assert_that("headers" in state).is_false()
    asserts.assert_that(state["_headers"] == request.headers).is_true()
    asserts.assert_that((state["path"], state["query_string"])).is_equal_to(("/p", "q=1"))

    request.url = "https://other.test/x?y=2"
    asserts.assert_that((request.host, request.path, request.query_string)).is_equal_to(
        ("other.test", "/x", "y=2"))
    request.body = b"body"
    asserts.assert_that(request.data).is_equal_to(b"body")
    request.headers = {"X-B": "2"}
    asserts.assert_that(request.headers.getall("x-b")).is_equal_to(["2"])
    asserts.assert_that(request.__dict__["_headers"] == request.headers).is_true()

    # add_header keeps the key, where Request's capitalizes it
    request.add_header("x-KEEP", "v")
    asserts.assert_that("x-KEEP" in [key for key, _ in request.headers.items()]).is_true()


def _test_response_state_and_properties():
    response = VGSHttpResponse(body=b"b", headers={"Content-Length": "1"}, status_code=201)
    asserts.assert_that(response.__class__ == VGSHttpResponse).is_true()
    asserts.assert_that(response.__name__).is_equal_to("VGSHttpResponse")
    asserts.assert_that(sorted(response.__dict__.keys())).is_equal_to(["_body", "_headers", "status_code"])

    response.body = b"changed"
    asserts.assert_that(response._body).is_equal_to(b"changed")
    asserts.assert_that(response.get_all_headers("content-length")).is_equal_to([])
    response.headers = [("A", "1"), ("a", "2")]
    asserts.assert_that(response.get_all_headers("A")).is_equal_to(["1", "2"])
    response.status_code = 404
    asserts.assert_that(response.__dict__["status_code"]).is_equal_to(404)


def _test_the_class_attributes_cannot_be_set():
    for value in [Request("https://example.test/"), VGSHttpRequest("https://example.test/"), VGSHttpResponse()]:
        for name in ["__class__", "__name__", "__dict__"]:
            asserts.assert_fails(lambda: setattr(value, name, 1), "attribute '%s' is read-only" % name)


def _test_dict_order():
    request = Request("https://example.test/p#f", data=b"d", method="PUT")
    asserts.assert_that(list(request.__dict__.keys())).is_equal_to([
        "_full_url", "fragment", "type", "host", "selector", "headers", "unredirected_hdrs",
        "_data", "_tunnel_host", "origin_req_host", "unverifiable", "method",
    ])
    request = VGSHttpRequest("https://example.test/p#f", data=b"d", method="PUT")
    asserts.assert_that(list(request.__dict__.keys())).is_equal_to([
        "_full_url", "fragment", "type", "host", "selector", "unredirected_hdrs",
        "_data", "_tunnel_host", "origin_req_host", "unverifiable", "method",
        "_headers", "path", "query_string",
    ])


def _test_removed_state_is_a_missing_attribute():
    request = Request("https://example.test/p#f")
    request.__delattr__("fragment")
    asserts.assert_fails(lambda: request.full_url, "'Request' object has no attribute 'fragment'")
    asserts.assert_that(hasattr(request, "fragment")).is_false()
    request.__delattr__("_full_url")
    asserts.assert_fails(lambda: request._parse(), "'Request' object has no attribute '_full_url'")
    asserts.assert_that("_full_url" in request.__dict__).is_false()
    asserts.assert_that("_full_url" in dir(request)).is_false()
    request._full_url = "http://again.test/x"
    request._parse()
    asserts.assert_that(request.host).is_equal_to("again.test")
    asserts.assert_fails(lambda: request.__delattr__("nope"), "'Request' object has no attribute 'nope'")
    asserts.assert_fails(lambda: request.__delattr__("data"), "can't delete attribute 'data'")

    response = VGSHttpResponse(body=b"b", status_code=200)
    response.__delattr__("_headers")
    asserts.assert_fails(lambda: setattr(response, "body", b"n"), "'VGSHttpResponse' object has no attribute '_headers'")
    response.__delattr__("status_code")
    asserts.assert_that(repr(response)).is_equal_to("<VGSHttpResponse [None]>")

    # an attribute a script added can be deleted too
    request = Request("https://example.test/")
    request.tag = 1
    request.__delattr__("tag")
    asserts.assert_that(hasattr(request, "tag")).is_false()
    asserts.assert_fails(lambda: request.nope, "no field or method 'nope'")


def _test_explicit_protocol_calls():
    request = Request("https://example.test/")
    asserts.assert_that(request.__getattribute__("host")).is_equal_to("example.test")
    asserts.assert_fails(lambda: request.__getattribute__("nope"), "no field or method 'nope'")
    request.__setattr__("full_url", "http://other.test/x")
    asserts.assert_that(request.host).is_equal_to("other.test")
    request.__setattr__("tag", 1)
    asserts.assert_that(request.__dict__["tag"]).is_equal_to(1)


def _test_getattr_hook_and_super():
    request = Request("https://example.test/")
    request.__getattr__ = lambda name: "hooked:" + name
    asserts.assert_that(getattr(request, "zzz", "D")).is_equal_to("hooked:zzz")
    asserts.assert_that(hasattr(request, "zzz")).is_true()

    # the class is the function: there is no class object for super() to hand out
    asserts.assert_fails(
        lambda: super(object, Request("https://example.test/")),
        "was expecting obj to of type PyObject, but got Request")


def _test_multidict_methods_are_the_class_s():
    headers = VGSCIMultiDict([("A", "1"), ("a", "2")])
    asserts.assert_that(headers.getall("A")).is_equal_to(["1", "2"])
    asserts.assert_that(headers.__dict__).is_equal_to({})
    asserts.assert_that("getall" in dir(headers)).is_true()

    # an attribute a script sets comes before the method
    headers.getall = lambda key: ["custom"]
    asserts.assert_that(headers.getall("A")).is_equal_to(["custom"])
    asserts.assert_that(headers.__dict__["getall"] == headers.getall).is_true()

    proxy = CIMultiDictProxy(CIMultiDict([("A", "1")]))
    asserts.assert_that(proxy.getall("a")).is_equal_to(["1"])
    asserts.assert_that(hasattr(proxy, "add")).is_false()
    asserts.assert_that(hasattr(proxy, "getall")).is_true()


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_request_state_is_its_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_properties_are_the_class_s))
    _suite.addTest(unittest.FunctionTestCase(_test_an_instance_attribute_comes_before_a_method))
    _suite.addTest(unittest.FunctionTestCase(_test_vgs_request_extends_request))
    _suite.addTest(unittest.FunctionTestCase(_test_response_state_and_properties))
    _suite.addTest(unittest.FunctionTestCase(_test_the_class_attributes_cannot_be_set))
    _suite.addTest(unittest.FunctionTestCase(_test_dict_order))
    _suite.addTest(unittest.FunctionTestCase(_test_removed_state_is_a_missing_attribute))
    _suite.addTest(unittest.FunctionTestCase(_test_explicit_protocol_calls))
    _suite.addTest(unittest.FunctionTestCase(_test_getattr_hook_and_super))
    _suite.addTest(unittest.FunctionTestCase(_test_multidict_methods_are_the_class_s))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
