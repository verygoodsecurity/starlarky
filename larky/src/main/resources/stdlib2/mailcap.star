def lineno_sort_key(entry):
    """
     Sort in ascending order, with unspecified entries at the end

    """
def getcaps():
    """
    Return a dictionary containing the mailcap database.

        The dictionary maps a MIME type (in all lowercase, e.g. 'text/plain')
        to a list of dictionaries corresponding to mailcap entries.  The list
        collects all the entries for that MIME type from all available mailcap
        files.  Each dictionary contains key-value pairs for that MIME type,
        where the viewing command is stored with the key "view".

    
    """
def listmailcapfiles():
    """
    Return a list of all mailcap files found on the system.
    """
def readmailcapfile(fp):
    """
    Read a mailcap file and return a dictionary keyed by MIME type.
    """
def _readmailcapfile(fp, lineno):
    """
    Read a mailcap file and return a dictionary keyed by MIME type.

        Each MIME type is mapped to an entry consisting of a list of
        dictionaries; the list will contain more than one such dictionary
        if a given MIME type appears more than once in the mailcap file.
        Each dictionary contains key-value pairs for that MIME type, where
        the viewing command is stored with the key "view".
    
    """
def parseline(line):
    """
    Parse one entry in a mailcap file and return a dictionary.

        The viewing command is stored as the value with the key "view",
        and the rest of the fields produce key-value pairs in the dict.
    
    """
def parsefield(line, i, n):
    """
    Separate one key-value pair in a mailcap entry.
    """
def findmatch(caps, MIMEtype, key='view', filename="/dev/null", plist=[]):
    """
    Find a match for a mailcap entry.

        Return a tuple containing the command line, and the mailcap entry
        used; (None, None) if no match is found.  This may invoke the
        'test' command of several matching entries before deciding which
        entry to use.

    
    """
def lookup(caps, MIMEtype, key=None):
    """
    '/'
    """
def subst(field, MIMEtype, filename, plist=[]):
    """
     XXX Actually, this is Unix-specific

    """
def findparam(name, plist):
    """
    '='
    """
def test():
    """
    usage: mailcap [MIMEtype file] ...
    """
def show(caps):
    """
    Mailcap files:
    """
