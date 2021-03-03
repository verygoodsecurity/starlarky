def _getlang():
    """
     Figure out what the current language is set to.

    """
def LocaleTime(object):
    """
    Stores and handles locale-specific information related to time.

        ATTRIBUTES:
            f_weekday -- full weekday names (7-item list)
            a_weekday -- abbreviated weekday names (7-item list)
            f_month -- full month names (13-item list; dummy value in [0], which
                        is added by code)
            a_month -- abbreviated month names (13-item list, dummy value in
                        [0], which is added by code)
            am_pm -- AM/PM representation (2-item list)
            LC_date_time -- format string for date/time representation (string)
            LC_date -- format string for date representation (string)
            LC_time -- format string for time representation (string)
            timezone -- daylight- and non-daylight-savings timezone representation
                        (2-item list of sets)
            lang -- Language used by instance (2-item tuple)
    
    """
    def __init__(self):
        """
        Set all attributes.

                Order of methods called matters for dependency reasons.

                The locale language is set at the offset and then checked again before
                exiting.  This is to make sure that the attributes were not set with a
                mix of information from more than one locale.  This would most likely
                happen when using threads where one thread calls a locale-dependent
                function while another thread changes the locale while the function in
                the other thread is still running.  Proper coding would call for
                locks to prevent changing the locale while locale-dependent code is
                running.  The check here is done in case someone does not think about
                doing this.

                Only other possible issue is if someone changed the timezone and did
                not call tz.tzset .  That is an issue for the programmer, though,
                since changing the timezone is worthless without that call.

        
        """
    def __calc_weekday(self):
        """
         Set self.a_weekday and self.f_weekday using the calendar
         module.

        """
    def __calc_month(self):
        """
         Set self.f_month and self.a_month using the calendar module.

        """
    def __calc_am_pm(self):
        """
         Set self.am_pm by using time.strftime().

         The magic date (1999,3,17,hour,44,55,2,76,0) is not really that
         magical; just happened to have used it everywhere else where a
         static date was needed.

        """
    def __calc_date_time(self):
        """
         Set self.date_time, self.date, & self.time by using
         time.strftime().

         Use (1999,3,17,22,44,55,2,76,0) for magic date because the amount of
         overloaded numbers is minimized.  The order in which searches for
         values within the format string is very important; it eliminates
         possible ambiguity for what something represents.

        """
    def __calc_timezone(self):
        """
         Set self.timezone by using time.tzname.
         Do not worry about possibility of time.tzname[0] == time.tzname[1]
         and time.daylight; handle that in strptime.

        """
def TimeRE(dict):
    """
    Handle conversion from format directives to regexes.
    """
    def __init__(self, locale_time=None):
        """
        Create keys/values.

                Order of execution is important for dependency reasons.

        
        """
    def __seqToRE(self, to_convert, directive):
        """
        Convert a list to a regex string for matching a directive.

                Want possible matching values to be from longest to shortest.  This
                prevents the possibility of a match occurring for a value that also
                a substring of a larger value that should have matched (e.g., 'abc'
                matching when 'abcdef' should have been the match).

        
        """
    def pattern(self, format):
        """
        Return regex pattern for the format string.

                Need to make sure that any characters that might be interpreted as
                regex syntax are escaped.

        
        """
    def compile(self, format):
        """
        Return a compiled re object for the format string.
        """
def _calc_julian_from_U_or_W(year, week_of_year, day_of_week, week_starts_Mon):
    """
    Calculate the Julian day based on the year, week of the year, and day of
        the week, with week_start_day representing whether the week of the year
        assumes the week starts on Sunday or Monday (6 or 0).
    """
def _calc_julian_from_V(iso_year, iso_week, iso_weekday):
    """
    Calculate the Julian day based on the ISO 8601 year, week, and weekday.
        ISO weeks start on Mondays, with week 01 being the week containing 4 Jan.
        ISO week days range from 1 (Monday) to 7 (Sunday).
    
    """
def _strptime(data_string, format="%a %b %d %H:%M:%S %Y"):
    """
    Return a 2-tuple consisting of a time struct and an int containing
        the number of microseconds based on the input string and the
        format string.
    """
def _strptime_time(data_string, format="%a %b %d %H:%M:%S %Y"):
    """
    Return a time struct based on the input string and the
        format string.
    """
def _strptime_datetime(cls, data_string, format="%a %b %d %H:%M:%S %Y"):
    """
    Return a class cls instance based on the input string and the
        format string.
    """
