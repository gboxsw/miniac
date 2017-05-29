package com.gboxsw.miniac;

import java.io.Serializable;
import java.util.*;

/**
 * A mapping from String keys to various values. The class is not thread-safe.
 */
public class Bundle {

	/**
	 * Internal map storing the values.
	 */
	private final Map<String, Object> values = new HashMap<>();

	/**
	 * Constructs the bundle.
	 */
	public Bundle() {

	}

	/**
	 * Returns whether the bundle is empty.
	 * 
	 * @return true, if the bundle is empty, false otherwise.
	 */
	public boolean isEmpty() {
		return values.isEmpty();
	}

	/**
	 * Returns the set of keys to which a value is associated in the bundle.
	 * 
	 * @return the set of keys.
	 */
	public Set<String> getKeys() {
		return new HashSet<>(values.keySet());
	}

	/**
	 * Returns whether the bundle contains a mapping for given key.
	 * 
	 * @param key
	 *            the key.
	 * @return true, if the bundle contains the mapping for the key, false
	 *         otherwise.
	 */
	public boolean containsKey(String key) {
		return values.containsKey(key);
	}

	public void put(String key, String value) {
		if (key != null) {
			values.put(key, value);
		}
	}

	public void put(String key, boolean value) {
		if (key != null) {
			values.put(key, value);
		}
	}

	public void put(String key, int value) {
		if (key != null) {
			values.put(key, value);
		}
	}

	public void put(String key, long value) {
		if (key != null) {
			values.put(key, value);
		}
	}

	public void put(String key, double value) {
		if (key != null) {
			values.put(key, value);
		}
	}

	public void put(String key, Serializable object) {
		if (key != null) {
			values.put(key, object);
		}
	}

	public String getAsString(String key, String defaultValue) {
		try {
			if (!values.containsKey(key)) {
				return defaultValue;
			} else {
				return (String) values.get(key);
			}
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public boolean getAsBoolean(String key, boolean defaultValue) {
		try {
			if (!values.containsKey(key)) {
				return defaultValue;
			} else {
				return (Boolean) values.get(key);
			}
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public int getAsInt(String key, int defaultValue) {
		try {
			if (!values.containsKey(key)) {
				return defaultValue;
			} else {
				return (Integer) values.get(key);
			}
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public long getAsLong(String key, long defaultValue) {
		try {
			if (!values.containsKey(key)) {
				return defaultValue;
			} else {
				return (Long) values.get(key);
			}
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public double getAsDouble(String key, double defaultValue) {
		try {
			if (!values.containsKey(key)) {
				return defaultValue;
			} else {
				return (Double) values.get(key);
			}
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public Object get(String key) {
		return values.get(key);
	}
}
