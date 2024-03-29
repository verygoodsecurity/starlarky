load("@stdlib//builtins","builtins")

load("@stdlib//base64", b16encode="b16encode")
load("@stdlib//functools", partial="partial")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//jopenssl", _JOpenSSL="jopenssl")
load("@stdlib//json", "json")


__all__ = [
    "FILETYPE_PEM",
    "FILETYPE_ASN1",
    "FILETYPE_TEXT",
    "TYPE_RSA",
    "TYPE_DSA",
    "Error",
    "PKey",
    "get_elliptic_curves",
    "get_elliptic_curve",
    "X509Name",
    "X509Extension",
    "X509Req",
    "X509",
    "X509StoreFlags",
    "X509Store",
    "X509StoreContextError",
    "X509StoreContext",
    "load_certificate",
    "dump_certificate",
    "dump_publickey",
    "dump_privatekey",
    "Revoked",
    "CRL",
    "PKCS7",
    "PKCS12",
    "NetscapeSPKI",
    "load_publickey",
    "load_privatekey",
    "dump_certificate_request",
    "load_certificate_request",
    "sign",
    "verify",
    "dump_crl",
    "load_crl",
    "load_pkcs7_data",
    "load_pkcs12",
]

FILETYPE_PEM = 1
FILETYPE_ASN1 = 2

# TODO This was an API mistake.  OpenSSL has no such constant.
FILETYPE_TEXT = pow(2, 16) - 1

# These magic constants are from openssl
# https://github.com/openssl/openssl/blob/master/include/openssl/obj_mac.h
# https://github.com/openssl/openssl/blob/master/include/openssl/obj_mac.h#L543
TYPE_RSA = 6
# https://github.com/openssl/openssl/blob/master/include/openssl/obj_mac.h#L136
TYPE_DSA = 116
# https://github.com/openssl/openssl/blob/master/include/openssl/obj_mac.h#L621
TYPE_DH = 28
# https://github.com/openssl/openssl/blob/master/include/openssl/obj_mac.h#L178
TYPE_EC = 408

# def Error():
#     """
#     An error occurred in an `OpenSSL.crypto` API.
#     """
#     return self
#
#
# _raise_current_error = partial(_exception_from_error_queue, Error)
# _openssl_assert = _make_assert(Error)


def _get_backend():
    """
    Importing the backend from cryptography has the side effect of activating
    the osrandom engine. This mutates the global state of OpenSSL in the
    process and causes issues for various programs that use subinterpreters or
    embed Python. By putting the import in this function we can avoid
    triggering this side effect unless _get_backend is called.
    """

    return "NONE"


def _untested_error(where):
    """
    An OpenSSL API failed somehow.  Additionally, the failure which was
    encountered isn't one that's exercised by the test suite so future behavior
    of pyOpenSSL is now somewhat less predictable.
    """
    fail(" RuntimeError(\"Unknown %s failure\" % (where,))")


# def _new_mem_buf(buffer=None):
#     """
#     Allocate a new OpenSSL memory BIO.
#
#     Arrange for the garbage collector to clean it up automatically.
#
#     :param buffer: None or some bytes to use to put into the BIO so that they
#         can be read out.
#     """
#     if buffer == None:
#         bio = _lib.BIO_new(_lib.BIO_s_mem())
#         free = _lib.BIO_free
#     else:
#         data = _ffi.new("char[]", buffer)
#         bio = _lib.BIO_new_mem_buf(data, len(buffer))
#
#         # Keep the memory alive as long as the bio is alive!
#         def free(bio, ref=data):
#             return _lib.BIO_free(bio)
#
#     _openssl_assert(bio != _ffi.NULL)
#
#     bio = _ffi.gc(bio, free)
#     return bio
#
#
# def _bio_to_string(bio):
#     """
#     Copy the contents of an OpenSSL BIO object into a Python byte string.
#     """
#     result_buffer = _ffi.new("char**")
#     buffer_length = _lib.BIO_get_mem_data(bio, result_buffer)
#     return _ffi.buffer(result_buffer[0], buffer_length)[:]


def _set_asn1_time(boundary, when):
    """
    The the time value of an ASN1 time object.

    @param boundary: An ASN1_TIME pointer (or an object safely
        castable to that type) which will have its value set.
    @param when: A string representation of the desired time value.

    @raise TypeError: If C{when} is not a L{bytes} string.
    @raise ValueError: If C{when} does not represent a time in the required
        format.
    @raise RuntimeError: If the time value cannot be set for some other
        (unspecified) reason.
    """
    if not types.is_bytelike(when):
        fail('TypeError: when must be a byte string')

    # set_result = _lib.ASN1_TIME_set_string(boundary, when)


def _get_asn1_time(timestamp):
    """
    Retrieve the time value of an ASN1 time object.

    @param timestamp: An ASN1_GENERALIZEDTIME* (or an object safely castable to
        that type) from which the time value will be retrieved.

    @return: The time value from C{timestamp} as a L{bytes} string in a certain
        format.  Or C{None} if the object contains no time value.
    """
    fail("TODO")
    # string_timestamp = _ffi.cast("ASN1_STRING*", timestamp)
    # if _lib.ASN1_STRING_length(string_timestamp) == 0:
    #     return None
    # elif (
    #     _lib.ASN1_STRING_type(string_timestamp) == _lib.V_ASN1_GENERALIZEDTIME
    # ):
    #     return _ffi.string(_lib.ASN1_STRING_data(string_timestamp))
    # else:
    #     generalized_timestamp = _ffi.new("ASN1_GENERALIZEDTIME**")
    #     _lib.ASN1_TIME_to_generalizedtime(timestamp, generalized_timestamp)
    #     if generalized_timestamp[0] == _ffi.NULL:
    #         # This may happen:
    #         #   - if timestamp was not an ASN1_TIME
    #         #   - if allocating memory for the ASN1_GENERALIZEDTIME failed
    #         #   - if a copy of the time data from timestamp cannot be made for
    #         #     the newly allocated ASN1_GENERALIZEDTIME
    #         #
    #         # These are difficult to test.  cffi enforces the ASN1_TIME type.
    #         # Memory allocation failures are a pain to trigger
    #         # deterministically.
    #         _untested_error("ASN1_TIME_to_generalizedtime")
    #     else:
    #         string_timestamp = _ffi.cast(
    #             "ASN1_STRING*", generalized_timestamp[0]
    #         )
    #         string_data = _lib.ASN1_STRING_data(string_timestamp)
    #         string_result = _ffi.string(string_data)
    #         _lib.ASN1_GENERALIZEDTIME_free(generalized_timestamp[0])
    #         return string_result


def _X509NameInvalidator():
    self = larky.mutablestruct(__class__='_X509NameInvalidator')
    def __init__():
        self._names = []
        return self
    self = __init__()

    def add(name):
        self._names.append(name)
    self.add = add

    def clear():
        self._names = []
    self.clear = clear
    return self


def PKey(pkey=None):
    """
    A class representing an DSA or RSA public key or key pair.
    """

    self = larky.mutablestruct(__class__='PKey')
    self._only_public = False
    self._initialized = False

    def __init__(pkey):
        self._pkey = pkey.pkey
        self._type = pkey.key_type
        self._bits = pkey.bits
        self._initialized = True
        return self
    self = __init__(pkey)

    def generate_key(type, bits):
        """
        Generate a key pair of the given type, with the given number of bits.

        This generates a key "into" the this object.

        :param type: The key type.
        :type type: :py:data:`TYPE_RSA` or :py:data:`TYPE_DSA`
        :param bits: The number of bits.
        :type bits: :py:data:`int` ``>= 0``
        :raises TypeError: If :py:data:`type` or :py:data:`bits` isn't
            of the appropriate type.
        :raises ValueError: If the number of bits isn't an integer of
            the appropriate size.
        :return: ``None``
        """
        if not types.is_int(type):
            fail(" TypeError(\"type must be an integer\")")

        if not types.is_int(bits):
            fail(" TypeError(\"bits must be an integer\")")

        if type == TYPE_RSA:
            if bits <= 0:
                fail(" ValueError(\"Invalid number of bits\")")

            # # TODO Check error return
            # exponent = _lib.BN_new()
            # exponent = _ffi.gc(exponent, _lib.BN_free)
            # _lib.BN_set_word(exponent, _lib.RSA_F4)
            #
            # rsa = _lib.RSA_new()
            #
            # result = _lib.RSA_generate_key_ex(rsa, bits, exponent, _ffi.NULL)
            # _openssl_assert(result == 1)
            #
            # result = _lib.EVP_PKEY_assign_RSA(self._pkey, rsa)
            # _openssl_assert(result == 1)

        elif type == TYPE_DSA:
            pass
            # dsa = _lib.DSA_new()
            # _openssl_assert(dsa != _ffi.NULL)
            #
            # dsa = _ffi.gc(dsa, _lib.DSA_free)
            # res = _lib.DSA_generate_parameters_ex(
            #     dsa, bits, _ffi.NULL, 0, _ffi.NULL, _ffi.NULL, _ffi.NULL
            # )
            # _openssl_assert(res == 1)
            #
            # _openssl_assert(_lib.DSA_generate_key(dsa) == 1)
            # _openssl_assert(_lib.EVP_PKEY_set1_DSA(self._pkey, dsa) == 1)
        else:
            fail(" Error(\"No such key type\")")

        self._initialized = True
    self.generate_key = generate_key

    def check():
        """
        Check the consistency of an RSA private key.

        This is the Python equivalent of OpenSSL's ``RSA_check_key``.

        :return: ``True`` if key is consistent.

        :raise OpenSSL.crypto.Error: if the key is inconsistent.

        :raise TypeError: if the key is of a type which cannot be checked.
            Only RSA keys can currently be checked.
        """
        if self._only_public:
            fail(" TypeError(\"public key only\")")

        # if _lib.EVP_PKEY_type(self.type()) != _lib.EVP_PKEY_RSA:
        #     fail(" TypeError(\"key type unsupported\")")
        #
        # rsa = _lib.EVP_PKEY_get1_RSA(self._pkey)
        # rsa = _ffi.gc(rsa, _lib.RSA_free)
        # result = _lib.RSA_check_key(rsa)
        # if result == 1:
        #     return True
        # _raise_current_error()
    self.check = check

    def type():
        """
        Returns the type of the key

        :return: The type of the key.
        """
        return self._type
    self.type = type

    def bits():
        """
        Returns the number of bits of the key

        :return: The number of bits of the key.
        """
        return self._bits
    self.bits = bits
    return self

# def _EllipticCurve(lib, nid, name):
#     """
#     A representation of a supported elliptic curve.
#
#     @cvar _curves: :py:obj:`None` until an attempt is made to load the curves.
#         Thereafter, a :py:type:`set` containing :py:type:`_EllipticCurve`
#         instances each of which represents one curve supported by the system.
#     @type _curves: :py:type:`NoneType` or :py:type:`set`
#     """
#
#     _curves = None
#
#     if not _PY2:
#         # This only necessary on Python 3.  Moreover, it is broken on Python 2.
#         def __ne__(other):
#             """
#             Implement cooperation with the right-hand side argument of ``!=``.
#
#             Python 3 seems to have dropped this cooperation in this very narrow
#             circumstance.
#             """
#             if isinstance(other, _EllipticCurve):
#                 return super(_EllipticCurve, self).__ne__(other)
#             return NotImplemented
#         self.__ne__ = __ne__
#
#     @classmethod
#     def _load_elliptic_curves(cls, lib):
#         """
#         Get the curves supported by OpenSSL.
#
#         :param lib: The OpenSSL library binding object.
#
#         :return: A :py:type:`set` of ``cls`` instances giving the names of the
#             elliptic curves the underlying library supports.
#         """
#         num_curves = lib.EC_get_builtin_curves(_ffi.NULL, 0)
#         builtin_curves = _ffi.new("EC_builtin_curve[]", num_curves)
#         # The return value on this call should be num_curves again.  We
#         # could check it to make sure but if it *isn't* then.. what could
#         # we do? Abort the whole process, I suppose...?  -exarkun
#         lib.EC_get_builtin_curves(builtin_curves, num_curves)
#         return set([cls.from_nid(lib, c.nid) for c in builtin_curves])
#     self._load_elliptic_curves = _load_elliptic_curves
#
#     @classmethod
#     def _get_elliptic_curves(cls, lib):
#         """
#         Get, cache, and return the curves supported by OpenSSL.
#
#         :param lib: The OpenSSL library binding object.
#
#         :return: A :py:type:`set` of ``cls`` instances giving the names of the
#             elliptic curves the underlying library supports.
#         """
#         if cls._curves == None:
#             cls._curves = cls._load_elliptic_curves(lib)
#         return cls._curves
#     self._get_elliptic_curves = _get_elliptic_curves
#
#     @classmethod
#     def from_nid(cls, lib, nid):
#         """
#         Instantiate a new :py:class:`_EllipticCurve` associated with the given
#         OpenSSL NID.
#
#         :param lib: The OpenSSL library binding object.
#
#         :param nid: The OpenSSL NID the resulting curve object will represent.
#             This must be a curve NID (and not, for example, a hash NID) or
#             subsequent operations will fail in unpredictable ways.
#         :type nid: :py:class:`int`
#
#         :return: The curve object.
#         """
#         return cls(lib, nid, _ffi.string(lib.OBJ_nid2sn(nid)).decode("ascii"))
#     self.from_nid = from_nid
#
#     def __init__(lib, nid, name):
#         """
#         :param _lib: The :py:mod:`cryptography` binding instance used to
#             interface with OpenSSL.
#
#         :param _nid: The OpenSSL NID identifying the curve this object
#             represents.
#         :type _nid: :py:class:`int`
#
#         :param name: The OpenSSL short name identifying the curve this object
#             represents.
#         :type name: :py:class:`unicode`
#         """
#         self._lib = lib
#         self._nid = nid
#         self.name = name
#     self = __init__(lib, nid, name)
#
#     def __repr__():
#         return "<Curve %r>" % (self.name,)
#     self.__repr__ = __repr__
#
#     def _to_EC_KEY():
#         """
#         Create a new OpenSSL EC_KEY structure initialized to use this curve.
#
#         The structure is automatically garbage collected when the Python object
#         is garbage collected.
#         """
#         key = self._lib.EC_KEY_new_by_curve_name(self._nid)
#         return _ffi.gc(key, _lib.EC_KEY_free)
#     self._to_EC_KEY = _to_EC_KEY
#     return self
#
#
# def get_elliptic_curves():
#     """
#     Return a set of objects representing the elliptic curves supported in the
#     OpenSSL build in use.
#
#     The curve objects have a :py:class:`unicode` ``name`` attribute by which
#     they identify themselves.
#
#     The curve objects are useful as values for the argument accepted by
#     :py:meth:`Context.set_tmp_ecdh` to specify which elliptical curve should be
#     used for ECDHE key exchange.
#     """
#     return _EllipticCurve._get_elliptic_curves(_lib)
#
#
# def get_elliptic_curve(name):
#     """
#     Return a single curve object selected by name.
#
#     See :py:func:`get_elliptic_curves` for information about curve objects.
#
#     :param name: The OpenSSL short name identifying the curve object to
#         retrieve.
#     :type name: :py:class:`unicode`
#
#     If the named curve is not supported then :py:class:`ValueError` is raised.
#     """
#     for curve in get_elliptic_curves():
#         if curve.name == name:
#             return curve
#     fail(" ValueError(\"unknown curve name\", name)")


def X509Name(name=None):
    """
    An X.509 Distinguished Name.

    :ivar countryName: The country of the entity.
    :ivar C: Alias for  :py:attr:`countryName`.

    :ivar stateOrProvinceName: The state or province of the entity.
    :ivar ST: Alias for :py:attr:`stateOrProvinceName`.

    :ivar localityName: The locality of the entity.
    :ivar L: Alias for :py:attr:`localityName`.

    :ivar organizationName: The organization name of the entity.
    :ivar O: Alias for :py:attr:`organizationName`.

    :ivar organizationalUnitName: The organizational unit of the entity.
    :ivar OU: Alias for :py:attr:`organizationalUnitName`

    :ivar commonName: The common name of the entity.
    :ivar CN: Alias for :py:attr:`commonName`.

    :ivar emailAddress: The e-mail address of the entity.
    """
    self = larky.mutablestruct(__class__='X509Name')

    def __init__(name):
        """
        Create a new X509Name, copying the given X509Name instance.

        :param name: The name to copy.
        :type name: :py:class:`X509Name`
        """
        self._name = name if not name else ""
        return self
    self = __init__(name)
    #
    # def __setattr__(name, value):
    #     if name.startswith("_"):
    #         return setattr(self, name, value)
    #
    #     # Note: we really do not want str subclasses here, so we do not use
    #     # isinstance.
    #     if not types.is_string(name):
    #         fail("TypeError: attribute name must be string, not '%s'" % type(value))
    #
    #     nid = None # _lib.OBJ_txt2nid(_byte_string(name))
    #     if nid == None: # _lib.NID_undef:
    #         fail('AttributeError: No such attribute')
    #
    #     # If there's an old entry for this NID, remove it
    #     # for i in range(_lib.X509_NAME_entry_count(self._name)):
    #     #     ent = _lib.X509_NAME_get_entry(self._name, i)
    #     #     ent_obj = _lib.X509_NAME_ENTRY_get_object(ent)
    #     #     ent_nid = _lib.OBJ_obj2nid(ent_obj)
    #     #     if nid == ent_nid:
    #     #         ent = _lib.X509_NAME_delete_entry(self._name, i)
    #     #         _lib.X509_NAME_ENTRY_free(ent)
    #     #         break
    #
    #     if types.is_string(value):
    #         value = codecs.encode(value, encoding="utf-8")
    #
    #     # add_result = _lib.X509_NAME_add_entry_by_NID(
    #     #     self._name, nid, _lib.MBSTRING_UTF8, value, -1, -1, 0
    #     # )
    #     # if not add_result:
    #     #     _raise_current_error()
    # self.__setattr__ = __setattr__
    #
    # def __getattr__(name):
    #     """
    #     Find attribute. An X509Name object has the following attributes:
    #     countryName (alias C), stateOrProvince (alias ST), locality (alias L),
    #     organization (alias O), organizationalUnit (alias OU), commonName
    #     (alias CN) and more...
    #     """
    #     nid = None # _lib.OBJ_txt2nid(_byte_string(name))
    #     if nid == None: # _lib.NID_undef:
    #         return getattr(self, name)
    #
    #     entry_index = -1 # _lib.X509_NAME_get_index_by_NID(self._name, nid, -1)
    #     if entry_index == -1:
    #         return None
    #
    #     fail("TOOD")
    #     result = None
    #     # entry = _lib.X509_NAME_get_entry(self._name, entry_index)
    #     # data = _lib.X509_NAME_ENTRY_get_data(entry)
    #     #
    #     # result_buffer = _ffi.new("unsigned char**")
    #     # data_length = _lib.ASN1_STRING_to_UTF8(result_buffer, data)
    #     # result = _ffi.buffer(result_buffer[0], data_length)[:].decode(
    #     #         "utf-8"
    #     #     )
    #     return result
    # self.__getattr__ = __getattr__
    #
    # def _cmp(op):
    #     def f(other):
    #         if not types.is_instance(other, X509Name):
    #             fail("not implemented")
    #         result = -1 # _lib.X509_NAME_cmp(self._name, other._name)
    #         return op(result, 0)
    #     self.f = f
    #
    #     return f
    # self._cmp = _cmp
    #
    # def __repr__():
    #     """
    #     String representation of an X509Name
    #     """
    #     return "<X509Name object '%s'>" % (
    #         self
    #     )
    # self.__repr__ = __repr__
    #
    # def hash():
    #     """
    #     Return an integer representation of the first four bytes of the
    #     MD5 digest of the DER representation of the name.
    #
    #     This is the Python equivalent of OpenSSL's ``X509_NAME_hash``.
    #
    #     :return: The (integer) hash of this name.
    #     :rtype: :py:class:`int`
    #     """
    #     fail("TODO")
    #     # return _lib.X509_NAME_hash(self._name)
    # self.hash = hash
    #
    # def der():
    #     """
    #     Return the DER encoding of this name.
    #
    #     :return: The DER encoded form of this name.
    #     :rtype: :py:class:`bytes`
    #     """
    #     fail("TODO")
    #     # return string_result
    # self.der = der
    #
    # def get_components():
    #     """
    #     Returns the components of this name, as a sequence of 2-tuples.
    #
    #     :return: The components of this name.
    #     :rtype: :py:class:`list` of ``name, value`` tuples.
    #     """
    #     result = []
    #     # for i in range(_lib.X509_NAME_entry_count(self._name)):
    #     #     ent = _lib.X509_NAME_get_entry(self._name, i)
    #     #
    #     #     fname = _lib.X509_NAME_ENTRY_get_object(ent)
    #     #     fval = _lib.X509_NAME_ENTRY_get_data(ent)
    #     #
    #     #     nid = _lib.OBJ_obj2nid(fname)
    #     #     name = _lib.OBJ_nid2sn(nid)
    #     #
    #     #     # ffi.string does not handle strings containing NULL bytes
    #     #     # (which may have been generated by old, broken software)
    #     #     value = _ffi.buffer(
    #     #         _lib.ASN1_STRING_data(fval), _lib.ASN1_STRING_length(fval)
    #     #     )[:]
    #     #     result.append((_ffi.string(name), value))
    #
    #     return result
    # self.get_components = get_components
    return self


def X509Extension(type_name, critical, value, subject=None, issuer=None):
    """
    An X.509 v3 certificate extension.
    """
    return
#     def __init__(type_name, critical, value, subject, issuer):
#         """
#         Initializes an X509 extension.
#
#         :param type_name: The name of the type of extension_ to create.
#         :type type_name: :py:data:`bytes`
#
#         :param bool critical: A flag indicating whether this is a critical
#             extension.
#
#         :param value: The value of the extension.
#         :type value: :py:data:`bytes`
#
#         :param subject: Optional X509 certificate to use as subject.
#         :type subject: :py:class:`X509`
#
#         :param issuer: Optional X509 certificate to use as issuer.
#         :type issuer: :py:class:`X509`
#
#         .. _extension: https://www.openssl.org/docs/manmaster/man5/
#             x509v3_config.html#STANDARD-EXTENSIONS
#         """
#         ctx = _ffi.new("X509V3_CTX*")
#
#         # A context is necessary for any extension which uses the r2i
#         # conversion method.  That is, X509V3_EXT_nconf may segfault if passed
#         # a NULL ctx. Start off by initializing most of the fields to NULL.
#         _lib.X509V3_set_ctx(ctx, _ffi.NULL, _ffi.NULL, _ffi.NULL, _ffi.NULL, 0)
#
#         # We have no configuration database - but perhaps we should (some
#         # extensions may require it).
#         _lib.X509V3_set_ctx_nodb(ctx)
#
#         # Initialize the subject and issuer, if appropriate.  ctx is a local,
#         # and as far as I can tell none of the X509V3_* APIs invoked here steal
#         # any references, so no need to mess with reference counts or
#         # duplicates.
#         if issuer != None:
#             if not isinstance(issuer, X509):
#                 fail(" TypeError(\"issuer must be an X509 instance\")")
#             ctx.issuer_cert = issuer._x509
#         if subject != None:
#             if not isinstance(subject, X509):
#                 fail(" TypeError(\"subject must be an X509 instance\")")
#             ctx.subject_cert = subject._x509
#
#         if critical:
#             # There are other OpenSSL APIs which would let us pass in critical
#             # separately, but they're harder to use, and since value is already
#             # a pile of crappy junk smuggling a ton of utterly important
#             # structured data, what's the point of trying to avoid nasty stuff
#             # with strings? (However, X509V3_EXT_i2d in particular seems like
#             # it would be a better API to invoke.  I do not know where to get
#             # the ext_struc it desires for its last parameter, though.)
#             value = bytes([0x63, 0x72, 0x69, 0x74, 0x69, 0x63, 0x61, 0x6c, 0x2c]) + value
#
#         extension = _lib.X509V3_EXT_nconf(_ffi.NULL, ctx, type_name, value)
#         if extension == _ffi.NULL:
#             _raise_current_error()
#         self._extension = _ffi.gc(extension, _lib.X509_EXTENSION_free)
#     self = __init__(type_name, critical, value, subject, issuer)
#
#     @property
#     def _nid():
#         return _lib.OBJ_obj2nid(
#             _lib.X509_EXTENSION_get_object(self._extension)
#         )
#     self._nid = _nid
#
#     _prefixes = {
#         _lib.GEN_EMAIL: "email",
#         _lib.GEN_DNS: "DNS",
#         _lib.GEN_URI: "URI",
#     }
#
#     def _subjectAltNameString():
#         names = _ffi.cast(
#             "GENERAL_NAMES*", _lib.X509V3_EXT_d2i(self._extension)
#         )
#
#         names = _ffi.gc(names, _lib.GENERAL_NAMES_free)
#         parts = []
#         for i in range(_lib.sk_GENERAL_NAME_num(names)):
#             name = _lib.sk_GENERAL_NAME_value(names, i)
#             try:
#                 label = self._prefixes[name.type]
#             except KeyError:
#                 bio = _new_mem_buf()
#                 _lib.GENERAL_NAME_print(bio, name)
#                 parts.append(_native(_bio_to_string(bio)))
#             else:
#                 value = _native(
#                     _ffi.buffer(name.d.ia5.data, name.d.ia5.length)[:]
#                 )
#                 parts.append(label + ":" + value)
#         return ", ".join(parts)
#     self._subjectAltNameString = _subjectAltNameString
#
#     def __str__():
#         """
#         :return: a nice text representation of the extension
#         """
#         if _lib.NID_subject_alt_name == self._nid:
#             return self._subjectAltNameString()
#
#         bio = _new_mem_buf()
#         print_result = _lib.X509V3_EXT_print(bio, self._extension, 0, 0)
#         _openssl_assert(print_result != 0)
#
#         return _native(_bio_to_string(bio))
#     self.__str__ = __str__
#
#     def get_critical():
#         """
#         Returns the critical field of this X.509 extension.
#
#         :return: The critical field.
#         """
#         return _lib.X509_EXTENSION_get_critical(self._extension)
#     self.get_critical = get_critical
#
#     def get_short_name():
#         """
#         Returns the short type name of this X.509 extension.
#
#         The result is a byte string such as :py:const:`b"basicConstraints"`.
#
#         :return: The short type name.
#         :rtype: :py:data:`bytes`
#
#         .. versionadded:: 0.12
#         """
#         obj = _lib.X509_EXTENSION_get_object(self._extension)
#         nid = _lib.OBJ_obj2nid(obj)
#         return _ffi.string(_lib.OBJ_nid2sn(nid))
#     self.get_short_name = get_short_name
#
#     def get_data():
#         """
#         Returns the data of the X509 extension, encoded as ASN.1.
#
#         :return: The ASN.1 encoded data of this X509 extension.
#         :rtype: :py:data:`bytes`
#
#         .. versionadded:: 0.12
#         """
#         octet_result = _lib.X509_EXTENSION_get_data(self._extension)
#         string_result = _ffi.cast("ASN1_STRING*", octet_result)
#         char_result = _lib.ASN1_STRING_data(string_result)
#         result_length = _lib.ASN1_STRING_length(string_result)
#         return _ffi.buffer(char_result, result_length)[:]
#     self.get_data = get_data
#     return self
#
#
# def X509Req():
#     """
#     An X.509 certificate signing requests.
#     """
#
#     def __init__():
#         req = _lib.X509_REQ_new()
#         self._req = _ffi.gc(req, _lib.X509_REQ_free)
#         # Default to version 0.
#         self.set_version(0)
#     self = __init__()
#
#     def to_cryptography():
#         """
#         Export as a ``cryptography`` certificate signing request.
#
#         :rtype: ``cryptography.x509.CertificateSigningRequest``
#
#         .. versionadded:: 17.1.0
#         """
#         load("@vendor//cryptography/hazmat/backends/openssl/x509", _CertificateSigningRequest="_CertificateSigningRequest")
#
#         backend = _get_backend()
#         return _CertificateSigningRequest(backend, self._req)
#     self.to_cryptography = to_cryptography
#
#     @classmethod
#     def from_cryptography(cls, crypto_req):
#         """
#         Construct based on a ``cryptography`` *crypto_req*.
#
#         :param crypto_req: A ``cryptography`` X.509 certificate signing request
#         :type crypto_req: ``cryptography.x509.CertificateSigningRequest``
#
#         :rtype: X509Req
#
#         .. versionadded:: 17.1.0
#         """
#         if not isinstance(crypto_req, x509.CertificateSigningRequest):
#             fail(" TypeError(\"Must be a certificate signing request\")")
#
#         req = cls()
#         req._req = crypto_req._x509_req
#         return req
#     self.from_cryptography = from_cryptography
#
#     def set_pubkey(pkey):
#         """
#         Set the public key of the certificate signing request.
#
#         :param pkey: The public key to use.
#         :type pkey: :py:class:`PKey`
#
#         :return: ``None``
#         """
#         set_result = _lib.X509_REQ_set_pubkey(self._req, pkey._pkey)
#         _openssl_assert(set_result == 1)
#     self.set_pubkey = set_pubkey
#
#     def get_pubkey():
#         """
#         Get the public key of the certificate signing request.
#
#         :return: The public key.
#         :rtype: :py:class:`PKey`
#         """
#         pkey = PKey.__new__(PKey)
#         pkey._pkey = _lib.X509_REQ_get_pubkey(self._req)
#         _openssl_assert(pkey._pkey != _ffi.NULL)
#         pkey._pkey = _ffi.gc(pkey._pkey, _lib.EVP_PKEY_free)
#         pkey._only_public = True
#         return pkey
#     self.get_pubkey = get_pubkey
#
#     def set_version(version):
#         """
#         Set the version subfield (RFC 2459, section 4.1.2.1) of the certificate
#         request.
#
#         :param int version: The version number.
#         :return: ``None``
#         """
#         set_result = _lib.X509_REQ_set_version(self._req, version)
#         _openssl_assert(set_result == 1)
#     self.set_version = set_version
#
#     def get_version():
#         """
#         Get the version subfield (RFC 2459, section 4.1.2.1) of the certificate
#         request.
#
#         :return: The value of the version subfield.
#         :rtype: :py:class:`int`
#         """
#         return _lib.X509_REQ_get_version(self._req)
#     self.get_version = get_version
#
#     def get_subject():
#         """
#         Return the subject of this certificate signing request.
#
#         This creates a new :class:`X509Name` that wraps the underlying subject
#         name field on the certificate signing request. Modifying it will modify
#         the underlying signing request, and will have the effect of modifying
#         any other :class:`X509Name` that refers to this subject.
#
#         :return: The subject of this certificate signing request.
#         :rtype: :class:`X509Name`
#         """
#         name = X509Name.__new__(X509Name)
#         name._name = _lib.X509_REQ_get_subject_name(self._req)
#         _openssl_assert(name._name != _ffi.NULL)
#
#         # The name is owned by the X509Req structure.  As long as the X509Name
#         # Python object is alive, keep the X509Req Python object alive.
#         name._owner = self
#
#         return name
#     self.get_subject = get_subject
#
#     def add_extensions(extensions):
#         """
#         Add extensions to the certificate signing request.
#
#         :param extensions: The X.509 extensions to add.
#         :type extensions: iterable of :py:class:`X509Extension`
#         :return: ``None``
#         """
#         stack = _lib.sk_X509_EXTENSION_new_null()
#         _openssl_assert(stack != _ffi.NULL)
#
#         stack = _ffi.gc(stack, _lib.sk_X509_EXTENSION_free)
#
#         for ext in extensions:
#             if not isinstance(ext, X509Extension):
#                 fail(" ValueError(\"One of the elements is not an X509Extension\")")
#
#             # TODO push can fail (here and elsewhere)
#             _lib.sk_X509_EXTENSION_push(stack, ext._extension)
#
#         add_result = _lib.X509_REQ_add_extensions(self._req, stack)
#         _openssl_assert(add_result == 1)
#     self.add_extensions = add_extensions
#
#     def get_extensions():
#         """
#         Get X.509 extensions in the certificate signing request.
#
#         :return: The X.509 extensions in this request.
#         :rtype: :py:class:`list` of :py:class:`X509Extension` objects.
#
#         .. versionadded:: 0.15
#         """
#         exts = []
#         native_exts_obj = _lib.X509_REQ_get_extensions(self._req)
#         native_exts_obj = _ffi.gc(
#             native_exts_obj,
#             lambda x: _lib.sk_X509_EXTENSION_pop_free(
#                 x,
#                 _ffi.addressof(_lib._original_lib, "X509_EXTENSION_free"),
#             ),
#         )
#
#         for i in range(_lib.sk_X509_EXTENSION_num(native_exts_obj)):
#             ext = X509Extension.__new__(X509Extension)
#             extension = _lib.X509_EXTENSION_dup(
#                 _lib.sk_X509_EXTENSION_value(native_exts_obj, i)
#             )
#             ext._extension = _ffi.gc(extension, _lib.X509_EXTENSION_free)
#             exts.append(ext)
#         return exts
#     self.get_extensions = get_extensions
#
#     def sign(pkey, digest):
#         """
#         Sign the certificate signing request with this key and digest type.
#
#         :param pkey: The key pair to sign with.
#         :type pkey: :py:class:`PKey`
#         :param digest: The name of the message digest to use for the signature,
#             e.g. :py:data:`b"sha256"`.
#         :type digest: :py:class:`bytes`
#         :return: ``None``
#         """
#         if pkey._only_public:
#             fail(" ValueError(\"Key has only public part\")")
#
#         if not pkey._initialized:
#             fail(" ValueError(\"Key is uninitialized\")")
#
#         digest_obj = _lib.EVP_get_digestbyname(_byte_string(digest))
#         if digest_obj == _ffi.NULL:
#             fail(" ValueError(\"No such digest method\")")
#
#         sign_result = _lib.X509_REQ_sign(self._req, pkey._pkey, digest_obj)
#         _openssl_assert(sign_result > 0)
#     self.sign = sign
#
#     def verify(pkey):
#         """
#         Verifies the signature on this certificate signing request.
#
#         :param PKey key: A public key.
#
#         :return: ``True`` if the signature is correct.
#         :rtype: bool
#
#         :raises OpenSSL.crypto.Error: If the signature is invalid or there is a
#             problem verifying the signature.
#         """
#         if not isinstance(pkey, PKey):
#             fail(" TypeError(\"pkey must be a PKey instance\")")
#
#         result = _lib.X509_REQ_verify(self._req, pkey._pkey)
#         if result <= 0:
#             _raise_current_error()
#
#         return result
#     self.verify = verify
#     return self
#
#
def X509():
    """
    An X.509 certificate.
    """

    self = larky.mutablestruct(__class__='X509')

    def __init__():
        self._x509 = {}

        self._issuer_invalidator = _X509NameInvalidator()
        self._subject_invalidator = _X509NameInvalidator()
        return self
    self = __init__()

    def _from_raw_x509_ptr(x509):
        cert = __init__()
        cert._x509 = x509
        cert._issuer_invalidator = _X509NameInvalidator()
        cert._subject_invalidator = _X509NameInvalidator()
        return cert
    self._from_raw_x509_ptr = _from_raw_x509_ptr

    def to_cryptography():
        """
        Export as a ``cryptography`` certificate.

        :rtype: ``cryptography.x509.Certificate``

        .. versionadded:: 17.1.0
        """
        fail('not implemented in larky, file issue ' +
             '@ github.com/verygoodsecurity/starlarky')
    self.to_cryptography = to_cryptography

    def from_cryptography(crypto_cert):
        """
        Construct based on a ``cryptography`` *crypto_cert*.

        :param crypto_key: A ``cryptography`` X.509 certificate.
        :type crypto_key: ``cryptography.x509.Certificate``

        :rtype: X509

        .. versionadded:: 17.1.0
        """
        fail('not implemented in larky, file issue ' +
             '@ github.com/verygoodsecurity/starlarky')
    self.from_cryptography = from_cryptography

    def set_version(version):
        """
        Set the version number of the certificate. Note that the
        version value is zero-based, eg. a value of 0 is V1.

        :param version: The version number of the certificate.
        :type version: :py:class:`int`

        :return: ``None``
        """
        if not types.is_int(version):
            fail('TypeError: version must be an integer')

        fail("TODO")
    self.set_version = set_version

    def get_version():
        """
        Return the version number of the certificate.

        :return: The version number of the certificate.
        :rtype: :py:class:`int`
        """
        fail("TODO")
    self.get_version = get_version

    def get_pubkey():
        """
        Get the public key of the certificate.

        :return: The public key.
        :rtype: :py:class:`PKey`
        """
        fail("TODO")
        pkey = PKey(self._req)
        pkey._only_public = True
        return pkey
    self.get_pubkey = get_pubkey

    def set_pubkey(pkey):
        """
        Set the public key of the certificate.

        :param pkey: The public key.
        :type pkey: :py:class:`PKey`

        :return: :py:data:`None`
        """
        if not types.is_instance(pkey, PKey):
            fail('TypeError: pkey must be a PKey instance')
        self._x509['pkey'] = pkey._pkey
    self.set_pubkey = set_pubkey

    def sign(pkey, digest):
        """
        Sign the certificate with this key and digest type.

        :param pkey: The key to sign with.
        :type pkey: :py:class:`PKey`

        :param digest: The name of the message digest to use.
        :type digest: :py:class:`bytes`

        :return: :py:data:`None`
        """
        if not types.is_instance(pkey, PKey):
            fail('TypeError: pkey must be a PKey instance')

        if pkey._only_public:
            fail('ValueError: Key only has public part')

        if not pkey._initialized:
            fail('ValueError: Key is uninitialized')

        if digest == None:
            fail('ValueError: No such digest method')
        self._signed = _JOpenSSL.OpenSSL.X509_sign(
            pkey._pkey,
            json.dumps({
                "gmtime_adj_notAfter": self._x509["gmtime_adj_notAfter"],
                "gmtime_adj_notBefore": self._x509["gmtime_adj_notBefore"],
                "issuer_name": ', '.join([
                    "%s=%s" % (k, getattr(self._x509["subject_name"], k))
                    for k in self._x509["issuer_name"].__dict__
                    if not k.startswith('_')
                ]),
                "subject_name": ', '.join([
                    "%s=%s" % (k, getattr(self._x509["subject_name"], k))
                    for k in self._x509["subject_name"].__dict__
                    if not k.startswith('_')
                ])
            }),
            digest
        )
        #sign_result = _lib.X509_sign(self._x509, pkey._pkey, evp_md)
        #_openssl_assert(sign_result > 0)
    self.sign = sign

    def get_signature_algorithm():
        """
        Return the signature algorithm used in the certificate.

        :return: The name of the algorithm.
        :rtype: :py:class:`bytes`

        :raises ValueError: If the signature algorithm is undefined.

        .. versionadded:: 0.13
        """
        #algor = _lib.X509_get0_tbs_sigalg(self._x509)
        #nid = _lib.OBJ_obj2nid(algor.algorithm)
        nid = None
        if nid == None:
            fail('ValueError: Undefined signature algorithm')
        fail("TODO")
        #return _ffi.string(_lib.OBJ_nid2ln(nid))
    self.get_signature_algorithm = get_signature_algorithm

    def digest(digest_name):
        """
        Return the digest of the X509 object.

        :param digest_name: The name of the digest algorithm to use.
        :type digest_name: :py:class:`bytes`

        :return: The digest of the object, formatted as
            :py:const:`b":"`-delimited hex pairs.
        :rtype: :py:class:`bytes`
        """
        digest = None
        # digest = _lib.EVP_get_digestbyname(_byte_string(digest_name))
        if digest == None:
            fail('ValueError: No such digest method')

        # result_buffer = _ffi.new("unsigned char[]", _lib.EVP_MAX_MD_SIZE)
        # result_length = _ffi.new("unsigned int[]", 1)
        # result_length[0] = len(result_buffer)
        #
        # digest_result = _lib.X509_digest(
        #     self._x509, digest, result_buffer, result_length
        # )
        # _openssl_assert(digest_result == 1)
        digest_result = ""
        return bytes(":", encoding="utf-8").join(
            [
                b16encode(ch).upper()
                for ch in digest_result
            ]
        )
    self.digest = digest

    def subject_name_hash():
        """
        Return the hash of the X509 subject.

        :return: The hash of the subject.
        :rtype: :py:class:`bytes`
        """
        fail("TODO")
        #return _lib.X509_subject_name_hash(self._x509)
    self.subject_name_hash = subject_name_hash

    def set_serial_number(serial):
        """
        Set the serial number of the certificate.

        :param serial: The new serial number.
        :type serial: :py:class:`int`

        :return: :py:data`None`
        """
        if not types.is_int(serial):
            fail('TypeError: serial must be an integer')

        hex_serial = hex(serial)[2:]
        if not types.is_bytelike(hex_serial):
            hex_serial = codecs.encode(hex_serial, encoding="ascii")
        fail("TODO")
        # bignum_serial = _ffi.new("BIGNUM**")
        #
        # # BN_hex2bn stores the result in &bignum.  Unless it doesn't feel like
        # # it.  If bignum is still NULL after this call, then the return value
        # # is actually the result.  I hope.  -exarkun
        # small_serial = _lib.BN_hex2bn(bignum_serial, hex_serial)
        #
        # if bignum_serial[0] == _ffi.NULL:
        #     set_result = _lib.ASN1_INTEGER_set(
        #         _lib.X509_get_serialNumber(self._x509), small_serial
        #     )
        #     if set_result:
        #         # TODO Not tested
        #         _raise_current_error()
        # else:
        #     asn1_serial = _lib.BN_to_ASN1_INTEGER(bignum_serial[0], _ffi.NULL)
        #     _lib.BN_free(bignum_serial[0])
        #     if asn1_serial == _ffi.NULL:
        #         # TODO Not tested
        #         _raise_current_error()
        #     asn1_serial = _ffi.gc(asn1_serial, _lib.ASN1_INTEGER_free)
        #     set_result = _lib.X509_set_serialNumber(self._x509, asn1_serial)
        #     _openssl_assert(set_result == 1)
    self.set_serial_number = set_serial_number

    def get_serial_number():
        """
        Return the serial number of this certificate.

        :return: The serial number.
        :rtype: int
        """
        # asn1_serial = _lib.X509_get_serialNumber(self._x509)
        # bignum_serial = _lib.ASN1_INTEGER_to_BN(asn1_serial, _ffi.NULL)
        # hex_serial = _lib.BN_bn2hex(bignum_serial)
        # hexstring_serial = _ffi.string(hex_serial)
        hexstring_serial = "0xc0ffee"
        serial = int(hexstring_serial, 16)
        fail("TODO")
        return serial

    self.get_serial_number = get_serial_number

    def gmtime_adj_notAfter(amount):
        """
        Adjust the time stamp on which the certificate stops being valid.

        :param int amount: The number of seconds by which to adjust the
            timestamp.
        :return: ``None``
        """
        if not types.is_int(amount):
            fail('TypeError: amount must be an integer')
        self._x509["gmtime_adj_notAfter"] = amount
    self.gmtime_adj_notAfter = gmtime_adj_notAfter

    def gmtime_adj_notBefore(amount):
        """
        Adjust the timestamp on which the certificate starts being valid.

        :param amount: The number of seconds by which to adjust the timestamp.
        :return: ``None``
        """
        if not types.is_int(amount):
            fail('TypeError: amount must be an integer')
        self._x509["gmtime_adj_notBefore"] = amount
    self.gmtime_adj_notBefore = gmtime_adj_notBefore

    def has_expired():
        """
        Check whether the certificate has expired.

        :return: ``True`` if the certificate has expired, ``False`` otherwise.
        :rtype: bool
        """
        fail("TODO")
        # time_string = _native(self.get_notAfter())
        # not_after = datetime.datetime.strptime(time_string, "%Y%m%d%H%M%SZ")
        #
        # return not_after < datetime.datetime.utcnow()
    self.has_expired = has_expired

    def _get_boundary_time(which):
        return _get_asn1_time(which(self._x509))
    self._get_boundary_time = _get_boundary_time

    def get_notBefore():
        """
        Get the timestamp at which the certificate starts being valid.

        The timestamp is formatted as an ASN.1 TIME::

            YYYYMMDDhhmmssZ

        :return: A timestamp string, or ``None`` if there is none.
        :rtype: bytes or NoneType
        """
        fail("TODO")
        #return self._get_boundary_time(_lib.X509_getm_notBefore)
    self.get_notBefore = get_notBefore

    def _set_boundary_time(which, when):
        return _set_asn1_time(which(self._x509), when)
    self._set_boundary_time = _set_boundary_time

    def set_notBefore(when):
        """
        Set the timestamp at which the certificate starts being valid.

        The timestamp is formatted as an ASN.1 TIME::

            YYYYMMDDhhmmssZ

        :param bytes when: A timestamp string.
        :return: ``None``
        """
        fail("TODO")
        #return self._set_boundary_time(_lib.X509_getm_notBefore, when)
    self.set_notBefore = set_notBefore

    def get_notAfter():
        """
        Get the timestamp at which the certificate stops being valid.

        The timestamp is formatted as an ASN.1 TIME::

            YYYYMMDDhhmmssZ

        :return: A timestamp string, or ``None`` if there is none.
        :rtype: bytes or NoneType
        """
        fail("TODO")
        #return self._get_boundary_time(_lib.X509_getm_notAfter)
    self.get_notAfter = get_notAfter

    def set_notAfter(when):
        """
        Set the timestamp at which the certificate stops being valid.

        The timestamp is formatted as an AS7N.1 TIME::

            YYYYMMDDhhmmssZ

        :param bytes when: A timestamp string.
        :return: ``None``
        """
        fail("TODO")
        #return self._set_boundary_time(_lib.X509_getm_notAfter, when)
    self.set_notAfter = set_notAfter

    def _get_name(key):
        if key not in self._x509:
            name = X509Name()
            self._x509[key] = name
        # # The name is owned by the X509 structure.  As long as the X509Name
        # # Python object is alive, keep the X509 Python object alive.
        # name._owner = self
        return self._x509[key]
    self._get_name = _get_name

    def _set_name(which, name):
        if not types.is_instance(name, X509Name):
            fail('TypeError: name must be an X509Name, got type: %s' % type(name))
        self._x509[which] = name
    self._set_name = _set_name

    def get_issuer():
        """
        Return the issuer of this certificate.

        This creates a new :class:`X509Name` that wraps the underlying issuer
        name field on the certificate. Modifying it will modify the underlying
        certificate, and will have the effect of modifying any other
        :class:`X509Name` that refers to this issuer.

        :return: The issuer of this certificate.
        :rtype: :class:`X509Name`
        """
        name = self._get_name('issuer_name')
        self._issuer_invalidator.add(name)
        return name
    self.get_issuer = get_issuer

    def set_issuer(issuer):
        """
        Set the issuer of this certificate.

        :param issuer: The issuer.
        :type issuer: :py:class:`X509Name`

        :return: ``None``
        """
        self._set_name("issuer_name", issuer)
        self._issuer_invalidator.clear()
    self.set_issuer = set_issuer

    def get_subject():
        """
        Return the subject of this certificate.

        This creates a new :class:`X509Name` that wraps the underlying subject
        name field on the certificate. Modifying it will modify the underlying
        certificate, and will have the effect of modifying any other
        :class:`X509Name` that refers to this subject.

        :return: The subject of this certificate.
        :rtype: :class:`X509Name`
        """
        name = self._get_name('subject_name')
        self._subject_invalidator.add(name)
        return name
    self.get_subject = get_subject

    def set_subject(subject):
        """
        Set the subject of this certificate.

        :param subject: The subject.
        :type subject: :py:class:`X509Name`

        :return: ``None``
        """
        self._set_name("subject_name", subject)
        self._subject_invalidator.clear()
    self.set_subject = set_subject

    def get_extension_count():
        """
        Get the number of extensions on this certificate.

        :return: The number of extensions.
        :rtype: :py:class:`int`

        .. versionadded:: 0.12
        """
        fail("TODO")
        # return _lib.X509_get_ext_count(self._x509)
    self.get_extension_count = get_extension_count

    def add_extensions(extensions):
        """
        Add extensions to the certificate.

        :param extensions: The extensions to add.
        :type extensions: An iterable of :py:class:`X509Extension` objects.
        :return: ``None``
        """
        for ext in extensions:
            if not types.is_instance(ext, X509Extension):
                fail('ValueError: One of the elements is not an X509Extension')
            fail("TODO")
            #add_result = _lib.X509_add_ext(self._x509, ext._extension, -1)
            #if not add_result:
            #    _raise_current_error()
    self.add_extensions = add_extensions

    def get_extension(index):
        """
        Get a specific extension of the certificate by index.

        Extensions on a certificate are kept in order. The index
        parameter selects which extension will be returned.

        :param int index: The index of the extension to retrieve.
        :return: The extension at the specified index.
        :rtype: :py:class:`X509Extension`
        :raises IndexError: If the extension index was out of bounds.

        .. versionadded:: 0.12
        """
        ext = X509Extension()
        ext._extension = None # _lib.X509_get_ext(self._x509, index)
        if ext._extension == None:
            fail('IndexError: extension index out of bounds')
        fail("TODO")
        # extension = _lib.X509_EXTENSION_dup(ext._extension)
        # ext._extension = _ffi.gc(extension, _lib.X509_EXTENSION_free)
        return ext
    self.get_extension = get_extension
    return self

#
# def X509StoreFlags():
#     """
#     Flags for X509 verification, used to change the behavior of
#     :class:`X509Store`.
#
#     See `OpenSSL Verification Flags`_ for details.
#
#     .. _OpenSSL Verification Flags:
#         https://www.openssl.org/docs/manmaster/man3/X509_VERIFY_PARAM_set_flags.html
#     """
#
#     CRL_CHECK = _lib.X509_V_FLAG_CRL_CHECK
#     CRL_CHECK_ALL = _lib.X509_V_FLAG_CRL_CHECK_ALL
#     IGNORE_CRITICAL = _lib.X509_V_FLAG_IGNORE_CRITICAL
#     X509_STRICT = _lib.X509_V_FLAG_X509_STRICT
#     ALLOW_PROXY_CERTS = _lib.X509_V_FLAG_ALLOW_PROXY_CERTS
#     POLICY_CHECK = _lib.X509_V_FLAG_POLICY_CHECK
#     EXPLICIT_POLICY = _lib.X509_V_FLAG_EXPLICIT_POLICY
#     INHIBIT_MAP = _lib.X509_V_FLAG_INHIBIT_MAP
#     NOTIFY_POLICY = _lib.X509_V_FLAG_NOTIFY_POLICY
#     CHECK_SS_SIGNATURE = _lib.X509_V_FLAG_CHECK_SS_SIGNATURE
#     return self
#
#
# def X509Store():
#     """
#     An X.509 store.
#
#     An X.509 store is used to describe a context in which to verify a
#     certificate. A description of a context may include a set of certificates
#     to trust, a set of certificate revocation lists, verification flags and
#     more.
#
#     An X.509 store, being only a description, cannot be used by itself to
#     verify a certificate. To carry out the actual verification process, see
#     :class:`X509StoreContext`.
#     """
#
#     def __init__():
#         store = _lib.X509_STORE_new()
#         self._store = _ffi.gc(store, _lib.X509_STORE_free)
#     self = __init__()
#
#     def add_cert(cert):
#         """
#         Adds a trusted certificate to this store.
#
#         Adding a certificate with this method adds this certificate as a
#         *trusted* certificate.
#
#         :param X509 cert: The certificate to add to this store.
#
#         :raises TypeError: If the certificate is not an :class:`X509`.
#
#         :raises OpenSSL.crypto.Error: If OpenSSL was unhappy with your
#             certificate.
#
#         :return: ``None`` if the certificate was added successfully.
#         """
#         if not isinstance(cert, X509):
#             fail(" TypeError()")
#
#         res = _lib.X509_STORE_add_cert(self._store, cert._x509)
#         _openssl_assert(res == 1)
#     self.add_cert = add_cert
#
#     def add_crl(crl):
#         """
#         Add a certificate revocation list to this store.
#
#         The certificate revocation lists added to a store will only be used if
#         the associated flags are configured to check certificate revocation
#         lists.
#
#         .. versionadded:: 16.1.0
#
#         :param CRL crl: The certificate revocation list to add to this store.
#         :return: ``None`` if the certificate revocation list was added
#             successfully.
#         """
#         _openssl_assert(_lib.X509_STORE_add_crl(self._store, crl._crl) != 0)
#     self.add_crl = add_crl
#
#     def set_flags(flags):
#         """
#         Set verification flags to this store.
#
#         Verification flags can be combined by oring them together.
#
#         .. note::
#
#           Setting a verification flag sometimes requires clients to add
#           additional information to the store, otherwise a suitable error will
#           be raised.
#
#           For example, in setting flags to enable CRL checking a
#           suitable CRL must be added to the store otherwise an error will be
#           raised.
#
#         .. versionadded:: 16.1.0
#
#         :param int flags: The verification flags to set on this store.
#             See :class:`X509StoreFlags` for available constants.
#         :return: ``None`` if the verification flags were successfully set.
#         """
#         _openssl_assert(_lib.X509_STORE_set_flags(self._store, flags) != 0)
#     self.set_flags = set_flags
#
#     def set_time(vfy_time):
#         """
#         Set the time against which the certificates are verified.
#
#         Normally the current time is used.
#
#         .. note::
#
#           For example, you can determine if a certificate was valid at a given
#           time.
#
#         .. versionadded:: 17.0.0
#
#         :param datetime vfy_time: The verification time to set on this store.
#         :return: ``None`` if the verification time was successfully set.
#         """
#         param = _lib.X509_VERIFY_PARAM_new()
#         param = _ffi.gc(param, _lib.X509_VERIFY_PARAM_free)
#
#         _lib.X509_VERIFY_PARAM_set_time(
#             param, calendar.timegm(vfy_time.timetuple())
#         )
#         _openssl_assert(_lib.X509_STORE_set1_param(self._store, param) != 0)
#     self.set_time = set_time
#
#     def load_locations(cafile, capath=None):
#         """
#         Let X509Store know where we can find trusted certificates for the
#         certificate chain.  Note that the certificates have to be in PEM
#         format.
#
#         If *capath* is passed, it must be a directory prepared using the
#         ``c_rehash`` tool included with OpenSSL.  Either, but not both, of
#         *cafile* or *capath* may be ``None``.
#
#         .. note::
#
#           Both *cafile* and *capath* may be set simultaneously.
#
#           Call this method multiple times to add more than one location.
#           For example, CA certificates, and certificate revocation list bundles
#           may be passed in *cafile* in subsequent calls to this method.
#
#         .. versionadded:: 20.0
#
#         :param cafile: In which file we can find the certificates (``bytes`` or
#                        ``unicode``).
#         :param capath: In which directory we can find the certificates
#                        (``bytes`` or ``unicode``).
#
#         :return: ``None`` if the locations were set successfully.
#
#         :raises OpenSSL.crypto.Error: If both *cafile* and *capath* is ``None``
#             or the locations could not be set for any reason.
#
#         """
#         if cafile == None:
#             cafile = _ffi.NULL
#         else:
#             cafile = _path_string(cafile)
#
#         if capath == None:
#             capath = _ffi.NULL
#         else:
#             capath = _path_string(capath)
#
#         load_result = _lib.X509_STORE_load_locations(
#             self._store, cafile, capath
#         )
#         if not load_result:
#             _raise_current_error()
#     self.load_locations = load_locations
#     return self
#
#
# def X509StoreContextError(message, certificate):
#     """
#     An exception raised when an error occurred while verifying a certificate
#     using `OpenSSL.X509StoreContext.verify_certificate`.
#
#     :ivar certificate: The certificate which caused verificate failure.
#     :type certificate: :class:`X509`
#     """
#
#     def __init__(message, certificate):
#         super(X509StoreContextError, self).__init__(message)
#         self.certificate = certificate
#     self = __init__(message, certificate)
#     return self
#
#
# def X509StoreContext(store, certificate, chain=None):
#     """
#     An X.509 store context.
#
#     An X.509 store context is used to carry out the actual verification process
#     of a certificate in a described context. For describing such a context, see
#     :class:`X509Store`.
#
#     :ivar _store_ctx: The underlying X509_STORE_CTX structure used by this
#         instance.  It is dynamically allocated and automatically garbage
#         collected.
#     :ivar _store: See the ``store`` ``__init__`` parameter.
#     :ivar _cert: See the ``certificate`` ``__init__`` parameter.
#     :ivar _chain: See the ``chain`` ``__init__`` parameter.
#     :param X509Store store: The certificates which will be trusted for the
#         purposes of any verifications.
#     :param X509 certificate: The certificate to be verified.
#     :param chain: List of untrusted certificates that may be used for building
#         the certificate chain. May be ``None``.
#     :type chain: :class:`list` of :class:`X509`
#     """
#
#     def __init__(store, certificate, chain):
#         store_ctx = _lib.X509_STORE_CTX_new()
#         self._store_ctx = _ffi.gc(store_ctx, _lib.X509_STORE_CTX_free)
#         self._store = store
#         self._cert = certificate
#         self._chain = self._build_certificate_stack(chain)
#         # Make the store context available for use after instantiating this
#         # class by initializing it now. Per testing, subsequent calls to
#         # :meth:`_init` have no adverse affect.
#         self._init()
#     self = __init__(store, certificate, chain)
#
#     @staticmethod
#     def _build_certificate_stack(certificates):
#         def cleanup(s):
#             # Equivalent to sk_X509_pop_free, but we don't
#             # currently have a CFFI binding for that available
#             for i in range(_lib.sk_X509_num(s)):
#                 x = _lib.sk_X509_value(s, i)
#                 _lib.X509_free(x)
#             _lib.sk_X509_free(s)
#         self.cleanup = cleanup
#
#         if certificates == None or len(certificates) == 0:
#             return _ffi.NULL
#
#         stack = _lib.sk_X509_new_null()
#         _openssl_assert(stack != _ffi.NULL)
#         stack = _ffi.gc(stack, cleanup)
#
#         for cert in certificates:
#             if not isinstance(cert, X509):
#                 fail(" TypeError(\"One of the elements is not an X509 instance\")")
#
#             _openssl_assert(_lib.X509_up_ref(cert._x509) > 0)
#             if _lib.sk_X509_push(stack, cert._x509) <= 0:
#                 _lib.X509_free(cert._x509)
#                 _raise_current_error()
#
#         return stack
#     self._build_certificate_stack = _build_certificate_stack
#
#     def _init():
#         """
#         Set up the store context for a subsequent verification operation.
#
#         Calling this method more than once without first calling
#         :meth:`_cleanup` will leak memory.
#         """
#         ret = _lib.X509_STORE_CTX_init(
#             self._store_ctx, self._store._store, self._cert._x509, self._chain
#         )
#         if ret <= 0:
#             _raise_current_error()
#     self._init = _init
#
#     def _cleanup():
#         """
#         Internally cleans up the store context.
#
#         The store context can then be reused with a new call to :meth:`_init`.
#         """
#         _lib.X509_STORE_CTX_cleanup(self._store_ctx)
#     self._cleanup = _cleanup
#
#     def _exception_from_context():
#         """
#         Convert an OpenSSL native context error failure into a Python
#         exception.
#
#         When a call to native OpenSSL X509_verify_cert fails, additional
#         information about the failure can be obtained from the store context.
#         """
#         errors = [
#             _lib.X509_STORE_CTX_get_error(self._store_ctx),
#             _lib.X509_STORE_CTX_get_error_depth(self._store_ctx),
#             _native(
#                 _ffi.string(
#                     _lib.X509_verify_cert_error_string(
#                         _lib.X509_STORE_CTX_get_error(self._store_ctx)
#                     )
#                 )
#             ),
#         ]
#         # A context error should always be associated with a certificate, so we
#         # expect this call to never return :class:`None`.
#         _x509 = _lib.X509_STORE_CTX_get_current_cert(self._store_ctx)
#         _cert = _lib.X509_dup(_x509)
#         pycert = X509._from_raw_x509_ptr(_cert)
#         return X509StoreContextError(errors, pycert)
#     self._exception_from_context = _exception_from_context
#
#     def set_store(store):
#         """
#         Set the context's X.509 store.
#
#         .. versionadded:: 0.15
#
#         :param X509Store store: The store description which will be used for
#             the purposes of any *future* verifications.
#         """
#         self._store = store
#     self.set_store = set_store
#
#     def verify_certificate():
#         """
#         Verify a certificate in a context.
#
#         .. versionadded:: 0.15
#
#         :raises X509StoreContextError: If an error occurred when validating a
#           certificate in the context. Sets ``certificate`` attribute to
#           indicate which certificate caused the error.
#         """
#         # Always re-initialize the store context in case
#         # :meth:`verify_certificate` is called multiple times.
#         #
#         # :meth:`_init` is called in :meth:`__init__` so _cleanup is called
#         # before _init to ensure memory is not leaked.
#         self._cleanup()
#         self._init()
#         ret = _lib.X509_verify_cert(self._store_ctx)
#         self._cleanup()
#         if ret <= 0:
#             fail(" self._exception_from_context()")
#     self.verify_certificate = verify_certificate
#
#     def get_verified_chain():
#         """
#         Verify a certificate in a context and return the complete validated
#         chain.
#
#         :raises X509StoreContextError: If an error occurred when validating a
#           certificate in the context. Sets ``certificate`` attribute to
#           indicate which certificate caused the error.
#
#         .. versionadded:: 20.0
#         """
#         # Always re-initialize the store context in case
#         # :meth:`verify_certificate` is called multiple times.
#         #
#         # :meth:`_init` is called in :meth:`__init__` so _cleanup is called
#         # before _init to ensure memory is not leaked.
#         self._cleanup()
#         self._init()
#         ret = _lib.X509_verify_cert(self._store_ctx)
#         if ret <= 0:
#             self._cleanup()
#             fail(" self._exception_from_context()")
#
#         # Note: X509_STORE_CTX_get1_chain returns a deep copy of the chain.
#         cert_stack = _lib.X509_STORE_CTX_get1_chain(self._store_ctx)
#         _openssl_assert(cert_stack != _ffi.NULL)
#
#         result = []
#         for i in range(_lib.sk_X509_num(cert_stack)):
#             cert = _lib.sk_X509_value(cert_stack, i)
#             _openssl_assert(cert != _ffi.NULL)
#             pycert = X509._from_raw_x509_ptr(cert)
#             result.append(pycert)
#
#         # Free the stack but not the members which are freed by the X509 class.
#         _lib.sk_X509_free(cert_stack)
#         self._cleanup()
#         return result
#     self.get_verified_chain = get_verified_chain
#     return self
#
#
# def load_certificate(type, buffer):
#     """
#     Load a certificate (X509) from the string *buffer* encoded with the
#     type *type*.
#
#     :param type: The file type (one of FILETYPE_PEM, FILETYPE_ASN1)
#
#     :param bytes buffer: The buffer the certificate is stored in
#
#     :return: The X509 object
#     """
#     if isinstance(buffer, _text_type):
#         buffer = buffer.encode("ascii")
#
#     bio = _new_mem_buf(buffer)
#
#     if type == FILETYPE_PEM:
#         x509 = _lib.PEM_read_bio_X509(bio, _ffi.NULL, _ffi.NULL, _ffi.NULL)
#     elif type == FILETYPE_ASN1:
#         x509 = _lib.d2i_X509_bio(bio, _ffi.NULL)
#     else:
#         fail(" ValueError(\"type argument must be FILETYPE_PEM or FILETYPE_ASN1\")")
#
#     if x509 == _ffi.NULL:
#         _raise_current_error()
#
#     return X509._from_raw_x509_ptr(x509)
#

def dump_certificate(type, cert):
    """
    Dump the certificate *cert* into a buffer string encoded with the type
    *type*.

    :param type: The file type (one of FILETYPE_PEM, FILETYPE_ASN1, or
        FILETYPE_TEXT)
    :param cert: The certificate to dump
    :return: The buffer with the dumped certificate in
    """
    fileformat = None
    if type == FILETYPE_PEM:
        fileformat = "PEM"
        # result_code = _lib.PEM_write_bio_X509(bio, cert._x509)
    elif type == FILETYPE_ASN1:
        fileformat = "ASN1"
        #result_code = _lib.i2d_X509_bio(bio, cert._x509)
    elif type == FILETYPE_TEXT:
        fileformat = "TEXT"
        #result_code = _lib.X509_print_ex(bio, cert._x509, 0, 0)
    else:
        fail('ValueError: type argument must be FILETYPE_PEM, ' +
             'FILETYPE_ASN1, or FILETYPE_TEXT')

    return _JOpenSSL.OpenSSL.dump_certificate(cert._signed, fileformat)


# def dump_publickey(type, pkey):
#     """
#     Dump a public key to a buffer.
#
#     :param type: The file type (one of :data:`FILETYPE_PEM` or
#         :data:`FILETYPE_ASN1`).
#     :param PKey pkey: The public key to dump
#     :return: The buffer with the dumped key in it.
#     :rtype: bytes
#     """
#     bio = _new_mem_buf()
#     if type == FILETYPE_PEM:
#         write_bio = _lib.PEM_write_bio_PUBKEY
#     elif type == FILETYPE_ASN1:
#         write_bio = _lib.i2d_PUBKEY_bio
#     else:
#         fail(" ValueError(\"type argument must be FILETYPE_PEM or FILETYPE_ASN1\")")
#
#     result_code = write_bio(bio, pkey._pkey)
#     if result_code != 1:  # pragma: no cover
#         _raise_current_error()
#
#     return _bio_to_string(bio)
#
#
# def dump_privatekey(type, pkey, cipher=None, passphrase=None):
#     """
#     Dump the private key *pkey* into a buffer string encoded with the type
#     *type*.  Optionally (if *type* is :const:`FILETYPE_PEM`) encrypting it
#     using *cipher* and *passphrase*.
#
#     :param type: The file type (one of :const:`FILETYPE_PEM`,
#         :const:`FILETYPE_ASN1`, or :const:`FILETYPE_TEXT`)
#     :param PKey pkey: The PKey to dump
#     :param cipher: (optional) if encrypted PEM format, the cipher to use
#     :param passphrase: (optional) if encrypted PEM format, this can be either
#         the passphrase to use, or a callback for providing the passphrase.
#
#     :return: The buffer with the dumped key in
#     :rtype: bytes
#     """
#     bio = _new_mem_buf()
#
#     if not isinstance(pkey, PKey):
#         fail(" TypeError(\"pkey must be a PKey\")")
#
#     if cipher != None:
#         if passphrase == None:
#             fail(" TypeError(\n                \"if a value is given for cipher \"\n                \"one must also be given for passphrase\"\n            )")
#         cipher_obj = _lib.EVP_get_cipherbyname(_byte_string(cipher))
#         if cipher_obj == _ffi.NULL:
#             fail(" ValueError(\"Invalid cipher name\")")
#     else:
#         cipher_obj = _ffi.NULL
#
#     helper = _PassphraseHelper(type, passphrase)
#     if type == FILETYPE_PEM:
#         result_code = _lib.PEM_write_bio_PrivateKey(
#             bio,
#             pkey._pkey,
#             cipher_obj,
#             _ffi.NULL,
#             0,
#             helper.callback,
#             helper.callback_args,
#         )
#         helper.raise_if_problem()
#     elif type == FILETYPE_ASN1:
#         result_code = _lib.i2d_PrivateKey_bio(bio, pkey._pkey)
#     elif type == FILETYPE_TEXT:
#         if _lib.EVP_PKEY_id(pkey._pkey) != _lib.EVP_PKEY_RSA:
#             fail(" TypeError(\"Only RSA keys are supported for FILETYPE_TEXT\")")
#
#         rsa = _ffi.gc(_lib.EVP_PKEY_get1_RSA(pkey._pkey), _lib.RSA_free)
#         result_code = _lib.RSA_print(bio, rsa, 0)
#     else:
#         fail(" ValueError(\n            \"type argument must be FILETYPE_PEM, FILETYPE_ASN1, or \"\n            \"FILETYPE_TEXT\"\n        )")
#
#     _openssl_assert(result_code != 0)
#
#     return _bio_to_string(bio)

#
# def Revoked():
#     """
#     A certificate revocation.
#     """
#
#     # https://www.openssl.org/docs/manmaster/man5/x509v3_config.html#CRL-distribution-points
#     # which differs from crl_reasons of crypto/x509v3/v3_enum.c that matches
#     # OCSP_crl_reason_str.  We use the latter, just like the command line
#     # program.
#     _crl_reasons = [
#         bytes([0x75, 0x6e, 0x73, 0x70, 0x65, 0x63, 0x69, 0x66, 0x69, 0x65, 0x64]),
#         bytes([0x6b, 0x65, 0x79, 0x43, 0x6f, 0x6d, 0x70, 0x72, 0x6f, 0x6d, 0x69, 0x73, 0x65]),
#         bytes([0x43, 0x41, 0x43, 0x6f, 0x6d, 0x70, 0x72, 0x6f, 0x6d, 0x69, 0x73, 0x65]),
#         bytes([0x61, 0x66, 0x66, 0x69, 0x6c, 0x69, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x43, 0x68, 0x61, 0x6e, 0x67, 0x65, 0x64]),
#         bytes([0x73, 0x75, 0x70, 0x65, 0x72, 0x73, 0x65, 0x64, 0x65, 0x64]),
#         bytes([0x63, 0x65, 0x73, 0x73, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x4f, 0x66, 0x4f, 0x70, 0x65, 0x72, 0x61, 0x74, 0x69, 0x6f, 0x6e]),
#         bytes([0x63, 0x65, 0x72, 0x74, 0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x65, 0x48, 0x6f, 0x6c, 0x64]),
#         # b"removeFromCRL",
#     ]
#
#     def __init__():
#         revoked = _lib.X509_REVOKED_new()
#         self._revoked = _ffi.gc(revoked, _lib.X509_REVOKED_free)
#     self = __init__()
#
#     def set_serial(hex_str):
#         """
#         Set the serial number.
#
#         The serial number is formatted as a hexadecimal number encoded in
#         ASCII.
#
#         :param bytes hex_str: The new serial number.
#
#         :return: ``None``
#         """
#         bignum_serial = _ffi.gc(_lib.BN_new(), _lib.BN_free)
#         bignum_ptr = _ffi.new("BIGNUM**")
#         bignum_ptr[0] = bignum_serial
#         bn_result = _lib.BN_hex2bn(bignum_ptr, hex_str)
#         if not bn_result:
#             fail(" ValueError(\"bad hex string\")")
#
#         asn1_serial = _ffi.gc(
#             _lib.BN_to_ASN1_INTEGER(bignum_serial, _ffi.NULL),
#             _lib.ASN1_INTEGER_free,
#         )
#         _lib.X509_REVOKED_set_serialNumber(self._revoked, asn1_serial)
#     self.set_serial = set_serial
#
#     def get_serial():
#         """
#         Get the serial number.
#
#         The serial number is formatted as a hexadecimal number encoded in
#         ASCII.
#
#         :return: The serial number.
#         :rtype: bytes
#         """
#         bio = _new_mem_buf()
#
#         asn1_int = _lib.X509_REVOKED_get0_serialNumber(self._revoked)
#         _openssl_assert(asn1_int != _ffi.NULL)
#         result = _lib.i2a_ASN1_INTEGER(bio, asn1_int)
#         _openssl_assert(result >= 0)
#         return _bio_to_string(bio)
#     self.get_serial = get_serial
#
#     def _delete_reason():
#         for i in range(_lib.X509_REVOKED_get_ext_count(self._revoked)):
#             ext = _lib.X509_REVOKED_get_ext(self._revoked, i)
#             obj = _lib.X509_EXTENSION_get_object(ext)
#             if _lib.OBJ_obj2nid(obj) == _lib.NID_crl_reason:
#                 _lib.X509_EXTENSION_free(ext)
#                 _lib.X509_REVOKED_delete_ext(self._revoked, i)
#                 break
#     self._delete_reason = _delete_reason
#
#     def set_reason(reason):
#         """
#         Set the reason of this revocation.
#
#         If :data:`reason` is ``None``, delete the reason instead.
#
#         :param reason: The reason string.
#         :type reason: :class:`bytes` or :class:`NoneType`
#
#         :return: ``None``
#
#         .. seealso::
#
#             :meth:`all_reasons`, which gives you a list of all supported
#             reasons which you might pass to this method.
#         """
#         if reason == None:
#             self._delete_reason()
#         elif not isinstance(reason, bytes):
#             fail(" TypeError(\"reason must be None or a byte string\")")
#         else:
#             reason = reason.lower().replace(bytes([0x20]), bytes(r"", encoding='utf-8'))
#             reason_code = [r.lower() for r in self._crl_reasons].index(reason)
#
#             new_reason_ext = _lib.ASN1_ENUMERATED_new()
#             _openssl_assert(new_reason_ext != _ffi.NULL)
#             new_reason_ext = _ffi.gc(new_reason_ext, _lib.ASN1_ENUMERATED_free)
#
#             set_result = _lib.ASN1_ENUMERATED_set(new_reason_ext, reason_code)
#             _openssl_assert(set_result != _ffi.NULL)
#
#             self._delete_reason()
#             add_result = _lib.X509_REVOKED_add1_ext_i2d(
#                 self._revoked, _lib.NID_crl_reason, new_reason_ext, 0, 0
#             )
#             _openssl_assert(add_result == 1)
#     self.set_reason = set_reason
#
#     def get_reason():
#         """
#         Get the reason of this revocation.
#
#         :return: The reason, or ``None`` if there is none.
#         :rtype: bytes or NoneType
#
#         .. seealso::
#
#             :meth:`all_reasons`, which gives you a list of all supported
#             reasons this method might return.
#         """
#         for i in range(_lib.X509_REVOKED_get_ext_count(self._revoked)):
#             ext = _lib.X509_REVOKED_get_ext(self._revoked, i)
#             obj = _lib.X509_EXTENSION_get_object(ext)
#             if _lib.OBJ_obj2nid(obj) == _lib.NID_crl_reason:
#                 bio = _new_mem_buf()
#
#                 print_result = _lib.X509V3_EXT_print(bio, ext, 0, 0)
#                 if not print_result:
#                     print_result = _lib.M_ASN1_OCTET_STRING_print(
#                         bio, _lib.X509_EXTENSION_get_data(ext)
#                     )
#                     _openssl_assert(print_result != 0)
#
#                 return _bio_to_string(bio)
#     self.get_reason = get_reason
#
#     def all_reasons():
#         """
#         Return a list of all the supported reason strings.
#
#         This list is a copy; modifying it does not change the supported reason
#         strings.
#
#         :return: A list of reason strings.
#         :rtype: :class:`list` of :class:`bytes`
#         """
#         return self._crl_reasons[:]
#     self.all_reasons = all_reasons
#
#     def set_rev_date(when):
#         """
#         Set the revocation timestamp.
#
#         :param bytes when: The timestamp of the revocation,
#             as ASN.1 TIME.
#         :return: ``None``
#         """
#         dt = _lib.X509_REVOKED_get0_revocationDate(self._revoked)
#         return _set_asn1_time(dt, when)
#     self.set_rev_date = set_rev_date
#
#     def get_rev_date():
#         """
#         Get the revocation timestamp.
#
#         :return: The timestamp of the revocation, as ASN.1 TIME.
#         :rtype: bytes
#         """
#         dt = _lib.X509_REVOKED_get0_revocationDate(self._revoked)
#         return _get_asn1_time(dt)
#     self.get_rev_date = get_rev_date
#     return self
#
#
# def CRL():
#     """
#     A certificate revocation list.
#     """
#
#     def __init__():
#         crl = _lib.X509_CRL_new()
#         self._crl = _ffi.gc(crl, _lib.X509_CRL_free)
#     self = __init__()
#
#     def to_cryptography():
#         """
#         Export as a ``cryptography`` CRL.
#
#         :rtype: ``cryptography.x509.CertificateRevocationList``
#
#         .. versionadded:: 17.1.0
#         """
#         load("@vendor//cryptography/hazmat/backends/openssl/x509", _CertificateRevocationList="_CertificateRevocationList")
#
#         backend = _get_backend()
#         return _CertificateRevocationList(backend, self._crl)
#     self.to_cryptography = to_cryptography
#
#     @classmethod
#     def from_cryptography(cls, crypto_crl):
#         """
#         Construct based on a ``cryptography`` *crypto_crl*.
#
#         :param crypto_crl: A ``cryptography`` certificate revocation list
#         :type crypto_crl: ``cryptography.x509.CertificateRevocationList``
#
#         :rtype: CRL
#
#         .. versionadded:: 17.1.0
#         """
#         if not isinstance(crypto_crl, x509.CertificateRevocationList):
#             fail(" TypeError(\"Must be a certificate revocation list\")")
#
#         crl = cls()
#         crl._crl = crypto_crl._x509_crl
#         return crl
#     self.from_cryptography = from_cryptography
#
#     def get_revoked():
#         """
#         Return the revocations in this certificate revocation list.
#
#         These revocations will be provided by value, not by reference.
#         That means it's okay to mutate them: it won't affect this CRL.
#
#         :return: The revocations in this CRL.
#         :rtype: :class:`tuple` of :class:`Revocation`
#         """
#         results = []
#         revoked_stack = _lib.X509_CRL_get_REVOKED(self._crl)
#         for i in range(_lib.sk_X509_REVOKED_num(revoked_stack)):
#             revoked = _lib.sk_X509_REVOKED_value(revoked_stack, i)
#             revoked_copy = _lib.Cryptography_X509_REVOKED_dup(revoked)
#             pyrev = Revoked.__new__(Revoked)
#             pyrev._revoked = _ffi.gc(revoked_copy, _lib.X509_REVOKED_free)
#             results.append(pyrev)
#         if results:
#             return tuple(results)
#     self.get_revoked = get_revoked
#
#     def add_revoked(revoked):
#         """
#         Add a revoked (by value not reference) to the CRL structure
#
#         This revocation will be added by value, not by reference. That
#         means it's okay to mutate it after adding: it won't affect
#         this CRL.
#
#         :param Revoked revoked: The new revocation.
#         :return: ``None``
#         """
#         copy = _lib.Cryptography_X509_REVOKED_dup(revoked._revoked)
#         _openssl_assert(copy != _ffi.NULL)
#
#         add_result = _lib.X509_CRL_add0_revoked(self._crl, copy)
#         _openssl_assert(add_result != 0)
#     self.add_revoked = add_revoked
#
#     def get_issuer():
#         """
#         Get the CRL's issuer.
#
#         .. versionadded:: 16.1.0
#
#         :rtype: X509Name
#         """
#         _issuer = _lib.X509_NAME_dup(_lib.X509_CRL_get_issuer(self._crl))
#         _openssl_assert(_issuer != _ffi.NULL)
#         _issuer = _ffi.gc(_issuer, _lib.X509_NAME_free)
#         issuer = X509Name.__new__(X509Name)
#         issuer._name = _issuer
#         return issuer
#     self.get_issuer = get_issuer
#
#     def set_version(version):
#         """
#         Set the CRL version.
#
#         .. versionadded:: 16.1.0
#
#         :param int version: The version of the CRL.
#         :return: ``None``
#         """
#         _openssl_assert(_lib.X509_CRL_set_version(self._crl, version) != 0)
#     self.set_version = set_version
#
#     def _set_boundary_time(which, when):
#         return _set_asn1_time(which(self._crl), when)
#     self._set_boundary_time = _set_boundary_time
#
#     def set_lastUpdate(when):
#         """
#         Set when the CRL was last updated.
#
#         The timestamp is formatted as an ASN.1 TIME::
#
#             YYYYMMDDhhmmssZ
#
#         .. versionadded:: 16.1.0
#
#         :param bytes when: A timestamp string.
#         :return: ``None``
#         """
#         return self._set_boundary_time(_lib.X509_CRL_get_lastUpdate, when)
#     self.set_lastUpdate = set_lastUpdate
#
#     def set_nextUpdate(when):
#         """
#         Set when the CRL will next be updated.
#
#         The timestamp is formatted as an ASN.1 TIME::
#
#             YYYYMMDDhhmmssZ
#
#         .. versionadded:: 16.1.0
#
#         :param bytes when: A timestamp string.
#         :return: ``None``
#         """
#         return self._set_boundary_time(_lib.X509_CRL_get_nextUpdate, when)
#     self.set_nextUpdate = set_nextUpdate
#
#     def sign(issuer_cert, issuer_key, digest):
#         """
#         Sign the CRL.
#
#         Signing a CRL enables clients to associate the CRL itself with an
#         issuer. Before a CRL is meaningful to other OpenSSL functions, it must
#         be signed by an issuer.
#
#         This method implicitly sets the issuer's name based on the issuer
#         certificate and private key used to sign the CRL.
#
#         .. versionadded:: 16.1.0
#
#         :param X509 issuer_cert: The issuer's certificate.
#         :param PKey issuer_key: The issuer's private key.
#         :param bytes digest: The digest method to sign the CRL with.
#         """
#         digest_obj = _lib.EVP_get_digestbyname(digest)
#         _openssl_assert(digest_obj != _ffi.NULL)
#         _lib.X509_CRL_set_issuer_name(
#             self._crl, _lib.X509_get_subject_name(issuer_cert._x509)
#         )
#         _lib.X509_CRL_sort(self._crl)
#         result = _lib.X509_CRL_sign(self._crl, issuer_key._pkey, digest_obj)
#         _openssl_assert(result != 0)
#     self.sign = sign
#
#     def export(
#         cert, key, type=FILETYPE_PEM, days=100, digest=_UNSPECIFIED
#     ):
#         """
#         Export the CRL as a string.
#
#         :param X509 cert: The certificate used to sign the CRL.
#         :param PKey key: The key used to sign the CRL.
#         :param int type: The export format, either :data:`FILETYPE_PEM`,
#             :data:`FILETYPE_ASN1`, or :data:`FILETYPE_TEXT`.
#         :param int days: The number of days until the next update of this CRL.
#         :param bytes digest: The name of the message digest to use (eg
#             ``b"sha256"``).
#         :rtype: bytes
#         """
#
#         if not isinstance(cert, X509):
#             fail(" TypeError(\"cert must be an X509 instance\")")
#         if not isinstance(key, PKey):
#             fail(" TypeError(\"key must be a PKey instance\")")
#         if not isinstance(type, int):
#             fail(" TypeError(\"type must be an integer\")")
#
#         if digest == _UNSPECIFIED:
#             fail(" TypeError(\"digest must be provided\")")
#
#         digest_obj = _lib.EVP_get_digestbyname(digest)
#         if digest_obj == _ffi.NULL:
#             fail(" ValueError(\"No such digest method\")")
#
#         bio = _lib.BIO_new(_lib.BIO_s_mem())
#         _openssl_assert(bio != _ffi.NULL)
#
#         # A scratch time object to give different values to different CRL
#         # fields
#         sometime = _lib.ASN1_TIME_new()
#         _openssl_assert(sometime != _ffi.NULL)
#
#         _lib.X509_gmtime_adj(sometime, 0)
#         _lib.X509_CRL_set_lastUpdate(self._crl, sometime)
#
#         _lib.X509_gmtime_adj(sometime, days * 24 * 60 * 60)
#         _lib.X509_CRL_set_nextUpdate(self._crl, sometime)
#
#         _lib.X509_CRL_set_issuer_name(
#             self._crl, _lib.X509_get_subject_name(cert._x509)
#         )
#
#         sign_result = _lib.X509_CRL_sign(self._crl, key._pkey, digest_obj)
#         if not sign_result:
#             _raise_current_error()
#
#         return dump_crl(type, self)
#     self.export = export
#     return self


# def PKCS7():
#     def type_is_signed():
#         """
#         Check if this NID_pkcs7_signed object
#
#         :return: True if the PKCS7 is of type signed
#         """
#         return bool(_lib.PKCS7_type_is_signed(self._pkcs7))
#     self.type_is_signed = type_is_signed
#
#     def type_is_enveloped():
#         """
#         Check if this NID_pkcs7_enveloped object
#
#         :returns: True if the PKCS7 is of type enveloped
#         """
#         return bool(_lib.PKCS7_type_is_enveloped(self._pkcs7))
#     self.type_is_enveloped = type_is_enveloped
#
#     def type_is_signedAndEnveloped():
#         """
#         Check if this NID_pkcs7_signedAndEnveloped object
#
#         :returns: True if the PKCS7 is of type signedAndEnveloped
#         """
#         return bool(_lib.PKCS7_type_is_signedAndEnveloped(self._pkcs7))
#     self.type_is_signedAndEnveloped = type_is_signedAndEnveloped
#
#     def type_is_data():
#         """
#         Check if this NID_pkcs7_data object
#
#         :return: True if the PKCS7 is of type data
#         """
#         return bool(_lib.PKCS7_type_is_data(self._pkcs7))
#     self.type_is_data = type_is_data
#
#     def get_type_name():
#         """
#         Returns the type name of the PKCS7 structure
#
#         :return: A string with the typename
#         """
#         nid = _lib.OBJ_obj2nid(self._pkcs7.type)
#         string_type = _lib.OBJ_nid2sn(nid)
#         return _ffi.string(string_type)
#     self.get_type_name = get_type_name
#     return self
#
#
# def PKCS12():
#     """
#     A PKCS #12 archive.
#     """
#
#     def __init__():
#         self._pkey = None
#         self._cert = None
#         self._cacerts = None
#         self._friendlyname = None
#     self = __init__()
#
#     def get_certificate():
#         """
#         Get the certificate in the PKCS #12 structure.
#
#         :return: The certificate, or :py:const:`None` if there is none.
#         :rtype: :py:class:`X509` or :py:const:`None`
#         """
#         return self._cert
#     self.get_certificate = get_certificate
#
#     def set_certificate(cert):
#         """
#         Set the certificate in the PKCS #12 structure.
#
#         :param cert: The new certificate, or :py:const:`None` to unset it.
#         :type cert: :py:class:`X509` or :py:const:`None`
#
#         :return: ``None``
#         """
#         if not isinstance(cert, X509):
#             fail(" TypeError(\"cert must be an X509 instance\")")
#         self._cert = cert
#     self.set_certificate = set_certificate
#
#     def get_privatekey():
#         """
#         Get the private key in the PKCS #12 structure.
#
#         :return: The private key, or :py:const:`None` if there is none.
#         :rtype: :py:class:`PKey`
#         """
#         return self._pkey
#     self.get_privatekey = get_privatekey
#
#     def set_privatekey(pkey):
#         """
#         Set the certificate portion of the PKCS #12 structure.
#
#         :param pkey: The new private key, or :py:const:`None` to unset it.
#         :type pkey: :py:class:`PKey` or :py:const:`None`
#
#         :return: ``None``
#         """
#         if not isinstance(pkey, PKey):
#             fail(" TypeError(\"pkey must be a PKey instance\")")
#         self._pkey = pkey
#     self.set_privatekey = set_privatekey
#
#     def get_ca_certificates():
#         """
#         Get the CA certificates in the PKCS #12 structure.
#
#         :return: A tuple with the CA certificates in the chain, or
#             :py:const:`None` if there are none.
#         :rtype: :py:class:`tuple` of :py:class:`X509` or :py:const:`None`
#         """
#         if self._cacerts != None:
#             return tuple(self._cacerts)
#     self.get_ca_certificates = get_ca_certificates
#
#     def set_ca_certificates(cacerts):
#         """
#         Replace or set the CA certificates within the PKCS12 object.
#
#         :param cacerts: The new CA certificates, or :py:const:`None` to unset
#             them.
#         :type cacerts: An iterable of :py:class:`X509` or :py:const:`None`
#
#         :return: ``None``
#         """
#         if cacerts == None:
#             self._cacerts = None
#         else:
#             cacerts = list(cacerts)
#             for cert in cacerts:
#                 if not isinstance(cert, X509):
#                     fail(" TypeError(\n                        \"iterable must only contain X509 instances\"\n                    )")
#             self._cacerts = cacerts
#     self.set_ca_certificates = set_ca_certificates
#
#     def set_friendlyname(name):
#         """
#         Set the friendly name in the PKCS #12 structure.
#
#         :param name: The new friendly name, or :py:const:`None` to unset.
#         :type name: :py:class:`bytes` or :py:const:`None`
#
#         :return: ``None``
#         """
#         if name == None:
#             self._friendlyname = None
#         elif not isinstance(name, bytes):
#             fail(" TypeError(\n                \"name must be a byte string or None (not %r)\" % (name,)\n            )")
#         self._friendlyname = name
#     self.set_friendlyname = set_friendlyname
#
#     def get_friendlyname():
#         """
#         Get the friendly name in the PKCS# 12 structure.
#
#         :returns: The friendly name,  or :py:const:`None` if there is none.
#         :rtype: :py:class:`bytes` or :py:const:`None`
#         """
#         return self._friendlyname
#     self.get_friendlyname = get_friendlyname
#
#     def export(passphrase=None, iter=2048, maciter=1):
#         """
#         Dump a PKCS12 object as a string.
#
#         For more information, see the :c:func:`PKCS12_create` man page.
#
#         :param passphrase: The passphrase used to encrypt the structure. Unlike
#             some other passphrase arguments, this *must* be a string, not a
#             callback.
#         :type passphrase: :py:data:`bytes`
#
#         :param iter: Number of times to repeat the encryption step.
#         :type iter: :py:data:`int`
#
#         :param maciter: Number of times to repeat the MAC step.
#         :type maciter: :py:data:`int`
#
#         :return: The string representation of the PKCS #12 structure.
#         :rtype:
#         """
#         passphrase = _text_to_bytes_and_warn("passphrase", passphrase)
#
#         if self._cacerts == None:
#             cacerts = _ffi.NULL
#         else:
#             cacerts = _lib.sk_X509_new_null()
#             cacerts = _ffi.gc(cacerts, _lib.sk_X509_free)
#             for cert in self._cacerts:
#                 _lib.sk_X509_push(cacerts, cert._x509)
#
#         if passphrase == None:
#             passphrase = _ffi.NULL
#
#         friendlyname = self._friendlyname
#         if friendlyname == None:
#             friendlyname = _ffi.NULL
#
#         if self._pkey == None:
#             pkey = _ffi.NULL
#         else:
#             pkey = self._pkey._pkey
#
#         if self._cert == None:
#             cert = _ffi.NULL
#         else:
#             cert = self._cert._x509
#
#         pkcs12 = _lib.PKCS12_create(
#             passphrase,
#             friendlyname,
#             pkey,
#             cert,
#             cacerts,
#             _lib.NID_pbe_WithSHA1And3_Key_TripleDES_CBC,
#             _lib.NID_pbe_WithSHA1And3_Key_TripleDES_CBC,
#             iter,
#             maciter,
#             0,
#         )
#         if pkcs12 == _ffi.NULL:
#             _raise_current_error()
#         pkcs12 = _ffi.gc(pkcs12, _lib.PKCS12_free)
#
#         bio = _new_mem_buf()
#         _lib.i2d_PKCS12_bio(bio, pkcs12)
#         return _bio_to_string(bio)
#     self.export = export
#     return self
#
#
# def NetscapeSPKI():
#     """
#     A Netscape SPKI object.
#     """
#
#     def __init__():
#         spki = _lib.NETSCAPE_SPKI_new()
#         self._spki = _ffi.gc(spki, _lib.NETSCAPE_SPKI_free)
#     self = __init__()
#
#     def sign(pkey, digest):
#         """
#         Sign the certificate request with this key and digest type.
#
#         :param pkey: The private key to sign with.
#         :type pkey: :py:class:`PKey`
#
#         :param digest: The message digest to use.
#         :type digest: :py:class:`bytes`
#
#         :return: ``None``
#         """
#         if pkey._only_public:
#             fail(" ValueError(\"Key has only public part\")")
#
#         if not pkey._initialized:
#             fail(" ValueError(\"Key is uninitialized\")")
#
#         digest_obj = _lib.EVP_get_digestbyname(_byte_string(digest))
#         if digest_obj == _ffi.NULL:
#             fail(" ValueError(\"No such digest method\")")
#
#         sign_result = _lib.NETSCAPE_SPKI_sign(
#             self._spki, pkey._pkey, digest_obj
#         )
#         _openssl_assert(sign_result > 0)
#     self.sign = sign
#
#     def verify(key):
#         """
#         Verifies a signature on a certificate request.
#
#         :param PKey key: The public key that signature is supposedly from.
#
#         :return: ``True`` if the signature is correct.
#         :rtype: bool
#
#         :raises OpenSSL.crypto.Error: If the signature is invalid, or there was
#             a problem verifying the signature.
#         """
#         answer = _lib.NETSCAPE_SPKI_verify(self._spki, key._pkey)
#         if answer <= 0:
#             _raise_current_error()
#         return True
#     self.verify = verify
#
#     def b64_encode():
#         """
#         Generate a base64 encoded representation of this SPKI object.
#
#         :return: The base64 encoded string.
#         :rtype: :py:class:`bytes`
#         """
#         encoded = _lib.NETSCAPE_SPKI_b64_encode(self._spki)
#         result = _ffi.string(encoded)
#         _lib.OPENSSL_free(encoded)
#         return result
#     self.b64_encode = b64_encode
#
#     def get_pubkey():
#         """
#         Get the public key of this certificate.
#
#         :return: The public key.
#         :rtype: :py:class:`PKey`
#         """
#         pkey = PKey.__new__(PKey)
#         pkey._pkey = _lib.NETSCAPE_SPKI_get_pubkey(self._spki)
#         _openssl_assert(pkey._pkey != _ffi.NULL)
#         pkey._pkey = _ffi.gc(pkey._pkey, _lib.EVP_PKEY_free)
#         pkey._only_public = True
#         return pkey
#     self.get_pubkey = get_pubkey
#
#     def set_pubkey(pkey):
#         """
#         Set the public key of the certificate
#
#         :param pkey: The public key
#         :return: ``None``
#         """
#         set_result = _lib.NETSCAPE_SPKI_set_pubkey(self._spki, pkey._pkey)
#         _openssl_assert(set_result == 1)
#     self.set_pubkey = set_pubkey
#     return self
#
#
# def load_publickey(type, buffer):
#     """
#     Load a public key from a buffer.
#
#     :param type: The file type (one of :data:`FILETYPE_PEM`,
#         :data:`FILETYPE_ASN1`).
#     :param buffer: The buffer the key is stored in.
#     :type buffer: A Python string object, either unicode or bytestring.
#     :return: The PKey object.
#     :rtype: :class:`PKey`
#     """
#     if isinstance(buffer, _text_type):
#         buffer = buffer.encode("ascii")
#
#     bio = _new_mem_buf(buffer)
#
#     if type == FILETYPE_PEM:
#         evp_pkey = _lib.PEM_read_bio_PUBKEY(
#             bio, _ffi.NULL, _ffi.NULL, _ffi.NULL
#         )
#     elif type == FILETYPE_ASN1:
#         evp_pkey = _lib.d2i_PUBKEY_bio(bio, _ffi.NULL)
#     else:
#         fail(" ValueError(\"type argument must be FILETYPE_PEM or FILETYPE_ASN1\")")
#
#     if evp_pkey == _ffi.NULL:
#         _raise_current_error()
#
#     pkey = PKey.__new__(PKey)
#     pkey._pkey = _ffi.gc(evp_pkey, _lib.EVP_PKEY_free)
#     pkey._only_public = True
#     return pkey
#

def load_privatekey(type, buffer, passphrase=None):
    """
    Load a private key (PKey) from the string *buffer* encoded with the type
    *type*.

    :param type: The file type (one of FILETYPE_PEM, FILETYPE_ASN1)
    :param buffer: The buffer the key is stored in
    :param passphrase: (optional) if encrypted PEM format, this can be
                       either the passphrase to use, or a callback for
                       providing the passphrase.

    :return: The PKey object
    """
    if types.is_string(buffer):
        buffer = codecs.encode(buffer, encoding='ascii')

    if type == FILETYPE_PEM:
        evp_pkey = _JOpenSSL.OpenSSL.load_privatekey(buffer, passphrase)
    elif type == FILETYPE_ASN1:
        fail("Not supported")
    else:
        fail('ValueError: type argument must be FILETYPE_PEM or FILETYPE_ASN1')

    pkey = PKey(evp_pkey)
    return pkey


# def dump_certificate_request(type, req):
#     """
#     Dump the certificate request *req* into a buffer string encoded with the
#     type *type*.
#
#     :param type: The file type (one of FILETYPE_PEM, FILETYPE_ASN1)
#     :param req: The certificate request to dump
#     :return: The buffer with the dumped certificate request in
#     """
#     bio = _new_mem_buf()
#
#     if type == FILETYPE_PEM:
#         result_code = _lib.PEM_write_bio_X509_REQ(bio, req._req)
#     elif type == FILETYPE_ASN1:
#         result_code = _lib.i2d_X509_REQ_bio(bio, req._req)
#     elif type == FILETYPE_TEXT:
#         result_code = _lib.X509_REQ_print_ex(bio, req._req, 0, 0)
#     else:
#         fail(" ValueError(\n            \"type argument must be FILETYPE_PEM, FILETYPE_ASN1, or \"\n            \"FILETYPE_TEXT\"\n        )")
#
#     _openssl_assert(result_code != 0)
#
#     return _bio_to_string(bio)
#
#
# def load_certificate_request(type, buffer):
#     """
#     Load a certificate request (X509Req) from the string *buffer* encoded with
#     the type *type*.
#
#     :param type: The file type (one of FILETYPE_PEM, FILETYPE_ASN1)
#     :param buffer: The buffer the certificate request is stored in
#     :return: The X509Req object
#     """
#     if isinstance(buffer, _text_type):
#         buffer = buffer.encode("ascii")
#
#     bio = _new_mem_buf(buffer)
#
#     if type == FILETYPE_PEM:
#         req = _lib.PEM_read_bio_X509_REQ(bio, _ffi.NULL, _ffi.NULL, _ffi.NULL)
#     elif type == FILETYPE_ASN1:
#         req = _lib.d2i_X509_REQ_bio(bio, _ffi.NULL)
#     else:
#         fail(" ValueError(\"type argument must be FILETYPE_PEM or FILETYPE_ASN1\")")
#
#     _openssl_assert(req != _ffi.NULL)
#
#     x509req = X509Req.__new__(X509Req)
#     x509req._req = _ffi.gc(req, _lib.X509_REQ_free)
#     return x509req
#
#
# def sign(pkey, data, digest):
#     """
#     Sign a data string using the given key and message digest.
#
#     :param pkey: PKey to sign with
#     :param data: data to be signed
#     :param digest: message digest to use
#     :return: signature
#
#     .. versionadded:: 0.11
#     """
#     data = _text_to_bytes_and_warn("data", data)
#
#     digest_obj = _lib.EVP_get_digestbyname(_byte_string(digest))
#     if digest_obj == _ffi.NULL:
#         fail(" ValueError(\"No such digest method\")")
#
#     md_ctx = _lib.Cryptography_EVP_MD_CTX_new()
#     md_ctx = _ffi.gc(md_ctx, _lib.Cryptography_EVP_MD_CTX_free)
#
#     _lib.EVP_SignInit(md_ctx, digest_obj)
#     _lib.EVP_SignUpdate(md_ctx, data, len(data))
#
#     length = _lib.EVP_PKEY_size(pkey._pkey)
#     _openssl_assert(length > 0)
#     signature_buffer = _ffi.new("unsigned char[]", length)
#     signature_length = _ffi.new("unsigned int *")
#     final_result = _lib.EVP_SignFinal(
#         md_ctx, signature_buffer, signature_length, pkey._pkey
#     )
#     _openssl_assert(final_result == 1)
#
#     return _ffi.buffer(signature_buffer, signature_length[0])[:]
#
#
# def verify(cert, signature, data, digest):
#     """
#     Verify the signature for a data string.
#
#     :param cert: signing certificate (X509 object) corresponding to the
#         private key which generated the signature.
#     :param signature: signature returned by sign function
#     :param data: data to be verified
#     :param digest: message digest to use
#     :return: ``None`` if the signature is correct, raise exception otherwise.
#
#     .. versionadded:: 0.11
#     """
#     data = _text_to_bytes_and_warn("data", data)
#
#     digest_obj = _lib.EVP_get_digestbyname(_byte_string(digest))
#     if digest_obj == _ffi.NULL:
#         fail(" ValueError(\"No such digest method\")")
#
#     pkey = _lib.X509_get_pubkey(cert._x509)
#     _openssl_assert(pkey != _ffi.NULL)
#     pkey = _ffi.gc(pkey, _lib.EVP_PKEY_free)
#
#     md_ctx = _lib.Cryptography_EVP_MD_CTX_new()
#     md_ctx = _ffi.gc(md_ctx, _lib.Cryptography_EVP_MD_CTX_free)
#
#     _lib.EVP_VerifyInit(md_ctx, digest_obj)
#     _lib.EVP_VerifyUpdate(md_ctx, data, len(data))
#     verify_result = _lib.EVP_VerifyFinal(
#         md_ctx, signature, len(signature), pkey
#     )
#
#     if verify_result != 1:
#         _raise_current_error()
#
#
# def dump_crl(type, crl):
#     """
#     Dump a certificate revocation list to a buffer.
#
#     :param type: The file type (one of ``FILETYPE_PEM``, ``FILETYPE_ASN1``, or
#         ``FILETYPE_TEXT``).
#     :param CRL crl: The CRL to dump.
#
#     :return: The buffer with the CRL.
#     :rtype: bytes
#     """
#     bio = _new_mem_buf()
#
#     if type == FILETYPE_PEM:
#         ret = _lib.PEM_write_bio_X509_CRL(bio, crl._crl)
#     elif type == FILETYPE_ASN1:
#         ret = _lib.i2d_X509_CRL_bio(bio, crl._crl)
#     elif type == FILETYPE_TEXT:
#         ret = _lib.X509_CRL_print(bio, crl._crl)
#     else:
#         fail(" ValueError(\n            \"type argument must be FILETYPE_PEM, FILETYPE_ASN1, or \"\n            \"FILETYPE_TEXT\"\n        )")
#
#     _openssl_assert(ret == 1)
#     return _bio_to_string(bio)
#
#
# def load_crl(type, buffer):
#     """
#     Load Certificate Revocation List (CRL) data from a string *buffer*.
#     *buffer* encoded with the type *type*.
#
#     :param type: The file type (one of FILETYPE_PEM, FILETYPE_ASN1)
#     :param buffer: The buffer the CRL is stored in
#
#     :return: The PKey object
#     """
#     if isinstance(buffer, _text_type):
#         buffer = buffer.encode("ascii")
#
#     bio = _new_mem_buf(buffer)
#
#     if type == FILETYPE_PEM:
#         crl = _lib.PEM_read_bio_X509_CRL(bio, _ffi.NULL, _ffi.NULL, _ffi.NULL)
#     elif type == FILETYPE_ASN1:
#         crl = _lib.d2i_X509_CRL_bio(bio, _ffi.NULL)
#     else:
#         fail(" ValueError(\"type argument must be FILETYPE_PEM or FILETYPE_ASN1\")")
#
#     if crl == _ffi.NULL:
#         _raise_current_error()7
#
#     result = CRL.__new__(CRL)
#     result._crl = _ffi.gc(crl, _lib.X509_CRL_free)
#     return result
#
#
# def load_pkcs7_data(type, buffer):
#     """
#     Load pkcs7 data from the string *buffer* encoded with the type
#     *type*.
#
#     :param type: The file type (one of FILETYPE_PEM or FILETYPE_ASN1)
#     :param buffer: The buffer with the pkcs7 data.
#     :return: The PKCS7 object
#     """
#     if isinstance(buffer, _text_type):
#         buffer = buffer.encode("ascii")
#
#     bio = _new_mem_buf(buffer)
#
#     if type == FILETYPE_PEM:
#         pkcs7 = _lib.PEM_read_bio_PKCS7(bio, _ffi.NULL, _ffi.NULL, _ffi.NULL)
#     elif type == FILETYPE_ASN1:
#         pkcs7 = _lib.d2i_PKCS7_bio(bio, _ffi.NULL)
#     else:
#         fail(" ValueError(\"type argument must be FILETYPE_PEM or FILETYPE_ASN1\")")
#
#     if pkcs7 == _ffi.NULL:
#         _raise_current_error()
#
#     pypkcs7 = PKCS7.__new__(PKCS7)
#     pypkcs7._pkcs7 = _ffi.gc(pkcs7, _lib.PKCS7_free)
#     return pypkcs7
#
#
# load_pkcs7_data = utils.deprecated(
#     load_pkcs7_data,
#     __name__,
#     (
#         "PKCS#7 support in pyOpenSSL is deprecated. You should use the APIs "
#         "in cryptography."
#     ),
#     DeprecationWarning,
# )
#
#
# def load_pkcs12(buffer, passphrase=None):
#     """
#     Load pkcs12 data from the string *buffer*. If the pkcs12 structure is
#     encrypted, a *passphrase* must be included.  The MAC is always
#     checked and thus required.
#
#     See also the man page for the C function :py:func:`PKCS12_parse`.
#
#     :param buffer: The buffer the certificate is stored in
#     :param passphrase: (Optional) The password to decrypt the PKCS12 lump
#     :returns: The PKCS12 object
#     """
#     passphrase = _text_to_bytes_and_warn("passphrase", passphrase)
#
#     if isinstance(buffer, _text_type):
#         buffer = buffer.encode("ascii")
#
#     bio = _new_mem_buf(buffer)
#
#     # Use null passphrase if passphrase is None or empty string. With PKCS#12
#     # password based encryption no password and a zero length password are two
#     # different things, but OpenSSL implementation will try both to figure out
#     # which one works.
#     if not passphrase:
#         passphrase = _ffi.NULL
#
#     p12 = _lib.d2i_PKCS12_bio(bio, _ffi.NULL)
#     if p12 == _ffi.NULL:
#         _raise_current_error()
#     p12 = _ffi.gc(p12, _lib.PKCS12_free)
#
#     pkey = _ffi.new("EVP_PKEY**")
#     cert = _ffi.new("X509**")
#     cacerts = _ffi.new("Cryptography_STACK_OF_X509**")
#
#     parse_result = _lib.PKCS12_parse(p12, passphrase, pkey, cert, cacerts)
#     if not parse_result:
#         _raise_current_error()
#
#     cacerts = _ffi.gc(cacerts[0], _lib.sk_X509_free)
#
#     # openssl 1.0.0 sometimes leaves an X509_check_private_key error in the
#     # queue for no particular reason.  This error isn't interesting to anyone
#     # outside this function.  It's not even interesting to us.  Get rid of it.
#     try:
#         _raise_current_error()
#     except Error:
#         pass
#
#     if pkey[0] == _ffi.NULL:
#         pykey = None
#     else:
#         pykey = PKey.__new__(PKey)
#         pykey._pkey = _ffi.gc(pkey[0], _lib.EVP_PKEY_free)
#
#     if cert[0] == _ffi.NULL:
#         pycert = None
#         friendlyname = None
#     else:
#         pycert = X509._from_raw_x509_ptr(cert[0])
#
#         friendlyname_length = _ffi.new("int*")
#         friendlyname_buffer = _lib.X509_alias_get0(
#             cert[0], friendlyname_length
#         )
#         friendlyname = _ffi.buffer(
#             friendlyname_buffer, friendlyname_length[0]
#         )[:]
#         if friendlyname_buffer == _ffi.NULL:
#             friendlyname = None
#
#     pycacerts = []
#     for i in range(_lib.sk_X509_num(cacerts)):
#         x509 = _lib.sk_X509_value(cacerts, i)
#         pycacert = X509._from_raw_x509_ptr(x509)
#         pycacerts.append(pycacert)
#     if not pycacerts:
#         pycacerts = None
#
#     pkcs12 = PKCS12.__new__(PKCS12)
#     pkcs12._pkey = pykey
#     pkcs12._cert = pycert
#     pkcs12._cacerts = pycacerts
#     pkcs12._friendlyname = friendlyname
#     return pkcs12

crypto = larky.struct(
    FILETYPE_PEM=FILETYPE_PEM,
    FILETYPE_ASN1=FILETYPE_ASN1,
    FILETYPE_TEXT=FILETYPE_TEXT,
    load_privatekey=load_privatekey,
    dump_certificate=dump_certificate,
    PKey=PKey,
    X509=X509,
    X509Name=X509Name,
    X509Extension=X509Extension,
)