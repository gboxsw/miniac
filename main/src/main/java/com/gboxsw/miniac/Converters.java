package com.gboxsw.miniac;

import com.gboxsw.miniac.converters.*;

/**
 * Collection of commonly used converters and helper methods.
 */
public class Converters {

	/**
	 * Default string to text-bytes converter.
	 */
	public static final Converter<String, byte[]> STRING_TO_TEXTBYTES = new StringConverter();

	/**
	 * Default text-bytes to string converter.
	 */
	public static final Converter<byte[], String> TEXTBYTES_TO_STRING = reverse(STRING_TO_TEXTBYTES);

	/**
	 * Default boolean to string converter.
	 */
	public static final Converter<Boolean, String> BOOL_TO_STRING = new BooleanToStringConverter("1", "0", false);

	/**
	 * Default boolean to text-bytes converter.
	 */
	public static final Converter<Boolean, byte[]> BOOL_TO_TEXTBYTES = chain(BOOL_TO_STRING, STRING_TO_TEXTBYTES);

	/**
	 * Default long to string converter.
	 */
	public static final Converter<Long, String> LONG_TO_STRING = new LongToStringConverter();

	/**
	 * Default long to text-bytes converter.
	 */
	public static final Converter<Long, byte[]> LONG_TO_TEXTBYTES = chain(LONG_TO_STRING, STRING_TO_TEXTBYTES);

	/**
	 * Default int to string converter.
	 */
	public static final Converter<Integer, String> INT_TO_STRING = new IntToStringConverter();

	/**
	 * Default int to text-bytes converter.
	 */
	public static final Converter<Integer, byte[]> INT_TO_TEXTBYTES = chain(INT_TO_STRING, STRING_TO_TEXTBYTES);

	/**
	 * Default double to string converter.
	 */
	public static final Converter<Double, String> DOUBLE_TO_STRING = new DoubleToStringConverter();

	/**
	 * Default double to text-bytes converter.
	 */
	public static final Converter<Double, byte[]> DOUBLE_TO_TEXTBYTES = chain(DOUBLE_TO_STRING, STRING_TO_TEXTBYTES);

	/**
	 * Returns the converter that realizes conversion in reverse direction
	 * comparing to provided converter.
	 * 
	 * @param converter
	 *            the converter.
	 * @param <S>
	 *            the source type.
	 * @param <T>
	 *            the target type.
	 * @return the reversed converter.
	 */
	public static <S, T> Converter<S, T> reverse(Converter<T, S> converter) {
		return new ReverseConverter<>(converter);
	}

	/**
	 * Returns the converter that chains two converters.
	 * 
	 * @param converter1
	 *            the first converter in the chain.
	 * @param converter2
	 *            the second converter in the chain.
	 * @param <S>
	 *            the source type.
	 * @param <I>
	 *            the type of intermediate value.
	 * @param <T>
	 *            the target type.
	 * @return the chaining converter.
	 */
	public static <S, T, I> Converter<S, T> chain(Converter<S, I> converter1, Converter<I, T> converter2) {
		return new ChainingConverter<>(converter1, converter2);
	}
}
