package com.gboxsw.miniac;

/**
 * The collection of device instances. The class simplifies management of device
 * instances.
 */
public class Devices {

	/**
	 * Internal register of devices.
	 */
	private static final InstanceRegister<Device> instances = new InstanceRegister<>(true);

	/**
	 * Private constructor (no instances of the class are allowed).
	 */
	private Devices() {

	}

	/**
	 * Register instance of a single-instance device.
	 * 
	 * @param device
	 *            the device instance.
	 */
	public static void register(Device device) {
		instances.register(device);
	}

	/**
	 * Register instance of a multi-instance device.
	 *
	 * @param device
	 *            the device instance.
	 * @param id
	 *            the identifier of the instance.
	 */
	public static void register(Device device, String id) {
		instances.register(device, id);
	}

	/**
	 * Returns the registered instance of a single-instance module.
	 * 
	 * @param deviceClass
	 *            the class of the device.
	 * @param <T>
	 *            the class of the device.
	 * @return the registered instance.
	 */
	public static <T extends Device> T getInstance(Class<T> deviceClass) {
		return instances.get(deviceClass);
	}

	/**
	 * Returns the registered instance of a multi-instance device.
	 * 
	 * @param deviceClass
	 *            the class of the device.
	 * @param id
	 *            the identifier of the instance.
	 * @param <T>
	 *            the class of the device.
	 * @return the registered instance.
	 */
	public static <T extends Device> T getInstance(Class<T> deviceClass, String id) {
		return instances.get(deviceClass, id);
	}
}
