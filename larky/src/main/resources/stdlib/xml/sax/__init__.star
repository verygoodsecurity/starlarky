def parse(source, handler, errorHandler=ErrorHandler()):
    """
     this is the parser list used by the make_parser function if no
     alternatives are given as parameters to the function


    """
def make_parser(parser_list=()):
    """
    Creates and returns a SAX parser.

        Creates the first parser it is able to instantiate of the ones
        given in the iterable created by chaining parser_list and
        default_parser_list.  The iterables must contain the names of Python
        modules containing both a SAX parser and a create_parser function.
    """
    def _create_parser(parser_name):
        """
        'create_parser'
        """
