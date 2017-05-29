package com.gboxsw.miniac;

import java.util.Map;

/**
 * The local gateway that echoes each published message.
 */
public class EchoGateway extends Gateway {

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
		// nothing to do
	}

	@Override
	protected void onPublish(Message message) {
		// echo each received message back
		handleReceivedMessage(message);
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
		return true;
	}
}
