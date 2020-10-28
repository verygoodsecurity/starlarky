# load("//testlib/builtinz", "setz") # works, but root is not defined.
# load("/testlib/builtinz", "setz")  # does not work
# load("./testlib/builtinz", "setz") # works
load("testlib/builtinz", "setz", "collections")

# # request = struct(
# #     max_content_length = native.request.max_content_length,
# # )
# request.max_content_length == 15
#
# requestobj = request.from_proto('string')
# requestobj.headers()
#
# load("operations.star", "common")
# common.reject()
print(collections)
print(setz.make())
print(hashlib.md5("foo"))
print(json.decode('{"one": 1, "two": 2}'))
print(json.decode('"\\ud83d\\ude39\\ud83d\\udc8d"'))