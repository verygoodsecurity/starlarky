load("@vendor//_multidict_core", CIMultiDict="CIMultiDict")
load("@stdlib//jmultidict", _multidict="jmultidict")


def VGSCIMultiDict(*args, **kwargs):
    """VGS specific CI MultiDict with support for duplicate case-insensitive keys.

    str() lists the lines as {"key": repr(value), ...}.
    """
    return _multidict.new_multidict(VGSCIMultiDict, "VGSCIMultiDict", True, args, kwargs, dict_str=True)
