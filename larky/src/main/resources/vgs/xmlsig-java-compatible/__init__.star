load("@stdlib//larky", larky="larky")

load("@vgs//xmlsig-java-compatible/algorithms", algorithms="algorithms")
load("@vgs//xmlsig-java-compatible/constants", constants="constants")
load("@vgs//xmlsig-java-compatible/ns", ns="ns")
load("@vgs//xmlsig-java-compatible/template", template="template")
load("@vgs//xmlsig-java-compatible/signature_context", SignatureContext="SignatureContext")


xmlsig = larky.struct(
    __name__='xmlsig',
    algorithms=algorithms,
    constants=constants,
    ns=ns,
    template=template,
    SignatureContext=SignatureContext
)