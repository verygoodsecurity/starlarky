load("@vgs//kitesurf", "kitesurf")

def find_email_field(fields):
    for field in fields:
        if field["autocomplete"] == "email":
            return field
    fail("email field unavailable")

kitesurf.open(checkout)
before = kitesurf.inspect()
field = find_email_field(before["fields"])
initially_filled = field["filled"]

# The embedding host resolves this reference. The script receives no field value.
kitesurf.fill(field["handle"], "checkout.email")

summary = kitesurf.inspect()
updated = find_email_field(summary["fields"])
filled = updated["filled"]
same_document = updated["handle"] == field["handle"]
