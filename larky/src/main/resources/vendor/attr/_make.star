# Ported from attrs 26.1.0 (7bfc49e9b22d5ba25b6e429524c3d49fee27cb36): src/attr/_make.py, src/attr/_funcs.py, src/attr/_config.py. See larky/src/main/resources/VENDORED.md.
#
# The MIT License (MIT)
#
# Copyright (c) 2015 Hynek Schlawack and the attrs contributors
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
"""
The class-building core of attrs: `attrib`, `attrs`, `make_class` and the
functions that work on instances.

attrs builds each class's ``__init__``, ``__repr__``, ``__eq__`` and ordering
methods by generating Python source and exec()ing it. Larky has neither exec
nor a ``class`` statement, so the generated methods are native (``jattrs``)
callables configured with the class's attributes, and a class is built from a
name, its bases and a namespace dict, as with `types.new_class`. See
``@vendor//attr`` for what is supported.
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//jattrs", _jattrs="jattrs")

NOTHING = _jattrs.NOTHING
"""
Sentinel to indicate the lack of a value when `None` is ambiguous.

``bool(NOTHING)`` is False.
"""

Factory = _jattrs.Factory
Attribute = _jattrs.Attribute

_DEFAULT_ON_SETATTR = _jattrs.DEFAULT_ON_SETATTR
_NO_OP = _jattrs.setters_NO_OP

_KW_ONLY_NO = "NO"
_KW_ONLY_YES = "YES"
_KW_ONLY_FORCE = "FORCE"


def _is_list_or_tuple(x):
    return type(x) == "list" or type(x) == "tuple"


def _counting_attr_marker():
    """Identifies the structs returned by `attrib`."""
    pass


def _is_counting_attr(x):
    return type(x) == "_CountingAttr" and getattr(x, "_counting_attr_marker", None) == _counting_attr_marker


def _CountingAttr(
        default,
        validator,
        repr,
        hash,
        init,
        converter,
        metadata,
        type,
        kw_only,
        eq,
        eq_key,
        order,
        order_key,
        on_setattr,
        alias):
    """
    Intermediate representation of attributes, returned by `attrib`.

    *Internal* data structure of the attrs library.  Running into is most
    likely the result of a bug like a forgotten `attr.s` decorator.
    """
    self = larky.mutablestruct(
        __class__="_CountingAttr",
        _counting_attr_marker=_counting_attr_marker,
        counter=_jattrs.next_counter(),
        _default=default,
        _validator=validator,
        converter=converter,
        repr=repr,
        eq=eq,
        eq_key=eq_key,
        order=order,
        order_key=order_key,
        hash=hash,
        init=init,
        metadata=metadata,
        type=type,
        kw_only=kw_only,
        on_setattr=on_setattr,
        alias=alias,
    )

    def validator_(meth):
        """
        Decorator that adds *meth* to the list of validators.

        Returns *meth* unchanged.
        """
        if self._validator == None:
            self._validator = meth
        else:
            self._validator = and_(self._validator, meth)
        return meth

    def default_(meth):
        """
        Decorator that allows to set the default for an attribute.

        Returns *meth* unchanged.

        Raises:
            DefaultAlreadySetError: If default has been set before.
        """
        if self._default != NOTHING:
            fail("DefaultAlreadySetError")
        self._default = Factory(meth, takes_self=True)
        return meth

    def __repr__():
        return (
            "_CountingAttr(counter=%r, _default=%r, repr=%r, eq=%r, order=%r, hash=%r, init=%r, on_setattr=%r, alias=%r, metadata=%r)"
            % (self.counter, self._default, self.repr, self.eq, self.order, self.hash, self.init, self.on_setattr, self.alias, self.metadata)
        )

    self.validator = validator_
    self.default = default_
    self.__repr__ = __repr__
    return self


def and_(*validators):
    """
    A validator that composes multiple validators into one.

    When called on a value, it runs all wrapped validators.

    Args:
        validators (~collections.abc.Iterable[typing.Callable]):
            Arbitrary number of validators.
    """
    return _jattrs.and_(*validators)


def attrib(
        default=NOTHING,
        validator=None,
        repr=True,
        cmp=None,
        hash=None,
        init=True,
        metadata=None,
        type=None,
        converter=None,
        factory=None,
        kw_only=None,
        eq=None,
        order=None,
        on_setattr=None,
        alias=None):
    """
    Create a new field / attribute on a class.

    Identical to `attrs.field`, except it's not keyword-only.

    Consider using `attrs.field` in new code (``attr.ib`` will *never* go away,
    though).

    ..  warning::

        Does **nothing** unless the class is also decorated with
        `attr.s` (or similar)!

    Larky: pass the result to `make_class` (or put it in the namespace of a
    class passed to `attr.s` / `attrs.define`). ``converter`` must be a
    callable taking the value; lists of converters (``pipe``) and
    ``attrs.Converter`` are not supported.
    """
    eq, eq_key, order, order_key = _jattrs.determine_attrib_eq_order(cmp, eq, order, True)

    if hash != None and hash != True and hash != False:
        fail("TypeError: Invalid value for hash.  Must be True, False, or None.")

    if factory != None:
        if default != NOTHING:
            fail("ValueError: The `default` and `factory` arguments are mutually exclusive.")
        if not _jattrs.callable(factory):
            fail("ValueError: The `factory` argument must be a callable.")
        default = Factory(factory)

    if metadata == None:
        metadata = {}

    # Apply syntactic sugar by auto-wrapping.
    if _is_list_or_tuple(on_setattr):
        on_setattr = _jattrs.setters_pipe(*on_setattr)

    if validator and _is_list_or_tuple(validator):
        validator = and_(*validator)

    if converter and _is_list_or_tuple(converter):
        fail("NotImplementedError: a list of converters (attrs.converters.pipe) is not supported in Larky")

    return _CountingAttr(
        default=default,
        validator=validator,
        repr=repr,
        hash=hash,
        init=init,
        converter=converter,
        metadata=metadata,
        type=type,
        kw_only=kw_only,
        eq=eq,
        eq_key=eq_key,
        order=order,
        order_key=order_key,
        on_setattr=on_setattr,
        alias=alias,
    )


def _determine_attrs_eq_order(cmp, eq, order, default_eq):
    """
    Validate the combination of *cmp*, *eq*, and *order*. Derive the effective
    values of eq and order.  If *eq* is None, set it to *default_eq*.
    """
    if cmp != None and (eq != None or order != None):
        fail("ValueError: Don't mix `cmp` with `eq' and `order`.")

    # cmp takes precedence due to bw-compatibility.
    if cmp != None:
        return cmp, cmp

    # If left None, equality is set to the specified default and ordering
    # mirrors equality.
    if eq == None:
        eq = default_eq

    if order == None:
        order = eq

    if eq == False and order == True:
        fail("ValueError: `order` can only be True if `eq` is True too.")

    return eq, order


def _determine_whether_to_implement(cls_dict, flag, auto_detect, dunders, default=True):
    """
    Check whether we should implement a set of methods for *cls*.

    *flag* is the argument passed into @attr.s like 'init', *auto_detect* the
    same as passed into @attr.s and *dunders* is a tuple of attribute names
    whose presence signal that the user has implemented it themselves.

    Return *default* if no reason for either for or against is found.
    """
    if flag == True or flag == False:
        return flag

    if flag == None and auto_detect == False:
        return default

    # Logically, flag is None and auto_detect is True here.
    for dunder in dunders:
        if dunder in cls_dict:
            return False

    return default


def is_frozen_class(cls):
    """Whether *cls* is a frozen attrs class or inherits from one (internal)."""
    props = getattr(cls, "__attrs_props__", None)
    return props != None and props.is_frozen


def _from_counting_attr(name, ca, kw_only):
    # The 'kw_only' argument is the class-level setting, and is used if the
    # attribute itself does not explicitly set 'kw_only'.
    return Attribute(
        name,
        ca._default,
        ca._validator,
        ca.repr,
        None,
        ca.hash,
        ca.init,
        False,
        metadata=ca.metadata,
        type=ca.type,
        converter=ca.converter,
        kw_only=kw_only if ca.kw_only == None else ca.kw_only,
        eq=ca.eq,
        eq_key=ca.eq_key,
        order=ca.order,
        order_key=ca.order_key,
        on_setattr=ca.on_setattr,
        alias=ca.alias,
        alias_is_default=ca.alias == None,
    )


def _collect_base_attrs(cls, taken_attr_names):
    """
    Collect attr.ibs from base classes of *cls*, except *taken_attr_names*.
    """
    base_attrs = []
    base_attr_map = {}  # A dictionary of base attrs to their classes.

    # Traverse the MRO and collect attributes.
    mro = list(cls.__mro__)
    for base_cls in reversed(mro[1:-1]):
        for a in getattr(base_cls, "__attrs_attrs__", []):
            if a.inherited or a.name in taken_attr_names:
                continue

            a = a.evolve(inherited=True)
            base_attrs.append(a)
            base_attr_map[a.name] = base_cls

    # For each name, only keep the freshest definition i.e. the furthest at the
    # back.  base_attr_map is fine because it gets overwritten with every new
    # instance.
    filtered = []
    seen = {}
    for a in reversed(base_attrs):
        if a.name in seen:
            continue
        filtered.insert(0, a)
        seen[a.name] = True

    return filtered, base_attr_map


def _transform_attrs(cls, cls_dict, these, auto_attribs, kw_only, field_transformer):
    """
    Transform all `_CountingAttr`s on a class into `Attribute`s.

    If *these* is passed, use that and don't look for them on the class.

    Return ``(attrs, base_attrs)``.
    """
    if these != None:
        ca_list = list(these.items())
    elif auto_attribs == True:
        # Larky has no annotations, so every attr.ib lacks one.
        unannotated = [
            name
            for name, a in sorted(cls_dict.items(), key=lambda e: getattr(e[1], "counter", 0))
            if _is_counting_attr(a)
        ]
        if unannotated:
            fail(
                "UnannotatedAttributeError: The following `attr.ib`s lack a type annotation: "
                + ", ".join(unannotated)
                + "."
            )
        ca_list = []
    else:
        ca_list = sorted(
            [(name, a) for name, a in cls_dict.items() if _is_counting_attr(a)],
            key=lambda e: e[1].counter,
        )

    for name, ca in ca_list:
        if not _is_counting_attr(ca):
            fail("TypeError: %r for %r is not an attr.ib() / attrs.field()" % (ca, name))

    own_attrs = [
        _from_counting_attr(attr_name, ca, kw_only != _KW_ONLY_NO)
        for attr_name, ca in ca_list
    ]

    base_attrs, base_attr_map = _collect_base_attrs(cls, [a.name for a in own_attrs])

    if kw_only == _KW_ONLY_FORCE:
        own_attrs = [a.evolve(kw_only=True) for a in own_attrs]
        base_attrs = [a.evolve(kw_only=True) for a in base_attrs]

    attrs = base_attrs + own_attrs

    # Resolve default field alias before executing field_transformer, so that
    # the transformer receives fully populated Attribute objects with usable
    # alias values.
    attrs = [
        a if a.alias else a.evolve(alias=_jattrs.default_init_alias_for(a.name), alias_is_default=True)
        for a in attrs
    ]

    if field_transformer != None:
        attrs = list(field_transformer(cls, attrs))

    # Check attr order after executing the field_transformer.
    # Mandatory vs non-mandatory attr order only matters when they are part of
    # the __init__ signature and when they aren't kw_only (which are moved to
    # the end and can be mandatory or non-mandatory in any order, as they will
    # be specified as keyword args anyway). Check the order of those attrs:
    had_default = False
    for a in attrs:
        if a.init == False or a.kw_only != False:
            continue
        if had_default == True and a.default == NOTHING:
            fail(
                "ValueError: No mandatory attributes allowed after an attribute with a default value or factory.  Attribute in question: %r"
                % (a,)
            )

        if had_default == False and a.default != NOTHING:
            had_default = True

    # Resolve default field alias for any new attributes that the
    # field_transformer may have added without setting an alias.
    attrs = [
        a if a.alias else a.evolve(alias=_jattrs.default_init_alias_for(a.name), alias_is_default=True)
        for a in attrs
    ]
    return attrs, base_attrs


def attrs(
        maybe_cls=None,
        these=None,
        repr_ns=None,
        repr=None,
        cmp=None,
        hash=None,
        init=None,
        slots=False,
        frozen=False,
        weakref_slot=True,
        str=False,
        auto_attribs=False,
        kw_only=False,
        cache_hash=False,
        auto_exc=False,
        eq=None,
        order=None,
        auto_detect=False,
        collect_by_mro=False,
        getstate_setstate=None,
        on_setattr=None,
        field_transformer=None,
        match_args=True,
        unsafe_hash=None,
        force_kw_only=True):
    r"""
    A class decorator that adds :term:`dunder methods` according to the
    specified attributes using `attr.ib` or the *these* argument.

    Consider using `attrs.define` / `attrs.frozen` in new code (``attr.s`` will
    *never* go away, though).

    Args:
        repr_ns (str):
            When using nested classes, there was no way in Python 2 to
            automatically detect that.  This argument allows to set a custom
            name for a more meaningful ``repr`` output.  This argument is
            pointless in Python 3 and is therefore deprecated.

    .. caution::
        Refer to `attrs.define` for the rest of the parameters, but note that they
        can have different defaults.

        Notably, leaving *on_setattr* as `None` will **not** add any hooks.

    Larky: Starlark has no class statement or decorator syntax. Call it on a
    class built with `types.new_class` whose namespace holds `attr.ib`\ s
    (``C = attr.s(C)`` or ``C = attr.s(frozen=True)(C)``), or use
    `make_class`. It returns a new class with the same name and bases.
    *slots*, *weakref_slot*, *getstate_setstate*, *match_args*, *auto_exc*,
    *collect_by_mro* and *repr_ns* are accepted and have no effect; base class
    attributes are always collected in MRO order. ``hash=True``,
    ``unsafe_hash=True`` and ``cache_hash=True`` fail: Larky instances hash
    by identity.
    """
    eq_, order_ = _determine_attrs_eq_order(cmp, eq, order, None)

    #  unsafe_hash takes precedence due to PEP 681.
    if unsafe_hash != None:
        hash = unsafe_hash

    if _is_list_or_tuple(on_setattr):
        on_setattr = _jattrs.setters_pipe(*on_setattr)

    def wrap(cls):
        if not _jattrs.is_class(cls):
            fail("TypeError: attrs can only decorate a class, not %s" % type(cls))
        cls_dict = _jattrs.class_body(cls)
        is_frozen = frozen or is_frozen_class(cls)

        if hash != None and hash != True and hash != False:
            fail("TypeError: Invalid value for hash.  Must be True, False, or None.")
        if hash == True or cache_hash:
            fail("NotImplementedError: hashing attrs instances by value (hash/unsafe_hash/cache_hash=True) is not supported in Larky")

        eq_impl = _determine_whether_to_implement(cls_dict, eq_, auto_detect, ("__eq__", "__ne__"))

        if kw_only:
            kwo = _KW_ONLY_FORCE if force_kw_only else _KW_ONLY_YES
        else:
            kwo = _KW_ONLY_NO

        added_init = _determine_whether_to_implement(cls_dict, init, auto_detect, ("__init__",))
        added_repr = _determine_whether_to_implement(cls_dict, repr, auto_detect, ("__repr__",))
        added_ordering = _determine_whether_to_implement(
            cls_dict, order_, auto_detect, ("__lt__", "__le__", "__gt__", "__ge__"))

        attrs_list, base_attrs = _transform_attrs(cls, cls_dict, these, auto_attribs, kwo, field_transformer)
        fields_tuple = _jattrs.make_fields(attrs_list)

        ns = {}
        attr_names = [a.name for a in attrs_list]
        for k, v in cls_dict.items():
            if these == None and (k in attr_names or _is_counting_attr(v)):
                continue  # attrs removes the attr.ib()s from the class body
            ns[k] = v

        ns.update(_jattrs.class_namespace(
            cls.__name__,
            fields_tuple,
            "__init__" if added_init else "__attrs_init__",
            added_repr,
            eq_impl,
            added_ordering,
            str,
            is_frozen,
            None if is_frozen and on_setattr == _NO_OP else on_setattr,
        ))
        ns["__attrs_props__"] = larky.struct(
            is_frozen=is_frozen,
            is_slotted=slots,
            added_init=added_init,
            added_repr=added_repr,
            added_eq=eq_impl,
            added_ordering=added_ordering,
            added_str=str,
            kw_only=kwo,
            on_setattr_hook=on_setattr,
            field_transformer=field_transformer,
        )
        return types.new_class(cls.__name__, tuple(cls.__bases__), None, lambda body: body.update(ns))

    # maybe_cls's type depends on the usage of the decorator.  It's a class
    # if it's used as `@attrs` but `None` if used as `@attrs()`.
    if maybe_cls == None:
        return wrap

    return wrap(maybe_cls)


_attrs = attrs


def make_class(name, attrs, bases=(object,), class_body=None, **attributes_arguments):
    r"""
    A quick way to create a new class called *name* with *attrs*.

    .. note::

        ``make_class()`` is a thin wrapper around `attr.s`, not `attrs.define`
        which means that it doesn't come with some of the improved defaults.

        For example, if you want the same ``on_setattr`` behavior as in
        `attrs.define`, you have to pass the hooks yourself: ``make_class(...,
        on_setattr=setters.pipe(setters.convert, setters.validate)``

    .. warning::

        It is *your* duty to ensure that the class name and the attribute names
        are valid identifiers. ``make_class()`` will *not* validate them for
        you.

    Args:
        name (str): The name for the new class.

        attrs (list | dict):
            A list of names or a dictionary of mappings of names to `attr.ib`\
            s / `attrs.field`\ s.

            The order is deduced from the order of the names or attributes
            inside *attrs*.  Otherwise the order of the definition of the
            attributes is used.

        bases (tuple[type, ...]): Classes that the new class will subclass.

        class_body (dict):
            An optional dictionary of class attributes for the new class.

        attributes_arguments: Passed unmodified to `attr.s`.

    Returns:
        type: A new class with *attrs*.
    """
    if type(attrs) == "dict":
        cls_dict = dict(attrs)
    elif _is_list_or_tuple(attrs):
        cls_dict = {a: attrib() for a in attrs}
    else:
        fail("TypeError: attrs argument must be a dict or a list.")

    pre_init = cls_dict.pop("__attrs_pre_init__", None)
    post_init = cls_dict.pop("__attrs_post_init__", None)
    user_init = cls_dict.pop("__init__", None)

    body = {}
    if class_body != None:
        body.update(class_body)
    if pre_init != None:
        body["__attrs_pre_init__"] = pre_init
    if post_init != None:
        body["__attrs_post_init__"] = post_init
    if user_init != None:
        body["__init__"] = user_init

    type_ = types.new_class(name, tuple(bases), None, lambda ns: ns.update(body))

    # We do it here for proper warnings with meaningful stacklevel.
    attributes_arguments = dict(attributes_arguments)
    cmp = attributes_arguments.pop("cmp", None)
    eq, order = _determine_attrs_eq_order(
        cmp,
        attributes_arguments.get("eq"),
        attributes_arguments.get("order"),
        True,
    )
    attributes_arguments["eq"] = eq
    attributes_arguments["order"] = order

    return _attrs(these=cls_dict, **attributes_arguments)(type_)


def fields(cls):
    """
    Return the tuple of *attrs* attributes for a class or instance.

    The tuple also allows accessing the fields by their names (see below for
    examples).

    Args:
        cls (type): Class or instance to introspect.

    Raises:
        TypeError: If *cls* is neither a class nor an *attrs* instance.

        attrs.exceptions.NotAnAttrsClassError:
            If *cls* is not an *attrs* class.

    Returns:
        tuple (with name accessors) of `attrs.Attribute`
    """
    return _jattrs.fields(cls)


def fields_dict(cls):
    """
    Return an ordered dictionary of *attrs* attributes for a class, whose keys
    are the attribute names.

    Args:
        cls (type): Class to introspect.

    Raises:
        TypeError: If *cls* is not a class.

        attrs.exceptions.NotAnAttrsClassError:
            If *cls* is not an *attrs* class.

    Returns:
        dict[str, attrs.Attribute]: Dict of attribute name to definition
    """
    return _jattrs.fields_dict(cls)


def validate(inst):
    """
    Validate all attributes on *inst* that have a validator.

    Leaves all exceptions through.

    Args:
        inst: Instance of a class with *attrs* attributes.
    """
    _jattrs.validate(inst)


def evolve(*args, **changes):
    """
    Create a new instance, based on the first positional argument with
    *changes* applied.

    Args:

        inst:
            Instance of a class with *attrs* attributes. *inst* must be passed
            as a positional argument.

        changes:
            Keyword changes in the new copy.

    Returns:
        A copy of inst with *changes* incorporated.

    Raises:
        TypeError:
            If *attr_name* couldn't be found in the class ``__init__``.

        attrs.exceptions.NotAnAttrsClassError:
            If *cls* is not an *attrs* class.
    """
    return _jattrs.evolve(*args, **changes)


def has(cls):
    """
    Check whether *cls* is a class with *attrs* attributes.

    Args:
        cls (type): Class to introspect.

    Raises:
        TypeError: If *cls* is not a class.

    Returns:
        bool:
    """
    return _jattrs.has(cls)


def asdict(
        inst,
        recurse=True,
        filter=None,
        dict_factory=dict,
        retain_collection_types=False,
        value_serializer=None):
    """
    Return the *attrs* attribute values of *inst* as a dict.

    Optionally recurse into other *attrs*-decorated classes.

    Args:
        inst: Instance of an *attrs*-decorated class.

        recurse (bool): Recurse into classes that are also *attrs*-decorated.

        filter (~typing.Callable):
            A callable whose return code determines whether an attribute or
            element is included (`True`) or dropped (`False`).  Is called with
            the `attrs.Attribute` as the first argument and the value as the
            second argument.

        dict_factory (~typing.Callable):
            A callable to produce dictionaries from.  For example, to produce
            ordered dictionaries instead of normal Python dictionaries, pass in
            ``collections.OrderedDict``.

        retain_collection_types (bool):
            Do not convert to `list` when encountering an attribute whose type
            is `tuple` or `set`.  Only meaningful if *recurse* is `True`.

        value_serializer (typing.Callable | None):
            A hook that is called for every attribute or dict key/value.  It
            receives the current instance, field and value and must return the
            (updated) value.  The hook is run *after* the optional *filter* has
            been applied.

    Returns:
        Return type of *dict_factory*.

    Raises:
        attrs.exceptions.NotAnAttrsClassError:
            If *cls* is not an *attrs* class.
    """
    return _jattrs.asdict(inst, recurse, filter, dict_factory, retain_collection_types, value_serializer)


def astuple(
        inst,
        recurse=True,
        filter=None,
        tuple_factory=tuple,
        retain_collection_types=False):
    """
    Return the *attrs* attribute values of *inst* as a tuple.

    Optionally recurse into other *attrs*-decorated classes.

    Args:
        inst: Instance of an *attrs*-decorated class.

        recurse (bool):
            Recurse into classes that are also *attrs*-decorated.

        filter (~typing.Callable):
            A callable whose return code determines whether an attribute or
            element is included (`True`) or dropped (`False`).  Is called with
            the `attrs.Attribute` as the first argument and the value as the
            second argument.

        tuple_factory (~typing.Callable):
            A callable to produce tuples from. For example, to produce lists
            instead of tuples.

        retain_collection_types (bool):
            Do not convert to `list` or `dict` when encountering an attribute
            which type is `tuple`, `dict` or `set`. Only meaningful if
            *recurse* is `True`.

    Returns:
        Return type of *tuple_factory*

    Raises:
        attrs.exceptions.NotAnAttrsClassError:
            If *cls* is not an *attrs* class.
    """
    return _jattrs.astuple(inst, recurse, filter, tuple_factory, retain_collection_types)


def set_run_validators(run):
    """
    Set whether or not validators are run.  By default, they are run.

    .. deprecated:: 21.3.0 It will not be removed, but it also will not be
        moved to new ``attrs`` namespace. Use `attrs.validators.set_disabled()`
        instead.

    Larky: the setting is per evaluation (Starlark thread), not global.
    """
    _jattrs.set_run_validators(run)


def get_run_validators():
    """
    Return whether or not validators are run.

    .. deprecated:: 21.3.0 It will not be removed, but it also will not be
        moved to new ``attrs`` namespace. Use `attrs.validators.get_disabled()`
        instead.
    """
    return _jattrs.get_run_validators()
