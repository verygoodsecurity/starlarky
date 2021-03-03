def errprint(*args):
    """

    """
def main():
    """
    qv
    """
def NannyNag(Exception):
    """

        Raised by process_tokens() if detecting an ambiguous indent.
        Captured and handled in check().
    
    """
    def __init__(self, lineno, msg, line):
        """
        check(file_or_dir)

            If file_or_dir is a directory and not a symbolic link, then recursively
            descend the directory tree named by file_or_dir, checking all .py files
            along the way. If file_or_dir is an ordinary Python source file, it is
            checked for whitespace related problems. The diagnostic messages are
            written to standard output using the print statement.
    
        """
def Whitespace:
    """
     the characters used for space and tab

    """
    def __init__(self, ws):
        """
         return length of longest contiguous run of spaces (whether or not
         preceding a tab)

        """
    def longest_run_of_spaces(self):
        """
         count, il = self.norm
         for i in range(len(count)):
            if count[i]:
                il = il + (i//tabsize + 1)*tabsize * count[i]
         return il

         quicker:
         il = trailing + sum (i//ts + 1)*ts*count[i] =
         trailing + ts * sum (i//ts + 1)*count[i] =
         trailing + ts * sum i//ts*count[i] + count[i] =
         trailing + ts * [(sum i//ts*count[i]) + (sum count[i])] =
         trailing + ts * [(sum i//ts*count[i]) + num_tabs]
         and note that i//ts*count[i] is 0 when i < ts


        """
    def equal(self, other):
        """
         return a list of tuples (ts, i1, i2) such that
         i1 == self.indent_level(ts) != other.indent_level(ts) == i2.
         Intended to be used after not self.equal(other) is known, in which
         case it will return at least one witnessing tab size.

        """
    def not_equal_witness(self, other):
        """
         Return True iff self.indent_level(t) < other.indent_level(t)
         for all t >= 1.
         The algorithm is due to Vincent Broman.
         Easy to prove it's correct.
         XXXpost that.
         Trivial to prove n is sharp (consider T vs ST).
         Unknown whether there's a faster general way.  I suspected so at
         first, but no longer.
         For the special (but common!) case where M and N are both of the
         form (T*)(S*), M.less(N) iff M.len() < N.len() and
         M.num_tabs() <= N.num_tabs(). Proof is easy but kinda long-winded.
         XXXwrite that up.
         Note that M is of the form (T*)(S*) iff len(M.norm[0]) <= 1.

        """
    def less(self, other):
        """
         the self.n >= other.n test already did it for ts=1

        """
    def not_less_witness(self, other):
        """
        at tab size
        """
def process_tokens(tokens):
    """

    """
