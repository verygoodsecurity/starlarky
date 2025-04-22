load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//pgp", "pgp")
load("@vgs//vault", vault="vault")

def _test_encryption_algorithms():
    """Test encryption with different algorithms"""
    
    # Test key from BouncyCastle examples - do not use in production
    public_key = """..."""

    private_key = """..."""

    # Test with all supported symmetric algorithms
    algorithms = [
        "AES-128", "AES128",
        "AES-192", "AES192",
        "AES-256", "AES256",
        "BLOWFISH",
        "CAMELLIA-128", "CAMELLIA128",
        "CAMELLIA-192", "CAMELLIA192",
        "CAMELLIA-256", "CAMELLIA256",
        "TWOFISH"
    ]
    
    message = bytes("Test encryption with different algorithms", "utf-8")
    
    # Test each algorithm
    for algo in algorithms:
        print(f"Testing algorithm: {algo}")
        
        # Encrypt with the algorithm
        encrypted = pgp.encrypt(
            message=message,
            public_key=public_key,
            file_name="test.txt",
            armor=True,
            algorithm=algo
        )
        
        # Decrypt and verify
        decrypted = pgp.decrypt(
            encrypted_message=encrypted,
            private_key=private_key,
            passphrase=None
        )
        
        # Verify the decryption worked
        asserts.assert_that(decrypted).is_equal_to(message)
    
    # Test invalid algorithm handling
    try:
        pgp.encrypt(
            message=message,
            public_key=public_key,
            file_name="test.txt",
            armor=True,
            algorithm="UNKNOWN-ALGORITHM"
        )
        assert_that(True).is_equal_to(False)  # Should not reach here
    except Exception as e:
        # Should throw an exception with a message containing supported algorithms
        error_msg = str(e)
        asserts.assert_that(error_msg).contains("Unsupported encryption algorithm")
        asserts.assert_that(error_msg).contains("AES-256")

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_encryption_algorithms))

_runner = unittest.TextTestRunner()
_runner.run(_suite())