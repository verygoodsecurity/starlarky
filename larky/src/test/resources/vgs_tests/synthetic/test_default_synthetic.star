"""Unit tests for @vgs//synthetic and vault.redact(alias=...) using the DefaultVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//synthetic", "Faker")
load("@vgs//vault", "vault")

def _config(spec):
    return dict(spec.decorator_config)

def _base(kind, scope_kind = False, **options):
    config = {"kind": kind, "entity_key": "entity-a", "country": "GB", "scope_kind": scope_kind}
    config.update(options)
    return config

def _test_every_method_returns_a_synthetic_spec():
    fake = Faker("en_GB", entity_key = "entity-a")
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
        asserts.assert_that(spec.entity_key).is_equal_to("entity-a")
        asserts.assert_that(spec.country).is_equal_to("GB")
        asserts.assert_that(spec.scope_kind).is_false()
        asserts.assert_that(_config(spec)).is_equal_to(_base(kind))

def _test_options_are_carried_when_set():
    fake = Faker("en_GB", entity_key = "entity-a", email_domain = "example.org", allow_unreserved_phone = True)
    asserts.assert_that(_config(fake.email())).is_equal_to(_base("email", email_domain = "example.org"))
    asserts.assert_that(_config(fake.phone_number())).is_equal_to(_base("phone", allow_unreserved_phone = True))
    asserts.assert_that(_config(fake.age(dob = "1990-05-17"))).is_equal_to(_base("age", dob = "1990-05-17"))
    asserts.assert_that(_config(fake.age())).is_equal_to(_base("age"))
    asserts.assert_that(_config(fake.city())).is_equal_to(_base("city"))

def _test_scope_kind_is_carried_in_every_spec():
    fake = Faker("en_GB", entity_key = "entity-a", scope_kind = True)
    spec = fake.city()
    asserts.assert_that(spec.scope_kind).is_true()
    asserts.assert_that(_config(spec)).is_equal_to(_base("city", scope_kind = True))
    asserts.assert_that(_config(fake.email())).is_equal_to(_base("email", scope_kind = True))

def _test_scope_kind_must_be_a_bool():
    asserts.assert_fails(
        lambda: Faker("en_GB", entity_key = "e", scope_kind = "yes"),
        "scope_kind must be a bool, not string",
    )

def _test_specs_are_immutable():
    spec = Faker("GB", entity_key = "entity-a").city()
    asserts.assert_fails(lambda: spec.decorator_config.update({"kind": "x"}), "trying to mutate a frozen dict value")

def _test_spec_fields_cannot_be_assigned():
    spec = Faker("GB", entity_key = "entity-a").city()
    asserts.assert_fails(lambda: setattr(spec, "kind", "x"), "type\\(alias_spec\\) does not support setattr.*")

def _test_text_forms_never_contain_the_date_of_birth():
    spec = Faker("GB", entity_key = "entity-a").age(dob = "1987-06-05")
    for text in [repr(spec), str(spec), "%s" % spec, "%r" % spec, "{}".format(spec)]:
        asserts.assert_true("1987" not in text, text)
        asserts.assert_true("dob=<set>" in text, text)
        asserts.assert_true("entity-a" in text, text)
    asserts.assert_that(spec.decorator_config["dob"]).is_equal_to("1987-06-05")

def _test_locale_parsing():
    for locale in ["en_GB", "en-GB", "GB", "en_gb", "EN-gb", "EN_GB"]:
        asserts.assert_that(Faker(locale, entity_key = "e").city().country).is_equal_to("GB")
    asserts.assert_that(Faker("fr_FR", entity_key = "e").city().country).is_equal_to("FR")
    for locale in ["", "G", "GBR", "eng_GB", "en_GBR", "en GB", "en_GB_x", "_GB", "en_"]:
        asserts.assert_fails(lambda: Faker(locale, entity_key = "e"), "Invalid locale.*")

def _test_bare_lower_case_locale_is_a_language_not_a_country():
    for locale in ["cy", "en", "de", "gb"]:
        asserts.assert_fails(
            lambda: Faker(locale, entity_key = "e"),
            "Invalid locale '" + locale + "': a bare lower-case value is a language code.*pass 'en_GB'.*or 'GB'.*",
        )

def _test_entity_key_is_required_and_not_blank():
    asserts.assert_fails(lambda: Faker("en_GB"), "missing 1 required positional argument: entity_key|missing.*entity_key")
    for entity_key in ["", " ", "\t"]:
        asserts.assert_fails(lambda: Faker("en_GB", entity_key = entity_key), "entity_key must not be empty or blank")
    asserts.assert_fails(lambda: Faker("en_GB", entity_key = 1), "entity_key must be a string")

def _test_option_misuse_fails():
    asserts.assert_fails(lambda: Faker("en_GB", entity_key = "e", email_domain = "a@b.org").email(), "Invalid email_domain")
    asserts.assert_fails(lambda: Faker("en_GB", entity_key = "e").age(dob = " "), "dob must not be empty or blank")
    asserts.assert_fails(lambda: Faker("en_GB", entity_key = "e").age(dob = 1), "dob must be a string, not int")

def _test_redact_with_alias_reaches_the_default_vault():
    fake = Faker("en_GB", entity_key = "entity-a")
    asserts.assert_fails(
        lambda: vault.redact("someone@example.net", alias = fake.email()),
        "Format 'SYNTHETIC' is not supported",
    )
    asserts.assert_fails(
        lambda: vault.redact("34", storage = "volatile", alias = fake.age(dob = "1990-05-17")),
        "Format 'SYNTHETIC' is not supported",
    )

def _test_alias_is_mutually_exclusive_with_format_and_decorator_config():
    spec = Faker("en_GB", entity_key = "entity-a").city()
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
    spec = Faker("en_GB", entity_key = "entity-a").city()
    asserts.assert_fails(lambda: vault.redact("Leeds", None, None, [], None, spec), "accepts no more than")

# DefaultVault has no SYNTHETIC generator, so these tests redact with a spec's decorator config
# and the default format: the lookup context is the same.

def _test_reveal_and_delete_pass_the_entity_key():
    fake = Faker("en_GB", entity_key = "entity-a")
    other = Faker("en_GB", entity_key = "entity-b")
    secret = "example-secret"
    alias = vault.redact(secret, decorator_config = _config(fake.city()))

    asserts.assert_that(other.reveal(alias)).is_equal_to(alias)
    asserts.assert_that(fake.reveal(alias)).is_equal_to(secret)
    asserts.assert_that(fake.reveal(alias)).is_equal_to(vault.reveal(alias, entity_key = "entity-a"))

    other.delete(alias)
    asserts.assert_that(fake.reveal(alias)).is_equal_to(secret)
    fake.delete(alias)
    asserts.assert_that(fake.reveal(alias)).is_equal_to(alias)

def _test_reveal_and_delete_pass_the_storage():
    fake = Faker("en_GB", entity_key = "entity-a")
    secret = "example-secret"
    alias = vault.redact(secret, storage = "volatile", decorator_config = {"entity_key": "entity-a"})

    asserts.assert_that(fake.reveal(alias)).is_equal_to(alias)
    asserts.assert_that(fake.reveal(alias, storage = "volatile")).is_equal_to(secret)
    fake.delete(alias, storage = "volatile")
    asserts.assert_that(fake.reveal(alias, storage = "volatile")).is_equal_to(alias)

def _test_default_faker_rejects_kind_on_reveal_and_delete():
    fake = Faker("en_GB", entity_key = "entity-a")
    message = "kind is not lookup scope for this Faker \\(scope_kind=False\\); omit it"
    asserts.assert_fails(lambda: fake.reveal("tok", kind = "city"), message)
    asserts.assert_fails(lambda: fake.delete("tok", kind = "city"), message)

def _test_default_faker_does_not_find_a_kind_scoped_entry():
    # The default Faker stores kind as a generation input only, so entity_key alone finds its alias,
    # while an alias stored with kind as scope is not found without that kind.
    fake = Faker("en_GB", entity_key = "entity-a")
    secret = "example-secret-k1"
    alias = vault.redact(secret, decorator_config = _config(fake.email()))
    asserts.assert_that(fake.reveal(alias)).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, entity_key = "entity-a", kind = "email")).is_equal_to(alias)

    scoped = Faker("en_GB", entity_key = "entity-a", scope_kind = True)
    scoped_alias = vault.redact(secret, decorator_config = _config(scoped.email()))
    asserts.assert_that(fake.reveal(scoped_alias)).is_equal_to(scoped_alias)

def _test_scoped_faker_requires_kind_on_reveal_and_delete():
    fake = Faker("en_GB", entity_key = "entity-a", scope_kind = True)
    message = "kind is required: this Faker has scope_kind=True, so kind is lookup scope"
    asserts.assert_fails(lambda: fake.reveal("tok"), message)
    asserts.assert_fails(lambda: fake.delete("tok"), message)
    asserts.assert_fails(lambda: fake.reveal("tok", kind = " "), "kind must not be empty or blank")
    asserts.assert_fails(lambda: fake.reveal("tok", kind = 1), "kind must be a string, not int")

def _test_scoped_faker_reveals_and_deletes_with_the_same_kind():
    fake = Faker("en_GB", entity_key = "entity-a", scope_kind = True)
    secret = "example-secret-k2"
    alias = vault.redact(secret, decorator_config = _config(fake.email()))

    asserts.assert_that(fake.reveal(alias, kind = "email")).is_equal_to(secret)
    asserts.assert_that(fake.reveal(alias, kind = "city")).is_equal_to(alias)
    asserts.assert_that(vault.reveal(alias, entity_key = "entity-a")).is_equal_to(alias)
    asserts.assert_that(fake.reveal(alias, kind = "email")).is_equal_to(
        vault.reveal(alias, entity_key = "entity-a", kind = "email"),
    )

    fake.delete(alias, kind = "city")
    asserts.assert_that(fake.reveal(alias, kind = "email")).is_equal_to(secret)
    fake.delete(alias, kind = "email")
    asserts.assert_that(fake.reveal(alias, kind = "email")).is_equal_to(alias)

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_every_method_returns_a_synthetic_spec))
    _suite.addTest(unittest.FunctionTestCase(_test_options_are_carried_when_set))
    _suite.addTest(unittest.FunctionTestCase(_test_scope_kind_is_carried_in_every_spec))
    _suite.addTest(unittest.FunctionTestCase(_test_scope_kind_must_be_a_bool))
    _suite.addTest(unittest.FunctionTestCase(_test_specs_are_immutable))
    _suite.addTest(unittest.FunctionTestCase(_test_spec_fields_cannot_be_assigned))
    _suite.addTest(unittest.FunctionTestCase(_test_text_forms_never_contain_the_date_of_birth))
    _suite.addTest(unittest.FunctionTestCase(_test_locale_parsing))
    _suite.addTest(unittest.FunctionTestCase(_test_bare_lower_case_locale_is_a_language_not_a_country))
    _suite.addTest(unittest.FunctionTestCase(_test_entity_key_is_required_and_not_blank))
    _suite.addTest(unittest.FunctionTestCase(_test_option_misuse_fails))
    _suite.addTest(unittest.FunctionTestCase(_test_redact_with_alias_reaches_the_default_vault))
    _suite.addTest(unittest.FunctionTestCase(_test_alias_is_mutually_exclusive_with_format_and_decorator_config))
    _suite.addTest(unittest.FunctionTestCase(_test_alias_rejects_a_dict_that_looks_like_a_spec))
    _suite.addTest(unittest.FunctionTestCase(_test_alias_rejects_a_string))
    _suite.addTest(unittest.FunctionTestCase(_test_alias_is_keyword_only))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_and_delete_pass_the_entity_key))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_and_delete_pass_the_storage))
    _suite.addTest(unittest.FunctionTestCase(_test_default_faker_rejects_kind_on_reveal_and_delete))
    _suite.addTest(unittest.FunctionTestCase(_test_default_faker_does_not_find_a_kind_scoped_entry))
    _suite.addTest(unittest.FunctionTestCase(_test_scoped_faker_requires_kind_on_reveal_and_delete))
    _suite.addTest(unittest.FunctionTestCase(_test_scoped_faker_reveals_and_deletes_with_the_same_kind))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())
