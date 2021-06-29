load("@stdlib/larky", "larky")
load("@stdlib/jtime", _time = "jtime")

def _struct_time(**kwargs):
    return larky.mutablestruct(__class__="struct_time", **kwargs)

def _gmtime(timestamp=None):
    datetime_dict = _time.gmtime(timestamp)
    return _struct_time(**datetime_dict)

time = larky.struct(
    time=_time.time,
    gmtime=_gmtime,
    strftime=_time.strftime
)