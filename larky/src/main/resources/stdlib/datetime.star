"""Concrete date/time and related types.
See http://www.iana.org/time-zones/repository/tz-link.html for
time zone and DST data sources.
"""

# load("@stdlib/larky", "larky")
# load("@stdlib//builtins","builtins")
# load("@stdlib//types", "types")
# load("@stdlib/jtime", _time = "jtime")

# __all__ = ("date", "datetime", "time", "timedelta", "timezone", "tzinfo",
#            "MINYEAR", "MAXYEAR")

# MINYEAR = 1
# MAXYEAR = 9999
# _MAXORDINAL = 3652059  # date.max.toordinal()

# _DAYS_IN_MONTH = [-1, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]

# def _days_in_month(year, month):
#     # year, month -> number of days in that month in that year.

#     # assert 1 <= month <= 12, month
#     if not 1 <= month <= 12:
#         fail("AssertionError('month must be in 1..12')")
#     if month == 2 and _is_leap(year):
#         return 29
#     return _DAYS_IN_MONTH[month]

# def _is_leap(year):
#     # year -> 1 if leap year, else 0.
#     return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)

# def _check_int_field(value):
#     if types.is_int(value):
#         return value
#     else:
#         fail("TypeError('an integer is required')")

# def _check_date_fields(year, month, day):
#     year = _check_int_field(year)
#     month = _check_int_field(month)
#     day = _check_int_field(day)
#     if not MINYEAR <= year <= MAXYEAR:
#         fail("ValueError('year must be in %d..%d')" % (MINYEAR, MAXYEAR))
#     if not 1 <= month <= 12:
#         fail("ValueError('month must be in 1..12')")
#     dim = _days_in_month(year, month)
#     if not 1 <= day <= dim:
#         fail("ValueError('day must be in 1..%d')" % dim)
#     return year, month, day

# def _check_time_fields(hour, minute, second, microsecond):
#     hour = _check_int_field(hour)
#     minute = _check_int_field(minute)
#     second = _check_int_field(second)
#     microsecond = _check_int_field(microsecond)
#     if not 0 <= hour <= 23:
#         fail("ValueError('hour must be in 0..23, %d')" % hour)
#     if not 0 <= minute <= 59:
#         fail("ValueError('minute must be in 0..59, %d')" % minute)
#     if not 0 <= second <= 59:
#         fail("ValueError('second must be in 0..59, %d')" % second)
#     if not 0 <= microsecond <= 999999:
#         fail("ValueError('second must be in 0..999999, %d')" % microsecond)
#     # if fold not in (0, 1):
#     #     fail("ValueError('fold must be either 0 or 1, %d')" % fold)
#     return hour, minute, second, microsecond

# def _date(year=1, month=1, day=1):
#     """
#     A date object represents a date (year, month and day) in an idealized calendar
#     """

#     self = larky.mutablestruct(__class__='date')

#     def __init__(year=1, month=1, day=1):
#         """Constructor.
#         Arguments:
#         year, month, day (required, base 1)
#         """
#         year, month, day = _check_date_fields(year, month, day)
#         self._year = year
#         self._month = month
#         self._day = day
#         self._hashcode = -1

#     def fromtimestamp(t):
#         "Construct a date from a POSIX timestamp (like time.time())."
#         y, m, d, hh, mm, ss, weekday, jday, dst = _time.localtime(t)
#         return _date(y, m, d)

#     def today():
#         "Construct a date from time.time()."
#         t = _time.time()
#         return fromtimestamp(t)

# def datetime(date):
#     """
#     datetime(year, month, day[, hour[, minute[, second[, microsecond[,tzinfo]]]]])
    
#     The year, month and day arguments are required. tzinfo may be None, or an
#     instance of a tzinfo subclass. The remaining arguments may be ints.
#     """
#     self = larky.mutablestruct(__class__='datetime')

#     def __init__(year, month=None, day=None, hour=0, minute=0, second=0,
#                 microsecond=0, tzinfo=None, *, fold=0):
#         year, month, day = _check_date_fields(year, month, day)
#         hour, minute, second, microsecond, fold = _check_time_fields(hour, minute, second, microsecond, fold)
#         # _check_tzinfo_arg(tzinfo)
#         self = object.__new__(cls)
#         self._year = year
#         self._month = month
#         self._day = day
#         self._hour = hour
#         self._minute = minute
#         self._second = second
#         self._microsecond = microsecond
#         self._tzinfo = tzinfo
#         self._hashcode = -1
#         self._fold = fold
    
#     def _fromtimestamp(cls, t, utc, tz):
#         """Construct a datetime from a POSIX timestamp (like time.time()).
#         A timezone info object may be passed in as well.
#         """
#         frac, t = _math.modf(t)
#         us = round(frac * 1e6)
#         if us >= 1000000:
#             t += 1
#             us -= 1000000
#         elif us < 0:
#             t -= 1
#             us += 1000000

#         converter = _time.gmtime if utc else _time.localtime
#         y, m, d, hh, mm, ss, weekday, jday, dst = converter(t)
#         ss = min(ss, 59)    # clamp out leap seconds if the platform has them
#         result = cls(y, m, d, hh, mm, ss, us, tz)
#         if tz is None:
#             # As of version 2015f max fold in IANA database is
#             # 23 hours at 1969-09-30 13:00:00 in Kwajalein.
#             # Let's probe 24 hours in the past to detect a transition:
#             max_fold_seconds = 24 * 3600

#             # On Windows localtime_s throws an OSError for negative values,
#             # thus we can't perform fold detection for values of time less
#             # than the max time fold. See comments in _datetimemodule's
#             # version of this method for more details.
#             if t < max_fold_seconds and sys.platform.startswith("win"):
#                 return result

#             y, m, d, hh, mm, ss = converter(t - max_fold_seconds)[:6]
#             probe1 = cls(y, m, d, hh, mm, ss, us, tz)
#             trans = result - probe1 - timedelta(0, max_fold_seconds)
#             if trans.days < 0:
#                 y, m, d, hh, mm, ss = converter(t + trans // timedelta(0, 1))[:6]
#                 probe2 = cls(y, m, d, hh, mm, ss, us, tz)
#                 if probe2 == result:
#                     result._fold = 1
#         else:
#             result = tz.fromutc(result)
#         return result

#     def now(cls, tz=None):
#         # Construct a UTC datetime from time.time()
#         t = _time.time()
#         return _fromtimestamp(t, True, None)

# datetime = larky.struct(
#     date=_date
# )
