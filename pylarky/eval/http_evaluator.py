from typing import Dict

from pylarky.eval.evaluator import Evaluator
from pylarky.model.http_message import HttpMessage


class HttpEvaluator(Evaluator):

    def __init__(self, script):
        super().__init__(script)

    def evaluate(self, http_message: HttpMessage, context: Dict[str, str]) -> HttpMessage:
        return super().evaluate(http_message, context)


if __name__ == "__main__":
    starlark_script = """\
load('@stdlib//json', 'json')
load("@stdlib//hashlib", "hashlib")

def process(input, ctx):
    print("start script")
    decoded_payload = json.decode(input)
    key="1234567890"
    signature = hashlib.sha512(bytes(key, encoding='utf-8')).hexdigest()
    print("changing payload")
    decoded_payload["signature"] = signature
    print("returning payload")
    return json.encode(decoded_payload)

    """

    request = HttpMessage(
        "http://httpbin.org/post",
        '{"credit_card": "411111111111111111", '
        '"cvv": "043", '
        '"expiration_date": "03/43"}',
        {
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Key": "1234567890",
        },
    )

    evaluator = HttpEvaluator(starlark_script)
    modified_request = evaluator.evaluate(request, {})
    print(modified_request)
