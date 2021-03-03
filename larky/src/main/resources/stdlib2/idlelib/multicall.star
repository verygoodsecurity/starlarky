def _SimpleBinder:
    """
    '<'
    """
    def bind(self, triplet, func):
        """
         An int in range(1 << len(_modifiers)) represents a combination of modifiers
         (if the least significant bit is on, _modifiers[0] is on, and so on).
         _state_subsets gives for each combination of modifiers, or *state*,
         a list of the states which are a subset of it. This list is ordered by the
         number of modifiers is the state - the most specific state comes first.

        """
def expand_substates(states):
    """
    '''For each item of states return a list containing all combinations of
        that item with individual bits reset, sorted by the number of set bits.
        '''
    """
    def nbits(n):
        """
        number of bits set in n base 2
        """
def _ComplexBinder:
    """
     This class binds many functions, and only unbinds them when it is deleted.
     self.handlerids is the list of seqs and ids of binded handler functions.
     The binded functions sit in a dictionary of lists of lists, which maps
     a detail (or None) and a state into a list of functions.
     When a new detail is discovered, handlers for all the possible states
     are binded.


    """
    def __create_handler(self, lists, mc_type, mc_state):
        """
         Call all functions in doafterhandler and remove them from list

        """
    def __init__(self, type, widget, widgetinst):
        """
         we don't want to change the lists of functions while a handler is
         running - it will mess up the loop and anyway, we usually want the
         change to happen from the next event. So we have a list of functions
         for the handler to run after it finishes calling the binded functions.
         It calls them only once.
         ishandlerrunning is a list. An empty one means no, otherwise - yes.
         this is done so that it would be mutable.

        """
    def bind(self, triplet, func):
        """
        <%s%s-%s>
        """
    def unbind(self, triplet, func):
        """
         define the list of event types to be handled by MultiEvent. the order is
         compatible with the definition of event type constants.

        """
def _parse_sequence(sequence):
    """
    Get a string which should describe an event sequence. If it is
        successfully parsed as one, return a tuple containing the state (as an int),
        the event type (as an index of _types), and the detail - None if none, or a
        string if there is one. If the parsing is unsuccessful, return None.
    
    """
def _triplet_to_sequence(triplet):
    """
    '<'
    """
def MultiCallCreator(widget):
    """
    Return a MultiCall class which inherits its methods from the
        given widget class (for example, Tkinter.Text). This is used
        instead of a templating mechanism.
    
    """
    def MultiCall (widget):
    """
     a dictionary which maps a virtual event to a tuple with:
      0. the function binded
      1. a list of triplets - the sequences it is binded to

    """
        def bind(self, sequence=None, func=None, add=None):
            """
            print("bind(%s, %s, %s)" % (sequence, func, add),
                  file=sys.__stderr__)

            """
        def unbind(self, sequence, funcid=None):
            """
            <<
            """
        def event_add(self, virtual, *sequences):
            """
            print("event_add(%s, %s)" % (repr(virtual), repr(sequences)),
                  file=sys.__stderr__)

            """
        def event_delete(self, virtual, *sequences):
            """
            print("Tkinter event_delete: %s" % seq, file=sys.__stderr__)

            """
        def event_info(self, virtual=None):
            """
             htest #
            """
    def bindseq(seq, n=[0]):
        """
        <<handler%d>>
        """
