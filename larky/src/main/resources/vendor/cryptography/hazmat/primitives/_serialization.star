# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//enum", enum="enum")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vendor//option/result", Error="Error")

# This exists to break an import cycle. These classes are normally accessible
# from the serialization module.

Encoding = enum.Enum('Encoding', dict(
    PEM = "PEM",
    DER = "DER",
    OpenSSH = "OpenSSH",
    Raw = "Raw",
    X962 = "ANSI X9.62",
    SMIME = "S/MIME",
).items())



PrivateFormat = enum.Enum('PrivateFormat', dict(
    PKCS8 = "PKCS8",
    TraditionalOpenSSL = "TraditionalOpenSSL",
    Raw = "Raw",
    OpenSSH = "OpenSSH",
).items())


PublicFormat = enum.Enum('PrivateFormat', dict(
    SubjectPublicKeyInfo = "X.509 subjectPublicKeyInfo with PKCS#1",
    PKCS1 = "Raw PKCS#1",
    OpenSSH = "OpenSSH",
    Raw = "Raw",
    CompressedPoint = "X9.62 Compressed Point",
    UncompressedPoint = "X9.62 Uncompressed Point",
).items())


ParameterFormat = enum.Enum('ParameterFormat', dict(PKCS3="PKCS3",).items())


def KeySerializationEncryption():
    return larky.mutablestruct(
        __name__="KeySerializationEncryption",
        __class__=KeySerializationEncryption)


def BestAvailableEncryption(password):
    # type: (bytes) -> "BestAvailableEncryption"
    self = KeySerializationEncryption()
    self.__name__ = 'BestAvailableEncryption'
    self.__class__ = BestAvailableEncryption

    def __init__(password):
        # type: (bytes) -> Any
        if not types.is_bytes(password) or len(password) == 0:
            fail("ValueError: Password must be 1 or more bytes.")
        self.password = password
        return self
    self = __init__(password)
    return self


def NoEncryption():
    self = KeySerializationEncryption()
    self.__name__ = 'NoEncryption'
    self.__class__ = NoEncryption
    return self


serialization = larky.struct(
    __name__='_serialization',
    Encoding=Encoding,
    PrivateFormat=PrivateFormat,
    PublicFormat=PublicFormat,
    ParameterFormat=ParameterFormat,
    KeySerializationEncryption=KeySerializationEncryption,
    BestAvailableEncryption=BestAvailableEncryption,
    NoEncryption=NoEncryption,
)