import pytest
from pylarky.model.http_message import HttpMessage
from pylarky.eval.http_evaluator import HttpEvaluator
from pylarky.eval.evaluator import Evaluator, FailedEvaluation


def test_evaluation():
    starlark_script = """
def modify():
    return {"body": ctx["body"], "headers": {"accept": "json", "content": "still-json"}}
modify()
"""
    input_script = """
ctx = {"body": "thisisabody", "headers": {"accept": "xml", "content": "still-xml"}}    
"""

    evaluator = Evaluator(starlark_script)
    eval_output = evaluator.evaluate(input_script)

    assert eval_output == '{"body": "thisisabody", "headers": {"accept": "json", "content": "still-json"}}'


def test_request_evaluation():
    starlark_script = """load('@stdlib/json', 'json')
load("@stdlib/hashlib", "hashlib")

def process(input_message):
    print("start script")
    decoded_payload = json.decode(input_message.data)
    
    key=input_message.get_header("Key")
    input_message.remove_header("Key")
    signature = hashlib.sha512(key)
    print("changing payload")
    decoded_payload["signature"] = signature
    input_message.data = json.encode(decoded_payload)
    print("returning payload")
    return input_message

process(request)
    """

    request = HttpMessage('http://httpbin.org/post',
                          data='{"credit_card": "411111111111111111", '
                               '"cvv": "043", '
                               '"expiration_date": "03/43"}',
                          headers={'Content-Type': 'application/json',
                                   'Accept': 'application/json',
                                   'Key': '1234567890'})

    evaluator = HttpEvaluator(starlark_script)
    modified_request: HttpMessage = evaluator.evaluate(request)
    assert modified_request.url == 'http://httpbin.org/post'
    assert modified_request.data == '{"credit_card":"411111111111111111","cvv":"043","expiration_date":"03/43","signature":"12b03226a6d8be9c6e8cd5e55dc6c7920caaa39df14aab92d5e3ea9340d1c8a4d3d0b8e4314f1f6ef131ba4bf1ceb9186ab87c801af0d5c95b1befb8cedae2b9"}'
    assert modified_request.headers == {"Content-Type": "application/json", "Accept": "application/json"}


def test_evaluation_exception():
    starlark_script = """
fun modify():
    return {"body": ctx["body"], "headers": {"accept": "json", "content": "still-json"}}
modify()
    """

    input_script = """
ctx = {"body": "thisisabody", "headers": {"accept": "xml", "content": "still-xml"}}
"""

    evaluator = Evaluator(starlark_script)

    with pytest.raises(FailedEvaluation):
        assert evaluator.evaluate(input_script)
