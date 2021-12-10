load("@vgs//vault", _vault="vault")
load("@stdlib/larky", "larky")

"""
generates an alias for value
"""
def _redact(value, storage=None, format=None, tags=[]):
    return _vault.redact(value, storage, format, tags)

"""
reveals aliased value
"""
def _reveal(value, storage=None):
    return _vault.reveal(value, storage)

vault = larky.struct(
  redact = _redact,
  reveal = _reveal)