def _cmp(x, y):
    """
     date.max.toordinal()
    """
def _is_leap(year):
    """
    year -> 1 if leap year, else 0.
    """
def _days_before_year(year):
    """
    year -> number of days before January 1st of year.
    """
def _days_in_month(year, month):
    """
    year, month -> number of days in that month in that year.
    """
def _days_before_month(year, month):
    """
    year, month -> number of days in year preceding first day of month.
    """
def _ymd2ord(year, month, day):
    """
    year, month, day -> ordinal, considering 01-Jan-0001 as day 1.
    """
def _ord2ymd(n):
    """
    ordinal -> (year, month, day), considering 01-Jan-0001 as day 1.
    """
def _build_struct_time(y, m, d, hh, mm, ss, dstflag):
    """
    'auto'
    """
def _format_offset(off):
    """
    ''
    """
def _wrap_strftime(object, format, timetuple):
    """
     Don't call utcoffset() or tzname() unless actually needed.

    """
def _parse_isoformat_date(dtstr):
    """
     It is assumed that this function will only be called with a
     string of length exactly 10, and (though this is not used) ASCII-only

    """
def _parse_hh_mm_ss_ff(tstr):
    """
     Parses things of the form HH[:MM[:SS[.fff[fff]]]]

    """
def _parse_isoformat_time(tstr):
    """
     Format supported is HH[:MM[:SS[.fff[fff]]]][+HH:MM[:SS[.ffffff]]]

    """
def _check_tzname(name):
    """
    tzinfo.tzname() must return None or string, 
    not '%s'
    """
def _check_utc_offset(name, offset):
    """
    utcoffset
    """
def _check_int_field(value):
    """
    'integer argument expected, got float'
    """
def _check_date_fields(year, month, day):
    """
    'year must be in %d..%d'
    """
def _check_time_fields(hour, minute, second, microsecond, fold):
    """
    'hour must be in 0..23'
    """
def _check_tzinfo_arg(tz):
    """
    tzinfo argument must be None or of a tzinfo subclass
    """
def _cmperror(x, y):
    """
    can't compare '%s' to '%s'
    """
def _divide_and_round(a, b):
    """
    divide a by b and round result to the nearest integer

        When the ratio is exactly half-way between two integers,
        the even integer is returned.
    
    """
def timedelta:
    """
    Represent the difference between two datetime objects.

        Supported operators:

        - add, subtract timedelta
        - unary plus, minus, abs
        - compare to timedelta
        - multiply, divide by int

        In addition, datetime supports subtraction of two datetime objects
        returning a timedelta, and addition or subtraction of a datetime
        and a timedelta giving a datetime.

        Representation: (days, seconds, microseconds).  Why?  Because I
        felt like it.
    
    """
2021-03-02 20:54:28,278 : INFO : tokenize_signature : --> do i ever get here?
    def __new__(cls, days=0, seconds=0, microseconds=0,
                milliseconds=0, minutes=0, hours=0, weeks=0):
        """
         Doing this efficiently and accurately in C is going to be difficult
         and error-prone, due to ubiquitous overflow possibilities, and that
         C double doesn't have enough bits of precision to represent
         microseconds over 10K years faithfully.  The code here tries to make
         explicit where go-fast assumptions can be relied on, in order to
         guide the C implementation; it's way more convoluted than speed-
         ignoring auto-overflow-to-long idiomatic Python could be.

         XXX Check that all inputs are ints or floats.

         Final values, all integer.
         s and us fit in 32-bit signed ints; d isn't bounded.

        """
    def __repr__(self):
        """
        days=%d
        """
    def __str__(self):
        """
        %d:%02d:%02d
        """
            def plural(n):
                """
                s
                """
    def total_seconds(self):
        """
        Total seconds in the duration.
        """
    def days(self):
        """
        days
        """
    def seconds(self):
        """
        seconds
        """
    def microseconds(self):
        """
        microseconds
        """
    def __add__(self, other):
        """
         for CPython compatibility, we cannot use
         our __class__ here, but need a real timedelta

        """
    def __sub__(self, other):
        """
         for CPython compatibility, we cannot use
         our __class__ here, but need a real timedelta

        """
    def __rsub__(self, other):
        """
         for CPython compatibility, we cannot use
         our __class__ here, but need a real timedelta

        """
    def __pos__(self):
        """
         for CPython compatibility, we cannot use
         our __class__ here, but need a real timedelta

        """
    def _to_microseconds(self):
        """
         Comparisons of timedelta objects with other.


        """
    def __eq__(self, other):
        """
         Pickle support.


        """
    def _getstate(self):
        """
        Concrete date type.

            Constructors:

            __new__()
            fromtimestamp()
            today()
            fromordinal()

            Operators:

            __repr__, __str__
            __eq__, __le__, __lt__, __ge__, __gt__, __hash__
            __add__, __radd__, __sub__ (add/radd only with timedelta arg)

            Methods:

            timetuple()
            toordinal()
            weekday()
            isoweekday(), isocalendar(), isoformat()
            ctime()
            strftime()

            Properties (readonly):
            year, month, day
    
        """
    def __new__(cls, year, month=None, day=None):
        """
        Constructor.

                Arguments:

                year, month, day (required, base 1)
        
        """
    def fromtimestamp(cls, t):
        """
        Construct a date from a POSIX timestamp (like time.time()).
        """
    def today(cls):
        """
        Construct a date from time.time().
        """
    def fromordinal(cls, n):
        """
        Construct a date from a proleptic Gregorian ordinal.

                January 1 of year 1 is day 1.  Only the year, month and day are
                non-zero in the result.
        
        """
    def fromisoformat(cls, date_string):
        """
        Construct a date from the output of date.isoformat().
        """
    def fromisocalendar(cls, year, week, day):
        """
        Construct a date from the ISO year, week number and weekday.

                This is the inverse of the date.isocalendar() function
        """
    def __repr__(self):
        """
        Convert to formal string, for repr().

                >>> dt = datetime(2010, 1, 1)
                >>> repr(dt)
                'datetime.datetime(2010, 1, 1, 0, 0)'

                >>> dt = datetime(2010, 1, 1, tzinfo=timezone.utc)
                >>> repr(dt)
                'datetime.datetime(2010, 1, 1, 0, 0, tzinfo=datetime.timezone.utc)'
        
        """
    def ctime(self):
        """
        Return ctime() style string.
        """
    def strftime(self, fmt):
        """
        Format using strftime().
        """
    def __format__(self, fmt):
        """
        must be str, not %s
        """
    def isoformat(self):
        """
        Return the date formatted according to ISO.

                This is 'YYYY-MM-DD'.

                References:
                - http://www.w3.org/TR/NOTE-datetime
                - http://www.cl.cam.ac.uk/~mgk25/iso-time.html
        
        """
    def year(self):
        """
        year (1-9999)
        """
    def month(self):
        """
        month (1-12)
        """
    def day(self):
        """
        day (1-31)
        """
    def timetuple(self):
        """
        Return local time tuple compatible with time.localtime().
        """
    def toordinal(self):
        """
        Return proleptic Gregorian ordinal for the year, month and day.

                January 1 of year 1 is day 1.  Only the year, month and day values
                contribute to the result.
        
        """
    def replace(self, year=None, month=None, day=None):
        """
        Return a new date with new values for the specified fields.
        """
    def __eq__(self, other):
        """
        Hash.
        """
    def __add__(self, other):
        """
        Add a date to a timedelta.
        """
    def __sub__(self, other):
        """
        Subtract two dates, or a date and a timedelta.
        """
    def weekday(self):
        """
        Return day of the week, where Monday == 0 ... Sunday == 6.
        """
    def isoweekday(self):
        """
        Return day of the week, where Monday == 1 ... Sunday == 7.
        """
    def isocalendar(self):
        """
        Return a 3-tuple containing ISO year, week number, and weekday.

                The first ISO week of the year is the (Mon-Sun) week
                containing the year's first Thursday; everything else derives
                from that.

                The first week is 1; Monday is 1 ... Sunday is 7.

                ISO calendar algorithm taken from
                http://www.phys.uu.nl/~vgent/calendar/isocalendar.htm
                (used with permission)
        
        """
    def _getstate(self):
        """
         so functions w/ args named "date" can get at the class
        """
def tzinfo:
    """
    Abstract base class for time zone info classes.

        Subclasses must override the name(), utcoffset() and dst() methods.
    
    """
    def tzname(self, dt):
        """
        datetime -> string name of time zone.
        """
    def utcoffset(self, dt):
        """
        datetime -> timedelta, positive for east of UTC, negative for west of UTC
        """
    def dst(self, dt):
        """
        datetime -> DST offset as timedelta, positive for east of UTC.

                Return 0 if DST not in effect.  utcoffset() must include the DST
                offset.
        
        """
    def fromutc(self, dt):
        """
        datetime in UTC -> datetime in local time.
        """
    def __reduce__(self):
        """
        __getinitargs__
        """
def time:
    """
    Time with time zone.

        Constructors:

        __new__()

        Operators:

        __repr__, __str__
        __eq__, __le__, __lt__, __ge__, __gt__, __hash__

        Methods:

        strftime()
        isoformat()
        utcoffset()
        tzname()
        dst()

        Properties (readonly):
        hour, minute, second, microsecond, tzinfo, fold
    
    """
    def __new__(cls, hour=0, minute=0, second=0, microsecond=0, tzinfo=None, *, fold=0):
        """
        Constructor.

                Arguments:

                hour, minute (required)
                second, microsecond (default to zero)
                tzinfo (default to None)
                fold (keyword only, default to zero)
        
        """
    def hour(self):
        """
        hour (0-23)
        """
    def minute(self):
        """
        minute (0-59)
        """
    def second(self):
        """
        second (0-59)
        """
    def microsecond(self):
        """
        microsecond (0-999999)
        """
    def tzinfo(self):
        """
        timezone info object
        """
    def fold(self):
        """
         Standard conversions, __hash__ (and helpers)

         Comparisons of time objects with other.


        """
    def __eq__(self, other):
        """
         arbitrary non-zero value
        """
    def __hash__(self):
        """
        Hash.
        """
    def _tzstr(self):
        """
        Return formatted timezone offset (+xx:xx) or an empty string.
        """
    def __repr__(self):
        """
        Convert to formal string, for repr().
        """
    def isoformat(self, timespec='auto'):
        """
        Return the time formatted according to ISO.

                The full format is 'HH:MM:SS.mmmmmm+zz:zz'. By default, the fractional
                part is omitted if self.microsecond == 0.

                The optional argument timespec specifies the number of additional
                terms of the time to include.
        
        """
    def fromisoformat(cls, time_string):
        """
        Construct a time from the output of isoformat().
        """
    def strftime(self, fmt):
        """
        Format using strftime().  The date part of the timestamp passed
                to underlying strftime should not be used.
        
        """
    def __format__(self, fmt):
        """
        must be str, not %s
        """
    def utcoffset(self):
        """
        Return the timezone offset as timedelta, positive east of UTC
                 (negative west of UTC).
        """
    def tzname(self):
        """
        Return the timezone name.

                Note that the name is 100% informational -- there's no requirement that
                it mean anything in particular. For example, "GMT", "UTC", "-500",
                "-5:00", "EDT", "US/Eastern", "America/New York" are all valid replies.
        
        """
    def dst(self):
        """
        Return 0 if DST is not in effect, or the DST offset (as timedelta
                positive eastward) if DST is in effect.

                This is purely informational; the DST offset has already been added to
                the UTC offset returned by utcoffset() if applicable, so there's no
                need to consult dst() unless you're interested in displaying the DST
                info.
        
        """
2021-03-02 20:54:28,307 : INFO : tokenize_signature : --> do i ever get here?
    def replace(self, hour=None, minute=None, second=None, microsecond=None,
                tzinfo=True, *, fold=None):
        """
        Return a new time with new values for the specified fields.
        """
    def _getstate(self, protocol=3):
        """
        bad tzinfo state arg
        """
    def __reduce_ex__(self, protocol):
        """
         so functions w/ args named "time" can get at the class
        """
def datetime(date):
    """
    datetime(year, month, day[, hour[, minute[, second[, microsecond[,tzinfo]]]]])

        The year, month and day arguments are required. tzinfo may be None, or an
        instance of a tzinfo subclass. The remaining arguments may be ints.
    
    """
2021-03-02 20:54:28,309 : INFO : tokenize_signature : --> do i ever get here?
    def __new__(cls, year, month=None, day=None, hour=0, minute=0, second=0,
                microsecond=0, tzinfo=None, *, fold=0):
        """
         Pickle support

        """
    def hour(self):
        """
        hour (0-23)
        """
    def minute(self):
        """
        minute (0-59)
        """
    def second(self):
        """
        second (0-59)
        """
    def microsecond(self):
        """
        microsecond (0-999999)
        """
    def tzinfo(self):
        """
        timezone info object
        """
    def fold(self):
        """
        Construct a datetime from a POSIX timestamp (like time.time()).

                A timezone info object may be passed in as well.
        
        """
    def fromtimestamp(cls, t, tz=None):
        """
        Construct a datetime from a POSIX timestamp (like time.time()).

                A timezone info object may be passed in as well.
        
        """
    def utcfromtimestamp(cls, t):
        """
        Construct a naive UTC datetime from a POSIX timestamp.
        """
    def now(cls, tz=None):
        """
        Construct a datetime from time.time() and optional time zone info.
        """
    def utcnow(cls):
        """
        Construct a UTC datetime from time.time().
        """
    def combine(cls, date, time, tzinfo=True):
        """
        Construct a datetime from a given date and a given time.
        """
    def fromisoformat(cls, date_string):
        """
        Construct a datetime from the output of datetime.isoformat().
        """
    def timetuple(self):
        """
        Return local time tuple compatible with time.localtime().
        """
    def _mktime(self):
        """
        Return integer POSIX timestamp.
        """
        def local(u):
            """
             Our goal is to solve t = local(u) for u.

            """
    def timestamp(self):
        """
        Return POSIX timestamp as float
        """
    def utctimetuple(self):
        """
        Return UTC time tuple compatible with time.gmtime().
        """
    def date(self):
        """
        Return the date part.
        """
    def time(self):
        """
        Return the time part, with tzinfo None.
        """
    def timetz(self):
        """
        Return the time part, with same tzinfo.
        """
2021-03-02 20:54:28,317 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:28,317 : INFO : tokenize_signature : --> do i ever get here?
    def replace(self, year=None, month=None, day=None, hour=None,
                minute=None, second=None, microsecond=None, tzinfo=True,
                *, fold=None):
        """
        Return a new datetime with new values for the specified fields.
        """
    def _local_timezone(self):
        """
         Extract TZ data

        """
    def astimezone(self, tz=None):
        """
        tz argument must be an instance of tzinfo
        """
    def ctime(self):
        """
        Return ctime() style string.
        """
    def isoformat(self, sep='T', timespec='auto'):
        """
        Return the time formatted according to ISO.

                The full format looks like 'YYYY-MM-DD HH:MM:SS.mmmmmm'.
                By default, the fractional part is omitted if self.microsecond == 0.

                If self.tzinfo is not None, the UTC offset is also attached, giving
                giving a full format of 'YYYY-MM-DD HH:MM:SS.mmmmmm+HH:MM'.

                Optional argument sep specifies the separator between date and
                time, default 'T'.

                The optional argument timespec specifies the number of additional
                terms of the time to include.
        
        """
    def __repr__(self):
        """
        Convert to formal string, for repr().
        """
    def __str__(self):
        """
        Convert to string, for str().
        """
    def strptime(cls, date_string, format):
        """
        'string, format -> new datetime parsed from a string (like time.strptime()).'
        """
    def utcoffset(self):
        """
        Return the timezone offset as timedelta positive east of UTC (negative west of
                UTC).
        """
    def tzname(self):
        """
        Return the timezone name.

                Note that the name is 100% informational -- there's no requirement that
                it mean anything in particular. For example, "GMT", "UTC", "-500",
                "-5:00", "EDT", "US/Eastern", "America/New York" are all valid replies.
        
        """
    def dst(self):
        """
        Return 0 if DST is not in effect, or the DST offset (as timedelta
                positive eastward) if DST is in effect.

                This is purely informational; the DST offset has already been added to
                the UTC offset returned by utcoffset() if applicable, so there's no
                need to consult dst() unless you're interested in displaying the DST
                info.
        
        """
    def __eq__(self, other):
        """
         Assume that allow_mixed means that we are called from __eq__

        """
    def __add__(self, other):
        """
        Add a datetime and a timedelta.
        """
    def __sub__(self, other):
        """
        Subtract two datetimes, or a datetime and a timedelta.
        """
    def __hash__(self):
        """
         Pickle support.


        """
    def _getstate(self, protocol=3):
        """
        bad tzinfo state arg
        """
    def __reduce_ex__(self, protocol):
        """
         Helper to calculate the day number of the Monday starting week 1
         XXX This could be done more efficiently

        """
def timezone(tzinfo):
    """
    '_offset'
    """
    def __new__(cls, offset, name=_Omitted):
        """
        offset must be a timedelta
        """
    def _create(cls, offset, name=None):
        """
        pickle support
        """
    def __eq__(self, other):
        """
        Convert to formal string, for repr().

                >>> tz = timezone.utc
                >>> repr(tz)
                'datetime.timezone.utc'
                >>> tz = timezone(timedelta(hours=-5), 'EST')
                >>> repr(tz)
                "datetime.timezone(datetime.timedelta(-1, 68400), 'EST')"
        
        """
    def __str__(self):
        """
        utcoffset() argument must be a datetime instance
         or None
        """
    def tzname(self, dt):
        """
        tzname() argument must be a datetime instance
         or None
        """
    def dst(self, dt):
        """
        dst() argument must be a datetime instance
         or None
        """
    def fromutc(self, dt):
        """
        fromutc: dt.tzinfo 
        is not self
        """
    def _name_from_offset(delta):
        """
        'UTC'
        """
