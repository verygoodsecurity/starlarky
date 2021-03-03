def HTMLParser(_markupbase.ParserBase):
    """
    Find tags and other markup and call handler functions.

        Usage:
            p = HTMLParser()
            p.feed(data)
            ...
            p.close()

        Start tags are handled by calling self.handle_starttag() or
        self.handle_startendtag(); end tags by self.handle_endtag().  The
        data between tags is passed from the parser to the derived class
        by calling self.handle_data() with the data as argument (the data
        may be split up in arbitrary chunks).  If convert_charrefs is
        True the character references are converted automatically to the
        corresponding Unicode character (and self.handle_data() is no
        longer split in chunks), otherwise they are passed by calling
        self.handle_entityref() or self.handle_charref() with the string
        containing respectively the named or numeric reference as the
        argument.
    
    """
    def __init__(self, *, convert_charrefs=True):
        """
        Initialize and reset this instance.

                If convert_charrefs is True (the default), all character references
                are automatically converted to the corresponding Unicode characters.
        
        """
    def reset(self):
        """
        Reset this instance.  Loses all unprocessed data.
        """
    def feed(self, data):
        """
        r"""Feed data to the parser.

                Call this as often as you want, with as little or as much text
                as you want (may include '\n').
        
        """
    def close(self):
        """
        Handle any buffered data.
        """
    def get_starttag_text(self):
        """
        Return full source of start tag: '<...>'.
        """
    def set_cdata_mode(self, elem):
        """
        r'</\s*%s\s*>'
        """
    def clear_cdata_mode(self):
        """
         Internal -- handle data as far as reasonable.  May leave state
         and data to be processed by a subsequent call.  If 'end' is
         true, force handling all data as if followed by EOF marker.

        """
    def goahead(self, end):
        """
        '<'
        """
    def parse_html_declaration(self, i):
        """
        '<!'
        """
    def parse_bogus_comment(self, i, report=1):
        """
        '<!'
        """
    def parse_pi(self, i):
        """
        '<?'
        """
    def parse_starttag(self, i):
        """
         Now parse the data between i+1 and j into a tag and attrs

        """
    def check_for_whole_start_tag(self, i):
        """
        >
        """
    def parse_endtag(self, i):
        """
        </
        """
    def handle_startendtag(self, tag, attrs):
        """
         Overridable -- handle start tag

        """
    def handle_starttag(self, tag, attrs):
        """
         Overridable -- handle end tag

        """
    def handle_endtag(self, tag):
        """
         Overridable -- handle character reference

        """
    def handle_charref(self, name):
        """
         Overridable -- handle entity reference

        """
    def handle_entityref(self, name):
        """
         Overridable -- handle data

        """
    def handle_data(self, data):
        """
         Overridable -- handle comment

        """
    def handle_comment(self, data):
        """
         Overridable -- handle declaration

        """
    def handle_decl(self, decl):
        """
         Overridable -- handle processing instruction

        """
    def handle_pi(self, data):
        """
         Internal -- helper to remove special character quoting

        """
    def unescape(self, s):
        """
        'The unescape method is deprecated and will be removed '
        'in 3.5, use html.unescape() instead.'
        """
