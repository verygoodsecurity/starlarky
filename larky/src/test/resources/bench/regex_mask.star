load("@stdlib//re", "re")
def process(s):
    return re.sub(r"\d{12}(\d{4})", r"************\1", s)
output = process("card=4111111111111111&exp=12/30")
