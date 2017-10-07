package com.gboxsw.miniac;

import java.util.*;

/**
 * The register of instances.
 * 
 * @param <T>
 *            the base class of instances that can be registered.
 */
class InstanceRegister<T> {

	/**
	 * Groups of instances.
	 */
	private static class InstanceGroup<T> {

		/**
		 * Reference to instance in the case of singleton group.
		 */
		private final T instance;

		/**
		 * Map of instances in the case of non-singleton group.
		 */
		private final Map<String, T> instances;

		/**
		 * Constructs singleton instance group.
		 * 
		 * @param instance
		 *            the instance forming the group.
		 */
		private InstanceGroup(T instance) {
			this.instance = instance;
			this.instances = null;
		}

		/**
		 * Constructs non-singleton instance group.
		 */
		private InstanceGroup() {
			instance = null;
			instances = new HashMap<>();
		}

		/**
		 * Returns whether the group is a singleton instance group.
		 * 
		 * @return true, if the group is singleton instance group, i.e., it
		 *         contains only one instance.
		 */
		private boolean isSingletonGroup() {
			return instances == null;
		}
	}

	/**
	 * Mapping of classes to instance groups.
	 */
	private final Map<Class<?>, InstanceGroup<T>> instanceGroups = new HashMap<>();

	/**
	 * Internal synchronization lock.
	 */
	private final Object lock = new Object();

	/**
	 * Registers the instance and marks the class of the instance as the class
	 * that does not allow multiple instances.
	 * 
	 * @param instance
	 *            the instance to be registered.
	 */
	public void register(T instance) {
		if (instance == null) {
			throw new NullPointerException("No instance to register.");
		}

		synchronized (lock) {
			InstanceGroup<T> instanceGroup = instanceGroups.get(instance.getClass());
			if (instanceGroup == null) {
				throw new IllegalStateException(
						"The instance cannot be registered. Another instance/instances registered.");
			}

			instanceGroups.put(instance.getClass(), new InstanceGroup<T>(instance));
		}
	}

	/**
	 * Registers the instance for class that allows multiple instances to be
	 * registered.
	 *
	 * @param instance
	 *            the instance.
	 * @param id
	 *            the identifier of the instance (must be unique only for
	 *            registered instances of the class).
	 */
	public void register(T instance, String id) {
		if (instance == null) {
			throw new NullPointerException("No instance to register.");
		}

		if (id == null) {
			throw new NullPointerException("The identifier cannot be null.");
		}

		synchronized (lock) {
			InstanceGroup<T> instanceGroup = instanceGroups.get(instance.getClass());
			if (instanceGroup == null) {
				instanceGroup = new InstanceGroup<T>();
				instanceGroups.put(instance.getClass(), instanceGroup);
			}

			if (instanceGroup.isSingletonGroup()) {
				throw new IllegalStateException(
						"The instance cannot be registered. The class does not allow multiple instance.");
			}

			if (instanceGroup.instances.containsKey(id)) {
				throw new IllegalArgumentException(
						"The instance cannot be registered. The instance with given identifier is already registered.");
			}

			if (instanceGroup.instances.containsValue(instance)) {
				throw new IllegalArgumentException("The instance is already registered.");
			}

			instanceGroup.instances.put(id, instance);
		}
	}

	/**
	 * Returns singleton associated with given class.
	 * 
	 * @param classOfResult
	 *            the class.
	 * @return the singleton.
	 */
	@SuppressWarnings("unchecked")
	public <R extends T> R get(Class<R> classOfResult) {
		if (classOfResult == null) {
			throw new NullPointerException("The class cannot be null.");
		}

		synchronized (lock) {
			InstanceGroup<T> instanceGroup = instanceGroups.get(classOfResult);
			if (instanceGroup == null) {
				return null;
			}

			if (!instanceGroup.isSingletonGroup()) {
				throw new IllegalArgumentException(
						"The class allows multiple instances. The identifier of instance must be provided.");
			}

			return (R) instanceGroup.instance;
		}
	}

	/**
	 * Returns instance associated with given class.
	 * 
	 * @param classOfResult
	 *            the class.
	 * @param id
	 *            the identifier of the instance.
	 * @return the instance.
	 */
	@SuppressWarnings("unchecked")
	public <R extends T> R get(Class<R> classOfResult, String id) {
		if (classOfResult == null) {
			throw new NullPointerException("The class cannot be null.");
		}

		if (id == null) {
			throw new NullPointerException("The identifier of instance cannot be null.");
		}

		synchronized (lock) {
			InstanceGroup<T> instanceGroup = instanceGroups.get(classOfResult);
			if (instanceGroup == null) {
				return null;
			}

			if (instanceGroup.isSingletonGroup()) {
				throw new IllegalArgumentException(
						"The class does not allow multiple instances - only retrieval of singleton instance is allowed.");
			}

			return (R) instanceGroup.instances.get(id);
		}
	}

	/**
	 * Returns list of all registered instances.
	 * 
	 * @return the list of registered instances.
	 */
	public List<T> getInstances() {
		List<T> result = new ArrayList<>();
		synchronized (lock) {
			for (InstanceGroup<T> instanceGroup : instanceGroups.values()) {
				if (instanceGroup.isSingletonGroup()) {
					result.add(instanceGroup.instance);
				} else {
					result.addAll(instanceGroup.instances.values());
				}
			}
		}

		return result;
	}
}
