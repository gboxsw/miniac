package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * The converter that chains two converters.
 * 
 * @param <S>
 *            the source type.
 * @param <I>
 *            the type of intermediate value.
 * @param <T>
 *            the target type.
 */
public class ChainingConverter<S, I, T> implements Converter<S, T> {

	/**
	 * The first converter.
	 */
	private final Converter<S, I> converter1;

	/**
	 * The seconds converter.
	 */
	private final Converter<I, T> converter2;

	/**
	 * Constructs the chaining converter.
	 * 
	 * @param converter1
	 *            the first converter.
	 * @param converter2
	 *            the seconds converter.
	 */
	public ChainingConverter(Converter<S, I> converter1, Converter<I, T> converter2) {
		this.converter1 = converter1;
		this.converter2 = converter2;
		if ((converter1 == null) || (converter2 == null)) {
			throw new NullPointerException("None of utilized converters cannot be null.");
		}
	}

	@Override
	public T convertSourceToTarget(S value) {
		return converter2.convertSourceToTarget(converter1.convertSourceToTarget(value));
	}

	@Override
	public S convertTargetToSource(T value) {
		return converter1.convertTargetToSource(converter2.convertTargetToSource(value));
	}

}
