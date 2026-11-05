"""Decrypt JWEs made by Larky with python-jose and joserfc.

larky_tokens.json holds [alg, enc, key, zip, token] rows that
make_larky_tokens.star wrote. A*GCMKW tokens are decrypted with joserfc
(python-jose does not support A*GCMKW); "zip": "DEF" tokens with python-jose
at the commit Larky's jose is ported from, since both write zlib data. Run
from the repository root:

    uv run --no-project \
        --with "python-jose[pycryptodome] @ git+https://github.com/mpdavis/python-jose@8572088b76f7c0d31068f38aeca0d4273ae8bc8f" \
        --with joserfc==1.7.5 python -I \
        larky/src/test/tools/jose_parity/check_larky_tokens.py \
        larky/src/test/tools/jose_parity/larky_tokens.json \
        >| larky/src/test/tools/jose_parity/check_larky_tokens.out

It fails unless every token decrypts to PLAINTEXT.
"""
import json
import sys
from importlib.metadata import version

from jose import jwe as pyjose_jwe
from joserfc import jwe as rfc_jwe
from joserfc.jwk import OctKey
from joserfc.jwe import JWERegistry

PLAINTEXT = b"hello" * 20


def main(path: str) -> None:
    print("python-jose %s (git 8572088b), joserfc %s" % (version("python-jose"), version("joserfc")))
    rows = json.load(open(path))
    for alg, enc, key, zip_, token in rows:
        if zip_ is None:
            registry = JWERegistry(algorithms=[alg, enc])
            plaintext = rfc_jwe.decrypt_compact(token, OctKey.import_key(key.encode()),
                                                registry=registry).plaintext
            checker = "joserfc"
        else:
            plaintext = pyjose_jwe.decrypt(token, key.encode())
            checker = "python-jose"
        assert plaintext == PLAINTEXT, (alg, enc, zip_)
        print("ok %s %s zip=%s (%s)" % (alg, enc, zip_, checker))
    print("%d of %d tokens decrypted" % (len(rows), len(rows)))


if __name__ == "__main__":
    main(sys.argv[1])
