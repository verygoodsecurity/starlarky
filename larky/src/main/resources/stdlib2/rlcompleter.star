def Completer:
    """
    Create a new completer for the command line.

            Completer([namespace]) -> completer instance.

            If unspecified, the default namespace where completions are performed
            is __main__ (technically, __main__.__dict__). Namespaces should be
            given as dictionaries.

            Completer instances should be used as the completion mechanism of
            readline via the set_completer() call:

            readline.set_completer(Completer(my_namespace).complete)
        
    """
    def complete(self, text, state):
        """
        Return the next possible completion for 'text'.

                This is called successively with state == 0, 1, 2, ... until it
                returns None.  The completion should begin with 'text'.

        
        """
    def _callable_postfix(self, val, word):
        """
        (
        """
    def global_matches(self, text):
        """
        Compute matches when text is a simple name.

                Return a list of all keywords, built-in functions and names currently
                defined in self.namespace that match.

        
        """
    def attr_matches(self, text):
        """
        Compute matches when text contains a dot.

                Assuming the text is of the form NAME.NAME....[NAME], and is
                evaluable in self.namespace, it will be evaluated and its attributes
                (as revealed by dir()) are used as possible completions.  (For class
                instances, class members are also considered.)

                WARNING: this can still invoke arbitrary C code, if an object
                with a __getattr__ hook is evaluated.

        
        """
def get_class_members(klass):
    """
    '__bases__'
    """
