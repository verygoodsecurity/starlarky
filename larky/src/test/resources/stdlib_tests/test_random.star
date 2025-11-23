load("@stdlib//random", random="random")
load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")

# randrange tests

def test_randrange_basic():
    # randrange(stop)
    for _ in range(20):
        x = random.randrange(5)
        asserts.assert_true(0 <= x and x < 5, "randrange(stop) out of range: {}".format(x))

    # randrange(start, stop)
    for _ in range(20):
        y = random.randrange(1, 10)
        asserts.assert_true(1 <= y and y < 10, "randrange(start,stop) out of range: {}".format(y))

    # randrange(start,stop,step)
    for _ in range(20):
        z = random.randrange(0, 10, 2)
        asserts.assert_true(z % 2 == 0, "randrange(0,10,2) returned odd: {}".format(z))
        asserts.assert_true(0 <= z and z < 10, "randrange(0,10,2) out of range: {}".format(z))


def test_randrange_negative_step():
    valid = [10, 8, 6, 4, 2]
    for _ in range(20):
        v = random.randrange(10, 0, -2)
        asserts.assert_true(v in valid, "randrange(10,0,-2) invalid: {}".format(v))


def test_randrange_single_value():
    for _ in range(10):
        asserts.assert_that(random.randrange(5, 6)).is_equal_to(5)


def test_randrange_zero_step_fails():
    # Should raise fatal error
    random.randrange(0, 10, 0)


def test_randrange_empty_range_fails():
    random.randrange(1, 1)


def test_randrange_bad_direction_fails():
    random.randrange(10, 0, 2)


def test_randrange_stop_zero_fails():
    random.randrange(0)


# randint tests

def test_randint_basic():
    for _ in range(20):
        v = random.randint(3, 7)
        asserts.assert_true(3 <= v and v <= 7, "randint out of range: {}".format(v))


def test_randint_single_value():
    asserts.assert_that(random.randint(5, 5)).is_equal_to(5)


def test_randint_invalid_fails():
    random.randint(10, 5)


# choice tests

def test_choice_basic():
    seq = [1, 2, 3]
    for _ in range(20):
        c = random.choice(seq)
        asserts.assert_true(c in seq, "choice returned invalid: {}".format(c))


def test_choice_empty_fails():
    random.choice([])


# sample tests

def test_sample_basic():
    s = random.sample([1, 2, 3, 4], 2)
    asserts.assert_that(len(s)).is_equal_to(2)
    for v in s:
        asserts.assert_true(v in [1,2,3,4], "sample invalid: {}".format(v))


def test_sample_zero():
    s = random.sample([1,2,3], 0)
    asserts.assert_that(len(s)).is_equal_to(0)


def test_sample_invalid_fails():
    random.sample([1,2,3], 5)


# shuffle tests

def test_shuffle_basic():
    original = [1,2,3,4]
    shuffled = random.shuffle(original)

    # original list must NOT change
    asserts.assert_that(original).is_equal_to([1,2,3,4])

    # shuffled must contain the same elements
    asserts.assert_that(sorted(shuffled)).is_equal_to([1,2,3,4])


# getrandbits tests

def test_getrandbits_basic():
    v = random.getrandbits(8)
    asserts.assert_true(0 <= v and v < (1<<8), "getrandbits(8) invalid: {}".format(v))


def test_getrandbits_zero():
    asserts.assert_that(random.getrandbits(0)).is_equal_to(0)


def test_getrandbits_negative_fails():
    random.getrandbits(-1)


# randbytes tests

def test_randbytes_basic():
    b = random.randbytes(16)
    asserts.assert_that(len(b)).is_equal_to(16)


def test_randbytes_zero():
    b = random.randbytes(0)
    asserts.assert_that(len(b)).is_equal_to(0)


# urandom tests

def test_urandom_basic():
    b = random.urandom(16)
    asserts.assert_that(len(b)).is_equal_to(16)


def test_urandom_zero():
    b = random.urandom(0)
    asserts.assert_that(len(b)).is_equal_to(0)


# Suite

def suite():
    s = unittest.TestSuite()

    # randrange positive
    s.addTest(unittest.FunctionTestCase(test_randrange_basic))
    s.addTest(unittest.FunctionTestCase(test_randrange_negative_step))
    s.addTest(unittest.FunctionTestCase(test_randrange_single_value))

    # randrange expected failures
    s.addTest(unittest.expectedFailure(unittest.FunctionTestCase(test_randrange_zero_step_fails)))
    s.addTest(unittest.expectedFailure(unittest.FunctionTestCase(test_randrange_empty_range_fails)))
    s.addTest(unittest.expectedFailure(unittest.FunctionTestCase(test_randrange_bad_direction_fails)))
    s.addTest(unittest.expectedFailure(unittest.FunctionTestCase(test_randrange_stop_zero_fails)))

    # randint positive
    s.addTest(unittest.FunctionTestCase(test_randint_basic))
    s.addTest(unittest.FunctionTestCase(test_randint_single_value))

    # randint expected failure
    s.addTest(unittest.expectedFailure(unittest.FunctionTestCase(test_randint_invalid_fails)))

    # choice positive
    s.addTest(unittest.FunctionTestCase(test_choice_basic))

    # choice expected failure
    s.addTest(unittest.expectedFailure(unittest.FunctionTestCase(test_choice_empty_fails)))

    # sample positive
    s.addTest(unittest.FunctionTestCase(test_sample_basic))
    s.addTest(unittest.FunctionTestCase(test_sample_zero))

    # sample expected failure
    s.addTest(unittest.expectedFailure(unittest.FunctionTestCase(test_sample_invalid_fails)))

    # shuffle
    s.addTest(unittest.FunctionTestCase(test_shuffle_basic))

    # getrandbits
    s.addTest(unittest.FunctionTestCase(test_getrandbits_basic))
    #NEXTREV fix bug in getrandbits
    #s.addTest(unittest.FunctionTestCase(test_getrandbits_zero))
    s.addTest(unittest.expectedFailure(unittest.FunctionTestCase(test_getrandbits_negative_fails)))

    # randbytes
    s.addTest(unittest.FunctionTestCase(test_randbytes_basic))
    s.addTest(unittest.FunctionTestCase(test_randbytes_zero))

    # urandom
    s.addTest(unittest.FunctionTestCase(test_urandom_basic))
    s.addTest(unittest.FunctionTestCase(test_urandom_zero))

    return s


runner = unittest.TextTestRunner()
runner.run(suite())