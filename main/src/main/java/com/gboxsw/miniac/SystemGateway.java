package com.gboxsw.miniac;

import java.util.Map;

/**
 * Gateway for system messaging.
 */
final class SystemGateway extends Gateway {

	/**
	 * Default (pre-defined) name of the gateway.
	 */
	public static final String DEFAULT_ID = "$SYS";
	
	/**
	 * Allowed system commands.
	 */
	private static final String[] allowedCommands = { "exit", "save" };

	@Override
	protected void onAddTopicFilter(String topicFilter) {
		// nothing to do
	}

	@Override
	protected void onRemoveTopicFilter(String topicFilter) {
		// nothing to do
	}

	@Override
	protected void onStart(Map<String, Bundle> bundles) {
		handleReceivedMessage(new Message("start"));
	}

	@Override
	protected void onPublish(Message message) {
		getApplication().handlePublishedSystemMessage(message);
	}

	@Override
	protected void onSaveState(Map<String, Bundle> outBundles) {
		// nothing to do
	}

	@Override
	protected void onStop() {
		// nothing to do
	}

	@Override
	protected boolean isValidTopicName(String topic) {
		topic = topic.toLowerCase();
		for (String commandTopic : allowedCommands) {
			if (commandTopic.equals(topic)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Emits a system message.
	 * 
	 * @param topic
	 *            the topic of the message.
	 * @param payload
	 *            the payload of the message.
	 */
	void emitSystemMessage(String topic, byte[] payload) {
		handleReceivedMessage(new Message(topic, payload));
	}
}
