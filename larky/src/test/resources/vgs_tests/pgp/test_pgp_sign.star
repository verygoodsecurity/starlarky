load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//pgp", "pgp")

def test_pgp_sign_verify():
    """Test basic PGP signing and verification"""
    pgp = setup().pgp
    
    # Test key pair - do not use in production
    private_key = """..."""

    public_key = """..."""

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
    assert_that(signed_text).contains("-----BEGIN PGP MESSAGE-----")
    
    # Verify the signature
    verified = pgp.verify(
        signed_message=signed_message,
        public_key=public_key
    )
    
    # Verify the verification worked
    assert_that(verified).is_equal_to(True)
    
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
        assert_that(is_valid).is_equal_to(True)
    
    print("All signature tests passed successfully!")

def test_sign_then_encrypt():
    """Test signing and then encrypting with PGP"""
    pgp = setup().pgp
    
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
    assert_that(encrypted_text).contains("-----BEGIN PGP MESSAGE-----")
    
    # Decrypt and automatically verify with the private key
    decrypted = pgp.decrypt(
        encrypted_message=encrypted_signed,
        private_key=private_key,
        verify=True  # Automatically verify the signature
    )
    
    # Verify decryption worked
    assert_that(decrypted).is_equal_to(message)
    
    print("Sign and encrypt test passed successfully!")

def _test_all():
    test_pgp_sign_verify()
    test_sign_then_encrypt()

run_test(_test_all)