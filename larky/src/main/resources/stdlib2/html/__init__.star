def escape(s, quote=True):
    """

        Replace special characters "&", "<" and ">" to HTML-safe sequences.
        If the optional flag quote is true (the default), the quotation mark
        characters, both double quote (") and single quote (') characters are also
        translated.
    
    """
def _replace_charref(s):
    """
    '#'
    """
def unescape(s):
    """

        Convert all named and numeric character references (e.g. &gt;, &#62;,
        &x3e;) in the string s to the corresponding unicode characters.
        This function uses the rules defined by the HTML 5 standard
        for both valid and invalid character references, and the list of
        HTML 5 named character references defined in html.entities.html5.
    
    """
