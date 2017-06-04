package com.gboxsw.miniac;

import java.util.Map;

/**
 * Gateway for internal messaging using mailboxes.
 */
final class MailboxGateway extends Gateway {

	@Override
	protected void onStart(Map<String, Bundle> bundles) {
		// nothing to do
	}

	@Override
	protected void onAddTopicFilter(String topicFilter) {
		// nothing to do
	}

	@Override
	protected void onRemoveTopicFilter(String topicFilter) {
		// nothing to do
	}

	@Override
	protected void onPublish(Message message) {
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
	protected boolean isValidTopicName(String topicName) {
		// mailbox topics must start with mb- prefix
		if (!topicName.startsWith("mb-")) {
			return false;
		}

		// only single level topic are allowed
		if (topicName.indexOf('/') >= 0) {
			return false;
		}

		return true;
	}
}
