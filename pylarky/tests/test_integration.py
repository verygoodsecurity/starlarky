import tempfile
import pytest
from pylarky.starlarky import evaluate, CalledProcessError


def test_evaluation():
    starlark_script = """
def modify():
    return {"body": ctx["body"], "headers": {"accept": "json", "content": "still-json"}}
modify()
"""

    script_file = tempfile.NamedTemporaryFile(mode='w+')
    script_file.write(starlark_script)
    script_file.flush()

    eval_output = \
        evaluate(script_file.name,
                 'ctx = {"body": "thisisabody", "headers": {"accept": "xml", "content": "still-xml"}}')

    assert eval_output == '{"body": "thisisabody", "headers": {"accept": "json", "content": "still-json"}}'


def test_evaluation_exception():
    starlark_script = """
fun modify():
    return {"body": ctx["body"], "headers": {"accept": "json", "content": "still-json"}}
modify()
    """

    script_file = tempfile.NamedTemporaryFile(mode='w+')
    script_file.write(starlark_script)
    script_file.flush()

    with pytest.raises(CalledProcessError):
        assert evaluate(script_file.name,
                        'ctx = {"body": "thisisabody", "headers": {"accept": "xml", "content": "still-xml"}}')
