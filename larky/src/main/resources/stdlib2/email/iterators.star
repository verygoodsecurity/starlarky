def walk(self):
    """
    Walk over the message tree, yielding each subpart.

        The walk is performed in depth-first order.  This method is a
        generator.
    
    """
def body_line_iterator(msg, decode=False):
    """
    Iterate over the parts, returning string payloads line-by-line.

        Optional decode (default False) is passed through to .get_payload().
    
    """
def typed_subpart_iterator(msg, maintype='text', subtype=None):
    """
    Iterate over the subparts with a given MIME type.

        Use `maintype' as the main MIME type to match against; this defaults to
        "text".  Optional `subtype' is the MIME subtype to match against; if
        omitted, only the main type is matched.
    
    """
def _structure(msg, fp=None, level=0, include_default=False):
    """
    A handy debugging aid
    """
