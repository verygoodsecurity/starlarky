load("@stdlib//larky", larky="larky")

def inverse_mod(a, m):
    """Inverse of a mod m."""
    if a == 0:  # pragma: no branch
        return 0
    return pow(a, -1, m)

numbertheory = larky.struct(
    inverse_mod=inverse_mod
)
