load("@vgs//http/request", "VGSHttpRequest")
load("@stdlib//json", "json")
def process():
    r = VGSHttpRequest("https://example.com/post", data=b'{"a": 1}', headers={"Content-Type": "application/json"}, method="POST")
    return json.loads(r.data.decode("utf-8"))["a"]
output = process()
