package com.gboxsw.miniac;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Base class for messaging gateways.
 */
public abstract class Gateway {

	/**
	 * Pattern defining valid identifier of a gateway.
	 */
	private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");

	/**
	 * Identifier of the messaging gateway.
	 */
	private volatile String id;

	/**
	 * The application to which the gateway is attached.
	 */
	private volatile Application application;

	/**
	 * Indicates whether the gateway is running, i.e., it has been started.
	 */
	private boolean running = false;

	/**
	 * Synchronization lock controlling binding to an application.
	 */
	private final Object lock = new Object();

	/**
	 * Protected constructor checking number of instances.
	 */
	protected Gateway() {
		SingletonHelper.notifyNewInstance(this);
	}

	/**
	 * Attaches the gateway to an application.
	 * 
	 * @param id
	 *            the identifier of the messaging gateway.
	 * @param application
	 *            the application to which the gateway is attached.
	 */
	final void attachToApplication(String id, Application application) {
		if (id == null) {
			throw new NullPointerException("The identifier cannot be null.");
		}

		if (application == null) {
			throw new NullPointerException("The application cannot be null.");
		}

		synchronized (lock) {
			if (this.application != null) {
				throw new IllegalStateException("The gateway is already attached to an application.");
			}

			// note: the order of assignments is important (the application must
			// be assigned as the last)
			this.id = id.intern();
			this.application = application;
		}
	}

	/**
	 * Returns the identifier of the gateway in attached application.
	 * 
	 * @return the identifier of the gateway.
	 */
	public final String getId() {
		return id;
	}

	/**
	 * Returns the application to which the gateway is attached.
	 * 
	 * @return the application.
	 */
	public final Application getApplication() {
		return application;
	}

	/**
	 * Returns whether the gateway is attached to the application.
	 * 
	 * @return true, if the gateway is attached, false otherwise.
	 */
	public final boolean isAttachedToApplication() {
		return application != null;
	}

	/**
	 * Returns the synchronization lock that ensures mutual exclusion before the
	 * gateway is started.
	 * 
	 * @return the synchronization lock.
	 */
	protected Object getLock() {
		return lock;
	}

	/**
	 * Returns whether the gateway is running, i.e., it has been started.
	 * 
	 * @return true, if the gateway is running, false otherwise.
	 */
	protected boolean isRunning() {
		synchronized (lock) {
			return running;
		}
	}

	/**
	 * Handles a received message by forwarding the message to the application
	 * to which the gateway is attached.
	 * 
	 * @param message
	 *            the received message.
	 */
	protected void handleReceivedMessage(Message message) {
		if (application != null) {
			application.pushReceivedMessage(id, message);
		} else {
			throw new IllegalStateException("The gateway is not attached to an application.");
		}
	}

	/**
	 * Starts the gateway. The methods is invoked by the application in its main
	 * thread.
	 * 
	 * @see #onStart(Map)
	 * @param bundles
	 *            the map with bundles storing state of items related to the
	 *            gateway.
	 */
	void start(Map<String, Bundle> bundles) {
		synchronized (lock) {
			running = true;
		}

		onStart(bundles);
	}

	/**
	 * Stops the gateway. The methods is invoked by the application in its main
	 * thread.
	 * 
	 * @see Gateway#onStop()
	 */
	void stop() {
		try {
			onStop();
		} finally {
			synchronized (lock) {
				running = false;
			}
		}
	}

	/**
	 * Life-cycle method called when the gateway is started by the application
	 * to which the gateway is attached. The methods is invoked by the
	 * application in its main thread.
	 * 
	 * @param bundles
	 *            the map with bundles storing state of items related to the
	 *            gateway.
	 */
	protected abstract void onStart(Map<String, Bundle> bundles);

	/**
	 * Life-cycle method called by the application in order to redirect all
	 * messages matching the topic filter to the application. The method is
	 * executed in the main thread of the application instance between
	 * {@link #onStart onStart} and {@link #onStop onStop} invocations.
	 * 
	 * @param topicFilter
	 *            the topic filter.
	 */
	protected abstract void onAddTopicFilter(String topicFilter);

	/**
	 * Life-cycle method called by the application in order to stop redirection
	 * of messages initiated by a previously added (registered) topic filter.
	 * The method is executed in the main thread of the application instance
	 * between {@link #onStart onStart} and {@link #onStop onStop} invocations.
	 * 
	 * @param topicFilter
	 *            the topic filter.
	 */
	protected abstract void onRemoveTopicFilter(String topicFilter);

	/**
	 * Life-cycle method called by the application in order to publish a message
	 * using the gateway. The method is executed in the main thread of the
	 * application instance between {@link #onStart onStart} and {@link #onStop
	 * onStop} invocations.
	 * 
	 * @param message
	 *            the message.
	 */
	protected abstract void onPublish(Message message);

	/**
	 * Life-cycle method called by the application in order to save state of all
	 * items related to the gateway. The method is executed in the main thread
	 * of the application instance between {@link #onStart onStart} and
	 * {@link #onStop onStop} invocations.
	 * 
	 * @param outBundles
	 *            the map to put bundles defining the state of gateway.
	 */
	protected abstract void onSaveState(Map<String, Bundle> outBundles);

	/**
	 * Life-cycle method called when the gateway is requested to stop by the
	 * application to which the gateway is attached. The method is executed in
	 * the main thread of the application instance.
	 */
	protected abstract void onStop();

	/**
	 * Returns whether the topic name is valid publication topic to this
	 * gateway. The method should be thread-safe.
	 * 
	 * @param topicName
	 *            the topic name.
	 * @return true, if the topic name is valid, false otherwise.
	 */
	protected abstract boolean isValidTopicName(String topicName);

	/**
	 * Returns whether the identifier is a valid identifier of a messaging
	 * gateway.
	 * 
	 * @param id
	 *            the identifier.
	 * @return true, if the identifier is valid, false otherwise.
	 */
	public static boolean isValidId(String id) {
		if (id != null) {
			return ID_PATTERN.matcher(id).matches();
		} else {
			return false;
		}
	}
}
