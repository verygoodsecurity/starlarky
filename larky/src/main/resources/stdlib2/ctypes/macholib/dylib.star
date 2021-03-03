def dylib_info(filename):
    """

        A dylib name can take one of the following four forms:
            Location/Name.SomeVersion_Suffix.dylib
            Location/Name.SomeVersion.dylib
            Location/Name_Suffix.dylib
            Location/Name.dylib

        returns None if not found or a mapping equivalent to:
            dict(
                location='Location',
                name='Name.SomeVersion_Suffix.dylib',
                shortname='Name',
                version='SomeVersion',
                suffix='Suffix',
            )

        Note that SomeVersion and Suffix are optional and may be None
        if not present.
    
    """
def test_dylib_info():
    """
    'completely/invalid'
    """
