load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")

load("@stdlib/json", "json")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//hashlib", hashlib="hashlib")
load("@vendor//larky_ecdh", "LarkyECDH")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//base64", b64decode="b64decode")

def test_applepay_decrypt():
    
    body = {
      "paymentData": {
        "data": "6Xb/d0zH/xVqjr2fSavSqp0eHTuT2Qe8EVO+u/TvwS8NmP5RV2a281ZJ7170crrHaADAKaioKHfU29S9q9G5nIIL5fVooC2Lx3w1dugmQZtAxmy4HQZpUbgBC6wwr9OkF5RTaxr08K/SxUUL0amY1o+Bz+IM4geRHv2h2gLAR3TbLKlrbnXhNOipWTspzCMKblutKXH1/Ps+2wmls/ajz0Kr2jWRC/jRWmU2X6JAIbVCI4jDhg02W3EFBfu+5ONSrQB++vFx5S3sZ0484HwVgtL8Hjg7+Zmz1KYtoIPKdq1qR/oWCFF+U6P7fBXek13o0aep38DTk/umgYGBSkIE5NjlaxnzcmejJyT19aUkzHOFYZeS5xbdhedrO/W/cDfmphVrTiLAIuKFf0+WJY6jWcELyQKX28mj4dBXv4TSoz8=",
        "header": {
          "ephemeralPublicKey": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE87h0qpsCbZaoPhmy+0KHKYVNxa1MgXoFI8ipYDlpJ01/nluXL5rmtL4XEY4iWpU5HtmBTDDt4qRdIqY+zkKuMQ==",
          "publicKeyHash": "wCIv2iQRKweeHYqoM+7qz9xebzv3gTFps9BnSgLQjCA=",
          "transactionId": "6bc125d20ac33ba5a04543705b152dab82adfb7eb20980510780b456a3e860ba"
        },
        "signature": "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",
        "version": "EC_v1"
      }
    }

    keystore = "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"
    keystorePass = "ISBN5o6o12o1623"
    merchant_id = "merchant.verygoodsecurity.demo.applepay"
    
    def decrypt_applepay(merchant_id, keystore, keystorePass, ephemeralPublicKey, payload):

      def generate_merchant_id_hash(text):
          hash_binary = hashlib.new("sha256", bytes(text, 'utf-8')).digest()
          print(binascii.hexlify(hash_binary))
          # Hex should equal below:
          print("9DAFDD8FB87BCD6698A345A6C9A2B78FD30346587C9F1A9FEF9B1C9AD9A1C9EA")
          return hash_binary

      def _generate_symmetric_key(shared_secret):
          sha = hashlib.new("sha256", b'\0' * 3)
          sha.update(b'\1')
          sha.update(shared_secret)
          sha.update(b'\x0did-aes256-GCM' + b'Apple' + _merc_id)
          return sha.digest()
          
      _merc_id = generate_merchant_id_hash(merchant_id)

      ecdh = LarkyECDH()
      ecdh.set_private_key(bytes(keystore, "utf-8"), type="PKCS12", passwd=keystorePass)
      ecdh.set_public_key(bytes(ephemeralPublicKey, 'utf-8'), type="X509")
      shared_secret = ecdh.exchange()
      print(binascii.hexlify(shared_secret))
      # Hex should equal below:
      print("ee46315a84ee960b749e8f7a87239f359d693c405f73eb6f0fcec0289e15c9d7")
      
      symmetric_key = _generate_symmetric_key(shared_secret)
      print(binascii.hexlify(symmetric_key))
      # Hex should equal below:
      print("76e6ab0e5175b44df17c907cb13f9dc8bb6d29dfe498e4914870563844fe70fd")

      iv = b'\0' * 16
      payload = b64decode(payload)
      cipher = AES.new(symmetric_key, AES.MODE_GCM, iv)
      data = cipher.decrypt(bytes(payload, 'utf-8'))
      return data 

    decrypted_json = decrypt_applepay(merchant_id, keystore, keystorePass, body["paymentData"]["header"]["ephemeralPublicKey"], body["paymentData"]["data"])
    print(decrypted_json)

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_applepay_decrypt))
    return _suite
    
_runner = unittest.TextTestRunner()
_runner.run(_testsuite())