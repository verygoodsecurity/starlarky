"""Random variable generators.
    bytes
    -----
           uniform bytes (values between 0 and 255)
    integers
    --------
           uniform within range
    sequences
    ---------
           pick random element
           pick random sample
           pick weighted random sample
           generate random permutation
    distributions on the real line:
    ------------------------------
           uniform
           triangular
           normal (Gaussian)
           lognormal
           negative exponential
           gamma
           beta
           pareto
           Weibull
    distributions on the circle (angles 0 to 2pi)
    ---------------------------------------------
           circular uniform
           von Mises
General notes on the underlying Mersenne Twister core generator:
* The period is 2**19937-1.
* It is one of the most extensively tested generators in existence.
* The random() method is implemented in C, executes in a single Python step,
  and is, therefore, threadsafe.
"""

load("@stdlib//larky", larky="larky")
load("@stdlib//jcrypto", _JCrypto="jcrypto")


random = larky.struct(
    getrandbits=_JCrypto.Random.getrandbits,
    randbytes=_JCrypto.Random.randbytes,
    randrange=_JCrypto.Random.randrange,
    randint=_JCrypto.Random.randint,
    choice=_JCrypto.Random.choice,
    shuffle=_JCrypto.Random.shuffle,
    sample=_JCrypto.Random.sample,
    urandom=_JCrypto.Random.urandom
)


getrandbits = random.getrandbits
randrange = random.randrange
randint = random.randint
choice = random.choice
shuffle = random.shuffle
sample = random.sample
urandom = random.urandom
randbytes = random.randbytes
