def _tokenize(plural):
    """
    'WHITESPACES'
    """
def _error(value):
    """
    'unexpected token in plural form: %s'
    """
def _parse(tokens, priority=-1):
    """
    ''
    """
def _as_int(n):
    """
    'Plural value must be an integer, got %s'
    """
def c2py(plural):
    """
    Gets a C expression as used in PO files for plural forms and returns a
        Python function that implements an equivalent expression.
    
    """
def _expand_lang(loc):
    """
     split up the locale into its base components

    """
def NullTranslations:
    """
    'lgettext() is deprecated, use gettext() instead'
    """
    def ngettext(self, msgid1, msgid2, n):
        """
        'lngettext() is deprecated, use ngettext() instead'
        """
    def pgettext(self, context, message):
        """
        'output_charset() is deprecated'
        """
    def set_output_charset(self, charset):
        """
        'set_output_charset() is deprecated'
        """
    def install(self, names=None):
        """
        '_'
        """
def GNUTranslations(NullTranslations):
    """
     Magic number of .mo files

    """
    def _get_versions(self, version):
        """
        Returns a tuple of major version, minor version
        """
    def _parse(self, fp):
        """
        Override this method to support alternative .mo formats.
        """
    def lgettext(self, message):
        """
        'lgettext() is deprecated, use gettext() instead'
        """
    def lngettext(self, msgid1, msgid2, n):
        """
        'lngettext() is deprecated, use ngettext() instead'
        """
    def gettext(self, message):
        """
         Locate a .mo file using the gettext strategy

        """
def find(domain, localedir=None, languages=None, all=False):
    """
     Get some reasonable defaults for arguments that were not supplied

    """
2021-03-02 20:46:44,881 : INFO : tokenize_signature : --> do i ever get here?
def translation(domain, localedir=None, languages=None,
                class_=None, fallback=False, codeset=_unspecified):
    """
    'No translation file found for domain'
    """
def install(domain, localedir=None, codeset=_unspecified, names=None):
    """
     a mapping b/w domains and locale directories

    """
def textdomain(domain=None):
    """
    'bind_textdomain_codeset() is deprecated'
    """
def dgettext(domain, message):
    """
    'ldgettext() is deprecated, use dgettext() instead'
    """
def dngettext(domain, msgid1, msgid2, n):
    """
    'ldngettext() is deprecated, use dngettext() instead'
    """
def dpgettext(domain, context, message):
    """
    'lgettext() is deprecated, use gettext() instead'
    """
def ngettext(msgid1, msgid2, n):
    """
    'lngettext() is deprecated, use ngettext() instead'
    """
def pgettext(context, message):
    """
     dcgettext() has been deemed unnecessary and is not implemented.

     James Henstridge's Catalog constructor from GNOME gettext.  Documented usage
     was:

        import gettext
        cat = gettext.Catalog(PACKAGE, localedir=LOCALEDIR)
        _ = cat.gettext
        print _('Hello World')

     The resulting catalog object currently don't support access through a
     dictionary API, which was supported (but apparently unused) in GNOME
     gettext.


    """
