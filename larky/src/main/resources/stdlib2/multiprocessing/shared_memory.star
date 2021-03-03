def _make_filename():
    """
    Create a random filename for the shared memory object.
    """
def SharedMemory:
    """
    Creates a new shared memory block or attaches to an existing
        shared memory block.

        Every shared memory block is assigned a unique name.  This enables
        one process to create a shared memory block with a particular name
        so that a different process can attach to that same shared memory
        block using that same name.

        As a resource for sharing data across processes, shared memory blocks
        may outlive the original process that created them.  When one process
        no longer needs access to a shared memory block that might still be
        needed by other processes, the close() method should be called.
        When a shared memory block is no longer needed by any process, the
        unlink() method should be called to ensure proper cleanup.
    """
    def __init__(self, name=None, create=False, size=0):
        """
        'size' must be a positive integer
        """
    def __del__(self):
        """
        f'{self.__class__.__name__}({self.name!r}, size={self.size})'
        """
    def buf(self):
        """
        A memoryview of contents of the shared memory block.
        """
    def name(self):
        """
        Unique name that identifies the shared memory block.
        """
    def size(self):
        """
        Size in bytes.
        """
    def close(self):
        """
        Closes access to the shared memory from this instance but does
                not destroy the shared memory block.
        """
    def unlink(self):
        """
        Requests that the underlying shared memory block be destroyed.

                In order to ensure proper cleanup of resources, unlink should be
                called once (and only once) across all processes which have access
                to the shared memory block.
        """
def ShareableList:
    """
    Pattern for a mutable list-like object shareable via a shared
        memory block.  It differs from the built-in list type in that these
        lists can not change their overall length (i.e. no append, insert,
        etc.)

        Because values are packed into a memoryview as bytes, the struct
        packing format for any storable value must require no more than 8
        characters to describe its format.
    """
    def _extract_recreation_code(value):
        """
        Used in concert with _back_transforms_mapping to convert values
                into the appropriate Python objects when retrieving them from
                the list as well as when storing them.
        """
    def __init__(self, sequence=None, *, name=None):
        """
        s
        """
    def _get_packing_format(self, position):
        """
        Gets the packing format for a single value stored in the list.
        """
    def _get_back_transform(self, position):
        """
        Gets the back transformation function for a single value.
        """
    def _set_packing_format_and_transform(self, position, fmt_as_str, value):
        """
        Sets the packing format and back transformation code for a
                single value in the list at the specified position.
        """
    def __getitem__(self, position):
        """
        index out of range
        """
    def __setitem__(self, position, value):
        """
        assignment index out of range
        """
    def __reduce__(self):
        """
        q
        """
    def __repr__(self):
        """
        f'{self.__class__.__name__}({list(self)}, name={self.shm.name!r})'
        """
    def format(self):
        """
        The struct packing format used by all currently stored values.
        """
    def _format_size_metainfo(self):
        """
        The struct packing format used for metainfo on storage sizes.
        """
    def _format_packing_metainfo(self):
        """
        The struct packing format used for the values' packing formats.
        """
    def _format_back_transform_codes(self):
        """
        The struct packing format used for the values' back transforms.
        """
    def _offset_data_start(self):
        """
         8 bytes per "q
        """
    def _offset_packing_formats(self):
        """
        L.count(value) -> integer -- return number of occurrences of value.
        """
    def index(self, value):
        """
        L.index(value) -> integer -- return first index of value.
                Raises ValueError if the value is not present.
        """
