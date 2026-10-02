from setuptools import setup
from setuptools.dist import Distribution


class BinaryDistribution(Distribution):
    """The wheel bundles the native larky-runner, so it is platform-specific
    (e.g. pylarky-<version>-cp310-cp310-linux_x86_64.whl), as with Poetry."""

    def has_ext_modules(self):
        return True


setup(distclass=BinaryDistribution)
