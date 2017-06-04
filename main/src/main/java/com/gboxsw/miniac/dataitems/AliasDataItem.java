package com.gboxsw.miniac.dataitems;

import com.gboxsw.miniac.*;

/**
 * Alias data item of another data item.
 *
 * @param <T>
 *            the type of value.
 */
public class AliasDataItem<T> extends DataItem<T> {

	/**
	 * The source data item.
	 */
	private final DataItem<T> source;

	/**
	 * Constructs the data item.
	 * 
	 * @param source
	 *            the data item that is aliased.
	 */
	public AliasDataItem(DataItem<T> source) {
		super(source.getType(), source.isReadOnly());
		this.source = source;
	}

	@Override
	protected void onActivate(Bundle savedState) {
		setDependencies(source);
	}

	@Override
	protected T onSynchronizeValue() {
		return source.getValue();
	}

	@Override
	protected void onValueChangeRequested(T newValue) {
		source.requestChange(newValue);
	}

	@Override
	protected void onSaveState(Bundle outState) {
		// nothing to do
	}

	@Override
	protected void onDeactivate() {
		// nothing to do
	}
}
