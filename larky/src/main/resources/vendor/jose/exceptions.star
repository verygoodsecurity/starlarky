# Ported from python-jose jose/exceptions.py; consistent with its master branch between
# 89c3d41bc6c60ad6f7d135f14dcd963cef633637 and 8572088b76f7c0d31068f38aeca0d4273ae8bc8f
# (no release; the version was not recorded, see jose/__init__.star).

load("@vendor//option/result", Result="Result")

def JOSEError():
    return Result.Err("JOSError")

def JWSError():
    return Result.Err("JWSError")

def JWSSignatureError():
    return Result.Err("JWSSignatureError")

def JWSAlgorithmError():
    return Result.Err("JWSAlgorithmError")

def JWTError():
    return Result.Err("JWTError")

def JWTClaimsError():
    return Result.Err("JWTClaimsError")

def ExpiredSignatureError():
    return Result.Err("ExpiredSignatureError")

def JWKError():
    return Result.Err("JWKError")

def JWEError():
    """ Base error for all JWE errors """
    return Result.Err("JWEError")

def JWEParseError():
    """ Could not parse the JWE string provided """
    return Result.Err("JWEParseError")

def JWEInvalidAuth():
    """
    The authentication tag did not match the protected sections of the
    JWE string provided
    """
    return Result.Err("JWEInvalidAuth")

def JWEAlgorithmUnsupportedError():
    """
    The JWE algorithm is not supported by the backend
    """
    return Result.Err("JWEAlgorithmUnsupportedError")


