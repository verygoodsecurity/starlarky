def ParserBase:
    """
    Parser base class which provides some common support methods used
        by the SGML/HTML and XHTML parsers.
    """
    def __init__(self):
        """
        _markupbase.ParserBase must be subclassed
        """
    def error(self, message):
        """
        subclasses of ParserBase must override error()
        """
    def reset(self):
        """
        Return current line number and offset.
        """
    def updatepos(self, i, j):
        """
        \n
        """
    def parse_declaration(self, i):
        """
         This is some sort of declaration; in "HTML as
         deployed," this should only be the document type
         declaration ("<!DOCTYPE html...>").
         ISO 8879:1986, however, has more complex
         declaration syntax for elements in <!...>, including:
         --comment--
         [marked section]
         name in the following list: ENTITY, DOCTYPE, ELEMENT,
         ATTLIST, NOTATION, SHORTREF, USEMAP,
         LINKTYPE, LINK, IDLINK, USELINK, SYSTEM

        """
    def parse_marked_section(self, i, report=1):
        """
        '<!['
        """
    def parse_comment(self, i, report=1):
        """
        '<!--'
        """
    def _parse_doctype_subset(self, i, declstartpos):
        """
        <
        """
    def _parse_doctype_element(self, i, declstartpos):
        """
         style content model; just skip until '>'

        """
    def _parse_doctype_attlist(self, i, declstartpos):
        """

        """
    def _parse_doctype_notation(self, i, declstartpos):
        """
         end of buffer; incomplete

        """
    def _parse_doctype_entity(self, i, declstartpos):
        """
        %
        """
    def _scan_name(self, i, declstartpos):
        """
         end of buffer
        """
    def unknown_decl(self, data):
