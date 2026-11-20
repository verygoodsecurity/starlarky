# Ported from CPython 3.5-3.10, pinned to 3.9.5 (0a7dcbd): Lib/operator.py. See larky/src/main/resources/VENDORED.md.

"""Shared comparison operators used without loading the full operator module."""
load("@stdlib//larky", "larky")

def create_rich_comparison(operator,    # type: str
                            name,        # type: str
                            reflection,  # type: str
                            default      # type: Callable[[str, Any, Any], bool]
                            ):
    # type: (...)  -> Callable[[Any, Any], Any]
    """Create a rich comparison function.
    The 'operator' parameter is the human-readable symbol of the operation (e.g.
    `>`). The 'name' parameter is the primary function (e.g. __gt__), while
    'reflection' is the reflection of that function (e.g. __lt__). The 'default'
    parameter is a callable to use when both functions don't exist and/or return
    NotImplemented.
    """

    def _rich_comparison(lhs, rhs):
        # type:  (Any, Any) -> Any
        """Implement the rich comparison `a {operator} b`."""
        lhs_type = type(lhs)
        lhs_method = getattr(lhs, name, larky.SENTINEL)

        rhs_type = type(rhs)
        rhs_method = getattr(rhs, reflection, larky.SENTINEL)

        call_lhs = lhs, lhs_method, rhs
        call_rhs = rhs, rhs_method, lhs
        # print("here1 @ ", name, operator, lhs_type, ":", call_lhs)
        # print("here2 @ ", name, operator, rhs_type, ":", call_rhs, "reflection?", reflection)
        if rhs_type == lhs_type:
            calls = call_rhs, call_lhs
        else:
            calls = call_lhs, call_rhs

        _larky_forelse_run_else = True
        for first_obj, meth, second_obj in calls:
            if meth == larky.SENTINEL:
                continue
            # print(meth, first_obj, second_obj)
            value = meth(second_obj)
            return value

        if _larky_forelse_run_else:
            return default(operator, lhs, rhs)

    return _rich_comparison


eq = create_rich_comparison("==",
                                 "__eq__", "__eq__", lambda _, a, b: a == b)
