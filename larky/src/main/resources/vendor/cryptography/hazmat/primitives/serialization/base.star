# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//larky", larky="larky")

load("@vendor//cryptography/hazmat/backends", backends="backends")
_get_backend = backends._get_backend


def load_pem_private_key(
    data,
    password,
    backend = None,
):
    backend = _get_backend(backend)
    return backend.load_pem_private_key(data, password)


def load_pem_public_key(
    data, backend = None
):
    backend = _get_backend(backend)
    return backend.load_pem_public_key(data)


def load_pem_parameters(
    data, backend = None
):
    backend = _get_backend(backend)
    return backend.load_pem_parameters(data)


def load_der_private_key(
    data,
    password,
    backend = None,
):
    backend = _get_backend(backend)
    return backend.load_der_private_key(data, password)


def load_der_public_key(
    data, backend = None
):
    backend = _get_backend(backend)
    return backend.load_der_public_key(data)


def load_der_parameters(
    data, backend = None
):
    backend = _get_backend(backend)
    return backend.load_der_parameters(data)


base = larky.struct(
    __name__='base',
    load_pem_private_key=load_pem_private_key,
    load_pem_public_key=load_pem_public_key,
    load_pem_parameters=load_pem_parameters,
    load_der_private_key=load_der_private_key,
    load_der_public_key=load_der_public_key,
    load_der_parameters=load_der_parameters,
)
