# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//larky", larky="larky")
load("@vendor//cryptography/hazmat/primitives/_serialization", _Encoding="Encoding")
load("@vendor//cryptography/hazmat/primitives/serialization/base",
     # load_der_parameters,
     # load_der_private_key,
     # load_der_public_key,
     # load_pem_parameters,
     # load_pem_private_key,
     # load_pem_public_key,
     "load_der_parameters",
     "load_der_private_key",
     "load_der_public_key",
     "load_pem_parameters",
     "load_pem_private_key",
     "load_pem_public_key",
     )

#
# load("@vendor//cryptography/hazmat/primitives/serialization/ssh",
#      # load_ssh_private_key,
#      # load_ssh_public_key,
#      "load_ssh_private_key",
#      "load_ssh_public_key",
#      )


Encoding = _Encoding


serialization = larky.struct(
    __name__='serialization',
    Encoding=Encoding,
    load_der_parameters=load_der_parameters,
    load_der_private_key=load_der_private_key,
    load_der_public_key=load_der_public_key,
    load_pem_parameters=load_pem_parameters,
    load_pem_private_key=load_pem_private_key,
    load_pem_public_key=load_pem_public_key,
)