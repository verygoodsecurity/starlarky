package com.verygood.security.larky.modules.crypto.Hash;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.ext.ByteList;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.CipherParameters;


public class LarkyPoly1305MAC implements StarlarkValue {
    final private Poly1305 mac;

    public LarkyPoly1305MAC(byte[] key, byte[] nonce) {
        this.mac = new Poly1305();
        CipherParameters cipherParams = new ParametersWithIV(new KeyParameter(key), nonce);
        this.mac.init(cipherParams);
    }

    @StarlarkMethod(
            name = "update",
            doc = "Update the mac object with the bytes in data.",
            parameters = {@Param(name = "data", allowedTypes = {
                    @ParamType(type = StarlarkBytes.class)
            })}
    )
    public void update(StarlarkBytes data) {
        byte[] input = data.toByteArray();
        this.mac.update(input, 0, input.length);
    }

    @StarlarkMethod(
            name = "digest",
            doc = "Return the poly1305mac of the bytes passed to the update() method\n" +
                    "so far as a bytes object.",
            useStarlarkThread = true
    )
    public StarlarkBytes digest(StarlarkThread thread) throws EvalException {
        return StarlarkBytes.of(thread.mutability(), this.getDigest());
    }

    @StarlarkMethod(
            name = "hexdigest",
            doc = "Like digest() except the digest is returned as a string\n" +
                    "of double length, containing only hexadecimal digits."
    )
    public String hexDigest() {
        return ByteList.wrap(this.getDigest()).hex();
    }

    private byte[] getDigest() {
        byte[] resBuf = new byte[this.mac.getMacSize()];
        this.mac.doFinal(resBuf, 0);
        return resBuf;
    }
}
