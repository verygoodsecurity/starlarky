def StrongRandom(object):
    """
    Cannot specify both 'rng' and 'randfunc'
    """
    def getrandbits(self, k):
        """
        Return an integer with k random bits.
        """
    def randrange(self, *args):
        """
        randrange([start,] stop[, step]):
                Return a randomly-selected element from range(start, stop, step).
        """
    def randint(self, a, b):
        """
        Return a random integer N such that a <= N <= b.
        """
    def choice(self, seq):
        """
        Return a random element from a (non-empty) sequence.

                If the seqence is empty, raises IndexError.
        
        """
    def shuffle(self, x):
        """
        Shuffle the sequence in place.
        """
    def sample(self, population, k):
        """
        Return a k-length list of unique elements chosen from the population sequence.
        """
