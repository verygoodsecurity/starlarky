# load("//lib/builtinz", "setz") # works, but root is not defined.
# load("/lib/builtinz", "setz")  # does not work
# load("./lib/builtinz", "setz") # works
load("lib/builtinz", "setz")

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
setz.make()