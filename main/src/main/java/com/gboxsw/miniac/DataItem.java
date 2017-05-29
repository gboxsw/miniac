package com.gboxsw.miniac;

import java.util.*;
import java.util.logging.*;
import java.util.regex.Pattern;

/**
 * Base class for data items.
 * 
 * @param <T>
 *            the type of value.
 */
public abstract class DataItem<T> {

	/**
	 * Pattern defining a level of valid identifier of a data item.
	 */
	private static final Pattern ID_PATTERN = Pattern.compile("^[\\.a-zA-Z0-9_]+$");

	/**
	 * Logger.
	 */
	private static final Logger logger = Logger.getLogger(DataItem.class.getName());

	/**
	 * States of a data item.
	 */
	private enum State {
		CREATED, ATTACHED, ACTIVATING, ACTIVE, DEACTIVATING, DEACTIVATED
	};

	/**
	 * Identifier of the data item.
	 */
	private volatile String id;

	/**
	 * Identifier of the data item within the gateway.
	 */
	private volatile String idInGateway;

	/**
	 * The gateway to which the data item is attached.
	 */
	private volatile DataGateway gateway;

	/**
	 * The application to which the data item is attached.
	 */
	private volatile Application application;

	/**
	 * Current state of the data item.
	 */
	private volatile State state = State.CREATED;

	/**
	 * Type of the data item.
	 */
	private final Class<T> type;

	/**
	 * Indicates whether the data item is read-only.
	 */
	private final boolean readOnly;

	/**
	 * The value stored in the data item.
	 */
	private volatile T value = null;

	/**
	 * List of dependencies of this data item.
	 */
	private List<DataItem<?>> dependencies;

	/**
	 * List of data items that are dependent on this data item.
	 */
	private List<DataItem<?>> dependants;

	/**
	 * Indicates whether there is a pending request for synchronization of the
	 * data item.
	 */
	private volatile boolean synchronizationPending = false;

	/**
	 * Synchronization lock controlling the binding.
	 */
	private final Object attachLock = new Object();

	/**
	 * Constructs the data item.
	 * 
	 * @param type
	 *            the type of data item.
	 * @param readOnly
	 *            true, if the data item is read-only, false otherwise.
	 */
	public DataItem(Class<T> type, boolean readOnly) {
		if (type == null) {
			throw new NullPointerException("Type of data item cannot be null.");
		}

		this.type = type;
		this.readOnly = readOnly;
	}

	/**
	 * Attaches the data item to a managing data gateway.
	 * 
	 * @param id
	 *            the identifier of the data item.
	 * @param idInGateway
	 *            the identifier of the data item within the gateway.
	 * @param gateway
	 *            the gateway that manages the data item.
	 */
	final void attachToGateway(String id, String idInGateway, DataGateway gateway) {
		synchronized (attachLock) {
			if (this.gateway != null) {
				throw new IllegalStateException("The data item is already attached to a gateway.");
			}

			// note: the order of assignments is important (the gateway must be
			// assigned as the last)
			this.id = id;
			this.idInGateway = idInGateway;
			this.application = gateway.getApplication();
			this.gateway = gateway;
			this.state = State.ATTACHED;
		}
	}

	/**
	 * Returns the identifier of the data item in attached application.
	 * 
	 * @return the identifier of the data item.
	 */
	public final String getId() {
		return id;
	}

	/**
	 * Returns the application to which the data item is attached.
	 * 
	 * @return the application.
	 */
	public final Application getApplication() {
		return application;
	}

	/**
	 * Returns the type of the data item.
	 * 
	 * @return the type of data item.
	 */
	public final Class<T> getType() {
		return type;
	}

	/**
	 * Returns whether the data item is read-only.
	 * 
	 * @return true, if the data item is read-only, false otherwise.
	 */
	public final boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Returns the value of the data item.
	 * 
	 * @return the value.
	 */
	public final T getValue() {
		return value;
	}

	/**
	 * Returns whether the value of the data item is valid, i.e., it is not
	 * null.
	 * 
	 * @return true, if the value of the data item is valid, false otherwise.
	 */
	public final boolean hasValidValue() {
		return value != null;
	}

	/**
	 * Requests change of value of the data item.
	 * 
	 * @param newValue
	 *            the desired value of the data item.
	 */
	public final void requestChange(T newValue) {
		if (readOnly) {
			throw new UnsupportedOperationException("The data item is read-only. Its value cannot be changed.");
		}

		if (gateway != null) {
			application.pushChangeRequest(this, newValue);
		}
	}

	/**
	 * Invalidates value of the data item.
	 */
	protected final void invalidate() {
		if (gateway != null) {
			// no action if there is a pending synchronization request
			if (synchronizationPending) {
				return;
			}

			// mark that there is a pending synchronization request
			synchronizationPending = true;
			application.pushSynchronizationRequest(this);
		} else {
			throw new IllegalStateException("The data item is not attached to an application.");
		}
	}

	/**
	 * Immediately updates the values of the data item from its source. The
	 * method should be invoked from the main thread of the associated
	 * application.
	 */
	protected final void update() {
		if ((state != State.ACTIVATING) && (state != State.ACTIVE)) {
			throw new IllegalStateException("Only activating or active data item can be updated.");
		}

		if (!application.isInApplicationThread()) {
			throw new IllegalThreadStateException(
					"The method \"update\" can be invoked only in the main application thread. Use the method \"invalidate\" instead of the method \"update\".");
		}

		synchronizeValue();
	}

	/**
	 * Set dependencies of this data item. This method can be invoked only in
	 * the {@link DataItem#onActivate()} method.
	 * 
	 * @param dataItems
	 *            data items on which this data item is dependent.
	 */
	protected final void setDependencies(DataItem<?>... dataItems) {
		if ((state != State.ACTIVATING) || !gateway.isInsideActivationCodeOfDataItem(this)) {
			throw new UnsupportedOperationException("The method can be invoked only from onActive method.");
		}

		if (dataItems != null) {
			dependencies = new ArrayList<>();
			for (DataItem<?> dataItem : dataItems) {
				if (dataItem == null) {
					throw new NullPointerException("No data item can be null.");
				}

				if (dataItem == this) {
					throw new IllegalArgumentException("Circular dependency detected.");
				}

				if (getApplication() != dataItem.getApplication()) {
					throw new IllegalArgumentException("Dependencies must be attached to same application.");
				}

				dependencies.add(dataItem);
			}
		}
	}

	/**
	 * Activates the data item. The methods is invoked by the application in its
	 * main thread. Hence, no synchronization is required.
	 * 
	 * @param savedState
	 *            the saved state of the data item from the previous launch of
	 *            the application.
	 */
	void activate(Bundle savedState) {
		state = State.ACTIVATING;
		onActivate(savedState);
		processDependencies();
		state = State.ACTIVE;
	}

	/**
	 * Deactivates the data item. The methods is invoked by the application in
	 * its main thread. Hence, no synchronization is required.
	 */
	void deactivate() {
		state = State.DEACTIVATING;
		// unregister as dependant
		if (dependencies != null) {
			for (DataItem<?> dependency : dependencies) {
				if (dependency.dependants != null) {
					dependency.dependants.remove(this);
					if (dependency.dependants.isEmpty()) {
						dependency.dependants = null;
					}
				}
			}
		}

		// clean dependencies
		dependencies = null;

		onDeactivate();
		state = State.DEACTIVATED;
	}

	/**
	 * Processes and setups the configured dependencies.
	 */
	private void processDependencies() {
		if (dependencies == null) {
			return;
		}

		// clean-up configured dependencies
		Set<DataItem<?>> uniqueDependencies = new HashSet<>(dependencies);
		if (uniqueDependencies.isEmpty()) {
			dependencies = null;
			return;
		} else {
			dependencies = new ArrayList<>(uniqueDependencies);
		}

		// detect circular dependencies
		for (DataItem<?> dependency : dependencies) {
			if (dependency.isDependentOn(this)) {
				dependencies = null;
				throw new IllegalStateException("Circular dependency of data item \"" + getId() + "\" detected.");
			}
		}

		// register as dependant
		for (DataItem<?> dependency : dependencies) {
			if (dependency.dependants == null) {
				dependency.dependants = new ArrayList<>();
			}

			dependency.dependants.add(this);
		}
	}

	/**
	 * Returns whether this data item is (recursively) dependent on a given data
	 * item. The method is invoked in the main thread of the associated
	 * application.
	 * 
	 * @param dataItem
	 *            the data item.
	 * @return true, if this data item is dependent on the given data item,
	 *         false otherwise.
	 */
	private boolean isDependentOn(DataItem<?> dataItem) {
		if (dependencies == null) {
			return false;
		}

		for (DataItem<?> dependency : dependencies) {
			if ((dependency == dataItem) || dependency.isDependentOn(dataItem)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Synchronizes the value of data item. The methods is invoked by the
	 * application in its main thread. Hence, no synchronization is required.
	 */
	void synchronizeValue() {
		synchronizationPending = false;
		T newValue;
		try {
			newValue = onSynchronizeValue();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Value synchronization of the data item \"" + getId() + "\" failed.", e);
			return;
		}

		boolean valueChanged;
		if (value != null) {
			valueChanged = !value.equals(newValue);
		} else {
			valueChanged = (newValue != null);
		}

		if (!valueChanged) {
			return;
		}

		value = newValue;

		// synchronize dependants
		if (dependants != null) {
			for (DataItem<?> dependant : dependants) {
				dependant.synchronizeValue();
			}
		}

		// notify change of value
		gateway.notifyValueChanged(idInGateway);
	}

	/**
	 * Realizes a request to change value of the data item. The methods is
	 * invoked by the application in its main thread. Hence, no synchronization
	 * is required.
	 * 
	 * @param value
	 *            the desired value.
	 */
	@SuppressWarnings("unchecked")
	void requestValueChange(Object value) {
		try {
			onValueChangeRequested((T) value);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Request to change value of the data item \"" + getId() + "\" failed.", e);
		}
	}

	/**
	 * Life-cycle method called when application activates attached data items.
	 * The methods is invoked by the application in its main thread.
	 * 
	 * @param savedState
	 *            the saved state of the data item from the previous launch of
	 *            the application.
	 */
	protected abstract void onActivate(Bundle savedState);

	/**
	 * Life-cycle method called by application in order to synchronize/update
	 * value of the data item. The method is executed in the main thread of the
	 * application instance between {@link #onActivate()} and
	 * {@link DataItem#onDeactivate() onDeactivate}.
	 * 
	 * @return the synchronized value, or null if the value is not available.
	 */
	protected abstract T onSynchronizeValue();

	/**
	 * Life-cycle method called by application in order to request change of
	 * value. The method is executed in the main thread of the application
	 * instance between {@link #onActivate()} and
	 * {@link DataItem#onDeactivate()}.
	 * 
	 * @param newValue
	 *            the desired value of the data item.
	 */
	protected abstract void onValueChangeRequested(T newValue);

	/**
	 * Life-cycle method called by application in order to serialize current
	 * state of the data item. The method is executed in the main thread of the
	 * application instance between {@link #onActivate()} and
	 * {@link DataItem#onDeactivate()}.
	 * 
	 * @param outState
	 *            the bundle for storing state of the data item.
	 */
	protected abstract void onSaveState(Bundle outState);

	/**
	 * Life-cycle method called when application deactivates attached data
	 * items. The methods is invoked by the application in its main thread.
	 */
	protected abstract void onDeactivate();

	/**
	 * Returns whether the identifier is a valid identifier of a data item.
	 * 
	 * @param id
	 *            the identifier.
	 * @return true, if the identifier is valid, false otherwise.
	 */
	public static boolean isValidId(String id) {
		if (id != null) {
			if (!Application.isValidTopicName(id)) {
				return false;
			}

			String[] parsedId = Application.parseTopicHierarchy(id);
			for (String idPart : parsedId) {
				if (!ID_PATTERN.matcher(idPart).matches()) {
					return false; 
				}
			}

			return true;
		} else {
			return false;
		}
	}
}
