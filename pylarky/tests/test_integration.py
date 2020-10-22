import tempfile
from pylarky.starlarky import evaluate


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
