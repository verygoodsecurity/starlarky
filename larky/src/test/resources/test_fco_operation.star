


# - name: github.com/verygoodsecurity/common/http/header/Get
#       config:
#         header: "Private-Key"
#         var: "ctx.Private-Key"
#


def input(request, ctx):
    ctx.payload = content_type.json.Remove(json_path='signature')(request)
    ctx['Private-Key'] = http.header.Get('Private-Key')
    http.header.Remove('Private-Key')
    content_type.json.Set(json_path='x-secret_key')(request, value=ctx['Private-Key'])
    # ?sort?
    # ?Concat?
    ctx.signature = crypto.sha.Hash("sha512")(ctx.concat)
    content_type.json.Set(json_path='signature')(ctx.payload, var)
    http.body.Set(ctx.payload)


def Config(request, ctx):
    ctx.payload = json.loads(request.payload)
    ctx.payload.popitem('signature')
    ctx['Private-Key'] = request.headers().get('Private-Key')
    headers = request.headers()
    headers.popitem('Private-Key')
    ctx.payload['x-secret-key'] = json.dumps(ctx['Private-Key'])