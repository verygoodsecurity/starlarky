def register(PyPIRCCommand):
    """
    register the distribution with the Python package index
    """
    def initialize_options(self):
        """
         setting options for the `check` subcommand

        """
    def run(self):
        """
         Run sub commands

        """
    def check_metadata(self):
        """
        Deprecated API.
        """
    def _set_config(self):
        """
        ''' Reads the configuration file and set attributes.
                '''
        """
    def classifiers(self):
        """
        ''' Fetch the list of classifiers from the server.
                '''
        """
    def verify_metadata(self):
        """
        ''' Send the metadata to the package index server to be checked.
                '''
        """
    def send_metadata(self):
        """
        ''' Send the metadata to the package index server.

                    Well, do the following:
                    1. figure who the user is, and then
                    2. send the data as a Basic auth'ed POST.

                    First we try to read the username/password from $HOME/.pypirc,
                    which is a ConfigParser-formatted file with a section
                    [distutils] containing username and password entries (both
                    in clear text). Eg:

                        [distutils]
                        index-servers =
                            pypi

                        [pypi]
                        username: fred
                        password: sekrit

                    Otherwise, to figure who the user is, we offer the user three
                    choices:

                     1. use existing login,
                     2. register as a new user, or
                     3. set the password to a random string and email the user.

                '''
        """
    def build_post_data(self, action):
        """
         figure the data to send - the metadata plus some additional
         information used by the package server

        """
    def post_to_server(self, data, auth=None):
        """
        ''' Post a query to the server, and return a string response.
                '''
        """
