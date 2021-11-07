load("@stdlib//types", types="types")
load("@stdlib//xml/etree/ElementTree", Element="Element", tostring="tostring")
load("@vendor//option/result", Error="Error")


LxmlSyntaxError = Error("LxmlSyntaxError")


def _FakeIncrementalFileWriter(output_file):
    """Replacement for _IncrementalFileWriter of lxml.
    Uses ElementTree to build xml in memory.

    """
    self = larky.mutablestruct(__name__='_FakeIncrementalFileWriter', __class__=_FakeIncrementalFileWriter)

    def __init__(output_file):
        self._element_stack = []
        self._top_element = None
        self._file = output_file
        self._have_root = False
        return self
    self = __init__(output_file)


    def push(tag, attrib=None, nsmap=None, **_extra):
        """Create a new xml element using a context manager.
        The elements are written when the top level context is left.

        This is for code compatibility only as it is quite slow.
        """
        # __enter__ part
        self._have_root = True
        if attrib == None:
            attrib = {}
        if nsmap:
            _extra['nsmap'] = nsmap
        self._top_element = Element(tag, attrib=attrib, **_extra)
        self._top_element.text = ""
        self._top_element.tail = ""
        self._element_stack.append(self._top_element)
        return self

    def pop():
        # __exit__ part
        el = self._element_stack.pop()
        if self._element_stack:
            parent = self._element_stack[-1]
            parent.append(self._top_element)
            self._top_element = parent
        else:
            self._write_element(el)
            self._top_element = None
        return self

    self.element = larky.mutablestruct(
        __enter__=push,
        __call__=push,
        __exit__=pop,
    )

    def write(arg):
        """Write a string or subelement."""

        if types.is_string(arg):
            # it is not allowed to write a string outside of an element
            if self._top_element == None:
                return LxmlSyntaxError().unwrap()

            if len(self._top_element) == 0:
                # element has no children: add string to text
                self._top_element.text += arg
            else:
                # element has children: add string to tail of last child
                self._top_element[-1].tail += arg

        else:
            if self._top_element != None:
                self._top_element.append(arg)
            elif not self._have_root:
                self._write_element(arg)
            else:
                return LxmlSyntaxError().unwrap()
    self.write = write

    def _write_element(element):
        xml = tostring(element)
        self._file.write(xml)
    self._write_element = _write_element

    def __enter__():
        return self
    self.__enter__ = __enter__

    def __exit__(type, value, traceback):
        # without root the xml document is incomplete
        if not self._have_root:
            return LxmlSyntaxError().unwrap()
    self.__exit__ = __exit__
    return self


def xmlfile(output_file, buffered=False, encoding=None, close=False):
    """Context manager that can replace lxml.etree.xmlfile."""
    self = larky.mutablestruct(__name__='xmlfile', __class__=xmlfile)

    def __init__(output_file, buffered, encoding, close):
        if types.is_string(output_file):
            fail("Output file is string (probably file path), expected io type")
        self._file = output_file
        self._close = close
        return self
    self = __init__(output_file, buffered, encoding, close)

    def __enter__():
        return _FakeIncrementalFileWriter(self._file)
    self.__enter__ = __enter__

    def __exit__(type, value, traceback):
        if self._close == True:
            self._file.close()
    self.__exit__ = __exit__
    return self

