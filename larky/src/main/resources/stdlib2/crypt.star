def _Method(_namedtuple('_Method', 'name ident salt_chars total_size')):
    """
    Class representing a salt method per the Modular Crypt Format or the
        legacy 2-character crypt method.
    """
    def __repr__(self):
        """
        '<crypt.METHOD_{}>'
        """
def mksalt(method=None, *, rounds=None):
    """
    Generate a salt for the specified method.

        If not specified, the strongest available method will be used.

    
    """
def crypt(word, salt=None):
    """
    Return a string representing the one-way hash of a password, with a salt
        prepended.

        If ``salt`` is not specified or is ``None``, the strongest
        available method will be selected and a salt generated.  Otherwise,
        ``salt`` may be one of the ``crypt.METHOD_*`` values, or a string as
        returned by ``crypt.mksalt()``.

    
    """
def _add_method(name, *args, rounds=None):
    """
    'METHOD_'
    """
