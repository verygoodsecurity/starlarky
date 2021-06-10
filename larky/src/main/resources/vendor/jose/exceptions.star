load("@vendor//option/result", Result="Result", Ok="Ok", Error="Error", safe="safe")

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


