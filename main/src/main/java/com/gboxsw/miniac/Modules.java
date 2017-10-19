package com.gboxsw.miniac;

/**
 * The collection of module instances. The class simplifies management of module
 * instances.
 */
public class Modules {

	/**
	 * Internal register of modules.
	 */
	private static final InstanceRegister<Module> instances = new InstanceRegister<>(true);

	/**
	 * Private constructor (no instances of the class are allowed).
	 */
	private Modules() {

	}

	/**
	 * Register instance of a single-instance module.
	 * 
	 * @param module
	 *            the module instance.
	 */
	public static void register(Module module) {
		instances.register(module);
	}

	/**
	 * Register instance of a multi-instance module.
	 *
	 * @param module
	 *            the module instance.
	 * @param id
	 *            the identifier of the instance.
	 */
	public static void register(Module module, String id) {
		instances.register(module, id);
	}

	/**
	 * Returns the registered instance of a single-instance module.
	 * 
	 * @param moduleClass
	 *            the class of the module.
	 * @param <T>
	 *            the class of the module.
	 * @return the registered instance.
	 */
	public static <T extends Module> T getInstance(Class<T> moduleClass) {
		return instances.get(moduleClass);
	}

	/**
	 * Returns the registered instance of a multi-instance module.
	 * 
	 * @param moduleClass
	 *            the class of the module.
	 * @param id
	 *            the identifier of the instance.
	 * @param <T>
	 *            the class of the module.
	 * @return the registered instance.
	 */
	public static <T extends Module> T getInstance(Class<T> moduleClass, String id) {
		return instances.get(moduleClass, id);
	}

	/**
	 * Adds all registered modules to application.
	 * 
	 * @param application
	 *            the application.
	 */
	public static void addToApplication(Application application) {
		if (application == null) {
			throw new NullPointerException("Application cannot be null.");
		}

		for (Module module : instances.getInstances()) {
			application.addModule(module);
		}
	}
}
