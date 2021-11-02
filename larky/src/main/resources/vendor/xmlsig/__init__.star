load("@stdlib//larky", larky="larky")

load("@vendor//xmlsig/algorithms", algorithms="algorithms")
load("@vendor//xmlsec/constants", constants="constants")
load("@vendor//xmlsec/ns", ns="ns")
load("@vendor//xmlsec/template", template="template")
load("@vendor//xmlsec/signature_context", SignatureContext="SignatureContext")


xmlsig = larky.struct(
    __name__='xmlsig',
    algorithms=algorithms,
    constants=constants,
    ns=ns,
    template=template,
    SignatureContext=SignatureContext
)