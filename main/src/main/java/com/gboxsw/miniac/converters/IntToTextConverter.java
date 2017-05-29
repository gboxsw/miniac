package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * Integer to string converter.
 */
public class IntToTextConverter implements Converter<Integer, byte[]> {

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
	public IntToTextConverter(boolean exceptionOnFail) {
		this.exceptionOnFail = exceptionOnFail;
	}

	/**
	 * Constructs a silent converter.
	 */
	public IntToTextConverter() {
		this(false);
	}

	@Override
	public byte[] convertSourceToTarget(Integer value) {
		if (value == null) {
			return null;
		} else {
			return value.toString().getBytes();
		}
	}

	@Override
	public Integer convertTargetToSource(byte[] value) {
		if ((value == null) || (value.length == 0)) {
			return null;
		}

		try {
			return Integer.parseInt(new String(value).trim());
		} catch (Exception e) {
			if (exceptionOnFail) {
				throw e;
			}
		}

		return null;
	}

}
