operations:
              - name: github.com/verygoodsecurity/common/compute/LarkyHttp
                parameters:
                  script: |-
                    load("@stdlib//json", json="json")
                    load("@vendor//jose/jwt", jwt="jwt")
                    def process(input, ctx):
                        return input