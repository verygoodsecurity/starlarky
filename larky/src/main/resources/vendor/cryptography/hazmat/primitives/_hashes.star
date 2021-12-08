# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
#
# This file is non-std to avoid the circular imports that can be handled
# in python, but cannot be handled in Larky
#
# This exists to break an import cycle. These classes are normally accessible
# from the hashes module.
load("@stdlib//larky", larky="larky")
load("@stdlib//builtins", builtins="builtins")
load("@vendor//cryptography/utils", utils="utils")


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
    # print("HashAlgorithm:", kwargs)
    return lambda : _HashAlgorithm(kwargs['name'], kwargs['digest_size'], kwargs['block_size'])


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


def Hash(algorithm, backend = None, ctx = None):
    # type: (HashAlgorithm, Optional[Backend], Optional["HashContext"]) -> Hash
    self = HashContext(algorithm)
    self.__name__ = 'Hash'
    self.__class__ = Hash
    def __init__(
        algorithm, # type: HashAlgorithm,
        backend,   # type: Optional[Backend] = None,
        ctx        # type: Optional["HashContext"] = None,
    ):
        if not backend:
            fail("Backend not found")
            # backend = backends._get_backend(backend)
        self._algorithm = algorithm
        self._backend = backend

        if ctx == None:
            self._ctx = self._backend.create_hash_ctx(self.algorithm)
        else:
            self._ctx = ctx
        return self
    self = __init__(algorithm, backend, ctx)

    def update(data):
        # type: (bytes) -> None
        if self._ctx == None:
            fail("AlreadyFinalized: Context was already finalized.")
        utils._check_byteslike("data", data)
        self._ctx.update(data)
    self.update = update

    def copy():
        # type: () -> "Hash"
        if self._ctx == None:
            fail("AlreadyFinalized: Context was already finalized.")
        return Hash(
            self.algorithm, backend=self._backend, ctx=self._ctx.copy()
        )
    self.copy = copy

    def finalize():
        # type: () -> bytes
        if self._ctx == None:
            fail("AlreadyFinalized: Context was already finalized.")
        digest = self._ctx.finalize()
        self._ctx = None
        return digest
    self.finalize = finalize
    return self

hashes = larky.struct(
    __name__='_hashes',
    HashContext=HashContext,
    Hash=Hash,
    HashAlgorithm=HashAlgorithm,
    _HashAlgorithm=_HashAlgorithm
)