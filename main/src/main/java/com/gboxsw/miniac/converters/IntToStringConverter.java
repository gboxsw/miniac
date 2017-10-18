package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * Integer to string converter.
 */
public class IntToStringConverter implements Converter<Integer, String> {

	/**
	 * Indicates whether an exception is thrown when conversion failed.
	 */
	private final boolean exceptionOnFail;

	/**
	 * Constructs the converter.
	 * 
	 * @param exceptionOnFail
	 *            true, if an exception is thrown when conversion failed, false,
	 *            otherwise.
	 */
	public IntToStringConverter(boolean exceptionOnFail) {
		this.exceptionOnFail = exceptionOnFail;
	}

	/**
	 * Constructs a silent converter.
	 */
	public IntToStringConverter() {
		this(false);
	}

	@Override
	public String convertSourceToTarget(Integer value) {
		if (value == null) {
			return null;
		} else {
			return value.toString();
		}
	}

	@Override
	public Integer convertTargetToSource(String value) {
		if ((value == null) || value.isEmpty()) {
			return null;
		}

		try {
			return Integer.parseInt(value.trim());
		} catch (Exception e) {
			if (exceptionOnFail) {
				throw e;
			}
		}

		return null;
	}
}
