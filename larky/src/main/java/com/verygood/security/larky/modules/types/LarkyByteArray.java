package com.verygood.security.larky.modules.types;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;

import com.verygood.security.larky.modules.codecs.TextUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;


@StarlarkBuiltin(
    name = "bytearray",
    documented = false
)
public final class LarkyByteArray extends LarkyByteLike implements HasBinary {

  private final StarlarkThread currentThread;

  public static Builder builder(StarlarkThread thread) {
    return new Builder(thread);
  }

  // Returns {@code this op that}, if thisLeft, or {@code that op this} otherwise
  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    switch (op) {
      case STAR:
        if (that instanceof StarlarkInt) {
          LarkyByteLike result = BinaryOperations.multiply(
              this,
              ((StarlarkInt) that).toIntUnchecked());
          this.setSequenceStorage(result.getSequenceStorage());
          return this;
        }
      case PLUS:
        if (that instanceof LarkyByteLike) {
          LarkyByteLike that_ = (LarkyByteLike) that;
          if (thisLeft) {
            this.setSequenceStorage(BinaryOperations.add(this, that_));
            return this;
          } else {
            that_.setSequenceStorage(BinaryOperations.add(that_, this));
            return that;
          }
        }
        // fallthrough
      default:
        // unsupported binary operation!
        return null;
//        throw Starlark.errorf(
//            "unsupported binary operation: %s %s %s", Starlark.type(this), op, Starlark.type(that));
    }
  }

  public static class Builder implements ByteLikeBuilder {
    // required parameters
    private final StarlarkThread currentThread;
    private Sequence<StarlarkInt> sequence;

    private Builder(StarlarkThread currentThread) {
      this.currentThread = currentThread;
    }

    @Override
    public ByteLikeBuilder setSequence(@NotNull Sequence<?> seq) throws EvalException {
      try {
        sequence = StarlarkList.copyOf(
            currentThread.mutability(),
            Sequence.cast(seq, StarlarkInt.class, "could not cast!")
                .stream()
                .mapToInt(StarlarkInt::toIntUnchecked)
                .map(UnsignedBytes::checkedCast)
                .mapToObj(Number.class::cast)
                .map(Number::byteValue)
                .map(Byte::toUnsignedInt)
                .map(StarlarkInt::of)
                .collect(Collectors.toList()));
      } catch (IllegalArgumentException e) {
        throw Starlark.errorf("%s, want value in unsigned 8-bit range", e.getMessage());
      }
      return this;
    }

    @Override
    public LarkyByteArray build() throws EvalException {
      return new LarkyByteArray(this);
    }

  }

  private LarkyByteArray(Builder builder) {
    currentThread = builder.currentThread;
    setSequenceStorage(builder.sequence);
  }


  @Override
  protected ByteLikeBuilder builder() {
    return builder(this.currentThread);
  }

  @Override
  public void setSequenceStorage(Sequence<StarlarkInt> store) {
    super.setSequenceStorage(store);
  }

  @Override
  public byte[] getBytes() {
    return Bytes.toArray(
        this.getSequenceStorage().stream()
            .map(StarlarkInt::toNumber)
            .map(Number::byteValue)
            .collect(Collectors.toList()));
  }

  @Override
  public void str(Printer printer) {
    printer.append(TextUtil.starlarkStringTranscoding(getBytes()));
  }

  @Override
  public void repr(Printer printer) {
    String s = TextUtil.starlarkDecodeUtf8(getBytes());
    String s2 = String.format("b\"%s\"", s);
    printer.append(s2);
  }


  @Override
  public boolean isImmutable() {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LarkyByteLike)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    return this.compareTo((LarkyByteLike) obj) == 0;
  }

  @Override
  public StarlarkInt get(int index) {
    return this.getSequenceStorage().get(index);
  }


  /**
   * Inserts an element at a given position to the list.
   *
   * @param index   the new element's index
   * @param element the element to add
   */
  public void addElementAt(int index, LarkyByteLike element) throws EvalException {
    //ArrayList<StarlarkInt> l = new ArrayList<>(this.getSequenceStorage().getImmutableList());
    //l.add(index, element);
    StarlarkList<StarlarkInt> s = StarlarkList.newList(currentThread.mutability());
    s.addElements(this.getSequenceStorage()
        .getSlice(this.currentThread.mutability(), 0, index, 1));
    s.addElements(element.getSequenceStorage());
    s.addElements(this.getSequenceStorage()
        .getSlice(currentThread.mutability(), index, size(), 1));
    this.setSequenceStorage(s);
  }

  /**
   * Appends all the elements to the end of the list.
   *
   * @param elements the elements to add
   */
  public void addElements(Iterable<? extends StarlarkInt> elements) throws EvalException {
    StarlarkList<StarlarkInt> s = StarlarkList.copyOf(
        this.currentThread.mutability(), this.getSequenceStorage());
    s.addElements(elements);
    this.setSequenceStorage(s);
  }

  @StarlarkMethod(
      name = "append",
      doc = "Adds an item to the end of the list.",
      parameters = {@Param(name = "item", doc = "Item to add at the end.")})
  @SuppressWarnings("unchecked")
  public void append(LarkyByteLike item) throws EvalException {
    this.addElements(item.getSequenceStorage());
  }

  @StarlarkMethod(
      name = "extend",
      doc = "Adds all items to the end of the list.",
      parameters = {@Param(name = "items", doc = "Items to add at the end.")})
  public void extend(LarkyByteLike items) throws EvalException {
    //@SuppressWarnings("unchecked")
    //Iterable<StarlarkInt> src = (Iterable<StarlarkInt>) Starlark.toIterable(items);
    addElements(items.getSequenceStorage());
  }

  @StarlarkMethod(
      name = "insert",
      doc = "Inserts an item at a given position.",
      parameters = {
          @Param(name = "index", doc = "The index of the given position."),
          @Param(name = "item", doc = "The item.")
      })
  public void insert(StarlarkInt index, LarkyByteLike item) throws EvalException {
    addElementAt(index.toIntUnchecked(), item);
  }

  @StarlarkMethod(name = "clear", doc = "Removes all the elements of the list.")
  public void clearElements() throws EvalException {
    this.setSequenceStorage(StarlarkList.newList(currentThread.mutability()));
  }

  @StarlarkMethod(
      name = "pop",
      doc =
          "Removes the item at the given position in the list, and returns it. "
              + "If no <code>index</code> is specified, "
              + "it removes and returns the last item in the list.",
      parameters = {
          @Param(
              name = "i",
              allowedTypes = {
                  @ParamType(type = StarlarkInt.class),
                  @ParamType(type = NoneType.class),
              },
              defaultValue = "-1",
              doc = "The index of the item.")
      })
  public StarlarkInt pop(Object i) throws EvalException {
    int arg = i == Starlark.NONE ? -1 : Starlark.toInt(i, "i");
    int index = getSequenceIndex(arg, size());
    StarlarkInt result = this.get(index);
    StarlarkList<StarlarkInt> si = StarlarkList.copyOf(
        currentThread.mutability(),
        this.getSequenceStorage());
    si.removeElementAt(index);
    return result;
  }

  /**
   * Resolves a positive or negative index to an index in the range [0, length), or throws
   * EvalException if it is out of range. If the index is negative, it counts backward from length.
   */
  static int getSequenceIndex(int index, int length) throws EvalException {
    int actualIndex = index;
    if (actualIndex < 0) {
      actualIndex += length;
    }
    if (actualIndex < 0 || actualIndex >= length) {
      throw Starlark.errorf(
          "index out of range (index is %d, but sequence has %d elements)", index, length);
    }
    return actualIndex;
  }
}
