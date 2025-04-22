load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//pgp", "pgp")

def test_pgp_sign_verify():
    """Test basic PGP signing and verification"""

    # Test key pair - do not use in production
    public_key = """-----BEGIN PGP PUBLIC KEY BLOCK-----

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
gmNtylgnstPHygBV8Qmryh+tF8bOwi0QjTsrvlA=
=F8Ao
-----END PGP PUBLIC KEY BLOCK-----"""

    private_key = """-----BEGIN PGP PRIVATE KEY BLOCK-----

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
AFXxCavKH60Xxs7CLRCNOyu+UA==
=obP3
-----END PGP PRIVATE KEY BLOCK-----"""

    # Test message and file name
    message = bytes("This is a test message for PGP signing", "utf-8")
    file_name = "test.txt"
    
    # Test sign with SHA-256
    signed_message = pgp.sign(
        message=message,
        private_key=private_key,
        file_name=file_name,
        hash_algorithm="SHA-256",
        armor=True
    )
    
    # Verify it's signed (should start with -----BEGIN PGP MESSAGE-----)
    signed_text = str(signed_message, "utf-8")
    asserts.assert_that(signed_text).contains("-----BEGIN PGP MESSAGE-----")
    
    # Verify the signature
    verified = pgp.verify(
        signed_message=signed_message,
        public_key=public_key
    )
    
    # Verify the verification worked
    asserts.assert_that(verified).is_equal_to(True)
    
    # Test with different hash algorithms
    hash_algorithms = ["SHA-1", "SHA-256", "SHA-384", "SHA-512"]
    
    for hash_algo in hash_algorithms:
        print(f"Testing with hash algorithm: {hash_algo}")
        
        # Sign with this algorithm
        signed = pgp.sign(
            message=message,
            private_key=private_key,
            file_name=file_name,
            hash_algorithm=hash_algo,
            armor=True
        )
        
        # Verify the signature
        is_valid = pgp.verify(
            signed_message=signed,
            public_key=public_key
        )
        
        # Check that validation worked
        asserts.assert_that(is_valid).is_equal_to(True)
    
    print("All signature tests passed successfully!")

def test_sign_then_encrypt():
    """Test signing and then encrypting with PGP"""

    # Test key pair - do not use in production
    private_key = """..."""

    public_key = """..."""

    # Test message and file name
    message = bytes("This is a test message for PGP signing and encryption", "utf-8")
    file_name = "test.txt"
    
    # Test sign and encrypt with combined operation
    encrypted_signed = pgp.encrypt(
        message=message,
        public_key=public_key,
        private_key=private_key,
        hash_algorithm="SHA-256",
        algorithm="AES-256",
        file_name=file_name,
        armor=True
    )
    
    # Should be encrypted (should start with -----BEGIN PGP MESSAGE-----)
    encrypted_text = str(encrypted_signed, "utf-8")
    asserts.assert_that(encrypted_text).contains("-----BEGIN PGP MESSAGE-----")
    
    # Decrypt and automatically verify with the private key
    decrypted = pgp.decrypt(
        encrypted_message=encrypted_signed,
        private_key=private_key,
        verify=True  # Automatically verify the signature
    )
    
    # Verify decryption worked
    asserts.assert_that(decrypted).is_equal_to(message)
    
    print("Sign and encrypt test passed successfully!")

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_pgp_sign_verify))
    _suite.addTest(unittest.FunctionTestCase(test_sign_then_encrypt))

    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())
