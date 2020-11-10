from larky_server.utils import preprocess_larky_request, eval_request


def test_preprocess_and_evaluation():
    p = preprocess_larky_request(
        script="""
def process(payload, ctx):
    ctx['foo'] = 'baz'
    return payload[::1]
    """,
        main_fn="process",
        ctx={
            "foo": "bar"
        },
        payload="asdf",
    )
    result = eval_request(p[0], p[1], p[2])
    assert result["payload"] == "asdf"
    assert result["ctx"]["foo"] == "baz"
