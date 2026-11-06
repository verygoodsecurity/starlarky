# Ported from python-xmlsig 0.1.6-0.1.7 (src/ identical), pinned to 0.1.6 (1836ccc): src/xmlsig/__init__.py. See larky/src/main/resources/VENDORED.md.
load("@stdlib//larky", larky="larky")

load("@vendor//xmlsig/algorithms", algorithms="algorithms")
load("@vendor//xmlsig/constants", constants="constants")
load("@vendor//xmlsig/ns", ns="ns")
load("@vendor//xmlsig/template", template="template")
load("@vendor//xmlsig/signature_context", SignatureContext="SignatureContext")


xmlsig = larky.struct(
    __name__='xmlsig',
    algorithms=algorithms,
    constants=constants,
    ns=ns,
    template=template,
    SignatureContext=SignatureContext
)