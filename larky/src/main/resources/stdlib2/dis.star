def _try_compile(source, name):
    """
    Attempts to compile the given source, first as an expression and
           then as a statement if the first approach fails.

           Utility function to accept strings in functions that otherwise
           expect code objects
    
    """
def dis(x=None, *, file=None, depth=None):
    """
    Disassemble classes, methods, functions, and other compiled objects.

        With no argument, disassemble the last traceback.

        Compiled objects currently include generator objects, async generator
        objects, and coroutine objects, all of which store their code object
        in a special attribute.
    
    """
def distb(tb=None, *, file=None):
    """
    Disassemble a traceback (default: last traceback).
    """
def pretty_flags(flags):
    """
    Return pretty representation of code flags.
    """
def _get_code_object(x):
    """
    Helper to handle methods, compiled or raw code objects, and strings.
    """
def code_info(x):
    """
    Formatted details of methods, functions, or code.
    """
def _format_code_info(co):
    """
    Name:              %s
    """
def show_code(co, *, file=None):
    """
    Print details of methods, functions, or code to *file*.

        If *file* is not provided, the output is printed on stdout.
    
    """
def Instruction(_Instruction):
    """
    Details for a bytecode operation

           Defined fields:
             opname - human readable name for operation
             opcode - numeric code for operation
             arg - numeric argument to operation (if any), otherwise None
             argval - resolved arg value (if known), otherwise same as arg
             argrepr - human readable description of operation argument
             offset - start index of operation within bytecode sequence
             starts_line - line started by this opcode (if any), otherwise None
             is_jump_target - True if other code jumps to here, otherwise False
    
    """
    def _disassemble(self, lineno_width=3, mark_as_current=False, offset_width=4):
        """
        Format instruction details for inclusion in disassembly output

                *lineno_width* sets the width of the line number field (0 omits it)
                *mark_as_current* inserts a '-->' marker arrow as part of the line
                *offset_width* sets the width of the instruction offset field
        
        """
def get_instructions(x, *, first_line=None):
    """
    Iterator for the opcodes in methods, functions or code

        Generates a series of Instruction named tuples giving the details of
        each operations in the supplied code.

        If *first_line* is not None, it indicates the line number that should
        be reported for the first source line in the disassembled code.
        Otherwise, the source line information (if any) is taken directly from
        the disassembled code object.
    
    """
def _get_const_info(const_index, const_list):
    """
    Helper to get optional details about const references

           Returns the dereferenced constant and its repr if the constant
           list is defined.
           Otherwise returns the constant index and its repr().
    
    """
def _get_name_info(name_index, name_list):
    """
    Helper to get optional details about named references

           Returns the dereferenced name as both value and repr if the name
           list is defined.
           Otherwise returns the name index and its repr().
    
    """
2021-03-02 20:46:40,234 : INFO : tokenize_signature : --> do i ever get here?
def _get_instructions_bytes(code, varnames=None, names=None, constants=None,
                      cells=None, linestarts=None, line_offset=0):
    """
    Iterate over the instructions in a bytecode string.

        Generates a sequence of Instruction namedtuples giving the details of each
        opcode.  Additional information about the code's runtime environment
        (e.g. variable names, constants) can be specified using optional
        arguments.

    
    """
def disassemble(co, lasti=-1, *, file=None):
    """
    Disassemble a code object.
    """
def _disassemble_recursive(co, *, file=None, depth=None):
    """
    'co_code'
    """
2021-03-02 20:46:40,237 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:40,237 : INFO : tokenize_signature : --> do i ever get here?
def _disassemble_bytes(code, lasti=-1, varnames=None, names=None,
                       constants=None, cells=None, linestarts=None,
                       *, file=None, line_offset=0):
    """
     Omit the line number column entirely if we have no line number info

    """
def _disassemble_str(source, **kwargs):
    """
    Compile the source string, then disassemble the code object.
    """
def _unpack_opargs(code):
    """
    Detect all offsets in a byte code which are jump targets.

        Return the list of offsets.

    
    """
def findlinestarts(code):
    """
    Find the offsets in a byte code which are start of lines in the source.

        Generate pairs (offset, lineno) as described in Python/compile.c.

    
    """
def Bytecode:
    """
    The bytecode operations of a piece of code

        Instantiate this with a function, method, other compiled object, string of
        code, or a code object (as returned by compile()).

        Iterating over this yields the bytecode operations as Instruction instances.
    
    """
    def __init__(self, x, *, first_line=None, current_offset=None):
        """
        {}({!r})
        """
    def from_traceback(cls, tb):
        """
         Construct a Bytecode from the given traceback 
        """
    def info(self):
        """
        Return formatted information about the code object.
        """
    def dis(self):
        """
        Return a formatted view of the bytecode operations.
        """
def _test():
    """
    Simple test program to disassemble a file.
    """
