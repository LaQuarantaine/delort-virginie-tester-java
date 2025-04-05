package com.parkit.parkingsystem;

import org.junit.jupiter.api.Test;

import com.parkit.parkingsystem.util.Duration;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

	public class DurationTest {

	    @Test
	    void testGetDurationMillis_nominal() {
	        Date start = new Date(0); // 00:00:00.000
	        Date end = new Date(3_600_000); // 01:00:00.000
	        long expected = 3_600_000L;
	        long result = Duration.getDurationMillis(start, end);
	        assertEquals(expected, result);
	    }

	    @Test
	    void testGetDurationMillis_negativeDuration() {
	        Date start = new Date(3_600_000); // 01:00:00.000
	        Date end = new Date(0); // 00:00:00.000
	        long expected = -3_600_000L;
	        long result = Duration.getDurationMillis(start, end);
	        assertEquals(expected, result);
	    }

	    @Test
	    void testGetDurationMillis_nullStart() {
	        Date end = new Date();
	        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
	            Duration.getDurationMillis(null, end);
	        });
	        assertEquals("Les dates ne peuvent pas être nulles", exception.getMessage());
	    }

	    @Test
	    void testGetDurationMillis_nullEnd() {
	        Date start = new Date();
	        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
	            Duration.getDurationMillis(start, null);
	        });
	        assertEquals("Les dates ne peuvent pas être nulles", exception.getMessage());
	    }

	    @Test
	    void testGetDurationToHoursDecimal_wholeHours() {
	        long millis = 2 * 60 * 60 * 1000; // 2h
	        double expected = 2.0;
	        double result = Duration.getDurationToHoursDecimal(millis);
	        assertEquals(expected, result, 0.0001);
	    }

	    @Test
	    void testGetDurationToHoursDecimal_partialHours() {
	        long millis = 90 * 60 * 1000; // 1h30
	        double expected = 1.5;
	        double result = Duration.getDurationToHoursDecimal(millis);
	        assertEquals(expected, result, 0.0001);
	    }
	}

