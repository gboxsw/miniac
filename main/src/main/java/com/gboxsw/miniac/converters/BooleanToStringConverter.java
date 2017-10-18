package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * Boolean to string converter.
 */
public class BooleanToStringConverter implements Converter<Boolean, String> {

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
	 *            if the value is converted to null.
	 */
	public BooleanToStringConverter(String trueLiteral, String falseLiteral, boolean exceptionOnFail) {
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
	public BooleanToStringConverter() {
		this("true", "false", false);
	}

	@Override
	public String convertSourceToTarget(Boolean value) {
		if (value == null) {
			return null;
		} else {
			if (value) {
				return trueLiteral;
			} else {
				return falseLiteral;
			}
		}
	}

	@Override
	public Boolean convertTargetToSource(String value) {
		if (value == null) {
			return null;
		}

		value = value.trim();
		if (trueLiteral.equalsIgnoreCase(value)) {
			return true;
		} else if (falseLiteral.equalsIgnoreCase(value)) {
			return false;
		} else {
			if (exceptionOnFail) {
				throw new RuntimeException("Conversion of \"" + value + "\" to a boolean value failed.");
			}

			return null;
		}
	}
}
