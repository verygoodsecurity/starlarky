load("@vgs//http/request", "VGSHttpRequest")
load("@stdlib//urllib/parse", parse="parse")

AUTH_TOKEN_URL = "https://auth.verygoodsecurity.io/auth/realms/vgs/protocol/openid-connect/token"

def gen_auth_token(client_id, client_secret):
    data = parse.urlencode(dict(
        client_id=client_id,
        client_secret=client_secret,
    ))
    resp = VGSHttpRequest(
        url=AUTH_TOKEN_URL,
        data=data,
        method="POST",
    )
    return resp


def lookup_account_updates(url, client_id, client_secret, pan, exp_month, exp_year):
    pass
