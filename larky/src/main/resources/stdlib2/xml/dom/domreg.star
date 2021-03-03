def registerDOMImplementation(name, factory):
    """
    registerDOMImplementation(name, factory)

        Register the factory function with the name. The factory function
        should return an object which implements the DOMImplementation
        interface. The factory function can either return the same object,
        or a new one (e.g. if that implementation supports some
        customization).
    """
def _good_enough(dom, features):
    """
    _good_enough(dom, features) -> Return 1 if the dom offers the features
    """
def getDOMImplementation(name=None, features=()):
    """
    getDOMImplementation(name = None, features = ()) -> DOM implementation.

        Return a suitable DOM implementation. The name is either
        well-known, the module name of a DOM implementation, or None. If
        it is not None, imports the corresponding module and returns
        DOMImplementation object if the import succeeds.

        If name is not given, consider the available implementations to
        find one with the required feature set. If no implementation can
        be found, raise an ImportError. The features list must be a sequence
        of (feature, version) pairs which are passed to hasFeature.
    """
def _parse_feature_string(s):
    """
    0123456789
    """
