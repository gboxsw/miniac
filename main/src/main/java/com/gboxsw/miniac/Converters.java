package com.gboxsw.miniac;

import com.gboxsw.miniac.converters.*;

/**
 * Collection of commonly used converters and helper methods.
 */
public class Converters {

	/**
	 * Default boolean to text converter.
	 */
	public static final BooleanToTextConverter BOOL2TEXT = new BooleanToTextConverter("1", "0", false);

	/**
	 * Default long to text converter.
	 */
	public static final LongToTextConverter LONG2TEXT = new LongToTextConverter();

	/**
	 * Default int to text converter.
	 */
	public static final IntToTextConverter INT2TEXT = new IntToTextConverter();

	/**
	 * Default double to text converter.
	 */
	public static final DoubleToTextConverter DOUBLE2TEXT = new DoubleToTextConverter();

	/**
	 * Default string converter.
	 */
	public static final StringConverter STRING2TEXT = new StringConverter();

	/**
	 * Returns the converter that realizes conversion in reverse direction
	 * comparing to provided converter.
	 * 
	 * @param converter
	 *            the converter.
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
	 * @return the chaining converter.
	 */
	public static <S, T, I> Converter<S, T> chain(Converter<S, I> converter1, Converter<I, T> converter2) {
		return new ChainingConverter<>(converter1, converter2);
	}
}
