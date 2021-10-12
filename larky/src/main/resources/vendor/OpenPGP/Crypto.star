load("@stdlib//codecs", codecs="codecs")
load("@vendor//Crypto/Util/number", long_to_bytes="long_to_bytes", bytes_to_long="bytes_to_long")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
# load("@stdlib//hashlib", hashlib="hashlib", math="math", sys="sys", copy="copy", collections="collections")
load("@stdlib//struct", struct="struct")
load("@stdlib//types", types="types")
load("@vendor//Crypto/Random", Random="Random")
load("@stdlib//builtins", builtins="builtins")
# load("@vendor//Crypto/Cipher", DES3="DES3")
# load("@vendor//Crypto/Cipher", Blowfish="Blowfish")
load("@vendor//Crypto/Cipher/PKCS1_v1_5", PKCS1_v1_5_Cipher="PKCS1_v1_5_Cipher")
# load("@vendor//Crypto/Cipher", CAST="CAST")
load("@vendor//Crypto/Cipher/AES", AES="AES")
# load("@vendor//Crypto/Hash", SHA512="SHA512")
# load("@vendor//Crypto/Hash", MD5="MD5")
# load("@vendor//Crypto/Hash", SHA224="SHA224")
# load("@vendor//Crypto/Hash", SHA384="SHA384")
# load("@vendor//Crypto/Hash", RIPEMD="RIPEMD")
load("@vendor//Crypto/Hash/SHA", SHA="SHA")
# load("@vendor//Crypto/Hash", SHA256="SHA256")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
# load("@vendor//Crypto/PublicKey", DSA="DSA")
# load("@vendor//Crypto/Random", random="random")
# load("@vendor//Crypto/Signature", PKCS1_v1_5="PKCS1_v1_5")
load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@vendor//option/result", Error="Error")

pack = struct.pack
unpack = struct.unpack

__all__ = ['Wrapper']
def Wrapper(packet):
    """ A wrapper for using the classes from OpenPGP.py with PyCrypto """
    self = larky.mutablestruct(__name__='Wrapper', __class__=Wrapper)

    # def _parse_packet(packet):
    #     if types.is_instance(packet, OpenPGP.Packet) or types.is_instance(packet, OpenPGP.Message) or types.is_instance(packet, Crypto.PublicKey.RSA._RSAobj) or types.is_instance(packet, Crypto.PublicKey.DSA._DSAobj):
    #         return packet
    #     elif types.is_tuple(packet) or types.is_list(packet):
    #         if sys.version_info[0] == 2 and types.is_instance(packet[0], long) or types.is_int(packet[0]):
    #             data = []
    #             for i in packet:
    #                 data.append(long_to_bytes(i)) # OpenPGP likes bytes
    #         else:
    #             data = packet
    #         data = packet
    #         return OpenPGP.SecretKeyPacket(keydata=data, algorithm=1, version=3) # V3 for fingerprint with no timestamp
    #     else:
    #         return OpenPGP.Message.parse(packet)
    # self._parse_packet = _parse_packet

    def __init__(packet):
        # packet = self._parse_packet(packet)
        self._key = None
        self._message = self._key
        # get OpenPGP.LiteralDataPacket when init wrapper, get OpenPGP.SecretKeyPacket when from encrypt()
        print("init packet:", packet) 
        # if isinstance(packet, OpenPGP.PublicKeyPacket) or (hasattr(packet, '__getitem__') and isinstance(packet[0], OpenPGP.PublicKeyPacket)): 
        # If it's a key (other keys are subclasses of this one)
        if types.is_instance(packet, OpenPGP.SecretKeyPacket) or types.is_instance(packet, OpenPGP.PublicKeyPacket) or (hasattr(packet, '__getitem__') and types.is_instance(packet[0], OpenPGP.PublicKeyPacket)): 
        # If it's a key (other keys are subclasses of this one)
            self._key = packet
        else:
            self._message = packet
        return self
    self = __init__(packet)

    def get_cipher(algo):
        def cipher(m, ks, bs):
                def _cipher(k, iv):
                    return m.new(k, mode=AES.MODE_CFB,
                        IV=iv or b'\0'*bs,
                        segment_size=bs*8)
                return (_cipher, ks, bs)
            # return (lambda k: lambda iv:
            #         m.new(k, mode=AES.MODE_CFB,
            #             IV=iv or b'\0'*bs,
            #             segment_size=bs*8),
            #     ks, bs)
        self.cipher = cipher
        
        return cipher(AES, 32, 16)
    self.get_cipher = get_cipher
        # if algo == 2:
        #     return cipher(Crypto.Cipher.DES3, 24, 8)
        # elif algo == 3:
        #     return cipher(Crypto.Cipher.CAST, 16, 8)
        # elif algo == 4:
        #     return cipher(Crypto.Cipher.Blowfish, 16, 8)
        # elif algo == 7:
        #     return cipher(Crypto.Cipher.AES, 16, 16)
        # elif algo == 8:
        #     return cipher(Crypto.Cipher.AES, 24, 16)
        # elif algo == 9:
        #     return cipher(Crypto.Cipher.AES, 32, 16)

        # return (None,None,None) # Not supported

    def _block_pad_unpad(siz, bs, go):
        pad_amount = siz - (len(bs) % siz)
        return go(bs + b'\0'*pad_amount)[:-pad_amount]
    self._block_pad_unpad = _block_pad_unpad

    def encrypt(passphrases_and_keys, symmetric_algorithm=9):
        cipher, key_bytes, key_block_bytes = self.get_cipher(symmetric_algorithm)
        if not cipher:
            fail('Error("Exception: Unsupported cipher")')
        prefix = Random.new().read(key_block_bytes)
        prefix += prefix[-2:]

        key = Random.new().read(key_bytes)
        # session_cipher = cipher(key)(None)
        session_cipher = cipher(key, None)

        to_encrypt = prefix + self._message.to_bytes()
        mdc = OpenPGP.ModificationDetectionCodePacket(SHA.new(to_encrypt + b'\xD3\x14').digest())
        to_encrypt += mdc.to_bytes()

        encrypted = [OpenPGP.IntegrityProtectedDataPacket(self._block_pad_unpad(key_block_bytes, to_encrypt, lambda x: session_cipher.encrypt(x)))]

        # if not types.is_iterable(passphrases_and_keys) or hasattr(passphrases_and_keys, 'encode'):
        if not types.is_list(passphrases_and_keys) or hasattr(passphrases_and_keys, 'encode'):
            passphrases_and_keys = [passphrases_and_keys]

        for psswd in passphrases_and_keys:
            # should get object of <class 'OpenPGP.SecretKeyPacket'>
            if types.is_instance(psswd, OpenPGP.SecretKeyPacket) or types.is_instance(psswd, OpenPGP.PublicKeyPacket):
                if not psswd.key_algorithm in [1,2,3]:
                    return fail('Error("Exception: Only RSA keys are supported.")')
                # below should get object of OpenPGP.SecretKeyPacket 
                rsa = self.__class__(psswd).public_key()
                pkcs1 = PKCS1_v1_5_Cipher.new(rsa)
                esk = pkcs1.encrypt(pack('!B', symmetric_algorithm) + key + pack('!H', OpenPGP.checksum(key)))
                esk = pack('!H', OpenPGP.bitlength(esk)) + esk
                encrypted = [OpenPGP.AsymmetricSessionKeyPacket(psswd.key_algorithm, psswd.fingerprint(), esk)] + encrypted
            elif hasattr(psswd, 'encode'):
                psswd = codecs.encode(psswd, encoding='utf-8')
                s2k = OpenPGP.S2K(Random.new().read(10))
                packet_cipher = cipher(s2k.make_key(psswd, key_bytes))(None)
                esk = self._block_pad_unpad(key_block_bytes, pack('!B', symmetric_algorithm) + key, lambda x: packet_cipher.encrypt(x))
                encrypted = [OpenPGP.SymmetricSessionKeyPacket(s2k, esk, symmetric_algorithm)] + encrypted

        return OpenPGP.Message(encrypted)
    self.encrypt = encrypt

    def decrypt_packet(epacket, symmetric_algorithm, key):
        cipher, key_bytes, key_block_bytes = self.get_cipher(symmetric_algorithm)
        if not cipher:
            return None
        cipher = cipher(key)

        if types.is_instance(epacket, OpenPGP.IntegrityProtectedDataPacket):
            data = self._block_pad_unpad(key_block_bytes, epacket.data, lambda x: cipher(None).decrypt(x))
            prefix = data[0:key_block_bytes+2]
            mdc = data[-22:][2:]
            data = data[key_block_bytes+2:-22]

            # mkMDC = hashlib.sha1(prefix + data + b'\xd3\x14').digest()
            mkMDC = SHA.new(prefix + data + b'\xd3\x14').digest()
            if mdc != mkMDC:
                return False

            # try:
            #     return OpenPGP.Message.parse(data)
            # except:
            #     return None
            return OpenPGP.Message.parse(data)
        else:
            # No MDC means decrypt with resync
            edata = epacket.data[key_block_bytes+2:]
            data = self._block_pad_unpad(key_block_bytes, edata, lambda x: cipher(epacket.data[2:key_block_bytes+2]).decrypt(x))
            # try:
            #     return OpenPGP.Message.parse(data)
            # except:
            #     return None
            return OpenPGP.Message.parse(data)

        return None
    self.decrypt_packet = decrypt_packet

    def try_decrypt_session(key, edata):
        pkcs15 = PKCS1_v1_5_Cipher.new(key)
        data = pkcs15.decrypt(edata, Random.new().read(len(edata)))
        sk = data[1:len(data)-2]
        chk = unpack('!H', data[-2:])[0]

        sk_chk = 0
        for i in range(0, len(sk)):
            sk_chk = (sk_chk + ord(sk[i:i+1])) % 65536

        if sk_chk != chk:
            return None
        return (ord(data[0:1]), sk)
    self.try_decrypt_session = try_decrypt_session

    def decrypt(packet):
        # if types.is_instance(packet, list):
        #     packet = OpenPGP.Message(packet)
        # elif not types.is_instance(packet, OpenPGP.Message):
        #     packet = OpenPGP.Message.parse(packet)

        # if types.is_instance(packet, OpenPGP.SecretKeyPacket) or types.is_instance(packet, Crypto.PublicKey.RSA._RSAobj) or (hasattr(packet, '__getitem__') and types.is_instance(packet[0], OpenPGP.SecretKeyPacket)):
        #     keys = packet
        # else:
        keys = self._key
        self._message = packet

        if not keys or not self._message:
            return None # Missing some data

        if not types.is_instance(keys, RSA.RsaKey):
            keys = self.__class__(keys)

        for p in self._message:
            if types.is_instance(p, OpenPGP.AsymmetricSessionKeyPacket):
                if types.is_instance(keys, RSA.RsaKey):
                    sk = self.try_decrypt_session(keys, p.encrypted_data[2:])
                elif len(p.keyid.replace('0','')) < 1:
                    for k in keys.key:
                        sk = self.try_decrypt_session(self.convert_private_key(k), p.encyrpted_data[2:]);
                        if sk:
                            break
                else:
                    key = keys.private_key(p.keyid)
                    sk = self.try_decrypt_session(key, p.encrypted_data[2:])

                if not sk:
                    continue

                r = self.decrypt_packet(self.encrypted_data(), sk[0], sk[1])
                if r:
                    return r

        return None # Failed
    self.decrypt = decrypt

    def private_key(keyid=None):
        """ Get _RSAobj or _DSAobj for the public key """
        return self.convert_private_key(self.key(keyid))
    self.private_key = private_key

    def convert_private_key(packet):
        return self.convert_key(packet, True)
    self.convert_private_key = convert_private_key

    def encrypted_data():
        if not self._message:
            return None

        for p in self._message:
            # if types.is_instance(p, OpenPGP.EncryptedDataPacket):
            if types.is_instance(p, OpenPGP.AsymmetricSessionKeyPacket) or types.is_instance(p, OpenPGP.SymmetricSessionKeyPacket) or types.is_instance(p, OpenPGP.IntegrityProtectedDataPacket):
                return p

        return None
    self.encrypted_data = encrypted_data

    def key(keyid=None):
        if not self._key: # No key
            return None
        if types.is_instance(self._key, OpenPGP.Message):
            for p in self._key:
                if types.is_instance(p, OpenPGP.PublicKeyPacket):
                    if not keyid or p.fingerprint()[len(keyid)*-1:].upper() == keyid.upper():
                        return p
        return self._key
    self.key = key

    def public_key(keyid=None):
        """ Get _RSAobj or _DSAobj for the public key """
        return self.convert_public_key(self.key(keyid))
    self.public_key = public_key

    def convert_key(packet, private=False):
        # if types.is_instance(packet, Crypto.PublicKey.RSA._RSAobj) or types.is_instance(packet, Crypto.PublicKey.DSA._DSAobj):
        # if types.is_instance(packet, RSA._RSAobj):
        #     return packet
        # packet = self._parse_packet(packet)
        if types.is_instance(packet, OpenPGP.Message):
            packet = packet[0]

        if packet.key_algorithm_name() == 'DSA':
            fail('Error("Exception: DSA is not supported currently.")')
            # public = (Crypto.Util.number.bytes_to_long(packet.key['y']),
            #           Crypto.Util.number.bytes_to_long(packet.key['g']),
            #           Crypto.Util.number.bytes_to_long(packet.key['p']),
            #           Crypto.Util.number.bytes_to_long(packet.key['q']))
            # if private:
            #     private = (Crypto.Util.number.bytes_to_long(packet.key['x']),)
            #     return Crypto.PublicKey.DSA.construct(public + private)
            # else:
            #     return Crypto.PublicKey.DSA.construct(public)
        else: # RSA
            public = (bytes_to_long(packet.key['n']), bytes_to_long(packet.key['e']))
            if private:
                private =  (bytes_to_long(packet.key['d']),)
                if 'p' in packet.key: # Has optional parts
                    private += (bytes_to_long(packet.key['p']), bytes_to_long(packet.key['q']), bytes_to_long(packet.key['u']))
                return RSA.construct(public + private)
            else:
                return RSA.construct(public)
    self.convert_key = convert_key

    def convert_public_key(packet):
        return self.convert_key(packet, False)
    self.convert_public_key = convert_public_key

    return self


Crypto = larky.struct(
    Wrapper=Wrapper,
    __name__ = 'Wrapper',
)


    # def verifier(h, m, s):
    #     """ Used in implementation of verify """
    #     key = self.public_key(s.issuer())
    #     if not key or (s.key_algorithm_name() == 'DSA' and not types.is_instance(key, Crypto.PublicKey.DSA._DSAobj)):
    #         return False
    #     if s.key_algorithm_name() == 'DSA':
    #         dsaSig = (Crypto.Util.number.bytes_to_long(s.data[0]), Crypto.Util.number.bytes_to_long(s.data[1]))
    #         dsaLen = int(Crypto.Util.number.size(key.q) / 8)
    #         return key.verify(h.new(m).digest()[0:dsaLen], dsaSig)
    #     else: # RSA
    #         protocol = Crypto.Signature.PKCS1_v1_5.new(key)
    #         return protocol.verify(h.new(m), s.data[0])
    # self.verifier = verifier

    # def verify(packet):
    #     """ Pass a message to verify with this key, or a key (OpenPGP, _RSAobj, or _DSAobj)
    #         to check this message with
    #         Second optional parameter to specify which signature to verify (if there is more than one)
    #     """
    #     m = None
    #     packet = self._parse_packet(packet)
    #     if not self._message:
    #         m = packet
    #         verifier = self.verifier
    #     else:
    #         m = self._message
    #         verifier = self.__class__(packet).verifier

    #     hashes = {
    #         'MD5':       lambda m, s: verifier(Crypto.Hash.MD5, m, s),
    #         'RIPEMD160': lambda m, s: verifier(Crypto.Hash.RIPEMD, m, s),
    #         'SHA1':      lambda m, s: verifier(Crypto.Hash.SHA, m, s),
    #         'SHA224':    lambda m, s: verifier(Crypto.Hash.SHA224, m, s),
    #         'SHA256':    lambda m, s: verifier(Crypto.Hash.SHA256, m, s),
    #         'SHA384':    lambda m, s: verifier(Crypto.Hash.SHA384, m, s),
    #         'SHA512':    lambda m, s: verifier(Crypto.Hash.SHA512, m, s)
    #     }

    #     return m.verified_signatures({'RSA': hashes, 'DSA': hashes})
    # self.verify = verify

    # def sign(packet, hash='SHA256', keyid=None):
    #     if self._key and not types.is_instance(packet, OpenPGP.Packet) and not types.is_instance(packet, OpenPGP.Message):
    #         packet = OpenPGP.LiteralDataPacket(packet)
    #     else:
    #         packet = self._parse_packet(packet)

    #     if types.is_instance(packet, OpenPGP.SecretKeyPacket) or types.is_instance(packet, Crypto.PublicKey.RSA._RSAobj) or types.is_instance(packet, Crypto.PublicKey.DSA._DSAobj) or (hasattr(packet, '__getitem__') and types.is_instance(packet[0], OpenPGP.SecretKeyPacket)):
    #         key = packet
    #         message = self._message
    #     else:
    #         key = self._key
    #         message = packet

    #     if not key or not message:
    #         return None # Missing some data

    #     if types.is_instance(message, OpenPGP.Message):
    #         message = message.signature_and_data()[1]

    #     if not (types.is_instance(key, Crypto.PublicKey.RSA._RSAobj) or types.is_instance(packet, Crypto.PublicKey.DSA._DSAobj)):
    #         key = self.__class__(key)
    #         if not keyid:
    #             keyid = key.key().fingerprint()[-16:]
    #         key = key.private_key(keyid)

    #     key_algorithm = None
    #     if types.is_instance(key, Crypto.PublicKey.RSA._RSAobj):
    #         key_algorithm = 'RSA'
    #     elif types.is_instance(key, Crypto.PublicKey.DSA._DSAobj):
    #         key_algorithm = 'DSA'

    #     sig = OpenPGP.SignaturePacket(message, key_algorithm, hash.upper())

    #     if keyid:
    #         sig.hashed_subpackets.append(OpenPGP.SignaturePacket.IssuerPacket(keyid))

    #     def doDSA(h, m):
    #         return list(key.sign(h.new(m).digest()[0:int(Crypto.Util.number.size(key.q) / 8)],
    #             Random.random.StrongRandom().randint(1,key.q-1)))
    #     self.doDSA = doDSA

    #     sig.sign_data({'RSA': {
    #             'MD5':       lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.MD5.new(m))],
    #             'RIPEMD160': lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.RIPEMD.new(m))],
    #             'SHA1':      lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.SHA.new(m))],
    #             'SHA224':    lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.SHA224.new(m))],
    #             'SHA256':    lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.SHA256.new(m))],
    #             'SHA384':    lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.SHA384.new(m))],
    #             'SHA512':    lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.SHA512.new(m))],
    #         }, 'DSA': {
    #             'MD5':       lambda m: doDSA(Crypto.Hash.MD5, m),
    #             'RIPEMD160': lambda m: doDSA(Crypto.Hash.RIPEMD, m),
    #             'SHA1':      lambda m: doDSA(Crypto.Hash.SHA, m),
    #             'SHA224':    lambda m: doDSA(Crypto.Hash.SHA224, m),
    #             'SHA256':    lambda m: doDSA(Crypto.Hash.SHA256, m),
    #             'SHA384':    lambda m: doDSA(Crypto.Hash.SHA384, m),
    #             'SHA512':    lambda m: doDSA(Crypto.Hash.SHA512, m),
    #         }})

    #     return OpenPGP.Message([sig, message])
    # self.sign = sign

    # # TODO: merge this with the normal sign function
    # def sign_key_userid(packet, hash='SHA256', keyid=None):
    #     if types.is_instance(packet, list):
    #         packet = OpenPGP.Message(packet)
    #     elif not types.is_instance(packet, OpenPGP.Message):
    #         packet = OpenPGP.Message.parse(packet)

    #     key = self.key(keyid)
    #     if not key or not packet: # Missing some data
    #         return None

    #     if not keyid:
    #         keyid = key.fingerprint()[-16:]

    #     key = self.private_key(keyid)

    #     sig = None
    #     for p in packet:
    #         if types.is_instance(p, OpenPGP.SignaturePacket):
    #             sig = p
    #     if not sig:
    #         sig = OpenPGP.SignaturePacket(packet, 'RSA', hash.upper())
    #         sig.signature_type = 0x13
    #         sig.hashed_subpackets.append(OpenPGP.SignaturePacket.KeyFlagsPacket([0x01]))
    #         sig.hashed_subpackets.append(OpenPGP.SignaturePacket.IssuerPacket(keyid))
    #         packet.append(sig)

    #     def doDSA(h, m):
    #         return list(key.sign(h.new(m).digest()[0:int(Crypto.Util.number.size(key.q) / 8)],
    #             Random.random.StrongRandom().randint(1,key.q-1)))
    #     self.doDSA = doDSA

    #     sig.sign_data({'RSA': {
    #             'MD5':       lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.MD5.new(m))],
    #             'RIPEMD160': lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.RIPEMD.new(m))],
    #             'SHA1':      lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.SHA.new(m))],
    #             'SHA224':    lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.SHA224.new(m))],
    #             'SHA256':    lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.SHA256.new(m))],
    #             'SHA384':    lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.SHA384.new(m))],
    #             'SHA512':    lambda m: [Crypto.Signature.PKCS1_v1_5.new(key).sign(Crypto.Hash.SHA512.new(m))],
    #         }, 'DSA': {
    #             'MD5':       lambda m: doDSA(Crypto.Hash.MD5, m),
    #             'RIPEMD160': lambda m: doDSA(Crypto.Hash.RIPEMD, m),
    #             'SHA1':      lambda m: doDSA(Crypto.Hash.SHA, m),
    #             'SHA224':    lambda m: doDSA(Crypto.Hash.SHA224, m),
    #             'SHA256':    lambda m: doDSA(Crypto.Hash.SHA256, m),
    #             'SHA384':    lambda m: doDSA(Crypto.Hash.SHA384, m),
    #             'SHA512':    lambda m: doDSA(Crypto.Hash.SHA512, m),
    #         }})

    #     return packet
    # self.sign_key_userid = sign_key_userid

    # def decrypt_symmetric(passphrase):
    #     epacket = self.encrypted_data()
    #     if hasattr(passphrase, 'encode'):
    #         passphrase = codecs.encode(passphrase, encoding='utf-8')

    #     decrypted = None
    #     for p in self._message:
    #         if types.is_instance(p, OpenPGP.SymmetricSessionKeyPacket):
    #             if len(p.encrypted_data) > 0:
    #                 cipher, key_bytes, key_block_bytes = self.get_cipher(p.symmetric_algorithm)
    #                 if not cipher:
    #                     continue
    #                 cipher = cipher(p.s2k.make_key(passphrase, key_bytes))
    #                 data = self._block_pad_unpad(key_block_bytes, p.encrypted_data, lambda x: cipher(None).decrypt(x))

    #                 decrypted = self.decrypt_packet(epacket, ord(data[0:1]), data[1:])
    #             else:
    #                 cipher, key_bytes, key_block_bytes = self.get_cipher(p.symmetric_algorithm)
    #                 if not cipher:
    #                     continue

    #                 decrypted = self.decrypt_packet(epacket, p.symmetric_algorithm, p.s2k.make_key(passphrase, key_bytes))

    #             if decrypted:
    #                 return decrypted

    #     return None # If we get here, we failed
    # self.decrypt_symmetric = decrypt_symmetric

    # def decrypt_secret_key(passphrase):
    #     if hasattr(passphrase, 'encode'):
    #         passphrase = codecs.encode(passphrase, encoding='utf-8')

    #     packet = copy.copy(self._message or self._key) # Do not mutate original

    #     cipher, key_bytes, key_block_bytes = self.get_cipher(packet.symmetric_algorithm)
    #     cipher = cipher(packet.s2k.make_key(passphrase, key_bytes))
    #     cipher = cipher(packet.encrypted_data[:key_block_bytes])
    #     material = self._block_pad_unpad(key_block_bytes, packet.encrypted_data[key_block_bytes:], lambda x: cipher.decrypt(x))

    #     if packet.s2k_useage == 254:
    #         chk = material[-20:]
    #         material = material[:-20]
    #         if(chk != hashlib.sha1(material)):
    #             return None
    #     else:
    #         chk = unpack('!H', material[-2:])[0]
    #         material = material[:-2]
    #         if chk != OpenPGP.checksum(material):
    #             return None

    #     packet.s2k_usage = 0
    #     packet.symmetric_alorithm = 0
    #     packet.encrypted_data = None
    #     packet.input = OpenPGP.PushbackGenerator(OpenPGP._gen_one(material))
    #     packet.length = len(material)
    #     packet.key_from_input()
    #     packet.input = None
    #     return packet
    # self.decrypt_secret_key = decrypt_secret_key

    # def _block_pad_unpad(cls, siz, bs, go):
    #     pad_amount = siz - (len(bs) % siz)
    #     return go(bs + b'\0'*pad_amount)[:-pad_amount]
    # self._block_pad_unpad = _block_pad_unpad
    # _block_pad_unpad = _block_pad_unpad
    # return self
