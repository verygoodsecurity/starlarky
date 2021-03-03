def PackageNotFoundError(ModuleNotFoundError):
    """
    The package was not found.
    """
2021-03-02 20:54:00,851 : INFO : tokenize_signature : --> do i ever get here?
def EntryPoint(
        collections.namedtuple('EntryPointBase', 'name value group')):
    """
    An entry point as defined by Python packaging conventions.

        See `the packaging docs on entry points
        <https://packaging.python.org/specifications/entry-points/>`_
        for more information.
    
    """
    def load(self):
        """
        Load the entry point from its definition. If only a module
                is indicated by the value, return that module. Otherwise,
                return the named object.
        
        """
    def extras(self):
        """
        r'\w+'
        """
    def _from_config(cls, config):
        """
        '='
        """
    def __iter__(self):
        """

                Supply iter so one may construct dicts of EntryPoints easily.
        
        """
    def __reduce__(self):
        """
        A reference to a path in a package
        """
    def read_text(self, encoding='utf-8'):
        """
        'rb'
        """
    def locate(self):
        """
        Return a path-like object for this path
        """
def FileHash:
    """
    '='
    """
    def __repr__(self):
        """
        '<FileHash mode: {} value: {}>'
        """
def Distribution:
    """
    A Python distribution package.
    """
    def read_text(self, filename):
        """
        Attempt to load metadata file given by the name.

                :param filename: The name of the file in the distribution info.
                :return: The text if found, otherwise None.
        
        """
    def locate_file(self, path):
        """

                Given a path to a file in this distribution, return a path
                to it.
        
        """
    def from_name(cls, name):
        """
        Return the Distribution for the given package name.

                :param name: The name of the distribution package to search for.
                :return: The Distribution instance (or subclass thereof) for the named
                    package, if found.
                :raises PackageNotFoundError: When the named package's distribution
                    metadata cannot be found.
        
        """
    def discover(cls, **kwargs):
        """
        Return an iterable of Distribution objects for all packages.

                Pass a ``context`` or pass keyword arguments for constructing
                a context.

                :context: A ``DistributionFinder.Context`` object.
                :return: Iterable of Distribution objects for all packages.
        
        """
    def at(path):
        """
        Return a Distribution for the indicated metadata path

                :param path: a string or path-like object
                :return: a concrete Distribution instance for the path
        
        """
    def _discover_resolvers():
        """
        Search the meta_path for resolvers.
        """
    def metadata(self):
        """
        Return the parsed metadata for this Distribution.

                The returned object will have keys that name the various bits of
                metadata.  See PEP 566 for details.
        
        """
    def version(self):
        """
        Return the 'Version' metadata for the distribution package.
        """
    def entry_points(self):
        """
        'entry_points.txt'
        """
    def files(self):
        """
        Files in this distribution.

                :return: List of PackagePath for this distribution or None

                Result is `None` if the metadata file that enumerates files
                (i.e. RECORD for dist-info or SOURCES.txt for egg-info) is
                missing.
                Result may be empty if the metadata exists but is empty.
        
        """
        def make_file(name, hash=None, size_str=None):
            """

                    Read the lines of RECORD
        
            """
    def _read_files_egginfo(self):
        """

                SOURCES.txt might contain literal commas, so wrap each line
                in quotes.
        
        """
    def requires(self):
        """
        Generated requirements specified for this Distribution
        """
    def _read_dist_info_reqs(self):
        """
        'Requires-Dist'
        """
    def _read_egg_info_reqs(self):
        """
        'requires.txt'
        """
    def _deps_from_requires_text(cls, source):
        """
        'line'
        """
    def _read_sections(lines):
        """
        r'\[(.*)\]$'
        """
    def _convert_egg_info_reqs_to_simple_reqs(sections):
        """

                Historically, setuptools would solicit and store 'extra'
                requirements, including those with environment markers,
                in separate sections. More modern tools expect each
                dependency to be defined separately, with any relevant
                extras and environment markers attached directly to that
                requirement. This method converts the former to the
                latter. See _test_deps_from_requires_text for an example.
        
        """
        def make_condition(name):
            """
            'extra == "{name}"'
            """
        def parse_condition(section):
            """
            ''
            """
def DistributionFinder(MetaPathFinder):
    """

        A MetaPathFinder capable of discovering installed distributions.
    
    """
    def Context:
    """

            Keyword arguments presented by the caller to
            ``distributions()`` or ``Distribution.discover()``
            to narrow the scope of a search for distributions
            in all DistributionFinders.

            Each DistributionFinder may expect any parameters
            and should attempt to honor the canonical
            parameters defined below when appropriate.
        
    """
        def __init__(self, **kwargs):
            """

                        The path that a distribution finder should search.

                        Typically refers to Python package paths and defaults
                        to ``sys.path``.
            
            """
    def find_distributions(self, context=Context()):
        """

                Find distributions.

                Return an iterable of all Distribution instances capable of
                loading the metadata for packages matching the ``context``,
                a DistributionFinder.Context instance.
        
        """
def FastPath:
    """

        Micro-optimized class for searching a path for
        children.
    
    """
    def __init__(self, root):
        """
        ''
        """
    def zip_children(self):
        """
        '.egg'
        """
    def search(self, name):
        """
         legacy case:

        """
def Prepared:
    """

        A prepared search for metadata on a possibly-named package.
    
    """
    def __init__(self, name):
        """
        '-'
        """
def MetadataPathFinder(DistributionFinder):
    """

            Find distributions.

            Return an iterable of all Distribution instances capable of
            loading the metadata for packages matching ``context.name``
            (or all names if ``None`` indicated) along the paths in the list
            of directories ``context.path``.
        
    """
    def _search_paths(cls, name, paths):
        """
        Find metadata directories in paths heuristically.
        """
def PathDistribution(Distribution):
    """
    Construct a distribution from a path to the metadata directory.

            :param path: A pathlib.Path or similar object supporting
                         .joinpath(), __div__, .parent, and .read_text().
        
    """
    def read_text(self, filename):
        """
        'utf-8'
        """
    def locate_file(self, path):
        """
        Get the ``Distribution`` instance for the named package.

            :param distribution_name: The name of the distribution package as a string.
            :return: A ``Distribution`` instance (or subclass thereof).
    
        """
def distributions(**kwargs):
    """
    Get all ``Distribution`` instances in the current environment.

        :return: An iterable of ``Distribution`` instances.
    
    """
def metadata(distribution_name):
    """
    Get the metadata for the named package.

        :param distribution_name: The name of the distribution package to query.
        :return: An email.Message containing the parsed metadata.
    
    """
def version(distribution_name):
    """
    Get the version string for the named package.

        :param distribution_name: The name of the distribution package to query.
        :return: The version string for the package as defined in the package's
            "Version" metadata key.
    
    """
def entry_points():
    """
    Return EntryPoint objects for all installed packages.

        :return: EntryPoint objects for all installed packages.
    
    """
def files(distribution_name):
    """
    Return a list of files for the named package.

        :param distribution_name: The name of the distribution package to query.
        :return: List of files composing the distribution.
    
    """
def requires(distribution_name):
    """

        Return a list of requirements for the named package.

        :return: An iterator of requirements, suitable for
        packaging.requirement.Requirement.
    
    """
