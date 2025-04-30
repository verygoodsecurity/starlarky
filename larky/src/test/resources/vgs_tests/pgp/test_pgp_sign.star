load("@vendor//asserts", "asserts")
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", "unittest")
load("@vgs//pgp", "pgp")

# Test key pair - do not use in production
client_public_key = """-----BEGIN PGP PUBLIC KEY BLOCK-----

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
gmNtylgnstPHygBV8Qmryh+tF8bOwi0QjTsrvlA=
=F8Ao
-----END PGP PUBLIC KEY BLOCK-----"""

# nosemgrep: secrets.misc.generic_private_key_pgp.generic_private_key_pgp
client_private_signing_subkey = """-----BEGIN PGP PRIVATE KEY BLOCK-----

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
AFXxCavKH60Xxs7CLRCNOyu+UA==
=obP3
-----END PGP PRIVATE KEY BLOCK-----"""

third_party_public_key = """-----BEGIN PGP PUBLIC KEY BLOCK-----

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
bQ==
=iHdS
-----END PGP PUBLIC KEY BLOCK-----"""

# nosemgrep: secrets.misc.generic_private_key_pgp.generic_private_key_pgp
third_party_private_key = """-----BEGIN PGP PRIVATE KEY BLOCK-----

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
=Yyfm
-----END PGP PRIVATE KEY BLOCK-----"""

def test_pgp_sign_verify():
    """Test basic PGP signing and verification"""

    # Test message and file name
    message = bytes("This is a test message for PGP signing", "utf-8")
    file_name = "test.txt"

    # Test sign with SHA-256
    signed_message = pgp.sign(
        message=message,
        private_key=third_party_private_key,
        hash_algorithm="SHA-256",
        armor=True
    )

    # Verify it's signed (should start with -----BEGIN PGP MESSAGE-----)
    signed_text = signed_message.decode("utf-8")
    asserts.assert_that(signed_text).contains("-----BEGIN PGP MESSAGE-----")

    # Verify the signature
    verified = pgp.verify(
        signed_message=signed_message,
        public_key=third_party_public_key
    )

    # Verify the verification worked
    asserts.assert_that(verified).is_equal_to(True)

    # Test with different hash algorithms
    hash_algorithms = ["SHA-1", "SHA-256", "SHA-384", "SHA-512"]

    for hash_algo in hash_algorithms:
        print("Testing with hash algorithm: " + hash_algo)

        # Sign with this algorithm
        signed = pgp.sign(
            message=message,
            private_key=third_party_private_key,
            hash_algorithm=hash_algo,
            armor=True
        )

        # Verify the signature
        is_valid = pgp.verify(
            signed_message=signed,
            public_key=third_party_public_key
        )

        # Check that validation worked
        asserts.assert_that(is_valid).is_equal_to(True)

    print("All signature tests passed successfully!")

def test_sign_then_encrypt(encryption_key_id, signing_key_id):
    """Test signing and then encrypting with PGP"""

    # Test message and file name
    message = bytes("This is a test message for PGP signing and encryption", "utf-8")
    file_name = "test.txt"
    
    # Test sign and encrypt with combined operation
    encrypted_signed = pgp.encrypt(
        message=message,
        public_key=third_party_public_key,
        public_key_id=encryption_key_id,
        private_key=client_private_signing_subkey,
        private_key_id=signing_key_id,
        hash_algorithm="SHA-256",
        algorithm="AES-256",
        # file_name=file_name,
        armor=True
    )
    
    # Should be encrypted (should start with -----BEGIN PGP MESSAGE-----)
    encrypted_text = encrypted_signed.decode("utf-8")
    asserts.assert_that(encrypted_text).contains("-----BEGIN PGP MESSAGE-----")


    decrypted = pgp.decrypt(encrypted_signed, third_party_private_key, encryption_key_id)
    asserts.assert_true(pgp.verify(decrypted, client_public_key, signing_key_id))

    # Decrypt and automatically verify with the private key
    decrypted = pgp.decrypt(
        encrypted_message=encrypted_signed,
        public_key=client_public_key,
        public_key_id=signing_key_id,
        private_key=third_party_private_key,
        private_key_id=encryption_key_id,
        verify=True  # Automatically verify the signature
    )

    # Verify decryption worked
    asserts.assert_that(decrypted).is_equal_to(message)
    
    print("Sign and encrypt test passed successfully!")

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_pgp_sign_verify))
    larky.parametrize(
        _suite.addTest, unittest.FunctionTestCase, "encryption_key_id,signing_key_id", [
            # primary implicitly + primary implicitly
            (None, None),
            # primary explicitly + primary implicitly
            ("287E6DF3DDA4E52D32E4E3E1BA6635113E6939A0", None),
            # primary implicitly + primary explicitly
            (None, "0AD9AE85343A401153506617C4C2282E6AEDA391"),
            # primary explicitly + primary explicitly
            ("287E6DF3DDA4E52D32E4E3E1BA6635113E6939A0", "0AD9AE85343A401153506617C4C2282E6AEDA391"),
            # sub explicitly + primary implicitly
            ("9C96C84C150A4918BABAAA3AA1259B7993EBE7E7", None),
            # primary implicitly + sub explicitly
            (None, "067B65AD2E09DA80A3878BD7798258EC65BFB2AE"),
            # sub explicitly + sub explicitly
            ("9C96C84C150A4918BABAAA3AA1259B7993EBE7E7", "067B65AD2E09DA80A3878BD7798258EC65BFB2AE"),
            # sub explicitly + primary explicitly
            ("9C96C84C150A4918BABAAA3AA1259B7993EBE7E7", "0AD9AE85343A401153506617C4C2282E6AEDA391"),
            # primary explicitly + sub explicitly
            ("287E6DF3DDA4E52D32E4E3E1BA6635113E6939A0", "067B65AD2E09DA80A3878BD7798258EC65BFB2AE"),
        ]
    )(test_sign_then_encrypt)

    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())
