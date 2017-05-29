package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * Double to string converter.
 */
public class DoubleToTextConverter implements Converter<Double, byte[]> {

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
	public DoubleToTextConverter(boolean exceptionOnFail) {
		this.exceptionOnFail = exceptionOnFail;
	}

	/**
	 * Constructs a silent converter.
	 */
	public DoubleToTextConverter() {
		this(false);
	}

	@Override
	public byte[] convertSourceToTarget(Double value) {
		if (value == null) {
			return null;
		} else {
			return value.toString().getBytes();
		}
	}

	@Override
	public Double convertTargetToSource(byte[] value) {
		if ((value == null) || (value.length == 0)) {
			return null;
		}

		try {
			return Double.parseDouble(new String(value).trim());
		} catch (Exception e) {
			if (exceptionOnFail) {
				throw e;
			}
		}

		return null;
	}

}
