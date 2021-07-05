"""Unit tests for test_jsonpath.star"""
load("@stdlib//larky", "larky")
load("@stdlib//types", types="types")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//jsonpath_ng", jsonpath_ng="jsonpath_ng")


FIXTURE = {
    "store": {
    "book": [
      { "category": "reference",
        "author": "Nigel Rees",
        "title": "Sayings of the Century",
        "price": 8.95
      },
      { "category": "fiction",
        "author": "Evelyn Waugh",
        "title": "Sword of Honour",
        "price": 12.99
      },
      { "category": "fiction",
        "author": "Herman Melville",
        "title": "Moby Dick",
        "isbn": "0-553-21311-3",
        "price": 8.99
      },
      { "category": "fiction",
        "author": "J. R. R. Tolkien",
        "title": "The Lord of the Rings",
        "isbn": "0-395-19395-8",
        "price": 22.99
      }
    ],
    "bicycle": {
      "color": "red",
      "price": 19.95
    }
  }
}


def _test_simple_jsonpath():
    expr = jsonpath_ng.parse("$.store.book[0].author")
    result = expr.find(FIXTURE)
    asserts.assert_that(result.value).is_equal_to("Nigel Rees")

    asserts.assert_fails(lambda : jsonpath_ng.parse("$.store.*"), ".*?ParsingException")
    asserts.assert_fails(lambda : jsonpath_ng.parse("$.store.book[*].author"), ".*?ParsingException")
    
    expr2 = jsonpath_ng.parse("$.store.drink")
    asserts.assert_fails(lambda : expr2.find(FIXTURE), ".*?ParsingException")


def _test_update_jsonpath():
    # update field value 
    expr = jsonpath_ng.parse("$.store.book[0].author")
    result = expr.update(FIXTURE, "William Cavendish")
    asserts.assert_that(result.value["store"]["book"][0]["author"]).is_equal_to("William Cavendish")

    # add field
    expr2 = jsonpath_ng.parse("$.store.bicycle.type")
    result = expr2.update(FIXTURE, "Mountain Bike")
    print('Updated json:', result.value)
    asserts.assert_that(result.value["store"]["bicycle"]["type"]).is_equal_to("Mountain Bike")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_simple_jsonpath))
    _suite.addTest(unittest.FunctionTestCase(_test_update_jsonpath))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
