def framework_info(filename):
    """

        A framework name can take one of the following four forms:
            Location/Name.framework/Versions/SomeVersion/Name_Suffix
            Location/Name.framework/Versions/SomeVersion/Name
            Location/Name.framework/Name_Suffix
            Location/Name.framework/Name

        returns None if not found, or a mapping equivalent to:
            dict(
                location='Location',
                name='Name.framework/Versions/SomeVersion/Name_Suffix',
                shortname='Name',
                version='SomeVersion',
                suffix='Suffix',
            )

        Note that SomeVersion and Suffix are optional and may be None
        if not present
    
    """
def test_framework_info():
    """
    'completely/invalid'
    """
