def NullFormatter:
    """
    A formatter which does nothing.

        If the writer parameter is omitted, a NullWriter instance is created.
        No methods of the writer are called by NullFormatter instances.

        Implementations should inherit from this class if implementing a writer
        interface but don't need to inherit any implementation.

    
    """
    def __init__(self, writer=None):
        """
        The standard formatter.

            This implementation has demonstrated wide applicability to many writers,
            and may be used directly in most circumstances.  It has been used to
            implement a full-featured World Wide Web browser.

    
        """
    def __init__(self, writer):
        """
         Output device
        """
    def end_paragraph(self, blankline):
        """
        ''
        """
    def format_letter(self, case, counter):
        """
        ''
        """
    def format_roman(self, case, counter):
        """
        'i'
        """
    def add_flowing_data(self, data):
        """
 
        """
    def add_literal_data(self, data):
        """
 
        """
    def flush_softspace(self):
        """
        ' '
        """
    def push_alignment(self, align):
        """
        ' '
        """
    def pop_font(self):
        """
        ' '
        """
    def pop_style(self, n=1):
        """
        Minimal writer interface to use in testing & inheritance.

            A writer which only provides the interface definition; no actions are
            taken on any methods.  This should be the base class for all writers
            which do not need to inherit any implementation methods.

    
        """
    def __init__(self): pass
        """
        A writer which can be used in debugging formatters, but not much else.

            Each method simply announces itself by printing its name and
            arguments on standard output.

    
        """
    def new_alignment(self, align):
        """
        new_alignment(%r)
        """
    def new_font(self, font):
        """
        new_font(%r)
        """
    def new_margin(self, margin, level):
        """
        new_margin(%r, %d)
        """
    def new_spacing(self, spacing):
        """
        new_spacing(%r)
        """
    def new_styles(self, styles):
        """
        new_styles(%r)
        """
    def send_paragraph(self, blankline):
        """
        send_paragraph(%r)
        """
    def send_line_break(self):
        """
        send_line_break()
        """
    def send_hor_rule(self, *args, **kw):
        """
        send_hor_rule()
        """
    def send_label_data(self, data):
        """
        send_label_data(%r)
        """
    def send_flowing_data(self, data):
        """
        send_flowing_data(%r)
        """
    def send_literal_data(self, data):
        """
        send_literal_data(%r)
        """
def DumbWriter(NullWriter):
    """
    Simple writer class which writes output on the file object passed in
        as the file parameter or, if file is omitted, on standard output.  The
        output is simply word-wrapped to the number of columns specified by
        the maxcol parameter.  This class is suitable for reflowing a sequence
        of paragraphs.

    
    """
    def __init__(self, file=None, maxcol=72):
        """
        '\n'
        """
    def send_line_break(self):
        """
        '\n'
        """
    def send_hor_rule(self, *args, **kw):
        """
        '\n'
        """
    def send_literal_data(self, data):
        """
        '\n'
        """
    def send_flowing_data(self, data):
        """
        '\n'
        """
def test(file = None):
    """
    '\n'
    """
