package com.gboxsw.miniac;

/**
 * Represents a bidirectional converter between two value types.
 * 
 * @param <S>
 *            the source type.
 * @param <T>
 *            the target type.
 */
public interface Converter<S, T> {

	/**
	 * Converts value of source type to a value of target type.
	 * 
	 * @param value
	 *            the value to be converted.
	 * @return the converted value.
	 */
	public T convertSourceToTarget(S value);

	/**
	 * Converts value of target type to a value of source type.
	 * 
	 * @param value
	 *            the value to be converted.
	 * @return the converted value.
	 */
	public S convertTargetToSource(T value);

}
