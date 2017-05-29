package com.gboxsw.miniac.dataitems;

import java.util.logging.*;
import com.gboxsw.miniac.*;

/**
 * Data item that accumulates increases of positive integer values produced by
 * the source data item.
 */
public final class AccumulatingDataItem extends DataItem<Long> {

	/**
	 * Key used to persistently store the value.
	 */
	private static final String SAVED_VALUE_KEY = "value";

	/**
	 * Key used to persistently store the source value.
	 */
	private static final String SAVED_SOURCE_KEY = "source";

	/**
	 * Logger.
	 */
	private static final Logger logger = Logger.getLogger(LocalDataItem.class.getName());

	/**
	 * Source data item.
	 */
	private final DataItem<? extends Number> source;

	/**
	 * Last known defined source value corresponding to the last known
	 * accumulated value.
	 */
	private long sourceValue;

	/**
	 * Last known accumulated value.
	 */
	private Long accumulatedValue;

	/**
	 * Constructs a data item that accumulates value nonnegative values of
	 * source data item.
	 * 
	 * @param source
	 *            the source data item.
	 * @param type
	 *            the value type of data item.
	 */
	public AccumulatingDataItem(DataItem<? extends Number> source) {
		super(Long.class, true);
		this.source = source;
	}

	@Override
	protected void onActivate(Bundle savedState) {
		if (getApplication().getPersistentStorage() == null) {
			throw new IllegalStateException("Persistent storage required for correct functionality of data item \""
					+ getId() + "\" is not defined in the application.");
		}

		setDependencies(source);

		if (savedState.containsKey(SAVED_SOURCE_KEY) && savedState.containsKey(SAVED_VALUE_KEY)) {
			try {
				Long savedSource = (Long) savedState.get(SAVED_SOURCE_KEY);
				Long savedValue = (Long) savedState.get(SAVED_VALUE_KEY);

				if ((savedSource != null) && (savedValue != null) && (savedSource >= 0) && (savedValue >= 0)) {
					sourceValue = savedSource;
					accumulatedValue = savedValue;

					update();
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "Incompatible state of data item \"" + getId()
						+ "\" found in the persistent storage (loading skipped).");
			}
		}
	}

	@Override
	protected Long onSynchronizeValue() {
		updateAccumulatedValue();
		return accumulatedValue;
	}

	@Override
	protected void onValueChangeRequested(Long newValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void onSaveState(Bundle outState) {
		if (accumulatedValue != null) {
			outState.put(SAVED_SOURCE_KEY, sourceValue);
			outState.put(SAVED_VALUE_KEY, accumulatedValue);
		}
	}

	@Override
	protected void onDeactivate() {
		// nothing to do
	}

	/**
	 * Returns the value as long.
	 * 
	 * @return the long value.
	 */
	public long getValueAsLong() {
		return getValue();
	}

	/**
	 * Returns the value as int.
	 * 
	 * @return the int value.
	 */
	public int getValueAsInt() {
		return getValue().intValue();
	}

	/**
	 * Returns the value as short.
	 * 
	 * @return the short value.
	 */
	public short getValueAsShort() {
		return getValue().shortValue();
	}

	/**
	 * Returns the value as byte.
	 * 
	 * @return the byte value.
	 */
	public byte getValueAsByte() {
		return getValue().byteValue();
	}

	/**
	 * Resets the accumulation.
	 */
	public void reset() {
		accumulatedValue = null;
		sourceValue = 0;
		updateAccumulatedValue();
		update();
	}

	/**
	 * Updates accumulated value.
	 */
	private void updateAccumulatedValue() {
		Number value = source.getValue();
		if (value == null) {
			return;
		}

		long longValue = value.longValue();
		if (longValue < 0) {
			return;
		}

		if (accumulatedValue == null) {
			sourceValue = longValue;
			accumulatedValue = longValue;
			return;
		}

		if (longValue < sourceValue) {
			accumulatedValue = accumulatedValue + longValue;
		} else {
			long increase = longValue - sourceValue;
			if (increase > 0) {
				accumulatedValue = accumulatedValue + increase;
			}
		}

		sourceValue = longValue;
	}
}
