from dataclasses import dataclass
from typing import Dict


@dataclass
class HttpMessage:
    url: str
    data: str
    headers: Dict[str, str]
