load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//pgp", "pgp")

def test_pgp_module_basics():
    """Test that the PGP module is properly loaded and functions are available"""

    # Check that module provides algorithm info
    assert_that(pgp.get_supported_algorithms()).is_instance_of(str)
    assert_that(pgp.get_supported_algorithms()).contains("AES-256")
    assert_that(pgp.get_supported_algorithms()).contains("SHA-384")
    
    # Test hash algorithm conversion
    sha384 = pgp.get_hash_algorithm("SHA-384")
    assert_that(sha384).is_instance_of(int)
    assert_that(sha384).is_not_equal_to(0)

def test_pgp_encrypt_decrypt():
    """Test basic PGP encryption and decryption"""
    pgp = setup().pgp
    
    # Test with different algorithms
    algorithms = ["AES-128", "AES-192", "AES-256"]
    
    # Test key from BouncyCastle examples - do not use in production
    public_key = """..."""

    private_key = """..."""

    # Test message
    message = bytes("This is a test message for PGP encryption", "utf-8")
    
    # Test each algorithm
    for algo in algorithms:
        # Encrypt with the public key
        encrypted = pgp.encrypt(
            message=message,
            public_key=public_key,
            file_name="test.txt",
            armor=True,
            algorithm=algo
        )
        
        # Verify it's encrypted (should start with -----BEGIN PGP MESSAGE-----)
        encrypted_text = encrypted.decode("utf-8")
        assert_that(encrypted_text).contains("-----BEGIN PGP MESSAGE-----")
        
        # Decrypt with the private key
        decrypted = pgp.decrypt(
            encrypted_message=encrypted,
            private_key=private_key,
            passphrase=None
        )
        
        # Verify the decryption worked
        assert_that(decrypted).is_equal_to(message)
        
    # Test non-armored output
    binary_encrypted = pgp.encrypt(
        message=message,
        public_key=public_key,
        file_name="test.txt",
        armor=False,
        algorithm="AES-256"
    )
    
    # Binary output should not contain PGP header text
    try:
        binary_text = binary_encrypted.decode("utf-8")
        contains_header = "-----BEGIN PGP MESSAGE-----" in binary_text
    except UnicodeDecodeError:
        # This is expected for binary data
        contains_header = False
    
    assert_that(contains_header).is_equal_to(False)
    
    # But we should still be able to decrypt it
    decrypted_binary = pgp.decrypt(
        encrypted_message=binary_encrypted,
        private_key=private_key,
        passphrase=None
    )
    
    assert_that(decrypted_binary).is_equal_to(message)
    
    # Test with different message sizes
    large_message = bytes("A" * 10000, "utf-8")
    
    encrypted_large = pgp.encrypt(
        message=large_message,
        public_key=public_key,
        file_name="large.txt",
        armor=True
    )
    
    decrypted_large = pgp.decrypt(
        encrypted_message=encrypted_large,
        private_key=private_key,
        passphrase=None
    )
    
    assert_that(decrypted_large).is_equal_to(large_message)

def _test_all():
    test_pgp_module_basics()
    test_pgp_encrypt_decrypt()

run_test(_test_all)