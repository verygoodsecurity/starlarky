load("@vgs//native_nts", _nts="native_nts")
load("@vgs//vault", vault="vault")
load("@stdlib//larky", larky="larky")
load("@stdlib//enum", enum="enum")
load("@stdlib//re", re="re")
load("@vendor//jsonpath_ng", jsonpath_ng="jsonpath_ng")
load("@vendor//option/result", safe="safe")

VGS_NETWORK_TOKEN_HEADER = "vgs-network-token"
PSPType = enum.Enum('PSPType', [
    'STRIPE',
    'ADYEN',
    'UNKNOWN',
])
REGEX_PSP_TYPES = [
    (re.compile(r'^https:\/\/api\.stripe\.com'), PSPType.STRIPE),
    (re.compile(r'^https:\/\/(.+)\.adyen\.com'), PSPType.ADYEN),
    (re.compile(r'^https:\/\/(.+)\.adyenpayments\.com'), PSPType.ADYEN),
    # TODO: extend this list to support detecting more PSPs here
]
CRYPTOGRAM_SUPPORTING_PSP_TYPES = {
    PSPType.STRIPE: 1,
    PSPType.ADYEN: 1,
}


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
           the corresponding network token and cryptogram to be returned. One of `cvv` or `dcvv` need to be provided,
           but not both in the same time.
    :param dcvv: JSONPath to the CVV of the credit card in the input payload. Used to pass to the network for retrieving
           the corresponding network token and dynamic CVV to be returned. One of `cvv` or `dcvv` need to be provided,
           but not both at the same time.

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
    amount_value = str(jsonpath_ng.parse(amount).find(input).value)
    currency_code_value = jsonpath_ng.parse(currency_code).find(input).value
    cvv_result = None
    if cvv != None and dcvv == None:
        cvv_result = safe(jsonpath_ng.parse(cvv).find)(input)
    elif dcvv != None and cvv == None:
        cvv_result = safe(jsonpath_ng.parse(dcvv).find)(input)
    elif all([cvv, dcvv]):
        fail("ValueError: only either one of cvv or dvcc can be provided")
    cvv_value = ""
    if cvv_result != None and cvv_result.is_ok:
        cvv_value = cvv_result.unwrap().value

    network_token = _nts.get_network_token(
        pan=pan_value,
        cvv=cvv_value,
        amount=amount_value,
        currency_code=currency_code_value,
        cryptogram_type="TAVV" if dcvv == None else "DTVV",
    )
    placements = [
        (pan, network_token["token"]),
        (exp_month, network_token["exp_month"]),
        (exp_year, network_token["exp_year"]),
        (cryptogram_value, network_token["cryptogram_value"]),
        (cryptogram_eci, network_token["cryptogram_eci"]),
    ]
    # If dynamic CVV is used and provided, we need to place it into the original payload here
    if dcvv != None and network_token.get("cryptogram_type") == "DTVV":
        placements.append((dcvv, network_token["cryptogram_value"]))
    for path, value in placements:
        if path == None:
            continue
        input = jsonpath_ng.parse(path).update(input, value).value
    return input


def supports_dcvv(input, pan):
    """Check a given pan alias value at the `pan` JSON path, see if the network of actual card number in the vault
    supports dynamic CVV or not (only Visa is supported for now).

    :param input: JSON payload to get the pan alias value and check its bin number
    :param pan: JSONPath to the PAN alias in the input payload. Used to look up the actual card number in vault and
           determine if the network supports dynamic CVV or not.
    :return: true if the pan value in the input payload at the given JSON path supports dynamic CVV feature
    """
    return vault.reveal(jsonpath_ng.parse(pan).find(input).value).startswith("4")



def supports_cryptogram(input):
    """Check and see if the PSP determined by given input supports cryptogram or not

    :param input: the HTTP request input
    :return: True if the PSP supports cryptogram otherwise False
    """
    psp_type = get_psp_type(input)
    return psp_type in CRYPTOGRAM_SUPPORTING_PSP_TYPES


def get_psp_type(input):
    """Determine the PSP type based on the HTTP request input

    :param input: the HTTP request input
    :return: PSP type based on request input values
    """
    for regex, psp_type in REGEX_PSP_TYPES:
        if regex.match(input.url):
            return psp_type
    return PSPType.UNKNOWN


def use_network_token(headers):
    """Check value in the headers and determine whether is network token should be used or not

    :param headers: HTTP request headers to be checked against
    :return: True if the network token is enabled, otherwise False
    """
    lower_case_headers = {key.lower(): value for key, value in headers.items()}
    return lower_case_headers.get(VGS_NETWORK_TOKEN_HEADER, "").lower() == "yes"


nts = larky.struct(
    PSPType=PSPType,
    get_network_token=_nts.get_network_token,
    render=render,
    supports_dcvv=supports_dcvv,
    supports_cryptogram=supports_cryptogram,
    get_psp_type=get_psp_type,
    use_network_token=use_network_token,
)
