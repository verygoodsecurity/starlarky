package com.verygood.security.larky.objects.mro;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.verygood.security.larky.objects.type.LarkyType;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;

import org.jetbrains.annotations.NotNull;

public class C3 {

  private C3() {
  }

  /**
   * Port of Python's MRO (method resolution order) algorithm (which is actually inspired/taken from the Dylan
   * programming language).
   *
   * More information can be found <a href="https://blog.pilosus.org/posts/2019/05/02/python-mro/">here</a>.
   *
   * Python's introduction (and python code) of this algorithm can be found in <a
   * href="https://www.python.org/download/releases/2.3/mro/">Python v2.3</a> release notes.
   *
   * @param c {@link LarkyType} to calculate the method resolution algorithm for
   * @return a linearized list of classes that make up the inheritance chain for method resolution
   */
  public static List<LarkyType> calculateMRO(LarkyType c) throws EvalException {
    final Sequence<LarkyType> superclasses = Sequence.cast(c.getBases(), LarkyType.class, "could not cast");
    List<List<LarkyType>> toMerge = Lists.newArrayList();

    for (LarkyType superclass : superclasses) {
      // casting Starlark Tuple (which is Object) to List<LarkyType>)
      final List<LarkyType> objects = superclass.getMRO().stream().filter(o -> LarkyType.class.isAssignableFrom(o.getClass())).map(LarkyType.class::cast).collect(Collectors.toList());
      toMerge.add(objects);
    }
    toMerge.add(new ArrayList<>(superclasses));
    List<LarkyType> order = Lists.newArrayList(c);
    return mroMerge(order, toMerge);
  }

  @NotNull
  private static List<LarkyType> mroMerge(List<LarkyType> order, List<List<LarkyType>> toMerge) throws EvalException {

    // C3 linearization algorithm
    while (!toMerge.stream().allMatch(List::isEmpty)) {
      LarkyType goodCandidate = null;
      outer:
      for (List<LarkyType> sublist : toMerge) {
        if (sublist.isEmpty()) continue;
        LarkyType candidate = sublist.get(0);
        // is the candidate present in any of the other lists, as any element after 0?
        for (List<LarkyType> other : toMerge) {
          if (other.indexOf(candidate) > 0) {
            continue outer; // reject the candidate
          }
        }
        goodCandidate = candidate;
        break;
      }
      if (goodCandidate == null) {
        throw Starlark.errorf(
          "Inconsistent hierarchy - unable to compute a consistent " +
            "method resolution order for %s", order.get(0));
      }
      order.add(goodCandidate);
      for (List<LarkyType> sublist : toMerge) {
        sublist.remove(goodCandidate);
      }
    }
    return order;
  }
}
