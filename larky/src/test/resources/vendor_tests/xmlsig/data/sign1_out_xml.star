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
        <DigestValue>9H/rQr2Axe9hYTV2n/tCp+3UIQQ=</DigestValue>
      </Reference>
    </SignedInfo>
    <SignatureValue>Mx4psIy9/UY+u8QBJRDrwQWKRaCGz0WOVftyDzAe6WHAFSjMNr7qb2ojq9kdipT8
Oub5q2OQ7mzdSLiiejkrO1VeqM/90yEIGI4En6KEB6ArEzw+iq4N1wm6EptcyxXx
M9StAOOa9ilWYqR9Tfx3SW1urUIuKYgUitxsONiUHBVaW6HeX51bsXoTF++4ZI+D
jiPBjN4HHmr0cbJ6BXk91S27ffZIfp1Qj5nL9onFLUGbR6EFgu2luiRzQbPuM2tP
XxyI7GZ8AfHnRJK28ARvBC9oi+O1ej20S79CIV7gdBxbLbFprozBHAwOEC57YgJc
x+YEjSjcO7SBIR1FiUA7pw==</SignatureValue>
    <KeyInfo>
	<KeyName>rsakey.pem</KeyName>
    </KeyInfo>
  </Signature>
</Envelope>"""