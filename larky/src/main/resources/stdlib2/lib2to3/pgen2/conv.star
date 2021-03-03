def Converter(grammar.Grammar):
    """
    Grammar subclass that reads classic pgen output files.

        The run() method reads the tables as produced by the pgen parser
        generator, typically contained in two C files, graminit.h and
        graminit.c.  The other methods are for internal use only.

        See the base class for more documentation.

    
    """
    def run(self, graminit_h, graminit_c):
        """
        Load the grammar tables from the text files written by pgen.
        """
    def parse_graminit_h(self, filename):
        """
        Parse the .h file written by pgen.  (Internal)

                This file is a sequence of #define statements defining the
                nonterminals of the grammar as numbers.  We build two tables
                mapping the numbers to names and back.

        
        """
    def parse_graminit_c(self, filename):
        """
        Parse the .c file written by pgen.  (Internal)

                The file looks as follows.  The first two lines are always this:

                #include "pgenheaders.h"
                #include "grammar.h"

                After that come four blocks:

                1) one or more state definitions
                2) a table defining dfas
                3) a table defining labels
                4) a struct defining the grammar

                A state definition has the following form:
                - one or more arc arrays, each of the form:
                  static arc arcs_<n>_<m>[<k>] = {
                          {<i>, <j>},
                          ...
                  };
                - followed by a state array, of the form:
                  static state states_<s>[<t>] = {
                          {<k>, arcs_<n>_<m>},
                          ...
                  };

        
        """
    def finish_off(self):
        """
        Create additional useful structures.  (Internal).
        """
