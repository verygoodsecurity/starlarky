import re
import json

from pylarky.eval.evaluator import Evaluator
from pylarky.model.http_message import HttpMessage


class HttpEvaluator(Evaluator):

    def __init__(self, script):
        super().__init__(script)

    def evaluate(self, http_message: HttpMessage) -> HttpMessage:
        modified_message = super().evaluate(http_message.to_starlark())
        struct_pattern = \
            r"""^.*url = \"(?P<url>.*)\", data = \"(?P<data>.*)\", headers = (?P<headers>.*), o.*"""
        match = re.search(struct_pattern, modified_message)

        return HttpMessage(match.group('url'),
                           match.group('data').replace("\\\"", "\""),
                           json.loads(match.group('headers')))
