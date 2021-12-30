# Response -- An object that encapsulates the state of a response.


load("@stdlib/larky", "larky")


def _add_header(self, key, val):
    # useful for something like authentication
    self._headers[key.capitalize()] = val


def _has_header(self, header_name):
    key = header_name.capitalize()
    return key in self._headers


def _get_header(self, header_name, default=None):
    key = header_name.capitalize()
    return self._headers.get(key, default)


def _remove_header(self, header_name):
    self._headers.pop(header_name, None)


# property (setter)
def _add_headers(self, headers):
    for k, v in headers.items():
        _add_header(self, k, v)


# property (getter)
def _get_headers(self):
    return self._headers


def _header_items(self):
    return list(self._headers.items())


# property (getter)
def _get_data(self):
    return self.data


# property (setter)
def _set_data(self, val):
    self.data = val


# property (getter)
def _get_status(self):
    return self.status


# property (setter)
def _set_status(self, status):
    self.status = status


def Response(data=None, status=200, headers={}):

    self = larky.mutablestruct(
        data=data,
        status=status,
        _headers={})
    _add_headers(self, headers)

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
        add_header = larky.partial(_add_header, self),
        has_header = larky.partial(_has_header, self),
        get_header = larky.partial(_get_header, self),
        remove_header = larky.partial(_remove_header, self),
        header_items = larky.partial(_header_items, self),
        headers = larky.property(
            larky.partial(_get_headers, self),
            larky.partial(_add_headers, self),
        ),
    )
    return klass
