# This is an example of how to redact data in a CSV file while it is being downloaded.

# General Testing libraries
load("@vendor//asserts","asserts")
load("@stdlib//unittest","unittest")
load("@vgs//http/request", "VGSHttpRequest")

# Test Specific libraries
load('@stdlib//re', 're')
load("@vgs//vault", "vault")
load("@stdlib//builtins", "builtins")
load("@vendor//luhn", luhn="luhn")

def process(input, ctx):
    lines = input.body.decode("utf-8").split('\n')
    # Only find cards that begin with these values
    CARD_PATTERN = r"((424242|222300|555555)\d{10})"
    for li in range(len(lines)):
        line = lines[li]
        pans = list()

        for matches in re.findall(CARD_PATTERN, line):
            if matches[0] not in pans and luhn.verify(matches[0]):
                pans.append(matches[0])

        redacted_pans = list()

        for i in range(0, len(pans)):
            pan = pans[i]
            # in a live setting, you can provide more format options, such as:
            #              vault.redact(pan, format='FPE_SIX_T_FOUR', storage='PERSISTENT')
            redacted_pan = vault.redact(pan)
            line = line.replace(pan, redacted_pan)

        lines[li] = line
    lines_str = '\n'.join(lines)
    input.body = builtins.bytes(lines_str)
    return input

def test_process():
    body = builtins.bytes("""NAME,CARD_NUMBER_1,CARD_NUMBER_2,HOUSE_NO
John Travolta,4242424242424242,2223003122003222,51
Humphrey Bogart,5555555555554444,,52
Christopher Walken,378282246310005,,53
TRAILER,110,ENDOFFILE""")
    request_body = body.decode("utf-8").split('\n')

    input = VGSHttpRequest("https://test.com", data=body, headers={}, method='GET')
    response = process(input, None)
    response_body = response.body.decode("utf-8").split('\n')
    print(response_body)

    asserts.assert_that(response_body[0] == request_body[0]).is_true()

    two_card_line = response_body[1].split(',')
    asserts.assert_that(two_card_line[0] == "John Travolta").is_true()
    asserts.assert_that(two_card_line[1].startswith("tok_")).is_true()
    asserts.assert_that(two_card_line[2].startswith("tok_")).is_true()
    asserts.assert_that(two_card_line[3] == "51").is_true()

    one_card_line = response_body[2].split(',')
    asserts.assert_that(one_card_line[0] == "Humphrey Bogart").is_true()
    asserts.assert_that(one_card_line[1].startswith("tok_")).is_true()
    asserts.assert_that(one_card_line[2] == "").is_true()
    asserts.assert_that(one_card_line[3] == "52").is_true()

    ignore_card_line = response_body[3]
    asserts.assert_that(ignore_card_line == request_body[3]).is_true()

    asserts.assert_that(response_body[4] == request_body[4]).is_true()


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_process))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())