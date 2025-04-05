package com.parkit.parkingsystem.util;

import java.util.Date;


public class Duration {

	public static long getDurationMillis(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Les dates ne peuvent pas être nulles");
        }
        return endTime.getTime() - startTime.getTime();
    }

	public static double getDurationToHoursDecimal(long durationMillis) {
        return durationMillis / (1000.0 * 60 * 60);
    }
}
