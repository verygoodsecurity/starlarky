# Keeps the native metrics module in a global. The cache reports RETAINS_HOST_SUPPLIED_PROVIDER.
load("@vgs//native_metrics", "native_metrics")

init_counter.count()

M = native_metrics

def ping():
    return "pong"
