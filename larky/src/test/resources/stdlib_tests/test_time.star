load("@vendor//asserts",  "asserts")
load("@stdlib//time", "time")
load("@stdlib//unittest", "unittest")
load("@stdlib//types", "types")
load("@stdlib//re", "re")

eq = asserts.eq

def _time_test():
    """Unit tests for time module"""

    # Example result timestamp: 1623036968.702
    print('Current timestamp:', time.time())
    asserts.assert_true(types.is_float(time.time()))


def _gmtime_test():
    current_gmt = time.gmtime()
    print('Current gmtime dict:', current_gmt)
    asserts.assert_true(types.is_int(current_gmt.tm_year))
    asserts.assert_true(types.is_int(current_gmt.tm_min))

    print('gmtime dict for given timestamp:', time.gmtime(1622789060))
    res_dict = time.gmtime(1622789060)
    eq(res_dict.tm_year, 2021)
    eq(res_dict.tm_mon, 6)
    eq(res_dict.tm_mday, 4)
    eq(res_dict.tm_hour, 6)
    eq(res_dict.tm_min, 44)
    eq(res_dict.tm_sec, 20)
    eq(res_dict.tm_wday, 4)
    eq(res_dict.tm_yday, 155)


def _strftime_test():
    # Example result: Fri, 04 Jun 2021 16:36:08 +0000
    utc_string1 = time.strftime("%a, %d %b %Y %H:%M:%S +0000")
    print('Current utc time string for given format 1:', utc_string1)
    m = re.search(r"\w{3}, \d{2} \w{3} \d{4} \d{2}:\d{2}:\d{2} \+0000", utc_string1)
    asserts.assert_that(m.group(0)).is_equal_to(utc_string1)

    # 20210604
    utc_string2 = time.strftime("%Y%m%d")
    print('Current utc time string for given format 2:', utc_string2)
    m = re.search(r"\d{8}", utc_string2)
    asserts.assert_that(m.group(0)).is_equal_to(utc_string2)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_time_test))
    _suite.addTest(unittest.FunctionTestCase(_gmtime_test))
    _suite.addTest(unittest.FunctionTestCase(_strftime_test))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())