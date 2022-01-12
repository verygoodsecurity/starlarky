load("@vgs//vault", _vault="vault")
load("@stdlib/larky", "larky")

def redact(value, storage=None, format=None, tags=[]):
    """
    generates an alias for value
    """
    return _vault.redact(value, storage, format, tags)


def reveal(value, storage=None):
    """
    reveals aliased value
    """
    return _vault.reveal(value, storage)


vault = larky.struct(
  redact = redact,
  reveal = reveal)