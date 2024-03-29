SIGN_OUT_XML = """\
<OrbiscomRequest xmlns:ds="http://www.w3.org/2000/09/xmldsig#" IssuerId="101118" Version="15.2">
    <OBOAuthenticateRequest><ds:Signature>
<ds:SignedInfo><ds:CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/><ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/><ds:Reference URI=""><ds:Transforms><ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/></ds:Transforms><ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/><ds:DigestValue>2aESUP7W6eikyrGrh8XAkrB0vv0=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>DMIbKnuzr7uMTwz/n8JxMWqR0SG1Q+zBwjx28MamPSWpRfsFkxOQrQ9GaTOOnSqo
Eou2xcDHW4pcblPeTZy1nYJI6+20S1dRCmb6/XAxAAeFvdSOvVwPtiB/LBPDN2sY
VNM9cVUWZAQe6jgJQ/fPGPreZMeVZiqAXIBCoKCbmsxEgi5xt/kXC8rW/7XoaaBv
XL+1vnvur4yBsyqHqrTfmwl+GvarM/e9bjxDuzjsmF+g76yb+DrQbdBnrm+7t0VV
BTmIisMDAD07ZrgrSY+VN3cymgVcnPT11jDifK7ri2MwSXTaewWfWul4zVZzX4H+
2eePPbOzrfoax6Z+b0KnhA==</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509SubjectName>CN=Extend-Enterprises, OU=incontrolmtf-retailapi, O=inControl-Retail, ST=New York, C=US</ds:X509SubjectName><ds:X509Certificate>MIID3jCCAsagAwIBAgIIDzMLWm4jQMswDQYJKoZIhvcNAQELBQAwgYUxCzAJBgNV
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
bgr71Xf9GIgujVYNPzJxhHs3BWORX1O4gn/BprbZ7LmmNg==</ds:X509Certificate><ds:X509SubjectName>CN=MasterCard ITF Messages Signing Sub CA G2, OU=Global Information Security, O=MasterCard Worlwide, C=BE</ds:X509SubjectName>
<ds:X509Certificate>MIIE/jCCAuagAwIBAgIQEnmGId5ldw6cBGHCXtcR0jANBgkqhkiG9w0BAQsFADCB
hzELMAkGA1UEBhMCQkUxHTAbBgNVBAoTFE1hc3RlckNhcmQgV29ybGR3aWRlMSQw
IgYDVQQLExtHbG9iYWwgSW5mb3JtYXRpb24gU2VjdXJpdHkxMzAxBgNVBAMTKk1h
c3RlckNhcmQgSVRGIE1lc3NhZ2VzIFNpZ25pbmcgUm9vdCBDQSBHMjAeFw0xNTAy
MTAxNjI5NDhaFw0yNTAyMTAxNjI5NDlaMIGFMQswCQYDVQQGEwJCRTEcMBoGA1UE
ChMTTWFzdGVyQ2FyZCBXb3Jsd2lkZTEkMCIGA1UECxMbR2xvYmFsIEluZm9ybWF0
aW9uIFNlY3VyaXR5MTIwMAYDVQQDEylNYXN0ZXJDYXJkIElURiBNZXNzYWdlcyBT
aWduaW5nIFN1YiBDQSBHMjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB
AL0HSw+9fd4NiL+tmxUMoj8kXtPzDEsa8K+vmYBusqscuCylfyPNoe+BNGcoFTk2
qwXlP1WgxtVRjP3qqoYy3dZ/lTp1yrV3V2VQCjtuJEr7hn7+PFWy8Gzd5ISz2vmy
o5gmMVPQ+Q+KenHw3VlBMhcYjoonwFHFvH5VV8x8/RauhizalAmLQ8gnl2hHTwy1
tT9LYpdhysRs1S2MMq6wovwpkO8e1wj6v4VNqmhheVH+69e4/7C86ML3BoqnS059
sa5/P4C/8oK5ZehRA0XAGZgltrqRLvyW3mZuvRvwzr/YoULDx/E5SwajhPzaWLMC
L129gZJUn6Bq5wEiUq+Ut78CAwEAAaNmMGQwDgYDVR0PAQH/BAQDAgGGMBIGA1Ud
EwEB/wQIMAYBAf8CAQAwHQYDVR0OBBYEFFs8QFIP7k+A65tq1IbZi2Dcf1hVMB8G
A1UdIwQYMBaAFC8W0uutA7E+X8MPu2WGFjDdHjkeMA0GCSqGSIb3DQEBCwUAA4IC
AQCXzcB9GUiItJGsRfPJWhgxcO1y1PmWrYlR4sRUWGuuNzK9ZiNL+5V+gsJ3sLVZ
n8SAnCERm5xJjyqy5itn6Nu9lgaHpIDDRW8dx4LJIBZckzIEdQuVvg4iiicMxpcu
qlenyMSvTOrFXG6feQL1eUZZtD1Es0CvArwPokwHTi70IFQo2yBFand0jkBMhC47
j3LzHea6RexaJ1arbaZwccFTJOViZKPC96i/Si/vxva0wDpZ1KoEHqi8dZi4IYbN
+O2Q8oWcx82WivV3dJvlr0kpWN5G55upD1dSYVtUSGuOBP4wuIizy+vNHIFKPaZi
qhjgZ1cbC5hhVlBRSzMIz7gG1MMiU6dd3fTM1tvomy+oCxoUn5RiCxeHOXhC8Kdt
8HrE/fA7aW+N10A1NNfh8qRFVdE7sROMepuHWZRb4Mn3fyoeG3Y8z7jS3+G9jiTr
sg0sADt24Ar5ec1o3H0pWIR2t33X+ezI1jv3bgEhLTDBaIdo2CmDuAx6AYswkcbr
aaBhLgclKvjXHNCFsoaHHOm+H+BgTnTDyo8349xv9OuFLBJpQZjsH3cMApFzow9e
upJLMY/cbFBxp1KuPVAtK7V5pSE7ahH118JGL8evNSDfFupFPKXVnonyxXne5aWH
4BC9dgvwtsm8gxbie7QPx4g6qGukUHx85BhcXa/QKnZfpQ==</ds:X509Certificate>
<ds:X509SubjectName>CN=MasterCard ITF Messages Signing Root CA G2, OU=Global Information Security, O=MasterCard Worldwide, C=BE</ds:X509SubjectName>
<ds:X509Certificate>MIIGADCCA+igAwIBAgIQcb5/pa7rZnXaXeE+v7Pi4jANBgkqhkiG9w0BAQsFADCB
hzELMAkGA1UEBhMCQkUxHTAbBgNVBAoTFE1hc3RlckNhcmQgV29ybGR3aWRlMSQw
IgYDVQQLExtHbG9iYWwgSW5mb3JtYXRpb24gU2VjdXJpdHkxMzAxBgNVBAMTKk1h
c3RlckNhcmQgSVRGIE1lc3NhZ2VzIFNpZ25pbmcgUm9vdCBDQSBHMjAeFw0xNTAy
MTAxNjI1MjNaFw0zNTAyMTAxNjI1MjNaMIGHMQswCQYDVQQGEwJCRTEdMBsGA1UE
ChMUTWFzdGVyQ2FyZCBXb3JsZHdpZGUxJDAiBgNVBAsTG0dsb2JhbCBJbmZvcm1h
dGlvbiBTZWN1cml0eTEzMDEGA1UEAxMqTWFzdGVyQ2FyZCBJVEYgTWVzc2FnZXMg
U2lnbmluZyBSb290IENBIEcyMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKC
AgEAtRftrgTFZKgQByZSKJQA2NlKaqR6LoXNoZOTJMxwYRndhjfkZZelDkli6ZYY
mh/s8hcufDdF+os21dpHWGm595Cd6RAQXfys0JY62wpLvo8dxFtJkRRRavYFIkwu
0owIKkowgpe/UovLJPMVoephsHIfoKEb5JNJRD8Q7rgpCyFIMu7DmWLARrI1eZNp
TcPP/XG0glsWVbSNUJaLP4TwOp2BDMaQlAz6FAue8IMeaC7STSzSGW2DoLOiKCDL
ZhlxWGGhEUpQ8Ihdwd6uOENa+hhqgn363mQO7fBxxhpkv5ryFTzeu/rHqf8CeSiE
ibOVYitW12EEQxX0dSBNAw7QBFxtGuGmXHZluPEoMUsFh3w3rHudhgJy2gIlHIm6
eyIg1W3guX0YLdhxSdsdmvcmYGiYfk0AeIg4/hpHzr0AMX86vE777mk7a9mJfTGU
ew6qnw2QdLnBRL8nyuRRQ0CQqzWsJ+lewXqwRPpoBoJPzdMTOW3oWGi18XTSE1c4
IQTkq9sQC2bN/UEJZv9g7+E2jzyY4x1oq1Kz/GnxTit1iwh48h/uKRUYGpxQjj21
zQEkW9wu0nEJl8BueMUn+p0Zdol9LWPU7JwmKSgFjUuDrRUNruh25bEILuEE81I0
ujs5Tl62Qt5VjdbPs9puZ7zHjCcxEsor+OYMdC2MkpEuKOsCAwEAAaNmMGQwDgYD
VR0PAQH/BAQDAgGGMBIGA1UdEwEB/wQIMAYBAf8CAQEwHQYDVR0OBBYEFC8W0uut
A7E+X8MPu2WGFjDdHjkeMB8GA1UdIwQYMBaAFC8W0uutA7E+X8MPu2WGFjDdHjke
MA0GCSqGSIb3DQEBCwUAA4ICAQATU5V1TGzyB7d7AncprMWjVX2Dvfwua6ElCOeK
Z52WHczDD2ZdUj0E9kWrhbYnaZrloah6Gl/2j1mowVf4uZps3rJGLMy5Y3pIUy7d
7Icy17mnKxtjEsKdsjHjHPNY+3+Uv55gHPUaUBxVf83p9ihPNHDNM0MDhToyBEda
eGAUx4Adbu/t2wwXHwEyukmU7kdvc+m7DJOGSlxzP3WDAWxONatBGsrEa6nyPS9n
WSE4pMzAnLZigZWqiYwMgRzmQ+lfJhBatfveb2IliRXcVumvlGbsCy702E5Ip2Gh
ZD4tZ0s4uXDbS06orqVkdU3l2liFFC4gcAI5nGG14NUD4sPA1RHjnUWuCHUw7l+r
jg/DY4U9PU0QlKmckK9WAsAceV8UcP99jTi7OHg5Hu7e6wBAj6t32lxFgqPip1DM
QZVlhyUjT1cACZUkIE6Mb0Gy1de6UVVGB1gfFx1oy224BkC1SpKULOVQKnbCG556
MPmkDeG+X6lfDdzTpRJDqOQEYheoogTIS0PH4vEm1sRHxd0MU3SvE5/DDVhNC3mr
UF8RPab5Vz/cP4LXm9wJTNhMpaGWyhRpLVlXTXM+PjotqwVFR1BEERiBl5wtVgDF
vsKPeT+i3tAcKowZgxLfchp+84hc1PIfu+vxYtUfKll+MrYL7SmY0+CiuPZdHAgx
Ab7QOQ==</ds:X509Certificate>
</ds:X509Data></ds:KeyInfo></ds:Signature></OBOAuthenticateRequest>
</OrbiscomRequest>\
"""