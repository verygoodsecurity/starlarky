"""
Bazel domain implementation for Sphinx.
Domain overview
===============
* **workspace**: contains the ``WORKSPACE`` file. Container for multiple packages
 * **package**: contains a ``BUILD`` file. Container for multiple targets
  * **target**: any file or .bzl-file. If last, following is available:
   * **rule**:  a defined rule inside a .bzl-file
   * **macro**: a defined macro-function inside a .bzl file
Domain directives
=================
* bazel-workspace
* bazel-package
* bazel-target
* bazel-rule
* bazel-macro
"""

# Implementation details/help taken from:
# Domains user manual : http://www.sphinx-doc.org/en/master/usage/restructuredtext/domains.html
# Domains API: http://www.sphinx-doc.org/en/master/extdev/domainapi.html#domain-api
# Python Domain impl: https://github.com/sphinx-doc/sphinx/blob/master/sphinx/domains/python.py
# Java Domain impl: https://github.com/bronto/javasphinx/tree/master/javasphinx
import re

import docutils.utils
from docutils import nodes
from docutils.parsers.rst import Directive, directives
from pkg_resources import parse_version

import sphinx
from sphinx import addnodes
from sphinx import version_info
from sphinx.directives import ObjectDescription
from sphinx.domains import Domain, ObjType
from sphinx.locale import _

sphinx_version = sphinx.__version__
if parse_version(sphinx_version) >= parse_version("1.6"):
    from sphinx.util import logging, split_explicit_title
else:
    import logging
logger = logging.getLogger(__name__)

# REs for Bazel signatures
bzl_sig_re = re.compile(
    r'''^  //([\w/.-]*)    # package name
(:([\w/.-]*))?    # target name
(:([\w/.-]*))?    # rule, macro or impl name (named later internally as internal)
(:([\w/.-]*))?    # attribute name 
$                  # and nothing more
 ''', re.VERBOSE)  # noqa


def create_indexnode(indextext, fullname):
    # See https://github.com/sphinx-doc/sphinx/issues/2673
    if version_info < (1, 4):
        return 'single', indextext, fullname, ''
    else:
        return 'single', indextext, fullname, '', None


class LarkyWorkspace(Directive):
    """
    Directive to mark description of a new workspace.
    """

    has_content = True
    required_arguments = 1
    optional_arguments = 0
    final_argument_whitespace = True
    option_spec = {
        'path': directives.unchanged,
        'hide': directives.flag,
        'show_type': directives.flag
    }

    def run(self):
        env = self.state.document.settings.env
        workspace_name = self.arguments[0].strip()
        workspace_path = self.options.get('path', '')
        env.ref_context['bazel:workspace'] = workspace_name
        ret = []

        env.domaindata['bazel']['workspaces'][workspace_name] = (env.docname,
                                                                 workspace_path)
        # make a duplicate entry in 'objects' to facilitate searching for
        # the module in PythonDomain.find_obj()
        env.domaindata['bazel']['objects'][workspace_name] = (env.docname, 'workspace')
        targetnode = nodes.target('', '', ids=['workspace-' + workspace_name],
                                  ismod=True)
        self.state.document.note_explicit_target(targetnode)
        # the platform and synopsis aren't printed; in fact, they are only
        # used in the modindex currently
        ret.append(targetnode)
        indextext = _('%s (workspace)') % workspace_name
        inode = addnodes.index(entries=[('single', indextext,
                                         'module-' + workspace_name, '', None)])
        ret.append(inode)

        if self.options.get('hide', False) is None:
            # No output is wanted
            return ret

        workspace_string = workspace_name
        if self.options.get('show_type', False) is None:
            sig_type_string = 'workspace: '
            ret.append(addnodes.desc_name(sig_type_string, sig_type_string,
                                          classes=['bazel', 'type', 'workspace']))

        if workspace_path:
            workspace_string += ' ({})'.format(workspace_path)

        workspace_name_node = addnodes.desc_name(workspace_string, workspace_string)

        ret.append(workspace_name_node)

        contentnode = addnodes.desc_content()
        ret.append(contentnode)
        self.state.nested_parse(self.content, self.content_offset, contentnode)
        # DocFieldTransformer(self).transform_all(contentnode)
        return ret


class LarkyObject(ObjectDescription):
    """
    Description of a general larky object
    """
    option_spec = {
        'path': directives.unchanged,  # Can be used to specify local, not-valid workspace
        'implementation': directives.unchanged,  # Used by bazel:rule to define implementation function
        'invocation': directives.unchanged,  # Used to define a string which represents a complete call
        'show_workspace': directives.flag,
        'show_workspace_path': directives.flag,
        'show_implementation': directives.flag,
        'show_invocation': directives.flag,
        'show_type': directives.flag,
    }

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self.implementation = None
        self.invocation = None
        self.specific_workspace_path = None

    def get_signature_prefix(self, sig):
        # type: (str) -> str
        """May return a prefix to put before the object name in the
        signature.
        """
        return ''

    def needs_arglist(self):
        # type: () -> bool
        """May return true if an empty argument list is to be generated even if
        the document contains none.
        """
        return False

    def handle_signature(self, sig, signode):
        """
        Transform a Bazel signature into RST nodes.
        :param sig:
                :param signode:
        :return:
        """
        m = bzl_sig_re.match(sig)
        if m is None:
            logger.error("Sphinx-Bazel: Parse problems with signature: {}".format(sig))
            raise ValueError
        package, after_package, target, after_target, internal, after_internal, attribute = m.groups()

        # Let's see if we have to use a specific workspace path or if we have to use the latest defined workspace
        self.specific_workspace_path = self.options.get('path', None)
        if self.specific_workspace_path:
            signode['workspace'] = self.specific_workspace_path
        else:
            try:
                signode['workspace'] = self.env.ref_context['bazel:workspace']
            except KeyError:
                logger.error("No workspace defined before given {} on page {} line {}".format(
                    self.name, self.state.document.current_source, self.state.document.current_line))

        signode['package'] = package
        signode['target'] = target

        sig_type = 'package'
        sig_text = '//{}'.format(package)
        if target:
            sig_text += ':{}'.format(target)
            sig_type = 'target'
        if internal:
            sig_text += ':{}'.format(internal)
            sig_type = self.objtype
        if attribute:
            sig_text += ':{}'.format(attribute)
            sig_type = 'attribute'

        if self.options.get('show_type', False) is None:
            sig_type_string = sig_type + ': '
            signode += addnodes.desc_name(sig_type_string, sig_type_string, classes=['bazel', 'type', sig_type])

        signode += addnodes.desc_name(sig_text, sig_text)

        if self.options.get('show_workspace', False) is None:  # if flag is set, value is None
            ws_string = 'workspace: {}'.format(signode['workspace'])
            self._add_signature_detail(signode, ws_string)

        if self.options.get('show_workspace_path', False) is None:  # if flag is set, value is None
            # If no extra workspace path was defined via :path:
            if self.specific_workspace_path is None:
                # Get the path of the current/latest workspace
                current_ws = self.env.ref_context['bazel:workspace']
                ws_obj = self.env.domaindata['bazel']['workspaces'][current_ws]
                ws_path = ws_obj[1]  # See workspace.py for details about stored data
            else:
                ws_path = self.specific_workspace_path
            ws_path_string = 'workspace path: {}'.format(ws_path)
            self._add_signature_detail(signode, ws_path_string)

        rule_impl = self.options.get('implementation', "")
        if rule_impl:
            self.implementation = rule_impl

        rule_invocation = self.options.get('invocation', "")
        if rule_invocation:
            self.invocation = rule_invocation

        if self.options.get('show_implementation', False) is None:
            impl_string = 'implementation: {}'.format(self.implementation)
            self._add_signature_detail(signode, impl_string)

        if self.options.get('show_invocation', False) is None:
            invocation_string = 'invocation: {}'.format(self.invocation)
            self._add_signature_detail(signode, invocation_string)

        return sig, sig

    def _add_signature_detail(self, signode, text):
        """
        Create a additional line under signature.
        Used to show additional details of an object like workspace name or path
        :param signode: node to add the line add the ending
        :param text: Text inside the new line
        :return: None
        """
        ws_line = nodes.line()
        ws_line += addnodes.desc_addname(text, text)
        signode += ws_line


class LarkyDomain(Domain):
    """Larky language"""

    name = 'larky'
    label = 'Larky'
    object_types = {
        'workspace': ObjType(_('workspace'), 'workspace', 'ref'),
        'package': ObjType(_('package'), 'package', 'ref'),
        'target': ObjType(_('target'), 'target', 'ref'),
        'rule': ObjType(_('rule'), 'rule', 'ref'),
        'macro': ObjType(_('macro'), 'macro', 'ref'),
        'impl': ObjType(_('impl'), 'impl', 'ref'),
        'attribute': ObjType(_('attribute'), 'attribute', 'ref')
    }
    directives = {
        'workspace': LarkyWorkspace,
        'package': LarkyObject,
        'target': LarkyObject,
        'rule': LarkyObject,
        'macro': LarkyObject,
        'implementation': LarkyObject,
        'impl': LarkyObject,
        'attribute': LarkyObject,
    }
    roles = {}
    initial_data = {
        'objects': {},
        'workspaces': {}
    }
    indices = []

    def clear_doc(self, docname):
        pass


SOURCE_URI = ('https://github.com/verygoodsecurity/starlarky'
              '/tree/master'
              '/larky/src/main/resources/stdlib/%s')

# Support for linking to Python source files easily

def source_role(typ, rawtext, text, lineno, inliner, options={}, content=[]):
    has_t, title, target = split_explicit_title(text)
    title = docutils.utils.unescape(title)
    target = docutils.utils.unescape(target)
    refnode = nodes.reference(title, title, refuri=SOURCE_URI % target)
    return [refnode], []