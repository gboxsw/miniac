package com.gboxsw.miniac;

/**
 * The interface of a message generator.
 */
public interface MessageProducer {

	/**
	 * Creates and returns a message.
	 * 
	 * @return the message.
	 */
	public Message createMessage();

}
