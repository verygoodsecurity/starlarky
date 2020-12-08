import pytest
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
