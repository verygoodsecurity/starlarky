"""Registration facilities for DOM. This module should not be used
directly. Instead, the functions getDOMImplementation and
registerDOMImplementation should be imported from xml.dom."""

load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@vendor//option/result", Result="Result", Error="Error")

# TODO(mahmoudimus): load these directly instead of lazily
well_known_implementations = {
    "minidom": "xml.dom.minidom",
    "4DOM": "xml.dom.DOMImplementation",
}

# DOM implementations not officially registered should register
# themselves with their

# TODO(mahmoudimus): in larky this won't work since this becomes immutable once
#  larky is done importing/loading this file *unless* we expose a specific
#  global *mutable* cache
registered = {}


def registerDOMImplementation(name, factory):
    """registerDOMImplementation(name, factory)
    Register the factory function with the name. The factory function
    should return an object which implements the DOMImplementation
    interface. The factory function can either return the same object,
    or a new one (e.g. if that implementation supports some
    customization)."""

    registered[name] = factory


def _good_enough(dom, features):
    "_good_enough(dom, features) -> Return 1 if the dom offers the features"
    for f, v in features:
        if not dom.hasFeature(f, v):
            return False
    return True


def getDOMImplementation(name=None, features=()):
    """getDOMImplementation(name = None, features = ()) -> DOM implementation.

    Return a suitable DOM implementation. The name is either
    well-known, the module name of a DOM implementation, or None. If
    it is not None, imports the corresponding module and returns
    DOMImplementation object if the import succeeds.

    If name is not given, consider the available implementations to
    find one with the required feature set. If no implementation can
    be found, raise an ImportError. The features list must be a sequence
    of (feature, version) pairs which are passed to hasFeature."""


    mod = well_known_implementations.get(name)
    if mod:
        return mod.getDOMImplementation()
    elif name:
        return registered[name]()

    # User did not specify a name, try implementations in arbitrary
    # order, returning the one that has the required features
    if builtins.isinstance(features, str):
        features = _parse_feature_string(features)
    for creator in registered.values():
        dom = creator()
        if _good_enough(dom, features):
            return dom

    for creator in well_known_implementations.keys():
        domres = Result.Ok(creator).map(getDOMImplementation)
        if domres.is_err:  # typically ImportError, or AttributeError
            continue
        dom = domres.unwrap()
        if _good_enough(dom, features):
            return dom

    fail("ImportError: no suitable DOM implementation found")


def _parse_feature_string(s):
    features = []
    parts = [part for part in s.split(" ") if part]
    i = 0
    length = len(parts)
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if i >= length:
            break
        feature = parts[i]
        if feature[0] in iter("0123456789"):
            fail("ValueError: " + "bad feature name: %r" % (feature,))
        i = i + 1
        version = None
        if i < length:
            v = parts[i]
            if v[0] in iter("0123456789"):
                i = i + 1
                version = v
        features.append((feature, version))
    return tuple(features)


domreg = larky.struct(
    __name__='domreg',
    well_known_implementations=well_known_implementations,
    registered=registered,
    registerDOMImplementation=registerDOMImplementation,
    _good_enough=_good_enough,
    getDOMImplementation=getDOMImplementation,
    _parse_feature_string=_parse_feature_string,)