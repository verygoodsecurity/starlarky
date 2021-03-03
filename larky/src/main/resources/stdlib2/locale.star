def _strcoll(a,b):
    """
     strcoll(string,string) -> int.
            Compares two strings according to the locale.
    
    """
def _strxfrm(s):
    """
     strxfrm(string) -> string.
            Returns a string that behaves for cmp locale-aware.
    
    """
    def localeconv():
        """
         localeconv() -> dict.
                    Returns numeric and monetary locale-specific parameters.
        
        """
    def setlocale(category, value=None):
        """
         setlocale(integer,string=None) -> string.
                    Activates/queries locale processing.
        
        """
def localeconv():
    """
     Number formatting APIs

     Author: Martin von Loewis
     improved by Georg Brandl

     Iterate over grouping intervals

    """
def _grouping_intervals(grouping):
    """
     if grouping is -1, we are done

    """
def _group(s, monetary=False):
    """
    'mon_thousands_sep'
    """
def _strip_padding(s, amount):
    """
    ' '
    """
def _format(percent, value, grouping=False, monetary=False, *additional):
    """
     floats and decimal ints need special action!

    """
def format_string(f, val, grouping=False, monetary=False):
    """
    Formats a string in the same way that the % formatting would use,
        but takes the current locale into account.

        Grouping is applied if the third parameter is true.
        Conversion uses monetary thousands separator and grouping strings if
        forth parameter monetary is true.
    """
def format(percent, value, grouping=False, monetary=False, *additional):
    """
    Deprecated, use format_string instead.
    """
def currency(val, symbol=True, grouping=False, international=False):
    """
    Formats val according to the currency settings
        in the current locale.
    """
def str(val):
    """
    Convert float to string, taking the locale into account.
    """
def delocalize(string):
    """
    Parses a string as a normalized number according to the locale settings.
    """
def atof(string, func=float):
    """
    Parses a string as a float according to the locale settings.
    """
def atoi(string):
    """
    Converts a string to an integer according to the locale settings.
    """
def _test():
    """

    """
def _replace_encoding(code, encoding):
    """
    '.'
    """
def _append_modifier(code, modifier):
    """
    'euro'
    """
def normalize(localename):
    """
     Returns a normalized locale code for the given locale
            name.

            The returned locale code is formatted for use with
            setlocale().

            If normalization fails, the original name is returned
            unchanged.

            If the given encoding is not known, the function defaults to
            the default encoding for the locale code just like setlocale()
            does.

    
    """
def _parse_localename(localename):
    """
     Parses the locale code for localename and returns the
            result as tuple (language code, encoding).

            The localename is normalized and passed through the locale
            alias engine. A ValueError is raised in case the locale name
            cannot be parsed.

            The language code corresponds to RFC 1766.  code and encoding
            can be None in case the values cannot be determined or are
            unknown to this implementation.

    
    """
def _build_localename(localetuple):
    """
     Builds a locale code from the given tuple (language code,
            encoding).

            No aliasing or normalizing takes place.

    
    """
def getdefaultlocale(envvars=('LC_ALL', 'LC_CTYPE', 'LANG', 'LANGUAGE')):
    """
     Tries to determine the default locale settings and returns
            them as tuple (language code, encoding).

            According to POSIX, a program which has not called
            setlocale(LC_ALL, "") runs using the portable 'C' locale.
            Calling setlocale(LC_ALL, "") lets it use the default locale as
            defined by the LANG variable. Since we don't want to interfere
            with the current locale setting we thus emulate the behavior
            in the way described above.

            To maintain compatibility with other platforms, not only the
            LANG variable is tested, but a list of variables given as
            envvars parameter. The first found to be defined will be
            used. envvars defaults to the search path used in GNU gettext;
            it must always contain the variable name 'LANG'.

            Except for the code 'C', the language code corresponds to RFC
            1766.  code and encoding can be None in case the values cannot
            be determined.

    
    """
def getlocale(category=LC_CTYPE):
    """
     Returns the current setting for the given locale category as
            tuple (language code, encoding).

            category may be one of the LC_* value except LC_ALL. It
            defaults to LC_CTYPE.

            Except for the code 'C', the language code corresponds to RFC
            1766.  code and encoding can be None in case the values cannot
            be determined.

    
    """
def setlocale(category, locale=None):
    """
     Set the locale for the given category.  The locale can be
            a string, an iterable of two strings (language code and encoding),
            or None.

            Iterables are converted to strings using the locale aliasing
            engine.  Locale strings are passed directly to the C lib.

            category may be given as one of the LC_* values.

    
    """
def resetlocale(category=LC_ALL):
    """
     Sets the locale for category to the default setting.

            The default setting is determined by calling
            getdefaultlocale(). category defaults to LC_ALL.

    
    """
    def getpreferredencoding(do_setlocale = True):
        """
        Return the charset that the user is likely using.
        """
            def getpreferredencoding(do_setlocale = True):
                """
                'UTF-8'
                """
            def getpreferredencoding(do_setlocale = True):
                """
                Return the charset that the user is likely using,
                                by looking at environment variables.
                """
        def getpreferredencoding(do_setlocale = True):
            """
            Return the charset that the user is likely using,
                        according to the system configuration.
            """
def _print_locale():
    """
     Test function.
    
    """
    def _init_categories(categories=categories):
        """
        'LC_'
        """
