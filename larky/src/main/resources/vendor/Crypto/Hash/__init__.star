load("@stdlib//larky", larky="larky")

load("@vendor//Crypto/Hash/BLAKE2s", BLAKE2s="BLAKE2s")
load("@vendor//Crypto/Hash/keccak", keccak="keccak")
load("@vendor//Crypto/Hash/MD5", MD5="MD5")
load("@vendor//Crypto/Hash/SHA1", SHA1="SHA1")
load("@vendor//Crypto/Hash/SHA224", SHA224="SHA224")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//Crypto/Hash/SHA384", SHA384="SHA384")
load("@vendor//Crypto/Hash/SHA512", SHA512="SHA512")
load("@vendor//Crypto/Hash/SHAKE128", SHAKE128="SHAKE128")
load("@vendor//Crypto/Hash/SHA3_256", SHA3_256="SHA3_256")


Hash = larky.struct(
    BLAKE2s=BLAKE2s,
    MD5=MD5,
    SHA1=SHA1,
    SHA224=SHA224,
    SHA256=SHA256,
    SHA384=SHA384,
    SHA512=SHA512,
    SHAKE128=SHAKE128,
    keccak=keccak,
    KECCAK=keccak,
    SHA3_256=SHA3_256,
)