# -*- coding: utf-8 -*-
#
#  Random/__init__.py : PyCrypto random number generation
#
# ===================================================================
# The contents of this file are dedicated to the public domain.  To
# the extent that dedication to the public domain is not available,
# everyone is granted a worldwide, perpetual, royalty-free,
# non-exclusive license to exercise all rights associated with the
# contents of this file for any purpose whatsoever.
# No rights are reserved.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
# BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
# ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
# ===================================================================
load("@stdlib//larky", "larky")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Random/random", random="random")

#: Function that returns a random byte string of the desired size.
get_random_bytes = _JCrypto.Random.urandom


def _UrandomRNG():
    self = larky.mutablestruct(__name__='_UrandomRNG', __class__=_UrandomRNG)

    def read(n):
        """Return a random byte string of the desired size."""
        return get_random_bytes(n)
    self.read = read

    def flush():
        """Method provided for backward compatibility only."""
        pass
    self.flush = flush

    def reinit():
        """Method provided for backward compatibility only."""
        pass
    self.reinit = reinit

    def close():
        """Method provided for backward compatibility only."""
        pass
    self.close = close
    return self


def new(*args, **kwargs):
    """Return a file-like object that outputs cryptographically random bytes."""
    return _UrandomRNG()


def atfork():
    pass


Random = larky.struct(
    new=new,
    get_random_bytes=get_random_bytes,
    atfork=atfork,
    random=random
)


