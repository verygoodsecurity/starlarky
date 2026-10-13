# Ported from python-xmlsig 0.1.6-0.1.7, pinned to 0.1.6 (1836ccc) via vendor/xmlsig: src/xmlsig/__init__.py. See larky/src/main/resources/VENDORED.md.
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