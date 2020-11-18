# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Skylib module containing functions checking types."""
load("@stdlib/larky", "larky")

# create instance singletons to avoid unnecessary allocations
_a_bool_type = type(True)
_a_dict_type = type({})
_a_list_type = type([])
_a_string_type = type("")
_a_tuple_type = type(())
_an_int_type = type(1)
_a_struct_type = type(larky.struct())

def _a_function():
    pass

_a_function_type = type(_a_function)

def _is_list(v):
    """Returns True if v is an instance of a list.

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v is an instance of a list, False otherwise.
    """
    return type(v) == _a_list_type

def _is_string(v):
    """Returns True if v is an instance of a string.

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v is an instance of a string, False otherwise.
    """
    return type(v) == _a_string_type

def _is_bool(v):
    """Returns True if v is an instance of a bool.

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v is an instance of a bool, False otherwise.
    """
    return type(v) == _a_bool_type

def _is_none(v):
    """Returns True if v has the type of None.

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v is None, False otherwise.
    """
    return type(v) == type(None)

def _is_int(v):
    """Returns True if v is an instance of a signed integer.

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v is an instance of a signed integer, False otherwise.
    """
    return type(v) == _an_int_type

def _is_tuple(v):
    """Returns True if v is an instance of a tuple.

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v is an instance of a tuple, False otherwise.
    """
    return type(v) == _a_tuple_type

def _is_dict(v):
    """Returns True if v is an instance of a dict.

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v is an instance of a dict, False otherwise.
    """
    return type(v) == _a_dict_type

def _is_function(v):
    """Returns True if v is an instance of a function.

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v is an instance of a function, False otherwise.
    """
    return type(v) == _a_function_type

def _is_set(v):
    """Returns True if v is a set created by sets.make().

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v was created by sets.make(), False otherwise.
    """
    return type(v) == _a_struct_type and hasattr(v, "_values") and _is_dict(v._values)


def _MethodType(func, instance):
    """
    Binds func to the instance class `ab`
    :return:
    """
    return larky.partial(func, instance)


def _is_instance(instance, some_class):
    t = type(instance)
    cls_type = str(some_class)
    if 'built-in' in cls_type:
        cls_type = cls_type.split(" ")[-1].rpartition(">")[0]
    return t == cls_type


def _is_subclass(sub_class, parent_class):
    if not hasattr(sub_class, '__mro__'):
        return False

    mro = getattr(sub_class, '__mro__', [])
    return parent_class in mro


def _type_maker(name, *args, **kwargs):
    print(name)
    print(args)
    print(kwargs)


# Provide a PEP 3115 compliant mechanism for class creation
def new_class(name, bases=(), kwds=None, exec_body=None):
    """Create a class object dynamically using the appropriate metaclass."""
    resolved_bases = resolve_bases(bases)
    meta, ns, kwds = prepare_class(name, resolved_bases, kwds)
    if exec_body != None:
        exec_body(ns)
    if resolved_bases != bases:
        ns['__orig_bases__'] = bases
    return meta(name, resolved_bases, ns, **kwds)


def resolve_bases(bases):
    """Resolve MRO entries dynamically as specified by PEP 560."""
    new_bases = list(bases)
    updated = False
    shift = 0
    for i, base in enumerate(bases):
        if _is_instance(base, type):
            continue
        if not hasattr(base, "__mro_entries__"):
            continue
        new_base = base.__mro_entries__(bases)
        updated = True
        if not _is_instance(new_base, tuple):
            fail("__mro_entries__ must return a tuple")
        else:
            _l = list(new_bases[:i+shift])
            _l.extend(new_base)
            _l.extend(new_bases[i+shift+1:])
            new_bases = _l
            shift += len(new_base) - 1
    if not updated:
        return bases
    return tuple(new_bases)


def prepare_class(name, bases=(), kwds=None):
    """Call the __prepare__ method of the appropriate metaclass.

    Returns (metaclass, namespace, kwds) as a 3-tuple

    *metaclass* is the appropriate metaclass
    *namespace* is the prepared class namespace
    *kwds* is an updated copy of the passed in kwds argument with any
    'metaclass' entry removed. If no kwds argument is passed in, this will
    be an empty dict.
    """
    if kwds == None:
        kwds = {}
    else:
        kwds = dict(kwds) # Don't alter the provided mapping
    if 'metaclass' in kwds:
        meta = kwds.pop('metaclass')
    else:
        if bases:
            meta = type(bases[0])
        else:
            meta = larky.partial(_type_maker, name)
    if _is_instance(meta, type):
        # when meta is a type, we first determine the most-derived metaclass
        # instead of invoking the initial candidate directly
        meta = _calculate_meta(meta, bases)
    if hasattr(meta, '__prepare__'):
        ns = meta.__prepare__(name, bases, **kwds)
    else:
        ns = {}
    return meta, ns, kwds


def _calculate_meta(meta, bases):
    """Calculate the most derived metaclass."""
    winner = meta
    for base in bases:
        base_meta = type(base)
        if _is_subclass(winner, base_meta):
            continue
        if _is_subclass(base_meta, winner):
            winner = base_meta
            continue
        # else:
        fail("metaclass conflict: "+
             "the metaclass of a derived class "+
                        "must be a (non-strict) subclass "+
                        "of the metaclasses of all its bases")
    return winner

# from collections import Callable
# from functools import partial
# from .mro import merge_mro

def head(seq):
    return seq[0]


def tail(seq):
    return seq[1:]


def map(func, iterable):
    return [func(x) for x in iterable]


def filter(func, iterable):
    return [x for x in iterable if func(x) == True]


def not_in_tails(seq):
    tails = map(tail, seq)
    # return lambda c: all(c not in s for s in tails)


def tail_if_not_eq(val):
    pass
    # return lambda s: tail(s) if head(s) == val else s


def non_empty(seq):
    return filter(bool, seq)


def merge_mro(seqs):
    seqs = map(list, seqs)

    non_empty_seqs = non_empty(seqs)

    # while True:
    #     try:
    #         heads = imap(head, non_empty_seqs)
    #         cand = next(ifilter(not_in_tails(non_empty_seqs),
    #                             heads))
    #     except StopIteration:
    #         raise Exception("Inconsistent hierarchy")
    #
    #     yield cand
    #
    #     non_empty_seqs = non_empty(
    #         map(tail_if_not_eq(cand), non_empty_seqs))
    #
    #     if not non_empty_seqs:
    #         return


def make_class(name, bases=tuple(), cls_dict=None):
    """
    Construct a class dictionary
    """
    cls = {
        '__name__': name,
        '__bases__': bases,
    }
    cls.update(cls_dict or {})

    base_mros = [[cls]] + [b['mro'] for b in bases]
    cls['mro'] = tuple(merge_mro(base_mros))
    return cls


def new(cls, *args, **kwargs):
    """
    Construct a new instance of the given class
    """
    instance = {
        '__class__': cls
    }

    init = cls.get('__init__', None)
    if init:
        init(instance, *args, **kwargs)

    return instance


def get(instance, attr_name):
    """
    Retrieve the instance attribute, binding it if it is a method.
    """
    if attr_name in instance:
        return instance[attr_name]

    for cls in instance['__class__']['mro']:
        if attr_name in cls:
            attr = cls[attr_name]

            if _is_function(attr):
                attr = larky.partial(attr, instance)
            # if isinstance(attr, Callable):
            #     attr = partial(attr, instance)
            #
            # elif isinstance(attr, staticmethod):
            #     attr = attr.__func__
            #
            # elif isinstance(attr, classmethod):
            #     attr = partial(attr.__func__, cls)

            return attr

    fail("'%s' instance has no attribute '%s'" %
                         (cls['__name__'], attr_name))


def set_(instance, attr_name, val):
    """
    Set the instance attribute to the value
    """
    instance[attr_name] = val


def del_(instance, attr_name):
    """
    Delete the instance attribute
    """
    if attr_name in instance:
        instance.pop(attr_name)
    fail(attr_name)


types = larky.struct(
    is_list = _is_list,
    is_string = _is_string,
    is_bool = _is_bool,
    is_none = _is_none,
    is_int = _is_int,
    is_tuple = _is_tuple,
    is_dict = _is_dict,
    is_function = _is_function,
    is_set = _is_set,
    is_instance = _is_instance,
    MethodType = _MethodType,
    new_class = new_class,
    resolve_bases = resolve_bases,
    prepare_class = prepare_class,
)