"""
Small set of shims that when imported by any larky file makes it valid python
 that can be imported and unit tested.

Only the necessary functions used by larky have been shimmed.

This technically would allow code that is not valid Larky/Starlark, but that is
covered by the other tests that exercise this within Larky.

"""
import os
import base64
import dataclasses
import hashlib
import json
from importlib.util import spec_from_loader, module_from_spec
from importlib.machinery import SourceFileLoader


# helper to create class with attribute on the fly
def create_cls(cls_name, attr_dict, bases=(ABC,)):              # noqa: D103
    for name, attr in attr_dict.items():
        attr.__set_name__(None, name)
    ns = {'__module__': __name__}
    ns.update(attr_dict)
    return type(str(cls_name), bases, ns)



# Shared Starlark/Python files must have a .bzl suffix for Starlark import, so
# we are forced to do this workaround.
def load_module(name, path):
    """
    # Modules
    envoy_repository_locations = load_module(
        'envoy_repository_locations', 'bazel/repository_locations.bzl')
    repository_locations_utils = load_module(
        'repository_locations_utils', os.path.join(api_path, 'bazel/repository_locations_utils.bzl'))
    api_repository_locations = load_module(
        'api_repository_locations', os.path.join(api_path, 'bazel/repository_locations.bzl'))

    """
    spec = spec_from_loader(name, SourceFileLoader(name, path))
    module = module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def preprocess_input_text(text):
    result = text
    for replacement in replacements:
        result = result.replace(replacement[0], replacement[1])
    return result


bazel_package_dir = "."
bazel_workspace_dir = "."


def load(filename, *args):
    if filename.startswith('@'):
        return
    elif filename.startswith(':'):
        filename = os.path.join(bazel_package_dir, filename[1:])
    elif filename.startswith('//'):
        split = filename[2:].split(':')
        filename = os.path.join(bazel_workspace_dir, split[0], split[1])

    src_file_content = open(filename).read()
    processed_file_content = preprocess_input_text(src_file_content)
    exec(processed_file_content, globals(), globals())


def __dataclass_eq(left, right):
    if not dataclasses.is_dataclass(right):
        return False
    return dataclasses.asdict(left) == dataclasses.asdict(right)


def __struct_to_json(s):
    return json.dumps(dataclasses.asdict(s))


def struct(**kwargs):
    cls = dataclasses.make_dataclass(
        "struct",
        [(k, type(v)) for k, v in kwargs.items()],
        namespace={"__eq__": __dataclass_eq, "to_json": __struct_to_json},
    )
    return cls(**kwargs)


class Fail(Exception):
    pass


def fail(msg, attr=None):
    if attr:  # pragma: no cover
        msg = f"{attr}: {msg}"
    raise Fail(msg)


class target_utils(object):
    @staticmethod
    def parse_target(target):
        if target.count(":") != 1:
            fail(f'rule name must contain exactly one ":" "{target}"')

        repo_base_path, name = target.split(":")
        if not repo_base_path:
            return (None, None, name)

        if repo_base_path.count("//") != 1:
            fail(
                'absolute rule name must contain one "//" '
                f'before ":": "{target}"'
            )

        repo, base_path = repo_base_path.split("//", 1)

        return (repo, base_path, name)


class structs(object):
    @staticmethod
    def is_struct(x):
        return dataclasses.is_dataclass(x)

    @staticmethod
    def to_dict(x):
        return dataclasses.asdict(x)


class types(object):
    @staticmethod
    def is_bool(x):
        return type(x) == bool

    @staticmethod
    def is_int(x):
        return type(x) == int

    @staticmethod
    def is_string(x):
        return type(x) == str

    @staticmethod
    def is_dict(x):
        return type(x) == dict

    @staticmethod
    def is_list(x):
        return type(x) == list

    @staticmethod
    def is_tuple(x):
        return type(x) == tuple