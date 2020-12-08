from pylarky.eval.evaluator import Evaluator
from pylarky.request import HttpRequest


class HttpEvaluator(Evaluator):

    def __init__(self, script, http_request: HttpRequest):
        super(HttpEvaluator, self).__init__(script, http_request.to_starlark())

    def evaluate(self) -> HttpRequest:
        _request = super(HttpEvaluator, self).evaluate()
        return HttpRequest()
