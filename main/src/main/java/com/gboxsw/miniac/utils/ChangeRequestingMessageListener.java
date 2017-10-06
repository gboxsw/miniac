package com.gboxsw.miniac.utils;

import com.gboxsw.miniac.*;

/**
 * Message listener that transforms payload of received message to change
 * request of associated data item.
 * 
 * @param <T>
 *            the value type of data item.
 */
public class ChangeRequestingMessageListener<T> implements MessageListener {

	/**
	 * The converter utilized to convert message payload to requested value of
	 * data item.
	 */
	private final Converter<byte[], T> converter;

	/**
	 * The associated data item whose value is requested to change when a
	 * message is received.
	 */
	private final DataItem<T> dataItem;

	/**
	 * Indicates whether null values or conversion exceptions are ignored, i.e.,
	 * no change request is executed in these cases.
	 */
	private final boolean ignoreNullValues;

	/**
	 * Constructs the message listener.
	 * 
	 * @param dataItem
	 *            the data item whose value is requested to change.
	 * @param converter
	 *            the converter that converts message payload to requested value
	 *            of data item.
	 * @param ignoreNullValues
	 *            if set to true, when result of conversion is null, then no
	 *            change request is generated.
	 */
	public ChangeRequestingMessageListener(DataItem<T> dataItem, Converter<byte[], T> converter,
			boolean ignoreNullValues) {
		if (dataItem == null) {
			throw new NullPointerException("Data item cannot be null.");
		}

		if (converter == null) {
			throw new NullPointerException("Converter cannot be null.");
		}

		if (dataItem.isReadOnly()) {
			throw new IllegalArgumentException("Data item cannot be read-only.");
		}

		this.dataItem = dataItem;
		this.converter = converter;
		this.ignoreNullValues = ignoreNullValues;
	}

	/**
	 * Constructs the message listener that ignores null values.
	 * 
	 * @param dataItem
	 *            the data item whose value is requested to change.
	 * @param converter
	 *            the converter that converts message payload to requested value
	 *            of data item.
	 */
	public ChangeRequestingMessageListener(DataItem<T> dataItem, Converter<byte[], T> converter) {
		this(dataItem, converter, true);
	}

	@Override
	public void onMessage(Message message) {
		T value = null;
		try {
			value = converter.convertSourceToTarget(message.getPayload());
		} catch (Exception e) {
			value = null;
		}

		if ((value == null) && (ignoreNullValues)) {
			return;
		}

		dataItem.requestChange(value);
	}

}
