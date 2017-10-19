package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * Bidirectional converter of enum values to their string representations.
 * 
 * @param <E>
 *            the enum type.
 */
public class EnumToStringConverter<E extends Enum<E>> implements Converter<E, String> {

	/**
	 * Class of the enum type.
	 */
	private Class<E> enumType;

	/**
	 * Constructs the enum to string converter.
	 * 
	 * @param enumType
	 *            the class of enum type.
	 */
	public EnumToStringConverter(Class<E> enumType) {
		if (enumType == null) {
			throw new NullPointerException("Enum type cannot be null.");
		}
		this.enumType = enumType;
	}

	@Override
	public String convertSourceToTarget(E value) {
		if (value == null) {
			return null;
		}

		return value.name();
	}

	@Override
	public E convertTargetToSource(String value) {
		if (value == null) {
			return null;
		}

		try {
			return Enum.valueOf(enumType, value);
		} catch (Exception e) {
			return null;
		}
	}

}
