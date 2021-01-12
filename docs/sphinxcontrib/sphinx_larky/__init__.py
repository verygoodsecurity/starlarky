# -*- coding: utf-8 -*-
"""
    sphinxcontrib.sphinx_larky
    ~~~~~~~~~~~~~~~~~~~~~~~~
"""
__import__("pkg_resources").declare_namespace(__name__)

from os import path

import sphinx
from pkg_resources import parse_version

from .autolarky_directive import AutoLarkyCommonDirective
from .domain import LarkyDomain, source_role

package_dir = path.abspath(path.dirname(__file__))
VERSION = "0.1.0"

sphinx_version = sphinx.__version__
if parse_version(sphinx_version) >= parse_version("1.6"):
    from sphinx.util import logging
else:
    import logging

    logging.basicConfig()  # Only need to do this once


def setup(app):
    app.add_role('source', source_role)
    app.add_domain(LarkyDomain)
    app.add_directive("autolarky-root", AutoLarkyCommonDirective)
    # app.add_directive('autobazel-package', AutoLarkyCommonDirective)
    # app.add_directive('autobazel-target', AutoLarkyCommonDirective)
    # app.add_directive('autobazel-rule', AutoLarkyCommonDirective)
    # app.add_directive('autobazel-macro', AutoLarkyCommonDirective)
    # app.add_directive('autobazel-implementation', AutoLarkyCommonDirective)
    # app.add_directive('autobazel-attribute', AutoLarkyCommonDirective)
    return {"version": VERSION}  # identifies the version of our extension
