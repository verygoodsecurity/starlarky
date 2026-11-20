load("@vendor//iso8583/encoder", Encoder="encoder")
load("@vendor//iso8583/specs", "default_ascii")
def process():
    doc = {"t": "0200", "2": "4111111111111111", "3": "000000", "4": "000000001000"}
    s, _ = Encoder.encode(doc, default_ascii)
    return s
output = process()
