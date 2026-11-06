# Ported from python-xmlsig 0.1.6-0.1.7 (tests/ not in sdist), pinned to 0.1.7 (eed8913): tests/data/sign1-in.xml. See larky/src/main/resources/VENDORED.md.
SIGN1_IN_XML = """\
<?xml version="1.0" encoding="UTF-8"?>
<!-- 
XML Security Library example: Simple signature template file for sign1 example. 
-->
<Envelope xmlns="urn:envelope">
  <Data>
	Hello, World!
  </Data>
  <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
    <SignedInfo>
      <CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315" />
      <SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1" />
      <Reference URI="">
        <Transforms>
          <Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature" />
        </Transforms>
        <DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1" />
        <DigestValue></DigestValue>
      </Reference>
    </SignedInfo>
    <SignatureValue/>
    <KeyInfo>
	<KeyName/>
    </KeyInfo>
  </Signature>
</Envelope>"""