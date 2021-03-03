def Mailbox:
    """
    A group of messages in a particular place.
    """
    def __init__(self, path, factory=None, create=True):
        """
        Initialize a Mailbox instance.
        """
    def add(self, message):
        """
        Add message and return assigned key.
        """
    def remove(self, key):
        """
        Remove the keyed message; raise KeyError if it doesn't exist.
        """
    def __delitem__(self, key):
        """
        If the keyed message exists, remove it.
        """
    def __setitem__(self, key, message):
        """
        Replace the keyed message; raise KeyError if it doesn't exist.
        """
    def get(self, key, default=None):
        """
        Return the keyed message, or default if it doesn't exist.
        """
    def __getitem__(self, key):
        """
        Return the keyed message; raise KeyError if it doesn't exist.
        """
    def get_message(self, key):
        """
        Return a Message representation or raise a KeyError.
        """
    def get_string(self, key):
        """
        Return a string representation or raise a KeyError.

                Uses email.message.Message to create a 7bit clean string
                representation of the message.
        """
    def get_bytes(self, key):
        """
        Return a byte string representation or raise a KeyError.
        """
    def get_file(self, key):
        """
        Return a file-like representation or raise a KeyError.
        """
    def iterkeys(self):
        """
        Return an iterator over keys.
        """
    def keys(self):
        """
        Return a list of keys.
        """
    def itervalues(self):
        """
        Return an iterator over all messages.
        """
    def __iter__(self):
        """
        Return a list of messages. Memory intensive.
        """
    def iteritems(self):
        """
        Return an iterator over (key, message) tuples.
        """
    def items(self):
        """
        Return a list of (key, message) tuples. Memory intensive.
        """
    def __contains__(self, key):
        """
        Return True if the keyed message exists, False otherwise.
        """
    def __len__(self):
        """
        Return a count of messages in the mailbox.
        """
    def clear(self):
        """
        Delete all messages.
        """
    def pop(self, key, default=None):
        """
        Delete the keyed message and return it, or default.
        """
    def popitem(self):
        """
        Delete an arbitrary (key, message) pair and return it.
        """
    def update(self, arg=None):
        """
        Change the messages that correspond to certain keys.
        """
    def flush(self):
        """
        Write any pending changes to the disk.
        """
    def lock(self):
        """
        Lock the mailbox.
        """
    def unlock(self):
        """
        Unlock the mailbox if it is locked.
        """
    def close(self):
        """
        Flush and close the mailbox.
        """
    def _string_to_bytes(self, message):
        """
         If a message is not 7bit clean, we refuse to handle it since it
         likely came from reading invalid messages in text mode, and that way
         lies mojibake.

        """
    def _dump_message(self, message, target, mangle_from_=False):
        """
         This assumes the target file is open in binary mode.

        """
def Maildir(Mailbox):
    """
    A qmail-style Maildir mailbox.
    """
    def __init__(self, dirname, factory=None, create=True):
        """
        Initialize a Maildir instance.
        """
    def add(self, message):
        """
        Add message and return assigned key.
        """
    def remove(self, key):
        """
        Remove the keyed message; raise KeyError if it doesn't exist.
        """
    def discard(self, key):
        """
        If the keyed message exists, remove it.
        """
    def __setitem__(self, key, message):
        """
        Replace the keyed message; raise KeyError if it doesn't exist.
        """
    def get_message(self, key):
        """
        Return a Message representation or raise a KeyError.
        """
    def get_bytes(self, key):
        """
        Return a bytes representation or raise a KeyError.
        """
    def get_file(self, key):
        """
        Return a file-like representation or raise a KeyError.
        """
    def iterkeys(self):
        """
        Return an iterator over keys.
        """
    def __contains__(self, key):
        """
        Return True if the keyed message exists, False otherwise.
        """
    def __len__(self):
        """
        Return a count of messages in the mailbox.
        """
    def flush(self):
        """
        Write any pending changes to disk.
        """
    def lock(self):
        """
        Lock the mailbox.
        """
    def unlock(self):
        """
        Unlock the mailbox if it is locked.
        """
    def close(self):
        """
        Flush and close the mailbox.
        """
    def list_folders(self):
        """
        Return a list of folder names.
        """
    def get_folder(self, folder):
        """
        Return a Maildir instance for the named folder.
        """
    def add_folder(self, folder):
        """
        Create a folder and return a Maildir instance representing it.
        """
    def remove_folder(self, folder):
        """
        Delete the named folder, which must be empty.
        """
    def clean(self):
        """
        Delete old files in "tmp".
        """
    def _create_tmp(self):
        """
        Create a file in the tmp subdirectory and open and return it.
        """
    def _refresh(self):
        """
        Update table of contents mapping.
        """
    def _lookup(self, key):
        """
        Use TOC to return subpath for given key, or raise a KeyError.
        """
    def next(self):
        """
        Return the next message in a one-time iteration.
        """
def _singlefileMailbox(Mailbox):
    """
    A single-file mailbox.
    """
    def __init__(self, path, factory=None, create=True):
        """
        Initialize a single-file mailbox.
        """
    def add(self, message):
        """
        Add message and return assigned key.
        """
    def remove(self, key):
        """
        Remove the keyed message; raise KeyError if it doesn't exist.
        """
    def __setitem__(self, key, message):
        """
        Replace the keyed message; raise KeyError if it doesn't exist.
        """
    def iterkeys(self):
        """
        Return an iterator over keys.
        """
    def __contains__(self, key):
        """
        Return True if the keyed message exists, False otherwise.
        """
    def __len__(self):
        """
        Return a count of messages in the mailbox.
        """
    def lock(self):
        """
        Lock the mailbox.
        """
    def unlock(self):
        """
        Unlock the mailbox if it is locked.
        """
    def flush(self):
        """
        Write any pending changes to disk.
        """
    def _pre_mailbox_hook(self, f):
        """
        Called before writing the mailbox to file f.
        """
    def _pre_message_hook(self, f):
        """
        Called before writing each message to file f.
        """
    def _post_message_hook(self, f):
        """
        Called after writing each message to file f.
        """
    def close(self):
        """
        Flush and close the mailbox.
        """
    def _lookup(self, key=None):
        """
        Return (start, stop) or raise KeyError.
        """
    def _append_message(self, message):
        """
        Append message to mailbox and return (start, stop) offsets.
        """
def _mboxMMDF(_singlefileMailbox):
    """
    An mbox or MMDF mailbox.
    """
    def get_message(self, key):
        """
        Return a Message representation or raise a KeyError.
        """
    def get_string(self, key, from_=False):
        """
        Return a string representation or raise a KeyError.
        """
    def get_bytes(self, key, from_=False):
        """
        Return a string representation or raise a KeyError.
        """
    def get_file(self, key, from_=False):
        """
        Return a file-like representation or raise a KeyError.
        """
    def _install_message(self, message):
        """
        Format a message and blindly write to self._file.
        """
def mbox(_mboxMMDF):
    """
    A classic mbox mailbox.
    """
    def __init__(self, path, factory=None, create=True):
        """
        Initialize an mbox mailbox.
        """
    def _post_message_hook(self, f):
        """
        Called after writing each message to file f.
        """
    def _generate_toc(self):
        """
        Generate key-to-(start, stop) table of contents.
        """
def MMDF(_mboxMMDF):
    """
    An MMDF mailbox.
    """
    def __init__(self, path, factory=None, create=True):
        """
        Initialize an MMDF mailbox.
        """
    def _pre_message_hook(self, f):
        """
        Called before writing each message to file f.
        """
    def _post_message_hook(self, f):
        """
        Called after writing each message to file f.
        """
    def _generate_toc(self):
        """
        Generate key-to-(start, stop) table of contents.
        """
def MH(Mailbox):
    """
    An MH mailbox.
    """
    def __init__(self, path, factory=None, create=True):
        """
        Initialize an MH instance.
        """
    def add(self, message):
        """
        Add message and return assigned key.
        """
    def remove(self, key):
        """
        Remove the keyed message; raise KeyError if it doesn't exist.
        """
    def __setitem__(self, key, message):
        """
        Replace the keyed message; raise KeyError if it doesn't exist.
        """
    def get_message(self, key):
        """
        Return a Message representation or raise a KeyError.
        """
    def get_bytes(self, key):
        """
        Return a bytes representation or raise a KeyError.
        """
    def get_file(self, key):
        """
        Return a file-like representation or raise a KeyError.
        """
    def iterkeys(self):
        """
        Return an iterator over keys.
        """
    def __contains__(self, key):
        """
        Return True if the keyed message exists, False otherwise.
        """
    def __len__(self):
        """
        Return a count of messages in the mailbox.
        """
    def lock(self):
        """
        Lock the mailbox.
        """
    def unlock(self):
        """
        Unlock the mailbox if it is locked.
        """
    def flush(self):
        """
        Write any pending changes to the disk.
        """
    def close(self):
        """
        Flush and close the mailbox.
        """
    def list_folders(self):
        """
        Return a list of folder names.
        """
    def get_folder(self, folder):
        """
        Return an MH instance for the named folder.
        """
    def add_folder(self, folder):
        """
        Create a folder and return an MH instance representing it.
        """
    def remove_folder(self, folder):
        """
        Delete the named folder, which must be empty.
        """
    def get_sequences(self):
        """
        Return a name-to-key-list dictionary to define each sequence.
        """
    def set_sequences(self, sequences):
        """
        Set sequences using the given name-to-key-list dictionary.
        """
    def pack(self):
        """
        Re-name messages to eliminate numbering gaps. Invalidates keys.
        """
    def _dump_sequences(self, message, key):
        """
        Inspect a new MHMessage and update sequences appropriately.
        """
def Babyl(_singlefileMailbox):
    """
    An Rmail-style Babyl mailbox.
    """
    def __init__(self, path, factory=None, create=True):
        """
        Initialize a Babyl mailbox.
        """
    def add(self, message):
        """
        Add message and return assigned key.
        """
    def remove(self, key):
        """
        Remove the keyed message; raise KeyError if it doesn't exist.
        """
    def __setitem__(self, key, message):
        """
        Replace the keyed message; raise KeyError if it doesn't exist.
        """
    def get_message(self, key):
        """
        Return a Message representation or raise a KeyError.
        """
    def get_bytes(self, key):
        """
        Return a string representation or raise a KeyError.
        """
    def get_file(self, key):
        """
        Return a file-like representation or raise a KeyError.
        """
    def get_labels(self):
        """
        Return a list of user-defined labels in the mailbox.
        """
    def _generate_toc(self):
        """
        Generate key-to-(start, stop) table of contents.
        """
    def _pre_mailbox_hook(self, f):
        """
        Called before writing the mailbox to file f.
        """
    def _pre_message_hook(self, f):
        """
        Called before writing each message to file f.
        """
    def _post_message_hook(self, f):
        """
        Called after writing each message to file f.
        """
    def _install_message(self, message):
        """
        Write message contents and return (start, stop).
        """
def Message(email.message.Message):
    """
    Message with mailbox-format-specific properties.
    """
    def __init__(self, message=None):
        """
        Initialize a Message instance.
        """
    def _become_message(self, message):
        """
        Assume the non-format-specific state of message.
        """
    def _explain_to(self, message):
        """
        Copy format-specific state to message insofar as possible.
        """
def MaildirMessage(Message):
    """
    Message with Maildir-specific properties.
    """
    def __init__(self, message=None):
        """
        Initialize a MaildirMessage instance.
        """
    def get_subdir(self):
        """
        Return 'new' or 'cur'.
        """
    def set_subdir(self, subdir):
        """
        Set subdir to 'new' or 'cur'.
        """
    def get_flags(self):
        """
        Return as a string the flags that are set.
        """
    def set_flags(self, flags):
        """
        Set the given flags and unset all others.
        """
    def add_flag(self, flag):
        """
        Set the given flag(s) without changing others.
        """
    def remove_flag(self, flag):
        """
        Unset the given string flag(s) without changing others.
        """
    def get_date(self):
        """
        Return delivery date of message, in seconds since the epoch.
        """
    def set_date(self, date):
        """
        Set delivery date of message, in seconds since the epoch.
        """
    def get_info(self):
        """
        Get the message's "info" as a string.
        """
    def set_info(self, info):
        """
        Set the message's "info" string.
        """
    def _explain_to(self, message):
        """
        Copy Maildir-specific state to message insofar as possible.
        """
def _mboxMMDFMessage(Message):
    """
    Message with mbox- or MMDF-specific properties.
    """
    def __init__(self, message=None):
        """
        Initialize an mboxMMDFMessage instance.
        """
    def get_from(self):
        """
        Return contents of "From " line.
        """
    def set_from(self, from_, time_=None):
        """
        Set "From " line, formatting and appending time_ if specified.
        """
    def get_flags(self):
        """
        Return as a string the flags that are set.
        """
    def set_flags(self, flags):
        """
        Set the given flags and unset all others.
        """
    def add_flag(self, flag):
        """
        Set the given flag(s) without changing others.
        """
    def remove_flag(self, flag):
        """
        Unset the given string flag(s) without changing others.
        """
    def _explain_to(self, message):
        """
        Copy mbox- or MMDF-specific state to message insofar as possible.
        """
def mboxMessage(_mboxMMDFMessage):
    """
    Message with mbox-specific properties.
    """
def MHMessage(Message):
    """
    Message with MH-specific properties.
    """
    def __init__(self, message=None):
        """
        Initialize an MHMessage instance.
        """
    def get_sequences(self):
        """
        Return a list of sequences that include the message.
        """
    def set_sequences(self, sequences):
        """
        Set the list of sequences that include the message.
        """
    def add_sequence(self, sequence):
        """
        Add sequence to list of sequences including the message.
        """
    def remove_sequence(self, sequence):
        """
        Remove sequence from the list of sequences including the message.
        """
    def _explain_to(self, message):
        """
        Copy MH-specific state to message insofar as possible.
        """
def BabylMessage(Message):
    """
    Message with Babyl-specific properties.
    """
    def __init__(self, message=None):
        """
        Initialize a BabylMessage instance.
        """
    def get_labels(self):
        """
        Return a list of labels on the message.
        """
    def set_labels(self, labels):
        """
        Set the list of labels on the message.
        """
    def add_label(self, label):
        """
        Add label to list of labels on the message.
        """
    def remove_label(self, label):
        """
        Remove label from the list of labels on the message.
        """
    def get_visible(self):
        """
        Return a Message representation of visible headers.
        """
    def set_visible(self, visible):
        """
        Set the Message representation of visible headers.
        """
    def update_visible(self):
        """
        Update and/or sensibly generate a set of visible headers.
        """
    def _explain_to(self, message):
        """
        Copy Babyl-specific state to message insofar as possible.
        """
def MMDFMessage(_mboxMMDFMessage):
    """
    Message with MMDF-specific properties.
    """
def _ProxyFile:
    """
    A read-only wrapper of a file.
    """
    def __init__(self, f, pos=None):
        """
        Initialize a _ProxyFile.
        """
    def read(self, size=None):
        """
        Read bytes.
        """
    def read1(self, size=None):
        """
        Read bytes.
        """
    def readline(self, size=None):
        """
        Read a line.
        """
    def readlines(self, sizehint=None):
        """
        Read multiple lines.
        """
    def __iter__(self):
        """
        Iterate over lines.
        """
    def tell(self):
        """
        Return the position.
        """
    def seek(self, offset, whence=0):
        """
        Change position.
        """
    def close(self):
        """
        Close the file.
        """
    def _read(self, size, read_method):
        """
        Read size bytes using read_method.
        """
    def __enter__(self):
        """
        Context management protocol support.
        """
    def __exit__(self, *exc):
        """
        '_file'
        """
def _PartialFile(_ProxyFile):
    """
    A read-only wrapper of part of a file.
    """
    def __init__(self, f, start=None, stop=None):
        """
        Initialize a _PartialFile.
        """
    def tell(self):
        """
        Return the position with respect to start.
        """
    def seek(self, offset, whence=0):
        """
        Change position, possibly with respect to start or stop.
        """
    def _read(self, size, read_method):
        """
        Read size bytes using read_method, honoring start and stop.
        """
    def close(self):
        """
         do *not* close the underlying file object for partial files,
         since it's global to the mailbox object

        """
def _lock_file(f, dotlock=True):
    """
    Lock file f using lockf and dot locking.
    """
def _unlock_file(f):
    """
    Unlock file f using lockf and dot locking.
    """
def _create_carefully(path):
    """
    Create a file if it doesn't exist and open for reading and writing.
    """
def _create_temporary(path):
    """
    Create a temp file based on path and open for reading and writing.
    """
def _sync_flush(f):
    """
    Ensure changes to file f are physically on disk.
    """
def _sync_close(f):
    """
    Close file f, ensuring all changes are physically on disk.
    """
def Error(Exception):
    """
    Raised for module-specific errors.
    """
def NoSuchMailboxError(Error):
    """
    The specified mailbox does not exist and won't be created.
    """
def NotEmptyError(Error):
    """
    The specified mailbox is not empty and deletion was requested.
    """
def ExternalClashError(Error):
    """
    Another process caused an action to fail.
    """
def FormatError(Error):
    """
    A file appears to have an invalid format.
    """
