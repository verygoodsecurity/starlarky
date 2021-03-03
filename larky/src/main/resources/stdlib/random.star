def Random(_random.Random):
    """
    Random number generator base class used by bound module functions.

        Used to instantiate instances of Random to get generators that don't
        share state.

        Class Random can also be subclassed if you want to use a different basic
        generator of your own devising: in that case, override the following
        methods:  random(), seed(), getstate(), and setstate().
        Optionally, implement a getrandbits() method so that randrange()
        can cover arbitrarily large ranges.

    
    """
    def __init__(self, x=None):
        """
        Initialize an instance.

                Optional argument x controls seeding, as for Random.seed().
        
        """
    def __init_subclass__(cls, /, **kwargs):
        """
        Control how subclasses generate random integers.

                The algorithm a subclass can use depends on the random() and/or
                getrandbits() implementation available to it and determines
                whether it can generate random integers from arbitrarily large
                ranges.
        
        """
    def seed(self, a=None, version=2):
        """
        Initialize internal state from hashable object.

                None or no argument seeds from current time or from an operating
                system specific randomness source if available.

                If *a* is an int, all bits are used.

                For version 2 (the default), all of the bits are used if *a* is a str,
                bytes, or bytearray.  For version 1 (provided for reproducing random
                sequences from older versions of Python), the algorithm for str and
                bytes generates a narrower range of seeds.

        
        """
    def getstate(self):
        """
        Return internal state; can be passed to setstate() later.
        """
    def setstate(self, state):
        """
        Restore internal state from object returned by getstate().
        """
    def __getstate__(self): # for pickle
        """
         for pickle
        """
    def __setstate__(self, state):  # for pickle
        """
         for pickle
        """
    def __reduce__(self):
        """
         -------------------- integer methods  -------------------


        """
    def randrange(self, start, stop=None, step=1, _int=int):
        """
        Choose a random item from range(start, stop[, step]).

                This fixes the problem with randint() which includes the
                endpoint; in Python this is usually not what you want.

        
        """
    def randint(self, a, b):
        """
        Return random integer in range [a, b], including both end points.
        
        """
    def _randbelow_with_getrandbits(self, n):
        """
        Return a random int in the range [0,n).  Raises ValueError if n==0.
        """
    def _randbelow_without_getrandbits(self, n, int=int, maxsize=1<<BPF):
        """
        Return a random int in the range [0,n).  Raises ValueError if n==0.

                The implementation does not use getrandbits, but only random.
        
        """
    def choice(self, seq):
        """
        Choose a random element from a non-empty sequence.
        """
    def shuffle(self, x, random=None):
        """
        Shuffle list x in place, and return None.

                Optional argument random is a 0-argument function returning a
                random float in [0.0, 1.0); if it is the default None, the
                standard random.random will be used.

        
        """
    def sample(self, population, k):
        """
        Chooses k unique random elements from a population sequence or set.

                Returns a new list containing elements from the population while
                leaving the original population unchanged.  The resulting list is
                in selection order so that all sub-slices will also be valid random
                samples.  This allows raffle winners (the sample) to be partitioned
                into grand prize and second place winners (the subslices).

                Members of the population need not be hashable or unique.  If the
                population contains repeats, then each occurrence is a possible
                selection in the sample.

                To choose a sample in a range of integers, use range as an argument.
                This is especially fast and space efficient for sampling from a
                large population:   sample(range(10000000), 60)
        
        """
    def choices(self, population, weights=None, *, cum_weights=None, k=1):
        """
        Return a k sized list of population elements chosen with replacement.

                If the relative weights or cumulative weights are not specified,
                the selections are made with equal probability.

        
        """
    def uniform(self, a, b):
        """
        Get a random number in the range [a, b) or [a, b] depending on rounding.
        """
    def triangular(self, low=0.0, high=1.0, mode=None):
        """
        Triangular distribution.

                Continuous distribution bounded by given lower and upper limits,
                and having a given mode value in-between.

                http://en.wikipedia.org/wiki/Triangular_distribution

        
        """
    def normalvariate(self, mu, sigma):
        """
        Normal distribution.

                mu is the mean, and sigma is the standard deviation.

        
        """
    def lognormvariate(self, mu, sigma):
        """
        Log normal distribution.

                If you take the natural logarithm of this distribution, you'll get a
                normal distribution with mean mu and standard deviation sigma.
                mu can have any value, and sigma must be greater than zero.

        
        """
    def expovariate(self, lambd):
        """
        Exponential distribution.

                lambd is 1.0 divided by the desired mean.  It should be
                nonzero.  (The parameter would be called "lambda", but that is
                a reserved word in Python.)  Returned values range from 0 to
                positive infinity if lambd is positive, and from negative
                infinity to 0 if lambd is negative.

        
        """
    def vonmisesvariate(self, mu, kappa):
        """
        Circular data distribution.

                mu is the mean angle, expressed in radians between 0 and 2*pi, and
                kappa is the concentration parameter, which must be greater than or
                equal to zero.  If kappa is equal to zero, this distribution reduces
                to a uniform random angle over the range 0 to 2*pi.

        
        """
    def gammavariate(self, alpha, beta):
        """
        Gamma distribution.  Not the gamma function!

                Conditions on the parameters are alpha > 0 and beta > 0.

                The probability distribution function is:

                            x ** (alpha - 1) * math.exp(-x / beta)
                  pdf(x) =  --------------------------------------
                              math.gamma(alpha) * beta ** alpha

        
        """
    def gauss(self, mu, sigma):
        """
        Gaussian distribution.

                mu is the mean, and sigma is the standard deviation.  This is
                slightly faster than the normalvariate() function.

                Not thread-safe without a lock around calls.

        
        """
    def betavariate(self, alpha, beta):
        """
        Beta distribution.

                Conditions on the parameters are alpha > 0 and beta > 0.
                Returned values range between 0 and 1.

        
        """
    def paretovariate(self, alpha):
        """
        Pareto distribution.  alpha is the shape parameter.
        """
    def weibullvariate(self, alpha, beta):
        """
        Weibull distribution.

                alpha is the scale parameter and beta is the shape parameter.

        
        """
def SystemRandom(Random):
    """
    Alternate random number generator using sources provided
        by the operating system (such as /dev/urandom on Unix or
        CryptGenRandom on Windows).

         Not available on all systems (see os.urandom() for details).
    
    """
    def random(self):
        """
        Get the next random number in the range [0.0, 1.0).
        """
    def getrandbits(self, k):
        """
        getrandbits(k) -> x.  Generates an int with k random bits.
        """
    def seed(self, *args, **kwds):
        """
        Stub method.  Not used for a system random number generator.
        """
    def _notimplemented(self, *args, **kwds):
        """
        Method should not be called for a system random number generator.
        """
def _test_generator(n, func, args):
    """
    'times'
    """
def _test(N=2000):
    """
     Create one instance, seeded from current time, and export its methods
     as module-level functions.  The functions share state across all uses
    (both in the user's code and in the Python libraries), but that's fine
     for most programs and is easier for the casual user than making them
     instantiate their own Random() instance.


    """
