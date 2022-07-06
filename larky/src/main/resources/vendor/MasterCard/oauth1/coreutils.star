
# -*- coding: utf-8 -*-
#
# Copyright 2019-2021 Mastercard
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification, are
# permitted provided that the following conditions are met:
#
# Redistributions of source code must retain the above copyright notice, this list of
# conditions and the following disclaimer.
# Redistributions in binary form must reproduce the above copyright notice, this list of
# conditions and the following disclaimer in the documentation and/or other materials
# provided with the distribution.
# Neither the name of the MasterCard International Incorporated nor the names of its
# contributors may be used to endorse or promote products derived from this software
# without specific prior written permission.
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
# EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
# OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
# SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
# TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
# OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
# IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
# IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
# Ported from https://github.com/Mastercard/oauth1-signer-python/blob/main/oauth1/coreutils.py
#

"""
Utility file having common functions
"""

load("@stdlib//base64", base64="base64")
load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky")
load("@stdlib//urllib/parse", parse="parse")
load("@vendor//Crypto/Random", Random="Random")

def normalize_params(url, params):
    """
    Combines the query parameters of url and extra params into a single queryString.
    All the query string parameters are lexicographically sorted
    """
    # parse the url
    p = parse.urlparse(url)

    # Get the query list
    qs_list = parse.parse_qsl(p.query, keep_blank_values=True)
    must_encode = False if p.query == parse.unquote(p.query) else True
    if params == None:
        combined_list = qs_list
    else:
        # Needs to be encoded before sorting
        combined_list = [encode_pair(must_encode, key, value) for (key, value) in list(qs_list)]
        combined_list += params.items()

    encoded_list = ["%s=%s" % (key, value) for (key, value) in combined_list]
    sorted_list = sorted(encoded_list, key=lambda x: x)

    return "&".join(sorted_list)


def encode_pair(must_encode, key, value):
    encoded_key = percent_encode(key) if must_encode else key.replace(' ', '+')
    value = value if builtins.isinstance(value, bytes) else str(value)
    encoded_value = percent_encode(value) if must_encode else value.replace(' ', '+')
    return encoded_key, encoded_value


def normalize_url(url):
    """
    Removes the query parameters from the URL
    """
    p = parse.urlparse(url)

    # netloc should be lowercase
    netloc = p.netloc.lower()
    if p.scheme == "http":
        if netloc.endswith(":80"):
            netloc = netloc[:-3]

    elif p.scheme == "https" and netloc.endswith(":443"):
        netloc = netloc[:-4]

    # add a '/' at the end of the netloc if there in no path
    if not p.path:
        netloc = netloc + "/"

    return "{}://{}{}".format(p.scheme, netloc, p.path)


def sha256_encode(text):
    """
    Returns the digest of SHA-256 of the text
    """
    _hash = hashlib.sha256
    if type(text) == str:
        return _hash(text.encode('utf8')).digest()
    elif type(text) == bytes:
        return _hash(text).digest()
    elif not text:
        # Generally for calls where the payload is empty. Eg: get calls
        # Fix for AttributeError: 'NoneType' object has no attribute 'encode'
        return _hash(bytes("",'utf8')).digest()
    else:
        return _hash(bytes(str(text), 'utf-8')).digest()


def base64_encode(text):
    """
    Base64 encodes the given input
    """
    if not builtins.isinstance(text, (bytes, bytearray)):
        text = bytes(text, 'utf-8')
    encode = base64.b64encode(text)
    return encode.decode('ascii')


def percent_encode(text):
    """
    Percent encodes a string as per https://tools.ietf.org/html/rfc3986
    """
    if text == None:
        return ''
    text = bytes(text, 'utf-8') if builtins.isinstance(text, str) else text
    text = parse.quote(text, safe=b'~')
    return text.replace('+', '%20').replace('*', '%2A').replace('%7E', '~')


def get_nonce(length=16):
    """
    Returns a random string of length=@length
    """
    characters = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'
    charlen = len(characters)
    return "".join([characters[Random.random.randint(0, charlen - 1)] for _ in range(0, length)])
    

coreutils = larky.struct(
    __name__="coreutils",
    get_nonce =get_nonce,
    percent_encode=percent_encode,
    base64_encode=base64_encode,
    sha256_encode=sha256_encode,
    normalize_url=normalize_url,
    encode_pair=encode_pair,
    normalize_params=normalize_params)