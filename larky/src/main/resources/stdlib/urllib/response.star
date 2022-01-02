# Response -- An object that encapsulates the state of a response.


load("@stdlib/larky", "larky")


def _add_header(self, key, val):
    # original implementation uses key.capitalize(), however, we will not modify the keys.
    self._headers[key] = val


def _has_header(self, header_name):
    return header_name in self._headers


def _get_header(self, header_name, default=None):
    return self._headers.get(header_name, default)


def _remove_header(self, header_name):
    self._headers.pop(header_name, None)


# property (setter)
def _set_headers(self, headers):
    self._headers = {}
    _add_headers(self, headers)


# property (getter)
def _get_headers(self):
    return self._headers


def _header_items(self):
    return list(self._headers.items())


# property (getter)
def _get_data(self):
    return self._data


# property (setter)
def _set_data(self, val):
    self._data = val


# property (getter)
def _get_status(self):
    return self._status


# property (setter)
def _set_status(self, status):
    self._status = status


def Response(data=None, status=200, headers={}):

    self = larky.mutablestruct(
        _data=data,
        _status=status,
        _headers={})
    _set_headers(self, headers)

    # print(_impl_function_name(_AssertionBuilder), " - ")
    klass = larky.mutablestruct(
        data = larky.property(
            larky.partial(_get_data, self),
            larky.partial(_set_data, self),
        ),
        body = larky.property(                  # synonymous to `self.data`
            larky.partial(_get_data, self),
            larky.partial(_set_data, self),
        ),
        status = larky.property(
            larky.partial(_get_status, self),
            larky.partial(_set_status, self),
        ),
        get_status = larky.partial(_get_status, self),
        add_header = larky.partial(_add_header, self),
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
