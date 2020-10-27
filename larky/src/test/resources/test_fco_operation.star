


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


def parameters():
    return {}


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


def phase():
    return message().phase


def body():
    return message().payload


def uri():
    return message().uri


def operate(http_message):
    payload = http_message.payload
    headers = http_message.headers

    decoded_payload = json.decode(payload)
    if "signature" in decoded_payload:
        del decoded_payload["signature"]
    headers.get_header()
    # Remove “signature” from json object (if it exists)
    # Get value of private key header
    # Remove private key header
    # Create x-secret-key header with private key header value
    # Sort the json object alphabetically
    # Concatenate all the values of json object into single string
    # Sha512 has the concat string
    # Set json”signature” value to the sha hash
    # Set http body to updated json object



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



def _process(fco_name, fco_params, input_message, return_ctx=True):
    msg = jsonpath.pop(input_message.payload, "$.signature")
    headers = http.headers(input_message.headers)
    key = headers.pop("Private-Key")
    jsonpath.set(input_message.payload, "x-secret_key", key)

    payload = sorted(json.decode(input_message.payload))
    signature = hashlib.sha512(payload)
    jsonpath.set(payload, "")


    """
     Remove “signature” from json object (if it exists)
Get value of private key header
Remove private key header
Create x-secret-key header with private key header value
Sort the json object alphabetically
Concatenate all the values of json object into single string
Sha512 has the concat string
Set json”signature” value to the sha hash
Set http body to updated json object
    ---
    CompositeOperation:
      name: Config
      type: composite
      operations:
        - name: github.com/redact
          config:
            input: xxxx
        
        - name: github.com/verygoodsecurity/common/starlark
          config:
            code: | 
                msg = jsonpath.pop(input_message.payload, "$.signature")
                headers = http.headers(input_message.headers)
                key = headers.pop("Private-Key")
                jsonpath.set(input_message.payload, "x-secret_key", key)
            
                payload = sorted(json.decode(input_message.payload))
                signature = hashlib.sha512(payload)
                jsonpath.set(payload, "")
        - name: github.com/verygoodsecurity/common/set/intersection
          config:
            parameter1: "signature"
            parameter2: "ctx.payload"
            output: ctx.result
        - name: github.com/verygoodsecurity/common/http/header/Get
          config:
            header: "Private-Key"
            var: "ctx.Private-Key"
        - name: github.com/verygoodsecurity/common/http/header/Remove
          config:
            header: "Private-Key"
        - name: github.com/verygoodsecurity/common/content-type/json/Set
          config:
            path: "x-secret_key"
            value: "ctx.Private-Key"
        - name: github.com/verygoodsecurity/common/content-type/json/Sort
        - name: github.com/verygoodsecurity/common/content-type/json/Concat
          config:
            var: "ctx.concat"
        - name: github.com/verygoodsecurity/common/utils/crypto/sha/Hash
          config:
            algorithm: "sha512"
            input: "ctx.concat"
            var: "ctx.signature"
        - name: github.com/verygoodsecurity/common/content-type/json/Set
          config:
            input: "ctx.payload"
            path: "signature"
            value: "ctx.signature"
            var: "ctx.payload"
        - name: github.com/verygoodsecurity/common/http/body/Set
          config:
            value: "ctx.payload"
    """


def processor():
    return struct(process=_process)


def test_config():
    redacted_message, ctx = processor().process(fco_name(), fco_params(), input_message(), return_ctx=True)
    payload = json.decode(redacted_message.payload)
    assert_eq(len(payload), 10)
    assert_eq(payload['signature'], 'f767337b377f843abac56d843afd1b71bdd000aef1610b115b4e812dff262f105c0f67b5c41a5836fb6c2d7caeac24e969b5a2e657568e417c124281fa9ce926')


def input(request, ctx):
    ctx.payload = content_type.json.Remove(json_path='signature')(request)
    ctx['Private-Key'] = http.header.Get('Private-Key')
    http.header.Remove('Private-Key')
    content_type.json.Set(json_path='x-secret_key')(request, value=ctx['Private-Key'])
    # ?sort?
    # ?Concat?
    ctx.signature = crypto.sha.Hash("sha512")(ctx.concat)
    content_type.json.Set(json_path='signature')(ctx.payload, var)
    http.body.Set(ctx.payload)


def Config(request, ctx):
    ctx.payload = json.loads(request.payload)
    ctx.payload.popitem('signature')
    ctx['Private-Key'] = request.headers().get('Private-Key')
    headers = request.headers()
    headers.popitem('Private-Key')
    ctx.payload['x-secret-key'] = json.dumps(ctx['Private-Key'])