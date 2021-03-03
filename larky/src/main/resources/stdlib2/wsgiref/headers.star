def _formatparam(param, value=None, quote=1):
    """
    Convenience function to format and return a key=value pair.

        This will quote the value if needed or if quote is true.
    
    """
def Headers:
    """
    Manage a collection of HTTP response headers
    """
    def __init__(self, headers=None):
        """
        Headers must be a list of name/value tuples
        """
    def _convert_string_type(self, value):
        """
        Convert/check value type.
        """
    def __len__(self):
        """
        Return the total number of headers, including duplicates.
        """
    def __setitem__(self, name, val):
        """
        Set the value of a header.
        """
    def __delitem__(self,name):
        """
        Delete all occurrences of a header, if present.

                Does *not* raise an exception if the header is missing.
        
        """
    def __getitem__(self,name):
        """
        Get the first header value for 'name'

                Return None if the header is missing instead of raising an exception.

                Note that if the header appeared multiple times, the first exactly which
                occurrence gets returned is undefined.  Use getall() to get all
                the values matching a header field name.
        
        """
    def __contains__(self, name):
        """
        Return true if the message contains the header.
        """
    def get_all(self, name):
        """
        Return a list of all the values for the named field.

                These will be sorted in the order they appeared in the original header
                list or were added to this instance, and may contain duplicates.  Any
                fields deleted and re-inserted are always appended to the header list.
                If no fields exist with the given name, returns an empty list.
        
        """
    def get(self,name,default=None):
        """
        Get the first header value for 'name', or return 'default'
        """
    def keys(self):
        """
        Return a list of all the header field names.

                These will be sorted in the order they appeared in the original header
                list, or were added to this instance, and may contain duplicates.
                Any fields deleted and re-inserted are always appended to the header
                list.
        
        """
    def values(self):
        """
        Return a list of all header values.

                These will be sorted in the order they appeared in the original header
                list, or were added to this instance, and may contain duplicates.
                Any fields deleted and re-inserted are always appended to the header
                list.
        
        """
    def items(self):
        """
        Get all the header fields and values.

                These will be sorted in the order they were in the original header
                list, or were added to this instance, and may contain duplicates.
                Any fields deleted and re-inserted are always appended to the header
                list.
        
        """
    def __repr__(self):
        """
        %s(%r)
        """
    def __str__(self):
        """
        str() returns the formatted headers, complete with end line,
                suitable for direct HTTP transmission.
        """
    def __bytes__(self):
        """
        'iso-8859-1'
        """
    def setdefault(self,name,value):
        """
        Return first matching header value for 'name', or 'value'

                If there is no header named 'name', add a new header with name 'name'
                and value 'value'.
        """
    def add_header(self, _name, _value, **_params):
        """
        Extended header setting.

                _name is the header field to add.  keyword arguments can be used to set
                additional parameters for the header field, with underscores converted
                to dashes.  Normally the parameter will be added as key="value" unless
                value is None, in which case only the key will be added.

                Example:

                h.add_header('content-disposition', 'attachment', filename='bud.gif')

                Note that unlike the corresponding 'email.message' method, this does
                *not* handle '(charset, language, value)' tuples: all values must be
                strings or None.
        
        """
