def Template:
    """
    Class representing a pipeline template.
    """
    def __init__(self):
        """
        Template() returns a fresh pipeline template.
        """
    def __repr__(self):
        """
        t.__repr__() implements repr(t).
        """
    def reset(self):
        """
        t.reset() restores a pipeline template to its initial state.
        """
    def clone(self):
        """
        t.clone() returns a new pipeline template with identical
                initial state as the current one.
        """
    def debug(self, flag):
        """
        t.debug(flag) turns debugging on or off.
        """
    def append(self, cmd, kind):
        """
        t.append(cmd, kind) adds a new step at the end.
        """
    def prepend(self, cmd, kind):
        """
        t.prepend(cmd, kind) adds a new step at the front.
        """
    def open(self, file, rw):
        """
        t.open(file, rw) returns a pipe or file object open for
                reading or writing; the file is the other end of the pipeline.
        """
    def open_r(self, file):
        """
        t.open_r(file) and t.open_w(file) implement
                t.open(file, 'r') and t.open(file, 'w') respectively.
        """
    def open_w(self, file):
        """
        'w'
        """
    def copy(self, infile, outfile):
        """
        'set -x; '
        """
def makepipeline(infile, steps, outfile):
    """
     Build a list with for each command:
     [input filename or '', command string, kind, output filename or '']


    """
