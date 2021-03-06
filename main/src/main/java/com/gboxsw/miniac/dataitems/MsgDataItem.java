package com.gboxsw.miniac.dataitems;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.gboxsw.miniac.*;

/**
 * Message based data item with content/value converter.
 * 
 * @param <T>
 *            the type of value.
 */
public final class MsgDataItem<T> extends DataItem<T> {

	/**
	 * Handling priority for messages holding new values of data items.
	 */
	public static final int SUBSCRIPTION_HANDLING_PRIORITY = Integer.MAX_VALUE / 2;

	/**
	 * Logger.
	 */
	private static final Logger logger = Logger.getLogger(MsgDataItem.class.getName());

	/**
	 * The converter of a value to an appropriate message content and vice
	 * versa.
	 */
	private final Converter<byte[], T> converter;

	/**
	 * The topic that determines the value of data item.
	 */
	private final String readTopic;

	/**
	 * The topic for receiving requests to change of value.
	 */
	private final String writeTopic;

	/**
	 * The subscription for messages with new value of data item.
	 */
	private volatile Subscription subscription;

	/**
	 * The last received value of the data item.
	 */
	private T remoteValue;

	/**
	 * Constructs the data item.
	 * 
	 * @param readTopic
	 *            the topic where new value is published.
	 * @param writeTopic
	 *            the topic where requests for change of value can be sent. If
	 *            the value is null, the data item is read-only.
	 * @param converter
	 *            the converter that encodes the message payload to value and
	 *            vice versa.
	 * @param type
	 *            the type of data item.
	 */
	public MsgDataItem(String readTopic, String writeTopic, Converter<byte[], T> converter, Class<T> type) {
		super(type, writeTopic == null);
		this.readTopic = readTopic;
		this.writeTopic = writeTopic;
		this.converter = converter;
	}

	/**
	 * Constructs the read-only data item.
	 * 
	 * @param readTopic
	 *            the topic where new value is published.
	 * @param converter
	 *            the converter that encodes the message payload to value.
	 * @param type
	 *            the type of data item.
	 */
	public MsgDataItem(String readTopic, Converter<byte[], T> converter, Class<T> type) {
		this(readTopic, null, converter, type);
	}

	@Override
	protected void onActivate(Bundle savedState) {
		subscription = getApplication().subscribe(readTopic, new MessageListener() {

			@Override
			public void onMessage(Message message) {
				try {
					remoteValue = converter.convertSourceToTarget(message.getPayload());
				} catch (Exception e) {
					remoteValue = null;
					logger.log(Level.WARNING, "Conversion of updating value for data item " + getId() + " failed.", e);
				}
				update();
			}
		}, SUBSCRIPTION_HANDLING_PRIORITY);
	}

	@Override
	protected T onSynchronizeValue() {
		return remoteValue;
	}

	@Override
	protected void onValueChangeRequested(T newValue) {
		byte[] messagePayload;
		try {
			messagePayload = converter.convertTargetToSource(newValue);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Conversion of new value of data item " + getId() + " failed.", e);
			throw e;
		}

		getApplication().publish(new Message(writeTopic, messagePayload));
	}

	@Override
	protected void onSaveState(Bundle outState) {
		// TODO add support for retained state
	}

	@Override
	protected void onDeactivate() {
		subscription.close();
	}

	/**
	 * Returns the topic where remote value of data item is published.
	 * 
	 * @return the read topic.
	 */
	public String getReadTopic() {
		return readTopic;
	}

	/**
	 * Returns the topic where new value of data item should be published.
	 * 
	 * @return the write topic.
	 */
	public String getWriteTopic() {
		return writeTopic;
	}
}
