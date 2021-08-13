"""Unit tests for VaultModule.java using DefaultVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//cerebro/text/analyzer", pii_analyzer="pii_analyzer")

def _test_default_analyze_defaults_ok():
    # Arrange
    card_number = "4111111111111111"

    # Act
    analyzer_result = pii_analyzer.analyze(card_number)

    # Assert
    asserts.assert_that(analyzer_result.is_ok).is_true()

    found_entities = analyzer_result.unwrap()
    asserts.assert_that(len(found_entities)).is_equal_to(1)
    asserts.assert_that(found_entities[0].entity_type).is_equal_to('CREDIT_CARD')
    asserts.assert_that(found_entities[0].start).is_equal_to(0)
    asserts.assert_that(found_entities[0].end).is_equal_to(16)
    asserts.assert_that(found_entities[0].score).is_equal_to(1.0)

def _test_default_analyze_params_ok():
    # Arrange
    card_number = "4111111111111111"

    # Act
    analyzer_result = pii_analyzer.analyze(card_number, language='EN', entities=['CREDIT_CARD'], score_threshold=1.0)

    # Assert
    # asserts.assert_that(analyzer_result.is_ok).is_true()

    found_entities = analyzer_result.unwrap()
    asserts.assert_that(len(found_entities)).is_equal_to(1)
    asserts.assert_that(found_entities[0].entity_type).is_equal_to('CREDIT_CARD')
    asserts.assert_that(found_entities[0].start).is_equal_to(0)
    asserts.assert_that(found_entities[0].end).is_equal_to(16)
    asserts.assert_that(found_entities[0].score).is_equal_to(1.0)

def _test_default_analyze_unsupportedLanguage_error():
    # Arrange
    card_number = "4111111111111111"

    # Act
    analyzer_result = pii_analyzer.analyze(card_number, language='ES', entities=['CREDIT_CARD'], score_threshold=1.0)

    # Assert
    asserts.assert_fails(lambda: analyzer_result.unwrap(), "Provided language: ES is not currently supported.*")
    asserts.assert_that(analyzer_result.is_err).is_true()

def _test_default_analyze_unsupportedEntities_error():
    # Arrange
    card_number = "4111111111111111"

    # Act
    analyzer_result = pii_analyzer.analyze(card_number, language='EN', entities=['CRYPTO'], score_threshold=1.0)

    # Assert
    asserts.assert_fails(lambda: analyzer_result.unwrap(), "Requested PII entities: \\[CRYPTO\\] are not currently supported.*")
    asserts.assert_that(analyzer_result.is_err).is_true()

def _test_default_supported_languages_defaults_ok():
    # Arrange
    # Act
    supported_languages = pii_analyzer.supported_languages()

    # Assert
    asserts.assert_that(len(supported_languages)).is_equal_to(1)
    asserts.assert_that(supported_languages[0]).is_equal_to('EN')

def _test_default_supported_entities_validInput_ok():
    # Arrange
    language = 'EN'
    # Act
    result = pii_analyzer.supported_entities(language)

    # Assert
    asserts.assert_that(result.is_ok).is_true()
    supported_entities = result.unwrap()
    asserts.assert_that(len(supported_entities)).is_equal_to(1)
    asserts.assert_that(supported_entities[0]).is_equal_to('CREDIT_CARD')

def _test_default_supported_entities_unsupportedLanguage_ok():
    # Arrange
    language = 'ES'
    # Act
    result = pii_analyzer.supported_entities(language)

    # Assert
    asserts.assert_that(result.is_err).is_true()
    asserts.assert_fails(lambda: result.unwrap(), "Provided language: ES is not currently supported.*")

def _suite():
    _suite = unittest.TestSuite()

    # Analyze Tests
    _suite.addTest(unittest.FunctionTestCase(_test_default_analyze_defaults_ok))
    _suite.addTest(unittest.FunctionTestCase(_test_default_analyze_params_ok))
    _suite.addTest(unittest.FunctionTestCase(_test_default_analyze_unsupportedLanguage_error))
    _suite.addTest(unittest.FunctionTestCase(_test_default_analyze_unsupportedEntities_error))

    _suite.addTest(unittest.FunctionTestCase(_test_default_supported_languages_defaults_ok))
    _suite.addTest(unittest.FunctionTestCase(_test_default_supported_entities_validInput_ok))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
