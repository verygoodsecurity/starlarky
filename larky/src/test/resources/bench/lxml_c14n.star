load("@vendor//lxml", "etree")
def process():
    tree = etree.XML("<root b='2' a='1'><child>text</child></root>")
    return etree.tostring(tree, method="c14n")
output = process()
