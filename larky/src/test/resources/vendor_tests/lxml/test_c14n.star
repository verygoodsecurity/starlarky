load("@stdlib//base64", base64="base64")
load("@stdlib//io", io="io")
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")

load("@vendor//lxml", etree="etree")


eg1 = """<?xml version="1.0"?>

<?xml-stylesheet   href="doc.xsl"
   type="text/xsl"   ?>

<!DOCTYPE doc SYSTEM "doc.dtd">

<doc>Hello, world!<!-- Comment 1
--></doc>

<?pi-without-data     ?>

<!-- Comment 2 -->

<!-- Comment 3 -->
"""

eg2 = """<doc>
   <clean>   </clean>
   <dirty>   A   B   </dirty>
   <mixed>
      A
      <clean>   </clean>
      B
      <dirty>   A   B   </dirty>
      C
   </mixed>
</doc>
"""

# TODO: We do not support DTD properly in Larky
# As a result, we will not respect "ATTLIST"
# So, we will inject "default" into the template since we will not
# do it in our larky code.
eg3 = """<!DOCTYPE doc [<!ATTLIST e9 attr CDATA "default">]>
<doc xmlns:foo="http://www.bar.org">
   <e1   />
   <e2   ></e2>
   <e3    name = "elem3"   id="elem3"    />
   <e4    name="elem4"   id="elem4"    ></e4>
   <e5 a:attr="out" b:attr="sorted" attr2="all" attr="I'm"
       xmlns:b="http://www.ietf.org"
       xmlns:a="http://www.w3.org"
       xmlns="http://example.org"/>
   <e6 xmlns="" xmlns:a="http://www.w3.org">
       <e7 xmlns="http://www.ietf.org">
           <e8 xmlns="" xmlns:a="http://www.w3.org" a:foo="bar">
               <e9 xmlns="" xmlns:a="http://www.ietf.org" attr="default" />
           </e8>
       </e7>
   </e6>
</doc>
"""

# TODO: We do not support DTD properly in Larky
# As a result, we will not respect "ATTLIST"
# So, we will not understand how to deal with the below ATTLIST types
# eg4 = """<!DOCTYPE doc [ <!ATTLIST normId id ID #IMPLIED> <!ATTLIST normNames attr NMTOKENS #IMPLIED> ]> <doc>
eg4 = """<!DOCTYPE doc> <doc>
   <text>First line&#x0d;&#10;Second line</text>
   <value>&#x32;</value>
   <compute><![CDATA[value>"0" && value<"10" ?"valid":"error"]]></compute>
   <compute expr='value>"0" &amp;&amp; value&lt;"10" ?"valid":"error"'>valid</compute>
   <norm attr=' &apos;   &#x20;&#13;&#xa;&#9;   &apos; '/>
   <normNames attr='   A   &#x20;&#13;&#xa;&#9;   B   '/>
   <normId id=' &apos;   &#x20;&#13;&#xa;&#9;   &apos; '/>
</doc>"""

# We expect the below to fail since we do not understand ENTITY resolution
# or DTD parsing
eg5 = """<!DOCTYPE doc [
<!ATTLIST doc attrExtEnt ENTITY #IMPLIED>
<!ENTITY ent1 "Hello">
<!ENTITY ent2 SYSTEM "world.txt">
<!ENTITY entExt SYSTEM "earth.gif" NDATA gif>
<!NOTATION gif SYSTEM "viewgif.exe">
]>
<doc attrExtEnt="entExt">
   &ent1;, &ent2;!
</doc>

<!-- Let world.txt contain "world" (excluding the quotes) -->
"""

# This should pass
eg6 = """<?xml version="1.0" encoding="ISO-8859-1"?>
<doc>&#169;</doc>"""

# again, this should fail.
eg7 = """<!DOCTYPE doc [
<!ATTLIST e2 xml:space (default|preserve) 'preserve'>
<!ATTLIST e3 id ID #IMPLIED>
]>
<doc xmlns="http://www.ietf.org" xmlns:w3c="http://www.w3.org">
   <e1>
      <e2 xmlns="">
         <e3 id="E3"/>
      </e2>
   </e1>
</doc>"""

examples = [eg1, eg2, eg3, eg4, eg5, eg6, eg7]

test_results = {
    eg1: '''PD94bWwtc3R5bGVzaGVldCBocmVmPSJkb2MueHNsIgogICB0eXBlPSJ0ZXh0L3hz
    bCIgICA/Pgo8ZG9jPkhlbGxvLCB3b3JsZCE8IS0tIENvbW1lbnQgMQotLT48L2Rv
    Yz4KPD9waS13aXRob3V0LWRhdGE/Pgo8IS0tIENvbW1lbnQgMiAtLT4KPCEtLSBD
    b21tZW50IDMgLS0+''',

    eg2: '''PGRvYz4KICAgPGNsZWFuPiAgIDwvY2xlYW4+CiAgIDxkaXJ0eT4gICBBICAgQiAg
    IDwvZGlydHk+CiAgIDxtaXhlZD4KICAgICAgQQogICAgICA8Y2xlYW4+ICAgPC9j
    bGVhbj4KICAgICAgQgogICAgICA8ZGlydHk+ICAgQSAgIEIgICA8L2RpcnR5Pgog
    ICAgICBDCiAgIDwvbWl4ZWQ+CjwvZG9jPg==''',

    eg3: '''PGRvYyB4bWxuczpmb289Imh0dHA6Ly93d3cuYmFyLm9yZyI+CiAgIDxlMT48L2Ux
    PgogICA8ZTI+PC9lMj4KICAgPGUzIGlkPSJlbGVtMyIgbmFtZT0iZWxlbTMiPjwv
    ZTM+CiAgIDxlNCBpZD0iZWxlbTQiIG5hbWU9ImVsZW00Ij48L2U0PgogICA8ZTUg
    eG1sbnM9Imh0dHA6Ly9leGFtcGxlLm9yZyIgeG1sbnM6YT0iaHR0cDovL3d3dy53
    My5vcmciIHhtbG5zOmI9Imh0dHA6Ly93d3cuaWV0Zi5vcmciIGF0dHI9IkknbSIg
    YXR0cjI9ImFsbCIgYjphdHRyPSJzb3J0ZWQiIGE6YXR0cj0ib3V0Ij48L2U1Pgog
    ICA8ZTYgeG1sbnM6YT0iaHR0cDovL3d3dy53My5vcmciPgogICAgICAgPGU3IHht
    bG5zPSJodHRwOi8vd3d3LmlldGYub3JnIj4KICAgICAgICAgICA8ZTggeG1sbnM9
    IiIgYTpmb289ImJhciI+CiAgICAgICAgICAgICAgIDxlOSB4bWxuczphPSJodHRw
    Oi8vd3d3LmlldGYub3JnIiBhdHRyPSJkZWZhdWx0Ij48L2U5PgogICAgICAgICAg
    IDwvZTg+CiAgICAgICA8L2U3PgogICA8L2U2Pgo8L2RvYz4=''',

    eg4: '''PGRvYz4KICAgPHRleHQ+Rmlyc3QgbGluZSYjeEQ7ClNlY29uZCBsaW5lPC90ZXh0
    PgogICA8dmFsdWU+MjwvdmFsdWU+CiAgIDxjb21wdXRlPnZhbHVlJmd0OyIwIiAm
    YW1wOyZhbXA7IHZhbHVlJmx0OyIxMCIgPyJ2YWxpZCI6ImVycm9yIjwvY29tcHV0
    ZT4KICAgPGNvbXB1dGUgZXhwcj0idmFsdWU+JnF1b3Q7MCZxdW90OyAmYW1wOyZh
    bXA7IHZhbHVlJmx0OyZxdW90OzEwJnF1b3Q7ID8mcXVvdDt2YWxpZCZxdW90Ozom
    cXVvdDtlcnJvciZxdW90OyI+dmFsaWQ8L2NvbXB1dGU+CiAgIDxub3JtIGF0dHI9
    IiAnICAgICYjeEQmI3hBJiN4OSAgICcgIj48L25vcm0+CiAgIDxub3JtTmFtZXMg
    YXR0cj0iQSAmI3hEJiN4QSYjeDkgQiI+PC9ub3JtTmFtZXM+CiAgIDxub3JtSWQg
    aWQ9IicgJiN4RCYjeEEmI3g5ICciPjwvbm9ybUlkPgo8L2RvYz4=''',

    eg5: '''PGRvYyBhdHRyRXh0RW50PSJlbnRFeHQiPgogICBIZWxsbywgd29ybGQhCjwvZG9j
    Pg==''',

    eg6: '''PGRvYz7CqTwvZG9jPg==''',

    eg7: '''PGRvYyB4bWxucz0iaHR0cDovL3d3dy5pZXRmLm9yZyIgeG1sbnM6dzNjPSJodHRw
    Oi8vd3d3LnczLm9yZyI+CiAgIDxlMT4KICAgICAgPGUyIHhtbG5zPSIiIHhtbDpz
    cGFjZT0icHJlc2VydmUiPgogICAgICAgICA8ZTMgaWQ9IkUzIj48L2UzPgogICAg
    ICA8L2UyPgogICA8L2UxPgo8L2RvYz4=''',

}


def test_c14n_eg1():
    tree = etree.parse(io.StringIO(eg1), preservews=False)
    # In [30]: print(etree.tostring(z).decode('utf-8'))
    expected = ('<?xml-stylesheet href="doc.xsl"\n   type="text/xsl"   ?>' +
                     '<!DOCTYPE doc SYSTEM "doc.dtd">\n<doc>Hello, world!' +
                     '<!-- Comment 1\n--></doc><?pi-without-data?>' +
                     '<!-- Comment 2 --><!-- Comment 3 -->')
    actual = etree.tostring(tree)
    # print(repr(actual))
    # print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)
    # print("--" * 50)
    actual = etree.tostring(tree, method='c14n')
    expected = base64.b64decode((test_results[eg1])).decode('utf-8')
    # print(repr(actual))
    # print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)


def test_c14n_eg2():
    builder = etree.TreeBuilder(preservews=True)
    tree = builder.fromstring(eg2)
    # In [30]: print(etree.tostring(z).decode('utf-8'))
    expected = ('<doc>\n   <clean>   </clean>\n   ' +
                '<dirty>   A   B   </dirty>\n   ' +
                '<mixed>\n      A\n      ' +
                '<clean>   </clean>\n      B\n      ' +
                '<dirty>   A   B   </dirty>\n      C\n   ' +
                '</mixed>\n</doc>')
    actual = etree.tostring(tree)
    # print(repr(actual))
    # print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)
    # print("--" * 50)
    actual = etree.tostring(tree, method='c14n').strip()
    expected = base64.b64decode((test_results[eg2])).decode('utf-8')
    # print(repr(actual))
    # print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)


def test_c14n_eg3():
    builder = etree.TreeBuilder(preservews=True)
    tree = builder.fromstring(eg3)
    # >> print(etree.tostring(z).decode('utf-8'))
    actual = etree.tostring(tree, space_inside_empty_tag=False)
    # TODO: We do not support DTD properly in Larky
    # As a result, we will not respect "ATTLIST"
    # So, we will inject "default" into the template since we will not
    # do it in our larky code.
    expected = '\n'.join([
        '<!DOCTYPE doc [',
        '<!ATTLIST e9 attr CDATA "default">',
        ']>',
        '<doc xmlns:foo="http://www.bar.org">',
        '   <e1/>',
        '   <e2/>',
        '   <e3 name="elem3" id="elem3"/>',
        '   <e4 name="elem4" id="elem4"/>',
        '   <e5 xmlns:b="http://www.ietf.org" xmlns:a="http://www.w3.org" xmlns="http://example.org" a:attr="out" b:attr="sorted" attr2="all" attr="I\'m"/>',
        '   <e6 xmlns="" xmlns:a="http://www.w3.org">',
        '       <e7 xmlns="http://www.ietf.org">',
        '           <e8 xmlns="" xmlns:a="http://www.w3.org" a:foo="bar">',
        '               <e9 xmlns="" xmlns:a="http://www.ietf.org" attr="default"/>',
        '           </e8>',
        '       </e7>',
        '   </e6>',
        '</doc>',
      ])
    # print(actual)
    # print(repr(actual))
    # print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)
    # print("--" * 50)
    actual = etree.tostring(tree, method='c14n').strip()
    expected = base64.b64decode((test_results[eg3])).decode('utf-8')
    print(repr(actual))
    print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)


def test_c14n_eg4():
    builder = etree.TreeBuilder(preservews=True)
    tree = builder.fromstring(eg4)
    # >> print(etree.tostring(z).decode('utf-8'))
    actual = etree.tostring(tree, space_inside_empty_tag=False)
    expected = '\n'.join([
        # '<!DOCTYPE doc [',
        # '<!ATTLIST normId id ID #IMPLIED>',
        # '<!ATTLIST normNames attr NMTOKENS #IMPLIED>',
        # ']>',
        '<!DOCTYPE doc>',
        '<doc>',
        '   <text>First line&#13;',
        'Second line</text>',
        '   <value>2</value>',
        '   <compute>value&gt;"0" &amp;&amp; value&lt;"10" ?"valid":"error"</compute>',
        '   <compute expr="value&gt;&quot;0&quot; &amp;&amp; value&lt;&quot;10&quot; ?&quot;valid&quot;:&quot;error&quot;">valid</compute>',
        '   <norm attr=" \'    &#13;&#10;&#9;   \' "/>',
        # '   <normNames attr="A &#13;&#10;&#9; B"/>',
        '   <normNames attr="   A    &#13;&#10;&#9;   B   "/>',
        # '   <normId id="\' &#13;&#10;&#9; \'"/>',
        '   <normId id=" \'    &#13;&#10;&#9;   \' "/>',
        '</doc>',
    ])
    # print(repr(actual))
    # print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)
    # print("--" * 50)
    actual = etree.tostring(tree, method='c14n').strip()
    # again, we do not support DTD...
    # expected = base64.b64decode((test_results[eg4])).decode('utf-8')
    # the following output was generated from python lxml
    # > base64.b64encode(etree.tostring(z3, method='c14n')).decode('utf-8'))
    expected = base64.b64decode(
        "PGRvYz4KICAgPHRleHQ+Rmlyc3QgbGluZSYjeEQ7ClNlY29uZCBsaW5lPC9" +
        "0ZXh0PgogICA8dmFsdWU+MjwvdmFsdWU+CiAgIDxjb21wdXRlPnZhbHVlJm" +
        "d0OyIwIiAmYW1wOyZhbXA7IHZhbHVlJmx0OyIxMCIgPyJ2YWxpZCI6ImVyc" +
        "m9yIjwvY29tcHV0ZT4KICAgPGNvbXB1dGUgZXhwcj0idmFsdWU+JnF1b3Q7" +
        "MCZxdW90OyAmYW1wOyZhbXA7IHZhbHVlJmx0OyZxdW90OzEwJnF1b3Q7ID8" +
        "mcXVvdDt2YWxpZCZxdW90OzomcXVvdDtlcnJvciZxdW90OyI+dmFsaWQ8L2" +
        "NvbXB1dGU+CiAgIDxub3JtIGF0dHI9IiAnICAgICYjeEQ7JiN4QTsmI3g5O" +
        "yAgICcgIj48L25vcm0+CiAgIDxub3JtTmFtZXMgYXR0cj0iICAgQSAgICAm" +
        "I3hEOyYjeEE7JiN4OTsgICBCICAgIj48L25vcm1OYW1lcz4KICAgPG5vcm1" +
        "JZCBpZD0iICcgICAgJiN4RDsmI3hBOyYjeDk7ICAgJyAiPjwvbm9ybUlkPg" +
        "o8L2RvYz4=").decode('utf-8')
    # print(repr(actual))
    # print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)


def test_c14n_eg5():
    builder = etree.TreeBuilder(preservews=True)
    tree = builder.fromstring(eg5)
    # >> print(etree.tostring(z).decode('utf-8'))
    actual = etree.tostring(tree)
    print(repr(actual))
    print(actual)
    # print(repr(expected))
    # asserts.assert_that(actual).is_equal_to(expected)
    print("--" * 50)
    # actual = etree.tostring(tree, method='c14n').strip()
    expected = base64.b64decode((test_results[eg5])).decode('utf-8')
    # print(repr(actual))
    print(repr(expected))
    # asserts.assert_that(actual).is_equal_to(expected)

# TODO(mahmoudimus): support encoding
def test_c14n_eg6():
    builder = etree.TreeBuilder(preservews=True, debug=True)
    tree = builder.fromstring(eg6)
    # tree = etree.parse(io.StringIO(eg6))
    # builder = etree.TreeBuilder(preservews=True)
    # tree = builder.fromstring(eg6)
    actual = etree.tostring(tree, debug=True)
    expected = b'<doc>&#169;</doc>'.decode('utf-8')
    # print(repr(actual))
    # print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)
    # print("--" * 50)
    actual = etree.tostring(tree, method='c14n')
    expected = base64.b64decode((test_results[eg6])).decode('utf-8')
    print(repr(actual))
    print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)

def test_c14n_eg7():
    builder = etree.TreeBuilder(preservews=True, debug=True)
    tree = builder.fromstring(eg7)
    # >> print(etree.tostring(z).decode('utf-8'))
    actual = etree.tostring(tree, debug=True)
    # print(repr(actual))
    # print(actual)
    # print(repr(expected))
    # asserts.assert_that(actual).is_equal_to(expected)
    # print("--" * 50)
    actual = etree.tostring(tree, method='c14n').strip()
    expected = base64.b64decode((test_results[eg7])).decode('utf-8')
    # print(repr(actual))
    # print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)

def _suite():
    _suite = unittest.TestSuite()
    # XXX:
    _suite.addTest(unittest.FunctionTestCase(test_c14n_eg1))
    _suite.addTest(unittest.FunctionTestCase(test_c14n_eg2))
    _suite.addTest(unittest.FunctionTestCase(test_c14n_eg3))
    _suite.addTest(unittest.FunctionTestCase(test_c14n_eg4))
    # TODO...support this..
    # TODO(mahmoudimus): support encoding
    # _suite.addTest(unittest.FunctionTestCase(test_c14n_eg6))
    # FAILING:
    # _suite.addTest(unittest.FunctionTestCase(test_c14n_eg5))
    # _suite.addTest(unittest.FunctionTestCase(test_c14n_eg7))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
