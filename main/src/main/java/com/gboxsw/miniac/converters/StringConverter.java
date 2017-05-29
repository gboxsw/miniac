package com.gboxsw.miniac.converters;

import java.nio.charset.Charset;

import com.gboxsw.miniac.Converter;

/**
 * String converter.
 */
public class StringConverter implements Converter<String, byte[]> {

	/**
	 * UTF-8 charset.
	 */
	private static final Charset UTF8_CHARSET = Charset.forName("utf-8");

	@Override
	public byte[] convertSourceToTarget(String value) {
		if (value == null) {
			return null;
		} else {
			return value.getBytes(UTF8_CHARSET);
		}
	}

	@Override
	public String convertTargetToSource(byte[] value) {
		if (value == null) {
			return null;
		} else {
			return new String(value, UTF8_CHARSET);
		}
	}

}
