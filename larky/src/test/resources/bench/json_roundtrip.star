load("@stdlib//json", "json")
def process(payload):
    d = json.loads(payload)
    d["card"] = d["card"][:6] + "******" + d["card"][-4:]
    return json.dumps(d)
output = process('{"card": "4111111111111111", "amount": 1000, "currency": "USD"}')
