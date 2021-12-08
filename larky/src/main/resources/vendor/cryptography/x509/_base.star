# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//enum", enum="enum")
load("@stdlib//larky", larky="larky")

# This exists to break an import cycle. These classes are normally accessible
# from the serialization module.

Version = enum.Enum('Version', [
    ("v1", 0),
    ("v3", 2),
])
