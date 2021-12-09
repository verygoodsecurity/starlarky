# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//larky", larky="larky")
load("@vendor//cryptography/hazmat/backends/pycryptodome", backend="backend")


def default_backend():
    # type: () -> Backend
    return backend()


def _get_backend(backend):
    # type: (typing.Optional[Backend]) -> Backend
    if backend == None:
        return default_backend()
    else:
        return backend


backends = larky.struct(
    __name__='backends',
    _get_backend=_get_backend,
    default_backend=default_backend,
)