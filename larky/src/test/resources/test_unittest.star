load("@stdlib/asserts", "asserts")

def success():
    asserts.assert_that(1).is_equal_to(1)
    asserts.assert_(1 == 1)
    asserts.assert_true(1 == 1)
    asserts.assert_false(1 == 2)


def failure():
    asserts.assert_false(True)
    asserts.assert_that(2).is_equal_to(1)


def suite():
    suite = unittest.TestSuite()
    suite.addTest(unittest.FunctionTestCase(success))
    suite.addTest(unittest.expectedFailure(
        unittest.FunctionTestCase(failure)
    ))
    return suite


runner = unittest.TextTestRunner()
runner.run(suite())