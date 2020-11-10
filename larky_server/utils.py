import tempfile
import json
import ulid

from pylarky.starlarky import evaluate


def preprocess_larky_request(script, main_fn, ctx, payload):
    new_ctx = {"payload": payload, "ctx": ctx}
    main_id_generated = ulid.new().str
    inner_ctx_id = ulid.new().str

    new_script = script + f"""\ndef main_{main_id_generated}(inner):
    return {{"payload": {main_fn}(inner["payload"], inner["ctx"]), "ctx": inner["ctx"]}}
main_{main_id_generated}(inner_ctx_{inner_ctx_id})
"""
    return new_script, new_ctx, inner_ctx_id


def eval_request(script, cxt, inner_ctx_id):
    script_file = tempfile.NamedTemporaryFile(mode='w+')
    script_file.write(script)
    script_file.flush()
    return json.loads(evaluate(
        script_file.name,
        f'inner_ctx_{inner_ctx_id} = {repr(cxt)}'
    ))
