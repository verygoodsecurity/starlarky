2021-03-02 20:53:55,560 : INFO : tokenize_signature : --> do i ever get here?
def _combine_flags(flags, add_flags, del_flags,
                   TYPE_FLAGS=sre_parse.TYPE_FLAGS):
    """
     internal: compile a (sub)pattern

    """
def _compile_charset(charset, flags, code):
    """
     compile charset subprogram

    """
def _optimize_charset(charset, iscased=None, fixup=None, fixes=None):
    """
     internal: optimize character set

    """
def _mk_bitmap(bits, _CODEBITS=_CODEBITS, _int=int):
    """
     Convert block indices to word array

    """
def _simple(p):
    """
     check if this subpattern is a "simple" operator

    """
def _generate_overlap_table(prefix):
    """

        Generate an overlap table for the following prefix.
        An overlap table is a table of the same size as the prefix which
        informs about the potential self-overlap for each index in the prefix:
        - if overlap[i] == 0, prefix[i:] can't overlap prefix[0:...]
        - if overlap[i] == k with 0 < k <= i, prefix[i-k+1:i+1] overlaps with
          prefix[0:k]
    
    """
def _get_iscased(flags):
    """
     look for literal prefix

    """
def _get_charset_prefix(pattern, flags):
    """
     internal: compile an info block.  in the current version,
     this contains min/max pattern width, and an optional literal
     prefix or a character map

    """
def isstring(obj):
    """
     compile info block

    """
def _hex_code(code):
    """
    '[%s]'
    """
def dis(code):
    """
    '(to %d)'
    """
        def print_2(*args):
            """
            ' '
            """
def compile(p, flags=0):
    """
     internal: convert pattern list to internal format


    """
