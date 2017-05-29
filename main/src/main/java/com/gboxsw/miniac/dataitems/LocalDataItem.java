package com.gboxsw.miniac.dataitems;

import com.gboxsw.miniac.*;
import java.util.logging.*;
import java.io.Serializable;

public final class LocalDataItem<T> extends DataItem<T> {

	/**
	 * Key used to persistently store the value.
	 */
	private static final String SAVED_VALUE_KEY = "value";

	/**
	 * Logger.
	 */
	private static final Logger logger = Logger.getLogger(LocalDataItem.class.getName());

	/**
	 * Indicates whether the value of the data item is persistent between two
	 * launches of the application.
	 */
	private final boolean persistent;

	/**
	 * The last requested (desired) value.
	 */
	private T desiredValue;

	/**
	 * Constructs the data item.
	 * 
	 * @param persistent
	 *            true, if the data item is persistent, false otherwise.
	 * @param type
	 *            the type of values stored in the data item.
	 */
	public LocalDataItem(boolean persistent, Class<T> type) {
		super(type, false);
		this.persistent = persistent;

		if (persistent && !Serializable.class.isAssignableFrom(type)) {
			throw new IllegalArgumentException(
					"The type " + type.toString() + " does not implement the interface Serializable.");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onActivate(Bundle savedState) {
		if (persistent && (getApplication().getPersistentStorage() == null)) {
			throw new IllegalStateException("Persistent storage required for correct functionality of data item \""
					+ getId() + "\" is not defined in the application.");
		}

		if (!persistent || !savedState.containsKey(SAVED_VALUE_KEY)) {
			return;
		}

		Object savedValue = savedState.get(SAVED_VALUE_KEY);
		if (savedValue == null) {
			return;
		}

		if (getType().isInstance(savedValue)) {
			desiredValue = (T) savedState.get(SAVED_VALUE_KEY);
			update();
		} else {
			logger.log(Level.WARNING, "Incompatible value of data item \"" + getId()
					+ "\" found in the persistent storage (loading skipped).");
		}
	}

	@Override
	protected T onSynchronizeValue() {
		return desiredValue;
	}

	@Override
	protected void onValueChangeRequested(T newValue) {
		desiredValue = newValue;
		update();
	}

	@Override
	protected void onSaveState(Bundle outState) {
		if (persistent) {
			outState.put(SAVED_VALUE_KEY, (Serializable) getValue());
		}
	}

	@Override
	protected void onDeactivate() {
		// nothing to do
	}

	/**
	 * Returns whether value of the data item is persistent between two launches
	 * of application.
	 * 
	 * @return true, if the value is persistent, false otherwise.
	 */
	public boolean isPersistent() {
		return persistent;
	}
}
