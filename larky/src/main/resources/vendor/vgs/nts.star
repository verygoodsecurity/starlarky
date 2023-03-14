load("@vendor//jsonpath_ng", jsonpath_ng="jsonpath_ng")
load("@vgs//nts", "nts")


def render(input, pan, exp_month=None, exp_year=None, cryptogram_value=None, cryptogram_eci=None):
    pan_value = jsonpath_ng.parse(pan).find(input).value_
    network_token = nts.get_network_token(pan_value)
    placements = [
        (pan, network_token["token"]),
        (exp_month, network_token["exp_month"]),
        (exp_year, network_token["exp_year"]),
        (cryptogram_value, network_token["cryptogram_value"]),
        (cryptogram_eci, network_token["cryptogram_eci"]),
    ]
    for path, value in placements:
        if path is None:
            continue
        input = jsonpath_ng.parse(path).update(input, value)
    return input


nts = larky.struct(
    render=render
)
