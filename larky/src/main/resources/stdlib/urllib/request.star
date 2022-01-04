# Request -- An object that encapsulates the state of a request.  The
# state can be as simple as the URL.  It can also include extra HTTP
# headers, e.g. a User-Agent.

# Direct copy from: https://github.com/python/cpython/blob/3.9/Lib/urllib/request.py
#

load("@stdlib/larky", "larky")


def _get_method(self):
    """Return a string indicating the HTTP request method."""
    default_method = "POST" if self._data != None else "GET"
    return getattr(self, '_method', default_method)


def _get_full_url(self):
    return self._full_url


def _set_full_url(self, url):
    self._full_url = url


def _set_proxy(self, host, type):
    if self.type == 'https' and not self._tunnel_host:
        self._tunnel_host = self.host
    else:
        self.type= type
        self.selector = self._full_url
    self.host = host


def _has_proxy(self):
    return self.selector == self._full_url


def _add_header(self, key, val):
    # original implementation uses key.capitalize(), however, we will not modify the keys.
    self._headers[key] = val


def _add_headers(self, headers):
    for k, v in headers.items():
        _add_header(self, k, v)

def _add_unredirected_header(self, key, val):
    # original implementation uses key.capitalize(), however, we will not modify the keys.
    self.unredirected_hdrs[key] = val


def _has_header(self, header_name):
    return (header_name in self._headers or
            header_name in self.unredirected_hdrs)


def _get_header(self, header_name, default=None):
    return self._headers.get(
        header_name,
        self.unredirected_hdrs.get(header_name, default))


def _remove_header(self, header_name):
    self._headers.pop(header_name, None)
    self.unredirected_hdrs.pop(header_name, None)


# property (setter)
def _set_headers(self, headers):
    self._headers = {}
    _add_headers(self, headers)


# property (getter)
def _get_headers(self):
    return self._headers


def _header_items(self):
    hdrs = {}
    hdrs.update(self.unredirected_hdrs)
    hdrs.update(self._headers)
    return list(hdrs.items())


# property (getter)
def _get_data(self):
    return self._data


# property (setter)
def _set_data(self, val):
    self._data = val


def _Request(**kwargs):
    return Request(
        url=kwargs.get('url', None),
        data=kwargs.get('data',None),
        headers=kwargs.get('headers', {}),
        method=kwargs.get('method', None)
    )


def Request(url=None, data=None, headers={},
                  origin_req_host=None, unverifiable=False,
                  method=None):


    self = larky.mutablestruct(
        _full_url=url,
        _data=data,
        _headers={},
        unredirected_hdrs={},
        origin_req_host=origin_req_host,
        unverifiable=unverifiable,
        _method=method)
    _set_headers(self, headers)

    klass = larky.mutablestruct(
        __name__='Request',
        __class__=Request,
        data = larky.property(
            larky.partial(_get_data, self),
            larky.partial(_set_data, self),
        ),
        body = larky.property(                  # synonymous to `self.data`
            larky.partial(_get_data, self),
            larky.partial(_set_data, self),
        ),
        method = larky.property(
            larky.partial(_get_method, self),
        ),
        get_method = larky.partial(_get_method, self),
        get_full_url = larky.partial(_get_full_url, self),
        url = larky.property(
            larky.partial(_get_full_url, self),
            larky.partial(_set_full_url, self),
        ),
        set_proxy = larky.partial(_set_proxy, self),
        has_proxy = larky.partial(_has_proxy, self),
        add_header = larky.partial(_add_header, self),
        add_unredirected_header = larky.partial(_add_unredirected_header, self),
        has_header = larky.partial(_has_header, self),
        get_header = larky.partial(_get_header, self),
        remove_header = larky.partial(_remove_header, self),
        header_items = larky.partial(_header_items, self),
        headers = larky.property(
            larky.partial(_get_headers, self),
            larky.partial(_set_headers, self),
        ),
    )
    return klass


request = larky.struct(
    Request=Request,
    _Request=_Request
)

# class Request:
#
#     def __init__(self, url, data=None, headers={},
#                  origin_req_host=None, unverifiable=False,
#                  method=None):
#         self.full_url = url
#         self.headers = {}
#         self.unredirected_hdrs = {}
#         self._data = None
#         self.data = data
#         self._tunnel_host = None
#         for key, value in headers.items():
#             self.add_header(key, value)
#         if origin_req_host is None:
#             origin_req_host = request_host(self)
#         self.origin_req_host = origin_req_host
#         self.unverifiable = unverifiable
#         if method:
#             self.method = method
#
#     @property
#     def full_url(self):
#         if self.fragment:
#             return '{}#{}'.format(self._full_url, self.fragment)
#         return self._full_url
#
#     @full_url.setter
#     def full_url(self, url):
#         # unwrap('<URL:type://host/path>') --> 'type://host/path'
#         self._full_url = unwrap(url)
#         self._full_url, self.fragment = _splittag(self._full_url)
#         self._parse()
#
#     @full_url.deleter
#     def full_url(self):
#         self._full_url = None
#         self.fragment = None
#         self.selector = ''
#
#     @property
#     def data(self):
#         return self._data
#
#     @data.setter
#     def data(self, data):
#         if data != self._data:
#             self._data = data
#             # issue 16464
#             # if we change data we need to remove content-length header
#             # (cause it's most probably calculated for previous value)
#             if self.has_header("Content-length"):
#                 self.remove_header("Content-length")
#
#     @data.deleter
#     def data(self):
#         self.data = None
#
#     def _parse(self):
#         self.type, rest = _splittype(self._full_url)
#         if self.type is None:
#             raise ValueError("unknown url type: %r" % self.full_url)
#         self.host, self.selector = _splithost(rest)
#         if self.host:
#             self.host = unquote(self.host)
#
#     def get_method(self):
#         """Return a string indicating the HTTP request method."""
#         default_method = "POST" if self.data is not None else "GET"
#         return getattr(self, 'method', default_method)
#
#     def get_full_url(self):
#         return self.full_url
#
#     def set_proxy(self, host, type):
#         if self.type == 'https' and not self._tunnel_host:
#             self._tunnel_host = self.host
#         else:
#             self.type= type
#             self.selector = self.full_url
#         self.host = host
#
#     def has_proxy(self):
#         return self.selector == self.full_url
#
#     def add_header(self, key, val):
#         # useful for something like authentication
#         self.headers[key.capitalize()] = val
#
#     def add_unredirected_header(self, key, val):
#         # will not be added to a redirected request
#         self.unredirected_hdrs[key.capitalize()] = val
#
#     def has_header(self, header_name):
#         return (header_name in self.headers or
#                 header_name in self.unredirected_hdrs)
#
#     def get_header(self, header_name, default=None):
#         return self.headers.get(
#             header_name,
#             self.unredirected_hdrs.get(header_name, default))
#
#     def remove_header(self, header_name):
#         self.headers.pop(header_name, None)
#         self.unredirected_hdrs.pop(header_name, None)
#
#     def header_items(self):
#         hdrs = {**self.unredirected_hdrs, **self.headers}
#         return list(hdrs.items())