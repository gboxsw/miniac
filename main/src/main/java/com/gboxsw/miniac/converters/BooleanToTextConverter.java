package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * Boolean to string converter.
 */
public class BooleanToTextConverter implements Converter<Boolean, byte[]> {

	/**
	 * Indicates whether an exception is thrown when conversion failed.
	 */
	private final boolean exceptionOnFail;

	/**
	 * Text representation of true value.
	 */
	private final String trueLiteral;

	/**
	 * Text representation of false value.
	 */
	private final String falseLiteral;

	/**
	 * Constructs the converter.
	 * 
	 * @param trueLiteral
	 *            the text that is evaluated to true.
	 * @param falseLiteral
	 *            the text that is evaluated to false.
	 * @param exceptionOnFail
	 *            true, if an exception is thrown when conversion failed, false,
	 *            otherwise.
	 */
	public BooleanToTextConverter(String trueLiteral, String falseLiteral, boolean exceptionOnFail) {
		if (trueLiteral == null) {
			throw new NullPointerException("True cannot be represented as null.");
		}

		if (falseLiteral == null) {
			throw new NullPointerException("False cannot be represented as null.");
		}

		this.trueLiteral = trueLiteral.trim();
		this.falseLiteral = falseLiteral.trim();
		if (this.trueLiteral.equalsIgnoreCase(this.falseLiteral)) {
			throw new IllegalArgumentException("True and false cannot have the same representation.");
		}

		this.exceptionOnFail = exceptionOnFail;
	}

	/**
	 * Constructs a silent converter.
	 */
	public BooleanToTextConverter() {
		this("true", "false", false);
	}

	@Override
	public byte[] convertSourceToTarget(Boolean value) {
		if (value == null) {
			return null;
		} else {
			return value.toString().getBytes();
		}
	}

	@Override
	public Boolean convertTargetToSource(byte[] value) {
		if (value == null) {
			return null;
		}

		String text = new String(value).trim();
		if (trueLiteral.equalsIgnoreCase(text)) {
			return true;
		} else if (falseLiteral.equalsIgnoreCase(text)) {
			return false;
		} else {
			if (exceptionOnFail) {
				throw new RuntimeException("Conversion of \"" + text + "\" to a boolean value failed.");
			}

			return null;
		}
	}
}
