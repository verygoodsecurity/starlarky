load("@stdlib//larky", larky="larky")
load("@vendor//jsonpath_ng", jsonpath_ng="jsonpath_ng")
load("@vgs//nts", "nts")


def render(
    input,
    pan,
    cvv,
    amount,
    currency_code,
    raw_pan=False,
    raw_cvv=False,
    raw_amount=False,
    raw_currency_code=False,
    output_pan=None,
    output_exp_month=None,
    output_exp_year=None,
    output_cryptogram_value=None,
    output_cryptogram_eci=None,
):
    if raw_pan:
        pan_value = pan
    else:
        pan_value = jsonpath_ng.parse(pan).find(input).value
    if raw_cvv:
        cvv_value = cvv
    else:
        cvv_value = jsonpath_ng.parse(cvv).find(input).value
    if raw_amount:
        amount_value = amount
    else:
        amount_value = str(jsonpath_ng.parse(amount).find(input).value)
    if raw_currency_code:
        currency_code_value = currency_code
    else:
        currency_code_value = jsonpath_ng.parse(currency_code).find(input).value

    network_token = nts.get_network_token(
        pan=pan_value,
        cvv=cvv_value,
        amount=amount_value,
        currency_code=currency_code_value,
    )
    output_pan_jp = output_pan
    if output_pan_jp == None and not raw_pan:
        output_pan_jp = pan
    placements = [
        (output_pan_jp, network_token["token"]),
        (output_exp_month, network_token["exp_month"]),
        (output_exp_year, network_token["exp_year"]),
        (output_cryptogram_value, network_token["cryptogram_value"]),
        (output_cryptogram_eci, network_token["cryptogram_eci"]),
    ]
    for path, value in placements:
        if path == None:
            continue
        input = jsonpath_ng.parse(path).update(input, value).value
    return input


nts_helpers = larky.struct(
    render=render
)
