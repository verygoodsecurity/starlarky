from dataclasses import dataclass
from typing import Dict


@dataclass
class HttpMessage:
    url: str
    data: str
    headers: Dict[str, str]

    @property
    def prefill(self) -> str:
        return "load('@stdlib/urllib/request', 'Request')"

    def to_starlark(self):
        return \
            f"{self.prefill}\n\n" \
            f"request = Request(" \
            f"url='{self.url}'," \
            f"data='{self.data}'," \
            f"headers={self.headers})"
