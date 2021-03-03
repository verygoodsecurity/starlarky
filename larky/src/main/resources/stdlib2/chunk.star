def Chunk:
    """
     whether to align to word (2-byte) boundaries
    """
    def getname(self):
        """
        Return the name (ID) of the current chunk.
        """
    def getsize(self):
        """
        Return the size of the current chunk.
        """
    def close(self):
        """
        I/O operation on closed file
        """
    def seek(self, pos, whence=0):
        """
        Seek to specified position into the chunk.
                Default position is 0 (start of chunk).
                If the file is not seekable, this will result in an error.
        
        """
    def tell(self):
        """
        I/O operation on closed file
        """
    def read(self, size=-1):
        """
        Read at most size bytes from the chunk.
                If size is omitted or negative, read until the end
                of the chunk.
        
        """
    def skip(self):
        """
        Skip the rest of the chunk.
                If you are not interested in the contents of the chunk,
                this method should be called so that the file points to
                the start of the next chunk.
        
        """
