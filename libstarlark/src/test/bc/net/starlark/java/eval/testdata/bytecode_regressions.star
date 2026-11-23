# Cases that previously diverged between the bytecode VM and the tree-walker.
# Run with ScriptTest; -Dstarlark.bytecode selects the execution mode.

def test_inplace_list_and_dict():
    # list += list extends in place; dict |= dict merges in place
    a = [1]
    alias = a
    a += [2]
    assert_eq(alias, [1, 2])
    d = {"x": 1}
    dalias = d
    d |= {"y": 2}
    assert_eq(dalias, {"x": 1, "y": 2})

test_inplace_list_and_dict()

def test_augmented_index_evaluates_once():
    calls = []

    def key():
        calls.append(1)
        return 0

    b = [10]
    b[key()] += 5
    assert_eq(b, [15])
    assert_eq(len(calls), 1)

test_augmented_index_evaluates_once()

def test_augmented_operators():
    n = 6
    n -= 1
    n *= 4
    n //= 3
    n %= 5
    n |= 8
    n &= 12
    n ^= 1
    n <<= 2
    n >>= 1
    assert_eq(n, 18)

test_augmented_operators()

def test_bitwise_operators():
    assert_eq(12 & 10, 8)
    assert_eq(12 | 3, 15)
    assert_eq(12 ^ 10, 6)
    assert_eq(1 << 4, 16)
    assert_eq(256 >> 4, 16)
    assert_eq(~5, -6)

test_bitwise_operators()

def make_adder(k):
    return lambda x: x + k

def test_lambdas():
    add = lambda x, y = 10: x + y
    assert_eq(add(1), 11)
    assert_eq(add(1, 2), 3)
    assert_eq(make_adder(3)(4), 7)
    assert_eq(sorted([3, 1, 2], key = lambda v: -v), [3, 2, 1])

test_lambdas()

def test_bytes_literals():
    assert_eq(type(b"ab"), "bytes")
    assert_eq(len(b"\x00\x01"), 2)

test_bytes_literals()

def test_numeric_constants_stay_distinct():
    # 1 == 1.0 and 0.0 == -0.0, but they are different constants
    values = [1.0, 1, -0.0, 0.0, 0]
    assert_eq([type(v) for v in values], ["float", "int", "float", "float", "int"])
    assert_eq([10, 20, 30][::-1], [30, 20, 10])

test_numeric_constants_stay_distinct()

def first(xs):
    for x in xs:
        return x

def nested_first(xs):
    for x in xs:
        for y in xs:
            return y

def test_return_from_loop_releases_lock():
    lst = [1, 2]
    first(lst)
    nested_first(lst)
    lst.append(3)
    assert_eq(lst, [1, 2, 3])

test_return_from_loop_releases_lock()

def kw(**kwargs):
    return list(kwargs.keys())

def test_keyword_argument_order():
    assert_eq(kw(c = 1, a = 2, b = 3), ["c", "a", "b"])
    assert_eq(kw(z = 1, **{"y": 2, "x": 3}), ["z", "y", "x"])

test_keyword_argument_order()
