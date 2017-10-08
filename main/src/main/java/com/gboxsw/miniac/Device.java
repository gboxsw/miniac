package com.gboxsw.miniac;

/**
 * The base class for a device - a group of data items that can be managed by
 * the class {@link Devices}.
 */
public class Device {

	/**
	 * Protected constructor checking number of instances.
	 */
	protected Device() {
		SingletonHelper.notifyNewInstance(this);
	}

}
