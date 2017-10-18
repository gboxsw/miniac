package com.gboxsw.miniac.utils;

/**
 * Implementation of a monotonic clock.
 */
public final class MonotonicClock {

	/**
	 * Start-time of monotonic clock.
	 */
	private static final long nanoTime0 = System.nanoTime();

	/**
	 * Private constructor to prevent instances.
	 */
	private MonotonicClock() {

	}

	/**
	 * Returns current time in milliseconds.
	 * 
	 * @return the difference, measured in milliseconds, between the current
	 *         time and time when the clock was created.
	 */
	public static long currentTimeMillis() {
		return (System.nanoTime() - nanoTime0) / 1_000_000l;
	}
}
