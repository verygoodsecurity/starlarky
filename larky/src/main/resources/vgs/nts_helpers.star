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
    """Retrieves a network token for the given PAN alias, renders the cryptogram, and injects the network token values
    into the payload.

    For the output JSONPaths, please note that inserting a value into a non-existing deep nested note is not currently
    supported. For example, for an input payload like this::

        input = {
            "data": {}
        }

    To insert into `$.data.network_token.exp_month` JSONPath, you need to place an empty value at the exact path first
    like this in order to make JSONPath value insertion work::

        input["data"]["network_token"] = {"exp_month": "TO_BE_REPLACED"}
        nts_helpers.render(input, ...)

    :param input: JSON payload to inject network token into
    :param pan: JSONPath to the PAN alias in the input payload or a raw PAN alias value if `raw_pan` is true.
           Used to look up the corresponding network token to be rendered and injected into the payload.
    :param cvv: JSONPath to the CVV of the credit card in the input payload or a raw CVV value if `raw_cvv` is true.
           Used to pass to the network for retrieving the corresponding network token and cryptogram to be returned.
    :param amount: JSONPath to the amount of payment for the transaction to be made with the network token in the input
           payload or a raw amount value if `raw_amount` is true. Used to pass to the network for retrieving the
           corresponding network token and cryptogram to be returned.
    :param currency_code: JSONPath to the currency code of payment amount for the transaction to be made with the
           network token in the input payload or a raw amount value if `raw_amount` is true. Used to pass to the
           network for retrieving the corresponding network token and cryptogram to be returned.
    :param raw_pan: treat `pan` value as a raw input value instead of a JSONPath value
    :param raw_cvv: treat `raw_cvv` value as a raw input value instead of a JSONPath value
    :param raw_amount: treat `raw_amount` value as a raw input value instead of a JSONPath value
    :param raw_currency_code: treat `raw_currency_code` value as a raw input value instead of a JSONPath value
    :param output_pan: JSONPath to insert the PAN value of the network token within the input payload.
           By default, the `pan` JSONPath path value will be used if no not provided.
    :param output_exp_month: JSONPath to insert the expiration month of the network token within the input payload
    :param output_exp_year: JSONPath to insert the expiration year of the network token within the input payload
    :param output_cryptogram_value: JSONPath to insert the cryptogram value of the network token within the input
           payload
    :param output_cryptogram_eci: JSONPath to insert the cryptogram ECI of the network token within the input payload
    :return: JSON payload injected with network token values
    """
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
