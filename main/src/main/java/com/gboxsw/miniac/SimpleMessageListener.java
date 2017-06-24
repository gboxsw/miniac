package com.gboxsw.miniac;

/**
 * Simplified listener interface for notifications about receipt of a message.
 * This interface is a convenience interface derived from
 * {@link MessageListener}.
 */
public interface SimpleMessageListener {

	/**
	 * Invoked when a (matching) message is received.
	 */
	public void onMessage();

}
