load("@stdlib//csv", csv="csv")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//io/StringIO", StringIO="StringIO")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", Result="Result", Error="Error")


def _test_simple_testcase():
    asserts.assert_that(True).is_equal_to(True)
    repr(csv.QuoteMinimalStrategy(csv.excel()))
    asserts.assert_that(csv.list_dialects()).is_equal_to(["excel"])


def _test_simple_reader():
    csvfile = StringIO("""first_name,last_name,email
John,Doe,john-doe@bogusemail.com
Mary,Smith-Robinson,maryjacobs@bogusemail.com
Dave,Smith,davesmith@bogusemail.com
Jane,Stuart,janestuart@bogusemail.com
Tom,Wright,tomwright@bogusemail.com""")
    spamreader = csv.reader(csvfile)
    for row in iter(spamreader):
        print(', '.join(row))
    # for row in spamreader:
    #     print(', '.join(row))

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_simple_testcase))
    _suite.addTest(unittest.FunctionTestCase(_test_simple_reader))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
