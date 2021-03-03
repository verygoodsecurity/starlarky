def alternates(members):
    """
    (
    """
def build_pattern(mapping=MAPPING):
    """
    ' | '
    """
def FixImports(fixer_base.BaseFix):
    """
     This is overridden in fix_imports2.

    """
    def build_pattern(self):
        """
        |
        """
    def compile_pattern(self):
        """
         We override this, so MAPPING can be pragmatically altered and the
         changes will be reflected in PATTERN.

        """
    def match(self, node):
        """
         Module usage could be in the trailer of an attribute lookup, so we
         might have nested matches when "bare_with_attr" is present.

        """
    def start_tree(self, tree, filename):
        """
        module_name
        """
