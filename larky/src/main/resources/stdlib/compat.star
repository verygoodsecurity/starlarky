load("@stdlib//larky", larky="larky")
load("@stdlib//re", re="re")

# utility functions that are compatible with python
def _split(tosplit, sep=None):
    if sep == None:
        return re.split(r'(?:\s|\x0B|\r?\n)+', tosplit)
    return tosplit.split(sep)


strings = larky.struct(
    split=_split,
)