package com.gboxsw.miniac;

import java.nio.charset.Charset;

/**
 * The message.
 */
public class Message {

	/**
	 * UTF-8 charset.
	 */
	private static final Charset UTF8_CHARSET = Charset.forName("utf-8");

	/**
	 * Zero-sized array for indicating an empty message payload.
	 */
	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	/**
	 * The topic of message.
	 */
	private final String topic;

	/**
	 * The payload of message.
	 */
	private final byte[] payload;

	/**
	 * Constructs a message.
	 * 
	 * @param topic
	 *            the topic.
	 * @param payload
	 *            the payload (it is not allowed to modify the payload array in
	 *            the future).
	 */
	public Message(String topic, byte[] payload) {
		this.topic = topic;
		this.payload = (payload == null) ? EMPTY_PAYLOAD : payload;
	}

	/**
	 * Constructs a message.
	 * 
	 * @param topic
	 *            the topic.
	 * @param payload
	 *            the payload.
	 */
	public Message(String topic, String payload) {
		this(topic, (payload == null) ? EMPTY_PAYLOAD : payload.getBytes(UTF8_CHARSET));
	}

	/**
	 * Constructs a message with empty payload.
	 * 
	 * @param topic
	 *            the topic.
	 */
	public Message(String topic) {
		this(topic, EMPTY_PAYLOAD);
	}

	public String getTopic() {
		return topic;
	}

	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Returns payload encoded as UTF-8 string.
	 * 
	 * @return the encoded payload.
	 */
	public String getContent() {
		if ((payload == null) || (payload.length == 0)) {
			return "";
		}

		return new String(this.payload, UTF8_CHARSET);
	}
}
