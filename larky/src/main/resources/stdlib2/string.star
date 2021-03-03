def capwords(s, sep=None):
    """
    capwords(s [,sep]) -> string

        Split the argument into words using split, capitalize each
        word using capitalize, and join the capitalized words using
        join.  If the optional second argument sep is absent or None,
        runs of whitespace characters are replaced by a single space
        and leading and trailing whitespace are removed, otherwise
        sep is used to split and join the words.

    
    """
def _TemplateMetadef(type):
    """
    r"""
        %(delim)s(?:
          (?P<escaped>%(delim)s) |   # Escape sequence of two delimiters
          (?P<named>%(id)s)      |   # delimiter and a Python identifier
          {(?P<braced>%(bid)s)}  |   # delimiter and a braced identifier
          (?P<invalid>)              # Other ill-formed delimiter exprs
        )
    
    """
    def __init__(cls, name, bases, dct):
        """
        'pattern'
        """
def Template(metadef=_TemplateMetadef):
    """
    A string class for supporting $-substitutions.
    """
    def __init__(self, template):
        """
         Search for $$, $identifier, ${identifier}, and any bare $'s


        """
    def _invalid(self, mo):
        """
        'invalid'
        """
    def substitute(self, mapping=_sentinel_dict, /, **kws):
        """
         Helper function for .sub()

        """
        def convert(mo):
            """
             Check the most common path first.

            """
    def safe_substitute(self, mapping=_sentinel_dict, /, **kws):
        """
         Helper function for .sub()

        """
        def convert(mo):
            """
            'named'
            """
def Formatter:
    """
    'Max string recursion exceeded'
    """
    def get_value(self, key, args, kwargs):
        """
         do any conversion on the resulting object

        """
    def parse(self, format_string):
        """
         given a field_name, find the object it references.
          field_name:   the field being looked up, e.g. "0.name
                         or "lookup[3]
          used_args:    a set of which args have been used
          args, kwargs: as passed in to vformat

        """
    def get_field(self, field_name, args, kwargs):
        """
         loop through the rest of the field_name, doing
          getattr or getitem as needed

        """
