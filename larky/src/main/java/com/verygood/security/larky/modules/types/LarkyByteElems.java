package com.verygood.security.larky.modules.types;

import com.verygood.security.larky.modules.codecs.TextUtil;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.StarlarkInt;

import java.util.AbstractList;

// A function that returns "fromValues".
@StarlarkBuiltin(name = "bytes.elems")
public class LarkyByteElems extends AbstractList<StarlarkInt> implements Sequence<StarlarkInt> {

  final private LarkyByteLike byteArray;

  public LarkyByteElems(LarkyByteLike byteArray) {
    this.byteArray = byteArray;
  }

  public byte[] getBytes() {
    return getLarkyByteArr().getBytes();
  }

  public LarkyByteLike getLarkyByteArr() {
    return byteArray;
  }

  @Override
  public void repr(Printer printer) {
    printer.append(
        String.format("b'\"%s\".elems()'",
            TextUtil.decodeUTF8(
                this.byteArray.getBytes(),
                this.byteArray.getBytes().length
            )));
  }

  @Override
  public StarlarkInt get(int index) {
    return this.byteArray.get(index);
  }

  @Override
  public int size() {
    return this.byteArray.size();
  }

  @Override
  public Sequence<StarlarkInt> getSlice(Mutability mu, int start, int stop, int step) {
    return this.byteArray.getSlice(mu, start, stop, step);
  }
}
