# Vendored and ported upstream code

Most of `vendor/` and part of `stdlib/` are hand ports of Python libraries
(and a few Starlark/Bazel files) into Larky. They keep upstream's API,
docstrings and test vectors on purpose, so scripts written against the Python
library behave the same. That only stays true if we know which upstream state
each file came from: to compare behavior, to pick up an upstream fix, or to
answer a licensing question. This file records that state. Each ported `.star`
file carries a one-line header pointing here:

    # Ported from <project> <version> (<git ref>): <upstream path>. See larky/src/main/resources/VENDORED.md.

Few of these ports recorded which upstream version they came from. The pins
below were reconstructed afterwards by comparing each Larky file, as first added,
with upstream's releases and history (normalized line sets, and the lines that
only some releases have), together with PR descriptions and comments in the
files. `vendor/jose` was pinned the same way on its own branch (see Notes).

## Confidence

- **exact**: the Larky file matches one specific upstream file state. If
  several releases ship that same file, the version column shows the range and
  the pinned release.
- **likely**: the evidence limits the upstream state to a range of releases
  or commits. The pin is the release the evidence favors, or else the one
  current when the port landed.
- **unknown**: the upstream file is known, but its content does not tell
  versions apart (or there is no content). The pin is a guess, normally the
  release current when the port landed.
- **vgs-original**: written for Larky. It may copy an API's names, some
  docstrings or a comment, but there is no upstream file to pin. These are
  listed in their own section and carry no header.

"A-B; pinned to C" in the version column means the content is consistent with
A through B and the file header names C.

## Updating an entry

- Re-syncing a file from upstream: update its row (version, git ref, upstream
  path, confidence) and the `# Ported from` header in the file. If only some
  files of a package move to the new version, give them their own row and say
  so under Notes. Keep "First added" as it is.
- Porting a new file: add a row and the header. Use a commit SHA or tag for
  the git ref, not a branch name.
- Writing a new Larky-only module: add it to the VGS-original list.
- `vendor/jose` and `vendor_tests/jose` headers are maintained in those files.

## vendor/

Paths are relative to `larky/src/main/resources`.

| Larky path(s) | Upstream | Version | Git ref | Upstream path(s) | License | Confidence | First added | Evidence |
|---|---|---|---|---|---|---|---|---|
| `vendor/Crypto/Cipher/{__init__,_EKSBlowfish,_mode_cbc,_mode_ccm,_mode_cfb,_mode_ctr,_mode_eax,_mode_ecb,_mode_gcm,_mode_ocb,_mode_ofb,_mode_openpgp,_mode_siv,AES,Blowfish,CAST,ChaCha20,ChaCha20_Poly1305,DES,DES3,Salsa20}.star`<br>`vendor/Crypto/Hash/{__init__,BLAKE2s,CMAC,HMAC,keccak,MD5,Poly1305,SHA,SHA1,SHA224,SHA256,SHA384,SHA3_256,SHA512}.star`<br>`vendor/Crypto/IO/{__init__,PEM}.star`<br>`vendor/Crypto/Math/__init__.star`<br>`vendor/Crypto/Protocol/{__init__,KDF,SecretSharing}.star`<br>`vendor/Crypto/PublicKey/{__init__,_openssh,ElGamal,RSA}.star`<br>`vendor/Crypto/Random/{__init__,random}.star`<br>`vendor/Crypto/Signature/{__init__,pkcs1_15,PKCS1_PSS,PKCS1_v1_5,pss}.star`<br>`vendor/Crypto/Util/{__init__,asn1,Counter,Padding,py3compat,RFC1751,strxor}.star`<br>`vendor/Crypto/__init__.star` | [pycryptodome](https://github.com/Legrandin/pycryptodome) | 3.10.0-3.10.1; pinned to **3.10.1** | [v3.10.1](https://github.com/Legrandin/pycryptodome/tree/339259c3e3bb9c801f24c1ae5a90507ef5078465) | lib/Crypto/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | likely | [#90](https://github.com/verygoodsecurity/starlarky/pull/90), [#93](https://github.com/verygoodsecurity/starlarky/pull/93), [#95](https://github.com/verygoodsecurity/starlarky/pull/95) (2021-03-25..04-05) | lib/Crypto is identical in v3.10.0 and v3.10.1 except the version; Util/Counter, Util/Padding, PublicKey/RSA match only >= v3.10.0; no post-v3.10.1 commit present at first add. |
| `vendor/Crypto/Cipher/{PKCS1_OAEP,PKCS1_v1_5}.star`<br>`vendor/Crypto/Util/number.star` | [pycryptodome](https://github.com/Legrandin/pycryptodome) | **3.10.4** | [v3.10.4](https://github.com/Legrandin/pycryptodome/tree/f8d40a60fdf1bc4336864b90fc85df8e312dcc33) | lib/Crypto/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | likely | [#93](https://github.com/verygoodsecurity/starlarky/pull/93) (re-synced in [#189](https://github.com/verygoodsecurity/starlarky/pull/189)) | Has the v3.10.4 OAEP fix 5dace638, PKCS#1 v1.5 hardening 85305493 and long_to_bytes fix f8d40a60; lacks v3.11.0 changes. |
| `vendor/Crypto/PublicKey/ECC.star`<br>`vendor/Crypto/IO/_PBES.star`<br>`vendor/Crypto/Cipher/{ARC2,ARC4}.star` | [pycryptodome](https://github.com/Legrandin/pycryptodome) | 3.10.1-3.11.0; pinned to **3.10.1** | [v3.10.1](https://github.com/Legrandin/pycryptodome/tree/339259c3e3bb9c801f24c1ae5a90507ef5078465) | lib/Crypto/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | likely | [#93](https://github.com/verygoodsecurity/starlarky/pull/93) (rewritten in [#173](https://github.com/verygoodsecurity/starlarky/pull/173)) | Upstream files identical in v3.10.1 and v3.11.0; ECC.star lacks the v3.12.0 ECC.py rewrite. |
| `vendor/Crypto/Hash/{BLAKE2b,MD2,MD4,RIPEMD,RIPEMD160,SHA3_224,SHA3_384,SHA3_512,SHAKE128,SHAKE256}.star` | [pycryptodome](https://github.com/Legrandin/pycryptodome) | 3.10.4-3.12.0; pinned to **3.12.0** | [v3.12.0](https://github.com/Legrandin/pycryptodome/tree/d8edf1a6a70d3a65dcda18ee24d96161525f2825) | lib/Crypto/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | likely | [#93](https://github.com/verygoodsecurity/starlarky/pull/93) (expanded in [#232](https://github.com/verygoodsecurity/starlarky/pull/232), [#234](https://github.com/verygoodsecurity/starlarky/pull/234), [#235](https://github.com/verygoodsecurity/starlarky/pull/235)) | SHA3_* have the v3.10.4 HMAC block_size/copy() additions (f30354b4); v3.12.0 was the latest release in 2022-01; other files have no discriminating lines. |
| `vendor/Crypto/Signature/DSS.star`<br>`vendor/Crypto/PublicKey/DSA.star`<br>`vendor/Crypto/Math/Primality.star` | [pycryptodome](https://github.com/Legrandin/pycryptodome) | 3.13.0-3.14.1; pinned to **3.14.1** | [v3.14.1](https://github.com/Legrandin/pycryptodome/tree/5c5db240da248e1d33982b625e1f2c4d327a2dfc) | lib/Crypto/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | likely | [#93](https://github.com/verygoodsecurity/starlarky/pull/93) (rewritten in [#250](https://github.com/verygoodsecurity/starlarky/pull/250), [#295](https://github.com/verygoodsecurity/starlarky/pull/295)) | DSS.star has d73b27c9 (first in v3.13.0); v3.14.1 was the latest release before 2022-05-04. |
| `vendor/Crypto/IO/PKCS8.star` | [pycryptodome](https://github.com/Legrandin/pycryptodome) | **3.14.1 + master 764c1e8** | [v3.14.1 + 764c1e8](https://github.com/Legrandin/pycryptodome/commit/764c1e81b940596d10cc22e00ca99cc628e504a6) | lib/Crypto/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | likely | [#93](https://github.com/verygoodsecurity/starlarky/pull/93) (rewritten in [#295](https://github.com/verygoodsecurity/starlarky/pull/295)) | Has RFC 5915 v2 parsing from master 764c1e81 (2022-04-15, released in 3.15.0) but keeps the pre-3.15 wrap(key_params=...). |
| `vendor/Crypto/st_common.star` | [pycryptodome](https://github.com/Legrandin/pycryptodome) | any (helper unchanged); pinned to **3.10.1** | [v3.10.1](https://github.com/Legrandin/pycryptodome/tree/339259c3e3bb9c801f24c1ae5a90507ef5078465) | `lib/Crypto/SelfTest/st_common.py` | BSD-2-Clause AND Unlicense | likely | [#95](https://github.com/verygoodsecurity/starlarky/pull/95) | strip_whitespace() from SelfTest/st_common.py; no discriminating lines; pinned with the 3.10.1 group added in the same PR. |
| `vendor/cryptography/LICENSE{,.APACHE,.BSD}`<br>`vendor/cryptography/__init__.star`<br>`vendor/cryptography/hazmat/{__init__,_oid}.star`<br>`vendor/cryptography/hazmat/backends/interfaces.star`<br>`vendor/cryptography/hazmat/primitives/{__init__,_serialization,hashes}.star`<br>`vendor/cryptography/hazmat/primitives/asymmetric/{__init__,padding,rsa}.star`<br>`vendor/cryptography/hazmat/primitives/serialization/{__init__,base,pkcs12,ssh}.star`<br>`vendor/cryptography/x509/{__init__,certificate_transparency,extensions,general_name,name,oid}.star` | [cryptography](https://github.com/pyca/cryptography) | **35.0.0** | [35.0.0](https://github.com/pyca/cryptography/tree/c7fbef767a94ee1569ae0630006fdb144d6a4e8d) | src/cryptography/&lt;same path&gt;.py | Apache-2.0 OR BSD-3-Clause | likely | [#226](https://github.com/verygoodsecurity/starlarky/pull/226) | hazmat/_oid.py first exists in 35.0.0; serialization/base.star matches 35.0.0 only (backend forms removed in 36.0.0). |
| `vendor/cryptography/hazmat/_der.star`<br>`vendor/cryptography/x509/base.star`<br>`vendor/cryptography/hazmat/primitives/asymmetric/utils.star`<br>`vendor/cryptography/hazmat/backends/__init__.star`<br>`vendor/cryptography/utils.star`<br>`vendor/cryptography/hazmat/primitives/serialization/pkcs7.star` | [cryptography](https://github.com/pyca/cryptography) | 3.4.x (<= 3.4.8); pinned to **3.4.8** | [3.4.8](https://github.com/pyca/cryptography/tree/70ccc2542cf3bf374861b55f6527424307722763) | src/cryptography/&lt;same path&gt;.py | Apache-2.0 OR BSD-3-Clause | likely | [#226](https://github.com/verygoodsecurity/starlarky/pull/226) | Pure-Python bodies that 35.0.0 moved to Rust (hazmat/_der.py absent in 35.0.0); bodies match 3.4.8. |
| `vendor/OpenSSL/crypto.star` | [pyOpenSSL](https://github.com/pyca/pyopenssl) | 20.0.0-20.0.1; pinned to **20.0.1** | [20.0.1](https://github.com/pyca/pyopenssl/tree/4211b909fb5aa2c4db2b0f5acbab1480972a0554) | `src/OpenSSL/crypto.py` | Apache-2.0 | likely | [#106](https://github.com/verygoodsecurity/starlarky/pull/106) | crypto.py identical in 20.0.0 and 20.0.1; commented-out blocks keep 20.0.x code that 21.0.0 replaced. |
| `vendor/OpenPGP/{__init__,Crypto}.star` | [OpenPGP-Python (mahmoudimus fork)](https://github.com/mahmoudimus/OpenPGP-Python) | **fork branch HEAD (no release)** | [a174c38](https://github.com/mahmoudimus/OpenPGP-Python/tree/a174c38a2d352893e54b92f1ae3db48f029bad85) | `src/OpenPGP/{__init__,Crypto}.py` | ISC AND MIT | exact | [#255](https://github.com/verygoodsecurity/starlarky/pull/255) (continues [#168](https://github.com/verygoodsecurity/starlarky/pull/168)) | Crypto.star matches 10 lines unique to fork commit a174c38 and none unique to earlier commits; 0 parent-only lines. |
| `vendor/xmlsig/` | [python-xmlsig](https://github.com/etobella/python-xmlsig) | 0.1.6-0.1.7 (src/ identical); pinned to **0.1.6** | [0.1.6](https://github.com/etobella/python-xmlsig/tree/1836ccceef543ebd635fd6007d283e04bfc6caab) | src/xmlsig/&lt;same path&gt;.py | BSD-3-Clause (0.1.6) or AGPL-3.0 (0.1.7); see licensing questions | likely | [#226](https://github.com/verygoodsecurity/starlarky/pull/226) (from [#225](https://github.com/verygoodsecurity/starlarky/pull/225)) | Has TransformHmacSha224 (added in 0.1.6); lacks f0233bc9 (2021-12-08); 0.1.6 and 0.1.7 src/ are byte-identical. |
| `vendor/pydukpt/{__init__,_core,compat,pinblock}.star` | [pydukpt](https://github.com/mahmoudimus/pydukpt) (published as [pydukpt](https://pypi.org/project/pydukpt/0.1.0/) on PyPI) | **0.1.0** | [v0.1.0](https://github.com/mahmoudimus/pydukpt/tree/3778da71dab2e11a185915a453e9175d6fe45aea) (3778da71) | `src/pydukpt/{__init__,_core,compat,pinblock}.py` | MIT | exact | [#783](https://github.com/verygoodsecurity/starlarky/pull/783) (for [#34](https://github.com/verygoodsecurity/starlarky/issues/34)) | Translated from tag v0.1.0; the PyPI 0.1.0 sdist and wheel ship byte-identical `src/pydukpt/*.py`. Larky differences in Notes. |
| `vendor/iso8583/{decoder,encoder,specs}.star` | [pyiso8583](https://github.com/knovichikhin/pyiso8583) | **2.1.0** | [v2.1.0](https://github.com/knovichikhin/pyiso8583/tree/0bf053899b53a78cad30508d65b7ec5f990c9ec1) | `iso8583/{decoder,encoder,specs}.py` | MIT | exact | [#112](https://github.com/verygoodsecurity/starlarky/pull/112) | specs.star shares 413/413 lines with 2.1.0 (358 with 2.0.x); all 2.1.0 len_count/nibble lines present. |
| `vendor/multidict.star` | [multidict](https://github.com/aio-libs/multidict) | 4.7.6-5.1.0; pinned to **5.1.0** | [v5.1.0](https://github.com/aio-libs/multidict/tree/c3eed831bb8bb944bb329013b4fd4b02c66fbf18) | `multidict/_multidict_py.py` | Apache-2.0 | likely | [#170](https://github.com/verygoodsecurity/starlarky/pull/170) | 136 lines shared equally with 4.7.6/5.0.2/5.1.0; upstream file unchanged 2020-07-26..2021-09-30. |
| `vendor/MasterCard/oauth1/{__init__,authenticationutils,coreutils,oauth}.star` | [oauth1-signer-python (Mastercard)](https://github.com/Mastercard/oauth1-signer-python) | 1.4.0-1.5.0; pinned to **1.5.0** | [1.5.0](https://github.com/Mastercard/oauth1-signer-python/tree/375210796f1e1784ff7bc85803955f77baf587ec) | `oauth1/{oauth,coreutils,authenticationutils,__init__}.py` | MIT | likely | [#312](https://github.com/verygoodsecurity/starlarky/pull/312) | oauth/coreutils have lines only in >= 1.4.0; 1.4.0->1.5.0 changed only files not ported. |
| `vendor/option/result.star` | [option](https://github.com/MaT1g3R/option) | **1.0.3 (result.py docstrings unchanged since 0.3.0)** | [d885a88 (master at 1.0.3)](https://github.com/MaT1g3R/option/tree/d885a88d87404e59acad060bf635296bdc35fa93) | `option/result.py` | MIT | likely | [#121](https://github.com/verygoodsecurity/starlarky/pull/121) | PR #121 links upstream result.py; 32/34 doctest lines present; implementation rewritten. |
| `vendor/luhn.star` | [luhn](https://github.com/mmcloughlin/luhn) | **0.2.0** | [0.2.0](https://github.com/mmcloughlin/luhn/tree/d6f3fa71072f99334a4c1d2a3f062fa93982797f) | `luhn.py` | MIT | likely | [#125](https://github.com/verygoodsecurity/starlarky/pull/125) | Docstrings and doctest values identical to 0.2.0; implementation rewritten. |
| `vendor/elementtree/{_SimpleXMLTreeBuilderHelper,SimpleXMLTreeBuilder}.star` | [ElementTree (effbot)](http://effbot.org/zone/element-index.htm) | **1.2.6** | SimpleXMLTreeBuilder.py r1862 (no git) | `elementtree/SimpleXMLTreeBuilder.py` | HPND-style ElementTree license | likely | [#124](https://github.com/verygoodsecurity/starlarky/pull/124) (helper split in [#226](https://github.com/verygoodsecurity/starlarky/pull/226)) | $Id r1862 header identical; 79/125 upstream lines present; 1.2.6 is the last 1.2.x with r1862. |
| `vendor/lxml/builder.star` | [lxml](https://github.com/lxml/lxml) | 4.3.0-4.7.1; pinned to **4.6.4** | [lxml-4.6.4](https://github.com/lxml/lxml/tree/bbee1e900d46bb7044dedf67455f29433aa385ac) | `src/lxml/builder.py` | HPND-style ElementTree license (file); BSD-3-Clause (lxml) | likely | [#226](https://github.com/verygoodsecurity/starlarky/pull/226) (from [#223](https://github.com/verygoodsecurity/starlarky/pull/223)) | builder.py unchanged 2018-12-02..2022-02-14; 4.6.4 was the latest release before 2021-12-08. |
| `vendor/lxml/_c14n.star` | [PyXML](https://sourceforge.net/projects/pyxml/) | **0.8.4** | 0.8.4 sdist (no git) | `xml/dom/ext/c14n.py` | PyXML license (upstream header not kept) | likely | [#226](https://github.com/verygoodsecurity/starlarky/pull/226) (from [#223](https://github.com/verygoodsecurity/starlarky/pull/223)) | Docstring header identical to PyXML 0.8.4 including the 2003/01/25 loewis $Date. |
| `vendor/lxml/_xmlwriter.star` | [elementtreewriter + Amara 2](https://github.com/collective/elementtreewriter) | **elementtreewriter 1.2; amara 2.0.0** | [elementtreewriter 40e79c3; amara a31cea7](https://github.com/collective/elementtreewriter/tree/40e79c30268c42b4da34f36f406a4c2279bb3a32) | `src/elementtreewriter/xmlwriter.py; amara lib/writers/_xmlprinters.py` | D-FSL (elementtreewriter); Apache-2.0 (Amara) | likely | [#226](https://github.com/verygoodsecurity/starlarky/pull/226) (from [#223](https://github.com/verygoodsecurity/starlarky/pull/223)) | XMLWriter docstring found only in elementtreewriter (84 shared lines with 1.1/1.2); xmlprinter section marked as from amara2. |
| `vendor/asserts.star` | [assertpy (+ bazel-skylib unittest.bzl)](https://github.com/assertpy/assertpy) | **assertpy 1.1; skylib <= 1.0.3** | [assertpy 1.1](https://github.com/assertpy/assertpy/tree/b9ad7ee2d673a45f9a5b68c3b7aa2d077560bb7c) | `assertpy/*.py; skylib lib/unittest.bzl` | BSD-3-Clause (assertpy); Apache-2.0 (skylib) | likely | [#10](https://github.com/verygoodsecurity/starlarky/pull/10) | 'modeled after assertpy'; 162 lines shared with 1.1 vs 160 with 1.0, incl. both 1.1-only lines. |
| `vendor/jose/{__init__,exceptions,jwe,jwk,jws,utils}.star`<br>`vendor/jose/backends/{__init__,_asn1,base,native,pycrypto_backend}.star`<br>`vendor/jose/constants/{__init__,algorithms}.star` | [python-jose](https://github.com/mpdavis/python-jose) | master 89c3d41..8572088 (no release); pinned to **8572088** | [89c3d41..8572088](https://github.com/mpdavis/python-jose/compare/89c3d41bc6c60ad6f7d135f14dcd963cef633637...8572088b76f7c0d31068f38aeca0d4273ae8bc8f) | jose/&lt;same path&gt;.py (`constants/*` from `jose/constants.py`) | MIT | likely | [#124](https://github.com/verygoodsecurity/starlarky/pull/124) (jwe: [#122](https://github.com/verygoodsecurity/starlarky/pull/122), [#297](https://github.com/verygoodsecurity/starlarky/pull/297)) | jws.star has 89c3d41 (JWK objects); pycrypto_backend.star ports the backend removed after 8572088; in range upstream changed only the unported ecdsa_backend.py. |
| `vendor/jose/jwt.star` | [python-jose](https://github.com/mpdavis/python-jose) | **3.5.0** | [3.5.0](https://github.com/mpdavis/python-jose/tree/018b310ddb8b50dcfd09a0c152117835a21dd656) | `jose/jwt.py` | MIT | exact | [#124](https://github.com/verygoodsecurity/starlarky/pull/124) (replaced on fix/jose-jwt, 87d6433f) | Translation of 3.5.0 jose/jwt.py on branch fix/jose-jwt (87d6433f); see Notes. |
| `vendor/attr/{__init__,_make,_next_gen,setters,validators}.star`<br>`vendor/attrs/__init__.star` | [attrs](https://github.com/python-attrs/attrs) | **26.1.0** | [26.1.0](https://github.com/python-attrs/attrs/tree/7bfc49e9b22d5ba25b6e429524c3d49fee27cb36) | `src/attr/{__init__,_make,_funcs,_config,_next_gen,setters,validators}.py`, `src/attrs/__init__.py` | MIT | exact | feat/vendor-attrs (Fixes [#15](https://github.com/verygoodsecurity/starlarky/issues/15)) | Written against 26.1.0 (tag commit 7bfc49e9); version pinned, content is a reimplementation (see Notes). |

## stdlib/

Paths are relative to `larky/src/main/resources`.

| Larky path(s) | Upstream | Version | Git ref | Upstream path(s) | License | Confidence | First added | Evidence |
|---|---|---|---|---|---|---|---|---|
| `stdlib/collections.star` | [bazel-skylib](https://github.com/bazelbuild/bazel-skylib) | 0.7.0-1.9.2; pinned to **1.0.3** | [1.0.3](https://github.com/bazelbuild/bazel-skylib/tree/2ec2e6d715e993d96ad6222770805b5bd25399ae) | `lib/collections.bzl` | Apache-2.0 | likely | [#5](https://github.com/verygoodsecurity/starlarky/pull/5) | after_each/before_each/uniq bodies and the 0.7.0+ python3 comment; rest of the file is VGS/CPython (see Notes). |
| `stdlib/dicts.star` | [bazel-skylib](https://github.com/bazelbuild/bazel-skylib) | 0.8.0-1.9.2; pinned to **1.0.3** | [1.0.3](https://github.com/bazelbuild/bazel-skylib/tree/2ec2e6d715e993d96ad6222770805b5bd25399ae) | `lib/dicts.bzl` | Apache-2.0 | likely | [#10](https://github.com/verygoodsecurity/starlarky/pull/10) | 22/41 lines; `**kwargs` handling appears in 0.8.0. |
| `stdlib/partial.star` | [bazel-skylib](https://github.com/bazelbuild/bazel-skylib) | 0.6.0-1.9.2; pinned to **1.0.3** | [1.0.3](https://github.com/bazelbuild/bazel-skylib/tree/2ec2e6d715e993d96ad6222770805b5bd25399ae) | `lib/partial.bzl` | Apache-2.0 | likely | [#10](https://github.com/verygoodsecurity/starlarky/pull/10) | 67/68 lines match 0.6.0+. |
| `stdlib/sets.star` | [bazel-skylib](https://github.com/bazelbuild/bazel-skylib) | 1.0.3-1.9.2; pinned to **1.0.3** | [1.0.3](https://github.com/bazelbuild/bazel-skylib/tree/2ec2e6d715e993d96ad6222770805b5bd25399ae) | `lib/new_sets.bzl` | Apache-2.0 | likely | [#5](https://github.com/verygoodsecurity/starlarky/pull/5) | 77/128 lines; 1.0.0-1.0.2 lack the `union(*args)` docstring Larky has; 1.0.3 was the latest release before first add. |
| `stdlib/types.star` | [bazel-skylib](https://github.com/bazelbuild/bazel-skylib) | 1.0.0-1.9.2; pinned to **1.0.3** | [1.0.3](https://github.com/bazelbuild/bazel-skylib/tree/2ec2e6d715e993d96ad6222770805b5bd25399ae) | `lib/types.bzl` | Apache-2.0 | likely | [#10](https://github.com/verygoodsecurity/starlarky/pull/10) | 38/203 lines; is_set/_a_struct_type appear from 1.0.0. |
| `stdlib/base64.star` | [CPython](https://github.com/python/cpython) | 3.3.3-3.3.7; pinned to **3.3.7** | [v3.3.7](https://github.com/python/cpython/tree/17bf6b4671ec02d80ad29b278639d5307baddeb5) | `Lib/base64.py` | PSF-2.0 | exact | [#102](https://github.com/verygoodsecurity/starlarky/pull/102) | Has 3.3.3's b32encode bytearray and 'Non-base32 digit found'; keeps 3.3 docstrings that 3.4 reworded. |
| `stdlib/_markupbase.star` | [CPython](https://github.com/python/cpython) | 3.10.0-3.13 (blob unchanged); pinned to **3.10.1** | [v3.10.1](https://github.com/python/cpython/tree/2cd268a3a9340346dd86b66db2e9b428b3f878fc) | `Lib/_markupbase.py` | PSF-2.0 | exact | [#226](https://github.com/verygoodsecurity/starlarky/pull/226) | Raises AssertionError where 3.9 called self.error() (3.10.0 change). |
| `stdlib/hashlib.star` | [CPython](https://github.com/python/cpython) | 3.7.1-3.10; pinned to **3.9.5** | [v3.9.5](https://github.com/python/cpython/tree/0a7dcbdb13f1f2ab6e76e1cff47e80fb263f5da0) | `Lib/hashlib.py` | PSF-2.0 | likely | [#124](https://github.com/verygoodsecurity/starlarky/pull/124) | Only the module docstring is CPython (40/78 lines, identical 3.7.1-3.10). |
| `stdlib/io/{_io,StringIO}.star` | [CPython](https://github.com/python/cpython) | 2.7.x; pinned to **2.7.18** | [v2.7.18](https://github.com/python/cpython/tree/8d21aa21f2cbc6d50aab3f420bb23be1d081dac4) | `Lib/StringIO.py (Python 2)` | PSF-2.0 | likely | [#124](https://github.com/verygoodsecurity/starlarky/pull/124) | Python 2 StringIO.py docstring; 126/187 lines match every 2.7.x. |
| `stdlib/operator.star` | [CPython](https://github.com/python/cpython) | 3.5-3.10; pinned to **3.9.5** | [v3.9.5](https://github.com/python/cpython/tree/0a7dcbdb13f1f2ab6e76e1cff47e80fb263f5da0) | `Lib/operator.py` | PSF-2.0 | likely | [#121](https://github.com/verygoodsecurity/starlarky/pull/121) | 64/309 lines match 3.5.0-main(2021-05) equally. |
| `stdlib/random.star` | [CPython](https://github.com/python/cpython) | 3.9.0-3.10.0; pinned to **3.9.6** | [v3.9.6](https://github.com/python/cpython/tree/db3ff76da19004f266b62e98a81bdfd322861436) | `Lib/random.py` | PSF-2.0 | likely | [#157](https://github.com/verygoodsecurity/starlarky/pull/157) | Only the module docstring is CPython; its bytes section exists from 3.9.0. |
| `stdlib/reprlib.star` | [CPython](https://github.com/python/cpython) | 3.2-3.10; pinned to **3.9.6** | [v3.9.6](https://github.com/python/cpython/tree/db3ff76da19004f266b62e98a81bdfd322861436) | `Lib/reprlib.py` | PSF-2.0 | unknown | [#157](https://github.com/verygoodsecurity/starlarky/pull/157) | 23/85 lines match every reprlib.py from 3.2.1 to 2021-08 main; pin is the release current at port time. |
| `stdlib/string.star` | [CPython](https://github.com/python/cpython) | 2.7.x; pinned to **2.7.18** | [v2.7.18](https://github.com/python/cpython/tree/8d21aa21f2cbc6d50aab3f420bb23be1d081dac4) | `Lib/string.py (Python 2)` | PSF-2.0 | likely | [#124](https://github.com/verygoodsecurity/starlarky/pull/124) | Python 2-only docstring; 43/75 lines match all 2.7.x. |
| `stdlib/textwrap.star` | [CPython](https://github.com/python/cpython) | 3.10.0-3.10.4; pinned to **3.10.4** | [v3.10.4](https://github.com/python/cpython/tree/9d38120e335357a3b294277fd5eff0a10e46e043) | `Lib/textwrap.py` | PSF-2.0 | likely | [#255](https://github.com/verygoodsecurity/starlarky/pull/255) | Has the 3.10 break_on_hyphens rewrite; 270/341 lines vs 261 for 3.9. |
| `stdlib/bz2.star` | [CPython](https://github.com/python/cpython) | 3.10.1-3.12; pinned to **3.10.4** | [v3.10.4](https://github.com/python/cpython/tree/9d38120e335357a3b294277fd5eff0a10e46e043) | `Lib/bz2.py` | PSF-2.0 | likely | [#255](https://github.com/verygoodsecurity/starlarky/pull/255) | Has io.text_encoding (3.10.0) and no BZ2File.__iter__ (removed in 3.10.1). |
| `stdlib/urllib/parse.star` | [CPython](https://github.com/python/cpython) | **3.9.6** | [v3.9.6](https://github.com/python/cpython/tree/db3ff76da19004f266b62e98a81bdfd322861436) | `Lib/urllib/parse.py` | PSF-2.0 | exact | [#126](https://github.com/verygoodsecurity/starlarky/pull/126) | Header says 'Direct copy from .../3.9/...'; has the bpo-43882 follow-up first released in 3.9.6 (3.9 branch 8a59574, same blob). |
| `stdlib/urllib/request.star` | [CPython](https://github.com/python/cpython) | 3.9.7-3.10.1; pinned to **3.10.1** | [v3.10.1](https://github.com/python/cpython/tree/2cd268a3a9340346dd86b66db2e9b428b3f878fc) | `Lib/urllib/request.py` | PSF-2.0 | likely | [#10](https://github.com/verygoodsecurity/starlarky/pull/10) (reworked in [#231](https://github.com/verygoodsecurity/starlarky/pull/231)) | Docstrings/structure only; 105/144 lines match 3.9.7-3.10.1. |
| `stdlib/uuid.star` | [CPython](https://github.com/python/cpython) | 3.7-3.10; pinned to **3.10.0** | [v3.10.0](https://github.com/python/cpython/tree/b494f5935c92951e75597bfe1c8b1f3112fec270) | `Lib/uuid.py` | PSF-2.0 | likely | [#190](https://github.com/verygoodsecurity/starlarky/pull/190) | 124/199 lines match every uuid.py 3.7.0-3.10.1. |
| `stdlib/xml/etree/{ElementPath,ElementTree}.star` | [CPython](https://github.com/python/cpython) | 3.10.0a3-3.10.0a5; pinned to **3.10.0a5** | [v3.10.0a5](https://github.com/python/cpython/tree/b0478d738a30ca6a1a57e059156e7445612f2828) | `Lib/xml/etree/{ElementTree,ElementPath}.py` | PSF-2.0 | likely | [#124](https://github.com/verygoodsecurity/starlarky/pull/124) | ElementPath has XPath != (first in 3.10.0a3); ElementTree keeps specified_attributes (removed in 3.10.0a6). |
| `stdlib/xmllib.star` | [CPython](https://github.com/python/cpython) | 2.7.x; pinned to **2.7.18** | [v2.7.18](https://github.com/python/cpython/tree/8d21aa21f2cbc6d50aab3f420bb23be1d081dac4) | `Lib/xmllib.py (Python 2)` | PSF-2.0 | likely | [#124](https://github.com/verygoodsecurity/starlarky/pull/124) | Python 2-only module; 389/590 lines match the single 2.7 blob. |
| `stdlib/zipfile.star` | [CPython](https://github.com/python/cpython) | 3.11.0-3.11.3; pinned to **3.11.3** | [v3.11.3](https://github.com/python/cpython/tree/f3909b8bc83675b7ab093dbc558e677558d8e718) | `Lib/zipfile.py` | PSF-2.0 | likely | [#332](https://github.com/verygoodsecurity/starlarky/pull/332) | 769/941 lines match 3.11.0-3.11.3 vs 761 for 3.10.x. |
| `stdlib/enum.star` | [CPython](https://github.com/python/cpython) | **3.9 branch 5a8ddcc** | [5a8ddcc (3.9 branch)](https://github.com/python/cpython/tree/5a8ddcc4524dca3880d7fc2818814ffae1cfb8a2) | `Lib/enum.py` | PSF-2.0 | likely | [#124](https://github.com/verygoodsecurity/starlarky/pull/124) | Source comment links Lib/enum.py at 5a8ddcc; only functional-API name handling is adapted (22/92 lines). |
| `stdlib/csv.star` | [backports.csv](https://github.com/ryanhiebert/backports.csv) | 1.0.5-1.0.7; pinned to **1.0.7** | [1.0.7 (PyPI sdist)](https://github.com/ryanhiebert/backports.csv/tree/1.0.7) | `src/backports/csv.py` | PSF-2.0 | likely | [#163](https://github.com/verygoodsecurity/starlarky/pull/163) | Header says it translates backports.csv; has 1.0.5's quotechar default; 1.0.6 and 1.0.7 files identical. |

## vgs/

Paths are relative to `larky/src/main/resources`.

| Larky path(s) | Upstream | Version | Git ref | Upstream path(s) | License | Confidence | First added | Evidence |
|---|---|---|---|---|---|---|---|---|
| `vgs/xmlsig-java-compatible/` | [python-xmlsig (via vendor/xmlsig)](https://github.com/etobella/python-xmlsig) | 0.1.6-0.1.7; pinned to **0.1.6** | [0.1.6](https://github.com/etobella/python-xmlsig/tree/1836ccceef543ebd635fd6007d283e04bfc6caab) | src/xmlsig/&lt;same path&gt;.py | as vendor/xmlsig (undetermined) | likely | [#325](https://github.com/verygoodsecurity/starlarky/pull/325) | Copy of vendor/xmlsig with VGS changes (2-28 lines per file; ns.star identical); git detects it as a copy. |

## Tests

Paths are relative to `larky/src/test/resources`.

| Larky path(s) | Upstream | Version | Git ref | Upstream path(s) | License | Confidence | First added | Evidence |
|---|---|---|---|---|---|---|---|---|
| `vendor_tests/Crypto/PublicKey/{test_import_RSA,test_RSA}.star`<br>`vendor_tests/Crypto/Util/{test_asn1,test_number,test_Padding,test_strxor}.star`<br>`vendor_tests/Crypto/Cipher/{test_CFB,test_GCM}.star`<br>`vendor_tests/Crypto/Hash/{test_BLAKE2,test_HMAC,test_SHAKE}.star`<br>`vendor_tests/Crypto/Protocol/test_KDF.star` | [pycryptodome (SelfTest)](https://github.com/Legrandin/pycryptodome) | <= 3.10.1 (some >= 3.10.0); pinned to **3.10.1** | [v3.10.1](https://github.com/Legrandin/pycryptodome/tree/339259c3e3bb9c801f24c1ae5a90507ef5078465) | lib/Crypto/SelfTest/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | likely | [#90](https://github.com/verygoodsecurity/starlarky/pull/90), [#93](https://github.com/verygoodsecurity/starlarky/pull/93), [#95](https://github.com/verygoodsecurity/starlarky/pull/95), [#102](https://github.com/verygoodsecurity/starlarky/pull/102), [#131](https://github.com/verygoodsecurity/starlarky/pull/131), [#137](https://github.com/verygoodsecurity/starlarky/pull/137), [#165](https://github.com/verygoodsecurity/starlarky/pull/165) | test_RSA, test_import_RSA, test_KDF match best only >= v3.10.0; added before 2021-09-24, so bounded by v3.10.1; others have no discriminating lines. |
| `vendor_tests/Crypto/PublicKey/test_ECC.star`<br>`vendor_tests/Crypto/Cipher/test_pkcs1_oaep.star`<br>`vendor_tests/Crypto/Signature/test_pkcs1_15.star` | [pycryptodome (SelfTest)](https://github.com/Legrandin/pycryptodome) | 3.10.0-3.11.0; pinned to **3.11.0** | [v3.11.0](https://github.com/Legrandin/pycryptodome/tree/b92494b64aaa7af54a8c3e14aed7a3f3a67e58b3) | lib/Crypto/SelfTest/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | likely | [#173](https://github.com/verygoodsecurity/starlarky/pull/173), [#189](https://github.com/verygoodsecurity/starlarky/pull/189) | test_pkcs1_oaep best-matches eda4f657 (>= v3.10.0); no v3.12.0-only content; added Oct 2021, when v3.11.0 was current. |
| `vendor_tests/Crypto/Cipher/test_pkcs1_15.star` | [pycryptodome (SelfTest)](https://github.com/Legrandin/pycryptodome) | **3.11.0** | [v3.11.0](https://github.com/Legrandin/pycryptodome/tree/b92494b64aaa7af54a8c3e14aed7a3f3a67e58b3) | lib/Crypto/SelfTest/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | exact | [#189](https://github.com/verygoodsecurity/starlarky/pull/189) | Has test_return_type (a792f5a6, first in v3.11.0); tag scan matches v3.11.0 only. |
| `vendor_tests/Crypto/Hash/{common,test_MD2,test_MD4,test_MD5,test_RIPEMD160,test_SHA3_224,test_SHA3_256,test_SHA3_384,test_SHA3_512}.star` | [pycryptodome (SelfTest)](https://github.com/Legrandin/pycryptodome) | <= 3.12.0 (no discriminating lines); pinned to **3.12.0** | [v3.12.0](https://github.com/Legrandin/pycryptodome/tree/d8edf1a6a70d3a65dcda18ee24d96161525f2825) | lib/Crypto/SelfTest/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | unknown | [#234](https://github.com/verygoodsecurity/starlarky/pull/234), [#235](https://github.com/verygoodsecurity/starlarky/pull/235) | Upstream files unchanged for years (every tag v3.9.0-v3.24.1 scores equally); pin is the release current in 2022-01. |
| `vendor_tests/Crypto/Cipher/test_CCM.star`<br>`vendor_tests/Crypto/PublicKey/test_DSA.star` | [pycryptodome (SelfTest)](https://github.com/Legrandin/pycryptodome) | 3.10.0-3.14.1; pinned to **3.14.1** | [v3.14.1](https://github.com/Legrandin/pycryptodome/tree/5c5db240da248e1d33982b625e1f2c4d327a2dfc) | lib/Crypto/SelfTest/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | likely | [#288](https://github.com/verygoodsecurity/starlarky/pull/288), [#295](https://github.com/verygoodsecurity/starlarky/pull/295) | test_CCM best-matches 4ec87b8d (first in v3.12.0); test_DSA >= v3.10.0; both added May 2022, before v3.15.0. |
| `vendor_tests/Crypto/Cipher/{test_CBC,test_CTR}.star` | [pycryptodome (SelfTest)](https://github.com/Legrandin/pycryptodome) | 3.12.0-3.15.0; pinned to **3.15.0** | [v3.15.0](https://github.com/Legrandin/pycryptodome/tree/c37fd1968b445a3f4b48519e126721a5435429ed) | lib/Crypto/SelfTest/&lt;same path&gt;.py | BSD-2-Clause AND Unlicense | likely | 964c3efc (direct commit), [#311](https://github.com/verygoodsecurity/starlarky/pull/311) | test_CTR best-matches 4ec87b8d (first in v3.12.0); test_CBC (all commented out) matches v3.12.0+; added Aug 2022. |
| `vendor_tests/OpenSSL/test_crypto.star` | [pyOpenSSL](https://github.com/pyca/pyopenssl) | **none (empty file)** | - | `tests/test_crypto.py (by name only)` | - | unknown | [#4](https://github.com/verygoodsecurity/starlarky/pull/4) / [#116](https://github.com/verygoodsecurity/starlarky/pull/116) | The file is empty (0 bytes); nothing to compare. |
| `vendor_tests/OpenPGP/{data_test_fixtures,test_Crypto,test_OpenPGP}.star` | [OpenPGP-Python (mahmoudimus fork)](https://github.com/mahmoudimus/OpenPGP-Python) | **fork a174c38 (loosely)** | [a174c38](https://github.com/mahmoudimus/OpenPGP-Python/tree/a174c38a2d352893e54b92f1ae3db48f029bad85) | `tests/{test_OpenPGP,test_Crypto}.py, tests/data/*` | ISC AND MIT | likely | [#255](https://github.com/verygoodsecurity/starlarky/pull/255) | Only 19/357 and 12/153 lines shared with the fork tests (8 and 5 fork-only, 0 parent-only); mostly rewritten. |
| `vendor_tests/xmlsig/{base,test_xmlsig}.star`<br>`vendor_tests/xmlsig/data/{rsacert_pem,rsakey_pem,rsapub_pem,sign1_in_xml,sign1_out_xml}.star` | [python-xmlsig](https://github.com/etobella/python-xmlsig) | 0.1.6-0.1.7 (tests/ not in sdist); pinned to **0.1.7** | [eed8913 (0.1.7)](https://github.com/etobella/python-xmlsig/tree/eed891371a80b6f65d0a8bec252b6b6c9af9bd1d) | `tests/{test_rsa,base}.py, tests/data/*` | as vendor/xmlsig (undetermined) | likely | [#226](https://github.com/verygoodsecurity/starlarky/pull/226) (from [#225](https://github.com/verygoodsecurity/starlarky/pull/225)) | Fixtures contain all upstream data lines; test_xmlsig.star is a rewrite (3 lines shared with test_rsa.py). |
| `vendor_tests/iso8583/test_encode.star` | [pyiso8583](https://github.com/knovichikhin/pyiso8583) | 2.0.2-2.1.0; pinned to **2.1.0** | [v2.1.0](https://github.com/knovichikhin/pyiso8583/tree/0bf053899b53a78cad30508d65b7ec5f990c9ec1) | `tests/test_encode.py` | MIT | likely | [#112](https://github.com/verygoodsecurity/starlarky/pull/112) | All 89 upstream test names present (same set in 2.0.2 and 2.1.0); paired with the 2.1.0 library port. |
| `vendor_tests/pydukpt/{conftest,helpers,test_compat,test_generated_vectors,test_keys,test_ksn,test_pinblock,test_published_vectors}.star`<br>`vendor_tests/pydukpt/fixtures/vectors.star` | [pydukpt](https://github.com/mahmoudimus/pydukpt) | **0.1.0** | [v0.1.0](https://github.com/mahmoudimus/pydukpt/tree/3778da71dab2e11a185915a453e9175d6fe45aea) (3778da71) | `tests/{conftest,helpers,test_compat,test_generated_vectors,test_keys,test_ksn,test_pinblock,test_published_vectors}.py`, `tests/fixtures/vectors.json` | MIT (vectors: see Notes) | exact | [#783](https://github.com/verygoodsecurity/starlarky/pull/783) | Every upstream test except `test_live_dukpt_1_0_1_if_installed`; `fixtures/vectors.star` is `vectors.json` (sha256 `8d9c8afb…e501bce2`) as a Starlark literal. See Notes. |
| `vendor_tests/multidict/test_mutable_multidict.star` | [multidict](https://github.com/aio-libs/multidict) | 4.7.6-5.1.0; pinned to **5.1.0** | [v5.1.0](https://github.com/aio-libs/multidict/tree/c3eed831bb8bb944bb329013b4fd4b02c66fbf18) | `tests/test_mutable_multidict.py` | Apache-2.0 | likely | [#170](https://github.com/verygoodsecurity/starlarky/pull/170) | 31/34 upstream test names; upstream file unchanged 2020-07-26..2021-09-30. |
| `vendor_tests/MasterCard/test_mcoauth.star` | [oauth1-signer-python (Mastercard)](https://github.com/Mastercard/oauth1-signer-python) | 1.4.0-1.5.0; pinned to **1.5.0** | [1.5.0](https://github.com/Mastercard/oauth1-signer-python/tree/375210796f1e1784ff7bc85803955f77baf587ec) | `tests/test_{oauth,utils,signer}.py` | MIT | likely | [#312](https://github.com/verygoodsecurity/starlarky/pull/312) | 6 of 13 tests share upstream names; the 7 `test_legacy_*` tests are VGS-written. |
| `vendor_tests/option/test_result.star` | [option](https://github.com/MaT1g3R/option) | **1.0.3** | [d885a88](https://github.com/MaT1g3R/option/tree/d885a88d87404e59acad060bf635296bdc35fa93) | `tests/test_result.py` | MIT | likely | [#121](https://github.com/verygoodsecurity/starlarky/pull/121) | 13/18 upstream test names; upstream file unchanged 2020-02-21..2022-03-31. |
| `vendor_tests/luhn/test_luhn.star` | [luhn](https://github.com/mmcloughlin/luhn) | **0.2.0** | [0.2.0](https://github.com/mmcloughlin/luhn/tree/d6f3fa71072f99334a4c1d2a3f062fa93982797f) | `test.py` | MIT | likely | [#125](https://github.com/verygoodsecurity/starlarky/pull/125) | 4 of 10 upstream tests with the same vectors; test.py is not in the 0.2.0 sdist, but the git tag has it, unchanged since a3b3922 (2015-06-23) and identical on master. |
| `vendor_tests/lxml/test_c14n.star` | [PyXML](https://sourceforge.net/projects/pyxml/) | **0.8.4** | 0.8.4 sdist (no git) | `test/test_c14n.py` | PyXML license | likely | [#226](https://github.com/verygoodsecurity/starlarky/pull/226) | 126/289 upstream lines present (W3C C14N examples); DTD cases modified. |
| `stdlib_tests/test_dicts.star` | [bazel-skylib](https://github.com/bazelbuild/bazel-skylib) | 0.8.0-1.0.3; pinned to **1.0.3** | [1.0.3](https://github.com/bazelbuild/bazel-skylib/tree/2ec2e6d715e993d96ad6222770805b5bd25399ae) | `tests/dicts_tests.bzl` | Apache-2.0 | likely | [#12](https://github.com/verygoodsecurity/starlarky/pull/12) | 19/44 lines match 0.8.0+. |
| `stdlib_tests/test_sets.star` | [bazel-skylib](https://github.com/bazelbuild/bazel-skylib) | 0.7.0-1.9.2; pinned to **1.0.3** | [1.0.3](https://github.com/bazelbuild/bazel-skylib/tree/2ec2e6d715e993d96ad6222770805b5bd25399ae) | `tests/new_sets_tests.bzl` | Apache-2.0 | likely | [#93](https://github.com/verygoodsecurity/starlarky/pull/93) | 35/138 lines match; rest is VGS Set() tests. |
| `stdlib_tests/test_types.star` | [bazel-skylib](https://github.com/bazelbuild/bazel-skylib) | 1.0.0-1.0.3; pinned to **1.0.3** | [1.0.3](https://github.com/bazelbuild/bazel-skylib/tree/2ec2e6d715e993d96ad6222770805b5bd25399ae) | `tests/types_tests.bzl` | Apache-2.0 | likely | [#90](https://github.com/verygoodsecurity/starlarky/pull/90) | 21/287 lines match; mostly VGS tests. |
| `stdlib_tests/test_struct.star` | [CPython](https://github.com/python/cpython) | 3.8.4-3.10; pinned to **3.9.2** | [v3.9.2](https://github.com/python/cpython/tree/1a79785e3e8fea80bcf6a800b45a04e06c787480) | `Lib/test/test_struct.py` | PSF-2.0 | likely | [#81](https://github.com/verygoodsecurity/starlarky/pull/81) | 187/754 lines match 3.8.4-3.9.x (185 for 3.8.0-3.8.3). |
| `stdlib_tests/test_zlib.star` | [CPython](https://github.com/python/cpython) | 3.9.0-3.9.6; pinned to **3.9.6** | [v3.9.6](https://github.com/python/cpython/tree/db3ff76da19004f266b62e98a81bdfd322861436) | `Lib/test/test_zlib.py` | PSF-2.0 | likely | [#157](https://github.com/verygoodsecurity/starlarky/pull/157) | 263/553 lines match 3.9.x vs 261 for 3.8; uses random.randbytes (3.9+). |
| `stdlib_tests/test_operator.star` | [CPython](https://github.com/python/cpython) | 3.8-3.10; pinned to **3.9.5** | [v3.9.5](https://github.com/python/cpython/tree/0a7dcbdb13f1f2ab6e76e1cff47e80fb263f5da0) | `Lib/test/test_operator.py` | PSF-2.0 | likely | [#121](https://github.com/verygoodsecurity/starlarky/pull/121) | 42/353 lines match 3.8.0+ vs 40 for 3.7. |
| `stdlib_tests/xml/etree/test_elementtree.star` | [CPython](https://github.com/python/cpython) | 3.9.0-3.10.0; pinned to **3.9.5** | [v3.9.5](https://github.com/python/cpython/tree/0a7dcbdb13f1f2ab6e76e1cff47e80fb263f5da0) | `Lib/test/test_xml_etree.py` | PSF-2.0 | likely | [#124](https://github.com/verygoodsecurity/starlarky/pull/124) | 40/129 string literals match 3.9.0-3.10.0 (3.8.10: 2); mostly VGS-written. |
| `stdlib_tests/test_base64.star` | [CPython](https://github.com/python/cpython) | 3.1.3-3.9; pinned to **3.3.7** | [v3.3.7](https://github.com/python/cpython/tree/17bf6b4671ec02d80ad29b278639d5307baddeb5) | `Lib/test/test_base64.py` | PSF-2.0 | unknown | [#102](https://github.com/verygoodsecurity/starlarky/pull/102) | 14/37 literals match every 3.1.3-main blob; pinned to match base64.star. |
| `stdlib_tests/test_this.star` | [CPython](https://github.com/python/cpython) | 3.0.1-3.10 (blob unchanged); pinned to **3.9.6** | [v3.9.6](https://github.com/python/cpython/tree/db3ff76da19004f266b62e98a81bdfd322861436) | `Lib/this.py` | PSF-2.0 | exact | [#161](https://github.com/verygoodsecurity/starlarky/pull/161) | rot13 Zen text and decoding loop; 23/24 upstream lines. |
| `stdlib_tests/test_uuid.star` | [CPython](https://github.com/python/cpython) | **main a02cb47 (not determined)** | [a02cb47](https://github.com/python/cpython/blob/a02cb474f9c097c83cd444a47e9fb5f99b4aaf45/Lib/test/test_uuid.py) | `Lib/test/test_uuid.py` | PSF-2.0 | unknown | [#190](https://github.com/verygoodsecurity/starlarky/pull/190) | One uuid4 test adapted from main; text does not discriminate versions. a02cb47 (2021-04-21) is the file's last change on main before #190 merged (2021-12-07); no release has that exact blob. |
| `stdlib_tests/urllib/test_parse.star` | [CPython](https://github.com/python/cpython) | **3.5+ (not determined)** | - | `Lib/test/test_urlparse.py` | PSF-2.0 | unknown | [#126](https://github.com/verygoodsecurity/starlarky/pull/126) | 17/94 URL vectors match 3.5-3.9 equally; 2/114 code lines; mostly VGS-written. |
| `stdlib_tests/test_bytes.star` | [starlark-go](https://github.com/google/starlark-go) | **ebe61bd (2021-02-12)** | [ebe61bd](https://github.com/google/starlark-go/tree/ebe61bd709bf23d7baddbb34e79084d7d156be04) | `starlark/testdata/bytes.star` | BSD-3-Clause | likely | [#71](https://github.com/verygoodsecurity/starlarky/pull/71) | Commit says 'match the go-upstream'; 33/205 lines match ebe61bd, the only revision before 2021-03-10. |
| `stdlib_tests/test_binascii.star` | [Adafruit_CircuitPython_Binascii](https://github.com/adafruit/Adafruit_CircuitPython_Binascii) | 1.0-1.2.5 (example code identical); pinned to **1.2.5** | [1.2.5](https://github.com/adafruit/Adafruit_CircuitPython_Binascii/tree/1.2.5) | `examples/binascii_simpletest.py` | MIT | exact | [#81](https://github.com/verygoodsecurity/starlarky/pull/81) | Line-for-line translation of binascii_simpletest.py; upstream SPDX header dropped. |
| `vendor_tests/attrs/{test_funcs,test_make,test_next_gen,test_validators}.star` | [attrs](https://github.com/python-attrs/attrs) | **26.1.0** | [26.1.0](https://github.com/python-attrs/attrs/tree/7bfc49e9b22d5ba25b6e429524c3d49fee27cb36) | `tests/test_{funcs,make,next_gen,setattr,validators}.py` | MIT | exact | feat/vendor-attrs (Fixes [#15](https://github.com/verygoodsecurity/starlarky/issues/15)) | Ported from 26.1.0 tests; `test_next_gen.star` also holds the ported `test_setattr.py` cases. |

## VGS-original files

Written for Larky; no upstream file to pin and no header. Paths are relative to `larky/src/main/resources` (vendor, stdlib, vgs) or `larky/src/test/resources` (tests). 87 files.

**vendor/**

- `vendor/Crypto/Math/Numbers.star` ([#90](https://github.com/verygoodsecurity/starlarky/pull/90)): Larky Integer class mimicking pycryptodome's _IntegerBase API; 23-65 of 348 lines match, none discriminating.
- `vendor/cryptography/hazmat/backends/pycryptodome.star` ([#226](https://github.com/verygoodsecurity/starlarky/pull/226)): VGS backend over pycryptodome/jopenssl; carries the upstream dual-license header.
- `vendor/cryptography/hazmat/primitives/_hashes.star` ([#226](https://github.com/verygoodsecurity/starlarky/pull/226)): Larky import-cycle breaker; carries the upstream dual-license header.
- `vendor/cryptography/x509/_base.star` ([#226](https://github.com/verygoodsecurity/starlarky/pull/226)): Larky import-cycle breaker; carries the upstream dual-license header.
- `vendor/larky_ecdh.star` ([#320](https://github.com/verygoodsecurity/starlarky/pull/320)): Wrapper over the native ECDH module.
- `vendor/lxml/etree.star` ([#226](https://github.com/verygoodsecurity/starlarky/pull/226)): lxml.etree API reimplemented over Larky ElementTree/xmllib; links to lxml sources as reference.
- `vendor/lxml/__init__.star` ([#226](https://github.com/verygoodsecurity/starlarky/pull/226)): Package init for the lxml reimplementation.
- `vendor/jsonpath_ng.star` ([#117](https://github.com/verygoodsecurity/starlarky/pull/117)): Basic JSONPath; only the API shape resembles jsonpath-ng (3 lines shared).
- `vendor/six.star` ([#124](https://github.com/verygoodsecurity/starlarky/pull/124)): Shim exporting six-named helpers over Crypto.Util.py3compat.
- `vendor/past/__init__.star` ([#226](https://github.com/verygoodsecurity/starlarky/pull/226)): Package init.
- `vendor/past/builtins.star` ([#226](https://github.com/verygoodsecurity/starlarky/pull/226)): cmp() only; implementation differs from python-future.
- `vendor/escapes.star` ([#71](https://github.com/verygoodsecurity/starlarky/pull/71)): Escape-sequence builder; only the CEscape name/docstring come from protobuf text_encoding.py.

**stdlib/**

- `stdlib/binascii.star` ([#81](https://github.com/verygoodsecurity/starlarky/pull/81)): Wrapper over Java jbinascii; docstrings are CPython 3.8-3.10 binascii __doc__ strings.
- `stdlib/builtins.star` ([#71](https://github.com/verygoodsecurity/starlarky/pull/71)): Python builtins shim.
- `stdlib/compat.star` ([#97](https://github.com/verygoodsecurity/starlarky/pull/97))
- `stdlib/functools.star` ([#96](https://github.com/verygoodsecurity/starlarky/pull/96)): cmp_to_key docstring and a banner from CPython functools.py.
- `stdlib/itertools.star` ([#124](https://github.com/verygoodsecurity/starlarky/pull/124)): Descriptions paraphrase the itertools docs.
- `stdlib/larky.star` ([#12](https://github.com/verygoodsecurity/starlarky/pull/12))
- `stdlib/math.star` ([#12](https://github.com/verygoodsecurity/starlarky/pull/12)): Wrapper over Java c99math.
- `stdlib/struct.star` ([#81](https://github.com/verygoodsecurity/starlarky/pull/81)): Wrapper over Java jstruct.
- `stdlib/zlib.star` ([#157](https://github.com/verygoodsecurity/starlarky/pull/157)): Wrapper over Java jzlib.
- `stdlib/re.star` ([#55](https://github.com/verygoodsecurity/starlarky/pull/55)): RE2-backed (rewritten in #717); docstrings and syntax summary are CPython 3.7-3.9 Lib/re.py text.
- `stdlib/io/__init__.star` ([#185](https://github.com/verygoodsecurity/starlarky/pull/185)): Re-exports io/_io.
- `stdlib/xml/etree/__init__.star` ([#4](https://github.com/verygoodsecurity/starlarky/pull/4)): Empty.

**vgs/**

- `vgs/soap-sign/signer.star` ([#325](https://github.com/verygoodsecurity/starlarky/pull/325))
- `vgs/jks/jks2pycryptodome.star` ([#325](https://github.com/verygoodsecurity/starlarky/pull/325)): Copies a function from vendor/cryptography/hazmat/backends/pycryptodome.star.
- `vgs/Chase/jwk.star` ([#275](https://github.com/verygoodsecurity/starlarky/pull/275))
- `vgs/cerebro/text/analyzer.star` ([#156](https://github.com/verygoodsecurity/starlarky/pull/156))
- `vgs/http/request.star` ([#231](https://github.com/verygoodsecurity/starlarky/pull/231))
- `vgs/http/response.star` ([#244](https://github.com/verygoodsecurity/starlarky/pull/244)): One comment from CPython urllib/request.py (Request.data setter).
- `vgs/metrics.star` ([#458](https://github.com/verygoodsecurity/starlarky/pull/458))
- `vgs/nts.star` ([#330](https://github.com/verygoodsecurity/starlarky/pull/330))
- `vgs/proxy.star` ([#705](https://github.com/verygoodsecurity/starlarky/pull/705))

**vendor_tests/**

- `vendor_tests/Crypto/Hash/test_hash.star` ([#133](https://github.com/verygoodsecurity/starlarky/pull/133)): No pycryptodome counterpart.
- `vendor_tests/ECDH/test_ecdh.star` ([#320](https://github.com/verygoodsecurity/starlarky/pull/320)): Includes EC keys from halturin/applepay test cases (version not established).
- `vendor_tests/OpenPGP/test_initial_test.star` ([#255](https://github.com/verygoodsecurity/starlarky/pull/255)): Larky-specific.
- `vendor_tests/xmlsig/data/pkcs12_pfx_encoded_pw.star` ([#226](https://github.com/verygoodsecurity/starlarky/pull/226)): Encrypted PEM key; not found in python-xmlsig tests/data at eed8913.
- `vendor_tests/iso8583/test_iso8583.star` ([#112](https://github.com/verygoodsecurity/starlarky/pull/112)): 0 lines shared with pyiso8583 tests.
- `vendor_tests/iso8583/test_base24_payload.star` ([#138](https://github.com/verygoodsecurity/starlarky/pull/138)): BASE24 payload tests.
- `vendor_tests/lxml/test_lxml.star` ([#226](https://github.com/verygoodsecurity/starlarky/pull/226)): Individual cases cite lxml tests at ea954da3/f163e639.
- `vendor_tests/test_jsonpath.star` ([#117](https://github.com/verygoodsecurity/starlarky/pull/117)): Fixture is the Goessner JSONPath example document.
- `vendor_tests/test_escapes.star` ([#71](https://github.com/verygoodsecurity/starlarky/pull/71))
- `vendor_tests/jose/test_jose.star` ([#124](https://github.com/verygoodsecurity/starlarky/pull/124)): 3 of 224 lines shared with python-jose tests (PEM delimiters).
- `vendor_tests/jose/test_jws.star` ([#297](https://github.com/verygoodsecurity/starlarky/pull/297)): 0 of 32 lines shared with python-jose tests.
- `vendor_tests/jose/test_signatures_no_jws.star` ([#297](https://github.com/verygoodsecurity/starlarky/pull/297)): 0 of 48 lines shared with python-jose tests.

**stdlib_tests/**

- `stdlib_tests/test_asserts.star` ([#10](https://github.com/verygoodsecurity/starlarky/pull/10))
- `stdlib_tests/test_builtins.star` ([#161](https://github.com/verygoodsecurity/starlarky/pull/161))
- `stdlib_tests/test_codecs.star` ([#716](https://github.com/verygoodsecurity/starlarky/pull/716)): Expected values from CPython 3.10.13.
- `stdlib_tests/test_collections.star` ([#173](https://github.com/verygoodsecurity/starlarky/pull/173))
- `stdlib_tests/test_csv.star` ([#163](https://github.com/verygoodsecurity/starlarky/pull/163))
- `stdlib_tests/test_format_builtin.star` ([#723](https://github.com/verygoodsecurity/starlarky/pull/723)): Expected values from CPython 3.13.9.
- `stdlib_tests/test_hashlib.star` ([#124](https://github.com/verygoodsecurity/starlarky/pull/124)): Digest vectors computed with CPython hashlib.
- `stdlib_tests/test_io.star` ([#185](https://github.com/verygoodsecurity/starlarky/pull/185))
- `stdlib_tests/test_json.star` ([#716](https://github.com/verygoodsecurity/starlarky/pull/716))
- `stdlib_tests/test_larky.star` ([#12](https://github.com/verygoodsecurity/starlarky/pull/12))
- `stdlib_tests/test_larky_struct_eq.star` ([#715](https://github.com/verygoodsecurity/starlarky/pull/715))
- `stdlib_tests/test_percent_format.star` ([#722](https://github.com/verygoodsecurity/starlarky/pull/722)): Expected values from CPython 3.10.13.
- `stdlib_tests/test_property_descriptor.star` ([#770](https://github.com/verygoodsecurity/starlarky/pull/770))
- `stdlib_tests/test_python_builtins_compat.star` ([#713](https://github.com/verygoodsecurity/starlarky/pull/713)): Expected values from CPython 3.10.
- `stdlib_tests/test_re.star` ([#55](https://github.com/verygoodsecurity/starlarky/pull/55))
- `stdlib_tests/test_re_cpython.star` ([#717](https://github.com/verygoodsecurity/starlarky/pull/717)): Expected values from CPython 3.12.
- `stdlib_tests/test_str_python.star` ([#714](https://github.com/verygoodsecurity/starlarky/pull/714)): Expected values from CPython 3.12.14.
- `stdlib_tests/test_zipfile.star` ([#332](https://github.com/verygoodsecurity/starlarky/pull/332))
- `stdlib_tests/urllib/test_request.star` ([#231](https://github.com/verygoodsecurity/starlarky/pull/231))

**vgs_tests/**

- `vgs_tests/xmlsig-java-compatible/` ([#325](https://github.com/verygoodsecurity/starlarky/pull/325)): base.star adapted from vendor_tests/xmlsig/base.star; data is VGS-specific.
- `vgs_tests/cerebro/` ([#156](https://github.com/verygoodsecurity/starlarky/pull/156))
- `vgs_tests/vault/` ([#156](https://github.com/verygoodsecurity/starlarky/pull/156))
- `vgs_tests/chase/` ([#275](https://github.com/verygoodsecurity/starlarky/pull/275))
- `vgs_tests/http/` ([#231](https://github.com/verygoodsecurity/starlarky/pull/231))
- `vgs_tests/nts/` ([#327](https://github.com/verygoodsecurity/starlarky/pull/327))
- `vgs_tests/soap-sign/` ([#325](https://github.com/verygoodsecurity/starlarky/pull/325))
- `vgs_tests/metrics/` ([#458](https://github.com/verygoodsecurity/starlarky/pull/458))
- `vgs_tests/pgp/` ([#606](https://github.com/verygoodsecurity/starlarky/pull/606))
- `vgs_tests/cybersource/` ([#643](https://github.com/verygoodsecurity/starlarky/pull/643))
- `vgs_tests/proxy/` ([#705](https://github.com/verygoodsecurity/starlarky/pull/705))

## Notes

### pycryptodome

- The first port ([#90](https://github.com/verygoodsecurity/starlarky/pull/90), [#93](https://github.com/verygoodsecurity/starlarky/pull/93), [#95](https://github.com/verygoodsecurity/starlarky/pull/95); March-April 2021) took v3.10.0-v3.10.1;
  the two releases differ only in `Crypto/__init__.py`'s version. Files were
  later rewritten or added from newer releases, so the package is a mix:
  `Cipher/PKCS1_OAEP`, `Cipher/PKCS1_v1_5` and `Util/number` were re-synced
  to v3.10.4 in [#189](https://github.com/verygoodsecurity/starlarky/pull/189); the hash modules added in [#232](https://github.com/verygoodsecurity/starlarky/pull/232), [#234](https://github.com/verygoodsecurity/starlarky/pull/234) and [#235](https://github.com/verygoodsecurity/starlarky/pull/235) are
  pinned to v3.12.0 (`SHA3_224/384/512` need >= v3.10.4, the others do not
  discriminate); `Signature/DSS`, `PublicKey/DSA` and `Math/Primality`
  from v3.13.0-v3.14.1 ([#250](https://github.com/verygoodsecurity/starlarky/pull/250), [#295](https://github.com/verygoodsecurity/starlarky/pull/295)).
- `IO/PKCS8.star` is v3.14.1 plus the unwrap half of master commit
  [764c1e81](https://github.com/Legrandin/pycryptodome/commit/764c1e81b940596d10cc22e00ca99cc628e504a6)
  ("Add Ed25519 keys", 2022-04-15, first released in v3.15.0): it parses
  RFC 5915 OneAsymmetricKey v2 but keeps the pre-3.15 `wrap(key_params=...)`.
- Library and tests were not always synced together: `Cipher/PKCS1_v1_5.star`
  is v3.10.4 while `vendor_tests/Crypto/Cipher/test_pkcs1_15.star` is v3.11.0.
- Most files are heavily rewritten: native jcrypto (BouncyCastle) calls replace
  the C extensions, errors use `Result`, loops use `larky.while_true` ([#655](https://github.com/verygoodsecurity/starlarky/pull/655)).
  `Hash/SHA` and `Signature/PKCS1_v1_5` are legacy alias shims ([#124](https://github.com/verygoodsecurity/starlarky/pull/124)).
  Five files of the 3.10.1 group are empty (`__init__`, `Math/__init__`,
  `Protocol/__init__`, `Util/__init__`, `Signature/PKCS1_PSS`) and carry no
  header. Later edits ([#96](https://github.com/verygoodsecurity/starlarky/pull/96), [#97](https://github.com/verygoodsecurity/starlarky/pull/97), [#102](https://github.com/verygoodsecurity/starlarky/pull/102), [#185](https://github.com/verygoodsecurity/starlarky/pull/185), [#189](https://github.com/verygoodsecurity/starlarky/pull/189), [#250](https://github.com/verygoodsecurity/starlarky/pull/250), [#288](https://github.com/verygoodsecurity/starlarky/pull/288), [#583](https://github.com/verygoodsecurity/starlarky/pull/583), [#655](https://github.com/verygoodsecurity/starlarky/pull/655), [#718](https://github.com/verygoodsecurity/starlarky/pull/718)) did not
  move any file out of its range; `vendor_tests/Crypto/Util/test_Padding.star`
  now expects pycryptodome 3.23.0 error messages ([#718](https://github.com/verygoodsecurity/starlarky/pull/718)).
- Many files have no lines that differ between releases; their pin rests on
  the commit they were added in, not on their content.

### cryptography

- The port ([#226](https://github.com/verygoodsecurity/starlarky/pull/226), 2021-12-09) is 35.0.0, but 35.0.0 implements some
  functions in Rust. For those, the pure-Python bodies of 3.4.x (matching
  3.4.8) were used instead: `hazmat/_der` (absent in 35.0.0), the `load_*`
  functions in `x509/base`, `encode/decode_dss_signature` in
  `asymmetric/utils`, `backends/__init__`, `utils` and
  `serialization/pkcs7`. Treat the package as a 35.0.0 / 3.4.8 mix.
- 36.0.0 (2021-11-21) was already released when the port landed.
- `LICENSE`, `LICENSE.APACHE` and `LICENSE.BSD` are upstream's (only a
  trailing newline/period differs in `LICENSE`).
- Many small files are thin shims with few upstream lines; their version is
  inferred from the rest of the package.

### pyOpenSSL

- `OpenSSL/crypto.star` is implemented on jcrypto/jopenssl natives with large
  parts commented out. `crypto.py` is identical in 20.0.0 and 20.0.1.
- `vendor_tests/OpenSSL/test_crypto.star` is an empty placeholder.

### OpenPGP-Python

- Ported from the VGS fork
  [mahmoudimus/OpenPGP-Python](https://github.com/mahmoudimus/OpenPGP-Python)
  (branch `fork`, commit a174c38, "Removed pycrypto and introduced
  pycryptodome", 2022-04-01) of singpolyma/OpenPGP-Python. The parent is ISC
  (COPYING, Stephen Paul Weber 2010); the fork adds an MIT LICENSE (Very Good
  Security 2022). The fork's `cryptography.py` backend is not ported.

### python-jose

- `vendor/jose` (except `jwt.star`) matches python-jose's master branch
  between 89c3d41 (2020-09-07, JWK objects in `jws`) and 8572088
  (2020-12-07, the last commit with the PyCrypto backend that
  `backends/pycrypto_backend.star` ports). In that range upstream only changed
  `backends/ecdsa_backend.py`, which is not ported, so every commit in it is
  equivalent for these files. No release matches: 3.2.0 has no `jwe`,
  `backends/native` or `AESKey`; 3.3.0 dropped PyCrypto.
- `jwt.star` is a translation of python-jose 3.5.0's `jose/jwt.py`, made on
  branch `fix/jose-jwt` (87d6433f), which also adds
  `vendor_tests/jose/test_jwt.star` (52 of the 53 tests in 3.5.0's
  `tests/test_jwt.py`). Until that branch is merged, `jwt.star` here is the
  earlier untranslated copy of `jwt.py`, which cannot be loaded.
- The per-file headers for `vendor/jose` are added on `fix/jose-jwt`
  (a4cf5705); this change does not edit those files.

### python-xmlsig

- python-xmlsig's `src/` is byte-identical in 0.1.6 and 0.1.7 (only
  `setup.py`, `LICENSE` and `PKG-INFO` differ). The port has
  `TransformHmacSha224` (added in 0.1.6) and lacks f0233bc9 (2021-12-08).
- `vgs/xmlsig-java-compatible` is a copy of `vendor/xmlsig` with VGS changes
  (stdlib ElementTree instead of lxml, X509SubjectName handling, enveloped
  signature transform; 2-28 lines per file).
- `vendor_tests/xmlsig`: only sign case 1 is ported; `tests/` is not in the
  sdist, so it was compared at eed8913 (0.1.7).

### lxml

- `lxml/etree.star` and `lxml/__init__.star` reimplement the lxml.etree API
  over Larky's ElementTree/xmllib (VGS-original).
- `lxml/_c14n.star` is PyXML 0.8.4's `xml/dom/ext/c14n.py` (PR [#223](https://github.com/verygoodsecurity/starlarky/pull/223) calls it
  a "4-Suite XML backport"; the same file later appears in ZSI/wstools and
  pysimplesoap with a 2006 `$Date`, which Larky's header does not have).
- `lxml/_xmlwriter.star` combines `XMLWriter` from collective/elementtreewriter
  1.2 (`xmlwriter.py`, same in 1.1) with the xmlprinter from Amara 2.0.0
  (`lib/writers/_xmlprinters.py`; the section is marked "modified from python
  amara2 library").

### bazel-skylib

- Only parts of these files are skylib: `_after_each`, `_before_each`,
  `_uniq` in `collections.star`; the rest of each file is VGS additions (e.g.
  Python-style `Set()`). `collections.namedtuple` ([#173](https://github.com/verygoodsecurity/starlarky/pull/173)) has the CPython
  `Lib/collections/__init__.py` docstring and `kwlist`/`softkwlist` from
  CPython 3.10/3.11 `Lib/keyword.py`.

### CPython

- `xml/etree/ElementTree.star` and `ElementPath.star` were copied from CPython
  main between 3.10.0a3 and 3.10.0a5 (2020-11-09..2021-02-24): ElementPath
  has the XPath `!=` predicate (bpo-40624, never in 3.9) and ElementTree still
  sets `parser.specified_attributes` (removed by bpo-42151 before 3.10.0a6).
- `urllib/parse.star` says "Direct copy from .../blob/3.9/..."; it was taken
  from the 3.9 branch between the 3.9.5 and 3.9.6 releases (8a59574), and
  v3.9.6 has the identical file. Later CVE fixes (3.9.17+: C0 control
  stripping, bracketed host checks) are not in it.
- `urllib/request.star` (first added in [#10](https://github.com/verygoodsecurity/starlarky/pull/10), 2020-11) was reworked in [#231](https://github.com/verygoodsecurity/starlarky/pull/231);
  only docstrings and structure are CPython, the HTTP code is VGS.
- `hashlib.star` and `random.star` take only their module docstrings from
  CPython; `io/_io.star` and `string.star` start from Python 2 modules.
- VGS-original `binascii.star` and `re.star` carry CPython docstrings:
  `binascii` uses the 3.8-3.10 `__doc__` strings, `re` the 3.7-3.9
  `Lib/re.py` function docstrings and syntax summary.

### backports.csv

- `csv.star` translates backports.csv, itself a backport of Python 3's
  `csv.py`; its overlap with CPython `Lib/csv.py` is all shared with
  backports.csv. 1.0.6 and 1.0.7 ship the same `csv.py`.

### multidict

- Pure-Python `_multidict_py.py` reworked into Larky structs. The content
  matches 4.7.6-5.1.0 equally; 5.1.0 was the release current at port time.

### MasterCard

- PR [#312](https://github.com/verygoodsecurity/starlarky/pull/312) was closed unmerged; its commits were pushed directly
  (4d927d4b). VGS additions: explicit timestamp, legacy wrapper and its
  `test_legacy_*` tests. The upstream repository LICENSE is MIT; the per-file
  headers kept in the Larky files carry BSD-style "Copyright 2019-2021
  Mastercard" text.

### option

- Interface and docstring port of `option/result.py` (Ok/Err `Result`
  only); the implementation is rewritten over Larky's Java `Result`.

### pydukpt

- Ported from [pydukpt](https://github.com/mahmoudimus/pydukpt) 0.1.0, tag
  v0.1.0 (commit 3778da71dab2e11a185915a453e9175d6fe45aea). The PyPI 0.1.0
  sdist (sha256 `2fad5a2981985496188ddfe7f1983d68ab06e68718145f61c9b51ed0674bcfca`)
  and wheel (`5b95cbc41d0b7cc0cc1962d52585daa93d3bc607d3aaaa94b8d9a98e5a1535ae`)
  ship `src/pydukpt/*.py`, `tests/` and `NOTICE` byte-identical to the tag.
- pydukpt is MIT (Copyright (c) 2026 Mahmoud Abdelkader) and, per its NOTICE,
  written from ANSI X9.24-1:2009 and ISO 9564-1 without code from other DUKPT
  implementations. `pydukpt.compat` reproduces the API names of
  [dukpt](https://pypi.org/project/dukpt/1.0.1/) 1.0.1
  ([allen246/dukpt](https://github.com/allen246/dukpt), from
  chokepoint/DUKPT, MIT, Copyright (c) 2017 Chokepoint) with new code. Each
  file carries the MIT notice and the parts of NOTICE that apply to it.
- Test vectors (from NOTICE): the ANSI X9.24-1 A.4.2 values are as
  transcribed in [moov-io/dukpt](https://github.com/moov-io/dukpt)
  (Apache-2.0) `pkg/des/des_test.go` at 9cb72e64; `vectors.json` was computed
  with moov-io/dukpt and cross-checked with
  [varhenn/dukpt_des_pinblock](https://github.com/varhenn/dukpt_des_pinblock)
  (BSD-2-Clause) and dukpt 1.0.1. Neither is included.
- Module layout: `@vendor//pydukpt` is `pydukpt/__init__.py` (the functional
  API, also as the `pydukpt` struct), `@vendor//pydukpt/pinblock` and
  `@vendor//pydukpt/compat` the submodules, `@vendor//pydukpt/_core` the
  implementation. `_core.py`'s module-level names are exported from
  `_core.star` as the `core` struct (Larky cannot load a private name).
- Larky differences: `bytes` and `bytearray` are accepted where pydukpt takes
  bytes-like values (no `memoryview`). Exceptions are `fail()`s whose message
  starts with the Python exception name (`TypeError: `, `ValueError: `,
  `KSNExhaustedError: `, `InvalidDUKPTArguments: `); `KSNExhaustedError` and
  `InvalidDUKPTArguments` are functions that fail that way. `DUKPT`, `Server`
  and `Client` are constructor functions over `larky.mutablestruct`, so the
  `DUKPT` class attributes are read from instances. TDES is built from
  single DES, as upstream does, so keys with equal halves work.
- Tests: Hypothesis strategies are replaced by a fixed-seed generator
  (`conftest.Examples`) with the same example counts (50 or 100). The slow
  2**20 - 1 KSN walk runs (about 10 s). `test_live_dukpt_1_0_1_if_installed`
  is not ported (it imports dukpt and bitstring). `fixtures/vectors.star` was
  generated from `vectors.json` with a one-off script and checked to load
  back to the same value.
- Replaces an earlier, unmerged port of dukpt 1.0.1 as `@vendor//dukpt` on
  the same PR.

### luhn

- Docstrings and doctests are luhn 0.2.0's; the implementation is rewritten
  (see Known divergences). `checksum()` is not exported.

### pyiso8583

- First added as `vendor/ISO8583{Decoder,Encoder,Specs}.star`, moved to
  `vendor/iso8583/` in [#118](https://github.com/verygoodsecurity/starlarky/pull/118). Later local edits: [#138](https://github.com/verygoodsecurity/starlarky/pull/138), [#144](https://github.com/verygoodsecurity/starlarky/pull/144).

### asserts

- `asserts.star` reimplements assertpy's assertion API ("modeled after
  assertpy") with assertpy 1.1 docstrings; `_impl_function_name` comes from
  bazel-skylib `lib/unittest.bzl` (<= 1.0.3).

### ElementTree

- `SimpleXMLTreeBuilder.star` keeps upstream's `$Id` (r1862, 2004-06-18)
  header; 1.2.6 is the last 1.2.x release containing r1862. The upstream site
  (effbot.org) is offline; compared against a mirror. The helper was split out
  in [#226](https://github.com/verygoodsecurity/starlarky/pull/226) ([#222](https://github.com/verygoodsecurity/starlarky/pull/222)).

### attrs

- Ported against attrs 26.1.0. attrs builds each class's `__init__`,
  `__repr__`, `__eq__` and ordering methods as source and `exec()`s it; Larky
  has no `exec`, so the `.star` files keep upstream's API and docstrings while
  the generated methods, field descriptors, validators and setters are native
  Java in `modules/attrs` (the `jattrs` module). The pin is exact as to the
  version written against, not a line-level match with upstream.
- `_make.star` also covers `_funcs.py` (`asdict`, `astuple`, `has`,
  `evolve`) and `_config.py` (`set_run_validators`, `get_run_validators`).
  Not ported: `converters`/`Converter`, `validators.not_` and
  `validators.disabled`, `cmp_using`, `resolve_types`, `assoc`, `inspect`,
  `filters`, `exceptions`, hashing by value and pickling. The `attr` module
  docstring lists these and the other differences.

## Open licensing questions

Facts collected while pinning, for review. Nothing here has been changed.

- **python-xmlsig** (`vendor/xmlsig`, `vgs/xmlsig-java-compatible`,
  `vendor_tests/xmlsig`): python-xmlsig's LICENSE changed from BSD-3-Clause to
  AGPL-3.0 in commit 5846f7cd7c (2021-11-20), which was released as 0.1.7;
  0.1.6's LICENSE and `setup.py` say BSD. The port was opened as #225 on
  2021-12-08 and merged with #226 on 2021-12-09. 0.1.6 and 0.1.7 have
  identical code, so the code does not show which release (and LICENSE) was
  used. Independently of that, the per-file headers of upstream's
  `src/xmlsig/*.py` say "License AGPL-3.0 or later" in 0.1.4, 0.1.6 and 0.1.7
  alike; the Larky files carry no license header. PyPI's current metadata for
  the package gives license "LGPL-3" with an AGPL-3 classifier.
- **`vendor/lxml/_xmlwriter.star`**: the `XMLWriter` part is derived from
  collective/elementtreewriter, licensed under the D-FSL (Deutsche
  Freie Software Lizenz), a copyleft license; the xmlprinter part is from
  Amara 2 (Apache-2.0). Neither upstream header is kept in the file.
- **Ported files that do not carry upstream's license or copyright header.**
  Checked against the upstream file at the pinned ref where a local copy was
  available.

  Upstream file has a per-file copyright/license header that the Larky file
  lacks (54 files):

  - pycryptodome; BSD-2-Clause AND Unlicense: `vendor/Crypto/Cipher/{Blowfish,CAST,ChaCha20,ChaCha20_Poly1305,Salsa20}.star`, `vendor/Crypto/Hash/{__init__,BLAKE2b,keccak,MD2,MD4,RIPEMD,RIPEMD160,SHA,SHA3_224,SHA3_384,SHA3_512,SHAKE256}.star`, `vendor/Crypto/IO/{__init__,PEM}.star`, `vendor/Crypto/Protocol/SecretSharing.star`, `vendor/Crypto/PublicKey/ElGamal.star`, `vendor/Crypto/Signature/__init__.star`, `vendor/Crypto/Util/strxor.star`, `vendor/Crypto/st_common.star`
  - cryptography; Apache-2.0 OR BSD-3-Clause: `vendor/cryptography/__init__.star`
  - OpenPGP-Python (mahmoudimus fork); ISC AND MIT: `vendor/OpenPGP/__init__.star`
  - python-xmlsig; BSD-3-Clause (0.1.6) or AGPL-3.0 (0.1.7); see licensing questions: `vendor/xmlsig/algorithms/{base,hmac,rsa}.star`, `vendor/xmlsig/{constants,ns,signature_context,template,utils}.star`
  - option; MIT: `vendor/option/result.star`, `vendor_tests/option/test_result.star`
  - ElementTree (effbot); HPND-style ElementTree license: `vendor/elementtree/_SimpleXMLTreeBuilderHelper.star`
  - PyXML; PyXML license (upstream header not kept): `vendor/lxml/_c14n.star`
  - elementtreewriter + Amara 2; D-FSL (elementtreewriter); Apache-2.0 (Amara): `vendor/lxml/_xmlwriter.star`
  - assertpy (+ bazel-skylib unittest.bzl); BSD-3-Clause (assertpy); Apache-2.0 (skylib): `vendor/asserts.star`
  - python-xmlsig (via vendor/xmlsig); as vendor/xmlsig (undetermined): `vgs/xmlsig-java-compatible/algorithms/{base,hmac,rsa}.star`, `vgs/xmlsig-java-compatible/{constants,ns,signature_context,template,utils}.star`
  - CPython; PSF-2.0: `stdlib/textwrap.star`
  - pycryptodome (SelfTest); BSD-2-Clause AND Unlicense: `vendor_tests/Crypto/Util/test_Padding.star`, `vendor_tests/Crypto/Hash/{common,test_HMAC}.star`, `vendor_tests/Crypto/Cipher/test_CFB.star`
  - bazel-skylib; Apache-2.0: `stdlib_tests/test_dicts.star`

  Upstream file has no per-file header (the license is in the repository's
  LICENSE only) and the Larky file has none either (49 files). Of these
  packages only `vendor/cryptography` ships upstream's LICENSE files:

  - pycryptodome; BSD-2-Clause AND Unlicense: `vendor/Crypto/Cipher/__init__.star`
  - pyOpenSSL; Apache-2.0: `vendor/OpenSSL/crypto.star`
  - OpenPGP-Python (mahmoudimus fork); ISC AND MIT: `vendor/OpenPGP/Crypto.star`, `vendor_tests/OpenPGP/{data_test_fixtures,test_Crypto,test_OpenPGP}.star`
  - python-xmlsig; BSD-3-Clause (0.1.6) or AGPL-3.0 (0.1.7); see licensing questions: `vendor/xmlsig/__init__.star`, `vendor/xmlsig/algorithms/__init__.star`
  - pyiso8583; MIT: `vendor/iso8583/{decoder,encoder,specs}.star`, `vendor_tests/iso8583/test_encode.star`
  - multidict; Apache-2.0: `vendor/multidict.star`, `vendor_tests/multidict/test_mutable_multidict.star`
  - oauth1-signer-python (Mastercard); MIT: `vendor/MasterCard/oauth1/__init__.star`
  - luhn; MIT: `vendor/luhn.star`
  - python-xmlsig (via vendor/xmlsig); as vendor/xmlsig (undetermined): `vgs/xmlsig-java-compatible/__init__.star`, `vgs/xmlsig-java-compatible/algorithms/__init__.star`
  - CPython; PSF-2.0: `stdlib/{_markupbase,base64,bz2,enum,operator,random,reprlib,string,uuid,xmllib,zipfile}.star`, `stdlib/io/{_io,StringIO}.star`, `stdlib/urllib/{parse,request}.star`, `stdlib_tests/{test_base64,test_operator,test_struct,test_this,test_zlib}.star`, `stdlib_tests/xml/etree/test_elementtree.star`
  - backports.csv; PSF-2.0: `stdlib/csv.star`
  - python-xmlsig; as vendor/xmlsig (undetermined): `vendor_tests/xmlsig/{base,test_xmlsig}.star`, `vendor_tests/xmlsig/data/{rsacert_pem,rsakey_pem,rsapub_pem,sign1_in_xml,sign1_out_xml}.star`
  - PyXML; PyXML license: `vendor_tests/lxml/test_c14n.star`
  - starlark-go; BSD-3-Clause: `stdlib_tests/test_bytes.star`

  Not checked, no local copy of the upstream file (4 files):

  - luhn; MIT: `vendor_tests/luhn/test_luhn.star`
  - CPython; PSF-2.0: `stdlib_tests/test_uuid.star`, `stdlib_tests/urllib/test_parse.star`
  - Adafruit_CircuitPython_Binascii; MIT: `stdlib_tests/test_binascii.star`

  The research notes for `stdlib_tests/test_binascii.star` record that it
  drops upstream's SPDX (MIT) header.

## Known divergences found while pinning

Facts only; none of these is fixed here.

- `vendor/luhn.star`: `generate()` returns `10 - (sum % 10)` without upstream's
  final `% 10`, so it returns 10 where luhn 0.2.0 returns 0. Checked
  2026-10-13 by running both over the inputs `'1'`..`'99999'`: the results
  differ for 9,999 of the 99,999 inputs, all of them Larky 10 / upstream 0,
  which are exactly the inputs where upstream returns 0.
- `vendor/past/builtins.star`: `cmp(a, b)` computes `(a >= b) - (a < b)`, so
  `cmp(a, a)` returns 1 instead of 0 (checked: `cmp(1, 1)` and
  `cmp("a", "a")` return 1; `cmp(1, 2)` returns -1). `vendor/lxml/_c14n.star`
  sorts with it through `functools.cmp_to_key`.
- `vendor_tests/OpenSSL/test_crypto.star` is empty (0 bytes).
- `vendor_tests/Crypto/Cipher/test_CBC.star` is entirely commented out; it has
  no executable lines.
