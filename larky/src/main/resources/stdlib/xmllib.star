"""A parser for XML, using the derived class as static DTD."""

load("@stdlib//_markupbase", ParserBase="ParserBase")
load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//re", re="re")
load("@stdlib//string", string="string")
load("@stdlib//types", types="types")
load("@vendor//option/result", safe="safe", try_="try_", Error="Error")

version = '0.3-larky'
_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION

# Regular expressions used for parsing

_S = '[ \t\r\n]+'                       # white space
_opS = '[ \t\r\n]*'                     # optional white space
_Name = '[a-zA-Z_:][-a-zA-Z0-9._:]*'    # valid XML name
_QStr = "(?:'[^']*'|\"[^\"]*\")"        # quoted XML string
illegal = re.compile('[^\t\r\n -\176\u00A0-\u00FF]') # illegal chars in content
interesting = re.compile('[]&<]')

amp = re.compile('&')
ref = re.compile('&(' + _Name + '|#[0-9]+|#x[0-9a-fA-F]+)[^-a-zA-Z0-9._:]')
entityref = re.compile('&(?P<name>' + _Name + ')[^-a-zA-Z0-9._:]')
charref = re.compile('&#(?P<char>[0-9]+[^0-9]|x[0-9a-fA-F]+[^0-9a-fA-F])')
space = re.compile(_S + '$')
newline = re.compile('\n')

attrfind = re.compile(
    _S + '(?P<name>' + _Name + ')' +
    '(' + _opS + '=' + _opS +
    '(?P<value>'+_QStr+'|[-a-zA-Z0-9.:+*%?!()_#=~]+))?')
starttagopen = re.compile('<' + _Name)
starttagend = re.compile(_opS + '(?P<slash>/?)>')
starttagmatch = re.compile('<(?P<tagname>'+_Name+')'+
                      '(?P<attrs>(?:'+attrfind.pattern+')*)'+
                      starttagend.pattern)
endtagopen = re.compile('</')
endbracket = re.compile(_opS + '>')
endbracketfind = re.compile('(?:[^>\'"]|'+_QStr+')*>')
tagfind = re.compile(_Name)
cdataopen = re.compile(r'<!\[CDATA\[')
cdataclose = re.compile(r']]>')
# this matches one of the following:
# SYSTEM SystemLiteral
# PUBLIC PubidLiteral SystemLiteral
_SystemLiteral = '(?P<%s>'+_QStr+')'
_PublicLiteral = '(?P<%s>"[-\'\\(\\)+,./:=?;!*#@$_%% \n\ra-zA-Z0-9]*"|' + \
                        "'[-\\(\\)+,./:=?;!*#@$_%% \n\ra-zA-Z0-9]*')"
_ExternalId = '(?:SYSTEM|'+ \
                 'PUBLIC'+_S+_PublicLiteral%'pubid'+ \
              ')'+_S+_SystemLiteral%'syslit'
decltag = re.compile(r'<!')
declDoctype = re.compile(r'DOCTYPE'+_S+'(?P<name>'+_Name+')' +
                         '(?:'+_S+_ExternalId+')?'+_opS)
doctype = re.compile(decltag.pattern + declDoctype.pattern)
special = re.compile(decltag.pattern + '(?P<special>[^<>]*)>')
xmldecl = re.compile(r'<\?xml'+_S+
                     'version'+_opS+'='+_opS+'(?P<version>'+_QStr+')'+
                     '(?:'+_S+'encoding'+_opS+'='+_opS+
                        "(?P<encoding>'[A-Za-z][-A-Za-z0-9._]*'|" +
                        '"[A-Za-z][-A-Za-z0-9._]*"))?' +
                     '(?:'+_S+'standalone'+_opS+'='+_opS+
                        '(?P<standalone>\'(?:yes|no)\'|"(?:yes|no)"))?'+
                     _opS+r'\?*>')
procopen = re.compile('<\\?(?P<proc>' + _Name + ')' + _opS)
procclose = re.compile(_opS + '\\?>')
commentopen = re.compile('<!--')
commentclose = re.compile('-->')
doubledash = re.compile('--')
# does not exist in starlark, use replace instead
# attrtrans = string.maketrans(' \r\n\t', '    ')

# definitions for XML namespaces
_NCName = '[a-zA-Z_][-a-zA-Z0-9._]*'    # XML Name, minus the ":"
ncname = re.compile(_NCName + '$')
qname = re.compile('(?:(?P<prefix>' + _NCName + '):)?' + # optional prefix
                   '(?P<local>' + _NCName + ')$')

xmlns = re.compile('xmlns(?::(?P<ncname>'+_NCName+'))?$')


def XMLParser(**kw):
    # self = larky.mutablestruct(__class__='xmllib.XMLParser')
    self = ParserBase()
    self.__name__ = 'XMLParser'
    self.__class__ = XMLParser

    self.attributes = {}                     # default, to be overridden
    self.elements = {}                       # default, to be overridden

    # parsing options, settable using keyword args in __init__
    self.__accept_unquoted_attributes = 0
    self.__accept_missing_endtag_name = 0
    self.__map_case = 0
    self.__accept_utf8 = 0
    self.__translate_attribute_references = 1

    def __fixelements():
        self.__fixed = 1
        self.elements = {}
        self.__fixdict(self.__dict__)
        self.__fixclass(self.__class__)
    self.__fixelements = __fixelements

    def __fixclass(kl):
        self.__fixdict(kl.__dict__)
        for k in kl.__bases__:
            self.__fixclass(k)
    self.__fixclass = __fixclass

    def __fixdict(dict):
        for key in list(dict.keys()):
            if key[:6] == 'start_':
                tag = key[6:]
                start, end = self.elements.get(tag, (None, None))
                if start == None:
                    self.elements[tag] = getattr(self, key), end
            elif key[:4] == 'end_':
                tag = key[4:]
                start, end = self.elements.get(tag, (None, None))
                if end == None:
                    self.elements[tag] = start, getattr(self, key)
    self.__fixdict = __fixdict

    # Interface -- reset this instance.  Loses all unprocessed data
    __ParserBase_reset = self.reset
    def reset():
        __ParserBase_reset()
        self.rawdata = ''
        self.stack = []
        self.nomoretags = 0
        self.literal = 0
        self.lineno = 1
        self.__at_start = 1
        self.__seen_doctype = None
        self.__seen_starttag = 0
        self.__use_namespaces = 0
        self.__namespaces = {'xml':None}   # xml is implicitly declared
        # backward compatibility hack: if elements not overridden,
        # fill it in ourselves
        # if self.elements == XMLParser.elements:
        #     self.__fixelements()
    self.reset = reset

    # Interface -- initialize and reset this instance
    def __init__(**kw):
        self.__fixed = 0
        if 'accept_unquoted_attributes' in kw:
            self.__accept_unquoted_attributes = kw['accept_unquoted_attributes']
        if 'accept_missing_endtag_name' in kw:
            self.__accept_missing_endtag_name = kw['accept_missing_endtag_name']
        if 'map_case' in kw:
            self.__map_case = kw['map_case']
        if 'accept_utf8' in kw:
            self.__accept_utf8 = kw['accept_utf8']
        if 'translate_attribute_references' in kw:
            self.__translate_attribute_references = kw['translate_attribute_references']
        self.reset()
        return self
    self = __init__(**kw)

    # For derived classes only -- enter literal mode (CDATA) till EOF
    def setnomoretags():
        self.nomoretags = 1
        self.literal = 1
    self.setnomoretags = setnomoretags

    # For derived classes only -- enter literal mode (CDATA)
    def setliteral(*args):
        self.literal = 1
    self.setliteral = setliteral

    # Interface -- feed some data to the parser.  Call this as
    # often as you want, with as little or as much text as you
    # want (may include '\n').  (This just saves the text, all the
    # processing is done by goahead().)
    def feed(data):
        self.rawdata = self.rawdata + data
        self.goahead(0)
    self.feed = feed

    # Interface -- handle the remaining data
    def close():
        func, arg = self.goahead, 1
        q = []
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            # if not q:
            #     break
            # func, arg = q.pop(0)
            r = func(arg)
            if r == None:
                for __ in range(len(q)):
                    self._gohead_finish(*q.pop())
                break
            elif types.is_tuple(r):
                func, arg, rawdata = r
                q.append((arg, rawdata))
                continue
            else:
                # error
                r.unwrap()
                break
        if self.__fixed:
            self.__fixed = 0
            # remove self.elements so that we don't leak
            self.elements.clear()
    self.close = close

    # Interface -- translate references
    def translate_references(data, all = 1):
        if not self.__translate_attribute_references:
            return data
        i = 0
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            res = amp.search(data, i)
            if res == None:
                return data
            s = res.start(0)
            res = ref.match(data, s)
            if res == None:
                self.syntax_error("bogus `&'")
                i = s+1
                continue
            i = res.end(0)
            str = res.group(1)
            rescan = 0
            if str[0] == '#':
                if str[1] == 'x':
                    str = chr(int(str[2:], 16))
                else:
                    str = chr(int(str[1:]))
                if data[i - 1] != ';':
                    self.syntax_error("`;' missing after char reference")
                    i = i-1
            elif all:
                if str in self.entitydefs:
                    str = self.entitydefs[str]
                    rescan = 1
                elif data[i - 1] != ';':
                    self.syntax_error("bogus `&'")
                    i = s + 1 # just past the &
                    continue
                else:
                    self.syntax_error("reference to unknown entity `&%s;'" % str)
                    str = '&' + str + ';'
            elif data[i - 1] != ';':
                self.syntax_error("bogus `&'")
                i = s + 1 # just past the &
                continue

            # when we get here, str contains the translated text and i points
            # to the end of the string that is to be replaced
            data = data[:s] + str + data[i:]
            if rescan:
                i = s
            else:
                i = s + len(str)
    self.translate_references = translate_references

    # Interface - return a dictionary of all namespaces currently valid
    def getnamespace():
        nsdict = {}
        for t, d, nst in self.stack:
            nsdict.update(d)
        return nsdict
    self.getnamespace = getnamespace

    # Internal -- handle data as far as reasonable.  May leave state
    # and data to be processed by a subsequent call.  If 'end' is
    # true, force handling all data as if followed by EOF marker.
    def goahead(end):
        rawdata = self.rawdata
        i = 0
        n = len(rawdata)
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if i >= n:
                break
            if i > 0:
                self.__at_start = 0
            if self.nomoretags:
                data = rawdata[i:n]
                self.handle_data(data)
                self.lineno = self.lineno + data.count('\n')
                i = n
                break
            res = interesting.search(rawdata, i)
            if res:
                j = res.start(0)
            else:
                j = n
            if i < j:
                data = rawdata[i:j]
                if self.__at_start and space.match(data) == None:
                    self.syntax_error('illegal data at start of file')
                self.__at_start = 0
                if not self.stack and space.match(data) == None:
                    self.syntax_error('data not in content')
                if not self.__accept_utf8 and illegal.search(data):
                    self.syntax_error('illegal character in content')
                self.handle_data(data)
                self.lineno = self.lineno + data.count('\n')
            i = j
            if i == n: break
            if rawdata[i] == '<':
                if starttagopen.match(rawdata, i):
                    if self.literal:
                        data = rawdata[i]
                        self.handle_data(data)
                        self.lineno = self.lineno + data.count('\n')
                        i = i+1
                        continue
                    k = self.parse_starttag(i)
                    if k < 0: break
                    self.__seen_starttag = 1
                    self.lineno = self.lineno + rawdata[i:k].count('\n')
                    i = k
                    continue
                if endtagopen.match(rawdata, i):
                    k = self.parse_endtag(i)
                    if k < 0: break
                    self.lineno = self.lineno + rawdata[i:k].count('\n')
                    i =  k
                    continue
                if commentopen.match(rawdata, i):
                    if self.literal:
                        data = rawdata[i]
                        self.handle_data(data)
                        self.lineno = self.lineno + data.count('\n')
                        i = i+1
                        continue
                    k = self.parse_comment(i)
                    if k < 0: break
                    self.lineno = self.lineno + rawdata[i:k].count('\n')
                    i = k
                    continue
                if cdataopen.match(rawdata, i):
                    k = self.parse_cdata(i)
                    if k < 0: break
                    self.lineno = self.lineno + rawdata[i:k].count('\n')
                    i = k
                    continue
                res = xmldecl.match(rawdata, i)
                if res:
                    if not self.__at_start:
                        self.syntax_error("<?xml?> declaration not at start of document")
                    version, encoding, standalone = res.group('version',
                                                              'encoding',
                                                              'standalone')
                    if version[1:-1] != '1.0':
                        return Error("Error: only XML version 1.0 supported")
                    if encoding: encoding = encoding[1:-1]
                    if standalone: standalone = standalone[1:-1]
                    self.handle_xml(encoding, standalone)
                    i = res.end(0)
                    continue
                res = procopen.match(rawdata, i)
                if res:
                    k = self.parse_proc(i)
                    if k < 0: break
                    self.lineno = self.lineno + rawdata[i:k].count('\n')
                    i = k
                    continue
                res = doctype.match(rawdata, i)
                if res:
                    if self.literal:
                       data = rawdata[i]
                       self.handle_data(data)
                       self.lineno = self.lineno + data.count('\n')
                       i = i+1
                       continue
                    if self.__seen_doctype:
                       self.syntax_error('multiple DOCTYPE elements')
                    if self.__seen_starttag:
                       self.syntax_error('DOCTYPE not at beginning of document')
                    k = self.parse_doctype(res)
                    if k < 0: break
                    self.__seen_doctype = res.group('name')
                    if self.__map_case:
                       self.__seen_doctype = self.__seen_doctype.lower()
                    self.lineno = self.lineno + rawdata[i:k].count('\n')
                    i = k
                    continue
                res = special.match(rawdata, i)
                if res:
                    if self.literal:
                        data = rawdata[i]
                        self.handle_data(data)
                        self.lineno = self.lineno + string.count(data, '\n')
                        i = i+1
                        continue
                    self.handle_special(res.group('special'))
                    self.lineno = self.lineno + string.count(res.group(0), '\n')
                    i = res.end(0)
                    continue
                # if rawdata.startswith("<!", i):
                #     k = self.parse_declaration(i)
                #     if k < 0: break
                #     self.lineno = self.lineno + rawdata[i:k].count('\n')
                #     i = k
                #     continue
            elif rawdata[i] == '&':
                if self.literal:
                    data = rawdata[i]
                    self.handle_data(data)
                    i = i+1
                    continue
                res = charref.match(rawdata, i)
                if res != None:
                    i = res.end(0)
                    if rawdata[i-1] != ';':
                        self.syntax_error("`;' missing in charref")
                        i = i-1
                    if not self.stack:
                        self.syntax_error('data not in content')
                    self.handle_charref(res.group('char')[:-1])
                    self.lineno = self.lineno + res.group(0).count('\n')
                    continue
                res = entityref.match(rawdata, i)
                if res != None:
                    i = res.end(0)
                    if rawdata[i-1] != ';':
                        self.syntax_error("`;' missing in entityref")
                        i = i-1
                    name = res.group('name')
                    if self.__map_case:
                        name = name.lower()
                    if name in self.entitydefs:
                        rawdata = rawdata[:res.start(0)] + self.entitydefs[name] + rawdata[i:]
                        self.rawdata = rawdata
                        n = len(rawdata)
                        i = res.start(0)
                    else:
                        self.unknown_entityref(name)
                    self.lineno = self.lineno + res.group(0).count('\n')
                    continue
            elif rawdata[i] == ']':
                if self.literal:
                    data = rawdata[i]
                    self.handle_data(data)
                    i = i+1
                    continue
                if n-i < 3:
                    break
                if cdataclose.match(rawdata, i):
                    self.syntax_error("bogus `]]>'")
                self.handle_data(rawdata[i])
                i = i+1
                continue
            else:
                return Error("Error: neither < nor & ??")
            # We get here only if incomplete matches but
            # nothing else
            break
        # end while
        if i > 0:
            self.__at_start = 0
        if end and i < n:
            data = rawdata[i]
            self.syntax_error("bogus `%s'" % data)
            if not self.__accept_utf8 and illegal.search(data):
                self.syntax_error('illegal character in content')
            self.handle_data(data)
            self.lineno = self.lineno + data.count('\n')
            self.rawdata = rawdata[i+1:]
            return self.goahead, end, rawdata[i:]
        else:
            self._gohead_finish(end, rawdata[i:])
            #return self.goahead(end)
    self.goahead = goahead
    def _gohead_finish(end, rawdata):
        self.rawdata = rawdata
        if end:
            if not self.__seen_starttag:
                self.syntax_error('no elements in file')
            if self.stack:
                self.syntax_error('missing end tags')
                for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
                    if not self.stack:
                        break
                    self.finish_endtag(self.stack[-1][0])
    self._gohead_finish = _gohead_finish

    # Internal -- parse comment, return length or -1 if not terminated
    def parse_comment(i):
        rawdata = self.rawdata
        if rawdata[i:i+4] != '<!--':
            return Error("Error: unexpected call to handle_comment")
        res = commentclose.search(rawdata, i+4)
        if res == None:
            return -1
        if doubledash.search(rawdata, i+4, res.start(0)):
            self.syntax_error("`--' inside comment")
        if rawdata[res.start(0)-1] == '-':
            self.syntax_error('comment cannot end in three dashes')
        if not self.__accept_utf8 and \
           illegal.search(rawdata, i+4, res.start(0)):
            self.syntax_error('illegal character in comment')
        self.handle_comment(rawdata[i+4: res.start(0)])
        return res.end(0)
    self.parse_comment = parse_comment

    # Internal -- handle DOCTYPE tag, return length or -1 if not terminated
    def handle_decl(data):
        res = declDoctype.match(data)
        if not res:
            fail("Not a doctype:", repr(data))
        if self.literal:
            data = data[0]
            self.handle_data(data)
            self.lineno = self.lineno + data.count('\n')
            return
        if self.__seen_doctype:
            self.syntax_error('multiple DOCTYPE elements')
        if self.__seen_starttag:
            self.syntax_error('DOCTYPE not at beginning of document')
        k = self.parse_doctype(res, rawdata=data)
        self.__seen_doctype = res.group('name')
        if self.__map_case:
            self.__seen_doctype = self.__seen_doctype.lower()
        return k   # should we call update_offset()?
    self.handle_decl = handle_decl

    def parse_doctype(res, rawdata=None):
        rawdata = rawdata if rawdata else self.rawdata
        n = len(rawdata)
        name = res.group('name')
        if self.__map_case:
            name = name.lower()
        pubid, syslit = res.group('pubid', 'syslit')
        if pubid != None:
            pubid = pubid[1:-1]         # remove quotes
            pubid = ' '.join([e for e in pubid.split(" ") if e]) # normalize
        if syslit != None:
            syslit = syslit[1:-1] # remove quotes
        j = res.end(0)
        k = j
        if k >= n:
            return -1
        if rawdata[k] == '[':
            level = 0
            k = k+1
            dq = 0
            sq = dq
            for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
                if k >= n:
                    break
                c = rawdata[k]
                if not sq and c == '"':
                    dq = not dq
                elif not dq and c == "'":
                    sq = not sq
                elif sq or dq:
                    pass
                elif level <= 0 and c == ']':
                    # print("level <= 0 and c == ']'")
                    # print("k", k, "n", n)
                    # if we snipped off the <> tags, if we passed in
                    # a subset of `rawdata` to this function,
                    # then it might be that we've come to the *end*
                    # of the statement we're parsing.
                    # let's check to see if that's the case by seeing
                    # if k+1 == n, if it's not, then we continue with
                    # the default behavior..
                    if k+1 != n:
                        res = endbracket.match(rawdata, k+1)
                        if res == None:
                            return -1
                    # find first <!, if any
                    # m = decltag.search(rawdata)
                    # if m:
                    #     decltype, j = self._scan_name(
                    #         m.span()[1]+len(decltag.pattern),
                    #         res.span()[0]
                    #     )
                    self.handle_doctype(name, pubid, syslit, rawdata[j+1:k])
                    return res.end(0)
                elif c == '<':
                    level = level + 1
                elif c == '>':
                    level = level - 1
                    if level < 0:
                        self.syntax_error("bogus `>' in DOCTYPE")
                k = k+1
        res = endbracketfind.match(rawdata, k)
        # print("res = endbracketfind.match(rawdata, k)")
        if res == None:
            return -1
        if endbracket.match(rawdata, k) == None:
            self.syntax_error('garbage in DOCTYPE')
        self.handle_doctype(name, pubid, syslit, None)
        return res.end(0)
    self.parse_doctype = parse_doctype

    # Internal -- handle CDATA tag, return length or -1 if not terminated
    def parse_cdata(i):
        rawdata = self.rawdata
        if rawdata[i:i+9] != '<![CDATA[':
            return Error("Error: unexpected call to parse_cdata")
        res = cdataclose.search(rawdata, i+9)
        if res == None:
            return -1
        if not self.__accept_utf8 and \
           illegal.search(rawdata, i+9, res.start(0)):
            self.syntax_error('illegal character in CDATA')
        if not self.stack:
            self.syntax_error('CDATA not in content')
        self.handle_cdata(rawdata[i+9:res.start(0)])
        return res.end(0)
    self.parse_cdata = parse_cdata

    __xml_namespace_attributes = {'ns':None, 'src':None, 'prefix':None}
    # Internal -- handle a processing instruction tag
    def parse_proc(i):
        rawdata = self.rawdata
        end = procclose.search(rawdata, i)
        if end == None:
            return -1
        j = end.end(0) - 2   # find the end "?>" and subtract the total #
        # j = end.start(0)
        # print("procclose.search", repr(procclose.pattern), "start=", i, "span: ", end.span())
        if not self.__accept_utf8 and illegal.search(rawdata, i+2, j):
            self.syntax_error('illegal character in processing instruction')
        res = tagfind.match(rawdata, i+2)
        if res == None:
            return Error("Error: unexpected call to parse_proc")
        k = res.end(0)
        name = res.group(0)
        if self.__map_case:
            name = name.lower()
        if name == 'xml:namespace':
            self.syntax_error('old-fashioned namespace declaration')
            self.__use_namespaces = -1
            # namespace declaration
            # this must come after the <?xml?> declaration (if any)
            # and before the <!DOCTYPE> (if any).
            if self.__seen_doctype or self.__seen_starttag:
                self.syntax_error('xml:namespace declaration too late in document')
            attrdict, namespace, k = self.parse_attributes(name, k, j)
            if namespace:
                self.syntax_error('namespace declaration inside namespace declaration')
            for attrname in list(attrdict.keys()):
                if not attrname in self.__xml_namespace_attributes:
                    self.syntax_error("unknown attribute `%s' in xml:namespace tag" % attrname)
            if not 'ns' in attrdict or not 'prefix' in attrdict:
                self.syntax_error('xml:namespace without required attributes')
            prefix = attrdict.get('prefix')
            if ncname.match(prefix) == None:
                self.syntax_error('xml:namespace illegal prefix value')
                return end.end(0)
            if prefix in self.__namespaces:
                self.syntax_error('xml:namespace prefix not unique')
            self.__namespaces[prefix] = attrdict['ns']
        else:
            if name.lower() == 'xml':
                self.syntax_error('illegal processing instruction target name')
            # maintain spaces but strip spaces to the left from name
            self.handle_proc(name, rawdata[k:j].lstrip())
        return end.end(0)
    self.parse_proc = parse_proc

    # Internal -- parse attributes between i and j
    def parse_attributes(tag, i, j):
        rawdata = self.rawdata
        attrdict = {}
        namespace = {}
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if i >= j:
                break
            res = attrfind.match(rawdata, i)
            if res == None:
                break
            attrname, attrvalue = res.group('name', 'value')
            if self.__map_case:
                attrname = attrname.lower()
            i = res.end(0)
            if attrvalue == None:
                self.syntax_error("no value specified for attribute `%s'" % attrname)
                attrvalue = attrname
            elif (attrvalue[:1] == "'") and ("'" == attrvalue[-1:]) or \
                 (attrvalue[:1] == '"') and ('"' == attrvalue[-1:]):
                attrvalue = attrvalue[1:-1]
            elif not self.__accept_unquoted_attributes:
                self.syntax_error("attribute `%s' value not quoted" % attrname)
            res = xmlns.match(attrname)
            if res != None:
                # namespace declaration
                ncname = res.group('ncname')
                # NOTE: larky addition below
                self.handle_startns(ncname, attrname, attrvalue)
                namespace[ncname or ''] = attrvalue or None
                if not self.__use_namespaces:
                    self.__use_namespaces = len(self.stack)+1
                continue
            if '<' in attrvalue:
                self.syntax_error("`<' illegal in attribute value")
            if attrname in attrdict:
                self.syntax_error("attribute `%s' specified twice" % attrname)
            # attrvalue = attrvalue.translate(attrtrans)
            attrvalue = attrvalue.replace('\r', ' ')
            attrvalue = attrvalue.replace('\t', ' ')
            attrvalue = attrvalue.replace('\n', ' ')
            attrdict[attrname] = self.translate_references(attrvalue)
        return attrdict, namespace, i
    self.parse_attributes = parse_attributes

    # Internal -- handle starttag, return length or -1 if not terminated
    def parse_starttag(i):
        rawdata = self.rawdata
        # i points to start of tag
        end = endbracketfind.match(rawdata, i+1)
        if end == None:
            return -1
        tag = starttagmatch.match(rawdata, i)
        if tag == None or tag.end(0) != end.end(0):
            self.syntax_error('garbage in starttag')
            return end.end(0)
        nstag = tag.group('tagname')
        tagname = nstag
        if self.__map_case:
            nstag = nstag.lower()
            tagname = nstag
        if not self.__seen_starttag and self.__seen_doctype and \
           tagname != self.__seen_doctype:
            self.syntax_error('starttag does not match DOCTYPE')
        if self.__seen_starttag and not self.stack:
            self.syntax_error('multiple elements on top level')
        k, j = tag.span('attrs')
        # Because k, j are the current tag's attrib indices, i is the current tag index in the whole xml string,
        # if not plus i, it will keep getting attrib between k and j in the first tag, not the current tag
        attrdict, nsdict, k = self.parse_attributes(tagname, i+k, i+j)
        self.stack.append((tagname, nsdict, nstag))
        res = qname.match(tagname) if self.__use_namespaces else None
        if res != None:
            prefix, nstag = res.group('prefix', 'local')
            if prefix == None:
                prefix = ''
            ns = None
            for t, d, nst in self.stack:
                if prefix in d:
                    ns = d[prefix]
            if ns == None and prefix != '':
                ns = self.__namespaces.get(prefix)
            if ns != None:
                nstag = ns + ' ' + nstag
            elif prefix != '':
                nstag = prefix + ':' + nstag # undo split
            self.stack[-1] = tagname, nsdict, nstag
        # translate namespace of attributes
        attrnamemap = {} # map from new name to old name (used for error reporting)
        for key in list(attrdict.keys()):
            attrnamemap[key] = key
        if self.__use_namespaces:
            nattrdict = {}
            # NOTE: this is done here (instead of after constructing the nattridct)
            # to preserve the expected order of insertion
            if nsdict:
                nattrdict['nsmap'] = {v:k for k, v in nsdict.items()}
            for key, val in list(attrdict.items()):
                okey = key
                res = qname.match(key)
                if res != None:
                    aprefix, key = res.group('prefix', 'local')
                    if self.__map_case:
                        key = key.lower()
                    if aprefix != None:
                        ans = None
                        for t, d, nst in self.stack:
                            if aprefix in d:
                                ans = d[aprefix]
                        if ans == None:
                            ans = self.__namespaces.get(aprefix)
                        if ans != None:
                            key = ans + ' ' + key
                        else:
                            key = aprefix + ':' + key
                nattrdict[key] = val
                attrnamemap[key] = okey
            attrdict = nattrdict
        attributes = self.attributes.get(nstag)
        if attributes != None:
            for key in list(attrdict.keys()):
                if not key in attributes:
                    self.syntax_error("unknown attribute `%s' in tag `%s'" % (attrnamemap[key], tagname))
            for key, val in list(attributes.items()):
                if val != None and not key in attrdict:
                    attrdict[key] = val
        method = self.elements.get(nstag, (None, None))[0]
        # print("xmllib: ", nstag, "!", attrdict, "!", method, "!", nsdict)
        self.finish_starttag(nstag, attrdict, method)
        if tag.group('slash') == '/':
            self.finish_endtag(tagname)
        return tag.end(0)
    self.parse_starttag = parse_starttag

    # Internal -- parse endtag
    def parse_endtag(i):
        rawdata = self.rawdata
        end = endbracketfind.match(rawdata, i+1)
        if end == None:
            return -1
        res = tagfind.match(rawdata, i+2)
        if res == None:
            if self.literal:
                self.handle_data(rawdata[i])
                return i+1
            if not self.__accept_missing_endtag_name:
                self.syntax_error('no name specified in end tag')
            tag = self.stack[-1][0]
            k = i+2
        else:
            tag = res.group(0)
            if self.__map_case:
                tag = tag.lower()
            if self.literal:
                if not self.stack or tag != self.stack[-1][0]:
                    self.handle_data(rawdata[i])
                    return i+1
            k = res.end(0)
        if endbracket.match(rawdata, k) == None:
            self.syntax_error('garbage in end tag')
        self.finish_endtag(tag)
        return end.end(0)
    self.parse_endtag = parse_endtag

    # Internal -- finish processing of start tag
    def finish_starttag(tagname, attrdict, method):
        if method != None:
            self.handle_starttag(tagname, method, attrdict)
        else:
            self.unknown_starttag(tagname, attrdict)
    self.finish_starttag = finish_starttag

    # Internal -- finish processing of end tag
    def finish_endtag(tag):
        self.literal = 0
        if not tag:
            self.syntax_error('name-less end tag')
            found = len(self.stack) - 1
            if found < 0:
                self.unknown_endtag(tag)
                return
        else:
            found = -1
            for i in range(len(self.stack)):
                if tag == self.stack[i][0]:
                    found = i
            if found == -1:
                self.syntax_error('unopened end tag')
                return
        ns = []
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if len(self.stack) <= found:
                break
            if found < len(self.stack) - 1:
                self.syntax_error('missing close tag for %s' % self.stack[-1][2])
            nstag = self.stack[-1][2]
            ns.append(self.stack[-1][1])
            method = self.elements.get(nstag, (None, None))[1]
            if method != None:
                self.handle_endtag(nstag, method)
            else:
                self.unknown_endtag(nstag)
            if self.__use_namespaces == len(self.stack):
                self.__use_namespaces = 0
            operator.delitem(self.stack, -1)

        for i in ns:
            if not i:
                continue
            for k in reversed(i):
                self.handle_endns(k)
            # if len(k) != 1:
            #     fail("ns length is greater than 1: k: %s, i: %s" %(k, i))
            # self.handle_endns(k[0])
        ns.clear()
    self.finish_endtag = finish_endtag

    # Overridable -- handle xml processing instruction
    def handle_xml(encoding, standalone):
        pass
    self.handle_xml = handle_xml

    # Overridable -- handle DOCTYPE
    def handle_doctype(tag, pubid, syslit, data):
        pass
    self.handle_doctype = handle_doctype

    # Example -- handle special instructions, could be overridden
    def handle_special(data):
        pass
    self.handle_special = handle_special

    # Overridable -- handle start tag
    def handle_starttag(tag, method, attrs):
        method(attrs)
    self.handle_starttag = handle_starttag

    # Overridable -- handle end tag
    def handle_endtag(tag, method):
        method()
    self.handle_endtag = handle_endtag

    # Example -- handle character reference, no need to override
    def handle_charref(name):

        def _try_handle_charref(name):
            if name[0] == 'x':
                n = int(name[1:], 16)
            else:
                n = int(name)
            return n
        rval = try_(lambda :_try_handle_charref(name))\
         .except_(lambda: self.unknown_charref(name))\
         .build()
        if rval.is_err:
            return

        n = rval.unwrap()
        if not ((0 <= n) and (n <= 255)):
            self.unknown_charref(name)
            return
        self.handle_data(chr(n))
    self.handle_charref = handle_charref

    # Definition of entities -- derived classes may override
    self.entitydefs = {'lt': '&#60;',        # must use charref
                  'gt': '&#62;',
                  'amp': '&#38;',       # must use charref
                  'quot': '&#34;',
                  'apos': '&#39;',
                  }

    # Example -- handle data, should be overridden
    def handle_data(data):
        pass
    self.handle_data = handle_data

    # Example -- handle startns, should be overridden
    def handle_startns(prefix, qualified, href):
        pass
        # print("start-ns - name:", prefix, "qname:", qualified, "href:", href)
    self.handle_startns = handle_startns

    # Example -- handle end, should be overridden
    def handle_endns(prefix):
        pass
        # for k in ns_map.keys():
        #     print("end-ns - name:", k or None)
    self.handle_endns = handle_endns

    # Example -- handle cdata, could be overridden
    def handle_cdata(data):
        pass
    self.handle_cdata = handle_cdata

    # Example -- handle comment, could be overridden
    def handle_comment(data):
        pass
    self.handle_comment = handle_comment

    # Example -- handle processing instructions, could be overridden
    def handle_proc(name, data):
        pass
    self.handle_proc = handle_proc
    self.handle_pi = handle_proc     # Added for Larky

    # Example -- handle relatively harmless syntax errors, could be overridden
    def syntax_error(message):
        return Error("Error: Syntax error at line %d: %s" % (self.lineno, message))
    self.syntax_error = syntax_error

    # To be overridden -- handlers for unknown objects
    def unknown_starttag(tag, attrs): pass
    self.unknown_starttag = unknown_starttag
    def unknown_endtag(tag): pass
    self.unknown_endtag = unknown_endtag
    def unknown_charref(ref): pass
    self.unknown_charref = unknown_charref
    def unknown_entityref(name):
        self.syntax_error("reference to unknown entity `&%s;'" % name)
    self.unknown_entityref = unknown_entityref
    return self


def TestXMLParser(**kw):
    self = larky.mutablestruct(__class__='TestXMLParser')

    def __init__(**kw):
        self = XMLParser(**kw)
        self.__class__ = 'TestXMLParser'
        return self
    self = __init__(**kw)

    def handle_xml(encoding, standalone):
        self.flush()
        print(('xml: encoding =',encoding,'standalone =',standalone))
    self.handle_xml = handle_xml

    def handle_doctype(tag, pubid, syslit, data):
        self.flush()
        print(('DOCTYPE:',tag, repr(data)))
    self.handle_doctype = handle_doctype

    def handle_data(data):
        self.testdata = self.testdata + data
        if len(repr(self.testdata)) >= 70:
            self.flush()
    self.handle_data = handle_data

    def flush():
        data = self.testdata
        if data:
            self.testdata = ""
            print(('data:', repr(data)))
    self.flush = flush

    def handle_cdata(data):
        self.flush()
        print(('cdata:', repr(data)))
    self.handle_cdata = handle_cdata

    def handle_proc(name, data):
        self.flush()
        print(('processing:',name,repr(data)))
    self.handle_proc = handle_proc

    def handle_comment(data):
        self.flush()
        r = repr(data)
        if len(r) > 68:
            r = r[:32] + '...' + r[-32:]
        print(('comment:', r))
    self.handle_comment = handle_comment

    def syntax_error(message):
        print(('error at line %d:' % self.lineno, message))
    self.syntax_error = syntax_error

    def unknown_starttag(tag, attrs):
        self.flush()
        if not attrs:
            print('start tag: <' + tag + '>')
        else:
            print('start tag: <' + tag)
            print(' ')
            for name, value in list(attrs.items()):
                print(name + '=' + '"' + value + '"')
                print(' ')
            print('>')
    self.unknown_starttag = unknown_starttag

    def unknown_endtag(tag):
        self.flush()
        print('end tag: </' + tag + '>')
    self.unknown_endtag = unknown_endtag

    def unknown_entityref(ref):
        self.flush()
        print('*** unknown entity ref: &' + ref + ';')
    self.unknown_entityref = unknown_entityref

    def unknown_charref(ref):
        self.flush()
        print('*** unknown char ref: &#' + ref + ';')
    self.unknown_charref = unknown_charref

    def close():
        XMLParser.close(self)
        self.flush()
    self.close = close
    return self


xmllib = larky.struct(
    XMLParser=XMLParser,
    TestXMLParser=TestXMLParser,
    version=version
)