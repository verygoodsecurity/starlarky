def async_chat(asyncore.dispatcher):
    """
    This is an abstract class.  You must derive from this class, and add
        the two methods collect_incoming_data() and found_terminator()
    """
    def __init__(self, sock=None, map=None):
        """
         for string terminator matching

        """
    def collect_incoming_data(self, data):
        """
        must be implemented in subclass
        """
    def _collect_incoming_data(self, data):
        """
        b''
        """
    def found_terminator(self):
        """
        must be implemented in subclass
        """
    def set_terminator(self, term):
        """
        Set the input delimiter.

                Can be a fixed string of any length, an integer, or None.
        
        """
    def get_terminator(self):
        """
         grab some more data from the socket,
         throw it to the collector method,
         check for the terminator,
         if found, transition to the next state.


        """
    def handle_read(self):
        """
         Continue to search for self.terminator in self.ac_in_buffer,
         while calling self.collect_incoming_data.  The while loop
         is necessary because we might read several data+terminator
         combos with a single recv(4096).


        """
    def handle_write(self):
        """
        'data argument must be byte-ish (%r)'
        """
    def push_with_producer(self, producer):
        """
        predicate for inclusion in the readable for select()
        """
    def writable(self):
        """
        predicate for inclusion in the writable for select()
        """
    def close_when_done(self):
        """
        automatically close this channel once the outgoing queue is empty
        """
    def initiate_send(self):
        """
         handle empty string/buffer or None entry

        """
    def discard_buffers(self):
        """
         Emergencies only!

        """
def simple_producer:
    """
    b''
    """
def find_prefix_at_end(haystack, needle):
