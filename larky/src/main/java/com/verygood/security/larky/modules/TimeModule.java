package com.verygood.security.larky.modules;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


@StarlarkBuiltin(
    name = "jtime",
    category = "BUILTIN",
    doc = "Return the time in seconds since the epoch as a floating point number. ")
public class TimeModule implements StarlarkValue {

  public static final TimeModule INSTANCE = new TimeModule();

  @StarlarkMethod(name = "time", doc = "")
  public StarlarkFloat time() {
    return StarlarkFloat.of(System.currentTimeMillis()/1000.0);
  }

  @StarlarkMethod(
          name = "gmtime",
          doc = "Convert a time expressed in seconds since the epoch to a struct_time in UTC." +
                "If secs is not provided or None, the current time as returned by time() is used.",
          parameters = {
                  @Param(name = "timestamp",
                          defaultValue = "None",
                          allowedTypes = {
                                  @ParamType(type = NoneType.class),
                                  @ParamType(type = StarlarkInt.class),
                          }),
  })
  public Dict<String, StarlarkInt> gmtime(Object timestamp) throws EvalException {
      DateTime dateTime;

   if (timestamp instanceof NoneType) {
      dateTime = new DateTime(DateTimeZone.UTC);
   } else {
       StarlarkInt ts = (StarlarkInt) timestamp;
      long millisSinceEpoch = ts.toLong("not long?")*1000;
      dateTime = new DateTime(millisSinceEpoch, DateTimeZone.UTC);
   }
      Dict<String, StarlarkInt> dict = Dict.of(Mutability.create());
      dict.putEntry("tm_year", StarlarkInt.of(dateTime.getYear()));
      dict.putEntry("tm_mon", StarlarkInt.of(dateTime.getMonthOfYear()));
      dict.putEntry("tm_mday", StarlarkInt.of(dateTime.getDayOfMonth()));
      dict.putEntry("tm_hour", StarlarkInt.of(dateTime.getHourOfDay()));
      dict.putEntry("tm_min", StarlarkInt.of(dateTime.getMinuteOfHour()));
      dict.putEntry("tm_sec", StarlarkInt.of(dateTime.getSecondOfMinute()));
      // Monday is 0 in python time but 1 in java joda time
      dict.putEntry("tm_wday", StarlarkInt.of(dateTime.getDayOfWeek()-1));
      dict.putEntry("tm_yday", StarlarkInt.of(dateTime.getDayOfYear()));
    return dict;
  }

  @StarlarkMethod(
          name = "strftime",
          doc = "Convert utc datetime to a string as specified by the format argument.",
          parameters = {
                  @Param(name = "format",
                          allowedTypes = {@ParamType(type = String.class),}),
  })
  public String strftime(String format) {
    Map<String, String> map = Stream.of(new String[][] {
            { "%a", "EEE" },
            { "%A", "EEEE" },
            { "%b", "MMM" },
            { "%B", "MMMM" },
            { "%c", "EEE MMM  d HH:mm:ss yyyy" },
            { "%d", "dd" },
            { "%H", "HH" },
            { "%I", "hh" },
            { "%j", "D" },
            { "%m", "MM" },
            { "%M", "mm" },
            { "%p", "a" },
            { "%S", "ss" },
            { "%y", "yy" },
            { "%Y", "yyyy" },
            { "%Z", "Z" }
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    Pattern r = Pattern.compile("%[aAbBcdHIjmMpSyYZ]{1}");
    Matcher m = r.matcher(format);
    while (m.find( )) {
      format = format.replace(m.group(0), map.get(m.group(0)));
    }
    DateTime currentTime = new DateTime(DateTimeZone.UTC);
    return currentTime.toString(format);
  }
}