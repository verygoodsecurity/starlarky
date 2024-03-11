load("@vgs//native_au", _au="native_au")
load("@stdlib//larky", larky="larky")

VGS_ACCOUNT_UPDATER_HEADER = "vgs-account-updater"

def lookup_card(pan, exp_month, exp_year, name, client_id, client_secret):
    return _au.lookup_card(
        pan=pan,
        exp_month=exp_month,
        exp_year=exp_year,
        name=name,
        client_id=client_id,
        client_secret=client_secret,
    )

def use_account_updater(headers):
    """Check value in the headers and determine whether is account updater looking up should be used or not

    :param headers: HTTP request headers to be checked against
    :return: True if the account updater looking up is enabled, otherwise False
    """
    lower_case_headers = {key.lower(): value for key, value in headers.items()}
    return lower_case_headers.get(VGS_ACCOUNT_UPDATER_HEADER, "").lower() == "yes"


aus = larky.struct(
    lookup_card=lookup_card,
    use_account_updater=use_account_updater,
)
