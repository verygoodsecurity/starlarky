package com.verygood.security.larky.objects.type;

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

import com.verygood.security.larky.modules.types.LarkyCollection;
import com.verygood.security.larky.modules.types.LarkyIndexable;
import com.verygood.security.larky.modules.types.LarkyIterator;
import com.verygood.security.larky.modules.types.LarkyObject;
import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.Nullable;

public class BinaryOpHelper {

  private BinaryOpHelper() {
  } // uninstantiable

  private static boolean contains(LarkyObject lhs, TokenKind op, Object rhs, boolean thisLeft, StarlarkThread thread) throws EvalException {
    if (lhs == null) {
      return false;
    }

    if(lhs instanceof LarkyCollection) {
      return ((LarkyCollection)lhs).__contains__((LarkyCollection) lhs, op, rhs, thisLeft, thread);
    } else if(lhs instanceof LarkyIndexable) {
      return ((LarkyIndexable)lhs).__contains__((LarkyIndexable) lhs, op, rhs, thisLeft, thread);
    } else if(!thisLeft && (lhs instanceof StarlarkIterable)) {
      // it does not. ok, is thisLeft = false & it is an iterator?
      @SuppressWarnings("rawtypes")
      final LarkyIterator iterator = (LarkyIterator) ((StarlarkIterable) lhs).iterator();
      Object res = iterator.binaryOp(op, rhs, false);
      return (res instanceof Boolean && (boolean) res);
    }
    return false;
  }

  public static boolean richComparison(
    LarkyObject lhs,
    Object rhs,
    String operator,
    String reflection,
    StarlarkThread thread
  ) throws EvalException {

    final StarlarkCallable lhs_method = (StarlarkCallable) lhs.getField(operator);
    final StarlarkCallable rhs_method = (rhs instanceof LarkyObject)
                                          ? (StarlarkCallable) ((LarkyObject) rhs).getField(reflection)
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
        final Object res = ((LarkyObject) rhs).invoke(thread, rhs_method, Tuple.of(lhs), Dict.empty());
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
        final Object res = ((LarkyObject) rhs).invoke(thread, rhs_method, Tuple.of(lhs), Dict.empty());
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
    LarkyObject lhs,
    Object rhs,
    String lhsMethodName,
    String operator,
    StarlarkThread thread
  ) throws EvalException {
    final StarlarkCallable lhs_method = (StarlarkCallable) lhs.getField(lhsMethodName);
    if (lhs_method != null) {
      return lhs.invoke(thread, lhs_method, Tuple.of(rhs), Dict.empty());
    }

    throw Starlark.errorf(
      "TypeError: unsupported operand type(s) for %s: %s and %s",
      operator,
      StarlarkUtil.richType(lhs),
      StarlarkUtil.richType(rhs)
    );
  }

  public static Object binaryOperation(
    LarkyObject lhs,
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
    final StarlarkCallable rhs_method = (rhs instanceof LarkyObject)
                                          ? (StarlarkCallable) ((LarkyObject) rhs).getField(rhsMethodName)
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
        return ((LarkyObject) rhs).invoke(thread, rhs_method, Tuple.of(lhs), Dict.empty());
      } else if (lhs_method != null) {
        return lhs.invoke(thread, lhs_method, Tuple.of(rhs), Dict.empty());
      }
    } else if (!lhsType.equals(rhsType)) {
      //  calls = call_lhs, call_rhs
      //  calls = (lhs, lhs_method, rhs), (rhs, rhs_method, lhs)
      if (lhs_method != null) {
        return lhs.invoke(thread, lhs_method, Tuple.of(rhs), Dict.empty());
      } else if (rhs_method != null) {
        // if meth == larky.SENTINEL:
        //    continue
        // value = meth(second_obj)
        return ((LarkyObject) rhs).invoke(thread, rhs_method, Tuple.of(lhs), Dict.empty());
      }
    } else {
      return lhs.invoke(thread, lhs_method, Tuple.of(rhs), Dict.empty());
    }
    throw Starlark.errorf(
      "TypeError: unsupported operand type(s) for %s: %s and %s",
      operator,
      StarlarkUtil.richType(lhs),
      StarlarkUtil.richType(rhs)
    );
  }

  @Nullable
  public static Object operatorDispatch(LarkyObject lhs, TokenKind op, Object rhs, boolean thisLeft, StarlarkThread thread) throws EvalException {
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
        if (contains(lhs, op, rhs, thisLeft, thread)) {
          return true;
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
