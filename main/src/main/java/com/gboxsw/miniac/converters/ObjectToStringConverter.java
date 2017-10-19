package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * Unidirectional converter that converts object to its string representation
 * using {@link Object#toString()}.
 */
public class ObjectToStringConverter implements Converter<Object, String> {

	@Override
	public String convertSourceToTarget(Object value) {
		if (value == null) {
			return null;
		}

		return value.toString();
	}

	@Override
	public Object convertTargetToSource(String value) {
		throw new UnsupportedOperationException("Conversion is not supported.");
	}

}
