def _format_size(size, sign):
    """
    'B'
    """
def Statistic:
    """

        Statistic difference on memory allocations between two Snapshot instance.
    
    """
    def __init__(self, traceback, size, count):
        """
        %s: size=%s, count=%i

        """
    def __repr__(self):
        """
        '<Statistic traceback=%r size=%i count=%i>'

        """
    def _sort_key(self):
        """

            Statistic difference on memory allocations between an old and a new
            Snapshot instance.
    
        """
    def __init__(self, traceback, size, size_diff, count, count_diff):
        """
        %s: size=%s (%s), count=%i (%+i)

        """
    def __repr__(self):
        """
        '<StatisticDiff traceback=%r size=%i (%+i) count=%i (%+i)>'

        """
    def _sort_key(self):
        """

            Frame of a traceback.
    
        """
    def __init__(self, frame):
        """
         frame is a tuple: (filename: str, lineno: int)

        """
    def filename(self):
        """
        %s:%s
        """
    def __repr__(self):
        """
        <Frame filename=%r lineno=%r>
        """
def Traceback(Sequence):
    """

        Sequence of Frame instances sorted from the oldest frame
        to the most recent frame.
    
    """
    def __init__(self, frames):
        """
         frames is a tuple of frame tuples: see Frame constructor for the
         format of a frame tuple; it is reversed, because _tracemalloc
         returns frames sorted from most recent to oldest, but the
         Python API expects oldest to most recent

        """
    def __len__(self):
        """
        <Traceback %r>
        """
    def format(self, limit=None, most_recent_first=False):
        """
        '  File "%s", line %s'

        """
def get_object_traceback(obj):
    """

        Get the traceback where the Python object *obj* was allocated.
        Return a Traceback instance.

        Return None if the tracemalloc module is not tracing memory allocations or
        did not trace the allocation of the object.
    
    """
def Trace:
    """

        Trace of a memory block.
    
    """
    def __init__(self, trace):
        """
         trace is a tuple: (domain: int, size: int, traceback: tuple).
         See Traceback constructor for the format of the traceback tuple.

        """
    def domain(self):
        """
        %s: %s
        """
    def __repr__(self):
        """
        <Trace domain=%s size=%s, traceback=%r>

        """
def _Traces(Sequence):
    """
     traces is a tuple of trace tuples: see Trace constructor

    """
    def __len__(self):
        """
        <Traces len=%s>
        """
def _normalize_filename(filename):
    """
    '.pyc'
    """
def BaseFilter:
    """

        Snapshot of traces of memory blocks allocated by Python.
    
    """
    def __init__(self, traces, traceback_limit):
        """
         traces is a tuple of trace tuples: see _Traces constructor for
         the exact format

        """
    def dump(self, filename):
        """

                Write the snapshot into a file.
        
        """
    def load(filename):
        """

                Load a snapshot from a file.
        
        """
    def _filter_trace(self, include_filters, exclude_filters, trace):
        """

                Create a new Snapshot instance with a filtered traces sequence, filters
                is a list of Filter or DomainFilter instances.  If filters is an empty
                list, return a new Snapshot instance with a copy of the traces.
        
        """
    def _group_by(self, key_type, cumulative):
        """
        'traceback'
        """
    def statistics(self, key_type, cumulative=False):
        """

                Group statistics by key_type. Return a sorted list of Statistic
                instances.
        
        """
    def compare_to(self, old_snapshot, key_type, cumulative=False):
        """

                Compute the differences with an old snapshot old_snapshot. Get
                statistics as a sorted list of StatisticDiff instances, grouped by
                group_by.
        
        """
def take_snapshot():
    """

        Take a snapshot of traces of memory blocks allocated by Python.
    
    """
