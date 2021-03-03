def CancelledError(BaseException):
    """
    The Future or Task was cancelled.
    """
def TimeoutError(Exception):
    """
    The operation exceeded the given deadline.
    """
def InvalidStateError(Exception):
    """
    The operation is not allowed in this state.
    """
def SendfileNotAvailableError(RuntimeError):
    """
    Sendfile syscall is not available.

        Raised if OS does not support sendfile syscall for given socket or
        file type.
    
    """
def IncompleteReadError(EOFError):
    """

        Incomplete read error. Attributes:

        - partial: read bytes string before the end of stream was reached
        - expected: total number of expected bytes (or None if unknown)
    
    """
    def __init__(self, partial, expected):
        """
        f'{len(partial)} bytes read on a total of '
        f'{expected!r} expected bytes'
        """
    def __reduce__(self):
        """
        Reached the buffer limit while looking for a separator.

            Attributes:
            - consumed: total number of to be consumed bytes.
    
        """
    def __init__(self, message, consumed):
