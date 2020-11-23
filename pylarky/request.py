from dataclasses import dataclass
from typing import Dict


@dataclass
class HttpRequest:
    url: str
    data: str
    headers: Dict[str, str]

    @property
    def prefill(self):
        return "load('@stdlib/urllib/request', 'Request')"

    def to_starlark(self):
        return \
            f"{self.prefill} \n" \
            f"request = Request(url='{self.url},'" \
            f"data = '{self.data}," \
            f"'headers = {str(self.headers)})"
