def Error(Exception):
    """
     backward compatibility
    """
def copy(x):
    """
    Shallow copy operation on arbitrary Python objects.

        See the module's __doc__ string for more info.
    
    """
def _copy_immutable(x):
    """
    CodeType
    """
def deepcopy(x, memo=None, _nil=[]):
    """
    Deep copy operation on arbitrary Python objects.

        See the module's __doc__ string for more info.
    
    """
def _deepcopy_atomic(x, memo):
    """
     We're not going to put the tuple in the memo, but it's still important we
     check for it, in case the tuple contains recursive mutable structures.

    """
def _deepcopy_dict(x, memo, deepcopy=deepcopy):
    """
     Copy instance methods
    """
def _keep_alive(x, memo):
    """
    Keeps a reference to the object x in the memo.

        Because we remember objects by their id, we have
        to assure that possibly temporary objects are kept
        alive by referencing them.
        We store a reference at the id of the memo, which should
        normally not be used unless someone tries to deepcopy
        the memo itself...
    
    """
2021-03-02 20:53:33,397 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:33,398 : INFO : tokenize_signature : --> do i ever get here?
def _reconstruct(x, memo, func, args,
                 state=None, listiter=None, dictiter=None,
                 deepcopy=deepcopy):
    """
    '__setstate__'
    """
