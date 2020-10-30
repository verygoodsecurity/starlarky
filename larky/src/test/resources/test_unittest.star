def success():
    print("did this work?")


def failure():
    fail("one")


def suite():
    suite = unittest.TestSuite()
    suite.addTest(unittest.FunctionTestCase(success))
    suite.addTest(unittest.FunctionTestCase(failure))
    return suite


runner = unittest.TextTestRunner()
runner.run(suite())




"""
hijack stdout, stdin, result
{
'stdout': [
 . ... 
 ],
 'stderr': [
 ],
 'result': [
 ]
}
def wrapper(result):
    print(result)
    
def run():
    wrapper(drew())

# start-customer-code
load('blah', 'blah')
def drew(ctx):
    a = 1
    b = 2
    result = blah.add(a, b)
    
# end-customer-code    
    
run()    
"""
