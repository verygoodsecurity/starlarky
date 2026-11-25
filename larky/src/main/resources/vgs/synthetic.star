"""Faker-style construction of SYNTHETIC aliases.

    load("@vgs//synthetic", "Faker")
    load("@vgs//vault", "vault")

    fake = Faker("en_GB", entity_key=customer_id)
    row["email"] = vault.redact(row["email"], alias=fake.email())
    row["age"] = vault.redact(row["age"], alias=fake.age(dob=row["dob"]))

    fake.reveal(alias)                          # the same as vault.reveal(alias, entity_key=customer_id)

`Faker(locale, entity_key, email_domain=None, allow_unreserved_phone=False, scope_kind=False)` binds a
locale and an entity key. `locale` is `xx_CC` or `xx-CC` (any case), or a bare upper-case country `CC`;
only the country is used. A bare lower-case value such as `en` is rejected, because faker-style bare
values are language codes. `entity_key` is required and must not be blank: it is the lookup scope of
every alias this Faker makes, so reveal and delete must give the same one.

Lookup uses exactly the context used at redact and never searches other scopes. Whether the kind of
value (email, city, ...) is part of that context is the caller's choice:

- `scope_kind=False` (the default): kind is only a generation input. `fake.reveal(alias)` and
  `fake.delete(alias)` need only the entity key, and passing `kind=` to them is an error.
- `scope_kind=True`: kind is lookup scope too. `fake.reveal(alias, kind="email")` and
  `fake.delete(alias, kind="email")` must name the kind the alias was made with; omitting it is an
  error. A kind that differs from the redact's finds nothing.

The choice travels in each spec's decorator config as `scope_kind`, so a vault provider knows whether
`kind` is scope or only a generation input. Without this module, the same context is
`vault.reveal(alias, entity_key=..., kind=...)`, where each field is optional and an omitted one adds
no scope.

Each generator method returns an immutable spec to pass as `vault.redact(..., alias=spec)`:

    name()            full_name         first_name()      first_name
    last_name()       last_name         email()           email
    street_address()  street            city()            city
    postcode()        postcode          administrative_unit(), state()   region
    country()         country           date_of_birth()   dob
    phone_number()    phone             age(dob=None)     age

`email_domain` applies to email(), `allow_unreserved_phone` to phone_number(), and `dob` to age(). A dob must be a string (for example ISO 8601, `1990-05-17`).
"""

load("@vgs//native_synthetic", _native="native_synthetic")
load("@vgs//vault", "vault")
load("@stdlib//larky", larky="larky")

def _dob_text(dob):
    """Returns dob, which must be a string or None."""
    if dob != None and type(dob) != "string":
        fail("dob must be a string, not %s" % type(dob))
    return dob

def Faker(locale, entity_key, email_domain = None, allow_unreserved_phone = False, scope_kind = False):
    """Creates a Faker for a locale and an entity key.

    Args:
      locale: `xx_CC` or `xx-CC` in any case, or an upper-case two-letter country code.
      entity_key: the entity every alias made by this Faker belongs to; not blank. It is lookup
        scope: reveal and delete use it.
      email_domain: the domain of email() addresses.
      allow_unreserved_phone: let phone_number() leave the reserved fictional ranges.
      scope_kind: whether kind is lookup scope as well as a generation input. When True, reveal
        and delete require `kind=`; when False (the default) they reject it.

    Returns:
      a struct of generator methods, reveal and delete.
    """
    if type(entity_key) != "string":
        fail("entity_key must be a string, not %s" % type(entity_key))
    if not entity_key.strip():
        fail("entity_key must not be empty or blank")
    if type(scope_kind) != "bool":
        fail("scope_kind must be a bool, not %s" % type(scope_kind))
    country = _native.locale_country(locale)

    def _spec(kind, **options):
        return _native.alias_spec(kind, entity_key, country, scope_kind = scope_kind, **options)

    def _email():
        return _spec("email", email_domain = email_domain)

    def _phone_number():
        return _spec("phone", allow_unreserved_phone = allow_unreserved_phone)

    def _age(dob = None):
        return _spec("age", dob = _dob_text(dob))

    def _lookup_kind(kind):
        """The kind to look up with: required when kind is scope, rejected when it is not."""
        if scope_kind:
            if kind == None:
                fail("kind is required: this Faker has scope_kind=True, so kind is lookup scope")
            if type(kind) != "string":
                fail("kind must be a string, not %s" % type(kind))
            return kind
        if kind != None:
            fail("kind is not lookup scope for this Faker (scope_kind=False); omit it")
        return None

    def _reveal(alias, storage = None, kind = None):
        return vault.reveal(alias, storage = storage, entity_key = entity_key, kind = _lookup_kind(kind))

    def _delete(alias, storage = None, kind = None):
        return vault.delete(alias, storage = storage, entity_key = entity_key, kind = _lookup_kind(kind))

    return larky.struct(
        name = lambda: _spec("full_name"),
        first_name = lambda: _spec("first_name"),
        last_name = lambda: _spec("last_name"),
        email = _email,
        street_address = lambda: _spec("street"),
        city = lambda: _spec("city"),
        postcode = lambda: _spec("postcode"),
        administrative_unit = lambda: _spec("region"),
        state = lambda: _spec("region"),
        country = lambda: _spec("country"),
        date_of_birth = lambda: _spec("dob"),
        phone_number = _phone_number,
        age = _age,
        reveal = _reveal,
        delete = _delete,
    )
