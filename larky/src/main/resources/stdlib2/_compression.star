def BaseStream(io.BufferedIOBase):
    """
    Mode-checking helper functions.
    """
    def _check_not_closed(self):
        """
        I/O operation on closed file
        """
    def _check_can_read(self):
        """
        File not open for reading
        """
    def _check_can_write(self):
        """
        File not open for writing
        """
    def _check_can_seek(self):
        """
        Seeking is only supported 
        on files open for reading
        """
def DecompressReader(io.RawIOBase):
    """
    Adapts the decompressor API to a RawIOBase reader API
    """
    def readable(self):
        """
         Current offset in decompressed stream
        """
    def close(self):
        """
        B
        """
    def read(self, size=-1):
        """
        b
        """
    def _rewind(self):
        """
         Recalculate offset as an absolute file position.

        """
    def tell(self):
        """
        Return the current file position.
        """
