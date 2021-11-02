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