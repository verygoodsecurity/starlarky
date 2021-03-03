def ismodule(object):
    """
    Return true if the object is a module.

        Module objects provide these attributes:
            __cached__      pathname to byte compiled file
            __doc__         documentation string
            __file__        filename (missing for built-in modules)
    """
def isclass(object):
    """
    Return true if the object is a class.

        Class objects provide these attributes:
            __doc__         documentation string
            __module__      name of module in which this class was defined
    """
def ismethod(object):
    """
    Return true if the object is an instance method.

        Instance method objects provide these attributes:
            __doc__         documentation string
            __name__        name with which this method was defined
            __func__        function object containing implementation of method
            __self__        instance to which this method is bound
    """
def ismethoddescriptor(object):
    """
    Return true if the object is a method descriptor.

        But not if ismethod() or isclass() or isfunction() are true.

        This is new in Python 2.2, and, for example, is true of int.__add__.
        An object passing this test has a __get__ attribute but not a __set__
        attribute, but beyond that the set of attributes varies.  __name__ is
        usually sensible, and __doc__ often is.

        Methods implemented via descriptors that also pass one of the other
        tests return false from the ismethoddescriptor() test, simply because
        the other tests promise more -- you can, e.g., count on having the
        __func__ attribute (etc) when an object passes ismethod().
    """
def isdatadescriptor(object):
    """
    Return true if the object is a data descriptor.

        Data descriptors have a __set__ or a __delete__ attribute.  Examples are
        properties (defined in Python) and getsets and members (defined in C).
        Typically, data descriptors will also have __name__ and __doc__ attributes
        (properties, getsets, and members have both of these attributes), but this
        is not guaranteed.
    """
    def ismemberdescriptor(object):
        """
        Return true if the object is a member descriptor.

                Member descriptors are specialized descriptors defined in extension
                modules.
        """
    def ismemberdescriptor(object):
        """
        Return true if the object is a member descriptor.

                Member descriptors are specialized descriptors defined in extension
                modules.
        """
    def isgetsetdescriptor(object):
        """
        Return true if the object is a getset descriptor.

                getset descriptors are specialized descriptors defined in extension
                modules.
        """
    def isgetsetdescriptor(object):
        """
        Return true if the object is a getset descriptor.

                getset descriptors are specialized descriptors defined in extension
                modules.
        """
def isfunction(object):
    """
    Return true if the object is a user-defined function.

        Function objects provide these attributes:
            __doc__         documentation string
            __name__        name with which this function was defined
            __code__        code object containing compiled function bytecode
            __defaults__    tuple of any default values for arguments
            __globals__     global namespace in which this function was defined
            __annotations__ dict of parameter annotations
            __kwdefaults__  dict of keyword only parameters with defaults
    """
def _has_code_flag(f, flag):
    """
    Return true if ``f`` is a function (or a method or functools.partial
        wrapper wrapping a function) whose code object has the given ``flag``
        set in its flags.
    """
def isgeneratorfunction(obj):
    """
    Return true if the object is a user-defined generator function.

        Generator function objects provide the same attributes as functions.
        See help(isfunction) for a list of attributes.
    """
def iscoroutinefunction(obj):
    """
    Return true if the object is a coroutine function.

        Coroutine functions are defined with "async def" syntax.
    
    """
def isasyncgenfunction(obj):
    """
    Return true if the object is an asynchronous generator function.

        Asynchronous generator functions are defined with "async def"
        syntax and have "yield" expressions in their body.
    
    """
def isasyncgen(object):
    """
    Return true if the object is an asynchronous generator.
    """
def isgenerator(object):
    """
    Return true if the object is a generator.

        Generator objects provide these attributes:
            __iter__        defined to support iteration over container
            close           raises a new GeneratorExit exception inside the
                            generator to terminate the iteration
            gi_code         code object
            gi_frame        frame object or possibly None once the generator has
                            been exhausted
            gi_running      set to 1 when generator is executing, 0 otherwise
            next            return the next item from the container
            send            resumes the generator and "sends" a value that becomes
                            the result of the current yield-expression
            throw           used to raise an exception inside the generator
    """
def iscoroutine(object):
    """
    Return true if the object is a coroutine.
    """
def isawaitable(object):
    """
    Return true if object can be passed to an ``await`` expression.
    """
def istraceback(object):
    """
    Return true if the object is a traceback.

        Traceback objects provide these attributes:
            tb_frame        frame object at this level
            tb_lasti        index of last attempted instruction in bytecode
            tb_lineno       current line number in Python source code
            tb_next         next inner traceback object (called by this level)
    """
def isframe(object):
    """
    Return true if the object is a frame object.

        Frame objects provide these attributes:
            f_back          next outer frame object (this frame's caller)
            f_builtins      built-in namespace seen by this frame
            f_code          code object being executed in this frame
            f_globals       global namespace seen by this frame
            f_lasti         index of last attempted instruction in bytecode
            f_lineno        current line number in Python source code
            f_locals        local namespace seen by this frame
            f_trace         tracing function for this frame, or None
    """
def iscode(object):
    """
    Return true if the object is a code object.

        Code objects provide these attributes:
            co_argcount         number of arguments (not including *, ** args
                                or keyword only arguments)
            co_code             string of raw compiled bytecode
            co_cellvars         tuple of names of cell variables
            co_consts           tuple of constants used in the bytecode
            co_filename         name of file in which this code object was created
            co_firstlineno      number of first line in Python source code
            co_flags            bitmap: 1=optimized | 2=newlocals | 4=*arg | 8=**arg
                                | 16=nested | 32=generator | 64=nofree | 128=coroutine
                                | 256=iterable_coroutine | 512=async_generator
            co_freevars         tuple of names of free variables
            co_posonlyargcount  number of positional only arguments
            co_kwonlyargcount   number of keyword only arguments (not including ** arg)
            co_lnotab           encoded mapping of line numbers to bytecode indices
            co_name             name with which this code object was defined
            co_names            tuple of names of local variables
            co_nlocals          number of local variables
            co_stacksize        virtual machine stack space required
            co_varnames         tuple of names of arguments and local variables
    """
def isbuiltin(object):
    """
    Return true if the object is a built-in function or method.

        Built-in functions and methods provide these attributes:
            __doc__         documentation string
            __name__        original name of this function or method
            __self__        instance to which a method is bound, or None
    """
def isroutine(object):
    """
    Return true if the object is any kind of function or method.
    """
def isabstract(object):
    """
    Return true if the object is an abstract base class (ABC).
    """
def getmembers(object, predicate=None):
    """
    Return all members of an object as (name, value) pairs sorted by name.
        Optionally, only return members that satisfy a given predicate.
    """
def classify_class_attrs(cls):
    """
    Return list of attribute-descriptor tuples.

        For each name in dir(cls), the return list contains a 4-tuple
        with these elements:

            0. The name (a string).

            1. The kind of attribute this is, one of these strings:
                   'class method'    created via classmethod()
                   'static method'   created via staticmethod()
                   'property'        created via property()
                   'method'          any other flavor of method or descriptor
                   'data'            not a method

            2. The class which defined this attribute (a class).

            3. The object as obtained by calling getattr; if this fails, or if the
               resulting object does not live anywhere in the class' mro (including
               metaclasses) then the object is looked up in the defining class's
               dict (found by walking the mro).

        If one of the items in dir(cls) is stored in the metaclass it will now
        be discovered and not have None be listed as the class in which it was
        defined.  Any items whose home class cannot be discovered are skipped.
    
    """
def getmro(cls):
    """
    Return tuple of base classes (including cls) in method resolution order.
    """
def unwrap(func, *, stop=None):
    """
    Get the object wrapped by *func*.

       Follows the chain of :attr:`__wrapped__` attributes returning the last
       object in the chain.

       *stop* is an optional callback accepting an object in the wrapper chain
       as its sole argument that allows the unwrapping to be terminated early if
       the callback returns a true value. If the callback never returns a true
       value, the last object in the chain is returned as usual. For example,
       :func:`signature` uses this to stop unwrapping if any object in the
       chain has a ``__signature__`` attribute defined.

       :exc:`ValueError` is raised if a cycle is encountered.

    
    """
        def _is_wrapper(f):
            """
            '__wrapped__'
            """
        def _is_wrapper(f):
            """
            '__wrapped__'
            """
def indentsize(line):
    """
    Return the indent size, in spaces, at the start of a line of text.
    """
def _findclass(func):
    """
    '.'
    """
def _finddoc(obj):
    """
    '__func__'
    """
def getdoc(object):
    """
    Get the documentation string for an object.

        All tabs are expanded to spaces.  To clean up docstrings that are
        indented to line up with blocks of code, any whitespace than can be
        uniformly removed from the second line onwards is removed.
    """
def cleandoc(doc):
    """
    Clean up indentation from docstrings.

        Any whitespace that can be uniformly removed from the second line
        onwards is removed.
    """
def getfile(object):
    """
    Work out which source or compiled file an object was defined in.
    """
def getmodulename(path):
    """
    Return the module name for a given file, or None.
    """
def getsourcefile(object):
    """
    Return the filename that can be used to locate an object's source.
        Return None if no way can be identified to get the source.
    
    """
def getabsfile(object, _filename=None):
    """
    Return an absolute path to the source or compiled file for an object.

        The idea is for each object to have a unique origin, so this routine
        normalizes the result as much as possible.
    """
def getmodule(object, _filename=None):
    """
    Return the module an object was defined in, or None if not found.
    """
def findsource(object):
    """
    Return the entire source file and starting line number for an object.

        The argument may be a module, class, method, function, traceback, frame,
        or code object.  The source code is returned as a list of all the lines
        in the file and the line number indexes a line in that list.  An OSError
        is raised if the source code cannot be retrieved.
    """
def getcomments(object):
    """
    Get lines of comments immediately preceding an object's source code.

        Returns None when source can't be found.
    
    """
def EndOfBlock(Exception): pass
    """
    Provide a tokeneater() method to detect the end of a code block.
    """
    def __init__(self):
        """
         skip any decorators

        """
def getblock(lines):
    """
    Extract the block of code at the top of the given list of lines.
    """
def getsourcelines(object):
    """
    Return a list of source lines and starting line number for an object.

        The argument may be a module, class, method, function, traceback, frame,
        or code object.  The source code is returned as a list of the lines
        corresponding to the object and the line number indicates where in the
        original source file the first line of code was found.  An OSError is
        raised if the source code cannot be retrieved.
    """
def getsource(object):
    """
    Return the text of the source code for an object.

        The argument may be a module, class, method, function, traceback, frame,
        or code object.  The source code is returned as a single string.  An
        OSError is raised if the source code cannot be retrieved.
    """
def walktree(classes, children, parent):
    """
    Recursive helper function for getclasstree().
    """
def getclasstree(classes, unique=False):
    """
    Arrange the given list of classes into a hierarchy of nested lists.

        Where a nested list appears, it contains classes derived from the class
        whose entry immediately precedes the list.  Each entry is a 2-tuple
        containing a class and a tuple of its base classes.  If the 'unique'
        argument is true, exactly one entry appears in the returned structure
        for each class in the given list.  Otherwise, classes using multiple
        inheritance and their descendants will appear multiple times.
    """
def getargs(co):
    """
    Get information about the arguments accepted by a code object.

        Three things are returned: (args, varargs, varkw), where
        'args' is the list of argument names. Keyword-only arguments are
        appended. 'varargs' and 'varkw' are the names of the * and **
        arguments or None.
    """
def getargspec(func):
    """
    Get the names and default values of a function's parameters.

        A tuple of four things is returned: (args, varargs, keywords, defaults).
        'args' is a list of the argument names, including keyword-only argument names.
        'varargs' and 'keywords' are the names of the * and ** parameters or None.
        'defaults' is an n-tuple of the default values of the last n parameters.

        This function is deprecated, as it does not support annotations or
        keyword-only parameters and will raise ValueError if either is present
        on the supplied callable.

        For a more structured introspection API, use inspect.signature() instead.

        Alternatively, use getfullargspec() for an API with a similar namedtuple
        based interface, but full support for annotations and keyword-only
        parameters.

        Deprecated since Python 3.5, use `inspect.getfullargspec()`.
    
    """
def getfullargspec(func):
    """
    Get the names and default values of a callable object's parameters.

        A tuple of seven things is returned:
        (args, varargs, varkw, defaults, kwonlyargs, kwonlydefaults, annotations).
        'args' is a list of the parameter names.
        'varargs' and 'varkw' are the names of the * and ** parameters or None.
        'defaults' is an n-tuple of the default values of the last n parameters.
        'kwonlyargs' is a list of keyword-only parameter names.
        'kwonlydefaults' is a dictionary mapping names from kwonlyargs to defaults.
        'annotations' is a dictionary mapping parameter names to annotations.

        Notable differences from inspect.signature():
          - the "self" parameter is always reported, even for bound methods
          - wrapper chains defined by __wrapped__ *not* unwrapped automatically
    
    """
def getargvalues(frame):
    """
    Get information about arguments passed into a particular frame.

        A tuple of four things is returned: (args, varargs, varkw, locals).
        'args' is a list of the argument names.
        'varargs' and 'varkw' are the names of the * and ** arguments or None.
        'locals' is the locals dictionary of the given frame.
    """
def formatannotation(annotation, base_module=None):
    """
    '__module__'
    """
def formatannotationrelativeto(object):
    """
    '__module__'
    """
    def _formatannotation(annotation):
        """
        '*'
        """
    def formatargandannotation(arg):
        """
        ': '
        """
2021-03-02 20:53:46,224 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:46,224 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:46,225 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:46,225 : INFO : tokenize_signature : --> do i ever get here?
def formatargvalues(args, varargs, varkw, locals,
                    formatarg=str,
                    formatvarargs=lambda name: '*' + name,
                    formatvarkw=lambda name: '**' + name,
                    formatvalue=lambda value: '=' + repr(value)):
    """
    Format an argument spec from the 4 values returned by getargvalues.

        The first four arguments are (args, varargs, varkw, locals).  The
        next four arguments are the corresponding optional formatting functions
        that are called to turn names and values into strings.  The ninth
        argument is an optional function to format the sequence of arguments.
    """
2021-03-02 20:53:46,225 : INFO : tokenize_signature : --> do i ever get here?
    def convert(name, locals=locals,
                formatarg=formatarg, formatvalue=formatvalue):
        """
        '('
        """
def _missing_arguments(f_name, argnames, pos, values):
    """
    {} and {}
    """
def _too_many(f_name, args, kwonly, varargs, defcount, given, values):
    """
    at least %d
    """
def getcallargs(func, /, *positional, **named):
    """
    Get the mapping of arguments to values.

        A dict is returned, with keys the function argument names (including the
        names of the * and ** arguments, if any), and values the respective bound
        values from 'positional' and 'named'.
    """
def getclosurevars(func):
    """

        Get the mapping of free variables to their current values.

        Returns a named tuple of dicts mapping the current nonlocal, global
        and builtin references as seen by the body of the function. A final
        set of unbound names that could not be resolved is also provided.
    
    """
def getframeinfo(frame, context=1):
    """
    Get information about a frame or traceback object.

        A tuple of five things is returned: the filename, the line number of
        the current line, the function name, a list of lines of context from
        the source code, and the index of the current line within that list.
        The optional second argument specifies the number of lines of context
        to return, which are centered around the current line.
    """
def getlineno(frame):
    """
    Get the line number from a frame object, allowing for optimization.
    """
def getouterframes(frame, context=1):
    """
    Get a list of records for a frame and all higher (calling) frames.

        Each record contains a frame object, filename, line number, function
        name, a list of lines of context, and index within the context.
    """
def getinnerframes(tb, context=1):
    """
    Get a list of records for a traceback's frame and all lower frames.

        Each record contains a frame object, filename, line number, function
        name, a list of lines of context, and index within the context.
    """
def currentframe():
    """
    Return the frame of the caller or None if this is not possible.
    """
def stack(context=1):
    """
    Return a list of records for the stack above the caller's frame.
    """
def trace(context=1):
    """
    Return a list of records for the stack below the current exception.
    """
def _static_getmro(klass):
    """
    '__mro__'
    """
def _check_instance(obj, attr):
    """
    __dict__
    """
def _check_class(klass, attr):
    """
    __dict__
    """
def getattr_static(obj, attr, default=_sentinel):
    """
    Retrieve attributes without triggering dynamic lookup via the
           descriptor protocol,  __getattr__ or __getattribute__.

           Note: this function may not be able to retrieve all attributes
           that getattr can fetch (like dynamically created attributes)
           and may find attributes that getattr can't (like descriptors
           that raise AttributeError). It can also return descriptor objects
           instead of instance members in some cases. See the
           documentation for details.
    
    """
def getgeneratorstate(generator):
    """
    Get current state of a generator-iterator.

        Possible states are:
          GEN_CREATED: Waiting to start execution.
          GEN_RUNNING: Currently being executed by the interpreter.
          GEN_SUSPENDED: Currently suspended at a yield expression.
          GEN_CLOSED: Execution has completed.
    
    """
def getgeneratorlocals(generator):
    """

        Get the mapping of generator local variables to their current values.

        A dict is returned, with the keys the local variable names and values the
        bound values.
    """
def getcoroutinestate(coroutine):
    """
    Get current state of a coroutine object.

        Possible states are:
          CORO_CREATED: Waiting to start execution.
          CORO_RUNNING: Currently being executed by the interpreter.
          CORO_SUSPENDED: Currently suspended at an await expression.
          CORO_CLOSED: Execution has completed.
    
    """
def getcoroutinelocals(coroutine):
    """

        Get the mapping of coroutine local variables to their current values.

        A dict is returned, with the keys the local variable names and values the
        bound values.
    """
def _signature_get_user_defined_method(cls, method_name):
    """
    Private helper. Checks if ``cls`` has an attribute
        named ``method_name`` and returns it only if it is a
        pure python function.
    
    """
def _signature_get_partial(wrapped_sig, partial, extra_args=()):
    """
    Private helper to calculate how 'wrapped_sig' signature will
        look like after applying a 'functools.partial' object (or alike)
        on it.
    
    """
def _signature_bound_method(sig):
    """
    Private helper to transform signatures for unbound
        functions to bound methods.
    
    """
def _signature_is_builtin(obj):
    """
    Private helper to test if `obj` is a callable that might
        support Argument Clinic's __text_signature__ protocol.
    
    """
def _signature_is_functionlike(obj):
    """
    Private helper to test if `obj` is a duck type of FunctionType.
        A good example of such objects are functions compiled with
        Cython, which have all attributes that a pure Python function
        would have, but have their code statically compiled.
    
    """
def _signature_get_bound_param(spec):
    """
     Private helper to get first parameter name from a
        __text_signature__ of a builtin method, which should
        be in the following format: '($param1, ...)'.
        Assumptions are that the first argument won't have
        a default value or an annotation.
    
    """
def _signature_strip_non_python_syntax(signature):
    """

        Private helper function. Takes a signature in Argument Clinic's
        extended signature format.

        Returns a tuple of three things:
          * that signature re-rendered in standard Python syntax,
          * the index of the "self" parameter (generally 0), or None if
            the function does not have a "self" parameter, and
          * the index of the last "positional only" parameter,
            or None if the signature has no positional-only parameters.
    
    """
def _signature_fromstr(cls, obj, s, skip_bound_arg=True):
    """
    Private helper to parse content of '__text_signature__'
        and return a Signature based on it.
    
    """
    def parse_name(node):
        """
        Annotations are not currently supported
        """
    def wrap_value(s):
        """
        .
        """
        def visit_Name(self, node):
            """
             non-keyword-only parameters

            """
def _signature_from_builtin(cls, func, skip_bound_arg=True):
    """
    Private helper function to get signature for
        builtin callables.
    
    """
def _signature_from_function(cls, func, skip_bound_arg=True):
    """
    Private helper: constructs Signature for the given python function.
    """
2021-03-02 20:53:46,245 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:46,245 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:46,245 : INFO : tokenize_signature : --> do i ever get here?
def _signature_from_callable(obj, *,
                             follow_wrapper_chains=True,
                             skip_bound_arg=True,
                             sigcls):
    """
    Private helper function to get signature for arbitrary
        callable objects.
    
    """
def _void:
    """
    A private marker - used in Parameter & Signature.
    """
def _empty:
    """
    Marker object for Signature.empty and Parameter.empty.
    """
def _ParameterKind(enum.IntEnum):
    """
    'positional-only'
    """
def Parameter:
    """
    Represents a parameter in a function signature.

        Has the following public attributes:

        * name : str
            The name of the parameter as a string.
        * default : object
            The default value for the parameter if specified.  If the
            parameter has no default value, this attribute is set to
            `Parameter.empty`.
        * annotation
            The annotation for the parameter if specified.  If the
            parameter has no annotation, this attribute is set to
            `Parameter.empty`.
        * kind : str
            Describes how argument values are bound to the parameter.
            Possible values: `Parameter.POSITIONAL_ONLY`,
            `Parameter.POSITIONAL_OR_KEYWORD`, `Parameter.VAR_POSITIONAL`,
            `Parameter.KEYWORD_ONLY`, `Parameter.VAR_KEYWORD`.
    
    """
    def __init__(self, name, kind, *, default=_empty, annotation=_empty):
        """
        f'value {kind!r} is not a valid Parameter.kind'
        """
    def __reduce__(self):
        """
        '_default'
        """
    def __setstate__(self, state):
        """
        '_default'
        """
    def name(self):
        """
        Creates a customized copy of the Parameter.
        """
    def __str__(self):
        """
         Add annotation and default value

        """
    def __repr__(self):
        """
        '<{} "{}">'
        """
    def __hash__(self):
        """
        Result of `Signature.bind` call.  Holds the mapping of arguments
            to the function's parameters.

            Has the following public attributes:

            * arguments : OrderedDict
                An ordered mutable mapping of parameters' names to arguments' values.
                Does not contain arguments' default values.
            * signature : Signature
                The Signature object that created this instance.
            * args : tuple
                Tuple of positional arguments values.
            * kwargs : dict
                Dict of keyword arguments values.
    
        """
    def __init__(self, signature, arguments):
        """
         We're done here. Other arguments
         will be mapped in 'BoundArguments.kwargs'

        """
    def kwargs(self):
        """
         **kwargs

        """
    def apply_defaults(self):
        """
        Set default values for missing arguments.

                For variable-positional arguments (*args) the default is an
                empty tuple.

                For variable-keyword arguments (**kwargs) the default is an
                empty dict.
        
        """
    def __eq__(self, other):
        """
        '_signature'
        """
    def __getstate__(self):
        """
        '_signature'
        """
    def __repr__(self):
        """
        '{}={!r}'
        """
def Signature:
    """
    A Signature object represents the overall signature of a function.
        It stores a Parameter object for each parameter accepted by the
        function, as well as information specific to the function itself.

        A Signature object has the following public attributes and methods:

        * parameters : OrderedDict
            An ordered mapping of parameters' names to the corresponding
            Parameter objects (keyword-only arguments are in the same order
            as listed in `code.co_varnames`).
        * return_annotation : object
            The annotation for the return type of the function if specified.
            If the function has no annotation for its return type, this
            attribute is set to `Signature.empty`.
        * bind(*args, **kwargs) -> BoundArguments
            Creates a mapping from positional and keyword arguments to
            parameters.
        * bind_partial(*args, **kwargs) -> BoundArguments
            Creates a partial mapping from positional and keyword arguments
            to parameters (simulating 'functools.partial' behavior.)
    
    """
2021-03-02 20:53:46,255 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, parameters=None, *, return_annotation=_empty,
                 __validate_parameters__=True):
        """
        Constructs Signature from the given list of Parameter
                objects and 'return_annotation'.  All arguments are optional.
        
        """
    def from_function(cls, func):
        """
        Constructs Signature for the given python function.

                Deprecated since Python 3.5, use `Signature.from_callable()`.
        
        """
    def from_builtin(cls, func):
        """
        Constructs Signature for the given builtin function.

                Deprecated since Python 3.5, use `Signature.from_callable()`.
        
        """
    def from_callable(cls, obj, *, follow_wrapped=True):
        """
        Constructs Signature for the given callable object.
        """
    def parameters(self):
        """
        Creates a customized copy of the Signature.
                Pass 'parameters' and/or 'return_annotation' arguments
                to override them in the new copy.
        
        """
    def _hash_basis(self):
        """
        Private method. Don't use directly.
        """
    def bind(self, /, *args, **kwargs):
        """
        Get a BoundArguments object, that maps the passed `args`
                and `kwargs` to the function's signature.  Raises `TypeError`
                if the passed arguments can not be bound.
        
        """
    def bind_partial(self, /, *args, **kwargs):
        """
        Get a BoundArguments object, that partially maps the
                passed `args` and `kwargs` to the function's signature.
                Raises `TypeError` if the passed arguments can not be bound.
        
        """
    def __reduce__(self):
        """
        '_return_annotation'
        """
    def __setstate__(self, state):
        """
        '_return_annotation'
        """
    def __repr__(self):
        """
        '<{} {}>'
        """
    def __str__(self):
        """
         It's not a positional-only parameter, and the flag
         is set to 'True' (there were pos-only params before.)

        """
def signature(obj, *, follow_wrapped=True):
    """
    Get a signature object for the passed callable.
    """
def _main():
    """
     Logic for inspecting an object given at command line 
    """
