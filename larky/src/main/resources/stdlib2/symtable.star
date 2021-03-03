def symtable(code, filename, compile_type):
    """

    """
    def get_type(self):
        """
        module
        """
    def get_id(self):
        """
        Return true if the scope uses exec.  Deprecated method.
        """
    def get_identifiers(self):
        """
         Default values for instance variables

        """
    def __idents_matching(self, test_func):
        """
         like PyST_GetScope()
        """
    def __repr__(self):
        """
        <symbol {0!r}>
        """
    def get_name(self):
        """
        Returns true if name binding introduces new namespace.

                If the name is used as the target of a function or class
                statement, this will be true.

                Note that a single name can be bound to multiple objects.  If
                is_namespace() is true, the name may also be bound to other
                objects, like an int or list, that does not introduce a new
                namespace.
        
        """
    def get_namespaces(self):
        """
        Return a list of namespaces bound to this name
        """
    def get_namespace(self):
        """
        Returns the single namespace bound to this name.

                Raises ValueError if the name is bound to multiple namespaces.
        
        """
