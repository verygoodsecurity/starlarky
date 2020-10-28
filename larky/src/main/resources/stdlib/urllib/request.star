# Request -- An object that encapsulates the state of a request.  The
# state can be as simple as the URL.  It can also include extra HTTP
# headers, e.g. a User-Agent.

# Direct copy from: https://github.com/python/cpython/blob/3.9/Lib/urllib/request.py
#
#

def _make(elements = None):
    """Creates a new Request.

    Args:
      elements: Optional sequence to construct the set out of.

    Returns:
      A Request-like object containing the passed in values.
    """

    elements = elements if elements else []
    return struct(_values = {e: None for e in elements})


Request = struct(
    make = _make,
    full_url = _full_url,
    data = _data,
    get_method = _get_method,
    get_full_url = _get_full_url,
    set_proxy = _set_proxy,
    has_proxy = _has_proxy,
    add_header = _add_header,
    add_unredirected_header = _add_unredirected_header,
    has_header = _has_header,
    get_header = _get_header,
    remove_header = _remove_header,
    header_items = _header_items
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