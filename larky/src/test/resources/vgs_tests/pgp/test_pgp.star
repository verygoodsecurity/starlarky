# load("asserts", "assert_that", "eq", "truth")
# load("vgstest_utils", setup="vgs_setup", run_test="run_test")

# def test_pgp_module_basics():
#     """Test that the PGP module is properly loaded and functions are available"""
#     pgp = setup().pgp
    
#     # Check that module provides algorithm info
#     assert_that(pgp.get_supported_algorithms()).is_instance_of(str)
#     assert_that(pgp.get_supported_algorithms()).contains("AES-256")
#     assert_that(pgp.get_supported_algorithms()).contains("SHA-384")
    
#     # Test hash algorithm conversion
#     sha384 = pgp.get_hash_algorithm("SHA-384")
#     assert_that(sha384).is_instance_of(int)
#     assert_that(sha384).is_not_equal_to(0)

# def test_pgp_encrypt_decrypt():
#     """Test basic PGP encryption and decryption"""
#     pgp = setup().pgp
    
#     # Test with different algorithms
#     algorithms = ["AES-128", "AES-192", "AES-256"]
    
#     # Test key from BouncyCastle examples - do not use in production
#     public_key = """-----BEGIN PGP PUBLIC KEY BLOCK-----
# Version: BCPG v1.54

# mQENBF0Rqv4BCACdh5XimVCC0+lLGOu2uRJSV72VnExdp78Z5JQ5oe5Ph5aZFJxV
# yQJdHpU/nvzYnpdhH/5ysAY3h1x9PvXvnZ0qDNR4XUvtQ95jxj+qhGnZsXMjDMI9
# rTC+zW38QJO1yGVfXyPO3pUMU1ExqHuM8kP+4HnWbGgmn9QJegXkk0exdE0+3GFD
# SFQF3NdD3q5pxaUXcm+FI/3A5qTVY+CzJbFUFxQ6kC18jQCchVuCf3zL6zxbqrju
# z5fH3GhEp5PtJDY5I7f9lGgLVCXgCEzG8V9/62wLh9rrEtwXCNGlM44w6R2lmH5e
# l69MXEpX1XCsUHAfsQvevcYOlp1msjHpHjzDABEBAAG0D1BHUCBUZXN0IChUZXN0
# KYkBHAQQAQIABgUCXRGq/gAKCRCu9FyJbB4bF3zOB/0eVgMc1VD2CQE22mnHjWJ+
# n7CHdIH+dTqcJ14+bPMwhJq5XR8CvPZHxOUYcnIPcx2Sn7Gwkwji+lseBntxJbLr
# fYAf1zOxB7NiZRwT5VazUSW3XU7yQ0tK9nC3rbUlxnp+a0+lZQMGu0/4mVL8/kKj
# 9yIczLXpGRKidaO4bBOMgDlvXH6qr5AFRZlkOIg7iY+UJb8gzJF48CDcvvOdFdUS
# TBxb3sDvRt1h6i1qMiRHEEaxd/+lpiXTV8DEwJ5qmvK8JyX4PmJkXxnD/nz0jyzl
# yPZobn7d8nTtJvJLBBeyGwQjLWVFJe4Mf4ij+7jUkZBMXwQT6Dx8pZ2ywE87Sihf
# =sWz7
# -----END PGP PUBLIC KEY BLOCK-----"""

#     private_key = """-----BEGIN PGP PRIVATE KEY BLOCK-----
# Version: BCPG v1.54

# lQOsBF0Rqv4BCACdh5XimVCC0+lLGOu2uRJSV72VnExdp78Z5JQ5oe5Ph5aZFJxV
# yQJdHpU/nvzYnpdhH/5ysAY3h1x9PvXvnZ0qDNR4XUvtQ95jxj+qhGnZsXMjDMI9
# rTC+zW38QJO1yGVfXyPO3pUMU1ExqHuM8kP+4HnWbGgmn9QJegXkk0exdE0+3GFD
# SFQF3NdD3q5pxaUXcm+FI/3A5qTVY+CzJbFUFxQ6kC18jQCchVuCf3zL6zxbqrju
# z5fH3GhEp5PtJDY5I7f9lGgLVCXgCEzG8V9/62wLh9rrEtwXCNGlM44w6R2lmH5e
# l69MXEpX1XCsUHAfsQvevcYOlp1msjHpHjzDABEBAAH/AwMC7l0/PgTCq4Rga35E
# FD+dTWdPg0O4R54E254X6Xp0nCOsYFhRY2NcQUdVJYLUwf2dQm3oZUOEHRsQUVH2
# wQnkzlKcHAEDpL9uPpeg1Km/z0xRo/eFfE7v3+tkq9rXGrNnbEU2TvJ/eGP0vxeA
# vBf/r0rUQZGS57E1vXEDzDe1bmZ4WM7X11v7EpXKAszHPdM9v9EufnWtN+l0RGr0
# RI8Tao8uyAu1yuJ2kLkXO6rDtYDOH3RJqQD5n6QYtpUAzW3FGcG1BN/ebHH/5NF1
# rnlqtBDRY/N8lwmbGtRz70GJgLzm8PJLMYztMydqHvXlcjoQAJ1h3iADgOlvzk4J
# Pzqe8tFbcQ9XFvaxsXBV9j8yhhK7K/gCwfBGmX9rY9fGGDwdF3PXocrIEGkr+0I3
# BKGMGnz2tXBXnS2l8X0e8J5E53Y0A61hPjbQAz4GHBjMQAC8cUTbRrMzrXvfSWU+
# A/wRsZ2rIPMbTyxQsFVRkZUk3c3hPBTlnO2/3PcW3RrO72PzX/y3AAcn2R+7PFhb
# o6Gy+QP9IB29aDGDUKipA4+ZB/7rQQrNc6G3LJZmqOtQBQ0fXHtYIg1v1ULFbJUh
# Z6zQu3HhPK27u9T7ABGlrDsWHvQOZZ17JJaPsYqkbXrOG90khTSuiQ6RQx30/u1U
# g7rYCbqxRhCPzj7ULNK3ByQPEpIgbNfNXHB38SPFuZRnUOLQS+ZKzilM0vXwJMZV
# ZNnjdBVqx+li4pZkwdgPkZRZZPV1q6uQG4PftFd76dXrNZr9W+h6J2HTUTXnYNAf
# JBKPLm9LDuUC0YxJnMo6FpLkZQexUZE5fNOjJ/j9eYM4sc2l8s6JiQcNvQGG/PD7
# rXxrWGq3n9yt+Sck21OdZg4OxbYI3CjNEU8htA9QR1AgVGVzdCAoVGVzdCmJARwE
# EAECAAYFAl0Rqv4ACgkQrvRciWweGxd8zgf9HlYDHNVQ9gkBNtppx41ifp+wh3SB
# /nU6nCdePmzzMISauV0fArz2R8TlGHJyD3Mdkp+xsJMI4vpbHgZ7cSWy632AH9cz
# sQezYmUcE+VWs1Elt11O8kNLSvZwt621JcZ6fmtPpWUDBrtP+JlS/P5Co/ciHMy1
# 6RkSonWjuGwTjIA5b1x+qq+QBUWZZDiIO4mPlCW/IMyRePAg3L7znRXVEkwcW97A
# 70bdYeotajIkRxBGsXf/paYl01fAxMCeapryvcl+D5iZF8Zw/589I8s5cj2aG5+3
# fJ07SbwSwRsEIy1lRSXuDH+Io/u41JGQsAtJKF8=
# =BVow
# -----END PGP PRIVATE KEY BLOCK-----"""

#     # Test message
#     message = bytes("This is a test message for PGP encryption", "utf-8")
    
#     # Test each algorithm
#     for algo in algorithms:
#         # Encrypt with the public key
#         encrypted = pgp.encrypt(
#             message=message,
#             public_key=public_key,
#             file_name="test.txt",
#             armor=True,
#             algorithm=algo
#         )
        
#         # Verify it's encrypted (should start with -----BEGIN PGP MESSAGE-----)
#         encrypted_text = encrypted.decode("utf-8")
#         assert_that(encrypted_text).contains("-----BEGIN PGP MESSAGE-----")
        
#         # Decrypt with the private key
#         decrypted = pgp.decrypt(
#             encrypted_message=encrypted,
#             private_key=private_key,
#             passphrase=None
#         )
        
#         # Verify the decryption worked
#         assert_that(decrypted).is_equal_to(message)
        
#     # Test non-armored output
#     binary_encrypted = pgp.encrypt(
#         message=message,
#         public_key=public_key,
#         file_name="test.txt",
#         armor=False,
#         algorithm="AES-256"
#     )
    
#     # Binary output should not contain PGP header text
#     try:
#         binary_text = binary_encrypted.decode("utf-8")
#         contains_header = "-----BEGIN PGP MESSAGE-----" in binary_text
#     except UnicodeDecodeError:
#         # This is expected for binary data
#         contains_header = False
    
#     assert_that(contains_header).is_equal_to(False)
    
#     # But we should still be able to decrypt it
#     decrypted_binary = pgp.decrypt(
#         encrypted_message=binary_encrypted,
#         private_key=private_key,
#         passphrase=None
#     )
    
#     assert_that(decrypted_binary).is_equal_to(message)
    
#     # Test with different message sizes
#     large_message = bytes("A" * 10000, "utf-8")
    
#     encrypted_large = pgp.encrypt(
#         message=large_message,
#         public_key=public_key,
#         file_name="large.txt",
#         armor=True
#     )
    
#     decrypted_large = pgp.decrypt(
#         encrypted_message=encrypted_large,
#         private_key=private_key,
#         passphrase=None
#     )
    
#     assert_that(decrypted_large).is_equal_to(large_message)

# def _test_all():
#     test_pgp_module_basics()
#     test_pgp_encrypt_decrypt()

# run_test(_test_all)