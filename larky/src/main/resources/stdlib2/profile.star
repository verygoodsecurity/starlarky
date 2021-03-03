def _Utils:
    """
    Support class for utility functions which are shared by
        profile.py and cProfile.py modules.
        Not supposed to be used directly.
    
    """
    def __init__(self, profiler):
        """
        **************************************************************************
         The following are the static member functions for the profiler class
         Note that an instance of Profile() is *not* needed to call them.
        **************************************************************************


        """
def run(statement, filename=None, sort=-1):
    """
    Run statement under profiler optionally saving results in filename

        This function takes a single argument that can be passed to the
        "exec" statement, and an optional file name.  In all cases this
        routine attempts to "exec" its first argument and gather profiling
        statistics from the execution. If no file name is present, then this
        function automatically prints a simple profiling report, sorted by the
        standard name string (file/line/function-name) that is presented in
        each line.
    
    """
def runctx(statement, globals, locals, filename=None, sort=-1):
    """
    Run statement under profiler, supplying your own globals and locals,
        optionally saving results in filename.

        statement and filename have the same semantics as profile.run
    
    """
def Profile:
    """
    Profiler class.

        self.cur is always a tuple.  Each such tuple corresponds to a stack
        frame that is currently active (self.cur[-2]).  The following are the
        definitions of its members.  We use this external "parallel stack" to
        avoid contaminating the program that we are profiling. (old profiler
        used to write into the frames local dictionary!!) Derived classes
        can change the definition of some entries, as long as they leave
        [-2:] intact (frame and previous tuple).  In case an internal error is
        detected, the -3 element is used as the function name.

        [ 0] = Time that needs to be charged to the parent frame's function.
               It is used so that a function call will not have to access the
               timing data for the parent frame.
        [ 1] = Total time spent in this frame's function, excluding time in
               subfunctions (this latter is tallied in cur[2]).
        [ 2] = Total time spent in subfunctions, excluding time executing the
               frame's function (this latter is tallied in cur[1]).
        [-3] = Name of the function that corresponds to this frame.
        [-2] = Actual frame that we correspond to (used to sync exception handling).
        [-1] = Our parent 6-tuple (corresponds to frame.f_back).

        Timing data for each function is stored as a 5-tuple in the dictionary
        self.timings[].  The index is always the name stored in self.cur[-3].
        The following are the definitions of the members:

        [0] = The number of times this function was called, not counting direct
              or indirect recursion,
        [1] = Number of times this function appears on the stack, minus one
        [2] = Total time spent internal to this function
        [3] = Cumulative time that this function was present on the stack.  In
              non-recursive functions, this is the total execution time from start
              to finish of each invocation of a function, including time spent in
              all subfunctions.
        [4] = A dictionary indicating for each function name, the number of times
              it was called by us.
    
    """
    def __init__(self, timer=None, bias=None):
        """

        """
                def get_time_timer(timer=timer, sum=sum):
                    """
                    'profiler'
                    """
    def trace_dispatch(self, frame, event, arg):
        """
        c_call
        """
    def trace_dispatch_i(self, frame, event, arg):
        """
        c_call
        """
    def trace_dispatch_mac(self, frame, event, arg):
        """
        c_call
        """
    def trace_dispatch_l(self, frame, event, arg):
        """
        c_call
        """
    def trace_dispatch_exception(self, frame, t):
        """
        Bad call
        """
    def trace_dispatch_c_call (self, frame, t):
        """

        """
    def trace_dispatch_return(self, frame, t):
        """
        Bad return
        """
    def set_cmd(self, cmd):
        """
         already set
        """
    def fake_code:
    """
    'profile'
    """
    def simulate_cmd_complete(self):
        """
         We *can* cause assertion errors here if
         dispatch_trace_return checks for a frame match!

        """
    def print_stats(self, sort=-1):
        """
        'wb'
        """
    def create_stats(self):
        """
         The following two methods can be called by clients to use
         a profiler to profile a statement, given as a string.


        """
    def run(self, cmd):
        """
         This method is more useful to profile a single function call.

        """
    def runcall(*args, **kw):
        """
        descriptor 'runcall' of 'Profile' object 
        needs an argument
        """
    def calibrate(self, m, verbose=0):
        """
        Subclasses must override .calibrate().
        """
    def _calibrate_inner(self, m, verbose):
        """
         Set up a test case to be run with and without profiling.  Include
         lots of calls, because we're trying to quantify stopwatch overhead.
         Do not raise any exceptions, though, because we want to know
         exactly how many profile events are generated (one call event, +
         one return event, per Python-level call).


        """
        def f(m, f1=f1):
            """
             warm up the cache
            """
def main():
    """
    profile.py [-o output_file_path] [-s sort] [-m module | scriptfile] [arg] ...
    """
