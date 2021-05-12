load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//ISO8583Decoder", Decoder="Decoder")
load("@vendor//ISO8583Encoder", Encoder="Encoder")
load("@vendor//ISO8583Specs", "default")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//builtins", "builtins")
load("@stdlib//sets", "sets")


spec = default


def test_EncodeError_exception():
    """
    Validate EncodeError class
    """
    # spec["h"]["data_enc"] = "ascii"
    # spec["h"]["len_type"] = 0
    # spec["h"]["max_len"] = 6
    # spec["t"]["data_enc"] = "ascii"
    # spec["p"]["data_enc"] = "ascii"
    # spec["1"]["len_type"] = 0
    # spec["1"]["max_len"] = 0

    spec = {
        "h": {
            "data_enc": "ascii",
            "len_type": 0,
            "max_len": 6,
        },
        "t": {
            "data_enc": "ascii",
        },
        "p": {
            "data_enc": "ascii",
        },
        "1": {
            "len_type": 0,
            "max_len": 0
        }
    }

    doc_dec = {"t": ""}
    asserts.assert_fails(lambda: Encoder.encode(doc_dec, spec), ".*?Field data is required according to specifications: field h")

# TODO pickle is not supported
# def test_EncodeError_exception_pickle():
#     """
#     Validate EncodeError class with pickle
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["1"]["len_type"] = 0
#     spec["1"]["max_len"] = 0
#
#     doc_dec = {"t": ""}
#
#     try:
#         iso8583.encode(doc_dec, spec=spec)
#     except iso8583.EncodeError as e:
#         p = pickle.dumps(e)
#         e_unpickled = pickle.loads(p)
#
#         asserts.assert_that(e.doc_dec == e_unpickled.doc_dec
#         asserts.assert_that(e.doc_enc == e_unpickled.doc_enc
#         asserts.assert_that(e.msg == e_unpickled.msg
#         asserts.assert_that(e.field == e_unpickled.field
#         asserts.assert_that(e.args[0] == e_unpickled.args[0]


def test_non_string_field_keys():
    """
    Input dictionary contains non
    """
    # spec["h"]["data_enc"] = "ascii"
    # spec["h"]["len_type"] = 0
    # spec["h"]["max_len"] = 6
    # spec["t"]["data_enc"] = "ascii"
    # spec["p"]["data_enc"] = "b"
    # spec["2"]["len_type"] = 2
    # spec["2"]["max_len"] = 10
    # spec["2"]["data_enc"] = "ascii"
    # spec["2"]["len_enc"] = "ascii"
    # spec["3"]["len_type"] = 2
    # spec["3"]["max_len"] = 10
    # spec["3"]["data_enc"] = "ascii"
    # spec["3"]["len_enc"] = "ascii"
    spec = {
        "h": {
            "data_enc": "ascii",
            "len_type": 0,
            "max_len": 6,
        },
        "t": {
            "data_enc": "ascii",
        },
        "p": {
            "data_enc": "b",
        },
        "2": {
            "len_type": 2,
            "max_len": 10,
            "data_enc": "ascii",
            "len_enc": "ascii"
        },
        "3": {
            "len_type": 2,
            "max_len": 10,
            "data_enc": "ascii",
            "len_enc": "ascii"
        }
    }

    doc_dec = {"h": "header", "t": "0210", 2: "1122"}
    asserts.assert_fails(lambda: Encoder.encode(doc_dec, spec), ".*?Dictionary contains invalid fields .2.: field p")

    doc_dec = {"h": "header", "t": "0210", 2: "1122", 3: "3344"}
    asserts.assert_fails(lambda: Encoder.encode(doc_dec, spec), ".*?Dictionary contains invalid fields .2, 3.: field p")

    doc_dec = {"h": "header", "t": "0210", 2.5: "1122", 3.5: "3344"}
    asserts.assert_fails(lambda: Encoder.encode(doc_dec, spec), ".*?Dictionary contains invalid fields .2.5, 3.5.: field p")

    doc_dec = {"h": "header", "t": "0210", 2.5: "1122", 3.5: "3344"}
    asserts.assert_fails(lambda: Encoder.encode(doc_dec, spec), ".*?Dictionary contains invalid fields .2.5, 3.5.: field p")

    doc_dec = {"h": "header", "t": "0210", (1, 2): "1122", (3, 4): "3344"}
    asserts.assert_fails(lambda: Encoder.encode(doc_dec, spec), ".*?Dictionary contains invalid fields ..1, 2., .3, 4..: field p")


# def test_input_type():
#     """
#     Encode accepts only dict.
#     """
#     s = bytes(r"", encoding='utf-8')
#     with pytest.raises(TypeError, match="Decoded ISO8583 data must be dict, not bytes"):
#         iso8583.encode(s, spec=spec)
#
#
# def test_header_no_key():
#     """
#     Message header is required and key is not provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["1"]["len_type"] = 0
#     spec["1"]["max_len"] = 0
#
#     doc_dec = {"t": ""}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Field data is required according to specifications: field h",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_header_ascii_absent():
#     """
#     ASCII header is not required by spec and not provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["max_len"] = 0
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "", "t": "0200"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x30, 0x32, 0x30, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_header_ascii_present():
#     """
#     ASCII header is required by spec and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0200"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x30, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_header_ebcdic_absent():
#     """
#     EBCDIC header is not required by spec and not provided
#     """
#     spec["h"]["data_enc"] = "cp500"
#     spec["h"]["max_len"] = 0
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "", "t": "0200"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x30, 0x32, 0x30, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_header_ebcdic_present():
#     """
#     EBCDIC header is required by spec and provided
#     """
#     spec["h"]["data_enc"] = "cp500"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0200"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x88, 0x85, 0x81, 0x84, 0x85, 0x99, 0x30, 0x32, 0x30, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x88, 0x85, 0x81, 0x84, 0x85, 0x99])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_header_bdc_absent():
#     """
#     BDC header is not required by spec and not provided
#     """
#     spec["h"]["data_enc"] = "b"
#     spec["h"]["max_len"] = 0
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "", "t": "0200"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x30, 0x32, 0x30, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_header_bcd_present():
#     """
#     BCD header is required by spec and provided
#     """
#     spec["h"]["data_enc"] = "b"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "A1A2A3A4A5A6", "t": "0200"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0xa1, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0x30, 0x32, 0x30, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0xa1, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6])
#     asserts.assert_that(doc_dec["h"] == "A1A2A3A4A5A6"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_header_not_required_provided():
#     """
#     String header is not required by spec but provided.
#     No error. Header is not included in the message.
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["max_len"] = 0
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0200"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x30, 0x32, 0x30, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_header_negative_missing():
#     """
#     String header is required by spec but not provided.
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "", "t": "0200"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 0 bytes, expecting 6: field h"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_header_negative_partial():
#     """
#     String header is required by spec but partially provided.
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "head", "t": "0200"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 4 bytes, expecting 6: field h"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_header_negative_incorrect_encoding():
#     """
#     String header is required by spec and provided.
#     However, the spec encoding is not correct
#     """
#     spec["h"]["data_enc"] = "invalid"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0200"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .unknown encoding: invalid.: field h",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_header_negative_incorrect_ascii_data():
#     """
#     ASCII header is required by spec and provided.
#     However, the data is not ASCII
#     CPython and PyPy throw differently worded exception
#     CPython: 'ascii' codec can't encode characters in position 0-5: ordinal not in range(128)
#     PyPy:    'ascii' codec can't encode character '\\xff' in position 0: ordinal not in range(128)
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {
#         "h": bytes([0xff, 0xff, 0xff, 0xff, 0xff, 0xff]).decode("latin-1"),
#         "t": "0200",
#     }
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .'ascii' codec can't encode character.*: ordinal not in range.128..: field h",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_header_negative_incorrect_bcd_data():
#     """
#     BCD header is required by spec and provided.
#     However, the data is not hex
#     """
#     spec["h"]["data_enc"] = "b"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0200"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .non-hexadecimal number found in fromhex.. arg at position 0.: field h",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_variable_header_ascii_over_max():
#     """
#     ASCII variable header is required and over max provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_enc"] = "ascii"
#     spec["h"]["len_type"] = 2
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#
#     doc_dec = {"h": "header12", "t": "0210"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Field data is 8 bytes, larger than maximum 6: field h",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_variable_header_ascii_present():
#     """
#     ASCII variable header is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_enc"] = "ascii"
#     spec["h"]["len_type"] = 2
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x30, 0x36, 0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes([0x30, 0x36])
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_variable_header_ascii_present_zero_legnth():
#     """
#     ASCII zero-length variable header
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_enc"] = "ascii"
#     spec["h"]["len_type"] = 2
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "", "t": "0210"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x30, 0x30, 0x30, 0x32, 0x31, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes([0x30, 0x30])
#     asserts.assert_that(doc_enc["h"]["data"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_dec["h"] == ""
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_variable_header_ebcdic_over_max():
#     """
#     EBCDIC variable header is required and over max provided
#     """
#     spec["h"]["data_enc"] = "cp500"
#     spec["h"]["len_enc"] = "cp500"
#     spec["h"]["len_type"] = 2
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#
#     doc_dec = {"h": "header1", "t": "0210"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Field data is 7 bytes, larger than maximum 6: field h",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_variable_header_ebcdic_present():
#     """
#     EBCDIC variable header is required and provided
#     """
#     spec["h"]["data_enc"] = "cp500"
#     spec["h"]["len_enc"] = "cp500"
#     spec["h"]["len_type"] = 2
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0xf0, 0xf6, 0x88, 0x85, 0x81, 0x84, 0x85, 0x99, 0x30, 0x32, 0x31, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes([0xf0, 0xf6])
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x88, 0x85, 0x81, 0x84, 0x85, 0x99])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_variable_header_ebcdic_present_zero_legnth():
#     """
#     EBCDIC zero-length variable header
#     """
#     spec["h"]["data_enc"] = "cp500"
#     spec["h"]["len_enc"] = "cp500"
#     spec["h"]["len_type"] = 2
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "", "t": "0210"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0xf0, 0xf0, 0x30, 0x32, 0x31, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes([0xf0, 0xf0])
#     asserts.assert_that(doc_enc["h"]["data"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_dec["h"] == ""
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_variable_header_bdc_over_max():
#     """
#     BDC variable header is required and over max is provided
#     """
#     spec["h"]["data_enc"] = "b"
#     spec["h"]["len_enc"] = "b"
#     spec["h"]["len_type"] = 2
#     spec["h"]["max_len"] = 2
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "abcdef", "t": "0210"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Field data is 3 bytes, larger than maximum 2: field h",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_variable_header_bdc_odd():
#     """
#     BDC variable header is required and odd length is provided
#     CPython and PyPy throw differently worded exception
#     CPython: non-hexadecimal number found in fromhex() arg at position 5
#     PyPy:    non-hexadecimal number found in fromhex() arg at position 4
#     """
#     spec["h"]["data_enc"] = "b"
#     spec["h"]["len_enc"] = "b"
#     spec["h"]["len_type"] = 2
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "abcde", "t": "0210"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .non-hexadecimal number found in fromhex.. arg at position 4|5.: field h",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_variable_header_bdc_ascii_length():
#     """
#     BDC variable header
#     The length is in ASCII.
#     """
#     spec["h"]["data_enc"] = "b"
#     spec["h"]["len_enc"] = "ascii"
#     spec["h"]["len_type"] = 3
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "abcd", "t": "0210"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x30, 0x30, 0x32, 0xab, 0xcd, 0x30, 0x32, 0x31, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes([0x30, 0x30, 0x32])
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0xab, 0xcd])
#     asserts.assert_that(doc_dec["h"] == "abcd"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_variable_header_bdc_ebcdic_length():
#     """
#     BDC variable header is required and provided
#     The length is in EBCDIC.
#     """
#     spec["h"]["data_enc"] = "b"
#     spec["h"]["len_enc"] = "cp500"
#     spec["h"]["len_type"] = 3
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "abcd", "t": "0210"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0xf0, 0xf0, 0xf2, 0xab, 0xcd, 0x30, 0x32, 0x31, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes([0xf0, 0xf0, 0xf2])
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0xab, 0xcd])
#     asserts.assert_that(doc_dec["h"] == "abcd"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_variable_header_bcd_present():
#     """
#     BCD variable header is required and provided
#     """
#     spec["h"]["data_enc"] = "b"
#     spec["h"]["len_enc"] = "b"
#     spec["h"]["len_type"] = 2
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "abcd", "t": "0210"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x00, 0x02, 0xab, 0xcd, 0x30, 0x32, 0x31, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes([0x00, 0x02])
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0xab, 0xcd])
#     asserts.assert_that(doc_dec["h"] == "abcd"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_variable_header_bcd_present_zero_length():
#     """
#     BCD zero-length variable header is required and provided
#     """
#     spec["h"]["data_enc"] = "b"
#     spec["h"]["len_enc"] = "b"
#     spec["h"]["len_type"] = 2
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "", "t": "0210"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x00, 0x00, 0x30, 0x32, 0x31, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes([0x00, 0x00])
#     asserts.assert_that(doc_enc["h"]["data"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_dec["h"] == ""
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_variable_header_incorrect_encoding():
#     """
#     variable header is required and provided.
#     However, the spec encoding is not correct for length
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_enc"] = "invalid"
#     spec["h"]["len_type"] = 2
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "abcd", "t": "0210"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode length .unknown encoding: invalid.: field h",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_no_key():
#     """
#     Message type is required and key is not provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["1"]["len_type"] = 0
#     spec["1"]["max_len"] = 0
#
#     doc_dec = {"h": "header", "2": ""}
#
#     with pytest.raises(iso8583.EncodeError, match="Field data is required: field t"):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_ascii_absent():
#     """
#     ASCII message type is required and not provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": ""}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 0 bytes, expecting 4: field t"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_ascii_partial():
#     """
#     ASCII message type is required and partial is provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "02"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 2 bytes, expecting 4: field t"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_ascii_over_max():
#     """
#     ASCII message type is required and over max is provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "02101"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 5 bytes, expecting 4: field t"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_ascii_incorrect_data():
#     """
#     ASCII message type is required and provided.
#     However, the data is not ASCII
#     CPython and PyPy throw differently worded exception
#     CPython: 'ascii' codec can't encode characters in position 0-3: ordinal not in range(128)
#     PyPy:    'ascii' codec can't encode character '\\xff' in position 0: ordinal not in range(128)
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {
#         "h": "header",
#         "t": bytes([0xff, 0xff, 0xff, 0xff]).decode("latin-1"),
#     }
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .'ascii' codec can't encode character.*: ordinal not in range.128..: field t",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_ascii_present():
#     """
#     ASCII message type is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0200"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x30, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_type_ebcdic_absent():
#     """
#     EBCDIC message type is required and not provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "cp500"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": ""}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 0 bytes, expecting 4: field t"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_ebcdic_partial():
#     """
#     EBCDIC message type is required and partial provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "cp500"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "02"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 2 bytes, expecting 4: field t"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_ebcdic_over_max():
#     """
#     EBCDIC message type is required and over max provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "cp500"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "02101"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 5 bytes, expecting 4: field t"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_ebcdic_present():
#     """
#     EBCDIC message type is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "cp500"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0200"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0xf0, 0xf2, 0xf0, 0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0xf0, 0xf2, 0xf0, 0xf0])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_type_bdc_absent():
#     """
#     BDC message type is required and not provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "b"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": ""}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 0 bytes, expecting 2: field t"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_bdc_partial():
#     """
#     BDC message type is required and partial is provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "b"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "02"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 1 bytes, expecting 2: field t"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_bdc_over_max():
#     """
#     BDC message type is required and over max is provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "b"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "021000"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 3 bytes, expecting 2: field t"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_bdc_odd():
#     """
#     BDC message type is required and odd length is provided
#     CPython and PyPy throw differently worded exception
#     CPython: non-hexadecimal number found in fromhex() arg at position 3
#     PyPy:    non-hexadecimal number found in fromhex() arg at position 2
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "b"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "021"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .non-hexadecimal number found in fromhex.. arg at position 2|3.: field t",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_bdc_non_hex():
#     """
#     BDC message type is required and provided
#     However, the data is not hex
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "b"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "021x"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .non-hexadecimal number found in fromhex.. arg at position 3.: field t",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_type_bcd_present():
#     """
#     BCD message type is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "b"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0200"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x02, 0x00])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "0000000000000000"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p"])
#
#
# def test_type_incorrect_encoding():
#     """
#     String message type is required and provided.
#     However, the spec encoding is not correct
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "invalid"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0200"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .unknown encoding: invalid.: field t",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_bitmap_range():
#     """
#     ISO8583 bitmaps must be between 1 and 128.
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0200"}
#
#     doc_dec["0"] = ""
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Dictionary contains fields outside of 1-128 range .0.: field p",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#     del doc_dec["0"]
#     doc_dec["129"] = ""
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Dictionary contains fields outside of 1-128 range .129.: field p",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#     for f in range(0, 130):
#         doc_dec[str(f)] = ""
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Dictionary contains fields outside of 1-128 range .0, 129.: field p",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#     for f in range(0, 131):
#         doc_dec[str(f)] = ""
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Dictionary contains fields outside of 1-128 range .0, 129, 130.: field p",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_bitmap_remove_secondary():
#     """
#     If 65-128 fields are not in bitmap then remove field 1.
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["data_enc"] = "ascii"
#     spec["2"]["len_enc"] = "ascii"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 19
#
#     doc_dec = {
#         "h": "header",
#         "t": "0200",
#         "1": "not needed",
#         "2": "1234567890",
#     }
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x30, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x31, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes([0x31, 0x30])
#     asserts.assert_that(doc_enc["2"]["data"] == bytes([0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30])
#     asserts.assert_that(doc_dec["2"] == "1234567890"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_bitmap_add_secondary():
#     """
#     If one of 65-128 fields are in bitmap then add field 1.
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["66"]["data_enc"] = "ascii"
#     spec["66"]["len_enc"] = "ascii"
#     spec["66"]["len_type"] = 2
#     spec["66"]["max_len"] = 19
#
#     doc_dec = {
#         "h": "header",
#         "t": "0200",
#         "66": "1234567890",
#     }
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that((
#         s
#         == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x30, 0x30, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x31, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30])
#     )
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0200"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "8000000000000000"
#
#     asserts.assert_that(doc_enc["1"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["1"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["1"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["66"]["len"] == bytes([0x31, 0x30])
#     asserts.assert_that(doc_enc["66"]["data"] == bytes([0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30])
#     asserts.assert_that(doc_dec["66"] == "1234567890"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "1", "66"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "1", "66"])
#
#
# def test_primary_bitmap_incorrect_encoding():
#     """
#     Incorrect encoding specified for primary bitmap
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "invalid"
#     spec["1"]["len_type"] = 0
#     spec["1"]["max_len"] = 0
#
#     doc_dec = {"h": "header", "t": "0210", "2": ""}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .unknown encoding: invalid.: field p",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_secondary_bitmap_incorrect_encoding():
#     """
#     Incorrect encoding specified for secondary bitmap
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["1"]["len_type"] = 0
#     spec["1"]["max_len"] = 16
#     spec["1"]["data_enc"] = "invalid"
#
#     doc_dec = {"h": "header", "t": "0210", "65": ""}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .unknown encoding: invalid.: field 1",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
def test_bitmaps_ascii():
    """
    Field is required and not key provided
    """
    # spec["h"]["data_enc"] = "ascii"
    # spec["h"]["len_type"] = 0
    # spec["h"]["max_len"] = 6
    # spec["t"]["data_enc"] = "ascii"
    # spec["p"]["data_enc"] = "ascii"
    # spec["1"]["data_enc"] = "ascii"
    # spec["105"]["len_enc"] = "ascii"


    spec = {
        "h": {
            "data_enc": "ascii",
            "len_type": 0,
            "max_len": 6,
        },
        "t": {
            "data_enc": "ascii",
        },
        "p": {
            "data_enc": "ascii",
        },
        "1": {
            "data_enc": "ascii",
            "len_type": 0,
            "max_len": 0
        },
        "105": {
            "data_enc": "ascii",
            "len_type": 0,
            "max_len": 0
        },
    }

    doc_dec = {"h": "header", "t": "0210", "105": ""}

    s, doc_enc = Encoder.encode(doc_dec, spec)
    print(str(s))
    print(str(doc_enc))
    asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x38, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x38, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30]))
    #
    asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72]))
    asserts.assert_that(doc_dec["h"] == "header")
    #
    asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30]))
    asserts.assert_that(doc_dec["t"] == "0210")
    #
    asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_enc["p"]["data"] == bytes([0x38, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30]))
    asserts.assert_that(doc_dec["p"] == "8000000000000000")
    #
    asserts.assert_that(doc_enc["1"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_enc["1"]["data"] == bytes([0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x38, 0x30, 0x30, 0x30, 0x30, 0x30]))
    asserts.assert_that(doc_dec["1"] == "0000000000800000")
    #
    asserts.assert_that(doc_enc["105"]["len"] == bytes([0x30, 0x30, 0x30]))
    asserts.assert_that(doc_enc["105"]["data"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_dec["105"] == "")
    #
    asserts.assert_that(doc_enc.keys() == sets.make(["h", "t", "p", "1", "105"]))
    asserts.assert_that(doc_dec.keys() == sets.make(["h", "t", "p", "1", "105"]))


def test_bitmaps_ebcidic():
    """
    Field is required and not key provided
    """
    # spec["h"]["data_enc"] = "ascii"
    # spec["h"]["len_type"] = 0
    # spec["h"]["max_len"] = 6
    # spec["t"]["data_enc"] = "ascii"
    # spec["p"]["data_enc"] = "cp500"
    # spec["1"]["data_enc"] = "cp500"
    # spec["105"]["len_enc"] = "ascii"
    spec = {
        "h": {
            "data_enc": "ascii",
            "len_type": 0,
            "max_len": 6,
        },
        "t": {
            "data_enc": "ascii",
        },
        "p": {
            "data_enc": "cp500",
        },
        "1": {
            "data_enc": "cp500",
            "len_type": 0,
            "max_len": 0
        },
        "105": {
            "data_enc": "ascii",
            "len_type": 0,
            "max_len": 0
        },
    }
    doc_dec = {"h": "header", "t": "0210", "105": ""}

    s, doc_enc = Encoder.encode(doc_dec, spec)

    asserts.assert_that(
        s
        == bytearray(bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0xf8, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0]))
        + bytearray(bytes([0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0]))
        + bytearray(bytes([0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf8, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0x30, 0x30, 0x30]))
    )

    asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72]))
    asserts.assert_that(doc_dec["h"] == "header")

    asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30]))
    asserts.assert_that(doc_dec["t"] == "0210")

    asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(
        doc_enc["p"]["data"]
        == bytes([0xf8, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0])
    )
    asserts.assert_that(doc_dec["p"] == "8000000000000000")

    asserts.assert_that(doc_enc["1"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(
        doc_enc["1"]["data"]
        == bytes([0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0, 0xf8, 0xf0, 0xf0, 0xf0, 0xf0, 0xf0])
    )
    asserts.assert_that(doc_dec["1"] == "0000000000800000")

    asserts.assert_that(doc_enc["105"]["len"] == bytes([0x30, 0x30, 0x30]))
    asserts.assert_that(doc_enc["105"]["data"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_dec["105"] == "")

    asserts.assert_that(doc_enc.keys() == sets.make(["h", "t", "p", "1", "105"]))
    asserts.assert_that(doc_dec.keys() == sets.make(["h", "t", "p", "1", "105"]))


def test_bitmaps_bcd():
    """
    Field is required and not key provided
    """
    # spec["h"]["data_enc"] = "ascii"
    # spec["h"]["len_type"] = 0
    # spec["h"]["max_len"] = 6
    # spec["t"]["data_enc"] = "ascii"
    # spec["p"]["data_enc"] = "b"
    # spec["1"]["data_enc"] = "b"
    # spec["105"]["len_enc"] = "ascii"
    spec = {
        "h": {
            "data_enc": "ascii",
            "len_type": 0,
            "max_len": 6,
        },
        "t": {
            "data_enc": "ascii",
        },
        "p": {
            "data_enc": "b",
        },
        "1": {
            "data_enc": "b",
            "len_type": 0,
            "max_len": 0
        },
        "105": {
            "data_enc": "ascii",
            "len_type": 0,
            "max_len": 0
        },
    }

    doc_dec = {"h": "header", "t": "0210", "105": ""}

    s, doc_enc = Encoder.encode(doc_dec, spec)

    asserts.assert_that(
        s
        == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x30, 0x30, 0x30])
    )

    asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72]))
    asserts.assert_that(doc_dec["h"] == "header")

    asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30]))
    asserts.assert_that(doc_dec["t"] == "0210")

    asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_enc["p"]["data"] == bytes([0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]))
    asserts.assert_that(doc_dec["p"] == "8000000000000000")

    asserts.assert_that(doc_enc["1"]["len"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_enc["1"]["data"] == bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00]))
    asserts.assert_that(doc_dec["1"] == "0000000000800000")

    asserts.assert_that(doc_enc["105"]["len"] == bytes([0x30, 0x30, 0x30]))
    asserts.assert_that(doc_enc["105"]["data"] == bytes(r"", encoding='utf-8'))
    asserts.assert_that(doc_dec["105"] == "")

    asserts.assert_that(doc_enc.keys() == sets.make(["h", "t", "p", "1", "105"]))
    asserts.assert_that(doc_dec.keys() == sets.make(["h", "t", "p", "1", "105"]))


# def test_primary_bitmap_ascii_upper_case():
#     """
#     This test makes sure that encoded primary bitmap is in upper case.
#     """
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 0
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#
#     spec["5"]["len_type"] = 0
#     spec["5"]["max_len"] = 1
#     spec["5"]["data_enc"] = "ascii"
#     spec["7"]["len_type"] = 0
#     spec["7"]["max_len"] = 1
#     spec["7"]["data_enc"] = "ascii"
#
#     doc_dec = {"t": "0200", "5": "A", "7": "B"}
#     s, doc_enc = iso8583.encode(doc_dec, spec)
#     asserts.assert_that(s == bytes([0x30, 0x32, 0x30, 0x30, 0x30, 0x41, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x41, 0x42])
#     asserts.assert_that(doc_dec["p"] == "0A00000000000000"
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x30, 0x41, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30])
#     asserts.assert_that(doc_enc["5"]["data"] == bytes([0x41])
#     asserts.assert_that(doc_enc["7"]["data"] == bytes([0x42])
#
#
# def test_secondary_bitmap_ascii_upper_case():
#     """
#     This test makes sure that encoded secondary bitmap is in upper case.
#     """
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 0
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["1"]["data_enc"] = "ascii"
#
#     spec["69"]["len_type"] = 0
#     spec["69"]["max_len"] = 1
#     spec["69"]["data_enc"] = "ascii"
#     spec["71"]["len_type"] = 0
#     spec["71"]["max_len"] = 1
#     spec["71"]["data_enc"] = "ascii"
#
#     doc_dec = {"t": "0200", "69": "A", "71": "B"}
#     s, doc_enc = iso8583.encode(doc_dec, spec)
#     asserts.assert_that(s == bytes([0x30, 0x32, 0x30, 0x30, 0x38, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x41, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x41, 0x42])
#     asserts.assert_that(doc_dec["p"] == "8000000000000000"
#     asserts.assert_that(doc_dec["1"] == "0A00000000000000"
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x30, 0x30])
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x38, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30])
#     asserts.assert_that(doc_enc["1"]["data"] == bytes([0x30, 0x41, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30])
#     asserts.assert_that(doc_enc["69"]["data"] == bytes([0x41])
#     asserts.assert_that(doc_enc["71"]["data"] == bytes([0x42])
#
#
# def test_fixed_field_ascii_absent():
#     """
#     ASCII fixed field is required and not provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "ascii"
#
#     doc_dec = {"h": "header", "t": "0210", "2": ""}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 0 bytes, expecting 2: field 2"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_ascii_partial():
#     """
#     ASCII fixed field is required and partially provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "ascii"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "1"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 1 bytes, expecting 2: field 2"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_ascii_over_max():
#     """
#     ASCII fixed field is required and over max provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "ascii"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "123"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 3 bytes, expecting 2: field 2"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_ascii_incorrect_data():
#     """
#     ASCII fixed field is required and provided.
#     However, the data is not ASCII
#     CPython and PyPy throw differently worded exception
#     CPython: 'ascii' codec can't encode characters in position 0-1: ordinal not in range(128)
#     PyPy:    'ascii' codec can't encode character '\\xff' in position 0: ordinal not in range(128)
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "ascii"
#
#     doc_dec = {
#         "h": "header",
#         "t": "0210",
#         "2": bytes([0xff, 0xff]).decode("latin-1"),
#     }
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .'ascii' codec can't encode character.*: ordinal not in range.128..: field 2",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_ascii_present():
#     """
#     ASCII fixed field is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "ascii"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "22"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x32, 0x32])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["2"]["data"] == bytes([0x32, 0x32])
#     asserts.assert_that(doc_dec["2"] == "22"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_fixed_field_ascii_present_zero_legnth():
#     """
#     ASCII zero-length fixed field is required and provided
#     This is pointless but should work.
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 0
#     spec["2"]["data_enc"] = "ascii"
#
#     doc_dec = {"h": "header", "t": "0210", "2": ""}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["2"]["data"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_dec["2"] == ""
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_fixed_field_ebcdic_absent():
#     """
#     EBCDIC fixed field is required and not provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "cp500"
#
#     doc_dec = {"h": "header", "t": "0210", "2": ""}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 0 bytes, expecting 2: field 2"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_ebcdic_partial():
#     """
#     EBCDIC fixed field is required and partially provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "cp500"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "1"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 1 bytes, expecting 2: field 2"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_ebcdic_over_max():
#     """
#     EBCDIC fixed field is required and over max provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "cp500"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "123"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 3 bytes, expecting 2: field 2"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_ebcdic_present():
#     """
#     EBCDIC fixed field is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "cp500"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "22"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf2, 0xf2])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["2"]["data"] == bytes([0xf2, 0xf2])
#     asserts.assert_that(doc_dec["2"] == "22"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_fixed_field_ebcdic_present_zero_legnth():
#     """
#     EBCDIC zero-length fixed field is required and provided
#     This is pointless but should work.
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 0
#     spec["2"]["data_enc"] = "cp500"
#
#     doc_dec = {"h": "header", "t": "0210", "2": ""}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["2"]["data"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_dec["2"] == ""
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_fixed_field_bdc_absent():
#     """
#     BDC fixed field is required and not provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210", "2": ""}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 0 bytes, expecting 2: field 2"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_bdc_partial():
#     """
#     BDC fixed field is required and partial is provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "12"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 1 bytes, expecting 2: field 2"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_bdc_over_max():
#     """
#     BDC fixed field is required and over max is provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "123456"}
#
#     with pytest.raises(
#         iso8583.EncodeError, match="Field data is 3 bytes, expecting 2: field 2"
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_bdc_odd():
#     """
#     BDC fixed field is required and odd length is provided
#     CPython and PyPy throw differently worded exception
#     CPython: non-hexadecimal number found in fromhex() arg at position 5
#     PyPy:    non-hexadecimal number found in fromhex() arg at position 4
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "12345"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .non-hexadecimal number found in fromhex.. arg at position 4|5.: field 2",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_bdc_non_hex():
#     """
#     BDC fixed field is required and provided
#     However, the data is not hex
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "11xx"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .non-hexadecimal number found in fromhex.. arg at position 2.: field 2",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_fixed_field_bcd_present():
#     """
#     BCD fixed field is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "1122"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x11])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["2"]["data"] == bytes([0x11])
#     asserts.assert_that(doc_dec["2"] == "1122"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_fixed_field_bcd_present_zero_length():
#     """
#     BCD zero-length fixed field is required and provided
#     This is pointless but should work.
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 0
#     spec["2"]["data_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210", "2": ""}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["2"]["data"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_dec["2"] == ""
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_fixed_field_incorrect_encoding():
#     """
#     Fixed field is required and provided.
#     However, the spec encoding is not correct
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 0
#     spec["2"]["max_len"] = 2
#     spec["2"]["data_enc"] = "invalid"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "1122"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .unknown encoding: invalid.: field 2",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_variable_field_ascii_over_max():
#     """
#     ASCII variable field is required and over max provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "ascii"
#     spec["2"]["len_enc"] = "ascii"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "12345678901"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Field data is 11 bytes, larger than maximum 10: field 2",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_variable_field_ascii_present():
#     """
#     ASCII variable field is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "ascii"
#     spec["2"]["len_enc"] = "ascii"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "1122"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x30, 0x34, 0x31, 0x31, 0x32, 0x32])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes([0x30, 0x34])
#     asserts.assert_that(doc_enc["2"]["data"] == bytes([0x31, 0x31, 0x32, 0x32])
#     asserts.assert_that(doc_dec["2"] == "1122"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_variable_field_ascii_present_zero_legnth():
#     """
#     ASCII zero-length variable field is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "ascii"
#     spec["2"]["len_enc"] = "ascii"
#
#     doc_dec = {"h": "header", "t": "0210", "2": ""}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x30, 0x30])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes([0x30, 0x30])
#     asserts.assert_that(doc_enc["2"]["data"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_dec["2"] == ""
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_variable_field_ebcdic_over_max():
#     """
#     EBCDIC variable field is required and over max provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "ascii"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "cp500"
#     spec["2"]["len_enc"] = "cp500"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "12345678901"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Field data is 11 bytes, larger than maximum 10: field 2",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_variable_field_ebcdic_present():
#     """
#     EBCDIC variable field is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "cp500"
#     spec["2"]["len_enc"] = "cp500"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "1122"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf0, 0xf4, 0xf1, 0xf1, 0xf2, 0xf2])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes([0xf0, 0xf4])
#     asserts.assert_that(doc_enc["2"]["data"] == bytes([0xf1, 0xf1, 0xf2, 0xf2])
#     asserts.assert_that(doc_dec["2"] == "1122"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_variable_field_ebcdic_present_zero_legnth():
#     """
#     EBCDIC zero-length variable field is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "cp500"
#     spec["2"]["len_enc"] = "cp500"
#
#     doc_dec = {"h": "header", "t": "0210", "2": ""}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf0, 0xf0])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes([0xf0, 0xf0])
#     asserts.assert_that(doc_enc["2"]["data"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_dec["2"] == ""
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_variable_field_bdc_over_max():
#     """
#     BDC variable field is required and over max is provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 5
#     spec["2"]["data_enc"] = "b"
#     spec["2"]["len_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "123456789012"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Field data is 6 bytes, larger than maximum 5: field 2",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_variable_field_bdc_odd():
#     """
#     BDC variable field is required and odd length is provided
#     CPython and PyPy throw differently worded exception
#     CPython: non-hexadecimal number found in fromhex() arg at position 5
#     PyPy:    non-hexadecimal number found in fromhex() arg at position 4
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "b"
#     spec["2"]["len_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "12345"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode .non-hexadecimal number found in fromhex.. arg at position 4|5.: field 2",
#     ):
#         iso8583.encode(doc_dec, spec=spec)
#
#
# def test_variable_field_bdc_ascii_length():
#     """
#     BDC variable field is required and provided
#     The length is in ASCII.
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 3
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "b"
#     spec["2"]["len_enc"] = "ascii"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "1122"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x30, 0x30, 0x32, 0x11])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes([0x30, 0x30, 0x32])
#     asserts.assert_that(doc_enc["2"]["data"] == bytes([0x11])
#     asserts.assert_that(doc_dec["2"] == "1122"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_variable_field_bdc_ebcdic_length():
#     """
#     BDC variable field is required and provided
#     The length is in EBCDIC.
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 3
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "b"
#     spec["2"]["len_enc"] = "cp500"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "1122"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf0, 0xf0, 0xf2, 0x11])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes([0xf0, 0xf0, 0xf2])
#     asserts.assert_that(doc_enc["2"]["data"] == bytes([0x11])
#     asserts.assert_that(doc_dec["2"] == "1122"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_variable_field_bcd_present():
#     """
#     BCD variable field is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "b"
#     spec["2"]["len_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "1122"}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x11])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes([0x00, 0x02])
#     asserts.assert_that(doc_enc["2"]["data"] == bytes([0x11])
#     asserts.assert_that(doc_dec["2"] == "1122"
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_variable_field_bcd_present_zero_length():
#     """
#     BCD zero-length variable field is required and provided
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "b"
#     spec["2"]["len_enc"] = "b"
#
#     doc_dec = {"h": "header", "t": "0210", "2": ""}
#
#     s, doc_enc = iso8583.encode(doc_dec, spec=spec)
#
#     asserts.assert_that(s == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72, 0x30, 0x32, 0x31, 0x30, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#
#     asserts.assert_that(doc_enc["h"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["h"]["data"] == bytes([0x68, 0x65, 0x61, 0x64, 0x65, 0x72])
#     asserts.assert_that(doc_dec["h"] == "header"
#
#     asserts.assert_that(doc_enc["t"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["t"]["data"] == bytes([0x30, 0x32, 0x31, 0x30])
#     asserts.assert_that(doc_dec["t"] == "0210"
#
#     asserts.assert_that(doc_enc["p"]["len"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_enc["p"]["data"] == bytes([0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
#     asserts.assert_that(doc_dec["p"] == "4000000000000000"
#
#     asserts.assert_that(doc_enc["2"]["len"] == bytes([0x00, 0x00])
#     asserts.assert_that(doc_enc["2"]["data"] == bytes(r"", encoding='utf-8')
#     asserts.assert_that(doc_dec["2"] == ""
#
#     asserts.assert_that(doc_enc.keys() == set(["h", "t", "p", "2"])
#     asserts.assert_that(doc_dec.keys() == set(["h", "t", "p", "2"])
#
#
# def test_variable_field_incorrect_encoding():
#     """
#     Variable field is required and provided.
#     However, the spec encoding is not correct for length
#     """
#     spec["h"]["data_enc"] = "ascii"
#     spec["h"]["len_type"] = 0
#     spec["h"]["max_len"] = 6
#     spec["t"]["data_enc"] = "ascii"
#     spec["p"]["data_enc"] = "b"
#     spec["2"]["len_type"] = 2
#     spec["2"]["max_len"] = 10
#     spec["2"]["data_enc"] = "ascii"
#     spec["2"]["len_enc"] = "invalid"
#
#     doc_dec = {"h": "header", "t": "0210", "2": "1122"}
#
#     with pytest.raises(
#         iso8583.EncodeError,
#         match="Failed to encode length .unknown encoding: invalid.: field 2",
#     ):
#         iso8583.encode(doc_dec, spec=spec)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_bitmaps_ascii))
    _suite.addTest(unittest.FunctionTestCase(test_bitmaps_ebcidic))
    _suite.addTest(unittest.FunctionTestCase(test_bitmaps_bcd))
    _suite.addTest(unittest.FunctionTestCase(test_EncodeError_exception))
    _suite.addTest(unittest.FunctionTestCase(test_non_string_field_keys))

    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
