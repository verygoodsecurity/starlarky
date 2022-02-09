load("@stdlib//larky", larky="larky")
load("@vendor//ecdsa/ecdsa", ecdsa="ecdsa")
load("@vendor//ecdsa/util", orderlen="orderlen")
load("@vendor//ecdsa/der", "der")
load("@stdlib//sets", Set="Set")
load("@vendor//ecdsa/_compat", normalise_bytes="normalise_bytes")


PRIME_FIELD_OID = (1, 2, 840, 10045, 1, 1)
CHARACTERISTIC_TWO_FIELD_OID = (1, 2, 840, 10045, 1, 2)

def Curve(name=None, curve=None, generator=None, oid=None, openssl_name=None):

    self = larky.mutablestruct(__class__=Curve, __name__="Curve")
    
    def __init__(name, curve, generator, oid, openssl_name=None):
        self.name = name
        self.openssl_name = openssl_name  # maybe None
        self.curve = curve
        self.generator = generator
        self.order = generator.order()
        # if isinstance(curve, ellipticcurve.CurveEdTw):
        #     # EdDSA keys are special in that both private and public
        #     # are the same size (as it's defined only with compressed points)

        #     # +1 for the sign bit and then round up
        #     self.baselen = (bit_length(curve.p()) + 1 + 7) // 8
        #     self.verifying_key_length = self.baselen
        # else:
        #     self.baselen = orderlen(self.order)
        #     self.verifying_key_length = 2 * orderlen(curve.p())
        self.baselen = orderlen(self.order)
        self.verifying_key_length = 2 * orderlen(curve.p())
        self.signature_length = 2 * self.baselen
        self.oid = oid
        if oid:
            self.encoded_oid = der.encode_oid(*oid)
        return self
    self = __init__(name, curve, generator, oid, openssl_name)
    
    return self

SECP256k1 = Curve(
    "SECP256k1",
    ecdsa.curve_secp256k1,
    ecdsa.generator_secp256k1,
    (1, 3, 132, 0, 10),
    "secp256k1",
)

# no order in particular, but keep previously added curves first
curves = [
    # NIST192p,
    # NIST224p,
    # NIST256p,
    # NIST384p,
    # NIST521p,
    SECP256k1,
    # BRAINPOOLP160r1,
    # BRAINPOOLP192r1,
    # BRAINPOOLP224r1,
    # BRAINPOOLP256r1,
    # BRAINPOOLP320r1,
    # BRAINPOOLP384r1,
    # BRAINPOOLP512r1,
    # SECP112r1,
    # SECP112r2,
    # SECP128r1,
    # SECP160r1,
    # Ed25519,
    # Ed448,
]

def find_curve(oid_curve):
    # for idx in range(len(curves)):
    #     c = curves[idx]
    #     if c.oid == oid_curve:
    #         return c
    if oid_curve == (1, 3, 132, 0, 10):
        return SECP256k1
    fail('UnknownCurveError("I don\'t know about the curve with oid %s. I only know about these: %s"' % (oid_curve, SECP256k1.name))

# @staticmethod
def from_der(data, valid_encodings=None):
    """Decode the curve parameters from DER file.
    :param data: the binary string to decode the parameters from
    :type data: :term:`bytes-like object`
    :param valid_encodings: set of names of allowed encodings, by default
        all (set by passing ``None``), supported ones are ``named_curve``
        and ``explicit``
    :type valid_encodings: :term:`set-like object`
    """
    if not valid_encodings:
        valid_encodings = Set(("named_curve", "explicit"))
    # if not all(i in ["named_curve", "explicit"] for i in valid_encodings):
    #     fail('ValueError("Only named_curve and explicit encodings supported")')
    # To do: cast the input into array of bytes
    data = normalise_bytes(data)
    if not der.is_sequence(data):
        if "named_curve" not in valid_encodings:
            fail('der.UnexpectedDER("named_curve curve parameters not allowed"')
        oid, empty = der.remove_object(data)
        if empty:
            fail('der.UnexpectedDER("Unexpected data after OID")')
        return find_curve(oid)
    else:
        fail('ValueError("Only SECP256k1 supported")')

    # if "explicit" not in valid_encodings:
    #     fail('der.UnexpectedDER("explicit curve parameters not allowed")')

    # seq, empty = der.remove_sequence(data)
    # if empty:
    #     fail('der.UnexpectedDER("Unexpected data after ECParameters structure")')
    # # decode the ECParameters sequence
    # version, rest = der.remove_integer(seq)
    # if version != 1:
    #     fail('der.UnexpectedDER("Unknown parameter encoding format")')
    # field_id, rest = der.remove_sequence(rest)
    # curve, rest = der.remove_sequence(rest)
    # base_bytes, rest = der.remove_octet_string(rest)
    # order, rest = der.remove_integer(rest)
    # cofactor = None
    # if rest:
    #     # the ASN.1 specification of ECParameters allows for future
    #     # extensions of the sequence, so ignore the remaining bytes
    #     cofactor, _ = der.remove_integer(rest)

    # # decode the ECParameters.fieldID sequence
    # field_type, rest = der.remove_object(field_id)
    # if field_type == CHARACTERISTIC_TWO_FIELD_OID:
    #     fail('UnknownCurveError("Characteristic 2 curves unsupported")')
    # if field_type != PRIME_FIELD_OID:
    #     fail('UnknownCurveError("Unknown field type: %s"' % field_type)
    # prime, empty = der.remove_integer(rest)
    # if empty:
    #     fail('der.UnexpectedDER("Unexpected data after ECParameters.fieldID.Prime-p element"')

    # # decode the ECParameters.curve sequence
    # curve_a_bytes, rest = der.remove_octet_string(curve)
    # curve_b_bytes, rest = der.remove_octet_string(rest)
    # # seed can be defined here, but we don't parse it, so ignore `rest`

    # curve_a = string_to_number(curve_a_bytes)
    # curve_b = string_to_number(curve_b_bytes)

    # curve_fp = ellipticcurve.CurveFp(prime, curve_a, curve_b, cofactor)

    # # decode the ECParameters.base point

    # base = ellipticcurve.PointJacobi.from_bytes(
    #     curve_fp,
    #     base_bytes,
    #     valid_encodings=("uncompressed", "compressed", "hybrid"),
    #     order=order,
    #     generator=True,
    # )
    # tmp_curve = Curve("unknown", curve_fp, base, None)

    # # if the curve matches one of the well-known ones, use the well-known
    # # one in preference, as it will have the OID and name associated
    # for i in curves:
    #     if tmp_curve == i:
    #         return i
    # return tmp_curve


curves = larky.struct(
    Curve=Curve,
    from_der=from_der
)
