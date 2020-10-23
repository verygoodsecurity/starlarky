load("sets.star", "set")

# request = struct(
#     max_content_length = native.request.max_content_length,
# )
request.max_content_length == 15

requestobj = request.from_proto('string')
requestobj.headers()

load("operations.star", "common")
common.reject()