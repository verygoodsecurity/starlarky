load("@stdlib/larky", "larky")
load("@vendor//option/result", safe="safe")
load("@vgs//cerebro", _cerebro="cerebro")

pii_analyzer = larky.struct(
  analyze = safe(_cerebro.TextAnalyzer.analyze),
  supported_entities = safe(_cerebro.TextAnalyzer.supported_entities),
  supported_languages = _cerebro.TextAnalyzer.supported_languages)
