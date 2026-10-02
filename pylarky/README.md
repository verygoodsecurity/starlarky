# pylarky

Python wrapper for the Larky runner: evaluates a Larky (Starlark) script from Python by running the native `larky-runner` bundled in the wheel.

```python
from pylarky.eval.evaluator import Evaluator, FailedEvaluation

script = """
def modify():
    return {"body": ctx["body"], "headers": {"accept": "json"}}
modify()
"""
output = Evaluator(script).evaluate('ctx = {"body": "thisisabody", "headers": {}}')
# output is the script's result as a JSON string; a failing script raises FailedEvaluation
```

`pylarky.eval.http_evaluator.HttpEvaluator` does the same for an `HttpMessage` (`pylarky.model.http_message`).
