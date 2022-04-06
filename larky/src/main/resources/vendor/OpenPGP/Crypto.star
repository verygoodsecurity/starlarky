load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//collections", collections="collections")
load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//larky", larky="larky")
load("@stdlib//math", math="math")
load("@stdlib//struct", pack="pack", unpack="unpack")
load("@stdlib//types", types="types")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Cipher/CAST", CAST="CAST")
load("@vendor//Crypto/Cipher/PKCS1_v1_5", PKCS1_v1_5_Cipher="PKCS1_v1_5_Cipher")
load("@vendor//Crypto/Cipher/DES3", DES3="DES3")
load("@vendor//Crypto/Cipher/Blowfish", Blowfish="Blowfish")
load("@vendor//Crypto/Hash", Hash="Hash")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//Crypto/PublicKey/DSA", DSA="DSA")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Signature", Signature="Signature")
load("@vendor//Crypto/Util/number", number="number")
load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@vendor//option/result", Result="Result", Error="Error")


__all__ = ["Wrapper"]


def _Wrapper():
    """A wrapper for using the classes from OpenPGP.py with PyCrypto"""
    cls = larky.mutablestruct(__name__='Wrapper')

    def __new__(packet):
        self = larky.mutablestruct(__name__='Wrapper', __class__=cls)

        def __init__(packet):
            packet = cls._parse_packet(packet)
            self._key = None
            self._message = self._key
            if builtins.isinstance(packet, OpenPGP.PublicKeyPacket) or (
                hasattr(packet, "__getitem__")
                and builtins.isinstance(packet[0], OpenPGP.PublicKeyPacket)
            ):  # If it's a key (other keys are subclasses of this one)
                self._key = packet
            else:
                self._message = packet
            return self
        self = __init__(packet)

        def key(keyid=None):
            if not self._key:  # No key
                return None
            if builtins.isinstance(self._key, OpenPGP.Message):
                for p in self._key:
                    if builtins.isinstance(p, OpenPGP.PublicKeyPacket):
                        if (
                            not keyid
                            or p.fingerprint()[len(keyid) * -1 :].upper() == keyid.upper()
                        ):
                            return p
            return self._key
        self.key = key

        def public_key(keyid=None):
            """Get RsaKey or DsaKey for the public key"""
            return cls.convert_public_key(self.key(keyid))
        self.public_key = public_key

        def private_key(keyid=None):
            """Get RsaKey or DsaKey for the public key"""
            return cls.convert_private_key(self.key(keyid))
        self.private_key = private_key

        def encrypted_data():
            if not self._message:
                return None

            for p in self._message:
                if builtins.isinstance(p, OpenPGP.EncryptedDataPacket):
                    return p

            return None
        self.encrypted_data = encrypted_data

        def verifier(h, m, s):
            """Used in implementation of verify"""
            key = self.public_key(s.issuer())
            if not key or (
                s.key_algorithm_name() == "DSA"
                and not builtins.isinstance(key, DSA.DsaKey)
            ):
                return False
            if s.key_algorithm_name() == "DSA":
                if len(s.data) == 1:
                    signature = s.data[0]
                elif len(s.data) == 2:
                    signature = s.data[0] + s.data[1]
                else:
                    fail("ValueError: " + "Unexpected length in s.data %s!" % len(s.data))

                verifier = Signature.DSS.new(key, 'fips-186-3')
                rval = Result.Ok(verifier.verify).map(lambda x: x(h.new(m), signature))
                if rval.is_ok:
                    # verifier.verify(h.new(m), signature)
                    return True
                # except ValueError as e:
                else:
                    print("signature verification failed!")
                    return False
            else:  # RSA
                protocol = Signature.PKCS1_v1_5.new(key)
                return protocol.verify(h.new(m), s.data[0])
        self.verifier = verifier

        def verify(packet):
            """Pass a message to verify with this key, or a key (OpenPGP, RsaKey, or DsaKey)
            to check this message with
            Second optional parameter to specify which signature to verify (if there is more than one)
            """
            packet = cls._parse_packet(packet)
            if not self._message:
                m = packet
                verifier = self.verifier
            else:
                m = self._message
                verifier = self.__class__(packet).verifier

            hashes = {
                "MD5": lambda m, s: verifier(Hash.MD5, m, s),
                "RIPEMD160": lambda m, s: verifier(Hash.RIPEMD, m, s),
                "SHA1": lambda m, s: verifier(Hash.SHA1, m, s),
                "SHA224": lambda m, s: verifier(Hash.SHA224, m, s),
                "SHA256": lambda m, s: verifier(Hash.SHA256, m, s),
                "SHA384": lambda m, s: verifier(Hash.SHA384, m, s),
                "SHA512": lambda m, s: verifier(Hash.SHA512, m, s),
            }

            return m.verified_signatures({"RSA": hashes, "DSA": hashes})
        self.verify = verify

        def sign(packet, hash="SHA256", keyid=None):
            if (
                self._key
                and not builtins.isinstance(packet, OpenPGP.Packet)
                and not builtins.isinstance(packet, OpenPGP.Message)
            ):
                packet = OpenPGP.LiteralDataPacket(packet)
            else:
                packet = cls._parse_packet(packet)

            if (
                builtins.isinstance(packet, OpenPGP.SecretKeyPacket)
                or builtins.isinstance(packet, RSA.RsaKey)
                or builtins.isinstance(packet, DSA.DsaKey)
                or (
                    hasattr(packet, "__getitem__")
                    and builtins.isinstance(packet[0], OpenPGP.SecretKeyPacket)
                )
            ):
                key = packet
                message = self._message
            else:
                key = self._key
                message = packet

            if not key or not message:
                return None  # Missing some data

            if builtins.isinstance(message, OpenPGP.Message):
                message = message.signature_and_data()[1]

            if not (
                builtins.isinstance(key, RSA.RsaKey)
                or builtins.isinstance(packet, DSA.DsaKey)
            ):
                key = self.__class__(key)
                if not keyid:
                    keyid = key.key().fingerprint()[-16:]
                key = key.private_key(keyid)

            key_algorithm = None
            if builtins.isinstance(key, RSA.RsaKey):
                key_algorithm = "RSA"
            elif builtins.isinstance(key, DSA.DsaKey):
                key_algorithm = "DSA"

            sig = OpenPGP.SignaturePacket(message, key_algorithm, hash.upper())

            if keyid:
                sig.hashed_subpackets.append(OpenPGP.SignaturePacket.IssuerPacket(keyid))

            def doDSA(h, m):
                signer = Signature.DSS.new(key, 'fips-186-3')
                signature = signer.sign(h.new(m))
                return [signature]
            self.doDSA = doDSA

            sig.sign_data(
                {
                    "RSA": {
                        "MD5": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.MD5.new(m)
                            )
                        ],
                        "RIPEMD160": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.RIPEMD.new(m)
                            )
                        ],
                        "SHA1": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.SHA1.new(m)
                            )
                        ],
                        "SHA224": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.SHA224.new(m)
                            )
                        ],
                        "SHA256": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.SHA256.new(m)
                            )
                        ],
                        "SHA384": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.SHA384.new(m)
                            )
                        ],
                        "SHA512": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.SHA512.new(m)
                            )
                        ],
                    },
                    "DSA": {
                        "MD5": lambda m: doDSA(Hash.MD5, m),
                        "RIPEMD160": lambda m: doDSA(Hash.RIPEMD, m),
                        "SHA1": lambda m: doDSA(Hash.SHA1, m),
                        "SHA224": lambda m: doDSA(Hash.SHA224, m),
                        "SHA256": lambda m: doDSA(Hash.SHA256, m),
                        "SHA384": lambda m: doDSA(Hash.SHA384, m),
                        "SHA512": lambda m: doDSA(Hash.SHA512, m),
                    },
                }
            )

            return OpenPGP.Message([sig, message])
        self.sign = sign

        # TODO: merge this with the normal sign function
        def sign_key_userid(packet, hash="SHA256", keyid=None):
            if builtins.isinstance(packet, list):
                packet = OpenPGP.Message(packet)
            elif not builtins.isinstance(packet, OpenPGP.Message):
                packet = OpenPGP.Message.parse(packet)

            key = self.key(keyid)
            if not key or not packet:  # Missing some data
                return None

            if not keyid:
                keyid = key.fingerprint()[-16:]

            key = self.private_key(keyid)

            sig = None
            for p in packet:
                if builtins.isinstance(p, OpenPGP.SignaturePacket):
                    sig = p
            if not sig:
                sig = OpenPGP.SignaturePacket(packet, "RSA", hash.upper())
                sig.signature_type = 0x13
                sig.hashed_subpackets.append(OpenPGP.SignaturePacket.KeyFlagsPacket([0x01]))
                sig.hashed_subpackets.append(OpenPGP.SignaturePacket.IssuerPacket(keyid))
                packet.append(sig)

            def doDSA(h, m):
                return list(
                    key.sign(
                        h.new(m).digest()[0 : int(number.size(key.q) / 8)],
                        Random.random.randint(1, key.q - 1),
                    )
                )
            self.doDSA = doDSA

            sig.sign_data(
                {
                    "RSA": {
                        "MD5": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.MD5.new(m)
                            )
                        ],
                        "RIPEMD160": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.RIPEMD.new(m)
                            )
                        ],
                        "SHA1": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.SHA1.new(m)
                            )
                        ],
                        "SHA224": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.SHA224.new(m)
                            )
                        ],
                        "SHA256": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.SHA256.new(m)
                            )
                        ],
                        "SHA384": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.SHA384.new(m)
                            )
                        ],
                        "SHA512": lambda m: [
                            Signature.PKCS1_v1_5.new(key).sign(
                                Hash.SHA512.new(m)
                            )
                        ],
                    },
                    "DSA": {
                        "MD5": lambda m: doDSA(Hash.MD5, m),
                        "RIPEMD160": lambda m: doDSA(Hash.RIPEMD, m),
                        "SHA1": lambda m: doDSA(Hash.SHA1, m),
                        "SHA224": lambda m: doDSA(Hash.SHA224, m),
                        "SHA256": lambda m: doDSA(Hash.SHA256, m),
                        "SHA384": lambda m: doDSA(Hash.SHA384, m),
                        "SHA512": lambda m: doDSA(Hash.SHA512, m),
                    },
                }
            )

            return packet
        self.sign_key_userid = sign_key_userid

        def decrypt(packet):
            if builtins.isinstance(packet, list):
                packet = OpenPGP.Message(packet)
            elif not builtins.isinstance(packet, OpenPGP.Message):
                packet = OpenPGP.Message.parse(packet)

            print(packet[0])
            if (
                builtins.isinstance(packet, OpenPGP.SecretKeyPacket)
                or builtins.isinstance(packet, RSA.RsaKey)
                or (
                    hasattr(packet, "__getitem__")
                    and builtins.isinstance(packet[0], OpenPGP.SecretKeyPacket)
                )
            ):
                keys = packet
            else:
                keys = self._key
                self._message = packet

            if not keys or not self._message:
                fail("Missing data: neither keys or message is set")

            print(keys)
            print(self._message)
            print("** here **")
            if not builtins.isinstance(keys, RSA.RsaKey):
                keys = self.__class__(keys)

            for p in self._message:
                if builtins.isinstance(p, OpenPGP.AsymmetricSessionKeyPacket):
                    if builtins.isinstance(keys, RSA.RsaKey):
                        sk = cls.try_decrypt_session(keys, p.encrypted_data[2:])
                    elif len(p.keyid.replace("0", "")) < 1:
                        for k in keys.key:
                            sk = cls.try_decrypt_session(
                                self.convert_private_key(k), p.encyrpted_data[2:]
                            )
                            if sk:
                                break
                    else:
                        key = keys.private_key(p.keyid)
                        sk = cls.try_decrypt_session(key, p.encrypted_data[2:])

                    if not sk:
                        continue

                    r = cls.decrypt_packet(self.encrypted_data(), sk[0], sk[1])
                    if r:
                        return r

            return None  # Failed
        self.decrypt = decrypt

        def encrypt(passphrases_and_keys, symmetric_algorithm=9):
            cipher, key_bytes, key_block_bytes = cls.get_cipher(symmetric_algorithm)
            if not cipher:
                fail("Exception: Unsupported cipher")
            prefix = Random.new().read(key_block_bytes)
            prefix += prefix[-2:]

            key = Random.new().read(key_bytes)
            session_cipher = cipher(key)(None)

            to_encrypt = prefix + self._message.to_bytes()
            mdc = OpenPGP.ModificationDetectionCodePacket(
                Hash.SHA1.new(to_encrypt + b"\xD3\x14").digest()
            )
            to_encrypt += mdc.to_bytes()

            encrypted = [
                OpenPGP.IntegrityProtectedDataPacket(
                    cls._block_pad_unpad(
                        key_block_bytes, to_encrypt, lambda x: session_cipher.encrypt(x)
                    )
                )
            ]

            if not types.is_iterable(passphrases_and_keys) or types.is_string(passphrases_and_keys):
                passphrases_and_keys = [passphrases_and_keys]

            for psswd in passphrases_and_keys:
                print(psswd)
                if builtins.isinstance(psswd, OpenPGP.PublicKeyPacket):
                    print("psswd isinstance OpenPGP.PublicKeyPacket")
                    if not psswd.key_algorithm in [1, 2, 3]:
                        fail("Exception: Only RSA keys are supported.")
                    print(self.__class__)
                    rsa = self.__class__(psswd).public_key()
                    pkcs1 = PKCS1_v1_5_Cipher.new(rsa)
                    esk = pkcs1.encrypt(
                        pack("!B", symmetric_algorithm)
                        + key
                        + pack("!H", OpenPGP.checksum(key))
                    )
                    esk = pack("!H", OpenPGP.bitlength(esk)) + esk
                    encrypted = [
                        OpenPGP.AsymmetricSessionKeyPacket(
                            psswd.key_algorithm, psswd.fingerprint(), esk
                        )
                    ] + encrypted
                elif types.is_string(psswd):
                    psswd = codecs.encode(psswd, encoding="utf-8")
                    s2k = OpenPGP.S2K(Random.new().read(10))
                    packet_cipher = cipher(s2k.make_key(psswd, key_bytes))(None)
                    esk = cls._block_pad_unpad(
                        key_block_bytes,
                        pack("!B", symmetric_algorithm) + key,
                        lambda x: packet_cipher.encrypt(x),
                    )
                    encrypted = [
                        OpenPGP.SymmetricSessionKeyPacket(s2k, esk, symmetric_algorithm)
                    ] + encrypted

            return OpenPGP.Message(encrypted)
        self.encrypt = encrypt

        def decrypt_symmetric(passphrase):
            epacket = self.encrypted_data()
            if types.is_string(passphrase):
                passphrase = bytes(passphrase, encoding="utf-8")

            decrypted = None
            for p in self._message:
                if builtins.isinstance(p, OpenPGP.SymmetricSessionKeyPacket):
                    if len(p.encrypted_data) > 0:
                        cipher, key_bytes, key_block_bytes = cls.get_cipher(
                            p.symmetric_algorithm
                        )
                        if not cipher:
                            continue
                        cipher = cipher(p.s2k.make_key(passphrase, key_bytes))
                        data = cls._block_pad_unpad(
                            key_block_bytes,
                            p.encrypted_data,
                            lambda x: cipher(None).decrypt(x),
                        )

                        decrypted = cls.decrypt_packet(epacket, ord(data[0:1]), data[1:])
                    else:
                        cipher, key_bytes, key_block_bytes = cls.get_cipher(
                            p.symmetric_algorithm
                        )
                        if not cipher:
                            continue

                        decrypted = cls.decrypt_packet(
                            epacket,
                            p.symmetric_algorithm,
                            p.s2k.make_key(passphrase, key_bytes),
                        )

                    if decrypted:
                        return decrypted

            fail("Decryption failed!")
        self.decrypt_symmetric = decrypt_symmetric

        def decrypt_secret_key(passphrase):
            if hasattr(passphrase, "encode"):
                passphrase = codecs.encode(passphrase, encoding="utf-8")

            # packet = copy.copy(self._message or self._key)  # Do not mutate original
            packet = larky.mutablestruct(**((self._message or self._key).__dict__))

            cipher, key_bytes, key_block_bytes = cls.get_cipher(packet.symmetric_algorithm)
            cipher = cipher(packet.s2k.make_key(passphrase, key_bytes))
            cipher = cipher(packet.encrypted_data[:key_block_bytes])
            material = cls._block_pad_unpad(
                key_block_bytes,
                packet.encrypted_data[key_block_bytes:],
                lambda x: cipher.decrypt(x),
            )

            if packet.s2k_useage == 254:
                chk = material[-20:]
                material = material[:-20]
                if chk != hashlib.sha1(material):
                    return None
            else:
                chk = unpack("!H", material[-2:])[0]
                material = material[:-2]
                if chk != OpenPGP.checksum(material):
                    return None

            packet.s2k_usage = 0
            packet.symmetric_alorithm = 0
            packet.encrypted_data = None
            packet.input = OpenPGP.PushbackGenerator(OpenPGP._gen_one(material))
            packet.length = len(material)
            packet.key_from_input()
            packet.input = None
            return packet
        self.decrypt_secret_key = decrypt_secret_key
        return self
    cls.__call__ = __new__

    def try_decrypt_session(key, edata):
        pkcs15 = PKCS1_v1_5_Cipher.new(key)
        data = pkcs15.decrypt(edata, Random.new().read(len(edata)))
        sk = data[1 : len(data) - 2]
        chk = unpack("!H", data[-2:])[0]

        sk_chk = 0
        for i in range(0, len(sk)):
            sk_chk = (sk_chk + ord(sk[i : i + 1])) % 65536

        if sk_chk != chk:
            return None
        return (ord(data[0:1]), sk)
    cls.try_decrypt_session = try_decrypt_session

    def decrypt_packet(epacket, symmetric_algorithm, key):
        cipher, key_bytes, key_block_bytes = cls.get_cipher(symmetric_algorithm)
        if not cipher:
            return None
        cipher = cipher(key)

        if builtins.isinstance(epacket, OpenPGP.IntegrityProtectedDataPacket):
            data = cls._block_pad_unpad(
                key_block_bytes, epacket.data, lambda x: cipher(None).decrypt(x)
            )
            prefix = data[0 : key_block_bytes + 2]
            mdc = data[-22:][2:]
            data = data[key_block_bytes + 2 : -22]

            mkMDC = hashlib.sha1(prefix + data + b"\xd3\x14").digest()
            if mdc != mkMDC:
                return False

            rval = Result.Ok(data).map(OpenPGP.Message.parse)
            if rval.is_ok:
                return rval.unwrap()
                # return OpenPGP.Message.parse(data)
            # except:
            else:
                print("Could not parse data in decrypt_packet!")
        else:
            # No MDC means decrypt with resync
            edata = epacket.data[key_block_bytes + 2 :]
            data = cls._block_pad_unpad(
                key_block_bytes,
                edata,
                lambda x: cipher(epacket.data[2 : key_block_bytes + 2]).decrypt(x),
            )
            rval = Result.Ok(data).map(OpenPGP.Message.parse)
            if rval.is_ok:
                return rval.unwrap()
                # return OpenPGP.Message.parse(data)
            # except:
            else:
                print("Could not parse data in decrypt_packet!")

        return None
    cls.decrypt_packet = decrypt_packet

    def _parse_packet(packet):
        if (
            builtins.isinstance(packet, OpenPGP.Packet)
            or builtins.isinstance(packet, OpenPGP.Message)
            or builtins.isinstance(packet, RSA.RsaKey)
            or builtins.isinstance(packet, DSA.DsaKey)
        ):
            return packet
        elif builtins.isinstance(packet, tuple) or builtins.isinstance(packet, list):
            if builtins.isinstance(packet[0], int):
                data = []
                for i in packet:
                    data.append(
                        number.long_to_bytes(i)
                    )  # OpenPGP likes bytes
            else:
                data = packet
            return OpenPGP.SecretKeyPacket(
                keydata=data, algorithm=1, version=3
            )  # V3 for fingerprint with no timestamp
        else:
            return OpenPGP.Message.parse(packet)
    cls._parse_packet = _parse_packet

    def get_cipher(algo):
        def cipher(m, ks, bs):
            return (
                lambda k: lambda iv: m.new(
                    k,
                    mode=m.MODE_CFB,
                    IV=iv or b"\0" * bs,
                    segment_size=bs * 8,
                ),
                ks,
                bs,
            )

        if algo == 2:
            return cipher(DES3, 24, 8)
        elif algo == 3:
            return cipher(CAST, 16, 8)
        elif algo == 4:
            return cipher(Blowfish, 16, 8)
        elif algo == 7:
            return cipher(AES, 16, 16)
        elif algo == 8:
            return cipher(AES, 24, 16)
        elif algo == 9:
            return cipher(AES, 32, 16)

        return (None, None, None)  # Not supported
    cls.get_cipher = get_cipher

    def convert_key(packet, private=False):
        if builtins.isinstance(packet, RSA.RsaKey) or builtins.isinstance(
            packet, DSA.DsaKey
        ):
            return packet
        packet = cls._parse_packet(packet)
        if builtins.isinstance(packet, OpenPGP.Message):
            packet = packet[0]

        if packet.key_algorithm_name() == "DSA":
            public = (
                number.bytes_to_long(packet.key["y"]),
                number.bytes_to_long(packet.key["g"]),
                number.bytes_to_long(packet.key["p"]),
                number.bytes_to_long(packet.key["q"]),
            )
            if private:
                private = (number.bytes_to_long(packet.key["x"]),)
                return DSA.construct(public + private)
            else:
                return DSA.construct(public)
        else:  # RSA
            public = (
                number.bytes_to_long(packet.key["n"]),
                number.bytes_to_long(packet.key["e"]),
            )
            if private:
                private = (number.bytes_to_long(packet.key["d"]),)
                if "p" in packet.key:  # Has optional parts
                    private += (
                        number.bytes_to_long(packet.key["p"]),
                        number.bytes_to_long(packet.key["q"]),
                        number.bytes_to_long(packet.key["u"]),
                    )
                return RSA.construct(public + private)
            else:
                return RSA.construct(public)
    cls.convert_key = convert_key

    def convert_public_key(packet):
        return cls.convert_key(packet, False)
    cls.convert_public_key = convert_public_key

    def convert_private_key(packet):
        return cls.convert_key(packet, True)
    cls.convert_private_key = convert_private_key

    def _block_pad_unpad(siz, bs, go):
        pad_amount = siz - (len(bs) % siz)
        return go(bs + b"\0" * pad_amount)[:-pad_amount]
    cls._block_pad_unpad = _block_pad_unpad
    return cls


Wrapper = _Wrapper()

Crypto = larky.struct(
    Wrapper=Wrapper,
    __name__ = 'Crypto',
)