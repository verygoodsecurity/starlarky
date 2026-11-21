load("@vendor//jose", "jwt")
def process():
    return jwt.encode({"sub": "1234", "amount": 10}, "secret", algorithm="HS384")
output = process()
