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
# Ported from https://github.com/Mastercard/oauth1-signer-python/blob/main/oauth1/oauth.py
#

load("@stdlib//json", json="json")
load("@stdlib//larky", larky="larky")
load("@vendor//Crypto/Signature", pkcs1_15="pkcs1_15")
load("@vendor//Crypto/Hash", SHA256="SHA256")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//MasterCard/oauth1/coreutils", util='coreutils')

def OAuth():
    self = larky.mutablestruct(__class__='OAuth')
    self.EMPTY_STRING = ""

    def get_authorization_header(uri, method, payload, consumer_key, signing_key, timestamp):
        oauth_parameters = self.get_oauth_parameters(uri, method, payload, consumer_key, signing_key, timestamp)

        # Get the updated base parameters dict
        oauth_base_parameters_dict = oauth_parameters.get_base_parameters_dict()

        # Generate the header value for OAuth Header
        oauth_key = oauth_parameters.OAUTH_KEY + " " + ",".join(
            [str(key) + "=\"" + str(value) + "\"" for (key, value) in oauth_base_parameters_dict.items()])
        return oauth_key
    self.get_authorization_header = get_authorization_header

    def get_oauth_parameters(uri, method, payload, consumer_key, signing_key, timestamp):
        # Get all the base parameters such as nonce and timestamp
        oauth_parameters = OAuthParameters()
        oauth_parameters.set_oauth_consumer_key(consumer_key)
        oauth_parameters.set_oauth_nonce(util.get_nonce())
        #oauth_parameters.set_oauth_timestamp(util.get_timestamp())
        oauth_parameters.set_oauth_timestamp(timestamp)
        oauth_parameters.set_oauth_signature_method("RSA-SHA256")
        oauth_parameters.set_oauth_version("1.0")

        payload_str = json.dumps(payload) if type(payload) == dict or type(payload) == list else payload
        if not payload_str:
            # If the request does not have an entity body, the hash should be taken over the empty string
            payload_str = OAuth().EMPTY_STRING

        encoded_hash = util.base64_encode(util.sha256_encode(payload_str))
        oauth_parameters.set_oauth_body_hash(encoded_hash)

        # Get the base string
        base_string = self.get_base_string(uri, method, oauth_parameters.get_base_parameters_dict())

        # Sign the base string using the private key
        signature = self.sign_message(base_string, signing_key)

        # Set the signature in the Base parameters
        oauth_parameters.set_oauth_signature(util.percent_encode(signature))

        return oauth_parameters
    self.get_oauth_parameters = get_oauth_parameters

    def copy_dict(orig):
        new_dict = {}
        for key in orig.keys():
            new_dict[key] = orig[key]
        return new_dict

    def get_base_string(url, method, oauth_parameters):
        #merge_params = oauth_parameters.copy()
        merge_params = copy_dict(oauth_parameters)
        return "{}&{}&{}".format(method.upper(),
                                 util.percent_encode(util.normalize_url(url)),
                                 util.percent_encode(util.normalize_params(url, merge_params)))
    self.get_base_string = get_base_string

    def sign_message(message, signing_key):
        #    Signs the message using the private signing key
        #sign = crypto.sign(signing_key, message.encode("utf-8"), 'SHA256')
        key = RSA.import_key(signing_key)
        h = SHA256.new(bytes(message, 'utf-8'))
        sign = pkcs1_15.new(key).sign(h)
        return util.base64_encode(sign)
    self.sign_message = sign_message

    return self

def OAuthParameters():
    """
    Stores the OAuth parameters required to generate the Base String and Headers constants
    """
    self = larky.mutablestruct(__class__='OAuthParameters')

    self.OAUTH_BODY_HASH_KEY = "oauth_body_hash"
    self.OAUTH_CONSUMER_KEY = "oauth_consumer_key"
    self.OAUTH_NONCE_KEY = "oauth_nonce"
    self.OAUTH_KEY = "OAuth"
    self.AUTHORIZATION = "Authorization"
    self.OAUTH_SIGNATURE_KEY = "oauth_signature"
    self.OAUTH_SIGNATURE_METHOD_KEY = "oauth_signature_method"
    self.OAUTH_TIMESTAMP_KEY = "oauth_timestamp"
    self.OAUTH_VERSION = "oauth_version"

    def __init__():
        self.base_parameters = {}
    self.__init__ = __init__
    __init__()

    def put(key, value):
        self.base_parameters[key] = value
    self.put = put

    def get(key):
        return self.base_parameters[key]
    self.get = get

    def set_oauth_consumer_key(consumer_key):
        self.put(self.OAUTH_CONSUMER_KEY, consumer_key)
    self.set_oauth_consumer_key = set_oauth_consumer_key

    def get_oauth_consumer_key():
        return self.get(self.OAUTH_CONSUMER_KEY)
    self.get_oauth_consumer_key = get_oauth_consumer_key

    def set_oauth_nonce(oauth_nonce):
        self.put(self.OAUTH_NONCE_KEY, oauth_nonce)
    self.set_oauth_nonce = set_oauth_nonce

    def get_oauth_nonce():
        return self.get(OAuthParameters.OAUTH_NONCE_KEY)
    self.get_oauth_nonce = get_oauth_nonce

    def set_oauth_timestamp(timestamp):
        self.put(self.OAUTH_TIMESTAMP_KEY, timestamp)
    self.set_oauth_timestamp = set_oauth_timestamp

    def get_oauth_timestamp():
        return self.get(self.OAUTH_TIMESTAMP_KEY)
    self.get_oauth_timestamp = get_oauth_timestamp

    def set_oauth_signature_method(signature_method):
        self.put(self.OAUTH_SIGNATURE_METHOD_KEY, signature_method)
    self.set_oauth_signature_method = set_oauth_signature_method

    def get_oauth_signature_method():
        return self.get(self.OAUTH_SIGNATURE_METHOD_KEY)
    self.get_oauth_signature_method = get_oauth_signature_method

    def set_oauth_signature(signature):
        self.put(self.OAUTH_SIGNATURE_KEY, signature)
    self.set_oauth_signature = set_oauth_signature

    def get_oauth_signature():
        return self.get(self.OAUTH_SIGNATURE_KEY)
    self.get_oauth_signature = get_oauth_signature

    def set_oauth_body_hash(body_hash):
        self.put(self.OAUTH_BODY_HASH_KEY, body_hash)
    self.set_oauth_body_hash = set_oauth_body_hash

    def get_oauth_body_hash():
        return self.get(self.OAUTH_BODY_HASH_KEY)
    self.get_oauth_body_hash = get_oauth_body_hash

    def set_oauth_version(version):
        self.put(self.OAUTH_VERSION, version)
    self.set_oauth_version = set_oauth_version

    def get_oauth_version():
        return self.get(self.OAUTH_VERSION)
    self.get_oauth_version = get_oauth_version

    def get_base_parameters_dict():
        return self.base_parameters
    self.get_base_parameters_dict = get_base_parameters_dict

    return self