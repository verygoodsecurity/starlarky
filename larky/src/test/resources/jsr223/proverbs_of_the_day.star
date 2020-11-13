proverbs = [
  "A rolling stone gathers no moss.",
  "A friend in need is a friend indeed.",
  "Every garden may have some weeds.",
  "Fine feathers make fine birds.",
]

def get_proverb():
  if day + 1 < len(proverbs):
    day += 1
  else:
    day = 0

  return proverbs[day]

output = get_proverb()
