def IllegalMonthError(ValueError):
    """
    bad month number %r; must be 1-12
    """
def IllegalWeekdayError(ValueError):
    """
    bad weekday number %r; must be 0 (Monday) to 6 (Sunday)
    """
def _localized_month:
    """

    """
    def __init__(self, format):
        """
         January 1, 2001, was a Monday.

        """
    def __init__(self, format):
        """
         Full and abbreviated names of weekdays

        """
def isleap(year):
    """
    Return True for leap years, False for non-leap years.
    """
def leapdays(y1, y2):
    """
    Return number of leap years in range [y1, y2).
           Assume y1 <= y2.
    """
def weekday(year, month, day):
    """
    Return weekday (0-6 ~ Mon-Sun) for year, month (1-12), day (1-31).
    """
def monthrange(year, month):
    """
    Return weekday (0-6 ~ Mon-Sun) and number of days (28-31) for
           year, month.
    """
def _monthlen(year, month):
    """

        Base calendar class. This class doesn't do any formatting. It simply
        provides data to subclasses.
    
    """
    def __init__(self, firstweekday=0):
        """
         0 = Monday, 6 = Sunday
        """
    def getfirstweekday(self):
        """

                Return an iterator for one week of weekday numbers starting with the
                configured first one.
        
        """
    def itermonthdates(self, year, month):
        """

                Return an iterator for one month. The iterator will yield datetime.date
                values and will always iterate through complete weeks, so it will yield
                dates outside the specified month.
        
        """
    def itermonthdays(self, year, month):
        """

                Like itermonthdates(), but will yield day numbers. For days outside
                the specified month the day number is 0.
        
        """
    def itermonthdays2(self, year, month):
        """

                Like itermonthdates(), but will yield (day number, weekday number)
                tuples. For days outside the specified month the day number is 0.
        
        """
    def itermonthdays3(self, year, month):
        """

                Like itermonthdates(), but will yield (year, month, day) tuples.  Can be
                used for dates outside of datetime.date range.
        
        """
    def itermonthdays4(self, year, month):
        """

                Like itermonthdates(), but will yield (year, month, day, day_of_week) tuples.
                Can be used for dates outside of datetime.date range.
        
        """
    def monthdatescalendar(self, year, month):
        """

                Return a matrix (list of lists) representing a month's calendar.
                Each row represents a week; week entries are datetime.date values.
        
        """
    def monthdays2calendar(self, year, month):
        """

                Return a matrix representing a month's calendar.
                Each row represents a week; week entries are
                (day number, weekday number) tuples. Day numbers outside this month
                are zero.
        
        """
    def monthdayscalendar(self, year, month):
        """

                Return a matrix representing a month's calendar.
                Each row represents a week; days outside this month are zero.
        
        """
    def yeardatescalendar(self, year, width=3):
        """

                Return the data for the specified year ready for formatting. The return
                value is a list of month rows. Each month row contains up to width months.
                Each month contains between 4 and 6 weeks and each week contains 1-7
                days. Days are datetime.date objects.
        
        """
    def yeardays2calendar(self, year, width=3):
        """

                Return the data for the specified year ready for formatting (similar to
                yeardatescalendar()). Entries in the week lists are
                (day number, weekday number) tuples. Day numbers outside this month are
                zero.
        
        """
    def yeardayscalendar(self, year, width=3):
        """

                Return the data for the specified year ready for formatting (similar to
                yeardatescalendar()). Entries in the week lists are day numbers.
                Day numbers outside this month are zero.
        
        """
def TextCalendar(Calendar):
    """

        Subclass of Calendar that outputs a calendar as a simple plain text
        similar to the UNIX program cal.
    
    """
    def prweek(self, theweek, width):
        """

                Print a single week (no newline).
        
        """
    def formatday(self, day, weekday, width):
        """

                Returns a formatted day.
        
        """
    def formatweek(self, theweek, width):
        """

                Returns a single week in a string (no newline).
        
        """
    def formatweekday(self, day, width):
        """

                Returns a formatted week day name.
        
        """
    def formatweekheader(self, width):
        """

                Return a header for a week.
        
        """
    def formatmonthname(self, theyear, themonth, width, withyear=True):
        """

                Return a formatted month name.
        
        """
    def prmonth(self, theyear, themonth, w=0, l=0):
        """

                Print a month's calendar.
        
        """
    def formatmonth(self, theyear, themonth, w=0, l=0):
        """

                Return a month's calendar string (multi-line).
        
        """
    def formatyear(self, theyear, w=2, l=1, c=6, m=3):
        """

                Returns a year's calendar as a multi-line string.
        
        """
    def pryear(self, theyear, w=0, l=0, c=6, m=3):
        """
        Print a year's calendar.
        """
def HTMLCalendar(Calendar):
    """

        This calendar returns complete HTML pages.
    
    """
    def formatday(self, day, weekday):
        """

                Return a day as a table cell.
        
        """
    def formatweek(self, theweek):
        """

                Return a complete week as a table row.
        
        """
    def formatweekday(self, day):
        """

                Return a weekday name as a table header.
        
        """
    def formatweekheader(self):
        """

                Return a header for a week as a table row.
        
        """
    def formatmonthname(self, theyear, themonth, withyear=True):
        """

                Return a month name as a table row.
        
        """
    def formatmonth(self, theyear, themonth, withyear=True):
        """

                Return a formatted month as a table.
        
        """
    def formatyear(self, theyear, width=3):
        """

                Return a formatted year as a table of tables.
        
        """
    def formatyearpage(self, theyear, width=3, css='calendar.css', encoding=None):
        """

                Return a formatted year as a complete HTML page.
        
        """
def different_locale:
    """

        This class can be passed a locale name in the constructor and will return
        month and weekday names in the specified locale. If this locale includes
        an encoding all strings containing month and weekday names will be returned
        as unicode.
    
    """
    def __init__(self, firstweekday=0, locale=None):
        """
        %s %r
        """
def LocaleHTMLCalendar(HTMLCalendar):
    """

        This class can be passed a locale name in the constructor and will return
        month and weekday names in the specified locale. If this locale includes
        an encoding all strings containing month and weekday names will be returned
        as unicode.
    
    """
    def __init__(self, firstweekday=0, locale=None):
        """
        '<th class="%s">%s</th>'
        """
    def formatmonthname(self, theyear, themonth, withyear=True):
        """
        '%s %s'
        """
def setfirstweekday(firstweekday):
    """
     Spacing of month columns for multi-column year calendar

    """
def format(cols, colwidth=_colwidth, spacing=_spacing):
    """
    Prints multi-column formatting for year calendars
    """
def formatstring(cols, colwidth=_colwidth, spacing=_spacing):
    """
    Returns a string formatted from n strings, centered within n columns.
    """
def timegm(tuple):
    """
    Unrelated but handy function to calculate Unix timestamp from GMT.
    """
def main(args):
    """
    'text only arguments'
    """
