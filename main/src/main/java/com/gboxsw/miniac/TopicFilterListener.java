package com.gboxsw.miniac;

/**
 * The listener interface for notifying that a message matching a topic filter
 * has been received.
 */
public interface TopicFilterListener {

	/**
	 * Invoked when a matching message is received.
	 */
	public void onMessage();

}
