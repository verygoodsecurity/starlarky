package com.verygood.security.larky.modules.crypto.Cipher;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.modes.gcm.GCMUtil;

import java.nio.ByteBuffer;
import java.security.ProviderException;

/**
 * This class represents the GHASH function defined in NIST 800-38D
 * under section 6.4. It needs to be constructed w/ a hash subkey, i.e.
 * block H. Given input of 128-bit blocks, it will process and output
 * a 128-bit block.
 *
 * <p>This function is used in the implementation of GCM mode.
 *
 * @since 1.8
 */
public class GHash implements StarlarkValue {

    private static long getLong(byte[] buffer, int offset) {
        long result = 0;
        int end = offset + 8;
        for (int i = offset; i < end; ++i) {
            result = (result << 8) + (buffer[i] & 0xFF);
        }
        return result;
    }

    private static void putLong(byte[] buffer, int offset, long value) {
        int end = offset + 8;
        for (int i = end - 1; i >= offset; --i) {
            buffer[i] = (byte) value;
            value >>= 8;
        }
    }

    private static final int AES_BLOCK_SIZE = 16;

    // Multiplies state[0], state[1] by subkeyH[0], subkeyH[1].
    private static void blockMult(long[] st, long[] subH) {
        GCMUtil.multiply(st, subH);
    }

    /* subkeyHtbl and state are stored in long[] for GHASH intrinsic use */

    // hashtable subkeyHtbl; holds 2*9 powers of subkeyH computed using carry-less multiplication
    private long[] subkeyHtbl;

    // buffer for storing hash
    private final long[] state;

    // variables for save/restore calls
    private long stateSave0, stateSave1;

    /**
     * Initializes the cipher in the specified mode with the given key
     * and iv.
     *
     * @param subkeyH the hash subkey
     *
     * @exception ProviderException if the given key is inappropriate for
     * initializing this digest
     */

    public GHash(byte[] subkeyH) throws ProviderException {
        if ((subkeyH == null) || subkeyH.length != AES_BLOCK_SIZE) {
            throw new ProviderException("Internal error");
        }
        state = new long[2];
        long[] longs = GCMUtil.asLongs(subkeyH);
        subkeyHtbl = new long[2*9];
        subkeyHtbl[0] = longs[0];
        subkeyHtbl[1] = longs[1];
    }

    /**
     * Resets the GHASH object to its original state, i.e. blank w/
     * the same subkey H. Used after digest() is called and to re-use
     * this object for different data w/ the same H.
     */
    void reset() {
        state[0] = 0;
        state[1] = 0;
    }

    /**
     * Save the current snapshot of this GHASH object.
     */
    void save() {
        stateSave0 = state[0];
        stateSave1 = state[1];
    }

    /**
     * Restores this object using the saved snapshot.
     */
    void restore() {
        state[0] = stateSave0;
        state[1] = stateSave1;
    }

    private static void processBlock(byte[] data, int ofs, long[] st, long[] subH) {
        st[0] ^= getLong(data, ofs);
        st[1] ^= getLong(data, ofs + 8);
        blockMult(st, subH);
    }

    @StarlarkMethod(
          name = "digest",
          doc = "Return the digest of the bytes passed to the update() method\n" +
              "so far as a bytes object.",
          useStarlarkThread = true
      )
      public LarkyByteLike digest(StarlarkThread thread) throws EvalException {
        byte[] resBuf = this.digest();
        return LarkyByte.builder(thread).setSequence(resBuf).build();
      }

    @StarlarkMethod(
          name = "update",
          doc = "Update the hash object with the bytes in data. Repeated calls\n" +
              "are equivalent to a single call with the concatenation of all\n" +
              "the arguments.",
          parameters = {@Param(name = "data", allowedTypes = {
              @ParamType(type = LarkyByteLike.class)
          })}
      )
    public void update(LarkyByteLike data) {
        byte[] input = data.getBytes();
        update(input);
    }

    void update(byte[] in) {
        update(in, 0, in.length);
    }

    void update(byte[] in, int inOfs, int inLen) {
        if (inLen == 0) {
            return;
        }
        ghashRangeCheck(in, inOfs, inLen, state, subkeyHtbl);
        processBlocks(in, inOfs, inLen/AES_BLOCK_SIZE, state, subkeyHtbl);
    }

    // Maximum buffer size rotating ByteBuffer->byte[] intrinsic copy
    private static final int MAX_LEN = 1024;

    // Will process as many blocks it can and will leave the remaining.
    int update(ByteBuffer src, int inLen) {
        inLen -= (inLen % AES_BLOCK_SIZE);
        if (inLen == 0) {
            return 0;
        }

        int processed = inLen;
        byte[] in = new byte[Math.min(MAX_LEN, inLen)];
        while (processed > MAX_LEN ) {
            src.get(in, 0, MAX_LEN);
            update(in, 0 , MAX_LEN);
            processed -= MAX_LEN;
        }
        src.get(in, 0, processed);
        update(in, 0, processed);
        return inLen;
    }

    void doLastBlock(ByteBuffer src, int inLen) {
        int processed = update(src, inLen);
        if (inLen == processed) {
            return;
        }
        byte[] block = new byte[AES_BLOCK_SIZE];
        src.get(block, 0, inLen - processed);
        update(block, 0, AES_BLOCK_SIZE);
    }

    private static void ghashRangeCheck(byte[] in, int inOfs, int inLen, long[] st, long[] subH) {
        if (inLen < 0) {
            throw new RuntimeException("invalid input length: " + inLen);
        }
        if (inOfs < 0) {
            throw new RuntimeException("invalid offset: " + inOfs);
        }
        if (inLen > in.length - inOfs) {
            throw new RuntimeException("input length out of bound: " +
                                       inLen + " > " + (in.length - inOfs));
        }
        if (inLen % AES_BLOCK_SIZE != 0) {
            throw new RuntimeException("input length/block size mismatch: " +
                                       inLen);
        }

        // These two checks are for C2 checking
        if (st.length != 2) {
            throw new RuntimeException("internal state has invalid length: " +
                                       st.length);
        }
        if (subH.length != 18) {
            throw new RuntimeException("internal subkeyHtbl has invalid length: " +
                                       subH.length);
        }
    }

    private static void processBlocks(byte[] data, int inOfs, int blocks, long[] st, long[] subH) {
        int offset = inOfs;
        while (blocks > 0) {
            processBlock(data, offset, st, subH);
            blocks--;
            offset += AES_BLOCK_SIZE;
        }
    }

    byte[] digest() {
        byte[] result = new byte[AES_BLOCK_SIZE];
        putLong(result, 0, state[0]);
        putLong(result, 8, state[1]);
        reset();
        return result;
    }
}
