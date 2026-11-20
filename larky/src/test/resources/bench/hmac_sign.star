load("@stdlib//hashlib", "hashlib")
load("@stdlib//base64", "base64")
load("@stdlib//json", "json")
def process(body):
    digest = hashlib.sha256(bytes(body, "utf-8")).digest()
    return base64.b64encode(digest)
output = process(json.dumps({"a": 1, "b": [1, 2, 3]}))
