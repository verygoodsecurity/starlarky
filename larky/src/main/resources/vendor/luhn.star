load("@stdlib//larky", "larky")
load("@stdlib//builtins", "builtins")


def _luhn_summation(nums,index):
    nums = nums[index::-1]
    doubledArr = []
    for i in range(len(nums)):
      if(i%2==0):
        doubledArr.append(builtins.sum(divmod(int(nums[i]) * 2,10)))
      else:
        doubledArr.append(int(nums[i]))
    luhn_sum = builtins.sum(doubledArr)
    return str(luhn_sum)

def verify(num):
    """
    Check if the provided string of digits satisfies the Luhn checksum.
    >>> verify('356938035643809')
    True
    >>> verify('534618613411236')
    False
    """
    if not isdigit(num):
        return False
    checksum = int(num[-1])
    cardSum = int(_luhn_summation(num,-2))
    return ((cardSum + checksum) % 10) == 0

def generate(num):
    """
    Generate the Luhn check digit to append to the provided string.
    >>> generate('35693803564380')
    9
    >>> generate('53461861341123')
    4
    """
    cardSum = int(_luhn_summation(num,-1))
    modulated = cardSum % 10
    return 10 - modulated

def append(num):
    """
    Append Luhn check digit to the end of the provided string.
    >>> append('53461861341123')
    '534618613411234'
    """
    return num + str(generate(num))

luhn = larky.struct(
  verify=verify,
  generate=generate,
  append=append
)

