load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//ISO8583Decoder", Decoder="Decoder")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")


def MyTestCase_test_decode():
    payload = unhexlify(bytes(hex_string_payload, encoding='utf-8'))
    decoded, encoded = Decoder.decode(payload, test_payload_spec)
    print(decoded)
    # asserts.assert_that('0100').is_equal_to(decoded)
    asserts.assert_that('0100').is_equal_to(decoded['t'])
    asserts.assert_that('FEFA448108E0E48A' == decoded['p'])
    asserts.assert_that('100194868740300').is_equal_to(decoded['2'])


hex_string_payload = '30313030fefa448108e0e48a0000000004020008313531303031393438363837343033303030303030303030303030303030303135303030303030303030303135303030303030303030303137303030333035313334393033363130303030303036313030303030303030303030313133343930333033303530333035353439393031323030303630313233343531303030303031303030303431323334353637383132333435363738393132333435364d5352204d45524348414e54202020202020202020202020204252555353454c5320202020202020555341383430383430383430303230303037323834304430303030303030303032303030303331363030313630303032303030303237313330303030303730202020202020202020202020202020202020202020202020202020202020202020203020202020202020202020202020202020202020202020204d4153544552434152442020313531303031393438363837343033303034333220202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020303030303030302020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020203030303030303030303030303030202020202020202020202020202020303030303030303030202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020203030302020202020303030202020202020202020202020202020202020202020202020202020202020202020203030303030302020202020202020202020202020203031373533373533363630303030203030303030303030303030303030303020202020202020202020202020206e7520202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020206e2020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020203020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020203030202020202020202020202020202020'

test_payload_spec = {
    "h": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 0,
        "desc": "Message Header",
    },
    "t": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 4,
        "desc": "Message Type",
    },
    "p": {
        "data_enc": "b",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 8,
        "desc": "Bitmap, Primary",
    },
    "1": {
        "data_enc": "b",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 8,
        "desc": "Bitmap, Secondary",
    },
    "2": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 2,
        "max_len": 19,
        "desc": "Primary Account Number (PAN)",
    },
    "3": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 6,
        "desc": "Processing Code",
    },
    "4": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 12,
        "desc": "Amount, Transaction",
    },
    "5": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 12,
        "desc": "Amount, Settlement",
    },
    "6": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 12,
        "desc": "Amount, Cardholder Billing",
    },
    "7": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 10,
        "desc": "Transmission Date and Time",
    },
    "8": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 8,
        "desc": "Amount, Cardholder Billing Fee",
    },
    "9": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 8,
        "desc": "Conversion Rate, Settlement",
    },
    "10": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 8,
        "desc": "Conversion Rate, Cardholder Billing",
    },
    "11": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 6,
        "desc": "System Trace Audit Number",
    },
    "12": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 6,
        "desc": "Time, Local Transaction",
    },
    "13": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 4,
        "desc": "Date, Local Transaction",
    },
    "14": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 4,
        "desc": "Date, Expiration",
    },
    "15": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 4,
        "desc": "Date, Settlement",
    },
    "16": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 4,
        "desc": "Date, Conversion",
    },
    "17": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 4,
        "desc": "Date, Capture",
    },
    "18": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 4,
        "desc": "Merchant Type",
    },
    "19": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "Acquiring Institution Country Code",
    },
    "20": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "PAN Country Code",
    },
    "21": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "Forwarding Institution Country Code",
    },
    "22": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "Point-of-Service Entry Mode",
    },
    "23": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "PAN Sequence Number",
    },
    "24": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "Network International ID (NII)",
    },
    "25": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 2,
        "desc": "Point-of-Service Condition Code",
    },
    "26": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 2,
        "desc": "Point-of-Service Capture Code",
    },
    "27": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 1,
        "desc": "Authorizing ID Response Length",
    },
    "28": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 9,
        "desc": "Amount, Transaction Fee",
    },
    "29": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 9,
        "desc": "Amount, Settlement Fee",
    },
    "30": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 9,
        "desc": "Amount, Transaction Processing Fee",
    },
    "31": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 9,
        "desc": "Amount, Settlement Processing Fee",
    },
    "32": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 2,
        "max_len": 11,
        "desc": "Acquiring Institution ID Code",
    },
    "33": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 2,
        "max_len": 11,
        "desc": "Forwarding Institution ID Code",
    },
    "34": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 2,
        "max_len": 28,
        "desc": "Primary Account Number, Extended",
    },
    "35": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 2,
        "max_len": 37,
        "desc": "Track 2 Data",
    },
    "36": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 104,
        "desc": "Track 3 Data",
    },
    "37": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 12,
        "desc": "Retrieval Reference Number",
    },
    "38": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 6,
        "desc": "Authorization ID Response",
    },
    "39": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 2,
        "desc": "Response Code",
    },
    "40": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "Service Restriction Code",
    },
    "41": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 8,
        "desc": "Card Acceptor Terminal ID",
    },
    "42": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 15,
        "desc": "Card Acceptor ID Code",
    },
    "43": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 40,
        "desc": "Card Acceptor Name/Location",
    },
    "44": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 2,
        "max_len": 25,
        "desc": "Additional Response Data",
    },
    "45": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 2,
        "max_len": 76,
        "desc": "Track 1 Data",
    },
    "46": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Additional Data - ISO",
    },
    "47": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Additional Data - National",
    },
    "48": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Additional Data - Private",
    },
    "49": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "Currency Code, Transaction",
    },
    "50": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "Currency Code, Settlement",
    },
    "51": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "Currency Code, Cardholder Billing",
    },
    "52": {
        "data_enc": "b",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 8,
        "desc": "PIN",
    },
    "53": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 16,
        "desc": "Security-Related Control Information",
    },
    "54": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        # "max_len": 240,
        "max_len": 840,
        "desc": "Additional Amounts",
    },
    "55": {
        "data_enc": "b",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 255,
        "desc": "ICC data",
    },
    "56": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved ISO",
    },
    "57": {
        # "data_enc": "ascii",
        "data_enc": "b",
        "len_enc": "ascii",
        # "len_type": 3,
        "len_type": 0,
        # "max_len": 999,
        "max_len": 102,
        "desc": "Reserved National",
    },
    "58": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved National",
    },
    "59": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved National",
    },
    "60": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        # "len_type": 3,
        "len_type": 0,
        "max_len": 999,
        "desc": "Reserved National",
    },
    "61": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        # "len_type": 3,
        "len_type": 0,
        # "max_len": 999,
        "max_len": 0,
        "desc": "Reserved Private",
    },
    "62": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved Private",
    },
    "63": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        # "len_type": 3,
        "len_type": 0,
        # "max_len": 999,
        "max_len": 0,
        "desc": "Reserved Private",
    },
    "64": {
        "data_enc": "b",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 8,
        "desc": "MAC",
    },
    "65": {
        "data_enc": "b",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 8,
        "desc": "Bitmap, Extended",
    },
    "66": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 1,
        "desc": "Settlement Code",
    },
    "67": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 2,
        "desc": "Extended Payment Code",
    },
    "68": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "Receiving Institution Country Code",
    },
    "69": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "Settlement Institution Country Code",
    },
    "70": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 3,
        "desc": "Network Management Information Code",
    },
    "71": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 4,
        "desc": "Message Number",
    },
    "72": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 4,
        "desc": "Message Number, Last",
    },
    "73": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 6,
        "desc": "Date, Action",
    },
    "74": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 10,
        "desc": "Credits, Number",
    },
    "75": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 10,
        "desc": "Credits, Reversal Number",
    },
    "76": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 10,
        "desc": "Debits, Number",
    },
    "77": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 10,
        "desc": "Debits, Reversal Number",
    },
    "78": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 10,
        "desc": "Transfer, Number",
    },
    "79": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 10,
        "desc": "Transfer, Reversal Number",
    },
    "80": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 10,
        "desc": "Inquiries, Number",
    },
    "81": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 10,
        "desc": "Authorizations, Number",
    },
    "82": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 12,
        "desc": "Credits, Processing Fee Amount",
    },
    "83": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 12,
        "desc": "Credits, Transaction Fee Amount",
    },
    "84": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 12,
        "desc": "Debits, Processing Fee Amount",
    },
    "85": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 12,
        "desc": "Debits, Transaction Fee Amount",
    },
    "86": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 16,
        "desc": "Credits, Amount",
    },
    "87": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 16,
        "desc": "Credits, Reversal Amount",
    },
    "88": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 16,
        "desc": "Debits, Amount",
    },
    "89": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 16,
        "desc": "Debits, Reversal Amount",
    },
    "90": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 42,
        "desc": "Original Data Elements",
    },
    "91": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 1,
        "desc": "File Update Code",
    },
    "92": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 2,
        "desc": "File Security Code",
    },
    "93": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 5,
        "desc": "Response Indicator",
    },
    "94": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 7,
        "desc": "Service Indicator",
    },
    "95": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 42,
        "desc": "Replacement Amounts",
    },
    "96": {
        "data_enc": "b",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 8,
        "desc": "Message Security Code",
    },
    "97": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 17,
        "desc": "Amount, Net Settlement",
    },
    "98": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 25,
        "desc": "Payee",
    },
    "99": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 2,
        "max_len": 11,
        "desc": "Settlement Institution ID Code",
    },
    "100": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 2,
        "max_len": 11,
        "desc": "Receiving Institution ID Code",
    },
    "101": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 2,
        "max_len": 17,
        "desc": "File Name",
    },
    "102": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        # "len_type": 2,
        "len_type": 0,
        # "max_len": 28,
        "max_len": 0,
        "desc": "Account ID 1",
    },
    "103": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 2,
        "max_len": 28,
        "desc": "Account ID 2",
    },
    "104": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 100,
        "desc": "Transaction Description",
    },
    "105": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for ISO Use",
    },
    "106": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for ISO Use",
    },
    "107": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for ISO Use",
    },
    "108": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for ISO Use",
    },
    "109": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for ISO Use",
    },
    "110": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for ISO Use",
    },
    "111": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        # "len_type": 3,
        "len_type": 0,
        "max_len": 0,
        # "max_len": 999,
        "desc": "Reserved for ISO Use",
    },
    "112": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for National Use",
    },
    "113": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for National Use",
    },
    "114": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for National Use",
    },
    "115": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for National Use",
    },
    "116": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for National Use",
    },
    "117": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for National Use",
    },
    "118": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for National Use",
    },
    "119": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for National Use",
    },
    "120": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for Private Use",
    },
    "121": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for Private Use",
    },
    "122": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for Private Use",
    },
    "123": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for Private Use",
    },
    "124": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for Private Use",
    },
    "125": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        # "len_type": 3,
        "len_type": 0,
        # "max_len": 999,
        "max_len": 0,
        "desc": "Reserved for Private Use",
    },
    "126": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for Private Use",
    },
    "127": {
        "data_enc": "ascii",
        "len_enc": "ascii",
        "len_type": 3,
        "max_len": 999,
        "desc": "Reserved for Private Use",
    },
    "128": {
        "data_enc": "b",
        "len_enc": "ascii",
        "len_type": 0,
        "max_len": 8,
        "desc": "MAC",
    },
}

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(MyTestCase_test_decode))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
