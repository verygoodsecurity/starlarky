# Configuration file for the Sphinx documentation builder.
#
# This file only contains a selection of the most common options. For a full
# list see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Path setup --------------------------------------------------------------

# If extensions (or modules to document with autodoc) are in another directory,
# add these directories to sys.path here. If the directory is relative to the
# documentation root, use os.path.abspath to make it absolute, like shown here.
#
import os
import sys
# from dotenv import load_dotenv
# load_dotenv()

sys.path.insert(0, os.path.abspath('../../../pylarky'))


# -- Project information -----------------------------------------------------

project = 'starlarky'
copyright = '2021, Very Good Security'
author = 'Very Good Security'

# The full version, including alpha/beta/rc tags
release = '0.0.1'


# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = [
            'autoapi.extension',  # https://sphinx-autoapi.readthedocs.io/en/latest/tutorials.html#setting-up-automatic-api-documentation-generation
            'sphinxcontrib.confluencebuilder',
              ]

autoapi_dirs = ['../../../pylarky']

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
# This pattern also affects html_static_path and html_extra_path.
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store', ]


# -- Options for HTML output -------------------------------------------------

# The theme to use for HTML and HTML Help pages.  See the documentation for
# a list of builtin themes.
#
html_theme = 'furo'  # https://pradyunsg.me/furo/customisation/sidebar/
# html_sidebars = [
#     "sidebar/scroll-start.html",
#     "sidebar/brand.html",
#     "sidebar/search.html",
#     "sidebar/navigation.html",
#     "sidebar/ethical-ads.html",
#     "sidebar/scroll-end.html",
# ]
# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
html_static_path = ['_static']

# -- Confluence options
confluence_publish_dryrun = False # see: https://sphinxcontrib-confluencebuilder.readthedocs.io/en/latest/configuration.html#confluence-publish-dryrun
confluence_publish = True
confluence_space_name = 'PRODUCT'
confluence_parent_page = 'Developer Resources'
# (for confluence cloud)
confluence_server_url = 'https://verygoodsecurity.atlassian.net/wiki'
confluence_server_user = 'blaise.pabon@vgs.io'
confluence_server_pass = os.getenv("CONFLUENCE_KEY")

# # -- Redoc settings
# redoc = [
#     {
#         'name': '{} API'.format(project),
#         'page': 'api/index',
#         'spec': 'reference/gateway_api.yaml',
#         'embed': True,
#     }
# ]