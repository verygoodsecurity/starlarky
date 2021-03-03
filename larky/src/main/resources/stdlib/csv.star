def Dialect:
    """
    Describe a CSV dialect.

        This must be subclassed (see csv.excel).  Valid attributes are:
        delimiter, quotechar, escapechar, doublequote, skipinitialspace,
        lineterminator, quoting.

    
    """
    def __init__(self):
        """
         We do this for compatibility with py2.3

        """
def excel(Dialect):
    """
    Describe the usual properties of Excel-generated CSV files.
    """
def excel_tab(excel):
    """
    Describe the usual properties of Excel-generated TAB-delimited files.
    """
def unix_dialect(Dialect):
    """
    Describe the usual properties of Unix-generated CSV files.
    """
def DictReader:
    """
    excel
    """
    def __iter__(self):
        """
         Used only for its side effect.

        """
def DictWriter:
    """

    """
    def writeheader(self):
        """
        raise
        """
    def writerow(self, rowdict):
        """
         Guard Sniffer's type checking against builds that exclude complex()

        """
def Sniffer:
    """
    '''
        "Sniffs" the format of a CSV file (i.e. delimiter, quotechar)
        Returns a Dialect object.
        '''
    """
    def __init__(self):
        """
         in case there is more than one possible delimiter

        """
    def sniff(self, sample, delimiters=None):
        """

                Returns a dialect (or None) corresponding to the sample
        
        """
        def dialect(Dialect):
    """
    sniffed
    """
    def _guess_quote_and_delimiter(self, data, delimiters):
        """

                Looks for text enclosed between two identical quotes
                (the probable quotechar) which are preceded and followed
                by the same character (the probable delimiter).
                For example:
                                 ,'some text',
                The quote with the most wins, same with the delimiter.
                If there is no quotechar the delimiter can't be determined
                this way.
        
        """
    def _guess_delimiter(self, data, delimiters):
        """

                The delimiter /should/ occur the same number of times on
                each row. However, due to malformed data, it may not. We don't want
                an all or nothing approach, so we allow for small variations in this
                number.
                  1) build a table of the frequency of each character on every line.
                  2) build a table of frequencies of this frequency (meta-frequency?),
                     e.g.  'x occurred 5 times in 10 rows, 6 times in 1000 rows,
                     7 times in 2 rows'
                  3) use the mode of the meta-frequency to determine the /expected/
                     frequency for that character
                  4) find out how often the character actually meets that goal
                  5) the character that best meets its goal is the delimiter
                For performance reasons, the data is evaluated in chunks, so it can
                try and evaluate the smallest portion of the data possible, evaluating
                additional chunks as necessary.
        
        """
    def has_header(self, sample):
        """
         Creates a dictionary of types of data in each column. If any
         column is of a single type (say, integers), *except* for the first
         row, then the first row is presumed to be labels. If the type
         can't be determined, it is assumed to be a string in which case
         the length of the string is the determining factor: if all of the
         rows except for the first are the same length, it's a header.
         Finally, a 'vote' is taken at the end for each column, adding or
         subtracting from the likelihood of the first row being a header.


        """
