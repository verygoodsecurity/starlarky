# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
#
# This file is non std to avoid the circular imports that can be handled
# in python, but cannot be handled in Larky
load("@stdlib//larky", larky="larky")
load("@stdlib//builtins", builtins="builtins")


def _HashAlgorithm(name, digest_size, block_size):
    # type: (str, int, Optional[int]) -> _HashAlgorithm
    """
    :param name: A string naming this algorithm (e.g. "sha256", "md5").
    :param digest_size: The size of the resulting digest in bytes.
    :param block_size: The internal block size of the hash function,
                        or None if the hash function does not use blocks
                        internally (e.g. SHA3).
    """
    return larky.struct(
        __name__='HashAlgorithm',
        __class__=HashAlgorithm,
        name=name,
        digest_size=digest_size,
        block_size=block_size,
    )


def HashAlgorithm(**kwargs):
    return lambda: _HashAlgorithm(**kwargs)


def HashContext(algorithm):
    # type: (HashAlgorithm) -> HashAlgorithm
    """
    :param algorithm: A HashAlgorithm that will be used by this context.
    """
    if not builtins.isinstance(algorithm, HashAlgorithm):
        fail("Expected instance of hashes.HashAlgorithm.")

    self = larky.mutablestruct(
        __name__='HashContext',
        __class__=HashContext,
        algorithm=larky.property(lambda: algorithm),
    )

    def update(data):
        # type: (bytes) -> None
        """
        Processes the provided bytes through the hash.
        """
    self.update = update

    def finalize():
        # type: () -> bytes
        """
        Finalizes the hash context and returns the hash digest as bytes.
        """
    self.finalize = finalize

    def copy():
        # type: () -> "HashContext"
        """
        Return a HashContext that is a copy of the current context.
        """
    self.copy = copy
    return self