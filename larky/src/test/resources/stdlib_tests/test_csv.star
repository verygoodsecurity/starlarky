load("@stdlib//csv", csv="csv")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//io/StringIO", StringIO="StringIO")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", Result="Result", Error="Error")


def _test_simple_testcase():
    asserts.assert_that(True).is_equal_to(True)
    asserts.assert_that(csv.list_dialects()).is_equal_to(["excel", "excel-tab", "unix"])


def _test_simple_reader():
    csvfile = StringIO("""first_name,last_name,email
John,Doe,john-doe@bogusemail.com
Mary,Smith-Robinson,maryjacobs@bogusemail.com
Dave,Smith,davesmith@bogusemail.com
Jane,Stuart,janestuart@bogusemail.com
Tom,Wright,tomwright@bogusemail.com""")

    expected = [
        ["first_name", "last_name", "email"],
        ["John", "Doe", "john-doe@bogusemail.com"],
        ["Mary", "Smith-Robinson", "maryjacobs@bogusemail.com"],
        ["Dave", "Smith", "davesmith@bogusemail.com"],
        ["Jane", "Stuart", "janestuart@bogusemail.com"],
        ["Tom", "Wright", "tomwright@bogusemail.com"],
    ]

    spamreader = csv.reader(csvfile)
    for i, row in enumerate(iter(spamreader)):
        asserts.assert_that(len(row)).is_equal_to(3)
        asserts.assert_that(row).is_equal_to(expected[i])


def _test_simple_writer():
    csvfile = StringIO()
    rows = [
        ["first_name", "last_name", "email"],
        ["John", "Doe", "john-doe@bogusemail.com"],
        ["Mary", "Smith-Robinson", "maryjacobs@bogusemail.com"],
        ["Dave", "Smith", "davesmith@bogusemail.com"],
        ["Jane", "Stuart", "janestuart@bogusemail.com"],
        ["Tom", "Wright", "tomwright@bogusemail.com"],
    ]
    expected = (
        "first_name,last_name,email\r\n" +
        "John,Doe,john-doe@bogusemail.com\r\n" +
        "Mary,Smith-Robinson,maryjacobs@bogusemail.com\r\n" +
        "Dave,Smith,davesmith@bogusemail.com\r\n" +
        "Jane,Stuart,janestuart@bogusemail.com\r\n" +
        "Tom,Wright,tomwright@bogusemail.com\r\n"
    )

    spamwriter = csv.writer(csvfile)
    for r in rows:
        spamwriter.writerow(r)
    asserts.assert_that(csvfile.getvalue()).is_equal_to(expected)

    csvfile.truncate(0)
    asserts.assert_that(csvfile.getvalue()).is_equal_to("")
    spamwriter = csv.writer(csvfile)
    spamwriter.writerows(rows)
    asserts.assert_that(csvfile.getvalue()).is_equal_to(expected)


def _test_simple_dictreader():
    csvfile = StringIO("""first_name,last_name,email
John,Doe,john-doe@bogusemail.com
Mary,Smith-Robinson,maryjacobs@bogusemail.com
Dave,Smith,davesmith@bogusemail.com
Jane,Stuart,janestuart@bogusemail.com
Tom,Wright,tomwright@bogusemail.com""")

    expected = [
        {"first_name": "John", "last_name": "Doe", "email": "john-doe@bogusemail.com"},
        {"first_name": "Mary", "last_name": "Smith-Robinson", "email": "maryjacobs@bogusemail.com"},
        {"first_name": "Dave", "last_name": "Smith", "email": "davesmith@bogusemail.com"},
        {"first_name": "Jane", "last_name": "Stuart", "email": "janestuart@bogusemail.com"},
        {"first_name": "Tom",  "last_name": "Wright", "email": "tomwright@bogusemail.com"},
    ]
    spamreader = csv.DictReader(csvfile)
    results = list(iter(spamreader))
    asserts.assert_that(results).is_equal_to(expected)


def _test_simple_dictwriter():
    csvfile = StringIO()
    fieldnames =  ["first_name", "last_name", "email"]

    expected_header = "first_name,last_name,email\r\n"
    expected_body = (
        "John,Doe,john-doe@bogusemail.com\r\n" +
        "Mary,Smith-Robinson,maryjacobs@bogusemail.com\r\n" +
        "Dave,Smith,davesmith@bogusemail.com\r\n" +
        "Jane,Stuart,janestuart@bogusemail.com\r\n" +
        "Tom,Wright,tomwright@bogusemail.com\r\n"
    )

    rows = [
       {"first_name": "John", "last_name": "Doe", "email": "john-doe@bogusemail.com"},
       {"first_name": "Mary", "last_name": "Smith-Robinson", "email": "maryjacobs@bogusemail.com"},
       {"first_name": "Dave", "last_name": "Smith", "email": "davesmith@bogusemail.com"},
       {"first_name": "Jane", "last_name": "Stuart", "email": "janestuart@bogusemail.com"},
       {"first_name": "Tom",  "last_name": "Wright", "email": "tomwright@bogusemail.com"},
    ]

    spamwriter = csv.DictWriter(csvfile, fieldnames=fieldnames)
    spamwriter.writeheader()
    for r in rows:
        spamwriter.writerow(r)
    asserts.assert_that(csvfile.getvalue()).is_equal_to(expected_header + expected_body)
    csvfile.truncate(0)
    asserts.assert_that(csvfile.getvalue()).is_equal_to("")
    # if we do not use writeheader() there is no header!
    spamwriter.writerows(rows)
    asserts.assert_that(csvfile.getvalue()).is_equal_to(expected_body)


def _test_writer_quotes_properly():
    csvfile = StringIO()
    rows = [
        ["first_name", "last_name", "email"],
        ["John", "Doe", "john-doe@bogusemail.com"],
        ["Mary", "Smith-Robinson", "\"maryjacobs@bogusemail.com"],
        ["Dave", "Smith", "davesmith@bogusemail.com"],
        ["Jane", "Stuart", "janestuart@bogusemail.com"],
        ["Tom", "Wright", "tomwright@bogusemail.com"],
    ]
    expected = (
        "first_name,last_name,email\r\n" +
        "John,Doe,john-doe@bogusemail.com\r\n" +
        "Mary,Smith-Robinson,\"\"\"maryjacobs@bogusemail.com\"\r\n" +
        "Dave,Smith,davesmith@bogusemail.com\r\n" +
        "Jane,Stuart,janestuart@bogusemail.com\r\n" +
        "Tom,Wright,tomwright@bogusemail.com\r\n"
    )

    spamwriter = csv.writer(csvfile)
    for r in rows:
        spamwriter.writerow(r)
    asserts.assert_that(csvfile.getvalue()).is_equal_to(expected)



def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_simple_testcase))
    _suite.addTest(unittest.FunctionTestCase(_test_simple_reader))
    _suite.addTest(unittest.FunctionTestCase(_test_simple_dictreader))
    _suite.addTest(unittest.FunctionTestCase(_test_simple_writer))
    _suite.addTest(unittest.FunctionTestCase(_test_simple_dictwriter))
    _suite.addTest(unittest.FunctionTestCase(_test_writer_quotes_properly))


    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
