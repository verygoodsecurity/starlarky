def RobotFileParser:
    """
     This class provides a set of methods to read, parse and answer
        questions about a single robots.txt file.

    
    """
    def __init__(self, url=''):
        """
        Returns the time the robots.txt file was last fetched.

                This is useful for long-running web spiders that need to
                check for new robots.txt files periodically.

        
        """
    def modified(self):
        """
        Sets the time the robots.txt file was last fetched to the
                current time.

        
        """
    def set_url(self, url):
        """
        Sets the URL referring to a robots.txt file.
        """
    def read(self):
        """
        Reads the robots.txt URL and feeds it to the parser.
        """
    def _add_entry(self, entry):
        """
        *
        """
    def parse(self, lines):
        """
        Parse the input lines from a robots.txt file.

                We allow that a user-agent: line is not preceded by
                one or more blank lines.
        
        """
    def can_fetch(self, useragent, url):
        """
        using the parsed robots.txt decide if useragent can fetch url
        """
    def crawl_delay(self, useragent):
        """
        '\n\n'
        """
def RuleLine:
    """
    A rule line is a single "Allow:" (allowance==True) or "Disallow:"
           (allowance==False) followed by a path.
    """
    def __init__(self, path, allowance):
        """
        ''
        """
    def applies_to(self, filename):
        """
        *
        """
    def __str__(self):
        """
        Allow
        """
def Entry:
    """
    An entry has one or more user-agents and zero or more rulelines
    """
    def __init__(self):
        """
        f"User-agent: {agent}
        """
    def applies_to(self, useragent):
        """
        check if this entry applies to the specified agent
        """
    def allowance(self, filename):
        """
        Preconditions:
                - our agent applies to this entry
                - filename is URL decoded
        """
