# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//enum", enum="enum")
load("@stdlib//larky", larky="larky")
load("@vendor//cryptography/x509/_base", _Version="Version")
load("@vendor//cryptography/hazmat/backends", backends="backends")

load("@vendor//option/result", Error="Error")

_get_backend = backends._get_backend
Version = _Version


def load_pem_x509_certificate(data, backend=None):
    backend = _get_backend(backend)
    return backend.load_pem_x509_certificate(data)


def load_der_x509_certificate(data, backend=None):
    backend = _get_backend(backend)
    return backend.load_der_x509_certificate(data)


def load_pem_x509_csr(data, backend=None):
    backend = _get_backend(backend)
    return backend.load_pem_x509_csr(data)


def load_der_x509_csr(data, backend=None):
    backend = _get_backend(backend)
    return backend.load_der_x509_csr(data)


def load_pem_x509_crl(data, backend=None):
    backend = _get_backend(backend)
    return backend.load_pem_x509_crl(data)


def load_der_x509_crl(data, backend=None):
    backend = _get_backend(backend)
    return backend.load_der_x509_crl(data)


base = larky.struct(
    __name__='base',
    Version=Version,
    load_pem_x509_certificate=load_pem_x509_certificate,
    load_der_x509_certificate=load_der_x509_certificate,
    load_pem_x509_csr=load_pem_x509_csr,
    load_der_x509_csr=load_der_x509_csr,
    load_pem_x509_crl=load_pem_x509_crl,
    load_der_x509_crl=load_der_x509_crl,
)
