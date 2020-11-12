from collections import namedtuple

def struct(**kwargs):
    return namedtuple('struct', ' '.join(kwargs.keys()))(**kwargs)