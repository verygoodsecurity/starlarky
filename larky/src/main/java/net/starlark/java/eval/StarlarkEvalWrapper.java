package net.starlark.java.eval;

public class StarlarkEvalWrapper {

  private StarlarkEvalWrapper() {}// uninstantiable

  /**
   * Publicly exposes the {@link StarlarkList#wrap} method to allow list creation via array
   * ownership transfer to allow "zero-copy" StarlarkList creation. Takes ownership of the
   * supplied array.
   *
   * Same limitations of the package-private {@link StarlarkList#wrap} method apply. The caller
   * <b>MUST NOT</b> subsequently modify the array.n
   *
   * @param mu The {@link Mutability} of the list.
   * @param arr The array to take ownership
   * @param <T> The type of elements in the array
   * @return A {@link StarlarkList} which takes ownership of the supplied {@code arr}
   */
  public static <T> StarlarkList<?> zeroCopyList(Mutability mu, T[] arr) {
    return StarlarkList.wrap(mu, arr);
  }

  /**
   * Defines the strict weak ordering of Starlark values used for sorting and the comparison operators. Throws
   * ClassCastException on failure.
   */
  public static int compareUnchecked(Object x, Object y) {
    return Starlark.compareUnchecked(x, y);
  }
}
