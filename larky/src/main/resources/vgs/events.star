"""Script API for guarded-route telemetry events.

``events.record(name, fields=None)`` never raises; invalid events are dropped and counted.

Field values are at most 64 printable ASCII characters. Each value may contain at most 8 ASCII
digits; each accepted event may contain at most 11 digits across its field values. No single
script execution's recorded events contain more than 11 digits in total, so they cannot carry a
card number in plain digits. This does not prevent a script from leaking data across many
executions or by encoding it in letters; only trusted (VGS template) scripts should run on
guarded routes.

``RecordedEvents`` (binding ``__vgs_recorded_events__``) is single-threaded per script execution.
"""

load("@stdlib//larky", larky="larky")
load("@vgs//native_events", _native="native_events")


def record(name, fields=None):
    _native.record(name, fields)


events = larky.struct(
    record=record
)
