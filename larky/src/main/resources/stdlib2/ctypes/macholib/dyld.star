def dyld_env(env, var):
    """
    ':'
    """
def dyld_image_suffix(env=None):
    """
    'DYLD_IMAGE_SUFFIX'
    """
def dyld_framework_path(env=None):
    """
    'DYLD_FRAMEWORK_PATH'
    """
def dyld_library_path(env=None):
    """
    'DYLD_LIBRARY_PATH'
    """
def dyld_fallback_framework_path(env=None):
    """
    'DYLD_FALLBACK_FRAMEWORK_PATH'
    """
def dyld_fallback_library_path(env=None):
    """
    'DYLD_FALLBACK_LIBRARY_PATH'
    """
def dyld_image_suffix_search(iterator, env=None):
    """
    For a potential path iterator, add DYLD_IMAGE_SUFFIX semantics
    """
    def _inject(iterator=iterator, suffix=suffix):
        """
        '.dylib'
        """
def dyld_override_search(name, env=None):
    """
     If DYLD_FRAMEWORK_PATH is set and this dylib_name is a
     framework name, use the first file that exists in the framework
     path if any.  If there is none go on to search the DYLD_LIBRARY_PATH
     if any.


    """
def dyld_executable_path_search(name, executable_path=None):
    """
     If we haven't done any searching and found a library and the
     dylib_name starts with "@executable_path/" then construct the
     library name.

    """
def dyld_default_search(name, env=None):
    """
    'name'
    """
def dyld_find(name, executable_path=None, env=None):
    """

        Find a library or framework using dyld semantics
    
    """
def framework_find(fn, executable_path=None, env=None):
    """

        Find a framework using dyld semantics in a very loose manner.

        Will take input such as:
            Python
            Python.framework
            Python.framework/Versions/Current
    
    """
def test_dyld_find():
    """
    'libSystem.dylib'
    """
