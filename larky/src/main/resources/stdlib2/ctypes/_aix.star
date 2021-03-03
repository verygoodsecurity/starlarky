def _last_version(libnames, sep):
    """
     "libxyz.so.MAJOR.MINOR" => [MAJOR, MINOR]

    """
def get_ld_header(p):
    """
     "nested-function, but placed at module level

    """
def get_ld_header_info(p):
    """
     "nested-function, but placed at module level
     as an ld_header was found, return known paths, archives and members
     these lines start with a digit

    """
def get_ld_headers(file):
    """

        Parse the header of the loader section of executable and archives
        This function calls /usr/bin/dump -H as a subprocess
        and returns a list of (ld_header, ld_header_info) tuples.
    
    """
def get_shared(ld_headers):
    """

        extract the shareable objects from ld_headers
        character "[" is used to strip off the path information.
        Note: the "[" and "]" characters that are part of dump -H output
        are not removed here.
    
    """
def get_one_match(expr, lines):
    """

        Must be only one match, otherwise result is None.
        When there is a match, strip leading "[" and trailing "]"
    
    """
def get_legacy(members):
    """

        This routine provides historical aka legacy naming schemes started
        in AIX4 shared library support for library members names.
        e.g., in /usr/lib/libc.a the member name shr.o for 32-bit binary and
        shr_64.o for 64-bit binary.
    
    """
def get_version(name, members):
    """

        Sort list of members and return highest numbered version - if it exists.
        This function is called when an unversioned libFOO.a(libFOO.so) has
        not been found.

        Versioning for the member name is expected to follow
        GNU LIBTOOL conventions: the highest version (x, then X.y, then X.Y.z)
         * find [libFoo.so.X]
         * find [libFoo.so.X.Y]
         * find [libFoo.so.X.Y.Z]

        Before the GNU convention became the standard scheme regardless of
        binary size AIX packagers used GNU convention "as-is" for 32-bit
        archive members but used an "distinguishing" name for 64-bit members.
        This scheme inserted either 64 or _64 between libFOO and .so
        - generally libFOO_64.so, but occasionally libFOO64.so
    
    """
def get_member(name, members):
    """

        Return an archive member matching the request in name.
        Name is the library name without any prefix like lib, suffix like .so,
        or version number.
        Given a list of members find and return the most appropriate result
        Priority is given to generic libXXX.so, then a versioned libXXX.so.a.b.c
        and finally, legacy AIX naming scheme.
    
    """
def get_libpaths():
    """

        On AIX, the buildtime searchpath is stored in the executable.
        as "loader header information".
        The command /usr/bin/dump -H extracts this info.
        Prefix searched libraries with LD_LIBRARY_PATH (preferred),
        or LIBPATH if defined. These paths are appended to the paths
        to libraries the python executable is linked with.
        This mimics AIX dlopen() behavior.
    
    """
def find_shared(paths, name):
    """

        paths is a list of directories to search for an archive.
        name is the abbreviated name given to find_library().
        Process: search "paths" for archive, and if an archive is found
        return the result of get_member().
        If an archive is not found then return None
    
    """
def find_library(name):
    """
    AIX implementation of ctypes.util.find_library()
        Find an archive member that will dlopen(). If not available,
        also search for a file (or link) with a .so suffix.

        AIX supports two types of schemes that can be used with dlopen().
        The so-called SystemV Release4 (svr4) format is commonly suffixed
        with .so while the (default) AIX scheme has the library (archive)
        ending with the suffix .a
        As an archive has multiple members (e.g., 32-bit and 64-bit) in one file
        the argument passed to dlopen must include both the library and
        the member names in a single string.

        find_library() looks first for an archive (.a) with a suitable member.
        If no archive+member pair is found, look for a .so file.
    
    """
