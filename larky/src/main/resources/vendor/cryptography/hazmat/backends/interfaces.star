# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//larky", larky="larky")

# TODO(mahmoudimus): Implement me
# https://github.com/pyca/cryptography/blob/main/src/cryptography/hazmat/backends/interfaces.py
#
# But honestly, is this even required any more?
# See:
# - https://github.com/pyca/cryptography/issues/6499
# - https://github.com/pyca/cryptography/pull/6518/files#diff-55df9d2f8b8d40391d9d451a1e8155292465d48c52e78dd34655be834cd3dc9fR191-R203
#
# What happened to the backend argument?
# --------------------------------------
# ``cryptography`` stopped requiring the use of ``backend`` arguments in
# version 3.1 and deprecated their use in version 36.0. If you are on an older
# version that requires these arguments please view the appropriate documentation
# version or upgrade to the latest release.
#
# Note that for forward compatibility ``backend`` is still silently accepted by
# functions that previously required it, but it is ignored and no longer
# documented.

interfaces = larky.struct(
    __name__='interfaces'
)