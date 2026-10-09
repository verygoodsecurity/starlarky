"""Script API for guarded-route telemetry events.

``events.record(name, fields=None)`` never raises; invalid events are dropped and counted.

Field values are at most 64 printable ASCII characters. Digit budgets limit numeric data in
logs: at most 8 ASCII digits per value, 11 per accepted event, and 24 across all accepted
events in one execution. These rules block plain card numbers; deliberate letter encoding is
not prevented. Route and filter identifiers are added by the platform, so scripts should not
need long numeric fields.

``RecordedEvents`` (binding ``__vgs_recorded_events__``) is single-threaded per script execution.
"""

load("@stdlib//larky", larky="larky")
load("@vgs//native_events", _native="native_events")


def record(name, fields=None):
    _native.record(name, fields)


events = larky.struct(
    record=record
)
