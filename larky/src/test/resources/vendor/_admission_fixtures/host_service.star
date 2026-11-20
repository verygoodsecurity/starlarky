# Keeps the metrics host service in a global: not shareable (HOST_SERVICE).
load("@vgs//native_metrics", "native_metrics")

init_counter.count()

M = native_metrics

def ping():
    return "pong"
