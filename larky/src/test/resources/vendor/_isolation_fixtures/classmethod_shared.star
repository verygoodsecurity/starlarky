# A classmethod built at load time and placed in a class that each call creates.
load("@stdlib//types", "types")

def _name(cls):
    return cls.__name__

_CM = classmethod(_name)

def class_name(data):
    C = types.new_class("C", (), {}, lambda ns: ns.update(m=_CM))
    return C().m() + "," + repr(_CM)
