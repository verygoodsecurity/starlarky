def _is_number(x, only_non_negative=False):
    """
    This class differs from BytesIO in that a ValueError exception is
        raised whenever EOF is reached.
    """
    def __init__(self, initial_bytes):
        """
        Not enough data for DER decoding: expected %d bytes and found %d
        """
    def read_byte(self):
        """
        Base class for defining a single DER object.

                This class should never be directly instantiated.
        
        """
2021-03-02 17:42:03,394 : INFO : tokenize_signature : --> do i ever get here?
        def __init__(self, asn1Id=None, payload=b'', implicit=None,
                     constructed=False, explicit=None):
            """
            Initialize the DER object according to a specific ASN.1 type.

                            :Parameters:
                              asn1Id : integer
                                The universal DER tag number for this object
                                (e.g. 0x10 for a SEQUENCE).
                                If None, the tag is not known yet.

                              payload : byte string
                                The initial payload of the object (that it,
                                the content octets).
                                If not specified, the payload is empty.

                              implicit : integer
                                The IMPLICIT tag number to use for the encoded object.
                                It overrides the universal tag *asn1Id*.

                              constructed : bool
                                True when the ASN.1 type is *constructed*.
                                False when it is *primitive*.

                              explicit : integer
                                The EXPLICIT tag number to use for the encoded object.
                
            """
        def _convertTag(self, tag):
            """
            Check if *tag* is a real DER tag.
                            Convert it from a character to number if necessary.
                
            """
        def _definite_form(length):
            """
            Build length octets according to BER/DER
                            definite form.
                
            """
        def encode(self):
            """
            Return this DER element, fully encoded as a binary byte string.
            """
        def _decodeLen(self, s):
            """
            Decode DER length octets from a file.
            """
        def decode(self, der_encoded, strict=False):
            """
            Decode a complete DER element, and re-initializes this
                            object with it.

                            Args:
                              der_encoded (byte string): A complete DER element.

                            Raises:
                              ValueError: in case of parsing errors.
                
            """
        def _decodeFromStream(self, s, strict):
            """
            Decode a complete DER element from a file.
            """
def DerInteger(DerObject):
    """
    Class to model a DER INTEGER.

            An example of encoding is::

              >>> from Crypto.Util.asn1 import DerInteger
              >>> from binascii import hexlify, unhexlify
              >>> int_der = DerInteger(9)
              >>> print hexlify(int_der.encode())

            which will show ``020109``, the DER encoding of 9.

            And for decoding::

              >>> s = unhexlify(b'020109')
              >>> try:
              >>>   int_der = DerInteger()
              >>>   int_der.decode(s)
              >>>   print int_der.value
              >>> except ValueError:
              >>>   print "Not a valid DER INTEGER"

            the output will be ``9``.

            :ivar value: The integer value
            :vartype value: integer
        
    """
        def __init__(self, value=0, implicit=None, explicit=None):
            """
            Initialize the DER object as an INTEGER.

                            :Parameters:
                              value : integer
                                The value of the integer.

                              implicit : integer
                                The IMPLICIT tag to use for the encoded object.
                                It overrides the universal tag for INTEGER (2).
                
            """
        def encode(self):
            """
            Return the DER INTEGER, fully encoded as a
                            binary string.
            """
        def decode(self, der_encoded, strict=False):
            """
            Decode a complete DER INTEGER DER, and re-initializes this
                            object with it.

                            Args:
                              der_encoded (byte string): A complete INTEGER DER element.

                            Raises:
                              ValueError: in case of parsing errors.
                
            """
        def _decodeFromStream(self, s, strict):
            """
            Decode a complete DER INTEGER from a file.
            """
def DerSequence(DerObject):
    """
    Class to model a DER SEQUENCE.

            This object behaves like a dynamic Python sequence.

            Sub-elements that are INTEGERs behave like Python integers.

            Any other sub-element is a binary string encoded as a complete DER
            sub-element (TLV).

            An example of encoding is:

              >>> from Crypto.Util.asn1 import DerSequence, DerInteger
              >>> from binascii import hexlify, unhexlify
              >>> obj_der = unhexlify('070102')
              >>> seq_der = DerSequence([4])
              >>> seq_der.append(9)
              >>> seq_der.append(obj_der.encode())
              >>> print hexlify(seq_der.encode())

            which will show ``3009020104020109070102``, the DER encoding of the
            sequence containing ``4``, ``9``, and the object with payload ``02``.

            For decoding:

              >>> s = unhexlify(b'3009020104020109070102')
              >>> try:
              >>>   seq_der = DerSequence()
              >>>   seq_der.decode(s)
              >>>   print len(seq_der)
              >>>   print seq_der[0]
              >>>   print seq_der[:]
              >>> except ValueError:
              >>>   print "Not a valid DER SEQUENCE"

            the output will be::

              3
              4
              [4, 9, b'\x07\x01\x02']

        
    """
        def __init__(self, startSeq=None, implicit=None):
            """
            Initialize the DER object as a SEQUENCE.

                            :Parameters:
                              startSeq : Python sequence
                                A sequence whose element are either integers or
                                other DER objects.

                              implicit : integer
                                The IMPLICIT tag to use for the encoded object.
                                It overrides the universal tag for SEQUENCE (16).
                
            """
        def __delitem__(self, n):
            """
            Return the number of items in this sequence that are
                            integers.

                            Args:
                              only_non_negative (boolean):
                                If ``True``, negative integers are not counted in.
                
            """
        def hasOnlyInts(self, only_non_negative=True):
            """
            Return ``True`` if all items in this sequence are integers
                            or non-negative integers.

                            This function returns False is the sequence is empty,
                            or at least one member is not an integer.

                            Args:
                              only_non_negative (boolean):
                                If ``True``, the presence of negative integers
                                causes the method to return ``False``.
            """
        def encode(self):
            """
            Return this DER SEQUENCE, fully encoded as a
                            binary string.

                            Raises:
                              ValueError: if some elements in the sequence are neither integers
                                          nor byte strings.
                
            """
        def decode(self, der_encoded, strict=False, nr_elements=None, only_ints_expected=False):
            """
            Decode a complete DER SEQUENCE, and re-initializes this
                            object with it.

                            Args:
                              der_encoded (byte string):
                                A complete SEQUENCE DER element.
                              nr_elements (None or integer or list of integers):
                                The number of members the SEQUENCE can have
                              only_ints_expected (boolean):
                                Whether the SEQUENCE is expected to contain only integers.
                              strict (boolean):
                                Whether decoding must check for strict DER compliancy.

                            Raises:
                              ValueError: in case of parsing errors.

                            DER INTEGERs are decoded into Python integers. Any other DER
                            element is not decoded. Its validity is not checked.
                
            """
        def _decodeFromStream(self, s, strict):
            """
            Decode a complete DER SEQUENCE from a file.
            """
def DerOctetString(DerObject):
    """
    Class to model a DER OCTET STRING.

        An example of encoding is:

        >>> from Crypto.Util.asn1 import DerOctetString
        >>> from binascii import hexlify, unhexlify
        >>> os_der = DerOctetString(b'\\xaa')
        >>> os_der.payload += b'\\xbb'
        >>> print hexlify(os_der.encode())

        which will show ``0402aabb``, the DER encoding for the byte string
        ``b'\\xAA\\xBB'``.

        For decoding:

        >>> s = unhexlify(b'0402aabb')
        >>> try:
        >>>   os_der = DerOctetString()
        >>>   os_der.decode(s)
        >>>   print hexlify(os_der.payload)
        >>> except ValueError:
        >>>   print "Not a valid DER OCTET STRING"

        the output will be ``aabb``.

        :ivar payload: The content of the string
        :vartype payload: byte string
    
    """
    def __init__(self, value=b'', implicit=None):
        """
        Initialize the DER object as an OCTET STRING.

                :Parameters:
                  value : byte string
                    The initial payload of the object.
                    If not specified, the payload is empty.

                  implicit : integer
                    The IMPLICIT tag to use for the encoded object.
                    It overrides the universal tag for OCTET STRING (4).
        
        """
def DerNull(DerObject):
    """
    Class to model a DER NULL element.
    """
    def __init__(self):
        """
        Initialize the DER object as a NULL.
        """
def DerObjectId(DerObject):
    """
    Class to model a DER OBJECT ID.

        An example of encoding is:

        >>> from Crypto.Util.asn1 import DerObjectId
        >>> from binascii import hexlify, unhexlify
        >>> oid_der = DerObjectId("1.2")
        >>> oid_der.value += ".840.113549.1.1.1"
        >>> print hexlify(oid_der.encode())

        which will show ``06092a864886f70d010101``, the DER encoding for the
        RSA Object Identifier ``1.2.840.113549.1.1.1``.

        For decoding:

        >>> s = unhexlify(b'06092a864886f70d010101')
        >>> try:
        >>>   oid_der = DerObjectId()
        >>>   oid_der.decode(s)
        >>>   print oid_der.value
        >>> except ValueError:
        >>>   print "Not a valid DER OBJECT ID"

        the output will be ``1.2.840.113549.1.1.1``.

        :ivar value: The Object ID (OID), a dot separated list of integers
        :vartype value: string
    
    """
    def __init__(self, value='', implicit=None, explicit=None):
        """
        Initialize the DER object as an OBJECT ID.

                :Parameters:
                  value : string
                    The initial Object Identifier (e.g. "1.2.0.0.6.2").
                  implicit : integer
                    The IMPLICIT tag to use for the encoded object.
                    It overrides the universal tag for OBJECT ID (6).
                  explicit : integer
                    The EXPLICIT tag to use for the encoded object.
        
        """
    def encode(self):
        """
        Return the DER OBJECT ID, fully encoded as a
                binary string.
        """
    def decode(self, der_encoded, strict=False):
        """
        Decode a complete DER OBJECT ID, and re-initializes this
                object with it.

                Args:
                    der_encoded (byte string):
                        A complete DER OBJECT ID.
                    strict (boolean):
                        Whether decoding must check for strict DER compliancy.

                Raises:
                    ValueError: in case of parsing errors.
        
        """
    def _decodeFromStream(self, s, strict):
        """
        Decode a complete DER OBJECT ID from a file.
        """
def DerBitString(DerObject):
    """
    Class to model a DER BIT STRING.

        An example of encoding is:

        >>> from Crypto.Util.asn1 import DerBitString
        >>> from binascii import hexlify, unhexlify
        >>> bs_der = DerBitString(b'\\xaa')
        >>> bs_der.value += b'\\xbb'
        >>> print hexlify(bs_der.encode())

        which will show ``040300aabb``, the DER encoding for the bit string
        ``b'\\xAA\\xBB'``.

        For decoding:

        >>> s = unhexlify(b'040300aabb')
        >>> try:
        >>>   bs_der = DerBitString()
        >>>   bs_der.decode(s)
        >>>   print hexlify(bs_der.value)
        >>> except ValueError:
        >>>   print "Not a valid DER BIT STRING"

        the output will be ``aabb``.

        :ivar value: The content of the string
        :vartype value: byte string
    
    """
    def __init__(self, value=b'', implicit=None, explicit=None):
        """
        Initialize the DER object as a BIT STRING.

                :Parameters:
                  value : byte string or DER object
                    The initial, packed bit string.
                    If not specified, the bit string is empty.
                  implicit : integer
                    The IMPLICIT tag to use for the encoded object.
                    It overrides the universal tag for OCTET STRING (3).
                  explicit : integer
                    The EXPLICIT tag to use for the encoded object.
        
        """
    def encode(self):
        """
        Return the DER BIT STRING, fully encoded as a
                binary string.
        """
    def decode(self, der_encoded, strict=False):
        """
        Decode a complete DER BIT STRING, and re-initializes this
                object with it.

                Args:
                    der_encoded (byte string): a complete DER BIT STRING.
                    strict (boolean):
                        Whether decoding must check for strict DER compliancy.

                Raises:
                    ValueError: in case of parsing errors.
        
        """
    def _decodeFromStream(self, s, strict):
        """
        Decode a complete DER BIT STRING DER from a file.
        """
def DerSetOf(DerObject):
    """
    Class to model a DER SET OF.

        An example of encoding is:

        >>> from Crypto.Util.asn1 import DerBitString
        >>> from binascii import hexlify, unhexlify
        >>> so_der = DerSetOf([4,5])
        >>> so_der.add(6)
        >>> print hexlify(so_der.encode())

        which will show ``3109020104020105020106``, the DER encoding
        of a SET OF with items 4,5, and 6.

        For decoding:

        >>> s = unhexlify(b'3109020104020105020106')
        >>> try:
        >>>   so_der = DerSetOf()
        >>>   so_der.decode(s)
        >>>   print [x for x in so_der]
        >>> except ValueError:
        >>>   print "Not a valid DER SET OF"

        the output will be ``[4, 5, 6]``.
    
    """
    def __init__(self, startSet=None, implicit=None):
        """
        Initialize the DER object as a SET OF.

                :Parameters:
                  startSet : container
                    The initial set of integers or DER encoded objects.
                  implicit : integer
                    The IMPLICIT tag to use for the encoded object.
                    It overrides the universal tag for SET OF (17).
        
        """
    def __getitem__(self, n):
        """
        Add an element to the set.

                Args:
                    elem (byte string or integer):
                      An element of the same type of objects already in the set.
                      It can be an integer or a DER encoded object.
        
        """
    def decode(self, der_encoded, strict=False):
        """
        Decode a complete SET OF DER element, and re-initializes this
                object with it.

                DER INTEGERs are decoded into Python integers. Any other DER
                element is left undecoded; its validity is not checked.

                Args:
                    der_encoded (byte string): a complete DER BIT SET OF.
                    strict (boolean):
                        Whether decoding must check for strict DER compliancy.

                Raises:
                    ValueError: in case of parsing errors.
        
        """
    def _decodeFromStream(self, s, strict):
        """
        Decode a complete DER SET OF from a file.
        """
    def encode(self):
        """
        Return this SET OF DER element, fully encoded as a
                binary string.
        
        """
