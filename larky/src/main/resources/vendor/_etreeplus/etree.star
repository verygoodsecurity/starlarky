# https://github.com/guillaume-humbert/python-xmlschema/blob/master/xmlschema/etree.py

# def etree_tostring(elem, namespaces=None, indent='', max_lines=None, spaces_for_tab=4, xml_declaration=False):
#     """
#     Serialize an Element tree to a string. Tab characters are replaced by whitespaces.
#     :param elem: the Element instance.
#     :param namespaces: is an optional mapping from namespace prefix to URI. Provided namespaces are \
#     registered before serialization.
#     :param indent: the base line indentation.
#     :param max_lines: if truncate serialization after a number of lines (default: do not truncate).
#     :param spaces_for_tab: number of spaces for replacing tab characters (default is 4).
#     :param xml_declaration: if set to `True` inserts the XML declaration at the head.
#     :return: a Unicode string.
#     """
#     def reindent(line):
#         if not line:
#             return line
#         elif line.startswith(min_indent):
#             return line[start:] if start >= 0 else indent[start:] + line
#         else:
#             return indent + line
#
#     if isinstance(elem, etree_element):
#         if namespaces:
#             for prefix, uri in namespaces.items():
#                 if not re.match(r'ns\d+$', prefix):
#                     etree_register_namespace(prefix, uri)
#         tostring = ElementTree.tostring
#
#     elif isinstance(elem, py_etree_element):
#         if namespaces:
#             for prefix, uri in namespaces.items():
#                 if not re.match(r'ns\d+$', prefix):
#                     PyElementTree.register_namespace(prefix, uri)
#         tostring = PyElementTree.tostring
#
#     elif lxml_etree is not None:
#         if namespaces:
#             for prefix, uri in namespaces.items():
#                 if prefix and not re.match(r'ns\d+$', prefix):
#                     lxml_etree_register_namespace(prefix, uri)
#         tostring = lxml_etree.tostring
#     else:
#         raise XMLSchemaTypeError("cannot serialize %r: lxml library not available." % type(elem))
#
#     if PY3:
#         xml_text = tostring(elem, encoding="unicode").replace('\t', ' ' * spaces_for_tab)
#     else:
#         xml_text = unicode(tostring(elem)).replace('\t', ' ' * spaces_for_tab)
#
#     lines = ['<?xml version="1.0" encoding="UTF-8"?>'] if xml_declaration else []
#     lines.extend(xml_text.splitlines())
#     while lines and not lines[-1].strip():
#         lines.pop(-1)
#
#     last_indent = ' ' * min(k for k in range(len(lines[-1])) if lines[-1][k] != ' ')
#     if len(lines) > 2:
#         child_indent = ' ' * min(k for line in lines[1:-1] for k in range(len(line)) if line[k] != ' ')
#         min_indent = min(child_indent, last_indent)
#     else:
#         min_indent = child_indent = last_indent
#
#     start = len(min_indent) - len(indent)
#
#     if max_lines is not None and len(lines) > max_lines + 2:
#         lines = lines[:max_lines] + [child_indent + '...'] * 2 + lines[-1:]
#
#     return '\n'.join(reindent(line) for line in lines)
#
#
# def etree_iterpath(elem, tag=None, path='.', namespaces=None, add_position=False):
#     """
#     Creates an iterator for the element and its subelements that yield elements and paths.
#     If tag is not `None` or '*', only elements whose matches tag are returned from the iterator.
#     :param elem: the element to iterate.
#     :param tag: tag filtering.
#     :param path: the current path, '.' for default.
#     :param add_position: add context position to child elements that appear multiple times.
#     :param namespaces: is an optional mapping from namespace prefix to URI.
#     """
#     if tag == "*":
#         tag = None
#     if tag is None or elem.tag == tag:
#         yield elem, path
#
#     if add_position:
#         children_tags = Counter([e.tag for e in elem])
#         positions = Counter([t for t in children_tags if children_tags[t] > 1])
#     else:
#         positions = ()
#
#     for child in elem:
#         if callable(child.tag):
#             continue  # Skip lxml comments
#
#         child_name = child.tag if namespaces is None else qname_to_prefixed(child.tag, namespaces)
#         if path == '/':
#             child_path = '/%s' % child_name
#         elif path:
#             child_path = '/'.join((path, child_name))
#         else:
#             child_path = child_name
#
#         if child.tag in positions:
#             child_path += '[%d]' % positions[child.tag]
#             positions[child.tag] += 1
#
#         for _child, _child_path in etree_iterpath(child, tag, child_path, namespaces):
#             yield _child, _child_path
#
#
# def etree_getpath(elem, root, namespaces=None, relative=True, add_position=False):
#     """
#     Returns the XPath path from *root* to descendant *elem* element.
#     :param elem: the descendant element.
#     :param root: the root element.
#     :param namespaces: is an optional mapping from namespace prefix to URI.
#     :param relative: returns a relative path.
#     :param add_position: add context position to child elements that appear multiple times.
#     :return: An XPath expression or `None` if *elem* is not a descendant of *root*.
#     """
#     if relative:
#         path = '.'
#     elif namespaces:
#         path = '/%s' % qname_to_prefixed(root.tag, namespaces)
#     else:
#         path = '/%s' % root.tag
#
#     for e, path in etree_iterpath(root, elem.tag, path, namespaces, add_position):
#         if e is elem:
#             return path