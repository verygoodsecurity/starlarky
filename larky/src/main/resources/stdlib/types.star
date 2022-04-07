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

_is_instance = larky.is_instance

# create instance singletons to avoid unnecessary allocations
_a_bool_type = type(True)
_a_dict_type = type({})
_a_list_type = type([])
_a_string_type = type("")
_a_tuple_type = type(())
_an_int_type = type(1)
_a_float_type = type(5.0)
_a_struct_type = type(larky.struct())
_a_mutablestruct_type = type(larky.mutablestruct())
_a_range_type = type(range(1))


def _a_function():
    pass


_a_function_type = type(_a_function)

_a_lambda_type = type(lambda x: 1)


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


def _is_float(v):
    """Returns True if v is an instance of a floating point number.

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v is an instance of a floating point number, False otherwise.
    """
    return type(v) == _a_float_type


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


def _is_lambda(v):
    """Returns True if v is an instance of a lambda.

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v is an instance of a lambda, False otherwise.
    """
    return type(v) == _a_lambda_type


def _is_callable(v):
    """Returns True if v is a callable: an instance of a function or a lambda

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v is an instance of a callable, False otherwise.
    """
    return _is_function(v) or _is_lambda(v)


def _is_set(v):
    """Returns True if v is a set created by sets.make().

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v was created by sets.make(), False otherwise.
    """
    return type(v) == _a_struct_type and hasattr(v, "_values") and _is_dict(
        v._values)


def _MethodType(func, instance):
    """
    Binds func to the instance class `ab`
    :return:
    """
    return larky.partial(func, instance)


def _is_subclass(sub_class, parent_class):
    if not hasattr(sub_class, '__mro__'):
        return False

    mro = getattr(sub_class, '__mro__', [])
    return parent_class in mro


def _is_range(iterz):
    return type(iterz) == _a_range_type


def _is_iterable(iterz):
    # Checking isinstance(obj, Iterable) detects classes that are
    # registered as Iterable or that have an __iter__() method,
    # but it does not detect classes that iterate with the __getitem__()
    # method. The only reliable way to determine whether an object
    # is iterable is to call iter(obj).
    return (_is_tuple(iterz)
            or _is_list(iterz)
            or _is_range(iterz)
            or hasattr(iterz, "__iter__"))


def _is_bytes(bobj):
    return _is_instance(bobj, bytes)


def _is_bytearray(barrobj):
    return _is_instance(barrobj, bytearray)


def _is_bytelike(b):
    return _is_bytes(b) or _is_bytearray(b)


def _is_mutablestruct(v):
    """Returns True if v is a mutablestruct created by larky.mutablestruct()

    Args:
      v: The value whose type should be checked.

    Returns:
      True if v was created by larky.mutablestruct(), False otherwise.
    """
    return type(v) == _a_mutablestruct_type


def _is_structlike(v):
    return _is_mutablestruct(v) or type(v) == _a_struct_type


def _type_maker(name, resolved_bases, ns, kwds):
    print("in type maker: ", name)
    print(resolved_bases)
    print(ns)
    print(kwds)
    return type(name, resolved_bases, ns, **kwds)


# Provide a PEP 3115 compliant mechanism for class creation
def new_class(name, bases=(), kwds=None, exec_body=None):
    """Create a class object dynamically using the appropriate metaclass.
    .. python::

        class MyStaticClass(object, metaclass=MySimpleMeta):
            pass

    is an equivalent to:

    .. python::

        MyStaticClass = types.new_class(
            "MyStaticClass",
            (object,),
            {"metaclass": MyMeta},
            lambda ns: ns
        )


    """
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
        if larky.is_instance(base, type):
            continue
        if not hasattr(base, "__mro_entries__"):
            continue
        new_base = base.__mro_entries__(bases)
        updated = True
        if not larky.is_instance(new_base, tuple):
            fail("__mro_entries__ must return a tuple")
        else:
            _l = list(new_bases[:i + shift])
            _l.extend(new_base)
            _l.extend(new_bases[i + shift + 1:])
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
        kwds = dict(kwds)  # Don't alter the provided mapping
    if 'metaclass' in kwds:
        meta = kwds.pop('metaclass')
    else:
        if bases:
            meta = type(bases[0])
        else:
            meta = type
    if larky.is_instance(meta, type):
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
        if larky.is_subclass(winner, base_meta):
            continue
        if larky.is_subclass(base_meta, winner):
            winner = base_meta
            continue
        # else:
        fail("metaclass conflict: " +
             "the metaclass of a derived class " +
             "must be a (non-strict) subclass " +
             "of the metaclasses of all its bases")
    return winner


def _head(seq):
    return seq[0]


def _tail(seq):
    return seq[1:]


def not_in_tails(seq):
    tails = [_tail(x) for x in seq]

    def _not_in_tails_(c):
        return all([c not in s for s in tails])

    return _not_in_tails_


def tail_if_not_eq(val):
    def _tail_if_not_eq_(s):
        return _tail(s) if _head(s) == val else s
    return _tail_if_not_eq_


def _filter(func, iterable):
    return [x for x in iterable if func(x) == True]


def non_empty(seq):
    return _filter(bool, seq)


def merge_mro(seqs):
    seqs = [list(x) for x in seqs]
    non_empty_seqs = non_empty(seqs)
    result = []

    for _while_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
        if not non_empty_seqs or len(non_empty_seqs) == 0:
            break
        cand = _filter(
            not_in_tails(non_empty_seqs),
            [_head(x) for x in non_empty_seqs]
        )

        if len(cand) == 0:
            fail("Inconsistent hierarchy")

        result.append(cand[0])

        non_empty_seqs = non_empty(
            [tail_if_not_eq(cand[0])(x) for x in non_empty_seqs]
        )

    return result


def make_type(name, bases=None, attrs=None):
    """Own analog of type for instantiation.
        Like the real one, it takes three arguments: class name,
        a list of his parents and a set of his attributes.
    """
    return dict(
        __my_name__=name,
        __my_bases__=bases or [],
        __my_dict__=attrs or {},
    )


def make_class(name, bases=(), cls_dict=None):
    """
    Construct a class dictionary
    """
    cls = {
        '__name__': name,
        '__bases__': bases,
        # HUGE HACK
        '__class_dict__': cls_dict or {}
    }
    cls.update(cls_dict or {})
    cls = larky.mutablestruct(**cls)
    base_mros = [[cls]] + [b['__mro__'] for b in bases]
    mro = tuple(merge_mro(base_mros))
    cls.mro = lambda: mro
    cls.__mro__ = mro
    cls.__call__ = larky.partial(new, cls)
    return cls


def new(cls, *args, **kwargs):
    """
    Construct a new instance of the given class
    """
    instance = larky.mutablestruct(
        __class__=cls,
        __name__=cls.__name__,
        # HUGE HACK
        **cls.__class_dict__
    )
    init = getattr(cls, '__init__', None)
    if init:
        init(instance, *args, **kwargs)

    return instance


def get(instance, attr_name):
    """
    Retrieve the instance attribute, binding it if it is a method.
    """
    attr = getattr(instance, attr_name, larky.SENTINEL)
    if attr != larky.SENTINEL:
        if _is_function(attr):
            attr = larky.partial(attr, instance)
        return attr

    for cls in instance.__class__.__mro__:
        # print("get:", cls, "attr", attr_name)
        attr = getattr(cls, attr_name, larky.SENTINEL)
        if attr == larky.SENTINEL:
            continue

        if _is_function(attr):
            attr = larky.partial(attr, instance)
        # no support for instance or static methods..
        # elif isinstance(attr, staticmethod):
        #     attr = attr.__func__
        #
        # elif isinstance(attr, classmethod):
        #     attr = partial(attr.__func__, cls)

        return attr

    fail("AttributeError: '%s' instance has no attribute '%s'" %
         (instance.__class__.__name__, attr_name))


def set_(instance, attr_name, val):
    """
    Set the instance attribute to the value
    """
    setattr(instance, attr_name, val)


def del_(instance, attr_name):
    """
    Delete the instance attribute
    """
    if hasattr(instance, attr_name):
        # TODO...impl delattr
        instance.pop(attr_name)
        return
    fail("AttributeError: %s" % attr_name)


types = larky.struct(
    is_list=_is_list,
    is_string=_is_string,
    is_bool=_is_bool,
    is_none=_is_none,
    is_int=_is_int,
    is_float=_is_float,
    is_tuple=_is_tuple,
    is_dict=_is_dict,
    is_function=_is_function,
    is_lambda=_is_lambda,
    is_callable=_is_callable,
    is_set=_is_set,
    is_range=_is_range,
    is_instance=_is_instance,
    is_iterable=_is_iterable,
    is_bytes=_is_bytes,
    is_bytearray=_is_bytearray,
    is_bytelike=_is_bytelike,
    is_mutablestruct=_is_mutablestruct,
    is_structlike=_is_structlike,
    MethodType=_MethodType,
    new_class=new_class,
    resolve_bases=resolve_bases,
    prepare_class=prepare_class
)
