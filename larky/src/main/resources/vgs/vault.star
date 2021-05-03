
load("@stdlib//larky", larky="larky")

token_map = dict()

def _makerand(seed=0):
  "_makerand returns a stateful generator of small pseudorandom numbers."
  state = [seed]
  def internal_rand():
    "internal_rand returns the next pseudorandom number in the sequence."
    state[0] = ((state[0] + 7207) * 9941) & 0xfff
    return state[0]
  return internal_rand
rand = _makerand(123)

def _redact(secret):
    return "tok_" + str(rand())

def _put(secret):
    token = _redact(secret)
    token_map[token]=secret
    return token

def _get(alias):
    return token_map.get(alias)

vault = larky.struct(
    put = _put,
    get = _get,
)