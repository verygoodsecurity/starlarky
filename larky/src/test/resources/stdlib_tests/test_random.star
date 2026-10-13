load("@stdlib//random", random="random")
load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@stdlib//larky", "larky")

# randrange tests

def test_randrange_basic(stop):
    for _ in range(20):
        x = random.randrange(stop)
        asserts.assert_true(0 <= x and x < stop, "randrange(stop) out of range: {}".format(x))


def test_randrange_start_stop(start, stop):
    for _ in range(20):
        y = random.randrange(start, stop)
        asserts.assert_true(start <= y and y < stop, "randrange(start,stop) out of range: {}".format(y))


def test_randrange_step(start, stop, step):
    for _ in range(20):
        z = random.randrange(start, stop, step)
        asserts.assert_true(z % step == 0, "randrange({}, {}, {}) returned invalid: {}".format(start, stop, step, z))
        asserts.assert_true(start <= z and z < stop, "randrange({}, {}, {}) out of range: {}".format(start, stop, step, z))


def test_randrange_negative_step():
    valid = [10, 8, 6, 4, 2]
    for _ in range(20):
        v = random.randrange(10, 0, -2)
        asserts.assert_true(v in valid, "randrange(10,0,-2) invalid: {}".format(v))


def test_randrange_single_value():
    for _ in range(10):
        asserts.assert_that(random.randrange(5, 6)).is_equal_to(5)


def test_randrange_all_values():
    # Every element of the range is reachable (randrange(0, 10, 2) used to
    # always return 0). Each check fails with probability < 5 * (4/5)**500.
    seen = {}
    for _ in range(500):
        seen[random.randrange(0, 10, 2)] = True
    asserts.assert_that(sorted(seen.keys())).is_equal_to([0, 2, 4, 6, 8])
    seen = {}
    for _ in range(500):
        seen[random.randrange(10, 0, -3)] = True
    asserts.assert_that(sorted(seen.keys())).is_equal_to([1, 4, 7, 10])
    seen = {}
    for _ in range(500):
        seen[random.randrange(-3, 2)] = True
    asserts.assert_that(sorted(seen.keys())).is_equal_to([-3, -2, -1, 0, 1])


def test_randrange_keywords():
    asserts.assert_that(random.randrange(start=3, stop=4)).is_equal_to(3)
    v = random.randrange(7, None)
    asserts.assert_true(0 <= v and v < 7, "randrange(7, None) out of range: {}".format(v))
    asserts.assert_that(random.randrange(1, None, 1)).is_equal_to(0)


def test_randrange_big_ints():
    big = 1 << 100
    asserts.assert_that(random.randrange(big, big + 1)).is_equal_to(big)
    for _ in range(20):
        v = random.randrange(big)
        asserts.assert_true(0 <= v and v < big, "randrange(2**100) out of range: {}".format(v))
        w = random.randrange(-big, big, 1 << 98)
        asserts.assert_true(w in [-big, -3 * (1 << 98), -(1 << 99), -(1 << 98), 0, 1 << 98, 1 << 99, 3 * (1 << 98)],
                            "randrange(-2**100, 2**100, 2**98) invalid: {}".format(w))
    asserts.assert_that(random.randint(big, big)).is_equal_to(big)


def test_randrange_errors():
    asserts.assert_fails(lambda: random.randrange(0, 10, 0), "ValueError: zero step for randrange\\(\\)")
    asserts.assert_fails(lambda: random.randrange(1, 1), "ValueError: empty range in randrange\\(1, 1\\)")
    asserts.assert_fails(lambda: random.randrange(10, 0, 2), "ValueError: empty range in randrange\\(10, 0, 2\\)")
    asserts.assert_fails(lambda: random.randrange(0, 10, -1), "ValueError: empty range in randrange\\(0, 10, -1\\)")
    asserts.assert_fails(lambda: random.randrange(0), "ValueError: empty range for randrange\\(\\)")
    asserts.assert_fails(lambda: random.randrange(-5), "ValueError: empty range for randrange\\(\\)")
    asserts.assert_fails(lambda: random.randrange(10, step=2), "TypeError: Missing a non-None stop argument")
    asserts.assert_fails(lambda: random.randrange(1 << 70, 1 << 70), "ValueError: empty range in randrange\\(1180591620717411303424, 1180591620717411303424\\)")
    asserts.assert_fails(lambda: random.randrange(1.0), "parameter 'start' got value of type 'float', want 'int'")
    asserts.assert_fails(lambda: random.randrange(0, "a"), "parameter 'stop' got value of type 'string', want 'int or NoneType'")


# randint tests

def test_randint_basic(a, b):
    for _ in range(20):
        v = random.randint(a, b)
        asserts.assert_true(a <= v and v <= b, "randint out of range: {}".format(v))

def test_randint_single_value():
    asserts.assert_that(random.randint(5, 5)).is_equal_to(5)


def test_randint_errors():
    asserts.assert_fails(lambda: random.randint(10, 5), "ValueError: empty range in randrange\\(10, 6\\)")
    asserts.assert_fails(lambda: random.randint(1.0, 2), "parameter 'min' got value of type 'float', want 'int'")


# choice tests

def test_choice_basic():
    seq = [1, 2, 3]
    for _ in range(20):
        c = random.choice(seq)
        asserts.assert_true(c in seq, "choice returned invalid: {}".format(c))


def test_choice_empty_fails():
    asserts.assert_fails(lambda: random.choice([]), "IndexError: Cannot choose from an empty sequence")


# sample tests

def test_sample_basic(n):
    s = random.sample([1, 2, 3, 4], n)
    asserts.assert_that(len(s)).is_equal_to(n)
    for v in s:
        asserts.assert_true(v in [1,2,3,4], "sample invalid: {}".format(v))


def test_sample_zero():
    s = random.sample([1,2,3], 0)
    asserts.assert_that(len(s)).is_equal_to(0)


def test_sample_all():
    s = random.sample([1, 2, 3], 3)
    asserts.assert_that(sorted(s)).is_equal_to([1, 2, 3])


def test_sample_errors():
    msg = "ValueError: Sample larger than population or is negative"
    asserts.assert_fails(lambda: random.sample([1, 2, 3], 5), msg)
    asserts.assert_fails(lambda: random.sample([1, 2], -1), msg)
    asserts.assert_fails(lambda: random.sample([1, 2], 1 << 70), msg)
    asserts.assert_fails(lambda: random.sample([], 1), msg)


# shuffle tests

def test_shuffle_basic():
    original = [1,2,3,4]
    shuffled = random.shuffle(original)

    # original list must NOT change
    asserts.assert_that(original).is_equal_to([1,2,3,4])

    # shuffled must contain the same elements
    asserts.assert_that(sorted(shuffled)).is_equal_to([1,2,3,4])


# getrandbits tests

def test_getrandbits_basic(bits):
    v = random.getrandbits(bits)
    asserts.assert_true(0 <= v and v < (1 << bits), "getrandbits({}) invalid: {}".format(bits, v))

def test_getrandbits_zero():
    asserts.assert_that(random.getrandbits(0)).is_equal_to(0)


def test_getrandbits_one_is_uniform():
    # getrandbits(1) used to retry until it drew a non-zero value, so it never
    # returned 0. Over 1000 draws each value is missing with probability 2**-1000.
    counts = [0, 0]
    for _ in range(1000):
        counts[random.getrandbits(1)] += 1
    asserts.assert_true(counts[0] > 0 and counts[1] > 0, "getrandbits(1) counts: {}".format(counts))


def test_getrandbits_big():
    for _ in range(20):
        v = random.getrandbits(100)
        asserts.assert_true(0 <= v and v < (1 << 100), "getrandbits(100) invalid: {}".format(v))


def test_getrandbits_errors():
    asserts.assert_fails(lambda: random.getrandbits(-1), "ValueError: number of bits must be non-negative")
    asserts.assert_fails(lambda: random.getrandbits(-(1 << 70)), "ValueError: number of bits must be non-negative")
    asserts.assert_fails(lambda: random.getrandbits(1 << 40), "got 1099511627776 for k, want value in signed 32-bit range")


# randbytes tests

def test_randbytes_basic(n):
    b = random.randbytes(n)
    asserts.assert_that(len(b)).is_equal_to(n)
    asserts.assert_that(b).is_instance_of(bytes)


def test_randbytes_errors():
    asserts.assert_fails(lambda: random.randbytes(-1), "ValueError: number of bits must be non-negative")
    asserts.assert_fails(lambda: random.randbytes(1 << 40), "got 1099511627776 for n, want value in signed 32-bit range")


# urandom tests

def test_urandom_basic(n):
    b = random.urandom(n)
    asserts.assert_that(len(b)).is_equal_to(n)


def test_urandom_errors():
    asserts.assert_fails(lambda: random.urandom(-1), "ValueError: negative argument not allowed")
    asserts.assert_fails(lambda: random.urandom(1 << 40), "got 1099511627776 for n, want value in signed 32-bit range")


# Suite

def suite():
    s = unittest.TestSuite()

    # randrange positive
    larky.parametrize(s.addTest, unittest.FunctionTestCase, "stop", [5])(test_randrange_basic)
    larky.parametrize(s.addTest, unittest.FunctionTestCase, ["start","stop"], [(1,10)])(test_randrange_start_stop)
    larky.parametrize(s.addTest, unittest.FunctionTestCase, ["start","stop","step"], [(0,10,2)])(test_randrange_step)

    s.addTest(unittest.FunctionTestCase(test_randrange_negative_step))
    s.addTest(unittest.FunctionTestCase(test_randrange_single_value))
    s.addTest(unittest.FunctionTestCase(test_randrange_all_values))
    s.addTest(unittest.FunctionTestCase(test_randrange_keywords))
    s.addTest(unittest.FunctionTestCase(test_randrange_big_ints))
    s.addTest(unittest.FunctionTestCase(test_randrange_errors))

    # randint positive
    larky.parametrize(s.addTest, unittest.FunctionTestCase, ["a","b"], [(3,7)])(test_randint_basic)
    s.addTest(unittest.FunctionTestCase(test_randint_single_value))

    s.addTest(unittest.FunctionTestCase(test_randint_errors))

    # choice positive
    s.addTest(unittest.FunctionTestCase(test_choice_basic))

    s.addTest(unittest.FunctionTestCase(test_choice_empty_fails))

    # sample positive
    larky.parametrize(s.addTest, unittest.FunctionTestCase, "n", [2])(test_sample_basic)
    s.addTest(unittest.FunctionTestCase(test_sample_zero))

    s.addTest(unittest.FunctionTestCase(test_sample_all))
    s.addTest(unittest.FunctionTestCase(test_sample_errors))

    # shuffle
    s.addTest(unittest.FunctionTestCase(test_shuffle_basic))

    # getrandbits
    larky.parametrize(s.addTest, unittest.FunctionTestCase, "bits", [8])(test_getrandbits_basic)
    s.addTest(unittest.FunctionTestCase(test_getrandbits_zero))
    s.addTest(unittest.FunctionTestCase(test_getrandbits_one_is_uniform))
    s.addTest(unittest.FunctionTestCase(test_getrandbits_big))
    s.addTest(unittest.FunctionTestCase(test_getrandbits_errors))

    # randbytes
    larky.parametrize(s.addTest, unittest.FunctionTestCase, "n", [16, 0])(test_randbytes_basic)
    s.addTest(unittest.FunctionTestCase(test_randbytes_errors))

    # urandom
    larky.parametrize(s.addTest, unittest.FunctionTestCase, "n", [16, 0])(test_urandom_basic)
    s.addTest(unittest.FunctionTestCase(test_urandom_errors))

    return s


runner = unittest.TextTestRunner()
runner.run(suite())