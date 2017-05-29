package com.gboxsw.miniac;

/**
 * The listener interface for handling received messages.
 */
public interface MessageListener {

	/**
	 * Invoked when a message is received.
	 * 
	 * @param message
	 *            the received message.
	 */
	public void messageReceived(Message message);

}
