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
	 * Indicates whether an exception is throw when requested instance is not
	 * available.
	 */
	private final boolean noInstanceException;

	/**
	 * Internal synchronization lock.
	 */
	private final Object lock = new Object();

	/**
	 * Constructs the register.
	 * 
	 * @param noInstanceException
	 *            true, if an exception should be thrown when requested instance
	 *            is not registered, false, if null should be returned.
	 */
	public InstanceRegister(boolean noInstanceException) {
		this.noInstanceException = noInstanceException;
	}

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

		Class<?> classOfInstance = instance.getClass();
		checkAnnotations(classOfInstance);

		synchronized (lock) {
			InstanceGroup<T> instanceGroup = instanceGroups.get(classOfInstance);
			if (instanceGroup != null) {
				if (instanceGroup.instance == instance) {
					return;
				}

				throw new IllegalStateException(
						"The instance cannot be registered. Another instance/instances registered.");
			}

			instanceGroups.put(classOfInstance, new InstanceGroup<T>(instance));
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

		Class<?> classOfInstance = instance.getClass();
		checkAnnotations(classOfInstance);
		if (SingletonHelper.isSingletonClass(classOfInstance)) {
			throw new IllegalArgumentException(
					"The class " + classOfInstance.getName() + " does not allow multiple instances.");
		}

		synchronized (lock) {
			InstanceGroup<T> instanceGroup = instanceGroups.get(classOfInstance);
			if (instanceGroup == null) {
				instanceGroup = new InstanceGroup<T>();
				instanceGroups.put(classOfInstance, instanceGroup);
			}

			if (instanceGroup.isSingletonGroup()) {
				throw new IllegalStateException(
						"The instance cannot be registered. There is a singleton instance already associated with the class "
								+ classOfInstance.getName());
			}

			if (instanceGroup.instances.containsKey(id)) {
				if (instanceGroup.instances.get(id) == instance) {
					return;
				}

				throw new IllegalArgumentException(
						"The instance cannot be registered. An instance with the same identifier is already registered.");
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
	 * @param aClass
	 *            the class.
	 * @return the singleton.
	 */
	@SuppressWarnings("unchecked")
	public <R extends T> R get(Class<R> aClass) {
		if (aClass == null) {
			throw new NullPointerException("The class cannot be null.");
		}

		synchronized (lock) {
			InstanceGroup<T> instanceGroup = instanceGroups.get(aClass);
			if (instanceGroup == null) {
				if (noInstanceException) {
					throw new IllegalStateException("No registered singleton instance for class " + aClass.getName());
				}

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
	 * @param aClass
	 *            the class.
	 * @param id
	 *            the identifier of the instance.
	 * @return the instance.
	 */
	@SuppressWarnings("unchecked")
	public <R extends T> R get(Class<R> aClass, String id) {
		if (aClass == null) {
			throw new NullPointerException("The class cannot be null.");
		}

		if (id == null) {
			throw new NullPointerException("The identifier of instance cannot be null.");
		}

		synchronized (lock) {
			InstanceGroup<T> instanceGroup = instanceGroups.get(aClass);
			if (instanceGroup == null) {
				if (noInstanceException) {
					throw new IllegalStateException("No registered instances for class " + aClass.getName());
				}

				return null;
			}

			if (instanceGroup.isSingletonGroup()) {
				throw new IllegalArgumentException(
						"The class does not allow multiple instances - only a singleton instance can be retrieved.");
			}

			R result = (R) instanceGroup.instances.get(id);
			if (result == null) {
				if (noInstanceException) {
					throw new IllegalStateException(
							"No registered instance for class " + aClass.getName() + " with id " + id + ".");
				}
			}

			return result;
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

	/**
	 * Checks whether class annotation are non-conflicting and present (if
	 * required).
	 * 
	 * @param aClass
	 *            the class.
	 * 
	 * @throws IllegalArgumentException
	 *             if annotation check failed.
	 */
	private void checkAnnotations(Class<?> aClass) {
		boolean singleton = SingletonHelper.isSingletonClass(aClass);
		boolean multiInstance = SingletonHelper.isMultiInstanceClass(aClass);
		if (singleton && multiInstance) {
			throw new IllegalArgumentException("The class " + aClass.getName()
					+ " is annotated as a singleton class and also as a multi-instance class.");
		}
	}
}
