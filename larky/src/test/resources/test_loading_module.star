# load("//testlib/builtinz", "setz") # works, but root is not defined.
# load("/testlib/builtinz", "setz")  # does not work
# load("./testlib/builtinz", "setz") # works
load("@stdlib/json", "json")
load("@stdlib/hashlib", "hashlib")
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

c1 = json.dumps({"one": 1, "two": 2})
d1 = json.dumps("😹💍")
print(c1)
print(d1)
c = json.decode(c1)
d = json.decode(d1)
print(json.loads('{"one": 1, "two": 2}') == c)
print(json.loads('"\\ud83d\\ude39\\ud83d\\udc8d"') == d)