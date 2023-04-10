load("@stdlib//base64", base64="base64")
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//io", io="io")
load("@stdlib//unittest", "unittest")
load("@stdlib//zipfile", ZipFile="ZipFile", ZipInfo="ZipInfo")
load("@vendor//asserts", "asserts")

test_zip_two_files = bytes([0x50, 0x4b, 0x03, 0x04, 0x14, 0x00, 0x08, 0x00, 0x08, 0x00, 0x49, 0x6b, 0x75, 0x56, 0x00, 0x00, 0x00,
                      0x00, 0x00, 0x00, 0x00, 0x00, 0x0b, 0x00, 0x00, 0x00, 0x08, 0x00, 0x20, 0x00, 0x74, 0x65, 0x73, 0x74,
                      0x2e, 0x74, 0x78, 0x74, 0x55, 0x54, 0x0d, 0x00, 0x07, 0xeb, 0x12, 0x1a, 0x64, 0xec, 0x12, 0x1a, 0x64,
                      0xeb, 0x12, 0x1a, 0x64, 0x75, 0x78, 0x0b, 0x00, 0x01, 0x04, 0xf6, 0x01, 0x00, 0x00, 0x04, 0x14, 0x00,
                      0x00, 0x00, 0x0b, 0x49, 0x2d, 0x2e, 0x51, 0xc8, 0xcc, 0x2b, 0x28, 0x2d, 0xe1, 0x02, 0x00, 0x50, 0x4b,
                      0x07, 0x08, 0xed, 0x41, 0xdd, 0x21, 0x0d, 0x00, 0x00, 0x00, 0x0b, 0x00, 0x00, 0x00, 0x50, 0x4b, 0x03,
                      0x04, 0x14, 0x00, 0x08, 0x00, 0x08, 0x00, 0x45, 0x69, 0x76, 0x56, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                      0x00, 0x00, 0x15, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x20, 0x00, 0x74, 0x65, 0x73, 0x74, 0x5f, 0x32, 0x2e,
                      0x74, 0x78, 0x74, 0x55, 0x54, 0x0d, 0x00, 0x07, 0xa3, 0x60, 0x1b, 0x64, 0xa5, 0x60, 0x1b, 0x64, 0xa3,
                      0x60, 0x1b, 0x64, 0x75, 0x78, 0x0b, 0x00, 0x01, 0x04, 0xf6, 0x01, 0x00, 0x00, 0x04, 0x14, 0x00, 0x00,
                      0x00, 0x73, 0x2d, 0x4b, 0xcd, 0x53, 0xf0, 0xf5, 0x0f, 0x72, 0x55, 0x08, 0x49, 0x2d, 0x2e, 0x51, 0xc8,
                      0xcc, 0x2b, 0x28, 0x2d, 0xe1, 0x02, 0x00, 0x50, 0x4b, 0x07, 0x08, 0x25, 0x3c, 0x8f, 0x7b, 0x17, 0x00,
                      0x00, 0x00, 0x15, 0x00, 0x00, 0x00, 0x50, 0x4b, 0x01, 0x02, 0x14, 0x03, 0x14, 0x00, 0x08, 0x00, 0x08,
                      0x00, 0x49, 0x6b, 0x75, 0x56, 0xed, 0x41, 0xdd, 0x21, 0x0d, 0x00, 0x00, 0x00, 0x0b, 0x00, 0x00, 0x00,
                      0x08, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xa4, 0x81, 0x00, 0x00, 0x00,
                      0x00, 0x74, 0x65, 0x73, 0x74, 0x2e, 0x74, 0x78, 0x74, 0x55, 0x54, 0x0d, 0x00, 0x07, 0xeb, 0x12, 0x1a,
                      0x64, 0xec, 0x12, 0x1a, 0x64, 0xeb, 0x12, 0x1a, 0x64, 0x75, 0x78, 0x0b, 0x00, 0x01, 0x04, 0xf6, 0x01,
                      0x00, 0x00, 0x04, 0x14, 0x00, 0x00, 0x00, 0x50, 0x4b, 0x01, 0x02, 0x14, 0x03, 0x14, 0x00, 0x08, 0x00,
                      0x08, 0x00, 0x45, 0x69, 0x76, 0x56, 0x25, 0x3c, 0x8f, 0x7b, 0x17, 0x00, 0x00, 0x00, 0x15, 0x00, 0x00,
                      0x00, 0x0a, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xa4, 0x81, 0x63, 0x00,
                      0x00, 0x00, 0x74, 0x65, 0x73, 0x74, 0x5f, 0x32, 0x2e, 0x74, 0x78, 0x74, 0x55, 0x54, 0x0d, 0x00, 0x07,
                      0xa3, 0x60, 0x1b, 0x64, 0xa5, 0x60, 0x1b, 0x64, 0xa3, 0x60, 0x1b, 0x64, 0x75, 0x78, 0x0b, 0x00, 0x01,
                      0x04, 0xf6, 0x01, 0x00, 0x00, 0x04, 0x14, 0x00, 0x00, 0x00, 0x50, 0x4b, 0x05, 0x06, 0x00, 0x00, 0x00,
                      0x00, 0x02, 0x00, 0x02, 0x00, 0xae, 0x00, 0x00, 0x00, 0xd2, 0x00, 0x00, 0x00, 0x00, 0x00])
expected_out_two_files = b"UEsDBBQAAAgIAElrdVZQBcSUUAAAAFUAAAAIACAAdGVzdC50eHRVVA0AB+sSGmTsEhpk6xIaZHV4CwABBPYBAAAEFAAAAAtJLS5RyMwrKC3h8kxTqMwvVUhOzFMoSk1MUSjJyCzWAZKpCj6JRdmVClGZBW6ZOalgBaV5VZkFOgq5+SmZaZUKiXkpQC1AEYXiktK0NEUAUEsDBBQAAAgIAEVpdlZfu+2QWAAAAF8AAAAKACAAdGVzdF8yLnR4dFVUDQAHo2AbZKVgG2SjYBtkdXgLAAEE9gEAAAQUAAAAHYtBCoAgFAX3neK19xoGQRFEq3aSX/pUX0kN7PRJm1kMM/ohwTjNGgvFBJaQU9M7FJ+xGcFNxiLtHFUlYTD3UbBy6PikP8jyclC4vGVXYMTWpRrElJ1rP1BLAQIUAxQAAAgIAElrdVZQBcSUUAAAAFUAAAAIACAAAAAAAAAAAACkgQAAAAB0ZXN0LnR4dFVUDQAH6xIaZOwSGmTrEhpkdXgLAAEE9gEAAAQUAAAAUEsBAhQDFAAACAgARWl2Vl+77ZBYAAAAXwAAAAoAIAAAAAAAAAAAAKSBlgAAAHRlc3RfMi50eHRVVA0AB6NgG2SlYBtko2AbZHV4CwABBPYBAAAEFAAAAFBLBQYAAAAAAgACAK4AAAA2AQAAAAA="

test_zip_one_file = base64.b64decode(b"UEsDBBQACAAIAElrdVYAAAAAAAAAAAsAAAAIACAAdGVzdC50eHRVVA0AB+sSGmTsEhpk6xIaZHV4CwABBPYBAAAEFAAAAAtJLS5RyMwrKC3hAgBQSwcI7UHdIQ0AAAALAAAAUEsBAhQDFAAIAAgASWt1Vu1B3SENAAAACwAAAAgAIAAAAAAAAAAAAKSBAAAAAHRlc3QudHh0VVQNAAfrEhpk7BIaZOsSGmR1eAsAAQT2AQAABBQAAABQSwUGAAAAAAEAAQBWAAAAYwAAAAAA")
expected_out_one_file = b"UEsDBBQAAAgIAElrdVZQBcSUUAAAAFUAAAAIACAAdGVzdC50eHRVVA0AB+sSGmTsEhpk6xIaZHV4CwABBPYBAAAEFAAAAAtJLS5RyMwrKC3h8kxTqMwvVUhOzFMoSk1MUSjJyCzWAZKpCj6JRdmVClGZBW6ZOalgBaV5VZkFOgq5+SmZaZUKiXkpQC1AEYXiktK0NEUAUEsBAhQDFAAACAgASWt1VlAFxJRQAAAAVQAAAAgAIAAAAAAAAAAAAKSBAAAAAHRlc3QudHh0VVQNAAfrEhpk7BIaZOsSGmR1eAsAAQT2AQAABBQAAABQSwUGAAAAAAEAAQBWAAAAlgAAAAAA"

def test_unzip_one():
    zip_file = ZipFile(io.BytesIO(test_zip_one_file))
    zipped_files = zip_file.infolist()
    asserts.assert_that(len(zipped_files)).is_equal_to(1)
    zipped_file = zipped_files[0]
    asserts.assert_that(zipped_file.filename).is_equal_to("test.txt")
    zipped_contents = zip_file.read(zipped_file.filename)
    asserts.assert_that(zipped_contents).is_equal_to(b"Test input\n")

def test_unzip_two():
    zip_file = ZipFile(io.BytesIO(test_zip_two_files))
    zipped_files = zip_file.infolist()
    asserts.assert_that(len(zipped_files)).is_equal_to(2)
    first_file = zipped_files[0]
    second_file = zipped_files[1]
    asserts.assert_that(first_file.filename).is_equal_to("test.txt")
    asserts.assert_that(second_file.filename).is_equal_to("test_2.txt")
    first_contents = zip_file.read(first_file.filename)
    second_contents = zip_file.read(second_file.filename)
    asserts.assert_that(first_contents).is_equal_to(b"Test input\n")
    asserts.assert_that(second_contents).is_equal_to(b"Even MORE Test input\n")

def test_edit_two_files():
    og_zip_bytes = io.BytesIO(test_zip_two_files)
    original_zip =  ZipFile(og_zip_bytes, "r")
    new_zip_bytes = io.BytesIO()
    new_zip = ZipFile(new_zip_bytes, "w")
    for item in original_zip.infolist():
        file_data = original_zip.read(item.filename)
        data_to_append = b"If you can read this, the Larky ZipFile can unzip, modify and rezip stuff!"
        # Append new data to the file
        file_data += data_to_append
        # Create a new ZipInfo object with updated file size and compressed size
        new_zip_info = ZipInfo(item.filename, item.date_time)
        new_zip_info.comment = item.comment
        new_zip_info.extra = item.extra
        new_zip_info.compress_type = item.compress_type
        new_zip_info.create_system = item.create_system
        new_zip_info.create_version = item.create_version
        new_zip_info.extract_version = item.extract_version
        new_zip_info.reserved = item.reserved
        new_zip_info.flag_bits = item.flag_bits
        new_zip_info.volume = item.volume
        new_zip_info.internal_attr = item.internal_attr
        new_zip_info.external_attr = item.external_attr
        new_zip_info.header_offset = item.header_offset
        new_zip.writestr(new_zip_info, file_data)
    new_zip.close()
    final_zip_bytes = new_zip_bytes.getvalue()
    asserts.assert_that(base64.b64encode(final_zip_bytes)).is_equal_to(expected_out_two_files)

def test_edit_one_file():
    og_zip_bytes = io.BytesIO(test_zip_one_file)
    original_zip =  ZipFile(og_zip_bytes, "r")

    new_zip_bytes = io.BytesIO()
    new_zip = ZipFile(new_zip_bytes, "w")
    for item in original_zip.infolist():
        file_data = original_zip.read(item.filename)
        data_to_append = b"If you can read this, the Larky ZipFile can unzip, modify and rezip stuff!"
        # Append new data to the file
        file_data += data_to_append
        # Create a new ZipInfo object with updated file size and compressed size
        new_zip_info = ZipInfo(item.filename, item.date_time)
        new_zip_info.comment = item.comment
        new_zip_info.extra = item.extra
        new_zip_info.compress_type = item.compress_type
        new_zip_info.create_system = item.create_system
        new_zip_info.create_version = item.create_version
        new_zip_info.extract_version = item.extract_version
        new_zip_info.reserved = item.reserved
        new_zip_info.flag_bits = item.flag_bits
        new_zip_info.volume = item.volume
        new_zip_info.internal_attr = item.internal_attr
        new_zip_info.external_attr = item.external_attr
        new_zip_info.header_offset = item.header_offset
        new_zip.writestr(new_zip_info, file_data)
    new_zip.close()
    final_zip_bytes = new_zip_bytes.getvalue()
    asserts.assert_that(base64.b64encode(final_zip_bytes)).is_equal_to(expected_out_one_file)

def test_zip_one():
    new_zip_bytes = io.BytesIO()
    new_zip = ZipFile(new_zip_bytes, "w")
    new_zip_info = ZipInfo("test.txt", (2023, 3, 21, 13, 26, 18))
    new_zip_info.comment = b""
    new_zip_info.extra = base64.b64decode(b"VVQNAAfrEhpk7BIaZOsSGmR1eAsAAQT2AQAABBQAAAA=")
    new_zip_info.compress_type = 8
    new_zip_info.create_system = 3
    new_zip_info.create_version = 20
    new_zip_info.extract_version = 20
    new_zip_info.reserved = 0
    new_zip_info.flag_bits = 8
    new_zip_info.volume = 0
    new_zip_info.internal_attr = 0
    new_zip_info.external_attr = 2175008768
    new_zip_info.header_offset = 0
    file_data = b"Test input\nIf you can read this, the Larky ZipFile can unzip, modify and rezip stuff!"
    new_zip.writestr(new_zip_info, file_data)
    new_zip.close()
    final_zip_bytes = new_zip_bytes.getvalue()
    asserts.assert_that(base64.b64encode(final_zip_bytes)).is_equal_to(expected_out_one_file)
    message = "ValueError: Attempt to write to ZIP archive that was already closed"
    asserts.assert_fails(lambda: new_zip.writestr(new_zip_info, "Data after closing"), message)

def test_zip_two():
    new_zip_bytes = io.BytesIO()
    new_zip = ZipFile(new_zip_bytes, "w")

    new_zip_info = ZipInfo("test.txt", (2023, 3, 21, 13, 26, 18))
    new_zip_info.comment = b""
    new_zip_info.extra = base64.b64decode(b"VVQNAAfrEhpk7BIaZOsSGmR1eAsAAQT2AQAABBQAAAA=")
    new_zip_info.compress_type = 8
    new_zip_info.create_system = 3
    new_zip_info.create_version = 20
    new_zip_info.extract_version = 20
    new_zip_info.reserved = 0
    new_zip_info.flag_bits = 8
    new_zip_info.volume = 0
    new_zip_info.internal_attr = 0
    new_zip_info.external_attr = 2175008768
    new_zip_info.header_offset = 0
    file_data = b"Test input\nIf you can read this, the Larky ZipFile can unzip, modify and rezip stuff!"
    new_zip.writestr(new_zip_info, file_data)

    new_zip_info = ZipInfo("test_2.txt", (2023, 3, 22, 13, 10, 10))
    new_zip_info.comment = b""
    new_zip_info.extra = base64.b64decode(b"VVQNAAejYBtkpWAbZKNgG2R1eAsAAQT2AQAABBQAAAA==")
    new_zip_info.compress_type = 8
    new_zip_info.create_system = 3
    new_zip_info.create_version = 20
    new_zip_info.extract_version = 20
    new_zip_info.reserved = 0
    new_zip_info.flag_bits = 8
    new_zip_info.volume = 0
    new_zip_info.internal_attr = 0
    new_zip_info.external_attr = 2175008768
    new_zip_info.header_offset = 0
    file_data = b"Even MORE Test input\nIf you can read this, the Larky ZipFile can unzip, modify and rezip stuff!"
    new_zip.writestr(new_zip_info, file_data)

    new_zip.close()
    final_zip_bytes = new_zip_bytes.getvalue()
    asserts.assert_that(base64.b64encode(final_zip_bytes)).is_equal_to(expected_out_two_files)

def test_bad_magic_number():
    bad_file = test_zip_one_file.replace(b"PK", b"__")
    bad_bytes = io.BytesIO(bad_file)
    message = "BadZipFile: File is not a zip file"
    asserts.assert_fails(lambda: ZipFile(bad_bytes), message)

def test_incomplete_file():
    message = "BadZipFile: File is not a zip file"
    asserts.assert_fails(lambda: ZipFile(io.BytesIO(test_zip_one_file[:len(test_zip_one_file)//2])), message)


def test_readable_writeable():
    new_zip_bytes = io.BytesIO(test_zip_one_file)
    new_zip = ZipFile(new_zip_bytes, "r")
    new_zip_info = ZipInfo("test.txt", (2023, 3, 21, 13, 26, 18))
    new_zip_info.comment = b""
    new_zip_info.extra = base64.b64decode(b"VVQNAAfrEhpk7BIaZOsSGmR1eAsAAQT2AQAABBQAAAA=")
    new_zip_info.compress_type = 8
    new_zip_info.create_system = 3
    new_zip_info.create_version = 20
    new_zip_info.extract_version = 20
    new_zip_info.reserved = 0
    new_zip_info.flag_bits = 8
    new_zip_info.volume = 0
    new_zip_info.internal_attr = 0
    new_zip_info.external_attr = 2175008768
    new_zip_info.header_offset = 0
    file_data = b"Test input\nIf you can read this, the Larky ZipFile can unzip, modify and rezip stuff!"
    message = "ValueError: write() *"
    asserts.assert_fails(lambda: new_zip.writestr(new_zip_info, file_data), message)

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_unzip_one))
    _suite.addTest(unittest.FunctionTestCase(test_unzip_two))
    _suite.addTest(unittest.FunctionTestCase(test_zip_one))
    _suite.addTest(unittest.FunctionTestCase(test_zip_two))
    _suite.addTest(unittest.FunctionTestCase(test_edit_one_file))
    _suite.addTest(unittest.FunctionTestCase(test_edit_two_files))
    _suite.addTest(unittest.FunctionTestCase(test_bad_magic_number))
    _suite.addTest(unittest.FunctionTestCase(test_incomplete_file))
    _suite.addTest(unittest.FunctionTestCase(test_readable_writeable))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())