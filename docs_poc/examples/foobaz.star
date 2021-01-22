"""
This is foobaz module
"""

# load should not break autodoc
load("@stdlib/larky", "larky")

def make_name(f_name, l_name):
    """
    Makes a name

    :param f_name: First name
    :type f_name: str
    :param l_name: Last name
    :type l_name: str
    """
    return f_name + ' ' + l_name
