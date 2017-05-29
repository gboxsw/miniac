package com.gboxsw.miniac;

/**
 * Convenience class representing an application module. The application module
 * is a collection of data items and message handlers.
 */
public abstract class Module {

	/**
	 * The application to which the module is associated.
	 */
	private volatile Application application;

	/**
	 * Returns whether the module has been initialized.
	 */
	private volatile boolean initialized;

	/**
	 * Synchronization lock controlling binding to an application.
	 */
	protected final Object lock = new Object();

	/**
	 * Attaches the module to an application.
	 * 
	 * @param application
	 *            the application to which the gateway is attached.
	 */
	final void attachToApplication(Application application) {
		synchronized (lock) {
			if (this.application != null) {
				throw new IllegalStateException("The module is already attached to an application.");
			}

			this.application = application;
		}
	}

	/**
	 * Returns the application to which the module is associated.
	 * 
	 * @return the application.
	 */
	public Application getApplication() {
		return application;
	}

	/**
	 * Returns whether the module has been initialized.
	 * 
	 * @return true, if the module has been initialized, false otherwise.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * Initializes the module.
	 */
	final void initialize() {
		onInitialize();
		initialized = true;
	}

	/**
	 * Life-cycle method invoked immediately before launching the application.
	 * The module should create and attach data items and message handlers to
	 * the application. The method is invoked in the thread executing the
	 * application launch.
	 */
	protected abstract void onInitialize();
}
