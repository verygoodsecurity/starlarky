from setuptools import find_packages, setup

setup(
    name='pylarky',
    packages=find_packages(),
    include_package_data=True,
    version='0.0.2',
    description='Python wrapper for starlarky runner',
    author='Very Good Security',
    license='Apache License v2.0',
    package_data={"pylarky": ["bin/larky-runner"]},
)
