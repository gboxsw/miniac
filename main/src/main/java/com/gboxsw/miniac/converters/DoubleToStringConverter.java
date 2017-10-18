package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * Double to string converter.
 */
public class DoubleToStringConverter implements Converter<Double, String> {

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
	public DoubleToStringConverter(boolean exceptionOnFail) {
		this.exceptionOnFail = exceptionOnFail;
	}

	/**
	 * Constructs a silent converter.
	 */
	public DoubleToStringConverter() {
		this(false);
	}

	@Override
	public String convertSourceToTarget(Double value) {
		if (value == null) {
			return null;
		} else {
			return value.toString();
		}
	}

	@Override
	public Double convertTargetToSource(String value) {
		if ((value == null) || value.isEmpty()) {
			return null;
		}

		try {
			return Double.parseDouble(value.trim());
		} catch (Exception e) {
			if (exceptionOnFail) {
				throw e;
			}
		}

		return null;
	}

}
