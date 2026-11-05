"""Unit tests for @vgs//synthetic and vault.redact(alias=...) using the DefaultVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//synthetic", "Faker")
load("@vgs//vault", "vault")

def _config(spec):
    return dict(spec.decorator_config)

def _test_every_method_returns_a_synthetic_spec():
    fake = Faker("en_GB", record = "record-a")
    expected = {
        "name": "full_name",
        "first_name": "first_name",
        "last_name": "last_name",
        "email": "email",
        "street_address": "street",
        "city": "city",
        "postcode": "postcode",
        "administrative_unit": "region",
        "state": "region",
        "country": "country",
        "date_of_birth": "dob",
        "phone_number": "phone",
        "age": "age",
    }
    for method, kind in expected.items():
        spec = getattr(fake, method)()
        asserts.assert_that(spec.format).is_equal_to("SYNTHETIC")
        asserts.assert_that(spec.kind).is_equal_to(kind)
        asserts.assert_that(spec.record).is_equal_to("record-a")
        asserts.assert_that(spec.country).is_equal_to("GB")
        asserts.assert_that(_config(spec)).is_equal_to({"kind": kind, "record": "record-a", "country": "GB"})

def _test_options_are_carried_when_set():
    fake = Faker("en_GB", record = "record-a", email_domain = "example.org", allow_unreserved_phone = True)
    asserts.assert_that(_config(fake.email())).is_equal_to(
        {"kind": "email", "record": "record-a", "country": "GB", "email_domain": "example.org"},
    )
    asserts.assert_that(_config(fake.phone_number())).is_equal_to(
        {"kind": "phone", "record": "record-a", "country": "GB", "allow_unreserved_phone": True},
    )
    asserts.assert_that(_config(fake.age(dob = "1990-05-17"))).is_equal_to(
        {"kind": "age", "record": "record-a", "country": "GB", "dob": "1990-05-17"},
    )
    asserts.assert_that(_config(fake.age())).is_equal_to({"kind": "age", "record": "record-a", "country": "GB"})
    asserts.assert_that(_config(fake.city())).is_equal_to({"kind": "city", "record": "record-a", "country": "GB"})

def _test_specs_are_immutable():
    spec = Faker("GB", record = "record-a").city()
    asserts.assert_fails(lambda: spec.decorator_config.update({"kind": "x"}), "trying to mutate a frozen dict value")
    asserts.assert_fails(lambda: setattr(spec, "kind", "x"), "")

def _test_locale_parsing():
    for locale in ["en_GB", "en-GB", "GB", "gb", "en_gb", "EN-gb"]:
        asserts.assert_that(Faker(locale, record = "r").city().country).is_equal_to("GB")
    asserts.assert_that(Faker("fr_FR", record = "r").city().country).is_equal_to("FR")
    for locale in ["", "G", "GBR", "eng_GB", "en_GBR", "en GB", "en_GB_x", "_GB", "en_"]:
        asserts.assert_fails(lambda: Faker(locale, record = "r"), "Invalid locale")

def _test_record_is_required_and_not_blank():
    asserts.assert_fails(lambda: Faker("en_GB"), "missing 1 required positional argument: record|missing.*record")
    for record in ["", " ", "\t"]:
        asserts.assert_fails(lambda: Faker("en_GB", record = record), "record must not be empty or blank")
    asserts.assert_fails(lambda: Faker("en_GB", record = 1), "record must be a string")

def _test_option_misuse_fails():
    asserts.assert_fails(lambda: Faker("en_GB", record = "r", email_domain = "a@b.org").email(), "Invalid email_domain")
    asserts.assert_fails(lambda: Faker("en_GB", record = "r").age(dob = " "), "dob must not be empty or blank")
    asserts.assert_fails(lambda: Faker("en_GB", record = "r").age(dob = 1), "dob must be a string or a date")

def _test_redact_with_alias_reaches_the_default_vault():
    fake = Faker("en_GB", record = "record-a")
    asserts.assert_fails(
        lambda: vault.redact("someone@example.net", alias = fake.email()),
        "Format 'SYNTHETIC' is not supported",
    )
    asserts.assert_fails(
        lambda: vault.redact("34", storage = "volatile", alias = fake.age(dob = "1990-05-17")),
        "Format 'SYNTHETIC' is not supported",
    )

def _test_alias_is_mutually_exclusive_with_format_and_decorator_config():
    spec = Faker("en_GB", record = "record-a").city()
    for kwargs in [{"format": "UUID"}, {"format": "SYNTHETIC"}, {"decorator_config": {}}, {"decorator_config": {"kind": "city"}}]:
        asserts.assert_fails(
            lambda: vault.redact("Leeds", alias = spec, **kwargs),
            "alias cannot be combined with format or decorator_config",
        )

# One argument-type failure per test: a call-site type error leaves its frame on the stack of
# this test runner, so a second assert_fails in the same test reports a recursive call.
_NOT_A_SPEC = "in call to redact\\(\\), parameter 'alias' got value of type '%s', want 'NoneType or alias_spec'"

def _test_alias_rejects_a_dict_that_looks_like_a_spec():
    asserts.assert_fails(
        lambda: vault.redact("Leeds", alias = {"format": "SYNTHETIC", "decorator_config": {"kind": "city"}}),
        _NOT_A_SPEC % "dict",
    )

def _test_alias_rejects_a_string():
    asserts.assert_fails(lambda: vault.redact("Leeds", alias = "city"), _NOT_A_SPEC % "string")

def _test_alias_is_keyword_only():
    spec = Faker("en_GB", record = "record-a").city()
    asserts.assert_fails(lambda: vault.redact("Leeds", None, None, [], None, spec), "accepts no more than")

def _test_reveal_and_delete_pass_the_record():
    fake = Faker("en_GB", record = "record-a")
    other = Faker("en_GB", record = "record-b")
    secret = "example-secret"
    alias = vault.redact(secret, decorator_config = {"kind": "city", "record": "record-a"})

    asserts.assert_that(other.reveal(alias)).is_equal_to(alias)
    asserts.assert_that(fake.reveal(alias)).is_equal_to(secret)
    asserts.assert_that(fake.reveal(alias)).is_equal_to(vault.reveal(alias, record = "record-a"))

    other.delete(alias)
    asserts.assert_that(fake.reveal(alias)).is_equal_to(secret)
    fake.delete(alias)
    asserts.assert_that(fake.reveal(alias)).is_equal_to(alias)

def _test_reveal_and_delete_pass_the_storage():
    fake = Faker("en_GB", record = "record-a")
    secret = "example-secret"
    alias = vault.redact(secret, storage = "volatile", decorator_config = {"record": "record-a"})

    asserts.assert_that(fake.reveal(alias)).is_equal_to(alias)
    asserts.assert_that(fake.reveal(alias, storage = "volatile")).is_equal_to(secret)
    fake.delete(alias, storage = "volatile")
    asserts.assert_that(fake.reveal(alias, storage = "volatile")).is_equal_to(alias)

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_every_method_returns_a_synthetic_spec))
    _suite.addTest(unittest.FunctionTestCase(_test_options_are_carried_when_set))
    _suite.addTest(unittest.FunctionTestCase(_test_specs_are_immutable))
    _suite.addTest(unittest.FunctionTestCase(_test_locale_parsing))
    _suite.addTest(unittest.FunctionTestCase(_test_record_is_required_and_not_blank))
    _suite.addTest(unittest.FunctionTestCase(_test_option_misuse_fails))
    _suite.addTest(unittest.FunctionTestCase(_test_redact_with_alias_reaches_the_default_vault))
    _suite.addTest(unittest.FunctionTestCase(_test_alias_is_mutually_exclusive_with_format_and_decorator_config))
    _suite.addTest(unittest.FunctionTestCase(_test_alias_rejects_a_dict_that_looks_like_a_spec))
    _suite.addTest(unittest.FunctionTestCase(_test_alias_rejects_a_string))
    _suite.addTest(unittest.FunctionTestCase(_test_alias_is_keyword_only))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_and_delete_pass_the_record))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_and_delete_pass_the_storage))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())
