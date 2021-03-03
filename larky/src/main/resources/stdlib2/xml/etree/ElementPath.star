def xpath_tokenizer(pattern, namespaces=None):
    """
    ''
    """
def get_parent_map(context):
    """
    '{*}'
    """
def _prepare_tag(tag):
    """
    '{*}*'
    """
        def select(context, result):
            """
            '{}*'
            """
        def select(context, result):
            """
            '{'
            """
        def select(context, result):
            """
            '}*'
            """
        def select(context, result):
            """
            f"internal parser error, got {tag}
            """
def prepare_child(next, token):
    """
    '{}'
    """
        def select(context, result):
            """
            *
            """
        def select(context, result):
            """
            '{}'
            """
        def select(context, result):
            """
             FIXME: raise error if .. is applied at toplevel?

            """
def prepare_predicate(next, token):
    """
     FIXME: replace with real parser!!! refs:
     http://effbot.org/zone/simple-iterator-parser.htm
     http://javascript.crockford.com/tdop/tdop.html

    """
        def select(context, result):
            """
            @-='
            """
        def select(context, result):
            """
            -
            """
        def select(context, result):
            """
            .='
            """
            def select(context, result):
                """

                """
            def select(context, result):
                """

                """
        def select(context, result):
            """
             FIXME: what if the selector is "*" ?

            """
def _SelectorContext:
    """
     --------------------------------------------------------------------


     Generate all matching objects.


    """
def iterfind(elem, path, namespaces=None):
    """
     compile selector pattern

    """
def find(elem, path, namespaces=None):
    """

     Find all matching objects.


    """
def findall(elem, path, namespaces=None):
    """

     Find text for first matching object.


    """
def findtext(elem, path, default=None, namespaces=None):
    """

    """
