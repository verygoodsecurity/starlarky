load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//pgp", "pgp")

def test_hash_algorithms():
    """Test the hash algorithm string to constant conversion"""

    # Define test cases: algorithm name and expected output (non-zero)
    test_cases = {
        "SHA-1": "SHA1",
        "SHA-224": "SHA224",
        "SHA-256": "SHA256",
        "SHA-384": "SHA384",
        "SHA-512": "SHA512",
        # Test different case and formats
        "sha1": "SHA1",
        "sha-1": "SHA1",
        "SHA384": "SHA384"
    }
    
    # Test each algorithm
    for algo_name, expected_name in test_cases.items():
        # Get the algorithm ID
        algo_id = pgp.get_hash_algorithm(algo_name)
        
        # Verify it's a valid ID (non-zero)
        asserts.assert_that(algo_id).is_instance_of(int)
        asserts.assert_that(algo_id).is_not_equal_to(0)
        
        # Test that different formats work (case insensitive)
        if algo_name != expected_name:
            expected_id = pgp.get_hash_algorithm(expected_name)
            asserts.assert_that(algo_id).is_equal_to(expected_id)
    
def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_hash_algorithms))

    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())
