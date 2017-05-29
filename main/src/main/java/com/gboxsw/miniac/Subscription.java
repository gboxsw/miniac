package com.gboxsw.miniac;

/**
 * Represents a subscription to topic-filtered messages.
 */
public interface Subscription {

	/**
	 * Returns the topic filter.
	 * 
	 * @return the topic filter.
	 */
	public String getTopicFilter();

	/**
	 * Closes the subscription.
	 */
	public void close();

}