package com.gboxsw.miniac.converters;

import com.gboxsw.miniac.Converter;

/**
 * The converter that realizes conversion using an associated converter in
 * reverse direction.
 * 
 * @param <S>
 *            the source type.
 * @param <T>
 *            the target type.
 */
public class ReverseConverter<S, T> implements Converter<S, T> {

	/**
	 * The associated converter.
	 */
	private final Converter<T, S> converter;

	/**
	 * Constructs the reverse converter.
	 * 
	 * @param converter
	 *            the associated (wrapped) converter.
	 */
	public ReverseConverter(Converter<T, S> converter) {
		if (converter == null) {
			throw new NullPointerException("The converter cannot be null.");
		}

		this.converter = converter;
	}

	@Override
	public T convertSourceToTarget(S value) {
		return converter.convertTargetToSource(value);
	}

	@Override
	public S convertTargetToSource(T value) {
		return converter.convertSourceToTarget(value);
	}
}
