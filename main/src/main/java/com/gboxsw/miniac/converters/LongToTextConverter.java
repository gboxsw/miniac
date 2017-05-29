package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * Long to string converter.
 */
public class LongToTextConverter implements Converter<Long, byte[]> {

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
	public LongToTextConverter(boolean exceptionOnFail) {
		this.exceptionOnFail = exceptionOnFail;
	}

	/**
	 * Constructs a silent converter.
	 */
	public LongToTextConverter() {
		this(false);
	}

	@Override
	public byte[] convertSourceToTarget(Long value) {
		if (value == null) {
			return null;
		} else {
			return value.toString().getBytes();
		}
	}

	@Override
	public Long convertTargetToSource(byte[] value) {
		if ((value == null) || (value.length == 0)) {
			return null;
		}

		try {
			return Long.parseLong(new String(value).trim());
		} catch (Exception e) {
			if (exceptionOnFail) {
				throw e;
			}
		}

		return null;
	}

}
