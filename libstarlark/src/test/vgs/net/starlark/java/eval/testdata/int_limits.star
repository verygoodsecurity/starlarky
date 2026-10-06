# VGS: ints are limited to 65,536 bits (IntLimits.MAX_BITS), checked before the work, so one
# operation can't run for seconds. Bit counts from CPython.

def power_of_two(bits):
    # 1 << bits, built with shifts under Starlark's 512 limit.
    x = 1
    for _ in range(bits // 500):
        x = x << 500
    return x << (bits % 500)

def test_products():
    x = power_of_two(32767)  # 32,768 bits
    assert_eq(x * x, power_of_two(65534))  # 65,535 bits: fits
    assert_fails(lambda: x * (x << 2), "int too large: \\* would make an int of about 65538 bits; the limit is 65536")

test_products()

def test_squaring_stops_early():
    # Unlimited, 25 squarings of 3 reach 53 million bits and take seconds.
    x = 3
    n = 0
    for _ in range(40):
        if x > power_of_two(40000):
            break
        x = x * x
        n += 1
    assert_eq(n, 15)  # 3 ** (2 ** 15) has 51,937 bits > 40,000, so the loop stopped
    assert_fails(lambda: x * x, "int too large")

test_squaring_stops_early()

def test_shifts():
    x = power_of_two(65100)  # 65,101 bits
    assert_eq(x << 400, power_of_two(65500))  # 65,501 bits: fits
    assert_fails(lambda: x << 511, "int too large: << would make an int of about 65612 bits")

test_shifts()

def test_parsing():
    assert_eq(len(str(int("9" * 19000))), 19000)  # 63,117 bits: fits
    assert_fails(lambda: int("9" * 19800), "int too large: a 19800-digit base-10 literal")
    assert_fails(lambda: int("f" * 16500, 16), "int too large")  # 66,000 bits

test_parsing()
