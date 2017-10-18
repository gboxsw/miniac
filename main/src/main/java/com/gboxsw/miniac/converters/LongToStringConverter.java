package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * Long to string converter.
 */
public class LongToStringConverter implements Converter<Long, String> {

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
	public LongToStringConverter(boolean exceptionOnFail) {
		this.exceptionOnFail = exceptionOnFail;
	}

	/**
	 * Constructs a silent converter.
	 */
	public LongToStringConverter() {
		this(false);
	}

	@Override
	public String convertSourceToTarget(Long value) {
		if (value == null) {
			return null;
		} else {
			return value.toString();
		}
	}

	@Override
	public Long convertTargetToSource(String value) {
		if ((value == null) || value.isEmpty()) {
			return null;
		}

		try {
			return Long.parseLong(value.trim());
		} catch (Exception e) {
			if (exceptionOnFail) {
				throw e;
			}
		}

		return null;
	}

}
