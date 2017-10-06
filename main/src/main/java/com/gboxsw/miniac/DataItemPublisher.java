package com.gboxsw.miniac;

import java.util.logging.*;

/**
 * Automatic publisher of data item values.
 */
public final class DataItemPublisher implements Subscription {

	/**
	 * Logger.
	 */
	private static final Logger logger = Logger.getLogger(DataItemPublisher.class.getName());

	/**
	 * Converter of value to a message payload.
	 * 
	 * @param <T>
	 *            type of value to be converted to message payload.
	 */
	public static interface ValueToPayloadConverter<T> {
		/**
		 * Converts the value to byte array.
		 * 
		 * @param value
		 *            the value to be converted.
		 * @return the value converted to byte array.
		 */
		public byte[] convert(T value);
	}

	/**
	 * Subscription to changes of data item.
	 */
	private final Subscription subscription;

	/**
	 * Constructs the publisher of data item values.
	 * 
	 * @param dataItem
	 *            the data item associated to an application.
	 * @param topic
	 *            the topic where message is published after value of data item
	 *            is changed.
	 * @param converter
	 *            the converter that converts value of data item to message
	 *            payload.
	 */
	private <T> DataItemPublisher(final DataItem<T> dataItem, String topic, final Converter<T, byte[]> converter) {
		this(dataItem, topic, new ValueToPayloadConverter<T>() {
			@Override
			public byte[] convert(T value) {
				return converter.convertSourceToTarget(value);
			}
		});

		if (converter == null) {
			throw new NullPointerException("Converter cannot be null.");
		}
	}

	/**
	 * Constructs the publisher of data item values.
	 * 
	 * @param dataItem
	 *            the data item associated to an application.
	 * @param topic
	 *            the topic where message is published after value of data item
	 *            is changed.
	 * @param converter
	 *            the converter that converts value of data item to message
	 *            payload.
	 */
	private <T> DataItemPublisher(final DataItem<T> dataItem, final String topic,
			final ValueToPayloadConverter<T> converter) {
		if (dataItem == null) {
			throw new NullPointerException("Data item cannot be null.");
		}

		if (converter == null) {
			throw new NullPointerException("Converter cannot be null.");
		}

		final Application app = dataItem.getApplication();
		if (app == null) {
			throw new IllegalStateException("Data item is not associated to an application.");
		}

		if (!Application.isValidTopicName(topic)) {
			throw new IllegalArgumentException("Topic is not valid.");
		}

		subscription = app.subscribe(dataItem.getId(), new SimpleMessageListener() {
			@Override
			public void onMessage() {
				byte[] payload = null;
				try {
					payload = converter.convert(dataItem.getValue());
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Value conversion of data item " + dataItem.getId() + " failed.", e);
					return;
				}

				try {
					app.publish(new Message(topic, payload));
				} catch (Exception e) {
					logger.log(Level.SEVERE,
							"Publication of value of data item " + dataItem.getId() + " to topic " + topic + " failed.",
							e);
				}
			}
		});
	}

	@Override
	public void close() {
		subscription.close();
	}

	@Override
	public String getTopicFilter() {
		return subscription.getTopicFilter();
	}

	/**
	 * Creates and returns publisher of data item values.
	 * 
	 * @param dataItem
	 *            the data item whose value are published, the data item must be
	 *            associated to an application.
	 * @param topic
	 *            the topic where message is published after value of data item
	 *            is changed.
	 * @param converter
	 *            the converter that converts value of data item to message
	 *            payload.
	 * @param <T>
	 *            the value type of published data item.
	 * 
	 * @return the closable publisher.
	 */
	public static <T> DataItemPublisher create(DataItem<T> dataItem, String topic,
			ValueToPayloadConverter<T> converter) {
		return new DataItemPublisher(dataItem, topic, converter);
	}

	/**
	 * Creates and returns publisher of data item values.
	 * 
	 * @param dataItem
	 *            the data item whose value are published, the data item must be
	 *            associated to an application.
	 * @param topic
	 *            the topic where message is published after value of data item
	 *            is changed.
	 * @param converter
	 *            the converter that converts value of data item to message
	 *            payload.
	 * @param <T>
	 *            the value type of published data item.
	 * 
	 * @return the closable publisher.
	 */
	public static <T> DataItemPublisher create(DataItem<T> dataItem, String topic, Converter<T, byte[]> converter) {
		return new DataItemPublisher(dataItem, topic, converter);
	}
}
