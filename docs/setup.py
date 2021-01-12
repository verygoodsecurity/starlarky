#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os
from setuptools import setup, find_packages

requires = ['Sphinx<2;python_version<"3.5"',
            'Sphinx<3;python_version<"3.6"',
            'Sphinx;python_version>="3.6"',
            ]

setup(
    name='sphinx-larky',
    version='0.1.0',
    url='http://github.com/verygoodsecurity/starlarky',
    license='Apache 2.0',
    author_email='G_G',
    description='Larky Documentation',
    zip_safe=False,
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Intended Audience :: Developers',
        'License :: OSI Approved :: Apache Software License',
        'Operating System :: OS Independent',
        'Programming Language :: Python',
        'Programming Language :: Python :: 2',
        'Programming Language :: Python :: 2.7',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.5',
        'Programming Language :: Python :: 3.6',
        'Programming Language :: Python :: 3.7',
        'Topic :: Documentation',
        'Topic :: Utilities',
    ],
    platforms='any',
    packages=find_packages(),
    include_package_data=True,
    install_requires=requires,
    namespace_packages=['sphinxcontrib'],
)