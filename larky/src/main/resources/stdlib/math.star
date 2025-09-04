load("@stdlib//larky", "larky")
load("@stdlib//c99math", _c99math="c99math")


pi = _c99math.PI
pow = _c99math.pow
sqrt = _c99math.sqrt
fabs = _c99math.fabs
ceil = _c99math.ceil
log = _c99math.log
floor = _c99math.floor


def gcd(x, y):
    term = int(y)
    r_p, r_n = abs(x), abs(term)
    iteration_limit_reached = False
    for _while_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
        if r_n <= 0:
            break
        q = r_p // r_n
        r_p, r_n = r_n, r_p - q * r_n
        
        # Check if this is the last iteration
        if _while_ == larky.WHILE_LOOP_EMULATION_ITERATION - 1:
            iteration_limit_reached = True
    
    # If we reached the iteration limit and algorithm didn't converge, fail
    if iteration_limit_reached and r_n > 0:
        fail("Iteration limit exceeded: GCD algorithm did not converge within WHILE_LOOP_EMULATION_ITERATION limit of %d" % larky.WHILE_LOOP_EMULATION_ITERATION)
        
    return r_p


math = larky.struct(
    __name__='math',
    pi=pi,
    pow=pow,
    sqrt=sqrt,
    fabs=fabs,
    ceil=ceil,
    log=log,
    floor=floor,
    gcd=gcd,
)
