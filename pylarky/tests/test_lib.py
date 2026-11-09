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

    assert (
        eval_output
        == '{"body": "thisisabody", "headers": {"accept": "json", "content": "still-json"}}'
    )


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


def test_evaluation_loads_stdlib():
    # The runner is a native image: Larky's .star modules must be built into it.
    starlark_script = """
load("@stdlib//base64", "base64")
def modify():
    return {"body": base64.b64encode(bytes(ctx["body"], "utf-8")).decode("utf-8")}
modify()
"""
    input_script = """
ctx = {"body": "hi"}
"""

    evaluator = Evaluator(starlark_script)
    eval_output = evaluator.evaluate(input_script)

    assert eval_output == '{"body": "aGk="}'
