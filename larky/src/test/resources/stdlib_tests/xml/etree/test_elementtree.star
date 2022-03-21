load("@stdlib//builtins", "builtins")
load("@stdlib//io/StringIO", "StringIO")
load("@stdlib//larky", "larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//re", re="re")
load("@stdlib//sets", sets="sets")
load("@stdlib//types", "types")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")

load("@stdlib//xml/etree/ElementTree", ElementTree="ElementTree")
load("@vendor//elementtree/SimpleXMLTreeBuilder", SimpleXMLTreeBuilder="SimpleXMLTreeBuilder")


def _bytes(s):
    return builtins.bytes(s, encoding='utf-8')


def parse(text, parser=None, tree_factory=ElementTree.ElementTree):
    #f = BytesIO(text) if isinstance(text, bytes) else StringIO(text)
    f = StringIO(text)
    return ElementTree.parse(f, parser=parser, tree_factory=tree_factory)


def _rootstring(self, tree):
    return ElementTree.tostring(
        tree.getroot())\
        .replace(_bytes(' '), _bytes(''))\
        .replace(_bytes('\n'), _bytes(''))


def _test_elementtree():
    f = StringIO(
        '<doc><one>One</one><two>Two</two>hm<three>Three</three></doc>'
    )
    parser = SimpleXMLTreeBuilder.TreeBuilder()
    # doc = ElementTree.ElementTree(file=f)
    doc = parse(
        '<doc><one>One</one><two>Two</two>hm<three>Three</three></doc>',
        parser
    )
    # doc = parse("""<root>
    #          <element key='value'>text</element>
    #          <element>text</element>tail
    #          <empty-element/>
    #       </root>""", parser
    # )
    root = doc.getroot()
    asserts.eq(3, operator.length_hint(root))
    asserts.eq('one', operator.getitem(root, 0).tag)
    asserts.eq('two', operator.getitem(root, 1).tag)
    asserts.eq('three', operator.getitem(root, 2).tag)


def _test_xpath():

    parser = SimpleXMLTreeBuilder.TreeBuilder()
    tree = parse(
        '<doc level="A"><one updated="Y">One</one><two updated="N">Two</two>hm<three>Three</three></doc>',
        parser
    )
    root = tree.getroot()
    # test top-level elements and children
    asserts.eq(1, len(root.findall(".")))
    asserts.eq(3, len(root.findall("./")))
    asserts.eq(3, len(root.findall("./*")))
    # test basic expression with tag and text
    asserts.eq('one', root.findall("./one")[0].tag)
    asserts.eq('One', root.findall("./one")[0].text)
    # test attrib
    asserts.eq('A', root.findall(".")[0].attrib['level'])
    asserts.eq('Y', root.findall("./")[0].attrib['updated'])
    asserts.eq('N', root.findall("./two")[0].attrib['updated'])


    tree = parse(
        '<data><actress name="Jenny"><tv>5</tv><born>1989</born></actress><actor name="Tim"><tv updated="Yes">3</tv><born>1990</born></actor><actor name="John"><film>8</film><born>1984</born></actor></data>',
        parser
    )
    root = tree.getroot()
    # test grand-children
    asserts.eq(4, len(root.findall("./actor/*")))
    asserts.eq('5', root.findall("./actress/tv")[0].text)
    # test all descendant
    asserts.eq(9, len(root.findall(".//")))
    asserts.eq(2, len(root.findall(".//tv")))
    asserts.eq(4, len(root.findall("./actor//")))
    asserts.eq(0, len(root.findall("./actress//film")))
    # test children in certain position
    asserts.eq(0, len(root.findall(".//tv[2]")))
    asserts.eq(2, len(root.findall(".//tv[1]")))
    asserts.eq(1, len(root.findall("./actress[1]")))
    asserts.eq('1984', root.findall("./actor[2]/born")[0].text)
    asserts.eq('8', root.findall("./actor/film[1]")[0].text)
    # test search by attrib
    asserts.eq('3', root.findall("./actor/*[@updated='Yes']")[0].text)
    asserts.eq('actress', root.findall(".//*[@name='Jenny']")[0].tag)


def normalize_newlines(s):
    return re.sub(r'(\r?\n)+', '\n', s)


def normalize_ws(s):
    _s = normalize_newlines(s)
    return ''.join([l.strip() for l in _s.split('\n')])
    # return re.sub(r'(\s|\x0B)+', replace, s).strip()


def _test_update_and_serialize():
    parser = SimpleXMLTreeBuilder.TreeBuilder()
    data = normalize_ws("""
    <data xmlns:x="http://example.com/ns/foo">nonetag
        <teacher name="Jenny">
            <born>1983</born>
        </teacher>teachertail
        <x:p/>ptail
        <student name="Tim">
            <performance>
                <Grade>A+</Grade>
            </performance>
            <info>
                <born>2005</born>
            </info>
        </student>
        <student name="John">
            <performance>
                <Grade>B</Grade>
            </performance>
            <info>
                <born>2004</born>birthtail
            </info>infotail
        </student>
    </data>""")
    tree = parse(data, parser)
    root = tree.getroot()

    # test update node text
    root.findall(".//*[@name='John']/performance/Grade")[0].text = 'A-'
    c = ElementTree.Comment('some comment')
    pi = ElementTree.ProcessingInstruction('Here are instuctions')

    root.append(c)
    root.append(pi)

    # Order of nodes no longer reversed, append() now appends instead of prepending
    expected_xml = ''.join(['<?xml version="1.0" encoding="utf-8"?>\n',
    '<data xmlns:x="http://example.com/ns/foo">nonetag<teacher name="Jenny">',
    '<born>1983</born></teacher>teachertail<x:p />ptail<student name="Tim">',
    '<performance><Grade>A+</Grade></performance><info><born>2005</born>',
    '</info></student><student name="John"><performance><Grade>A-</Grade>',
    '</performance><info><born>2004</born>birthtail</info>infotail</student>',
    '<!--some comment--><?Here are instuctions?></data>'
   ])
    actual_xml = ElementTree.tostring(root,  encoding ='utf-8', xml_declaration=True)
    asserts.eq(expected_xml, actual_xml)
    # print('to string:', ElementTree.tostring(root))
    # test serialize on subelement and update attribute
    root.findall('.//')[2].set("name", "Jim")
    actual = ElementTree.tostring(root.findall('.//')[2])
    expected = '<student name="Jim"><performance><Grade>A+</Grade></performance><info><born>2005</born></info></student>'
    # print("expected: ", expected)
    # print("  actual: ", actual)
    asserts.eq(expected, actual)

def _test_append_and_flatten():
  first = ElementTree.Element('First')
  second = first.makeelement('Second',{})
  first.append(second)
  third = first.makeelement('Third',{})
  first.append(third)
  fourth = first.makeelement('Fourth',{})
  first.append(fourth)

  result = ElementTree.tostring(first)
  asserts.assert_that(result).is_not_equal_to('<First><Fourth /><Third /><Second /></First>')
  asserts.assert_that(result).is_equal_to('<First><Second /><Third /><Fourth /></First>')

def _test_wsse_signed_payload():

    namespaces = {
        'soap-env': "http://schemas.xmlsoap.org/soap/envelope/",
        'wsse': "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
        'ds': "http://www.w3.org/2000/09/xmldsig#",
        'ns0': "http://schemas.mastercard.com.chssecure/2011/01",
        "ns1": "http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request",
    }

    parser = SimpleXMLTreeBuilder.TreeBuilder()
    data = """\
    <?xml version='1.0' encoding='utf-8'?>
    <soap-env:Envelope
      xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/" soap-env:mustUnderstand="1">
      <soap-env:Header
        xmlns:wsa="http://www.w3.org/2005/08/addressing">
        <wsa:Action>http://schemas.mastercard.com.chssecure/2011/01/MasterCardCHSService/GetBenefitInformation</wsa:Action>
        <wsa:MessageID>urn:uuid:2cdb9e0c-6a7d-419b-b243-ebd76866cec0</wsa:MessageID>
        <wsa:To>https://X.mastercard.com/X/ALB/CHSSecureWebService</wsa:To>
        <wsse:Security
          xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
          <Signature
            xmlns="http://www.w3.org/2000/09/xmldsig#">
            <SignedInfo>
              <CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
              <SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha512"/>
              <Reference URI="#id-46957075-bddd-460e-b4b1-062bfb2b52dc">
                <Transforms>
                  <Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                </Transforms>
                <DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha512"/>
                <DigestValue>bNc+C8QVxuJPmZq51OywslTN7MJQ5xBGkPB7TAEuFtqeSc1SdJxSZBx943B3lDoH
        9ENcoKCnfHPE7bXQ9moqAA==</DigestValue>
              </Reference>
              <Reference URI="#id-b39fb7e5-43c8-4904-913f-5ce8fe87eb8d">
                <Transforms>
                  <Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                </Transforms>
                <DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha512"/>
                <DigestValue>3wPnIvcNM8upEAQJCE/5q2tTdLv5FMK2V8xWwTIFRKcFD5qmv71Ztg15H/ENFi3B
        AWlI9QoDEse6vJMX7b5LPQ==</DigestValue>
              </Reference>
            </SignedInfo>
            <SignatureValue>V77yMeUfV8GODtwCRqAH2ixFMhvMeikTvT/xG4C4e/V+pHnOq3laG5JY4byGxGGh
        cixsLJznd9c+J3S5awNI5Zj63Kr7mdG5sCGLiiSzwJN7KvnjR2m4Qk34H1tguzLa
        xYbL64m7WZBwPvV2ps4wv96q5uiPr5ey4Gvoi8Wsc0RPr0FRrUjd34E6R8xKJIYa
        xiupg2t/faArbZIJEn3F4v1k4jyP9o9UQlBjquGE8qFpbVUaTRx//iMObY5qv6Wg
        flZ2iKgdg/qSxL336pDsutMkNfsLx3iVu50/QQ7xm4xmKsWujpiDDppWQay9/gRY
        g7xw7dmqB1c/9yrFdXJDug==</SignatureValue>
            <KeyInfo>
              <wsse:SecurityTokenReference>
                <X509Data>
                  <X509IssuerSerial>
                    <X509IssuerName>CN=MasterCard ITF Messages Signing Sub CA G2,OU=Global Information Security,O=MasterCard Worlwide,C=BE</X509IssuerName>
                    <X509SerialNumber>1095231617403928779</X509SerialNumber>
                  </X509IssuerSerial>
                  <X509Certificate>MIID3jCCAsagAwIBAgIIDzMLWm4jQMswDQYJKoZIhvcNAQELBQAwgYUxCzAJBgNV
        BAYTAkJFMRwwGgYDVQQKExNNYXN0ZXJDYXJkIFdvcmx3aWRlMSQwIgYDVQQLExtH
        bG9iYWwgSW5mb3JtYXRpb24gU2VjdXJpdHkxMjAwBgNVBAMTKU1hc3RlckNhcmQg
        SVRGIE1lc3NhZ2VzIFNpZ25pbmcgU3ViIENBIEcyMB4XDTIxMDIyMzEwNDAyOFoX
        DTI0MDIyMzEwNDAyOFoweTELMAkGA1UEBhMCVVMxETAPBgNVBAgMCE5ldyBZb3Jr
        MRkwFwYDVQQKDBBpbkNvbnRyb2wtUmV0YWlsMR8wHQYDVQQLDBZpbmNvbnRyb2xt
        dGYtcmV0YWlsYXBpMRswGQYDVQQDDBJFeHRlbmQtRW50ZXJwcmlzZXMwggEiMA0G
        CSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQChkMYrphLmbXRJd7whR3LKdlQZHY4f
        RVHQgM4pvQJ9ORADRwdh52z7JYke9phvR+DBuYmEhUAkSr1Nknp30SGm1lTLk62d
        fMJ5x0EJTSEMXh60X8tqjubHIhyFfx312g7oas5sVDmSzXyQ8Fu5BQboTPa54+xV
        jNiiUwO5U1kBcOWRvxpcKF54SqtwOA3FTHaF7UZ/0rkJAMv1HEq0DNSiJJXtia5x
        MYex9+QXxDwOcz9zPf6n0cBOXTEIID40BDkWI+PVI29UPc94HZoDtbPwPLipvh2t
        l7wGQGR5VbnVuprUClvcvYyw2JHX1JfqcEtEDCNtsV0YayCU/cfxNRV5AgMBAAGj
        XTBbMA4GA1UdDwEB/wQEAwIAgDAJBgNVHRMEAjAAMB0GA1UdDgQWBBRFMN8wCNA1
        g/CuroxUDQ766EbWOTAfBgNVHSMEGDAWgBRbPEBSD+5PgOubatSG2Ytg3H9YVTAN
        BgkqhkiG9w0BAQsFAAOCAQEAks3QIb77EU5dYig0PWjnXTgrBEVeIgZzReh/r1Ub
        f9JQCGw301QhVWAWV9ZVAwyk7p+6G83kSwdbI+SYgTP9WW0sGWtsnYvo3VxaDCnO
        /Tnj8Hl+H7vdc9TKmas/xtE05ulZ97oeY9s/NDGgEoYRm5qvCiBH917CW0tSM5Zx
        3GOwmvSwtVKSsBLrNWwgKU1SVP5Q41WrNbkTW6mCAAPWF3UtQgTjdviNpIyOIALp
        DI3mxMKJ7Fef6FiJ+R9875UhcaPZbGksHXiBeFpSN2dcv1FMhdqQh3/qMMEJH5+Y
        bgr71Xf9GIgujVYNPzJxhHs3BWORX1O4gn/BprbZ7LmmNg==</X509Certificate>
                </X509Data>
              </wsse:SecurityTokenReference>
            </KeyInfo>
          </Signature>
          <wsu:Timestamp
            xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="id-b39fb7e5-43c8-4904-913f-5ce8fe87eb8d">
            <wsu:Created>2021-02-09T20:57:37Z</wsu:Created>
            <wsu:Expires>2021-02-09T21:57:37Z</wsu:Expires>
          </wsu:Timestamp>
        </wsse:Security>
      </soap-env:Header>
      <soap-env:Body
        xmlns:ns0="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" ns0:Id="id-46957075-bddd-460e-b4b1-062bfb2b52dc">
        <ns0:GetBenefitInformation
          xmlns:ns0="http://schemas.mastercard.com.chssecure/2011/01">
          <ns0:benefitInfoReq>
            <ns1:CardNumber
              xmlns:ns1="http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request">0000000000000000
            </ns1:CardNumber>
            <ns2:ServiceProviderCode
              xmlns:ns2="http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request">01
            </ns2:ServiceProviderCode>
            <ns3:SrcSysCode
              xmlns:ns3="http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request">X
            </ns3:SrcSysCode>
          </ns0:benefitInfoReq>
        </ns0:GetBenefitInformation>
      </soap-env:Body>
    </soap-env:Envelope>"""
    tree = parse(data, parser)
    root = tree.getroot()
    # print(root.findall('./soap-env:Envelope/soap-env:Header/' +
    #                     'wsse:Security/ds:Signature/ds:KeyInfo/' +
    #                     'wsse:SecurityTokenReference/ds:X509Data/' +
    #                     'ds:X509IssuerSerial/ds:X509IssuerName', namespaces=namespaces))
    # print(root.findall('./soap-env:Envelope/soap-env:Header/' +
    #                     'wsse:Security/Signature/KeyInfo/' +
    #                     'wsse:SecurityTokenReference/X509Data/' +
    #                     'X509IssuerSerial/X509IssuerName', namespaces=namespaces))
    # print(root.findall('.'))
    issuer = root.findall(''.join([
            './soap-env:Header/',
            'wsse:Security/ds:Signature/ds:KeyInfo/',
            'wsse:SecurityTokenReference/ds:X509Data/',
            'ds:X509IssuerSerial/ds:X509IssuerName'
        ]), namespaces=namespaces)[0].text

    asserts.assert_that(issuer).is_equal_to(
            'CN=MasterCard ITF Messages Signing Sub CA G2,OU=Global Information Security,' +
            'O=MasterCard Worlwide,C=BE'
    )
    card_number = root.findall(''.join([
            './soap-env:Body/',
            'ns0:GetBenefitInformation/ns0:benefitInfoReq/',
            'ns1:CardNumber',
        ]), namespaces=namespaces)[0].text.strip()
    asserts.assert_that(card_number).is_equal_to("0000000000000000")


def _test_nonstd_xpath_functions():
    parser = SimpleXMLTreeBuilder.TreeBuilder()
    data = """\
    <?xml version='1.0' encoding='utf-8'?>
    <soap-env:Envelope
      xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/" soap-env:mustUnderstand="1">
      <soap-env:Header
        xmlns:wsa="http://www.w3.org/2005/08/addressing">
        <wsa:Action>http://schemas.mastercard.com.chssecure/2011/01/MasterCardCHSService/GetBenefitInformation</wsa:Action>
        <wsa:MessageID>urn:uuid:2cdb9e0c-6a7d-419b-b243-ebd76866cec0</wsa:MessageID>
        <wsa:To>https://X.mastercard.com/X/ALB/CHSSecureWebService</wsa:To>
        <wsse:Security
          xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
          <Signature
            xmlns="http://www.w3.org/2000/09/xmldsig#">
            <SignedInfo>
              <CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
              <SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha512"/>
              <Reference URI="#id-46957075-bddd-460e-b4b1-062bfb2b52dc">
                <Transforms>
                  <Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                </Transforms>
                <DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha512"/>
                <DigestValue>bNc+C8QVxuJPmZq51OywslTN7MJQ5xBGkPB7TAEuFtqeSc1SdJxSZBx943B3lDoH
        9ENcoKCnfHPE7bXQ9moqAA==</DigestValue>
              </Reference>
              <Reference URI="#id-b39fb7e5-43c8-4904-913f-5ce8fe87eb8d">
                <Transforms>
                  <Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                </Transforms>
                <DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha512"/>
                <DigestValue>3wPnIvcNM8upEAQJCE/5q2tTdLv5FMK2V8xWwTIFRKcFD5qmv71Ztg15H/ENFi3B
        AWlI9QoDEse6vJMX7b5LPQ==</DigestValue>
              </Reference>
            </SignedInfo>
            <SignatureValue>V77yMeUfV8GODtwCRqAH2ixFMhvMeikTvT/xG4C4e/V+pHnOq3laG5JY4byGxGGh
        cixsLJznd9c+J3S5awNI5Zj63Kr7mdG5sCGLiiSzwJN7KvnjR2m4Qk34H1tguzLa
        xYbL64m7WZBwPvV2ps4wv96q5uiPr5ey4Gvoi8Wsc0RPr0FRrUjd34E6R8xKJIYa
        xiupg2t/faArbZIJEn3F4v1k4jyP9o9UQlBjquGE8qFpbVUaTRx//iMObY5qv6Wg
        flZ2iKgdg/qSxL336pDsutMkNfsLx3iVu50/QQ7xm4xmKsWujpiDDppWQay9/gRY
        g7xw7dmqB1c/9yrFdXJDug==</SignatureValue>
            <KeyInfo>
              <wsse:SecurityTokenReference>
                <X509Data>
                  <X509IssuerSerial>
                    <X509IssuerName>CN=MasterCard ITF Messages Signing Sub CA G2,OU=Global Information Security,O=MasterCard Worlwide,C=BE</X509IssuerName>
                    <X509SerialNumber>1095231617403928779</X509SerialNumber>
                  </X509IssuerSerial>
                  <X509Certificate>MIID3jCCAsagAwIBAgIIDzMLWm4jQMswDQYJKoZIhvcNAQELBQAwgYUxCzAJBgNV
        BAYTAkJFMRwwGgYDVQQKExNNYXN0ZXJDYXJkIFdvcmx3aWRlMSQwIgYDVQQLExtH
        bG9iYWwgSW5mb3JtYXRpb24gU2VjdXJpdHkxMjAwBgNVBAMTKU1hc3RlckNhcmQg
        SVRGIE1lc3NhZ2VzIFNpZ25pbmcgU3ViIENBIEcyMB4XDTIxMDIyMzEwNDAyOFoX
        DTI0MDIyMzEwNDAyOFoweTELMAkGA1UEBhMCVVMxETAPBgNVBAgMCE5ldyBZb3Jr
        MRkwFwYDVQQKDBBpbkNvbnRyb2wtUmV0YWlsMR8wHQYDVQQLDBZpbmNvbnRyb2xt
        dGYtcmV0YWlsYXBpMRswGQYDVQQDDBJFeHRlbmQtRW50ZXJwcmlzZXMwggEiMA0G
        CSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQChkMYrphLmbXRJd7whR3LKdlQZHY4f
        RVHQgM4pvQJ9ORADRwdh52z7JYke9phvR+DBuYmEhUAkSr1Nknp30SGm1lTLk62d
        fMJ5x0EJTSEMXh60X8tqjubHIhyFfx312g7oas5sVDmSzXyQ8Fu5BQboTPa54+xV
        jNiiUwO5U1kBcOWRvxpcKF54SqtwOA3FTHaF7UZ/0rkJAMv1HEq0DNSiJJXtia5x
        MYex9+QXxDwOcz9zPf6n0cBOXTEIID40BDkWI+PVI29UPc94HZoDtbPwPLipvh2t
        l7wGQGR5VbnVuprUClvcvYyw2JHX1JfqcEtEDCNtsV0YayCU/cfxNRV5AgMBAAGj
        XTBbMA4GA1UdDwEB/wQEAwIAgDAJBgNVHRMEAjAAMB0GA1UdDgQWBBRFMN8wCNA1
        g/CuroxUDQ766EbWOTAfBgNVHSMEGDAWgBRbPEBSD+5PgOubatSG2Ytg3H9YVTAN
        BgkqhkiG9w0BAQsFAAOCAQEAks3QIb77EU5dYig0PWjnXTgrBEVeIgZzReh/r1Ub
        f9JQCGw301QhVWAWV9ZVAwyk7p+6G83kSwdbI+SYgTP9WW0sGWtsnYvo3VxaDCnO
        /Tnj8Hl+H7vdc9TKmas/xtE05ulZ97oeY9s/NDGgEoYRm5qvCiBH917CW0tSM5Zx
        3GOwmvSwtVKSsBLrNWwgKU1SVP5Q41WrNbkTW6mCAAPWF3UtQgTjdviNpIyOIALp
        DI3mxMKJ7Fef6FiJ+R9875UhcaPZbGksHXiBeFpSN2dcv1FMhdqQh3/qMMEJH5+Y
        bgr71Xf9GIgujVYNPzJxhHs3BWORX1O4gn/BprbZ7LmmNg==</X509Certificate>
                </X509Data>
              </wsse:SecurityTokenReference>
            </KeyInfo>
          </Signature>
          <wsu:Timestamp
            xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="id-b39fb7e5-43c8-4904-913f-5ce8fe87eb8d">
            <wsu:Created>2021-02-09T20:57:37Z</wsu:Created>
            <wsu:Expires>2021-02-09T21:57:37Z</wsu:Expires>
          </wsu:Timestamp>
        </wsse:Security>
      </soap-env:Header>
      <soap-env:Body
        xmlns:ns0="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" ns0:Id="id-46957075-bddd-460e-b4b1-062bfb2b52dc">
        <ns0:GetBenefitInformation
          xmlns:ns0="http://schemas.mastercard.com.chssecure/2011/01">
          <ns0:benefitInfoReq>
            <ns1:CardNumber
              xmlns:ns1="http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request">0000000000000000
            </ns1:CardNumber>
            <ns2:ServiceProviderCode
              xmlns:ns2="http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request">01
            </ns2:ServiceProviderCode>
            <ns3:SrcSysCode
              xmlns:ns3="http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request">X
            </ns3:SrcSysCode>
          </ns0:benefitInfoReq>
        </ns0:GetBenefitInformation>
      </soap-env:Body>
    </soap-env:Envelope>"""
    tree = parse(data, parser)
    root = tree.getroot()
    query = ".//*[@*[local-name() = 'Id' ]='id-46957075-bddd-460e-b4b1-062bfb2b52dc']"
    result = root.findall(query)
    asserts.assert_that(len(result)).is_equal_to(1)
    asserts.assert_that(result[0].tag).is_equal_to("{http://schemas.xmlsoap.org/soap/envelope/}Body")
    asserts.assert_that(result[0].get('{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Id')).is_equal_to("id-46957075-bddd-460e-b4b1-062bfb2b52dc")
    # query = ".//*[@*[local-name() = 'Id' ]='id-46957075-bddd-460e-b4b1-062bfb2b52dc']"
    # result = root.findall(".//*[@*='id-46957075-bddd-460e-b4b1-062bfb2b52dc']")
    # asserts.assert_that(len(result)).is_equal_to(1)


ET = ElementTree


def T_test_indent():
    elem = ET.XML("<root></root>")
    ET.indent(elem)
    asserts.assert_that(ET.tostring(elem, encoding=None)).is_equal_to(b"<root />")

    elem = ET.XML("<html><body>text</body></html>")
    ET.indent(elem)
    asserts.assert_that(ET.tostring(elem, encoding=None)).is_equal_to(b'<html>\n  <body>text</body>\n</html>')

    elem = ET.XML("<html> <body>text</body>  </html>")
    ET.indent(elem)
    asserts.assert_that(ET.tostring(elem, encoding=None)).is_equal_to(b'<html>\n  <body>text</body>\n</html>')

    elem = ET.XML("<html><body>text</body>tail</html>")
    ET.indent(elem)
    asserts.assert_that(ET.tostring(elem, encoding=None)).is_equal_to(b"<html>\n  <body>text</body>tail</html>")

    elem = ET.XML("<html><body><p>par</p>\n<p>text</p>\t<p><br/></p></body></html>")
    ET.indent(elem)
    # print("\n", ET.tostring(elem, encoding=None), "\n\n", repr(ET.tostring(elem, encoding=None)))
    asserts.assert_that(
        ET.tostring(elem, encoding=None)
    ).is_equal_to(
          b'<html>\n'
        + b'  <body>\n'
        + b'    <p>par</p>\n'
        + b'    <p>text</p>\n'
        + b'    <p>\n'
        + b'      <br />\n'
        + b'    </p>\n'
        + b'  </body>\n'
        + b'</html>'
    )

    elem = ET.XML("<html><body><p>pre<br/>post</p><p>text</p></body></html>")
    ET.indent(elem)
    # print("\n", ET.tostring(elem, encoding=None), "\n\n", repr(ET.tostring(elem, encoding=None)))
    asserts.assert_that(
        ET.tostring(elem, encoding=None)
    ).is_equal_to(
          b"<html>\n"
        + b"  <body>\n"
        + b"    <p>pre<br />post</p>\n"
        + b"    <p>text</p>\n"
        + b"  </body>\n"
        + b"</html>"
    )

def T_test_indent_space():
    elem = ET.XML("<html><body><p>pre<br/>post</p><p>text</p></body></html>")
    ET.indent(elem, space="\t")
    # print("\n", ET.tostring(elem, encoding=None), "\n\n", repr(ET.tostring(elem, encoding=None)))
    asserts.assert_that(
        ET.tostring(elem, encoding=None)
    ).is_equal_to(
        b"<html>\n" +
        b"\t<body>\n" +
        b"\t\t<p>pre<br />post</p>\n" +
        b"\t\t<p>text</p>\n" +
        b"\t</body>\n" +
        b"</html>"
    )

    elem = ET.XML("<html><body><p>pre<br/>post</p><p>text</p></body></html>")
    ET.indent(elem, space="")
    asserts.assert_that(
        ET.tostring(elem, encoding=None)
    ).is_equal_to(
        b"<html>\n" +
        b"<body>\n" +
        b"<p>pre<br />post</p>\n" +
        b"<p>text</p>\n" +
        b"</body>\n" +
        b"</html>"
    )

def T_test_indent_space_caching():
    elem = ET.XML("<html><body><p>par</p><p>text</p><p><br/></p><p /></body></html>")
    ET.indent(elem)
    asserts.assert_that(sets.Set([el.tail for el in elem.iter()])).is_equal_to(sets.Set([None, "\n", "\n  ", "\n    "]))
    asserts.assert_that(sets.Set([el.text for el in elem.iter()])).is_equal_to(sets.Set([None, "\n  ", "\n    ", "\n      ", "par", "text"]))
    asserts.assert_that(len(sets.Set([el.tail for el in elem.iter()]))).is_equal_to(len(sets.Set([el.tail for el in elem.iter()])))


def T_test_indent_level():
    elem = ET.XML("<html><body><p>pre<br/>post</p><p>text</p></body></html>")
    def _larky_2739997752():
        ET.indent(elem, level=-1)
    asserts.assert_fails(lambda: _larky_2739997752(), ".*?ValueError")
    asserts.assert_that(ET.tostring(elem, encoding=None)).is_equal_to(
        b"<html><body><p>pre<br />post</p><p>text</p></body></html>"
    )

    ET.indent(elem, level=2)
    asserts.assert_that(ET.tostring(elem, encoding=None)).is_equal_to((b"<html>\n" +
        b"      <body>\n" +
        b"        <p>pre<br />post</p>\n" +
        b"        <p>text</p>\n" +
        b"      </body>\n" +
        b"    </html>"))

    elem = ET.XML("<html><body><p>pre<br/>post</p><p>text</p></body></html>", parser=SimpleXMLTreeBuilder.TreeBuilder())
    ET.indent(elem, level=1, space=" ")
    asserts.assert_that(ET.tostring(elem, encoding=None)).is_equal_to((b"<html>\n" +
        b"  <body>\n" +
        b"   <p>pre<br />post</p>\n" +
        b"   <p>text</p>\n" +
        b"  </body>\n" +
        b" </html>"))


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_elementtree))
    _suite.addTest(unittest.FunctionTestCase(_test_xpath))
    _suite.addTest(unittest.FunctionTestCase(_test_update_and_serialize))
    _suite.addTest(unittest.FunctionTestCase(_test_append_and_flatten))
    _suite.addTest(unittest.FunctionTestCase(_test_wsse_signed_payload))
    _suite.addTest(unittest.FunctionTestCase(_test_nonstd_xpath_functions))
    _suite.addTest(unittest.FunctionTestCase(T_test_indent))
    _suite.addTest(unittest.FunctionTestCase(T_test_indent_space))
    _suite.addTest(unittest.FunctionTestCase(T_test_indent_space_caching))
    _suite.addTest(unittest.FunctionTestCase(T_test_indent_level))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
