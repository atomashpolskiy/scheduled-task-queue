package org.example;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class ChronoUtil {

    // copied with minor changes from JDK9
    // https://hg.openjdk.java.net/jdk9/jdk9/jdk/file/65464a307408/src/java.base/share/classes/java/util/concurrent/TimeUnit.java
    public static ChronoUnit toChronoUnit(TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:  return ChronoUnit.NANOS;
            case MICROSECONDS: return ChronoUnit.MICROS;
            case MILLISECONDS: return ChronoUnit.MILLIS;
            case SECONDS:      return ChronoUnit.SECONDS;
            case MINUTES:      return ChronoUnit.MINUTES;
            case HOURS:        return ChronoUnit.HOURS;
            case DAYS:         return ChronoUnit.DAYS;
            default: throw new AssertionError();
        }
    }
}
