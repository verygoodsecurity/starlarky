def pad(data_to_pad, block_size, style='pkcs7'):
    """
    Apply standard padding.

        Args:
          data_to_pad (byte string):
            The data that needs to be padded.
          block_size (integer):
            The block boundary to use for padding. The output length is guaranteed
            to be a multiple of :data:`block_size`.
          style (string):
            Padding algorithm. It can be *'pkcs7'* (default), *'iso7816'* or *'x923'*.

        Return:
          byte string : the original data with the appropriate padding added at the end.
    
    """
def unpad(padded_data, block_size, style='pkcs7'):
    """
    Remove standard padding.

        Args:
          padded_data (byte string):
            A piece of data with padding that needs to be stripped.
          block_size (integer):
            The block boundary to use for padding. The input length
            must be a multiple of :data:`block_size`.
          style (string):
            Padding algorithm. It can be *'pkcs7'* (default), *'iso7816'* or *'x923'*.
        Return:
            byte string : data without padding.
        Raises:
          ValueError: if the padding is incorrect.
    
    """
