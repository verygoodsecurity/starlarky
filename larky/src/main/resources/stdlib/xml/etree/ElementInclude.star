def FatalIncludeError(SyntaxError):
    """

     Default loader.  This loader reads an included resource from disk.

     @param href Resource reference.
     @param parse Parse mode.  Either "xml" or "text".
     @param encoding Optional text encoding (UTF-8 by default for "text").
     @return The expanded resource.  If the parse mode is "xml", this
        is an ElementTree instance.  If the parse mode is "text", this
        is a Unicode string.  If the loader fails, it can return None
        or raise an OSError exception.
     @throws OSError If the loader fails to load the resource.


    """
def default_loader(href, parse, encoding=None):
    """
    xml
    """
def include(elem, loader=None):
    """
     look for xinclude elements

    """
