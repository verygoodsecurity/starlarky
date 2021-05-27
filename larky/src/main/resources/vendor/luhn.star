load("@stdlib//larky", "larky")
load("@stdlib//builtins", "builtins")
   
def _mod_digits(d):
    if d < 10:
        return d
    else:
        return ((d % 10) + (d // 10))

def _luhn_summation(nums,index):
    nums = nums[index::-1]
    doubledArr = []
    for i in range(len(nums)):
      if(i%2==0):
        doubledArr.append(_mod_digits(int(nums[i]) * 2))
      else:
        doubledArr.append(int(nums[i]))
    luhn_sum = builtins.sum(doubledArr)
    return str(luhn_sum)

def verify(num):
    checksum = int(num[-1])
    cardSum = int(_luhn_summation(num,-2))
    return ((cardSum + checksum) % 10) == 0

def generate(num):
    cardSum = int(_luhn_summation(num,-1))
    modulated = cardSum % 10
    return 10 - modulated

def append(num):
    return num + str(generate(num))

luhn = larky.struct(
  verify=verify,
  generate=generate,
  append=append
)

