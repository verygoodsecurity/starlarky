package com.verygood.security.larky.modules.types.structs;

import static net.starlark.java.syntax.TokenKind.AMPERSAND;
import static net.starlark.java.syntax.TokenKind.AMPERSAND_EQUALS;
import static net.starlark.java.syntax.TokenKind.CARET;
import static net.starlark.java.syntax.TokenKind.CARET_EQUALS;
import static net.starlark.java.syntax.TokenKind.GREATER_GREATER;
import static net.starlark.java.syntax.TokenKind.GREATER_GREATER_EQUALS;
import static net.starlark.java.syntax.TokenKind.LESS_LESS;
import static net.starlark.java.syntax.TokenKind.LESS_LESS_EQUALS;
import static net.starlark.java.syntax.TokenKind.MINUS;
import static net.starlark.java.syntax.TokenKind.MINUS_EQUALS;
import static net.starlark.java.syntax.TokenKind.PERCENT;
import static net.starlark.java.syntax.TokenKind.PERCENT_EQUALS;
import static net.starlark.java.syntax.TokenKind.PIPE;
import static net.starlark.java.syntax.TokenKind.PIPE_EQUALS;
import static net.starlark.java.syntax.TokenKind.PLUS;
import static net.starlark.java.syntax.TokenKind.PLUS_EQUALS;
import static net.starlark.java.syntax.TokenKind.SLASH;
import static net.starlark.java.syntax.TokenKind.SLASH_EQUALS;
import static net.starlark.java.syntax.TokenKind.SLASH_SLASH;
import static net.starlark.java.syntax.TokenKind.SLASH_SLASH_EQUALS;
import static net.starlark.java.syntax.TokenKind.STAR;
import static net.starlark.java.syntax.TokenKind.STAR_EQUALS;
import static net.starlark.java.syntax.TokenKind.STAR_STAR;

import com.verygood.security.larky.modules.types.LarkyIterator;
import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.Nullable;

public class StructBinOp {

  private StructBinOp() {
  } // uninstantiable

  /**
   * The below does not belong in LarkyObject because LarkyObject does not dictate what operations should exist on an
   * object. That is left to the interface implementer.
   *
   * However, for SimpleStruct and its hierarchy tree, in Larky, we can simply "tack-on" the magic method (i.e. __len__
   * or __contains__, etc.) and we expect various operations to work on that object, which is why we want to enable
   * binaryOp on SimpleStruct.
   */
  public static Object __contains__(SimpleStruct lhs, TokenKind op, Object rhs, boolean thisLeft, StarlarkThread thread) throws EvalException {
    try {
      return lhs.__contains__(lhs, op, rhs, thisLeft, thread);
    } catch(EvalException ignored) {
    }
    // it does not. ok, is thisLeft = false & it is an iterator?
    if (!thisLeft) {
      try {
        final LarkyIterator iterator = (LarkyIterator) lhs.iterator();
        return iterator.binaryOp(op, rhs, false);
      } catch (RuntimeException ignored) {
      }
    }
    return null;
  }

  public static boolean richComparison(
    SimpleStruct lhs,
    Object rhs,
    String operator,
    String reflection,
    StarlarkThread thread
  ) throws EvalException {

    final StarlarkCallable lhs_method = (StarlarkCallable) lhs.getField(operator);
    final StarlarkCallable rhs_method = (rhs instanceof SimpleStruct)
                                          ? (StarlarkCallable) ((SimpleStruct) rhs).getField(reflection)
                                          : null;
    //    call_lhs = lhs, lhs_method, rhs
    //    call_rhs = rhs, rhs_method, lhs
    if (StarlarkUtil.richType(rhs).equals(StarlarkUtil.richType(lhs))) {
      //  calls = call_rhs, call_lhs
      //  calls = (rhs, rhs_method, lhs), (lhs, lhs_method, rhs)
      if (rhs_method != null) {
        // if meth == larky.SENTINEL:
        //    continue
        // value = meth(second_obj)
        final Object res = ((SimpleStruct) rhs).invoke(thread, rhs_method, Tuple.of(lhs), Dict.empty());
        return Starlark.truth(res);
      } else if (lhs_method != null) {
        final Object res = lhs.invoke(thread, lhs_method, Tuple.of(rhs), Dict.empty());
        return Starlark.truth(res);
      }
    } else {
      //  calls = call_lhs, call_rhs
      //  calls = (lhs, lhs_method, rhs), (rhs, rhs_method, lhs)
      if (lhs_method != null) {
        final Object res = lhs.invoke(thread, lhs_method, Tuple.of(rhs), Dict.empty());
        return Starlark.truth(res);
      } else if (rhs_method != null) {
        // if meth == larky.SENTINEL:
        //    continue
        // value = meth(second_obj)
        final Object res = ((SimpleStruct) rhs).invoke(thread, rhs_method, Tuple.of(lhs), Dict.empty());
        return Starlark.truth(res);
      }
    }
    throw Starlark.errorf(
      "TypeError: unsupported operand type(s) for %s: %s and %s",
      operator,
      StarlarkUtil.richType(lhs),
      StarlarkUtil.richType(rhs)
    );
  }


  public static Object inplaceBinaryOperation(
    SimpleStruct lhs,
    Object rhs,
    String lhsMethodName,
    String operator,
    StarlarkThread thread
  ) throws EvalException {
    final StarlarkCallable lhs_method = (StarlarkCallable) lhs.getField(lhsMethodName);
    if (lhs_method != null) {
      final Object res = lhs.invoke(thread, lhs_method, Tuple.of(rhs), Dict.empty());
      return res;
    }

    throw Starlark.errorf(
      "TypeError: unsupported operand type(s) for %s: %s and %s",
      operator,
      StarlarkUtil.richType(lhs),
      StarlarkUtil.richType(rhs)
    );
  }


  public static Object binaryOperation(
    SimpleStruct lhs,
    Object rhs,
    String lhsMethodName,
    String rhsMethodName,
    String operator,
    StarlarkThread thread
  ) throws EvalException {

    String lhsType = StarlarkUtil.richType(lhs);
    String rhsType = StarlarkUtil.richType(rhs);

    final StarlarkCallable lhs_method = (StarlarkCallable) lhs.getField(lhsMethodName);
    final StarlarkCallable lhs_rmethod = (StarlarkCallable) lhs.getField(rhsMethodName);
    final StarlarkCallable rhs_method = (rhs instanceof SimpleStruct)
                                          ? (StarlarkCallable) ((SimpleStruct) rhs).getField(rhsMethodName)
                                          : null;
    //  call_lhs = lhs, lhs_method, rhs
    //  call_rhs = rhs, rhs_method, lhs
    if (!rhsType.equals(lhsType) && lhs_rmethod != rhs_method) {
      //  calls = call_rhs, call_lhs
      //  calls = (rhs, rhs_method, lhs), (lhs, lhs_method, rhs)
      if (rhs_method != null) {
        // if meth == larky.SENTINEL:
        //    continue
        // value = meth(second_obj)
        final Object res = ((SimpleStruct) rhs).invoke(thread, rhs_method, Tuple.of(lhs), Dict.empty());
        return res;
      } else if (lhs_method != null) {
        final Object res = lhs.invoke(thread, lhs_method, Tuple.of(rhs), Dict.empty());
        return res;
      }
    } else if (!lhsType.equals(rhsType)) {
      //  calls = call_lhs, call_rhs
      //  calls = (lhs, lhs_method, rhs), (rhs, rhs_method, lhs)
      if (lhs_method != null) {
        final Object res = lhs.invoke(thread, lhs_method, Tuple.of(rhs), Dict.empty());
        return res;
      } else if (rhs_method != null) {
        // if meth == larky.SENTINEL:
        //    continue
        // value = meth(second_obj)
        final Object res = ((SimpleStruct) rhs).invoke(thread, rhs_method, Tuple.of(lhs), Dict.empty());
        return res;
      }
    } else {
      final Object res = lhs.invoke(thread, lhs_method, Tuple.of(rhs), Dict.empty());
      return res;
    }
    throw Starlark.errorf(
      "TypeError: unsupported operand type(s) for %s: %s and %s",
      operator,
      StarlarkUtil.richType(lhs),
      StarlarkUtil.richType(rhs)
    );
  }

  @Nullable
  public static Object operatorDispatch(SimpleStruct lhs, TokenKind op, Object rhs, boolean thisLeft, StarlarkThread thread) throws EvalException {
    switch (op) {
      case GREATER:
        return richComparison(lhs, rhs, PyProtocols.__GT__, PyProtocols.__LT__, thread);
      case LESS:
        return richComparison(lhs, rhs, PyProtocols.__LT__, PyProtocols.__GT__, thread);
      case GREATER_EQUALS:
        return richComparison(lhs, rhs, PyProtocols.__GE__, PyProtocols.__LE__, thread);
      case LESS_EQUALS:
        return richComparison(lhs, rhs, PyProtocols.__LE__, PyProtocols.__GE__, thread);
      case PLUS:
        return binaryOperation(lhs, rhs, PyProtocols.__ADD__, PyProtocols.__RADD__, PLUS.name(), thread);
      case PLUS_EQUALS:
        return inplaceBinaryOperation(lhs, rhs, PyProtocols.__IADD__, PLUS_EQUALS.name(), thread);
      case MINUS:
        return binaryOperation(lhs, rhs, PyProtocols.__SUB__, PyProtocols.__RSUB__, MINUS.name(), thread);
      case MINUS_EQUALS:
        return inplaceBinaryOperation(lhs, rhs, PyProtocols.__ISUB__, MINUS_EQUALS.name(), thread);
      case STAR:
        return binaryOperation(lhs, rhs, PyProtocols.__MUL__, PyProtocols.__RMUL__, STAR.name(), thread);
      case STAR_EQUALS:
        return inplaceBinaryOperation(lhs, rhs, PyProtocols.__IMUL__, STAR_EQUALS.name(), thread);
      case STAR_STAR:
        return binaryOperation(lhs, rhs, PyProtocols.__POW__, PyProtocols.__POW__, STAR_STAR.name(), thread);
      case PERCENT:
        return binaryOperation(lhs, rhs, PyProtocols.__MOD__, PyProtocols.__RMOD__, PERCENT.name(), thread);
      case PERCENT_EQUALS:
        return inplaceBinaryOperation(lhs, rhs, PyProtocols.__IMOD__, PERCENT_EQUALS.name(), thread);
      case SLASH:
        return binaryOperation(lhs, rhs, PyProtocols.__TRUEDIV__, PyProtocols.__TRUEDIV__, SLASH.name(), thread);
      case SLASH_EQUALS:
        return inplaceBinaryOperation(lhs, rhs, PyProtocols.__ITRUEDIV__, SLASH_EQUALS.name(), thread);
      case SLASH_SLASH:
        return binaryOperation(lhs, rhs, PyProtocols.__FLOORDIV__, PyProtocols.__RFLOORDIV__, SLASH_SLASH.name(), thread);
      case SLASH_SLASH_EQUALS:
        return inplaceBinaryOperation(lhs, rhs, PyProtocols.__IFLOORDIV__, SLASH_SLASH_EQUALS.name(), thread);
      case LESS_LESS:
        return binaryOperation(lhs, rhs, PyProtocols.__LSHIFT__, PyProtocols.__LSHIFT__, LESS_LESS.name(), thread);
      case LESS_LESS_EQUALS:
        return inplaceBinaryOperation(lhs, rhs, PyProtocols.__ILSHIFT__, LESS_LESS_EQUALS.name(), thread);
      case GREATER_GREATER:
        return binaryOperation(lhs, rhs, PyProtocols.__RSHIFT__, PyProtocols.__RSHIFT__, GREATER_GREATER.name(), thread);
      case GREATER_GREATER_EQUALS:
        return inplaceBinaryOperation(lhs, rhs, PyProtocols.__IRSHIFT__, GREATER_GREATER_EQUALS.name(), thread);
      case AMPERSAND:
        return binaryOperation(lhs, rhs, PyProtocols.__AND__, PyProtocols.__AND__, AMPERSAND.name(), thread);
      case AMPERSAND_EQUALS:
        return inplaceBinaryOperation(lhs, rhs, PyProtocols.__IAND__, AMPERSAND_EQUALS.name(), thread);
      case CARET:
        return binaryOperation(lhs, rhs, PyProtocols.__XOR__, PyProtocols.__XOR__, CARET.name(), thread);
      case CARET_EQUALS:
        return inplaceBinaryOperation(lhs, rhs, PyProtocols.__IXOR__, CARET_EQUALS.name(), thread);
      case PIPE:
        return binaryOperation(lhs, rhs, PyProtocols.__OR__, PyProtocols.__OR__, PIPE.name(), thread);
      case PIPE_EQUALS:
        return inplaceBinaryOperation(lhs, rhs, PyProtocols.__IOR__, PIPE_EQUALS.name(), thread);
      case IN:
        Object contains = __contains__(lhs, op, rhs, thisLeft, thread);
        if (contains != null) {
          return contains;
        }
        // *not in* case will be handled by EvalUtils
        // fallthrough
      case EQUALS_EQUALS:
        /* this is handled by {@link EvalUtils#binaryOp} in libstarlark */
      default:
        // unsupported binary operation!
        return null;
    }
  }
}
