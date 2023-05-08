load("@vgs//native_nts", _nts="native_nts")
load("@vgs//vault", vault="vault")
load("@stdlib//larky", larky="larky")
load("@vendor//jsonpath_ng", jsonpath_ng="jsonpath_ng")


def render(
    input,
    pan,
    amount,
    currency_code,
    cvv=None,
    dcvv=None,
    exp_month=None,
    exp_year=None,
    cryptogram_value=None,
    cryptogram_eci=None,
):
    """Retrieves a network token for the given PAN alias, renders the cryptogram, and injects the network token values
    into the payload.

    For the output JSONPaths, please note that inserting a value into a non-existing deep nested note is not currently
    supported. For example, for an input payload like this::

        input = {
            "data": {}
        }

    To insert into `$.data.network_token.exp_month` JSONPath, you need to place any value at the exact path first
    like this in order to make JSONPath value insertion work::

        input["data"]["network_token"] = {"exp_month": "TO_BE_REPLACED"}
        nts.render(input, ...)

    :param input: JSON payload to inject network token into
    :param pan: JSONPath to the PAN alias in the input payload. Used to look up the corresponding network token to be
           rendered and injected into the payload.
    :param cvv: JSONPath to the CVV of the credit card in the input payload. Used to pass to the network for retrieving
           the corresponding network token and cryptogram to be returned.
    :param dcvv: JSONPath to the DCVV of the credit card in the input payload. Used to pass to the network for retrieving
               the corresponding network token and cryptogram to be returned.
    :param amount: JSONPath to the amount of payment for the transaction to be made with the network token in the input
           payload. Used to pass to the network for retrieving the corresponding network token and cryptogram to be
           returned.
    :param currency_code: JSONPath to the currency code of payment amount for the transaction to be made with the
           network token in the input payload. Used to pass to the network for retrieving the corresponding network
           token and cryptogram to be returned.
    :param exp_month: JSONPath to insert the expiration month of the network token within the input payload
    :param exp_year: JSONPath to insert the expiration year of the network token within the input payload
    :param cryptogram_value: JSONPath to insert the cryptogram value of the network token within the input
           payload
    :param cryptogram_eci: JSONPath to insert the cryptogram ECI of the network token within the input payload
    :return: JSON payload injected with network token values
    """
    pan_value = jsonpath_ng.parse(pan).find(input).value
    if cvv is not None and dcvv is None:
        cvv_value = jsonpath_ng.parse(cvv).find(input).value
    elif dcvv is not None and cvv is None:
        cvv_value = jsonpath_ng.parse(dcvv).find(input).value
    elif cvv is None and dcvv is None:
        fail("ValueError: either one of cvv or dvcc should be provided")
    else:
        fail("ValueError: either one of cvv or dvcc can be provided")
    amount_value = str(jsonpath_ng.parse(amount).find(input).value)
    currency_code_value = jsonpath_ng.parse(currency_code).find(input).value

    network_token = _nts.get_network_token(
        pan=pan_value,
        cvv=cvv_value,
        amount=amount_value,
        currency_code=currency_code_value,
    )
    placements = [
        (pan, network_token["token"]),
        (exp_month, network_token["exp_month"]),
        (exp_year, network_token["exp_year"]),
        (cryptogram_value, network_token["cryptogram_value"]),
        (cryptogram_eci, network_token["cryptogram_eci"]),
    ]
    # If dynamic CVV is used, we need to place it into the original payload here
    if dcvv is not None:
        placements.append((dcvv, network_token["dcvv"]))
    for path, value in placements:
        if path == None:
            continue
        input = jsonpath_ng.parse(path).update(input, value).value
    return input


def supports_dcvv(input, pan):
    return vault.getValue(jsonpath_ng.parse(pan)).startswith(4)


nts = larky.struct(
    get_network_token=_nts.get_network_token,
    render=render,
    supports_dcvv=supports_dcvv
)
