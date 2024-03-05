load("@stdlib//larky", larky="larky")
load("@vgs//native_metrics", _metrics="native_metrics")

def track(
        amount=None,
        bin=None,
        currency=None,
        psp=None,
        result=None,
        type=None,
        **kwargs):
    _metrics.track(
        amount=amount,
        bin=bin,
        currency=currency,
        psp=psp,
        result=result,
        type=type,
        dictionary=kwargs
    )


metrics = larky.struct(
    track=track
)
