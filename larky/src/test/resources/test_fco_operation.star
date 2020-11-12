load('@stdlib/json', 'json')
load('@stdlib/unittest', 'unittest')
load('@stdlib/urllib/request', 'Request')
load("@stdlib/hashlib", "hashlib")
load("@stdlib/asserts", "asserts")

load("testlib/vgs_messages", "http_pb2")
load("testlib/builtinz", "simple_set")


def _name():
    return "github.com/verygoodsecurity/xxxx/e75fcdb7-7b86-4384-870e-a24fdca31ef5/tntzr6gpm1s/16c6e5c7-8a2a-4e6e-9e78-2ffc11650a52/38c28e87-3927-4931-9be6-6c699e1954e7/Config"


def _get_second_item(pair):
    return pair[1]


def process(message, ctx):
    request = Request(
        message.uri,
        data=message.payload,
        headers=dict(message.headers),
    )

    # Remove “signature” from json object (if it exists)
    decoded_payload = json.decode(request.data)
    if "signature" in decoded_payload:
        _ = decoded_payload.pop("signature")

    # TODO: what if this key is not in the request?
    # TODO: How do we return errors to the customer?
    header_value = request.remove_header("Private-Key")

    # Create x-secret-key header with private key header value
    request.add_header("x-secret-key", header_value)

    # Sort the json object alphabetically
    sorted_pairs = sorted(decoded_payload.items(),
                          key=_get_second_item,
                          reverse=True)

    # Concatenate all the values of json object into single string
    # Sha512 hash the concat string
    signature = hashlib.sha512("".join([v for _, v in sorted_pairs]))

    # Set json "signature" value to the sha hash
    decoded_payload["signature"] = signature

    print(json.encode(decoded_payload))
    # Set http body to updated json object
    request.data = json.encode(decoded_payload)
    return request


def _input_message():
    return http_pb2.HttpMessage(
        payload='{"mid":"1007778759","order_id":"ORD123","api_mode":"direct_token_api","transaction_type":"C","payer_email":"abc@abc.com","payer_name":"Payer name","card_no":"4111111111111111","exp_date":"082019","cvv2":"123","signature":"f767337b377f843abac56d843afd1b71bdd000aef1610b115b4e812dff262f105c0f67b5c41a5836fb6c2d7caeac24e969b5a2e657568e417c124281fa9ce925"}',
        headers=[
            http_pb2.HttpHeader(
               key='Private-Key',
               value='1m4gHHSKpGp6lS2qKKolXQGzSAVGP5DAB2byaOuJEPyCSomz02i3vXUPWnOofNdBNmQIdEpzOY4XO9SfNSjdDrmUQ3wk4dwpST5GJexjhP9aFc6uUOd6BFNuNSk8ZIYn'
           )
        ],
        uri="/post",
        phase=http_pb2.HttpPhase.REQUEST)


def _test_config():
    modified = process(_input_message(), {})
    payload = json.decode(modified.data)
    asserts.assert_that(payload).is_length(10)
    (asserts
     .assert_that(payload['signature'])
     .is_equal_to('f767337b377f843abac56d843afd1b71bdd000aef1610b115b4e812dff262f105c0f67b5c41a5836fb6c2d7caeac24e969b5a2e657568e417c124281fa9ce926'))


def suite():
    suite = unittest.TestSuite()
    suite.addTest(unittest.FunctionTestCase(_test_config))
    return suite


runner = unittest.TextTestRunner()
runner.run(suite())

