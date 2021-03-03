def _comparable_version(version):
    """
    '._+-'
    """
def libc_ver(executable=None, lib='', version='', chunksize=16384):
    """
     Tries to determine the libc version that the file executable
            (which defaults to the Python interpreter) is linked against.

            Returns a tuple of strings (lib,version) which default to the
            given parameters in case the lookup fails.

            Note that the function has intimate knowledge of how different
            libc versions add symbols to the executable and thus is probably
            only useable for executables compiled using gcc.

            The file is read and scanned in chunks of chunksize bytes.

    
    """
def _norm_version(version, build=''):
    """
     Normalize the version and build strings and return a single
            version string using the format major.minor.build (or patchlevel).
    
    """
2021-03-02 20:53:44,728 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:44,728 : INFO : tokenize_signature : --> do i ever get here?
def _syscmd_ver(system='', release='', version='',

               supported_platforms=('win32', 'win16', 'dos')):
    """
     Tries to figure out the OS version used and returns
            a tuple (system, release, version).

            It uses the "ver" shell command for this which is known
            to exists on Windows, DOS. XXX Others too ?

            In case this fails, the given parameters are used as
            defaults.

    
    """
def win32_is_iot():
    """
    'IoTUAP'
    """
def win32_edition():
    """
    r'SOFTWARE\Microsoft\Windows NT\CurrentVersion'
    """
def win32_ver(release='', version='', csd='', ptype=''):
    """
    '{0}.{1}.{2}'
    """
def _mac_ver_xml():
    """
    '/System/Library/CoreServices/SystemVersion.plist'
    """
def mac_ver(release='', versioninfo=('', '', ''), machine=''):
    """
     Get macOS version information and return it as tuple (release,
            versioninfo, machine) with versioninfo being a tuple (version,
            dev_stage, non_release_version).

            Entries which cannot be determined are set to the parameter values
            which default to ''. All tuple entries are strings.
    
    """
def _java_getprop(name, default):
    """
    ''
    """
def system_alias(system, release, version):
    """
     Returns (system, release, version) aliased to common
            marketing names used for some systems.

            It also does some reordering of the information in some cases
            where it would otherwise cause confusion.

    
    """
def _platform(*args):
    """
     Helper to format the platform string in a filename
            compatible format e.g. "system-version-machine".
    
    """
def _node(default=''):
    """
     Helper to determine the node name of this machine.
    
    """
def _follow_symlinks(filepath):
    """
     In case filepath is a symlink, follow it until a
            real file is reached.
    
    """
def _syscmd_uname(option, default=''):
    """
     Interface to the system's uname command.
    
    """
def _syscmd_file(target, default=''):
    """
     Interface to the system's file command.

            The function uses the -b option of the file command to have it
            omit the filename in its output. Follow the symlinks. It returns
            default in case the command should fail.

    
    """
def architecture(executable=sys.executable, bits='', linkage=''):
    """
     Queries the given executable (defaults to the Python interpreter
            binary) for various architecture information.

            Returns a tuple (bits, linkage) which contains information about
            the bit architecture and the linkage format used for the
            executable. Both values are returned as strings.

            Values that cannot be determined are returned as given by the
            parameter presets. If bits is given as '', the sizeof(pointer)
            (or sizeof(long) on Python version < 1.5.2) is used as
            indicator for the supported pointer size.

            The function relies on the system's "file" command to do the
            actual work. This is available on most if not all Unix
            platforms. On some non-Unix platforms where the "file" command
            does not exist and the executable is set to the Python interpreter
            binary defaults from _default_architecture are used.

    
    """
def uname():
    """
     Fairly portable uname interface. Returns a tuple
            of strings (system, node, release, version, machine, processor)
            identifying the underlying platform.

            Note that unlike the os.uname function this also returns
            possible processor information as an additional tuple entry.

            Entries which cannot be determined are set to ''.

    
    """
def system():
    """
     Returns the system/OS name, e.g. 'Linux', 'Windows' or 'Java'.

            An empty string is returned if the value cannot be determined.

    
    """
def node():
    """
     Returns the computer's network name (which may not be fully
            qualified)

            An empty string is returned if the value cannot be determined.

    
    """
def release():
    """
     Returns the system's release, e.g. '2.2.0' or 'NT'

            An empty string is returned if the value cannot be determined.

    
    """
def version():
    """
     Returns the system's release version, e.g. '#3 on degas'

            An empty string is returned if the value cannot be determined.

    
    """
def machine():
    """
     Returns the machine type, e.g. 'i386'

            An empty string is returned if the value cannot be determined.

    
    """
def processor():
    """
     Returns the (true) processor name, e.g. 'amdk6'

            An empty string is returned if the value cannot be
            determined. Note that many platforms do not provide this
            information or simply return the same value as for machine(),
            e.g.  NetBSD does this.

    
    """
def _sys_version(sys_version=None):
    """
     Returns a parsed version of Python's sys.version as tuple
            (name, version, branch, revision, buildno, builddate, compiler)
            referring to the Python implementation name, version, branch,
            revision, build number, build date/time as string and the compiler
            identification string.

            Note that unlike the Python sys.version, the returned value
            for the Python version will always include the patchlevel (it
            defaults to '.0').

            The function returns empty strings for tuple entries that
            cannot be determined.

            sys_version may be given to parse an alternative version
            string, e.g. if the version was read from a different Python
            interpreter.

    
    """
def python_implementation():
    """
     Returns a string identifying the Python implementation.

            Currently, the following implementations are identified:
              'CPython' (C implementation of Python),
              'IronPython' (.NET implementation of Python),
              'Jython' (Java implementation of Python),
              'PyPy' (Python implementation of Python).

    
    """
def python_version():
    """
     Returns the Python version as string 'major.minor.patchlevel'

            Note that unlike the Python sys.version, the returned value
            will always include the patchlevel (it defaults to 0).

    
    """
def python_version_tuple():
    """
     Returns the Python version as tuple (major, minor, patchlevel)
            of strings.

            Note that unlike the Python sys.version, the returned value
            will always include the patchlevel (it defaults to 0).

    
    """
def python_branch():
    """
     Returns a string identifying the Python implementation
            branch.

            For CPython this is the SCM branch from which the
            Python binary was built.

            If not available, an empty string is returned.

    
    """
def python_revision():
    """
     Returns a string identifying the Python implementation
            revision.

            For CPython this is the SCM revision from which the
            Python binary was built.

            If not available, an empty string is returned.

    
    """
def python_build():
    """
     Returns a tuple (buildno, builddate) stating the Python
            build number and date as strings.

    
    """
def python_compiler():
    """
     Returns a string identifying the compiler used for compiling
            Python.

    
    """
def platform(aliased=0, terse=0):
    """
     Returns a single string identifying the underlying platform
            with as much useful information as possible (but no more :).

            The output is intended to be human readable rather than
            machine parseable. It may look different on different
            platforms and this is intended.

            If "aliased" is true, the function will use aliases for
            various platforms that report system names which differ from
            their common names, e.g. SunOS will be reported as
            Solaris. The system_alias() function is used to implement
            this.

            Setting terse to true causes the function to return only the
            absolute minimum information needed to identify the platform.

    
    """
