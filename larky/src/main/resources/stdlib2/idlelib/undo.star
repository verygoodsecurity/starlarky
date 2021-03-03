def UndoDelegator(Delegator):
    """
    <<undo>>
    """
    def dump_event(self, event):
        """
        pointer:
        """
    def reset_undo(self):
        """
         or a CommandSequence instance
        """
    def set_saved(self, flag):
        """
         Clients should call undo_block_start() and undo_block_stop()
         around a sequence of editing cmds to be treated as a unit by
         undo & redo.  Nested matching calls are OK, and the inner calls
         then act like nops.  OK too if no editing cmds, or only one
         editing cmd, is issued in between:  if no cmds, the whole
         sequence has no effect; and if only one cmd, that cmd is entered
         directly into the undo list, as if undo_block_xxx hadn't been
         called.  The intent of all that is to make this scheme easy
         to use:  all the client has to worry about is making sure each
         _start() call is matched by a _stop() call.


        """
    def undo_block_start(self):
        """
         no need to wrap a single cmd

        """
    def addcmd(self, cmd, execute=True):
        """
        print "truncating undo list

        """
    def undo_event(self, event):
        """
        break
        """
    def redo_event(self, event):
        """
        break
        """
def Command:
    """
     Base class for Undoable commands


    """
    def __init__(self, index1, index2, chars, tags=None):
        """
        insert
        """
    def set_marks(self, text, marks):
        """
         Undoable insert command


        """
    def __init__(self, index1, chars, tags=None):
        """
        >
        """
    def redo(self, text):
        """
        'insert'
        """
    def undo(self, text):
        """
        'insert'
        """
    def merge(self, cmd):
        """
        _
        """
    def classify(self, c):
        """
        alphanumeric
        """
def DeleteCommand(Command):
    """
     Undoable delete command


    """
    def __init__(self, index1, index2=None):
        """
         +1c
        """
    def redo(self, text):
        """
        'insert'
        """
    def undo(self, text):
        """
        'insert'
        """
def CommandSequence(Command):
    """
     Wrapper for a sequence of undoable cmds to be undone/redone
     as a unit


    """
    def __init__(self):
        """
            %r
        """
    def __len__(self):
        """
         htest #
        """
