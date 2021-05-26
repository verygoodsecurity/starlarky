load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//larky", larky="larky")
load("@stdlib//sets", sets="sets")


def Algorithms():
    x = dict(
        # DS Algorithms
        NONE="none",
        HS256="HS256",
        HS384="HS384",
        HS512="HS512",
        RS256="RS256",
        RS384="RS384",
        RS512="RS512",
        ES256="ES256",
        ES384="ES384",
        ES512="ES512",
        # Content Encryption Algorithms,
        A128CBC_HS256="A128CBC-HS256",
        A192CBC_HS384="A192CBC-HS384",
        A256CBC_HS512="A256CBC-HS512",
        A128GCM="A128GCM",
        A192GCM="A192GCM",
        A256GCM="A256GCM",
        # Pseudo algorithm for encryption,
        A128CBC="A128CBC",
        A192CBC="A192CBC",
        A256CBC="A256CBC",
        # CEK Encryption Algorithms,
        DIR="dir",
        RSA1_5="RSA1_5",
        RSA_OAEP="RSA-OAEP",
        RSA_OAEP_256="RSA-OAEP-256",
        A128KW="A128KW",
        A192KW="A192KW",
        A256KW="A256KW",
        ECDH_ES="ECDH-ES",
        ECDH_ES_A128KW="ECDH-ES+A128KW",
        ECDH_ES_A192KW="ECDH-ES+A192KW",
        ECDH_ES_A256KW="ECDH-ES+A256KW",
        A128GCMKW="A128GCMKW",
        A192GCMKW="A192GCMKW",
        A256GCMKW="A256GCMKW",
        PBES2_HS256_A128KW="PBES2-HS256+A128KW",
        PBES2_HS384_A192KW="PBES2-HS384+A192KW",
        PBES2_HS512_A256KW="PBES2-HS512+A256KW",
        # Compression Algorithms
        DEF="DEF",
    )
    x.update(
        HMAC=sets.make([x["HS256"], x["HS384"], x["HS512"]]),
        RSA_DS=sets.make([x["RS256"], x["RS384"], x["RS512"]]),
        RSA_KW=sets.make([x["RSA1_5"], x["RSA_OAEP"], x["RSA_OAEP_256"]]),
    )
    x.update(
        RSA=sets.union(x["RSA_DS"], x["RSA_KW"]),
        EC_DS=sets.make([x["ES256"], x["ES384"], x["ES512"]]),
        EC_KW=sets.make([
                x["ECDH_ES"], x["ECDH_ES_A128KW"], x["ECDH_ES_A192KW"], x["ECDH_ES_A256KW"]
        ]),
    )
    x.update(
        EC=sets.union(x["EC_DS"], x["EC_KW"]),
        AES_PSEUDO=sets.make((
            x["A128CBC"],
            x["A192CBC"],
            x["A256CBC"],
            x["A128GCM"],
            x["A192GCM"],
            x["A256GCM"],
        )),
        AES_JWE_ENC=sets.make((
            x["A128CBC_HS256"],
            x["A192CBC_HS384"],
            x["A256CBC_HS512"],
            x["A128GCM"],
            x["A192GCM"],
            x["A256GCM"],
        )),
    )
    x.update(
        AES_ENC=sets.union(x["AES_JWE_ENC"], x["AES_PSEUDO"]),
        AES_KW=sets.make([x["A128KW"], x["A192KW"], x["A256KW"]]),
        AEC_GCM_KW=sets.make([x["A128GCMKW"], x["A192GCMKW"], x["A256GCMKW"]]),
    )
    x.update(
        AES=sets.union(x["AES_ENC"], x["AES_KW"]),
        PBES2_KW=sets.make((
            x["PBES2_HS256_A128KW"], x["PBES2_HS384_A192KW"], x["PBES2_HS512_A256KW"],
        )),
        HMAC_AUTH_TAG=sets.make((
            x["A128CBC_HS256"], x["A192CBC_HS384"], x["A256CBC_HS512"],
        )),
        GCM=sets.make([x["A128GCM"], x["A192GCM"], x["A256GCM"]]),
    )
    x.update(
        SUPPORTED=sets.union(
            x["HMAC"],
            x["RSA_DS"],
            x["EC_DS"],
            sets.make([x["DIR"]]),
            x["AES_JWE_ENC"],
            x["RSA_KW"],
            x["AES_KW"],
        )
    )
    x.update(
        ALL=sets.union(
            x["SUPPORTED"], sets.make([x["NONE"]]), x["AEC_GCM_KW"], x["EC_KW"], x["PBES2_KW"]
        ),
    )
    # x.update(
    #     HASHES={
    #         HS256: hashlib.sha256,
    #         HS384: hashlib.sha384,
    #         HS512: hashlib.sha512,
    #         RS256: hashlib.sha256,
    #         RS384: hashlib.sha384,
    #         RS512: hashlib.sha512,
    #         ES256: hashlib.sha256,
    #         ES384: hashlib.sha384,
    #         ES512: hashlib.sha512,
    #     }
    # )
    x.update(KEYS={})
    return larky.struct(**x)


ALGORITHMS = Algorithms()

ZIPS = larky.struct(
    DEF="DEF",
    NONE=None,
    SUPPORTED=sets.make(("DEF", None,)),
)
