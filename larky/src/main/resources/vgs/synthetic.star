"""Faker-style construction of SYNTHETIC aliases.

    load("@vgs//synthetic", "Faker")
    load("@vgs//vault", "vault")

    fake = Faker("en_GB", record=customer_id)
    row["email"] = vault.redact(row["email"], alias=fake.email())
    row["age"] = vault.redact(row["age"], alias=fake.age(dob=row["dob"]))

    vault.reveal(alias, record=customer_id)   # the same as fake.reveal(alias)

`Faker(locale, record, email_domain=None, allow_unreserved_phone=False)` binds a locale and a
record. `locale` is `xx_CC`, `xx-CC` or a bare two-letter country `CC`; only the country is used.
`record` is required and must not be blank: a SYNTHETIC alias is only unique within its record, so
the same record must be given to reveal it.

Each generator method returns an immutable spec to pass as `vault.redact(..., alias=spec)`:

    name()            full_name         first_name()      first_name
    last_name()       last_name         email()           email
    street_address()  street            city()            city
    postcode()        postcode          administrative_unit(), state()   region
    country()         country           date_of_birth()   dob
    phone_number()    phone             age(dob=None)     age

`email_domain` applies to email(), `allow_unreserved_phone` to phone_number(), and `dob` to age().
"""

load("@vgs//native_synthetic", _native="native_synthetic")
load("@vgs//vault", "vault")
load("@stdlib//larky", larky="larky")

def _dob_text(dob):
    """Returns dob as text; dates and datetimes are written in ISO 8601 form."""
    if dob == None or type(dob) == "string":
        return dob
    if hasattr(dob, "isoformat"):
        return dob.isoformat()
    fail("dob must be a string or a date, not %s" % type(dob))

def Faker(locale, record, email_domain = None, allow_unreserved_phone = False):
    """Creates a Faker for a locale and a record.

    Args:
      locale: `xx_CC`, `xx-CC` or a two-letter country code.
      record: the record every alias made by this Faker belongs to; not blank.
      email_domain: the domain of email() addresses.
      allow_unreserved_phone: let phone_number() leave the reserved fictional ranges.

    Returns:
      a struct of generator methods, reveal and delete.
    """
    if type(record) != "string":
        fail("record must be a string, not %s" % type(record))
    if not record.strip():
        fail("record must not be empty or blank")
    country = _native.locale_country(locale)

    def _spec(kind, **options):
        return _native.alias_spec(kind, record, country, **options)

    def _email():
        return _spec("email", email_domain = email_domain)

    def _phone_number():
        return _spec("phone", allow_unreserved_phone = allow_unreserved_phone)

    def _age(dob = None):
        return _spec("age", dob = _dob_text(dob))

    def _reveal(alias, storage = None):
        return vault.reveal(alias, storage = storage, record = record)

    def _delete(alias, storage = None):
        return vault.delete(alias, storage = storage, record = record)

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
