load("@stdlib//larky", larky="larky")
load("@stdlib//collections", deque="deque")
load("@stdlib//operator", operator="operator")
load("@stdlib//random", random="random")
load("@stdlib//struct", struct="struct")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")

BIG = 100000


def fail():
    # PY2LARKY: pay attention to this!
    return SyntaxError
    return 1


def BadCmp():
    def BadCmp___eq__(other):
        # PY2LARKY: pay attention to this!
        return RuntimeError

    self.BadCmp___eq__ = BadCmp___eq__
    return self


def MutateCmp():
    def MutateCmp___init__(deque, result):
        deque = deque
        result = result

    self.MutateCmp___init__ = MutateCmp___init__

    def MutateCmp___eq__(other):
        deque.clear()
        return result

    self.MutateCmp___eq__ = MutateCmp___eq__
    return self


def TestBasic_test_basics():
    d = deque(range(-5125, -5000))
    d.__init__(range(200))
    for i in range(200, 400):
        d.append(i)
    for i in reversed(range(-200, 0)):
        d.appendleft(i)
    asserts.assert_that(list(d)).is_equal_to(list(range(-200, 400)))
    asserts.assert_that(len(d)).is_equal_to(600)

    left = [d.popleft() for i in range(250)]
    asserts.assert_that(left).is_equal_to(list(range(-200, 50)))
    asserts.assert_that(list(d)).is_equal_to(list(range(50, 400)))

    right = [d.pop() for i in range(250)]
    right.reverse()
    asserts.assert_that(right).is_equal_to(list(range(150, 400)))
    asserts.assert_that(list(d)).is_equal_to(list(range(50, 150)))


def TestBasic_test_maxlen():
    asserts.assert_fails(lambda: deque("abc", -1), ".*?ValueError")
    asserts.assert_fails(lambda: deque("abc", -2), ".*?ValueError")
    it = iter(range(10))
    d = deque(it, maxlen=3)
    asserts.assert_that(list(it)).is_equal_to([])
    asserts.assert_that(repr(d)).is_equal_to("deque([7, 8, 9], maxlen=3)")
    asserts.assert_that(list(d)).is_equal_to([7, 8, 9])
    asserts.assert_that(d).is_equal_to(deque(range(10), 3))
    d.append(10)
    asserts.assert_that(list(d)).is_equal_to([8, 9, 10])
    d.appendleft(7)
    asserts.assert_that(list(d)).is_equal_to([7, 8, 9])
    d.extend([10, 11])
    asserts.assert_that(list(d)).is_equal_to([9, 10, 11])
    d.extendleft([8, 7])
    asserts.assert_that(list(d)).is_equal_to([7, 8, 9])
    d = deque(range(200), maxlen=10)
    d.append(d)
    asserts.assert_that(repr(d)[-30:]).is_equal_to(
        ", 198, 199, [...]], maxlen=10)")
    d = deque(range(10), maxlen=None)
    asserts.assert_that(repr(d)).is_equal_to(
        "deque([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])")


def TestBasic_test_maxlen_zero():
    it = iter(range(100))
    deque(it, maxlen=0)
    asserts.assert_that(list(it)).is_equal_to([])

    it = iter(range(100))
    d = deque(maxlen=0)
    d.extend(it)
    asserts.assert_that(list(it)).is_equal_to([])

    it = iter(range(100))
    d = deque(maxlen=0)
    d.extendleft(it)
    asserts.assert_that(list(it)).is_equal_to([])


def TestBasic_test_maxlen_attribute():
    asserts.assert_that(deque().maxlen).is_equal_to(None)
    asserts.assert_that(deque("abc").maxlen).is_equal_to(None)
    asserts.assert_that(deque("abc", maxlen=4).maxlen).is_equal_to(4)
    asserts.assert_that(deque("abc", maxlen=2).maxlen).is_equal_to(2)
    asserts.assert_that(deque("abc", maxlen=0).maxlen).is_equal_to(0)

    def TestBasic_TestBasic__larky_2340700181():
        d = deque("abc")
        d.maxlen = 10

    asserts.assert_fails(lambda: _larky_2340700181(), ".*?AttributeError")


def MutatingCompare_test_count():
    for s in ("", "abracadabra", "simsalabim" * 500 + "abc"):
        s = list(s)
        d = deque(s)
        for letter in "abcdefghijklmnopqrstuvwxyz":
            asserts.assert_that(s.count(letter)).is_equal_to(d.count(letter))
    asserts.assert_fails(lambda: d.count(), ".*?TypeError")  # too few args
    asserts.assert_fails(lambda: d.count(1, 2), ".*?TypeError")  # too many args

    class BadCompare:
        def MutatingCompare_BadCompare___eq__(other):
            # PY2LARKY: pay attention to this!
            return ArithmeticError

    d = deque([1, 2, BadCompare(), 3])
    asserts.assert_fails(lambda: d.count(2), ".*?ArithmeticError")
    d = deque([1, 2, 3])
    asserts.assert_fails(lambda: d.count(BadCompare()), ".*?ArithmeticError")

    class MutatingCompare:
        def MutatingCompare_MutatingCompare___eq__(other):
            d.pop()
            return True

    m = MutatingCompare()
    d = deque([1, 2, 3, m, 4, 5])
    m.d = d
    asserts.assert_fails(lambda: d.count(3), ".*?RuntimeError")

    # test issue11004
    # block advance failed after rotation aligned elements on right side of block
    d = deque([None] * 16)
    for i in range(len(d)):
        d.rotate(-1)
    d.rotate(1)
    asserts.assert_that(d.count(1)).is_equal_to(0)
    asserts.assert_that(d.count(None)).is_equal_to(16)


def MutatingCompare_test_comparisons():
    d = deque("xabc")
    d.popleft()
    for e in [d, deque("abc"), deque("ab"), deque(), list(d)]:
        asserts.assert_that(d == e).is_equal_to(
            type(d) == type(e) and list(d) == list(e))
        asserts.assert_that(d != e).is_equal_to(
            not (type(d) == type(e) and list(d) == list(e)))

    args = map(deque, ("", "a", "b", "ab", "ba", "abc", "xba", "xabc", "cba"))
    for x in args:
        for y in args:
            asserts.assert_that(x == y).is_equal_to(list(x) == list(y))
            asserts.assert_that(x != y).is_equal_to(list(x) != list(y))
            asserts.assert_that(x < y).is_equal_to(list(x) < list(y))
            asserts.assert_that(x <= y).is_equal_to(list(x) <= list(y))
            asserts.assert_that(x > y).is_equal_to(list(x) > list(y))
            asserts.assert_that(x >= y).is_equal_to(list(x) >= list(y))


def MutatingCompare_test_contains():
    n = 200

    d = deque(range(n))
    for i in range(n):
        asserts.assert_that(i in d).is_true()
    asserts.assert_that((n + 1) not in d).is_true()

    # Test detection of mutation during iteration
    d = deque(range(n))
    d[n // 2] = MutateCmp(d, False)

    def MutatingCompare_MutatingCompare__larky_1767105571():
        n in d

    asserts.assert_fails(lambda: _larky_1767105571(), ".*?RuntimeError")

    # Test detection of comparison exceptions
    d = deque(range(n))
    d[n // 2] = BadCmp()

    def MutatingCompare_MutatingCompare__larky_1767105571():
        n in d

    asserts.assert_fails(lambda: _larky_1767105571(), ".*?RuntimeError")


def A_test_contains_count_stop_crashes():
    class A:
        def A_A___eq__(other):
            d.clear()
            return NotImplemented

    d = deque([A(), A()])

    def A_A__larky_2080401019():
        _ = 3 in d

    asserts.assert_fails(lambda: _larky_2080401019(), ".*?RuntimeError")
    d = deque([A(), A()])

    def A_A__larky_4025946405():
        _ = d.count(3)

    asserts.assert_fails(lambda: _larky_4025946405(), ".*?RuntimeError")


def A_test_extend():
    d = deque("a")
    asserts.assert_fails(lambda: d.extend(1), ".*?TypeError")
    d.extend("bcd")
    asserts.assert_that(list(d)).is_equal_to(list("abcd"))
    d.extend(d)
    asserts.assert_that(list(d)).is_equal_to(list("abcdabcd"))


def A_test_add():
    d = deque()
    e = deque("abc")
    f = deque("def")
    asserts.assert_that(d + d).is_equal_to(deque())
    asserts.assert_that(e + f).is_equal_to(deque("abcdef"))
    asserts.assert_that(e + e).is_equal_to(deque("abcabc"))
    asserts.assert_that(e + d).is_equal_to(deque("abc"))
    asserts.assert_that(d + e).is_equal_to(deque("abc"))
    asserts.assert_that(d + d).is_not_equal_to(deque())
    asserts.assert_that(e + d).is_not_equal_to(deque("abc"))
    asserts.assert_that(d + e).is_not_equal_to(deque("abc"))

    g = deque("abcdef", maxlen=4)
    h = deque("gh")
    asserts.assert_that(g + h).is_equal_to(deque("efgh"))

    def A_A__larky_2592321630():
        deque("abc") + "def"

    asserts.assert_fails(lambda: _larky_2592321630(), ".*?TypeError")


def A_test_iadd():
    d = deque("a")
    d += "bcd"
    asserts.assert_that(list(d)).is_equal_to(list("abcd"))
    d += d
    asserts.assert_that(list(d)).is_equal_to(list("abcdabcd"))


def A_test_extendleft():
    d = deque("a")
    asserts.assert_fails(lambda: d.extendleft(1), ".*?TypeError")
    d.extendleft("bcd")
    asserts.assert_that(list(d)).is_equal_to(list(reversed("abcd")))
    d.extendleft(d)
    asserts.assert_that(list(d)).is_equal_to(list("abcddcba"))
    d = deque()
    d.extendleft(range(1000))
    asserts.assert_that(list(d)).is_equal_to(list(reversed(range(1000))))
    asserts.assert_fails(lambda: d.extendleft(fail()), ".*?SyntaxError")


def A_test_getitem():
    n = 200
    d = deque(range(n))
    l = list(range(n))
    for i in range(n):
        d.popleft()
        l.pop(0)
        if random.random() < 0.5:
            d.append(i)
            l.append(i)
        for j in range(1 - len(l), len(l)):
            assert d[j] == l[j]

    d = deque("superman")
    asserts.assert_that(d[0]).is_equal_to("s")
    asserts.assert_that(d[-1]).is_equal_to("n")
    d = deque()
    asserts.assert_fails(lambda: d.__getitem__(0), ".*?IndexError")
    asserts.assert_fails(lambda: d.__getitem__(-1), ".*?IndexError")


def A_test_index():
    for n in 1, 2, 30, 40, 200:

        d = deque(range(n))
        for i in range(n):
            asserts.assert_that(d.index(i)).is_equal_to(i)

        def A_A__larky_1630639679():
            d.index(n + 1)

        asserts.assert_fails(lambda: _larky_1630639679(), ".*?ValueError")

        # Test detection of mutation during iteration
        d = deque(range(n))
        d[n // 2] = MutateCmp(d, False)

        def A_A__larky_534789762():
            d.index(n)

        asserts.assert_fails(lambda: _larky_534789762(), ".*?RuntimeError")

        # Test detection of comparison exceptions
        d = deque(range(n))
        d[n // 2] = BadCmp()

        def A_A__larky_534789762():
            d.index(n)

        asserts.assert_fails(lambda: _larky_534789762(), ".*?RuntimeError")

        # Test start and stop arguments behavior matches list.index()
    elements = "ABCDEFGHI"
    nonelement = "Z"
    d = deque(elements * 2)
    s = list(elements * 2)
    for start in range(-5 - len(s) * 2, 5 + len(s) * 2):
        for stop in range(-5 - len(s) * 2, 5 + len(s) * 2):
            for element in elements + "Z":
                try:
                    target = s.index(element, start, stop)
                except ValueError:
                    def A_A__larky_2928366533():
                        d.index(element, start, stop)

                    asserts.assert_fails(lambda: _larky_2928366533(),
                                         ".*?ValueError")
                else:
                    asserts.assert_that(
                        d.index(element, start, stop)).is_equal_to(target)

        # Test large start argument
    d = deque(range(0, 10000, 10))
    for step in range(100):
        i = d.index(8500, 700)
        asserts.assert_that(d[i]).is_equal_to(8500)
        # Repeat test with a different internal offset
        d.rotate()


def A_test_index_bug_24913():
    d = deque("A" * 3)

    def A_A__larky_1920688794():
        i = d.index("Hello world", 0, 4)

    asserts.assert_fails(lambda: _larky_1920688794(), ".*?ValueError")


def A_test_insert():
    # Test to make sure insert behaves like lists
    elements = "ABCDEFGHI"
    for i in range(-5 - len(elements) * 2, 5 + len(elements) * 2):
        d = deque("ABCDEFGHI")
        s = list("ABCDEFGHI")
        d.insert(i, "Z")
        s.insert(i, "Z")
        asserts.assert_that(list(d)).is_equal_to(s)


def A_test_insert_bug_26194():
    data = "ABC"
    d = deque(data, maxlen=len(data))

    def A_A__larky_1426357960():
        d.insert(2, None)

    asserts.assert_fails(lambda: _larky_1426357960(), ".*?IndexError")

    elements = "ABCDEFGHI"
    for i in range(-len(elements), len(elements)):
        d = deque(elements, maxlen=len(elements) + 1)
        d.insert(i, "Z")
        if i >= 0:
            asserts.assert_that(d[i]).is_equal_to("Z")
        else:
            asserts.assert_that(d[i - 1]).is_equal_to("Z")


def A_test_imul():
    for n in (-10, -1, 0, 1, 2, 10, 1000):
        d = deque()
        d *= n
        asserts.assert_that(d).is_equal_to(deque())
        asserts.assert_that(d.maxlen).is_none()

    for n in (-10, -1, 0, 1, 2, 10, 1000):
        d = deque("a")
        d *= n
        asserts.assert_that(d).is_equal_to(deque("a" * n))
        asserts.assert_that(d.maxlen).is_none()

    for n in (-10, -1, 0, 1, 2, 10, 499, 500, 501, 1000):
        d = deque("a", 500)
        d *= n
        asserts.assert_that(d).is_equal_to(deque("a" * min(n, 500)))
        asserts.assert_that(d.maxlen).is_equal_to(500)

    for n in (-10, -1, 0, 1, 2, 10, 1000):
        d = deque("abcdef")
        d *= n
        asserts.assert_that(d).is_equal_to(deque("abcdef" * n))
        asserts.assert_that(d.maxlen).is_none()

    for n in (-10, -1, 0, 1, 2, 10, 499, 500, 501, 1000):
        d = deque("abcdef", 500)
        d *= n
        asserts.assert_that(d).is_equal_to(deque(("abcdef" * n)[-500:]))
        asserts.assert_that(d.maxlen).is_equal_to(500)


def A_test_mul():
    d = deque("abc")
    asserts.assert_that(d * -5).is_equal_to(deque())
    asserts.assert_that(d * 0).is_equal_to(deque())
    asserts.assert_that(d * 1).is_equal_to(deque("abc"))
    asserts.assert_that(d * 2).is_equal_to(deque("abcabc"))
    asserts.assert_that(d * 3).is_equal_to(deque("abcabcabc"))
    asserts.assert_that(d * 1).is_not_equal_to(d)

    asserts.assert_that(deque() * 0).is_equal_to(deque())
    asserts.assert_that(deque() * 1).is_equal_to(deque())
    asserts.assert_that(deque() * 5).is_equal_to(deque())

    asserts.assert_that(-5 * d).is_equal_to(deque())
    asserts.assert_that(0 * d).is_equal_to(deque())
    asserts.assert_that(1 * d).is_equal_to(deque("abc"))
    asserts.assert_that(2 * d).is_equal_to(deque("abcabc"))
    asserts.assert_that(3 * d).is_equal_to(deque("abcabcabc"))

    d = deque("abc", maxlen=5)
    asserts.assert_that(d * -5).is_equal_to(deque())
    asserts.assert_that(d * 0).is_equal_to(deque())
    asserts.assert_that(d * 1).is_equal_to(deque("abc"))
    asserts.assert_that(d * 2).is_equal_to(deque("bcabc"))
    asserts.assert_that(d * 30).is_equal_to(deque("bcabc"))


def A_test_setitem():
    n = 200
    d = deque(range(n))
    for i in range(n):
        d[i] = 10 * i
    asserts.assert_that(list(d)).is_equal_to([10 * i for i in range(n)])
    l = list(d)
    for i in range(1 - n, 0, -1):
        d[i] = 7 * i
        l[i] = 7 * i
    asserts.assert_that(list(d)).is_equal_to(l)


def A_test_delitem():
    n = 500  # O(n**2) test, don't make this too big
    d = deque(range(n))
    asserts.assert_fails(lambda: d.__delitem__(-n - 1), ".*?IndexError")
    asserts.assert_fails(lambda: d.__delitem__(n), ".*?IndexError")
    for i in range(n):
        asserts.assert_that(len(d)).is_equal_to(n - i)
        j = random.randrange(-len(d), len(d))
        val = d[j]
        asserts.assert_that(val).is_in(d)
        operator.delitem(d, j)
        asserts.assert_that(val).is_not_in(d)
    asserts.assert_that(len(d)).is_equal_to(0)


def A_test_reverse():
    n = 500  # O(n**2) test, don't make this too big
    data = [random.random() for i in range(n)]
    for i in range(n):
        d = deque(data[:i])
        r = d.reverse()
        asserts.assert_that(list(d)).is_equal_to(list(reversed(data[:i])))
        asserts.assert_that(r).is_equal_to(None)
        d.reverse()
        asserts.assert_that(list(d)).is_equal_to(data[:i])
    asserts.assert_fails(lambda: d.reverse(1), ".*?TypeError")  # Arity is zero


def A_test_rotate():
    s = tuple("abcde")
    n = len(s)

    d = deque(s)
    d.rotate(1)  # verify rot(1)
    asserts.assert_that("".join(d)).is_equal_to("eabcd")

    d = deque(s)
    d.rotate(-1)  # verify rot(-1)
    asserts.assert_that("".join(d)).is_equal_to("bcdea")
    d.rotate()  # check default to 1
    asserts.assert_that(tuple(d)).is_equal_to(s)

    for i in range(n * 3):
        d = deque(s)
        e = deque(d)
        d.rotate(i)  # check vs. rot(1) n times
        for j in range(i):
            e.rotate(1)
        asserts.assert_that(tuple(d)).is_equal_to(tuple(e))
        d.rotate(-i)  # check that it works in reverse
        asserts.assert_that(tuple(d)).is_equal_to(s)
        e.rotate(n - i)  # check that it wraps forward
        asserts.assert_that(tuple(e)).is_equal_to(s)

    for i in range(n * 3):
        d = deque(s)
        e = deque(d)
        d.rotate(-i)
        for j in range(i):
            e.rotate(-1)  # check vs. rot(-1) n times
        asserts.assert_that(tuple(d)).is_equal_to(tuple(e))
        d.rotate(i)  # check that it works in reverse
        asserts.assert_that(tuple(d)).is_equal_to(s)
        e.rotate(i - n)  # check that it wraps backaround
        asserts.assert_that(tuple(e)).is_equal_to(s)

    d = deque(s)
    e = deque(s)
    e.rotate(BIG + 17)  # verify on long series of rotates
    dr = d.rotate
    for i in range(BIG + 17):
        dr()
    asserts.assert_that(tuple(d)).is_equal_to(tuple(e))

    asserts.assert_fails(lambda: d.rotate("x"),
                         ".*?TypeError")  # Wrong arg type
    asserts.assert_fails(lambda: d.rotate(1, 10),
                         ".*?TypeError")  # Too many args

    d = deque()
    d.rotate()  # rotate an empty deque
    asserts.assert_that(d).is_equal_to(deque())


def A_test_len():
    d = deque("ab")
    asserts.assert_that(len(d)).is_equal_to(2)
    d.popleft()
    asserts.assert_that(len(d)).is_equal_to(1)
    d.pop()
    asserts.assert_that(len(d)).is_equal_to(0)
    asserts.assert_fails(lambda: d.pop(), ".*?IndexError")
    asserts.assert_that(len(d)).is_equal_to(0)
    d.append("c")
    asserts.assert_that(len(d)).is_equal_to(1)
    d.appendleft("d")
    asserts.assert_that(len(d)).is_equal_to(2)
    d.clear()
    asserts.assert_that(len(d)).is_equal_to(0)


def A_test_underflow():
    d = deque()
    asserts.assert_fails(lambda: d.pop(), ".*?IndexError")
    asserts.assert_fails(lambda: d.popleft(), ".*?IndexError")


def A_test_clear():
    d = deque(range(100))
    asserts.assert_that(len(d)).is_equal_to(100)
    d.clear()
    asserts.assert_that(len(d)).is_equal_to(0)
    asserts.assert_that(list(d)).is_equal_to([])
    d.clear()  # clear an empty deque
    asserts.assert_that(list(d)).is_equal_to([])


def A_test_remove():
    d = deque("abcdefghcij")
    d.remove("c")
    asserts.assert_that(d).is_equal_to(deque("abdefghcij"))
    d.remove("c")
    asserts.assert_that(d).is_equal_to(deque("abdefghij"))
    asserts.assert_fails(lambda: d.remove("c"), ".*?ValueError")
    asserts.assert_that(d).is_equal_to(deque("abdefghij"))

    # Handle comparison errors
    d = deque(["a", "b", BadCmp(), "c"])
    e = deque(d)
    asserts.assert_fails(lambda: d.remove("c"), ".*?RuntimeError")
    for x, y in zip(d, e):
        # verify that original order and values are retained.
        asserts.assert_that(x == y).is_true()

    # Handle evil mutator
    for match in (True, False):
        d = deque(["ab"])
        d.extend([MutateCmp(d, match), "c"])
        asserts.assert_fails(lambda: d.remove("c"), ".*?IndexError")
        asserts.assert_that(d).is_equal_to(deque())


def A_test_repr():
    d = deque(range(200))
    e = eval(repr(d))
    asserts.assert_that(list(d)).is_equal_to(list(e))
    d.append(d)
    asserts.assert_that(repr(d)[-20:]).is_equal_to("7, 198, 199, [...]])")


def A_test_init():
    asserts.assert_fails(lambda: deque("abc", 2, 3), ".*?TypeError")
    asserts.assert_fails(lambda: deque(1), ".*?TypeError")


def A_test_hash():
    asserts.assert_fails(lambda: hash(deque("abc")), ".*?TypeError")


def A_test_long_steadystate_queue_popleft():
    for size in (0, 1, 2, 100, 1000):
        d = deque(range(size))
        append, pop = d.append, d.popleft
        for i in range(size, BIG):
            append(i)
            x = pop()
            if x != i - size:
                asserts.assert_that(x).is_equal_to(i - size)
        asserts.assert_that(list(d)).is_equal_to(list(range(BIG - size, BIG)))


def A_test_long_steadystate_queue_popright():
    for size in (0, 1, 2, 100, 1000):
        d = deque(reversed(range(size)))
        append, pop = d.appendleft, d.pop
        for i in range(size, BIG):
            append(i)
            x = pop()
            if x != i - size:
                asserts.assert_that(x).is_equal_to(i - size)
        asserts.assert_that(list(reversed(list(d)))).is_equal_to(
            list(range(BIG - size, BIG)))


def A_test_big_queue_popleft():
    pass
    d = deque()
    append, pop = d.append, d.popleft
    for i in range(BIG):
        append(i)
    for i in range(BIG):
        x = pop()
        if x != i:
            asserts.assert_that(x).is_equal_to(i)


def A_test_big_queue_popright():
    d = deque()
    append, pop = d.appendleft, d.pop
    for i in range(BIG):
        append(i)
    for i in range(BIG):
        x = pop()
        if x != i:
            asserts.assert_that(x).is_equal_to(i)


def A_test_big_stack_right():
    d = deque()
    append, pop = d.append, d.pop
    for i in range(BIG):
        append(i)
    for i in reversed(range(BIG)):
        x = pop()
        if x != i:
            asserts.assert_that(x).is_equal_to(i)
    asserts.assert_that(len(d)).is_equal_to(0)


def A_test_big_stack_left():
    d = deque()
    append, pop = d.appendleft, d.popleft
    for i in range(BIG):
        append(i)
    for i in reversed(range(BIG)):
        x = pop()
        if x != i:
            asserts.assert_that(x).is_equal_to(i)
    asserts.assert_that(len(d)).is_equal_to(0)


def A_test_roundtrip_iter_init():
    d = deque(range(200))
    e = deque(d)
    asserts.assert_that(id(d)).is_not_equal_to(id(e))
    asserts.assert_that(list(d)).is_equal_to(list(e))


def A_test_pickle():
    for d in deque(range(200)), deque(range(200), 100):
        for i in range(pickle.HIGHEST_PROTOCOL + 1):
            s = pickle.dumps(d, i)
            e = pickle.loads(s)
            asserts.assert_that(id(e)).is_not_equal_to(id(d))
            asserts.assert_that(list(e)).is_equal_to(list(d))
            asserts.assert_that(e.maxlen).is_equal_to(d.maxlen)


def A_test_pickle_recursive():
    for d in deque("abc"), deque("abc", 3):
        d.append(d)
        for i in range(pickle.HIGHEST_PROTOCOL + 1):
            e = pickle.loads(pickle.dumps(d, i))
            asserts.assert_that(id(e)).is_not_equal_to(id(d))
            asserts.assert_that(id(e[-1])).is_equal_to(id(e))
            asserts.assert_that(e.maxlen).is_equal_to(d.maxlen)


def A_test_iterator_pickle():
    orig = deque(range(200))
    data = [i * 1.01 for i in orig]
    for proto in range(pickle.HIGHEST_PROTOCOL + 1):
        # initial iterator
        itorg = iter(orig)
        dump = pickle.dumps((itorg, orig), proto)
        it, d = pickle.loads(dump)
        for i, x in enumerate(data):
            d[i] = x
        asserts.assert_that(type(it)).is_equal_to(type(itorg))
        asserts.assert_that(list(it)).is_equal_to(data)

        # running iterator
        next(itorg)
        dump = pickle.dumps((itorg, orig), proto)
        it, d = pickle.loads(dump)
        for i, x in enumerate(data):
            d[i] = x
        asserts.assert_that(type(it)).is_equal_to(type(itorg))
        asserts.assert_that(list(it)).is_equal_to(data[1:])

        # empty iterator
        for i in range(1, len(data)):
            next(itorg)
        dump = pickle.dumps((itorg, orig), proto)
        it, d = pickle.loads(dump)
        for i, x in enumerate(data):
            d[i] = x
        asserts.assert_that(type(it)).is_equal_to(type(itorg))
        asserts.assert_that(list(it)).is_equal_to([])

        # exhausted iterator
        asserts.assert_fails(lambda: next(itorg), ".*?StopIteration")
        dump = pickle.dumps((itorg, orig), proto)
        it, d = pickle.loads(dump)
        for i, x in enumerate(data):
            d[i] = x
        asserts.assert_that(type(it)).is_equal_to(type(itorg))
        asserts.assert_that(list(it)).is_equal_to([])


def A_test_deepcopy():
    mut = [10]
    d = deque([mut])
    e = copy.deepcopy(d)
    asserts.assert_that(list(d)).is_equal_to(list(e))
    mut[0] = 11
    asserts.assert_that(id(d)).is_not_equal_to(id(e))
    asserts.assert_that(list(d)).is_not_equal_to(list(e))


def A_test_copy():
    mut = [10]
    d = deque([mut])
    e = copy.copy(d)
    asserts.assert_that(list(d)).is_equal_to(list(e))
    mut[0] = 11
    asserts.assert_that(id(d)).is_not_equal_to(id(e))
    asserts.assert_that(list(d)).is_equal_to(list(e))

    for i in range(5):
        for maxlen in range(-1, 6):
            s = [random.random() for j in range(i)]
            d = deque(s) if maxlen == -1 else deque(s, maxlen)
            e = d.copy()
            asserts.assert_that(d).is_equal_to(e)
            asserts.assert_that(d.maxlen).is_equal_to(e.maxlen)
            asserts.assert_that(all([x == y for x, y in zip(d, e)])).is_true()


def A_test_copy_method():
    mut = [10]
    d = deque([mut])
    e = d.copy()
    asserts.assert_that(list(d)).is_equal_to(list(e))
    mut[0] = 11
    asserts.assert_that(id(d)).is_not_equal_to(id(e))
    asserts.assert_that(list(d)).is_equal_to(list(e))


def A_test_reversed():
    for s in ("abcd", range(2000)):
        asserts.assert_that(list(reversed(deque(s)))).is_equal_to(
            list(reversed(s)))


def A_test_reversed_new():
    klass = type(reversed(deque()))
    for s in ("abcd", range(2000)):
        asserts.assert_that(list(klass(deque(s)))).is_equal_to(
            list(reversed(s)))


def A_test_gc_doesnt_blowup():
    load("@stdlib//gc", gc="gc")

    # This used to assert-fail in deque_traverse() under a debug
    # build, or run wild with a NULL pointer in a release build.
    d = deque()
    for i in range(100):
        d.append(1)
        gc.collect()


def C_test_container_iterator():
    # Bug #3680: tp_traverse was not implemented for deque iterator objects
    class C(object):
        pass

    for i in range(2):
        obj = C()
        ref = weakref.ref(obj)
        if i == 0:
            container = deque([obj, 1])
        else:
            container = reversed(deque([obj, 1]))
        obj.x = iter(container)
        del obj, container
        gc.collect()
        asserts.assert_that(ref() == None).is_true()


check_sizeof = support.check_sizeof


def C_test_sizeof():
    BLOCKLEN = 64
    basesize = support.calcvobjsize("2P4nP")
    blocksize = struct.calcsize("P%dPP" % BLOCKLEN)
    asserts.assert_that(object.__sizeof__(deque())).is_equal_to(basesize)
    check = check_sizeof
    check(deque(), basesize + blocksize)
    check(deque("a"), basesize + blocksize)
    check(deque("a" * (BLOCKLEN - 1)), basesize + blocksize)
    check(deque("a" * BLOCKLEN), basesize + 2 * blocksize)
    check(deque("a" * (42 * BLOCKLEN)), basesize + 43 * blocksize)


test_sizeof = support.cpython_only(test_sizeof)


def TestVariousIteratorArgs_test_constructor():
    for s in ("123", "", range(1000), ("do", 1.2), range(2000, 2200, 5)):
        for g in (
                seq_tests.Sequence,
                seq_tests.IterFunc,
                seq_tests.IterGen,
                seq_tests.IterFuncStop,
                seq_tests.itermulti,
                seq_tests.iterfunc,
        ):
            asserts.assert_that(list(deque(g(s)))).is_equal_to(list(g(s)))
        asserts.assert_fails(lambda: deque(seq_tests.IterNextOnly(s)),
                             ".*?TypeError")
        asserts.assert_fails(lambda: deque(seq_tests.IterNoNext(s)),
                             ".*?TypeError")
        asserts.assert_fails(lambda: deque(seq_tests.IterGenExc(s)),
                             ".*?ZeroDivisionError")


def TestVariousIteratorArgs_test_iter_with_altered_data():
    d = deque("abcdefg")
    it = iter(d)
    d.pop()
    asserts.assert_fails(lambda: next(it), ".*?RuntimeError")


def TestVariousIteratorArgs_test_runtime_error_on_empty_deque():
    d = deque()
    it = iter(d)
    d.append(10)
    asserts.assert_fails(lambda: next(it), ".*?RuntimeError")


def Deque():
    pass
    return self


def DequeWithBadIter():
    def DequeWithBadIter___iter__():
        # PY2LARKY: pay attention to this!
        return TypeError

    self.DequeWithBadIter___iter__ = DequeWithBadIter___iter__
    return self


def TestSubclass_test_basics():
    d = Deque(range(25))
    d.__init__(range(200))
    for i in range(200, 400):
        d.append(i)
    for i in reversed(range(-200, 0)):
        d.appendleft(i)
    asserts.assert_that(list(d)).is_equal_to(list(range(-200, 400)))
    asserts.assert_that(len(d)).is_equal_to(600)

    left = [d.popleft() for i in range(250)]
    asserts.assert_that(left).is_equal_to(list(range(-200, 50)))
    asserts.assert_that(list(d)).is_equal_to(list(range(50, 400)))

    right = [d.pop() for i in range(250)]
    right.reverse()
    asserts.assert_that(right).is_equal_to(list(range(150, 400)))
    asserts.assert_that(list(d)).is_equal_to(list(range(50, 150)))

    d.clear()
    asserts.assert_that(len(d)).is_equal_to(0)


def TestSubclass_test_copy_pickle():
    d = Deque("abc")

    e = d.__copy__()
    asserts.assert_that(type(d)).is_equal_to(type(e))
    asserts.assert_that(list(d)).is_equal_to(list(e))

    e = Deque(d)
    asserts.assert_that(type(d)).is_equal_to(type(e))
    asserts.assert_that(list(d)).is_equal_to(list(e))

    for proto in range(pickle.HIGHEST_PROTOCOL + 1):
        s = pickle.dumps(d, proto)
        e = pickle.loads(s)
        asserts.assert_that(id(d)).is_not_equal_to(id(e))
        asserts.assert_that(type(d)).is_equal_to(type(e))
        asserts.assert_that(list(d)).is_equal_to(list(e))

    d = Deque("abcde", maxlen=4)

    e = d.__copy__()
    asserts.assert_that(type(d)).is_equal_to(type(e))
    asserts.assert_that(list(d)).is_equal_to(list(e))

    e = Deque(d)
    asserts.assert_that(type(d)).is_equal_to(type(e))
    asserts.assert_that(list(d)).is_equal_to(list(e))

    for proto in range(pickle.HIGHEST_PROTOCOL + 1):
        s = pickle.dumps(d, proto)
        e = pickle.loads(s)
        asserts.assert_that(id(d)).is_not_equal_to(id(e))
        asserts.assert_that(type(d)).is_equal_to(type(e))
        asserts.assert_that(list(d)).is_equal_to(list(e))


def TestSubclass_test_pickle_recursive():
    for proto in range(pickle.HIGHEST_PROTOCOL + 1):
        for d in Deque("abc"), Deque("abc", 3):
            d.append(d)

            e = pickle.loads(pickle.dumps(d, proto))
            asserts.assert_that(id(e)).is_not_equal_to(id(d))
            asserts.assert_that(type(e)).is_equal_to(type(d))
            asserts.assert_that(e.maxlen).is_equal_to(d.maxlen)
            dd = d.pop()
            ee = e.pop()
            asserts.assert_that(id(ee)).is_equal_to(id(e))
            asserts.assert_that(e).is_equal_to(d)

            d.x = d
            e = pickle.loads(pickle.dumps(d, proto))
            asserts.assert_that(id(e.x)).is_equal_to(id(e))

        for d in DequeWithBadIter("abc"), DequeWithBadIter("abc", 2):
            asserts.assert_fails(lambda: pickle.dumps(d, proto), ".*?TypeError")


def TestSubclass_test_weakref():
    d = deque("gallahad")
    p = weakref.proxy(d)
    asserts.assert_that(str(p)).is_equal_to(str(d))
    d = None
    asserts.assert_fails(lambda: str(p), ".*?ReferenceError")


def X_test_strange_subclass():
    class X(deque):
        def X_X___iter__():
            return iter([])

    d1 = X([1, 2, 3])
    d2 = X([4, 5, 6])
    d1 == d2  # not clear if this is supposed to be True or False,
    # but it used to give a SystemError


def X_test_bug_31608():
    # The interpreter used to crash in specific cases where a deque
    # subclass returned a non-deque.
    class X(deque):
        pass

    d = X()

    def X_X_bad___new__(cls, *args, **kwargs):
        return [42]

    bad___new__ = bad___new__

    X.__new__ = bad___new__

    def X_X__larky_3899975758():
        d * 42  # shouldn't crash

    asserts.assert_fails(lambda: _larky_3899975758(), ".*?TypeError")

    def X_X__larky_1120355827():
        d + deque([1, 2, 3])  # shouldn't crash

    asserts.assert_fails(lambda: _larky_1120355827(), ".*?TypeError")


test_bug_31608 = support.cpython_only(test_bug_31608)


def SubclassWithKwargs():
    def SubclassWithKwargs___init__(newarg=1):
        deque.__init__(self)

    self.SubclassWithKwargs___init__ = SubclassWithKwargs___init__
    return self


def TestSubclassWithKwargs_test_subclass_with_kwargs():
    # SF bug #1486663 -- this used to erroneously raise a TypeError
    SubclassWithKwargs(newarg=1)


def TestSequence():
    type2test = deque

    def TestSequence_test_getitem():
        # For now, bypass tests that require slicing
        pass

    self.TestSequence_test_getitem = TestSequence_test_getitem

    def TestSequence_test_getslice():
        # For now, bypass tests that require slicing
        pass

    self.TestSequence_test_getslice = TestSequence_test_getslice

    def TestSequence_test_subscript():
        # For now, bypass tests that require slicing
        pass

    self.TestSequence_test_subscript = TestSequence_test_subscript

    def TestSequence_test_free_after_iterating():
        # For now, bypass tests that require slicing
        skipTest("Exhausted deque iterator doesn't free a deque")

    self.TestSequence_test_free_after_iterating = TestSequence_test_free_after_iterating
    return self


# ==============================================================================

libreftest = """
Example from the Library Reference:  Doc/lib/libcollections.tex

>>> from collections import deque
>>> d = deque('ghi')                 # make a new deque with three items
>>> for elem in d:                   # iterate over the deque's elements
...     print(elem.upper())
G
H
I
>>> d.append('j')                    # add a new entry to the right side
>>> d.appendleft('f')                # add a new entry to the left side
>>> d                                # show the representation of the deque
deque(['f', 'g', 'h', 'i', 'j'])
>>> d.pop()                          # return and remove the rightmost item
'j'
>>> d.popleft()                      # return and remove the leftmost item
'f'
>>> list(d)                          # list the contents of the deque
['g', 'h', 'i']
>>> d[0]                             # peek at leftmost item
'g'
>>> d[-1]                            # peek at rightmost item
'i'
>>> list(reversed(d))                # list the contents of a deque in reverse
['i', 'h', 'g']
>>> 'h' in d                         # search the deque
True
>>> d.extend('jkl')                  # add multiple elements at once
>>> d
deque(['g', 'h', 'i', 'j', 'k', 'l'])
>>> d.rotate(1)                      # right rotation
>>> d
deque(['l', 'g', 'h', 'i', 'j', 'k'])
>>> d.rotate(-1)                     # left rotation
>>> d
deque(['g', 'h', 'i', 'j', 'k', 'l'])
>>> deque(reversed(d))               # make a new deque in reverse order
deque(['l', 'k', 'j', 'i', 'h', 'g'])
>>> d.clear()                        # empty the deque
>>> d.pop()                          # cannot pop from an empty deque
Traceback (most recent call last):
  File "<pyshell#6>", line 1, in -toplevel-
    d.pop()
IndexError: pop from an empty deque

>>> d.extendleft('abc')              # extendleft() reverses the input order
>>> d
deque(['c', 'b', 'a'])



>>> def delete_nth(d, n):
...     d.rotate(-n)
...     d.popleft()
...     d.rotate(n)
...
>>> d = deque('abcdef')
>>> delete_nth(d, 2)   # remove the entry at d[2]
>>> d
deque(['a', 'b', 'd', 'e', 'f'])



>>> def roundrobin(*iterables):
...     pending = deque(iter(i) for i in iterables)
...     while pending:
...         task = pending.popleft()
...         try:
...             yield next(task)
...         except StopIteration:
...             continue
...         pending.append(task)
...

>>> for value in roundrobin('abc', 'd', 'efgh'):
...     print(value)
...
a
d
e
b
f
c
g
h


>>> def maketree(iterable):
...     d = deque(iterable)
...     while len(d) > 1:
...         pair = [d.popleft(), d.popleft()]
...         d.append(pair)
...     return list(d)
...
>>> print(maketree('abcdefgh'))
[[[['a', 'b'], ['c', 'd']], [['e', 'f'], ['g', 'h']]]]

"""

# ==============================================================================


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(TestBasic_test_basics))
    _suite.addTest(unittest.FunctionTestCase(TestBasic_test_maxlen))
    _suite.addTest(unittest.FunctionTestCase(TestBasic_test_maxlen_zero))
    _suite.addTest(unittest.FunctionTestCase(TestBasic_test_maxlen_attribute))
    _suite.addTest(unittest.FunctionTestCase(MutatingCompare_test_count))
    _suite.addTest(unittest.FunctionTestCase(MutatingCompare_test_comparisons))
    _suite.addTest(unittest.FunctionTestCase(MutatingCompare_test_contains))
    _suite.addTest(
        unittest.FunctionTestCase(A_test_contains_count_stop_crashes))
    _suite.addTest(unittest.FunctionTestCase(A_test_extend))
    _suite.addTest(unittest.FunctionTestCase(A_test_add))
    _suite.addTest(unittest.FunctionTestCase(A_test_iadd))
    _suite.addTest(unittest.FunctionTestCase(A_test_extendleft))
    _suite.addTest(unittest.FunctionTestCase(A_test_getitem))
    _suite.addTest(unittest.FunctionTestCase(A_test_index))
    _suite.addTest(unittest.FunctionTestCase(A_test_index_bug_24913))
    _suite.addTest(unittest.FunctionTestCase(A_test_insert))
    _suite.addTest(unittest.FunctionTestCase(A_test_insert_bug_26194))
    _suite.addTest(unittest.FunctionTestCase(A_test_imul))
    _suite.addTest(unittest.FunctionTestCase(A_test_mul))
    _suite.addTest(unittest.FunctionTestCase(A_test_setitem))
    _suite.addTest(unittest.FunctionTestCase(A_test_delitem))
    _suite.addTest(unittest.FunctionTestCase(A_test_reverse))
    _suite.addTest(unittest.FunctionTestCase(A_test_rotate))
    _suite.addTest(unittest.FunctionTestCase(A_test_len))
    _suite.addTest(unittest.FunctionTestCase(A_test_underflow))
    _suite.addTest(unittest.FunctionTestCase(A_test_clear))
    _suite.addTest(unittest.FunctionTestCase(A_test_remove))
    _suite.addTest(unittest.FunctionTestCase(A_test_repr))
    _suite.addTest(unittest.FunctionTestCase(A_test_init))
    _suite.addTest(unittest.FunctionTestCase(A_test_hash))
    _suite.addTest(
        unittest.FunctionTestCase(A_test_long_steadystate_queue_popleft))
    _suite.addTest(
        unittest.FunctionTestCase(A_test_long_steadystate_queue_popright))
    _suite.addTest(unittest.FunctionTestCase(A_test_big_queue_popleft))
    _suite.addTest(unittest.FunctionTestCase(A_test_big_queue_popright))
    _suite.addTest(unittest.FunctionTestCase(A_test_big_stack_right))
    _suite.addTest(unittest.FunctionTestCase(A_test_big_stack_left))
    _suite.addTest(unittest.FunctionTestCase(A_test_roundtrip_iter_init))
    _suite.addTest(unittest.FunctionTestCase(A_test_pickle))
    _suite.addTest(unittest.FunctionTestCase(A_test_pickle_recursive))
    _suite.addTest(unittest.FunctionTestCase(A_test_iterator_pickle))
    _suite.addTest(unittest.FunctionTestCase(A_test_deepcopy))
    _suite.addTest(unittest.FunctionTestCase(A_test_copy))
    _suite.addTest(unittest.FunctionTestCase(A_test_copy_method))
    _suite.addTest(unittest.FunctionTestCase(A_test_reversed))
    _suite.addTest(unittest.FunctionTestCase(A_test_reversed_new))
    _suite.addTest(unittest.FunctionTestCase(A_test_gc_doesnt_blowup))
    _suite.addTest(unittest.FunctionTestCase(C_test_container_iterator))
    _suite.addTest(unittest.FunctionTestCase(C_test_sizeof))
    _suite.addTest(
        unittest.FunctionTestCase(TestVariousIteratorArgs_test_constructor))
    _suite.addTest(unittest.FunctionTestCase(
        TestVariousIteratorArgs_test_iter_with_altered_data))
    _suite.addTest(unittest.FunctionTestCase(
        TestVariousIteratorArgs_test_runtime_error_on_empty_deque))
    _suite.addTest(unittest.FunctionTestCase(TestSubclass_test_basics))
    _suite.addTest(unittest.FunctionTestCase(TestSubclass_test_copy_pickle))
    _suite.addTest(
        unittest.FunctionTestCase(TestSubclass_test_pickle_recursive))
    _suite.addTest(unittest.FunctionTestCase(TestSubclass_test_weakref))
    _suite.addTest(unittest.FunctionTestCase(X_test_strange_subclass))
    _suite.addTest(unittest.FunctionTestCase(X_test_bug_31608))
    _suite.addTest(unittest.FunctionTestCase(
        TestSubclassWithKwargs_test_subclass_with_kwargs))
    _suite.addTest(unittest.FunctionTestCase(TestSequence_test_main))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
