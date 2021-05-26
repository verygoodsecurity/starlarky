load("@vendor//option/result", Result="Result", Ok="Ok", Error="Error", safe="safe")

def JOSEError():
    return Result.Error("JOSError")

def JWSError():
    return Result.Error("JWSError")

def JWSSignatureError():
    return Result.Error("JWSSignatureError")

def JWSAlgorithmError():
    return Result.Error("JWSAlgorithmError")

def JWTError():
    return Result.Error("JWTError")

def JWTClaimsError():
    return Result.Error("JWTClaimsError")

def ExpiredSignatureError():
    return Result.Error("ExpiredSignatureError")

def JWKError():
    return Result.Error("JWKError")

def JWEError():
    """ Base error for all JWE errors """
    return Result.Error("JWEError")

def JWEParseError():
    """ Could not parse the JWE string provided """
    return Result.Error("JWEParseError")

def JWEInvalidAuth():
    """
    The authentication tag did not match the protected sections of the
    JWE string provided
    """
    return Result.Error("JWEInvalidAuth")

def JWEAlgorithmUnsupportedError():
    """
    The JWE algorithm is not supported by the backend
    """
    return Result.Error("JWEAlgorithmUnsupportedError")


