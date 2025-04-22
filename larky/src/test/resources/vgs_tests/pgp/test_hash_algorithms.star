# load("asserts", "assert_that", "eq", "truth")
# load("vgstest_utils", setup="vgs_setup", run_test="run_test")

# def test_hash_algorithms():
#     """Test the hash algorithm string to constant conversion"""
#     pgp = setup().pgp
    
#     # Define test cases: algorithm name and expected output (non-zero)
#     test_cases = {
#         "SHA-1": "SHA1",
#         "SHA-224": "SHA224",
#         "SHA-256": "SHA256",
#         "SHA-384": "SHA384",
#         "SHA-512": "SHA512",
#         # Test different case and formats
#         "sha1": "SHA1",
#         "sha-1": "SHA1",
#         "SHA384": "SHA384"
#     }
    
#     # Test each algorithm
#     for algo_name, expected_name in test_cases.items():
#         # Get the algorithm ID
#         algo_id = pgp.get_hash_algorithm(algo_name)
        
#         # Verify it's a valid ID (non-zero)
#         assert_that(algo_id).is_instance_of(int)
#         assert_that(algo_id).is_not_equal_to(0)
        
#         # Test that different formats work (case insensitive)
#         if algo_name != expected_name:
#             expected_id = pgp.get_hash_algorithm(expected_name)
#             assert_that(algo_id).is_equal_to(expected_id)
    
#     # Test invalid algorithm handling
#     try:
#         pgp.get_hash_algorithm("UNKNOWN-HASH")
#         assert_that(True).is_equal_to(False)  # Should not reach here
#     except Exception as e:
#         # Should throw an exception with a message containing supported algorithms
#         error_msg = str(e)
#         assert_that(error_msg).contains("Unsupported hash algorithm")
#         assert_that(error_msg).contains("SHA-384")

# def _test_all():
#     test_hash_algorithms()

# run_test(_test_all)