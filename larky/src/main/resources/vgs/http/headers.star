load("@vendor//multidict", CIMultiDict="CIMultiDict")


def VGSCIMultiDict(*args, **kwargs):
    """VGS specific CI MultiDict with support for duplicate case-insensitive keys."""
    self = CIMultiDict(*args, **kwargs)
    self.__name__ = 'VGSCIMultiDict'
    self.__class__ = VGSCIMultiDict

    def __str__():
        body = ", ".join(["\"{}\": {}".format(k, repr(v)) for k, v in iter(self.items())])
        return "{{{}}}".format(body)
    self.__str__ = __str__
    return self
