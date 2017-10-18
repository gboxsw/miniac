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
	 * String representing empty content.
	 */
	private static final String EMPTY_CONTENT = "";

	/**
	 * The topic of message.
	 */
	private final String topic;

	/**
	 * The payload of message.
	 */
	private final byte[] payload;

	/**
	 * Cached content of the message.
	 */
	private volatile String cachedContent;

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
	 * @param content
	 *            the content.
	 */
	public Message(String topic, String content) {
		this(topic, (content == null) ? EMPTY_PAYLOAD : content.getBytes(UTF8_CHARSET));
		cachedContent = (content == null) ? EMPTY_CONTENT : content;
	}

	/**
	 * Constructs a message with empty payload.
	 * 
	 * @param topic
	 *            the topic.
	 */
	public Message(String topic) {
		this(topic, (String) null);
	}

	/**
	 * Returns the topic of the message.
	 * 
	 * @return the topic.
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * Returns the payload of the message. The returned array is internal array
	 * of the message. The content of the array cannot be modified.
	 * 
	 * @return the payload.
	 */
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Returns the cloned payload of the message, i.e., the content of the
	 * returned array can be modified.
	 * 
	 * @return the cloned payload.
	 */
	public byte[] getPayloadClone() {
		return payload.clone();
	}

	/**
	 * Returns payload encoded as UTF-8 string.
	 * 
	 * @return the encoded payload.
	 */
	public String getContent() {
		if (cachedContent != null) {
			return cachedContent;
		}

		if (payload.length == 0) {
			return EMPTY_CONTENT;
		}

		cachedContent = new String(this.payload, UTF8_CHARSET);
		return cachedContent;
	}

	/**
	 * Clones the message with changed topic.
	 * 
	 * @param newTopic
	 *            the topic of returned the message.
	 * @return the cloned message with modified topic.
	 */
	public Message cloneWithNewTopic(String newTopic) {
		Message result = new Message(newTopic, payload);
		if (cachedContent != null) {
			result.cachedContent = cachedContent;
		}

		return result;
	}

	/**
	 * Creates topic by merging topic levels.
	 * 
	 * @param topicLevels
	 *            the topic levels.
	 * @return the topic.
	 */
	public static String createTopic(String... topicLevels) {
		if (topicLevels == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		boolean firstTopicLevel = true;
		for (String topicLevel : topicLevels) {
			if (topicLevel == null) {
				continue;
			}

			if (!firstTopicLevel) {
				sb.append('/');
			}

			sb.append(topicLevel);
			firstTopicLevel = false;
		}

		return sb.toString();
	}
}
