


# - name: github.com/verygoodsecurity/common/http/header/Get
#       config:
#         header: "Private-Key"
#         var: "ctx.Private-Key"
#


# from vgs_messages.http_pb2 import HttpMessage
# from vgs_messages.http_pb2 import HttpHeader
# from vgs_messages.http_pb2 import HttpPhase

load('urllib/request', 'Request')

def name():
    return "github.com/verygoodsecurity/xxxx/e75fcdb7-7b86-4384-870e-a24fdca31ef5/tntzr6gpm1s/16c6e5c7-8a2a-4e6e-9e78-2ffc11650a52/38c28e87-3927-4931-9be6-6c699e1954e7/Config"


def message():
    return struct(
        payload=b'....',
        phase=HttpPhase.REQUEST,
        headers=[
                   HttpHeader(
                       key=b'Private-Key',
                       value=b'1m4gHHSKpGp6lS2qKKolXQGzSAVGP5DAB2byaOuJEPyCSomz02i3vXUPWnOofNdBNmQIdEpzOY4XO9SfNSjdDrmUQ3wk4dwpST5GJexjhP9aFc6uUOd6BFNuNSk8ZIYn'
                   )
                ],
    )

def operate(http_message):
    request = Request.make(
        payload=http_message.payload,
        headers=http_message.headers,
        phase=http_message.phase,
    )

    # Remove “signature” from json object (if it exists)
    decoded_payload = json.decode(request.get_data())
    if "signature" in decoded_payload:
        _ = decoded_payload.pop("signature")

    # TODO: what if this key is not in the request?
    # TODO: How do we return errors to the customer?
    header_value = request.remove_header("Private-Key")

    # Create x-secret-key header with private key header value
    request.add_header(("x-secret-key", header_value))

    # Sort the json object alphabetically
    def _get_second_item(pair):
        return pair[1]

    sorted_pairs = sorted(decoded_payload.items(),
                          key=_get_second_item,
                          reverse=True)
    # Concatenate all the values of json object into single string
    # Sha512 hash the concat string
    signature = hashlib.sha512("".join([v for _, v in sorted_pairs]))

    # Set json "signature" value to the sha hash
    decoded_payload["signature"] = signature

    # Set http body to updated json object
    request.payload = json.encode(decoded_payload)
    return request


def input_message():
    return HttpMessage(
        payload=b'{"mid":"1007778759","order_id":"ORD123","api_mode":"direct_token_api","transaction_type":"C","payer_email":"abc@abc.com","payer_name":"Payer name","card_no":"4111111111111111","exp_date":"082019","cvv2":"123","signature":"f767337b377f843abac56d843afd1b71bdd000aef1610b115b4e812dff262f105c0f67b5c41a5836fb6c2d7caeac24e969b5a2e657568e417c124281fa9ce925"}',
        headers=[
           HttpHeader(
               key=b'Private-Key',
               value=b'1m4gHHSKpGp6lS2qKKolXQGzSAVGP5DAB2byaOuJEPyCSomz02i3vXUPWnOofNdBNmQIdEpzOY4XO9SfNSjdDrmUQ3wk4dwpST5GJexjhP9aFc6uUOd6BFNuNSk8ZIYn'
           )
        ],
        uri="/post",
        phase=HttpPhase.REQUEST)



def test_config():
    request = operate(input_message())
    payload = json.decode(redacted_message.payload)
    assert_eq(len(payload), 10)
    assert_eq(payload['signature'], 'f767337b377f843abac56d843afd1b71bdd000aef1610b115b4e812dff262f105c0f67b5c41a5836fb6c2d7caeac24e969b5a2e657568e417c124281fa9ce926')