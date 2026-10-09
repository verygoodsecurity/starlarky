load("@stdlib//larky", larky="larky")
load("@vgs//native_events", _native="native_events")


def record(name, fields=None):
    _native.record(name, fields)


events = larky.struct(
    record=record
)
