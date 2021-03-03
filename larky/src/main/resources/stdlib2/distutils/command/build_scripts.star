def build_scripts(Command):
    """
    \"build\" scripts (copy and fixup #! line)
    """
    def initialize_options(self):
        """
        'build'
        """
    def get_source_files(self):
        """
        r"""Copy each script listed in 'self.scripts'; if it's marked as a
                Python script in the Unix way (first line matches 'first_line_re',
                ie. starts with "\#!" and contains "python"), then adjust the first
                line to refer to the current Python interpreter as we copy.
        
        """
def build_scripts_2to3(build_scripts, Mixin2to3):
