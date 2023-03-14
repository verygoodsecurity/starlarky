SIGN1_OUT_XML = """\
<?xml version="1.0" encoding="UTF-8"?>
<!--
XML Security Library example: Signed file (sign1 example).
-->
<Envelope xmlns="urn:envelope">
  <Data>
	Hello, World!
  </Data>
  <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
    <SignedInfo>
      <CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/>
      <SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
      <Reference URI="">
        <Transforms>
          <Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
        </Transforms>
        <DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
        <DigestValue>mWe97TSz4IlTlrGBw7ZMr2PJZxc=</DigestValue>
      </Reference>
    </SignedInfo>
    <SignatureValue>WuEPfJS4Zc4r2pQsXSs9dvwAMJ23fKaLJI7u1aM5npIWzYtecFq2xuA2Ajy+jV0n
L0X+/TuH9aDHpvR+bYa0h9hqWPtrsvYdmZ2lwyl4BONJJkvJUlirMJZ8oKl24fXn
SVEC8dlSQShUSWc0Nq900DpaVgwexUhL7UIjnL1hqSHLa82qTfYCka9KjPKgskOE
guRF1ep5rDLUbE0YJgAbslNPt9EZCLObSdTCBq5j4U+o0dXGj9PT2Eb4vPFlDWhG
61sN/oOLM3ZQgt5SX/UnEqZniWT5JeBERzMcoKa/HzoKiUtPTQ9F6g3tw0vVkQhq
YGDm5Fxl4Lj3PtYT7ShKPg==</SignatureValue>
    <KeyInfo>
	<KeyName>rsakey.pem</KeyName>
    </KeyInfo>
  </Signature>
</Envelope>"""