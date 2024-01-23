load("@vgs//native_au", _au="native_au")
load("@stdlib//larky", larky="larky")

def lookup_card(pan, exp_month, exp_year, name, client_id, client_secret):
    return _au.lookup_card(
        number=pan,
        exp_month=exp_month,
        exp_year=exp_year,
        name=name,
        client_id=client_id,
        client_secret=client_secret,
    )

aus = larky.struct(
    lookup_card=lookup_card,
)
