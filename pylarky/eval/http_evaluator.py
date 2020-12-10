import re
import json

from pylarky.eval.evaluator import Evaluator
from pylarky.model.request import HttpRequest


class HttpEvaluator(Evaluator):

    def __init__(self, script):
        super().__init__(script)

    def evaluate(self, http_request: HttpRequest) -> HttpRequest:
        modified_request = super().evaluate(http_request.to_starlark())
        struct_pattern = \
            r"""^.*url = \"(?P<url>.*)\", data = \"(?P<data>.*)\", headers = (?P<headers>.*), o.*"""
        match = re.search(struct_pattern, modified_request)

        return HttpRequest(match.group('url'),
                           match.group('data').replace("\\\"", "\""),
                           json.loads(match.group('headers')))
