"""
autolarky-directive
===================
"""
import ast
import re

from docutils.parsers.rst import Directive, directives

import os
from pkg_resources import parse_version
import sphinx

sphinx_version = sphinx.__version__
if parse_version(sphinx_version) >= parse_version("1.6"):
    from sphinx.util import logging
else:
    import logging
logger = logging.getLogger(__name__)


class AutoLarkyCommonDirective(Directive):
    has_content = False
    required_arguments = 1
    optional_arguments = 0
    option_spec = {
        'packages': directives.unchanged,  # Shall packages inside a workspace be printed?
        'targets': directives.unchanged,  # Shall targets inside packages be printed?
        'rules': directives.flag,  # Shall rules inside bzl-files be printed?
        'macros': directives.flag,  # Shall macros inside bzl-files be printed?
        'implementations': directives.flag,  # Shall implementations inside bzl-files be printed?
        'attributes': directives.flag,  # Shall attributes of rule be printed?
        'path': directives.unchanged,  # Used as temporary workspace path, if given.
        'name': directives.unchanged,  # Used to manually set a name of a workspace

        'hide': directives.flag,  # Shall the root-object be printed (e.g. no workspace)
        'show_workspace': directives.flag,  # Prints workspace name to all documented elements
        'show_workspace_path': directives.flag,  # Prints workspace_path name to all documented elements
        'show_implementation': directives.flag,  # Prints the used function name for implementation of a rule
        'show_invocation': directives.flag,  # Prints the invocation string
        'show_type': directives.flag,  # Prints the type as prefix before the name/path
        'raw': directives.flag,  # Integrates documentation as source code, so no rendering happens
    }
    final_argument_whitespace = True

    def __init__(self, *args, **kw):
        super(AutoLarkyCommonDirective, self).__init__(*args, **kw)
        self.log = logging.getLogger(__name__)

        self.root_path = None
        self.workspace_name = None
        self.workspace_path = None
        self.workspace_path_abs = None

        self.bzl_content = {}

    @property
    def env(self):
        return self.state.document.settings.env

    @property
    def docname(self):
        return self.state.document.settings.env.docname

    def run(self):
        logger.info("name: %s", self.name)
        if 'workspace' not in self.name:
            self.root_path = self.options.get('path', False)
            logger.info("path: %s", self.root_path)
            if self.root_path:
                self.workspace_path = self.root_path
                self.workspace_name = os.path.basename(self.root_path)
            else:
                try:
                    self.workspace_name = self.env.ref_context['bazel:workspace']
                except KeyError:
                    self.log.error("No workspace was defined before the {} definition of {}.\n "
                                   "Please define one, which can than be used as reference for "
                                   "calculating file paths.\n"
                                   "Or use option :root-path: to document something without"
                                   "a bazel workspace.".format(self.name, self.arguments[0]))
                    return []

                try:
                    self.workspace_path = self.env.domaindata['bazel']['workspaces'][self.workspace_name][1]
                except KeyError:
                    self.log.error("Could not find workspace_name in defined workspace. That's strange!")
                    return []
        else:
            if self.options.get('path', False):
                self.log.warning(':path: not supported by autobazel-workspace. Argument is used instead.')
            self.workspace_path = self.arguments[0]

        if not os.path.isabs(self.workspace_path):
            self.workspace_path_abs = os.path.join(self.env.app.confdir, self.workspace_path)
        else:
            self.workspace_path_abs = self.workspace_path

        if not os.path.exists(self.workspace_path_abs):
            self.log.error("Given workspace does not exist: {}".format(self.arguments[0]))
            return []

        if 'workspace' in self.name:
            return self._handle_workspace()
        elif 'package' in self.name:
            return self._handle_package()
        elif 'target' in self.name:
            return self._handle_target()
        elif 'rule' in self.name:
            return self._handle_rule()
        elif 'macro' in self.name:
            return self._handle_macro()
        elif 'implementation' in self.name:
            return self._handle_implementation()
        elif 'attribute' in self.name:
            return self._handle_attribute()

        return []

    def _handle_workspace(self):
        workspace_path = self.arguments[0]

        workspace_file_path = os.path.join(self.workspace_path_abs, 'WORKSPACE')
        if not os.path.exists(workspace_file_path):
            self.log.error("Given workspace path contains no WORKSPACE file.")
            return []

        workspace_name = ""
        with open(workspace_file_path) as f:
            tree = ast.parse(f.read(), workspace_file_path)
            workfile_docstring = ast.get_docstring(tree)
            try:
                for element in tree.body:
                    if isinstance(element.value, ast.Call) and element.value.func.id == 'workspace':
                        for keyword in element.value.keywords:
                            if keyword.arg == 'name':
                                workspace_name = keyword.value.s
                                break
            except (KeyError, AttributeError):
                pass

            if not workspace_name:
                workspace_name = os.path.basename(os.path.normpath(workspace_path))

        #  Set a specific workspace name, if given
        if self.options.get('name', False):
            workspace_name = self.options.get('name', '')

        workfile_docstring = self._check_docstring(workfile_docstring)

        options_rst = ''
        if self.options.get('hide', False) is None:  # If hide is set, no workpackage output
            options_rst += '   :hide:\n'
        elif self.options.get('show_type', False) is None:
            options_rst += "   :show_type:\n"
        workspace_rst = """
.. bazel:workspace:: {workspace_name}
   :path: {path}
{options}
   {docstring}
            """.format(workspace_name=workspace_name,
                       path=workspace_path,
                       options=options_rst,
                       docstring="\n   ".join(workfile_docstring.split('\n')))

        if 'packages' in self.options:
            # Check if package contains regex definition
            pattern = False
            package_regex = self.options.get('packages')
            if isinstance(package_regex, str) and len(package_regex) > 0:
                try:
                    pattern = re.compile(package_regex)
                except Exception as e:
                    raise SyntaxError('Given regex for packages is invalid. Error: {}'.format(e))

            # Find packages inside workspace
            for root, dirs, files in os.walk(self.workspace_path_abs):
                if "BUILD" in files:
                    package = root.replace(self.workspace_path_abs, "")
                    if not package.startswith('/'):
                        package = "/" + package
                    if not package.startswith('//'):
                        package = "/" + package

                    package = package.replace("\\", "/")

                    # If pattern is defined but does not match the package name, we go on
                    if pattern and not pattern.match(package):
                        continue

                    workspace_rst += """
.. autobazel-package:: {package}""".format(package=package)
                    workspace_rst = self._prepare_options(workspace_rst,
                                                          ['show_workspace', 'show_workspace_path', 'targets', 'rules',
                                                           'implementations', 'macros', 'attributes',
                                                           'show_implementation',
                                                           'show_invocation', 'show_type', 'raw'])

                    if 'path' in self.options:
                        workspace_rst += "\n   :path: {}".format(self.root_path)

        self.state_machine.insert_input(workspace_rst.split('\n'),
                                        self.state_machine.document.attributes['source'])
        return []

    def _handle_package(self):
        package = self.arguments[0]

        package_path = os.path.join(self.workspace_path_abs, package.replace('//', ''))
        package_build_file = os.path.join(self.workspace_path_abs, package_path, 'BUILD')
        if not os.path.exists(package_build_file):
            self.log.error("No BUILD file detected for calculated package path: {} in file {}."
                           "Used workspace {} in {}".format(package, self.state.document.current_source,
                                                            self.workspace_name, self.workspace_path_abs))
            return []

        with open(package_build_file) as f:
            tree = ast.parse(f.read(), package_build_file)
            package_docstring = ast.get_docstring(tree)

        package_docstring = self._check_docstring(package_docstring)

        package_name_string = package
        if 'hide' in self.options:  # If hide is set, no package output
            package_rst = ""
        else:
            options_rst = ""
            options_rst = self._prepare_options(options_rst, ['show_workspace', 'show_workspace_path', 'show_type'])
            if 'path' in self.options:
                options_rst += "\n   :path: {}".format(self.workspace_path_abs)

            package_rst = """
.. bazel:package:: {package}{options}
   {docstring}
            """.format(package=package_name_string,
                       options=options_rst,
                       docstring="\n   ".join(package_docstring.split('\n')))

        # Add target information
        if 'targets' in self.options:
            # Check if package contains regex definition
            pattern = False
            target_regex = self.options.get('targets')
            if isinstance(target_regex, str) and len(target_regex) > 0:
                try:
                    pattern = re.compile(target_regex)
                except Exception as e:
                    raise SyntaxError('Given regex for targets is invalid. Error: {}'.format(e))

            target_files = [f for f in os.listdir(package_path) if os.path.isfile(os.path.join(package_path, f))]
            for target_file in target_files:
                if target_file not in ['BUILD']:
                    target_signature = "{package}:{target_path}".format(
                        package=package,
                        target_path=target_file
                    )
                    # If pattern is defined but does not match the package name, we go on
                    if pattern and not pattern.match(target_signature):
                        continue

                    package_rst += "\n.. autobazel-target:: {target}".format(target=target_signature)
                    package_rst = self._prepare_options(package_rst,
                                                        ['show_workspace', 'show_workspace_path', 'rules',
                                                         'implementations', 'macros', 'attributes',
                                                         'show_implementation', 'show_invocation', 'show_type',
                                                         'raw'])

                    if self.options.get("path", False):
                        package_rst += "\n   :path: {}".format(self.root_path)
                    package_rst += "\n"

        if 'packages' in self.options:
            sub_package_dirs = [d for d in os.listdir(package_path) if os.path.isdir(os.path.join(package_path, d))]

            package_regex = self.options.get('packages')
            pattern = None
            if isinstance(package_regex, str) and len(package_regex) > 0:
                try:
                    pattern = re.compile(package_regex)
                except Exception as e:
                    raise SyntaxError('Given regex for packages is invalid. Error: {}'.format(e))

            for sub_package_dir in sub_package_dirs:
                subpackge_path = os.path.join(package_path, sub_package_dir)
                sub_package_files = [f for f in os.listdir(subpackge_path)
                                     if os.path.isfile(os.path.join(subpackge_path, f))]

                if 'BUILD' not in sub_package_files:
                    continue

                subpackage_label = package_name_string + '/' + sub_package_dir
                # If pattern is defined but does not match the package name, we go on
                if pattern and not pattern.match(subpackage_label):
                    continue
                package_rst += "\n.. autobazel-package:: {subpackage}".format(subpackage=subpackage_label)
                package_rst = self._prepare_options(package_rst,
                                                    ['show_workspace', 'show_workspace_path', 'targets', 'rules',
                                                     'implementations', 'macros', 'attributes',
                                                     'show_implementation', 'show_invocation', 'show_type',
                                                     'raw'])

                if self.options.get("path", False):
                    package_rst += "\n   :path: {}".format(self.root_path)
                package_rst += "\n"

        self.state_machine.insert_input(package_rst.split('\n'),
                                        self.state_machine.document.attributes['source'])
        return []

    def _handle_target(self):
        target = self.arguments[0]

        target_path = os.path.join(self.workspace_path_abs, target.replace('//', '').replace(':', '/'))

        if not os.path.exists(target_path):
            self.log.error("Target does not exist: {target_path}".format(target_path=target_path))
            return []

        file_path, file_extension = os.path.splitext(target_path)
        if file_extension in ['.bzl']:  # Only check for docstring, if we are sure AST can handle it.
            try:
                content = self._parse_bzl(target_path)
                target_docstring = content['doc']
            except LarkyParseException as e:
                self.log.warning(e)
                target_docstring = ''
        else:
            target_docstring = ''

        target_docstring = self._check_docstring(target_docstring)

        options_rst = ""
        target_name_string = target
        if self.options.get('hide', False) is None:  # If hide is set, no target output
            target_rst = ""
        else:
            options_rst += self._prepare_options(options_rst,
                                                 ['show_workspace', 'show_workspace_path', 'show_type'])
            if self.options.get('path', False):
                options_rst += "\n   :path: {}\n".format(self.workspace_path_abs)

            target_rst = """
.. bazel:target:: {target}{options}
   {docstring}
            """.format(target=target_name_string,
                       options=options_rst,
                       docstring="\n   ".join(target_docstring.split('\n')))

        # Add rule, macro, implementation information
        if (self.options.get('rules', False) is None
            or self.options.get('macros', False) is None
            or self.options.get('implementations', False) is None) \
                and file_extension in ['.bzl']:

            if self.options.get('rules', False) is None:
                for rule_name in content['rules']:
                    rule_signature = "{target}:{rule}".format(
                        target=target,
                        rule=rule_name)
                    target_rst += "\n.. autobazel-rule:: {rule}".format(rule=rule_signature)
                    target_rst += self._add_common_options()

            if self.options.get('macros', False) is None:
                for macro_name in content['macros']:
                    macro_signature = "{target}:{macro}".format(
                        target=target,
                        macro=macro_name)
                    target_rst += "\n.. autobazel-macro:: {macro}".format(macro=macro_signature)
                    target_rst += self._add_common_options()

            if self.options.get('implementations', False) is None:
                for implementation_name in content['implementations']:
                    implementation_signature = "{target}:{implementation}".format(
                        target=target,
                        implementation=implementation_name)
                    target_rst += "\n.. autobazel-implementation:: {implementation}".format(
                        implementation=implementation_signature)
                    target_rst += self._add_common_options()

        self.state_machine.insert_input(target_rst.split('\n'),
                                        self.state_machine.document.attributes['source'])
        return []

    def _handle_rule(self):
        rule = self.arguments[0]
        rule_name = rule.rsplit(':', 1)[1]

        rule_file_path = os.path.join(self.workspace_path_abs,
                                      rule.replace('//', '').rsplit(":", 1)[0].replace(':', '/'))

        if not os.path.exists(rule_file_path):
            self.log.error("Target for rule does not exist: {rule_file_path}".format(target_path=rule_file_path))
            return []

        content = self._parse_bzl(rule_file_path)

        if rule_name not in content['rules'].keys():
            self.log.warning('Unknown rule {}'.format(rule))
        try:
            rule_doc = content['rules'][rule_name]['doc']
        except KeyError:
            rule_doc = ''

        rule_impl = content['rules'][rule_name]['implementation']
        rule_attrs = content['rules'][rule_name]['attributes']
        rule_invocation = '{}({})'.format(rule_name, ', '.join(rule_attrs.keys()))

        rule_doc = self._check_docstring(rule_doc)

        options_rst = ""
        options_rst += "   :implementation: {impl}\n".format(impl=rule_impl)
        options_rst += "   :invocation: {impl}\n".format(impl=rule_invocation)
        if self.options.get('show_workspace', False) is None:
            options_rst += "   :show_workspace:\n"
        if self.options.get('show_workspace_path', False) is None:
            options_rst += "   :show_workspace_path:\n"
        if self.options.get("show_implementation", False) is None:
            options_rst += "   :show_implementation: \n"
        if self.options.get("show_invocation", False) is None:
            options_rst += "   :show_invocation: \n"
        if self.options.get("show_type", False) is None:
            options_rst += "   :show_type: \n"
        if self.options.get('path', False):
            options_rst += "   :path: {}\n".format(self.workspace_path_abs)

        rule_rst = """
.. bazel:rule:: {rule}
{options}
   {docstring}
        """.format(rule=rule,
                   options=options_rst,
                   docstring="\n   ".join(rule_doc.split('\n')))

        if self.options.get('attributes', False) is None:
            for attr_name in rule_attrs:
                attribute_signature = "{rule}:{attribute}".format(rule=rule, attribute=attr_name)
                rule_rst += "\n.. autobazel-attribute:: {attribute}".format(attribute=attribute_signature)
                rule_rst += self._add_common_options()

        self.state_machine.insert_input(rule_rst.split('\n'),
                                        self.state_machine.document.attributes['source'])

        return []

    def _handle_macro(self):
        macro = self.arguments[0]
        macro_name = macro.rsplit(':', 1)[1]

        macro_file_path = os.path.join(self.workspace_path_abs,
                                       macro.replace('//', '').rsplit(":", 1)[0].replace(':', '/'))

        if not os.path.exists(macro_file_path):
            self.log.error("Target for macro does not exist: {target_path}".format(target_path=macro_file_path))
            return []

        content = self._parse_bzl(macro_file_path)

        if macro_name not in content['macros'].keys():
            self.log.warning('Macro {} not found in file {}'.format(macro_name, macro_file_path))
            return []

        macro_doc = content['macros'][macro_name]['doc']

        macro_doc = self._check_docstring(macro_doc)

        options_rst = ""
        if self.options.get('show_workspace', False) is None:
            options_rst += "   :show_workspace:\n"
        if self.options.get('show_workspace_path', False) is None:
            options_rst += "   :show_workspace_path:\n"
        if self.options.get("show_type", False) is None:
            options_rst += "   :show_type: \n"
        if self.options.get('path', False):
            options_rst += "   :path: {}\n".format(self.workspace_path_abs)

        macro_rst = """
.. bazel:macro:: {macro}
{options}
   {docstring}
        """.format(macro=macro,
                   options=options_rst,
                   docstring="\n   ".join(macro_doc.split('\n')))

        self.state_machine.insert_input(macro_rst.split('\n'),
                                        self.state_machine.document.attributes['source'])

        return []

    def _handle_implementation(self):
        impl = self.arguments[0]
        impl_name = impl.rsplit(':', 1)[1]

        impl_file_path = os.path.join(self.workspace_path_abs,
                                      impl.replace('//', '').rsplit(":", 1)[0].replace(':', '/'))

        if not os.path.exists(impl_file_path):
            self.log.error("Target for implementation does not exist: {target_path}".format(target_path=impl_file_path))
            return []

        content = self._parse_bzl(impl_file_path)

        if impl_name not in content['implementations'].keys():
            self.log.warning('Implementation {} not found in file {}'.format(impl_name, impl_file_path))
            return []

        impl_doc = content['implementations'][impl_name]['doc']

        impl_doc = self._check_docstring(impl_doc)

        options_rst = ""
        if self.options.get('show_workspace', False) is None:
            options_rst += "   :show_workspace:\n"
        if self.options.get('show_workspace_path', False) is None:
            options_rst += "   :show_workspace_path:\n"
        if self.options.get("show_type", False) is None:
            options_rst += "   :show_type: \n"
        if self.options.get('path', False):
            options_rst += "   :path: {}\n".format(self.workspace_path_abs)

        implementation_rst = """
.. bazel:implementation:: {macro}
{options}
   {docstring}
        """.format(macro=impl,
                   options=options_rst,
                   docstring="\n   ".join(impl_doc.split('\n')))

        self.state_machine.insert_input(implementation_rst.split('\n'),
                                        self.state_machine.document.attributes['source'])

        return []

    def _handle_attribute(self):
        """Cares about the correct handling of autobazel-attributes"""
        attribute_path = self.arguments[0]

        try:
            package_name, target_name, rule_name, attribute_name = attribute_path.rsplit(':', 3)
        except (IndexError, ValueError):
            self.log.warning("bazel-path for autobazel-attribute looks strange: {}".format(attribute_path))

        if target_name.startswith('/'):
            target_path_name = target_name[1:]
        else:
            target_path_name = target_name
        target_file_path = os.path.join(self.workspace_path_abs, package_name.replace('//', ''),
                                        target_path_name.replace(':', '/'))

        if not os.path.exists(target_file_path):
            self.log.error("Target for rule does not exist: {target_path}".format(target_path=target_file_path))
            return []

        content = self._parse_bzl(target_file_path)

        if rule_name not in content['rules'].keys():
            self.log.warning('Rule {} not found for autobazel-attribute'.format(rule_name))
            return []

        attributes = content['rules'][rule_name]['attributes']
        if attribute_name not in attributes.keys():
            self.log.warning('Attribute {} not found in rule {}. Available attributes: {}'.format(
                attribute_name, rule_name, ', '.join(attributes.keys())
            ))
            return []

        attribute = attributes[attribute_name]

        try:
            doc_string = attribute['parameters']['doc']
        except KeyError:
            doc_string = ''

        doc_string = self._check_docstring(doc_string)

        options_rst = ""
        if self.options.get('show_workspace', False) is None:
            options_rst += "   :show_workspace:\n"
        if self.options.get('show_workspace_path', False) is None:
            options_rst += "   :show_workspace_path:\n"
        if self.options.get("show_type", False) is None:
            options_rst += "   :show_type: \n"
        if self.options.get('path', False):
            options_rst += "   :path: {}\n".format(self.workspace_path_abs)

        rule_rst = """
.. bazel:rule:: {path}
{options}
   {docstring}
        """.format(path=attribute_path,
                   options=options_rst,
                   docstring="\n   ".join(doc_string.split('\n')))

        self.state_machine.insert_input(rule_rst.split('\n'),
                                        self.state_machine.document.attributes['source'])

        return []

    def _prepare_options(self, rst_string, option_list):
        """
        Adds given option to rst_string, if the option is part of self.options and therefore
        set by user.
        """
        for option in option_list:
            if option in self.options:
                rst_string += "\n   :{}:".format(option)

        return rst_string

    def _add_common_options(self):
        """
        Adds needed options
        """
        directive_rst = ""
        directive_rst += self._prepare_options(directive_rst,
                                               ['show_workspace', 'show_workspace_path', 'attributes',
                                                'show_implementation', 'show_invocation', 'show_type', 'raw'])
        if self.options.get("path", False):
            directive_rst += "\n   :path: {}".format(self.root_path)
        return directive_rst

    def _parse_bzl(self, bzl_file):
        """
        Parse a bzl-file and stores its contents inside a dictionary.
        It stores docstring, rule, macro, implementations and rule_attributes.
        :param bzl_file: path to the bzl_file
        :return: dictionary of parsed results
        """

        # Let's be sure to parse a bzl file only once.
        # If it got already parsed, return the result
        if bzl_file in self.bzl_content.keys():
            return self.bzl_content[bzl_file]

        content = {
            'doc': '',
            'rules': {},
            'macros': {},
            'implementations': {},
            'attributes': {}
        }

        with open(bzl_file) as f:
            try:
                tree = ast.parse(f.read(), bzl_file)
                content['doc'] = ast.get_docstring(tree)
                for element in tree.body:
                    # Check for rule data
                    if isinstance(element, ast.Assign) and isinstance(element.value, ast.Call) \
                            and hasattr(element.value.func, 'id') and element.value.func.id == 'rule':
                        rule = {
                            "name": element.targets[0].id,
                            "implementation": {},
                            "attributes": {}
                        }
                        for keyword in element.value.keywords:
                            if keyword.arg == 'doc':
                                try:
                                    rule['doc'] = keyword.value.s
                                except AttributeError:
                                    # A varaible may be referenced here, which stores the doc content.
                                    if hasattr(keyword.value, 'id'):
                                        rule['doc'] = ''
                                        # rule_id = keyword.value.id  # EG GENPY_DOC
                                        # ToDo: Values from an assignment can be really complex and maybe impossible
                                        # to read without code execution.
                                        # See following example, where a string gets finally a .strip() handling.
                                        # So how to safely read MYPY_DOC??
                                        #
                                        # MYPY_DOC = """
                                        # Generate python classes from messages with mypy
                                        # """.strip()
                                        self.log.debug('DocAccessProblems: doc is defined in an extra variable.\n'
                                                       'Rule name: {}\nFile: {}'.format(rule['name'], bzl_file))
                                    else:
                                        rule['doc'] = ''
                            if keyword.arg == 'implementation':
                                rule['implementation'] = keyword.value.id
                            if keyword.arg == 'attrs':
                                rule['attributes'] = {}
                                if isinstance(keyword.value, ast.Dict):
                                    for index, key in enumerate(keyword.value.keys):
                                        value = keyword.value.values[index]
                                        # Be sure we handle no spooky (e.g. ast.Name) stuff here
                                        if isinstance(value, ast.Call):
                                            rule['attributes'][key.s] = {
                                                'type': value.func.attr,
                                                'parameters': {}
                                            }
                                            # Check for parameters
                                            for param_keyword in value.keywords:
                                                if isinstance(param_keyword.value, ast.NameConstant):
                                                    needed_value = str(param_keyword.value.value)
                                                elif isinstance(param_keyword.value, ast.Str):
                                                    needed_value = param_keyword.value.s
                                                else:
                                                    needed_value = ""
                                                rule['attributes'][key.s]['parameters'][param_keyword.arg] = \
                                                    needed_value
                                else:
                                    needed_value = ""
                        content['rules'][rule['name']] = rule
                    # Check for macro data
                    elif isinstance(element, ast.FunctionDef):
                        if len(element.args.args) == 1 and element.args.args[0].arg == 'ctx':
                            try:
                                impl = {}
                                impl['name'] = element.name
                                # impl['doc'] = element.body[0].value.s
                                impl['doc'] = ast.get_docstring(element)
                                content['implementations'][impl['name']] = impl
                            except AttributeError:
                                pass
                        elif isinstance(element, ast.FunctionDef):
                            macro = {}
                            macro['name'] = element.name
                            macro['doc'] = ast.get_docstring(element)
                            content['macros'][macro['name']] = macro

            except (SyntaxError, KeyError) as e:
                # Looks like file has no Python based syntax. So no documentation to catch
                raise LarkyParseException("Problems during parsing of {} happened: {}".format(bzl_file, e))

        # Store parsed content for further requests
        self.bzl_content[bzl_file] = content

        return content

    def _check_docstring(self, doc_string):
        if doc_string is None:
            return ''

        if self.options.get('raw', False) is None and doc_string != '':
            new_doc_string = '.. code-block:: rst\n\n   {doc_string}'.format(
                doc_string='\n   '.join(doc_string.split('\n')))
            return new_doc_string

        return doc_string


class LarkyParseException(BaseException):
    pass