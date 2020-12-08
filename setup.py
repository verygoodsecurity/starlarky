import os
from setuptools import find_packages, setup


version = 'nover'
try:
    version = os.environ['CIRCLE_TAG']
except KeyError:
    pass

setup(
    name='pylarky',
    packages=find_packages(),
    include_package_data=True,
    version=version,
    description='Python wrapper for starlarky runner',
    author='Very Good Security',
    license='Apache License v2.0',
    data_files=[('bin', ['larky-runner'])]
)
