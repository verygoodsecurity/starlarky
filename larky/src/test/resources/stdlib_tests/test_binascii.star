load("@stdlib//larky", "larky")
load("@stdlib//builtins", "builtins")
load("@vendor//asserts", "asserts")
load("@stdlib//binascii", "hexlify", "unhexlify", "b2a_base64", "a2b_base64")

# print("-- Binary<->Hex Conversions --")
# Binary data.
data = builtins.bytes("CircuitPython is Awesome!")
# print("Original Binary Data: ", data)

# Get the hexadecimal representation of the binary data
hex_data = hexlify(data)
# print("Hex Data: ", hex_data)
# Verify data
asserts.assert_(
    hex_data == builtins.bytes("43697263756974507974686f6e20697320417765736f6d6521"),
    "hexlified data does not match expected data.")
# Get the binary data represented by hex_data
bin_data = unhexlify(hex_data)
# print("Binary Data: ", bin_data)
# Verify data
asserts.assert_(bin_data == data, "unhexlified binary data does not match original binary data.")

# print("-- Base64 ASCII <-> Binary Conversions --")
data = builtins.bytes("Blinka")
# print("Original Binary Data: ", data)
# Convert binary data to a line of ASCII characters in base64 coding.
b64_ascii_data = b2a_base64(data)
# print("Base64 ASCII Data: ", b64_ascii_data)
asserts.assert_that(b64_ascii_data).is_equal_to(builtins.bytes("Qmxpbmth\n"))#, "Expected base64 coding does not match.")

# Convert a block of base64 data back to binary data.
bin_data = a2b_base64("Qmxpbmth\n")
asserts.assert_that(str(bin_data)).is_equal_to("Blinka")
asserts.assert_that(repr(bin_data)).is_equal_to("b\"Blinka\"")
# print("Converted b64 ASCII->Binary Data: ", repr(bin_data))
asserts.assert_that(type(bin_data)).is_equal_to("bytes")
asserts.assert_(bin_data == data, "Expected binary data does not match.")

asserts.assert_that(a2b_base64("AQAB=="), b'\x01\x00\x01')
asserts.assert_that(a2b_base64("AQAB==="), b'\x01\x00\x01')